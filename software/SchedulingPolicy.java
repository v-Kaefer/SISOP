package software;

/**
 * Enumeração dos tipos de algoritmos de escalonamento disponíveis
 * 
 * Etapa 3: Suporte a múltiplos algoritmos de escalonamento
 */
public enum SchedulingPolicy {
    
    /**
     * Round-Robin: Cada processo recebe um quantum de tempo
     * Características: Preemptivo, justo, bom para sistemas interativos
     */
    ROUND_ROBIN("Round-Robin", true, "Quantum de tempo fixo para cada processo"),
    
    /**
     * First Come First Served: Processos executam na ordem de chegada
     * Características: Não-preemptivo, simples, pode causar convoy effect
     */
    FCFS("First Come First Served", false, "Processos executam na ordem de chegada"),
    
    /**
     * Shortest Job First: Processos com menor tempo de execução primeiro
     * Características: Não-preemptivo, minimiza tempo médio de espera
     */
    SJF("Shortest Job First", false, "Processos com menor tempo de execução primeiro"),
    
    /**
     * Shortest Remaining Time First: Versão preemptiva do SJF
     * Características: Preemptivo, ótimo para tempo médio de espera
     */
    SRTF("Shortest Remaining Time First", true, "Versão preemptiva do SJF"),
    
    /**
     * Priority Scheduling: Processos com maior prioridade executam primeiro
     * Características: Pode ser preemptivo ou não, com sistema de aging
     */
    PRIORITY("Priority Scheduling", true, "Processos com maior prioridade executam primeiro"),
    
    /**
     * Multilevel Queue: Múltiplas filas com diferentes prioridades
     * Características: Preemptivo, adequado para diferentes tipos de processo
     */
    MULTILEVEL("Multilevel Queue", true, "Múltiplas filas com diferentes prioridades"),
    
    /**
     * Multilevel Feedback Queue: Processos podem mudar entre filas
     * Características: Preemptivo, adaptativo, complexo mas eficiente
     */
    MULTILEVEL_FEEDBACK("Multilevel Feedback Queue", true, "Processos podem mudar entre filas dinamicamente");
    
    private final String nome;
    private final boolean preemptivo;
    private final String descricao;
    
    SchedulingPolicy(String nome, boolean preemptivo, String descricao) {
        this.nome = nome;
        this.preemptivo = preemptivo;
        this.descricao = descricao;
    }
    
    public String getNome() {
        return nome;
    }
    
    public boolean isPreemptivo() {
        return preemptivo;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    @Override
    public String toString() {
        return nome + (preemptivo ? " (Preemptivo)" : " (Não-preemptivo)");
    }
    
    /**
     * Retorna todas as políticas disponíveis como string
     */
    public static String listarPoliticas() {
        StringBuilder sb = new StringBuilder();
        sb.append("Algoritmos de Escalonamento Disponíveis:\n");
        for (SchedulingPolicy policy : values()) {
            sb.append("- ").append(policy.toString()).append(": ").append(policy.descricao).append("\n");
        }
        return sb.toString();
    }
}