import math
from enum import Enum

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
    NO_INTERRUPT, INT_ENDERECO_INVALIDO, INT_INSTRUCAO_INVALIDA, INT_OVERFLOW = range(4)

class Word:
    def __init__(self, opc, ra, rb, p):
        self.opc = opc
        self.ra = ra
        self.rb = rb
        self.p = p

class Memory:
    def __init__(self, size):
        self.pos = [Word(Opcode.___, -1, -1, -1) for _ in range(size)]

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
            elif opc == Opcode.SYSCALL: self.sys_call.handle(); self.pc += 1
            elif opc == Opcode.STOP: self.sys_call.stop(); self.cpu_stop = True
            else: self.irpt = Interrupts.INT_INSTRUCAO_INVALIDA

            if self.irpt != Interrupts.NO_INTERRUPT: self.ih.handle(self.irpt, self.pc); self.cpu_stop = True
            
            self.instructions_executed += 1
        
        if self.running_process and self.running_process.state != PCB.ProcessState.FINISHED:
            self.running_process.pc = self.pc
            self.running_process.registers = self.reg.copy()
            self.running_process.state = PCB.ProcessState.READY

class HW:
    def __init__(self, tam_mem):
        self.mem = Memory(tam_mem)
        self.cpu = CPU(self.mem, debug=False)

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

class GerenteProcessos:
    def __init__(self, gm, hw, utils):
        self.gm, self.hw, self.utils = gm, hw, utils
        self.ready_queue, self.all_processes = [], []

    def cria_processo(self, programa, frame_inicial=None):
        """
        Cria um processo com ID baseado no frame inicial
        
        Args:
            programa: Lista de instruções do programa
            frame_inicial: Frame específico onde alocar (opcional)
        """
        if not programa:
            print("Erro: Programa não encontrado.")
            return -1
        
        # Tentar alocar memória
        if frame_inicial is not None:
            print(f"Tentando alocar programa no frame {frame_inicial}...")
            page_table = self.gm.aloca(len(programa), frame_inicial)
        else:
            print("Alocando programa automaticamente...")
            page_table = self.gm.aloca(len(programa))
        
        if page_table is None:
            print("Erro: Não há memória suficiente para criar o processo.")
            return -1
        
        # Verificar se já existe um processo com este ID (frame inicial)
        processo_id = page_table[0]
        if self._find_pcb(processo_id) is not None:
            print(f"ERRO INTERNO: Processo com ID {processo_id} já existe!")
            self.gm.desaloca(page_table)
            return -1
        
        pcb = PCB(page_table)
        self.all_processes.append(pcb)
        self._load_program_to_memory(programa, pcb.page_table)
        self.ready_queue.append(pcb)
        
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
            self.gm.desaloca(pcb.page_table)
            if pcb in self.ready_queue: 
                self.ready_queue.remove(pcb)
            self.all_processes.remove(pcb)
            print(f"Processo {proc_id} (frame {proc_id}) removido.")
        else: 
            print(f"Erro: Processo com ID {proc_id} não encontrado.")
    
    def get_next_ready(self): 
        return self.ready_queue.pop(0) if self.ready_queue else None
    
    def add_ready(self, pcb): 
        self.ready_queue.append(pcb)

    def list_all_processes(self):
        print("=" * 70)
        print("LISTA DE PROCESSOS")
        print("=" * 70)
        if not self.all_processes: 
            print("Nenhum processo no sistema.")
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
        if not pcb: 
            print(f"Erro: Processo com ID {proc_id} não encontrado.")
            return
        
        print(f"\n{'='*50}")
        print(f"DUMP Processo ID: {pcb.id} (Processo #{pcb.processo_number})")
        print(f"{'='*50}")
        print(f"Estado: {pcb.state.name}")
        print(f"PC: {pcb.pc}")
        print(f"Registradores: {pcb.registers}")
        print(f"Tabela de Páginas: {pcb.page_table}")
        
        # Mostrar mapeamento detalhado
        tam_pg = self.gm.get_tam_pg()
        print(f"\nMapeamento Lógico → Físico:")
        for i, frame in enumerate(pcb.page_table):
            log_inicio = i * tam_pg
            log_fim = log_inicio + tam_pg - 1
            fis_inicio = frame * tam_pg
            fis_fim = fis_inicio + tam_pg - 1
            print(f"  Página {i}: Lógico {log_inicio:3d}-{log_fim:3d} → Frame {frame} → Físico {fis_inicio:4d}-{fis_fim:4d}")
        
        print(f"\nConteúdo da Memória do Processo:")
        print("-" * 50)
        logical_size = len(pcb.page_table) * tam_pg
        for log_addr in range(min(logical_size, len(pcb.page_table) * tam_pg)):
            page, offset = log_addr // tam_pg, log_addr % tam_pg
            if page >= len(pcb.page_table): 
                continue
            frame = pcb.page_table[page]
            phys_addr = (frame * tam_pg) + offset
            if phys_addr < len(self.hw.mem.pos):
                word = self.hw.mem.pos[phys_addr]
                print(f"Log.{log_addr:03d} → Fis.{phys_addr:04d}: ", end="")
                self.utils.dump(word)
        print("=" * 50)

    def _find_pcb(self, proc_id):
        for pcb in self.all_processes:
            if pcb.id == proc_id: 
                return pcb
        return None
    
    def estatisticas(self):
        """Mostra estatísticas dos processos"""
        print(f"\n=== ESTATÍSTICAS DE PROCESSOS ===")
        print(f"Processos ativos: {len(self.all_processes)}")
        print(f"Processos na fila de prontos: {len(self.ready_queue)}")
        print(f"Total de processos criados: {PCB.get_processo_count()}")
        
        if self.all_processes:
            estados = {}
            for pcb in self.all_processes:
                estado = pcb.state.name
                estados[estado] = estados.get(estado, 0) + 1
            
            print(f"Distribuição por estado:")
            for estado, count in estados.items():
                print(f"  {estado}: {count}")
        print("=" * 35)

class Escalonador:
    def __init__(self, cpu, gp, quantum):
        self.cpu, self.gp, self.quantum = cpu, gp, quantum
        self.running = None

    def run_all(self):
        print("\n--- Iniciando execucao escalonada ---")
        self.running = self.gp.get_next_ready()
        while self.running:
            print(f"\nExecutando processo ID: {self.running.id} (Quantum: {self.quantum})")
            self.cpu.set_context(self.running)
            self.cpu.run(self.quantum)

            if self.running.state == PCB.ProcessState.FINISHED:
                print(f"Processo {self.running.id} terminou.")
                self.gp.desaloca_processo(self.running.id)
            else:
                self.gp.add_ready(self.running)
            self.running = self.gp.get_next_ready()
        print("\n--- Fila de prontos vazia. Execucao escalonada encerrada ---")

class InterruptHandling:
    def __init__(self, cpu):
        self.cpu = cpu

    def handle(self, irpt, pc):
        print(f"      INTERRUPCAO {irpt.name} (PC Logico: {pc})")
        if irpt in [Interrupts.INT_ENDERECO_INVALIDO, Interrupts.INT_INSTRUCAO_INVALIDA, Interrupts.INT_OVERFLOW]:
            if self.cpu.running_process:
                self.cpu.running_process.state = PCB.ProcessState.FINISHED

class SysCallHandling:
    def __init__(self, hw):
        self.hw = hw

    def stop(self):
        print("      SYSCALL: STOP")
        if self.hw.cpu.running_process:
            self.hw.cpu.running_process.state = PCB.ProcessState.FINISHED

    def handle(self):
        if self.hw.cpu.reg[8] == 2: # WRITE
            addr = self.hw.cpu._translate_address(self.hw.cpu.reg[9])
            if addr != -1: print(f"      SYSCALL: WRITE, CONTEUDO: {self.hw.mem.pos[addr].p}")

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

class SO:
    def __init__(self, hw, tam_pg, quantum):
        self.hw, self.tam_pg, self.quantum = hw, tam_pg, quantum
        self.ih = InterruptHandling(hw.cpu)
        self.sc = SysCallHandling(hw)
        self.utils = Utilities()
        self.utils.hw = hw
        self.gm = GerenteMemoria(len(hw.mem.pos), tam_pg)
        self.gp = GerenteProcessos(self.gm, hw, self.utils)
        self.escalonador = Escalonador(hw.cpu, self.gp, quantum)
        hw.cpu.set_address_of_handlers(self.ih, self.sc)
        hw.cpu.set_gerente_memoria(self.gm)
        hw.cpu.set_utilities(self.utils)

    def executa_processo(self, proc_id):
        pcb = self.gp._find_pcb(proc_id)
        if not pcb: print(f"Erro: Processo com ID {proc_id} nao encontrado."); return
        if pcb.state != PCB.ProcessState.READY: print(f"Erro: Processo {proc_id} nao esta no estado READY."); return

        print(f"\n--- Executando processo unico ID: {pcb.id} ---")
        if pcb in self.gp.ready_queue:
            self.gp.ready_queue.remove(pcb)
        
        self.hw.cpu.set_context(pcb)
        self.hw.cpu.run(quantum=-1)

        if pcb.state == PCB.ProcessState.FINISHED:
            print(f"Processo {pcb.id} terminou.")
            self.gp.desaloca_processo(pcb.id)
        elif pcb.state == PCB.ProcessState.READY:
            print(f"Processo {pcb.id} pausado, retornando a fila de prontos.")
            self.gp.add_ready(pcb)
        print("--- Fim da execucao unica ---")

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

class Sistema:
    def __init__(self, tam_mem, tam_pg, quantum):
        self.hw = HW(tam_mem)
        self.so = SO(self.hw, tam_pg, quantum)
        self.progs = Programs()
    
    def run(self):
        print("=" * 60)
        print("SISTEMA OPERACIONAL COM ID BASEADO EM LOCALIZAÇÃO")
        print("=" * 60)
        print("Comandos disponíveis:")
        print("  new <programa> [frame]  - Criar processo")
        print("  rm <id>                 - Remover processo") 
        print("  ps                      - Listar processos")
        print("  exec <id>               - Executar processo")
        print("  execall                 - Executar todos processos")
        print("  dump <id>               - Dump detalhado do processo")
        print("  dumpm <inicio> <fim>    - Dump da memória física")
        print("  memstat                 - Status da memória")
        print("  stats                   - Estatísticas de processos")
        print("  traceon/traceoff        - Controle de debug")
        print("  exit                    - Sair")
        print("=" * 60)
        print("NOTA: O ID do processo corresponde ao frame inicial na memória")
        print("=" * 60)
        
        while True:
            try:
                cmd_line = input(f"[Mem:{self.so.gm.free_frames.count(True)}/{self.so.gm.num_frames}] > ").strip().split()
                if not cmd_line: continue
                cmd = cmd_line[0].lower()

                if cmd == "new":
                    if len(cmd_line) >= 2:
                        programa_nome = cmd_line[1]
                        programa = self.progs.retrieve_program(programa_nome)
                        
                        if programa is None:
                            print(f"Erro: Programa '{programa_nome}' não encontrado.")
                            print("Programas disponíveis:", [p.name for p in self.progs.progs])
                            continue
                        
                        if len(cmd_line) >= 3:
                            # new programa frame_inicial
                            try:
                                frame_inicial = int(cmd_line[2])
                                self.so.gp.cria_processo(programa, frame_inicial)
                            except ValueError:
                                print("Erro: Frame deve ser um número inteiro")
                        else:
                            # new programa (alocação automática)
                            self.so.gp.cria_processo(programa)
                    else: 
                        print("Uso: new <nomePrograma> [frame_inicial]")
                        print("Programas disponíveis:", [p.name for p in self.progs.progs])
                        
                elif cmd == "memstat":
                    self.so.gm.mostrar_status()
                
                elif cmd == "stats":
                    self.so.gp.estatisticas()
                    
                elif cmd == "rm":
                    if len(cmd_line) > 1: 
                        self.so.gp.desaloca_processo(int(cmd_line[1]))
                    else: 
                        print("Uso: rm <id>")
                        
                elif cmd == "ps":
                    self.so.gp.list_all_processes()
                    
                elif cmd == "exec":
                    if len(cmd_line) > 1: 
                        self.so.executa_processo(int(cmd_line[1]))
                    else: 
                        print("Uso: exec <id>")
                        
                elif cmd == "execall":
                    self.so.escalonador.run_all()
                    
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
                        
                elif cmd == "traceon":
                    self.hw.cpu.debug = True
                    print("Modo trace ativado.")
                    
                elif cmd == "traceoff":
                    self.hw.cpu.debug = False
                    print("Modo trace desativado.")
                    
                elif cmd == "exit":
                    break
                    
                else:
                    print(f"Comando desconhecido: '{cmd}'")
                    
            except (ValueError, IndexError):
                print("Argumento inválido ou ausente para o comando.")
            except (EOFError, KeyboardInterrupt):
                break

        print("\n" + "=" * 60)
        print("SISTEMA ENCERRADO")
        print(f"Total de processos criados durante a sessão: {PCB.get_processo_count()}")
        print("=" * 60)

if __name__ == "__main__":
    s = Sistema(tam_mem=1024, tam_pg=16, quantum=5)
    s.run()