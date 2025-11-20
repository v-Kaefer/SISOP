# Roteiro de Apresentação - Sistema Operacional T2b

## Autores
Gabriel Martins, Vitor Guttler, Eduardo Netz, Esthevan Pereira, Vinicius Alencar

---

## 1. Demonstração Inicial - Sistema Funcionando

### 1.1. Iniciar o Sistema
```bash
cd Python/GabrielMartins_VitorGuttler_EduardoNetz_EsthevanPereira_ViniciusAlencar/Part2
python3 sistema_os.py
```

**O que mostrar**:
- Sistema inicia em modo T2b (Memória Virtual)
- Memória: 512 palavras (16 frames × 32 palavras)
- Disco virtual ativo

### 1.2. Demonstração Básica de Funcionamento

**Comandos para demonstrar**:
```bash
# 1. Criar processo com lazy loading
new fatorial
memstat
# Mostrar: Apenas página 0 carregada (lazy loading!)

# 2. Ativar trace para ver detalhes
trace

# 3. Iniciar escalonamento
start

# 4. Observar execução
ps
# Mostrar: Processo alternando entre RUNNING/READY

# 5. Ver memória após execução
memstat
dump 0
```

**Pontos a destacar**:
- ✅ Lazy loading (só primeira página carregada)
- ✅ Escalonamento Round-Robin com quantum
- ✅ Processo finaliza e memória é preservada para inspeção
- ✅ Sistema operante (aceita comandos durante execução)

---

## 2. Explicação do Fluxo de Page Fault

### 2.1. O que é Page Fault

**Explicação verbal**:
> "Page fault ocorre quando a CPU tenta acessar uma página que não está na memória física. O sistema precisa buscar essa página do disco e carregá-la na memória antes de continuar a execução."

### 2.2. Fluxo Completo de Page Fault no Sistema

**Passo-a-passo para explicar**:

#### **Passo 1: Tradução de Endereço Falha**
```
CPU tenta acessar endereço lógico (ex: 35)
↓
GerenteMemoria.traduz() é chamado
↓
Calcula: página = 35 // 32 = 1
↓
Verifica tabela de páginas[1]
↓
Estado: NEVER_LOADED (não está em memória!)
↓
GERA INTERRUPÇÃO: INT_PAGE_FAULT
```

**Código relevante** (linha 322-326):
```python
if entry.get('state') == 'IN_MEMORY':
    frame = entry['frame']
    return frame * self.palavras_por_frame + offset
else:
    return None  # ← Page fault!
```

#### **Passo 2: CPU Detecta Page Fault**
```
CPU recebe None da tradução
↓
Cria interrupção INT_PAGE_FAULT
↓
Salva contexto do processo (PC, registradores)
↓
Processo vai para estado BLOCKED
↓
Escalonador escolhe próximo processo
```

**Código relevante** (linha 819-829):
```python
if endereco_fisico is None:
    self.interrupcoes_pendentes.append({
        'tipo': Interrupcoes.INT_PAGE_FAULT,
        'pid': self.processo_atual.id,
        'pagina': pagina,
        'endereco_logico': self.processo_atual.reg[registrador]
    })
```

#### **Passo 3: Handler Processa Page Fault**
```
Handler recebe INT_PAGE_FAULT
↓
Identifica: PID, número da página necessária
↓
Solicita ao DiskDevice carregar página
↓
DiskDevice inicia operação assíncrona (1 segundo)
```

**Código relevante** (linha 654-670):
```python
def handle_page_fault(self, interrupcao):
    pid = interrupcao['pid']
    pagina = interrupcao['pagina']
    
    # Aloca frame na memória
    frame = self.gm.aloca_frame(pid)
    
    if frame is not None:
        # Solicita carregamento do disco
        self.disk_device.load_page_from_disk(...)
    else:
        # Precisa vitimar uma página!
```

#### **Passo 4: Carregamento da Página**
```
DiskDevice trabalha em background
↓
Simula tempo de acesso ao disco (1 segundo)
↓
Carrega conteúdo da página na memória
↓
Atualiza tabela de páginas: NEVER_LOADED → IN_MEMORY
↓
Gera interrupção: INT_PAGE_LOAD_COMPLETE
```

**Código relevante** (linha 891-906):
```python
def load_page_from_disk(self, pid, pagina, frame, program_name):
    def load_task():
        time.sleep(1)  # Simula acesso ao disco
        
        # Carrega conteúdo
        conteudo = self.virtual_disk[program_name][pagina]
        self.gm.memoria[frame] = conteúdo
        
        # Atualiza tabela de páginas
        pcb.page_table[pagina] = {
            'state': 'IN_MEMORY',
            'frame': frame,
            'disk_location': program_name
        }
        
        # Notifica conclusão
        self.interrupcoes.append(INT_PAGE_LOAD_COMPLETE)
```

#### **Passo 5: Processo Desbloqueado**
```
Handler recebe INT_PAGE_LOAD_COMPLETE
↓
Encontra processo que estava bloqueado
↓
Processo: BLOCKED → READY
↓
Processo volta para fila de prontos
↓
Será executado novamente pelo escalonador
↓
Desta vez, tradução funciona!
```

### 2.3. Demonstração Prática de Page Fault

**Comandos para demonstrar**:
```bash
# Limpar sistema
exit
python3 sistema_os.py

# Criar processo que cause page fault
new fibonacci10
trace  # Para ver os detalhes
start

# Observar nos logs:
# 1. [PAGE FAULT] Processo X, Página Y
# 2. [DISK] Carregando página Y do disco...
# 3. [INT_PAGE_LOAD_COMPLETE] Página Y carregada
# 4. [GP] Processo X desbloqueado
```

**Pontos a destacar**:
- ✅ Page fault ocorre automaticamente no acesso a páginas não carregadas
- ✅ Processo bloqueia enquanto espera o disco
- ✅ Outros processos continuam executando (sistema operante)
- ✅ Processo retoma de onde parou após carregamento

---

## 3. Como Vitimar uma Página

### 3.1. Explicação Verbal

> "Vitimização ocorre quando a memória está cheia e precisamos carregar uma nova página. O sistema escolhe uma página vítima (usando FIFO), salva seu conteúdo no disco se foi modificada, e libera o frame para a nova página."

### 3.2. Condições para Vitimização

**Para vitimização ocorrer**:
1. Memória física cheia (todos frames ocupados)
2. Page fault em novo processo ou nova página
3. Sistema precisa liberar um frame

### 3.3. Algoritmo FIFO Implementado

**Explicação do código** (linha 342-358):
```python
def aloca_frame(self, pid):
    # Procura frame livre
    for i, livre in enumerate(self.free_frames):
        if livre:
            self.free_frames[i] = False
            self.frame_to_process[i] = pid
            return i
    
    # Não há frames livres → VITIMIZAÇÃO FIFO!
    frame_vitima = self.fifo_queue.pop(0)  # Remove primeiro da fila
    pid_vitima = self.frame_to_process[frame_vitima]
    
    # Libera frame da vítima
    # Atualiza tabela da vítima: IN_MEMORY → SWAPPED
    
    # Aloca para novo processo
    self.free_frames[frame_vitima] = False
    self.frame_to_process[frame_vitima] = pid
    self.fifo_queue.append(frame_vitima)  # Adiciona no fim
    
    return frame_vitima
```

### 3.4. Fluxo Completo de Vitimização

**Passo-a-passo**:

#### **Passo 1: Memória Cheia**
```
Todos 16 frames ocupados
↓
Novo page fault ocorre
↓
aloca_frame() não encontra frame livre
↓
INICIA VITIMIZAÇÃO
```

#### **Passo 2: Escolha da Vítima (FIFO)**
```
fifo_queue = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
↓
Remove primeiro: frame_vitima = 0
↓
Identifica processo dono: pid_vitima = X
```

#### **Passo 3: Salvar Vítima no Disco (se modificada)**
```
Verifica se página foi modificada (dirty bit)
↓
Se modificada:
    DiskDevice.save_page_to_disk(conteúdo)
    Atualiza tabela: IN_MEMORY → SWAPPED
Se não modificada:
    Apenas descarta (já está no disco)
```

#### **Passo 4: Carregar Nova Página**
```
Frame 0 agora livre
↓
Carrega nova página no frame 0
↓
Atualiza tabela do novo processo: página → frame 0
↓
Adiciona frame 0 no fim da fila FIFO
fifo_queue = [1, 2, 3, ..., 15, 0]
```

### 3.5. Demonstração de Vitimização

**Configuração necessária**:
```python
# Reduzir memória para 256 palavras (8 frames)
# No sistema_os.py, linha ~18:
MEMORY_SIZE_WORDS = 256  # Era 512
```

**Comandos para demonstrar**:
```bash
# Com memória reduzida
python3 sistema_os.py

# Criar múltiplos processos
new fatorial
new fibonacci10  
new progMinimo
trace
start

# Observar nos logs:
# [VITIMIZAÇÃO] Frame X será vitimado (PID: Y, Página: Z)
# [DISK] Salvando página Z no disco...
# [DISK] Carregando página W no frame X...
```

**Pontos a destacar**:
- ✅ FIFO: Primeira página carregada é primeira a sair
- ✅ Página vítima salva no disco antes de ser substituída
- ✅ Transparente para o processo (continua executando)
- ✅ Tabela de páginas atualizada automaticamente

---

## 4. Recursos Implementados (Checklist)

### T1 - Fundamentos
- ✅ Memória paginada (16 frames × 32 palavras)
- ✅ Processos com PCB completo
- ✅ Escalonamento Round-Robin com quantum

### T2a - Multithreading
- ✅ CPU thread (execução)
- ✅ I/O Device thread (entrada/saída assíncrona)
- ✅ Disk Device thread (acesso ao disco)
- ✅ 3 estados de processo (READY, RUNNING, BLOCKED)
- ✅ Sistema operante (aceita comandos durante execução)

### T2b - Memória Virtual
- ✅ Lazy loading (só página 0 inicial)
- ✅ Page fault com interrupção
- ✅ Carregamento sob demanda
- ✅ Vitimização FIFO
- ✅ Disco virtual
- ✅ Estados de página (NEVER_LOADED, IN_MEMORY, SWAPPED)

---

## 5. Dicas para Apresentação

### O que o Professor Pode Perguntar

**1. "Como funciona o lazy loading?"**
> "No momento da criação do processo, carregamos apenas a página 0 na memória. As demais páginas ficam marcadas como NEVER_LOADED na tabela de páginas. Quando o processo tenta acessar essas páginas, ocorre page fault e elas são carregadas sob demanda."

**2. "O que acontece quando a memória fica cheia?"**
> "Usamos algoritmo FIFO para escolher uma página vítima. A primeira página carregada é a primeira a sair. Salvamos o conteúdo no disco se foi modificada, depois carregamos a nova página no frame liberado."

**3. "Como o processo não perde dados durante page fault?"**
> "Quando ocorre page fault, salvamos o contexto completo do processo (PC, registradores) e o colocamos em estado BLOCKED. Após a página ser carregada, restauramos o contexto exato e o processo continua de onde parou."

**4. "O sistema continua funcionando durante I/O?"**
> "Sim! Usamos threads separadas para CPU, I/O e Disk. Quando um processo bloqueia por I/O ou page fault, o escalonador escolhe outro processo pronto e a CPU continua executando."

### Comandos Essenciais para Demonstrar

```bash
# Ver processos e estados
ps

# Ver memória física
memstat

# Ver tabela de páginas de um processo
dump <id>

# Ativar/desativar logging detalhado
trace

# Criar processo
new <programa>

# Iniciar escalonamento
start

# Sair
exit
```

---

## 6. Arquivos para Entrega

```
GabrielMartins_VitorGuttler_EduardoNetz_EsthevanPereira_ViniciusAlencar/
├── sistema_os.py              # Código fonte principal
├── programas/                 # Programas de teste
│   ├── fatorial.txt
│   ├── fibonacci10.txt
│   ├── fibonacciREAD.txt
│   └── progMinimo.txt
├── README.md                  # Documentação principal (344 linhas)
├── README.html                # Versão HTML para PDF
├── ANALISE_SIMPLIFICACAO.md   # Análise de código
├── AJUSTES_SOLICITADOS.md     # Roadmap T2b
└── ROTEIRO_APRESENTACAO.md    # Este arquivo
```

**Gerar ZIP para entrega**:
```bash
cd Python
zip -r GabrielMartins_VitorGuttler_EduardoNetz_EsthevanPereira_ViniciusAlencar.zip \
  GabrielMartins_VitorGuttler_EduardoNetz_EsthevanPereira_ViniciusAlencar/
```

---

## 7. Sequência Sugerida de Apresentação

1. **Iniciar sistema** e mostrar modo T2b ativo
2. **Criar processo** (`new fatorial`) e mostrar lazy loading com `memstat`
3. **Ativar trace** para mostrar logs detalhados
4. **Iniciar execução** (`start`) e observar escalonamento
5. **Explicar fluxo de page fault** usando diagrama verbal
6. **Demonstrar page fault** com processo maior (`new fibonacci10`)
7. **Explicar vitimização FIFO** (se tempo permitir, com memória reduzida)
8. **Mostrar comandos** `ps`, `dump`, `memstat` para inspeção
9. **Responder perguntas** do professor

**Tempo estimado**: 10-15 minutos

Boa sorte na apresentação! 🚀
