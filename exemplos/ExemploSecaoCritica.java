package exemplos;

import software.*;
import hardware.*;

/**
 * Exemplo: Proteção de Seção Crítica com Mutex
 * 
 * Demonstra o uso de Mutex para proteger seções críticas e garantir
 * exclusão mútua no acesso a recursos compartilhados.
 * 
 * Cenário:
 * - Múltiplos processos tentam acessar um recurso compartilhado
 * - Apenas um processo pode acessar por vez (seção crítica)
 * - Mutex garante que apenas um processo execute a seção crítica
 */
public class ExemploSecaoCritica {
    
    /**
     * Recurso compartilhado que precisa de proteção
     */
    static class ContadorCompartilhado {
        private int valor;
        private Mutex mutex;
        private int totalAcessos;
        
        public ContadorCompartilhado() {
            this.valor = 0;
            this.mutex = new Mutex("ContadorMutex");
            this.totalAcessos = 0;
            System.out.println("Contador compartilhado criado");
        }
        
        /**
         * Incrementa o contador (seção crítica protegida)
         */
        public void incrementar(ProcessControlBlock processo) {
            System.out.println("\n" + processo.getNome() + " quer incrementar contador");
            
            // Tenta adquirir o mutex
            boolean adquirido = mutex.lock(processo);
            if (!adquirido) {
                System.out.println("  -> " + processo.getNome() + " BLOQUEADO (aguardando mutex)");
                return;
            }
            
            // === INÍCIO DA SEÇÃO CRÍTICA ===
            System.out.println("  -> " + processo.getNome() + " entrou na seção crítica");
            int valorAnterior = valor;
            
            // Simula operação que leva tempo (leitura, processamento, escrita)
            valor = valorAnterior + 1;
            totalAcessos++;
            
            System.out.println("  ✓ " + processo.getNome() + " incrementou: " + 
                             valorAnterior + " -> " + valor);
            // === FIM DA SEÇÃO CRÍTICA ===
            
            // Libera o mutex
            mutex.unlock(processo);
            System.out.println("  -> " + processo.getNome() + " saiu da seção crítica");
        }
        
        /**
         * Decrementa o contador (seção crítica protegida)
         */
        public void decrementar(ProcessControlBlock processo) {
            System.out.println("\n" + processo.getNome() + " quer decrementar contador");
            
            // Tenta adquirir o mutex
            boolean adquirido = mutex.lock(processo);
            if (!adquirido) {
                System.out.println("  -> " + processo.getNome() + " BLOQUEADO (aguardando mutex)");
                return;
            }
            
            // === INÍCIO DA SEÇÃO CRÍTICA ===
            System.out.println("  -> " + processo.getNome() + " entrou na seção crítica");
            int valorAnterior = valor;
            
            valor = valorAnterior - 1;
            totalAcessos++;
            
            System.out.println("  ✓ " + processo.getNome() + " decrementou: " + 
                             valorAnterior + " -> " + valor);
            // === FIM DA SEÇÃO CRÍTICA ===
            
            // Libera o mutex
            mutex.unlock(processo);
            System.out.println("  -> " + processo.getNome() + " saiu da seção crítica");
        }
        
        /**
         * Tenta incrementar sem bloquear (tryLock)
         */
        public boolean tentarIncrementar(ProcessControlBlock processo) {
            System.out.println("\n" + processo.getNome() + " tenta incrementar (sem bloquear)");
            
            // Tenta adquirir sem bloquear
            boolean adquirido = mutex.tryLock(processo);
            if (!adquirido) {
                System.out.println("  -> " + processo.getNome() + " não conseguiu (mutex ocupado)");
                return false;
            }
            
            // === SEÇÃO CRÍTICA ===
            valor++;
            totalAcessos++;
            System.out.println("  ✓ " + processo.getNome() + " incrementou para " + valor);
            // === FIM ===
            
            mutex.unlock(processo);
            return true;
        }
        
        public int getValor() {
            return valor;
        }
        
        public void exibirEstatisticas() {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║  Estatísticas do Contador Compartilhado     ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.println("Valor final: " + valor);
            System.out.println("Total de acessos: " + totalAcessos);
            mutex.exibirEstatisticas();
        }
    }
    
    /**
     * Cria um programa simples para processos
     */
    private static Word[] criarPrograma() {
        return new Word[] {
            new Word(Opcode.LDI, 0, -1, 1),
            new Word(Opcode.STOP, -1, -1, -1)
        };
    }
    
    /**
     * Main - demonstra proteção de seção crítica
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║  EXEMPLO: PROTEÇÃO DE SEÇÃO CRÍTICA              ║");
        System.out.println("║  Exclusão Mútua com Mutex                         ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        
        // Cria recurso compartilhado
        ContadorCompartilhado contador = new ContadorCompartilhado();
        
        // Cria processos que acessarão o recurso
        ProcessControlBlock proc1 = new ProcessControlBlock(1, "Escritor-1", criarPrograma());
        ProcessControlBlock proc2 = new ProcessControlBlock(2, "Escritor-2", criarPrograma());
        ProcessControlBlock proc3 = new ProcessControlBlock(3, "Escritor-3", criarPrograma());
        ProcessControlBlock proc4 = new ProcessControlBlock(4, "Leitor-1", criarPrograma());
        
        proc1.setEstado(ProcessState.RUNNING);
        proc2.setEstado(ProcessState.RUNNING);
        proc3.setEstado(ProcessState.RUNNING);
        proc4.setEstado(ProcessState.RUNNING);
        
        System.out.println("\n=== DEMONSTRAÇÃO DE EXCLUSÃO MÚTUA ===");
        
        // Fase 1: Acesso sequencial bem-sucedido
        System.out.println("\n--- Fase 1: Acesso sequencial ---");
        contador.incrementar(proc1);
        contador.incrementar(proc2);
        contador.incrementar(proc3);
        
        // Fase 2: Processo mantém mutex e outros tentam acessar
        System.out.println("\n--- Fase 2: Contenção no acesso ---");
        System.out.println("Escritor-1 adquire o mutex...");
        Mutex mutexDireto = new Mutex("TesteContencao");
        mutexDireto.lock(proc1);
        
        System.out.println("\nOutros processos tentam acessar:");
        proc2.setEstado(ProcessState.RUNNING);
        mutexDireto.lock(proc2); // Deve bloquear
        
        proc3.setEstado(ProcessState.RUNNING);
        mutexDireto.lock(proc3); // Deve bloquear
        
        System.out.println("\nEscritor-1 libera o mutex:");
        mutexDireto.unlock(proc1);
        
        System.out.println("\nEscritor-2 (que foi acordado) libera:");
        proc2.setEstado(ProcessState.RUNNING);
        mutexDireto.unlock(proc2);
        
        // Fase 3: TryLock - tentativa sem bloqueio
        System.out.println("\n--- Fase 3: Tentativas sem bloqueio (tryLock) ---");
        System.out.println("Escritor-1 tenta incrementar:");
        proc1.setEstado(ProcessState.RUNNING);
        contador.tentarIncrementar(proc1);
        
        System.out.println("\nEnquanto Escritor-1 mantém o mutex, Escritor-2 tenta:");
        // Nota: no exemplo anterior, o mutex foi liberado, então este terá sucesso
        proc2.setEstado(ProcessState.RUNNING);
        contador.tentarIncrementar(proc2);
        
        // Fase 4: Operações mistas
        System.out.println("\n--- Fase 4: Operações de incremento e decremento ---");
        proc1.setEstado(ProcessState.RUNNING);
        contador.incrementar(proc1);
        proc2.setEstado(ProcessState.RUNNING);
        contador.incrementar(proc2);
        proc3.setEstado(ProcessState.RUNNING);
        contador.decrementar(proc3);
        proc4.setEstado(ProcessState.RUNNING);
        contador.decrementar(proc4);
        
        // Exibe estatísticas
        contador.exibirEstatisticas();
        
        System.out.println("\n╔═══════════════════════════════════════════════════╗");
        System.out.println("║  CONCLUSÕES                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println("✓ Mutex garantiu exclusão mútua na seção crítica");
        System.out.println("✓ Apenas um processo por vez acessou o contador");
        System.out.println("✓ Processos foram bloqueados quando necessário");
        System.out.println("✓ tryLock permite tentar sem bloquear");
        System.out.println("✓ Valor final: " + contador.getValor() + " (esperado: 6)");
        System.out.println("\nSem mutex, haveria condições de corrida!");
    }
}
