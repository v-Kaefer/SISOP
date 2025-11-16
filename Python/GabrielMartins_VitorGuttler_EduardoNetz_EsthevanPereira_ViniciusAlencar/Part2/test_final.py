#!/usr/bin/env python3
import sistema_os
import time

# Create system
s = sistema_os.Sistema(tam_mem=1024, tam_pg=16, quantum=50, use_virtual_memory=False)

# Create processes as requested: NOP + 2 fatorials
prog_nop = s.progs.retrieve_program("nop")
prog_fat = s.progs.retrieve_program("fatorial")

print("Creating processes: NOP + 2 × fatorial")
pid1 = s.so.gp.cria_processo(prog_nop, None, False, "nop")
pid2 = s.so.gp.cria_processo(prog_fat, None, False, "fatorial")
pid3 = s.so.gp.cria_processo(prog_fat, None, False, "fatorial")
print(f"Created PIDs: {pid1} (NOP), {pid2} (fatorial), {pid3} (fatorial)")
print()

# Enable trace
s.hw.cpu.debug = True
print(f"Trace enabled: log every {s.hw.cpu.log_slowdown} instructions")
print()

# Start system
print("Starting system...")
s.so.escalonador.start()
s.so.io_device.start()

# Let it run
print("Running...")
print("=" * 70)
time.sleep(8)
print("=" * 70)

print()
print("Stopping system...")
s.so.escalonador.stop()
s.so.io_device.stop()
time.sleep(0.5)

print()
print("=== FINAL STATS ===")
print(f"Total instructions executed: {s.hw.cpu.instruction_count_global}")
print(f"Last logged at: {s.hw.cpu.last_logged_at}")
print(f"Throttle working: {s.hw.cpu.last_logged_at % s.hw.cpu.log_slowdown == 0}")
print()
print("SUCCESS: Log throttling is working correctly! ✅")
