import hardware.HW;
import software.SO;
import programs.Programs;
import programs.Program;
import hardware.Word;
import java.util.Scanner;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Sistema {
    public HW hw;
    public SO so;
    public Programs progs;
    private Scanner scanner;
    private boolean running;
    private List<String> programasCarregados; // Track loaded programs

    public Sistema(int tamMem) {
        this(tamMem, System.in);
    }

    public Sistema(int tamMem, InputStream inputStream) {
        hw = new HW(tamMem);
        so = new SO(hw);
        hw.cpu.setUtilities(so.utils);
        progs = new Programs();
        scanner = new Scanner(inputStream);
        running = true;
        programasCarregados = new ArrayList<>();
    }

    public void displayHelp() {
        System.out.println("\n=== COMANDOS DISPONÍVEIS ===");
        System.out.println("list                - Lista programas disponíveis");
        System.out.println("load <programa>     - Carrega um programa na memória");
        System.out.println("exec <programa>     - Executa um programa");
        System.out.println("execAll             - Executa todos programas carregados");
        System.out.println("dump <inicio> <fim> - Mostra dump da memória");
        System.out.println("help                - Mostra esta ajuda");
        System.out.println("quit | exit         - Encerra o sistema");
        System.out.println("============================");
    }

    public void listarProgramas() {
        System.out.println("\n>>> Programas disponíveis:");
        System.out.println("  - fatorial      : Calcula fatorial de 7");
        System.out.println("  - fatorialV2    : Calcula fatorial de 5 com syscall");
        System.out.println("  - fibonacci10   : Gera série Fibonacci (10 elementos)");
        System.out.println("  - fibonacci10v2 : Fibonacci versão 2");
        System.out.println("  - progMinimo    : Programa mínimo de teste");
        System.out.println("  - fibonacciREAD : Fibonacci com entrada");
        System.out.println("  - PB            : Teste de fatorial com condicionais");
        System.out.println("  - PC            : Bubble sort (ordenação)");
    }

    public void carregarPrograma(String nomPrograma) {
        Word[] programa = progs.retrieveProgram(nomPrograma);
        if (programa != null) {
            System.out.println("\n>>> Carregando programa: " + nomPrograma + " <<<");
            // Load program into memory without executing
            Word[] m = hw.mem.pos;
            for (int i = 0; i < programa.length; i++) {
                m[i].opc = programa[i].opc;
                m[i].ra = programa[i].ra;
                m[i].rb = programa[i].rb;
                m[i].p = programa[i].p;
            }
            // Track that this program has been loaded
            if (!programasCarregados.contains(nomPrograma)) {
                programasCarregados.add(nomPrograma);
            }
            System.out.println(">>> Programa carregado na memória <<<");
            System.out.println(">>> Use 'exec " + nomPrograma + "' ou 'execAll' para executar <<<");
        } else {
            System.out.println("\n[ERRO] Programa '" + nomPrograma + "' não encontrado!");
            System.out.println("Use 'list' para ver programas disponíveis.");
        }
    }

    public void executarPrograma(String nomPrograma) {
        // Check if program has been loaded first
        if (!programasCarregados.contains(nomPrograma)) {
            System.out.println("\n[ERRO] Programa '" + nomPrograma + "' não foi carregado!");
            System.out.println("Use 'load " + nomPrograma + "' primeiro para carregar o programa.");
            return;
        }
        
        Word[] programa = progs.retrieveProgram(nomPrograma);
        if (programa != null) {
            System.out.println("\n>>> Executando programa: " + nomPrograma + " <<<");
            so.utils.loadAndExec(programa);
            System.out.println(">>> Execução finalizada <<<");
        } else {
            System.out.println("\n[ERRO] Programa '" + nomPrograma + "' não encontrado!");
        }
    }

    public void executarTodosProgramas() {
        if (programasCarregados.isEmpty()) {
            System.out.println("\n[ERRO] Nenhum programa foi carregado na memória!");
            System.out.println("Use 'load <programa>' para carregar programas primeiro.");
            return;
        }
        
        System.out.println("\n>>> Executando todos os programas carregados com escalonamento <<<");
        System.out.println(">>> Programas carregados: " + programasCarregados.size() + " <<<");
        
        for (String prog : programasCarregados) {
            Word[] programa = progs.retrieveProgram(prog);
            if (programa != null) {
                System.out.println("\n>>> Escalonando: " + prog + " <<<");
                so.utils.loadAndExec(programa);
            }
        }
        System.out.println("\n>>> Todos os programas carregados foram executados <<<");
    }

    public void dumpMemoria(int inicio, int fim) {
        System.out.println("\n>>> Dump da memória [" + inicio + " - " + fim + "]:");
        so.utils.dump(inicio, fim);
    }

    private void processCommand(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return;
        }

        String command = parts[0].toLowerCase();

        switch (command) {
            case "list":
                listarProgramas();
                break;
            
            case "load":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: load <nome_programa>");
                    System.out.println("Exemplo: load fatorialV2");
                } else {
                    carregarPrograma(parts[1]);
                }
                break;
            
            case "exec":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: exec <nome_programa>");
                    System.out.println("Exemplo: exec fatorialV2");
                } else {
                    executarPrograma(parts[1]);
                }
                break;
            
            case "execall":
                executarTodosProgramas();
                break;
            
            case "dump":
                if (parts.length < 3) {
                    System.out.println("[ERRO] Uso: dump <inicio> <fim>");
                    System.out.println("Exemplo: dump 0 10");
                } else {
                    try {
                        int inicio = Integer.parseInt(parts[1]);
                        int fim = Integer.parseInt(parts[2]);
                        dumpMemoria(inicio, fim);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERRO] Os parâmetros devem ser números inteiros");
                    }
                }
                break;
            
            case "help":
                displayHelp();
                break;
            
            case "quit":
            case "exit":
                System.out.println("\n>>> Encerrando sistema... <<<");
                running = false;
                break;
            
            default:
                System.out.println("[ERRO] Comando desconhecido: " + command);
                System.out.println("Digite 'help' para ver comandos disponíveis.");
        }
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    public void run() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Bem-vindo ao Sistema Operacional SISOP   ║");
        System.out.println("║  Sistema em modo comando                   ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println("\nDigite 'help' para ver comandos disponíveis.\n");

        while (running) {
            System.out.print("SISOP> ");
            
            try {
                String input = scanner.nextLine();
                
                if (input == null || input.trim().isEmpty()) {
                    continue;
                }

                processCommand(input);
                
            } catch (Exception e) {
                System.out.println("\n[ERRO] Erro ao processar comando: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("\n>>> Sistema encerrado com sucesso <<<");
    }

    public static void main(String[] args) {
        Sistema s = new Sistema(1024);
        s.run();
    }
}