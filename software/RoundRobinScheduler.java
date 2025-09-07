package software;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * Escalonador Round-Robin para o sistema SISOP
 * 
 * Implementa o algoritmo de escalonamento Round-Robin conforme especificado:
 * - Cada processo recebe um quantum de tempo (fatia de tempo)
 * - Quando o quantum expira, o processo é colocado no final da fila
 * - Próximo processo da fila é selecionado para execução
 * - Garante fairness entre os processos
 */
public class RoundRobinScheduler {
    
    // Configuração do escalonador
    private int quantum;                    // Quantum de tempo padrão (em ciclos de CPU)
    private int quantumAtual;               // Quantum restante do processo atual
    
    // Filas de processos
    private Queue<ProcessControlBlock> filaProtos;  // Processos prontos para execução
    private ProcessControlBlock processoAtual;      // Processo atualmente executando
    
    // Estatísticas do escalonador
    private long totalContextSwitches;     // Número total de trocas de contexto
    private long cicloCPUAtual;           // Contador de ciclos para controle de tempo
    
    /**
     * Construtor com quantum padrão
     */
    public RoundRobinScheduler() {
        this(10); // Quantum padrão de 10 ciclos
    }
    
    /**
     * Construtor com quantum customizado
     */
    public RoundRobinScheduler(int quantum) {
        this.quantum = quantum;
        this.quantumAtual = 0;
        this.filaProtos = new LinkedList<>();
        this.processoAtual = null;
        this.totalContextSwitches = 0;
        this.cicloCPUAtual = 0;
    }
    
    /**
     * Adiciona um processo à fila de prontos
     */
    public void adicionarProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.READY) {
            pcb.setQuantumRestante(quantum);
            filaProtos.offer(pcb);
            System.out.println("Processo adicionado à fila: " + pcb.getNome() + " (PID: " + pcb.getPid() + ")");
        }
    }
    
    /**
     * Remove um processo da fila de prontos
     */
    public boolean removerProcesso(int pid) {
        return filaProtos.removeIf(pcb -> pcb.getPid() == pid);
    }
    
    /**
     * Seleciona o próximo processo para execução
     * Implementa a lógica do Round-Robin
     */
    public ProcessControlBlock selecionarProximoProcesso() {
        // Se não há processo atual ou quantum expirou
        if (processoAtual == null || quantumAtual <= 0 || processoAtual.isFinished()) {
            return executarContextSwitch();
        }
        
        // Se há processo atual com quantum, continua executando
        return processoAtual;
    }
    
    /**
     * Executa troca de contexto (context switch)
     */
    private ProcessControlBlock executarContextSwitch() {
        // Se há processo atual, coloca de volta na fila (se não terminou)
        if (processoAtual != null && !processoAtual.isFinished()) {
            if (processoAtual.getEstado() == ProcessState.RUNNING) {
                processoAtual.setEstado(ProcessState.READY);
                processoAtual.setQuantumRestante(quantum);
                filaProtos.offer(processoAtual);
                System.out.println("Context switch: " + processoAtual.getNome() + " retorna à fila de prontos");
            }
        }
        
        // Seleciona próximo processo da fila
        processoAtual = filaProtos.poll();
        
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.RUNNING);
            quantumAtual = quantum;
            processoAtual.setQuantumRestante(quantumAtual);
            processoAtual.setTempoUltimaExecucao(cicloCPUAtual);
            totalContextSwitches++;
            
            System.out.println("Context switch: " + processoAtual.getNome() + " inicia execução (Quantum: " + quantum + ")");
        } else {
            System.out.println("Nenhum processo disponível para execução");
        }
        
        return processoAtual;
    }
    
    /**
     * Verifica se deve ocorrer preempção (quantum expirou)
     */
    public boolean devePreemptar() {
        return quantumAtual <= 0 && processoAtual != null;
    }
    
    /**
     * Notifica que um ciclo de CPU foi executado
     * Atualiza contadores de quantum e tempo
     */
    public void executarCicloCPU() {
        cicloCPUAtual++;
        
        if (processoAtual != null) {
            quantumAtual--;
            processoAtual.decrementarQuantum();
            processoAtual.adicionarTempoCPU(1);
            
            // Atualiza tempo de espera para processos na fila
            for (ProcessControlBlock pcb : filaProtos) {
                pcb.adicionarTempoEspera(1);
            }
        }
    }
    
    /**
     * Bloqueia o processo atual (para I/O ou outro evento)
     */
    public void bloquearProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.WAITING);
            System.out.println("Processo bloqueado: " + processoAtual.getNome());
            processoAtual = null; // Remove da CPU
            quantumAtual = 0;
        }
    }
    
    /**
     * Desbloqueia um processo e o coloca na fila de prontos
     */
    public void desbloquearProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.WAITING) {
            pcb.setEstado(ProcessState.READY);
            adicionarProcesso(pcb);
            System.out.println("Processo desbloqueado: " + pcb.getNome());
        }
    }
    
    /**
     * Finaliza o processo atual
     */
    public void finalizarProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.finalizar();
            System.out.println("Processo finalizado: " + processoAtual.getNome() + " (PID: " + processoAtual.getPid() + ")");
            processoAtual = null;
            quantumAtual = 0;
        }
    }
    
    /**
     * Retorna o processo atualmente em execução
     */
    public ProcessControlBlock getProcessoAtual() {
        return processoAtual;
    }
    
    /**
     * Retorna quantum restante do processo atual
     */
    public int getQuantumRestante() {
        return quantumAtual;
    }
    
    /**
     * Retorna número de processos na fila de prontos
     */
    public int getNumeroProcessosNaFila() {
        return filaProtos.size();
    }
    
    /**
     * Verifica se há processos para executar
     */
    public boolean temProcessosParaExecutar() {
        return !filaProtos.isEmpty() || (processoAtual != null && !processoAtual.isFinished());
    }
    
    /**
     * Retorna lista de todos os processos prontos
     */
    public List<ProcessControlBlock> getProcessosProntos() {
        return new ArrayList<>(filaProtos);
    }
    
    /**
     * Estatísticas do escalonador
     */
    public String getEstatisticas() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Estatísticas do Escalonador Round-Robin ===\n");
        sb.append("Quantum configurado: ").append(quantum).append(" ciclos\n");
        sb.append("Quantum restante: ").append(quantumAtual).append(" ciclos\n");
        sb.append("Total de context switches: ").append(totalContextSwitches).append("\n");
        sb.append("Ciclo atual da CPU: ").append(cicloCPUAtual).append("\n");
        sb.append("Processos na fila de prontos: ").append(filaProtos.size()).append("\n");
        
        if (processoAtual != null) {
            sb.append("Processo atual: ").append(processoAtual.getNome()).append(" (PID: ").append(processoAtual.getPid()).append(")\n");
        } else {
            sb.append("Nenhum processo executando\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Mostra estado detalhado do escalonador
     */
    public void exibirEstado() {
        System.out.println("\n" + getEstatisticas());
        
        if (!filaProtos.isEmpty()) {
            System.out.println("Fila de processos prontos:");
            int i = 1;
            for (ProcessControlBlock pcb : filaProtos) {
                System.out.println("  " + i + ". " + pcb.toString());
                i++;
            }
        }
        
        if (processoAtual != null) {
            System.out.println("Processo em execução:");
            System.out.println("  " + processoAtual.toDetailedString());
        }
        System.out.println();
    }
    
    /**
     * Define novo quantum para o escalonador
     */
    public void setQuantum(int novoQuantum) {
        if (novoQuantum > 0) {
            this.quantum = novoQuantum;
            System.out.println("Quantum do escalonador alterado para: " + quantum + " ciclos");
        }
    }
    
    /**
     * Força um context switch mesmo com quantum restante
     */
    public ProcessControlBlock forcarContextSwitch() {
        quantumAtual = 0;
        return executarContextSwitch();
    }
}