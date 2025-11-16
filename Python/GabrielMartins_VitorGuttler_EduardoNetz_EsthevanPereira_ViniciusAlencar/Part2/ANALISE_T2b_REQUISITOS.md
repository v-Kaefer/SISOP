# Análise Completa - Requisitos T2b (Memória Virtual)

**Disciplina**: Sistemas Operacionais - PUCRS  
**Professor**: Fernando Luís Dotti  
**Trabalho**: T2b - Memória Virtual

---

## 1. REQUISITOS IDENTIFICADOS (do Enunciado T2b)

### 1.1. Carregamento Sob Demanda (Lazy Loading)
**Descrição**: Ao criar um processo, carregar **apenas a primeira página** em um frame, não todas.

**Código Atual (GerenteMemoria.aloca)**:
```python
# PROBLEMA: Aloca TODAS as páginas necessárias
paginas_necessarias = math.ceil(num_palavras / self.tam_pg)
```

**Modificação Necessária**:
```python
# SOLUÇÃO: Alocar apenas 1 frame (primeira página)
paginas_necessarias = 1  # Sempre 1 na criação
```

**Componentes Afetados**:
- `GerenteMemoria.aloca()` - linha ~252
- `GerenteProcessos.cria_processo()` - linha ~350

---

### 1.2. Page Fault - Detecção e Tratamento

**Descrição**: Quando um endereço lógico é utilizado e a página **não está em memória**, gerar page-fault.

**Código Atual (CPU.legal)**:
```python
def legal(self, la):
    # Traduz endereço lógico para físico
    pagina = la // self.gm.tam_pg
    if pagina >= len(self.running_process.page_table):
        self.irpt = Interrupts.INT_ENDERECO_INVALIDO
        return -1
    # ⚠️ FALTA: Verificar se página está carregada em memória
```

**Modificação Necessária**:
```python
def legal(self, la):
    pagina = la // self.gm.tam_pg
    if pagina >= len(self.running_process.page_table):
        self.irpt = Interrupts.INT_ENDERECO_INVALIDO
        return -1
    
    # NOVO: Verificar estado da página
    page_entry = self.running_process.page_table[pagina]
    if page_entry['state'] != 'IN_MEMORY':
        self.irpt = Interrupts.INT_PAGE_FAULT
        self.page_fault_info = {'process_id': self.running_process.id, 'page': pagina}
        return -1
    
    # Tradução normal se página está em memória
    frame = page_entry['frame']
    offset = la % self.gm.tam_pg
    return frame * self.gm.tam_pg + offset
```

**Componentes Afetados**:
- `CPU.legal()` - linha ~175
- `Interrupts` enum - adicionar `INT_PAGE_FAULT`
- Tabela de páginas - mudar de `list[int]` para `list[dict]`

---

### 1.3. Tratamento de Page Fault (Handler)

**Fluxo Completo**:
1. Pede quadro ao GM
2. **Se GM tem quadro livre**:
   - Adiciona mapeamento página/quadro na tabela
   - Encaminha pedido para **Disco** trazer página
   - Processo vai para **BLOCKED**
   - Escalona outro processo
3. **Se GM NÃO tem quadro livre**:
   - GM escolhe vítima (política simples)
   - Encaminha pedido para **Disco** salvar página vítima
   - Processo demandante vai para **BLOCKED**
   - Escalona outro processo

**Código Necessário (InterruptHandling)**:
```python
def handle_page_fault(self):
    # Pegar informações do page fault
    process_id = self.hw.cpu.page_fault_info['process_id']
    page_num = self.hw.cpu.page_fault_info['page']
    pcb = self.gp._find_pcb(process_id)
    
    # Pedir quadro ao GM
    frame = self.gm.allocate_frame_for_page_fault()
    
    if frame is not None:
        # Caso 1: Quadro livre disponível
        # Criar pedido para Disco trazer página
        disk_request = {
            'type': 'LOAD_PAGE',
            'process_id': process_id,
            'page': page_num,
            'frame': frame
        }
        self.disk_device.io_queue.put(disk_request)
        
        # Bloquear processo
        self.gp.block_process(process_id)
        
        # Resetar flag
        self.hw.cpu.irpt = Interrupts.NO_INTERRUPT
    else:
        # Caso 2: Sem quadro livre - precisa vitimar
        victim_info = self.gm.find_victim()
        
        # Criar pedido para Disco salvar vítima
        disk_request = {
            'type': 'SAVE_PAGE',
            'victim_process_id': victim_info['process_id'],
            'victim_page': victim_info['page'],
            'victim_frame': victim_info['frame'],
            'requesting_process_id': process_id,
            'requesting_page': page_num
        }
        self.disk_device.io_queue.put(disk_request)
        
        # Bloquear processo demandante
        self.gp.block_process(process_id)
        
        # Resetar flag
        self.hw.cpu.irpt = Interrupts.NO_INTERRUPT
```

**Componentes Novos**:
- `InterruptHandling.handle_page_fault()` - CRIAR
- `GerenteMemoria.allocate_frame_for_page_fault()` - CRIAR
- `GerenteMemoria.find_victim()` - CRIAR

---

### 1.4. Novas Interrupções de Disco

**INT_PAGE_SAVE_COMPLETE** (Fim de salvamento de página)
- Ocorre quando Disco termina de salvar página vítima
- Passa quadro liberado para processo demandante
- Continua com carga da página demandada

**INT_PAGE_LOAD_COMPLETE** (Fim de carga de página)
- Ocorre quando Disco termina de carregar página em memória
- Passa processo de BLOCKED para READY

**Código Necessário**:
```python
# Adicionar em Interrupts enum
class Interrupts(Enum):
    NO_INTERRUPT, 
    INT_ENDERECO_INVALIDO, 
    INT_INSTRUCAO_INVALIDA, 
    INT_OVERFLOW, 
    INT_IO_COMPLETE,
    INT_PAGE_FAULT,           # NOVO
    INT_PAGE_SAVE_COMPLETE,   # NOVO
    INT_PAGE_LOAD_COMPLETE    # NOVO
    = range(8)

# Handler para salvamento completo
def handle_page_save_complete(self):
    save_info = self.hw.cpu.page_save_info
    
    # Libera frame da vítima
    victim_frame = save_info['victim_frame']
    
    # Criar pedido para carregar página demandada neste frame
    disk_request = {
        'type': 'LOAD_PAGE',
        'process_id': save_info['requesting_process_id'],
        'page': save_info['requesting_page'],
        'frame': victim_frame
    }
    self.disk_device.io_queue.put(disk_request)
    
    # Resetar flag
    self.hw.cpu.irpt = Interrupts.NO_INTERRUPT

# Handler para carregamento completo
def handle_page_load_complete(self):
    load_info = self.hw.cpu.page_load_info
    
    # Atualizar tabela de páginas
    pcb = self.gp._find_pcb(load_info['process_id'])
    page_entry = pcb.page_table[load_info['page']]
    page_entry['frame'] = load_info['frame']
    page_entry['state'] = 'IN_MEMORY'
    
    # Desbloquear processo
    self.gp.unblock_process(load_info['process_id'])
    
    # Resetar flag
    self.hw.cpu.irpt = Interrupts.NO_INTERRUPT
```

---

### 1.5. Dispositivo de Disco (DiskDevice)

**Descrição**: Novo dispositivo para operações de paginação.

**Funcionalidades**:
1. **Armazenar programas originais** - conteúdo inicial das páginas
2. **Salvar páginas vitimadas** - cópia de frames de memória
3. **Trazer páginas de volta** - de disco para memória

**Operações**:
- `LOAD_PAGE_FROM_PROGRAM` - trazer página do programa original
- `SAVE_PAGE_TO_SWAP` - salvar página vitimada (swap out)
- `LOAD_PAGE_FROM_SWAP` - trazer página vitimada de volta (swap in)

**Estrutura Similar ao IODevice**:
```python
class DiskDevice(threading.Thread):
    """
    DISPOSITIVO DE DISCO (T2b - Memória Virtual)
    Thread que processa pedidos de paginação de forma assíncrona.
    
    Funcionalidades:
    - Armazena programas originais (conteúdo inicial)
    - Swap space para páginas vitimadas
    - Simula latência de disco (maior que I/O console)
    """
    
    def __init__(self, hw, gp, ih):
        threading.Thread.__init__(self, daemon=True)
        self.hw = hw
        self.gp = gp
        self.ih = ih
        self.running = True
        
        # FILA DE PEDIDOS DISCO (similar ao IODevice)
        self.disk_queue = self.SimpleQueue()
        
        # ARMAZENAMENTO
        self.programs = {}  # {program_name: [Words]}
        self.swap_space = {}  # {(process_id, page): [Words]}
        
        # Latência de disco (maior que I/O console - 2s)
        self.disk_latency = 3.0  # 3 segundos
    
    class SimpleQueue:
        """Fila thread-safe para pedidos de disco"""
        def __init__(self):
            self.items = []
            self.lock = threading.Lock()
            self.not_empty = threading.Condition(self.lock)
        
        def put(self, item):
            with self.lock:
                self.items.append(item)
                self.not_empty.notify()
        
        def get(self, timeout=None):
            with self.not_empty:
                if not self.items:
                    self.not_empty.wait(timeout)
                if not self.items:
                    raise QueueEmpty()
                return self.items.pop(0)
    
    def load_program(self, program_name, program_words):
        """Carrega programa no disco (disponível para paginação)"""
        self.programs[program_name] = program_words
    
    def run(self):
        """Loop principal: aguarda pedidos, processa com latência"""
        while self.running:
            try:
                request = self.disk_queue.get(timeout=0.5)
                
                if request['type'] == 'LOAD_PAGE':
                    self._handle_load_page(request)
                elif request['type'] == 'SAVE_PAGE':
                    self._handle_save_page(request)
                    
            except QueueEmpty:
                continue
    
    def _handle_load_page(self, request):
        """Carrega página do disco para memória"""
        # Simular latência de disco
        time.sleep(self.disk_latency)
        
        # Determinar origem: programa original ou swap
        process_id = request['process_id']
        page = request['page']
        frame = request['frame']
        
        # Copiar dados para frame de memória
        # (implementação depende de como programa está armazenado)
        
        # Sinalizar CPU que carregamento terminou
        self.hw.cpu.page_load_info = {
            'process_id': process_id,
            'page': page,
            'frame': frame
        }
        self.hw.cpu.irpt_page_load_complete = process_id
    
    def _handle_save_page(self, request):
        """Salva página vítima da memória para disco"""
        # Simular latência de disco
        time.sleep(self.disk_latency)
        
        victim_process_id = request['victim_process_id']
        victim_page = request['victim_page']
        victim_frame = request['victim_frame']
        
        # Copiar frame de memória para swap space
        # (salvar para trazer de volta depois se necessário)
        
        # Sinalizar CPU que salvamento terminou
        self.hw.cpu.page_save_info = {
            'victim_frame': victim_frame,
            'requesting_process_id': request['requesting_process_id'],
            'requesting_page': request['requesting_page']
        }
        self.hw.cpu.irpt_page_save_complete = victim_process_id
```

---

### 1.6. Extensão do Esquema de Paginação

**Estados de uma Página**:
1. **NEVER_LOADED**: Nunca foi carregada em memória (conteúdo no programa original)
2. **IN_MEMORY**: Está em um frame de memória
3. **SWAPPED**: Foi vitimada e está no swap space do disco

**Nova Estrutura da Tabela de Páginas**:
```python
# ANTES (T2a): lista de frames
page_table = [0, 1, 2, 3]  # process com 4 páginas nos frames 0-3

# DEPOIS (T2b): lista de dicionários
page_table = [
    {'state': 'IN_MEMORY', 'frame': 0, 'disk_location': None},       # Página 0
    {'state': 'NEVER_LOADED', 'frame': None, 'disk_location': None},  # Página 1
    {'state': 'NEVER_LOADED', 'frame': None, 'disk_location': None},  # Página 2
    {'state': 'SWAPPED', 'frame': None, 'disk_location': 'swap_15'}   # Página 3 (vitimada)
]
```

**Modificações Necessárias**:
```python
class GerenteMemoria:
    def aloca_t2b(self, num_palavras_total, program_name):
        """
        T2b: Alocar apenas primeira página em memória.
        Criar entradas para demais páginas como NEVER_LOADED.
        """
        # Calcular total de páginas necessárias
        total_pages = math.ceil(num_palavras_total / self.tam_pg)
        
        # Alocar frame apenas para primeira página
        first_frame = self._get_free_frame()
        if first_frame is None:
            return None
        
        # Marcar frame como ocupado
        self.free_frames[first_frame] = False
        
        # Criar tabela de páginas
        page_table = []
        
        # Primeira página: IN_MEMORY
        page_table.append({
            'state': 'IN_MEMORY',
            'frame': first_frame,
            'disk_location': None
        })
        
        # Demais páginas: NEVER_LOADED
        for i in range(1, total_pages):
            page_table.append({
                'state': 'NEVER_LOADED',
                'frame': None,
                'disk_location': program_name  # Referência ao programa original
            })
        
        return page_table
```

---

## 2. COMPONENTES A SEREM MODIFICADOS/CRIADOS

### 2.1. MODIFICAR (código existente)

| Componente | Arquivo | Linhas | Modificação |
|------------|---------|--------|-------------|
| `Interrupts` | sistema_os.py | ~37 | Adicionar INT_PAGE_FAULT, INT_PAGE_SAVE_COMPLETE, INT_PAGE_LOAD_COMPLETE |
| `CPU` | sistema_os.py | ~58-83 | Adicionar flags page_fault_info, page_save_info, page_load_info |
| `CPU.legal()` | sistema_os.py | ~175 | Verificar estado página, gerar page fault |
| `GerenteMemoria.aloca()` | sistema_os.py | ~252 | Criar versão T2b que aloca só primeira página |
| `GerenteProcessos.cria_processo()` | sistema_os.py | ~350 | Usar nova alocação T2b |
| `InterruptHandling.handle()` | sistema_os.py | ~675 | Adicionar cases para novos interrupts |
| `CPUThread.run()` | sistema_os.py | ~590 | Verificar novas flags de interrupção |

### 2.2. CRIAR (código novo)

| Componente | Descrição | Estimativa Linhas |
|------------|-----------|-------------------|
| `DiskDevice` class | Thread de disco similar ao IODevice | ~150 linhas |
| `InterruptHandling.handle_page_fault()` | Handler de page fault | ~40 linhas |
| `InterruptHandling.handle_page_save_complete()` | Handler fim salvamento | ~20 linhas |
| `InterruptHandling.handle_page_load_complete()` | Handler fim carregamento | ~25 linhas |
| `GerenteMemoria.find_victim()` | Política de vitimização | ~30 linhas |
| `GerenteMemoria.allocate_frame_for_page_fault()` | Tentar alocar frame | ~15 linhas |

---

## 3. QUESTÕES A ESCLARECER ANTES DA IMPLEMENTAÇÃO

### 3.1. Política de Vitimização
**Pergunta**: Qual política usar para escolher vítima?

**Opções**:
- ✅ **FIFO** (First In First Out) - mais simples, rastrear ordem de alocação
- ✅ **Random** - ainda mais simples, escolhe frame aleatório
- ❌ **LRU** (Least Recently Used) - mais complexo, requer timestamp em cada acesso

**Recomendação**: FIFO ou Random (enunciado diz "política simples")

### 3.2. Estrutura do Disco
**Pergunta**: Como representar o disco?

**Opções**:
- ✅ **Opção A**: Array separado (mais realista)
  ```python
  self.disk_storage = [Word(Opcode.___, -1, -1, -1)] * disk_size
  ```
- ✅ **Opção B**: Dicionários em memória (mais simples)
  ```python
  self.programs = {}  # programas originais
  self.swap_space = {}  # páginas vitimadas
  ```

**Recomendação**: Opção B (mais simples, educacional)

### 3.3. Integração com IODevice
**Pergunta**: Criar DiskDevice separado ou estender IODevice?

**Opções**:
- ✅ **Opção A**: Classe DiskDevice separada (mais claro, separação de responsabilidades)
- ❌ **Opção B**: Estender IODevice (mais compacto, mas mistura I/O console com paginação)

**Recomendação**: Opção A - DiskDevice separado

### 3.4. Latência de Disco
**Pergunta**: Quanto tempo simular de latência?

**Referência**: IODevice usa 2 segundos

**Opções**:
- 3 segundos (1.5x mais lento que console)
- 5 segundos (2.5x mais lento)

**Recomendação**: 3 segundos (diferente mas não excessivo)

### 3.5. Carregamento de Programas no Disco
**Pergunta**: Como carregar programas no disco antes de criar processos?

**Opção A**: Modificar cria_processo() para registrar programa no disco primeiro
```python
def cria_processo(self, program_name):
    # 1. Registrar programa no disco
    self.disk_device.load_program(program_name, Programs.prog[program_name])
    
    # 2. Criar PCB com alocação T2b (só primeira página)
    ...
```

**Opção B**: Pré-carregar todos os programas no disco na inicialização
```python
def __init__(self):
    ...
    # Carregar todos os programas no disco
    for name, code in Programs.prog.items():
        self.disk_device.load_program(name, code)
```

**Recomendação**: Opção B (mais simples, todos programas "instalados" no sistema)

---

## 4. PLANO DE IMPLEMENTAÇÃO PROPOSTO

### Fase 1: Estruturas de Dados (1-2 horas)
1. ✅ Adicionar novas interrupções ao enum
2. ✅ Modificar tabela de páginas para dicionários
3. ✅ Adicionar flags no CPU para page fault info

### Fase 2: DiskDevice (2-3 horas)
1. ✅ Criar classe DiskDevice com SimpleQueue
2. ✅ Implementar load_program()
3. ✅ Implementar _handle_load_page()
4. ✅ Implementar _handle_save_page()
5. ✅ Integrar no Sistema.__init__()

### Fase 3: Gerente de Memória (1-2 horas)
1. ✅ Implementar aloca_t2b() - aloca só primeira página
2. ✅ Implementar find_victim() - política FIFO ou Random
3. ✅ Implementar allocate_frame_for_page_fault()
4. ✅ Modificar cria_processo() para usar T2b

### Fase 4: Detecção de Page Fault (1 hora)
1. ✅ Modificar CPU.legal() para verificar estado da página
2. ✅ Gerar INT_PAGE_FAULT quando necessário

### Fase 5: Handlers de Interrupção (2-3 horas)
1. ✅ Implementar handle_page_fault()
2. ✅ Implementar handle_page_save_complete()
3. ✅ Implementar handle_page_load_complete()
4. ✅ Integrar no InterruptHandling.handle()

### Fase 6: CPU Thread (1 hora)
1. ✅ Adicionar verificação de irpt_page_save_complete
2. ✅ Adicionar verificação de irpt_page_load_complete
3. ✅ Chamar handlers apropriados

### Fase 7: Testes (2-3 horas)
1. ✅ Testar com programas pequenos (fibonacci10)
2. ✅ Testar com múltiplos processos
3. ✅ Verificar page faults acontecendo
4. ✅ Verificar vitimização quando memória cheia
5. ✅ Verificar processos bloqueando/desbloqueando

**TOTAL ESTIMADO: 10-15 horas de desenvolvimento**

---

## 5. COMPATIBILIDADE COM T2a

### O que NÃO mudar:
- ✅ IODevice (Console I/O) - mantém funcionando
- ✅ INT_IO_COMPLETE - continua funcionando
- ✅ SysCallHandling - READ e WRITE mantidos
- ✅ Escalonador Round-Robin - sem alterações
- ✅ Estados de processo (READY, RUNNING, BLOCKED, FINISHED)

### O que adicionar:
- ✅ DiskDevice (novo dispositivo, não substitui IODevice)
- ✅ Novas interrupções (INT_PAGE_FAULT, etc)
- ✅ Novo motivo para BLOCKED (page fault, além de I/O)

---

## 6. PERGUNTAS FINAIS PARA O DESENVOLVEDOR

Antes de começar a implementação, por favor confirme:

1. **Política de Vítima**: FIFO, Random, ou outra? (Recomendo FIFO)

2. **Latência de Disco**: 3 segundos está bom? (maior que I/O console de 2s)

3. **Estrutura de Disco**: Usar dicionários em memória (simples) ou array separado (realista)?

4. **Carregamento de Programas**: Pré-carregar todos na inicialização do disco ou carregar sob demanda?

5. **Tamanho da Memória**: Manter atual (1024 palavras, 16 frames de 64 palavras) ou diminuir para forçar mais page faults? Sugestão: 512 palavras (8 frames) para testes.

6. **Debug/Trace**: Adicionar logs específicos de page fault (ex: "Page fault: Processo 1, Página 3") para facilitar depuração?

---

## 7. EXEMPLO DE EXECUÇÃO ESPERADA (T2b)

```bash
> new fibonacci10
Programa 'fibonacci10' carregado no disco
Processo 1 criado - APENAS página 0 alocada no frame 0
Páginas 1-2 marcadas como NEVER_LOADED

> start
[CPU] Executando processo 1...
[CPU] Acesso ao endereço lógico 65 (página 1)
[CPU] PAGE FAULT - Página 1 não está em memória
[InterruptHandler] Tratando page fault...
[GM] Frame livre disponível: frame 1
[DiskDevice] Carregando página 1 do programa 'fibonacci10' para frame 1
[GP] Processo 1 -> BLOCKED (aguardando página)
[CPU] Escalonando próximo processo...

(3 segundos depois)

[DiskDevice] Página 1 carregada com sucesso
[CPU] INT_PAGE_LOAD_COMPLETE recebida
[InterruptHandler] Página 1 carregada - frame 1 mapeado
[GP] Processo 1 -> READY
[CPU] Processo 1 retoma execução...
```

**Com Vitimização** (quando memória cheia):
```bash
[CPU] PAGE FAULT - Página 5 não está em memória
[InterruptHandler] Tratando page fault...
[GM] SEM frames livres - escolhendo vítima
[GM] Vítima: Processo 2, Página 1, Frame 3 (FIFO)
[DiskDevice] Salvando página vítima (P2, pag1) para swap space
[GP] Processo 1 -> BLOCKED (aguardando salvamento + carga)

(3 segundos depois)

[DiskDevice] Página vítima salva - frame 3 liberado
[DiskDevice] Carregando página 5 do processo 1 para frame 3

(3 segundos depois)

[DiskDevice] Página 5 carregada com sucesso
[GP] Processo 1 -> READY
```

---

**Aguardando confirmação para prosseguir com a implementação.**
