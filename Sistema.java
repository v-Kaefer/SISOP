import hardware.HW;
import software.SO;
import programs.Programs;

public class Sistema {
    public HW hw;
    public SO so;
    public Programs progs;

    public Sistema(int tamMem) {
        hw = new HW(tamMem);
        so = new SO(hw);
        hw.cpu.setUtilities(so.utils);
        progs = new Programs();
    }

    public void run() {

		so.utils.loadAndExec(progs.retrieveProgram("fatorialV2"));

		// so.utils.loadAndExec(progs.retrieveProgram("fatorial"));
		// fibonacci10,
		// fibonacci10v2,
		// progMinimo,
		// fatorialWRITE, // saida
		// fibonacciREAD, // entrada
		// PB
		// PC, // bubble sort
	}

    public static void main(String[] args) {
        Sistema s = new Sistema(1024);
        s.run();
    }
}