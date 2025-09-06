/**
 * Representa uma posição de memória da máquina virtual.
 * Cada posição codifica: [OPCODE; R1: 1 REG de 0..7; R2: 1 REG de 0..7, PARAMETRO: K ou A conforme OPCODE]
 * K significa constante, A é endereço.
 */
public class PosicaoDeMemoria {
    private Opcode opcode;   // Código da operação
    private int r1;          // Registrador 1 (0-7)
    private int r2;          // Registrador 2 (0-7) 
    private int p;           // Parâmetro (K=constante ou A=endereço, ou dado se opcode=DATA)
    
    /**
     * Construtor para instruções
     */
    public PosicaoDeMemoria(Opcode opcode, int r1, int r2, int p) {
        validarRegistrador(r1);
        validarRegistrador(r2);
        
        this.opcode = opcode;
        this.r1 = r1;
        this.r2 = r2;
        this.p = p;
    }
    
    /**
     * Construtor para dados (usa opcode DATA)
     */
    public PosicaoDeMemoria(int dado) {
        this.opcode = Opcode.DATA;
        this.r1 = 0;
        this.r2 = 0;
        this.p = dado;
    }
    
    /**
     * Construtor vazio (inicialização)
     */
    public PosicaoDeMemoria() {
        this.opcode = Opcode.DATA;
        this.r1 = 0;
        this.r2 = 0;
        this.p = 0;
    }
    
    private void validarRegistrador(int reg) {
        if (reg < 0 || reg > 7) {
            throw new IllegalArgumentException("Registrador deve estar entre 0 e 7. Valor recebido: " + reg);
        }
    }
    
    // Getters
    public Opcode getOpcode() { return opcode; }
    public int getR1() { return r1; }
    public int getR2() { return r2; }
    public int getP() { return p; }
    
    // Setters
    public void setOpcode(Opcode opcode) { this.opcode = opcode; }
    public void setR1(int r1) { 
        validarRegistrador(r1);
        this.r1 = r1; 
    }
    public void setR2(int r2) { 
        validarRegistrador(r2);
        this.r2 = r2; 
    }
    public void setP(int p) { this.p = p; }
    
    /**
     * Verifica se a posição contém dados (não é instrução)
     */
    public boolean isDado() {
        return opcode == Opcode.DATA;
    }
    
    /**
     * Para posições de dados, retorna o valor do dado
     */
    public int getDado() {
        if (!isDado()) {
            throw new IllegalStateException("Esta posição não contém um dado");
        }
        return p;
    }
    
    @Override
    public String toString() {
        if (isDado()) {
            return String.format("DATA: %d", p);
        }
        return String.format("%s R%d, R%d, %d", opcode, r1, r2, p);
    }
    
    /**
     * Cria uma cópia da posição de memória
     */
    public PosicaoDeMemoria copia() {
        return new PosicaoDeMemoria(this.opcode, this.r1, this.r2, this.p);
    }
}