package software;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo Round-Robin que implementa a interface Scheduler
 * 
 * Esta é a versão refatorada do RoundRobinScheduler original,
 * agora seguindo a arquitetura modular da Etapa 3.
 * 
 * Etapa 3: Round-Robin Modular
 */
public class RoundRobinSchedulerImpl implements Scheduler {
    
    // Configuração do escalonador
    private int quantum;                    // Quantum de tempo padrão (em ciclos de CPU)
    private int quantumAtual;               // Quantum restante do processo atual
    
    // Filas de processos
    private Queue<ProcessControlBlock> filaProtos;  // Processos prontos para execução
    private ProcessControlBlock processoAtual;      // Processo atualmente executando
    
    // Métricas e estatísticas
    private SchedulingMetrics metricas;
    private long cicloCPUAtual;           // Contador de ciclos para controle de tempo
    
    /**
     * Construtor com quantum padrão
     */
    public RoundRobinSchedulerImpl() {
        this(10); // Quantum padrão de 10 ciclos
    }
    
    /**
     * Construtor com quantum customizado
     */
    public RoundRobinSchedulerImpl(int quantum) {
        this.quantum = quantum;
        this.quantumAtual = 0;
        this.filaProtos = new LinkedList<>();
        this.processoAtual = null;
        this.metricas = new SchedulingMetrics();
        this.cicloCPUAtual = 0;
    }
    
    @Override
    public void adicionarProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.READY) {
            pcb.setQuantumRestante(quantum);
            filaProtos.offer(pcb);
            System.out.println("RR: Processo adicionado à fila: " + pcb.getNome() + " (PID: " + pcb.getPid() + ")");
        }
    }
    
    @Override
    public boolean removerProcesso(int pid) {
        return filaProtos.removeIf(pcb -> pcb.getPid() == pid);
    }
    
    @Override
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
                System.out.println("RR: Context switch: " + processoAtual.getNome() + " retorna à fila de prontos");
            }
        }
        
        // Seleciona próximo processo da fila
        processoAtual = filaProtos.poll();
        
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.RUNNING);
            quantumAtual = quantum;
            processoAtual.setQuantumRestante(quantumAtual);
            processoAtual.setTempoUltimaExecucao(cicloCPUAtual);
            metricas.registrarContextSwitch();
            metricas.registrarInicioExecucao();
            
            System.out.println("RR: Context switch: " + processoAtual.getNome() + " inicia execução (Quantum: " + quantum + ")");
        } else {
            System.out.println("RR: Nenhum processo disponível para execução");
        }
        
        return processoAtual;
    }
    
    @Override
    public void executarCicloCPU() {
        cicloCPUAtual++;
        metricas.registrarCicloCPU();
        
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
    
    @Override
    public boolean devePreemptar() {
        return quantumAtual <= 0 && processoAtual != null;
    }
    
    @Override
    public void bloquearProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.WAITING);
            System.out.println("RR: Processo bloqueado: " + processoAtual.getNome());
            processoAtual = null; // Remove da CPU
            quantumAtual = 0;
        }
    }
    
    @Override
    public void desbloquearProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.WAITING) {
            pcb.setEstado(ProcessState.READY);
            adicionarProcesso(pcb);
            System.out.println("RR: Processo desbloqueado: " + pcb.getNome());
        }
    }
    
    @Override
    public void finalizarProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.finalizar();
            metricas.registrarFinalizacaoProcesso(processoAtual);
            System.out.println("RR: Processo finalizado: " + processoAtual.getNome() + " (PID: " + processoAtual.getPid() + ")");
            processoAtual = null;
            quantumAtual = 0;
        }
    }
    
    @Override
    public ProcessControlBlock getProcessoAtual() {
        return processoAtual;
    }
    
    @Override
    public boolean temProcessosParaExecutar() {
        return !filaProtos.isEmpty() || (processoAtual != null && !processoAtual.isFinished());
    }
    
    @Override
    public List<ProcessControlBlock> getProcessosProntos() {
        return new ArrayList<>(filaProtos);
    }
    
    @Override
    public String getEstatisticas() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Estatísticas do Escalonador Round-Robin ===\n");
        sb.append("Quantum configurado: ").append(quantum).append(" ciclos\n");
        sb.append("Quantum restante: ").append(quantumAtual).append(" ciclos\n");
        sb.append("Ciclo atual da CPU: ").append(cicloCPUAtual).append("\n");
        sb.append("Processos na fila de prontos: ").append(filaProtos.size()).append("\n");
        
        if (processoAtual != null) {
            sb.append("Processo atual: ").append(processoAtual.getNome()).append(" (PID: ").append(processoAtual.getPid()).append(")\n");
        } else {
            sb.append("Nenhum processo executando\n");
        }
        
        return sb.toString();
    }
    
    @Override
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
    
    @Override
    public String getTipoEscalonamento() {
        return "Round-Robin (Quantum: " + quantum + " ciclos)";
    }
    
    @Override
    public ProcessControlBlock forcarContextSwitch() {
        quantumAtual = 0;
        return executarContextSwitch();
    }
    
    @Override
    public SchedulingMetrics getMetricas() {
        return metricas;
    }
    
    @Override
    public void configurarParametros(String parametro, Object valor) {
        switch (parametro.toLowerCase()) {
            case "quantum":
                if (valor instanceof Integer) {
                    setQuantum((Integer) valor);
                }
                break;
            default:
                System.err.println("RR: Parâmetro desconhecido: " + parametro);
        }
    }
    
    /**
     * Define novo quantum para o escalonador
     */
    public void setQuantum(int novoQuantum) {
        if (novoQuantum > 0) {
            this.quantum = novoQuantum;
            System.out.println("RR: Quantum do escalonador alterado para: " + quantum + " ciclos");
        }
    }
    
    public int getQuantum() {
        return quantum;
    }
    
    public int getQuantumRestante() {
        return quantumAtual;
    }
}