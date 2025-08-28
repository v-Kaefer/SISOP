package pucrs.sisop.software;

import pucrs.sisop.hardware.HW;
import pucrs.sisop.hardware.Opcode;
import pucrs.sisop.hardware.Interrupts;
import pucrs.sisop.hardware.Memory;
import pucrs.sisop.hardware.Word;
public class InterruptHandling {
		private HW hw; // referencia ao hw se tiver que setar algo

		public InterruptHandling(HW _hw) {
			hw = _hw;
		}

		public void handle(Interrupts irpt) {
			// apenas avisa - todas interrupcoes neste momento finalizam o programa
			System.out.println(
					"                                               Interrupcao " + irpt + "   pc: " + hw.cpu.pc);
		}
	}
