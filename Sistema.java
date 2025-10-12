import hardware.HW;
import software.SO;
import programs.Programs;
import programs.Program;
import hardware.Word;
import java.util.Scanner;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Sistema {
    // Simple Process representation
    private class Processo {
        int id;
        String nome;
        Word[] programa;
        boolean carregado;
        
        Processo(int id, String nome, Word[] programa) {
            this.id = id;
            this.nome = nome;
            this.programa = programa;
            this.carregado = false;
        }
    }
    
    public HW hw;
    public SO so;
    public Programs progs;
    private Scanner scanner;
    private boolean running;
    private List<String> programasCarregados; // Track loaded programs (legacy)
    private Map<Integer, Processo> processos; // Track processes by ID
    private int nextProcessId; // Next process ID to assign
    private boolean traceMode; // Trace execution mode

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
        processos = new HashMap<>();
        nextProcessId = 1;
        traceMode = false;
    }

    public void displayHelp() {
        System.out.println("\n=== COMANDOS DISPONÍVEIS ===");
        System.out.println("list                - Lista programas disponíveis");
        System.out.println("load <programa>     - Carrega um programa na memória (compatibilidade)");
        System.out.println("new <programa>      - Cria processo com ID único");
        System.out.println("rm <id>             - Remove processo por ID");
        System.out.println("ps                  - Lista todos os processos");
        System.out.println("exec <programa|id>  - Executa programa ou processo por ID");
        System.out.println("execAll             - Executa todos programas carregados");
        System.out.println("dump <id>           - Dump do processo (PCB e memória)");
        System.out.println("dumpM <inicio> <fim>- Dump da memória");
        System.out.println("traceOn             - Liga modo trace");
        System.out.println("traceOff            - Desliga modo trace");
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

    // New process management commands
    
    public void criarProcesso(String nomPrograma) {
        Word[] programa = progs.retrieveProgram(nomPrograma);
        if (programa != null) {
            int id = nextProcessId++;
            Processo processo = new Processo(id, nomPrograma, programa);
            processos.put(id, processo);
            System.out.println("\n>>> Processo criado com ID: " + id + " <<<");
            System.out.println(">>> Programa: " + nomPrograma + " <<<");
            System.out.println(">>> Use 'exec " + id + "' para executar <<<");
        } else {
            System.out.println("\n[ERRO] Programa '" + nomPrograma + "' não encontrado!");
            System.out.println("Use 'list' para ver programas disponíveis.");
        }
    }
    
    public void removerProcesso(int id) {
        if (processos.containsKey(id)) {
            Processo processo = processos.get(id);
            processos.remove(id);
            System.out.println("\n>>> Processo " + id + " (" + processo.nome + ") removido <<<");
        } else {
            System.out.println("\n[ERRO] Processo com ID " + id + " não encontrado!");
            System.out.println("Use 'ps' para ver processos disponíveis.");
        }
    }
    
    public void listarProcessos() {
        if (processos.isEmpty()) {
            System.out.println("\n>>> Nenhum processo criado <<<");
            return;
        }
        
        System.out.println("\n>>> Processos no sistema:");
        System.out.println("ID\tPrograma\t\tStatus");
        System.out.println("----------------------------------------");
        for (Map.Entry<Integer, Processo> entry : processos.entrySet()) {
            Processo p = entry.getValue();
            String status = p.carregado ? "Carregado" : "Criado";
            System.out.println(p.id + "\t" + p.nome + "\t\t" + status);
        }
    }
    
    public void executarProcessoPorId(int id) {
        if (!processos.containsKey(id)) {
            System.out.println("\n[ERRO] Processo com ID " + id + " não encontrado!");
            System.out.println("Use 'ps' para ver processos disponíveis.");
            return;
        }
        
        Processo processo = processos.get(id);
        System.out.println("\n>>> Executando processo ID " + id + ": " + processo.nome + " <<<");
        
        if (traceMode) {
            System.out.println(">>> Modo TRACE ativado <<<");
        }
        
        so.utils.loadAndExec(processo.programa);
        processo.carregado = true;
        System.out.println(">>> Execução finalizada <<<");
    }
    
    public void dumpProcesso(int id) {
        if (!processos.containsKey(id)) {
            System.out.println("\n[ERRO] Processo com ID " + id + " não encontrado!");
            System.out.println("Use 'ps' para ver processos disponíveis.");
            return;
        }
        
        Processo processo = processos.get(id);
        System.out.println("\n>>> Dump do Processo ID " + id + " <<<");
        System.out.println("Nome: " + processo.nome);
        System.out.println("Status: " + (processo.carregado ? "Executado" : "Aguardando execução"));
        System.out.println("Tamanho: " + processo.programa.length + " palavras");
        System.out.println("\n>>> Conteúdo do Programa:");
        for (int i = 0; i < processo.programa.length; i++) {
            Word w = processo.programa[i];
            System.out.println(i + ": " + w.opc + " ra=" + w.ra + " rb=" + w.rb + " p=" + w.p);
        }
    }
    
    public void ativarTrace() {
        traceMode = true;
        System.out.println("\n>>> Modo TRACE ativado <<<");
        System.out.println(">>> Cada instrução será exibida durante execução <<<");
    }
    
    public void desativarTrace() {
        traceMode = false;
        System.out.println("\n>>> Modo TRACE desativado <<<");
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
            
            case "new":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: new <nome_programa>");
                    System.out.println("Exemplo: new fatorialV2");
                } else {
                    criarProcesso(parts[1]);
                }
                break;
            
            case "rm":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: rm <id>");
                    System.out.println("Exemplo: rm 1");
                } else {
                    try {
                        int id = Integer.parseInt(parts[1]);
                        removerProcesso(id);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERRO] O ID deve ser um número inteiro");
                    }
                }
                break;
            
            case "ps":
                listarProcessos();
                break;
            
            case "exec":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: exec <nome_programa|id>");
                    System.out.println("Exemplo: exec fatorialV2  ou  exec 1");
                } else {
                    // Try to parse as ID first, if fails treat as program name
                    try {
                        int id = Integer.parseInt(parts[1]);
                        executarProcessoPorId(id);
                    } catch (NumberFormatException e) {
                        // Not a number, treat as program name (legacy)
                        executarPrograma(parts[1]);
                    }
                }
                break;
            
            case "execall":
                executarTodosProgramas();
                break;
            
            case "dump":
                if (parts.length < 2) {
                    System.out.println("[ERRO] Uso: dump <id>");
                    System.out.println("Exemplo: dump 1");
                } else {
                    try {
                        int id = Integer.parseInt(parts[1]);
                        dumpProcesso(id);
                    } catch (NumberFormatException e) {
                        System.out.println("[ERRO] O ID deve ser um número inteiro");
                        System.out.println("Use 'dumpM <inicio> <fim>' para dump de memória");
                    }
                }
                break;
            
            case "dumpm":
                if (parts.length < 3) {
                    System.out.println("[ERRO] Uso: dumpM <inicio> <fim>");
                    System.out.println("Exemplo: dumpM 0 10");
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
            
            case "traceon":
                ativarTrace();
                break;
            
            case "traceoff":
                desativarTrace();
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
        System.out.println("\n+--------------------------------------------+");
        System.out.println("|  Bem-vindo ao Sistema Operacional SISOP    |");
        System.out.println("|  Sistema em modo comando                   |");
        System.out.println("+--------------------------------------------+");
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