package hardware;

import memory.MemoryManagerPonte;
import software.Utilities;

public class CPU {
	private int maxInt;
	private int minInt;

	// CONTEXTO DA CPU
	private int pc;
	private Word ir;
	private int[] reg;
	private Interrupts interrupt;
	private boolean traceMode;

	// REFERÊNCIAS DE HARDWARE E SOFTWARE
	private final Word[] m; // Memória física
	private MemoryManagerPonte memoryManager; // Ponte para o gerenciador de memória
	private Utilities u;

	public CPU(Memory _mem, MemoryManagerPonte _mm, boolean _trace) {
		maxInt = 32767;
		minInt = -32767;
		m = _mem.pos;
		memoryManager = _mm; // O Gerenciador de Memória agora é parte do contexto da CPU
		reg = new int[10];
		traceMode = _trace;
		interrupt = Interrupts.noInterrupt;
	}

	public void setUtilities(Utilities _u) {
		u = _u;
	}

	// MÉTODOS DE CONTROLE DE CONTEXTO (usados pelo ProcessManager)
	public void setContext(int _pc) {
		this.pc = _pc;
		this.interrupt = Interrupts.noInterrupt;
	}

	public int getPc() { return pc; }
	public int getReg(int i) { return reg[i]; }
	public void setReg(int i, int v) { if (i >= 0 && i < reg.length) reg[i] = v; }
	public Interrupts getInterrupt() { return interrupt; }
	public void setInterrupt(Interrupts i) { this.interrupt = i; }
	public void setTrace(boolean on) { this.traceMode = on; }

	private boolean testOverflow(int v) {
		if ((v < minInt) || (v > maxInt)) {
			interrupt = Interrupts.OVERFLOW;
			return false;
		}
		return true;
	}

	/**
	 * Executa uma única instrução. Este método substitui o antigo loop `run()`.
	 * @param pageTable A tabela de páginas do processo atual, para tradução de endereços.
	 */
	public void runInstruction(int[] pageTable) {
		if (m.length == 0) return;

		// Reseta a interrupção antes de cada instrução
		interrupt = Interrupts.noInterrupt;

		try {
			// 1. FETCH (BUSCA) - Traduz o PC lógico para um endereço físico
			int physicalAddress = memoryManager.traduzirEndereco(pc, pageTable);
			ir = m[physicalAddress];

			if (traceMode) {
				System.out.println("  PC[" + pc + "]: " + ir.opc + " " + ir.ra + "," + ir.rb + "," + ir.p);
			}

			// 2. DECODE & EXECUTE (DECODIFICAÇÃO E EXECUÇÃO)
			switch (ir.opc) {
				case LDI:
					reg[ir.ra] = ir.p;
					break;
				case LDD:
					reg[ir.ra] = m[memoryManager.traduzirEndereco(ir.p, pageTable)].p;
					break;
				case LDX:
					reg[ir.ra] = m[memoryManager.traduzirEndereco(reg[ir.rb], pageTable)].p;
					break;
				case STD:
					m[memoryManager.traduzirEndereco(ir.p, pageTable)].p = reg[ir.ra];
					break;
				case STX:
					m[memoryManager.traduzirEndereco(reg[ir.ra], pageTable)].p = reg[ir.rb];
					break;
				case MOVE:
					reg[ir.ra] = reg[ir.rb];
					break;
				case ADD:
					reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
					testOverflow(reg[ir.ra]);
					break;
				case ADDI:
					reg[ir.ra] = reg[ir.ra] + ir.p;
					testOverflow(reg[ir.ra]);
					break;
				case SUB:
					reg[ir.ra] = reg[ir.ra] - reg[ir.rb];
					testOverflow(reg[ir.ra]);
					break;
				case SUBI:
					reg[ir.ra] = reg[ir.ra] - ir.p;
					testOverflow(reg[ir.ra]);
					break;
				case MULT:
					reg[ir.ra] = reg[ir.ra] * reg[ir.rb];
					testOverflow(reg[ir.ra]);
					break;
				case JMP:
					pc = ir.p;
					return; // Pula o incremento do PC
				case JMPI:
					pc = reg[ir.ra];
					return; // Pula o incremento do PC
				case JMPIE:
					if (reg[ir.rb] == 0) { pc = reg[ir.ra]; return; }
					break;
				case JMPIG:
					if (reg[ir.rb] > 0) { pc = reg[ir.ra]; return; }
					break;
				case JMPIL:
					if (reg[ir.rb] < 0) { pc = reg[ir.ra]; return; }
					break;
				case JMPIGT:
					if (reg[ir.ra] > reg[ir.rb]) { pc = ir.p; return; }
					break;
				case JMPIM:
					pc = m[memoryManager.traduzirEndereco(ir.p, pageTable)].p;
					return;
				case JMPIEM:
					if (reg[ir.rb] == 0) { pc = m[memoryManager.traduzirEndereco(ir.p, pageTable)].p; return; }
					break;
				case JMPIGM:
					if (reg[ir.rb] > 0) { pc = m[memoryManager.traduzirEndereco(ir.p, pageTable)].p; return; }
					break;
				case JMPILM:
					if (reg[ir.rb] < 0) { pc = m[memoryManager.traduzirEndereco(ir.p, pageTable)].p; return; }
					break;
				case JMPIEK:
					if (reg[ir.rb] == 0) { pc = ir.p; return; }
					break;
				case JMPIGK:
					if (reg[ir.rb] > 0) { pc = ir.p; return; }
					break;
				case JMPILK:
					if (reg[ir.rb] < 0) { pc = ir.p; return; }
					break;
				case STOP:
					interrupt = Interrupts.END;
					break;
				case SYSCALL:
					interrupt = Interrupts.SYSCALL;
					break;
				case DATA:
				default:
					interrupt = Interrupts.INVALID_INSTRUCTION;
					break;
			}

			// 3. INCREMENT PC
			if (interrupt == Interrupts.noInterrupt) {
				pc++;
			}

		} catch (IndexOutOfBoundsException e) {
			// Ocorre se traduzeEndereco falhar (acesso a página inválida)
			interrupt = Interrupts.INVALID_ADDRESS;
			if (traceMode) System.out.println(" -> INVALID_ADDRESS");
		}
	}

	/**
	 * O método run() original foi descontinuado em favor de runInstruction(),
	 * que permite ao SO ter controle sobre a execução.
	 * Mantido aqui para referência ou compatibilidade com testes antigos.
	 */
	public void run() {
		System.out.println("AVISO: O método CPU.run() é obsoleto. Use o ProcessManager para executar ciclos do SO.");
		// A lógica de execução agora está em runInstruction().
	}
}