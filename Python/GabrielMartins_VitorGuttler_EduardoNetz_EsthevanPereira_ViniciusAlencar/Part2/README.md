# Simulador de Sistema Operacional - Python

**PUCRS - Escola Politécnica - Sistemas Operacionais**  
**Professor**: Fernando Luís Dotti  
**Versão**: T2b (Memória Virtual)

---

## 🎯 Visão Geral

Simulador educacional de sistema operacional implementado em Python, modelando componentes fundamentais de hardware e software.

### Funcionalidades

- ✅ **T1**: Gerenciamento de Memória (paginação), Processos e Escalonamento
- ✅ **T2a**: Threads concorrentes, I/O assíncrono, 3 estados (READY/RUNNING/BLOCKED)
- ✅ **T2b**: Memória Virtual (lazy loading, page fault, swap, vitimização FIFO)

---

## 🚀 Início Rápido

### Executar

```bash
python3 sistema_os.py
```

### Modo de Operação

**T2a (Memória Completa)** - padrão:
```python
USE_VIRTUAL_MEMORY = False  # linha ~1689
```

**T2b (Memória Virtual)**:
```python
USE_VIRTUAL_MEMORY = True  # linha ~1689
```

### Comandos Básicos

```bash
> new fibonacci10    # Criar processo
> start              # Iniciar escalonamento  
> ps                 # Listar processos
> stats              # Estatísticas
> exit               # Sair
```

---

## 📚 Documentação

| Documento | Finalidade |
|-----------|------------|
| **README_CONSOLIDADO.md** | 📖 Documentação completa e guia de uso |
| **MELHORIAS_PROPOSTAS.md** | 🔧 Propostas de otimização (incluindo NOP) |
| **T2b-Enunciado.md** | 📋 Especificação oficial T2b |
| **sistema_os.py** | 💻 Código fonte (~1700 linhas) |

**➡️ Consulte `README_CONSOLIDADO.md` para documentação detalhada**

---

## 🏗️ Arquitetura

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│ Thread Shell │ ──▶ │  Thread CPU  │ ──▶ │Thread Console│ ──▶ │ Thread Disk  │
│              │     │+ Escalonador │     │  (IODevice)  │     │ (DiskDevice) │
│ Comandos     │     │ Round-Robin  │     │   I/O Async  │     │  Paginação   │
│ Interativa   │     │  Execução    │     │   READ/WRITE │     │   T2b Only   │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
       │                     │                     │                     │
       ▼                     ▼                     ▼                     ▼
    GP (PCB)          Memória Paginada       Fila I/O            Swap Space
```

---

## 🎮 T2b - Memória Virtual

### Carregamento Sob Demanda

```bash
> new fibonacci10
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
```

### Page Fault

```bash
> start
[PAGE FAULT] Processo 0, Página 1
[DISK] Carregando página 1 do processo 0 para frame 1...
[DISK] Página 1 carregada no frame 1
```

### Vitimização (memória cheia)

```bash
[PAGE FAULT] SEM frames livres - escolhendo vítima
[PAGE FAULT] Vítima: Proc 0, Pág 0, Frame 0 (FIFO)
[DISK] Salvando vítima...
[DISK] Vítima salva, frame 0 liberado
```

---

## 📊 Estrutura do Código

| Componente | Linhas | Trabalho |
|------------|--------|----------|
| Hardware (CPU, Memory) | 1-208 | T1 |
| GerenteMemoria | 209-333 | T1 + T2b |
| GerenteProcessos | 334-540 | T1 + T2a + T2b |
| IODevice (Console) | 593-680 | T2a |
| **DiskDevice (Paginação)** | **681-873** | **T2b** |
| InterruptHandling | 979-1120 | T1 + T2a + T2b |
| Programs | 1257-1530 | T1 |
| Sistema (CLI) | 1531-1688 | T1 + T2a + T2b |

**Total**: ~1700 linhas  
**Comentários**: Marcados com T1, T2a ou T2b

---

## 🧪 Programas de Teste

| Programa | Descrição | Páginas | T2b |
|----------|-----------|---------|-----|
| `fatorial` | Calcula fatorial de 7 | 1 | ❌ |
| `fibonacci10` | Fibonacci até 10 termos | 2 | ✅ |
| `fibonacciREAD` | Fibonacci com READ interativo | 3 | ✅ |
| `PC` | Bubble Sort de vetor | 4+ | ✅ |
| `nop` | Loop infinito (manter sistema ativo) | 1 | ⚠️ * |

**\*** Ver `MELHORIAS_PROPOSTAS.md` para otimização de log do NOP

---

## 🔧 Configuração

### Parâmetros (main, linha ~1689)

```python
USE_VIRTUAL_MEMORY = False  # True para T2b
tam_mem = 512 if USE_VIRTUAL_MEMORY else 1024
tam_pg = 16      # Tamanho da página (palavras)
quantum = 50     # Instruções por fatia de tempo
```

### Memória

- **T2a**: 1024 palavras = 16 frames × 64 palavras
- **T2b**: 512 palavras = 8 frames × 64 palavras (forçar page faults)

---

## ✅ Status de Implementação

### T1 - Base
- [x] Gerenciamento de Memória (paginação)
- [x] Gerenciamento de Processos (PCB)
- [x] Escalonamento Round-Robin
- [x] Comandos Shell

### T2a - Concorrência
- [x] Arquitetura Multithreaded
- [x] I/O Assíncrono (IODevice)
- [x] 3 Estados (READY/RUNNING/BLOCKED)
- [x] Interrupção INT_IO_COMPLETE
- [x] Bugs corrigidos (PC após I/O, race conditions)

### T2b - Memória Virtual
- [x] Lazy loading (primeira página apenas)
- [x] Page fault (detecção e tratamento)
- [x] Vitimização (política FIFO)
- [x] DiskDevice (thread de paginação)
- [x] Swap space
- [x] Estados de página (NEVER_LOADED, IN_MEMORY, SWAPPED)
- [x] Compatibilidade T2a mantida

---

## 🐛 Bugs Conhecidos e Corrigidos

### T2a - Corrigidos
- ✅ PC não incrementado após I/O (causava loop infinito)
- ✅ Race condition em irpt_io_complete
- ✅ Parâmetro faltante no IODevice.__init__

### Melhorias Propostas
- ⏳ Log throttling para processo NOP (ver MELHORIAS_PROPOSTAS.md)
- ⏳ Comandos vmstat e swapstat (T2b)
- ⏳ Estatísticas de page fault por processo

---

## 👥 Equipe

- Gabriel Martins
- Vitor Guttler
- Eduardo Netz
- Esthevan Pereira
- Vinicius Alencar

---

## 📝 Notas

### Para Testes T2b

1. Ativar memória virtual: `USE_VIRTUAL_MEMORY = True`
2. Usar memória pequena para forçar page faults: `tam_mem=256`
3. Executar programas maiores: `fibonacci10`, `PC`
4. Observar logs de page fault e vitimização

### Compatibilidade

- Python 3.6+
- Sem dependências externas (apenas stdlib)
- Multiplataforma (Linux, macOS, Windows)

---

**➡️ Documentação completa em `README_CONSOLIDADO.md`**

**Última Atualização**: 2025-11-16  
**Versão**: T2b (Memória Virtual)  
**Status**: ✅ Funcional

## Funcionalidades Principais

- **Arquitetura de CPU Simulada:** Uma CPU com um conjunto de instruções (ISA) próprio, registradores, e capacidade de gerar interrupções.
- **Gerenciamento de Memória Paginada:** A memória física é dividida em frames, e os processos recebem um espaço de endereçamento lógico através de tabelas de páginas.
- **Gerenciamento de Processos:** Suporte completo ao ciclo de vida de um processo, incluindo criação, término e armazenamento de contexto em um Process Control Block (PCB).
- **Escalonador Preemptivo (Round-Robin):** Um escalonador que gerencia uma fila de processos prontos, concedendo a cada um uma fatia de tempo (quantum) de CPU.
- **Tratamento de Interrupções e System Calls:** O sistema é capaz de lidar com exceções de hardware (endereço inválido, instrução inválida, overflow) e chamadas de sistema para I/O (leitura e escrita).
- **Interface de Linha de Comando (CLI):** Uma interface interativa para carregar programas, gerenciar processos e inspecionar o estado do sistema.

## Arquitetura do Sistema

O simulador é dividido em duas camadas principais: Hardware (HW) e Software (SO), refletindo a separação de responsabilidades em um sistema computacional real.

### Camada de Hardware (HW)

- **`CPU`**: O cérebro da simulação. Executa o ciclo de instrução (busca, decodificação, execução). Contém:
    - Registradores (`reg`): Armazenamento rápido para operações.
    - Program Counter (`pc`): Aponta para a próxima instrução lógica a ser executada.
    - Instruction Register (`ir`): Armazena a instrução atual.
    - Lógica de tradução de endereços (MMU simulada) que converte endereços lógicos em físicos usando a tabela de páginas do processo.
- **`Memory`**: Representa a memória RAM como um array de `Word`.
- **`Word`**: A menor unidade de dados/instrução, contendo um `Opcode` e seus parâmetros.
- **`Opcode` e `Interrupts`**: Enumerações que definem o conjunto de instruções suportado pela CPU e os tipos de interrupções que ela pode gerar.

### Camada de Software (SO)

- **`GerenteMemoria`**: Responsável por alocar e desalocar frames de memória física. Ele mantém um mapa de frames livres e cria as tabelas de páginas para os novos processos.
- **`PCB (Process Control Block)`**: Uma estrutura de dados que armazena todo o contexto de um processo: seu ID, estado (Pronto, Executando, Terminado), PC, valores dos registradores e sua tabela de páginas.
- **`GerenteProcessos`**: Gerencia o ciclo de vida dos processos. É responsável por:
    - Criar novos processos (alocando memória e criando um PCB).
    - Manter a fila de processos prontos (`ready_queue`).
    - Terminar processos, liberando seus recursos.
- **`Escalonador`**: Implementa o algoritmo de escalonamento Round-Robin. Ele seleciona o próximo processo da fila de prontos, restaura seu contexto na CPU e o executa por um `quantum`.
- **`InterruptHandling` e `SysCallHandling`**: Contêm as rotinas de tratamento que são invocadas pela CPU quando uma interrupção ou uma chamada de sistema ocorre. Por exemplo, uma interrupção de endereço inválido faz com que o `InterruptHandling` marque o processo como terminado.
- **`Sistema`**: A classe principal que inicializa e integra todos os componentes de HW e SO. Ela também gerencia a interface de linha de comando (CLI), interpretando os comandos do usuário.

## Como Executar

Para iniciar o simulador, basta executar o script Python em um terminal.

```bash
python3 sistema_os.py
```

O sistema irá apresentar um prompt `>` aguardando comandos.

## Comandos Disponíveis

- `new <nomePrograma>`: Cria um novo processo a partir de um programa pré-definido.
- `rm <id>`: Remove um processo do sistema, liberando sua memória.
- `ps`: Lista todos os processos existentes e seus estados.
- `exec <id>`: Executa um único processo até que ele termine ou seja interrompido.
- `execall`: Inicia o escalonador para executar todos os processos na fila de prontos de forma concorrente.
- `dump <id>`: Exibe informações detalhadas de um processo, incluindo seu PCB e o conteúdo de sua memória lógica.
- `dumpm <inicio> <fim>`: Exibe o conteúdo de um intervalo da memória física.
- `traceon` / `traceoff`: Ativa ou desativa o modo de depuração, que mostra cada instrução sendo executada.
- `exit`: Encerra a execução do simulador.

## Programas de Exemplo

O simulador inclui vários programas para teste, como:

- `fatorial`, `fatorialV2`: Calcula o fatorial de um número.
- `fibonacci10`, `fibonacciREAD`: Gera a sequência de Fibonacci. A versão `READ` utiliza uma chamada de sistema para ler o tamanho da sequência do usuário.
- `PC`: Ordena um vetor de números usando o algoritmo Bubble Sort.
- **`nop`**: Processo NOP (No Operation Process) - Loop infinito que mantém o sistema ativo. Útil para garantir que o sistema continue rodando e aceitando novos processos via CLI. Recomendado iniciar este processo antes de outros para manter o sistema operacional sempre ativo.

### Uso Recomendado do Processo NOP

Para manter o sistema operacional rodando continuamente e permitir a criação de novos processos a qualquer momento:

```bash
# 1. Criar o processo NOP
> new nop

# 2. Iniciar o escalonamento
> start

# 3. O sistema agora está rodando continuamente
# Você pode adicionar mais processos a qualquer momento
> new fatorial
> new fibonacci10
```

O processo NOP garante que sempre haja pelo menos um processo na fila de prontos, mantendo o sistema operacional ativo e responsivo.

# Implementação da Parte 2 do Trabalho

Instruções para entrega no moodle:  
    Devido ao alto número de grupos, o professor pode ter que executar os trabalhos off-line,
    sem interação com o grupo, para proceder a avaliação.    Assim pede-se seguir estas orientações.
    Monte um diretório cujo nome seja a concatenação dos primeiros nomes dos integrantes do grupo.   
    Ex.: JoãoMariaJosé.
    Coloque seus fontes neste diretório, com toda sua estrutura de arquivos para compilar e executar.
    Escreva um arquivo README.pdf (ou txt) contendo os itens abaixo, e coloque neste diretório.
              Nomes dos integrantes. 
              Seção Implementação: 
                     Informe se seu programa implementa todas as características solicitadas.
                     E se há alguma restrição (situação que não está funcionando).
              Seção Testes
                     Para cada teste (cenário de execução) indique como executar e 
                     o resultado esperado em cada caso.
      Comprima este diretório com .ZIP e entregue na sala do moodle.   
      Por favor use *somente .ZIP*  e NÃO RAR.
   O objetivo é que ao abrir este .ZIP na máquina do professor, 
   ele terá uma pasta completa com seu trabalho, com tudo que é necessário para 
   executar ali dentro, seguindo o seu relatório.



Hey, add `libspnav` as a dependency. Otherwise, it will fail with:
'''bash
    CMake Error at /usr/share/cmake/Modules/FindPackageHandleStandardArgs.cmake:227 (message):  Could NOT find SPNAV (missing: SPNAV_LIBRARY SPNAV_INCLUDE_DIR) Call Stack (most recent call first):  /usr/share/cmake/Modules/FindPackageHandleStandardArgs.cmake:591 (_FPHSA_FAILURE_MESSAGE)  cmake/FindSPNAV.cmake:51 (find_package_handle_standard_args)  CMakeLists.txt:814 (find_package)
'''