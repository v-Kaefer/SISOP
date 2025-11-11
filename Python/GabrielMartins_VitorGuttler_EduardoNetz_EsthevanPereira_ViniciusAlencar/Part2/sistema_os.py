import math
import threading
import time
from enum import Enum

# PUCRS - Escola Politécnica - Sistemas Operacionais
# Prof. Fernando Dotti
# Código fornecido como parte da solução do projeto de Sistemas Operacionais
#
# TRABALHO T2a: Implementação de Concorrência e I/O Assíncrono
# Baseado no esquema do SO (multithreaded) fornecido no enunciado
#
# Este código implementa:
# - T1: Gerenciamento de Memória (paginação), Processos e Escalonamento
# - T2a: Threads concorrentes (Shell, CPU, Console), I/O assíncrono, 3 estados

# -------------------------------------------------------------------------------------------------------
# --------------------- ESTRUTURAS AUXILIARES
# -------------------------------------------------------------------------------------------------------

# Exceção para fila vazia
class QueueEmpty(Exception):
    """Exceção levantada quando fila está vazia"""
    pass

# -------------------------------------------------------------------------------------------------------
# --------------------- H A R D W A R E - Definições de HW (Trabalho T1)
# -------------------------------------------------------------------------------------------------------

# Conjunto de instruções da CPU (ISA - Instruction Set Architecture)
class Opcode(Enum):
    DATA, ___, JMP, JMPI, JMPIG, JMPIL, JMPIE, JMPIM, JMPIGM, JMPILM, JMPIEM, JMPIGK, JMPILK, JMPIEK, JMPIGT, ADDI, SUBI, ADD, SUB, MULT, LDI, LDD, STD, LDX, STX, MOVE, SYSCALL, STOP = range(28)

# Tipos de interrupções do hardware
# INT_IO_COMPLETE adicionado para T2a (int IO - retorno de I/O assíncrono)
class Interrupts(Enum):
    NO_INTERRUPT, INT_ENDERECO_INVALIDO, INT_INSTRUCAO_INVALIDA, INT_OVERFLOW, INT_IO_COMPLETE = range(5)

# Palavra de memória: representa uma instrução ou dado
# Formato: [opcode, registrador_a, registrador_b, parâmetro]
class Word:
    def __init__(self, opc, ra, rb, p):
        self.opc, self.ra, self.rb, self.p = opc, ra, rb, p

# ====================================================================================================
# MEMÓRIA - Componente de hardware (referenciado no esquema do SO como "MEMÓRIA")
# Acesso direto via DMA para operações de I/O (conforme diagrama)
# ====================================================================================================
class Memory:
    def __init__(self, size):
        self.pos = [Word(Opcode.___, -1, -1, -1) for _ in range(size)]

# ====================================================================================================
# ESTADO CPU - Componente de hardware (caixa "estado CPU" no esquema)
# Contém: registradores, PC, IR, flags de interrupção
# Thread CPU usa este componente para execução de instruções
# ====================================================================================================
class CPU:
    def __init__(self, mem, debug=False):
        # Limites para detecção de overflow
        self.max_int, self.min_int = 32767, -32767
        # Referência à memória física
        self.m = mem.pos
        # Registradores (R0-R9)
        self.reg = [0] * 10
        # Modo debug (trace)
        self.debug = debug
        # Program Counter
        self.pc = 0
        # Instruction Register
        self.ir = None
        # Flag de interrupção
        self.irpt = Interrupts.NO_INTERRUPT
        # Ponteiro para processo em execução (running no diagrama)
        self.running_process = None
        # Referências para handlers e gerentes
        self.ih, self.sys_call, self.gm, self.u = None, None, None, None
        # Flag para parar execução
        self.cpu_stop = False
        # Contador de instruções (para quantum)
        self.instructions_executed = 0
        # Flag de interrupção de I/O completo (T2a - sinalização do Console para CPU)
        self.irpt_io_complete = None

    def set_address_of_handlers(self, ih, sys_call):
        self.ih, self.sys_call = ih, sys_call

    def set_utilities(self, u):
        self.u = u

    def set_gerente_memoria(self, gm):
        self.gm = gm

    # Restaura contexto do PCB na CPU (usado pelo Thread Escalonador)
    # Corresponde a "restaura contexto na CPU" no diagrama
    def set_context(self, pcb):
        self.running_process = pcb
        self.pc = pcb.pc
        self.reg = pcb.registers.copy()
        self.irpt = Interrupts.NO_INTERRUPT
        self.instructions_executed = 0
        pcb.state = PCB.ProcessState.RUNNING

    # Verifica se endereço físico é válido (controle de acesso indevido)
    def _legal(self, e):
        if 0 <= e < len(self.m): return True
        self.irpt = Interrupts.INT_ENDERECO_INVALIDO
        return False

    # Mapeamento de endereço lógico para físico (esquema de paginação - T1)
    # Caixa "Mapeamento de endereço e controle de acesso indevido" no diagrama
    def _translate_address(self, logical_address):
        if not self.running_process:
            self.irpt = Interrupts.INT_ENDERECO_INVALIDO
            return -1
        tam_pg = self.gm.get_tam_pg()
        page, offset = logical_address // tam_pg, logical_address % tam_pg
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
        if not self.running_process: return
        self.cpu_stop = False
        while not self.cpu_stop and (self.instructions_executed < quantum or quantum == -1):
            
            physical_pc = self._translate_address(self.pc)
            if not self._legal(physical_pc): break
            
            self.ir = self.m[physical_pc]
            if self.debug:
                print(f"    PC: {self.pc} -> INSTR: ", end="")
                self.u.dump(self.ir)
            
            opc, ra, rb, p = self.ir.opc, self.ir.ra, self.ir.rb, self.ir.p
            
            if opc == Opcode.LDI: self.reg[ra] = p; self.pc += 1
            elif opc == Opcode.LDD:
                addr = self._translate_address(p)
                if self._legal(addr): self.reg[ra] = self.m[addr].p; self.pc += 1
            elif opc == Opcode.LDX:
                addr = self._translate_address(self.reg[rb])
                if self._legal(addr): self.reg[ra] = self.m[addr].p; self.pc += 1
            elif opc == Opcode.STD:
                addr = self._translate_address(p)
                if self._legal(addr): self.m[addr] = Word(Opcode.DATA, -1, -1, self.reg[ra]); self.pc += 1
            elif opc == Opcode.STX:
                addr = self._translate_address(self.reg[ra])
                if self._legal(addr): self.m[addr] = Word(Opcode.DATA, -1, -1, self.reg[rb]); self.pc += 1
            elif opc == Opcode.MOVE: self.reg[ra] = self.reg[rb]; self.pc += 1
            elif opc == Opcode.ADD: self.reg[ra] += self.reg[rb]; self._test_overflow(self.reg[ra]); self.pc += 1
            elif opc == Opcode.ADDI: self.reg[ra] += p; self._test_overflow(self.reg[ra]); self.pc += 1
            elif opc == Opcode.SUB: self.reg[ra] -= self.reg[rb]; self._test_overflow(self.reg[ra]); self.pc += 1
            elif opc == Opcode.SUBI: self.reg[ra] -= p; self._test_overflow(self.reg[ra]); self.pc += 1
            elif opc == Opcode.MULT: self.reg[ra] *= self.reg[rb]; self._test_overflow(self.reg[ra]); self.pc += 1
            elif opc == Opcode.JMP: self.pc = p
            elif opc == Opcode.JMPIM:
                addr = self._translate_address(p)
                if self._legal(addr): self.pc = self.m[addr].p
            elif opc == Opcode.JMPIG: self.pc = self.reg[ra] if self.reg[rb] > 0 else self.pc + 1
            elif opc == Opcode.JMPIGK: self.pc = p if self.reg[rb] > 0 else self.pc + 1
            elif opc == Opcode.JMPILK: self.pc = p if self.reg[rb] < 0 else self.pc + 1
            elif opc == Opcode.JMPIEK: self.pc = p if self.reg[rb] == 0 else self.pc + 1
            elif opc == Opcode.JMPIL: self.pc = self.reg[ra] if self.reg[rb] < 0 else self.pc + 1
            elif opc == Opcode.JMPIE: self.pc = self.reg[ra] if self.reg[rb] == 0 else self.pc + 1
            elif opc == Opcode.JMPIGM:
                addr = self._translate_address(p)
                if self._legal(addr): self.pc = self.m[addr].p if self.reg[rb] > 0 else self.pc + 1
            elif opc == Opcode.JMPILM:
                addr = self._translate_address(p)
                if self._legal(addr): self.pc = self.m[addr].p if self.reg[rb] < 0 else self.pc + 1
            elif opc == Opcode.JMPIEM:
                addr = self._translate_address(p)
                if self._legal(addr): self.pc = self.m[addr].p if self.reg[rb] == 0 else self.pc + 1
            elif opc == Opcode.JMPIGT: self.pc = p if self.reg[ra] > self.reg[rb] else self.pc + 1
            elif opc == Opcode.SYSCALL:
                self.sys_call.handle()
                if not self.cpu_stop: self.pc += 1
            elif opc == Opcode.STOP: self.sys_call.stop(); self.cpu_stop = True
            else: self.irpt = Interrupts.INT_INSTRUCAO_INVALIDA

            if self.irpt != Interrupts.NO_INTERRUPT:
                self.ih.handle(self.irpt, self.pc)
                if self.irpt != Interrupts.INT_IO_COMPLETE:
                    self.cpu_stop = True
                self.irpt = Interrupts.NO_INTERRUPT
            self.instructions_executed += 1
        # Isso garante que, se a CPU estiver rodando quando a interrupção chega, o handler também roda pela CPU, mas como o device já terá tratado, será idempotente (sem efeitos colaterais).
        if self.running_process:
            if self.running_process.state == PCB.ProcessState.RUNNING:
                self.running_process.state = PCB.ProcessState.READY
            if self.running_process.state != PCB.ProcessState.FINISHED:
                self.running_process.pc = self.pc
                self.running_process.registers = self.reg.copy()

class HW:
    def __init__(self, tam_mem):
        self.mem = Memory(tam_mem)
        self.cpu = CPU(self.mem, debug=False)

# -------------------------------------------------------------------------------------------------------
# --------------------- SW - Sistema Operacional
# -------------------------------------------------------------------------------------------------------

class IORequest:
    def __init__(self, process_id, operation, address, pcb):
        self.process_id, self.operation, self.address, self.pcb = process_id, operation, address, pcb
        self.timestamp = time.time()

class PCB:
    _processo_count = 0
    
    class ProcessState(Enum): 
        READY, RUNNING, BLOCKED, FINISHED = range(4)
        
    def __init__(self, page_table):
        self.id = page_table[0] if page_table else 0
        PCB._processo_count += 1
        self.processo_number = PCB._processo_count
        self.pc, self.registers = 0, [0] * 10
        self.page_table = page_table
        self.state = PCB.ProcessState.READY
    
    @classmethod
    def get_processo_count(cls): return cls._processo_count
    
    @classmethod
    def reset_count(cls): cls._processo_count = 0

# ====================================================================================================
# GM: GERENTE DE MEMÓRIA (Trabalho T1 - conforme diagrama "GM" no esquema)
# Responsável por:
# - alocar memória (usando esquema de paginação)
# - desalocar memória
# O esquema de paginação fica implementado aqui conforme T1
# ====================================================================================================
class GerenteMemoria:
    def __init__(self, tam_mem, tam_pg):
        self.tam_mem, self.tam_pg = tam_mem, tam_pg
        self.num_frames = tam_mem // tam_pg
        self.free_frames = [True] * self.num_frames
        self.frame_to_process = [-1] * self.num_frames

    def aloca(self, num_palavras, frame_inicial=None):
        paginas_necessarias = math.ceil(num_palavras / self.tam_pg) if num_palavras > 0 else 0
        if frame_inicial is not None: return self._aloca_especifico(paginas_necessarias, frame_inicial)
        else: return self._aloca_automatico(paginas_necessarias)

    def _aloca_especifico(self, paginas_necessarias, frame_inicial):
        if not (0 <= frame_inicial < self.num_frames):
            print(f"Erro: Frame {frame_inicial} inválido (0-{self.num_frames-1})")
            return None
        if frame_inicial + paginas_necessarias > self.num_frames:
            print(f"Erro: Não há {paginas_necessarias} frames consecutivos a partir do frame {frame_inicial}")
            return None
        for i in range(frame_inicial, frame_inicial + paginas_necessarias):
            if not self.free_frames[i]:
                print(f"Erro: Frame {i} já está ocupado pelo processo ID {self.frame_to_process[i]}")
                return None
        tabela_paginas = []
        for i in range(frame_inicial, frame_inicial + paginas_necessarias):
            self.free_frames[i] = False
            tabela_paginas.append(i)
            self.frame_to_process[i] = frame_inicial
        print(f"Alocado nos frames consecutivos: {tabela_paginas}")
        return tabela_paginas

    def _aloca_automatico(self, paginas_necessarias):
        for start_frame in range(self.num_frames - paginas_necessarias + 1):
            frames_disponiveis = True
            for i in range(start_frame, start_frame + paginas_necessarias):
                if not self.free_frames[i]: frames_disponiveis = False; break
            if frames_disponiveis:
                tabela_paginas = []
                for i in range(start_frame, start_frame + paginas_necessarias):
                    self.free_frames[i] = False
                    tabela_paginas.append(i)
                    self.frame_to_process[i] = start_frame
                print(f"Alocado automaticamente nos frames: {tabela_paginas}")
                return tabela_paginas
        return None
    
    def desaloca(self, tabela_paginas):
        if tabela_paginas:
            for frame in tabela_paginas:
                if 0 <= frame < self.num_frames: 
                    self.free_frames[frame] = True
                    self.frame_to_process[frame] = -1

    def get_tam_pg(self): return self.tam_pg
    
    def mostrar_status(self):
        print(f"=== STATUS DA MEMÓRIA ===")
        print(f"Total de frames: {self.num_frames}")
        print(f"Frames livres: {self.free_frames.count(True)}")
        print(f"Frames ocupados: {self.free_frames.count(False)}")
        print(f"\nMapa de ocupação:")
        for i in range(0, self.num_frames, 16):
            linha_status, linha_processos = [], []
            for j in range(i, min(i + 16, self.num_frames)):
                if self.free_frames[j]: linha_status.append("."); linha_processos.append("  ")
                else: linha_status.append("X"); linha_processos.append(f"{self.frame_to_process[j]:2d}")
            print(f"Frames {i:2d}-{min(i+15, self.num_frames-1):2d}: {''.join(linha_status)}")
            print(f"Proc. IDs: {''.join(linha_processos)}")
        print("=========================")

# ====================================================================================================
# GP: GERENTE DE PROCESSOS (Trabalho T1 e T2a - conforme diagrama "GP" no esquema)
# Responsável por (T1):
# - criação de processo: solicita memória, carrega imagem, cria PCB, coloca na fila de prontos
# - finalização de processos: desaloca PCB e memória, retira de filas
# 
# Adições T2a (3 estados):
# - Gerencia fila de bloqueados (blocked_queue) além de prontos (ready_queue)
# - Métodos block_process e unblock_process para transições de estado
#
# FILAS DO ESCALONADOR (conforme diagrama - dentro da área do escalonador):
# - Fila Prontos (ready_queue): processos prontos para executar
# - Fila Bloqueados (blocked_queue): processos aguardando I/O
# ====================================================================================================
class GerenteProcessos:
    def __init__(self, gm, hw, utils):
        self.gm, self.hw, self.utils = gm, hw, utils
        self.ready_queue, self.blocked_queue, self.all_processes = [], [], []
        self.lock = threading.Lock()

    def cria_processo(self, programa, frame_inicial=None):
        if not programa:
            print("Erro: Programa não encontrado.")
            return -1
        if frame_inicial is not None:
            print(f"Tentando alocar programa no frame {frame_inicial}...")
            page_table = self.gm.aloca(len(programa), frame_inicial)
        else:
            print("Alocando programa automaticamente...")
            page_table = self.gm.aloca(len(programa))
        if page_table is None:
            print("Erro: Não há memória suficiente para criar o processo.")
            return -1
        processo_id = page_table[0]
        if self._find_pcb(processo_id) is not None:
            print(f"ERRO INTERNO: Processo com ID {processo_id} já existe!")
            self.gm.desaloca(page_table)
            return -1
        pcb = PCB(page_table)
        with self.lock:
            self.all_processes.append(pcb)
            self.ready_queue.append(pcb)
        self._load_program_to_memory(programa, pcb.page_table)
        print(f"Processo criado:")
        print(f"  ID (Frame inicial): {pcb.id}")
        print(f"  Número sequencial: {pcb.processo_number}")
        print(f"  Frames ocupados: {pcb.page_table}")
        print(f"  Total de processos criados: {PCB.get_processo_count()}")
        return pcb.id

    def _load_program_to_memory(self, program, page_table):
        tam_pg = self.gm.get_tam_pg()
        for i, instruction in enumerate(program):
            pagina, deslocamento = i // tam_pg, i % tam_pg
            frame = page_table[pagina]
            endereco_fisico = (frame * tam_pg) + deslocamento
            self.hw.mem.pos[endereco_fisico] = Word(instruction.opc, instruction.ra, instruction.rb, instruction.p)

    def desaloca_processo(self, proc_id):
        pcb = self._find_pcb(proc_id)
        if pcb:
            with self.lock:
                self.gm.desaloca(pcb.page_table)
                if pcb in self.ready_queue: self.ready_queue.remove(pcb)
                if pcb in self.blocked_queue: self.blocked_queue.remove(pcb)
                self.all_processes.remove(pcb)
            print(f"Processo {proc_id} (frame {proc_id}) removido.")
        else: print(f"Erro: Processo com ID {proc_id} não encontrado.")
    
    def get_next_ready(self):
        with self.lock:
            return self.ready_queue.pop(0) if self.ready_queue else None
    
    def add_ready(self, pcb):
        with self.lock:
            if pcb not in self.ready_queue:
                pcb.state = PCB.ProcessState.READY
                self.ready_queue.append(pcb)
    
    def block_process(self, pcb):
        with self.lock:
            pcb.state = PCB.ProcessState.BLOCKED
            if pcb in self.ready_queue: self.ready_queue.remove(pcb)
            if pcb not in self.blocked_queue: self.blocked_queue.append(pcb)
    
    def unblock_process(self, proc_id):
        with self.lock:
            for pcb in self.blocked_queue:
                if pcb.id == proc_id:
                    self.blocked_queue.remove(pcb)
                    pcb.state = PCB.ProcessState.READY
                    self.ready_queue.append(pcb)
                    print(f"[GP] Processo {proc_id} desbloqueado e pronto para executar")
                    return True
            return False

    def list_all_processes(self):
        print("=" * 70)
        print("LISTA DE PROCESSOS")
        print("=" * 70)
        with self.lock:
            if not self.all_processes: print("Nenhum processo no sistema.")
            else:
                print(f"{'ID':<3} {'Seq':<4} {'Estado':<8} {'PC':<3} {'Frames':<20} {'Endereços Físicos'}")
                print("-" * 70)
                for pcb in sorted(self.all_processes, key=lambda p: p.id):
                    frames_str = str(pcb.page_table)
                    tam_pg = self.gm.get_tam_pg()
                    inicio_fisico = pcb.page_table[0] * tam_pg
                    fim_fisico = pcb.page_table[-1] * tam_pg + tam_pg - 1
                    enderecos_str = f"{inicio_fisico}-{fim_fisico}"
                    print(f"{pcb.id:<3} {pcb.processo_number:<4} {pcb.state.name:<8} {pcb.pc:<3} {frames_str:<20} {enderecos_str}")
            print("-" * 70)
            print(f"Total de processos ativos: {len(self.all_processes)}")
            print(f"Total de processos criados: {PCB.get_processo_count()}")
        print("=" * 70)

    def dump_processo(self, proc_id):
        pcb = self._find_pcb(proc_id)
        if not pcb: print(f"Erro: Processo com ID {proc_id} não encontrado."); return
        print(f"\n{'='*50}")
        print(f"DUMP Processo ID: {pcb.id} (Processo #{pcb.processo_number})")
        print(f"{'='*50}")
        print(f"Estado: {pcb.state.name}")
        print(f"PC: {pcb.pc}")
        print(f"Registradores: {pcb.registers}")
        print(f"Tabela de Páginas: {pcb.page_table}")
        tam_pg = self.gm.get_tam_pg()
        print(f"\nMapeamento Lógico → Físico:")
        for i, frame in enumerate(pcb.page_table):
            log_inicio, log_fim = i * tam_pg, i * tam_pg + tam_pg - 1
            fis_inicio, fis_fim = frame * tam_pg, frame * tam_pg + tam_pg - 1
            print(f"  Página {i}: Lógico {log_inicio:3d}-{log_fim:3d} → Frame {frame} → Físico {fis_inicio:4d}-{fis_fim:4d}")
        print(f"\nConteúdo da Memória do Processo:")
        print("-" * 50)
        logical_size = len(pcb.page_table) * tam_pg
        for log_addr in range(min(logical_size, len(pcb.page_table) * tam_pg)):
            page, offset = log_addr // tam_pg, log_addr % tam_pg
            if page >= len(pcb.page_table): continue
            frame = pcb.page_table[page]
            phys_addr = (frame * tam_pg) + offset
            if phys_addr < len(self.hw.mem.pos):
                word = self.hw.mem.pos[phys_addr]
                print(f"Log.{log_addr:03d} → Fis.{phys_addr:04d}: ", end="")
                self.utils.dump(word)
        print("=" * 50)

    def _find_pcb(self, proc_id):
        for pcb in self.all_processes:
            if pcb.id == proc_id: return pcb
        return None
    
    def estatisticas(self):
        print(f"\n=== ESTATÍSTICAS DE PROCESSOS ===")
        with self.lock:
            print(f"Processos ativos: {len(self.all_processes)}")
            print(f"Processos na fila de prontos: {len(self.ready_queue)}")
            print(f"Processos bloqueados: {len(self.blocked_queue)}")
            print(f"Total de processos criados: {PCB.get_processo_count()}")
            if self.all_processes:
                estados = {}
                for pcb in self.all_processes:
                    estado = pcb.state.name
                    estados[estado] = estados.get(estado, 0) + 1
                print(f"Distribuição por estado:")
                for estado, count in estados.items(): print(f"  {estado}: {count}")
        print("=" * 35)

# ====================================================================================================
# THREAD CONSOLE (T2a - conforme esquema "Thread Console" no diagrama)
# Thread concorrente que processa requisições de I/O
# Funcionalidade (conforme diagrama):
# - loop sempre: aguarda pedido na fila
# - pega pedido da fila (consumidor - fila de pedidos Console)
# - se leitura: lê do usuario e escreve na memória no endereço fornecido (DMA)
# - se escrita: lê da memória e escreve no console
# - interrompe CPU: sinaliza conclusão via irpt_io_complete
#
# FILA PEDIDOS CONSOLE (conforme diagrama - dentro da caixa "Thread Console")
# Fila thread-safe para requisições de I/O (produtor/consumidor)
# ====================================================================================================
class IODevice(threading.Thread):
    # Classe interna: Fila de pedidos de I/O (conforme "Fila Pedidos Console" no diagrama)
    class SimpleQueue:
        """
        Fila thread-safe para comunicação entre threads.
        Implementa o padrão produtor/consumidor para requisições de I/O.
        Localizada na Thread Console conforme esquema do SO multithreaded.
        """
        def __init__(self):
            self.items = []
            self.lock = threading.Lock()
            self.not_empty = threading.Condition(self.lock)
        
        def put(self, item):
            """Adiciona item na fila (produtor - SystemCall)"""
            with self.lock:
                self.items.append(item)
                self.not_empty.notify()
        
        def get(self, timeout=None):
            """Remove e retorna item da fila (consumidor - IODevice). Bloqueia se vazia."""
            with self.not_empty:
                if timeout is None:
                    while not self.items:
                        self.not_empty.wait()
                    return self.items.pop(0)
                else:
                    # Com timeout
                    if not self.not_empty.wait_for(lambda: len(self.items) > 0, timeout):
                        raise QueueEmpty()
                    return self.items.pop(0)
        
        def qsize(self):
            """Retorna tamanho aproximado da fila (para debug)"""
            with self.lock:
                return len(self.items)
    
    def __init__(self, hw, gp, ih):
        super().__init__(daemon=True, name="IODevice")
        self.hw, self.gp, self.ih = hw, gp, ih
        # Fila Pedidos Console (interna à Thread Console - conforme diagrama)
        self.io_queue = IODevice.SimpleQueue()
        self.running, self.io_delay = True, 2.0

    def run(self):
        print("[I/O Device] Dispositivo iniciado e aguardando requisições...")
        while self.running:
            try:
                request = self.io_queue.get(timeout=0.5)
                print(f"[I/O Device] Processando {request.operation} para processo {request.process_id}...")
                time.sleep(self.io_delay)
                # T2 - sinaliza e manda pro handler:
                self._process_io(request)
                self.hw.cpu.irpt_io_complete = request.process_id
                self.hw.cpu.irpt = Interrupts.INT_IO_COMPLETE
                self.ih.handle(Interrupts.INT_IO_COMPLETE, pc=self.hw.cpu.pc)  # dispara a rotina agora
                print(f"[I/O Device] {request.operation} concluído para processo {request.process_id}")
            except QueueEmpty: continue
            except Exception as e: print(f"[I/O Device] Erro ao processar I/O: {e}")
    
    def _process_io(self, request):
        tam_pg = self.gp.gm.get_tam_pg()
        page, offset = request.address // tam_pg, request.address % tam_pg
        if page >= len(request.pcb.page_table):
            print(f"[I/O Device] ERRO: Endereço inválido {request.address}")
            return
        frame = request.pcb.page_table[page]
        physical_addr = (frame * tam_pg) + offset
        if request.operation == 'READ':
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
            value = self.hw.mem.pos[physical_addr].p
            print(f"\n{'='*50}")
            print(f"[I/O Device] SAÍDA do processo {request.process_id}: {value}")
            print(f"{'='*50}\n")
    
    def stop(self):
        self.running = False

# ====================================================================================================
# THREAD CPU + THREAD ESCALONADOR (T2a - conforme esquema do diagrama)
# No diagrama aparecem separadas, aqui estão integradas em uma única thread
# 
# Funcionalidade Thread Escalonador (conforme diagrama):
# - aguarda bloqueado (semaCPU.wait)
# - escolhe processo da fila de prontos
# - restaura contexto na CPU
# - libera CPU (semaCPU.notify)
#
# Funcionalidade Thread CPU (conforme diagrama):
# - loop: busca e executa instrução
# - se completou número de instruções no ciclo: liga int timer (quantum)
# - se tem interrupção: desvia para rotina de tratamento
# ====================================================================================================
class CPUThread(threading.Thread):
    def __init__(self, cpu, escalonador, semaphore):
        super().__init__(daemon=True, name="CPU")
        self.cpu, self.escalonador, self.semaphore = cpu, escalonador, semaphore
        self.running = True

    def run(self):
        print("[CPU Thread] CPU iniciada e aguardando processos...")
        while self.running:
            self.semaphore.acquire()
            if not self.running: break
            pcb = self.escalonador.gp.get_next_ready()
            if pcb is None: continue
            print(f"\n[CPU Thread] Executando processo {pcb.id} (Quantum: {self.escalonador.quantum})")
            self.cpu.set_context(pcb)
            self.cpu.run(self.escalonador.quantum)
            if pcb.state == PCB.ProcessState.FINISHED:
                print(f"[CPU Thread] Processo {pcb.id} FINALIZOU")
                self.escalonador.gp.desaloca_processo(pcb.id)
            elif pcb.state == PCB.ProcessState.BLOCKED:
                print(f"[CPU Thread] Processo {pcb.id} BLOQUEADO (aguardando I/O)")
            elif pcb.state == PCB.ProcessState.READY:
                print(f"[CPU Thread] Processo {pcb.id} teve quantum expirado")
                self.escalonador.gp.add_ready(pcb)
    
    def stop(self):
        self.running = False
        self.semaphore.release()

class Escalonador:
    def __init__(self, cpu, gp, quantum):
        self.cpu, self.gp, self.quantum = cpu, gp, quantum
        self.semaphore = threading.Semaphore(0)
        self.cpu_thread, self.scheduler_thread = None, None
        self.running = False

    def start(self):
        if self.running:
            print("Escalonador já está rodando!")
            return
        self.running = True
        self.cpu_thread = CPUThread(self.cpu, self, self.semaphore)
        self.cpu_thread.start()
        self.scheduler_thread = threading.Thread(target=self._scheduler_loop, daemon=True, name="Scheduler")
        self.scheduler_thread.start()
        print("[Escalonador] Sistema de escalonamento iniciado!")
    
    def _scheduler_loop(self):
        while self.running:
            if self.gp.ready_queue: self.semaphore.release()
            time.sleep(0.01)
    
    def stop(self):
        self.running = False
        if self.cpu_thread: self.cpu_thread.stop()
        print("[Escalonador] Sistema de escalonamento encerrado!")

# ====================================================================================================
# ROTINAS DE TRATAMENTO DE INTERRUPÇÕES (conforme esquema do diagrama)
# 
# Rot Trat STOP, overflow, Acesso indevido:
# - Tratamento de STOP, overflow, endereço inválido
# - Finaliza processo, libera escalonador
#
# Rot Trat TIMER:
# - Salva estado do processo
# - Coloca na fila de prontos
# - Libera escalonador (semaSch.notify)
#
# Rot Trat Ret IO (T2a - nova interrupção):
# - Passa processo de bloqueado para pronto
# - Retorna e continua processo (avança PC!)
# ====================================================================================================
#parteT2 - atualizado
class InterruptHandling:
    def __init__(self, cpu, gp):
        self.cpu = cpu
        self.gp = gp

    def handle(self, irpt, pc):
        print(f"      INTERRUPCAO {irpt.name} (PC Logico: {pc})")
        if irpt == Interrupts.INT_IO_COMPLETE:
            pid = self.cpu.irpt_io_complete
            if pid is not None:
                # 1) pegue o PCB do processo que acabou o I/O
                pcb = self.gp._find_pcb(pid)
                if pcb is not None:
                    # 2) avance o PC para “pular” o SYSCALL que bloqueou
                    pcb.pc += 1
                # 3) devolva o processo para READY
                self.gp.unblock_process(pid)
            # 4) limpe os registradores de interrupção
            self.cpu.irpt_io_complete = None
            self.cpu.irpt = Interrupts.NO_INTERRUPT
            return

        if irpt in [Interrupts.INT_ENDERECO_INVALIDO,
                    Interrupts.INT_INSTRUCAO_INVALIDA,
                    Interrupts.INT_OVERFLOW]:
            if self.cpu.running_process:
                self.cpu.running_process.state = PCB.ProcessState.FINISHED


# ====================================================================================================
# CHAMADA IO / SYSTEM CALL (conforme esquema "Chamada IO" no diagrama)
# Funcionalidade (conforme diagrama):
# - Salva estado do processo
# - Bloqueia processo (passa de RUNNING para BLOCKED)
# - Empacota pedido para console (adiciona na fila de pedidos)
# - Libera escalonador (semaSch.notify) - CPU para, volta para escalonador
# ====================================================================================================
#parteT2 - atualizado
class SysCallHandling:
    def __init__(self, hw, io_device, gp):
        self.hw = hw
        self.io_device = io_device  # Referência à Thread Console (que contém a fila)
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
        
        # Produtor: coloca requisição na fila do dispositivo (Thread Console)
        self.io_device.io_queue.put(request)
        
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
        
        # Produtor: coloca requisição na fila (Thread Console)
        self.io_device.io_queue.put(request)
        
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
        self.utils = Utilities()
        self.utils.hw = hw
        self.gm = GerenteMemoria(len(hw.mem.pos), tam_pg)
        self.gp = GerenteProcessos(self.gm, hw, self.utils)
        self.ih = InterruptHandling(hw.cpu, self.gp)
        
        # IMPORTANTE: IODevice (Thread Console) deve ser criado antes de SysCallHandling
        # pois contém a "Fila Pedidos Console" (conforme diagrama)
        self.io_device = IODevice(hw, self.gp, self.ih)
        
        # Handlers com acesso ao dispositivo de I/O (que contém a fila)
        self.sc = SysCallHandling(hw, self.io_device, self.gp)
        
        # Escalonador
        self.escalonador = Escalonador(hw.cpu, self.gp, quantum)
        
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

# ====================================================================================================
# SISTEMA PRINCIPAL - Integração de todos os componentes (HW + SO)
# Implementa THREAD SHELL (conforme esquema "Thread Shell" no diagrama)
# 
# Thread Shell (conforme diagrama):
# - loop: lê entrada do usuário
# - submete pedido ao SO (cria processo, remove, lista, etc.)
# - ou manda comando para console (escolhe responder I/O quando Thread Console pede entrada)
# 
# Usuário (conforme diagrama):
# - Fornece nome de programa a executar
# - Escolhe responder I/O quando requisitado
# - Fica esperando resposta de pedido de IN
# ====================================================================================================
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
                    print(f"Requisições de I/O pendentes: {self.so.io_device.io_queue.qsize()}")
                
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