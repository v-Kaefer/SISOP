package software;

/**
 * Classe para armazenar métricas de performance do escalonamento
 * 
 * Coleta e calcula estatísticas importantes para avaliação
 * da eficiência dos algoritmos de escalonamento.
 * 
 * Etapa 3: Métricas Avançadas de Performance
 */
public class SchedulingMetrics {
    
    // Métricas básicas
    private long totalContextSwitches;
    private long totalCiclosCPU;
    private long totalProcessosExecutados;
    private long totalProcessosFinalizados;
    
    // Métricas de tempo
    private double tempoMedioEspera;
    private double tempoMedioTurnaround;
    private double tempoMedioResposta;
    private double throughput;
    
    // Métricas de eficiência
    private double utilizacaoCPU;
    private double overheadContextSwitch;
    
    // Contadores para cálculo
    private long somaTempoDEspera;
    private long somaTempoTurnaround;
    private long somaTempoResposta;
    private long tempoInicioSistema;
    
    public SchedulingMetrics() {
        this.tempoInicioSistema = System.currentTimeMillis();
        reset();
    }
    
    /**
     * Reseta todas as métricas
     */
    public void reset() {
        totalContextSwitches = 0;
        totalCiclosCPU = 0;
        totalProcessosExecutados = 0;
        totalProcessosFinalizados = 0;
        somaTempoDEspera = 0;
        somaTempoTurnaround = 0;
        somaTempoResposta = 0;
        recalcularMetricas();
    }
    
    /**
     * Registra um context switch
     */
    public void registrarContextSwitch() {
        totalContextSwitches++;
    }
    
    /**
     * Registra um ciclo de CPU executado
     */
    public void registrarCicloCPU() {
        totalCiclosCPU++;
    }
    
    /**
     * Registra que um processo começou a executar
     */
    public void registrarInicioExecucao() {
        totalProcessosExecutados++;
    }
    
    /**
     * Registra finalização de um processo
     */
    public void registrarFinalizacaoProcesso(ProcessControlBlock pcb) {
        totalProcessosFinalizados++;
        
        // Coleta métricas do processo
        somaTempoDEspera += pcb.getTempoEspera();
        
        // Turnaround time = tempo total no sistema
        long turnaround = pcb.getTempoCPU() + pcb.getTempoEspera();
        somaTempoTurnaround += turnaround;
        
        // Response time = tempo até primeira execução (simplificado)
        somaTempoResposta += pcb.getTempoResposta();
        
        recalcularMetricas();
    }
    
    /**
     * Recalcula as métricas derivadas
     */
    private void recalcularMetricas() {
        if (totalProcessosFinalizados > 0) {
            tempoMedioEspera = (double) somaTempoDEspera / totalProcessosFinalizados;
            tempoMedioTurnaround = (double) somaTempoTurnaround / totalProcessosFinalizados;
            tempoMedioResposta = (double) somaTempoResposta / totalProcessosFinalizados;
        }
        
        // Throughput: processos finalizados por unidade de tempo
        long tempoExecucao = System.currentTimeMillis() - tempoInicioSistema;
        if (tempoExecucao > 0) {
            throughput = (double) totalProcessosFinalizados / (tempoExecucao / 1000.0);
        }
        
        // Utilização da CPU (assumindo que context switches têm overhead)
        if (totalCiclosCPU > 0) {
            overheadContextSwitch = (double) totalContextSwitches / totalCiclosCPU * 100;
            utilizacaoCPU = Math.max(0, 100.0 - overheadContextSwitch);
        }
    }
    
    // Getters
    public long getTotalContextSwitches() { return totalContextSwitches; }
    public long getTotalCiclosCPU() { return totalCiclosCPU; }
    public long getTotalProcessosExecutados() { return totalProcessosExecutados; }
    public long getTotalProcessosFinalizados() { return totalProcessosFinalizados; }
    public double getTempoMedioEspera() { return tempoMedioEspera; }
    public double getTempoMedioTurnaround() { return tempoMedioTurnaround; }
    public double getTempoMedioResposta() { return tempoMedioResposta; }
    public double getThroughput() { return throughput; }
    public double getUtilizacaoCPU() { return utilizacaoCPU; }
    public double getOverheadContextSwitch() { return overheadContextSwitch; }
    
    /**
     * Gera relatório completo das métricas
     */
    public String gerarRelatorio() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== MÉTRICAS DE PERFORMANCE DO ESCALONAMENTO ===\n");
        sb.append(String.format("Total de Context Switches: %d\n", totalContextSwitches));
        sb.append(String.format("Total de Ciclos de CPU: %d\n", totalCiclosCPU));
        sb.append(String.format("Processos Executados: %d\n", totalProcessosExecutados));
        sb.append(String.format("Processos Finalizados: %d\n", totalProcessosFinalizados));
        sb.append("\n--- TEMPOS MÉDIOS ---\n");
        sb.append(String.format("Tempo Médio de Espera: %.2f ciclos\n", tempoMedioEspera));
        sb.append(String.format("Tempo Médio de Turnaround: %.2f ciclos\n", tempoMedioTurnaround));
        sb.append(String.format("Tempo Médio de Resposta: %.2f ciclos\n", tempoMedioResposta));
        sb.append("\n--- EFICIÊNCIA ---\n");
        sb.append(String.format("Throughput: %.2f processos/segundo\n", throughput));
        sb.append(String.format("Utilização da CPU: %.1f%%\n", utilizacaoCPU));
        sb.append(String.format("Overhead de Context Switch: %.1f%%\n", overheadContextSwitch));
        
        return sb.toString();
    }
    
    /**
     * Compara métricas com outro conjunto de métricas
     */
    public String compararCom(SchedulingMetrics outras, String nomeOutro) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== COMPARAÇÃO DE MÉTRICAS ===\n");
        sb.append(String.format("%-25s | %-15s | %-15s | %-10s\n", "Métrica", "Atual", nomeOutro, "Melhor"));
        sb.append("-".repeat(70)).append("\n");
        
        // Tempo médio de espera (menor é melhor)
        String melhorEspera = tempoMedioEspera <= outras.tempoMedioEspera ? "Atual" : nomeOutro;
        sb.append(String.format("%-25s | %-15.2f | %-15.2f | %-10s\n", 
            "Tempo Médio Espera", tempoMedioEspera, outras.tempoMedioEspera, melhorEspera));
            
        // Throughput (maior é melhor)
        String melhorThroughput = throughput >= outras.throughput ? "Atual" : nomeOutro;
        sb.append(String.format("%-25s | %-15.2f | %-15.2f | %-10s\n", 
            "Throughput", throughput, outras.throughput, melhorThroughput));
            
        // Context switches (menor overhead é melhor)
        String melhorContext = overheadContextSwitch <= outras.overheadContextSwitch ? "Atual" : nomeOutro;
        sb.append(String.format("%-25s | %-15.1f | %-15.1f | %-10s\n", 
            "Overhead Context (%)", overheadContextSwitch, outras.overheadContextSwitch, melhorContext));
        
        return sb.toString();
    }
}