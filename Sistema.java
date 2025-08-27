// PUCRS - Escola Politécnica - Sistemas Operacionais
// Prof. Fernando Dotti
// Código fornecido como parte da solução do projeto de Sistemas Operacionais
//
// Estrutura deste código:
//    Sistema modularizado em pacotes separados:
//           hardware - Memory, Word, CPU, HW
//           software - InterruptHandling, SysCallHandling, Utilities, SO
//           programs - Program, Programs
//           common - Opcode, Interrupts
//    Veja o main.  Ele instancia o Sistema com os elementos mencionados acima.
//           em seguida solicita a execução de algum programa com  loadAndExec

import hardware.HW;
import software.SO;
import programs.Programs;

public class Sistema {

	// ------------------- S I S T E M A
	// --------------------------------------------------------------------

	public HW hw;
	public SO so;
	public Programs progs;

	public Sistema(int tamMem) {
		hw = new HW(tamMem);           // memoria do HW tem tamMem palavras
		so = new SO(hw);
		hw.cpu.setUtilities(so.utils); // permite cpu fazer dump de memoria ao avancar
		progs = new Programs();
	}

	public void run() {

		so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));

		// so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
		// so.utils.loadAndExec(progs.retrieveProgram("fibonacci10"));
		// fibonacci10v2,
		// progMinimo,
		// fatorialWRITE, // saida
		// fibonacciREAD, // entrada
		// PB
		// PC, // bubble sort
	}
	// ------------------- S I S T E M A - fim
	// --------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------

	// -------------------------------------------------------------------------------------------------------
	// ------------------- instancia e testa sistema
	public static void main(String args[]) {
		Sistema s = new Sistema(1024);
		s.run();
	}
}