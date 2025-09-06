/**
 * Classe de teste para o Gerenciador de Memória com Paginação
 * Testa todos os aspectos conforme especificação da Etapa 01
 */
public class TesteGerenciadorMemoriaCompleto {
    
    public static void main(String[] args) {
        System.out.println("=== TESTE COMPLETO DO GERENCIADOR DE MEMÓRIA ===\n");
        
        // Teste com configuração padrão
        testeConfiguracaoPadrao();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Teste com configuração customizada
        testeConfiguracaoCustomizada();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Teste de tradução de endereços
        testeTraducaoEnderecos();
        
        System.out.println("\n" + "=".repeat(60) + "\n");
        
        // Teste de fragmentação
        testeFragmentacao();
    }
    
    /**
     * Teste com configuração padrão (1024 palavras, páginas de 8)
     */
    public static void testeConfiguracaoPadrao() {
        System.out.println(">>> TESTE COM CONFIGURAÇÃO PADRÃO <<<");
        
        GerenciadorMemoria gm = new GerenciadorMemoria();
        System.out.println(gm.getEstatisticas());
        
        // Programa pequeno baseado na tabela de comandos
        testeProgramaPequeno(gm);
        
        // Programa médio
        testeProgramaMedio(gm);
        
        // Teste de limite de memória
        testeLimiteMemoria(gm);
        
        System.out.println("\n" + gm.getEstatisticas());
    }
    
    /**
     * Teste com configuração customizada
     */
    public static void testeConfiguracaoCustomizada() {
        System.out.println(">>> TESTE COM CONFIGURAÇÃO CUSTOMIZADA <<<");
        
        // Memória menor para testar limites
        GerenciadorMemoria gm = new GerenciadorMemoria(64, 4); // 16 frames de 4 palavras
        System.out.println(gm.getEstatisticas());
        
        // Teste com essa configuração menor
        int[] tabela1 = new int[3];
        boolean sucesso1 = gm.aloca(10, tabela1, "Prog-Custom-1");
        System.out.println("Alocação 1: " + sucesso1);
        
        int[] tabela2 = new int[4];
        boolean sucesso2 = gm.aloca(15, tabela2, "Prog-Custom-2");
        System.out.println("Alocação 2: " + sucesso2);
        
        // Deve falhar - não há frames suficientes
        int[] tabela3 = new int[10];
        boolean sucesso3 = gm.aloca(40, tabela3, "Prog-Custom-3");
        System.out.println("Alocação 3 (deve falhar): " + sucesso3);
        
        gm.exibeMapaMemoria();
        
        // Libera um programa
        gm.desaloca(tabela1);
        
        // Agora deve conseguir alocar
        boolean sucesso4 = gm.aloca(12, tabela3, "Prog-Custom-4");
        System.out.println("Alocação 4 (após liberação): " + sucesso4);
        
        System.out.println("\n" + gm.getEstatisticas());
    }
    
    /**
     * Teste específico de tradução de endereços
     */
    public static void testeTraducaoEnderecos() {
        System.out.println(">>> TESTE DE TRADUÇÃO DE ENDEREÇOS <<<");
        
        GerenciadorMemoria gm = new GerenciadorMemoria(32, 4); // 8 frames de 4 palavras
        
        // Aloca um programa
        int[] tabelaPaginas = new int[3];
        boolean sucesso = gm.aloca(10, tabelaPaginas, "Prog-Traducao");
        
        if (sucesso) {
            System.out.println("Tabela de páginas: " + Arrays.toString(tabelaPaginas));
            
            // Testa tradução de vários endereços lógicos
            System.out.println("\nTradução de endereços:");
            System.out.println("Lógico -> Físico");
            
            for (int endLogico = 0; endLogico < 10; endLogico++) {
                try {
                    int endFisico = gm.traduzeEndereco(endLogico, tabelaPaginas);
                    int pagina = endLogico / gm.getTamPg();
                    int deslocamento = endLogico % gm.getTamPg();
                    int frame = tabelaPaginas[pagina];
                    
                    System.out.printf("  %2d -> %2d (página %d, desloc %d, frame %d)%n", 
                                     endLogico, endFisico, pagina, deslocamento, frame);
                } catch (Exception e) {
                    System.out.printf("  %2d -> ERRO: %s%n", endLogico, e.getMessage());
                }
            }
            
            // Teste de acesso fora dos limites
            System.out.println("\nTeste de proteção de memória:");
            try {
                gm.traduzeEndereco(15, tabelaPaginas); // Fora do programa alocado
            } catch (Exception e) {
                System.out.println("Proteção funcionando: " + e.getMessage());
            }
        }
    }
    
    /**
     * Teste de fragmentação e defragmentação
     */
    public static void testeFragmentacao() {
        System.out.println(">>> TESTE DE FRAGMENTAÇÃO <<<");
        
        GerenciadorMemoria gm = new GerenciadorMemoria(48, 4); // 12 frames de 4 palavras
        
        // Aloca vários programas pequenos
        int[][] tabelas = new int[5][2];
        String[] processosIds = {"Frag-A", "Frag-B", "Frag-C", "Frag-D", "Frag-E"};
        
        System.out.println("Alocando 5 programas pequenos:");
        for (int i = 0; i < 5; i++) {
            boolean sucesso = gm.aloca(6, tabelas[i], processosIds[i]);
            System.out.printf("  %s: %s%n", processosIds[i], sucesso ? "OK" : "FALHOU");
        }
        
        gm.exibeMapaMemoria();
        
        // Libera programas intercalados (cria fragmentação)
        System.out.println("\nLiberando programas B e D (cria fragmentação):");
        gm.desaloca(tabelas[1]); // Frag-B
        gm.desaloca(tabelas[3]); // Frag-D
        
        gm.exibeMapaMemoria();
        
        // Tenta alocar um programa que precisa de frames contíguos (vai encontrar frames esparsos)
        System.out.println("\nTentando alocar programa grande (12 palavras = 3 frames):");
        int[] tabelaGrande = new int[3];
        boolean sucessoGrande = gm.aloca(12, tabelaGrande, "Frag-Grande");
        System.out.println("Resultado: " + (sucessoGrande ? "OK" : "FALHOU"));
        
        if (sucessoGrande) {
            System.out.println("Frames alocados: " + Arrays.toString(tabelaGrande));
            System.out.println("Observe que os frames não são contíguos - isso é normal em paginação!");
        }
        
        System.out.println("\n" + gm.getEstatisticas());
    }
    
    // Métodos auxiliares para criar programas de teste
    
    private static void testeProgramaPequeno(GerenciadorMemoria gm) {
        System.out.println("\n--- Teste: Programa Pequeno (10 palavras) ---");
        
        int[] tabelaPaginas = new int[2];
        boolean sucesso = gm.aloca(10, tabelaPaginas, "Prog-Pequeno");
        
        if (sucesso) {
            PosicaoDeMemoria[] programa = criarProgramaExemplo();
            gm.carregaPrograma(programa, tabelaPaginas);
            
            System.out.println("Tabela de páginas: " + Arrays.toString(tabelaPaginas));
            
            // Testa alguns acessos
            System.out.println("Testando acessos:");
            for (int i = 0; i < 5; i++) {
                PosicaoDeMemoria pos = gm.acessaMemoria(i, tabelaPaginas);
                System.out.printf("  [%d] %s%n", i, pos);
            }
        }
    }
    
    private static void testeProgramaMedio(GerenciadorMemoria gm) {
        System.out.println("\n--- Teste: Programa Médio (50 palavras) ---");
        
        int[] tabelaPaginas = new int[7];
        boolean sucesso = gm.aloca(50, tabelaPaginas, "Prog-Medio");
        
        if (sucesso) {
            System.out.println("Tabela de páginas: " + Arrays.toString(Arrays.copyOf(tabelaPaginas, 7)));
        }
    }
    
    private static void testeLimiteMemoria(GerenciadorMemoria gm) {
        System.out.println("\n--- Teste: Limite de Memória ---");
        
        // Tenta alocar mais que o disponível
        int[] tabelaPaginas = new int[130];
        boolean sucesso = gm.aloca(1000, tabelaPaginas, "Prog-Impossivel");
        System.out.println("Alocação impossível (deve falhar): " + sucesso);
    }
    
    /**
     * Cria um programa de exemplo baseado na tabela de comandos fornecida
     */
    private static PosicaoDeMemoria[] criarProgramaExemplo() {
        return new PosicaoDeMemoria[] {
            new PosicaoDeMemoria(Opcode.LDI, 1, 0, 10),      // LDI R1, 10
            new PosicaoDeMemoria(Opcode.LDI, 2, 0, 20),      // LDI R2, 20  
            new PosicaoDeMemoria(Opcode.ADD, 3, 1, 2),       // ADD R3, R1, R2
            new PosicaoDeMemoria(Opcode.STD, 3, 0, 100),     // STD [100], R3
            new PosicaoDeMemoria(Opcode.JMPIE, 1, 2, 8),     // JMPIE R1, R2, 8
            new PosicaoDeMemoria(Opcode.ADDI, 1, 0, 1),      // ADDI R1, 1
            new PosicaoDeMemoria(Opcode.JMP, 0, 0, 2),       // JMP 2
            new PosicaoDeMemoria(Opcode.STOP, 0, 0, 0),      // STOP
            new PosicaoDeMemoria(42),                        // DATA: 42
            new PosicaoDeMemoria(100)                        // DATA: 100
        };
    }
}