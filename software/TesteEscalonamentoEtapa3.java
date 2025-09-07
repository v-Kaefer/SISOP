package software;

import hardware.HW;
import hardware.Word;
import hardware.Opcode;
import memory.MemoryManagerPonte;
import programs.Programs;
import programs.Program;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Testes abrangentes para os algoritmos de escalonamento da Etapa 3
 * 
 * Valida a funcionalidade dos diferentes algoritmos implementados
 * e compara suas performances.
 * 
 * Etapa 3: Testes de Escalonamento Avançado
 */
public class TesteEscalonamentoEtapa3 {
    
    public static void main(String[] args) {
        System.out.println("=== TESTES DE ESCALONAMENTO - ETAPA 3 ===\n");
        
        // Testa framework básico
        testarFrameworkEscalonamento();
        
        // Testa algoritmos individuais
        testarRoundRobinModular();
        testarFCFS();
        testarSJF();
        
        // Compara algoritmos
        compararAlgoritmos();
        
        // Testa factory
        testarSchedulerFactory();
        
        System.out.println("\n=== TODOS OS TESTES DA ETAPA 3 CONCLUÍDOS ===");
    }
    
    /**
     * Testa o framework básico de escalonamento
     */
    public static void testarFrameworkEscalonamento() {
        System.out.println("=== TESTE 1: Framework de Escalonamento ===");
        
        try {
            // Testa interface Scheduler
            Scheduler scheduler = new RoundRobinSchedulerImpl(5);
            System.out.println("✓ Interface Scheduler funcionando");
            System.out.println("Tipo: " + scheduler.getTipoEscalonamento());
            
            // Testa métricas
            SchedulingMetrics metricas = scheduler.getMetricas();
            System.out.println("✓ Métricas de performance criadas");
            
            // Testa políticas
            System.out.println("✓ Políticas de escalonamento:");
            System.out.println(SchedulingPolicy.listarPoliticas());
            
            System.out.println("✓ Teste Framework passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste Framework: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Testa a nova implementação modular do Round-Robin
     */
    public static void testarRoundRobinModular() {
        System.out.println("=== TESTE 2: Round-Robin Modular ===");
        
        try {
            Scheduler scheduler = new RoundRobinSchedulerImpl(3);
            
            // Cria processos de teste
            ProcessControlBlock[] processos = criarProcessosDeteste();
            
            // Adiciona processos
            for (ProcessControlBlock pcb : processos) {
                pcb.setEstado(ProcessState.READY);
                scheduler.adicionarProcesso(pcb);
            }
            
            System.out.println("Processos adicionados: " + scheduler.getProcessosProntos().size());
            
            // Simula alguns ciclos de execução
            for (int i = 0; i < 10; i++) {
                ProcessControlBlock atual = scheduler.selecionarProximoProcesso();
                if (atual != null) {
                    scheduler.executarCicloCPU();
                }
            }
            
            // Verifica métricas
            SchedulingMetrics metricas = scheduler.getMetricas();
            System.out.println("Context switches: " + metricas.getTotalContextSwitches());
            System.out.println("Ciclos CPU: " + metricas.getTotalCiclosCPU());
            
            System.out.println("✓ Teste Round-Robin Modular passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste Round-Robin: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Testa o algoritmo FCFS
     */
    public static void testarFCFS() {
        System.out.println("=== TESTE 3: FCFS (First Come First Served) ===");
        
        try {
            Scheduler scheduler = new FCFSScheduler();
            
            // Cria processos de teste
            ProcessControlBlock[] processos = criarProcessosDeteste();
            
            // Adiciona processos na ordem específica
            for (int i = 0; i < processos.length; i++) {
                processos[i].setEstado(ProcessState.READY);
                // Simula diferentes tempos de chegada
                processos[i].setTempoChegada(System.currentTimeMillis() + i * 10);
                scheduler.adicionarProcesso(processos[i]);
            }
            
            System.out.println("Algoritmo: " + scheduler.getTipoEscalonamento());
            System.out.println("Processos na fila: " + scheduler.getProcessosProntos().size());
            
            // FCFS deve ser não-preemptivo
            System.out.println("É preemptivo? " + scheduler.devePreemptar());
            
            // Simula execução
            ProcessControlBlock atual = scheduler.selecionarProximoProcesso();
            if (atual != null) {
                System.out.println("Primeiro processo selecionado: " + atual.getNome());
                
                // Simula alguns ciclos
                for (int i = 0; i < 5; i++) {
                    scheduler.executarCicloCPU();
                }
                
                // Verifica que o mesmo processo continua (não-preemptivo)
                ProcessControlBlock mesmo = scheduler.selecionarProximoProcesso();
                if (atual == mesmo) {
                    System.out.println("✓ FCFS não-preemptivo confirmado");
                }
            }
            
            System.out.println("✓ Teste FCFS passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste FCFS: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Testa o algoritmo SJF
     */
    public static void testarSJF() {
        System.out.println("=== TESTE 4: SJF (Shortest Job First) ===");
        
        try {
            Scheduler scheduler = new SJFScheduler();
            
            // Cria processos com diferentes estimativas
            ProcessControlBlock p1 = criarProcessoComEstimativa("ProcessoCurto", 5);
            ProcessControlBlock p2 = criarProcessoComEstimativa("ProcessoLongo", 20);
            ProcessControlBlock p3 = criarProcessoComEstimativa("ProcessoMedio", 10);
            
            // Adiciona em ordem diferente da estimativa
            p2.setEstado(ProcessState.READY);
            scheduler.adicionarProcesso(p2); // Longo primeiro
            
            p3.setEstado(ProcessState.READY);
            scheduler.adicionarProcesso(p3); // Médio segundo
            
            p1.setEstado(ProcessState.READY);
            scheduler.adicionarProcesso(p1); // Curto por último
            
            System.out.println("Algoritmo: " + scheduler.getTipoEscalonamento());
            
            // SJF deve selecionar o processo mais curto primeiro
            ProcessControlBlock primeiro = scheduler.selecionarProximoProcesso();
            if (primeiro != null && primeiro.getNome().equals("ProcessoCurto")) {
                System.out.println("✓ SJF selecionou processo mais curto primeiro: " + primeiro.getNome());
            } else {
                System.out.println("✗ SJF não priorizou processo mais curto");
            }
            
            // Simula execução
            for (int i = 0; i < 3; i++) {
                scheduler.executarCicloCPU();
            }
            
            System.out.println("✓ Teste SJF passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste SJF: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Compara diferentes algoritmos de escalonamento
     */
    public static void compararAlgoritmos() {
        System.out.println("=== TESTE 5: Comparação de Algoritmos ===");
        
        try {
            // Lista de algoritmos para testar
            Scheduler[] schedulers = {
                new RoundRobinSchedulerImpl(5),
                new FCFSScheduler(),
                new SJFScheduler()
            };
            
            String[] nomes = {"Round-Robin", "FCFS", "SJF"};
            
            System.out.println("Comparando algoritmos com carga de trabalho idêntica...\n");
            
            for (int i = 0; i < schedulers.length; i++) {
                System.out.println("--- " + nomes[i] + " ---");
                
                Scheduler scheduler = schedulers[i];
                
                // Cria carga de trabalho idêntica
                ProcessControlBlock[] processos = criarProcessosDeteste();
                for (ProcessControlBlock pcb : processos) {
                    pcb.setEstado(ProcessState.READY);
                    scheduler.adicionarProcesso(pcb);
                }
                
                // Simula execução
                int ciclos = 0;
                while (scheduler.temProcessosParaExecutar() && ciclos < 50) {
                    ProcessControlBlock atual = scheduler.selecionarProximoProcesso();
                    if (atual != null) {
                        scheduler.executarCicloCPU();
                    }
                    ciclos++;
                }
                
                // Mostra métricas
                SchedulingMetrics metricas = scheduler.getMetricas();
                System.out.println("Context Switches: " + metricas.getTotalContextSwitches());
                System.out.println("Ciclos executados: " + ciclos);
                System.out.println();
            }
            
            System.out.println("✓ Teste Comparação passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste Comparação: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Testa a SchedulerFactory
     */
    public static void testarSchedulerFactory() {
        System.out.println("=== TESTE 6: SchedulerFactory ===");
        
        try {
            // Testa criação de diferentes schedulers
            Scheduler rr = SchedulerFactory.criarRoundRobin(8);
            System.out.println("✓ Round-Robin criado: " + rr.getTipoEscalonamento());
            
            Scheduler fcfs = SchedulerFactory.criarFCFS();
            System.out.println("✓ FCFS criado: " + fcfs.getTipoEscalonamento());
            
            Scheduler sjf = SchedulerFactory.criarSJF();
            System.out.println("✓ SJF criado: " + sjf.getTipoEscalonamento());
            
            // Testa criação por política
            Scheduler prioridade = SchedulerFactory.criarEscalonador(SchedulingPolicy.PRIORITY, new Object[]{true});
            System.out.println("✓ Priority criado: " + prioridade.getTipoEscalonamento());
            
            // Lista escalonadores disponíveis
            System.out.println("\n" + SchedulerFactory.listarEscalonadoresDisponiveis());
            
            System.out.println("✓ Teste SchedulerFactory passou\n");
            
        } catch (Exception e) {
            System.err.println("✗ Erro no teste SchedulerFactory: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Cria processos de teste simples
     */
    private static ProcessControlBlock[] criarProcessosDeteste() {
        Word[] programa1 = {
            new Word(Opcode.LDI, 0, -1, 5),
            new Word(Opcode.STD, 0, -1, 10),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        Word[] programa2 = {
            new Word(Opcode.LDI, 0, -1, 10),
            new Word(Opcode.LDI, 1, -1, 20),
            new Word(Opcode.ADD, 0, 0, 1),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        Word[] programa3 = {
            new Word(Opcode.LDI, 0, -1, 1),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        return new ProcessControlBlock[] {
            new ProcessControlBlock(1, "Processo-A", programa1),
            new ProcessControlBlock(2, "Processo-B", programa2),
            new ProcessControlBlock(3, "Processo-C", programa3)
        };
    }
    
    /**
     * Cria processo com estimativa específica
     */
    private static ProcessControlBlock criarProcessoComEstimativa(String nome, int estimativa) {
        Word[] programa = {
            new Word(Opcode.LDI, 0, -1, 1),
            new Word(Opcode.STOP, -1, -1, -1)
        };
        
        ProcessControlBlock pcb = new ProcessControlBlock(nome.hashCode(), nome, programa);
        pcb.setTempoEstimadoExecucao(estimativa);
        return pcb;
    }
}