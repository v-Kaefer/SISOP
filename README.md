# SISOP - Sistema Operacional Virtual
**Trabalho Acadêmico - Sistemas Operacionais**

Este projeto implementa um **Sistema Operacional completo** que funciona em uma **Máquina Virtual**, desenvolvido em Java para fins educacionais. O sistema permite entender na prática como funcionam os principais componentes de um SO real.

## Estado Atual do Projeto

### ✅ Etapas Implementadas
- **[Etapa 1]** Gerenciamento de Memória com Paginação - **COMPLETA**
- **[Etapa 2]** Gerenciamento de Processos com Round-Robin - **COMPLETA**
- **[Etapa 3]** Sincronização entre Processos (Semáforos, Mutex) - **COMPLETA**

### 📋 Próximas Etapas Planejadas
- **[Etapa 4]** Sistema de Arquivos
- **[Etapa 5]** Interface Gráfica de Administração

## Arquitetura do Sistema

```
SISOP/
├── Sistema.java                      # Ponto de entrada principal
├── hardware/                         # Hardware virtual completo
│   ├── CPU.java                     # Processador com contexto de processos
│   ├── Memory.java                  # Memória RAM virtual
│   ├── Word.java                    # Palavra de memória (instrução/dado)
│   ├── Opcode.java                  # Conjunto de instruções da CPU
│   └── HW.java                      # Hardware integrado
├── memory/                           # Gerenciamento de Memória (Etapa 1)
│   ├── GerenciadorMemoria.java      # Sistema de paginação
│   ├── PosicaoDeMemoria.java        # Representação de instrução/dado
│   ├── MemoryManagerPonte.java      # Interface de integração
│   ├── TesteGerenciadorMemoria.java # Testes unitários de memória
│   └── TesteIntegracao.java         # Testes de integração
├── software/                         # Sistema Operacional (Etapas 2 e 3)
│   ├── SO.java                      # Núcleo do sistema operacional
│   ├── ProcessState.java            # Estados de processo (NEW, READY, WAITING, etc.)
│   ├── ProcessControlBlock.java     # PCB completo com contexto
│   ├── RoundRobinScheduler.java     # Escalonador Round-Robin
│   ├── ProcessManager.java          # Gerenciador de processos
│   ├── Semaforo.java                # Semáforos (Etapa 3)
│   ├── Mutex.java                   # Mutex - Exclusão Mútua (Etapa 3)
│   ├── TesteGerenciaProcessos.java  # Testes modulares
│   ├── TesteSincronizacao.java      # Testes de sincronização (Etapa 3)
│   ├── Utilities.java               # Funções auxiliares
│   ├── InterruptHandling.java       # Tratamento de interrupções
│   └── SysCallHandling.java         # Chamadas de sistema
├── programs/                         # Programas executáveis
│   ├── Programs.java                # Biblioteca de programas
│   └── Program.java                 # Estrutura de programa
└── exemplos/                         # Exemplos práticos
    ├── ExemploGerenciaProcessos.java      # Exemplo básico
    ├── ExemploExecucaoConcorrente.java    # Execução concorrente
    ├── ExemploProdutorConsumidor.java     # Produtor-Consumidor (Etapa 3)
    └── ExemploSecaoCritica.java           # Seção Crítica com Mutex (Etapa 3)
```

## Documentação por Etapa

### 📚 Etapa 1 - Gerenciamento de Memória
- **[DOCUMENTACAO_ETAPA01.md](DOCUMENTACAO_ETAPA01.md)** - Guia completo da implementação de paginação

### 📚 Etapa 2 - Gerenciamento de Processos  
- **[DOCUMENTACAO_ETAPA02.md](DOCUMENTACAO_ETAPA02.md)** - Guia completo do gerenciamento de processos

### 📚 Etapa 3 - Sincronização entre Processos
- **[DOCUMENTACAO_ETAPA03.md](DOCUMENTACAO_ETAPA03.md)** - Guia completo de semáforos e mutex

## Como Executar o Sistema

### 🚀 Execução Básica (Sistema Original)
```bash
# Compilar o sistema
javac Sistema.java

# Executar programa de exemplo
java Sistema
```

### 🧪 Testes Automatizados

#### Testes de Memória (Etapa 1)
```bash
javac memory/*.java
java memory.TesteGerenciadorMemoria    # Testes unitários
java memory.TesteIntegracao           # Testes de integração
```

#### Testes de Processos (Etapa 2)
```bash
javac software/*.java memory/*.java hardware/*.java programs/*.java
java software.TesteGerenciaProcessos  # Testes modulares completos
```

#### Testes de Sincronização (Etapa 3)
```bash
javac software/TesteSincronizacao.java
java software.TesteSincronizacao      # Testes de semáforos e mutex
```

#### Exemplos Práticos
```bash
javac exemplos/*.java software/*.java memory/*.java hardware/*.java programs/*.java
java exemplos.ExemploGerenciaProcessos      # Exemplo básico
java exemplos.ExemploExecucaoConcorrente    # Execução concorrente
java exemplos.ExemploProdutorConsumidor     # Produtor-Consumidor (Etapa 3)
java exemplos.ExemploSecaoCritica           # Seção Crítica com Mutex (Etapa 3)
```

## Funcionalidades Implementadas

### 🔧 Etapa 1 - Gerenciamento de Memória
- ✅ **Paginação**: Memória dividida em frames/páginas configuráveis
- ✅ **Alocação dinâmica**: Alocação e desalocação de frames por processo
- ✅ **Tradução de endereços**: Conversão lógico → físico automática
- ✅ **Proteção de memória**: Isolamento entre processos
- ✅ **Estatísticas**: Monitoramento detalhado de uso
- ✅ **Tratamento de fragmentação**: Gerenciamento eficiente

### ⚙️ Etapa 2 - Gerenciamento de Processos
- ✅ **Estados de processo**: NEW, READY, RUNNING, WAITING, TERMINATED
- ✅ **Process Control Block (PCB)**: Contexto completo com estatísticas
- ✅ **Escalonamento Round-Robin**: Quantum configurável
- ✅ **Context switching**: Troca eficiente entre processos
- ✅ **Execução concorrente**: Múltiplos processos simultâneos
- ✅ **Gestão de recursos**: Criação, admissão e finalização automática

### 🔒 Etapa 3 - Sincronização entre Processos
- ✅ **Semáforos**: Primitiva clássica de sincronização (down/up)
- ✅ **Mutex**: Exclusão mútua com ownership
- ✅ **Operações bloqueantes**: down() e lock() com fila de espera
- ✅ **Operações não-bloqueantes**: tryDown() e tryLock()
- ✅ **Fila FIFO**: Processos acordados na ordem de chegada
- ✅ **Problema Produtor-Consumidor**: Implementação completa
- ✅ **Proteção de seção crítica**: Garantia de exclusão mútua
- ✅ **Estatísticas**: Monitoramento de bloqueios e operações

## Programas Disponíveis

O sistema inclui uma biblioteca de programas prontos para execução:
- **`fatorial`** / **`fatorialV2`**: Cálculo de fatorial
- **`fibonacci10`** / **`fibonacci10v2`**: Sequência de Fibonacci
- **`progMinimo`**: Programa mínimo para testes
- **`PC`**: Algoritmo bubble sort (ordenação)
- **`fatorialWRITE`**: Fatorial com saída
- **`fibonacciREAD`**: Fibonacci com entrada

## Resultados Esperados

### Demonstração do Sistema Funcionando

#### 1. Execução Básica
```
java Sistema
```
**Resultado**: Execução do programa `fatorialV2` com cálculo de 5! = 120, incluindo dump de memória antes/depois.

#### 2. Teste de Memória
```
java memory.TesteGerenciadorMemoria
```
**Resultado**: Validação completa do sistema de paginação com relatórios de frames, tradução de endereços e fragmentação.

#### 3. Teste de Processos
```
java software.TesteGerenciaProcessos
```
**Resultado**: Execução de 5 testes modulares validando todos os componentes do gerenciamento de processos.

#### 4. Execução Concorrente
```
java examples.ExemploExecucaoConcorrente
```
**Resultado**: Demonstração de múltiplos processos executando simultaneamente com escalonamento Round-Robin visível.

## Conceitos Implementados (Para Estudo Acadêmico)

### 🧠 Fundamentos de Sistemas Operacionais
- **Máquina Virtual**: Computador completo simulado em software
- **Conjunto de Instruções**: CPU virtual com opcodes (LDI, ADD, MULT, JMP, etc.)
- **Ciclo de execução**: Fetch → Decode → Execute → Write-back

### 💾 Gerenciamento de Memória
- **Paginação**: Solução para fragmentação externa
- **Tradução de endereços**: MMU virtual (lógico → físico)
- **Proteção de memória**: Isolamento entre processos
- **Alocação dinâmica**: Gestão automática de frames

### ⚙️ Gerenciamento de Processos
- **Estados de processo**: Ciclo de vida completo (NEW → READY → RUNNING → TERMINATED)
- **Context switching**: Salvamento/restauração de contexto de CPU
- **Process Control Block**: Estrutura de dados completa do processo
- **Escalonamento Round-Robin**: Algoritmo de escalonamento justo

## Testes e Validação

O projeto inclui uma suíte completa de testes automatizados:

### ✅ Testes Unitários
- **ProcessState**: Validação de todos os estados
- **ProcessControlBlock**: Criação e manipulação de PCB
- **RoundRobinScheduler**: Algoritmo de escalonamento
- **GerenciadorMemoria**: Sistema de paginação

### ✅ Testes de Integração
- **Múltiplos processos**: Execução concorrente real
- **Programas da biblioteca**: Validação com código real
- **Memória + Processos**: Integração completa

### ✅ Métricas de Performance
- **Context switches**: Número de trocas de processo
- **Tempo de CPU**: Por processo e total
- **Utilização de memória**: Estatísticas detalhadas
- **Quantum**: Eficiência do escalonamento

## Desenvolvimento e CI/CD

O projeto utiliza **GitHub Actions** para integração contínua:
- ✅ Compilação automática em todo push/PR
- ✅ Execução de todos os testes
- ✅ Validação em ambiente limpo (Ubuntu + Java 17)
- ✅ Proteção de qualidade de código

## Contribuições Acadêmicas

Este projeto demonstra implementações completas de:
1. **Simulação de hardware**: CPU, Memória, Instruções
2. **Algoritmos de SO**: Paginação, Round-Robin, Context switching
3. **Estruturas de dados**: PCB, Filas de processos, Tabelas de páginas
4. **Testes automatizados**: Validação sistemática de funcionalidades
5. **Documentação técnica**: Guias didáticos e especificações

---

### 📖 Documentação Técnica Detalhada
- **[DOCUMENTACAO_ETAPA01.md](DOCUMENTACAO_ETAPA01.md)** - Implementação completa do gerenciamento de memória
- **[DOCUMENTACAO_ETAPA02.md](DOCUMENTACAO_ETAPA02.md)** - Implementação completa do gerenciamento de processos
- **[CHANGELOG.md](CHANGELOG.md)** - Histórico completo de desenvolvimento

### 🔧 Para Desenvolvedores
- **[.github/instructions/.instructions.md](.github/instructions/.instructions.md)** - Especificações técnicas e contexto para próximas etapas