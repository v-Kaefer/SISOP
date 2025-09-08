package memory;

import hardware.Memory;
import hardware.Opcode;
import hardware.Word;

/**
 * Ponte class to connect the advanced GerenciadorMemoria with the existing system
 * that uses Word objects. This allows the system to use pagination without major changes.
 */
public class MemoryManagerPonte {
    private GerenciadorMemoria gerenciador;
    private Memory hwMemory; // Referência para a memória real do hardware
    
    public MemoryManagerPonte(int tamMem, int tamPg) {
        this.gerenciador = new GerenciadorMemoria(tamMem, tamPg);
    }
    
    public MemoryManagerPonte() {
        this.gerenciador = new GerenciadorMemoria(); // default: 1024 words, 8 words per page
    }

    /**
     * Define a referência para a memória real do hardware.
     * Isso é crucial para sincronizar a memória gerenciada com a memória usada pela CPU.
     */
    public void setHwMemory(Memory mem) {
        this.hwMemory = mem;
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
        return alocaPrograma(programa, programa.length, processoId);
    }

    /**
     * Allocates a specific amount of memory for a program and returns the page table.
     * This allows programs to have a data segment larger than their code.
     * @param programa The program code to load.
     * @param requiredSize The total number of words required by the program (code + data).
     * @param processoId A unique identifier for the process.
     * @return The page table for the allocated memory, or null if allocation fails.
     */
    public int[] alocaPrograma(Word[] programa, int requiredSize, String processoId) {
        int[] tabelaPaginas = new int[calcularNumPaginas(requiredSize)];
        boolean sucesso = gerenciador.aloca(requiredSize, tabelaPaginas, processoId);
        
        if (sucesso) {
            // Load the program into allocated memory
            PosicaoDeMemoria[] posicoes = wordToPosicao(programa);
            gerenciador.carregaPrograma(posicoes, tabelaPaginas);

            // SINCRONIZAÇÃO: Carrega o programa na memória real do HW que a CPU usa
            if (hwMemory != null) {
                for (int i = 0; i < programa.length; i++) {
                    int physicalAddress = gerenciador.traduzeEndereco(i, tabelaPaginas);
                    hwMemory.pos[physicalAddress] = programa[i];
                }
            }
            return tabelaPaginas;
        }
        
        return null; // Allocation failed
    }
    
    /**
     * Deallocates memory for a program
     */
    public void desalocaPrograma(int[] tabelaPaginas) {
        gerenciador.desaloca(tabelaPaginas);

        // SINCRONIZAÇÃO: Limpa a memória real do HW
        if (hwMemory != null) {
            for (int frame : tabelaPaginas) {
                if (frame >= 0 && frame < gerenciador.getNumFrames()) {
                    int inicioFrame = frame * gerenciador.getTamFrame();
                    int fimFrame = inicioFrame + gerenciador.getTamFrame();
                    for (int i = inicioFrame; i < fimFrame; i++) {
                        hwMemory.pos[i] = new Word(Opcode.___, -1, -1, -1);
                    }
                }
            }
        }
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
     * Exibe o conteúdo de todos os frames pertencentes a um processo.
     * @param tabelaPaginas A tabela de páginas do processo.
     */
    public void exibeConteudoProcesso(int[] tabelaPaginas) {
        gerenciador.exibeConteudoProcesso(tabelaPaginas);
    }

    /**
     * Exibe o conteúdo da memória física em um dado intervalo.
     * @param inicio Endereço físico inicial.
     * @param fim Endereço físico final.
     */
    public void dumpMemoriaFisica(int inicio, int fim) {
        gerenciador.dumpMemoriaFisica(inicio, fim);
    }
    
    /**
     * Get the underlying memory manager
     */
    public GerenciadorMemoria getGerenciador() {
        return gerenciador;
    }
}