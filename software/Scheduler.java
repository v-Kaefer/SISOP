package software;

import java.util.List;

/**
 * Interface abstrata para algoritmos de escalonamento
 * 
 * Define o contrato que todos os escalonadores devem seguir,
 * permitindo implementação de diferentes algoritmos de forma modular.
 * 
 * Etapa 3: Framework de Escalonamento Flexível
 */
public interface Scheduler {
    
    /**
     * Adiciona um processo à estrutura de escalonamento
     */
    void adicionarProcesso(ProcessControlBlock pcb);
    
    /**
     * Remove um processo da estrutura de escalonamento
     */
    boolean removerProcesso(int pid);
    
    /**
     * Seleciona o próximo processo para execução
     * Implementa a lógica específica do algoritmo
     */
    ProcessControlBlock selecionarProximoProcesso();
    
    /**
     * Notifica que um ciclo de CPU foi executado
     */
    void executarCicloCPU();
    
    /**
     * Verifica se deve ocorrer preempção
     */
    boolean devePreemptar();
    
    /**
     * Bloqueia o processo atual
     */
    void bloquearProcessoAtual();
    
    /**
     * Desbloqueia um processo
     */
    void desbloquearProcesso(ProcessControlBlock pcb);
    
    /**
     * Finaliza o processo atual
     */
    void finalizarProcessoAtual();
    
    /**
     * Retorna o processo atualmente em execução
     */
    ProcessControlBlock getProcessoAtual();
    
    /**
     * Verifica se há processos para executar
     */
    boolean temProcessosParaExecutar();
    
    /**
     * Retorna lista de processos prontos
     */
    List<ProcessControlBlock> getProcessosProntos();
    
    /**
     * Retorna estatísticas do escalonador
     */
    String getEstatisticas();
    
    /**
     * Exibe estado detalhado do escalonador
     */
    void exibirEstado();
    
    /**
     * Retorna o tipo/nome do algoritmo de escalonamento
     */
    String getTipoEscalonamento();
    
    /**
     * Força um context switch
     */
    ProcessControlBlock forcarContextSwitch();
    
    /**
     * Retorna métricas de performance
     */
    SchedulingMetrics getMetricas();
    
    /**
     * Configura parâmetros específicos do algoritmo
     */
    void configurarParametros(String parametro, Object valor);
}