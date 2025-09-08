package hardware;

import memory.MemoryManagerPonte;

public class HW {
		public Memory mem;
		public CPU cpu;

		public HW(int tamMem, MemoryManagerPonte mm) {
			mem = new Memory(tamMem);
			// A CPU agora recebe o gerenciador de memória para traduzir endereços
			cpu = new CPU(mem, mm, true); // true liga debug

			// Conecta a ponte do gerenciador de memória à memória real do hardware.
			// Isso garante que o gerenciador e a CPU operem na mesma memória.
			if (mm != null) {
				mm.setHwMemory(mem);
			}
		}
	}
