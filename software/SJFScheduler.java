package software;

import java.util.PriorityQueue;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementação do algoritmo Shortest Job First (SJF)
 * 
 * Características:
 * - Não-preemptivo: Processo executa até terminar ou bloquear
 * - Otimal para tempo médio de espera: Processa trabalhos mais curtos primeiro
 * - Requer estimativa de tempo de execução
 * - Pode causar starvation para processos longos
 * 
 * Etapa 3: Algoritmo SJF
 */
public class SJFScheduler implements Scheduler {
    
    // Fila de prioridade ordenada por tempo estimado de execução
    private PriorityQueue<ProcessControlBlock> filaProtos;
    private ProcessControlBlock processoAtual;      // Processo atualmente executando
    
    // Métricas e estatísticas
    private SchedulingMetrics metricas;
    private long cicloCPUAtual;           // Contador de ciclos para controle de tempo
    
    /**
     * Construtor do escalonador SJF
     */
    public SJFScheduler() {
        // Fila ordenada por tempo estimado de execução (menor primeiro)
        this.filaProtos = new PriorityQueue<>(new Comparator<ProcessControlBlock>() {
            @Override
            public int compare(ProcessControlBlock p1, ProcessControlBlock p2) {
                // Compara por tempo estimado de execução
                int tempoEstimado1 = p1.getTempoEstimadoExecucao();
                int tempoEstimado2 = p2.getTempoEstimadoExecucao();
                
                if (tempoEstimado1 != tempoEstimado2) {
                    return Integer.compare(tempoEstimado1, tempoEstimado2);
                }
                
                // Em caso de empate, usa ordem de chegada (PID menor = chegou primeiro)
                return Integer.compare(p1.getPid(), p2.getPid());
            }
        });
        
        this.processoAtual = null;
        this.metricas = new SchedulingMetrics();
        this.cicloCPUAtual = 0;
    }
    
    @Override
    public void adicionarProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.READY) {
            // Se não tem estimativa, usa tamanho do programa como estimativa
            if (pcb.getTempoEstimadoExecucao() <= 0) {
                int estimativa = Math.max(5, pcb.getTamanhoPrograma() * 2); // Estimativa conservadora
                pcb.setTempoEstimadoExecucao(estimativa);
                System.out.println("SJF: Estimativa gerada para " + pcb.getNome() + ": " + estimativa + " ciclos");
            }
            
            filaProtos.offer(pcb);
            System.out.println("SJF: Processo adicionado à fila: " + pcb.getNome() + 
                " (PID: " + pcb.getPid() + ", Estimativa: " + pcb.getTempoEstimadoExecucao() + " ciclos)");
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
        
        // SJF é não-preemptivo: processo atual continua executando
        return processoAtual;
    }
    
    /**
     * Executa troca de contexto (context switch)
     * Seleciona o processo com menor tempo estimado
     */
    private ProcessControlBlock executarContextSwitch() {
        // Seleciona processo com menor tempo estimado (cabeça da fila de prioridade)
        processoAtual = filaProtos.poll();
        
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.RUNNING);
            processoAtual.setTempoUltimaExecucao(cicloCPUAtual);
            metricas.registrarContextSwitch();
            metricas.registrarInicioExecucao();
            
            System.out.println("SJF: Context switch: " + processoAtual.getNome() + 
                " inicia execução (Estimativa: " + processoAtual.getTempoEstimadoExecucao() + " ciclos)");
        } else {
            System.out.println("SJF: Nenhum processo disponível para execução");
        }
        
        return processoAtual;
    }
    
    @Override
    public void executarCicloCPU() {
        cicloCPUAtual++;
        metricas.registrarCicloCPU();
        
        if (processoAtual != null) {
            processoAtual.adicionarTempoCPU(1);
            
            // Atualiza estimativa baseada no tempo real de execução
            atualizarEstimativa(processoAtual);
            
            // Atualiza tempo de espera para processos na fila
            for (ProcessControlBlock pcb : filaProtos) {
                pcb.adicionarTempoEspera(1);
            }
        }
    }
    
    /**
     * Atualiza estimativa de tempo com base no histórico de execução
     */
    private void atualizarEstimativa(ProcessControlBlock pcb) {
        // Fórmula de média exponencial para refinar estimativas
        // nova_estimativa = alfa * tempo_real + (1 - alfa) * estimativa_anterior
        double alfa = 0.3; // Peso para o tempo real observado
        
        long tempoReal = pcb.getTempoCPU();
        int estimativaAnterior = pcb.getTempoEstimadoExecucao();
        
        if (tempoReal > 0) {
            int novaEstimativa = (int) (alfa * tempoReal + (1 - alfa) * estimativaAnterior);
            pcb.setTempoEstimadoExecucao(novaEstimativa);
        }
    }
    
    @Override
    public boolean devePreemptar() {
        // SJF é não-preemptivo
        return false;
    }
    
    @Override
    public void bloquearProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.setEstado(ProcessState.WAITING);
            System.out.println("SJF: Processo bloqueado: " + processoAtual.getNome());
            processoAtual = null; // Remove da CPU para próximo processo
        }
    }
    
    @Override
    public void desbloquearProcesso(ProcessControlBlock pcb) {
        if (pcb != null && pcb.getEstado() == ProcessState.WAITING) {
            pcb.setEstado(ProcessState.READY);
            // Recalcula posição na fila baseado na estimativa atualizada
            adicionarProcesso(pcb);
            System.out.println("SJF: Processo desbloqueado: " + pcb.getNome());
        }
    }
    
    @Override
    public void finalizarProcessoAtual() {
        if (processoAtual != null) {
            processoAtual.finalizar();
            metricas.registrarFinalizacaoProcesso(processoAtual);
            
            // Feedback: compara tempo real vs estimado
            long tempoReal = processoAtual.getTempoCPU();
            int estimativa = processoAtual.getTempoEstimadoExecucao();
            double precisao = 100.0 * Math.min(tempoReal, estimativa) / Math.max(tempoReal, estimativa);
            
            System.out.println("SJF: Processo finalizado: " + processoAtual.getNome() + 
                " (PID: " + processoAtual.getPid() + 
                ", Real: " + tempoReal + 
                ", Estimado: " + estimativa + 
                ", Precisão: " + String.format("%.1f", precisao) + "%)");
            
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
        sb.append("=== Estatísticas do Escalonador SJF ===\n");
        sb.append("Algoritmo: Shortest Job First (Não-preemptivo)\n");
        sb.append("Ciclo atual da CPU: ").append(cicloCPUAtual).append("\n");
        sb.append("Processos na fila de prontos: ").append(filaProtos.size()).append("\n");
        
        if (processoAtual != null) {
            sb.append("Processo atual: ").append(processoAtual.getNome()).append(" (PID: ").append(processoAtual.getPid()).append(")\n");
            sb.append("Estimativa atual: ").append(processoAtual.getTempoEstimadoExecucao()).append(" ciclos\n");
            sb.append("Tempo executado: ").append(processoAtual.getTempoCPU()).append(" ciclos\n");
        } else {
            sb.append("Nenhum processo executando\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public void exibirEstado() {
        System.out.println("\n" + getEstatisticas());
        
        if (!filaProtos.isEmpty()) {
            System.out.println("Fila SJF (ordenada por estimativa de tempo):");
            int i = 1;
            List<ProcessControlBlock> lista = new ArrayList<>(filaProtos);
            for (ProcessControlBlock pcb : lista) {
                System.out.println("  " + i + ". " + pcb.toString() + 
                    " (Est: " + pcb.getTempoEstimadoExecucao() + " ciclos)");
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
        return "Shortest Job First (SJF)";
    }
    
    @Override
    public ProcessControlBlock forcarContextSwitch() {
        // Em SJF, só força context switch se não há processo atual
        if (processoAtual == null) {
            return executarContextSwitch();
        }
        // Caso contrário, não faz nada (SJF é não-preemptivo)
        System.out.println("SJF: Não é possível forçar context switch - algoritmo é não-preemptivo");
        return processoAtual;
    }
    
    @Override
    public SchedulingMetrics getMetricas() {
        return metricas;
    }
    
    @Override
    public void configurarParametros(String parametro, Object valor) {
        switch (parametro.toLowerCase()) {
            case "estimativa_padrao":
                // Poderia implementar configuração de estimativa padrão
                System.out.println("SJF: Parâmetro estimativa_padrao configurado");
                break;
            default:
                System.out.println("SJF: Parâmetro desconhecido: " + parametro);
        }
    }
    
    /**
     * Retorna estatísticas de precisão das estimativas
     */
    public String getEstatisticasPrecisao() {
        // Esta implementação seria expandida para coletar dados históricos
        return "Estatísticas de precisão das estimativas seriam implementadas com mais dados históricos";
    }
}