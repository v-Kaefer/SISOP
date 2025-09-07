package examples;

import hardware.HW;
import software.SO;
import programs.Programs;
import memory.GerenciadorMemoria;
import memory.MemoryManagerPonte;
import hardware.Word;

/**
 * Exemplo prático demonstrando as funcionalidades de gerência de processos
 * atualmente implementadas no sistema SISOP.
 * 
 * Este exemplo mostra:
 * 1. Como múltiplos "processos" podem alocar memória simultaneamente
 * 2. Como o contexto da CPU é configurado para um processo
 * 3. Limitações atuais (execução sequencial, não concorrente)
 */
public class ExemploGerenciaProcessos {
    
    public static void main(String[] args) {
        System.out.println("=== EXEMPLO: Gerência de Processos - Estado Atual ===\n");
        
        // 1. Inicializar sistema
        HW hw = new HW(1024);
        SO so = new SO(hw);
        hw.cpu.setUtilities(so.utils); // Fix: Set utilities properly
        Programs progs = new Programs();
        MemoryManagerPonte bridge = new MemoryManagerPonte(1024, 8);
        GerenciadorMemoria gm = bridge.getGerenciador();
        
        // 2. Simular múltiplos "processos" alocando memória
        System.out.println(">>> SIMULAÇÃO: Múltiplos Processos Alocando Memória <<<");
        
        // Processo 1: Fatorial
        Word[] progFatorial = progs.retrieveProgram("fatorial");
        int[] tabela1 = bridge.alocaPrograma(progFatorial, "Processo-Fatorial");
        System.out.println("✓ Processo-Fatorial: " + progFatorial.length + " palavras alocadas");
        
        // Processo 2: Fibonacci
        Word[] progFibonacci = progs.retrieveProgram("fibonacci10");
        int[] tabela2 = bridge.alocaPrograma(progFibonacci, "Processo-Fibonacci");
        System.out.println("✓ Processo-Fibonacci: " + progFibonacci.length + " palavras alocadas");
        
        // Processo 3: Programa mínimo
        Word[] progMinimo = progs.retrieveProgram("progMinimo");
        int[] tabela3 = bridge.alocaPrograma(progMinimo, "Processo-Minimo");
        System.out.println("✓ Processo-Minimo: " + progMinimo.length + " palavras alocadas");
        
        System.out.println(gm.getEstatisticas());
        
        // 3. Demonstrar contexto de processo na CPU
        System.out.println("\n>>> CONTEXTO DE PROCESSO NA CPU <<<");
        
        System.out.println("Estado inicial da CPU:");
        mostrarContextoCPU(hw, "INICIAL");
        
        // Configurar contexto para Processo-Fatorial
        System.out.println("\nConfigurando contexto para Processo-Fatorial...");
        hw.cpu.setContext(0);  // PC = 0 (início do programa)
        mostrarContextoCPU(hw, "PROCESSO-FATORIAL");
        
        // 4. Executar um processo (limitação atual)
        System.out.println("\n>>> EXECUÇÃO DE PROCESSO (LIMITAÇÃO ATUAL) <<<");
        System.out.println("⚠️  LIMITAÇÃO: Sistema executa apenas 1 processo por vez");
        System.out.println("   O sistema carrega o programa diretamente na memória física");
        System.out.println("   Não há context switching ou execução concorrente\n");
        
        // Executar o processo fatorial
        System.out.println("Executando Processo-Fatorial:");
        so.utils.loadAndExec(progFatorial);
        
        // 5. Mostrar o que está faltando
        System.out.println("\n>>> O QUE AINDA PRECISA SER IMPLEMENTADO <<<");
        System.out.println("❌ Process Control Block (PCB)");
        System.out.println("❌ Estados de processo (NEW, READY, RUNNING, WAITING, TERMINATED)");
        System.out.println("❌ Fila de processos prontos");
        System.out.println("❌ Context switching automático");
        System.out.println("❌ Escalonamento Round-Robin");
        System.out.println("❌ Execução concorrente de múltiplos processos");
        
        System.out.println("\n✅ Fundações implementadas:");
        System.out.println("✅ Contexto básico da CPU (PC, registradores)");
        System.out.println("✅ Alocação de memória por processo");
        System.out.println("✅ Isolamento de memória entre processos");
        System.out.println("✅ Sistema de chamadas básico (STOP, I/O)");
        System.out.println("✅ Tratamento de interrupções");
        
        // Cleanup
        bridge.desalocaPrograma(tabela1);
        bridge.desalocaPrograma(tabela2);
        bridge.desalocaPrograma(tabela3);
        
        System.out.println("\n=== FIM DO EXEMPLO ===");
    }
    
    private static void mostrarContextoCPU(HW hw, String label) {
        System.out.printf("  %s - PC: %d, Registradores: [", label, hw.cpu.getPc());
        for (int i = 0; i < 10; i++) {
            System.out.printf("R%d:%d", i, hw.cpu.getReg(i));
            if (i < 9) System.out.print(", ");
        }
        System.out.println("]");
    }
}