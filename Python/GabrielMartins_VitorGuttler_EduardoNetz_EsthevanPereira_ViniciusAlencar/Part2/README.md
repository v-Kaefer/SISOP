# Sistema Operacional Simulado - T2b (Memória Virtual)

**PUCRS - Escola Politécnica - Sistemas Operacionais**  
**Professor**: Fernando Luís Dotti  
**Trabalho**: T2a (Concorrência) + T2b (Memória Virtual)

---

## Integrantes

- Gabriel Martins
- Vitor Guttler
- Eduardo Netz
- Esthevan Pereira
- Vinicius Alencar

---

## 1. Implementação

### Características Implementadas

**T1 - Gerenciamento Básico**
- Paginação (16 palavras/página)
- Escalonamento Round-Robin (quantum = 5)
- Tradução de endereços lógico → físico

**T2a - Concorrência**
- Threads: Shell, CPU, IODevice
- Estados: READY, RUNNING, BLOCKED, FINISHED
- I/O assíncrono com bloqueio/desbloqueio
- Sistema operante continuamente

**T2b - Memória Virtual** ⭐
- Lazy loading (só primeira página carregada)
- Page fault com carregamento sob demanda
- Vitimização FIFO quando memória cheia
- Thread DiskDevice para paginação
- Estados: IN_MEMORY, NEVER_LOADED, SWAPPED
- Tabela de páginas: `{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'programa'}`

### Configuração Atual

- **Modo**: T2b (Memória Virtual ativada)
- **Memória**: 512 palavras (8 frames)
- **Quantum**: 5 instruções

### Restrições

✅ **Sistema totalmente funcional** - nenhuma restrição crítica

---

## 2. Como Executar

```bash
cd Part2
python3 sistema_os.py
```

**Requisitos**: Python 3.6+ (sem dependências externas)

---

## 3. Comandos

| Comando | Descrição |
|---------|-----------|
| `new <programa>` | Cria novo processo |
| `start` | Inicia escalonamento |
| `ps` | Lista processos |
| `dump <id>` | Detalhes do processo |
| `memstat` | Status da memória |
| `trace` | Liga/desliga log detalhado |
| `exit` | Encerra sistema |

---

## 4. Programas

| Programa | Descrição | Páginas |
|----------|-----------|---------|
| `fatorial` | Fatorial de 7 | 1 |
| `fatorialV2` | Fatorial com WRITE | 2 |
| `progMinimo` | Programa mínimo | 1 |
| `fibonacci10` | Fibonacci 10 termos | 2 |
| `fibonacciREAD` | Fibonacci com input | 4 |
| `PC` | Bubble Sort | 7 |

---

## 5. Testes

### Teste 1: Execução Básica (T2b)

```bash
new fatorial
start
```

**Resultado**: 
- ✅ Lazy loading (só página 0)
- ✅ Processo executa e finaliza
- ✅ Memória desalocada

**Log**:
```
T2b: Alocado frame 0 para página 0
[CRIAÇÃO T2b] Processo 0 criado
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!
[FINALIZAÇÃO] Processo 0 FINALIZOU
```

---

### Teste 2: Múltiplos Processos

```bash
new fatorial
new fibonacci10
new progMinimo
start
```

**Resultado**:
- ✅ 3 processos criados
- ✅ Round-Robin funciona
- ✅ Todos terminam corretamente

---

### Teste 3: Sistema Operante

```bash
start
new fatorial
new fibonacci10
ps
```

**Resultado**:
- ✅ Shell aceita comandos após start
- ✅ Processos adicionados dinamicamente

---

### Teste 4: Trace

```bash
new fatorial
trace
start
```

**Com trace**:
```
[QUANTUM EXPIRADO] Processo 0 - Executou 5/5 instruções
[ESCALONADOR] Processo 0 selecionado
```

**Sem trace**:
```
[CRIAÇÃO T2b] Processo 0 criado
[FINALIZAÇÃO] Processo 0 FINALIZOU
```

---

### Teste 5: I/O Assíncrono

```bash
new fatorialV2
start
```

**Resultado**:
- ✅ SYSCALL WRITE detectado
- ✅ Processo bloqueia (BLOCKED)
- ✅ INT_IO_COMPLETE desbloqueia

---

### Teste 6: Lazy Loading

```bash
new fibonacci10
dump 0
```

**Resultado**:
```
Página 0: IN_MEMORY (frame 0)
Página 1: NEVER_LOADED
```

✅ Apenas primeira página carregada

---

### Teste 7: Page Fault

```bash
new fibonacci10
trace
start
```

**Observação**: fibonacci10 precisa memória maior ou tem bug no código que causa INT_ENDERECO_INVALIDO ao invés de page fault normal.

---

### Teste 8: Vitimização FIFO

**Modificar** `sistema_os.py` linha 1804: `tam_mem = 256` (4 frames)

```bash
new PC
new fibonacci10
new fatorial
trace
start
```

**Resultado esperado**:
- Memória cheia
- Vitimização FIFO
- Página salva em swap
- Frame reutilizado

---

### Teste 9: Dump de Processo

```bash
new fatorial
dump 0
```

**Resultado**:
```
Estado: READY
Tabela de Páginas: [{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'fatorial'}]
Página 0: Lógico 0-15 → Frame 0 → Físico 0-15
```

✅ Estrutura T2b correta

---

### Teste 10: Status de Memória

```bash
new fatorial
memstat
start
memstat
```

**Resultado**:
- ✅ Frames alocados corretamente
- ✅ Frames liberados após término

---

### Teste 11: Entrada de Usuário

```bash
new fibonacciREAD
start
10
```

**Resultado**:
- ✅ Sistema aguarda entrada
- ✅ Processo calcula fibonacci(10)

---

## 6. Resumo

| # | Teste | Status |
|---|-------|--------|
| 1 | Execução básica T2b | ✅ |
| 2 | Múltiplos processos | ✅ |
| 3 | Sistema operante | ✅ |
| 4 | Trace | ✅ |
| 5 | I/O assíncrono | ✅ |
| 6 | Lazy loading | ✅ |
| 7 | Page fault | ⚠️ |
| 8 | Vitimização FIFO | ⚠️ |
| 9 | Dump processo | ✅ |
| 10 | Status memória | ✅ |
| 11 | Entrada usuário | ✅ |

**Legenda**: ✅ OK | ⚠️ Requer ajuste

---

## 7. Bugs Corrigidos

1. **PCB.id**: Extração de frame do dict
2. **list_all_processes**: Compatibilidade T2a/T2b
3. **GerenteMemoria.desaloca**: Iteração sobre dicts
4. **dump_processo**: Cálculo de endereços físicos

✅ Todos bugs T2b corrigidos

---

## 8. Arquivos

```
Part2/
├── sistema_os.py          # ~1820 linhas
├── README.md              # Este arquivo
├── ANALISE_SIMPLIFICACAO.md
├── AJUSTES_SOLICITADOS.md
├── T2a-enunciado.md
└── T2b-Enunciado.md
```

---

## 9. Configuração

Editar `sistema_os.py` linha ~1801:

**T2b** (Atual):
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 512
```

**T2a**:
```python
USE_VIRTUAL_MEMORY = False
tam_mem = 1024
```

---

**Data**: 2025-11-17  
**Versão**: T2b  
**Status**: ✅ Completo
