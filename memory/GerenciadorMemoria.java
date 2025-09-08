package memory;

import java.util.*;

/**
 * Gerenciador de Memória implementando paginação para a Máquina Virtual.
 * Conforme especificação da Etapa 01.
 */
public class GerenciadorMemoria {
    // Configurações de memória (podem ser alteradas para testes)
    private final int tamMem;      // Tamanho total da memória em palavras
    private final int tamPg;       // Tamanho da página em palavras  
    private final int tamFrame;    // Tamanho do frame (= tamPg)
    private final int numFrames;   // Número total de frames
    
    // Estruturas de dados internas
    private final PosicaoDeMemoria[] memoria;     // Array da memória física
    private final boolean[] framesAlocados;       // Controle de frames: true=alocado, false=livre
    private final Map<Integer, String> frameOwner; // Para debugging: qual processo possui cada frame
    
    // Estatísticas
    private int totalAlocacoes = 0;
    private int totalDesalocacoes = 0;
    
    /**
     * Construtor com valores padrão (conforme especificação)
     */
    public GerenciadorMemoria() {
        this(1024, 8); // tamMem=1024, tamPg=8 -> 128 frames
    }
    
    /**
     * Construtor parametrizado para testes com diferentes tamanhos
     */
    public GerenciadorMemoria(int tamMem, int tamPg) {
        if (tamMem <= 0 || tamPg <= 0) {
            throw new IllegalArgumentException("Tamanhos de memória e página devem ser positivos");
        }
        if (tamMem % tamPg != 0) {
            throw new IllegalArgumentException("Tamanho da memória deve ser múltiplo do tamanho da página");
        }
        
        this.tamMem = tamMem;
        this.tamPg = tamPg;
        this.tamFrame = tamPg;  // Frame tem mesmo tamanho que página
        this.numFrames = tamMem / tamPg;
        
        // Inicializa estruturas
        this.memoria = new PosicaoDeMemoria[tamMem];
        this.framesAlocados = new boolean[numFrames];
        this.frameOwner = new HashMap<>();
        
        // Inicializa todas as posições de memória como vazias
        for (int i = 0; i < tamMem; i++) {
            memoria[i] = new PosicaoDeMemoria(); // DATA com valor 0
        }
        
        System.out.printf("Gerenciador de Memória inicializado: %d palavras, %d frames de %d palavras cada%n", 
                         tamMem, numFrames, tamFrame);
    }
    
    /**
     * Aloca frames para um processo
     * @param nroPalavras número de palavras necessárias para o processo
     * @param tabelaPaginas array de saída que receberá os índices dos frames alocados
     * @return true se conseguiu alocar, false caso contrário
     */
    public boolean aloca(int nroPalavras, int[] tabelaPaginas) {
        return aloca(nroPalavras, tabelaPaginas, "Processo-" + (totalAlocacoes + 1));
    }
    
    /**
     * Aloca frames para um processo (versão com identificador)
     */
    public boolean aloca(int nroPalavras, int[] tabelaPaginas, String processoId) {
        if (nroPalavras <= 0) {
            System.err.println("Erro: Número de palavras deve ser positivo");
            return false;
        }
        
        // Calcula quantas páginas são necessárias
        int paginasNecessarias = (int) Math.ceil((double) nroPalavras / tamPg);
        
        if (paginasNecessarias > tabelaPaginas.length) {
            System.err.printf("Erro: Array tabelaPaginas muito pequeno. Necessário: %d, Fornecido: %d%n", 
                             paginasNecessarias, tabelaPaginas.length);
            return false;
        }
        
        // Verifica disponibilidade
        List<Integer> framesDisponiveis = new ArrayList<>();
        for (int i = 0; i < numFrames; i++) {
            if (!framesAlocados[i]) {
                framesDisponiveis.add(i);
            }
        }
        
        if (framesDisponiveis.size() < paginasNecessarias) {
            System.err.printf("Erro: Memória insuficiente. Necessário: %d frames, Disponível: %d frames%n",
                             paginasNecessarias, framesDisponiveis.size());
            return false;
        }
        
        // Aloca os frames (pega os primeiros disponíveis)
        for (int i = 0; i < paginasNecessarias; i++) {
            int frame = framesDisponiveis.get(i);
            framesAlocados[frame] = true;
            frameOwner.put(frame, processoId);
            tabelaPaginas[i] = frame;
        }
        
        // Limpa frames não utilizados na tabela
        for (int i = paginasNecessarias; i < tabelaPaginas.length; i++) {
            tabelaPaginas[i] = -1; // Marca como não utilizado
        }
        
        totalAlocacoes++;
        System.out.printf("Alocação bem-sucedida para %s: %d palavras em %d páginas (frames: %s)%n",
                         processoId, nroPalavras, paginasNecessarias, 
                         Arrays.toString(Arrays.copyOf(tabelaPaginas, paginasNecessarias)));
        
        return true;
    }
    
    /**
     * Desaloca frames de um processo
     * @param tabelaPaginas array com os índices dos frames a serem liberados
     */
    public void desaloca(int[] tabelaPaginas) {
        int framesLiberados = 0;
        List<Integer> framesLiberadosList = new ArrayList<>();
        
        for (int frame : tabelaPaginas) {
            if (frame >= 0 && frame < numFrames && framesAlocados[frame]) {
                framesAlocados[frame] = false;
                
                // Limpa o conteúdo do frame
                int inicioFrame = frame * tamFrame;
                int fimFrame = inicioFrame + tamFrame;
                for (int i = inicioFrame; i < fimFrame; i++) {
                    memoria[i] = new PosicaoDeMemoria(); // Reset para DATA com valor 0
                }
                
                framesLiberadosList.add(frame);
                frameOwner.remove(frame);
                framesLiberados++;
            }
        }
        
        totalDesalocacoes++;
        System.out.printf("Desalocação realizada: %d frames liberados %s%n", 
                         framesLiberados, framesLiberadosList);
    }
    
    /**
     * Carrega um programa na memória usando a tabela de páginas
     * Conforme seção 1.3: "Cada página i do programa deve ser copiada (exatamente como tal) 
     * para o frame informado em tabelaPaginas[i]"
     */
    public void carregaPrograma(PosicaoDeMemoria[] programa, int[] tabelaPaginas) {
        if (programa == null || programa.length == 0) {
            System.err.println("Erro: Programa vazio ou nulo");
            return;
        }
        
        int posicaoPrograma = 0;
        int paginaAtual = 0;
        int posicaoNaPagina = 0;
        
        System.out.printf("Carregando programa de %d instruções...%n", programa.length);
        
        while (posicaoPrograma < programa.length && paginaAtual < tabelaPaginas.length) {
            int frame = tabelaPaginas[paginaAtual];
            
            if (frame < 0) break; // Frame inválido, fim da tabela válida
            
            // Calcula endereço físico
            int enderecoFisico = frame * tamFrame + posicaoNaPagina;
            
            // Copia a instrução
            memoria[enderecoFisico] = programa[posicaoPrograma].copia();
            
            posicaoPrograma++;
            posicaoNaPagina++;
            
            // Se encheu a página atual, vai para a próxima
            if (posicaoNaPagina >= tamPg) {
                posicaoNaPagina = 0;
                paginaAtual++;
            }
        }
        
        System.out.printf("Programa carregado: %d instruções em %d páginas%n", 
                         posicaoPrograma, paginaAtual + (posicaoNaPagina > 0 ? 1 : 0));
    }
    
    /**
     * Traduz endereço lógico para físico usando tabela de páginas
     * Conforme seção 1.4: conversão endereço lógico -> página + deslocamento -> frame + deslocamento
     */
    public int traduzeEndereco(int enderecoLogico, int[] tabelaPaginas) {
        if (enderecoLogico < 0) {
            throw new IllegalArgumentException("Endereço lógico não pode ser negativo: " + enderecoLogico);
        }
        
        // Calcula página e deslocamento
        int pagina = enderecoLogico / tamPg;
        int deslocamento = enderecoLogico % tamPg;
        
        // Verifica limites
        if (pagina >= tabelaPaginas.length || tabelaPaginas[pagina] < 0) {
            throw new IndexOutOfBoundsException(
                String.format("Acesso à página inválida: %d (endereço lógico: %d)", pagina, enderecoLogico));
        }
        
        // Calcula endereço físico
        int frame = tabelaPaginas[pagina];
        int enderecoFisico = frame * tamFrame + deslocamento;
        
        if (enderecoFisico >= tamMem) {
            throw new IndexOutOfBoundsException(
                String.format("Endereço físico fora dos limites: %d (max: %d)", enderecoFisico, tamMem - 1));
        }
        
        return enderecoFisico;
    }
    
    /**
     * Acessa posição de memória (leitura) com tradução de endereço
     */
    public PosicaoDeMemoria acessaMemoria(int enderecoLogico, int[] tabelaPaginas) {
        int enderecoFisico = traduzeEndereco(enderecoLogico, tabelaPaginas);
        return memoria[enderecoFisico];
    }
    
    /**
     * Escreve na memória com tradução de endereço
     */
    public void escreveMemoria(int enderecoLogico, PosicaoDeMemoria valor, int[] tabelaPaginas) {
        int enderecoFisico = traduzeEndereco(enderecoLogico, tabelaPaginas);
        memoria[enderecoFisico] = valor.copia();
    }
    
    // Getters para as configurações
    public int getTamMem() { return tamMem; }
    public int getTamPg() { return tamPg; }
    public int getTamFrame() { return tamFrame; }
    public int getNumFrames() { return numFrames; }
    
    /**
     * Retorna estatísticas do gerenciador
     */
    public String getEstatisticas() {
        int framesLivres = 0;
        int framesOcupados = 0;
        
        for (boolean alocado : framesAlocados) {
            if (alocado) framesOcupados++;
            else framesLivres++;
        }
        
        double percentualUso = (double) framesOcupados / numFrames * 100;
        
        return String.format(
            "=== Estatísticas do Gerenciador de Memória ===%n" +
            "Memória: %d palavras (%d frames de %d palavras)%n" +
            "Frames livres: %d/%d (%.1f%% livre)%n" +
            "Frames ocupados: %d/%d (%.1f%% ocupado)%n" +
            "Total de alocações: %d%n" +
            "Total de desalocações: %d%n",
            tamMem, numFrames, tamFrame,
            framesLivres, numFrames, (100.0 - percentualUso),
            framesOcupados, numFrames, percentualUso,
            totalAlocacoes, totalDesalocacoes
        );
    }
    
    /**
     * Exibe mapa detalhado da memória (para debugging)
     */
    public void exibeMapaMemoria() {
        System.out.println("=== Mapa da Memória ===");
        for (int frame = 0; frame < numFrames; frame++) {
            int inicio = frame * tamFrame;
            int fim = inicio + tamFrame - 1;
            String status = framesAlocados[frame] ? "OCUPADO" : "LIVRE";
            String owner = frameOwner.getOrDefault(frame, "N/A");
            
            System.out.printf("Frame %3d: [%4d-%4d] %s %s%n", 
                             frame, inicio, fim, status, 
                             framesAlocados[frame] ? "(" + owner + ")" : "");
        }
    }
    
    /**
     * Exibe conteúdo de uma página específica (para debugging)
     */
    public void exibeConteudoPagina(int frame) {
        if (frame < 0 || frame >= numFrames) {
            System.err.println("Frame inválido: " + frame);
            return;
        }
        
        System.out.printf("=== Conteúdo do Frame %d ===\n", frame);
        int inicio = frame * tamFrame;
        
        for (int i = 0; i < tamFrame; i++) {
            int endereco = inicio + i;
            PosicaoDeMemoria pos = memoria[endereco];
            System.out.printf("  [%4d] %s%n", endereco, pos);
        }
    }

    /**
     * Exibe o conteúdo de todos os frames pertencentes a um processo.
     * @param tabelaPaginas A tabela de páginas do processo.
     */
    public void exibeConteudoProcesso(int[] tabelaPaginas) {
        if (tabelaPaginas == null) {
            System.out.println("Tabela de páginas nula.");
            return;
        }
        boolean hasContent = false;
        for (int pagina = 0; pagina < tabelaPaginas.length; pagina++) {
            int frame = tabelaPaginas[pagina];
            if (frame != -1) {
                hasContent = true;
                System.out.printf("--- Página Lógica %d -> Frame Físico %d ---\n", pagina, frame);
                exibeConteudoPagina(frame);
            }
        }
        if (!hasContent) {
            System.out.println("Nenhum frame de memória alocado para este processo.");
        }
    }

    /**
     * Exibe o conteúdo da memória física em um dado intervalo.
     * @param inicio Endereço físico inicial.
     * @param fim Endereço físico final.
     */
    public void dumpMemoriaFisica(int inicio, int fim) {
        if (inicio < 0 || fim > tamMem || inicio > fim) {
            System.err.printf("Intervalo de memória inválido: [%d, %d]. Válido: [0, %d]\n", inicio, fim, tamMem - 1);
            return;
        }
        for (int i = inicio; i < fim; i++) {
            System.out.printf("[%04d]: %s\n", i, memoria[i].toString());
        }
    }
}