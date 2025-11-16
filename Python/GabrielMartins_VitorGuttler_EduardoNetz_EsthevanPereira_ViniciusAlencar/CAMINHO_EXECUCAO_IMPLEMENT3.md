# Caminho de Execução - Implement3 Sistema Operacional

**Análise do Fluxo de Execução**  
**Implementação:** Part2/Implement3/sistema_os.py  
**Data:** 2025-11-10

---

## ÍNDICE

1. [Arquitetura Geral](#1-arquitetura-geral)
2. [Fluxo de Inicialização](#2-fluxo-de-inicialização)
3. [Fluxo de Criação de Processo](#3-fluxo-de-criação-de-processo)
4. [Fluxo de Execução de Programa](#4-fluxo-de-execução-de-programa)
5. [Fluxo de I/O (Estado Atual - COM BUGS)](#5-fluxo-de-io-estado-atual---com-bugs)
6. [Fluxo de I/O (Esperado - CORRETO)](#6-fluxo-de-io-esperado---correto)
7. [Fluxo de Escalonamento](#7-fluxo-de-escalonamento)
8. [Diagramas de Sequência](#8-diagramas-de-sequência)

---

## 1. ARQUITETURA GERAL

### 1.1. Componentes e Threads

```
┌─────────────────────────────────────────────────────────────┐
│                    IMPLEMENT3 ARCHITECTURE                   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ MAIN THREAD  │  │  CPU THREAD  │  │  I/O THREAD  │      │
│  │   (Shell)    │  │              │  │  (IODevice)  │      │
│  ├──────────────┤  ├──────────────┤  ├──────────────┤      │
│  │              │  │              │  │              │      │
│  │ • run_shell()│  │ • CPUThread  │  │ • IODevice   │      │
│  │ • input()    │  │ • run()      │  │ • run()      │      │
│  │ • comandos   │  │ • escalonador│  │ • io_queue   │      │
│  │              │  │ • semaphore  │  │ • time.sleep │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                 │                 │              │
│         ▼                 ▼                 ▼              │
│  ┌──────────────────────────────────────────────────┐      │
│  │           ESTRUTURAS COMPARTILHADAS              │      │
│  ├──────────────────────────────────────────────────┤      │
│  │ • GerenteProcessos (ready_queue, blocked_queue)  │      │
│  │ • CPU (irpt_io_complete)                         │      │
│  │ • Queue (io_queue)                               │      │
│  │ • Semaphore (cpu_semaphore)                      │      │
│  │ • Memory (shared)                                │      │
│  └──────────────────────────────────────────────────┘      │
└─────────────────────────────────────────────────────────────┘
```

### 1.2. Estados de Processo

```
┌─────────┐
│   NEW   │ (Processo criado)
└────┬────┘
     │
     ▼
┌─────────┐  quantum    ┌──────────┐
│  READY  │◄────────────┤ RUNNING  │
└────┬────┘  esgotado   └────┬─────┘
     ▲                       │
     │                       │ I/O request
     │                       ▼
     │  I/O complete   ┌──────────┐
     └─────────────────┤ BLOCKED  │
                       └────┬─────┘
                            │ STOP
                            ▼
                       ┌──────────┐
                       │ FINISHED │
                       └──────────┘
```

---

## 2. FLUXO DE INICIALIZAÇÃO

### 2.1. Passo a Passo

```python
# Linha 1003-1005
if __name__ == "__main__":
    s = Sistema(tam_mem=1024, tam_pg=16, quantum=5)
    s.run()
```

**Sequência de Inicialização:**

```
1. Sistema.__init__()
   ├─ 1.1. Cria Hardware
   │      └─ HW(tam_mem=1024)
   │         ├─ Memory(1024)
   │         └─ CPU(mem)
   │
   ├─ 1.2. Cria Sistema Operacional
   │      └─ SO(hw, tam_pg=16, quantum=5)
   │         ├─ GerenteMemoria(1024, 16)
   │         ├─ GerenteProcessos(gm, hw, utils)
   │         ├─ InterruptHandling(cpu, gp)
   │         ├─ Queue() → io_queue
   │         ├─ IODevice(hw, gp, io_queue, ih) ← BUG: falta ih
   │         ├─ Escalonador(cpu, gp, quantum=5)
   │         └─ CPUThread(cpu, escalonador, semaphore)
   │
   ├─ 1.3. Cria biblioteca de programas
   │      └─ Programs()
   │
   └─ 1.4. Inicializa controle
          ├─ system_running = True
          └─ cpu_semaphore = Semaphore(1)

2. Sistema.run()
   ├─ 2.1. Inicia Thread I/O
   │      └─ io_device.start()
   │         └─ Roda IODevice.run() em thread daemon
   │
   ├─ 2.2. Inicia Thread CPU
   │      └─ cpu_thread.start()
   │         └─ Roda CPUThread.run() em thread daemon
   │
   └─ 2.3. Executa Shell (Main Thread)
          └─ run_shell()
             └─ Loop infinito aguardando comandos
```

**Threads Ativas após Inicialização:**

| Thread | Estado | Função |
|--------|--------|--------|
| Main (Shell) | RUNNING | Aguarda input() do usuário |
| CPU Thread | WAITING | Bloqueado no semaphore.acquire() |
| I/O Thread | WAITING | Bloqueado no queue.get(timeout=0.5) |

---

## 3. FLUXO DE CRIAÇÃO DE PROCESSO

### 3.1. Comando: `new fatorialV2`

```
SHELL THREAD (Main)
─────────────────────────────────────────────────────────

1. Usuário digita: new fatorialV2
   │
   ▼
2. Shell processa comando (linha ~950)
   │
   ├─ 2.1. Recupera programa
   │      └─ prog = progs.retrieve_program("fatorialV2")
   │         └─ Retorna lista de 20 Words
   │
   ├─ 2.2. Chama GerenteProcessos
   │      └─ so.gp.cria_processo(prog)
   │         │
   │         ├─ 2.2.1. Aloca memória
   │         │      └─ gm.aloca(len(prog)=20)
   │         │         └─ Retorna page_table=[0, 1] (2 frames)
   │         │
   │         ├─ 2.2.2. Cria PCB
   │         │      └─ pcb = PCB(page_table=[0,1])
   │         │         ├─ pcb.id = 0 (frame inicial)
   │         │         ├─ pcb.pc = 0
   │         │         ├─ pcb.registers = [0]*10
   │         │         └─ pcb.state = READY
   │         │
   │         ├─ 2.2.3. Carrega programa na memória
   │         │      └─ _load_program_to_memory(prog, page_table)
   │         │         └─ Copia 20 Words para frames 0 e 1
   │         │
   │         └─ 2.2.4. Adiciona à fila de prontos
   │                └─ ready_queue.append(pcb)
   │                   └─ ready_queue = [PCB(id=0)]
   │
   └─ 2.3. Retorna ao prompt
          └─ Shell aguarda próximo comando

CPU THREAD
──────────────────────────────────────────────────────

3. CPU Thread detecta processo na fila
   │
   ├─ 3.1. Semaphore.acquire() → libera (já tinha released)
   │
   ├─ 3.2. Busca processo
   │      └─ pcb = gp.get_next_ready()
   │         └─ Retorna PCB(id=0) e remove da fila
   │
   ├─ 3.3. Configura contexto
   │      └─ cpu.set_context(pcb)
   │         ├─ cpu.running_process = pcb
   │         ├─ cpu.pc = pcb.pc = 0
   │         ├─ cpu.reg = pcb.registers.copy()
   │         └─ pcb.state = RUNNING
   │
   ├─ 3.4. Executa
   │      └─ cpu.run(quantum=5)
   │         └─ [ENTRA NO LOOP DE EXECUÇÃO - Ver Seção 4]
   │
   └─ 3.5. Após execução
          ├─ Se FINISHED: desaloca processo
          ├─ Se READY: add_ready(pcb)
          └─ Se BLOCKED: mantém na blocked_queue
```

**Resultado:**
- Processo criado: ID=0, Estado=READY
- Memória alocada: Frames 0 e 1 (endereços físicos 0-31)
- Processo na fila: ready_queue=[PCB(0)]
- **CPU Thread automaticamente escalona e executa**

---

## 4. FLUXO DE EXECUÇÃO DE PROGRAMA

### 4.1. Loop de Execução na CPU

```python
# CPU.run(quantum=5) - Linha ~88-165

LOOP DE EXECUÇÃO (CPU Thread)
──────────────────────────────────────────────────────

while not cpu_stop and (instructions_executed < quantum):
    
    ┌─────────────────────────────────────────┐
    │ CICLO DE INSTRUÇÃO                      │
    ├─────────────────────────────────────────┤
    │                                         │
    │ 1. FETCH (Busca)                        │
    │    ├─ Traduz PC lógico → físico        │
    │    │  └─ page = pc // 16               │
    │    │  └─ offset = pc % 16               │
    │    │  └─ frame = page_table[page]       │
    │    │  └─ phys = frame*16 + offset       │
    │    │                                     │
    │    └─ Lê instrução da memória          │
    │       └─ ir = mem[phys]                │
    │                                         │
    │ 2. DECODE (Decodificação)               │
    │    └─ opc, ra, rb, p = ir               │
    │                                         │
    │ 3. EXECUTE (Execução)                   │
    │    ├─ LDI: reg[ra] = p; pc++           │
    │    ├─ ADD: reg[ra] += reg[rb]; pc++    │
    │    ├─ MULT: reg[ra] *= reg[rb]; pc++   │
    │    ├─ STD: mem[p] = reg[ra]; pc++      │
    │    ├─ SYSCALL: sys_call.handle(); pc++ │
    │    └─ STOP: cpu_stop = True            │
    │                                         │
    │ 4. CHECK INTERRUPTS                     │
    │    ├─ Se irpt != NO_INTERRUPT:         │
    │    │  └─ ih.handle(irpt); break        │
    │    │                                     │
    │    └─ Se irpt_io_complete != None:     │
    │       └─ ih.handle_io_complete(pid)    │
    │                                         │
    └─────────────────────────────────────────┘
    
    instructions_executed++
```

### 4.2. Exemplo: Execução de fatorialV2

```
PROGRAMA: fatorialV2 (20 instruções)
──────────────────────────────────────────────────────

PC  | Instrução          | Ação
────┼────────────────────┼──────────────────────────
 0  | LDI R0, 5          | R0 = 5
 1  | STD R0, 19         | mem[19] = 5
 2  | LDD R0, 19         | R0 = mem[19] = 5
 3  | LDI R1, -1         | R1 = -1
 4  | LDI R2, 13         | R2 = 13
    | [Quantum=5 ESGOTADO - Context Switch]
────┼────────────────────┼──────────────────────────
 5  | JMPIL R2, R0       | if R0<0: pc=13, else: pc=6
 6  | LDI R1, 1          | R1 = 1
 7  | LDI R6, 1          | R6 = 1
 8  | LDI R7, 13         | R7 = 13
 9  | JMPIE R7, R0       | if R0==0: pc=13, else: pc=10
    | [Quantum=5 ESGOTADO - Context Switch]
────┼────────────────────┼──────────────────────────
10  | MULT R1, R0        | R1 = R1 * R0 = 1*5 = 5
11  | SUB R0, R6         | R0 = R0 - 1 = 4
12  | JMP 9              | pc = 9
 9  | JMPIE R7, R0       | if R0==0: pc=13, else: pc=10
10  | MULT R1, R0        | R1 = 5 * 4 = 20
    | [Quantum=5 ESGOTADO - Context Switch]
────┼────────────────────┼──────────────────────────
    | ... loop continua ...
    | R1 = 20*3*2*1 = 120
────┼────────────────────┼──────────────────────────
13  | STD R1, 18         | mem[18] = 120
14  | LDI R8, 2          | R8 = 2 (WRITE)
15  | LDI R9, 18         | R9 = 18 (endereço)
16  | SYSCALL            | ═══► [VER SEÇÃO 5 - I/O]
```

---

## 5. FLUXO DE I/O (ESTADO ATUAL - COM BUGS)

### 5.1. Fluxo de System Call WRITE (BUGADO)

```
CPU THREAD - Executa SYSCALL (PC=16)
─────────────────────────────────────────────────────

1. CPU executa SYSCALL (linha ~149)
   │
   ├─ Opcode == SYSCALL
   │
   └─> sys_call.handle()
       │
       ▼

SYSTEM CALL HANDLER (linha ~605-625)
─────────────────────────────────────────────────────

2. SysCallHandling.handle()
   │
   ├─ Verifica tipo: cpu.reg[8] == 2 (WRITE)
   │
   ├─ 2.1. Cria requisição I/O
   │      └─ request = {
   │           'pid': cpu.running_process.id,
   │           'operation': 'WRITE',
   │           'address': cpu.reg[9]  # 18
   │        }
   │
   ├─ 2.2. Adiciona à fila I/O
   │      └─ io_queue.put(request)
   │         └─ Enfileira para IODevice thread
   │
   ├─ 2.3. Bloqueia processo
   │      └─ gp.block_process(cpu.running_process)
   │         ├─ pcb.state = BLOCKED
   │         ├─ Remove de ready_queue
   │         └─ Adiciona a blocked_queue
   │
   └─ 2.4. Para CPU
          └─ cpu.cpu_stop = True
             └─ Sai do loop de execução

🐛 BUG #1: PC NÃO FOI INCREMENTADO!
   ─────────────────────────────────
   • PC ainda é 16 (aponta para SYSCALL)
   • Quando processo desbloquear, vai executar SYSCALL DE NOVO!


CPU THREAD - Após SYSCALL
─────────────────────────────────────────────────────

3. CPU retorna de run()
   │
   ├─ pcb.state == BLOCKED
   │
   └─ Permanece na blocked_queue (não volta para ready)


I/O DEVICE THREAD
─────────────────────────────────────────────────────

4. IODevice.run() detecta requisição (linha ~427-464)
   │
   ├─ 4.1. Dequeue requisição
   │      └─ request = io_queue.get(timeout=0.5)
   │         └─ pid=0, operation='WRITE', address=18
   │
   ├─ 4.2. Simula latência
   │      └─ time.sleep(2.0)  # 2 segundos
   │
   ├─ 4.3. Processa WRITE
   │      ├─ Traduz endereço lógico 18 → físico
   │      ├─ Lê valor da memória: mem[phys].p = 120
   │      └─ print("[I/O] Processo 0 escreveu: 120")
   │
   └─ 4.4. Gera interrupção
          └─ cpu.irpt_io_complete = 0  # PID do processo
             └─ Sinaliza para CPU desbloquear processo


CPU THREAD - Detecta Interrupção
─────────────────────────────────────────────────────

5. CPU verifica interrupção (linha ~161)
   │
   ├─ if cpu.irpt_io_complete is not None:
   │
   └─> ih.handle_io_complete(0)


INTERRUPT HANDLER (linha ~540-554)
─────────────────────────────────────────────────────

6. InterruptHandling.handle_io_complete(pid=0)
   │
   ├─ 6.1. Busca processo
   │      └─ pcb = gp._find_pcb(0)
   │         └─ PCB(id=0, state=BLOCKED, pc=16) ← AINDA 16!
   │
   ├─ 6.2. Desbloqueia processo
   │      └─ gp.unblock_process(0)
   │         ├─ Remove de blocked_queue
   │         ├─ pcb.state = READY
   │         └─ Adiciona a ready_queue
   │
   └─ 6.3. Limpa interrupção
          └─ cpu.irpt_io_complete = None

🐛 BUG #1 MANIFESTADO:
   ──────────────────────
   • PCB voltou para ready_queue com PC=16
   • Próxima execução: vai executar SYSCALL NOVAMENTE!


CPU THREAD - Escalona Novamente
─────────────────────────────────────────────────────

7. CPU pega processo da fila
   │
   ├─ pcb = gp.get_next_ready()
   │    └─ PCB(id=0, pc=16, state=READY)
   │
   ├─ cpu.set_context(pcb)
   │    ├─ cpu.pc = 16  ← SYSCALL DE NOVO!
   │    └─ pcb.state = RUNNING
   │
   └─ cpu.run(quantum=5)
       │
       └─> Executa instrução no PC=16
           └─> SYSCALL! 🔄
               └─> Bloqueia novamente
                   └─> Loop infinito! ❌

═══════════════════════════════════════════════════════
RESULTADO: DEADLOCK - Processo nunca avança além do SYSCALL
═══════════════════════════════════════════════════════
```

### 5.2. Diagrama de Estados (BUGADO)

```
┌─────────┐
│ RUNNING │  PC=16 (SYSCALL)
└────┬────┘
     │ SYSCALL
     │ (PC não incrementa)
     ▼
┌─────────┐
│ BLOCKED │  PC=16 (ainda no SYSCALL!)
└────┬────┘
     │ I/O completo
     │ (handler NÃO avança PC)
     ▼
┌─────────┐
│  READY  │  PC=16 (SYSCALL)
└────┬────┘
     │ Escalona
     ▼
┌─────────┐
│ RUNNING │  PC=16 (executa SYSCALL DE NOVO)
└────┬────┘
     │
     └──► LOOP INFINITO! 🔄
```

---

## 6. FLUXO DE I/O (ESPERADO - CORRETO)

### 6.1. Fluxo Correto com Correções

```
CPU THREAD - Executa SYSCALL (PC=16)
─────────────────────────────────────────────────────

1. CPU executa SYSCALL
   └─> sys_call.handle()


SYSTEM CALL HANDLER (IGUAL AO ATUAL)
─────────────────────────────────────────────────────

2. SysCallHandling.handle()
   │
   ├─ Cria requisição I/O
   ├─ Adiciona à io_queue
   ├─ Bloqueia processo (state=BLOCKED)
   └─ Para CPU (cpu_stop=True)

   ✅ CORRETO: PC NÃO deve incrementar aqui
              (será incrementado no handler)


I/O DEVICE THREAD (IGUAL AO ATUAL)
─────────────────────────────────────────────────────

3. IODevice processa requisição
   │
   ├─ Dequeue
   ├─ time.sleep(2.0)
   ├─ Processa WRITE
   └─ Gera interrupção: cpu.irpt_io_complete = 0


INTERRUPT HANDLER (CORRIGIDO)
─────────────────────────────────────────────────────

4. InterruptHandling.handle_io_complete(pid=0)
   │
   ├─ 4.1. Busca processo
   │      └─ pcb = gp._find_pcb(0)
   │         └─ PCB(id=0, state=BLOCKED, pc=16)
   │
   ├─ ✅ FIX: AVANÇA PC!
   │      └─ pcb.pc += 1
   │         └─ pcb.pc = 17 (próxima instrução)
   │
   ├─ 4.2. Desbloqueia processo
   │      └─ gp.unblock_process(0)
   │         ├─ pcb.state = READY
   │         └─ Adiciona a ready_queue
   │            └─ PCB(id=0, pc=17, state=READY) ✅
   │
   └─ 4.3. Limpa interrupção
          └─ cpu.irpt_io_complete = None


CPU THREAD - Escalona Novamente
─────────────────────────────────────────────────────

5. CPU pega processo da fila
   │
   ├─ pcb = gp.get_next_ready()
   │    └─ PCB(id=0, pc=17, state=READY) ✅
   │
   ├─ cpu.set_context(pcb)
   │    ├─ cpu.pc = 17  ✅ PRÓXIMA INSTRUÇÃO!
   │    └─ pcb.state = RUNNING
   │
   └─ cpu.run(quantum=5)
       │
       └─> Executa instrução no PC=17
           └─> STOP! ✅ Programa termina normalmente


═══════════════════════════════════════════════════════
RESULTADO: SUCESSO - Processo completa execução
═══════════════════════════════════════════════════════
```

### 6.2. Diagrama de Estados (CORRETO)

```
┌─────────┐
│ RUNNING │  PC=16 (SYSCALL)
└────┬────┘
     │ SYSCALL
     │ (PC não incrementa ainda)
     ▼
┌─────────┐
│ BLOCKED │  PC=16 (aguardando I/O)
└────┬────┘
     │ I/O completo
     │ handler AVANÇA PC → 17 ✅
     ▼
┌─────────┐
│  READY  │  PC=17 (próxima instrução)
└────┬────┘
     │ Escalona
     ▼
┌─────────┐
│ RUNNING │  PC=17 (STOP)
└────┬────┘
     │ STOP
     ▼
┌─────────┐
│FINISHED │ ✅ Sucesso!
└─────────┘
```

### 6.3. Código da Correção

```python
# ARQUIVO: sistema_os.py
# LINHA: ~540-554

class InterruptHandling:
    def handle_io_complete(self, process_id):
        print(f"      INTERRUPCAO: I/O completado para processo {process_id}")
        
        pcb = self.gp._find_pcb(process_id)
        if pcb and pcb.state == PCB.ProcessState.BLOCKED:
            
            # ═══════════════════════════════════════
            # ✅ FIX BUG #1: AVANÇA PC DO PROCESSO
            # ═══════════════════════════════════════
            pcb.pc += 1  # ← ADICIONAR ESTA LINHA
            print(f"      PC avançado: {process_id} agora em PC={pcb.pc}")
            
            # Desbloqueia processo
            self.gp.unblock_process(process_id)
            print(f"      Processo {process_id} desbloqueado")
            
        # Limpa interrupção
        self.cpu.irpt_io_complete = None
```

---

## 7. FLUXO DE ESCALONAMENTO

### 7.1. Loop do Escalonador (CPU Thread)

```python
# CPUThread.run() - Linha ~467-504

CPU THREAD - Loop Infinito
─────────────────────────────────────────────────────

while running:
    
    ┌────────────────────────────────────────┐
    │ CICLO DE ESCALONAMENTO                 │
    ├────────────────────────────────────────┤
    │                                        │
    │ 1. AGUARDA SINAL                       │
    │    └─ semaphore.acquire()              │
    │       └─ Bloqueia até release()        │
    │                                        │
    │ 2. BUSCA PRÓXIMO PROCESSO              │
    │    └─ pcb = gp.get_next_ready()        │
    │       ├─ Se ready_queue vazia: None    │
    │       └─ Senão: pop primeiro da fila   │
    │                                        │
    │ 3. EXECUTA PROCESSO                    │
    │    └─ if pcb:                          │
    │       ├─ cpu.set_context(pcb)          │
    │       ├─ cpu.run(quantum)              │
    │       │                                 │
    │       └─ Após run():                   │
    │          ├─ Se FINISHED: desaloca      │
    │          ├─ Se READY: add_ready        │
    │          └─ Se BLOCKED: mantém na fila │
    │                                        │
    │ 4. LIBERA SINAL                        │
    │    └─ semaphore.release()              │
    │                                        │
    │ 5. PAUSA                                │
    │    └─ time.sleep(0.01)                 │
    │       └─ Evita busy-wait               │
    │                                        │
    └────────────────────────────────────────┘
```

### 7.2. Round-Robin em Ação

```
EXEMPLO: 3 Processos na Fila
──────────────────────────────────────────────────────

ready_queue = [P1, P2, P3]  (todos PC=0, READY)

┌─────────────────────────────────────────────────────┐
│ CICLO 1                                             │
├─────────────────────────────────────────────────────┤
│ 1. get_next_ready() → P1                            │
│ 2. Executa P1 por quantum=5 instruções              │
│ 3. Quantum esgotado, P1 state=READY                 │
│ 4. add_ready(P1)                                    │
│    └─ ready_queue = [P2, P3, P1]                    │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ CICLO 2                                             │
├─────────────────────────────────────────────────────┤
│ 1. get_next_ready() → P2                            │
│ 2. Executa P2 por quantum=5 instruções              │
│ 3. P2 faz SYSCALL no meio, state=BLOCKED            │
│    └─ blocked_queue = [P2]                          │
│    └─ ready_queue = [P3, P1]                        │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ CICLO 3                                             │
├─────────────────────────────────────────────────────┤
│ 1. get_next_ready() → P3                            │
│ 2. Executa P3 por quantum=5 instruções              │
│ 3. Quantum esgotado, P3 state=READY                 │
│ 4. add_ready(P3)                                    │
│    └─ ready_queue = [P1, P3]                        │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│ CICLO 4                                             │
├─────────────────────────────────────────────────────┤
│ 1. get_next_ready() → P1                            │
│ 2. Executa P1 por quantum=5 instruções              │
│ 3. Quantum esgotado, P1 state=READY                 │
│ 4. add_ready(P1)                                    │
│    └─ ready_queue = [P3, P1]                        │
│                                                     │
│ [Meanwhile] I/O completo para P2!                   │
│    └─ unblock_process(P2)                           │
│       └─ ready_queue = [P3, P1, P2]                 │
└─────────────────────────────────────────────────────┘

... continua até todos FINISHED ...
```

---

## 8. DIAGRAMAS DE SEQUÊNCIA

### 8.1. Criação e Execução de Processo

```
Usuario     Shell      GerenteProc   CPU Thread    CPU
  │           │             │             │         │
  │ new prog  │             │             │         │
  ├──────────>│             │             │         │
  │           │ cria_proc() │             │         │
  │           ├────────────>│             │         │
  │           │             │ aloca mem   │         │
  │           │             │ cria PCB    │         │
  │           │             │ add_ready   │         │
  │           │<────────────┤             │         │
  │           │             │             │         │
  │<──────────┤             │             │         │
  │           │             │  get_next() │         │
  │           │             │<────────────┤         │
  │           │             │ PCB         │         │
  │           │             ├────────────>│         │
  │           │             │             │set_ctx  │
  │           │             │             ├────────>│
  │           │             │             │  run()  │
  │           │             │             ├────────>│
  │           │             │             │ executa │
  │           │             │             │<────────┤
  │           │             │             │         │
```

### 8.2. Fluxo de I/O (BUGADO vs CORRETO)

**BUGADO (Estado Atual):**

```
CPU        SysCall    IODevice    Handler    GerenteProc
 │            │          │           │             │
 │ SYSCALL   │          │           │             │
 ├──────────>│          │           │             │
 │           │ block    │           │             │
 │           ├──────────┼───────────┼────────────>│
 │           │ enqueue  │           │             │
 │           ├─────────>│           │             │
 │           │          │ sleep(2s) │             │
 │           │          │ processo  │             │
 │           │          │ irpt=PID  │             │
 │           │          ├──────────>│             │
 │           │          │           │ unblock     │
 │           │          │           ├────────────>│
 │<──────────┤          │           │             │
 │ PC=16 ❌  │          │           │             │
 │ SYSCALL   │          │           │             │
 ├──────────>│          │           │             │
 │ LOOP! 🔄  │          │           │             │
```

**CORRETO (Com Fix):**

```
CPU        SysCall    IODevice    Handler    GerenteProc
 │            │          │           │             │
 │ SYSCALL   │          │           │             │
 ├──────────>│          │           │             │
 │           │ block    │           │             │
 │           ├──────────┼───────────┼────────────>│
 │           │ enqueue  │           │             │
 │           ├─────────>│           │             │
 │           │          │ sleep(2s) │             │
 │           │          │ processo  │             │
 │           │          │ irpt=PID  │             │
 │           │          ├──────────>│             │
 │           │          │           │ pc++ ✅     │
 │           │          │           │ unblock     │
 │           │          │           ├────────────>│
 │<──────────┤          │           │             │
 │ PC=17 ✅  │          │           │             │
 │ STOP      │          │           │             │
 │ Sucesso!  │          │           │             │
```

---

## RESUMO DOS CAMINHOS

### Caminho Normal (Sem I/O)

```
Usuario
   ↓
Shell (new prog)
   ↓
GerenteProcessos (cria_processo)
   ↓
ready_queue.append(PCB)
   ↓
CPU Thread detecta
   ↓
get_next_ready() → PCB
   ↓
cpu.set_context(PCB)
   ↓
cpu.run(quantum) → Loop de instruções
   ↓
Quantum esgotado ou STOP
   ↓
Se READY: volta para ready_queue
Se FINISHED: desaloca
```

### Caminho com I/O (BUGADO)

```
Usuario
   ↓
Shell (new prog)
   ↓
[... mesmos passos até cpu.run() ...]
   ↓
CPU executa SYSCALL (PC=16)
   ↓
SysCall.handle() → bloqueia (PC=16 ❌)
   ↓
blocked_queue.append(PCB)
   ↓
IODevice processa (2s)
   ↓
IODevice gera interrupção
   ↓
Handler.handle_io_complete()
   ↓
unblock_process() (PC=16 ❌)
   ↓
ready_queue.append(PCB)
   ↓
CPU escalona novamente
   ↓
cpu.run() executa PC=16
   ↓
SYSCALL DE NOVO! 🔄
   ↓
LOOP INFINITO ❌
```

### Caminho com I/O (CORRETO)

```
Usuario
   ↓
Shell (new prog)
   ↓
[... mesmos passos até cpu.run() ...]
   ↓
CPU executa SYSCALL (PC=16)
   ↓
SysCall.handle() → bloqueia (PC=16)
   ↓
blocked_queue.append(PCB)
   ↓
IODevice processa (2s)
   ↓
IODevice gera interrupção
   ↓
Handler.handle_io_complete()
   ↓
✅ pcb.pc += 1  (PC=17)
   ↓
unblock_process() (PC=17 ✅)
   ↓
ready_queue.append(PCB)
   ↓
CPU escalona novamente
   ↓
cpu.run() executa PC=17
   ↓
STOP ✅
   ↓
Processo termina com sucesso!
```

---

## CONCLUSÃO

### Estado Atual do Implement3

✅ **Arquitetura Correta:**
- 3 threads concorrentes (Shell, CPU, I/O)
- Filas de estados (ready, blocked)
- I/O assíncrono com Queue
- Escalonamento Round-Robin

❌ **Bug Crítico:**
- PC não avança após I/O
- Causa loop infinito em qualquer programa com SYSCALL

### Correção Necessária

**Arquivo:** `sistema_os.py`  
**Linha:** ~545 (dentro de `handle_io_complete`)  
**Adição:** `pcb.pc += 1`

**Resultado:** Sistema 100% funcional para T2a ✅

---

**Autor:** GitHub Copilot Code Review Agent  
**Data:** 2025-11-10  
**Documento:** Caminho de Execução - Implement3
