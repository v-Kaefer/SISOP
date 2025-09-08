package software;

import hardware.HW;
import memory.MemoryManagerPonte;
import programs.Program;
import programs.Programs;

import java.util.Arrays;
import java.util.Scanner;

/**
 * Shell interativo para o sistema operacional SISOP.
 * Lê comandos do usuário e interage com o ProcessManager.
 */
public class InteractiveShell {

    private static ProcessManager pm;
    private static Programs programs;

    public static void main(String[] args) {
        // --- Inicialização do Sistema Operacional ---
        // MemoryManagerPonte deve ser criado antes do HW para resolver a dependência da CPU
        MemoryManagerPonte mm = new MemoryManagerPonte(1024, 8);
        HW hardware = new HW(1024, mm);
        pm = new ProcessManager(hardware, mm); // Agora o hardware já contém uma CPU ciente da memória
        programs = new Programs(); // Biblioteca de programas

        Scanner scanner = new Scanner(System.in);
        System.out.println("--- Sistema Operacional Interativo SISOP ---");
        System.out.println("Comandos: new, rm, ps, dump, dumpm, run, setscheduler, traceon, traceoff, exit");

        // --- Loop de Comandos ---
        while (true) {
            System.out.print("> ");
            String[] line = scanner.nextLine().trim().split("\\s+");
            String cmd = line[0].toLowerCase();

            if (cmd.isEmpty()) continue;

            switch (cmd) {
                case "new":
                    handleNew(line);
                    break;
                case "rm":
                    handleRm(line);
                    break;
                case "ps":
                    handlePs();
                    break;
                case "dump":
                    handleDump(line);
                    break;
                case "dumpm":
                    handleDumpM(line);
                    break;
                case "run":
                    handleRun(line);
                    break;
                case "setscheduler":
                    handleSetScheduler(line);
                    break;
                case "traceon":
                    handleTrace(true);
                    break;
                case "traceoff":
                    handleTrace(false);
                    break;
                case "exit":
                    System.out.println("Encerrando o sistema.");
                    scanner.close();
                    return;
                default:
                    System.out.println("Comando inválido: " + cmd);
            }
        }
    }

    private static void handleNew(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: new <nomeDoPrograma>");
            System.out.println("Programas disponíveis: " + Arrays.toString(programs.getProgramNames()));
            return;
        }
        String progName = args[1];
        Program program = programs.getProgramByName(progName);
        if (program == null) {
            System.out.println("Erro: Programa '" + progName + "' não encontrado.");
            return;
        }

        ProcessControlBlock pcb = pm.criarProcesso(program);
        if (pcb != null) {
            pm.admitirProcesso(pcb.getPid());
            System.out.println("Processo '" + progName + "' criado com PID: " + pcb.getPid());
        }
    }

    private static void handleRm(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: rm <pid>");
            return;
        }
        try {
            int pid = Integer.parseInt(args[1]);
            if (pm.finalizarProcesso(pid)) {
                System.out.println("Processo " + pid + " removido.");
            } else {
                System.out.println("Erro: Processo " + pid + " não encontrado.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Erro: PID inválido. Deve ser um número.");
        }
    }

    private static void handlePs() {
        pm.exibirEstatisticas();
    }

    private static void handleDump(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: dump <pid>");
            return;
        }
        try {
            int pid = Integer.parseInt(args[1]);
            pm.dumpProcesso(pid);
        } catch (NumberFormatException e) {
            System.out.println("Erro: PID inválido. Deve ser um número.");
        }
    }

    private static void handleDumpM(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: dumpm <inicio,fim>");
            return;
        }
        try {
            String[] range = args[1].split(",");
            int start = Integer.parseInt(range[0]);
            int end = Integer.parseInt(range[1]);
            pm.dumpMemoria(start, end);
        } catch (Exception e) {
            System.out.println("Erro: Formato inválido. Use: dumpm <inicio,fim>");
        }
    }

    private static void handleRun(String[] args) {
        System.out.println("Executando ciclos do SO... (o sistema irá rodar até não haver processos prontos)");
        pm.iniciarSistema();
        while (pm.executarCicloSO());
    }

    private static void handleSetScheduler(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: setscheduler <RR|FCFS|SJF>");
            return;
        }
        try {
            SchedulingPolicy policy = SchedulingPolicy.valueOf(args[1].toUpperCase());
            pm.setEscalonador(policy);
        } catch (IllegalArgumentException e) {
            System.out.println("Política de escalonamento inválida.");
        }
    }

    private static void handleTrace(boolean on) {
        pm.setTrace(on);
    }
}