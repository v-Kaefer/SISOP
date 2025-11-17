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

2. **Log do Processo NOP**: Processo em loop infinito pode gerar muitos logs
   - Sistema implementa log throttling (intervalo de 10s para NOP)
   - Use comando `lognop <segundos>` para ajustar intervalo

3. **Compatibilidade T2a/T2b**: Sistema suporta ambos modos
   - T2a: `USE_VIRTUAL_MEMORY = False` (memória 1024 palavras)
   - T2b: `USE_VIRTUAL_MEMORY = True` (memória 512 palavras)
   - **Atualmente configurado**: T2b (Memória Virtual ativada)

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
| `nop` | Loop infinito (mantém sistema ativo) | 1 |

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

**Resultado esperado**:
```
T2b: Alocado frame 0 para página 0, 0 páginas NEVER_LOADED
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
[Sistema] Sistema iniciado em modo T2b (Memória Virtual)!
[SYSCALL] STOP
[FINALIZAÇÃO] Processo 0 FINALIZOU

Lista mostrará: Nenhum processo no sistema (processo terminou)
```

**O que verifica**:
- Lazy loading (apenas página 0 carregada)
- Execução completa até STOP
- Desalocação de memória ao terminar

---

### Teste 2: Múltiplos Processos com Escalonamento (T2a/T2b)

**Como executar**:
```
new nop
new fatorial
new progMinimo
start
(aguardar ~3 segundos)
ps
stats
exit
```

**Resultado esperado**:
```
3 processos criados
Sistema inicia escalonamento Round-Robin
- nop: permanece READY/RUNNING (loop infinito)
- fatorial: executa e termina (FINISHED)
- progMinimo: executa e termina (FINISHED)

ps mostrará:
- Processo 0 (nop): READY ou RUNNING
- Processos 1 e 2 não aparecem (terminaram)

stats mostrará:
- Processos ativos: 1
- Total criados: 3
```

**O que verifica**:
- Criação de múltiplos processos
- Round-Robin funcionando
- Estados de processo (READY/RUNNING/FINISHED)
- NOP mantém sistema ativo

---

### Teste 3: Sistema Operante Todo Tempo (T2a Requisito)

**Como executar**:
```
new nop
start
(sistema agora está rodando)
new fatorial
(aguardar ~2 segundos)
new fibonacci10
(aguardar ~2 segundos)
ps
exit
```

**Resultado esperado**:
```
Sistema aceita novos processos após start
- nop inicia executando
- fatorial é adicionado dinamicamente e executa
- fibonacci10 é adicionado e executa

ps mostrará processos ativos:
- nop continua rodando
- fatorial e fibonacci10 podem estar executando ou terminados
```

**O que verifica**:
- Shell aceita comandos enquanto sistema executa
- Processos criados dinamicamente entram na fila
- Sistema permanece responsivo

---

### Teste 4: I/O Assíncrono com Bloqueio (T2a)

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

### Teste 5: Memória Virtual - Lazy Loading (T2b)

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

### Teste 6: Page Fault e Carregamento Sob Demanda (T2b)

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

### Teste 7: Vitimização FIFO (T2b com Memória Limitada)

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

### Teste 8: Dump de Processo (T2b)

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

### Teste 9: Memstat - Status de Memória (T2b)

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

### Teste 10: Programa com Entrada de Usuário (T2a/T2b)

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
| 4 | I/O assíncrono | ✅ Passa |
| 5 | Lazy loading | ✅ Passa |
| 6 | Page fault | ✅ Passa |
| 7 | Vitimização FIFO | ✅ Passa |
| 8 | Dump de processo | ✅ Passa |
| 9 | Status de memória | ✅ Passa |
| 10 | I/O com usuário | ✅ Passa |

**Todos os testes passam com sucesso**.

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
