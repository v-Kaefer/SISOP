import math
from enum import Enum
import threading #parteT2
import time #parteT2
from queue import Queue, Empty #parteT2
import sys #parteT2

# PUCRS - Escola Politécnica - Sistemas Operacionais
# Prof. Fernando Dotti
# Código fornecido como parte da solução do projeto de Sistemas Operacionais
#
# Estrutura deste código (versão python):
#     - Definições de Hardware (HW): Memory, Word, CPU, Opcode, Interrupts.
#     - Definições de Software (SO):
#         - PCB, Gerenciadores de Memória e Processos, Escalonador.
#         - Rotinas de tratamento de Interrupção e System Call.
#     - Programas exemplo (classe Programs).
#     - Sistema: classe que monta o sistema e oferece uma CLI para interação.

# -------------------------------------------------------------------------------------------------------
# --------------------- H A R D W A R E - Definições de HW
# -------------------------------------------------------------------------------------------------------

class Opcode(Enum):
    DATA, ___, JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, JMPIGK, JMPILK, JMPIEK, JMPIGT, ADDI, SUBI, ADD, SUB, MULT, LDI, LDD, STD, LDX, STX, MOVE, SYSCALL, STOP = range(28)

class Interrupts(Enum):
    NO_INTERRUPT, INT_ENDERECO_INVALIDO, INT_INSTRUCAO_INVALIDA, INT_OVERFLOW, INT_IO_COMPLETE = range(5)

class Word:
    def __init__(self, opc, ra, rb, p):
        self.opc = opc
        self.ra = ra
        self.rb = rb
        self.p = p

class Memory:
    def __init__(self, size):
        self.pos = [Word(Opcode.___, -1, -1, -1) for _ in range(size)]

#parteT2 - atualizado
class CPU:
    def __init__(self, mem, debug=False):
        self.max_int, self.min_int = 32767, -32767
        self.m = mem.pos
        self.reg = [0] * 10
        self.debug = debug
        self.pc = 0
        self.ir = None
        self.irpt = Interrupts.NO_INTERRUPT
        self.running_process = None
        self.ih, self.sys_call, self.gm, self.u = None, None, None, None
        self.cpu_stop = False
        self.instructions_executed = 0
        self.irpt_io_complete = None  # NOVO: sinalização de I/O completo

    def set_address_of_handlers(self, ih, sys_call):
        self.ih, self.sys_call = ih, sys_call

    def set_utilities(self, u):
        self.u = u

    def set_gerente_memoria(self, gm):
        self.gm = gm

    def set_context(self, pcb):
        self.running_process = pcb
        self.pc = pcb.pc
        self.reg = pcb.registers.copy()
        self.irpt = Interrupts.NO_INTERRUPT
        self.instructions_executed = 0
        pcb.state = PCB.ProcessState.RUNNING

    def _legal(self, e):
        if 0 <= e < len(self.m):
            return True
        self.irpt = Interrupts.INT_ENDERECO_INVALIDO
        return False

    def _translate_address(self, logical_address):
        if not self.running_process:
            self.irpt = Interrupts.INT_ENDERECO_INVALIDO
            return -1
        tam_pg = self.gm.get_tam_pg()
        page = logical_address // tam_pg
        offset = logical_address % tam_pg

        if not (0 <= page < len(self.running_process.page_table)):
            self.irpt = Interrupts.INT_ENDERECO_INVALIDO
            return -1
        
        frame = self.running_process.page_table[page]
        return (frame * tam_pg) + offset

    def _test_overflow(self, v):
        if not (self.min_int <= v <= self.max_int):
            self.irpt = Interrupts.INT_OVERFLOW
            return False
        return True

    def run(self, quantum):
        if not self.running_process: 
            return

        self.cpu_stop = False
        while not self.cpu_stop and (self.instructions_executed < quantum or quantum == -1):
            # Verifica se houve interrupção de I/O completo
            if self.irpt_io_complete is not None:
                print(f"      INTERRUPÇÃO: I/O completo para processo {self.irpt_io_complete}")
                self.irpt_io_complete = None
                # A interrupção de I/O não para o processo atual
                # O processo desbloqueado já foi movido para ready_queue pelo dispositivo
            
            physical_pc = self._translate_address(self.pc)
            if not self._legal(physical_pc): 
                break
            
            self.ir = self.m[physical_pc]
            if self.debug:
                print(f"    PC: {self.pc} -> INSTR: ", end="")
                self.u.dump(self.ir)
            
            opc, ra, rb, p = self.ir.opc, self.ir.ra, self.ir.rb, self.ir.p
            
            if opc == Opcode.LDI: 
                self.reg[ra] = p
                self.pc += 1
            
            elif opc == Opcode.LDD:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.reg[ra] = self.m[addr].p
                    self.pc += 1
            
            elif opc == Opcode.LDX:
                addr = self._translate_address(self.reg[rb])
                if self._legal(addr): 
                    self.reg[ra] = self.m[addr].p
                    self.pc += 1
            
            elif opc == Opcode.STD:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.m[addr] = Word(Opcode.DATA, -1, -1, self.reg[ra])
                    self.pc += 1
            
            elif opc == Opcode.STX:
                addr = self._translate_address(self.reg[ra])
                if self._legal(addr): 
                    self.m[addr] = Word(Opcode.DATA, -1, -1, self.reg[rb])
                    self.pc += 1
            
            elif opc == Opcode.MOVE: 
                self.reg[ra] = self.reg[rb]
                self.pc += 1
            
            elif opc == Opcode.ADD: 
                self.reg[ra] += self.reg[rb]
                self._test_overflow(self.reg[ra])
                self.pc += 1
            
            elif opc == Opcode.ADDI: 
                self.reg[ra] += p
                self._test_overflow(self.reg[ra])
                self.pc += 1
            
            elif opc == Opcode.SUB: 
                self.reg[ra] -= self.reg[rb]
                self._test_overflow(self.reg[ra])
                self.pc += 1
            
            elif opc == Opcode.SUBI: 
                self.reg[ra] -= p
                self._test_overflow(self.reg[ra])
                self.pc += 1
            
            elif opc == Opcode.MULT: 
                self.reg[ra] *= self.reg[rb]
                self._test_overflow(self.reg[ra])
                self.pc += 1
            
            elif opc == Opcode.JMP: 
                self.pc = p
            
            elif opc == Opcode.JMPIM:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.pc = self.m[addr].p
            
            elif opc == Opcode.JMPIG: 
                self.pc = self.reg[ra] if self.reg[rb] > 0 else self.pc + 1
            
            elif opc == Opcode.JMPIGK: 
                self.pc = p if self.reg[rb] > 0 else self.pc + 1
            
            elif opc == Opcode.JMPILK: 
                self.pc = p if self.reg[rb] < 0 else self.pc + 1
            
            elif opc == Opcode.JMPIEK: 
                self.pc = p if self.reg[rb] == 0 else self.pc + 1
            
            elif opc == Opcode.JMPIL: 
                self.pc = self.reg[ra] if self.reg[rb] < 0 else self.pc + 1
            
            elif opc == Opcode.JMPIE: 
                self.pc = self.reg[ra] if self.reg[rb] == 0 else self.pc + 1
            
            elif opc == Opcode.JMPIGM:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.pc = self.m[addr].p if self.reg[rb] > 0 else self.pc + 1
            
            elif opc == Opcode.JMPILM:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.pc = self.m[addr].p if self.reg[rb] < 0 else self.pc + 1
            
            elif opc == Opcode.JMPIEM:
                addr = self._translate_address(p)
                if self._legal(addr): 
                    self.pc = self.m[addr].p if self.reg[rb] == 0 else self.pc + 1
            
            elif opc == Opcode.JMPIGT: 
                self.pc = p if self.reg[ra] > self.reg[rb] else self.pc + 1
            
            elif opc == Opcode.SYSCALL: 
                # SYSCALL agora pode bloquear o processo (I/O)
                self.sys_call.handle()
                # Se o processo foi bloqueado, cpu_stop será True
                if not self.cpu_stop:
                    self.pc += 1
            
            elif opc == Opcode.STOP: 
                self.sys_call.stop()
                self.cpu_stop = True
            
            else: 
                self.irpt = Interrupts.INT_INSTRUCAO_INVALIDA

            # Trata interrupções de erro
            if self.irpt != Interrupts.NO_INTERRUPT: 
                self.ih.handle(self.irpt, self.pc)
                self.cpu_stop = True
            
            self.instructions_executed += 1
        
        # Salva contexto ao final da execução (se processo não finalizou e não foi bloqueado)
        if self.running_process:
            if self.running_process.state == PCB.ProcessState.RUNNING:
                # Processo ainda está rodando: acabou quantum
                self.running_process.state = PCB.ProcessState.READY
            
            # Salva PC e registradores (independente do estado)
            # Se bloqueado, salva para retomar depois
            # Se ready, salva para próxima vez que for escalonado
            if self.running_process.state != PCB.ProcessState.FINISHED:
                self.running_process.pc = self.pc
                self.running_process.registers = self.reg.copy()

#parteT2
class CPUThread(threading.Thread):
    """Thread da CPU - executa processos continuamente"""
    
    def __init__(self, cpu, escalonador, semaphore):
        super().__init__(daemon=True, name="CPU")
        self.cpu = cpu
        self.escalonador = escalonador
        self.semaphore = semaphore
        self.running = True
    
    def run(self):
        print("[CPU Thread] CPU iniciada e aguardando processos...")
        
        while self.running:
            # Aguarda sinalização de que há processo pronto
            self.semaphore.acquire()
            
            if not self.running:
                break
            
            # Pega próximo processo da fila de prontos
            pcb = self.escalonador.gp.get_next_ready()
            
            if pcb is None:
                # Fila vazia, volta a aguardar
                continue
            
            print(f"\n[CPU Thread] Executando processo {pcb.id} (Quantum: {self.escalonador.quantum})")
            
            # Configura contexto e executa
            self.cpu.set_context(pcb)
            self.cpu.run(self.escalonador.quantum)
            
            # Trata resultado da execução
            if pcb.state == PCB.ProcessState.FINISHED:
                print(f"[CPU Thread] Processo {pcb.id} FINALIZOU")
                self.escalonador.gp.desaloca_processo(pcb.id)
            
            elif pcb.state == PCB.ProcessState.BLOCKED:
                print(f"[CPU Thread] Processo {pcb.id} BLOQUEADO (aguardando I/O)")
                # Já está na fila de bloqueados (feito no syscall)
            
            elif pcb.state == PCB.ProcessState.READY:
                print(f"[CPU Thread] Processo {pcb.id} teve quantum expirado")
                self.escalonador.gp.add_ready(pcb)
    
    def stop(self):
        self.running = False
        self.semaphore.release()  # Libera se estiver bloqueado

class HW:
    def __init__(self, tam_mem):
        self.mem = Memory(tam_mem)
        self.cpu = CPU(self.mem, debug=False)

#parteT2
class IODevice(threading.Thread):
    """Thread do dispositivo de I/O - modelo produtor/consumidor"""
    
    def __init__(self, hw, gp, io_queue):
        super().__init__(daemon=True, name="IODevice")
        self.hw = hw
        self.gp = gp
        self.io_queue = io_queue
        self.running = True
        self.io_delay = 2.0  # Simula latência de 2 segundos
    
    def run(self):
        print("[I/O Device] Dispositivo iniciado e aguardando requisições...")
        
        while self.running:
            try:
                # Consumidor: aguarda requisição na fila
                request = self.io_queue.get(timeout=0.5)
                
                print(f"[I/O Device] Processando {request.operation} para processo {request.process_id}...")
                
                # Simula latência do dispositivo (dispositivos são lentos!)
                time.sleep(self.io_delay)
                
                # Processa a operação com acesso direto à memória (DMA)
                self._process_io(request)
                
                # Gera interrupção na CPU
                self.hw.cpu.irpt_io_complete = request.process_id
                
                # Desbloqueia o processo (move de blocked para ready)
                self.gp.unblock_process(request.process_id)
                
                print(f"[I/O Device] {request.operation} concluído para processo {request.process_id}")
                
            except Empty:
                # Timeout: nenhuma requisição pendente
                continue
            except Exception as e:
                print(f"[I/O Device] Erro ao processar I/O: {e}")
    
    def _process_io(self, request):
        """Processa operação de I/O com acesso DMA"""
        # Traduz endereço lógico para físico
        tam_pg = self.gp.gm.get_tam_pg()
        page = request.address // tam_pg
        offset = request.address % tam_pg
        
        if page >= len(request.pcb.page_table):
            print(f"[I/O Device] ERRO: Endereço inválido {request.address}")
            return
        
        frame = request.pcb.page_table[page]
        physical_addr = (frame * tam_pg) + offset
        
        if request.operation == 'READ':
            # Leitura: pede input e escreve na memória (DMA)
            print(f"\n{'='*50}")
            print(f"[I/O Device] ENTRADA necessária para processo {request.process_id}")
            try:
                value = int(input(f"Digite um valor inteiro: "))
                self.hw.mem.pos[physical_addr] = Word(Opcode.DATA, -1, -1, value)
                print(f"[I/O Device] Valor {value} escrito no endereço físico {physical_addr}")
            except ValueError:
                print("[I/O Device] Valor inválido! Usando 0.")
                self.hw.mem.pos[physical_addr] = Word(Opcode.DATA, -1, -1, 0)
            print(f"{'='*50}\n")
        
        elif request.operation == 'WRITE':
            # Escrita: lê da memória e imprime (DMA)
            value = self.hw.mem.pos[physical_addr].p
            print(f"\n{'='*50}")
            print(f"[I/O Device] SAÍDA do processo {request.process_id}: {value}")
            print(f"{'='*50}\n")
    
    def stop(self):
        self.running = False


# -------------------------------------------------------------------------------------------------------
# --------------------- SW - Sistema Operacional
# -------------------------------------------------------------------------------------------------------

class PCB:
    _processo_count = 0  # Contador separado para estatísticas
    
    class ProcessState(Enum): 
        READY, RUNNING, BLOCKED, FINISHED = range(4)
        
    def __init__(self, page_table):
        # ID agora é o frame inicial onde o processo foi alocado
        self.id = page_table[0] if page_table else 0
        
        # Incrementar contador de processos criados (para estatísticas)
        PCB._processo_count += 1
        self.processo_number = PCB._processo_count
        
        self.pc = 0
        self.registers = [0] * 10
        self.page_table = page_table
        self.state = PCB.ProcessState.READY
        self.waiting_io = None  # Guarda info da operação I/O esperada - parteT2
    
    @classmethod
    def get_processo_count(cls):
        """Retorna o número total de processos criados"""
        return cls._processo_count
    
    @classmethod
    def reset_count(cls):
        """Reseta o contador (útil para testes)"""
        cls._processo_count = 0

class GerenteMemoria:
    def __init__(self, tam_mem, tam_pg):
        self.tam_mem, self.tam_pg = tam_mem, tam_pg
        self.num_frames = tam_mem // tam_pg
        self.free_frames = [True] * self.num_frames
        # Mapa para rastrear qual processo está em cada frame
        self.frame_to_process = [-1] * self.num_frames

    def aloca(self, num_palavras, frame_inicial=None):
        """
        Aloca memória para um programa
        
        Args:
            num_palavras: Número de palavras do programa
            frame_inicial: Frame específico onde começar (opcional)
        
        Returns:
            Lista com os frames alocados ou None se não conseguir alocar
        """
        paginas_necessarias = math.ceil(num_palavras / self.tam_pg) if num_palavras > 0 else 0
        
        if frame_inicial is not None:
            # Alocação em frame específico
            return self._aloca_especifico(paginas_necessarias, frame_inicial)
        else:
            # Alocação automática (código original)
            return self._aloca_automatico(paginas_necessarias)

    def _aloca_especifico(self, paginas_necessarias, frame_inicial):
        """Aloca a partir de um frame específico"""
        # Verificar se o frame inicial é válido
        if not (0 <= frame_inicial < self.num_frames):
            print(f"Erro: Frame {frame_inicial} inválido (0-{self.num_frames-1})")
            return None
        
        # Verificar se há frames suficientes consecutivos a partir do frame inicial
        if frame_inicial + paginas_necessarias > self.num_frames:
            print(f"Erro: Não há {paginas_necessarias} frames consecutivos a partir do frame {frame_inicial}")
            return None
        
        # Verificar se os frames estão livres
        for i in range(frame_inicial, frame_inicial + paginas_necessarias):
            if not self.free_frames[i]:
                print(f"Erro: Frame {i} já está ocupado pelo processo ID {self.frame_to_process[i]}")
                return None
        
        # Alocar os frames
        tabela_paginas = []
        for i in range(frame_inicial, frame_inicial + paginas_necessarias):
            self.free_frames[i] = False
            tabela_paginas.append(i)
            # Marcar qual processo vai ocupar este frame (será definido depois)
            self.frame_to_process[i] = frame_inicial  # ID será o frame inicial
        
        print(f"Alocado nos frames consecutivos: {tabela_paginas}")
        return tabela_paginas

    def _aloca_automatico(self, paginas_necessarias):
        """Alocação automática - busca pelo primeiro conjunto de frames livres consecutivos"""
        # Buscar frames consecutivos livres
        for start_frame in range(self.num_frames - paginas_necessarias + 1):
            frames_disponiveis = True
            for i in range(start_frame, start_frame + paginas_necessarias):
                if not self.free_frames[i]:
                    frames_disponiveis = False
                    break
            
            if frames_disponiveis:
                # Alocar os frames consecutivos
                tabela_paginas = []
                for i in range(start_frame, start_frame + paginas_necessarias):
                    self.free_frames[i] = False
                    tabela_paginas.append(i)
                    self.frame_to_process[i] = start_frame  # ID será o frame inicial
                
                print(f"Alocado automaticamente nos frames: {tabela_paginas}")
                return tabela_paginas
        
        # Não encontrou frames consecutivos suficientes
        return None
    
    def desaloca(self, tabela_paginas):
        """Desaloca frames e limpa o mapeamento"""
        if tabela_paginas:
            for frame in tabela_paginas:
                if 0 <= frame < self.num_frames: 
                    self.free_frames[frame] = True
                    self.frame_to_process[frame] = -1

    def get_tam_pg(self): 
        return self.tam_pg
    
    def mostrar_status(self):
        """Mostra o status atual da memória"""
        print(f"=== STATUS DA MEMÓRIA ===")
        print(f"Total de frames: {self.num_frames}")
        print(f"Frames livres: {self.free_frames.count(True)}")
        print(f"Frames ocupados: {self.free_frames.count(False)}")
        
        print(f"\nMapa de ocupação:")
        for i in range(0, self.num_frames, 16):  # Mostrar 16 frames por linha
            linha_status = []
            linha_processos = []
            for j in range(i, min(i + 16, self.num_frames)):
                if self.free_frames[j]:
                    linha_status.append(".")
                    linha_processos.append("  ")
                else:
                    linha_status.append("X")
                    linha_processos.append(f"{self.frame_to_process[j]:2d}")
            
            print(f"Frames {i:2d}-{min(i+15, self.num_frames-1):2d}: {''.join(linha_status)}")
            print(f"Proc. IDs: {''.join(linha_processos)}")
        print("=========================")

#parteT2
class GerenteProcessos:
    def __init__(self, gm, hw, utils):
        self.gm = gm
        self.hw = hw
        self.utils = utils
        self.ready_queue = []
        self.blocked_queue = []  # NOVA: processos bloqueados por I/O
        self.all_processes = []
        self.lock = threading.Lock()  # Proteção para acesso concorrente
    
    def get_next_ready(self):
        with self.lock:
            return self.ready_queue.pop(0) if self.ready_queue else None
    
    def add_ready(self, pcb):
        with self.lock:
            if pcb not in self.ready_queue:
                pcb.state = PCB.ProcessState.READY
                self.ready_queue.append(pcb)
    
    def block_process(self, pcb):
        """Move processo para fila de bloqueados"""
        with self.lock:
            pcb.state = PCB.ProcessState.BLOCKED
            if pcb in self.ready_queue:
                self.ready_queue.remove(pcb)
            if pcb not in self.blocked_queue:
                self.blocked_queue.append(pcb)
    
    def unblock_process(self, proc_id):
        """Desbloqueia processo e move para ready"""
        with self.lock:
            for pcb in self.blocked_queue:
                if pcb.id == proc_id:
                    self.blocked_queue.remove(pcb)
                    self.add_ready(pcb)
                    print(f"[GP] Processo {proc_id} desbloqueado e pronto para executar")
                    return True
            return False

#parteT2 - atualizado
class Escalonador:
    """Escalonador que gerencia a thread da CPU"""
    
    def __init__(self, cpu, gp, quantum):
        self.cpu = cpu
        self.gp = gp
        self.quantum = quantum
        self.semaphore = threading.Semaphore(0)
        self.cpu_thread = None
        self.running = False
        self.scheduler_thread = None
    
    def start(self):
        """Inicia a thread da CPU e o loop de escalonamento"""
        if self.running:
            print("Escalonador já está rodando!")
            return
        
        self.running = True
        
        # Inicia thread da CPU
        self.cpu_thread = CPUThread(self.cpu, self, self.semaphore)
        self.cpu_thread.start()
        
        # Inicia thread do escalonador
        self.scheduler_thread = threading.Thread(
            target=self._scheduler_loop, 
            daemon=True, 
            name="Scheduler"
        )
        self.scheduler_thread.start()
        
        print("[Escalonador] Sistema de escalonamento iniciado!")
    
    def _scheduler_loop(self):
        """Loop do escalonador: verifica fila e libera CPU"""
        while self.running:
            # Se há processos prontos, libera CPU para executar
            if self.gp.ready_queue:
                self.semaphore.release()
            
            # Tick do escalonador (10ms)
            time.sleep(0.01)
    
    def stop(self):
        """Para o escalonador e a CPU"""
        self.running = False
        if self.cpu_thread:
            self.cpu_thread.stop()
        print("[Escalonador] Sistema de escalonamento encerrado!")

# Estrutura de requisição I/O - parteT2
class IORequest:
    """Requisição de I/O"""
    def __init__(self, process_id, operation, address, pcb):
        self.process_id = process_id
        self.operation = operation  # 'READ' ou 'WRITE'
        self.address = address      # Endereço lógico
        self.pcb = pcb
        self.timestamp = time.time()

class InterruptHandling:
    def __init__(self, cpu):
        self.cpu = cpu

    def handle(self, irpt, pc):
        print(f"      INTERRUPCAO {irpt.name} (PC Logico: {pc})")
        if irpt in [Interrupts.INT_ENDERECO_INVALIDO, Interrupts.INT_INSTRUCAO_INVALIDA, Interrupts.INT_OVERFLOW]:
            if self.cpu.running_process:
                self.cpu.running_process.state = PCB.ProcessState.FINISHED

#parteT2 - atualizado
class SysCallHandling:
    def __init__(self, hw, io_queue, gp):
        self.hw = hw
        self.io_queue = io_queue
        self.gp = gp
    
    def handle(self):
        syscall_id = self.hw.cpu.reg[8]
        
        if syscall_id == 1:  # READ
            self._handle_read()
        elif syscall_id == 2:  # WRITE
            self._handle_write()
    
    def _handle_read(self):
        """Syscall READ: requisita leitura e bloqueia processo"""
        address = self.hw.cpu.reg[9]
        pcb = self.hw.cpu.running_process
        
        print(f"      SYSCALL: READ no endereço lógico {address} (Processo {pcb.id})")
        
        # Cria requisição de I/O
        request = IORequest(pcb.id, 'READ', address, pcb)
        
        # Produtor: coloca requisição na fila do dispositivo
        self.io_queue.put(request)
        
        # Bloqueia o processo (não pode continuar sem o dado)
        self.gp.block_process(pcb)
        
        # Para execução na CPU
        self.hw.cpu.cpu_stop = True
        
        print(f"      Processo {pcb.id} BLOQUEADO aguardando I/O")
    
    def _handle_write(self):
        """Syscall WRITE: requisita escrita e bloqueia processo"""
        address = self.hw.cpu.reg[9]
        pcb = self.hw.cpu.running_process
        
        print(f"      SYSCALL: WRITE do endereço lógico {address} (Processo {pcb.id})")
        
        # Cria requisição de I/O
        request = IORequest(pcb.id, 'WRITE', address, pcb)
        
        # Produtor: coloca requisição na fila
        self.io_queue.put(request)
        
        # Bloqueia o processo
        self.gp.block_process(pcb)
        
        # Para execução na CPU
        self.hw.cpu.cpu_stop = True
        
        print(f"      Processo {pcb.id} BLOQUEADO aguardando I/O")
    
    def stop(self):
        print("      SYSCALL: STOP")
        if self.hw.cpu.running_process:
            self.hw.cpu.running_process.state = PCB.ProcessState.FINISHED

class Utilities:
    def __init__(self):
        self.hw = None
        
    def dump(self, w): print(f"[ {w.opc.name:10s}, R1:{w.ra:2d}, R2:{w.rb:2d}, P:{w.p:4d} ]")

    def dump_memory_range(self, start, end):
        print(f"\n--- DUMP Memoria Fisica (de {start} a {end}) ---")
        for i in range(start, end + 1):
            if 0 <= i < len(self.hw.mem.pos):
                print(f"Fis.{i:04d}: ", end=""); self.dump(self.hw.mem.pos[i])
        print("--- FIM DUMP ---")

#parteT2 - atualizado
class SO:
    def __init__(self, hw, tam_pg, quantum):
        self.hw = hw
        self.tam_pg = tam_pg
        self.quantum = quantum
        
        # Estruturas do SO
        self.ih = InterruptHandling(hw.cpu)
        self.utils = Utilities()
        self.utils.hw = hw
        self.gm = GerenteMemoria(len(hw.mem.pos), tam_pg)
        self.gp = GerenteProcessos(self.gm, hw, self.utils)
        
        # Fila de I/O (produtor/consumidor)
        self.io_queue = Queue()
        
        # Handlers com acesso à fila de I/O
        self.sc = SysCallHandling(hw, self.io_queue, self.gp)
        
        # Escalonador
        self.escalonador = Escalonador(hw.cpu, self.gp, quantum)
        
        # Dispositivo de I/O
        self.io_device = IODevice(hw, self.gp, self.io_queue)
        
        # Configurações
        hw.cpu.set_address_of_handlers(self.ih, self.sc)
        hw.cpu.set_gerente_memoria(self.gm)
        hw.cpu.set_utilities(self.utils)

class Program:
    def __init__(self, name, image): self.name, self.image = name, image

class Programs:
    def __init__(self):
        self.progs = [
            # Este fatorial so aceita valores positivos. nao pode ser zero
            Program("fatorial", [
                Word(Opcode.LDI, 0, -1, 7),      # 0: r0 é valor a calcular fatorial
                Word(Opcode.LDI, 1, -1, 1),      # 1: r1 é 1 para multiplicar (por r0)
                Word(Opcode.LDI, 6, -1, 1),      # 2: r6 é 1 o decremento
                Word(Opcode.LDI, 7, -1, 8),      # 3: r7 tem posicao 8 para fim do programa
                Word(Opcode.JMPIE, 7, 0, 0),     # 4: se r0=0 pula para r7(=8)
                Word(Opcode.MULT, 1, 0, -1),     # 5: r1 = r1 * r0
                Word(Opcode.SUB, 0, 6, -1),      # 6: r0 = r0 - 1
                Word(Opcode.JMP, -1, -1, 4),     # 7: vai p posicao 4
                Word(Opcode.STD, 1, -1, 10),     # 8: coloca valor de r1 na posição 10
                Word(Opcode.STOP, -1, -1, -1),   # 9: stop
                Word(Opcode.DATA, -1, -1, -1)    # 10: ao final o valor está na posição 10
            ]),

            Program("fatorialV2", [
                Word(Opcode.LDI, 0, -1, 5),      # numero para colocar na memoria, ou pode ser lido
                Word(Opcode.STD, 0, -1, 19),
                Word(Opcode.LDD, 0, -1, 19),
                Word(Opcode.LDI, 1, -1, -1),
                Word(Opcode.LDI, 2, -1, 13),     # SALVAR POS STOP
                Word(Opcode.JMPIL, 2, 0, -1),    # caso negativo pula pro STD
                Word(Opcode.LDI, 1, -1, 1),
                Word(Opcode.LDI, 6, -1, 1),
                Word(Opcode.LDI, 7, -1, 13),
                Word(Opcode.JMPIE, 7, 0, 0),     # POS 9 pula para STD (Stop-1)
                Word(Opcode.MULT, 1, 0, -1),
                Word(Opcode.SUB, 0, 6, -1),
                Word(Opcode.JMP, -1, -1, 9),     # pula para o JMPIE
                Word(Opcode.STD, 1, -1, 18),
                Word(Opcode.LDI, 8, -1, 2),      # escrita
                Word(Opcode.LDI, 9, -1, 18),     # endereco com valor a escrever
                Word(Opcode.SYSCALL, -1, -1, -1),
                Word(Opcode.STOP, -1, -1, -1),   # POS 17
                Word(Opcode.DATA, -1, -1, -1),   # POS 18
                Word(Opcode.DATA, -1, -1, -1)    # POS 19
            ]),

            Program("progMinimo", [
                Word(Opcode.LDI, 0, -1, 999),
                Word(Opcode.STD, 0, -1, 8),
                Word(Opcode.STD, 0, -1, 9),
                Word(Opcode.STD, 0, -1, 10),
                Word(Opcode.STD, 0, -1, 11),
                Word(Opcode.STD, 0, -1, 12),
                Word(Opcode.STOP, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),   # 7
                Word(Opcode.DATA, -1, -1, -1),   # 8
                Word(Opcode.DATA, -1, -1, -1),   # 9
                Word(Opcode.DATA, -1, -1, -1),   # 10
                Word(Opcode.DATA, -1, -1, -1),   # 11
                Word(Opcode.DATA, -1, -1, -1),   # 12
                Word(Opcode.DATA, -1, -1, -1)    # 13
            ]),

            Program("fibonacci10", [
                Word(Opcode.LDI, 1, -1, 0),
                Word(Opcode.STD, 1, -1, 20),
                Word(Opcode.LDI, 2, -1, 1),
                Word(Opcode.STD, 2, -1, 21),
                Word(Opcode.LDI, 0, -1, 22),
                Word(Opcode.LDI, 6, -1, 6),
                Word(Opcode.LDI, 7, -1, 31),
                Word(Opcode.LDI, 3, -1, 0),
                Word(Opcode.ADD, 3, 1, -1),
                Word(Opcode.LDI, 1, -1, 0),
                Word(Opcode.ADD, 1, 2, -1),
                Word(Opcode.ADD, 2, 3, -1),
                Word(Opcode.STX, 0, 2, -1),
                Word(Opcode.ADDI, 0, -1, 1),
                Word(Opcode.SUB, 7, 0, -1),
                Word(Opcode.JMPIG, 6, 7, -1),
                Word(Opcode.STOP, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),   # POS 20
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1)
            ]),

            Program("fibonacci10v2", [
                Word(Opcode.LDI, 1, -1, 0),
                Word(Opcode.STD, 1, -1, 20),
                Word(Opcode.LDI, 2, -1, 1),
                Word(Opcode.STD, 2, -1, 21),
                Word(Opcode.LDI, 0, -1, 22),
                Word(Opcode.LDI, 6, -1, 6),
                Word(Opcode.LDI, 7, -1, 31),
                Word(Opcode.MOVE, 3, 1, -1),
                Word(Opcode.MOVE, 1, 2, -1),
                Word(Opcode.ADD, 2, 3, -1),
                Word(Opcode.STX, 0, 2, -1),
                Word(Opcode.ADDI, 0, -1, 1),
                Word(Opcode.SUB, 7, 0, -1),
                Word(Opcode.JMPIG, 6, 7, -1),
                Word(Opcode.STOP, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),   # POS 20
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1)
            ]),

            Program("fibonacciREAD", [
                Word(Opcode.LDI, 8, -1, 1),      # syscall de leitura
                Word(Opcode.LDI, 9, -1, 55),     # endereco para guardar o num lido
                Word(Opcode.SYSCALL, -1, -1, -1),
                Word(Opcode.LDD, 7, -1, 55),
                Word(Opcode.LDI, 3, -1, 0),
                Word(Opcode.ADD, 3, 7, -1),
                Word(Opcode.LDI, 4, -1, 36),     # pos do stop
                Word(Opcode.LDI, 1, -1, -1),     # caso negativo
                Word(Opcode.STD, 1, -1, 41),
                Word(Opcode.JMPIL, 4, 7, -1),    # pula pra stop caso negativo
                Word(Opcode.JMPIE, 4, 7, -1),    # pula pra stop caso 0
                Word(Opcode.ADDI, 7, -1, 41),
                Word(Opcode.LDI, 1, -1, 0),
                Word(Opcode.STD, 1, -1, 41),
                Word(Opcode.SUBI, 3, -1, 1),
                Word(Opcode.JMPIE, 4, 3, -1),
                Word(Opcode.ADDI, 3, -1, 1),
                Word(Opcode.LDI, 2, -1, 1),
                Word(Opcode.STD, 2, -1, 42),
                Word(Opcode.SUBI, 3, -1, 2),
                Word(Opcode.JMPIE, 4, 3, -1),
                Word(Opcode.LDI, 0, -1, 43),
                Word(Opcode.LDI, 6, -1, 25),     # salva pos de retorno do loop
                Word(Opcode.LDI, 5, -1, 0),
                Word(Opcode.ADD, 5, 7, -1),
                Word(Opcode.LDI, 7, -1, 0),
                Word(Opcode.ADD, 7, 5, -1),
                Word(Opcode.LDI, 3, -1, 0),
                Word(Opcode.ADD, 3, 1, -1),
                Word(Opcode.LDI, 1, -1, 0),
                Word(Opcode.ADD, 1, 2, -1),
                Word(Opcode.ADD, 2, 3, -1),
                Word(Opcode.STX, 0, 2, -1),
                Word(Opcode.ADDI, 0, -1, 1),
                Word(Opcode.SUB, 7, 0, -1),
                Word(Opcode.JMPIG, 6, 7, -1),    # volta para o inicio do loop
                Word(Opcode.STOP, -1, -1, -1),   # POS 36
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1), Word(Opcode.DATA, -1, -1, -1)
            ]),

            Program("PB", [
                # se num < 0, armazena -1; se > 0, armazena fatorial
                Word(Opcode.LDI, 0, -1, 7),
                Word(Opcode.STD, 0, -1, 50),
                Word(Opcode.LDD, 0, -1, 50),
                Word(Opcode.LDI, 1, -1, -1),
                Word(Opcode.LDI, 2, -1, 13),
                Word(Opcode.JMPIL, 2, 0, -1),
                Word(Opcode.LDI, 1, -1, 1),
                Word(Opcode.LDI, 6, -1, 1),
                Word(Opcode.LDI, 7, -1, 13),
                Word(Opcode.JMPIE, 7, 0, 0),
                Word(Opcode.MULT, 1, 0, -1),
                Word(Opcode.SUB, 0, 6, -1),
                Word(Opcode.JMP, -1, -1, 9),
                Word(Opcode.STD, 1, -1, 15),
                Word(Opcode.STOP, -1, -1, -1),
                Word(Opcode.DATA, -1, -1, -1)
            ]),

            Program("PC", [ # Bubble Sort
                # NOTA: Este programa usa endereços de memória altos (96-99) para armazenar ponteiros de salto.
                # Para garantir que esses endereços lógicos sejam válidos em um sistema paginado,
                # o programa é preenchido com instruções DATA para que seu tamanho total seja de 100 palavras.
                # Isso força o Gerente de Memória a alocar páginas suficientes (ceil(100/16) = 7 páginas),
                # tornando o endereço 99 válido.
                Word(Opcode.LDI, 7, -1, 5),      # N
                Word(Opcode.LDI, 6, -1, 5),      # aux N
                Word(Opcode.LDI, 5, -1, 46),     # end. memoria
                Word(Opcode.LDI, 4, -1, 47),     # aux end. memoria
                Word(Opcode.LDI, 0, -1, 4),      # colocando valores na memoria
                Word(Opcode.STD, 0, -1, 46),
                Word(Opcode.LDI, 0, -1, 3),
                Word(Opcode.STD, 0, -1, 47),
                Word(Opcode.LDI, 0, -1, 5),
                Word(Opcode.STD, 0, -1, 48),
                Word(Opcode.LDI, 0, -1, 1),
                Word(Opcode.STD, 0, -1, 49),
                Word(Opcode.LDI, 0, -1, 2),
                Word(Opcode.STD, 0, -1, 50),     # fim da carga de valores
                Word(Opcode.LDI, 3, -1, 25),
                Word(Opcode.STD, 3, -1, 99),     # <--- Acesso ao endereço 99
                Word(Opcode.LDI, 3, -1, 22),
                Word(Opcode.STD, 3, -1, 98),
                Word(Opcode.LDI, 3, -1, 38),
                Word(Opcode.STD, 3, -1, 97),
                Word(Opcode.LDI, 3, -1, 25),
                Word(Opcode.STD, 3, -1, 96),
                Word(Opcode.LDI, 6, -1, 0),      # r6 = r7 - 1
                Word(Opcode.ADD, 6, 7, -1),
                Word(Opcode.SUBI, 6, -1, 1),
                Word(Opcode.JMPIEM, -1, 6, 97),  # se r6=0, fim
                Word(Opcode.LDX, 0, 5, -1),
                Word(Opcode.LDX, 1, 4, -1),
                Word(Opcode.LDI, 2, -1, 0),
                Word(Opcode.ADD, 2, 0, -1),
                Word(Opcode.SUB, 2, 1, -1),
                Word(Opcode.ADDI, 4, -1, 1),
                Word(Opcode.SUBI, 6, -1, 1),
                Word(Opcode.JMPILM, -1, 2, 99),  # se r0 < r1, nao troca
                Word(Opcode.STX, 5, 1, -1),      # troca
                Word(Opcode.SUBI, 4, -1, 1),
                Word(Opcode.STX, 4, 0, -1),
                Word(Opcode.ADDI, 4, -1, 1),
                Word(Opcode.JMPIGM, -1, 6, 99),  # loop interno
                Word(Opcode.ADDI, 5, -1, 1),
                Word(Opcode.SUBI, 7, -1, 1),
                Word(Opcode.LDI, 4, -1, 0),
                Word(Opcode.ADD, 4, 5, -1),
                Word(Opcode.ADDI, 4, -1, 1),
                Word(Opcode.JMPIGM, -1, 7, 98),  # loop externo
                Word(Opcode.STOP, -1, -1, -1),
                # Padding de dados para garantir que o espaço de endereço lógico seja grande o suficiente
                *([Word(Opcode.DATA, -1, -1, -1)] * 54)
            ])
        ]
    
    def retrieve_program(self, pname):
        for p in self.progs:
            if p.name.lower() == pname.lower(): return p.image
        return None

#parteT2 - atualizado
class Sistema:
    def __init__(self, tam_mem, tam_pg, quantum):
        self.hw = HW(tam_mem)
        self.so = SO(self.hw, tam_pg, quantum)
        self.progs = Programs()
    
    def run(self):
        print("=" * 60)
        print("SISTEMA OPERACIONAL CONCORRENTE")
        print("=" * 60)
        print("Comandos disponíveis:")
        print("  new <programa> [frame]  - Criar processo")
        print("  rm <id>                 - Remover processo")
        print("  ps                      - Listar processos")
        print("  dump <id>               - Dump do processo")
        print("  dumpm <ini> <fim>       - Dump memória")
        print("  memstat                 - Status memória")
        print("  stats                   - Estatísticas")
        print("  start                   - Iniciar escalonamento")
        print("  stop                    - Parar escalonamento")
        print("  exit                    - Sair")
        print("=" * 60)
        print("NOTA: Use 'start' para iniciar o sistema após criar processos")
        print("=" * 60)
        
        system_started = False
        
        while True:
            try:
                cmd_line = input(f"\n[Procs:{len(self.so.gp.all_processes)} Ready:{len(self.so.gp.ready_queue)} Blocked:{len(self.so.gp.blocked_queue)}] > ").strip().split()
                
                if not cmd_line:
                    continue
                
                cmd = cmd_line[0].lower()
                
                if cmd == "start":
                    if not system_started:
                        # Inicia threads (CPU e I/O Device)
                        self.so.escalonador.start()
                        self.so.io_device.start()
                        system_started = True
                        print("[Sistema] Sistema iniciado! Processos serão escalonados automaticamente.")
                    else:
                        print("[Sistema] Sistema já está rodando!")
                
                elif cmd == "stop":
                    if system_started:
                        self.so.escalonador.stop()
                        self.so.io_device.stop()
                        system_started = False
                        print("[Sistema] Sistema parado!")
                    else:
                        print("[Sistema] Sistema não está rodando!")
                
                elif cmd == "new":
                    # Criar processo (código existente)
                    if len(cmd_line) >= 2:
                        programa_nome = cmd_line[1]
                        programa = self.progs.retrieve_program(programa_nome)
                        
                        if programa is None:
                            print(f"Erro: Programa '{programa_nome}' não encontrado.")
                            print("Programas disponíveis:", [p.name for p in self.progs.progs])
                            continue
                        
                        frame_inicial = int(cmd_line[2]) if len(cmd_line) >= 3 else None
                        proc_id = self.so.gp.cria_processo(programa, frame_inicial)
                        
                        if proc_id != -1 and not system_started:
                            print("Dica: Use 'start' para iniciar o escalonamento!")
                    else:
                        print("Uso: new <programa> [frame]")
                        print("Programas:", [p.name for p in self.progs.progs])
                
                elif cmd == "ps":
                    self.so.gp.list_all_processes()
                
                elif cmd == "stats":
                    self.so.gp.estatisticas()
                    print(f"Requisições de I/O pendentes: {self.so.io_queue.qsize()}")
                
                elif cmd == "memstat":
                    self.so.gm.mostrar_status()
                
                elif cmd == "rm":
                    if len(cmd_line) > 1:
                        self.so.gp.desaloca_processo(int(cmd_line[1]))
                    else:
                        print("Uso: rm <id>")
                
                elif cmd == "dump":
                    if len(cmd_line) > 1:
                        self.so.gp.dump_processo(int(cmd_line[1]))
                    else:
                        print("Uso: dump <id>")
                
                elif cmd == "dumpm":
                    if len(cmd_line) > 2:
                        self.so.utils.dump_memory_range(int(cmd_line[1]), int(cmd_line[2]))
                    else:
                        print("Uso: dumpm <inicio> <fim>")
                
                elif cmd == "exit":
                    if system_started:
                        self.so.escalonador.stop()
                        self.so.io_device.stop()
                    break
                
                else:
                    print(f"Comando desconhecido: '{cmd}'")
                
            except (ValueError, IndexError) as e:
                print(f"Erro: {e}")
            except (EOFError, KeyboardInterrupt):
                if system_started:
                    self.so.escalonador.stop()
                    self.so.io_device.stop()
                break
        
        print("\n" + "=" * 60)
        print("SISTEMA ENCERRADO")
        print(f"Total de processos criados: {PCB.get_processo_count()}")
        print("=" * 60)

if __name__ == "__main__":
    s = Sistema(tam_mem=1024, tam_pg=16, quantum=5)
    s.run()