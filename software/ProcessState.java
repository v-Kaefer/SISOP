package software;

/**
 * Estados de processo implementados no sistema SISOP
 * 
 * Estados seguem o modelo clássico de sistemas operacionais:
 * - NEW: Processo recém-criado, ainda não foi admitido pelo escalonador
 * - READY: Processo pronto para execução, aguardando ser selecionado pelo escalonador
 * - RUNNING: Processo atualmente executando na CPU
 * - WAITING: Processo bloqueado aguardando por algum evento (I/O, etc.)
 * - TERMINATED: Processo finalizado, recursos podem ser liberados
 */
public enum ProcessState {
    NEW("NEW"),         // Processo criado, ainda não pronto
    READY("READY"),     // Pronto para execução
    RUNNING("RUNNING"), // Executando na CPU
    WAITING("WAITING"), // Bloqueado aguardando evento
    TERMINATED("TERMINATED"); // Finalizado
    
    private final String description;
    
    ProcessState(String description) {
        this.description = description;
    }
    
    @Override
    public String toString() {
        return description;
    }
    
    /**
     * Verifica se o processo está em um estado ativo (pode receber CPU)
     */
    public boolean isActive() {
        return this == READY || this == RUNNING;
    }
    
    /**
     * Verifica se o processo pode ser escalonado
     */
    public boolean canBeScheduled() {
        return this == READY;
    }
    
    /**
     * Verifica se o processo terminou
     */
    public boolean isFinished() {
        return this == TERMINATED;
    }
}