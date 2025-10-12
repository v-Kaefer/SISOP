package exemplos;

import hardware.HW;
import hardware.Word;
import hardware.Opcode;
import memory.MemoryManagerPonte;
import programs.Programs;
import programs.Program;
import software.*;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Demonstração prática dos novos algoritmos de escalonamento da Etapa 3
 * 
 * Este exemplo mostra:
 * - Diferentes algoritmos de escalonamento funcionando
 * - Comparação de performance entre algoritmos
 * - Métricas avançadas de escalonamento
 * - Framework flexível de escalonamento
 * 
 * Etapa 3: Demonstração de Escalonamento Avançado
 */
public class ExemploEscalonamentoEtapa3 {
    
    public static void main(String[] args) {
        System.out.println("=== DEMONSTRAÇÃO ETAPA 3: ESCALONAMENTO AVANÇADO ===\n");
        
        // Demonstra diferentes algoritmos
        demonstrarRoundRobinAvancado();
        demonstrarFCFS();
        demonstrarSJF();
        
        // Compara algoritmos lado a lado
        compararAlgoritmosComMesmosCarga();
        
        // Demonstra métricas avançadas
        demonstrarMetricasAvancadas();
        
        System.out.println("\n=== FIM DA DEMONSTRAÇÃO ===");
    }
    
    /**
     * Demonstra Round-Robin com controle avançado
     */
    public static void demonstrarRoundRobinAvancado() {
        System.out.println("=== DEMONSTRAÇÃO 1: Round-Robin Avançado ===");
        
        // Cria sistema pequeno para visualização clara
        HW hardware = new HW(512);
        MemoryManagerPonte memoriaManager = new MemoryManagerPonte(512, 8);
        ProcessManager processManager = new ProcessManager(hardware, memoriaManager);
        
        // Usa Round-Robin com quantum pequeno para demonstrar context switches
        Scheduler roundRobin = SchedulerFactory.criarRoundRobin(3);
        
        System.out.println("Algoritmo selecionado: " + roundRobin.getTipoEscalonamento());
        System.out.println("Configuração: Quantum = 3 ciclos\n");
        
        // Cria processos com diferentes características
        criarProcessosDemo(roundRobin);
        
        // Executa step-by-step
        System.out.println("Execução step-by-step:");
        executarStepByStep(roundRobin, 15);
        
        // Mostra métricas finais
        mostrarMetricas(roundRobin, "Round-Robin");
        
        System.out.println();
    }
    
    /**
     * Demonstra FCFS
     */
    public static void demonstrarFCFS() {
        System.out.println("=== DEMONSTRAÇÃO 2: FCFS (First Come First Served) ===");
        
        Scheduler fcfs = SchedulerFactory.criarFCFS();
        
        System.out.println("Algoritmo selecionado: " + fcfs.getTipoEscalonamento());
        System.out.println("Características: Não-preemptivo, ordem de chegada\n");
        
        // Cria processos com tempos diferentes
        criarProcessosDemo(fcfs);
        
        // Executa
        System.out.println("Execução FCFS:");
        executarStepByStep(fcfs, 10);
        
        mostrarMetricas(fcfs, "FCFS");
        
        System.out.println();
    }
    
    /**
     * Demonstra SJF
     */
    public static void demonstrarSJF() {
        System.out.println("=== DEMONSTRAÇÃO 3: SJF (Shortest Job First) ===");
        
        Scheduler sjf = SchedulerFactory.criarSJF();
        
        System.out.println("Algoritmo selecionado: " + sjf.getTipoEscalonamento());
        System.out.println("Características: Não-preemptivo, prioriza trabalhos mais curtos\n");
        
        // Cria processos com estimativas variadas
        criarProcessosComEstimativas(sjf);
        
        // Executa
        System.out.println("Execução SJF:");
        executarStepByStep(sjf, 10);
        
        mostrarMetricas(sjf, "SJF");
        
        System.out.println();
    }
    
    /**
     * Compara diferentes algoritmos com a mesma carga
     */
    public static void compararAlgoritmosComMesmosCarga() {
        System.out.println("=== DEMONSTRAÇÃO 4: Comparação de Algoritmos ===");
        
        System.out.println("Comparando Round-Robin, FCFS e SJF com carga idêntica...\n");
        
        // Cria três escalonadores
        Scheduler[] schedulers = {
            SchedulerFactory.criarRoundRobin(4),
            SchedulerFactory.criarFCFS(),
            SchedulerFactory.criarSJF()
        };
        
        String[] nomes = {"Round-Robin (Q=4)", "FCFS", "SJF"};
        SchedulingMetrics[] resultados = new SchedulingMetrics[3];
        
        // Executa mesmo workload em cada algoritmo
        for (int i = 0; i < schedulers.length; i++) {
            System.out.println("--- Executando: " + nomes[i] + " ---");
            
            // Carga de trabalho idêntica
            criarProcessosDemo(schedulers[i]);
            
            // Executa por 25 ciclos
            for (int ciclo = 0; ciclo < 25; ciclo++) {
                ProcessControlBlock atual = schedulers[i].selecionarProximoProcesso();
                if (atual != null) {
                    schedulers[i].executarCicloCPU();
                }
            }
            
            resultados[i] = schedulers[i].getMetricas();
            System.out.println("Context switches: " + resultados[i].getTotalContextSwitches());
            System.out.println("Overhead: " + String.format("%.1f%%", resultados[i].getOverheadContextSwitch()));
            System.out.println();
        }
        
        // Compara resultados
        System.out.println("=== COMPARAÇÃO FINAL ===");
        System.out.printf("%-20s | %-15s | %-15s | %-15s\n", "Algoritmo", "Context Switches", "Overhead (%)", "Throughput");
        System.out.println("-".repeat(75));
        
        for (int i = 0; i < 3; i++) {
            System.out.printf("%-20s | %-15d | %-15.1f | %-15.2f\n",
                nomes[i],
                resultados[i].getTotalContextSwitches(),
                resultados[i].getOverheadContextSwitch(),
                resultados[i].getThroughput());
        }
        
        System.out.println();
    }
    
    /**
     * Demonstra métricas avançadas
     */
    public static void demonstrarMetricasAvancadas() {
        System.out.println("=== DEMONSTRAÇÃO 5: Métricas Avançadas ===");
        
        Scheduler scheduler = SchedulerFactory.criarRoundRobin(5);
        
        // Cria e executa alguns processos
        criarProcessosDemo(scheduler);
        
        System.out.println("Executando workload para coletar métricas...");
        
        // Simula finalizações de processo para métricas
        for (int i = 0; i < 20; i++) {
            ProcessControlBlock atual = scheduler.selecionarProximoProcesso();
            if (atual != null) {
                scheduler.executarCicloCPU();
                
                // Simula finalização ocasional
                if (i % 7 == 0 && atual != null) {
                    scheduler.finalizarProcessoAtual();
                }
            }
        }
        
        // Mostra relatório completo de métricas
        SchedulingMetrics metricas = scheduler.getMetricas();
        System.out.println("\n" + metricas.gerarRelatorio());
        
        System.out.println();
    }
    
    /**
     * Cria processos de demonstração
     */
    private static void criarProcessosDemo(Scheduler scheduler) {
        // Programas simples de diferentes tamanhos
        Word[] programaCurto = {
            new Word(Opcode.LDI, 0, -1, 5),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        Word[] programaMedio = {
            new Word(Opcode.LDI, 0, -1, 10),
            new Word(Opcode.LDI, 1, -1, 20),
            new Word(Opcode.ADD, 0, 0, 1),
            new Word(Opcode.STD, 0, -1, 30),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        Word[] programaLongo = {
            new Word(Opcode.LDI, 0, -1, 1),
            new Word(Opcode.LDI, 1, -1, 1),
            new Word(Opcode.ADD, 0, 0, 1),
            new Word(Opcode.STD, 0, -1, 10),
            new Word(Opcode.LDI, 2, -1, 5),
            new Word(Opcode.MULT, 0, 0, 2),
            new Word(Opcode.STD, 0, -1, 20),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        // Cria PCBs
        ProcessControlBlock[] processos = {
            new ProcessControlBlock(1, "ProcessoCurto", programaCurto),
            new ProcessControlBlock(2, "ProcessoMedio", programaMedio),
            new ProcessControlBlock(3, "ProcessoLongo", programaLongo)
        };
        
        // Adiciona ao escalonador
        for (ProcessControlBlock pcb : processos) {
            pcb.setEstado(ProcessState.READY);
            scheduler.adicionarProcesso(pcb);
        }
    }
    
    /**
     * Cria processos com estimativas específicas para SJF
     */
    private static void criarProcessosComEstimativas(Scheduler scheduler) {
        Word[] programa = {
            new Word(Opcode.LDI, 0, -1, 1),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        // Processos com diferentes estimativas (em ordem inversa de prioridade)
        ProcessControlBlock p1 = new ProcessControlBlock(1, "Longo(20)", programa);
        p1.setTempoEstimadoExecucao(20);
        p1.setEstado(ProcessState.READY);
        
        ProcessControlBlock p2 = new ProcessControlBlock(2, "Medio(10)", programa);
        p2.setTempoEstimadoExecucao(10);
        p2.setEstado(ProcessState.READY);
        
        ProcessControlBlock p3 = new ProcessControlBlock(3, "Curto(5)", programa);
        p3.setTempoEstimadoExecucao(5);
        p3.setEstado(ProcessState.READY);
        
        // Adiciona em ordem diferente da estimativa
        scheduler.adicionarProcesso(p1); // Longo primeiro
        scheduler.adicionarProcesso(p2); // Médio segundo  
        scheduler.adicionarProcesso(p3); // Curto último
        
        System.out.println("Processos adicionados com estimativas:");
        System.out.println("- " + p1.getNome() + ": " + p1.getTempoEstimadoExecucao() + " ciclos");
        System.out.println("- " + p2.getNome() + ": " + p2.getTempoEstimadoExecucao() + " ciclos");
        System.out.println("- " + p3.getNome() + ": " + p3.getTempoEstimadoExecucao() + " ciclos");
        System.out.println("(SJF deve executar na ordem: Curto → Medio → Longo)\n");
    }
    
    /**
     * Executa scheduler step-by-step
     */
    private static void executarStepByStep(Scheduler scheduler, int maxCiclos) {
        for (int ciclo = 1; ciclo <= maxCiclos; ciclo++) {
            ProcessControlBlock atual = scheduler.selecionarProximoProcesso();
            
            if (atual == null) {
                System.out.println("Ciclo " + ciclo + ": Nenhum processo para executar");
                break;
            }
            
            scheduler.executarCicloCPU();
            
            // Mostra estado a cada 3 ciclos
            if (ciclo % 3 == 0) {
                System.out.println("Ciclo " + ciclo + ": Executando " + atual.getNome() + 
                    " (CPU: " + atual.getTempoCPU() + " ciclos)");
            }
        }
    }
    
    /**
     * Mostra métricas de um escalonador
     */
    private static void mostrarMetricas(Scheduler scheduler, String nome) {
        System.out.println("\n--- Métricas " + nome + " ---");
        SchedulingMetrics metricas = scheduler.getMetricas();
        System.out.println("Total de context switches: " + metricas.getTotalContextSwitches());
        System.out.println("Total de ciclos CPU: " + metricas.getTotalCiclosCPU());
        System.out.println("Overhead de context switch: " + String.format("%.1f%%", metricas.getOverheadContextSwitch()));
        
        if (metricas.getTotalProcessosFinalizados() > 0) {
            System.out.println("Tempo médio de espera: " + String.format("%.1f", metricas.getTempoMedioEspera()) + " ciclos");
        }
    }
}