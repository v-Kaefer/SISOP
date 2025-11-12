package exemplos;

import software.*;
import hardware.*;
import programs.*;

/**
 * Exemplo: Problema Produtor-Consumidor
 * 
 * Demonstra o uso de semáforos para sincronização entre processos
 * em um cenário clássico de sistemas operacionais.
 * 
 * Cenário:
 * - Um buffer compartilhado de tamanho limitado
 * - Produtores que adicionam itens ao buffer
 * - Consumidores que removem itens do buffer
 * - Sincronização usando semáforos para evitar:
 *   - Produtor adicionar quando buffer está cheio
 *   - Consumidor remover quando buffer está vazio
 *   - Condições de corrida no acesso ao buffer
 */
public class ExemploProdutorConsumidor {
    
    /**
     * Buffer compartilhado (simulado)
     */
    static class BufferCompartilhado {
        private int[] buffer;
        private int tamanho;
        private int count;
        private int in;  // Próxima posição para inserir
        private int out; // Próxima posição para remover
        
        // Semáforos para sincronização
        private Semaforo espacosVazios;  // Conta espaços vazios no buffer
        private Semaforo itensCheios;    // Conta itens disponíveis no buffer
        private Mutex mutexBuffer;       // Protege acesso ao buffer
        
        public BufferCompartilhado(int tamanho) {
            this.tamanho = tamanho;
            this.buffer = new int[tamanho];
            this.count = 0;
            this.in = 0;
            this.out = 0;
            
            // Inicializa semáforos
            this.espacosVazios = new Semaforo("EspacosVazios", tamanho); // Inicialmente todos vazios
            this.itensCheios = new Semaforo("ItensCheios", 0);           // Inicialmente nenhum item
            this.mutexBuffer = new Mutex("MutexBuffer");                  // Para exclusão mútua
            
            System.out.println("Buffer compartilhado criado (tamanho: " + tamanho + ")");
        }
        
        /**
         * Produtor: adiciona item ao buffer
         */
        public void produzir(ProcessControlBlock produtor, int item) {
            System.out.println("\n" + produtor.getNome() + " quer produzir item " + item);
            
            // Aguarda espaço vazio
            boolean temEspaco = espacosVazios.down(produtor);
            if (!temEspaco) {
                System.out.println("  -> " + produtor.getNome() + " BLOQUEADO (buffer cheio)");
                return; // Em implementação real, processo seria reescalonado
            }
            
            // Adquire exclusão mútua
            boolean adquiriuMutex = mutexBuffer.lock(produtor);
            if (!adquiriuMutex) {
                System.out.println("  -> " + produtor.getNome() + " BLOQUEADO (aguardando mutex)");
                return;
            }
            
            // === SEÇÃO CRÍTICA: insere no buffer ===
            buffer[in] = item;
            in = (in + 1) % tamanho;
            count++;
            System.out.println("  ✓ " + produtor.getNome() + " produziu item " + item + 
                             " (buffer: " + count + "/" + tamanho + ")");
            // === FIM DA SEÇÃO CRÍTICA ===
            
            // Libera exclusão mútua
            mutexBuffer.unlock(produtor);
            
            // Sinaliza item disponível
            itensCheios.up();
        }
        
        /**
         * Consumidor: remove item do buffer
         */
        public void consumir(ProcessControlBlock consumidor) {
            System.out.println("\n" + consumidor.getNome() + " quer consumir item");
            
            // Aguarda item disponível
            boolean temItem = itensCheios.down(consumidor);
            if (!temItem) {
                System.out.println("  -> " + consumidor.getNome() + " BLOQUEADO (buffer vazio)");
                return; // Em implementação real, processo seria reescalonado
            }
            
            // Adquire exclusão mútua
            boolean adquiriuMutex = mutexBuffer.lock(consumidor);
            if (!adquiriuMutex) {
                System.out.println("  -> " + consumidor.getNome() + " BLOQUEADO (aguardando mutex)");
                return;
            }
            
            // === SEÇÃO CRÍTICA: remove do buffer ===
            int item = buffer[out];
            out = (out + 1) % tamanho;
            count--;
            System.out.println("  ✓ " + consumidor.getNome() + " consumiu item " + item + 
                             " (buffer: " + count + "/" + tamanho + ")");
            // === FIM DA SEÇÃO CRÍTICA ===
            
            // Libera exclusão mútua
            mutexBuffer.unlock(consumidor);
            
            // Sinaliza espaço disponível
            espacosVazios.up();
        }
        
        public void exibirEstatisticas() {
            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║  Estatísticas do Buffer Compartilhado       ║");
            System.out.println("╚══════════════════════════════════════════════╝");
            System.out.println("Estado atual: " + count + "/" + tamanho + " itens");
            espacosVazios.exibirEstatisticas();
            itensCheios.exibirEstatisticas();
            mutexBuffer.exibirEstatisticas();
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
     * Main - demonstra o problema produtor-consumidor
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║  EXEMPLO: PROBLEMA PRODUTOR-CONSUMIDOR           ║");
        System.out.println("║  Sincronização com Semáforos                      ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        
        // Cria buffer compartilhado com capacidade 3
        BufferCompartilhado buffer = new BufferCompartilhado(3);
        
        // Cria processos produtores e consumidores
        ProcessControlBlock produtor1 = new ProcessControlBlock(1, "Produtor-1", criarPrograma());
        ProcessControlBlock produtor2 = new ProcessControlBlock(2, "Produtor-2", criarPrograma());
        ProcessControlBlock consumidor1 = new ProcessControlBlock(3, "Consumidor-1", criarPrograma());
        ProcessControlBlock consumidor2 = new ProcessControlBlock(4, "Consumidor-2", criarPrograma());
        
        produtor1.setEstado(ProcessState.RUNNING);
        produtor2.setEstado(ProcessState.RUNNING);
        consumidor1.setEstado(ProcessState.RUNNING);
        consumidor2.setEstado(ProcessState.RUNNING);
        
        System.out.println("\n=== SIMULAÇÃO DE EXECUÇÃO ===");
        
        // Sequência de operações demonstrando sincronização
        System.out.println("\n--- Fase 1: Produtores enchem o buffer ---");
        buffer.produzir(produtor1, 100);
        buffer.produzir(produtor2, 200);
        buffer.produzir(produtor1, 300);
        
        System.out.println("\n--- Fase 2: Produtor tenta adicionar (buffer cheio) ---");
        produtor2.setEstado(ProcessState.RUNNING); // Reset estado para simulação
        buffer.produzir(produtor2, 400); // Deve bloquear
        
        System.out.println("\n--- Fase 3: Consumidores removem itens ---");
        consumidor1.setEstado(ProcessState.RUNNING); // Reset estado
        buffer.consumir(consumidor1);
        
        System.out.println("\n--- Fase 4: Produtor bloqueado pode prosseguir ---");
        produtor2.setEstado(ProcessState.RUNNING); // Simula processo acordado
        buffer.produzir(produtor2, 400);
        
        System.out.println("\n--- Fase 5: Mais consumidores ---");
        consumidor2.setEstado(ProcessState.RUNNING);
        buffer.consumir(consumidor2);
        consumidor1.setEstado(ProcessState.RUNNING);
        buffer.consumir(consumidor1);
        
        System.out.println("\n--- Fase 6: Consumidor tenta remover (buffer com poucos itens) ---");
        consumidor2.setEstado(ProcessState.RUNNING);
        buffer.consumir(consumidor2);
        
        System.out.println("\n--- Fase 7: Mais produção ---");
        produtor1.setEstado(ProcessState.RUNNING);
        buffer.produzir(produtor1, 500);
        produtor2.setEstado(ProcessState.RUNNING);
        buffer.produzir(produtor2, 600);
        
        // Exibe estatísticas finais
        buffer.exibirEstatisticas();
        
        System.out.println("\n╔═══════════════════════════════════════════════════╗");
        System.out.println("║  CONCLUSÕES                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        System.out.println("✓ Semáforos evitaram condições de corrida");
        System.out.println("✓ Produtores bloqueiam quando buffer está cheio");
        System.out.println("✓ Consumidores bloqueiam quando buffer está vazio");
        System.out.println("✓ Mutex garante exclusão mútua no acesso ao buffer");
        System.out.println("\nEste exemplo demonstra sincronização clássica entre processos!");
    }
}
