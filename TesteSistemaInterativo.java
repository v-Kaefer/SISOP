import hardware.HW;
import software.SO;
import programs.Programs;
import hardware.Word;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Teste automatizado para o Sistema Interativo
 * 
 * Este teste demonstra como testar as funções do sistema em modo interativo
 * sem necessidade de entrada manual do usuário.
 */
public class TesteSistemaInterativo {

    private static int testCount = 0;
    private static int passCount = 0;

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║       TESTE AUTOMATIZADO - SISTEMA INTERATIVO SISOP       ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝\n");

        // Teste 1: Inicialização do sistema
        testeInicializacao();

        // Teste 2: Execução de programa fatorialV2
        testeExecutarFatorialV2();

        // Teste 3: Execução de programa fibonacci10
        testeExecutarFibonacci10();

        // Teste 4: Execução de programa progMinimo
        testeExecutarProgMinimo();

        // Teste 5: Execução de programa por nome
        testeExecutarProgramaPorNome();

        // Teste 6: Listar programas
        testeListarProgramas();

        // Teste 7: Programa não encontrado
        testeProgramaNaoEncontrado();

        // Teste 8: Stop do sistema
        testeStopSistema();

        // Teste 9: Múltiplas execuções
        testeMultiplasExecucoes();

        // Relatório final
        System.out.println("\n╔════════════════════════════════════════════════════════════╗");
        System.out.println("║                    RELATÓRIO FINAL                         ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");
        System.out.println("Total de testes: " + testCount);
        System.out.println("Testes aprovados: " + passCount);
        System.out.println("Testes falhados: " + (testCount - passCount));
        
        if (passCount == testCount) {
            System.out.println("\n✅ TODOS OS TESTES PASSARAM!");
        } else {
            System.out.println("\n❌ ALGUNS TESTES FALHARAM!");
        }
    }

    private static void testeInicializacao() {
        testCount++;
        System.out.println("Teste 1: Inicialização do Sistema");
        try {
            Sistema s = new Sistema(1024);
            
            // Verificar se componentes foram inicializados
            if (s.hw != null && s.so != null && s.progs != null && s.isRunning()) {
                System.out.println("✓ Sistema inicializado corretamente");
                System.out.println("  - Hardware: OK");
                System.out.println("  - SO: OK");
                System.out.println("  - Programs: OK");
                System.out.println("  - Status: Running");
                passCount++;
            } else {
                System.out.println("✗ Falha na inicialização");
            }
        } catch (Exception e) {
            System.out.println("✗ Exceção: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeExecutarFatorialV2() {
        testCount++;
        System.out.println("Teste 2: Executar FatorialV2");
        try {
            Sistema s = new Sistema(1024);
            s.executarPrograma("fatorialV2");
            System.out.println("✓ FatorialV2 executado com sucesso");
            passCount++;
        } catch (Exception e) {
            System.out.println("✗ Falha ao executar fatorialV2: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeExecutarFibonacci10() {
        testCount++;
        System.out.println("Teste 3: Executar Fibonacci10");
        try {
            Sistema s = new Sistema(1024);
            s.executarPrograma("fibonacci10");
            System.out.println("✓ Fibonacci10 executado com sucesso");
            passCount++;
        } catch (Exception e) {
            System.out.println("✗ Falha ao executar fibonacci10: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeExecutarProgMinimo() {
        testCount++;
        System.out.println("Teste 4: Executar ProgMinimo");
        try {
            Sistema s = new Sistema(1024);
            s.executarPrograma("progMinimo");
            System.out.println("✓ ProgMinimo executado com sucesso");
            passCount++;
        } catch (Exception e) {
            System.out.println("✗ Falha ao executar progMinimo: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeExecutarProgramaPorNome() {
        testCount++;
        System.out.println("Teste 5: Executar Programa por Nome (fatorial)");
        try {
            Sistema s = new Sistema(1024);
            s.executarPrograma("fatorial");
            System.out.println("✓ Fatorial executado com sucesso");
            passCount++;
        } catch (Exception e) {
            System.out.println("✗ Falha ao executar fatorial: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeListarProgramas() {
        testCount++;
        System.out.println("Teste 6: Listar Programas Disponíveis");
        try {
            Sistema s = new Sistema(1024);
            
            // Capturar saída
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            s.listarProgramas();
            
            // Restaurar saída original
            System.setOut(originalOut);
            String output = outputStream.toString();
            
            // Verificar se os programas principais estão listados
            boolean hasPrograms = output.contains("fatorial") && 
                                  output.contains("fibonacci10") && 
                                  output.contains("progMinimo");
            
            if (hasPrograms) {
                System.out.println("✓ Listagem de programas OK");
                System.out.println("  - Todos os programas principais encontrados na lista");
                passCount++;
            } else {
                System.out.println("✗ Listagem incompleta");
            }
        } catch (Exception e) {
            System.out.println("✗ Falha ao listar programas: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeProgramaNaoEncontrado() {
        testCount++;
        System.out.println("Teste 7: Programa Não Encontrado (tratamento de erro)");
        try {
            Sistema s = new Sistema(1024);
            
            // Capturar saída
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            s.executarPrograma("programaInexistente");
            
            // Restaurar saída original
            System.setOut(originalOut);
            String output = outputStream.toString();
            
            if (output.contains("ERRO") || output.contains("não encontrado")) {
                System.out.println("✓ Tratamento de erro funcionando corretamente");
                System.out.println("  - Mensagem de erro apropriada exibida");
                passCount++;
            } else {
                System.out.println("✗ Tratamento de erro não funcionou");
            }
        } catch (Exception e) {
            System.out.println("✗ Exceção inesperada: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeStopSistema() {
        testCount++;
        System.out.println("Teste 8: Stop do Sistema");
        try {
            Sistema s = new Sistema(1024);
            
            if (s.isRunning()) {
                s.stop();
                if (!s.isRunning()) {
                    System.out.println("✓ Sistema parado corretamente");
                    System.out.println("  - Status alterado de running para stopped");
                    passCount++;
                } else {
                    System.out.println("✗ Sistema não parou");
                }
            } else {
                System.out.println("✗ Sistema não estava rodando inicialmente");
            }
        } catch (Exception e) {
            System.out.println("✗ Falha ao parar sistema: " + e.getMessage());
        }
        System.out.println();
    }

    private static void testeMultiplasExecucoes() {
        testCount++;
        System.out.println("Teste 9: Múltiplas Execuções Sequenciais");
        try {
            Sistema s = new Sistema(1024);
            
            // Executar vários programas em sequência
            s.executarPrograma("progMinimo");
            s.executarPrograma("fatorialV2");
            s.executarPrograma("fibonacci10");
            
            System.out.println("✓ Múltiplas execuções completadas com sucesso");
            System.out.println("  - progMinimo: OK");
            System.out.println("  - fatorialV2: OK");
            System.out.println("  - fibonacci10: OK");
            passCount++;
        } catch (Exception e) {
            System.out.println("✗ Falha em múltiplas execuções: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Teste demonstrativo do modo interativo com entrada simulada
     * (Comentado pois requer thread separada para funcionar adequadamente)
     */
    /*
    private static void testeModoInterativoSimulado() {
        testCount++;
        System.out.println("Teste 10: Modo Interativo Simulado");
        try {
            // Simular entrada do usuário
            String input = "1\n3\n0\n"; // Lista programas, executa fatorialV2, sai
            ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
            
            Sistema s = new Sistema(1024, inputStream);
            
            // Executar em thread separada para não bloquear
            Thread sistemaThread = new Thread(() -> {
                s.run();
            });
            sistemaThread.start();
            
            // Aguardar conclusão (com timeout)
            sistemaThread.join(5000);
            
            if (!s.isRunning()) {
                System.out.println("✓ Modo interativo executado e encerrado");
                passCount++;
            } else {
                System.out.println("✗ Sistema não encerrou corretamente");
            }
        } catch (Exception e) {
            System.out.println("✗ Falha no modo interativo: " + e.getMessage());
        }
        System.out.println();
    }
    */
}
