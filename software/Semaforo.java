package software;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Semáforo - Primitiva de sincronização entre processos
 * 
 * Implementa um semáforo contador clássico com operações P (down/wait) e V (up/signal).
 * Um semáforo possui:
 * - Um valor inteiro (contador de recursos disponíveis)
 * - Uma fila de processos bloqueados aguardando o recurso
 * 
 * Operações:
 * - down() / P() / wait(): Decrementa o contador. Se ≤ 0, bloqueia o processo
 * - up() / V() / signal(): Incrementa o contador e acorda um processo bloqueado
 */
public class Semaforo {
    
    // Identificação do semáforo
    private String nome;
    
    // Valor do semáforo (contador de recursos)
    private int valor;
    
    // Fila de processos bloqueados aguardando este semáforo
    private Queue<ProcessControlBlock> filaEspera;
    
    // Controle de acesso
    private boolean ocupado; // Flag para simular seção crítica do próprio semáforo
    
    // Estatísticas
    private int totalOperacoesDown;
    private int totalOperacoesUp;
    private int totalBloqueios;
    
    /**
     * Construtor do Semáforo
     * 
     * @param nome Nome identificador do semáforo
     * @param valorInicial Valor inicial do contador (geralmente >= 0)
     */
    public Semaforo(String nome, int valorInicial) {
        this.nome = nome;
        this.valor = valorInicial;
        this.filaEspera = new LinkedList<>();
        this.ocupado = false;
        
        this.totalOperacoesDown = 0;
        this.totalOperacoesUp = 0;
        this.totalBloqueios = 0;
        
        System.out.println("Semáforo criado: " + nome + " (valor inicial: " + valorInicial + ")");
    }
    
    /**
     * Operação DOWN (P, wait)
     * 
     * Decrementa o valor do semáforo. Se o valor ficar negativo ou zero,
     * bloqueia o processo chamador até que recursos estejam disponíveis.
     * 
     * @param processo Processo que está solicitando o recurso
     * @return true se o processo conseguiu o recurso, false se foi bloqueado
     */
    public synchronized boolean down(ProcessControlBlock processo) {
        totalOperacoesDown++;
        
        System.out.println("Semáforo[" + nome + "].down() chamado por " + processo.getNome() + 
                          " (valor atual: " + valor + ")");
        
        valor--;
        
        if (valor < 0) {
            // Não há recursos disponíveis - bloquear processo
            filaEspera.add(processo);
            processo.setEstado(ProcessState.WAITING);
            totalBloqueios++;
            
            System.out.println("  -> Processo " + processo.getNome() + " BLOQUEADO (fila de espera: " + 
                             filaEspera.size() + " processos)");
            return false;
        } else {
            // Recurso obtido com sucesso
            System.out.println("  -> Processo " + processo.getNome() + " obteve o recurso");
            return true;
        }
    }
    
    /**
     * Operação UP (V, signal)
     * 
     * Incrementa o valor do semáforo e acorda um processo bloqueado (se houver).
     * 
     * @return Processo que foi acordado, ou null se nenhum processo estava bloqueado
     */
    public synchronized ProcessControlBlock up() {
        totalOperacoesUp++;
        
        System.out.println("Semáforo[" + nome + "].up() chamado (valor atual: " + valor + ")");
        
        valor++;
        
        // Se há processos bloqueados, acorda um deles
        if (!filaEspera.isEmpty()) {
            ProcessControlBlock processoAcordado = filaEspera.poll();
            processoAcordado.setEstado(ProcessState.READY);
            
            System.out.println("  -> Processo " + processoAcordado.getNome() + " ACORDADO" +
                             " (fila de espera: " + filaEspera.size() + " processos)");
            return processoAcordado;
        } else {
            System.out.println("  -> Nenhum processo bloqueado");
            return null;
        }
    }
    
    /**
     * Versão não bloqueante do down
     * Tenta obter o recurso, mas não bloqueia se não disponível
     * 
     * @return true se conseguiu o recurso, false caso contrário
     */
    public synchronized boolean tryDown() {
        if (valor > 0) {
            valor--;
            totalOperacoesDown++;
            return true;
        }
        return false;
    }
    
    /**
     * Retorna o valor atual do semáforo
     */
    public synchronized int getValor() {
        return valor;
    }
    
    /**
     * Retorna o número de processos bloqueados
     */
    public synchronized int getProcessosBloqueados() {
        return filaEspera.size();
    }
    
    /**
     * Verifica se há processos bloqueados
     */
    public synchronized boolean temProcessosBloqueados() {
        return !filaEspera.isEmpty();
    }
    
    /**
     * Retorna o nome do semáforo
     */
    public String getNome() {
        return nome;
    }
    
    /**
     * Exibe estatísticas do semáforo
     */
    public void exibirEstatisticas() {
        System.out.println("\n=== Estatísticas do Semáforo: " + nome + " ===");
        System.out.println("Valor atual: " + valor);
        System.out.println("Processos bloqueados: " + filaEspera.size());
        System.out.println("Total de operações down(): " + totalOperacoesDown);
        System.out.println("Total de operações up(): " + totalOperacoesUp);
        System.out.println("Total de bloqueios: " + totalBloqueios);
        
        if (!filaEspera.isEmpty()) {
            System.out.println("\nProcessos na fila de espera:");
            int i = 1;
            for (ProcessControlBlock pcb : filaEspera) {
                System.out.println("  " + i + ". " + pcb.getNome() + " (PID: " + pcb.getPid() + ")");
                i++;
            }
        }
    }
    
    @Override
    public String toString() {
        return String.format("Semaforo[%s, valor=%d, bloqueados=%d]", 
                           nome, valor, filaEspera.size());
    }
}
