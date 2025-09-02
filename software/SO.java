package software;

import hardware.HW;
import software.InterruptHandling;
import software.SysCallHandling;
public class SO {
		public InterruptHandling ih;
		public SysCallHandling sc;
		public Utilities utils;

		public SO(HW hw) {
			ih = new InterruptHandling(hw); // rotinas de tratamento de int
			sc = new SysCallHandling(hw); // chamadas de sistema
			hw.cpu.setAddressOfHandlers(ih, sc);
			utils = new Utilities(hw);
		}
	}
