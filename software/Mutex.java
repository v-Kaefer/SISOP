package software;

/**
 * Mutex - Exclusão Mútua (Mutual Exclusion)
 * 
 * Implementa um mecanismo de exclusão mútua para proteger seções críticas.
 * Um Mutex é essencialmente um semáforo binário com propriedade de ownership.
 * 
 * Características:
 * - Apenas um processo pode possuir o mutex por vez
 * - Apenas o processo que fez lock() pode fazer unlock()
 * - Protege seções críticas de código
 * 
 * Operações:
 * - lock(): Adquire o mutex (bloqueia se já está ocupado)
 * - unlock(): Libera o mutex
 * - tryLock(): Tenta adquirir sem bloquear
 */
public class Mutex {
    
    // Identificação do mutex
    private String nome;
    
    // Estado do mutex
    private boolean disponivel;
    
    // Processo que atualmente possui o mutex (null se disponível)
    private ProcessControlBlock proprietario;
    
    // Semáforo interno para implementar a sincronização
    private Semaforo semaforo;
    
    // Estatísticas
    private int totalLocks;
    private int totalUnlocks;
    private int totalTentativasFalhadas;
    
    /**
     * Construtor do Mutex
     * 
     * @param nome Nome identificador do mutex
     */
    public Mutex(String nome) {
        this.nome = nome;
        this.disponivel = true;
        this.proprietario = null;
        
        // Usa um semáforo binário (valor inicial 1) para implementação
        this.semaforo = new Semaforo("Mutex-" + nome, 1);
        
        this.totalLocks = 0;
        this.totalUnlocks = 0;
        this.totalTentativasFalhadas = 0;
        
        System.out.println("Mutex criado: " + nome);
    }
    
    /**
     * Operação LOCK - Adquire o mutex
     * 
     * Se o mutex está disponível, adquire imediatamente.
     * Se está ocupado, bloqueia o processo até que seja liberado.
     * 
     * @param processo Processo que está tentando adquirir o mutex
     * @return true se adquiriu o mutex, false se foi bloqueado
     */
    public synchronized boolean lock(ProcessControlBlock processo) {
        System.out.println("Mutex[" + nome + "].lock() chamado por " + processo.getNome());
        
        // Verifica se o processo já possui o mutex (evita deadlock consigo mesmo)
        if (proprietario != null && proprietario.getPid() == processo.getPid()) {
            System.out.println("  AVISO: Processo " + processo.getNome() + 
                             " já possui o mutex (lock recursivo não permitido)");
            return false;
        }
        
        totalLocks++;
        
        // Tenta adquirir o semáforo
        boolean adquirido = semaforo.down(processo);
        
        if (adquirido) {
            // Mutex adquirido com sucesso
            disponivel = false;
            proprietario = processo;
            System.out.println("  -> Mutex ADQUIRIDO por " + processo.getNome());
            return true;
        } else {
            // Processo foi bloqueado
            System.out.println("  -> Processo " + processo.getNome() + " BLOQUEADO aguardando mutex");
            return false;
        }
    }
    
    /**
     * Operação UNLOCK - Libera o mutex
     * 
     * Apenas o processo que fez lock() pode fazer unlock().
     * 
     * @param processo Processo que está tentando liberar o mutex
     * @return true se liberou com sucesso, false se não é o proprietário
     */
    public synchronized boolean unlock(ProcessControlBlock processo) {
        System.out.println("Mutex[" + nome + "].unlock() chamado por " + processo.getNome());
        
        // Verifica se o processo realmente possui o mutex
        if (proprietario == null || proprietario.getPid() != processo.getPid()) {
            System.out.println("  ERRO: Processo " + processo.getNome() + 
                             " não possui o mutex (proprietário: " + 
                             (proprietario != null ? proprietario.getNome() : "nenhum") + ")");
            return false;
        }
        
        totalUnlocks++;
        
        // Libera o mutex
        ProcessControlBlock processoAcordado = semaforo.up();
        
        if (processoAcordado != null) {
            // Há um processo aguardando - ele será o novo proprietário
            proprietario = processoAcordado;
            System.out.println("  -> Mutex TRANSFERIDO para " + processoAcordado.getNome());
        } else {
            // Nenhum processo aguardando - mutex fica livre
            proprietario = null;
            disponivel = true;
            System.out.println("  -> Mutex LIBERADO");
        }
        
        return true;
    }
    
    /**
     * Versão não bloqueante do lock
     * Tenta adquirir o mutex, mas não bloqueia se não disponível
     * 
     * @param processo Processo tentando adquirir
     * @return true se adquiriu, false caso contrário
     */
    public synchronized boolean tryLock(ProcessControlBlock processo) {
        if (disponivel && proprietario == null) {
            boolean adquirido = semaforo.tryDown();
            if (adquirido) {
                disponivel = false;
                proprietario = processo;
                totalLocks++;
                System.out.println("Mutex[" + nome + "].tryLock() SUCESSO para " + processo.getNome());
                return true;
            }
        }
        
        totalTentativasFalhadas++;
        System.out.println("Mutex[" + nome + "].tryLock() FALHOU para " + processo.getNome());
        return false;
    }
    
    /**
     * Verifica se o mutex está disponível
     */
    public synchronized boolean isDisponivel() {
        return disponivel;
    }
    
    /**
     * Retorna o processo que possui o mutex (null se livre)
     */
    public synchronized ProcessControlBlock getProprietario() {
        return proprietario;
    }
    
    /**
     * Verifica se há processos bloqueados aguardando o mutex
     */
    public synchronized boolean temProcessosBloqueados() {
        return semaforo.temProcessosBloqueados();
    }
    
    /**
     * Retorna o número de processos bloqueados
     */
    public synchronized int getProcessosBloqueados() {
        return semaforo.getProcessosBloqueados();
    }
    
    /**
     * Retorna o nome do mutex
     */
    public String getNome() {
        return nome;
    }
    
    /**
     * Exibe estatísticas do mutex
     */
    public void exibirEstatisticas() {
        System.out.println("\n=== Estatísticas do Mutex: " + nome + " ===");
        System.out.println("Disponível: " + disponivel);
        System.out.println("Proprietário: " + (proprietario != null ? proprietario.getNome() : "nenhum"));
        System.out.println("Processos bloqueados: " + getProcessosBloqueados());
        System.out.println("Total de locks: " + totalLocks);
        System.out.println("Total de unlocks: " + totalUnlocks);
        System.out.println("Total de tentativas falhadas (tryLock): " + totalTentativasFalhadas);
        
        // Mostra estatísticas do semáforo interno
        semaforo.exibirEstatisticas();
    }
    
    @Override
    public String toString() {
        return String.format("Mutex[%s, disponivel=%s, proprietario=%s, bloqueados=%d]", 
                           nome, disponivel, 
                           (proprietario != null ? proprietario.getNome() : "nenhum"),
                           getProcessosBloqueados());
    }
}
