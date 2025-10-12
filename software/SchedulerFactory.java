package software;

/**
 * Factory para criação de diferentes tipos de escalonadores
 * 
 * Implementa o padrão Factory para permitir criação dinâmica
 * de algoritmos de escalonamento conforme necessário.
 * 
 * Etapa 3: Fábrica de Escalonadores
 */
public class SchedulerFactory {
    
    /**
     * Cria um escalonador baseado na política especificada
     */
    public static Scheduler criarEscalonador(SchedulingPolicy policy) {
        return criarEscalonador(policy, null);
    }
    
    /**
     * Cria um escalonador com parâmetros específicos
     */
    public static Scheduler criarEscalonador(SchedulingPolicy policy, Object[] parametros) {
        switch (policy) {
            case ROUND_ROBIN:
                int quantum = (parametros != null && parametros.length > 0) ? 
                    (Integer) parametros[0] : 10;
                return new RoundRobinSchedulerImpl(quantum);
                
            case FCFS:
                return new FCFSScheduler();
                
            case SJF:
                return new SJFScheduler();
                
            case SRTF:
                return new SRTFScheduler();
                
            case PRIORITY:
                boolean aging = (parametros != null && parametros.length > 0) ? 
                    (Boolean) parametros[0] : true;
                return new PriorityScheduler(aging);
                
            case MULTILEVEL:
                int numFilas = (parametros != null && parametros.length > 0) ? 
                    (Integer) parametros[0] : 3;
                return new MultilevelScheduler(numFilas);
                
            case MULTILEVEL_FEEDBACK:
                int numFilasFeedback = (parametros != null && parametros.length > 0) ? 
                    (Integer) parametros[0] : 3;
                return new MultilevelFeedbackScheduler(numFilasFeedback);
                
            default:
                System.err.println("Política de escalonamento não implementada: " + policy);
                return new RoundRobinSchedulerImpl(10); // Fallback para Round-Robin
        }
    }
    
    /**
     * Cria um escalonador Round-Robin com quantum específico
     */
    public static Scheduler criarRoundRobin(int quantum) {
        return new RoundRobinSchedulerImpl(quantum);
    }
    
    /**
     * Cria um escalonador FCFS
     */
    public static Scheduler criarFCFS() {
        return new FCFSScheduler();
    }
    
    /**
     * Cria um escalonador SJF
     */
    public static Scheduler criarSJF() {
        return new SJFScheduler();
    }
    
    /**
     * Cria um escalonador por prioridade
     */
    public static Scheduler criarPrioridade(boolean comAging) {
        return new PriorityScheduler(comAging);
    }
    
    /**
     * Lista todos os escalonadores disponíveis
     */
    public static String listarEscalonadoresDisponiveis() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ESCALONADORES DISPONÍVEIS ===\n");
        
        for (SchedulingPolicy policy : SchedulingPolicy.values()) {
            sb.append(String.format("%-25s: %s\n", 
                policy.getNome(), 
                policy.getDescricao()));
        }
        
        return sb.toString();
    }
    
    /**
     * Valida se uma política é suportada
     */
    public static boolean isPoliticaSuportada(SchedulingPolicy policy) {
        try {
            criarEscalonador(policy);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Retorna configurações recomendadas para cada algoritmo
     */
    public static String getConfiguracaoRecomendada(SchedulingPolicy policy) {
        switch (policy) {
            case ROUND_ROBIN:
                return "Quantum recomendado: 10-20 ciclos (ajustar conforme carga)";
            case FCFS:
                return "Sem parâmetros específicos. Bom para batch processing.";
            case SJF:
                return "Requer estimativa de tempo de execução. Ótimo para minimizar tempo médio.";
            case SRTF:
                return "Versão preemptiva do SJF. Melhor para sistemas interativos.";
            case PRIORITY:
                return "Recomenda-se ativar aging para evitar starvation.";
            case MULTILEVEL:
                return "3-4 filas com diferentes prioridades. Separar por tipo de processo.";
            case MULTILEVEL_FEEDBACK:
                return "Complexo mas adaptativo. Quantum menor para filas de maior prioridade.";
            default:
                return "Configuração não disponível.";
        }
    }
}