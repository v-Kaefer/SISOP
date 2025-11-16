# NOMENCLATURAS - Sistema Operacional SISOP

Este documento define os principais termos técnicos utilizados no projeto SISOP (Sistema Operacional Virtual).

---

## Glossário Geral

### A

**Alocação de Memória**
- Processo de reservar espaço na memória RAM para armazenar instruções e dados de um processo.
- No SISOP: Implementado através do sistema de paginação (Etapa 1).

---

### B

**Bloqueio (Blocking)**
- Estado em que um processo não pode prosseguir sua execução até que uma condição seja satisfeita.
- Exemplo: Processo aguardando operação de I/O ou aguardando um semáforo.
- Estado associado: `WAITING` ou `BLOCKED`.

**Buffer**
- Área de memória temporária utilizada para armazenar dados durante transferências.
- Exemplo: Buffer circular no problema Produtor-Consumidor.

---

### C

**Context Switch (Troca de Contexto)**
- Processo de salvar o estado de um processo em execução e restaurar o estado de outro processo.
- Inclui: salvar/restaurar PC (Program Counter), registradores, estado de interrupções.
- No SISOP: Implementado no `ProcessManager` (Etapa 2).

**CPU (Central Processing Unit)**
- Unidade central de processamento que executa instruções dos programas.
- No SISOP: Implementada na classe `CPU.java` com conjunto de instruções próprio.

**Ciclo de Instrução**
- Sequência de etapas para executar uma instrução: Fetch → Decode → Execute → Write-back.

---

### D

**Deadlock**
- Situação onde dois ou mais processos ficam bloqueados indefinidamente, aguardando recursos uns dos outros.
- Prevenção: Uso correto de semáforos e mutex.

**Desalocação**
- Liberação de recursos (memória, frames) previamente alocados a um processo.
- Ocorre quando: processo termina ou é removido do sistema.

---

### E

**Escalonador (Scheduler)**
- Componente do SO responsável por decidir qual processo executará na CPU.
- No SISOP: Implementado como `RoundRobinScheduler` (Etapa 2).

**Escalonamento Round-Robin**
- Algoritmo de escalonamento onde cada processo recebe uma fatia de tempo igual (quantum).
- Processos são executados em ordem circular (fila FIFO).
- Vantagens: Justiça, simplicidade, sem starvation.

**Estado de Processo**
- Situação atual de um processo no sistema.
- Estados no SISOP:
  - `NEW`: Processo criado, ainda não pronto
  - `READY`: Pronto para execução, aguardando CPU
  - `RUNNING`: Executando na CPU
  - `WAITING`: Bloqueado aguardando evento (I/O, semáforo)
  - `TERMINATED`: Finalizado

**Exclusão Mútua (Mutual Exclusion)**
- Garantia de que apenas um processo pode acessar um recurso compartilhado por vez.
- Implementada através de: Mutex, semáforos binários.

---

### F

**FIFO (First In, First Out)**
- Política de fila onde o primeiro elemento a entrar é o primeiro a sair.
- Usado em: fila de processos prontos, fila de espera de semáforos.

**Fragmentação**
- Desperdício de memória devido a espaços não utilizados.
- Tipos:
  - Externa: Espaços livres entre blocos alocados
  - Interna: Espaço não utilizado dentro de um bloco alocado
- No SISOP: Paginação elimina fragmentação externa.

**Frame**
- Divisão de tamanho fixo da memória física.
- No SISOP: Memória dividida em frames de tamanho configurável (ex: 8 palavras).

---

### I

**I/O (Input/Output)**
- Operações de entrada e saída de dados.
- No SISOP: Processos bloqueiam durante I/O (operações READ/WRITE).

**Instrução**
- Comando básico executado pela CPU.
- Formato: [Opcode, Registrador_A, Registrador_B, Parâmetro]
- Exemplos: LDI (Load Immediate), ADD, MULT, JMP, SYSCALL, STOP.

**Interrupção**
- Sinal que suspende temporariamente a execução normal da CPU.
- Tipos no SISOP:
  - Endereço inválido
  - Instrução inválida
  - Overflow
  - I/O completo

---

### M

**Memória Lógica (Virtual)**
- Espaço de endereçamento que cada processo "enxerga".
- Endereços são traduzidos para endereços físicos pelo sistema de paginação.

**Memória Física**
- Memória RAM real do hardware.
- No SISOP: Implementada como array de palavras (Words).

**MMU (Memory Management Unit)**
- Unidade de gerenciamento de memória que traduz endereços lógicos para físicos.
- No SISOP: Função implementada no `GerenciadorMemoria`.

**Mutex (Mutual Exclusion Lock)**
- Primitiva de sincronização que garante exclusão mútua.
- Características no SISOP:
  - Apenas o processo que fez `lock()` pode fazer `unlock()`
  - Previne locks recursivos
  - Propriedade de ownership
- Implementação: `Mutex.java` (Etapa 3)

---

### N

**NOP (No Operation Process)**
- Processo especial que mantém o sistema operacional rodando continuamente.
- Características:
  - Loop infinito executando operações mínimas (incremento de contador)
  - Nunca finaliza (não possui instrução STOP)
  - Garante que sempre há pelo menos um processo na fila de prontos
- No SISOP Python: Programa `nop` disponível na biblioteca de programas
- Uso recomendado:
  - Criar e iniciar no início da sessão do sistema
  - Permite que usuário adicione novos processos via CLI a qualquer momento
  - Evita que sistema pare por falta de processos
  - Pode ser removido quando não mais necessário

---

### O

**Opcode (Operation Code)**
- Código que identifica a operação que uma instrução deve executar.
- Exemplos: LDI, ADD, SUB, MULT, JMP, SYSCALL, STOP.

**Overhead**
- Custo adicional de processamento ou memória devido a operações de controle.
- Exemplo: Overhead de context switch, overhead de paginação.

---

### P

**Página (Page)**
- Divisão de tamanho fixo da memória lógica de um processo.
- Mapeada para um frame na memória física.
- No SISOP: Tamanho configurável (ex: 8 palavras).

**Paginação**
- Técnica de gerenciamento de memória que divide memória lógica e física em blocos de tamanho fixo.
- Vantagens: Elimina fragmentação externa, permite alocação não contígua.
- No SISOP: Implementada no `GerenciadorMemoria` (Etapa 1).

**PC (Program Counter)**
- Registrador que armazena o endereço da próxima instrução a ser executada.
- Atualizado a cada instrução executada ou durante jumps.

**PCB (Process Control Block)**
- Estrutura de dados que contém todas as informações sobre um processo.
- Conteúdo no SISOP:
  - PID (identificador único)
  - Nome do processo
  - Estado atual (NEW, READY, RUNNING, WAITING, TERMINATED)
  - Contexto da CPU (PC, registradores)
  - Tabela de páginas
  - Estatísticas (tempo de CPU, tempo de espera)
  - Prioridade
- Implementação: `ProcessControlBlock.java` (Etapa 2)

**PID (Process Identifier)**
- Número único que identifica cada processo no sistema.
- No SISOP: Gerado sequencialmente (1, 2, 3, ...).

**Prioridade**
- Valor numérico que indica a importância relativa de um processo.
- No SISOP: Armazenado no PCB, pode ser usado por escalonadores futuros.

**Processo**
- Programa em execução, com seu próprio espaço de memória e estado.
- Possui: código, dados, pilha, PCB.

**Produtor-Consumidor**
- Problema clássico de sincronização onde processos produtores geram dados e consumidores os consomem.
- Solução no SISOP: Semáforos contadores + Mutex (Etapa 3).

---

### Q

**Quantum (Fatia de Tempo / Time Slice)**
- Intervalo de tempo (em ciclos de CPU) que um processo pode executar antes de ser interrompido.
- No SISOP:
  - Medido em número de instruções executadas
  - Valor padrão Java: 10 ciclos
  - Valor Python: 50 instruções
  - Configurável por usuário
- Quando quantum expira:
  - Processo passa de RUNNING para READY
  - Context switch ocorre
  - Próximo processo da fila é escalonado
- Impacto do valor:
  - Quantum pequeno: Mais trocas de contexto (overhead), melhor responsividade
  - Quantum grande: Menos overhead, pior responsividade, mais tempo de espera

---

### R

**Registrador**
- Memória de alta velocidade dentro da CPU para armazenar dados temporários.
- No SISOP: CPU tem 10 registradores (R0-R9).

**Round-Robin**
- Ver "Escalonamento Round-Robin".

---

### S

**Seção Crítica**
- Trecho de código que acessa recursos compartilhados e deve ser executado atomicamente.
- Proteção: Mutex, semáforos binários.

**Semáforo**
- Primitiva de sincronização para controlar acesso a recursos.
- Tipos:
  - **Binário**: Valor 0 ou 1 (similar a mutex)
  - **Contador**: Valor N (controla N recursos idênticos)
- Operações no SISOP:
  - `down()` (P, wait): Decrementa contador, bloqueia se < 0
  - `up()` (V, signal): Incrementa contador, acorda processo bloqueado
  - `tryDown()`: Tenta decrementar sem bloquear
- Implementação: `Semaforo.java` (Etapa 3)

**Sincronização**
- Coordenação de execução entre múltiplos processos para evitar condições de corrida.
- Mecanismos: Semáforos, Mutex, Monitores, Variáveis de condição.

**Sistema Operacional (SO)**
- Software que gerencia hardware e fornece serviços para programas.
- Funções principais: Gerenciamento de processos, memória, I/O, arquivos.
- No SISOP: Implementado na classe `SO.java`.

**STOP**
- Instrução que finaliza a execução de um processo.
- Efeito: Processo muda para estado TERMINATED.

**Syscall (System Call / Chamada de Sistema)**
- Interface para processos solicitarem serviços do sistema operacional.
- Exemplos no SISOP: READ (I/O entrada), WRITE (I/O saída).

---

### T

**Tabela de Páginas**
- Estrutura de dados que mapeia páginas lógicas para frames físicos.
- Uma tabela por processo.
- No SISOP: Array de inteiros onde índice = número da página, valor = número do frame.

**Thread**
- Linha de execução dentro de um processo.
- No SISOP Python: Usado para implementar CPU e I/O concorrentes (T2a).

**Tradução de Endereços**
- Conversão de endereço lógico (usado pelo processo) para endereço físico (memória real).
- Fórmula: `endereço_físico = (frame × tamanho_página) + offset`
- No SISOP: Executado pelo MMU/GerenciadorMemoria.

---

### W

**Word (Palavra)**
- Unidade básica de dados/instrução na memória.
- No SISOP: Estrutura com 4 campos [Opcode, RA, RB, Parâmetro].

---

## Acrônimos e Siglas

- **CPU**: Central Processing Unit (Unidade Central de Processamento)
- **DMA**: Direct Memory Access (Acesso Direto à Memória)
- **FIFO**: First In, First Out (Primeiro a Entrar, Primeiro a Sair)
- **HW**: Hardware
- **I/O**: Input/Output (Entrada/Saída)
- **ISA**: Instruction Set Architecture (Arquitetura do Conjunto de Instruções)
- **MMU**: Memory Management Unit (Unidade de Gerenciamento de Memória)
- **OS**: Operating System (Sistema Operacional)
- **PC**: Program Counter (Contador de Programa)
- **PCB**: Process Control Block (Bloco de Controle de Processo)
- **PID**: Process Identifier (Identificador de Processo)
- **RAM**: Random Access Memory (Memória de Acesso Aleatório)
- **SO**: Sistema Operacional
- **SW**: Software

---

## Estruturas de Dados Principais

### ProcessControlBlock (PCB)
```java
- pid: int                      // Identificador único
- nome: String                  // Nome do processo
- estado: ProcessState          // Estado atual
- pc: int                       // Program Counter
- registradores: int[10]        // R0-R9
- tabelaPaginas: int[]          // Mapeamento página→frame
- tempoCPU: long                // Ciclos executados
- tempoEspera: long             // Tempo em READY
- prioridade: int               // Prioridade de escalonamento
- quantumRestante: int          // Quantum atual
```

### Semaforo
```java
- nome: String                  // Identificador
- valor: int                    // Contador de recursos
- filaEspera: Queue<PCB>        // Processos bloqueados (FIFO)
- totalOperacoesDown: int       // Estatísticas
- totalOperacoesUp: int
- totalBloqueios: int
```

### Mutex
```java
- nome: String                  // Identificador
- disponivel: boolean           // Estado (livre/ocupado)
- proprietario: PCB             // Quem possui o lock
- semaforo: Semaforo            // Implementação interna
```

---

## Valores e Configurações Padrão

### Memória
- **Tamanho total**: 1024 palavras
- **Tamanho da página/frame**: 8 palavras
- **Número de frames**: 128 (1024 ÷ 8)

### Processos
- **Quantum (Java)**: 10 ciclos
- **Quantum (Python)**: 50 instruções
- **Estados**: 5 (NEW, READY, RUNNING, WAITING, TERMINATED)
- **Registradores**: 10 (R0-R9)

### Escalonamento
- **Algoritmo**: Round-Robin
- **Política de fila**: FIFO
- **Preempção**: Por quantum (time slice)

---

## Referências

- **Silberschatz, Galvin, Gagne**: "Operating System Concepts"
- **Tanenbaum, Bos**: "Modern Operating Systems"
- **Dijkstra, E. W.** (1965): "Cooperating sequential processes" (Semáforos)

---

**Última atualização**: Novembro 2024  
**Versão do Sistema**: Etapa 3 completa (Sincronização)  
**Documento**: NOMENCLATURAS.md
