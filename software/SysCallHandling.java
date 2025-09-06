package software;

import hardware.HW;
import hardware.Opcode;
import hardware.Interrupts;
import hardware.Memory;
import hardware.Word;
public class SysCallHandling {
		private HW hw; // referencia ao hw se tiver que setar algo

		public SysCallHandling(HW _hw) {
			hw = _hw;
		}

		public void stop() { // chamada de sistema indicando final de programa
							 // nesta versao cpu simplesmente pára
			System.out.println("                                               SYSCALL STOP");
		}

		public void handle() { // chamada de sistema 
			                   // suporta somente IO, com parametros 
							   // reg[8] = in ou out    e reg[9] endereco do inteiro
			System.out.println("SYSCALL pars:  " + hw.cpu.getReg(8) + " / " + hw.cpu.getReg(9));

			if  (hw.cpu.getReg(8)==1){
				  // leitura ...

			} else if (hw.cpu.getReg(8)==2){
				  // escrita - escreve o conteuodo da memoria na posicao dada em reg[9]
				  System.out.println("OUT:   "+ hw.mem.pos[hw.cpu.getReg(8)].p);
			} else {System.out.println("  PARAMETRO INVALIDO"); }		
		}
	}
