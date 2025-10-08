import hardware.HW;
import software.SO;
import programs.Programs;
import programs.Program;
import hardware.Word;
import java.util.Scanner;

public class Sistema {
    public HW hw;
    public SO so;
    public Programs progs;
    private Scanner scanner;
    private boolean running;

    public Sistema(int tamMem) {
        hw = new HW(tamMem);
        so = new SO(hw);
        hw.cpu.setUtilities(so.utils);
        progs = new Programs();
        scanner = new Scanner(System.in);
        running = true;
    }

    public void displayMenu() {
        System.out.println("\n===============================================");
        System.out.println("           SISTEMA OPERACIONAL SISOP          ");
        System.out.println("===============================================");
        System.out.println("1. Listar programas disponíveis");
        System.out.println("2. Executar programa");
        System.out.println("3. Executar fatorialV2 (demonstração)");
        System.out.println("4. Executar fibonacci10 (demonstração)");
        System.out.println("5. Executar progMinimo (demonstração)");
        System.out.println("0. Sair");
        System.out.println("===============================================");
        System.out.print("Escolha uma opção: ");
    }

    public void listarProgramas() {
        System.out.println("\n=== PROGRAMAS DISPONÍVEIS ===");
        System.out.println("1. fatorial - Calcula fatorial de 7");
        System.out.println("2. fatorialV2 - Calcula fatorial de 5 com syscall");
        System.out.println("3. fibonacci10 - Gera série Fibonacci (10 elementos)");
        System.out.println("4. fibonacci10v2 - Fibonacci versão 2");
        System.out.println("5. progMinimo - Programa mínimo de teste");
        System.out.println("6. fibonacciREAD - Fibonacci com entrada");
        System.out.println("7. PB - Teste de fatorial com condicionais");
        System.out.println("8. PC - Bubble sort (ordenação)");
        System.out.println("================================");
    }

    public void executarPrograma(String nomPrograma) {
        Word[] programa = progs.retrieveProgram(nomPrograma);
        if (programa != null) {
            System.out.println("\n>>> Executando programa: " + nomPrograma + " <<<\n");
            so.utils.loadAndExec(programa);
            System.out.println("\n>>> Execução finalizada <<<");
        } else {
            System.out.println("\n[ERRO] Programa '" + nomPrograma + "' não encontrado!");
        }
    }

    public void executarProgramaPorEscolha() {
        System.out.println("\nDigite o nome do programa para executar:");
        System.out.println("(fatorial, fatorialV2, fibonacci10, fibonacci10v2, progMinimo,");
        System.out.println(" fibonacciREAD, PB, PC)");
        System.out.print("Nome: ");
        String nome = scanner.nextLine().trim();
        
        if (!nome.isEmpty()) {
            executarPrograma(nome);
        } else {
            System.out.println("[ERRO] Nome inválido!");
        }
    }

    public void run() {
        System.out.println("\n╔════════════════════════════════════════════╗");
        System.out.println("║  Bem-vindo ao Sistema Operacional SISOP   ║");
        System.out.println("║  Sistema em modo interativo                ║");
        System.out.println("╚════════════════════════════════════════════╝");

        while (running) {
            displayMenu();
            
            try {
                String input = scanner.nextLine().trim();
                
                if (input.isEmpty()) {
                    continue;
                }

                int opcao = Integer.parseInt(input);

                switch (opcao) {
                    case 1:
                        listarProgramas();
                        break;
                    case 2:
                        executarProgramaPorEscolha();
                        break;
                    case 3:
                        executarPrograma("fatorialV2");
                        break;
                    case 4:
                        executarPrograma("fibonacci10");
                        break;
                    case 5:
                        executarPrograma("progMinimo");
                        break;
                    case 0:
                        System.out.println("\n>>> Encerrando sistema... <<<");
                        running = false;
                        break;
                    default:
                        System.out.println("\n[ERRO] Opção inválida! Tente novamente.");
                }
            } catch (NumberFormatException e) {
                System.out.println("\n[ERRO] Por favor, digite um número válido.");
            } catch (Exception e) {
                System.out.println("\n[ERRO] Erro inesperado: " + e.getMessage());
            }
        }

        scanner.close();
        System.out.println("\nSistema encerrado com sucesso.");
    }

    public static void main(String[] args) {
        Sistema s = new Sistema(1024);
        s.run();
    }
}