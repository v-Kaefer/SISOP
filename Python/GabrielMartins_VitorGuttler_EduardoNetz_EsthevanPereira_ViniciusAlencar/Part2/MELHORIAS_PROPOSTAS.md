# Melhorias Propostas - Sistema Operacional Simulado

**Data**: 2025-11-16  
**Versão**: 1.0  
**Status**: Proposta de Implementação

---

## 🎯 Objetivo

Este documento lista melhorias propostas para o sistema operacional simulado, incluindo otimizações de performance, usabilidade e manutenibilidade.

---

## 📋 Lista de Melhorias

### 1. ⚠️ PRIORITÁRIO: Otimização de Log do Processo NOP

**Problema Identificado**:
O processo NOP (No Operation Process) executa um loop infinito que gera prints contínuos, causando:
- Sobrecarga de I/O no terminal
- Dificuldade de leitura dos logs do sistema
- Consumo desnecessário de recursos da máquina
- Poluição visual que dificulta debugging

**Código Atual (Programs.nop)**:
```python
Program("nop", [
    Word(Opcode.LDI, 0, -1, 0),      # 0: r0 = 0
    Word(Opcode.ADDI, 0, -1, 1),     # 1: r0 = r0 + 1
    Word(Opcode.JMP, -1, -1, 1),     # 2: volta para posição 1 (loop infinito)
])
```

**Impacto Visual**:
```
[CPU] Executando instrução no PC 1...
[CPU] Executando instrução no PC 2...
[CPU] Executando instrução no PC 1...
[CPU] Executando instrução no PC 2...
[CPU] Executando instrução no PC 1...
[CPU] Executando instrução no PC 2...
... (repetição infinita, centenas de linhas por segundo)
```

---

### 📝 Proposta de Implementação: Sistema de Log Throttling

#### Opção 1: Contador de Execuções com Log Periódico (RECOMENDADA)

**Descrição**: Adicionar um sistema de contagem de execuções que exibe log apenas a cada N execuções.

**Implementação**:

```python
# T2b: Adicionar na classe CPU
class CPU:
    def __init__(self, mem, debug=False):
        # ... código existente ...
        
        # T2b: Sistema de throttling para logs
        self.log_throttle_enabled = True
        self.instruction_count = 0
        self.log_every_n_instructions = 1000  # Log a cada 1000 instruções
        self.last_log_instruction = 0
    
    def run(self, quantum):
        # ... código existente ...
        
        # T2b: Log throttling para processos em loop (como NOP)
        if self.debug:
            self.instruction_count += 1
            
            # Exibir log apenas periodicamente
            if self.instruction_count - self.last_log_instruction >= self.log_every_n_instructions:
                print(f"[CPU] Executadas {self.instruction_count} instruções (PC: {self.pc}, Proc: {self.running_process.id})")
                self.last_log_instruction = self.instruction_count
```

**Vantagens**:
- ✅ Reduz drasticamente o volume de logs (de milhares para dezenas)
- ✅ Mantém visibilidade do progresso do sistema
- ✅ Não altera a lógica de execução
- ✅ Configurável via parâmetro

**Desvantagens**:
- ⚠️ Perde granularidade de debug em alguns casos

---

#### Opção 2: Log Silencioso para Processos NOP

**Descrição**: Detectar processos NOP e suprimir seus logs automaticamente.

**Implementação**:

```python
# T2b: Adicionar flag no PCB
class PCB:
    def __init__(self, page_table, is_background=False):
        # ... código existente ...
        self.is_background = is_background  # T2b: Processo em background (sem logs verbosos)

# T2b: Modificar GerenteProcessos
def cria_processo(self, programa, frame_inicial=None, use_virtual_memory=False, program_name=None):
    # ... código existente ...
    
    # T2b: Detectar processos NOP/background
    is_background = program_name and program_name.lower() == 'nop'
    
    pcb = PCB(page_table, is_background=is_background)
    # ... resto do código ...

# T2b: Modificar CPU.run para verificar flag
def run(self, quantum):
    # ... código existente ...
    
    # T2b: Log condicional baseado no tipo de processo
    if self.debug and not (self.running_process and self.running_process.is_background):
        print(f"      Executando instrução...")
```

**Vantagens**:
- ✅ Silencia completamente processos em background
- ✅ Mantém logs detalhados para processos importantes
- ✅ Automático (sem configuração manual)

**Desvantagens**:
- ⚠️ Pode ocultar problemas em processos NOP
- ⚠️ Requer lógica adicional para detecção

---

#### Opção 3: Log em Arquivo Separado

**Descrição**: Redirecionar logs de processos NOP para arquivo separado.

**Implementação**:

```python
# T2b: Sistema de logging com múltiplos destinos
import logging

class CPU:
    def __init__(self, mem, debug=False):
        # ... código existente ...
        
        # T2b: Configurar loggers separados
        self.main_logger = logging.getLogger('main')
        self.background_logger = logging.getLogger('background')
        
        # Main: console
        self.main_logger.addHandler(logging.StreamHandler())
        
        # Background: arquivo
        file_handler = logging.FileHandler('nop_processes.log')
        self.background_logger.addHandler(file_handler)
    
    def _log_instruction(self, message):
        """T2b: Log com destino baseado no tipo de processo"""
        if self.running_process and self.running_process.is_background:
            self.background_logger.info(message)
        else:
            self.main_logger.info(message)
            print(message)
```

**Vantagens**:
- ✅ Preserva todos os logs para análise posterior
- ✅ Console limpo
- ✅ Auditoria completa

**Desvantagens**:
- ⚠️ Requer biblioteca logging
- ⚠️ Cria arquivos adicionais
- ⚠️ Mais complexo de implementar

---

#### Opção 4: Log com Nível de Verbosidade

**Descrição**: Sistema de níveis de log (INFO, DEBUG, TRACE).

**Implementação**:

```python
# T2b: Enum para níveis de log
class LogLevel(Enum):
    ERROR = 0    # Apenas erros
    WARN = 1     # Avisos e erros
    INFO = 2     # Informações gerais (default)
    DEBUG = 3    # Detalhes de execução
    TRACE = 4    # Todas as instruções

class CPU:
    def __init__(self, mem, debug=False, log_level=LogLevel.INFO):
        # ... código existente ...
        self.log_level = log_level
    
    def _should_log(self, level):
        """T2b: Verificar se deve logar no nível atual"""
        return level.value <= self.log_level.value
    
    def run(self, quantum):
        # ... código existente ...
        
        # T2b: Log condicional por nível
        if self._should_log(LogLevel.TRACE):
            print(f"      Executando instrução PC={self.pc}")
        elif self._should_log(LogLevel.DEBUG) and self.instruction_count % 100 == 0:
            print(f"      [DEBUG] {self.instruction_count} instruções executadas")
```

**Uso**:
```python
# Executar com menos verbosidade
s = Sistema(tam_mem=1024, tam_pg=16, quantum=50, log_level=LogLevel.INFO)
```

**Vantagens**:
- ✅ Flexível e configurável
- ✅ Padrão da indústria
- ✅ Fácil de ajustar em runtime

**Desvantagens**:
- ⚠️ Requer refatoração de múltiplos prints
- ⚠️ Mais código para manter

---

### 📊 Comparação das Opções

| Critério | Opção 1 (Throttling) | Opção 2 (Silencioso) | Opção 3 (Arquivo) | Opção 4 (Níveis) |
|----------|---------------------|---------------------|------------------|-----------------|
| **Facilidade** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ |
| **Performance** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **Flexibilidade** | ⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| **Manutenção** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **Educacional** | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |

---

### 🎯 Recomendação Final

**Implementar Opção 1 (Throttling) como solução imediata**:
- Menor impacto no código existente
- Solução rápida e efetiva
- Pode ser combinada com outras opções futuramente

**Código Completo Proposto**:

```python
# T2b: Adicionar na classe CPU (após linha ~80)
class CPU:
    def __init__(self, mem, debug=False):
        # ... código existente ...
        
        # T2b: Sistema de throttling de logs para processos em loop
        self.log_throttle = True
        self.instruction_count_global = 0
        self.log_interval = 1000  # Exibir a cada 1000 instruções
        self.last_logged_at = 0
        
    def _should_log_instruction(self):
        """
        T2b: Determina se deve exibir log da instrução atual.
        Reduz verbosidade para processos em loop (ex: NOP).
        """
        if not self.log_throttle:
            return True
        
        self.instruction_count_global += 1
        
        if self.instruction_count_global - self.last_logged_at >= self.log_interval:
            self.last_logged_at = self.instruction_count_global
            return True
        
        return False
    
    # T2b: Modificar método run (linha ~135)
    def run(self, quantum):
        # ... código existente ...
        
        for _ in range(quantum):
            if self.cpu_stop:
                break
            
            # T2b: Log throttling
            if self.debug and self._should_log_instruction():
                print(f"      [CPU] Instrução #{self.instruction_count_global} (PC: {self.pc}, Proc: {self.running_process.id if self.running_process else 'None'})")
            
            # ... resto do código de execução ...
```

**Resultado Esperado**:
```
[CPU] Instrução #1000 (PC: 1, Proc: 1)
[CPU] Instrução #2000 (PC: 2, Proc: 1)
[CPU] Instrução #3000 (PC: 1, Proc: 1)
... (muito mais limpo e legível)
```

---

## 📝 To-Do List de Implementação

### Melhoria NOP - Log Throttling

- [ ] Adicionar atributos de throttling na classe CPU
- [ ] Implementar método `_should_log_instruction()`
- [ ] Modificar loops de execução para usar throttling
- [ ] Adicionar parâmetro `log_interval` configurável no Sistema
- [ ] Testar com processo NOP em loop
- [ ] Documentar no README
- [ ] Adicionar comando shell para ajustar intervalo: `loginterval <n>`

### Código Exemplo de Comando Shell

```python
# T2b: Adicionar no Sistema.run() (após outros comandos)
elif cmd == "loginterval":
    if len(cmd_line) >= 2:
        try:
            interval = int(cmd_line[1])
            if interval > 0:
                self.hw.cpu.log_interval = interval
                print(f"[Sistema] Intervalo de log ajustado para {interval} instruções")
            else:
                print("Erro: Intervalo deve ser positivo")
        except ValueError:
            print("Erro: Intervalo inválido")
    else:
        print(f"Intervalo atual: {self.hw.cpu.log_interval} instruções")
        print("Uso: loginterval <n>")
```

---

## 🔧 Outras Melhorias Propostas

### 2. Estatísticas de Page Fault (T2b)

**Descrição**: Adicionar contador de page faults por processo.

**Implementação**:
```python
# T2b: Adicionar no PCB
class PCB:
    def __init__(self, page_table, is_background=False):
        # ... código existente ...
        self.page_fault_count = 0  # T2b: Contador de page faults

# T2b: Incrementar no handle_page_fault
def handle_page_fault(self):
    # ... código existente ...
    pcb.page_fault_count += 1
    print(f"      [PAGE FAULT #{pcb.page_fault_count}] Processo {process_id}, Página {page_num}")
```

---

### 3. Comando para Exibir Estado da Memória Virtual (T2b)

**Descrição**: Comando `vmstat` para visualizar estado da memória virtual.

**Implementação**:
```python
# T2b: Adicionar comando no Sistema.run()
elif cmd == "vmstat":
    if self.use_virtual_memory:
        print("\n=== ESTADO DA MEMÓRIA VIRTUAL ===")
        for pcb in self.so.gp.all_processes:
            print(f"\nProcesso {pcb.id}:")
            for i, page in enumerate(pcb.page_table):
                if isinstance(page, dict):
                    print(f"  Página {i}: {page['state']:12s} | Frame: {page['frame'] if page['frame'] is not None else 'N/A':3s} | Disk: {page['disk_location']}")
        print(f"\nPage Faults Totais: {sum(p.page_fault_count for p in self.so.gp.all_processes)}")
        print("================================\n")
    else:
        print("Memória virtual não está ativa (use_virtual_memory=False)")
```

---

### 4. Visualização de Swap Space (T2b)

**Descrição**: Mostrar páginas no swap space.

**Implementação**:
```python
# T2b: Adicionar comando
elif cmd == "swapstat":
    if self.use_virtual_memory and self.so.disk_device:
        print("\n=== SWAP SPACE ===")
        if self.so.disk_device.swap_space:
            for (proc_id, page), data in self.so.disk_device.swap_space.items():
                print(f"  Processo {proc_id}, Página {page}: {len(data)} words")
        else:
            print("  Swap space vazio")
        print("==================\n")
    else:
        print("Swap space não disponível")
```

---

## 📅 Cronograma de Implementação

| Melhoria | Prioridade | Esforço | Prazo Sugerido |
|----------|-----------|---------|----------------|
| 1. Log Throttling NOP | ⭐⭐⭐⭐⭐ | 1h | Imediato |
| 2. Estatísticas Page Fault | ⭐⭐⭐⭐ | 30min | Curto prazo |
| 3. Comando vmstat | ⭐⭐⭐ | 1h | Médio prazo |
| 4. Comando swapstat | ⭐⭐ | 30min | Médio prazo |

---

## ✅ Checklist de Validação

Antes de considerar a melhoria completa:

- [ ] Código compila sem erros
- [ ] Testes manuais com processo NOP
- [ ] Comparação de performance (antes/depois)
- [ ] Documentação atualizada
- [ ] Comentários no código marcados com "T2b"
- [ ] Compatibilidade com T2a mantida

---

**Documento criado para guiar implementações futuras de melhorias no sistema.**

---

*Última atualização: 2025-11-16*  
*Versão do Sistema: T2b (Memória Virtual)*
