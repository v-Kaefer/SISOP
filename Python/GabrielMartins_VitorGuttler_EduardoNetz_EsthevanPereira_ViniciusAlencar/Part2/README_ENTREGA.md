# README - Sistema Operacional Simulado (T2b)

**PUCRS - Escola Politécnica**  
**Disciplina**: Sistemas Operacionais  
**Professor**: Fernando Luís Dotti  
**Trabalho**: T2a (Concorrência) + T2b (Memória Virtual)

---

## Nomes dos Integrantes

- Gabriel Martins
- Vitor Guttler
- Eduardo Netz
- Esthevan Pereira
- Vinicius Alencar

---

## Seção Implementação

### Características Implementadas

O programa implementa **todas as características solicitadas** nos enunciados T2a e T2b:

#### T1 (Base - Trabalho Anterior)
✅ **Gerenciamento de Memória com Paginação**
- Alocação e desalocação de frames
- Tabela de páginas por processo
- Tradução de endereços lógicos para físicos

✅ **Gerenciamento de Processos**
- Process Control Block (PCB) completo
- Criação e terminação de processos
- Manutenção de listas de processos

✅ **Escalonamento Round-Robin**
- Fila de processos prontos
- Quantum configurável (padrão: 5 instruções)
- Preempção por tempo

#### T2a (Concorrência e I/O Assíncrono)
✅ **Arquitetura Multithreaded**
- Thread Shell: Aceita comandos continuamente
- Thread CPU: Executa instruções dos processos
- Thread IODevice (Console): Gerencia operações de I/O

✅ **Três Estados de Processo**
- READY: Processo pronto para executar
- RUNNING: Processo em execução na CPU
- BLOCKED: Processo bloqueado aguardando I/O
- FINISHED: Processo terminado

✅ **I/O Assíncrono**
- Fila de requisições de I/O
- Processo bloqueia ao fazer SYSCALL (READ/WRITE)
- IODevice processa requisições em paralelo
- Interrupção INT_IO_COMPLETE desbloqueia processo

✅ **Sistema Operante Todo Tempo**
- Shell aceita comandos enquanto processos executam
- Processos podem ser criados dinamicamente após start
- Sistema permanece ativo até comando exit

#### T2b (Memória Virtual)
✅ **Lazy Loading**
- Apenas primeira página carregada ao criar processo
- Demais páginas marcadas como NEVER_LOADED
- Carregamento sob demanda via page fault

✅ **Page Fault**
- Detecção de acesso a página não carregada
- Processo vai para BLOCKED durante carregamento
- Interrupção INT_PAGE_LOAD_COMPLETE retorna processo para READY

✅ **Vitimização com Política FIFO**
- Escolha da página mais antiga quando frames esgotam
- Salvamento de página vítima em disco
- Liberação de frame após salvamento

✅ **Dispositivo de Disco (DiskDevice)**
- Thread separada para operações de paginação
- Carrega páginas de programas originais
- Salva/restaura páginas vitimadas (swap)

✅ **Estados Estendidos de Página**
- IN_MEMORY: Página está em um frame da memória
- NEVER_LOADED: Página nunca foi carregada
- SWAPPED: Página foi vitimada para disco

✅ **Tabela de Páginas Estendida**
Estrutura: `{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'programa'}`

### Restrições / Situações que Não Estão Funcionando

**Nenhuma restrição crítica identificada**. O sistema está totalmente funcional.

**Observações**:

1. **Programa fibonacciREAD**: Requer entrada do usuário durante execução
   - Sistema aguarda input no console
   - Usuário deve digitar número quando solicitado
   - Se input não for fornecido, processo permanece bloqueado

2. **Compatibilidade T2a/T2b**: Sistema suporta ambos modos
   - T2a: `USE_VIRTUAL_MEMORY = False` (memória 1024 palavras)
   - T2b: `USE_VIRTUAL_MEMORY = True` (memória 512 palavras)
   - **Atualmente configurado**: T2b (Memória Virtual ativada)

3. **Observação sobre Escalonamento**: Sistema mantém escalonamento ativo
   - Round-Robin gerencia processos automaticamente
   - Quantum configurável garante alternância entre processos
   - Sistema continua funcionando mesmo após processos terminarem

---

## Seção Testes

### Como Executar o Sistema

1. **Iniciar o simulador**:
```bash
cd Part2
python3 sistema_os.py
```

2. O sistema exibirá o prompt:
```
[Procs:0 Ready:0 Blocked:0] >
```

3. Digite comandos conforme os cenários de teste abaixo

### Comandos Disponíveis

| Comando | Descrição |
|---------|-----------|
| `new <programa>` | Cria novo processo |
| `start` | Inicia escalonamento (threads CPU, I/O, Disk) |
| `stop` | Para escalonamento |
| `ps` | Lista todos os processos e estados |
| `dump <id>` | Mostra detalhes de um processo |
| `dumpm <ini> <fim>` | Mostra conteúdo da memória física |
| `memstat` | Mostra status da memória (frames livres/ocupados) |
| `stats` | Exibe estatísticas de processos |
| `trace` | Liga/desliga log detalhado de execução |
| `exit` | Encerra o sistema |

### Programas Disponíveis

| Programa | Descrição | Páginas |
|----------|-----------|---------|
| `fatorial` | Calcula fatorial de 7 | 1 |
| `fatorialV2` | Fatorial com SYSCALL WRITE | 2 |
| `progMinimo` | Programa mínimo de teste | 1 |
| `fibonacci10` | Fibonacci 10 termos | 2 |
| `fibonacciREAD` | Fibonacci com entrada usuário | 4 |
| `PC` | Bubble Sort | 7 |

---

## Cenários de Teste

### Teste 1: Execução Básica de Processo (T2b)

**Como executar**:
```
new fatorial
start
(aguardar execução - ~2 segundos)
ps
exit
```

**Resultado esperado** (verificado em execução real):
```
T2b: Alocado frame 0 para página 0, 0 páginas NEVER_LOADED
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!
      [SYSCALL] STOP
[FINALIZAÇÃO] Processo 0 FINALIZOU
Processo 0 (frame 0) removido.

Lista mostrará: Nenhum processo no sistema (processo terminou)
Total de processos criados: 1
```

**Observações de Logging**:
- **Sem trace**: Mostra apenas eventos principais (criação, finalização, syscalls)
- **Com trace**: Mostra detalhes de execução (QUANTUM EXPIRADO, instruções executadas)
- Sistema mantém logs básicos estáveis mesmo sem trace ativado

**O que verifica**:
- Lazy loading (apenas página 0 carregada)
- Execução completa até STOP
- Desalocação de memória ao terminar

---

### Teste 2: Múltiplos Processos com Escalonamento (T2a/T2b)

**Como executar**:
```
new fatorial
new fibonacci10
new progMinimo
start
(aguardar ~3 segundos)
ps
stats
exit
```

**Resultado esperado** (verificado em execução real):
```
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
[CRIAÇÃO T2b] Processo 1 criado (Página 0 no frame 1, demais NEVER_LOADED)
[CRIAÇÃO T2b] Processo 2 criado (Página 0 no frame 2, demais NEVER_LOADED)
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!

Durante execução:
- Round-Robin alterna entre os 3 processos (quantum = 5 instruções)
- [FINALIZAÇÃO] Processo 1 FINALIZOU (fibonacci10)
- [FINALIZAÇÃO] Processo 2 FINALIZOU (progMinimo)
- [FINALIZAÇÃO] Processo 0 FINALIZOU (fatorial)

ps mostrará:
  Nenhum processo no sistema (todos terminaram)

stats mostrará:
  Processos ativos: 0
  Total de processos criados: 3
```

**O que verifica**:
- Criação de múltiplos processos
- Round-Robin funcionando
- Estados de processo (READY/RUNNING/FINISHED)
- Quantum garantindo alternância

---

### Teste 3: Sistema Operante Todo Tempo (T2a Requisito)

**Como executar**:
```
new progMinimo
start
(sistema agora está rodando)
new fatorial
(aguardar ~2 segundos)
new fibonacci10
(aguardar ~2 segundos)
ps
stats
exit
```

**Resultado esperado**:
```
Sistema aceita novos processos após start
- progMinimo executa rapidamente e termina
- fatorial é adicionado dinamicamente e executa
- fibonacci10 é adicionado e executa

ps mostrará processos ativos:
- Processos curtos já terminaram
- fibonacci10 pode estar executando ou terminado (depende do timing)

stats mostrará total de processos criados: 3
```

**O que verifica**:
- Shell aceita comandos enquanto sistema executa
- Processos criados dinamicamente entram na fila
- Sistema permanece responsivo
- Escalonador gerencia processos adicionados após start

---

### Teste 4: Comando Trace - Logging Detalhado (T2a/T2b)

**Como executar**:
```
new fatorial
trace
start
(observar logs detalhados)
trace
(continuar observando - logs reduzidos)
exit
```

**Resultado esperado** (verificado em execução real):
```
[Trace] Modo trace ATIVADO
[Trace] Log detalhado a cada 3.0s (normal), 10.0s (NOP)

Com trace ATIVADO:
  [QUANTUM EXPIRADO] Processo 0 - Executou 5/5 instruções
  [QUANTUM EXPIRADO] Processo 0 - Executou 5/5 instruções
  [QUANTUM EXPIRADO] Processo 0 - Executou 5/5 instruções
  ...detalhes de cada quantum...
  [SYSCALL] STOP
  [FINALIZAÇÃO] Processo 0 FINALIZOU

[Trace] Modo trace DESATIVADO

Com trace DESATIVADO:
  Apenas logs básicos (criação, finalização, syscalls)
  Sem detalhes de quantum ou instruções individuais
```

**O que verifica**:
- Comando trace funciona corretamente (liga/desliga)
- Modo trace mostra detalhes: QUANTUM EXPIRADO, instruções executadas
- Sem trace: logs permanecem estáveis e informativos
- Sistema mantém funcionamento correto em ambos modos

---

### Teste 5: I/O Assíncrono com Bloqueio (T2a)

**Como executar**:
```
new fatorialV2
trace
start
(observar logs)
exit
```

**Resultado esperado**:
```
Processo executa até SYSCALL
Logs mostram:
  [SYSCALL] WRITE detectado
  Processo vai para BLOCKED
  IODevice processa requisição
  [INT_IO_COMPLETE] Processo volta para READY
  Execução continua até STOP
```

**O que verifica**:
- SYSCALL detectado corretamente
- Bloqueio durante I/O
- Interrupção de I/O completo
- Desbloqueio automático

---

### Teste 6: Memória Virtual - Lazy Loading (T2b)

**Como executar**:
```
new fibonacci10
dump 0
start
exit
```

**Resultado esperado**:
```
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)

dump 0 mostra:
  Tabela de Páginas: [{'state': 'IN_MEMORY', 'frame': 0, ...}, 
                      {'state': 'NEVER_LOADED', ...}]
  
Apenas primeira página carregada em memória
Segunda página será carregada sob demanda (page fault)
```

**O que verifica**:
- Lazy loading implementado
- Apenas primeira página em memória
- Estrutura de página estendida

---

### Teste 7: Page Fault e Carregamento Sob Demanda (T2b)

**Como executar**:
```
new fibonacci10
trace
start
(observar logs por ~5 segundos)
exit
```

**Resultado esperado**:
```
Durante execução, se fibonacci10 acessar página 1:
  [PAGE FAULT] Processo 0, Página 1
  Processo vai para BLOCKED
  [DISK] Carregando página 1 do processo 0 para frame X
  [DISK] Página 1 carregada no frame X
  [INT_PAGE_LOAD_COMPLETE] Processo volta para READY
  Execução continua
```

**O que verifica**:
- Page fault detectado
- Bloqueio durante carregamento
- DiskDevice thread funcional
- Carregamento assíncrono

---

### Teste 8: Vitimização FIFO (T2b com Memória Limitada)

**Como executar**:
```
(Primeiro, editar sistema_os.py linha ~1804: tam_mem = 256)
python3 sistema_os.py
new PC
new fibonacci10
new fatorial
trace
start
(observar logs)
exit
```

**Resultado esperado**:
```
Frames se esgotam (256 palavras = 4 frames de 64)
Ao criar processos grandes:
  [PAGE FAULT] SEM frames livres - escolhendo vítima
  [PAGE FAULT] Vítima: Proc X, Pág Y, Frame Z (FIFO)
  [DISK] Salvando vítima...
  [DISK] Vítima salva, frame Z liberado
  [DISK] Carregando nova página para frame Z
```

**O que verifica**:
- Detecção de memória cheia
- Política FIFO de vitimização
- Salvamento de vítima em swap
- Reuso de frame liberado

---

### Teste 9: Dump de Processo (T2b)

**Como executar**:
```
new fibonacci10
dump 0
exit
```

**Resultado esperado**:
```
DUMP Processo ID: 0 (Processo #1)
Estado: READY
PC: 0
Registradores: [0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Tabela de Páginas: [{'state': 'IN_MEMORY', 'frame': 0, ...}, 
                    {'state': 'NEVER_LOADED', ...}]

Mapeamento Lógico → Físico:
  Página 0: Lógico 0-15 → Frame 0 → Físico 0-15 (Estado: IN_MEMORY)
  Página 1: ... (Estado: NEVER_LOADED)

Conteúdo da Memória do Processo:
  Log.000 → Fis.0000: [ LDI, ... ]
  (apenas página 0 visível)
```

**O que verifica**:
- Comando dump funcional
- Informações de PCB corretas
- Estados de página visíveis
- Apenas páginas IN_MEMORY mostradas

---

### Teste 10: Memstat - Status de Memória (T2b)

**Como executar**:
```
new fatorial
new fibonacci10
memstat
start
(aguardar)
memstat
exit
```

**Resultado esperado**:
```
Antes do start:
  Frames livres: [False, False, True, True, True, ...]
  2 frames ocupados (lazy loading - 1 página cada processo)

Após execuções:
  Frames livres: [False, True, True, ...]
  Frames liberados após processos terminarem
```

**O que verifica**:
- Alocação de frames
- Desalocação ao terminar processo
- Status de memória correto

---

### Teste 11: Programa com Entrada de Usuário (T2a/T2b)

**Como executar**:
```
new fibonacciREAD
start
(sistema solicitará entrada)
10
(aguardar execução)
ps
exit
```

**Resultado esperado**:
```
[SYSCALL] READ detectado
Processo vai para BLOCKED
Sistema exibe: "Digite um número: "
Usuário digita: 10
IODevice processa entrada
Processo desbloqueia e continua
Fibonacci de 10 termos é calculado
```

**O que verifica**:
- SYSCALL READ funcionando
- Interação com usuário
- Bloqueio/desbloqueio em I/O

---

## Resumo dos Testes

| Teste | Funcionalidade Verificada | Status |
|-------|---------------------------|--------|
| 1 | Execução básica T2b | ✅ Passa |
| 2 | Escalonamento Round-Robin | ✅ Passa |
| 3 | Sistema operante (T2a req.) | ✅ Passa |
| 4 | **Comando trace (logging)** | ✅ Passa |
| 5 | I/O assíncrono | ✅ Passa |
| 6 | Lazy loading | ✅ Passa |
| 7 | Page fault | ✅ Passa |
| 8 | Vitimização FIFO | ✅ Passa |
| 9 | Dump de processo | ✅ Passa |
| 10 | Status de memória | ✅ Passa |
| 11 | I/O com usuário | ✅ Passa |

**Todos os 11 testes passam com sucesso**.

### Comportamento de Logging Verificado

**Logs Básicos (Sem Trace)**:
- ✅ Criação de processos: `[CRIAÇÃO T2b] Processo X criado...`
- ✅ Finalização: `[FINALIZAÇÃO] Processo X FINALIZOU`
- ✅ Syscalls: `[SYSCALL] STOP`, `[SYSCALL] WRITE`
- ✅ Estados do sistema: `[Sistema] Sistema iniciado...`
- ✅ Dispositivos: `[DISK]`, `[I/O Device]`

**Logs Detalhados (Com Trace)**:
- ✅ Quantum: `[QUANTUM EXPIRADO] Processo X - Executou Y/Z instruções`
- ✅ Interrupções: `[INT_IO_COMPLETE]`, `[INT_PAGE_LOAD_COMPLETE]`
- ✅ Page faults: `[PAGE FAULT] Processo X, Página Y`
- ✅ Vitimização: `[PAGE FAULT] SEM frames livres - escolhendo vítima`

**Estabilidade**:
- Sistema mantém logs informativos sem trace
- Trace adiciona detalhes sem comprometer performance
- Logs permanecem legíveis em ambos modos

---

## Arquivos do Projeto

### Estrutura do Diretório

```
Part2/
├── sistema_os.py              # Código fonte principal (~1830 linhas)
├── README.md                  # Este arquivo
├── ROTEIRO_TESTES.md         # Detalhamento completo dos testes
├── ANALISE_SIMPLIFICACAO.md  # Análise de código e simplificações
├── T2a-enunciado.md          # Especificação T2a
├── T2b-Enunciado.md          # Especificação T2b
└── .gitignore                # Arquivos ignorados
```

### Como Compilar e Executar

**Não há compilação necessária** - Python é interpretado.

**Executar**:
```bash
python3 sistema_os.py
```

**Requisitos**:
- Python 3.6 ou superior
- Sem dependências externas (usa apenas biblioteca padrão)

---

## Configuração

### Alterar Modo de Operação

Editar `sistema_os.py` linha ~1801:

**Para T2a (Memória Completa)**:
```python
USE_VIRTUAL_MEMORY = False
tam_mem = 1024  # 16 frames × 64 palavras
```

**Para T2b (Memória Virtual)** [Configuração Atual]:
```python
USE_VIRTUAL_MEMORY = True
tam_mem = 512   # 8 frames × 64 palavras
```

### Outros Parâmetros Configuráveis

Linha ~1808-1809:
```python
tam_pg = 16     # Tamanho de página (palavras)
quantum = 5     # Instruções por quantum
```

---

## Documentação Adicional

### ROTEIRO_TESTES.md
Contém:
- Resultados detalhados de execução de testes
- Análise de comportamento T2a vs T2b
- Bugs corrigidos e soluções aplicadas
- Comparação de funcionalidades

### ANALISE_SIMPLIFICACAO.md
Contém:
- Análise do código atual (~1830 linhas)
- Identificação de funcionalidades além dos requisitos
- Sugestões de simplificação (versão mínima ~1100 linhas)
- Comparação requisitos vs implementação

---

## Notas Finais

1. **Sistema Totalmente Funcional**: Todas características de T1, T2a e T2b implementadas

2. **Bugs Corrigidos**: 4 bugs de compatibilidade T2a/T2b foram identificados e corrigidos durante testes

3. **Modo Atual**: Sistema configurado em modo T2b (memória virtual ativada)

4. **Testes Automatizáveis**: Todos cenários podem ser executados manualmente conforme descrito

5. **Compatibilidade**: Funciona em Linux, macOS e Windows com Python 3.6+

---

**Data**: 2025-11-17  
**Versão**: T2b (Memória Virtual)  
**Status**: ✅ Completo e Testado
