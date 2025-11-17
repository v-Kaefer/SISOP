# Detecção de NOP + Slowdown de Logs

## 1. Objetivo do mecanismo

Esta implementação tem **dois objetivos principais**:

1. **Detectar o processo NOP** de forma confiável, sem depender do ID do frame.
2. **Aplicar o "slowdown" (throttle) de logs o tempo todo**, independente do `trace` estar ligado ou não.

   * O `trace` passa a controlar **apenas** se o log será **detalhado** (instrução a instrução) ou não.
   * O ritmo de log (devagar/rápido) continua controlado pelos parâmetros de slowdown.

---

## 2. Conceito geral

A ideia é:

* Cada instrução executada pela CPU passa por um **ponto único de decisão**: `_should_log_instruction()`.
* Esse método:

  * incrementa um contador global de instruções;
  * verifica quanto tempo passou desde o último log;
  * escolhe o intervalo correto (NOP vs processo normal);
  * decide se **já está na hora de logar de novo**.
* Se for hora de logar:

  * Se `trace` estiver **ligado**, gera um log **detalhado** (ex.: `[Instrução #X] PC: ...`).
  * Se `trace` estiver **desligado**, o slowdown ainda está valendo, e gera um log **compacto** (ex.: `[CPU] Exec #X (PC=Y)`).

Assim, o slowdown sempre funciona; o trace só define o "nível de zoom" do log.

---

## 3. Detecção robusta do processo NOP

Antes, a detecção de NOP era baseada em:

```python
len(self.running_process.page_table) == 1 and self.running_process.id == 0
```

Isso é frágil porque:

* O `id` do processo é o **frame inicial**, que depende de onde o processo foi alocado.
* Se o NOP cair em outro frame que não o 0, ele **não é reconhecido** como NOP.

### 3.1. Flag `is_nop` no PCB

Para resolver isso, o PCB passa a ter uma flag explícita:

```python
class PCB:
    def __init__(self, ...):
        ...
        self.is_nop = False
```

Quando o NOP é criado, marcamos essa flag, dentro do `GerenteProcessos.cria_processo`:

```python
def cria_processo(self, program_name, ...):
    pcb = PCB(...)
    
    # T2b: Marcar processo NOP com flag is_nop (detecção robusta)
    if program_name and program_name.lower() == "nop":
        pcb.is_nop = True
    
    ...
    return pcb.id
```

Dessa forma:

* A identificação de NOP é **independente** do frame.
* Qualquer lugar do código pode perguntar: `if pcb.is_nop:` sem "adivinhar" por ID ou tamanho da page table.

---

## 4. Lógica de slowdown centralizada

Na CPU, mantemos os parâmetros de slowdown e os contadores:

```python
class CPU:
    def __init__(self, ...):
        ...
        self.log_slowdown = 3.0        # segundos para processos normais
        self.log_slowdown_nop = 10.0   # segundos para o NOP
        self.instruction_count_global = 0
        self.last_logged_time = time.time()
        self.last_logged_instruction = 0
        self.debug = False             # controlado pelo comando trace
```

### 4.1. `_should_log_instruction()`

O método de decisão:

```python
def _should_log_instruction(self):
    """
    T2b: Determina se deve exibir log da instrução atual.
    
    Este método SEMPRE roda, independente do modo trace.
    - Incrementa contador global de instruções
    - Atualiza timer continuamente
    - Detecta NOP via flag is_nop (não via ID do frame)
    - Previne múltiplos logs no mesmo quantum
    - Usa intervalo diferente: NOP (10s) vs processos normais (3s)
    """
    self.instruction_count_global += 1
    current_time = time.time()
    elapsed = current_time - self.last_logged_time

    # Detecta se o processo atual é NOP com base na flag
    if self.running_process and getattr(self.running_process, "is_nop", False):
        interval = self.log_slowdown_nop
    else:
        interval = self.log_slowdown

    if (elapsed >= interval and
        self.instruction_count_global > self.last_logged_instruction + 1):

        self.last_logged_time = current_time
        self.last_logged_instruction = self.instruction_count_global
        return True

    return False
```

**Pontos importantes:**

* O throttle é **baseado em tempo** (`elapsed >= interval`) e não mais apenas em contagem de instruções.
* `interval` muda dinamicamente de acordo com o tipo de processo (NOP x normal).
* A função **não depende do `debug`/`trace`** → ela roda sempre.

---

## 5. Integração com o loop de execução da CPU

No `run()` (loop principal da CPU), a lógica foi invertida:

1. Sempre chama `_should_log_instruction()` (slowdown sempre ativo).
2. Usa o resultado para decidir se loga.
3. Usa `self.debug` **apenas** para dizer se o log é detalhado ou não.

Padrão de uso:

```python
# dentro do loop principal da CPU, após buscar/decodificar a instrução
should_log = self._should_log_instruction()

if should_log:
    if self.debug:
        # Log detalhado (trace ligado)
        print(f"    [Instrução #{self.instruction_count_global}] PC: {self.pc} -> INSTR: ", end="")
        self.u.dump(self.ir)
    else:
        # Log compacto (trace desligado)
        print(f"[CPU] Exec #{self.instruction_count_global} (PC={self.pc})")
```

O importante é que:

* `_should_log_instruction()` sempre roda → slowdown sempre aplicado.
* `self.debug` não controla mais **se** o slowdown existe, e sim **como** o log vai aparecer.

---

## 6. Comandos de runtime: `trace`, `logtime`, `lognop`

Os comandos interagem assim com a nova lógica:

* **`trace`**

  * Alterna `self.hw.cpu.debug` entre `True`/`False`.

  * Quando passa para `True`, resetamos os timers:

    ```python
    self.hw.cpu.last_logged_time = time.time()
    self.hw.cpu.last_logged_instruction = self.hw.cpu.instruction_count_global
    ```

  * Resultado:

    * `trace on` → logs detalhados, mas ainda respeitando slowdown.
    * `trace off` → logs compactos, também com slowdown.

* **`logtime <segundos>`**

  * Ajusta `self.hw.cpu.log_slowdown` → afeta **todos os processos não-NOP**.

* **`lognop <segundos>`**

  * Ajusta `self.hw.cpu.log_slowdown_nop` → afeta **somente o processo NOP**.

Exemplos de uso:

```bash
> logtime 5
[Log] Intervalo de log alterado para: 5.0s

> lognop 20
[Log] Intervalo de log para NOP alterado para: 20.0s

> trace
[Trace] Modo trace ATIVADO
[Trace] Log detalhado a cada 5.0s (normal), 20.0s (NOP)

> trace
[Trace] Modo trace DESATIVADO
[Trace] Log compacto continuará a cada 5.0s (normal), 20.0s (NOP)
```

---

## 7. Resumo da nova semântica

* **Detecção do NOP**

  * **Antes**: heurística frágil (`id == 0` + `len(page_table) == 1`).
  * **Agora**: flag explícita `pcb.is_nop`.

* **Slowdown**

  * Sempre ativo, através de `_should_log_instruction()`.
  * Usa tempo (`log_slowdown`, `log_slowdown_nop`) e não depende de `debug`.

* **Trace**

  * Não liga/desliga o throttle.
  * Só muda o **nível de detalhe**:

    * ON → logs de instrução detalhados.
    * OFF → logs compactos, mas no mesmo ritmo.

---

## 8. Exemplo de Execução

### Cenário 1: NOP com trace ligado

```bash
python3 sistema_os.py
> new nop
> trace
[Trace] Modo trace ATIVADO
[Trace] Log detalhado a cada 3.0s (normal), 10.0s (NOP)
> start

# Após 10 segundos
    [Instrução #50234] PC: 1 -> INSTR: [ ADDI      , R1: 0, R2:-1, P:   1 ]

# Após mais 10 segundos
    [Instrução #100567] PC: 2 -> INSTR: [ JMP       , R1:-1, R2:-1, P:   1 ]
```

### Cenário 2: NOP com trace desligado

```bash
> new nop
> start

# Após 10 segundos
[CPU] Exec #50234 (PC=1)

# Após mais 10 segundos
[CPU] Exec #100567 (PC=2)
```

### Cenário 3: Processo normal (fibonacci)

```bash
> new fibonacci10
> trace
> start

# Após 3 segundos
    [Instrução #1234] PC: 15 -> INSTR: [ MULT      , R1: 2, R2: 3, P:   4 ]

# Após mais 3 segundos
    [Instrução #2567] PC: 8 -> INSTR: [ ADD       , R1: 1, R2: 2, P:   3 ]
```

---

## 9. Benefícios da Nova Implementação

1. **Detecção robusta de NOP**: Não depende de frame inicial, funciona independente de onde o NOP for alocado.

2. **Slowdown sempre ativo**: Terminal nunca é inundado, mesmo com trace desligado.

3. **Trace controla apenas detalhe**: Permite visualização compacta (trace off) ou detalhada (trace on), sempre com throttling.

4. **Configurável em runtime**: Comandos `logtime` e `lognop` permitem ajustar intervalos sem reiniciar o sistema.

5. **Prevenção de duplicatas**: `last_logged_instruction` garante que não haverá múltiplos logs no mesmo quantum.

6. **Timer sempre atualizado**: Contador de tempo é atualizado em cada verificação, não apenas quando loga.

---

## 10. Referências no Código

* **PCB.is_nop**: Linha ~295 em `sistema_os.py`
* **Marcação de NOP**: Linha ~519 em `GerenteProcessos.cria_processo()`
* **_should_log_instruction()**: Linha ~155 em `CPU`
* **Uso no loop principal**: Linha ~195 em `CPU.run()`
* **Comando trace**: Linha ~1724 em `Sistema.run()`
* **Comandos logtime/lognop**: Linha ~1734 e ~1751 em `Sistema.run()`

---

**PUCRS - Sistemas Operacionais - Prof. Fernando Dotti**  
**Trabalho T2b - Memória Virtual + Otimização de Logs**  
**Equipe**: Gabriel Martins, Vitor Guttler, Eduardo Netz, Esthevan Pereira, Vinicius Alencar
