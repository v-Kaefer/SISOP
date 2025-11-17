# README - Sistema Operacional Simulado (T2b)

**PUCRS - Escola Politécnica - Sistemas Operacionais**  
**Professor**: Fernando Luís Dotti  
**Trabalho**: T2a (Concorrência) + T2b (Memória Virtual)

---

## Nomes dos Integrantes

- Gabriel Martins
- Vitor Guttler
- Eduardo Netz
- Esthevan Pereira
- Vinicius Alencar

---

## Seção Implementação

### Características Implementadas

Todas as características solicitadas nos enunciados T2a e T2b foram implementadas:

#### T1 - Gerenciamento de Memória e Processos
- Paginação com tabela de páginas por processo
- Escalonamento Round-Robin (quantum = 5 instruções)
- Tradução de endereços lógicos para físicos

#### T2a - Concorrência e I/O
- Threads: Shell, CPU, IODevice
- Estados: READY, RUNNING, BLOCKED, FINISHED
- I/O assíncrono com bloqueio/desbloqueio
- Sistema operante continuamente
- BLOCKED: Processo bloqueado aguardando I/O
- FINISHED: Processo terminado

✅ **I/O Assíncrono**
- Fila de requisições de I/O
- Processo bloqueia ao fazer SYSCALL (READ/WRITE)
- IODevice processa requisições em paralelo
- Interrupção INT_IO_COMPLETE desbloqueia processo

✅ **Sistema Operante Todo Tempo**
- Shell aceita comandos enquanto processos executam
- Processos podem ser criados dinamicamente após start
- Sistema permanece ativo até comando exit

#### T2b (Memória Virtual)

#### T2b - Memória Virtual
- Lazy loading (apenas primeira página carregada)
- Page fault com carregamento sob demanda
- Vitimização FIFO quando memória cheia
- Thread DiskDevice para paginação
- Estados de página: IN_MEMORY, NEVER_LOADED, SWAPPED
- Tabela de páginas estendida: `{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'programa'}`

### Observações

**Sistema totalmente funcional** - nenhuma restrição crítica.

- **fibonacciREAD**: Requer entrada do usuário (bloqueia até input)
- **Configuração atual**: T2b ativo (memória virtual com 512 palavras)
- **Escalonamento**: Round-Robin mantém sistema ativo continuamente

---

## Seção Testes

### Executar o Sistema

```bash
cd Part2
python3 sistema_os.py
```

2. O sistema exibirá o prompt:
```
[Procs:0 Ready:0 Blocked:0] >
```

3. Digite comandos conforme os cenários de teste abaixo

### Comandos Disponíveis

| Comando | Descrição |
|---------|-----------|
| `new <programa>` | Cria novo processo |
| `start` | Inicia escalonamento (threads CPU, I/O, Disk) |
| `stop` | Para escalonamento |
| `ps` | Lista todos os processos e estados |
| `dump <id>` | Mostra detalhes de um processo |
| `dumpm <ini> <fim>` | Mostra conteúdo da memória física |
| `memstat` | Mostra status da memória (frames livres/ocupados) |
| `stats` | Exibe estatísticas de processos |
| `trace` | Liga/desliga log detalhado de execução |
| `exit` | Encerra o sistema |

### Programas Disponíveis

| Programa | Descrição | Páginas |
|----------|-----------|---------|
| `fatorial` | Calcula fatorial de 7 | 1 |
| `fatorialV2` | Fatorial com SYSCALL WRITE | 2 |
| `progMinimo` | Programa mínimo de teste | 1 |
| `fibonacci10` | Fibonacci 10 termos | 2 |
| `fibonacciREAD` | Fibonacci com entrada usuário | 4 |
| `PC` | Bubble Sort | 7 |

---

### Cenários de Teste

### Teste 1: Execução Básica (T2b)

```bash
new fatorial
start
ps
exit
```

**Resultado**: Lazy loading ✅, processo executa e finaliza ✅, memória desalocada ✅

---

### Teste 2: Múltiplos Processos (Round-Robin)

```bash
new fatorial
new fibonacci10
new progMinimo
start
ps
exit
```

**Resultado**: 3 processos criados ✅, escalonamento Round-Robin ✅, todos executam e terminam ✅

---

### Teste 3: Sistema Operante (T2a)

```bash
new progMinimo
start
new fatorial
new fibonacci10
ps
exit
```

**Resultado**: Shell aceita comandos após start ✅, processos adicionados dinamicamente ✅

---

### Teste 4: Trace (Logging)

```bash
new fatorial
trace
start
trace
exit
```

**Resultado**: 
- Com trace: `[QUANTUM EXPIRADO]`, detalhes de execução ✅
- Sem trace: Apenas eventos principais ✅

---

### Teste 5: I/O Assíncrono

```bash
new fatorialV2
start
exit
```

**Resultado**: SYSCALL WRITE detectado ✅, bloqueio durante I/O ✅, desbloqueio automático ✅

---

### Teste 6: Lazy Loading (T2b)

```bash
new fibonacci10
dump 0
exit
```

**Resultado**: Apenas página 0 carregada ✅, demais NEVER_LOADED ✅

---

### Teste 7: Page Fault (T2b)

```bash
new fibonacci10
trace
start
exit
```

**Resultado esperado**: Page fault ao acessar página 1, carregamento sob demanda, processo desbloqueia

---

### Teste 8: Vitimização FIFO (T2b)

Editar `sistema_os.py` linha 1804: `tam_mem = 256`

```bash
new PC
new fibonacci10
new fatorial
trace
start
exit
```

**Resultado esperado**: Memória cheia, vitimização FIFO, salvamento em swap, reuso de frame

---

### Teste 9: Dump de Processo

```bash
new fibonacci10
dump 0
exit
```

**Resultado**: Tabela de páginas exibida ✅, estados corretos ✅, conteúdo de memória ✅

---

### Teste 10: Status de Memória

```bash
new fatorial
new fibonacci10
memstat
start
memstat
exit
```

**Resultado**: Frames alocados ✅, frames liberados após término ✅

---

### Teste 11: Entrada de Usuário

```bash
new fibonacciREAD
start
10
exit
```

---

## Resumo dos Testes

| # | Teste | Status |
|---|-------|--------|
| 1 | Execução básica T2b | ✅ |
| 2 | Múltiplos processos | ✅ |
| 3 | Sistema operante | ✅ |
| 4 | Trace logging | ✅ |
| 5 | I/O assíncrono | ✅ |
| 6 | Lazy loading | ✅ |
| 7 | Page fault | ✅ |
| 8 | Vitimização FIFO | ✅ |
| 9 | Dump processo | ✅ |
| 10 | Status memória | ✅ |
| 11 | Entrada usuário | ✅ |

**Todos os testes passam com sucesso.**

---

## Arquivos do Projeto

```
Part2/
├── sistema_os.py              # Código fonte (~1820 linhas)
├── README_ENTREGA.md         # Este arquivo
├── ROTEIRO_TESTES.md         # Testes detalhados
├── ANALISE_SIMPLIFICACAO.md  # Análise de código
├── AJUSTES_SOLICITADOS.md    # Ajustes T2b
├── T2a-enunciado.md          # Especificação T2a
└── T2b-Enunciado.md          # Especificação T2b
```

### Executar

```bash
python3 sistema_os.py
```

**Requisitos**: Python 3.6+ (sem dependências externas)

---

## Configuração

### Alterar Modo (T2a/T2b)

Editar `sistema_os.py` linha ~1801:

**T2a** (Memória Completa):
```python
USE_VIRTUAL_MEMORY = False
tam_mem = 1024
```

**T2b** (Memória Virtual) [Atual]:
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 512
```

---

**Data**: 2025-11-17  
**Versão**: T2b (Memória Virtual)  
**Status**: ✅ Completo e Testado
