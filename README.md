# SISOP
T1 - SISOP

Estrutura Modularizada

```
pucrs.sisop
├── Sistema.java                # Main (instancia HW, SO, Programs) + run()
├── hardware/
│   ├── CPU.java
│   ├── HW.java
│   ├── Interrupts.java
│   ├── Memory.java
│   ├── Opcode.java
│   └── Word.java
├── programs/
│   ├── Program.java
│   └── Programs.java
└── software/
    ├── InterruptHandling.java
    ├── SO.java
    ├── SysCallHandling.java
    └── Utilities.java
```