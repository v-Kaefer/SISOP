package software;

import hardware.Word;
import hardware.Interrupts;

/**
 * Process Control Block (PCB) - Bloco de Controle de Processo
 * 
 * Armazena todas as informações necessárias sobre um processo, incluindo:
 * - Identificação do processo (PID, nome)
 * - Estado atual do processo
 * - Contexto da CPU (PC, registradores)
 * - Informações de memória (tabela de páginas)
 * - Estatísticas de execução
 */
public class ProcessControlBlock {
    // Identificação do processo
    private int pid;                    // Process ID único
    private String nome;                // Nome do processo
    
    // Estado do processo
    private ProcessState estado;        // Estado atual (NEW, READY, RUNNING, etc.)
    
    // Contexto da CPU (salvo durante context switch)
    private int pc;                     // Program Counter
    private int[] registradores;        // Estado dos registradores R0-R9
    private Interrupts interrupcao;     // Estado de interrupção
    
    // Gerenciamento de memória
    private int[] tabelaPaginas;        // Tabela de páginas do processo
    private Word[] programa;            // Código do programa
    private int tamanhoPrograma;        // Número de instruções
    
    // Estatísticas de execução
    private long tempoCPU;              // Tempo total de CPU usado (em ciclos)
    private long tempoEspera;           // Tempo aguardando na fila de prontos
    private long tempoInicioExecucao;   // Timestamp do início da execução
    private int prioridade;             // Prioridade do processo (para futuros escalonadores)
    
    // Controle de escalonamento
    private long tempoUltimaExecucao;   // Último momento que executou
    private int quantumRestante;        // Quantum restante no Round-Robin
    
    /**
     * Construtor do PCB
     */
    public ProcessControlBlock(int pid, String nome, Word[] programa) {
        this.pid = pid;
        this.nome = nome;
        this.programa = programa.clone(); // Cópia do programa
        this.tamanhoPrograma = programa.length;
        
        // Estado inicial
        this.estado = ProcessState.NEW;
        
        // Contexto inicial da CPU
        this.pc = 0;
        this.registradores = new int[10]; // R0-R9
        this.interrupcao = Interrupts.noInterrupt;
        
        // Estatísticas iniciais
        this.tempoCPU = 0;
        this.tempoEspera = 0;
        this.prioridade = 1; // Prioridade padrão
        this.quantumRestante = 0;
    }
    
    // === GETTERS E SETTERS ===
    
    public int getPid() {
        return pid;
    }
    
    public String getNome() {
        return nome;
    }
    
    public ProcessState getEstado() {
        return estado;
    }
    
    public void setEstado(ProcessState novoEstado) {
        this.estado = novoEstado;
    }
    
    public int getPc() {
        return pc;
    }
    
    public void setPc(int pc) {
        this.pc = pc;
    }
    
    public int[] getRegistradores() {
        return registradores.clone(); // Retorna cópia para proteção
    }
    
    public void setRegistradores(int[] registradores) {
        System.arraycopy(registradores, 0, this.registradores, 0, Math.min(registradores.length, 10));
    }
    
    public int getRegistrador(int indice) {
        if (indice >= 0 && indice < registradores.length) {
            return registradores[indice];
        }
        return 0;
    }
    
    public void setRegistrador(int indice, int valor) {
        if (indice >= 0 && indice < registradores.length) {
            registradores[indice] = valor;
        }
    }
    
    public Interrupts getInterrupcao() {
        return interrupcao;
    }
    
    public void setInterrupcao(Interrupts interrupcao) {
        this.interrupcao = interrupcao;
    }
    
    public int[] getTabelaPaginas() {
        return tabelaPaginas;
    }
    
    public void setTabelaPaginas(int[] tabelaPaginas) {
        this.tabelaPaginas = tabelaPaginas.clone();
    }
    
    public Word[] getPrograma() {
        return programa.clone(); // Retorna cópia para proteção
    }
    
    public int getTamanhoPrograma() {
        return tamanhoPrograma;
    }
    
    public long getTempoCPU() {
        return tempoCPU;
    }
    
    public void adicionarTempoCPU(long ciclos) {
        this.tempoCPU += ciclos;
    }
    
    public long getTempoEspera() {
        return tempoEspera;
    }
    
    public void adicionarTempoEspera(long tempo) {
        this.tempoEspera += tempo;
    }
    
    public int getPrioridade() {
        return prioridade;
    }
    
    public void setPrioridade(int prioridade) {
        this.prioridade = prioridade;
    }
    
    public int getQuantumRestante() {
        return quantumRestante;
    }
    
    public void setQuantumRestante(int quantum) {
        this.quantumRestante = quantum;
    }
    
    public void decrementarQuantum() {
        if (quantumRestante > 0) {
            quantumRestante--;
        }
    }
    
    public long getTempoUltimaExecucao() {
        return tempoUltimaExecucao;
    }
    
    public void setTempoUltimaExecucao(long tempo) {
        this.tempoUltimaExecucao = tempo;
    }
    
    /**
     * Salva o contexto completo da CPU neste PCB
     */
    public void salvarContexto(int pc, int[] registradores, Interrupts interrupcao) {
        this.pc = pc;
        setRegistradores(registradores);
        this.interrupcao = interrupcao;
    }
    
    /**
     * Verifica se o processo pode ser executado
     */
    public boolean podeExecutar() {
        return estado == ProcessState.READY && !isFinished();
    }
    
    /**
     * Verifica se o processo terminou
     */
    public boolean isFinished() {
        return estado == ProcessState.TERMINATED;
    }
    
    /**
     * Finaliza o processo
     */
    public void finalizar() {
        setEstado(ProcessState.TERMINATED);
    }
    
    /**
     * Representação textual do PCB para debug
     */
    @Override
    public String toString() {
        return String.format("PCB[PID=%d, Nome=%s, Estado=%s, PC=%d, CPU=%d ciclos, Espera=%d, Prioridade=%d]",
                pid, nome, estado, pc, tempoCPU, tempoEspera, prioridade);
    }
    
    /**
     * Representação detalhada do PCB
     */
    public String toDetailedString() {
        StringBuilder sb = new StringBuilder();
        sb.append(toString()).append("\n");
        sb.append("  Registradores: ");
        for (int i = 0; i < registradores.length; i++) {
            sb.append(String.format("R%d=%d ", i, registradores[i]));
        }
        sb.append("\n");
        sb.append(String.format("  Tamanho do programa: %d instruções\n", tamanhoPrograma));
        sb.append(String.format("  Quantum restante: %d\n", quantumRestante));
        return sb.toString();
    }
}