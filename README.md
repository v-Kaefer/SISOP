# SISOP
T1 - SISOP

Estrutura Modularizada

```
SISOP
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


# TO-DO

[ ] Gerencia de Memória
[ ] Gerencia de Processos
[ ] Escalonamento -> Round-Robin (definido pelo prof)

* Para o controle do tempo, vamos usar os ciclos de CPU.

Melhor metodologia para desenvolver o trabalho:

* Gerente de Memória -> Gerente de Processos -> Escalonamento

# Para compilar e rodar:

Na pasta SISOP, execute:

`javac Sistema.java`

Para rodar, execute:

`java Sistema`