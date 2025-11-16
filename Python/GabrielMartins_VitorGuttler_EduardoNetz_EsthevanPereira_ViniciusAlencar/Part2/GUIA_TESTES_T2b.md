# 🧪 Guia de Testes - T2b Memória Virtual

**Versão**: T2b  
**Data**: 2025-11-16  
**Objetivo**: Validar funcionalidade de memória virtual

---

## 🎯 Pré-requisitos

1. Python 3.6+ instalado
2. Arquivo `sistema_os.py` no diretório atual
3. Terminal com suporte a UTF-8

---

## 🚀 Teste 1: Validação Básica (OBRIGATÓRIO)

### Objetivo
Verificar que código compila e importa sem erros.

### Passos

```bash
cd Part2
python3 -m py_compile sistema_os.py
echo "✅ Compilação bem-sucedida"

python3 -c "import sistema_os; print('✅ Import bem-sucedido')"
```

### Resultado Esperado
```
✅ Compilação bem-sucedida
✅ Import bem-sucedido
```

---

## 🧪 Teste 2: Modo T2a (Compatibilidade)

### Objetivo
Verificar que T2a continua funcionando.

### Configuração
```python
# Em sistema_os.py, linha ~1689
USE_VIRTUAL_MEMORY = False
```

### Passos
```bash
python3 sistema_os.py
```

```
> new fibonacci10
[CRIAÇÃO] Processo 0 criado (Frames: [0, 1], Estado: READY)

> start
[Sistema] Sistema iniciado em modo T2a!

> ps
# Verificar processo executando

> exit
```

### Resultado Esperado
- ✅ Processo cria com todas as páginas alocadas
- ✅ Execução sem page faults
- ✅ Sistema funciona normalmente

---

## 🔥 Teste 3: Modo T2b - Lazy Loading

### Objetivo
Verificar carregamento sob demanda (primeira página apenas).

### Configuração
```python
# Em sistema_os.py, linha ~1689
USE_VIRTUAL_MEMORY = True
```

### Passos
```bash
python3 sistema_os.py
```

```
> new fibonacci10
# Observar log de criação

> memstat
# Verificar frames alocados

> exit
```

### Resultado Esperado
```
[DISK] Programa 'fibonacci10' carregado no disco
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
```

**Verificar**:
- ✅ Apenas 1 frame alocado (não 2 como em T2a)
- ✅ Log menciona "NEVER_LOADED"
- ✅ Programa carregado no disco

---

## 📄 Teste 4: Page Fault Simples

### Objetivo
Observar page fault durante execução.

### Configuração
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 1024  # Memória grande (sem vitimização)
```

### Passos
```bash
python3 sistema_os.py
```

```
> new fibonacci10
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)

> start
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!

# Aguardar execução...
# Observar logs de page fault

> stats
> exit
```

### Resultado Esperado
```
[PAGE FAULT] Processo 0, Página 1
[PAGE FAULT] Frame 1 disponível
[DISK] Carregando página 1 do processo 0 para frame 1...
(aguardar 3 segundos - latência do disco)
[DISK] Página 1 carregada com sucesso no frame 1
```

**Verificar**:
- ✅ Page fault detectado na página 1
- ✅ Frame livre encontrado
- ✅ Carregamento do disco (3s de latência)
- ✅ Processo desbloqueia e continua
- ✅ Programa termina corretamente

---

## 🔥 Teste 5: Vitimização (FIFO)

### Objetivo
Forçar vitimização de páginas quando memória cheia.

### Configuração
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 256  # Memória pequena (4 frames de 64 palavras)
```

### Passos
```bash
python3 sistema_os.py
```

```
> new PC
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)

> start
# Aguardar...
# Observar logs de page fault e vitimização

> stats
> exit
```

### Resultado Esperado
```
[PAGE FAULT] Processo 0, Página 1
[PAGE FAULT] Frame 1 disponível
[DISK] Carregando página 1...
[DISK] Página 1 carregada

[PAGE FAULT] Processo 0, Página 2
[PAGE FAULT] Frame 2 disponível
...

[PAGE FAULT] Processo 0, Página 4
[PAGE FAULT] SEM frames livres - escolhendo vítima
[PAGE FAULT] Vítima: Proc 0, Pág 0, Frame 0 (FIFO)
[DISK] Salvando vítima (Proc 0, Pag 0)...
(aguardar 3s)
[DISK] Vítima salva, frame 0 liberado
[DISK] Carregando página 4 do processo 0 no frame 0...
(aguardar 3s)
[DISK] Página 4 carregada no frame 0
```

**Verificar**:
- ✅ Frames livres esgotam
- ✅ Vítima escolhida (FIFO - mais antiga)
- ✅ Salvamento em swap (3s)
- ✅ Carregamento da nova página (3s)
- ✅ Total: 6s de latência (save + load)

---

## 📊 Teste 6: Múltiplos Processos

### Objetivo
Testar com vários processos concorrentes.

### Configuração
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 512  # Memória média
```

### Passos
```bash
python3 sistema_os.py
```

```
> new nop
[CRIAÇÃO T2b] Processo 0 criado

> new fibonacci10
[CRIAÇÃO T2b] Processo 1 criado

> new fatorial
[CRIAÇÃO T2b] Processo 2 criado

> start
# Observar escalonamento entre processos
# Observar page faults de diferentes processos

> ps
# Ver estado dos processos

> memstat
# Ver frames alocados

> stats
# Ver estatísticas

> stop
> exit
```

### Resultado Esperado
- ✅ 3 processos criados
- ✅ Cada um com apenas página 0 alocada
- ✅ Page faults de processos diferentes
- ✅ Escalonador alterna entre processos
- ✅ Memória compartilhada corretamente

---

## 🔍 Teste 7: Comandos de Debug

### Objetivo
Testar comandos de visualização.

### Passos
```bash
python3 sistema_os.py
```

```
> new fibonacci10

> ps
# Listar processos

> memstat
# Status da memória (frames livres/ocupados)

> dump 0
# Detalhes do processo 0

> dumpm 0 63
# Dump da memória física (frame 0)

> stats
# Estatísticas gerais

> exit
```

### Resultado Esperado
- ✅ Todos os comandos funcionam
- ✅ Informações corretas exibidas
- ✅ Sem erros

---

## 📋 Checklist de Validação

### Funcionalidades Core
- [ ] ✅ Lazy loading funciona (só 1ª página alocada)
- [ ] ✅ Page fault detectado corretamente
- [ ] ✅ Page fault tratado (frame livre)
- [ ] ✅ Vitimização FIFO funciona
- [ ] ✅ Salvamento em swap (3s latência)
- [ ] ✅ Carregamento do disco (3s latência)
- [ ] ✅ Processo bloqueia durante I/O disco
- [ ] ✅ Processo desbloqueia após load
- [ ] ✅ Compatibilidade T2a mantida

### Comportamentos Esperados
- [ ] ✅ Programas terminam corretamente
- [ ] ✅ Múltiplos processos funcionam
- [ ] ✅ Escalonador continua operando
- [ ] ✅ Sem deadlocks ou travamentos
- [ ] ✅ Logs claros e informativos

### Comandos
- [ ] ✅ `new` cria processo T2b
- [ ] ✅ `start` inicia DiskDevice
- [ ] ✅ `ps` lista processos
- [ ] ✅ `memstat` mostra memória
- [ ] ✅ `dump` funciona
- [ ] ✅ `stats` mostra estatísticas
- [ ] ✅ `stop` para threads
- [ ] ✅ `exit` encerra limpo

---

## 🐛 Problemas Conhecidos

### Processo NOP
**Problema**: Gera prints infinitos, polui terminal.

**Solução Temporária**: Não iniciar NOP ou usar com `stop` rápido.

**Solução Definitiva**: Implementar log throttling (ver MELHORIAS_PROPOSTAS.md).

### Observação
Este é um comportamento conhecido e há uma melhoria documentada para resolver.

---

## 📊 Métricas Esperadas

### Tempos de Latência
- **Console I/O**: 2 segundos
- **Disk I/O (load)**: 3 segundos
- **Disk I/O (save)**: 3 segundos
- **Disk I/O (save+load)**: 6 segundos

### Memória
- **T2a (1024 bytes)**: 16 frames
- **T2b (512 bytes)**: 8 frames
- **T2b teste (256 bytes)**: 4 frames
- **Tamanho página**: 16 palavras (64 bytes)

### Page Faults Esperados
- **fibonacci10**: ~1-2 page faults
- **PC (bubble sort)**: ~3-5 page faults
- **Com vitimização**: múltiplos ciclos

---

## ✅ Critérios de Sucesso

### Mínimo Aceitável
- ✅ Código compila e importa
- ✅ T2a funciona (compatibilidade)
- ✅ T2b cria processo com lazy loading
- ✅ Page fault detectado e tratado
- ✅ Programas simples terminam

### Ideal
- ✅ Vitimização funciona
- ✅ Múltiplos processos executam
- ✅ Sem travamentos
- ✅ Logs claros
- ✅ Comandos funcionam

---

## 📞 Suporte

### Documentação
- **README_CONSOLIDADO.md** - Guia completo
- **MELHORIAS_PROPOSTAS.md** - Otimizações
- **RESUMO_IMPLEMENTACAO_T2b.md** - Resumo técnico

### Código
- **sistema_os.py** - Código fonte
- Comentários marcados com T1/T2a/T2b

---

## 🎯 Próximos Passos Após Testes

1. ✅ Validar todos os testes passaram
2. ✅ Documentar qualquer comportamento inesperado
3. ✅ Considerar implementar melhorias (MELHORIAS_PROPOSTAS.md)
4. ✅ Arquivar documentos de análise (opcional)

---

**Status dos Testes**: ⏳ Aguardando Execução

**Executar todos os 7 testes para validação completa!**

---

*Fim do Guia de Testes T2b*
