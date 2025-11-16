#!/usr/bin/env python3
import sistema_os
import time

# Create system
s = sistema_os.Sistema(tam_mem=1024, tam_pg=16, quantum=50, use_virtual_memory=False)

# Create processes
print("Creating NOP and fatorial processes...")
prog_nop = s.progs.retrieve_program("nop")
prog_fat = s.progs.retrieve_program("fatorial")

pid1 = s.so.gp.cria_processo(prog_nop, None, False, "nop")
pid2 = s.so.gp.cria_processo(prog_fat, None, False, "fatorial")
pid3 = s.so.gp.cria_processo(prog_fat, None, False, "fatorial")

print(f"Created PIDs: {pid1}, {pid2}, {pid3}")
print()

# Enable trace
print("Enabling trace...")
s.hw.cpu.debug = True
print(f"Trace enabled: {s.hw.cpu.debug}")
print(f"log_slowdown: {s.hw.cpu.log_slowdown}")
print()

# Start system
print("Starting system...")
s.so.escalonador.start()
s.so.io_device.start()

# Let it run
print("Running for 5 seconds...")
print("=" * 60)
sys.stdout.flush()
time.sleep(5)
print("=" * 60)

print()
print("Stopping system...")
s.so.escalonador.stop()
s.so.io_device.stop()

time.sleep(0.5)

print()
print("=== STATS ===")
print(f"Total instructions executed: {s.hw.cpu.instruction_count_global}")
print(f"Last logged at instruction: {s.hw.cpu.last_logged_at}")
print(f"Expected first log at: 2048")
print(f"Expected second log at: 4096")
