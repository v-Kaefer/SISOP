# Diagnóstico: INT_ENDERECO_INVALIDO ao invés de INT_PAGE_FAULT

## Problema Identificado

Quando executamos `fibonacci10`, o sistema gera `INT_ENDERECO_INVALIDO` ao invés de `INT_PAGE_FAULT` quando tenta acessar página 1 (que deveria estar em estado NEVER_LOADED).

## Análise do Fluxo

### Programa fibonacci10
- Tamanho: 30 palavras (linhas 1435-1465 do sistema_os.py)
- Páginas necessárias: ceil(30/16) = 2 páginas
  - Página 0: endereços 0-15
  - Página 1: endereços 16-29

### Criação do Processo (T2b)
Log mostra: `"T2b: Alocado frame 0 para página 0, 1 páginas NEVER_LOADED"`
- Isso indica que `total_pages = 2` (correto!)
- Página 0: `{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'fibonacci10'}`
- Página 1: `{'state': 'NEVER_LOADED', 'frame': None, 'disk_location': 'fibonacci10'}`

### Execução
1. **PC=0**: Instrução `LDI 1, -1, 0` - Funciona OK (página 0)
2. **PC=1**: Instrução `STD 1, -1, 20` - Tenta acessar endereço lógico 20
   - Cálculo: page = 20 // 16 = 1, offset = 20 % 16 = 4
   - Deveria verificar `page_table[1]['state']` e encontrar 'NEVER_LOADED'
   - Deveria gerar `INT_PAGE_FAULT`
   - **MAS está gerando `INT_ENDERECO_INVALIDO`**

## Possíveis Causas

### Causa #1: Verificação de bounds na page_table
Linha 129 do sistema_os.py:
```python
if not (0 <= page < len(self.running_process.page_table)):
    self.irpt = Interrupts.INT_ENDERECO_INVALIDO
    return -1
```

Se `len(page_table)` for 1 ao invés de 2, então `page=1` falharia neste teste.

### Causa #2: Process não inicializado corretamente
O processo pode não ter sido criado com a page_table completa.

## Solução Proposta

### Opção 1: Adicionar log de debug
Adicionar prints temporários para verificar:
- Tamanho do page_table após criação
- Valor de `page` e `len(page_table)` em _translate_address

### Opção 2: Verificar criação do PCB
Verificar se o PCB está sendo inicializado corretamente com a page_table completa.

### Opção 3: Usar programa menor para testar
Usar `progMinimo` (14 palavras, 1 página) para verificar se lazy loading funciona.
Depois usar programa maior como `fatorialV2` (20 palavras, 2 páginas).

## Teste Recomendado

```bash
new progMinimo
start
# Deve funcionar - 1 página apenas

new fatorialV2
start
# 20 palavras, 2 páginas - deve gerar page fault em página 1
```

## Código para Adicionar Debug Temporário

Adicionar após linha 129:
```python
print(f"[DEBUG] PC={self.pc}, addr={logical_address}, page={page}, "
      f"page_table_len={len(self.running_process.page_table)}, "
      f"page_table={self.running_process.page_table}")
```

Isso mostraria exatamente o que está acontecendo.
