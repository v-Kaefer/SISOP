package software;

import hardware.Word;
import hardware.Opcode;

/**
 * Testes para Primitivas de Sincronização
 * 
 * Valida o funcionamento de:
 * - Semáforos (operações down/up)
 * - Mutex (operações lock/unlock)
 * - Integração com ProcessManager
 */
public class TesteSincronizacao {
    
    /**
     * Cria um programa simples para testes
     */
    private static Word[] criarProgramaSimples() {
        return new Word[] {
            new Word(Opcode.LDI, 0, -1, 5),   // R0 = 5
            new Word(Opcode.LDI, 1, -1, 3),   // R1 = 3
            new Word(Opcode.ADD, 2, 0, 1),    // R2 = R0 + R1
            new Word(Opcode.STD, 2, -1, 50),  // Mem[50] = R2
            new Word(Opcode.STOP, -1, -1, -1) // Para
        };
    }
    
    /**
     * Teste 1: Criação e operações básicas do Semáforo
     */
    private static boolean testeSemaforoBasico() {
        System.out.println("\n=== TESTE 1: Semáforo Básico ===");
        
        try {
            // Cria um semáforo com valor inicial 2
            Semaforo sem = new Semaforo("TesteSem", 2);
            
            // Cria processos de teste
            ProcessControlBlock p1 = new ProcessControlBlock(1, "Processo1", criarProgramaSimples());
            ProcessControlBlock p2 = new ProcessControlBlock(2, "Processo2", criarProgramaSimples());
            ProcessControlBlock p3 = new ProcessControlBlock(3, "Processo3", criarProgramaSimples());
            
            p1.setEstado(ProcessState.RUNNING);
            p2.setEstado(ProcessState.RUNNING);
            p3.setEstado(ProcessState.RUNNING);
            
            // Testa operações down
            System.out.println("\nTestando operações down:");
            boolean r1 = sem.down(p1); // Deve ter sucesso (valor: 2->1)
            boolean r2 = sem.down(p2); // Deve ter sucesso (valor: 1->0)
            boolean r3 = sem.down(p3); // Deve bloquear (valor: 0->-1)
            
            if (!r1 || !r2 || r3) {
                System.out.println("ERRO: Comportamento incorreto do down()");
                return false;
            }
            
            if (p3.getEstado() != ProcessState.WAITING) {
                System.out.println("ERRO: Processo 3 deveria estar WAITING");
                return false;
            }
            
            // Testa operações up
            System.out.println("\nTestando operações up:");
            ProcessControlBlock acordado = sem.up(); // Deve acordar p3 (valor: -1->0)
            
            if (acordado != p3) {
                System.out.println("ERRO: Processo errado foi acordado");
                return false;
            }
            
            if (p3.getEstado() != ProcessState.READY) {
                System.out.println("ERRO: Processo 3 deveria estar READY após ser acordado");
                return false;
            }
            
            sem.exibirEstatisticas();
            
            System.out.println("✓ Teste Semáforo Básico passou");
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Teste 2: Criação e operações básicas do Mutex
     */
    private static boolean testeMutexBasico() {
        System.out.println("\n=== TESTE 2: Mutex Básico ===");
        
        try {
            // Cria um mutex
            Mutex mutex = new Mutex("TesteMutex");
            
            // Cria processos de teste
            ProcessControlBlock p1 = new ProcessControlBlock(1, "Processo1", criarProgramaSimples());
            ProcessControlBlock p2 = new ProcessControlBlock(2, "Processo2", criarProgramaSimples());
            ProcessControlBlock p3 = new ProcessControlBlock(3, "Processo3", criarProgramaSimples());
            
            p1.setEstado(ProcessState.RUNNING);
            p2.setEstado(ProcessState.RUNNING);
            p3.setEstado(ProcessState.RUNNING);
            
            // Primeiro lock - deve ter sucesso
            System.out.println("\nTestando primeiro lock:");
            boolean r1 = mutex.lock(p1);
            if (!r1) {
                System.out.println("ERRO: Primeiro lock deveria ter sucesso");
                return false;
            }
            
            if (mutex.getProprietario() != p1) {
                System.out.println("ERRO: P1 deveria ser o proprietário");
                return false;
            }
            
            // Segundo lock - deve bloquear
            System.out.println("\nTestando segundo lock (deve bloquear):");
            boolean r2 = mutex.lock(p2);
            if (r2) {
                System.out.println("ERRO: Segundo lock deveria bloquear");
                return false;
            }
            
            if (p2.getEstado() != ProcessState.WAITING) {
                System.out.println("ERRO: P2 deveria estar WAITING");
                return false;
            }
            
            // Unlock - deve transferir para p2
            System.out.println("\nTestando unlock:");
            boolean r3 = mutex.unlock(p1);
            if (!r3) {
                System.out.println("ERRO: Unlock deveria ter sucesso");
                return false;
            }
            
            if (mutex.getProprietario() != p2) {
                System.out.println("ERRO: P2 deveria ser o novo proprietário");
                return false;
            }
            
            if (p2.getEstado() != ProcessState.READY) {
                System.out.println("ERRO: P2 deveria estar READY após ser acordado");
                return false;
            }
            
            // Tenta unlock por processo que não possui o mutex
            System.out.println("\nTestando unlock inválido:");
            boolean r4 = mutex.unlock(p3);
            if (r4) {
                System.out.println("ERRO: Unlock por não-proprietário deveria falhar");
                return false;
            }
            
            mutex.exibirEstatisticas();
            
            System.out.println("✓ Teste Mutex Básico passou");
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Teste 3: TryLock e TryDown
     */
    private static boolean testeTryOperations() {
        System.out.println("\n=== TESTE 3: TryLock e TryDown ===");
        
        try {
            // Testa tryDown do semáforo
            Semaforo sem = new Semaforo("TrySem", 1);
            
            boolean r1 = sem.tryDown(); // Deve ter sucesso
            boolean r2 = sem.tryDown(); // Deve falhar (sem bloquear)
            
            if (!r1 || r2) {
                System.out.println("ERRO: tryDown() com comportamento incorreto");
                return false;
            }
            
            // Testa tryLock do mutex
            Mutex mutex = new Mutex("TryMutex");
            ProcessControlBlock p1 = new ProcessControlBlock(1, "Processo1", criarProgramaSimples());
            ProcessControlBlock p2 = new ProcessControlBlock(2, "Processo2", criarProgramaSimples());
            
            p1.setEstado(ProcessState.RUNNING);
            p2.setEstado(ProcessState.RUNNING);
            
            boolean r3 = mutex.tryLock(p1); // Deve ter sucesso
            boolean r4 = mutex.tryLock(p2); // Deve falhar (sem bloquear)
            
            if (!r3 || r4) {
                System.out.println("ERRO: tryLock() com comportamento incorreto");
                return false;
            }
            
            if (p2.getEstado() != ProcessState.RUNNING) {
                System.out.println("ERRO: P2 não deveria ter sido bloqueado no tryLock");
                return false;
            }
            
            System.out.println("✓ Teste TryOperations passou");
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Teste 4: Semáforo como contador (múltiplos recursos)
     */
    private static boolean testeSemaforoContador() {
        System.out.println("\n=== TESTE 4: Semáforo Contador ===");
        
        try {
            // Simula um pool de 3 recursos
            Semaforo poolRecursos = new Semaforo("PoolRecursos", 3);
            
            ProcessControlBlock[] processos = new ProcessControlBlock[5];
            for (int i = 0; i < 5; i++) {
                processos[i] = new ProcessControlBlock(i + 1, "Processo" + (i + 1), criarProgramaSimples());
                processos[i].setEstado(ProcessState.RUNNING);
            }
            
            // Primeiros 3 processos devem obter recursos
            System.out.println("\nAlocando 3 recursos:");
            for (int i = 0; i < 3; i++) {
                boolean resultado = poolRecursos.down(processos[i]);
                if (!resultado) {
                    System.out.println("ERRO: Processo " + i + " deveria ter obtido o recurso");
                    return false;
                }
            }
            
            // Próximos 2 processos devem ser bloqueados
            System.out.println("\nTentando alocar mais 2 recursos (deve bloquear):");
            for (int i = 3; i < 5; i++) {
                boolean resultado = poolRecursos.down(processos[i]);
                if (resultado) {
                    System.out.println("ERRO: Processo " + i + " deveria ter sido bloqueado");
                    return false;
                }
                if (processos[i].getEstado() != ProcessState.WAITING) {
                    System.out.println("ERRO: Processo " + i + " deveria estar WAITING");
                    return false;
                }
            }
            
            // Libera um recurso - deve acordar processo 3
            System.out.println("\nLiberando um recurso:");
            ProcessControlBlock acordado = poolRecursos.up();
            if (acordado != processos[3]) {
                System.out.println("ERRO: Processo errado foi acordado");
                return false;
            }
            
            poolRecursos.exibirEstatisticas();
            
            System.out.println("✓ Teste Semáforo Contador passou");
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Teste 5: Exclusão Mútua com Mutex
     */
    private static boolean testeExclusaoMutua() {
        System.out.println("\n=== TESTE 5: Exclusão Mútua ===");
        
        try {
            Mutex mutex = new Mutex("SecaoCritica");
            
            ProcessControlBlock p1 = new ProcessControlBlock(1, "Escritor1", criarProgramaSimples());
            ProcessControlBlock p2 = new ProcessControlBlock(2, "Escritor2", criarProgramaSimples());
            ProcessControlBlock p3 = new ProcessControlBlock(3, "Escritor3", criarProgramaSimples());
            
            p1.setEstado(ProcessState.RUNNING);
            p2.setEstado(ProcessState.RUNNING);
            p3.setEstado(ProcessState.RUNNING);
            
            // Simula sequência de operações em seção crítica
            System.out.println("\nEscritor1 entra na seção crítica:");
            mutex.lock(p1);
            
            System.out.println("\nEscritor2 tenta entrar (deve bloquear):");
            mutex.lock(p2);
            
            System.out.println("\nEscritor3 tenta entrar (deve bloquear):");
            mutex.lock(p3);
            
            // Verifica que apenas p1 possui o mutex
            if (mutex.getProprietario() != p1) {
                System.out.println("ERRO: P1 deveria ser o proprietário");
                return false;
            }
            
            if (mutex.getProcessosBloqueados() != 2) {
                System.out.println("ERRO: Deveria haver 2 processos bloqueados");
                return false;
            }
            
            System.out.println("\nEscritor1 sai da seção crítica:");
            mutex.unlock(p1);
            
            // P2 deve ter assumido o mutex
            if (mutex.getProprietario() != p2) {
                System.out.println("ERRO: P2 deveria ser o novo proprietário");
                return false;
            }
            
            System.out.println("\nEscritor2 sai da seção crítica:");
            mutex.unlock(p2);
            
            // P3 deve ter assumido o mutex
            if (mutex.getProprietario() != p3) {
                System.out.println("ERRO: P3 deveria ser o novo proprietário");
                return false;
            }
            
            System.out.println("\nEscritor3 sai da seção crítica:");
            mutex.unlock(p3);
            
            // Mutex deve estar livre
            if (!mutex.isDisponivel()) {
                System.out.println("ERRO: Mutex deveria estar disponível");
                return false;
            }
            
            mutex.exibirEstatisticas();
            
            System.out.println("✓ Teste Exclusão Mútua passou");
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Main - executa todos os testes
     */
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════╗");
        System.out.println("║  TESTES DE SINCRONIZAÇÃO - ETAPA 3                ║");
        System.out.println("║  Semáforos e Mutex                                ║");
        System.out.println("╚═══════════════════════════════════════════════════╝");
        
        int totalTestes = 5;
        int testesPassados = 0;
        
        // Executa todos os testes
        if (testeSemaforoBasico()) testesPassados++;
        if (testeMutexBasico()) testesPassados++;
        if (testeTryOperations()) testesPassados++;
        if (testeSemaforoContador()) testesPassados++;
        if (testeExclusaoMutua()) testesPassados++;
        
        // Resultado final
        System.out.println("\n" + "=".repeat(55));
        System.out.println("RESULTADO FINAL: " + testesPassados + "/" + totalTestes + " testes passaram");
        
        if (testesPassados == totalTestes) {
            System.out.println("✓ TODOS OS TESTES PASSARAM!");
        } else {
            System.out.println("✗ ALGUNS TESTES FALHARAM");
        }
        System.out.println("=".repeat(55));
    }
}
