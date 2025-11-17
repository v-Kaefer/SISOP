# Sistema semi-funcional

* Preso em loop no Escalonador ao executar + 1 processo.
* Deadlock no CPU, ao interagir com IODevice e mandar interrupções para o InterruptHandle

    Depois do primeiro WRITE do processo 1, ele é desbloqueado e volta a rodar — mas executa o MESMO SYSCALL novamente:
    Isso acontece porque, quando você bloqueia no SYSCALL, a CPU não incrementa o PC (ela dá cpu_stop = True). Se o handler não avançar o PC quando o I/O termina, o processo volta exatamente na mesma instrução e repete o WRITE indefinidamente → aparência de “deadlock”.

Achei dois bugs objetivos no arquivo que explicam o “travamento” após o I/O, além de um race condition que pode perder a interrupção:

IODevice é construído sem o ih (handler) e o construtor está errado

A classe IODevice agora recebe ih, mas no SO.__init__ você a instancia com 3 argumentos (falta o ih).

Dentro do __init__ do IODevice há uma atribuição com quantidades diferentes (4 valores para 3 variáveis), que quebraria ou deixaria o ih perdido. 

sistema_os

Race condition: a CPU “zera” o irpt_io_complete fora do handler

No início do loop de CPU.run, há este trecho que apaga o irpt_io_complete assim que vê o valor — antes do InterruptHandling rodar:

* Espera infinita no Handler pela CPU

solução: Avance o PC do processo que completou o I/O dentro do handler de interrupção.
Faça isso usando o PCB do pid sinalizado, não o cpu.pc (que pode estar em outro processo).