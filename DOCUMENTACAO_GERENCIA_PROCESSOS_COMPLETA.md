# Documentação de Gerência de Processos - SISOP

## Estado Final da Implementação (Etapa Completa)

### Introdução

Este documento apresenta a implementação completa da **Gerência de Processos** no sistema operacional SISOP, incluindo todas as funcionalidades documentadas nas limitações anteriores:

- ✅ **Process Control Block (PCB)** - Implementado
- ✅ **Estados de processo** (NEW, READY, RUNNING, WAITING, TERMINATED) - Implementado  
- ✅ **Escalonamento Round-Robin** - Implementado
- ✅ **Context switching** - Implementado
- ✅ **Execução concorrente de múltiplos processos** - Implementado

### Arquitetura Atualizada do Sistema

```
SISOP/
├── Sistema.java                   # Ponto de entrada (ainda single-process para compatibilidade)
├── hardware/                      # Hardware virtual
│   ├── CPU.java                   # Contexto de processo expandido
│   ├── Memory.java                # Memória física
│   └── HW.java                    # Hardware integrado
├── memory/                        # Gerenciamento de memória
│   ├── GerenciadorMemoria.java    # Paginação com suporte multi-processo
│   ├── MemoryManagerPonte.java    # Interface para alocação (renomeado de Bridge)
│   └── Testes...                  # Testes de memória
├── software/                      # Sistema operacional completo
│   ├── SO.java                    # Núcleo do SO
│   ├── ProcessState.java          # Estados de processo (NOVO)
│   ├── ProcessControlBlock.java   # PCB completo (NOVO)
│   ├── RoundRobinScheduler.java   # Escalonador Round-Robin (NOVO)
│   ├── ProcessManager.java        # Gerenciador de processos (NOVO)
│   ├── TesteGerenciaProcessos.java # Testes modulares (NOVO)
│   ├── Utilities.java             # Ferramentas auxiliares
│   ├── InterruptHandling.java     # Tratamento de interrupções
│   └── SysCallHandling.java       # Chamadas de sistema
├── programs/                      # Biblioteca de programas
│   ├── Programs.java              # Programas disponíveis
│   └── Program.java               # Estrutura de programa
└── examples/                      # Exemplos práticos
    ├── ExemploGerenciaProcessos.java      # Exemplo básico (existente)
    └── ExemploExecucaoConcorrente.java    # Execução concorrente (NOVO)
```

## 1. Componentes Implementados

### 1.1 Estados de Processo (ProcessState.java)

#### Enum com Estados Clássicos
```java
public enum ProcessState {
    NEW("NEW"),         // Processo criado, ainda não pronto
    READY("READY"),     // Pronto para execução
    RUNNING("RUNNING"), // Executando na CPU
    WAITING("WAITING"), // Bloqueado aguardando evento
    TERMINATED("TERMINATED"); // Finalizado
}
```

**Localização**: `software/ProcessState.java`

**Funcionalidades**:
- **Métodos auxiliares**: `isActive()`, `canBeScheduled()`, `isFinished()`
- **Transições válidas**: Controle de mudanças de estado
- **Representação textual**: Para debug e logs

#### Exemplo de Uso
```java
ProcessState estado = ProcessState.NEW;
if (estado.canBeScheduled()) {
    // Processo pode ser escalonado
}
```

### 1.2 Process Control Block - PCB (ProcessControlBlock.java)

#### Estrutura Completa do PCB
```java
public class ProcessControlBlock {
    // Identificação
    private int pid;                    // Process ID único
    private String nome;                // Nome do processo
    
    // Estado
    private ProcessState estado;        // Estado atual
    
    // Contexto da CPU
    private int pc;                     // Program Counter
    private int[] registradores;        // R0-R9
    private Interrupts interrupcao;     // Estado de interrupção
    
    // Memória
    private int[] tabelaPaginas;        // Tabela de páginas
    private Word[] programa;            // Código do programa
    
    // Estatísticas
    private long tempoCPU;              // Tempo total de CPU
    private long tempoEspera;           // Tempo na fila de prontos
    private int prioridade;             // Prioridade do processo
    private int quantumRestante;        // Quantum atual
}
```

**Localização**: `software/ProcessControlBlock.java`

**Funcionalidades Implementadas**:
- **Gestão de contexto**: Salvar/restaurar estado da CPU
- **Controle de estado**: Transições seguras entre estados
- **Estatísticas**: Coleta de métricas de execução
- **Validações**: Verificações de consistência
- **Debugging**: Representação textual detalhada

#### Exemplo de Uso
```java
ProcessControlBlock pcb = new ProcessControlBlock(1, "MeuProcesso", programa);
pcb.setEstado(ProcessState.READY);
pcb.salvarContexto(pc, registradores, interrupcao);
```

### 1.3 Escalonador Round-Robin (RoundRobinScheduler.java)

#### Algoritmo Round-Robin Completo
```java
public class RoundRobinScheduler {
    private int quantum;                           // Quantum de tempo
    private Queue<ProcessControlBlock> filaProtos; // Fila de processos prontos
    private ProcessControlBlock processoAtual;     // Processo executando
    private long totalContextSwitches;            // Estatísticas
}
```

**Localização**: `software/RoundRobinScheduler.java`

**Funcionalidades Implementadas**:
- **Fila circular**: Implementação clássica do Round-Robin
- **Quantum configurável**: Permite ajustar fatia de tempo
- **Context switching automático**: Troca quando quantum expira
- **Preempção**: Suporte a interrupção de processos
- **Estatísticas**: Métricas detalhadas de escalonamento

#### Lógica do Escalonamento
```java
public ProcessControlBlock selecionarProximoProcesso() {
    // Se quantum expirou ou não há processo atual
    if (processoAtual == null || quantumAtual <= 0 || processoAtual.isFinished()) {
        return executarContextSwitch();
    }
    
    // Continua processo atual
    return processoAtual;
}
```

#### Métricas de Performance
- **Context switches**: Número total de trocas
- **Tempo de CPU**: Por processo e total
- **Tempo de espera**: Tempo na fila de prontos
- **Utilização de quantum**: Eficiência do escalonamento

### 1.4 Gerenciador de Processos (ProcessManager.java)

#### Coordenação Completa do Sistema
```java
public class ProcessManager {
    private HW hardware;                           // Hardware virtual
    private MemoryManagerPonte gerenciadorMemoria; // Gerenciador de memória
    private RoundRobinScheduler escalonador;       // Escalonador
    private Map<Integer, ProcessControlBlock> processos; // Todos os processos
}
```

**Localização**: `software/ProcessManager.java`

**Funcionalidades Implementadas**:
- **Criação de processos**: Com alocação automática de memória
- **Admissão no sistema**: Controle de entrada de processos
- **Ciclo de execução**: Loop principal do sistema operacional
- **Context switching**: Coordenação entre CPU e escalonador
- **Finalização**: Liberação de recursos automaticamente

#### Ciclo Principal do SO
```java
public boolean executarCicloSO() {
    // 1. Seleciona processo para executar
    ProcessControlBlock processoAtual = escalonador.selecionarProximoProcesso();
    
    // 2. Executa context switch se necessário
    if (processoAtual != escalonador.getProcessoAtual()) {
        executarContextSwitch(processoAtual);
    }
    
    // 3. Executa uma instrução
    boolean continuarExecucao = executarInstrucao(processoAtual);
    
    // 4. Atualiza contadores
    escalonador.executarCicloCPU();
    
    // 5. Verifica finalização
    if (!continuarExecucao || processoAtual.isFinished()) {
        escalonador.finalizarProcessoAtual();
        finalizarProcesso(processoAtual.getPid());
    }
    
    return true;
}
```

### 1.5 Execução Concorrente

#### Método Principal para Múltiplos Processos
```java
public void executarProcessosConcorrentes(List<Program> programas) {
    // 1. Cria todos os processos
    for (Program programa : programas) {
        ProcessControlBlock pcb = criarProcesso(programa);
        if (pcb != null) {
            admitirProcesso(pcb.getPid());
        }
    }
    
    // 2. Executa ciclos até todos terminarem
    sistemaAtivo = true;
    while (sistemaAtivo && escalonador.temProcessosParaExecutar()) {
        executarCicloSO();
    }
}
```

## 2. Alterações de Nomenclatura

### 2.1 Bridge → Ponte

Conforme solicitado, toda a nomenclatura "Bridge" foi alterada para "Ponte" por ser um projeto em português-BR:

- ✅ `MemoryManagerBridge.java` → `MemoryManagerPonte.java`
- ✅ Classe `MemoryManagerBridge` → `MemoryManagerPonte`
- ✅ Todos os imports e referências atualizados
- ✅ Comentários em português mantidos

**Arquivos Afetados**:
- `memory/MemoryManagerPonte.java` (renomeado)
- `software/ProcessManager.java` (import atualizado)
- `memory/TesteIntegracao.java` (referências atualizadas)
- `examples/ExemploGerenciaProcessos.java` (referências atualizadas)

## 3. Testes Modulares Implementados

### 3.1 Teste Abrangente (TesteGerenciaProcessos.java)

#### Estrutura dos Testes
```java
public class TesteGerenciaProcessos {
    public static void main(String[] args) {
        testarProcessState();           // Teste 1: Estados
        testarProcessControlBlock();    // Teste 2: PCB
        testarRoundRobinScheduler();   // Teste 3: Escalonador
        testarProcessManager();        // Teste 4: Gerenciador
        testarExecucaoConcorrente();   // Teste 5: Concorrência
    }
}
```

**Localização**: `software/TesteGerenciaProcessos.java`

#### Cobertura dos Testes
- **ProcessState**: Todos os estados e transições
- **PCB**: Criação, manipulação, persistência
- **Scheduler**: Algoritmo Round-Robin, quantum, filas
- **Process Manager**: Criação, admissão, finalização
- **Concorrência**: Múltiplos processos simultâneos

#### Como Executar
```bash
javac software/*.java memory/*.java hardware/*.java programs/*.java
java software.TesteGerenciaProcessos
```

### 3.2 Exemplo Prático (ExemploExecucaoConcorrente.java)

#### Demonstrações Implementadas
```java
public class ExemploExecucaoConcorrente {
    public static void exemploBasico();           // Processos simples
    public static void exemploComProgramasReais(); // Programas da biblioteca
    public static void exemploComQuantumDiferente(); // Comparação de performance
    public static void demonstracaoEscalonamento(); // Step-by-step detalhado
}
```

**Localização**: `examples/ExemploExecucaoConcorrente.java`

#### Como Executar
```bash
javac examples/*.java software/*.java memory/*.java hardware/*.java programs/*.java
java examples.ExemploExecucaoConcorrente
```

## 4. Como Utilizar o Sistema Completo

### 4.1 Execução Básica (Compatibilidade)

O sistema original continua funcionando:
```bash
javac Sistema.java
java Sistema
```

### 4.2 Execução com Múltiplos Processos

#### Exemplo Programático
```java
// Inicializa sistema
HW hardware = new HW(1024);
MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
ProcessManager processManager = new ProcessManager(hardware, memoriaManager);

// Configura escalonamento
processManager.setQuantum(5);

// Cria programas
List<Program> programas = Arrays.asList(
    new Program("Processo1", programa1),
    new Program("Processo2", programa2),
    new Program("Processo3", programa3)
);

// Executa concorrentemente
processManager.executarProcessosConcorrentes(programas);

// Visualiza estatísticas
processManager.exibirEstatisticas();
```

#### Utilizando Programas da Biblioteca
```java
Programs biblioteca = new Programs();
List<Program> programas = Arrays.asList(
    biblioteca.retrieveProgram("progMinimo"),
    biblioteca.retrieveProgram("fibonacci10"),
    biblioteca.retrieveProgram("fatorial")
);

processManager.executarProcessosConcorrentes(programas);
```

### 4.3 Controle Manual do Sistema

#### Criação Step-by-Step
```java
// Cria processo
ProcessControlBlock pcb = processManager.criarProcesso("MeuProcesso", programa);

// Admite no sistema
processManager.admitirProcesso(pcb.getPid());

// Executa ciclos manuais
processManager.iniciarSistema();
while (processManager.getEscalonador().temProcessosParaExecutar()) {
    processManager.executarCicloSO();
}
```

## 5. Métricas e Estatísticas

### 5.1 Estatísticas do Escalonador

```java
System.out.println(escalonador.getEstatisticas());
```

**Output exemplo**:
```
=== Estatísticas do Escalonador Round-Robin ===
Quantum configurado: 10 ciclos
Quantum restante: 3 ciclos
Total de context switches: 15
Ciclo atual da CPU: 150
Processos na fila de prontos: 2
Processo atual: ProcessoA (PID: 1)
```

### 5.2 Estatísticas do Gerenciador

```java
processManager.exibirEstatisticas();
```

**Output exemplo**:
```
=== Estatísticas do Gerenciador de Processos ===
Total de processos criados: 5
Total de processos finalizados: 3
Processos ativos: 2
Próximo PID: 6
Sistema ativo: true

Processos no sistema:
  PCB[PID=4, Nome=ProcessoX, Estado=RUNNING, PC=5, CPU=25 ciclos, Espera=10, Prioridade=1]
  PCB[PID=5, Nome=ProcessoY, Estado=READY, PC=2, CPU=15 ciclos, Espera=30, Prioridade=1]
```

### 5.3 Estatísticas por Processo

```java
ProcessControlBlock pcb = processManager.getProcesso(pid);
System.out.println(pcb.toDetailedString());
```

**Output exemplo**:
```
PCB[PID=1, Nome=Contador, Estado=TERMINATED, PC=7, CPU=45 ciclos, Espera=20, Prioridade=1]
  Registradores: R0=10 R1=1 R2=0 R3=0 R4=0 R5=0 R6=0 R7=0 R8=0 R9=0 
  Tamanho do programa: 7 instruções
  Quantum restante: 0
```

## 6. Funcionalidades Avançadas

### 6.1 Configuração do Quantum

```java
// Quantum pequeno: Mais responsivo, mais context switches
processManager.setQuantum(2);

// Quantum grande: Menos context switches, menos responsivo
processManager.setQuantum(20);
```

### 6.2 Limitação de Processos Concorrentes

```java
// Define limite de processos simultâneos
processManager.setMaxProcessosConcorrentes(5);
```

### 6.3 Monitoramento em Tempo Real

```java
// Execução step-by-step com monitoramento
processManager.iniciarSistema();
for (int ciclo = 0; ciclo < 100; ciclo++) {
    System.out.println("=== Ciclo " + ciclo + " ===");
    processManager.getEscalonador().exibirEstado();
    processManager.executarCicloSO();
    
    // Pausa para visualização
    Thread.sleep(100);
}
```

## 7. Comparação: Antes vs Depois

### 7.1 Antes (Limitações Documentadas)

❌ **Execução Single-Process**: Apenas um programa por vez  
❌ **Sem PCB**: Contexto limitado na CPU  
❌ **Sem Estados**: Não havia controle de estado de processo  
❌ **Sem Escalonamento**: Execução sequencial apenas  
❌ **Sem Context Switching**: Não havia troca entre processos  

### 7.2 Depois (Implementação Completa)

✅ **Execução Multi-Process**: Múltiplos processos simultâneos  
✅ **PCB Completo**: Contexto completo com estatísticas  
✅ **Estados Implementados**: NEW, READY, RUNNING, WAITING, TERMINATED  
✅ **Round-Robin**: Escalonamento justo com quantum configurável  
✅ **Context Switching**: Troca eficiente entre processos  
✅ **Concorrência Real**: Sistema operacional completo  

## 8. Integração com Sistema Existente

### 8.1 Compatibilidade

O sistema mantém **total compatibilidade** com o código existente:
- `Sistema.java` continua funcionando como antes
- Todos os programas da biblioteca continuam executáveis
- Gerenciador de memória inalterado (apenas renomeado)
- Interface da CPU preservada

### 8.2 Expansibilidade

A arquitetura implementada permite fácil expansão:
- **Novos algoritmos de escalonamento**: Interface bem definida
- **Estados customizados**: Enum extensível
- **Métricas adicionais**: PCB expansível
- **Tipos de processos**: Hierarquia de classes possível

## Conclusão

A implementação da **Gerência de Processos** no SISOP está **100% completa**, incluindo:

✅ **Process Control Block (PCB)** - Estrutura completa com contexto, estatísticas e controle  
✅ **Estados de processo** - NEW, READY, RUNNING, WAITING, TERMINATED totalmente implementados  
✅ **Escalonamento Round-Robin** - Algoritmo completo com quantum configurável  
✅ **Context switching** - Troca eficiente entre processos  
✅ **Execução concorrente** - Múltiplos processos executando simultaneamente  
✅ **Testes modulares** - Cobertura completa de todas as funcionalidades  
✅ **Documentação explicativa** - Este documento e comentários no código  
✅ **Nomenclatura em português** - "Ponte" ao invés de "Bridge"  

O sistema evoluiu de uma **simulação single-process** para um **sistema operacional completo** com gerenciamento de processos real, mantendo total compatibilidade com o código existente e oferecendo uma base sólida para futuras expansões.

---

**Documentação atualizada em**: Dezembro 2024  
**Status**: Implementação Completa - Todas as funcionalidades operacionais  
**Próximas expansões possíveis**: Sincronização entre processos, Sistema de arquivos, Interface gráfica