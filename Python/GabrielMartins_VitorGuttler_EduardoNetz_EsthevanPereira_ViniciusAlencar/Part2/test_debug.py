#!/usr/bin/env python3
import sistema_os
import time

# Create system
s = sistema_os.Sistema(tam_mem=1024, tam_pg=16, quantum=50, use_virtual_memory=False)

print(f"Initial debug state: {s.hw.cpu.debug}")
print(f"Initial log_slowdown: {s.hw.cpu.log_slowdown}")
print(f"Initial instruction_count_global: {s.hw.cpu.instruction_count_global}")
print(f"Initial last_logged_at: {s.hw.cpu.last_logged_at}")
print()

# Create one NOP process
prog_nop = s.progs.retrieve_program("nop")
pid1 = s.so.gp.cria_processo(prog_nop, None, False, "nop")
print(f"Created NOP process with PID: {pid1}")
print()

# Enable trace
s.hw.cpu.debug = True
print(f"After enabling, debug: {s.hw.cpu.debug}")
print()

# Start system
print("Starting system...")
s.so.escalonador.start()
s.so.io_device.start()

# Let it run briefly
print("Running for 2 seconds...")
time.sleep(2)

# Check counters
print()
print("=== CURRENT STATE ===")
print(f"debug: {s.hw.cpu.debug}")
print(f"instruction_count_global: {s.hw.cpu.instruction_count_global}")
print(f"last_logged_at: {s.hw.cpu.last_logged_at}")
print()

#Continue running
print("Running for 3 more seconds...")
time.sleep(3)

print()
print("Stopping system...")
s.so.escalonador.stop()
s.so.io_device.stop()

time.sleep(0.5)

print()
print("=== FINAL STATS ===")
print(f"Total instructions executed: {s.hw.cpu.instruction_count_global}")
print(f"Last logged at instruction: {s.hw.cpu.last_logged_at}")
print(f"Should have logged at: 2048, 4096, 6144, ...")
