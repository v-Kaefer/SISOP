package software;

import java.util.LinkedList;
import java.util.Queue;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo First Come First Served (FCFS)
 * 
 * Características:
 * - Não-preemptivo: Processo executa até terminar ou bloquear
 * - Simples: Processos executam na ordem de chegada
 * - Convoy Effect: Processos curtos podem esperar muito por processos longos
 * 
 * Etapa 3: Algoritmo FCFS
 */
public class FCFSScheduler implements Scheduler {
    
    // Fila de processos
    private Queue<ProcessControlBlock> filaProtos;  // Processos prontos para execução
    private ProcessControlBlock processoAtual;      // Processo atualmente executando
    
    // Métricas e estatísticas
    private SchedulingMetrics metricas;
    private long cicloCPUAtual;           // Contador de ciclos para controle de tempo
    
    /**
     * Construtor do escalonador FCFS
     */
    public FCFSScheduler() {
        this.filaProtos = new LinkedList<>();
        this.processoAtual = null;
        this.metricas = new SchedulingMetrics();
        this.cicloCPUAtual = 0;
    }
    
    @Override
    public void adicionarProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.READY) {
            filaProtos.offer(pcb);
            System.out.println("FCFS: Processo adicionado à fila: " + pcb.getNome() + " (PID: " + pcb.getPid() + ")");
        }
    }
    
    @Override
    public boolean removerProcesso(int pid) {
        return filaProtos.removeIf(pcb -> pcb.getPid() == pid);
    }
    
    @Override
    public ProcessControlBlock selecionarProximoProcesso() {
        // Se não há processo atual ou processo terminou
        if (processoAtual == null || processoAtual.isFinished()) {
            return executarContextSwitch();
        }
        
        // FCFS é não-preemptivo: processo atual continua executando
        return processoAtual;
    }
    
    /**
     * Executa troca de contexto (context switch)
     * Em FCFS, só acontece quando processo termina ou bloqueia
     */
    private ProcessControlBlock executarContextSwitch() {
        // Seleciona próximo processo da fila (FIFO)
        processoAtual = filaProtos.poll();
        
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.RUNNING);
            processoAtual.setTempoUltimaExecucao(cicloCPUAtual);
            metricas.registrarContextSwitch();
            metricas.registrarInicioExecucao();
            
            System.out.println("FCFS: Context switch: " + processoAtual.getNome() + " inicia execução");
        } else {
            System.out.println("FCFS: Nenhum processo disponível para execução");
        }
        
        return processoAtual;
    }
    
    @Override
    public void executarCicloCPU() {
        cicloCPUAtual++;
        metricas.registrarCicloCPU();
        
        if (processoAtual != null) {
            processoAtual.adicionarTempoCPU(1);
            
            // Atualiza tempo de espera para processos na fila
            for (ProcessControlBlock pcb : filaProtos) {
                pcb.adicionarTempoEspera(1);
            }
        }
    }
    
    @Override
    public boolean devePreemptar() {
        // FCFS é não-preemptivo
        return false;
    }
    
    @Override
    public void bloquearProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.WAITING);
            System.out.println("FCFS: Processo bloqueado: " + processoAtual.getNome());
            processoAtual = null; // Remove da CPU para próximo processo
        }
    }
    
    @Override
    public void desbloquearProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.WAITING) {
            pcb.setEstado(ProcessState.READY);
            // Em FCFS, processo desbloqueado vai para o final da fila
            adicionarProcesso(pcb);
            System.out.println("FCFS: Processo desbloqueado: " + pcb.getNome());
        }
    }
    
    @Override
    public void finalizarProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.finalizar();
            metricas.registrarFinalizacaoProcesso(processoAtual);
            System.out.println("FCFS: Processo finalizado: " + processoAtual.getNome() + " (PID: " + processoAtual.getPid() + ")");
            processoAtual = null;
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
        sb.append("=== Estatísticas do Escalonador FCFS ===\n");
        sb.append("Algoritmo: First Come First Served (Não-preemptivo)\n");
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
            System.out.println("Fila FCFS (ordem de chegada):");
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
        return "First Come First Served (FCFS)";
    }
    
    @Override
    public ProcessControlBlock forcarContextSwitch() {
        // Em FCFS, só força context switch se não há processo atual
        if (processoAtual == null) {
            return executarContextSwitch();
        }
        // Caso contrário, não faz nada (FCFS é não-preemptivo)
        System.out.println("FCFS: Não é possível forçar context switch - algoritmo é não-preemptivo");
        return processoAtual;
    }
    
    @Override
    public SchedulingMetrics getMetricas() {
        return metricas;
    }
    
    @Override
    public void configurarParametros(String parametro, Object valor) {
        // FCFS não tem parâmetros configuráveis
        System.out.println("FCFS: Algoritmo não possui parâmetros configuráveis");
    }
    
    /**
     * Retorna posição de um processo na fila
     */
    public int getPosicaoNaFila(int pid) {
        int posicao = 1;
        for (ProcessControlBlock pcb : filaProtos) {
            if (pcb.getPid() == pid) {
                return posicao;
            }
            posicao++;
        }
        return -1; // Não encontrado na fila
    }
    
    /**
     * Retorna o tempo estimado de espera para um processo
     */
    public long getTempoEstimadoEspera(int pid) {
        long tempoEstimado = 0;
        
        // Se há processo atual executando, conta o tempo restante dele
        if (processoAtual != null && !processoAtual.isFinished()) {
            // Estimativa simples: assume que processo atual vai executar por mais alguns ciclos
            tempoEstimado += 10; // Estimativa conservadora
        }
        
        // Soma tempo estimado dos processos na frente na fila
        for (ProcessControlBlock pcb : filaProtos) {
            if (pcb.getPid() == pid) {
                break; // Encontrou o processo, para de contar
            }
            // Estimativa: 20 ciclos por processo (pode ser refinada)
            tempoEstimado += 20;
        }
        
        return tempoEstimado;
    }
}