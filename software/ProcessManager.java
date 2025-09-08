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
    private Scheduler escalonador;
    
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
        // Usa a factory para criar o escalonador padrão (Etapa 3)
        this.escalonador = SchedulerFactory.criarEscalonador(SchedulingPolicy.ROUND_ROBIN, new Object[]{10});
        
        this.processos = new HashMap<>();
        this.proximoPID = 1;
        this.sistemaAtivo = false;
        this.maxProcessosConcorrentes = 10; // Limite padrão
        
        this.totalProcessosCriados = 0;
        this.totalProcessosFinalizados = 0;
        
        System.out.println("Gerenciador de Processos inicializado com escalonador: " + escalonador.getTipoEscalonamento());
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
        
        // Define a memória total necessária. Alguns programas, como o 'PC' (Bubble Sort),
        // precisam de mais espaço para dados do que o tamanho do seu código.
        int requiredSize = programa.length;
        if (nome.equals("PC")) {
            requiredSize = 100; // O programa PC acessa endereços de memória até 99.
        }

        // Aloca memória para o processo
        int[] tabelaPaginas = gerenciadorMemoria.alocaPrograma(programa, requiredSize, "Processo-" + pid);
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

        // Salva contexto do processo anterior (se houver e se for diferente)
        ProcessControlBlock processoAnterior = escalonador.getProcessoAtual();
        if (processoAnterior != null && processoAnterior != processoAtual && !processoAnterior.isFinished()) {
            salvarContextoCPU(processoAnterior);
        }

        // Executa uma instrução do processo
        boolean continuarExecucao = executarInstrucao(processoAtual);
        
        // Atualiza contadores do escalonador
        escalonador.executarCicloCPU();
        
        // Verifica se processo terminou
        if (!continuarExecucao || processoAtual.isFinished()) {
            escalonador.finalizarProcessoAtual();
            finalizarProcesso(processoAtual.getPid());
        } else {
            // Salva o contexto do processo que acabou de executar, caso ele continue
            salvarContextoCPU(processoAtual);
        }

        return true;
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
        
        pcb.setInterrupcao(hardware.cpu.getInterrupt());
    }
    
    /**
     * Carrega o contexto de um processo na CPU
     */
    private void carregarContextoCPU(ProcessControlBlock pcb) {
        // Define contexto na CPU
        hardware.cpu.setContext(pcb.getPc());
        hardware.cpu.setInterrupt(pcb.getInterrupcao());

        int[] regs = pcb.getRegistradores();
        for (int i = 0; i < regs.length; i++) {
            hardware.cpu.setReg(i, regs[i]);
        }
    }
    
    /**
     * Executa uma instrução do processo atual
     */
    private boolean executarInstrucao(ProcessControlBlock pcb) {
        try {
            // Carrega o contexto do processo na CPU antes de executar
            carregarContextoCPU(pcb);

            // 1. Pede para a CPU executar uma única instrução.
            //    A CPU precisa da tabela de páginas para traduzir endereços de memória.
            hardware.cpu.runInstruction(pcb.getTabelaPaginas());

            // 2. Após a execução, verifica se houve alguma interrupção (ex: STOP, I/O)
            Interrupts interrupt = hardware.cpu.getInterrupt();
            switch (interrupt) {
                case END: // Corrigido: Usar Interrupts.END
                    return false; // Processo terminou
                case INVALID_ADDRESS: // Corrigido: Usar Interrupts.INVALID_ADDRESS
                case INVALID_INSTRUCTION: // Corrigido: Usar Interrupts.INVALID_INSTRUCTION
                case OVERFLOW: // Corrigido: Usar Interrupts.OVERFLOW
                    System.out.println("ERRO na execução do processo " + pcb.getNome() + ": Interrupção " + interrupt);
                    return false; // Termina processo com erro
                default:
                    return true; // Continua execução
            }
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
     * Exibe o conteúdo do PCB e da memória de um processo
     */
    public void dumpProcesso(int pid) {
        ProcessControlBlock pcb = processos.get(pid);
        if (pcb == null) {
            System.out.println("Erro: Processo com PID " + pid + " não encontrado.");
            return;
        }

        System.out.println("--- DUMP DO PROCESSO " + pid + " (" + pcb.getNome() + ") ---");
        System.out.println(pcb.toDetailedString());

        System.out.println("--- DUMP DE MEMÓRIA DO PROCESSO ---");
        if (pcb.getTabelaPaginas() != null) {
            gerenciadorMemoria.exibeConteudoProcesso(pcb.getTabelaPaginas());
        } else {
            System.out.println("Processo não possui memória alocada.");
        }
    }

    /**
     * Exibe o conteúdo da memória física em um intervalo
     */
    public void dumpMemoria(int inicio, int fim) {
        System.out.println("--- DUMP DA MEMÓRIA FÍSICA [" + inicio + " - " + fim + "] ---");
        gerenciadorMemoria.dumpMemoriaFisica(inicio, fim);
    }

    /**
     * Ativa ou desativa o modo de trace da CPU
     */
    public void setTrace(boolean on) {
        // Esta funcionalidade depende de um método na CPU: hardware.cpu.setTraceMode(on);
        System.out.println("Modo trace " + (on ? "ativado." : "desativado."));
        System.out.println("AVISO: A funcionalidade de trace depende de implementação na CPU.java.");
    }

    /**
     * Define quantum do escalonador
     */
    public void setQuantum(int quantum) {
        escalonador.configurarParametros("quantum", quantum);
    }

    /**
     * Altera o algoritmo de escalonamento em tempo de execução
     */
    public void setEscalonador(SchedulingPolicy policy, Object... params) {
        Scheduler novoEscalonador = SchedulerFactory.criarEscalonador(policy, params);
        // Transfere processos do escalonador antigo para o novo
        if (escalonador != null) {
            escalonador.getProcessosProntos().forEach(novoEscalonador::adicionarProcesso);
        }
        this.escalonador = novoEscalonador;
        System.out.println("Escalonador alterado para: " + escalonador.getTipoEscalonamento());
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
    public Scheduler getEscalonador() {
        return escalonador;
    }
}