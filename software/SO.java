package software;

import hardware.HW;
public class SO {
		public InterruptHandling ih;
		public SysCallHandling sc;
		public Utilities utils;

		public SO(HW hw) {
			ih = new InterruptHandling(hw); // rotinas de tratamento de int
			sc = new SysCallHandling(hw); // chamadas de sistema
			// A linha abaixo foi removida pois a CPU agora é controlada pelo ProcessManager
			// hw.cpu.setAddressOfHandlers(ih, sc);
			utils = new Utilities(hw);
		}
	}
