# Ajustes Solicitados - Alinhamento com Enunciado T2b

## Resumo Executivo

Este documento descreve os ajustes necessários para alinhar a implementação atual do `sistema_os.py` com o modelo exato descrito nos enunciados T2a e T2b.

---

## Ajustes Identificados

### 1. ✅ Estados da Tabela de Páginas (IMPLEMENTADO PARCIALMENTE)

**Status Atual**:
- ✅ Sistema usa estados: `IN_MEMORY`, `NEVER_LOADED`, `SWAPPED`
- ⚠️ **Problema**: Ao vitimar página, estado não é consistentemente atualizado

**Ajuste Necessário**:
```python
# Ao salvar página vítima, atualizar tabela de páginas:
victim_page_entry['state'] = 'ON_DISK'  # ou 'SWAPPED'
victim_page_entry['frame'] = None
victim_page_entry['disk_location'] = swap_location
```

**Localização no código**: Linhas ~820-850 (DiskDevice)

---

### 2. ⚠️ Interrupção INT_PAGE_SAVE_COMPLETE (NÃO IMPLEMENTADO COMPLETAMENTE)

**Status Atual**:
- Sistema define interrupção mas não a usa separadamente
- Salvamento de vítima e carregamento de página são uma operação única

**Ajuste Necessário - Fluxo Completo**:
```
1. Page Fault detectado
   ↓
2. Handler tenta alocar frame
   ↓
3. Se sem frames: escolhe vítima, envia pedido SAVE ao disco
   ↓
4. Disco salva, dispara INT_PAGE_SAVE_COMPLETE
   ↓
5. Handler de INT_PAGE_SAVE_COMPLETE:
   - Marca frame como livre
   - Atualiza tabela da vítima (state=ON_DISK, frame=None)
   - Envia pedido LOAD da página demandada
   ↓
6. Disco carrega, dispara INT_PAGE_LOAD_COMPLETE
   ↓
7. Handler de INT_PAGE_LOAD_COMPLETE:
   - Atualiza tabela da página demandada (state=IN_MEMORY, frame=X)
   - Desbloqueia processo
```

**Localização**: 
- InterruptHandling (linhas ~979-1120)
- DiskDevice (linhas ~681-873)

---

### 3. ⚠️ Responsabilidade entre Handler e DiskDevice

**Problema Atual**:
- DiskDevice atualiza diretamente a tabela de páginas
- Handler não tem controle total sobre decisões de paginação

**Ajuste Necessário**:

**Handler de Page Fault deve**:
- Decidir qual frame usar (livre ou após vítima)
- Atualizar tabela de páginas com mapeamento
- Apenas enviar requisições ao disco

**DiskDevice deve**:
- Receber requisições: "salve página X no local Y" ou "carregue de Y para frame Z"
- Copiar dados entre memória e disco
- Sinalizar interrupção ao terminar
- **NÃO modificar tabelas de páginas**

---

### 4. ⚠️ Consistência no Acesso à Tabela de Páginas

**Problema**:
- IODevice e CPU usam lógicas diferentes para tradução de endereços
- IODevice não trata corretamente estrutura dict do T2b

**Ajuste Necessário**:
```python
# Criar função compartilhada de tradução
def translate_address(logical_addr, page_table, tam_pg):
    page = logical_addr // tam_pg
    offset = logical_addr % tam_pg
    
    if isinstance(page_table[page], dict):
        if page_table[page]['state'] != 'IN_MEMORY':
            return None  # Page fault
        frame = page_table[page]['frame']
    else:
        frame = page_table[page]
    
    return (frame * tam_pg) + offset

# Usar em CPU, IODevice, e outras partes
```

**Localização**: CPU (linhas ~100-200), IODevice (linhas ~593-680)

---

### 5. ✅ Representação em Disco (IMPLEMENTADO)

**Status Atual**:
- ✅ Sistema tem `swap_space` para armazenar páginas
- ⚠️ Precisa garantir atualização consistente da tabela

**Verificar**:
- Toda página em swap_space tem entrada na tabela com `state='ON_DISK'`

---

### 6. ⚠️ IODevice - Tradução de Endereços

**Problema**:
- IODevice faz tradução própria sem reutilizar lógica da CPU
- Pode divergir do comportamento com T2b

**Ajuste Necessário**:
```python
# Em IODevice, reutilizar função de tradução compartilhada
phys_addr = self.translate_address(log_addr, pcb.page_table, tam_pg)
if phys_addr is None:
    # Page fault durante I/O - tratar adequadamente
```

---

### 7. ⚠️ Separação Save/Load (MODELO DIDÁTICO)

**Ajuste Necessário**:

**Operações separadas no DiskDevice**:
```python
# Requisição tipo 1: SAVE
{
    'type': 'SAVE_VICTIM',
    'process_id': pid,
    'page': page_num,
    'frame': current_frame,
    'swap_location': location
}

# Requisição tipo 2: LOAD  
{
    'type': 'LOAD_PAGE',
    'process_id': pid,
    'page': page_num,
    'frame': target_frame,
    'source': 'PROGRAM' | 'SWAP'
}
```

**Duas interrupções distintas**:
- `INT_PAGE_SAVE_COMPLETE`: Após salvar vítima
- `INT_PAGE_LOAD_COMPLETE`: Após carregar página

---

## Priorização dos Ajustes

### Prioridade ALTA (Crítico para modelo correto)
1. **Separar save e load** (Ajuste #7)
2. **Usar INT_PAGE_SAVE_COMPLETE** (Ajuste #2)
3. **Responsabilidades Handler vs Disk** (Ajuste #3)

### Prioridade MÉDIA (Importante para consistência)
4. **Atualizar estados consistentemente** (Ajuste #1)
5. **Unificar tradução de endereços** (Ajuste #4, #6)

### Prioridade BAIXA (Verificação)
6. **Representação em disco** (Ajuste #5)

---

## Impacto das Mudanças

### Código Afetado
- `InterruptHandling` (~100 linhas de alteração)
- `DiskDevice` (~80 linhas de alteração)
- `IODevice` (~30 linhas de alteração)
- `CPU` (~20 linhas de alteração)

### Testes Necessários
- Executar programa multi-página (fibonacci10, PC)
- Forçar page faults com memória limitada
- Verificar vitimização FIFO
- Validar estados de página durante execução

---

## Próximos Passos

1. **Fase 1**: Implementar separação save/load + interrupções
2. **Fase 2**: Mover lógica de decisão para handlers
3. **Fase 3**: Unificar tradução de endereços
4. **Fase 4**: Testar exaustivamente com trace ativado

---

## Observações Importantes

### Por que essas mudanças?

**Objetivo Didático**: O enunciado busca ensinar conceitos de memória virtual de forma clara e separada:
- Page fault → decisão do SO
- Salvamento → operação de I/O
- Carregamento → outra operação de I/O
- Cada operação com sua interrupção

**Modelo Atual**: Operações fundidas, dificulta entendimento do fluxo completo.

**Modelo Ajustado**: Separa responsabilidades conforme arquitetura real de SOs.

---

**Data**: 2025-11-17  
**Documento**: Ajustes Solicitados T2b  
**Status**: Plano de Ação Definido
