import math
import time
import threading
from enum import Enum
from collections import deque
import queue

# -------------------------------------------------------------------------------------------------------
# --------------------- H A R D W A R E - Definições de HW
# -------------------------------------------------------------------------------------------------------

class Opcode(Enum):
    DATA, ___, JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, JMPIGK, JMPILK, JMPIEK, JMPIGT, ADDI, SUBI, ADD, SUB, MULT, LDI, LDD, STD, LDX, STX, MOVE, SYSCALL, STOP = range(28)

class Interrupts(Enum):
    NO_INTERRUPT, INT_ENDERECO_INVALIDO, INT_INSTRUCAO_INVALIDA, INT_OVERFLOW, PAGE_FAULT, IO_COMPLETION = range(6)

class Word:
    def __init__(self, opc, ra, rb, p): self.opc, self.ra, self.rb, self.p = opc, ra, rb, p

class Memory:
    def __init__(self, size): self.data = [Word(Opcode.___, -1, -1, -1) for _ in range(size)]

class CPU(threading.Thread):
    def __init__(self, memory, so, debug=False):
        super().__init__(name="CPU")
        self.max_int, self.min_int = 32767, -32767
        self.memory, self.reg, self.debug, self.so = memory.data, [0] * 10, debug, so
        self.pc, self.ir, self.running_process, self.instructions_executed = 0, None, None, 0
        self.interrupt_register, self.interrupt_data = Interrupts.NO_INTERRUPT, None
        self.interrupt_lock, self.cpu_has_process_event = threading.Lock(), threading.Event()
        self.gm = None

    def set_context(self, pcb):
        self.running_process, self.pc, self.reg, self.instructions_executed = pcb, pcb.pc, pcb.registers.copy(), 0
        pcb.state = PCB.ProcessState.RUNNING

    def set_gerente_memoria(self, gm):
        self.gm = gm

    def _translate_address(self, logical_address, is_write_op=False):
        if self.running_process is None: self.so.interrupt_handler.handle(Interrupts.INT_ENDERECO_INVALIDO, logical_address); return -1
        tam_pg = self.gm.get_tam_pg()
        page_index = logical_address // tam_pg
        if not (0 <= page_index < len(self.running_process.page_table)): self.so.interrupt_handler.handle(Interrupts.INT_ENDERECO_INVALIDO, logical_address); return -1
        page_entry = self.running_process.page_table[page_index]
        if not page_entry.valid_bit: self.so.interrupt_handler.handle(Interrupts.PAGE_FAULT, logical_address); return -1
        if is_write_op: page_entry.dirty_bit = True
        return (page_entry.frame_number * tam_pg) + (logical_address % tam_pg)

    def _execute_one_instruction(self):
        physical_pc = self._translate_address(self.pc)
        if physical_pc == -1: return True
        self.ir = self.memory[physical_pc]
        if self.debug: print(f"    [CPU] PC: {self.pc} (Proc: {self.running_process.id}) -> INSTR: ", end=""); self.so.utils.dump_word(self.ir)
        
        opc, ra, rb, p = self.ir.opc, self.ir.ra, self.ir.rb, self.ir.p
        
        stop_quantum = False
        
        if opc == Opcode.SYSCALL: self.so.syscall_handler.handle(); stop_quantum = True
        elif opc == Opcode.STOP: self.so.syscall_handler.stop(); stop_quantum = True
        else:
            addr = -1
            is_write = opc in [Opcode.STD, Opcode.STX]
            if opc == Opcode.STD: addr = self._translate_address(p, is_write)
            elif opc == Opcode.STX: addr = self._translate_address(self.reg[ra], is_write)
            elif opc == Opcode.LDD: addr = self._translate_address(p)
            elif opc == Opcode.LDX: addr = self._translate_address(self.reg[rb])
            elif opc == Opcode.JMPIM: addr = self._translate_address(p)
            elif opc in [Opcode.JMPIGM, Opcode.JMPILM, Opcode.JMPIEM]: addr = self._translate_address(p)

            if addr == -1 and opc in [Opcode.STD, Opcode.STX, Opcode.LDD, Opcode.LDX, Opcode.JMPIM, Opcode.JMPIGM, Opcode.JMPILM, Opcode.JMPIEM]:
                return True

            if opc == Opcode.LDI: self.reg[ra] = p; self.pc += 1
            elif opc == Opcode.LDD: self.reg[ra] = self.memory[addr].p; self.pc += 1
            elif opc == Opcode.LDX: self.reg[ra] = self.memory[addr].p; self.pc += 1
            elif opc == Opcode.STD: self.memory[addr] = Word(Opcode.DATA, -1, -1, self.reg[ra]); self.pc += 1
            elif opc == Opcode.STX: self.memory[addr] = Word(Opcode.DATA, -1, -1, self.reg[rb]); self.pc += 1
            elif opc == Opcode.MOVE: self.reg[ra] = self.reg[rb]; self.pc += 1
            elif opc in [Opcode.ADD, Opcode.ADDI, Opcode.SUB, Opcode.SUBI, Opcode.MULT]:
                val = self.reg[rb] if opc in [Opcode.ADD, Opcode.SUB, Opcode.MULT] else p
                if opc in [Opcode.ADD, Opcode.ADDI]: self.reg[ra] += val
                elif opc in [Opcode.SUB, Opcode.SUBI]: self.reg[ra] -= val
                elif opc == Opcode.MULT: self.reg[ra] *= val
                self.pc += 1
            elif opc == Opcode.JMP: self.pc = p
            elif opc == Opcode.JMPIM: self.pc = self.memory[addr].p
            elif opc == Opcode.JMPIG and self.reg[rb]>0: self.pc=self.reg[ra]
            elif opc == Opcode.JMPIL and self.reg[rb]<0: self.pc=self.reg[ra]
            elif opc == Opcode.JMPIE and self.reg[rb]==0: self.pc=self.reg[ra]
            elif opc == Opcode.JMPIGK and self.reg[rb]>0: self.pc=p
            elif opc == Opcode.JMPILK and self.reg[rb]<0: self.pc=p
            elif opc == Opcode.JMPIEK and self.reg[rb]==0: self.pc=p
            elif opc == Opcode.JMPIGT and self.reg[ra]>self.reg[rb]: self.pc=p
            elif opc == Opcode.JMPIGM and self.reg[rb]>0: self.pc=self.memory[addr].p
            elif opc == Opcode.JMPILM and self.reg[rb]<0: self.pc=self.memory[addr].p
            elif opc == Opcode.JMPIEM and self.reg[rb]==0: self.pc=self.memory[addr].p
            else: self.pc += 1
        
        self.instructions_executed += 1
        return stop_quantum

    def run(self):
        while not self.so.shutdown:
            # 1. Trata interrupções pendentes. Esta é a maior prioridade.
            with self.interrupt_lock:
                if self.interrupt_register != Interrupts.NO_INTERRUPT:
                    self.so.interrupt_handler.handle(self.interrupt_register, self.interrupt_data)
                    self.interrupt_register, self.interrupt_data = Interrupts.NO_INTERRUPT, None
                    if self.running_process and self.running_process.state != PCB.ProcessState.RUNNING:
                        self.running_process = None

            # 2. Se a CPU está livre, tenta escalar um novo processo.
            if self.running_process is None:
                self.so.scheduler.schedule()

            # 3. Se um processo foi escalonado, executa seu quantum.
            if self.running_process:
                quantum_end = False
                while not quantum_end and self.instructions_executed < self.so.quantum:
                    quantum_end = self._execute_one_instruction()
                    # Uma interrupção externa (como I/O) pode preemptar o processo.
                    with self.interrupt_lock:
                        if self.interrupt_register != Interrupts.NO_INTERRUPT:
                            quantum_end = True

                # 4. Após o quantum, trata o processo que estava em execução.
                if self.running_process:
                    if self.running_process.state == PCB.ProcessState.RUNNING:
                        self.so.scheduler.handle_quantum_end(self.running_process)
                    # Se foi bloqueado/finalizado, o handler já tratou. A CPU fica livre.
                    self.running_process = None
            else:
                # 5. Se não há processo para rodar, a CPU espera por um evento.
                self.cpu_has_process_event.wait(timeout=1.0)
                if self.so.shutdown: break
                self.cpu_has_process_event.clear()

class IODevice(threading.Thread):
    def __init__(self, so):
        super().__init__(name="IODevice")
        self.so, self.request_queue, self.simulated_io_time = so, queue.Queue(), 2.0
    def submit_request(self, pcb, op): self.request_queue.put((pcb, op))
    def run(self):
        while not self.so.shutdown:
            try:
                pcb, op = self.request_queue.get(timeout=1)
                print(f"    [IO_DEVICE] Iniciando I/O para o processo {pcb.id}")
                time.sleep(self.simulated_io_time)
                print(f"    [IO_DEVICE] I/O para o processo {pcb.id} concluído.")
                with self.so.hw.cpu.interrupt_lock:
                    self.so.hw.cpu.interrupt_register = Interrupts.IO_COMPLETION
                    self.so.hw.cpu.interrupt_data = pcb
                self.so.hw.cpu.cpu_has_process_event.set() # Acorda a CPU para tratar a interrupcao
            except queue.Empty: continue

class HW:
    def __init__(self, mem_size, so_ref):
        self.mem = Memory(mem_size)
        self.cpu = CPU(self.mem, so_ref, debug=False)

# -------------------------------------------------------------------------------------------------------
# --------------------- SW - Sistema Operacional
# -------------------------------------------------------------------------------------------------------
class PageTableEntry:
    def __init__(self, page_idx): self.frame_number, self.valid_bit, self.dirty_bit, self.on_swap, self.page_idx = -1, False, False, False, page_idx
class PCB:
    _next_id = 0
    class ProcessState(Enum): NEW, READY, RUNNING, BLOCKED, FINISHED = range(5)
    def __init__(self, program, num_pages):
        self.id = PCB._next_id; PCB._next_id += 1
        self.program, self.pc, self.registers, self.state, self.block_reason = program, 0, [0] * 10, PCB.ProcessState.READY, ""
        self.page_table = [PageTableEntry(i) for i in range(num_pages)]

class GerenteDisco:
    def __init__(self, so): self.so, self.swap_area = so, {}
    def save_to_swap(self, pcb, page_entry, page_data):
        print(f"    [DISK] Salvando pagina SUJA {page_entry.page_idx} do processo {pcb.id} na area de swap.")
        self.swap_area[(pcb.id, page_entry.page_idx)] = list(page_data)
        page_entry.on_swap = True
    def load_page(self, pcb, page_idx):
        page_entry = pcb.page_table[page_idx]
        page_size = self.so.gm.get_tam_pg()
        if page_entry.on_swap: print(f"    [DISK] Carregando pagina {page_idx} do processo {pcb.id} da area de swap."); return self.swap_area.get((pcb.id, page_idx))
        else: print(f"    [DISK] Carregando pagina {page_idx} do processo {pcb.id} do programa original."); start = page_idx * page_size; return pcb.program.image[start : start + page_size]

class GerenteMemoria:
    def __init__(self, tam_mem, tam_pg):
        self.tam_mem, self.tam_pg, self.num_frames = tam_mem, tam_pg, tam_mem // tam_pg
        self.free_frames = [True] * self.num_frames
        self.frame_load_order, self.frame_to_pcb_map, self.lock = deque(), {}, threading.Lock()
    def find_free_frame(self): return next((i for i, free in enumerate(self.free_frames) if free), -1)
    def find_victim_frame_fifo(self):
        victim_frame = self.frame_load_order.popleft()
        victim_pcb = self.frame_to_pcb_map.pop(victim_frame, None)
        victim_page_entry = None
        if victim_pcb:
            for entry in victim_pcb.page_table:
                if entry.frame_number == victim_frame:
                    entry.valid_bit = False; victim_page_entry = entry
                    print(f"    [MEM] Vitimacao! Frame {victim_frame} (do processo {victim_pcb.id}, pagina {entry.page_idx}) foi escolhido.")
                    break
        return victim_frame, victim_pcb, victim_page_entry
    def allocate_frame_for_page(self, pcb, page_entry, frame_number):
        self.free_frames[frame_number] = False
        self.frame_load_order.append(frame_number)
        self.frame_to_pcb_map[frame_number] = pcb
        page_entry.frame_number, page_entry.valid_bit, page_entry.dirty_bit = frame_number, True, False
    def get_tam_pg(self): return self.tam_pg
    def exibir_status(self):
        with self.lock:
            print("\n--- STATUS DA MEMORIA (MAPA DE FRAMES) ---")
            for i in range(self.num_frames):
                if self.free_frames[i]:
                    print(f"  Frame {i:02d}: Livre")
                else:
                    pcb = self.frame_to_pcb_map.get(i)
                    if pcb:
                        page_idx = next((entry.page_idx for entry in pcb.page_table if entry.frame_number == i), -1)
                        print(f"  Frame {i:02d}: Ocupado -> (Processo: {pcb.id}, Pagina: {page_idx})")
                    else: print(f"  Frame {i:02d}: Ocupado (Processo Desconhecido)")
            print("------------------------------------------")

class GerenteProcessos:
    def __init__(self, so):
        self.so = so
        self.ready_queue, self.blocked_queue, self.all_processes = deque(), [], []
        self.lock = threading.Lock()
    def cria_processo(self, programa):
        with self.lock:
            if not programa: print("Erro: Programa nao encontrado."); return -1
            num_pages = math.ceil(len(programa.image) / self.so.gm.get_tam_pg())
            pcb = PCB(programa, num_pages) # State is READY by default
            # Immediately block it for the initial page load.
            pcb.state = PCB.ProcessState.BLOCKED
            pcb.block_reason = "Initial Load"
            self.all_processes.append(pcb)
            self.blocked_queue.append(pcb)
            print(f"[KERNEL] Processo {pcb.id} ({programa.name}) criado. Bloqueado para carga inicial.");
        self.so.interrupt_handler.load_initial_page(pcb)
        return pcb.id
    def desaloca_processo(self, pcb):
        with self.so.gm.lock:
            for page_entry in pcb.page_table:
                if page_entry.valid_bit:
                    frame = page_entry.frame_number
                    self.so.gm.free_frames[frame] = True
                    if frame in self.so.gm.frame_to_pcb_map: del self.so.gm.frame_to_pcb_map[frame]
                    if frame in self.so.gm.frame_load_order: self.so.gm.frame_load_order.remove(frame)
        self.all_processes.remove(pcb)
        print(f"[KERNEL] Processo {pcb.id} finalizado e memoria liberada.")
    def list_all_processes(self):
        with self.lock:
            print("\n--- LISTA DE PROCESSOS ---")
            if not self.all_processes: print("Nenhum processo no sistema.")
            for p in self.all_processes:
                blocked_reason = f" ({p.block_reason})" if p.state == PCB.ProcessState.BLOCKED else ""
                print(f"  ID: {p.id} | Nome: {p.program.name:<12} | Estado: {p.state.name:<8}{blocked_reason}")
            print("--------------------------------------------------")
    def dump_processo(self, pcb_id):
        with self.lock:
            pcb = self._find_pcb(pcb_id)
            if not pcb: print(f"Erro: Processo com ID {pcb_id} nao encontrado."); return
            print(f"\n--- DUMP Processo ID: {pcb.id} ---")
            print(f"Estado: {pcb.state.name}, PC: {pcb.pc}, Registradores: {pcb.registers}")
            print("Tabela de Paginas:")
            for i, entry in enumerate(pcb.page_table):
                print(f"  Pagina {i}: Frame={entry.frame_number if entry.valid_bit else 'N/A'}, Valido={entry.valid_bit}, Sujo={entry.dirty_bit}, NoSwap={entry.on_swap}")
            print("--- FIM DUMP ---")
    def exibir_estatisticas(self):
        with self.lock:
            print("\n--- ESTATISTICAS DE PROCESSOS ---")
            total = len(self.all_processes)
            ready = len(self.ready_queue)
            blocked = len(self.blocked_queue)
            running = 1 if self.so.hw.cpu.running_process else 0
            print(f"  Total de Processos: {total}, Prontos: {ready}, Bloqueados: {blocked}, Executando: {running}")
            print("--------------------------------------------------")
    def executa_processo(self, pcb_id):
        with self.lock:
            pcb = self._find_pcb(pcb_id)
            if not pcb:
                print(f"Erro: Processo {pcb_id} nao encontrado.")
                return False
            
            if pcb.state == PCB.ProcessState.READY:
                if pcb not in self.ready_queue:
                    # Primeira execucao: adiciona ao final da fila de prontos
                    self.ready_queue.append(pcb)
                    print(f"[KERNEL] Processo {pcb_id} adicionado a fila de prontos.")
                else:
                    # Processo ja esta na fila, prioriza movendo para o inicio
                    self.ready_queue.remove(pcb)
                    self.ready_queue.appendleft(pcb)
                    print(f"[KERNEL] Processo {pcb_id} priorizado.")
                return True
            else:
                print(f"Erro: Processo {pcb_id} nao esta pronto para execucao (estado: {pcb.state.name}).")
                return False
    def executa_todos_prontos(self):
        with self.lock:
            added_count = 0
            # Itera sobre uma cópia da lista, pois a fila de prontos pode ser modificada
            for pcb in list(self.all_processes):
                if pcb.state == PCB.ProcessState.READY and pcb not in self.ready_queue:
                    self.ready_queue.append(pcb)
                    added_count += 1
            if added_count > 0:
                print(f"[KERNEL] {added_count} processo(s) adicionado(s) a fila de prontos.")
                return True
            else:
                print("[KERNEL] Nenhum processo novo para adicionar a fila de prontos.")
                return False
    def _find_pcb(self, pcb_id):
        return next((p for p in self.all_processes if p.id == pcb_id), None)

class Utilities:
    def __init__(self, hw): self.hw = hw
    def dump_word(self, w): print(f"[ {w.opc.name:10s}, R1:{w.ra:2d}, R2:{w.rb:2d}, P:{w.p:4d} ]")
    def dump_memory_range(self, start, end):
        with self.hw.so.gm.lock:
            print(f"\n--- DUMP Memoria Fisica (de {start} a {end}) ---")
            for i in range(start, end + 1):
                if 0 <= i < len(self.hw.mem.data): print(f"Fis.{i:04d}: ", end=""); self.dump_word(self.hw.mem.data[i])
            print("--- FIM DUMP ---")

class SO:
    def __init__(self, mem_size, tam_pg, quantum):
        self.quantum, self.shutdown = quantum, False
        self.hw = HW(mem_size, self)
        self.gm = GerenteMemoria(mem_size, tam_pg)
        self.disk = GerenteDisco(self)
        self.gp = GerenteProcessos(self)
        self.utils = Utilities(self.hw)
        self.scheduler = Scheduler(self)
        self.interrupt_handler = InterruptHandling(self)
        self.syscall_handler = SysCallHandling(self)
        self.io_device = IODevice(self)
        self.hw.cpu.set_gerente_memoria(self.gm)

class Scheduler:
    def __init__(self, so): self.so = so
    def schedule(self):
        if self.so.hw.cpu.running_process or self.so.shutdown: return
        with self.so.gp.lock:
            if self.so.gp.ready_queue:
                pcb = self.so.gp.ready_queue.popleft()
                print(f"    [ESCALONADOR] Processo {pcb.id} escalonado para a CPU.")
                self.so.hw.cpu.set_context(pcb)
                self.so.hw.cpu.cpu_has_process_event.set()
    def handle_quantum_end(self, pcb):
        with self.so.gp.lock:
            if pcb.state == PCB.ProcessState.RUNNING:
                print(f"    [ESCALONADOR] Quantum do processo {pcb.id} terminou.")
                pcb.state, pcb.pc, pcb.registers = PCB.ProcessState.READY, self.so.hw.cpu.pc, self.so.hw.cpu.reg.copy()
                self.so.gp.ready_queue.append(pcb)
        # A chamada para o escalonador agora eh centralizada no loop da CPU.

class InterruptHandling:
    def __init__(self, so): self.so = so
    def handle(self, interrupt, data):
        pcb = self.so.hw.cpu.running_process if interrupt != Interrupts.IO_COMPLETION else data
        if not pcb: return
        print(f"    [KERNEL] Interrupcao! Tipo: {interrupt.name}, Processo: {pcb.id}")
        with self.so.gp.lock:
            if interrupt == Interrupts.IO_COMPLETION:
                if pcb in self.so.gp.blocked_queue:
                    self.so.gp.blocked_queue.remove(pcb)
                    pcb.state, pcb.block_reason = PCB.ProcessState.READY, ""
                    self.so.gp.ready_queue.append(pcb)
            elif interrupt == Interrupts.PAGE_FAULT:
                if pcb.state == PCB.ProcessState.RUNNING:
                    pcb.state, pcb.block_reason = PCB.ProcessState.BLOCKED, "Page Fault"
                    pcb.pc = self.so.hw.cpu.pc; pcb.registers = self.so.hw.cpu.reg.copy()
                if pcb not in self.so.gp.blocked_queue: self.so.gp.blocked_queue.append(pcb)
                thread = threading.Thread(target=self._handle_page_fault_async, args=(pcb, data))
                thread.daemon = True
                thread.start()
            else:
                print(f"    [KERNEL] Interrupcao fatal para o processo {pcb.id}. Removendo.")
                if pcb in self.so.gp.ready_queue: self.so.gp.ready_queue.remove(pcb)
                if pcb in self.so.gp.blocked_queue: self.so.gp.blocked_queue.remove(pcb)
                self.so.gp.desaloca_processo(pcb)
                if self.so.hw.cpu.running_process == pcb:
                    self.so.hw.cpu.running_process = None
        # A chamada para o escalonador agora eh centralizada no loop da CPU.
    def _handle_page_fault_async(self, pcb, logical_address):
        time.sleep(0.2)
        page_needed = logical_address // self.so.gm.get_tam_pg()

        # Atomically find and reserve a frame
        victim_pcb, victim_page_entry = None, None
        with self.so.gm.lock:
            frame = self.so.gm.find_free_frame()
            if frame == -1:
                frame, victim_pcb, victim_page_entry = self.so.gm.find_victim_frame_fifo()

        # If a victim was chosen and it's dirty, write it to swap (I/O, no lock)
        if victim_page_entry and victim_page_entry.dirty_bit:
            page_size = self.so.gm.get_tam_pg()
            start_addr = victim_page_entry.frame_number * page_size
            page_data = self.so.hw.mem.data[start_addr : start_addr + page_size]
            self.so.disk.save_to_swap(victim_pcb, victim_page_entry, page_data)
            time.sleep(0.5) # Simulate disk write

        # Load the required page into the reserved frame
        self.load_page_into_memory(pcb, page_needed, frame)
        
    def load_initial_page(self, pcb):
        thread = threading.Thread(target=self._handle_page_fault_async, args=(pcb, 0))
        thread.daemon = True
        thread.start()
    def load_page_into_memory(self, pcb, page_idx, frame):
        page_data = self.so.disk.load_page(pcb, page_idx)
        page_size = self.so.gm.get_tam_pg()
        time.sleep(0.3)
        with self.so.gm.lock:
            frame_addr = frame * page_size
            for i in range(len(page_data)): self.so.hw.mem.data[frame_addr + i] = page_data[i]
            self.so.gm.allocate_frame_for_page(pcb, pcb.page_table[page_idx], frame)
        with self.so.gp.lock:
            if pcb in self.so.gp.blocked_queue:
                is_initial_load = pcb.block_reason == "Initial Load"
                self.so.gp.blocked_queue.remove(pcb)
                pcb.state, pcb.block_reason = PCB.ProcessState.READY, ""
                if is_initial_load:
                    print(f"[KERNEL] Processo {pcb.id} pronto. Use 'exec {pcb.id}' para iniciar.")
                else:
                    self.so.gp.ready_queue.append(pcb)

class SysCallHandling:
    def __init__(self, so): self.so = so
    def handle(self):
        pcb = self.so.hw.cpu.running_process
        with self.so.gp.lock:
            pcb.state, pcb.block_reason = PCB.ProcessState.BLOCKED, "I/O Request"
            pcb.pc, pcb.registers = self.so.hw.cpu.pc + 1, self.so.hw.cpu.reg.copy()
            if pcb not in self.so.gp.blocked_queue: self.so.gp.blocked_queue.append(pcb)
        self.so.io_device.submit_request(pcb, "WRITE")
        print(f"    [SYSCALL] Processo {pcb.id} bloqueado por I/O.")
        # A CPU ira re-escalonar apos tratar a syscall.
    def stop(self):
        pcb = self.so.hw.cpu.running_process
        print(f"    [SYSCALL] STOP no processo {pcb.id}")
        with self.so.gp.lock:
            if pcb is not None:
                pcb.state = PCB.ProcessState.FINISHED
                if pcb in self.so.gp.ready_queue: self.so.gp.ready_queue.remove(pcb)
                if pcb in self.so.gp.blocked_queue: self.so.gp.blocked_queue.remove(pcb)
                self.so.gp.desaloca_processo(pcb)
                self.so.hw.cpu.running_process = None
        # A CPU ira re-escalonar apos tratar a syscall.

class Program:
    def __init__(self, name, image): self.name, self.image = name, image
class Programs:
    def __init__(self):
        self.programs = [
            Program("fatorialV2", [Word(Opcode.LDI,0,-1,5), Word(Opcode.SYSCALL,-1,-1,-1), Word(Opcode.STD,0,-1,19), Word(Opcode.LDD,0,-1,19), Word(Opcode.LDI,1,-1,-1), Word(Opcode.LDI,2,-1,13), Word(Opcode.JMPIL,2,0,-1), Word(Opcode.LDI,1,-1,1), Word(Opcode.LDI,6,-1,1), Word(Opcode.LDI,7,-1,14), Word(Opcode.JMPIE,7,0,0), Word(Opcode.MULT,1,0,-1), Word(Opcode.SUB,0,6,-1), Word(Opcode.JMP,-1,-1,10), Word(Opcode.STD,1,-1,18), Word(Opcode.STOP,-1,-1,-1), Word(Opcode.DATA,-1,-1,-1), Word(Opcode.DATA,-1,-1,-1)]),
            Program("progMinimo", [Word(Opcode.LDI,0,-1,999), Word(Opcode.STD,0,-1,8), Word(Opcode.STOP,-1,-1,-1)]),
            Program("PB", [Word(Opcode.LDI,0,-1,7), Word(Opcode.STD,0,-1,50), Word(Opcode.LDD,0,-1,50), Word(Opcode.LDI,1,-1,-1), Word(Opcode.LDI,2,-1,13), Word(Opcode.JMPIL,2,0,-1), Word(Opcode.LDI,1,-1,1), Word(Opcode.LDI,6,-1,1), Word(Opcode.LDI,7,-1,13), Word(Opcode.JMPIE,7,0,0), Word(Opcode.MULT,1,0,-1), Word(Opcode.SUB,0,6,-1), Word(Opcode.JMP,-1,-1,9), Word(Opcode.STD,1,-1,15), Word(Opcode.STOP,-1,-1,-1), Word(Opcode.DATA,-1,-1,-1)]),
            Program("PC", [Word(Opcode.LDI,7,-1,5), Word(Opcode.STD,7,-1,45), Word(Opcode.LDI,5,-1,46), Word(Opcode.LDI,0,-1,4), Word(Opcode.STD,0,-1,46), Word(Opcode.LDI,0,-1,3), Word(Opcode.STD,0,-1,47), Word(Opcode.LDI,0,-1,5), Word(Opcode.STD,0,-1,48), Word(Opcode.LDI,0,-1,1), Word(Opcode.STD,0,-1,49), Word(Opcode.LDI,0,-1,2), Word(Opcode.STD,0,-1,50), Word(Opcode.LDI,3,-1,38), Word(Opcode.LDI,6,-1,0), Word(Opcode.LDD,7,-1,45), Word(Opcode.SUBI,6,7,1), Word(Opcode.JMPIE,3,6,0), Word(Opcode.LDX,0,5,-1), Word(Opcode.ADDI,4,5,1), Word(Opcode.LDX,1,4,-1), Word(Opcode.SUB,0,1,0), Word(Opcode.JMPIL,3,0,0), Word(Opcode.STX,5,1,-1), Word(Opcode.STX,4,0,-1), Word(Opcode.ADDI,5,-1,1), Word(Opcode.SUBI,6,-1,1), Word(Opcode.JMPIG,3,6,0), Word(Opcode.SUBI,7,-1,1), Word(Opcode.JMP, -1,-1,14), Word(Opcode.STOP,-1,-1,-1)])
        ]
    def retrieve_program(self, name): return next((p for p in self.programs if p.name.lower() == name.lower()), None)

class Sistema:
    def __init__(self, mem_size, tam_pg, quantum):
        self.so = SO(mem_size, tam_pg, quantum)
        self.progs = Programs()
    def run_shell(self):
        print("\n============== Sistema Operacional Concorrente ==============")
        print("Comandos: new <prog>, ps, rm <id>, exec <id>, execall, dump <id>, dumpm <ini> <fim>, stats, memstat, traceon, traceoff, exit")
        while not self.so.shutdown:
            try:
                cmd_line = input("> ").strip().split()
                if not cmd_line: continue
                cmd = cmd_line[0].lower()
                if cmd == "new" and len(cmd_line) > 1: self.so.gp.cria_processo(self.progs.retrieve_program(cmd_line[1]))
                elif cmd == "ps": self.so.gp.list_all_processes()
                elif cmd == "rm" and len(cmd_line) > 1:
                    pcb_id_to_remove = int(cmd_line[1])
                    with self.so.gp.lock: # Lock to safely check running process and queues
                        pcb = self.so.gp._find_pcb(pcb_id_to_remove)
                        if pcb:
                            if self.so.hw.cpu.running_process and self.so.hw.cpu.running_process.id == pcb_id_to_remove:
                                print(f"Erro: Nao e possivel remover o processo {pcb_id_to_remove} enquanto esta em execucao.")
                            else:
                                if pcb in self.so.gp.ready_queue: self.so.gp.ready_queue.remove(pcb)
                                if pcb in self.so.gp.blocked_queue: self.so.gp.blocked_queue.remove(pcb)
                                self.so.gp.desaloca_processo(pcb)
                        else: print(f"Processo com ID {pcb_id_to_remove} nao encontrado.")
                elif cmd == "exec" and len(cmd_line) > 1:
                    if self.so.gp.executa_processo(int(cmd_line[1])):
                        self.so.hw.cpu.cpu_has_process_event.set() # Acorda a CPU para escalonar
                elif cmd == "execall":
                    if self.so.gp.executa_todos_prontos():
                        self.so.hw.cpu.cpu_has_process_event.set() # Acorda a CPU para escalonar
                elif cmd == "dump" and len(cmd_line) > 1: self.so.gp.dump_processo(int(cmd_line[1]))
                elif cmd == "dumpm" and len(cmd_line) > 2: self.so.utils.dump_memory_range(int(cmd_line[1]), int(cmd_line[2]))
                elif cmd == "stats": self.so.gp.exibir_estatisticas()
                elif cmd == "memstat": self.so.gm.exibir_status()
                elif cmd == "traceon": self.so.hw.cpu.debug = True; print("Modo trace ativado.")
                elif cmd == "traceoff": self.so.hw.cpu.debug = False; print("Modo trace desativado.")
                elif cmd == "exit": self.so.shutdown = True; self.so.hw.cpu.cpu_has_process_event.set(); break
                else: print("Comando invalido ou argumentos faltando.")
            except (ValueError, IndexError): print("Erro no comando: Argumento invalido ou ausente.")
            except Exception as e: print(f"Erro inesperado: {e}")
    def run(self):
        shell_thread = threading.Thread(target=self.run_shell, name="Shell")
        # Todas as threads de trabalho devem ser daemon para permitir o encerramento limpo.
        shell_thread.daemon = True
        self.so.io_device.daemon = True
        self.so.hw.cpu.daemon = True
        
        self.so.io_device.start()
        self.so.hw.cpu.start()
        shell_thread.start()
        
        try:
            # O thread principal (não-daemon) espera aqui, mantendo o programa vivo.
            # O Ctrl+C será capturado por este bloco.
            while shell_thread.is_alive():
                shell_thread.join(timeout=1.0) # Usa timeout para não bloquear indefinidamente
        except KeyboardInterrupt:
            print("\nCtrl+C detectado. Encerrando o sistema...")
        
        self.so.shutdown = True
        self.so.hw.cpu.cpu_has_process_event.set() # Acorda a CPU caso ela esteja em wait()
        print("Shell encerrado. Finalizando threads...")

if __name__ == "__main__":
    sistema = Sistema(mem_size=64, tam_pg=16, quantum=5)
    sistema.run()