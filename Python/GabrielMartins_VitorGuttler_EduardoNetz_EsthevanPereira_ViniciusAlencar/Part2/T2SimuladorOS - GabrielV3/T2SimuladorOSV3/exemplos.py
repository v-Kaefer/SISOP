#!/usr/bin/env python3
"""
Script de exemplo de uso do simulador de SO
Demonstra como criar e executar processos programaticamente
"""

import time
from sistema_os import Sistema

def exemplo_basico():
    """Exemplo básico: criar e executar um processo"""
    print("\n=== EXEMPLO BÁSICO ===\n")
    
    # Criar sistema com configuração padrão
    sistema = Sistema(mem_size=64, tam_pg=16, quantum=5)
    
    # Iniciar threads do sistema
    sistema.so.io_device.daemon = True
    sistema.so.hw.cpu.daemon = True
    sistema.so.io_device.start()
    sistema.so.hw.cpu.start()
    
    # Criar processo
    print("Criando processo Fibonacci...")
    pid = sistema.so.gp.cria_processo(sistema.progs.retrieve_program("fibonacci"))
    time.sleep(1)
    
    # Adicionar à fila de prontos
    print("Adicionando processo à fila de prontos...")
    sistema.so.gp.executa_processo(pid)
    sistema.so.hw.cpu.cpu_has_process_event.set()
    
    # Aguardar execução
    print("Aguardando execução (10 segundos)...")
    time.sleep(10)
    
    # Exibir estado
    sistema.exibir_estado_completo()
    
    # Encerrar
    sistema.so.shutdown = True
    sistema.so.hw.cpu.cpu_has_process_event.set()
    time.sleep(1)
    print("\nExemplo concluído!")

def exemplo_multiplos_processos():
    """Exemplo avançado: múltiplos processos concorrentes"""
    print("\n=== EXEMPLO MÚLTIPLOS PROCESSOS ===\n")
    
    sistema = Sistema(mem_size=64, tam_pg=16, quantum=3, log_file="exemplo_log.txt")
    
    sistema.so.io_device.daemon = True
    sistema.so.hw.cpu.daemon = True
    sistema.so.io_device.start()
    sistema.so.hw.cpu.start()
    
    # Criar vários processos
    programas = ["fibonacci", "fatorial", "PB", "PC"]
    pids = []
    
    print("Criando processos...")
    for prog in programas:
        pid = sistema.so.gp.cria_processo(sistema.progs.retrieve_program(prog))
        pids.append(pid)
        time.sleep(0.5)
    
    print(f"\nProcessos criados: {pids}")
    
    # Adicionar todos à fila de prontos
    print("\nAdicionando todos à fila de prontos...")
    sistema.so.gp.executa_todos_prontos()
    sistema.so.hw.cpu.cpu_has_process_event.set()
    
    # Monitorar execução
    print("\nMonitorando execução (20 segundos)...")
    for i in range(4):
        time.sleep(5)
        print(f"\n--- Status após {(i+1)*5} segundos ---")
        sistema.so.gp.exibir_estatisticas()
    
    # Estado final
    print("\n--- ESTADO FINAL ---")
    sistema.exibir_estado_completo()
    
    # Encerrar
    sistema.so.shutdown = True
    sistema.so.hw.cpu.cpu_has_process_event.set()
    time.sleep(1)
    print("\nExemplo concluído! Verifique 'exemplo_log.txt' e 'memory_dumps/'")

def exemplo_page_faults():
    """Exemplo focado em page-faults e vitimação"""
    print("\n=== EXEMPLO PAGE-FAULTS E VITIMAÇÃO ===\n")
    
    # Memória pequena para forçar vitimação
    sistema = Sistema(mem_size=48, tam_pg=16, quantum=5, log_file="pagefault_log.txt")
    
    sistema.so.io_device.daemon = True
    sistema.so.hw.cpu.daemon = True
    sistema.so.io_device.start()
    sistema.so.hw.cpu.start()
    
    print("Criando processos com múltiplas páginas...")
    programas = ["PC", "PB", "fibonacci", "fatorial"]
    
    for prog in programas:
        sistema.so.gp.cria_processo(sistema.progs.retrieve_program(prog))
        time.sleep(0.3)
    
    sistema.so.gp.executa_todos_prontos()
    sistema.so.hw.cpu.cpu_has_process_event.set()
    
    print("\nAguardando page-faults e vitimações (15 segundos)...")
    time.sleep(15)
    
    print("\n--- ANÁLISE DE MEMÓRIA ---")
    sistema.so.gm.exibir_status()
    
    print("\n--- PÁGINAS EM SWAP ---")
    print(f"Total de páginas vitimadas: {len(sistema.so.disk.swap_area)}")
    for (pid, page), _ in sistema.so.disk.swap_area.items():
        print(f"  Processo {pid}, Página {page}")
    
    sistema.so.shutdown = True
    sistema.so.hw.cpu.cpu_has_process_event.set()
    time.sleep(1)
    print("\nExemplo concluído! Verifique 'pagefault_log.txt'")

if __name__ == "__main__":
    import sys
    
    if len(sys.argv) > 1:
        exemplo = sys.argv[1]
        if exemplo == "basico":
            exemplo_basico()
        elif exemplo == "multiplos":
            exemplo_multiplos_processos()
        elif exemplo == "pagefaults":
            exemplo_page_faults()
        else:
            print("Exemplos disponíveis: basico, multiplos, pagefaults")
    else:
        print("Uso: python exemplos.py [basico|multiplos|pagefaults]")
        print("\nExecutando exemplo básico por padrão...\n")
        exemplo_basico()
