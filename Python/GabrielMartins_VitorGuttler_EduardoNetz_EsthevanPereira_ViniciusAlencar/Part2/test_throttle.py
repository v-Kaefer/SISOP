import sistema_os
import time

# Create system in T2a mode with debug enabled
hw = sistema_os.HW(1024)
so = sistema_os.SO(hw, 16, 50, use_virtual_memory=False)
hw.cpu.debug = True

print("=== TESTE DO LOG THROTTLING ===")
print(f"log_slowdown configurado: {hw.cpu.log_slowdown}")
print()

# Create processes
print("Criando processos...")
so.gp.cria_processo("nop", use_virtual_memory=False)
so.gp.cria_processo("fatorial", use_virtual_memory=False)
so.gp.cria_processo("fatorial", use_virtual_memory=False)
print(f"Processos criados: {len(so.gp.all_processes)}")
print()

# Start system
print("Iniciando sistema...")
so.start_system()

# Let it run for a bit
print("Executando por 5 segundos...")
time.sleep(5)

print()
print("=== VERIFICAÇÃO ===")
print(f"Instruções executadas total: {hw.cpu.instruction_count_global}")
print(f"Última instrução logada em: {hw.cpu.last_logged_at}")
print(f"Próximo log em: {hw.cpu.last_logged_at + hw.cpu.log_slowdown}")

# Stop
so.stop_system()
time.sleep(1)

print()
print("Teste concluído.")
