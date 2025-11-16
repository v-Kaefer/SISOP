#!/usr/bin/env python3
import sistema_os
import threading
import time

# Enable debug/trace mode by modifying the source temporarily
print("=== TESTE DE THROTTLE COM TRACE ===")
print()

# Create HW and SO
hw = sistema_os.HW(1024)
hw.cpu.debug = True  # Enable trace
print(f"Debug mode: {hw.cpu.debug}")
print(f"log_slowdown: {hw.cpu.log_slowdown}")
print()

so = sistema_os.SO(hw, 16, 50, use_virtual_memory=False)

# Create processes
print("Creating processes...")
pid1 = so.gp.create_process_from_name("nop")
pid2 = so.gp.create_process_from_name("fatorial")
pid3 = so.gp.create_process_from_name("fatorial")
print(f"Created: {pid1}, {pid2}, {pid3}")
print()

# Start system
print("Starting system...")
so.start_system()

# Run for a few seconds
print("Running for 3 seconds...")
time.sleep(3)

print("\n=== COUNTERS ===")
print(f"Total instructions: {hw.cpu.instruction_count_global}")
print(f"Last logged at: {hw.cpu.last_logged_at}")
print()

# Stop
so.stop_system()
time.sleep(0.5)
print("Done.")
