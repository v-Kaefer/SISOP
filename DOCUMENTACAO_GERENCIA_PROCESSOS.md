# Documentação da Gerência de Processos - SISOP
## Estado Atual da Implementação (Etapa 02)

### Introdução

Este documento explica o estado atual da implementação da **Gerência de Processos** no sistema operacional SISOP. Embora o sistema tenha sido projetado com as fundações necessárias para gerenciamento de processos, a implementação completa desta funcionalidade ainda está em desenvolvimento.

### Arquitetura Atual do Sistema

```
SISOP/
├── Sistema.java                   # Ponto de entrada - execução single-process
├── hardware/
│   ├── CPU.java                  # Contexto de processo básico implementado
│   ├── Memory.java               # Memória física
│   └── HW.java                   # Hardware integrado
├── memory/                       # Gerenciamento de memória (Etapa 01 - COMPLETA)
│   ├── GerenciadorMemoria.java   # Paginação com suporte a múltiplos processos
│   └── MemoryManagerBridge.java  # Interface para alocação por processo
├── software/
│   ├── SO.java                   # Núcleo do SO (básico)
│   ├── Utilities.java            # Carregamento e execução de programas
│   ├── InterruptHandling.java    # Tratamento de interrupções
│   └── SysCallHandling.java      # Chamadas de sistema (STOP, I/O)
└── programs/
    └── Programs.java             # Biblioteca de programas executáveis
```

## 1. Fundações de Processo Management Implementadas

### 1.1 Contexto de Processo na CPU (hardware/CPU.java)

#### Estrutura do Contexto
```java
public class CPU {
    // CONTEXTO da CPU - tudo que precisa sobre o estado de um processo
    private int pc;             // Program Counter
    private Word ir;            // Instruction Register  
    private int[] reg;          // Registradores da CPU (R0-R9)
    private Interrupts irpt;    // Estado de interrupção
    // FIM CONTEXTO DA CPU
}
```

**Localização**: `hardware/CPU.java`, linhas 13-20

**Funcionalidade Implementada**:
- **Contexto básico de processo**: PC, registradores, instruction register
- **Método setContext()**: Para inicializar contexto de um processo
- **Comentário explícito**: "tudo que precisa sobre o estado de um processo para executa-lo"

#### Métodos de Controle de Contexto
```java
public void setContext(int _pc) {
    pc = _pc;                               // Define ponto de entrada do processo
    irpt = Interrupts.noInterrupt;          // Reset de interrupções
}

public int getPc() {
    return pc;                              // Acesso ao Program Counter
}

public int getReg(int i) {
    return reg[i];                          // Acesso aos registradores
}
```

**Localização**: `hardware/CPU.java`, linhas 89-102

### 1.2 Suporte a Múltiplos Processos na Memória (memory/)

#### Alocação por Processo
```java
public boolean aloca(int nroPalavras, int[] tabelaPaginas, String processoId) {
    // Aloca frames para um processo específico
    // Mantém mapeamento frame → processo
}
```

**Localização**: `memory/GerenciadorMemoria.java`, linha 69

**Funcionalidade Implementada**:
- **Identificação de processos**: Cada alocação associada a um processoId
- **Isolamento de memória**: Frames pertencem a processos específicos
- **Proteção de memória**: Tradução de endereços com validação

#### Exemplo de Uso Multi-Processo
```java
// Exemplo do sistema de testes
String[] processosIds = {"Frag-A", "Frag-B", "Frag-C", "Frag-D", "Frag-E"};
for (int i = 0; i < 5; i++) {
    boolean sucesso = gm.aloca(6, tabelas[i], processosIds[i]);
}
```

**Localização**: `memory/TesteGerenciadorMemoria.java`, linhas 185-190

### 1.3 Sistema de Chamadas com Controle de Processo

#### Finalização de Processo
```java
public void stop() {
    System.out.println("SYSCALL STOP");    // Indica fim de processo
}
```

**Localização**: `software/SysCallHandling.java`, linha 25

#### Flags de Controle
```java
private boolean cpuStop;    // Flag para parar CPU quando processo termina
```

**Localização**: `hardware/CPU.java`, linha 27

## 2. Limitações da Implementação Atual

### 2.1 Execução Single-Process

**Problema**: O sistema atual executa apenas um processo por vez

```java
public void run() {
    so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));
    // Executa apenas um programa e para
}
```

**Localização**: `Sistema.java`, linha 18

### 2.2 Falta de Process Control Block (PCB)

**O que está faltando**:
- Estrutura de dados para armazenar estado completo do processo
- Estados de processo (NEW, READY, RUNNING, WAITING, TERMINATED)
- Informações como prioridade, PID, estatísticas

### 2.3 Ausência de Escalonamento

**O que está faltando**:
- Fila de processos prontos
- Algoritmo de escalonamento (Round-Robin planejado)
- Context switching entre processos
- Time slicing

## 3. Como Executar os Exemplos Atuais

### 3.1 Execução Básica do Sistema
```bash
cd /home/runner/work/SISOP/SISOP
javac Sistema.java
java Sistema
```

**Output esperado**: Execução do programa fatorial com dump de memória

### 3.2 Teste do Gerenciador de Memória Multi-Processo
```bash
javac memory/*.java
java memory.TesteGerenciadorMemoria
```

**Output esperado**: Simulação de múltiplos processos alocando/desalocando memória

### 3.3 Teste de Integração
```bash
java memory.TesteIntegracao
```

**Output esperado**: Testes com programas reais (fatorial, fibonacci)

## 4. Exemplos Práticos de Uso Atual

### 4.1 Carregamento e Execução de Processo

```java
// Como um "processo" é atualmente carregado e executado
public void loadAndExec(Word[] p) {
    loadProgram(p);                    // 1. Carrega na memória
    System.out.println("programa carregado na memoria");
    dump(0, p.length);                 // 2. Mostra estado inicial
    
    hw.cpu.setContext(0);              // 3. Seta contexto (PC = 0)
    System.out.println("inicia execucao");
    hw.cpu.run();                      // 4. CPU executa até STOP
    
    System.out.println("memoria após execucao");
    dump(0, p.length);                 // 5. Mostra estado final
}
```

**Localização**: `software/Utilities.java`, linhas 35-47

### 4.2 Alocação de Memória por Processo

```java
// Como o sistema aloca memória para diferentes processos
public int[] alocaPrograma(Word[] programa, String processoId) {
    int[] tabelaPaginas = new int[programa.length / TAM_PAGINA + 1];
    boolean sucesso = gerenciador.aloca(programa.length, tabelaPaginas, processoId);
    
    if (sucesso) {
        gerenciador.carregaPrograma(convertePrograma(programa), tabelaPaginas);
        return tabelaPaginas;
    }
    return null;
}
```

**Localização**: `memory/MemoryManagerBridge.java`, linhas 15-25

### 4.3 Exemplo Prático Completo

Um exemplo prático demonstrando todas as funcionalidades atuais está disponível em:

```bash
javac examples/*.java
java examples.ExemploGerenciaProcessos
```

**Localização**: `examples/ExemploGerenciaProcessos.java`

Este exemplo demonstra:
- ✅ Alocação simultânea de memória para múltiplos "processos"
- ✅ Configuração de contexto da CPU para um processo
- ✅ Execução de processo com dump de estado
- ⚠️ Limitações atuais (execução sequencial)

## 5. Roadmap para Implementação Completa

### 5.1 Próximos Passos (Etapa 03)

#### Process Control Block (PCB)
```java
// Estrutura proposta para implementação
public class ProcessControlBlock {
    private int pid;                    // Process ID
    private ProcessState state;         // NEW, READY, RUNNING, WAITING, TERMINATED
    private int pc;                     // Program Counter
    private int[] registers;            // Estado dos registradores
    private int[] pageTable;            // Tabela de páginas
    private int priority;               // Prioridade do processo
    private long cpuTime;               // Tempo de CPU usado
}
```

#### Gerenciador de Processos
```java
// Estrutura proposta
public class ProcessManager {
    private List<PCB> processes;        // Lista de todos os processos
    private Queue<PCB> readyQueue;      // Fila de processos prontos
    private PCB runningProcess;         // Processo atualmente executando
    
    public PCB createProcess(Word[] program);
    public void terminateProcess(int pid);
    public void contextSwitch(PCB from, PCB to);
}
```

### 5.2 Escalonamento (Etapa 04)

#### Round-Robin Scheduler
```java
// Estrutura proposta
public class RoundRobinScheduler {
    private Queue<PCB> readyQueue;
    private int timeQuantum;            // Fatia de tempo por processo
    private int currentQuantum;         // Tempo restante do processo atual
    
    public PCB selectNextProcess();
    public boolean shouldPreempt();
    public void addToReadyQueue(PCB process);
}
```

## 6. Análise de Gap Implementation

### 6.1 O que ESTÁ implementado ✅

1. **Contexto básico de processo** (CPU.java)
   - Program Counter, registradores, instruction register
   - Método setContext() para inicializar processo

2. **Gerenciamento de memória multi-processo** (memory/)
   - Alocação/desalocação por processoId
   - Isolamento de memória entre processos
   - Proteção via tradução de endereços

3. **Sistema de chamadas básico** (SysCallHandling.java)
   - STOP para finalizar processo
   - I/O básico

4. **Infraestrutura de interrupções** (InterruptHandling.java)
   - Base para implementar preempção

### 6.2 O que NÃO ESTÁ implementado ❌

1. **Process Control Block (PCB)**
   - Estrutura de dados completa do processo
   - Estados de processo
   - Metadados (PID, prioridade, etc.)

2. **Gerenciador de Processos**
   - Criação/destruição de processos
   - Lista/fila de processos
   - Context switching

3. **Escalonamento**
   - Algoritmo Round-Robin
   - Time slicing
   - Preempção

4. **Execução Concorrente**
   - Múltiplos processos simultaneamente
   - Sincronização entre processos

## Conclusão

O sistema SISOP possui **fundações sólidas** para gerenciamento de processos:
- ✅ Contexto de CPU implementado
- ✅ Gerenciamento de memória multi-processo
- ✅ Sistema de chamadas básico
- ✅ Infraestrutura de hardware completa

Entretanto, ainda **falta implementar** os componentes centrais:
- ❌ Process Control Block (PCB)
- ❌ Gerenciador de Processos
- ❌ Escalonamento Round-Robin
- ❌ Context switching

A **Etapa 02** estabeleceu as bases, mas a **Gerência de Processos completa** será implementada na **Etapa 03**, seguindo o roadmap definido no arquivo de instruções.

---

**Documentação gerada em**: Dezembro 2024  
**Status**: Etapa 02 - Fundações implementadas, aguardando Etapa 03 para implementação completa  
**Próximo milestone**: Implementar PCB e Gerenciador de Processos