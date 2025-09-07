package software;

import java.util.List;
import java.util.ArrayList;

/**
 * Implementação temporária do algoritmo Priority Scheduling
 * 
 * Esta é uma implementação básica que será expandida na continuação do desenvolvimento.
 * 
 * Etapa 3: Algoritmo Priority (Implementação Inicial)
 */
public class PriorityScheduler implements Scheduler {
    
    private boolean aging;
    private SchedulingMetrics metricas;
    
    public PriorityScheduler(boolean aging) {
        this.aging = aging;
        this.metricas = new SchedulingMetrics();
        System.out.println("Priority: Escalonador criado (aging: " + aging + ") - implementação em desenvolvimento");
    }
    
    @Override
    public void adicionarProcesso(ProcessControlBlock pcb) {
        System.out.println("Priority: Funcionalidade em desenvolvimento");
    }
    
    @Override
    public boolean removerProcesso(int pid) {
        return false;
    }
    
    @Override
    public ProcessControlBlock selecionarProximoProcesso() {
        return null;
    }
    
    @Override
    public void executarCicloCPU() {
        metricas.registrarCicloCPU();
    }
    
    @Override
    public boolean devePreemptar() {
        return false;
    }
    
    @Override
    public void bloquearProcessoAtual() {
        // Implementação futura
    }
    
    @Override
    public void desbloquearProcesso(ProcessControlBlock pcb) {
        // Implementação futura
    }
    
    @Override
    public void finalizarProcessoAtual() {
        // Implementação futura
    }
    
    @Override
    public ProcessControlBlock getProcessoAtual() {
        return null;
    }
    
    @Override
    public boolean temProcessosParaExecutar() {
        return false;
    }
    
    @Override
    public List<ProcessControlBlock> getProcessosProntos() {
        return new ArrayList<>();
    }
    
    @Override
    public String getEstatisticas() {
        return "=== Priority Scheduler ===\nAging: " + aging + "\nImplementação em desenvolvimento\n";
    }
    
    @Override
    public void exibirEstado() {
        System.out.println(getEstatisticas());
    }
    
    @Override
    public String getTipoEscalonamento() {
        return "Priority Scheduling" + (aging ? " (com Aging)" : "") + " - Em Desenvolvimento";
    }
    
    @Override
    public ProcessControlBlock forcarContextSwitch() {
        return null;
    }
    
    @Override
    public SchedulingMetrics getMetricas() {
        return metricas;
    }
    
    @Override
    public void configurarParametros(String parametro, Object valor) {
        System.out.println("Priority: Configuração em desenvolvimento");
    }
}