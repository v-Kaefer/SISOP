package software;

import hardware.HW;
import hardware.Word;
import hardware.Interrupts;
import memory.MemoryManagerPonte;
import programs.Program;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Gerenciador de Processos do sistema SISOP
 * 
 * Responsável por:
 * - Criação e destruição de processos
 * - Gerenciamento de PCBs (Process Control Blocks)
 * - Alocação e liberação de memória para processos
 * - Coordenação com o escalonador
 * - Execução concorrente de múltiplos processos
 */
public class ProcessManager {
    
    // Componentes do sistema
    private HW hardware;
    private MemoryManagerPonte gerenciadorMemoria;
    private RoundRobinScheduler escalonador;
    
    // Controle de processos
    private Map<Integer, ProcessControlBlock> processos; // Todos os processos do sistema
    private int proximoPID;                             // Contador para PIDs únicos
    
    // Estado do gerenciador
    private boolean sistemaAtivo;                       // Sistema está executando?
    private int maxProcessosConcorrentes;               // Limite de processos simultâneos
    
    // Estatísticas
    private long totalProcessosCriados;
    private long totalProcessosFinalizados;
    
    /**
     * Construtor do Gerenciador de Processos
     */
    public ProcessManager(HW hardware, MemoryManagerPonte gerenciadorMemoria) {
        this.hardware = hardware;
        this.gerenciadorMemoria = gerenciadorMemoria;
        this.escalonador = new RoundRobinScheduler(10); // Quantum padrão de 10 ciclos
        
        this.processos = new HashMap<>();
        this.proximoPID = 1;
        this.sistemaAtivo = false;
        this.maxProcessosConcorrentes = 10; // Limite padrão
        
        this.totalProcessosCriados = 0;
        this.totalProcessosFinalizados = 0;
        
        System.out.println("Gerenciador de Processos inicializado");
    }
    
    /**
     * Cria um novo processo a partir de um programa
     */
    public ProcessControlBlock criarProcesso(String nome, Word[] programa) {
        if (processos.size() >= maxProcessosConcorrentes) {
            System.out.println("ERRO: Limite máximo de processos atingido (" + maxProcessosConcorrentes + ")");
            return null;
        }
        
        // Cria PCB
        int pid = proximoPID++;
        ProcessControlBlock pcb = new ProcessControlBlock(pid, nome, programa);
        
        // Aloca memória para o processo
        int[] tabelaPaginas = gerenciadorMemoria.alocaPrograma(programa, "Processo-" + pid);
        if (tabelaPaginas == null) {
            System.out.println("ERRO: Falha na alocação de memória para processo " + nome);
            return null;
        }
        
        pcb.setTabelaPaginas(tabelaPaginas);
        pcb.setEstado(ProcessState.NEW);
        
        // Registra o processo
        processos.put(pid, pcb);
        totalProcessosCriados++;
        
        System.out.println("Processo criado: " + nome + " (PID: " + pid + ", " + programa.length + " instruções)");
        return pcb;
    }
    
    /**
     * Cria processo a partir de um objeto Program
     */
    public ProcessControlBlock criarProcesso(Program program) {
        return criarProcesso(program.name, program.image);
    }
    
    /**
     * Admite um processo no sistema (NEW -> READY)
     */
    public boolean admitirProcesso(int pid) {
        ProcessControlBlock pcb = processos.get(pid);
        if (pcb != null && pcb.getEstado() == ProcessState.NEW) {
            pcb.setEstado(ProcessState.READY); // Muda o estado primeiro
            escalonador.adicionarProcesso(pcb);
            System.out.println("Processo admitido no sistema: " + pcb.getNome() + " (PID: " + pid + ")");
            return true;
        }
        return false;
    }
    
    /**
     * Finaliza um processo e libera seus recursos
     */
    public boolean finalizarProcesso(int pid) {
        ProcessControlBlock pcb = processos.get(pid);
        if (pcb == null) {
            return false;
        }
        
        // Finaliza o processo
        pcb.finalizar();
        
        // Libera memória
        if (pcb.getTabelaPaginas() != null) {
            gerenciadorMemoria.desalocaPrograma(pcb.getTabelaPaginas());
        }
        
        // Remove das estruturas de dados
        processos.remove(pid);
        escalonador.removerProcesso(pid);
        totalProcessosFinalizados++;
        
        System.out.println("Processo finalizado e recursos liberados: " + pcb.getNome() + " (PID: " + pid + ")");
        return true;
    }
    
    /**
     * Executa um ciclo do sistema operacional
     * Esta é a função principal que coordena execução de processos
     */
    public boolean executarCicloSO() {
        if (!sistemaAtivo) {
            return false;
        }
        
        // Seleciona processo para executar
        ProcessControlBlock processoAtual = escalonador.selecionarProximoProcesso();
        
        if (processoAtual == null) {
            // Não há processos para executar
            if (processos.isEmpty()) {
                System.out.println("Todos os processos finalizaram. Sistema encerrado.");
                sistemaAtivo = false;
                return false;
            }
            return true; // Aguarda novos processos
        }
        
        // Executa context switch se necessário
        if (processoAtual != escalonador.getProcessoAtual()) {
            executarContextSwitch(processoAtual);
        }
        
        // Executa uma instrução do processo
        boolean continuarExecucao = executarInstrucao(processoAtual);
        
        // Atualiza contadores do escalonador
        escalonador.executarCicloCPU();
        
        // Verifica se processo terminou
        if (!continuarExecucao || processoAtual.isFinished()) {
            escalonador.finalizarProcessoAtual();
            finalizarProcesso(processoAtual.getPid());
        }
        
        return true;
    }
    
    /**
     * Executa context switch para um processo
     */
    private void executarContextSwitch(ProcessControlBlock novoProcesso) {
        // Salva contexto do processo anterior (se houver)
        ProcessControlBlock processoAnterior = escalonador.getProcessoAtual();
        if (processoAnterior != null && !processoAnterior.isFinished()) {
            salvarContextoCPU(processoAnterior);
        }
        
        // Carrega contexto do novo processo
        carregarContextoCPU(novoProcesso);
        
        System.out.println("Context switch: " + 
            (processoAnterior != null ? processoAnterior.getNome() : "idle") + " -> " + novoProcesso.getNome());
    }
    
    /**
     * Salva o contexto atual da CPU no PCB
     */
    private void salvarContextoCPU(ProcessControlBlock pcb) {
        pcb.setPc(hardware.cpu.getPc());
        
        // Salva registradores
        int[] regs = new int[10];
        for (int i = 0; i < 10; i++) {
            regs[i] = hardware.cpu.getReg(i);
        }
        pcb.setRegistradores(regs);
        
        // Salva estado de interrupção
        // Nota: No código atual, não há acesso direto ao estado de interrupção da CPU
        // Esta funcionalidade seria expandida numa implementação completa
    }
    
    /**
     * Carrega o contexto de um processo na CPU
     */
    private void carregarContextoCPU(ProcessControlBlock pcb) {
        // Define contexto na CPU
        hardware.cpu.setContext(pcb.getPc());
        
        // Nota: O código atual da CPU não permite definir registradores diretamente
        // Esta funcionalidade requer expansão da interface da CPU
        // Por ora, o contexto básico (PC) é suficiente para demonstrar o conceito
    }
    
    /**
     * Executa uma instrução do processo atual
     */
    private boolean executarInstrucao(ProcessControlBlock pcb) {
        try {
            // Aqui seria a execução de uma única instrução
            // Por limitações da CPU atual, executamos um ciclo completo controlado
            
            // Verifica se processo chegou ao fim
            if (pcb.getPc() >= pcb.getTamanhoPrograma()) {
                return false; // Processo terminou
            }
            
            // A CPU atual executa até STOP, então precisamos de uma abordagem diferente
            // Por ora, vamos simular a execução controlada
            return true;
            
        } catch (Exception e) {
            System.out.println("ERRO na execução do processo " + pcb.getNome() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Executa múltiplos processos de forma concorrente (simulada)
     */
    public void executarProcessosConcorrentes(List<Program> programas) {
        System.out.println("\n=== Iniciando execução concorrente de " + programas.size() + " processos ===");
        
        // Cria todos os processos
        List<ProcessControlBlock> pcbsCriados = new ArrayList<>();
        for (Program programa : programas) {
            ProcessControlBlock pcb = criarProcesso(programa);
            if (pcb != null) {
                pcbsCriados.add(pcb);
            }
        }
        
        // Admite todos os processos
        for (ProcessControlBlock pcb : pcbsCriados) {
            admitirProcesso(pcb.getPid());
        }
        
        // Inicia sistema
        sistemaAtivo = true;
        
        // Executa ciclos até todos os processos terminarem
        int maxCiclos = 1000; // Proteção contra loop infinito
        int cicloAtual = 0;
        
        while (sistemaAtivo && cicloAtual < maxCiclos && escalonador.temProcessosParaExecutar()) {
            executarCicloSO();
            cicloAtual++;
            
            // Mostra progresso a cada 100 ciclos
            if (cicloAtual % 100 == 0) {
                System.out.println("Ciclo " + cicloAtual + " - Processos ativos: " + processos.size());
            }
        }
        
        System.out.println("\n=== Execução concorrente finalizada ===");
        exibirEstatisticas();
    }
    
    /**
     * Inicia o sistema
     */
    public void iniciarSistema() {
        sistemaAtivo = true;
        System.out.println("Sistema de processos iniciado");
    }
    
    /**
     * Para o sistema
     */
    public void pararSistema() {
        sistemaAtivo = false;
        System.out.println("Sistema de processos parado");
    }
    
    /**
     * Retorna processo por PID
     */
    public ProcessControlBlock getProcesso(int pid) {
        return processos.get(pid);
    }
    
    /**
     * Retorna lista de todos os processos
     */
    public List<ProcessControlBlock> getTodosProcessos() {
        return new ArrayList<>(processos.values());
    }
    
    /**
     * Retorna lista de processos ativos
     */
    public List<ProcessControlBlock> getProcessosAtivos() {
        return processos.values().stream()
                .filter(pcb -> !pcb.isFinished())
                .collect(ArrayList::new, (list, pcb) -> list.add(pcb), ArrayList::addAll);
    }
    
    /**
     * Exibe estatísticas do gerenciador
     */
    public void exibirEstatisticas() {
        System.out.println("\n=== Estatísticas do Gerenciador de Processos ===");
        System.out.println("Total de processos criados: " + totalProcessosCriados);
        System.out.println("Total de processos finalizados: " + totalProcessosFinalizados);
        System.out.println("Processos ativos: " + getProcessosAtivos().size());
        System.out.println("Próximo PID: " + proximoPID);
        System.out.println("Sistema ativo: " + sistemaAtivo);
        
        escalonador.exibirEstado();
        
        if (!processos.isEmpty()) {
            System.out.println("Processos no sistema:");
            for (ProcessControlBlock pcb : processos.values()) {
                System.out.println("  " + pcb.toString());
            }
        }
    }
    
    /**
     * Define quantum do escalonador
     */
    public void setQuantum(int quantum) {
        escalonador.setQuantum(quantum);
    }
    
    /**
     * Define limite máximo de processos concorrentes
     */
    public void setMaxProcessosConcorrentes(int max) {
        this.maxProcessosConcorrentes = max;
        System.out.println("Limite máximo de processos concorrentes definido para: " + max);
    }
    
    /**
     * Retorna o escalonador
     */
    public RoundRobinScheduler getEscalonador() {
        return escalonador;
    }
}