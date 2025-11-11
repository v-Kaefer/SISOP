# Simulador de Sistema Operacional Concorrente com Memória Virtual
## PUCRS - Trabalhos T2a/T2b

---

## 📋 Descrição

Este é um simulador completo de sistema operacional multithreaded implementado em Python, que demonstra conceitos fundamentais de sistemas operacionais modernos:

- **Multithreading**: CPU, I/O Device, Shell e Disco operando concorrentemente
- **Escalonamento**: Algoritmo FIFO com preempção por quantum
- **Memória Virtual**: Paginação sob demanda com substituição FIFO
- **I/O Assíncrono**: Operações de entrada/saída não-bloqueantes com DMA simulado
- **Gerenciamento de Processos**: Estados (READY, RUNNING, BLOCKED, FINISHED)
- **Logging Estruturado**: Rastreamento completo de mudanças de estado
- **Dumps de Memória**: Inspeção post-mortem de processos finalizados

---

## 🏗️ Arquitetura do Sistema

### Componentes Principais

#### 1. **Hardware (HW)**
- **CPU (Thread)**: Executa instruções, gerencia quantum, trata interrupções
- **Memory**: Memória física dividida em frames
- **IODevice (Thread)**: Processa requisições de I/O assincronamente

#### 2. **Sistema Operacional (SO)**
- **Gerente de Processos (GP)**: Cria/remove processos, gerencia filas
- **Gerente de Memória (GM)**: Aloca/desaloca frames, implementa paginação
- **Gerente de Disco**: Simula área de swap para páginas vitimadas
- **Scheduler**: Escalonamento FIFO de processos prontos
- **InterruptHandling**: Trata interrupções (PAGE_FAULT, IO_COMPLETION, etc.)
- **SysCallHandling**: Processa chamadas de sistema (SYSCALL, STOP)
- **SystemLogger**: Registra todas as mudanças de estado em formato estruturado

#### 3. **Threads do Sistema**
```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Shell     │     │     CPU     │     │  IODevice   │     │ DiskThread  │
│  (Thread)   │     │  (Thread)   │     │  (Thread)   │     │  (Thread)   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │                   │
       │  Cria Processos   │  Executa          │  Processa         │  Carrega/
       │  ────────────────>│  Instruções       │  I/O Async        │  Salva
       │                   │                   │                   │  Páginas
       │                   │<──────────────────│  Interrupção      │
       │                   │   IO_COMPLETION   │                   │
       │                   │<──────────────────────────────────────│
       │                   │           PAGE_LOAD                   │
       └───────────────────┴───────────────────┴───────────────────┘
```

### Estruturas de Dados

#### PCB (Process Control Block)
```python
{
    id: int,
    program: Program,
    pc: int,
    registers: [int],
    state: ProcessState,
    block_reason: str,
    page_table: [PageTableEntry]
}
```

#### PageTableEntry
```python
{
    page_idx: int,
    frame_number: int,
    valid_bit: bool,
    dirty_bit: bool,
    on_swap: bool,
    where_flag: "mp" | "ms" | "_"  # memória principal, swap, não carregada
}
```

#### Frame Metadata
```python
{
    pid: int,
    page: int,
    dirty: bool,
    last_used: int
}
```

---

## ⚙️ Configuração

O sistema pode ser configurado através do arquivo `config.ini`:

```ini
[MEMORIA]
mem_size = 64        # Tamanho da memória física em palavras
tam_pg = 16          # Tamanho de cada página em palavras

[CPU]
quantum = 5          # Quantum de tempo (número de instruções)
debug = False        # Modo debug (exibe cada instrução)

[IO]
io_latency = 2.0     # Latência de I/O em segundos
disk_latency = 0.3   # Latência de disco em segundos

[LOGGING]
log_file = system_log.txt
dump_dir = memory_dumps

[PAGINACAO]
replacement_policy = FIFO  # Política de substituição (FIFO ou LRU)
```

---

## 🚀 Como Executar

### Requisitos
- Python 3.7+
- Bibliotecas padrão (threading, queue, collections, datetime, configparser)

### Modo Interativo
```bash
# Usando o script de execução (recomendado)
./run.sh

# Ou diretamente com Python
python3 sistema_os.py
```

### Modo de Teste Automatizado
```bash
# Usando o script de execução
./run.sh test

# Ou diretamente com Python
python3 sistema_os.py test
```

### Ajuda
```bash
./run.sh help
```

---

## 💻 Comandos do Shell

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `new <prog>` | Cria novo processo | `new fibonacci` |
| `ps` | Lista todos os processos | `ps` |
| `rm <id>` | Remove processo (se não estiver executando) | `rm 1` |
| `dump <id>` | Exibe informações detalhadas do processo | `dump 0` |
| `dumpm <ini> <fim>` | Dump de memória física (endereços) | `dumpm 0 15` |
| `sysstate` | Estado completo do sistema (NOVO) | `sysstate` |
| `traceon` | Ativa modo trace (debug) | `traceon` |
| `traceoff` | Desativa modo trace | `traceoff` |
| `exit` | Encerra o sistema | `exit` |

---

## 📝 Formato do Log

Cada mudança de estado é registrada no formato:

```
ID  NomeProg         Razao                EstadoInicial  ProximoEstado  TabelaDePaginas
0   fibonacci        criacao              nulo           BLOCKED        { [0,_,_], [1,_,_] }
0   fibonacci        carga_inicial        BLOCKED        READY          { [0,5,mp], [1,_,_] }
0   fibonacci        escalonamento        READY          RUNNING        { [0,5,mp], [1,_,_] }
0   fibonacci        page_fault           RUNNING        BLOCKED        { [0,5,mp], [1,_,_] }
0   fibonacci        retorno_page_fault   BLOCKED        READY          { [0,5,mp], [1,7,mp] }
0   fibonacci        syscall_io           RUNNING        BLOCKED        { [0,5,mp], [1,7,mp] }
0   fibonacci        retorno_io           BLOCKED        READY          { [0,5,mp], [1,7,mp] }
0   fibonacci        preempcao_quantum    RUNNING        READY          { [0,5,mp], [1,7,mp] }
0   fibonacci        finalizacao          RUNNING        FINISHED       { [0,5,mp], [1,7,mp] }
```

### Campos:
- **ID**: Identificador único do processo
- **NomeProg**: Nome do programa
- **Razao**: Motivo da mudança de estado
- **EstadoInicial**: Estado antes da transição
- **ProximoEstado**: Estado após a transição
- **TabelaDePaginas**: Estado atual da tabela de páginas
  - Formato: `[página, frame, onde]`
  - `onde`: `mp` (memória principal), `ms` (swap), `_` (não carregada)

---

## 📦 Dumps de Memória

Quando um processo finaliza, um dump completo é gerado em `memory_dumps/`:

```
processo_<ID>_<nome>_dump.txt
```

**Conteúdo:**
- Informações do PCB (PC, registradores, estado)
- Tabela de páginas completa
- Conteúdo físico de todos os frames alocados (sem sobrescrever memória)

---

## 🧪 Testes Automatizados

### Teste A: Processos com I/O e Page-Faults
- Cria processos Fibonacci e Fatorial
- Força acessos a múltiplas páginas
- Executa chamadas SYSCALL (I/O)
- Verifica preempção por quantum

### Teste B: Vitimação de Páginas
- Cria múltiplos processos para esgotar frames
- Força substituição de páginas (FIFO)
- Verifica salvamento em swap
- Confirma carregamento de páginas vitimadas

### Teste C: Verificação de Logs
- Valida formato do log estruturado
- Confirma sequência de estados
- Verifica dumps de memória gerados

**Executar testes:**
```bash
python sistema_os.py test
```

---

## ⚙️ Configuração

Parâmetros ajustáveis no `__main__`:

```python
Sistema(
    mem_size=64,        # Tamanho da memória física (palavras)
    tam_pg=16,          # Tamanho da página (palavras)
    quantum=5,          # Quantum de tempo (instruções)
    log_file="system_log.txt",  # Arquivo de log
    io_latency=2.0,     # Latência de I/O (segundos)
    disk_latency=0.3    # Latência de disco (segundos)
)
```

---

## 📊 Programas Disponíveis

| Nome | Descrição | Páginas | I/O |
|------|-----------|---------|-----|
| `fibonacci` | Calcula Fibonacci com I/O | 2-4 | Sim |
| `fatorial` | Calcula fatorial com I/O | 2-4 | Sim |
| `fatorialV2` | Versão alternativa do fatorial | 2 | Sim |
| `progMinimo` | Programa mínimo de teste | 1 | Não |
| `PB` | Programa de teste B | 3-4 | Não |
| `PC` | Programa de ordenação | 4-5 | Não |

---

## 🔍 Observabilidade

### Comando `sysstate`
Exibe estado completo do sistema em tempo real:
- Lista de todos os processos e seus estados
- Fila de prontos (ordem de escalonamento)
- Fila de bloqueados (com razão do bloqueio)
- Processo em execução na CPU
- Mapa de frames (livres/ocupados com metadados)
- Páginas em área de swap
- Tabelas de páginas de todos os processos

---

## 🎯 Requisitos Atendidos

### ✅ Funcionalidades Implementadas

1. **Multithreading**
   - ✅ Thread Shell (comandos interativos)
   - ✅ Thread CPU (execução de instruções)
   - ✅ Thread IODevice (I/O assíncrono)
   - ✅ Thread Disco (carregamento/salvamento de páginas)
   - ⚠️ Thread Escalonador (integrado na CPU - funcional mas não separado)

2. **Gerenciamento de Processos**
   - ✅ PCB completo com estados
   - ✅ Filas de prontos e bloqueados
   - ✅ Criação/remoção de processos
   - ✅ Escalonamento FIFO

3. **Memória Virtual**
   - ✅ Paginação sob demanda
   - ✅ Tabela de páginas por processo
   - ✅ Page-fault handling
   - ✅ Substituição de páginas (FIFO)
   - ✅ Área de swap (disco simulado)
   - ✅ DMA simulado

4. **I/O Assíncrono**
   - ✅ Operações não-bloqueantes
   - ✅ Filas de requisições
   - ✅ Interrupções de conclusão
   - ✅ Bloqueio/desbloqueio de processos

5. **Logging e Dumps**
   - ✅ Log estruturado com formato especificado
   - ✅ Rastreamento de mudanças de estado
   - ✅ Dumps de memória sem sobrescrever conteúdo
   - ✅ Tabelas de páginas em formato legível

6. **Configurabilidade**
   - ✅ Parâmetros ajustáveis
   - ✅ Latências configuráveis
   - ✅ Tamanho de memória/página configurável

7. **Testes**
   - ✅ Modo de teste automatizado
   - ✅ Cenários A, B, C implementados
   - ✅ Verificação de logs e dumps

---

## 📚 Conceitos Demonstrados

### Concorrência
- Paralelismo entre CPU e dispositivos
- Sincronização com locks e eventos
- Modelo produtor-consumidor (filas de I/O)

### Gerenciamento de Memória
- Tradução de endereços lógicos → físicos
- Paginação sob demanda (lazy loading)
- Algoritmo de substituição FIFO
- Bits de controle (valid, dirty)

### Escalonamento
- Preempção por quantum
- Transições de estado (READY ↔ RUNNING ↔ BLOCKED)
- Priorização de processos

### Interrupções
- Timer (quantum)
- I/O completion
- Page-fault
- Tratamento assíncrono

---

## 🐛 Debugging

### Modo Trace
```bash
> traceon
```
Exibe cada instrução executada pela CPU com detalhes.

### Logs Detalhados
Todos os eventos importantes são logados no console e no arquivo de log.

### Inspeção de Estado
Use `sysstate` para snapshot completo do sistema.

---

## 📄 Arquivos Gerados

```
.
├── sistema_os.py           # Código principal
├── system_log.txt          # Log estruturado (modo interativo)
├── test_log.txt            # Log estruturado (modo teste)
└── memory_dumps/           # Dumps de processos finalizados
    ├── processo_0_fibonacci_dump.txt
    ├── processo_1_fatorial_dump.txt
    └── ...
```

---

## 🎓 Referências

- Trabalhos T2a e T2b - PUCRS
- Conceitos de Sistemas Operacionais Modernos
- Paginação, Escalonamento e Concorrência

---

## 👨‍💻 Autor

Simulador desenvolvido para fins educacionais - PUCRS

---

## 📝 Notas de Implementação

### Decisões de Design

1. **Thread Disco Dedicada**: Implementada thread separada (`DiskThread`) para operações de carregamento e salvamento de páginas, simulando DMA e operações assíncronas de disco.

2. **Política de Substituição**: FIFO implementada. Estrutura preparada para LRU (metadados `last_used` disponíveis).

3. **Sincronização**: Uso extensivo de locks para proteção de estruturas compartilhadas entre threads.

4. **Latências**: Simuladas com `time.sleep()` para demonstrar comportamento assíncrono realista.

5. **Configuração Flexível**: Sistema carrega configurações do arquivo `config.ini`, permitindo ajustes sem modificar código.

6. **Cálculo Dinâmico de Páginas**: O número de páginas necessário para cada processo é calculado dinamicamente considerando todos os endereços que o programa pode acessar, não apenas o tamanho do código.

### Melhorias Implementadas

✅ **Thread de Disco Separada**: Operações de disco agora são tratadas em thread dedicada com fila de requisições
✅ **Carregamento Assíncrono de Páginas**: Page-faults são tratados de forma assíncrona sem bloquear a CPU
✅ **Vitimação de Páginas**: Implementado algoritmo FIFO para seleção de vítimas quando não há frames livres
✅ **Salvamento de Páginas Sujas**: Páginas modificadas são salvas no disco antes de serem substituídas
✅ **Comando sysstate**: Novo comando para visualizar estado completo do sistema (processos, filas, memória, disco)
✅ **Configuração via Arquivo**: Parâmetros do sistema podem ser ajustados via `config.ini`
✅ **Dumps de Memória Detalhados**: Processos finalizados geram dumps completos com tabelas de páginas e conteúdo da memória
✅ **Logs Estruturados**: Todas as transições de estado são registradas com formato padronizado incluindo tabelas de páginas
✅ **Script de Execução**: Script `run.sh` facilita execução em modo interativo ou de teste
✅ **Testes Automatizados**: Cenários de teste completos validam page-faults, vitimação e I/O assíncrono

5. **DMA**: Simulado através de acesso direto à memória pelo dispositivo de I/O.

### Limitações Conhecidas

- Thread escalonador não separada (integrada na CPU)
- Política de substituição fixa em FIFO (LRU preparado mas não ativado)
- Sem suporte a múltiplos dispositivos de I/O
- Sem proteção de memória entre processos (simulação educacional)

---

## 🚀 Melhorias Futuras

- [ ] Separar thread do escalonador
- [ ] Implementar LRU como política alternativa
- [ ] Adicionar múltiplos dispositivos de I/O
- [ ] Implementar prioridades de processos
- [ ] Adicionar estatísticas de desempenho (tempo de resposta, throughput)
- [ ] Interface gráfica para visualização

---

**Versão**: 2.0  
**Data**: 2025  
**Licença**: Educacional
