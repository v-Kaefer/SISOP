# 📚 Sistema Operacional Simulado - Documentação Consolidada

**PUCRS - Escola Politécnica - Sistemas Operacionais**  
**Professor**: Fernando Luís Dotti  
**Versão**: T2b (Memória Virtual)  
**Data**: 2025-11-16

---

## 📋 Índice

1. [Visão Geral](#visão-geral)
2. [Arquitetura do Sistema](#arquitetura-do-sistema)
3. [Trabalhos Implementados](#trabalhos-implementados)
4. [Memória Virtual (T2b)](#memória-virtual-t2b)
5. [Como Usar](#como-usar)
6. [Documentação de Referência](#documentação-de-referência)
7. [Melhorias Propostas](#melhorias-propostas)

---

## 🎯 Visão Geral

Este projeto é um **simulador de sistema operacional** educacional implementado em Python. Modela os componentes fundamentais de hardware e software, permitindo a criação, gerenciamento e execução de múltiplos processos em um ambiente com memória paginada e escalonamento preemptivo.

### Características Principais

- ✅ **T1**: Gerenciamento de Memória (paginação), Processos e Escalonamento
- ✅ **T2a**: Threads concorrentes (Shell, CPU, Console), I/O assíncrono, 3 estados
- ✅ **T2b**: Memória Virtual (lazy loading, page fault, swap)

---

## 🏗️ Arquitetura do Sistema

### Componentes de Hardware (HW)

| Componente | Descrição | Implementação |
|------------|-----------|---------------|
| **CPU** | Executa instruções, gerencia interrupções | `class CPU` |
| **Memory** | Memória RAM (array de Words) | `class Memory` |
| **Word** | Unidade básica (instrução ou dado) | `class Word` |
| **Opcode** | Conjunto de instruções (ISA) | `enum Opcode` |

### Componentes de Software (SO)

| Componente | Descrição | Trabalho |
|------------|-----------|----------|
| **GerenteMemoria (GM)** | Paginação, alocação/desalocação | T1, T2b |
| **GerenteProcessos (GP)** | Criação, PCB, filas (ready, blocked) | T1, T2a |
| **Escalonador** | Round-Robin com quantum | T1, T2a |
| **InterruptHandling** | Tratamento de interrupções | T1, T2a, T2b |
| **SysCallHandling** | System calls (READ, WRITE) | T2a |
| **IODevice** | Thread de I/O assíncrono (console) | T2a |
| **DiskDevice** | Thread de paginação assíncrona | **T2b** |

### Threads do Sistema (T2a/T2b)

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Thread Shell │ ──▶ │  Thread CPU  │ ──▶ │Thread Console│ ──▶ │ Thread Disk  │
│              │     │+ Escalonador │     │  (IODevice)  │     │ (DiskDevice) │
│ Comandos     │     │ Round-Robin  │     │   I/O Read   │     │  Paginação   │
│ Interativa   │     │  Execução    │     │   I/O Write  │     │   T2b Only   │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
```

---

## 📖 Trabalhos Implementados

### T1: Base do Sistema

**Componentes**:
- Gerenciamento de Memória com Paginação
- Gerenciamento de Processos (criação, PCB)
- Escalonamento Round-Robin
- Comandos Shell (new, rm, ps, dump, dumpm)

**Status**: ✅ Completo

### T2a: Concorrência e I/O

**Componentes**:
- Arquitetura Multithreaded (3 threads principais)
- I/O Assíncrono (IODevice thread)
- Modelo de 3 Estados (READY → RUNNING → BLOCKED)
- Interrupção INT_IO_COMPLETE
- Sistema Reativo (Shell não bloqueia)

**Bugs Corrigidos**:
- PC não incrementado após I/O (CRÍTICO)
- Race condition em irpt_io_complete
- Parâmetro faltante no IODevice

**Status**: ✅ Completo e Corrigido

### T2b: Memória Virtual

**Requisitos Implementados**:

#### 1. Carregamento Sob Demanda (Lazy Loading)
- Ao criar processo, carrega apenas primeira página
- Demais páginas marcadas como NEVER_LOADED
- Implementação: `GerenteMemoria.aloca_t2b()`

#### 2. Page Fault - Detecção
- CPU detecta acesso a página não carregada
- Gera interrupção INT_PAGE_FAULT
- Implementação: `CPU._translate_address()`

#### 3. Page Fault - Tratamento
- Aloca frame livre ou vitima página
- Carrega página do disco via DiskDevice
- Processo vai para BLOCKED
- Implementação: `InterruptHandling.handle_page_fault()`

#### 4. Vitimização de Páginas
- Política FIFO (First In First Out)
- Salva vítima em swap space
- Libera frame para nova página
- Implementação: `GerenteMemoria.find_victim()`

#### 5. Novas Interrupções
- `INT_PAGE_FAULT` - acesso a página não carregada
- `INT_PAGE_SAVE_COMPLETE` - fim salvamento vítima
- `INT_PAGE_LOAD_COMPLETE` - fim carregamento página

#### 6. Dispositivo de Disco
- Thread assíncrona para paginação
- Armazena programas originais
- Swap space para páginas vitimadas
- Latência: 3 segundos (vs 2s do console)
- Implementação: `class DiskDevice`

#### 7. Estados de Página
Três estados possíveis:
- **NEVER_LOADED**: Nunca foi carregada (programa original no disco)
- **IN_MEMORY**: Está em frame de memória
- **SWAPPED**: Foi vitimada (cópia no swap space)

**Estrutura da Tabela de Páginas T2b**:
```python
page_table = [
    {'state': 'IN_MEMORY',    'frame': 0,    'disk_location': 'fibonacci10'},
    {'state': 'NEVER_LOADED', 'frame': None, 'disk_location': 'fibonacci10'},
    {'state': 'SWAPPED',      'frame': None, 'disk_location': 'swap_42'}
]
```

**Status**: ✅ Completo

---

## 🚀 Como Usar

### Instalação

```bash
cd Python/GabrielMartins_VitorGuttler_EduardoNetz_EsthevanPereira_ViniciusAlencar/Part2
python3 sistema_os.py
```

### Configuração de Modo

**Modo T2a (Memória Completa)**:
```python
# Em sistema_os.py, linha ~1689
USE_VIRTUAL_MEMORY = False
```

**Modo T2b (Memória Virtual)**:
```python
# Em sistema_os.py, linha ~1689
USE_VIRTUAL_MEMORY = True
```

### Comandos Disponíveis

| Comando | Descrição | Exemplo |
|---------|-----------|---------|
| `new <programa> [frame]` | Criar processo | `new fibonacci10` |
| `rm <id>` | Remover processo | `rm 0` |
| `ps` | Listar processos | `ps` |
| `dump <id>` | Dump do processo | `dump 0` |
| `dumpm <ini> <fim>` | Dump memória física | `dumpm 0 63` |
| `memstat` | Status da memória | `memstat` |
| `stats` | Estatísticas gerais | `stats` |
| `start` | Iniciar escalonamento | `start` |
| `stop` | Parar escalonamento | `stop` |
| `exit` | Sair do sistema | `exit` |

### Programas Disponíveis

| Programa | Descrição | Páginas | Recomendado T2b |
|----------|-----------|---------|-----------------|
| `fatorial` | Calcula fatorial de 7 | 1 | Não |
| `fibonacci10` | Fibonacci até 10 termos | 2 | ✅ Sim |
| `fibonacciREAD` | Fibonacci com READ | 3 | ✅ Sim |
| `PC` | Bubble Sort de vetor | 4+ | ✅ Sim |
| `nop` | Loop infinito (manter sistema ativo) | 1 | Não |

### Fluxo de Uso Típico (T2b)

```bash
# 1. Iniciar sistema
python3 sistema_os.py

# 2. Criar processo com memória virtual
> new fibonacci10
[DISK] Programa 'fibonacci10' carregado no disco
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)

# 3. Iniciar escalonamento
> start
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!

# 4. Observar page faults
[PAGE FAULT] Processo 0, Página 1
[DISK] Carregando página 1 do processo 0 para frame 1...
[DISK] Página 1 carregada no frame 1

# 5. Ver estatísticas
> stats

# 6. Sair
> exit
```

### Exemplo com Vitimização

```bash
# 1. Usar memória pequena (forçar page faults)
# Alterar no código: tam_mem=256 (4 frames de 64 palavras)

> new PC
[PAGE FAULT] Processo 0, Página 3
[PAGE FAULT] SEM frames livres - escolhendo vítima
[PAGE FAULT] Vítima: Proc 0, Pág 0, Frame 0 (FIFO)
[DISK] Salvando vítima...
[DISK] Vítima salva, frame 0 liberado
[DISK] Carregando página 3 no frame 0...
```

---

## 📚 Documentação de Referência

### Documentos Principais

| Documento | Finalidade | Quando Consultar |
|-----------|------------|------------------|
| **README_CONSOLIDADO.md** | Este documento - visão geral | Início, referência rápida |
| **T2b-Enunciado.md** | Especificação oficial T2b | Requisitos detalhados |
| **MELHORIAS_PROPOSTAS.md** | Propostas de otimização | Implementar melhorias |
| **sistema_os.py** | Código fonte completo | Implementação |

### Documentos de Análise (Arquivados)

Estes documentos foram criados durante o planejamento e podem ser consultados para detalhes:

- `ANALISE_T2b_REQUISITOS.md` - Análise técnica detalhada (628 linhas)
- `MAPA_VISUAL_T2b.md` - Diagramas visuais de fluxo (278 linhas)
- `RESUMO_T2b.md` - Resumo executivo (232 linhas)
- `INDICE_T2b.md` - Guia de navegação (286 linhas)
- `RESUMO_FINAL_T2b.md` - Resumo final (277 linhas)

**Nota**: Estes documentos de análise podem ser removidos após validação da implementação.

### Documentos de Contexto (T2a)

- `T2a-enunciado.md` - Especificação T2a
- `CONTEXTO_MODIFICACOES.md` - Análise T2a
- `ARQUITETURA_COMPARACAO.md` - Comparação arquitetural
- `BUGS.md` - Bugs conhecidos (corrigidos)

---

## 🔧 Melhorias Propostas

### Prioridade ALTA

#### 1. Otimização de Log do Processo NOP

**Problema**: NOP gera prints infinitos, sobrecarregando terminal.

**Solução Proposta**: Log Throttling (exibir a cada N instruções)

```python
# T2b: Implementação sugerida
class CPU:
    def __init__(self, mem, debug=False):
        self.log_interval = 1000  # Exibir a cada 1000 instruções
        self.instruction_count_global = 0
        self.last_logged_at = 0
```

**Detalhes**: Ver `MELHORIAS_PROPOSTAS.md`

#### 2. Comandos de Diagnóstico T2b

**vmstat**: Visualizar estado da memória virtual
```bash
> vmstat
Processo 0:
  Página 0: IN_MEMORY    | Frame: 0   | Disk: fibonacci10
  Página 1: SWAPPED      | Frame: N/A | Disk: swap_15
```

**swapstat**: Visualizar swap space
```bash
> swapstat
Processo 0, Página 1: 64 words
Processo 0, Página 3: 64 words
```

### Prioridade MÉDIA

- Estatísticas de page fault por processo
- Logs de debug níveis configuráveis
- Gráfico de timeline de estados

**Ver mais**: `MELHORIAS_PROPOSTAS.md`

---

## 🧪 Testes e Validação

### Checklist de Funcionalidades

**T1 (Base)**:
- [x] Criação de processos
- [x] Alocação de memória paginada
- [x] Escalonamento Round-Robin
- [x] Comandos shell funcionando

**T2a (Concorrência)**:
- [x] Threads executando concorrentemente
- [x] I/O assíncrono (READ/WRITE)
- [x] Estados READY/RUNNING/BLOCKED
- [x] Interrupção INT_IO_COMPLETE
- [x] PC incrementado após I/O (bug corrigido)

**T2b (Memória Virtual)**:
- [x] Lazy loading (primeira página apenas)
- [x] Page fault detectado
- [x] Page fault tratado (com frame livre)
- [x] Vitimização FIFO
- [x] Salvamento de vítima em swap
- [x] Carregamento de página do disco
- [x] Processo desbloqueia após load
- [x] Compatibilidade T2a mantida

### Casos de Teste

**Teste 1: Lazy Loading**
```bash
> new fibonacci10
# Espera: Apenas página 0 alocada
```

**Teste 2: Page Fault Simples**
```bash
> start
# Espera: Page fault na página 1, carregamento do disco
```

**Teste 3: Vitimização**
```bash
# Usar memória pequena (256 bytes)
> new PC
# Espera: Múltiplos page faults, vitimizações FIFO
```

**Teste 4: Compatibilidade T2a**
```bash
# USE_VIRTUAL_MEMORY = False
> new fibonacci10
# Espera: Todas as páginas alocadas imediatamente
```

---

## 📊 Estrutura do Código

### Organização do Arquivo `sistema_os.py`

```
Linhas    | Seção
----------|--------------------------------------------------
1-25      | Imports e estruturas auxiliares
26-208    | Hardware (CPU, Memory, Word, Opcode, Interrupts)
209-333   | GerenteMemoria (T1 + métodos T2b)
334-540   | GerenteProcessos e PCB (T1 + T2a + T2b)
541-592   | Escalonador (T1 + T2a)
593-680   | IODevice - Thread Console (T2a)
681-873   | DiskDevice - Thread Disk (T2b) ← NOVO
874-978   | CPUThread (T2a)
979-1120  | InterruptHandling (T1 + T2a + handlers T2b)
1121-1228 | SysCallHandling (T2a)
1229-1256 | Classe SO (integração)
1257-1530 | Programs (programas de teste)
1531-1688 | Sistema (CLI e comandos)
1689-1703 | Main
```

### Comentários Marcados por Etapa

Todos os comentários no código seguem o padrão:
- `# T1:` - Funcionalidade do trabalho 1
- `# T2a:` - Funcionalidade do trabalho 2a
- `# T2b:` - Funcionalidade do trabalho 2b (memória virtual)

---

## 👥 Equipe

- Gabriel Martins
- Vitor Guttler
- Eduardo Netz
- Esthevan Pereira
- Vinicius Alencar

---

## 📝 Notas de Implementação

### Decisões de Design T2b

1. **Política de Vítima**: FIFO escolhida por simplicidade
2. **Estrutura de Disco**: Dicionários (educacional, não array)
3. **Latência de Disco**: 3s (diferenciado do console 2s)
4. **Tabela de Páginas**: Dicionários (flexível vs lista)
5. **Compatibilidade**: T2a continua funcionando (flag)

### Performance

- Memória: 1024 bytes (T2a) ou 512 bytes (T2b)
- Tamanho de página: 16 palavras (64 bytes)
- Quantum: 50 instruções
- Latência I/O Console: 2 segundos
- Latência Disco: 3 segundos

---

## 🔗 Links Úteis

- **Repositório**: https://github.com/v-Kaefer/SISOP
- **Enunciado T2b**: `T2b-Enunciado.md`
- **Melhorias**: `MELHORIAS_PROPOSTAS.md`
- **Código**: `sistema_os.py`

---

**Última Atualização**: 2025-11-16  
**Versão**: T2b - Memória Virtual Implementada  
**Status**: ✅ Funcional e Testado
