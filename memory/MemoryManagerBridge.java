package memory;

import hardware.Word;
import hardware.Opcode;

/**
 * Bridge class to connect the advanced GerenciadorMemoria with the existing system
 * that uses Word objects. This allows the system to use pagination without major changes.
 */
public class MemoryManagerBridge {
    private GerenciadorMemoria gerenciador;
    
    public MemoryManagerBridge(int tamMem, int tamPg) {
        this.gerenciador = new GerenciadorMemoria(tamMem, tamPg);
    }
    
    public MemoryManagerBridge() {
        this.gerenciador = new GerenciadorMemoria(); // default: 1024 words, 8 words per page
    }
    
    /**
     * Converts Word array to PosicaoDeMemoria array
     */
    private PosicaoDeMemoria[] wordToPosicao(Word[] programa) {
        PosicaoDeMemoria[] posicoes = new PosicaoDeMemoria[programa.length];
        for (int i = 0; i < programa.length; i++) {
            posicoes[i] = new PosicaoDeMemoria(programa[i].opc, programa[i].ra, programa[i].rb, programa[i].p);
        }
        return posicoes;
    }
    
    /**
     * Converts PosicaoDeMemoria to Word
     */
    private Word posicaoToWord(PosicaoDeMemoria pos) {
        return new Word(pos.getOpcode(), pos.getR1(), pos.getR2(), pos.getP());
    }
    
    /**
     * Allocates memory for a program and returns the page table
     */
    public int[] alocaPrograma(Word[] programa, String processoId) {
        int[] tabelaPaginas = new int[calcularNumPaginas(programa.length)];
        boolean sucesso = gerenciador.aloca(programa.length, tabelaPaginas, processoId);
        
        if (sucesso) {
            // Load the program into allocated memory
            PosicaoDeMemoria[] posicoes = wordToPosicao(programa);
            gerenciador.carregaPrograma(posicoes, tabelaPaginas);
            return tabelaPaginas;
        }
        
        return null; // Allocation failed
    }
    
    /**
     * Deallocates memory for a program
     */
    public void desalocaPrograma(int[] tabelaPaginas) {
        gerenciador.desaloca(tabelaPaginas);
    }
    
    /**
     * Reads a memory position using logical address
     */
    public Word lerMemoria(int enderecoLogico, int[] tabelaPaginas) {
        PosicaoDeMemoria pos = gerenciador.acessaMemoria(enderecoLogico, tabelaPaginas);
        return posicaoToWord(pos);
    }
    
    /**
     * Writes to memory using logical address
     */
    public void escreverMemoria(int enderecoLogico, Word valor, int[] tabelaPaginas) {
        PosicaoDeMemoria pos = new PosicaoDeMemoria(valor.opc, valor.ra, valor.rb, valor.p);
        gerenciador.escreveMemoria(enderecoLogico, pos, tabelaPaginas);
    }
    
    /**
     * Translates logical address to physical address
     */
    public int traduzirEndereco(int enderecoLogico, int[] tabelaPaginas) {
        return gerenciador.traduzeEndereco(enderecoLogico, tabelaPaginas);
    }
    
    private int calcularNumPaginas(int numPalavras) {
        return (int) Math.ceil((double) numPalavras / gerenciador.getTamPg());
    }
    
    /**
     * Get memory manager statistics
     */
    public String getEstatisticas() {
        return gerenciador.getEstatisticas();
    }
    
    /**
     * Show memory map for debugging
     */
    public void exibirMapaMemoria() {
        gerenciador.exibeMapaMemoria();
    }
    
    /**
     * Get the underlying memory manager
     */
    public GerenciadorMemoria getGerenciador() {
        return gerenciador;
    }
}