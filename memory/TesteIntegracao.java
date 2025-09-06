package memory;

import hardware.Word;
import hardware.Opcode;
import programs.Programs;

/**
 * Test to demonstrate Memory Manager integration with the existing system
 */
public class TesteIntegracao {
    
    public static void main(String[] args) {
        System.out.println("=== TESTE DE INTEGRAÇÃO DO GERENCIADOR DE MEMÓRIA ===\n");
        
        // Test with existing programs
        testWithExistingPrograms();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Test multiple programs simultaneously
        testMultiplePrograms();
    }
    
    /**
     * Test memory manager with existing programs from the system
     */
    public static void testWithExistingPrograms() {
        System.out.println(">>> TESTE COM PROGRAMAS EXISTENTES <<<");
        
        MemoryManagerBridge bridge = new MemoryManagerBridge(1024, 8); // Same as default system
        Programs progs = new Programs();
        
        // Load factorial program (same as used in Sistema.java)
        Word[] factorial = progs.retrieveProgram("fatorialV2");
        
        System.out.println("Programa fatorialV2 tem " + factorial.length + " palavras");
        
        // Allocate memory for the program
        int[] tabelaPaginas = bridge.alocaPrograma(factorial, "FatorialV2");
        
        if (tabelaPaginas != null) {
            System.out.println("Alocação bem-sucedida!");
            System.out.println("Tabela de páginas: " + java.util.Arrays.toString(tabelaPaginas));
            
            // Test reading some instructions
            System.out.println("\nPrimeiras 5 instruções do programa:");
            for (int i = 0; i < Math.min(5, factorial.length); i++) {
                Word instrucao = bridge.lerMemoria(i, tabelaPaginas);
                System.out.printf("  [%d] %s %d, %d, %d%n", i, instrucao.opc, instrucao.ra, instrucao.rb, instrucao.p);
            }
            
            // Test address translation
            System.out.println("\nTradução de endereços:");
            for (int i = 0; i < Math.min(10, factorial.length); i++) {
                int endFisico = bridge.traduzirEndereco(i, tabelaPaginas);
                System.out.printf("  Lógico %d -> Físico %d%n", i, endFisico);
            }
            
            // Deallocate when done
            bridge.desalocaPrograma(tabelaPaginas);
            System.out.println("\nPrograma desalocado.");
        } else {
            System.out.println("Falha na alocação!");
        }
        
        System.out.println("\n" + bridge.getEstatisticas());
    }
    
    /**
     * Test multiple programs simultaneously
     */
    public static void testMultiplePrograms() {
        System.out.println(">>> TESTE COM MÚLTIPLOS PROGRAMAS <<<");
        
        MemoryManagerBridge bridge = new MemoryManagerBridge(256, 8); // Smaller memory for testing
        Programs progs = new Programs();
        
        // Load different programs
        String[] programNames = {"fibonacci10", "progMinimo", "fatorialV2"};
        int[][] tabelasPaginas = new int[programNames.length][];
        
        System.out.println("Carregando múltiplos programas na memória:");
        
        for (int i = 0; i < programNames.length; i++) {
            Word[] programa = progs.retrieveProgram(programNames[i]);
            System.out.printf("\nCarregando %s (%d palavras)...%n", programNames[i], programa.length);
            
            tabelasPaginas[i] = bridge.alocaPrograma(programa, programNames[i]);
            
            if (tabelasPaginas[i] != null) {
                System.out.printf("  Sucesso! Páginas: %s%n", 
                    java.util.Arrays.toString(tabelasPaginas[i]));
            } else {
                System.out.println("  Falha na alocação!");
            }
        }
        
        System.out.println("\nMapa da memória após carregar todos os programas:");
        bridge.exibirMapaMemoria();
        
        // Test accessing different programs
        System.out.println("\nTestando acesso aos programas carregados:");
        for (int i = 0; i < programNames.length; i++) {
            if (tabelasPaginas[i] != null) {
                try {
                    Word instrucao = bridge.lerMemoria(0, tabelasPaginas[i]);
                    System.out.printf("  %s - primeira instrução: %s %d, %d, %d%n", 
                        programNames[i], instrucao.opc, instrucao.ra, instrucao.rb, instrucao.p);
                } catch (Exception e) {
                    System.out.printf("  %s - erro no acesso: %s%n", programNames[i], e.getMessage());
                }
            }
        }
        
        // Clean up - deallocate all programs
        System.out.println("\nDesalocando todos os programas:");
        for (int i = 0; i < tabelasPaginas.length; i++) {
            if (tabelasPaginas[i] != null) {
                bridge.desalocaPrograma(tabelasPaginas[i]);
                System.out.printf("  %s desalocado%n", programNames[i]);
            }
        }
        
        System.out.println("\n" + bridge.getEstatisticas());
    }
}