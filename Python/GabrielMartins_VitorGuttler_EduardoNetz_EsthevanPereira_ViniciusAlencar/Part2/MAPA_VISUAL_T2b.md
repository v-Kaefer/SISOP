# Mapa Visual - Implementação T2b

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    REQUISITOS T2b - MEMÓRIA VIRTUAL                      │
└─────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 1. CARREGAMENTO SOB DEMANDA (Lazy Loading)                               │
├───────────────────────────────────────────────────────────────────────────┤
│ ANTES (T2a):                                                              │
│   new fibonacci10 → Aloca 3 páginas → Frames 0, 1, 2                     │
│                                                                           │
│ DEPOIS (T2b):                                                             │
│   new fibonacci10 → Aloca 1 página  → Frame 0                            │
│                     Páginas 1,2 marcadas NEVER_LOADED                     │
│                                                                           │
│ COMPONENTE: GerenteMemoria.aloca() ← MODIFICAR (não recriar)             │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 2. PAGE FAULT - Detecção                                                 │
├───────────────────────────────────────────────────────────────────────────┤
│ Fluxo:                                                                    │
│   CPU executa → Acessa endereço 65 (página 1)                            │
│                                                                           │
│   CPU.legal(65) → Calcula: página = 65 / 64 = 1                          │
│                → Verifica: page_table[1]['state'] == 'NEVER_LOADED'      │
│                → Gera: INT_PAGE_FAULT                                     │
│                → Bloqueia CPU (cpu_stop = True)                           │
│                                                                           │
│ COMPONENTE: CPU.legal() ← MODIFICAR (+5 linhas verificação)              │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 3. PAGE FAULT - Tratamento (SEM Vítima)                                  │
├───────────────────────────────────────────────────────────────────────────┤
│ InterruptHandler.handle_page_fault():                                     │
│   1. GM.allocate_frame_for_page_fault() → retorna frame 1 (livre)        │
│   2. Criar pedido LOAD_PAGE para DiskDevice                               │
│   3. GP.block_process(processo_id)                                        │
│   4. Escalonador escolhe próximo processo                                 │
│                                                                           │
│ (3 segundos depois...)                                                    │
│                                                                           │
│ DiskDevice termina → CPU.irpt_page_load_complete = processo_id           │
│                                                                           │
│ InterruptHandler.handle_page_load_complete():                             │
│   1. Atualiza page_table[1] = {state: 'IN_MEMORY', frame: 1}             │
│   2. GP.unblock_process(processo_id)                                      │
│   3. Processo volta para fila READY                                       │
│                                                                           │
│ COMPONENTE: InterruptHandling ← ADICIONAR MÉTODOS (2 novos)              │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 4. PAGE FAULT - Tratamento (COM Vítima)                                  │
├───────────────────────────────────────────────────────────────────────────┤
│ InterruptHandler.handle_page_fault():                                     │
│   1. GM.allocate_frame_for_page_fault() → retorna None (sem livre)       │
│   2. GM.find_victim() → escolhe {proc: 2, page: 1, frame: 3} (FIFO)      │
│   3. Criar pedido SAVE_PAGE para DiskDevice (salvar vítima)               │
│   4. GP.block_process(processo_demandante)                                │
│                                                                           │
│ (3 segundos depois...)                                                    │
│                                                                           │
│ DiskDevice termina salvamento → CPU.irpt_page_save_complete              │
│                                                                           │
│ InterruptHandler.handle_page_save_complete():                             │
│   1. Marca vítima: page_table[1] = {state: 'SWAPPED', disk: 'swap_15'}   │
│   2. Criar pedido LOAD_PAGE (agora usando frame 3 liberado)               │
│                                                                           │
│ (3 segundos depois...)                                                    │
│                                                                           │
│ DiskDevice termina carregamento → CPU.irpt_page_load_complete            │
│                                                                           │
│ InterruptHandler.handle_page_load_complete():                             │
│   1. Atualiza page_table do demandante                                    │
│   2. GP.unblock_process(processo_demandante)                              │
│                                                                           │
│ COMPONENTE: GerenteMemoria ← ADICIONAR find_victim() (novo método)       │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 5. DISPOSITIVO DE DISCO                                                  │
├───────────────────────────────────────────────────────────────────────────┤
│ class DiskDevice(threading.Thread):                                       │
│     """                                                                   │
│     Thread independente para operações de paginação                       │
│     Similar ao IODevice (console I/O)                                     │
│     """                                                                   │
│                                                                           │
│     def __init__(self, hw, gp, ih):                                       │
│         self.disk_queue = SimpleQueue()  ← COPIAR do IODevice             │
│         self.programs = {}               # Programas originais            │
│         self.swap_space = {}             # Páginas vitimadas              │
│         self.disk_latency = 3.0          # 3s (vs 2s do console)          │
│                                                                           │
│     def run(self):                                                        │
│         """Loop: aguarda pedidos, processa, sinaliza CPU"""               │
│         while self.running:                                               │
│             request = self.disk_queue.get()                               │
│             if request['type'] == 'LOAD_PAGE':                            │
│                 self._handle_load_page(request)                           │
│             elif request['type'] == 'SAVE_PAGE':                          │
│                 self._handle_save_page(request)                           │
│                                                                           │
│     def _handle_load_page(self, request):                                 │
│         time.sleep(self.disk_latency)  # Simula acesso ao disco           │
│         # Copiar dados para memória                                       │
│         # Sinalizar CPU                                                   │
│                                                                           │
│     def _handle_save_page(self, request):                                 │
│         time.sleep(self.disk_latency)                                     │
│         # Copiar memória para swap_space                                  │
│         # Sinalizar CPU                                                   │
│                                                                           │
│ NOVO COMPONENTE: DiskDevice ← CRIAR CLASSE (~150 linhas)                 │
│ JUSTIFICATIVA: Não dá para adicionar ao IODevice (responsabilidades       │
│                diferentes - console vs paginação)                         │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 6. ESTADOS DE PÁGINA                                                     │
├───────────────────────────────────────────────────────────────────────────┤
│ ANTES (T2a): page_table = [0, 1, 2]  # Lista de frames                   │
│                                                                           │
│ DEPOIS (T2b): page_table = [                                             │
│     {'state': 'IN_MEMORY',    'frame': 0,    'disk': None},               │
│     {'state': 'NEVER_LOADED', 'frame': None, 'disk': 'fibonacci10'},      │
│     {'state': 'SWAPPED',      'frame': None, 'disk': 'swap_42'}           │
│ ]                                                                         │
│                                                                           │
│ Estados:                                                                  │
│   NEVER_LOADED → Página nunca carregada (disk = nome do programa)        │
│   IN_MEMORY    → Página em memória (frame = número do frame)             │
│   SWAPPED      → Página vitimada (disk = localização no swap)            │
│                                                                           │
│ COMPONENTE: PCB.page_table ← MODIFICAR ESTRUTURA                          │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ 7. NOVAS INTERRUPÇÕES                                                    │
├───────────────────────────────────────────────────────────────────────────┤
│ class Interrupts(Enum):                                                   │
│     NO_INTERRUPT,                   # Existente                           │
│     INT_ENDERECO_INVALIDO,          # Existente                           │
│     INT_INSTRUCAO_INVALIDA,         # Existente                           │
│     INT_OVERFLOW,                   # Existente                           │
│     INT_IO_COMPLETE,                # Existente (T2a)                     │
│     INT_PAGE_FAULT,          ← NOVO # Acesso a página não carregada       │
│     INT_PAGE_SAVE_COMPLETE,  ← NOVO # Fim salvamento vítima               │
│     INT_PAGE_LOAD_COMPLETE   ← NOVO # Fim carregamento página             │
│     = range(8)                                                            │
│                                                                           │
│ COMPONENTE: Interrupts enum ← ADICIONAR 3 VALORES                         │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ DIAGRAMA DE FLUXO COMPLETO                                               │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│  [new fibonacci10]                                                        │
│         │                                                                 │
│         ▼                                                                 │
│  DiskDevice.load_program('fibonacci10', [Words...])                      │
│         │                                                                 │
│         ▼                                                                 │
│  GM.aloca_t2b(num_palavras)                                               │
│     → Aloca frame 0 para página 0                                         │
│     → Marca páginas 1,2 como NEVER_LOADED                                 │
│         │                                                                 │
│         ▼                                                                 │
│  [start] → CPU executa                                                    │
│         │                                                                 │
│         ▼                                                                 │
│  CPU acessa endereço 65 (página 1)                                        │
│         │                                                                 │
│         ▼                                                                 │
│  CPU.legal(65) → page_table[1]['state'] == 'NEVER_LOADED'                │
│         │                                                                 │
│         ▼                                                                 │
│  CPU.irpt = INT_PAGE_FAULT                                                │
│         │                                                                 │
│         ▼                                                                 │
│  InterruptHandler.handle_page_fault()                                     │
│         │                                                                 │
│         ├─ GM tem frame livre? ─┬─ SIM ─┐                                │
│         │                        │       │                                │
│         │                        └─ NÃO ─┼─ GM.find_victim()              │
│         │                                │   DiskDevice.SAVE_PAGE         │
│         │                                │   (3s)                         │
│         │                                │   handle_page_save_complete()  │
│         │                                │                                │
│         └────────────────────────────────┘                                │
│                                          │                                │
│                                          ▼                                │
│                              DiskDevice.LOAD_PAGE                         │
│                                      (3s)                                 │
│                                          │                                │
│                                          ▼                                │
│                          handle_page_load_complete()                      │
│                                          │                                │
│                                          ▼                                │
│                          GP.unblock_process()                             │
│                                          │                                │
│                                          ▼                                │
│                              Processo volta a executar                    │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ RESUMO DE MODIFICAÇÕES                                                    │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│ ✅ REUTILIZAR (90% do trabalho):                                          │
│    • Padrão threading (IODevice → DiskDevice)                             │
│    • SimpleQueue (copiar implementação)                                   │
│    • Interrupts enum (adicionar valores)                                  │
│    • InterruptHandling (adicionar métodos)                                │
│    • GerenteMemoria (adicionar métodos)                                   │
│    • CPU.legal() (adicionar verificação)                                  │
│    • Estado BLOCKED (reutilizar)                                          │
│                                                                           │
│ 📝 CRIAR (10% do trabalho):                                               │
│    • DiskDevice class                (~150 linhas)                        │
│    • handle_page_fault()             (~40 linhas)                         │
│    • handle_page_save_complete()     (~20 linhas)                         │
│    • handle_page_load_complete()     (~25 linhas)                         │
│    • find_victim()                   (~30 linhas)                         │
│    • allocate_frame_for_page_fault() (~15 linhas)                         │
│    ──────────────────────────────────────────────                         │
│    TOTAL:                            ~280 linhas                          │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ PERGUNTAS ANTES DE IMPLEMENTAR                                           │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│ ❓ Q1: Criar DiskDevice separado?                                         │
│    → Não dá para adicionar ao IODevice (responsabilidades diferentes)    │
│    → Aprovado? [ ] Sim [ ] Não [ ] Alternativa: ___________              │
│                                                                           │
│ ❓ Q2: Política de vítima?                                                │
│    → FIFO (rastrear ordem) ou Random (mais simples)?                     │
│    → Aprovado? [ ] FIFO [ ] Random [ ] Outra: ___________                │
│                                                                           │
│ ❓ Q3: Estrutura de disco?                                                │
│    → Dicionários (simples) ou Array (realista)?                          │
│    → Aprovado? [ ] Dicionários [ ] Array                                 │
│                                                                           │
│ ❓ Q4: Latência de disco?                                                 │
│    → 3 segundos? (vs 2s do console)                                      │
│    → Aprovado? [ ] 3s [ ] 5s [ ] Outro: ___________                      │
│                                                                           │
│ ❓ Q5: Tamanho de memória para testes?                                    │
│    → 8 frames (força page faults) ou 16 (atual)?                         │
│    → Aprovado? [ ] 8 [ ] 16 [ ] Configurável                             │
│                                                                           │
│ ❓ Q6: Adicionar logs de debug?                                           │
│    → [PAGE FAULT] [DISK] [VICTIM] para verificar funcionamento           │
│    → Aprovado? [ ] Sim [ ] Não [ ] Só se trace ativo                     │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────────────────────┐
│ STATUS: ✅ ANÁLISE COMPLETA - ⏸️ AGUARDANDO APROVAÇÃO                     │
└───────────────────────────────────────────────────────────────────────────┘
```

**Arquivos Gerados:**
1. `ANALISE_T2b_REQUISITOS.md` - Análise técnica detalhada (628 linhas)
2. `RESUMO_T2b.md` - Resumo executivo com questões (240 linhas)
3. `MAPA_VISUAL_T2b.md` - Este diagrama visual

**Próximo Passo:**
Responder às 6 perguntas acima para iniciar implementação.
