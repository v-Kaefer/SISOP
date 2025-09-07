package software;

import hardware.HW;
import hardware.Word;
import hardware.Opcode;
import memory.MemoryManagerPonte;
import programs.Program;

import java.util.ArrayList;
import java.util.List;

/**
 * Teste modular para os componentes de Gerência de Processos
 * 
 * Testa:
 * - ProcessState enum
 * - ProcessControlBlock (PCB) 
 * - RoundRobinScheduler
 * - ProcessManager
 * - Execução concorrente de múltiplos processos
 */
public class TesteGerenciaProcessos {
    
    public static void main(String[] args) {
        System.out.println("=== TESTE MODULAR: GERÊNCIA DE PROCESSOS ===\n");
        
        // Executa todos os testes
        testarProcessState();
        testarProcessControlBlock();
        testarRoundRobinScheduler();
        testarProcessManager();
        testarExecucaoConcorrente();
        
        System.out.println("=== TODOS OS TESTES CONCLUÍDOS ===");
    }
    
    /**
     * Teste 1: Estados de Processo
     */
    public static void testarProcessState() {
        System.out.println("=== TESTE 1: ProcessState ===");
        
        // Testa todos os estados
        ProcessState[] estados = ProcessState.values();
        for (ProcessState estado : estados) {
            System.out.println("Estado: " + estado + " | Ativo: " + estado.isActive() + 
                             " | Escalonável: " + estado.canBeScheduled() + " | Finalizado: " + estado.isFinished());
        }
        
        // Testa transições válidas
        assert ProcessState.NEW.canBeScheduled() == false;
        assert ProcessState.READY.canBeScheduled() == true;
        assert ProcessState.RUNNING.isActive() == true;
        assert ProcessState.TERMINATED.isFinished() == true;
        
        System.out.println("✓ Teste ProcessState passou\n");
    }
    
    /**
     * Teste 2: Process Control Block
     */
    public static void testarProcessControlBlock() {
        System.out.println("=== TESTE 2: ProcessControlBlock ===");
        
        // Cria programa simples
        Word[] programa = criarProgramaSimples();
        
        // Cria PCB
        ProcessControlBlock pcb = new ProcessControlBlock(1, "TesteProcesso", programa);
        
        // Testa estado inicial
        assert pcb.getPid() == 1;
        assert pcb.getNome().equals("TesteProcesso");
        assert pcb.getEstado() == ProcessState.NEW;
        assert pcb.getPc() == 0;
        assert pcb.getTamanhoPrograma() == programa.length;
        
        // Testa mudanças de estado
        pcb.setEstado(ProcessState.READY);
        assert pcb.podeExecutar() == true;
        
        pcb.setEstado(ProcessState.RUNNING);
        assert pcb.getEstado() == ProcessState.RUNNING;
        
        // Testa registradores
        pcb.setRegistrador(0, 42);
        assert pcb.getRegistrador(0) == 42;
        
        // Testa estatísticas
        pcb.adicionarTempoCPU(10);
        pcb.adicionarTempoEspera(5);
        assert pcb.getTempoCPU() == 10;
        assert pcb.getTempoEspera() == 5;
        
        // Testa finalização
        pcb.finalizar();
        assert pcb.isFinished() == true;
        assert pcb.podeExecutar() == false;
        
        System.out.println("PCB criado: " + pcb.toString());
        System.out.println("✓ Teste ProcessControlBlock passou\n");
    }
    
    /**
     * Teste 3: Round-Robin Scheduler
     */
    public static void testarRoundRobinScheduler() {
        System.out.println("=== TESTE 3: RoundRobinScheduler ===");
        
        RoundRobinScheduler escalonador = new RoundRobinScheduler(5); // Quantum de 5 ciclos
        
        // Cria processos de teste
        Word[] prog1 = criarProgramaSimples();
        Word[] prog2 = criarProgramaSimples();
        
        ProcessControlBlock pcb1 = new ProcessControlBlock(1, "Processo1", prog1);
        ProcessControlBlock pcb2 = new ProcessControlBlock(2, "Processo2", prog2);
        
        pcb1.setEstado(ProcessState.READY);
        pcb2.setEstado(ProcessState.READY);
        
        // Testa adição de processos
        escalonador.adicionarProcesso(pcb1);
        escalonador.adicionarProcesso(pcb2);
        
        assert escalonador.getNumeroProcessosNaFila() == 2;
        assert escalonador.temProcessosParaExecutar() == true;
        
        // Testa seleção de processo
        ProcessControlBlock primeiro = escalonador.selecionarProximoProcesso();
        assert primeiro != null;
        assert primeiro.getEstado() == ProcessState.RUNNING;
        
        // Simula execução com quantum
        for (int i = 0; i < 7; i++) { // Excede quantum
            escalonador.executarCicloCPU();
        }
        
        // Deve ocorrer context switch
        ProcessControlBlock segundo = escalonador.selecionarProximoProcesso();
        assert segundo != primeiro; // Processo diferente
        
        // Testa estatísticas
        System.out.println(escalonador.getEstatisticas());
        
        System.out.println("✓ Teste RoundRobinScheduler passou\n");
    }
    
    /**
     * Teste 4: Process Manager
     */
    public static void testarProcessManager() {
        System.out.println("=== TESTE 4: ProcessManager ===");
        
        // Inicializa componentes
        HW hardware = new HW(1024);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Testa criação de processo
        Word[] programa = criarProgramaSimples();
        ProcessControlBlock pcb = processManager.criarProcesso("TesteProcesso", programa);
        
        assert pcb != null;
        assert pcb.getPid() == 1;
        assert pcb.getEstado() == ProcessState.NEW;
        
        // Testa admissão no sistema
        boolean admitido = processManager.admitirProcesso(pcb.getPid());
        assert admitido == true;
        assert pcb.getEstado() == ProcessState.READY;
        
        // Testa estatísticas
        List<ProcessControlBlock> ativos = processManager.getProcessosAtivos();
        assert ativos.size() == 1;
        
        // Testa finalização
        boolean finalizado = processManager.finalizarProcesso(pcb.getPid());
        assert finalizado == true;
        
        System.out.println("✓ Teste ProcessManager passou\n");
    }
    
    /**
     * Teste 5: Execução Concorrente
     */
    public static void testarExecucaoConcorrente() {
        System.out.println("=== TESTE 5: Execução Concorrente ===");
        
        // Inicializa sistema
        HW hardware = new HW(1024);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Cria múltiplos programas
        List<Program> programas = new ArrayList<>();
        programas.add(new Program("Processo-A", criarProgramaSimples()));
        programas.add(new Program("Processo-B", criarProgramaSimples()));
        programas.add(new Program("Processo-C", criarProgramaSimples()));
        
        // Simula execução concorrente
        System.out.println("Iniciando simulação de execução concorrente...");
        
        // Cria e admite processos
        List<ProcessControlBlock> pcbsCriados = new ArrayList<>();
        for (Program programa : programas) {
            ProcessControlBlock pcb = processManager.criarProcesso(programa);
            if (pcb != null) {
                pcbsCriados.add(pcb);
                processManager.admitirProcesso(pcb.getPid());
            }
        }
        
        // Verifica que os processos foram criados
        assert pcbsCriados.size() == 3;
        assert processManager.getProcessosAtivos().size() == 3;
        
        // Simula alguns ciclos de execução
        processManager.iniciarSistema();
        for (int i = 0; i < 20; i++) {
            processManager.executarCicloSO();
        }
        
        // Exibe estatísticas finais
        processManager.exibirEstatisticas();
        
        System.out.println("✓ Teste Execução Concorrente passou\n");
    }
    
    /**
     * Cria um programa simples para testes
     */
    private static Word[] criarProgramaSimples() {
        return new Word[] {
            new Word(Opcode.LDI, 0, -1, 10),     // R0 = 10
            new Word(Opcode.LDI, 1, -1, 5),      // R1 = 5
            new Word(Opcode.ADD, 0, 1, -1),      // R0 = R0 + R1
            new Word(Opcode.STD, 0, -1, 20),     // MEM[20] = R0
            new Word(Opcode.STOP, -1, -1, -1)    // Para
        };
    }
    
    /**
     * Demonstração completa do sistema de processos
     */
    public static void demonstracao() {
        System.out.println("\n=== DEMONSTRAÇÃO COMPLETA ===");
        
        // Setup do sistema
        HW hardware = new HW(1024);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(1024, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Configura quantum menor para melhor visualização
        processManager.setQuantum(3);
        
        // Cria programas diferentes
        List<Program> programas = criarProgramasVariados();
        
        // Executa demonstração
        processManager.executarProcessosConcorrentes(programas);
        
        System.out.println("=== FIM DA DEMONSTRAÇÃO ===");
    }
    
    /**
     * Cria uma variedade de programas para demonstração
     */
    private static List<Program> criarProgramasVariados() {
        List<Program> programas = new ArrayList<>();
        
        // Programa 1: Contador simples
        programas.add(new Program("Contador", new Word[] {
            new Word(Opcode.LDI, 0, -1, 0),      // R0 = 0
            new Word(Opcode.LDI, 1, -1, 1),      // R1 = 1
            new Word(Opcode.ADD, 0, 1, -1),      // R0 = R0 + 1
            new Word(Opcode.STD, 0, -1, 10),     // MEM[10] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        // Programa 2: Multiplicação
        programas.add(new Program("Multiplicador", new Word[] {
            new Word(Opcode.LDI, 0, -1, 3),      // R0 = 3
            new Word(Opcode.LDI, 1, -1, 4),      // R1 = 4
            new Word(Opcode.MULT, 0, 1, -1),     // R0 = R0 * R1
            new Word(Opcode.STD, 0, -1, 11),     // MEM[11] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        // Programa 3: Operações diversas
        programas.add(new Program("Operacoes", new Word[] {
            new Word(Opcode.LDI, 0, -1, 15),     // R0 = 15
            new Word(Opcode.LDI, 1, -1, 3),      // R1 = 3
            new Word(Opcode.SUB, 0, 1, -1),      // R0 = R0 - R1
            new Word(Opcode.STD, 0, -1, 12),     // MEM[12] = R0
            new Word(Opcode.STOP, -1, -1, -1)
        }));
        
        return programas;
    }
}