package examples;

import hardware.HW;
import hardware.Word;
import hardware.Opcode;
import memory.MemoryManagerPonte;
import programs.Program;
import programs.Programs;
import software.ProcessManager;
import software.ProcessControlBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Exemplo prático demonstrando execução concorrente de múltiplos processos
 * 
 * Este exemplo mostra:
 * - Criação de múltiplos processos
 * - Escalonamento Round-Robin
 * - Context switching
 * - Gerenciamento de memória por processo
 * - Estatísticas de execução
 */
public class ExemploExecucaoConcorrente {
    
    public static void main(String[] args) {
        System.out.println("=== EXEMPLO: EXECUÇÃO CONCORRENTE DE PROCESSOS ===\n");
        
        // Demonstra diferentes cenários
        exemploBasico();
        exemploComProgramasReais();
        exemploComQuantumDiferente();
        
        System.out.println("\n=== EXEMPLO CONCLUÍDO ===");
    }
    
    /**
     * Exemplo básico com programas simples
     */
    public static void exemploBasico() {
        System.out.println("=== EXEMPLO 1: Processos Básicos ===");
        
        // Inicializa sistema
        HW hardware = new HW(1024);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Configura quantum pequeno para melhor visualização
        processManager.setQuantum(3);
        
        // Cria programas simples
        List<Program> programas = criarProgramasSimples();
        
        // Executa
        processManager.executarProcessosConcorrentes(programas);
        
        System.out.println("=== FIM EXEMPLO 1 ===\n");
    }
    
    /**
     * Exemplo com programas reais da biblioteca
     */
    public static void exemploComProgramasReais() {
        System.out.println("=== EXEMPLO 2: Programas Reais ===");
        
        // Inicializa sistema
        HW hardware = new HW(2048); // Mais memória para programas maiores
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(2048, 16);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Quantum médio
        processManager.setQuantum(5);
        
        // Usar programas da biblioteca
        Programs biblioteca = new Programs();
        
        List<Program> programas = new ArrayList<>();
        
        Word[] progMinimo = biblioteca.retrieveProgram("progMinimo");
        if (progMinimo != null) {
            programas.add(new Program("progMinimo", progMinimo));
        }
        
        Word[] fibonacci = biblioteca.retrieveProgram("fibonacci10");
        if (fibonacci != null) {
            programas.add(new Program("fibonacci10", fibonacci));
        }
        
        Word[] fatorial = biblioteca.retrieveProgram("fatorial");
        if (fatorial != null) {
            programas.add(new Program("fatorial", fatorial));
        }
        
        // Executa
        processManager.executarProcessosConcorrentes(programas);
        
        System.out.println("=== FIM EXEMPLO 2 ===\n");
    }
    
    /**
     * Exemplo demonstrando diferentes quantums
     */
    public static void exemploComQuantumDiferente() {
        System.out.println("=== EXEMPLO 3: Comparação de Quantums ===");
        
        int[] quantums = {2, 5, 10};
        
        for (int quantum : quantums) {
            System.out.println("--- Testando quantum = " + quantum + " ---");
            
            // Inicializa sistema
            HW hardware = new HW(1024);
            MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
            ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
            
            processManager.setQuantum(quantum);
            
            // Mesmos programas para comparação
            List<Program> programas = criarProgramasComparativos();
            
            long inicioTempo = System.currentTimeMillis();
            processManager.executarProcessosConcorrentes(programas);
            long fimTempo = System.currentTimeMillis();
            
            System.out.println("Tempo de execução: " + (fimTempo - inicioTempo) + "ms");
            System.out.println();
        }
        
        System.out.println("=== FIM EXEMPLO 3 ===\n");
    }
    
    /**
     * Cria programas simples para demonstração
     */
    private static List<Program> criarProgramasSimples() {
        List<Program> programas = new ArrayList<>();
        
        // Programa A: Contador
        programas.add(new Program("ContadorA", new Word[] {
            new Word(Opcode.LDI, 0, -1, 0),      // R0 = 0 (contador)
            new Word(Opcode.LDI, 1, -1, 1),      // R1 = 1 (incremento)
            new Word(Opcode.ADD, 0, 1, -1),      // R0 = R0 + 1
            new Word(Opcode.ADD, 0, 1, -1),      // R0 = R0 + 1
            new Word(Opcode.ADD, 0, 1, -1),      // R0 = R0 + 1
            new Word(Opcode.STD, 0, -1, 10),     // MEM[10] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        // Programa B: Multiplicador
        programas.add(new Program("MultiplicadorB", new Word[] {
            new Word(Opcode.LDI, 0, -1, 2),      // R0 = 2
            new Word(Opcode.LDI, 1, -1, 3),      // R1 = 3
            new Word(Opcode.MULT, 0, 1, -1),     // R0 = R0 * R1 = 6
            new Word(Opcode.MULT, 0, 1, -1),     // R0 = R0 * R1 = 18
            new Word(Opcode.STD, 0, -1, 11),     // MEM[11] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        // Programa C: Operações diversas
        programas.add(new Program("OperacoesC", new Word[] {
            new Word(Opcode.LDI, 0, -1, 20),     // R0 = 20
            new Word(Opcode.LDI, 1, -1, 5),      // R1 = 5
            new Word(Opcode.SUB, 0, 1, -1),      // R0 = R0 - R1 = 15
            new Word(Opcode.SUB, 0, 1, -1),      // R0 = R0 - R1 = 10
            new Word(Opcode.STD, 0, -1, 12),     // MEM[12] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        return programas;
    }
    
    /**
     * Cria programas para comparação de performance
     */
    private static List<Program> criarProgramasComparativos() {
        List<Program> programas = new ArrayList<>();
        
        // Programa de alta CPU
        programas.add(new Program("CPU-Intensivo", new Word[] {
            new Word(Opcode.LDI, 0, -1, 0),      // R0 = 0
            new Word(Opcode.LDI, 1, -1, 1),      // R1 = 1
            new Word(Opcode.ADD, 0, 1, -1),      // Loop: R0++
            new Word(Opcode.ADD, 0, 1, -1),      
            new Word(Opcode.ADD, 0, 1, -1),      
            new Word(Opcode.ADD, 0, 1, -1),      
            new Word(Opcode.ADD, 0, 1, -1),      
            new Word(Opcode.STD, 0, -1, 15),     // MEM[15] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        // Programa rápido
        programas.add(new Program("Rapido", new Word[] {
            new Word(Opcode.LDI, 0, -1, 42),     // R0 = 42
            new Word(Opcode.STD, 0, -1, 16),     // MEM[16] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        return programas;
    }
    
    /**
     * Demonstração detalhada do escalonamento
     */
    public static void demonstracaoEscalonamento() {
        System.out.println("=== DEMONSTRAÇÃO DETALHADA DO ESCALONAMENTO ===");
        
        // Sistema pequeno para visualização clara
        HW hardware = new HW(512);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(512, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Quantum muito pequeno para mostrar context switches
        processManager.setQuantum(2);
        
        // Cria 4 processos pequenos
        List<ProcessControlBlock> pcbs = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            Word[] programa = new Word[] {
                new Word(Opcode.LDI, 0, -1, i * 10),      // R0 = valor único
                new Word(Opcode.STD, 0, -1, 20 + i),      // MEM[20+i] = R0
                new Word(Opcode.STOP, -1, -1, -1)
            };
            
            ProcessControlBlock pcb = processManager.criarProcesso("Processo" + i, programa);
            if (pcb != null) {
                pcbs.add(pcb);
                processManager.admitirProcesso(pcb.getPid());
            }
        }
        
        System.out.println("Processos criados e admitidos. Iniciando execução step-by-step:");
        
        // Execução manual step-by-step
        processManager.iniciarSistema();
        for (int i = 0; i < 30 && processManager.getEscalonador().temProcessosParaExecutar(); i++) {
            System.out.println("\n--- Ciclo " + (i + 1) + " ---");
            processManager.getEscalonador().exibirEstado();
            processManager.executarCicloSO();
        }
        
        System.out.println("\n=== FIM DEMONSTRAÇÃO ESCALONAMENTO ===");
    }
}