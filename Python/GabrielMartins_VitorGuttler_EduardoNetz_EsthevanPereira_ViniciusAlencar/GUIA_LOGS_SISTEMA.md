# GUIA DE LOGS DO SISTEMA - Python T2a

Este documento explica o significado de cada tipo de log gerado pelo sistema operacional SISOP (versão Python).

---

## Índice
1. [Logs de Inicialização](#logs-de-inicialização)
2. [Logs de Criação de Processos](#logs-de-criação-de-processos)
3. [Logs de Escalonamento](#logs-de-escalonamento)
4. [Logs de Execução](#logs-de-execução)
5. [Logs de I/O](#logs-de-io)
6. [Logs de Finalização](#logs-de-finalização)

---

## Logs de Inicialização

### `[Sistema] Sistema iniciado! Processos serão escalonados automaticamente.`
**O que significa:** O sistema operacional está pronto para executar.
**O que acontece:** As threads de CPU e I/O foram iniciadas e estão aguardando processos.

### `[Escalonador] Sistema de escalonamento iniciado!`
**O que significa:** O escalonador Round-Robin está ativo.
**O que acontece:**
- O escalonador começa a gerenciar a fila de processos prontos
- Processos serão selecionados em ordem FIFO (primeiro a entrar, primeiro a executar)
- Cada processo receberá uma fatia de tempo (quantum) para executar

### `[CPU Thread] CPU iniciada e aguardando processos...`
**O que significa:** A thread da CPU está pronta para executar instruções.
**O que acontece:** A CPU aguarda que o escalonador selecione um processo da fila de prontos.

### `[I/O Device] Dispositivo iniciado e aguardando requisições...`
**O que significa:** O dispositivo de I/O está pronto para processar operações READ/WRITE.
**O que acontece:** O dispositivo aguarda requisições de I/O vindas de processos.

---

## Logs de Criação de Processos

### `[CRIAÇÃO] Processo criado: ID X`
**O que significa:** Um novo processo foi criado e adicionado ao sistema.
**O que acontece:**
- Memória foi alocada (frames da tabela de páginas)
- PCB (Process Control Block) foi criado
- Programa foi carregado na memória
- Processo foi adicionado à fila de prontos
- Estado inicial: READY

**Informações exibidas:**
- `ID (PID)`: Identificador único do processo (número do frame inicial)
- `Frames alocados`: Quais frames de memória foram alocados
- `Estado`: READY (pronto para executar)

---

## Logs de Escalonamento

### `[ESCALONADOR] Processo X selecionado`
**O que significa:** O escalonador escolheu este processo para executar na CPU.
**O que acontece:**
- Processo foi removido da fila de prontos
- Contexto do processo (PC, registradores) será restaurado na CPU
- Processo receberá quantum de tempo para executar

### `[CONTEXTO] Restaurando contexto do processo X`
**O que significa:** O estado anterior do processo está sendo carregado na CPU.
**O que acontece:**
- Program Counter (PC) é restaurado
- Registradores (R0-R9) são restaurados
- Contador de instruções é zerado
- Estado muda de READY para RUNNING

---

## Logs de Execução

### `[CPU] Executando processo X (Quantum: Y)`
**O que significa:** O processo está executando instruções na CPU.
**O que acontece:**
- CPU executa até Y instruções
- Processo pode:
  - Completar antes do quantum (executar STOP)
  - Usar todo o quantum (quantum expira)
  - Bloquear em I/O (executar SYSCALL READ/WRITE)

### `[QUANTUM EXPIRADO] Processo X - Executou Y/Z instruções`
**O que significa:** O processo usou todo seu quantum de tempo.
**Por que acontece:** O processo executou Z instruções (o limite do quantum).
**O que acontece:**
- Processo muda de RUNNING para READY
- Processo retorna ao **FIM** da fila de prontos (Round-Robin)
- Próximo processo da fila terá sua vez
- **Este é o comportamento normal do Round-Robin!**

**Importante:** Quantum expirado NÃO é um erro! É o mecanismo que garante justiça - todos os processos recebem tempo igual de CPU.

---

## Logs de I/O

### `[SYSCALL] READ no endereço X (Processo Y)`
**O que significa:** O processo solicitou uma operação de leitura.
**O que acontece:**
1. Processo cria requisição de I/O
2. Requisição é enviada à fila do dispositivo de I/O
3. Processo muda de RUNNING para BLOCKED
4. Processo sai da CPU (não pode continuar sem o dado)
5. CPU fica livre para executar outro processo

### `[SYSCALL] WRITE do endereço X (Processo Y)`
**O que significa:** O processo solicitou uma operação de escrita.
**O que acontece:**
1. Processo cria requisição de I/O
2. Requisição é enviada à fila do dispositivo de I/O
3. Processo muda de RUNNING para BLOCKED
4. Processo sai da CPU
5. CPU fica livre para executar outro processo

### `[I/O Device] Processando READ/WRITE para processo X`
**O que significa:** O dispositivo está executando a operação de I/O.
**O que acontece:**
- Dispositivo processa a requisição (leva tempo - simulado com sleep)
- Durante este tempo, a CPU continua executando outros processos
- **Este é o paralelismo entre CPU e I/O!**

### `[I/O Device] READ/WRITE concluído para processo X`
**O que significa:** A operação de I/O terminou.
**O que acontece:**
1. Dispositivo gera interrupção para a CPU
2. Processo X muda de BLOCKED para READY
3. Processo X retorna à fila de prontos
4. Quando chegar sua vez, será escalonado novamente

### `[BLOQUEADO] Processo X aguardando I/O`
**O que significa:** O processo está parado aguardando I/O completar.
**O que acontece:**
- Processo está na fila de bloqueados (não na fila de prontos)
- Não consome CPU enquanto aguarda
- Será desbloqueado quando I/O completar

---

## Logs de Finalização

### `[SYSCALL] STOP`
**O que significa:** O processo executou a instrução STOP.
**O que acontece:**
- Processo finalizou sua execução normalmente
- Estado muda de RUNNING para FINISHED
- Próximo: memória será liberada

### `[FINALIZAÇÃO] Processo X FINALIZOU`
**O que significa:** O processo completou sua execução.
**O que acontece:**
1. Processo é marcado como FINISHED
2. Memória é desalocada (frames liberados)
3. PCB é removido do sistema
4. Próximo processo da fila será escalonado

### `Processo X (frame Y) removido.`
**O que significa:** O processo foi completamente removido do sistema.
**O que acontece:**
- Frames de memória foram liberados
- Estão disponíveis para novos processos
- Recurso retornou ao pool de memória livre

---

## Estados de Processo

Durante a execução, um processo passa por diferentes estados:

```
NEW → READY → RUNNING → FINISHED
         ↑        ↓
         └─ BLOCKED (I/O)
```

### NEW
- Processo foi criado
- Memória foi alocada
- Ainda não está na fila de prontos

### READY
- Processo está na fila de prontos
- Aguardando ser selecionado pelo escalonador
- Pode executar quando chegar sua vez

### RUNNING
- Processo está executando na CPU
- Tem quantum de tempo para usar
- Pode ir para READY, BLOCKED ou FINISHED

### BLOCKED
- Processo aguarda operação de I/O
- Não está na fila de prontos
- Não consome tempo de CPU
- Retorna para READY quando I/O completar

### FINISHED
- Processo completou execução (STOP)
- Memória será liberada
- Será removido do sistema

---

## Conceitos Importantes

### Quantum (Fatia de Tempo)
- **O que é:** Número máximo de instruções que um processo pode executar por vez
- **Valor atual:** 50 instruções
- **Por que existe:** Garante que todos os processos recebem tempo de CPU justo
- **Quando expira:** Processo executou todas as 50 instruções sem finalizar ou bloquear

### Round-Robin
- **O que é:** Algoritmo de escalonamento que dá tempo igual para todos
- **Como funciona:**
  1. Processos formam fila (FIFO)
  2. Primeiro da fila executa por quantum de tempo
  3. Se quantum expira, processo vai para o FIM da fila
  4. Próximo da fila executa
- **Vantagem:** Justo - todos processos progridem
- **Desvantagem:** Muitas trocas de contexto se quantum for pequeno

### Context Switch (Troca de Contexto)
- **O que é:** Salvar estado de um processo e carregar estado de outro
- **Quando acontece:**
  - Quantum expira
  - Processo bloqueia em I/O
  - Processo finaliza
- **Custo:** Leva tempo (overhead)
- **O que é salvo/restaurado:**
  - Program Counter (PC)
  - Registradores (R0-R9)
  - Estado do processo

### Paralelismo CPU-I/O
- **Conceito:** CPU e dispositivo I/O trabalham ao mesmo tempo
- **Como:**
  - Processo A bloqueia em I/O
  - CPU executa Processo B
  - Enquanto isso, I/O processa requisição de A
  - Quando I/O termina, A volta para fila de prontos
- **Benefício:** Melhor utilização de recursos

---

## Exemplos de Sequências de Logs

### Exemplo 1: Processo que completa dentro do quantum

```
[ESCALONADOR] Processo 0 selecionado
[CONTEXTO] Restaurando contexto do processo 0
[CPU] Executando processo 0 (Quantum: 50)
[SYSCALL] STOP
[FINALIZAÇÃO] Processo 0 FINALIZOU
Processo 0 (frame 0) removido.
```

**Interpretação:** Processo 0 executou menos de 50 instruções e finalizou normalmente.

### Exemplo 2: Processo que usa todo o quantum

```
[ESCALONADOR] Processo 1 selecionado
[CONTEXTO] Restaurando contexto do processo 1
[CPU] Executando processo 1 (Quantum: 50)
[QUANTUM EXPIRADO] Processo 1 - Executou 50/50 instruções
```

**Interpretação:** Processo 1 executou as 50 instruções completas e voltou para a fila. Será executado novamente mais tarde.

### Exemplo 3: Processo que bloqueia em I/O

```
[ESCALONADOR] Processo 2 selecionado
[CONTEXTO] Restaurando contexto do processo 2
[CPU] Executando processo 2 (Quantum: 50)
[SYSCALL] READ no endereço 10 (Processo 2)
[BLOQUEADO] Processo 2 aguardando I/O
[I/O Device] Processando READ para processo 2...
[I/O Device] READ concluído para processo 2
```

**Interpretação:** Processo 2 precisou ler dados e ficou bloqueado. Enquanto I/O processava, CPU executou outros processos. Quando I/O terminou, processo 2 voltou para fila de prontos.

---

## Troubleshooting

### "Processo teve quantum expirado" aparece muito
**É normal!** Significa que o processo precisa de mais tempo do que um quantum para completar.
- Processo executará múltiplas vezes até finalizar
- Cada vez executa 50 instruções
- É assim que Round-Robin funciona

### "Processo BLOQUEADO aguardando I/O"
**É normal!** Significa que o processo precisa de dados externos.
- Processo não consome CPU enquanto aguarda
- Outros processos executam
- Quando I/O completar, processo retorna

### Muitas trocas de contexto
**Possível causa:** Quantum muito pequeno
- Quantum atual: 50 instruções
- Se aumentar: menos trocas, mais tempo por processo
- Se diminuir: mais trocas, mais responsividade

---

## Processo NOP (No Operation Process)

### O que é o processo NOP?
O processo NOP é um programa especial que mantém o sistema operacional ativo e rodando continuamente.

**Características:**
- **Nome:** `nop`
- **Função:** Loop infinito executando operações mínimas
- **Propósito:** Garantir que o sistema nunca fique sem processos para executar

### Por que usar o NOP?

**Problema sem NOP:**
Se todos os processos terminarem, o escalonador ficaria sem processos na fila de prontos. O sistema pararia de executar e não seria possível adicionar novos processos via CLI.

**Solução com NOP:**
- O processo NOP fica em loop infinito
- Sempre há pelo menos um processo na fila de prontos
- Sistema continua operacional indefinidamente
- Usuário pode adicionar novos processos a qualquer momento
- CLI permanece responsiva

### Como usar o processo NOP

```bash
# Criar o processo NOP para manter o sistema ativo
[Procs:0 Ready:0 Blocked:0] > new nop

# Iniciar o escalonamento
[Procs:1 Ready:1 Blocked:0] > start

# Sistema continua rodando - você pode adicionar mais processos
[Procs:1 Ready:1 Blocked:0] > new fatorial
[Procs:2 Ready:2 Blocked:0] > new progMinimo
```

### Comportamento do NOP

**Características de execução:**
- Executa loop infinito: incrementa contador e volta ao início
- Consome quantum completo (50 instruções)
- Retorna ao fim da fila (Round-Robin)
- Permite que outros processos executem entre suas iterações
- Nunca finaliza (não tem instrução STOP)

**Logs típicos do NOP:**
```
[ESCALONADOR] Processo X selecionado
[CONTEXTO] Restaurando contexto (PC=1, Quantum=50)
[QUANTUM EXPIRADO] Processo X - Executou 50/50 instruções
```

### Quando usar o NOP

**Recomendado:**
- ✓ Ao iniciar o sistema pela primeira vez
- ✓ Quando você quer manter o sistema rodando por muito tempo
- ✓ Para demonstrar escalonamento contínuo
- ✓ Em ambientes de teste e desenvolvimento

**Não necessário:**
- Se você vai executar processos continuamente
- Se vai finalizar o sistema logo após os testes
- Se tem muitos processos ativos na fila

### Remover o processo NOP

Se desejar parar o processo NOP:

```bash
# Listar processos para encontrar o PID do NOP
[Procs:X Ready:Y Blocked:0] > ps

# Remover o processo pelo PID
[Procs:X Ready:Y Blocked:0] > rm <PID_do_NOP>
```

**Atenção:** Ao remover o NOP, certifique-se de ter outros processos na fila ou o sistema pode parar de escalonar.

---

**Última atualização:** Novembro 2024  
**Versão do Sistema:** T2a (Concorrência e I/O Assíncrono)  
**Documento:** GUIA_LOGS_SISTEMA.md
