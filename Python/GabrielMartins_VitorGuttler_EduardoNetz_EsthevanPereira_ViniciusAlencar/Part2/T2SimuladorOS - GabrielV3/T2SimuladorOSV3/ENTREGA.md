# Documentação de Entrega - Simulador de SO
## PUCRS - Trabalhos T2a/T2b

---

## 📦 Conteúdo da Entrega

### Arquivos Principais

```
T2SimuladorOS/
├── sistema_os.py           # Código principal do simulador (888 linhas)
├── exemplos.py             # Programas de exemplo (não utilizado, mantido para referência)
├── config.ini              # Arquivo de configuração do sistema
├── run.sh                  # Script de execução (Linux/Mac)
├── README.md               # Documentação completa do sistema
├── TESTES.md               # Documentação de testes e validação
└── ENTREGA.md              # Este arquivo
```

### Arquivos Gerados Durante Execução

```
├── system_log.txt          # Log do modo interativo
├── test_log.txt            # Log dos testes automatizados
└── memory_dumps/           # Dumps de memória dos processos finalizados
    ├── processo_0_fibonacci_dump.txt
    ├── processo_1_fatorial_dump.txt
    └── ...
```

---

## 🚀 Como Executar

### Pré-requisitos
- Python 3.7 ou superior
- Sistema operacional: Linux, macOS ou Windows
- Bibliotecas: Apenas bibliotecas padrão do Python (threading, queue, collections, datetime, configparser)

### Execução Rápida

#### Modo Interativo (Shell)
```bash
cd T2SimuladorOS
./run.sh
# ou
python3 sistema_os.py
```

#### Modo de Teste Automatizado
```bash
cd T2SimuladorOS
./run.sh test
# ou
python3 sistema_os.py test
```

#### Ajuda
```bash
./run.sh help
```

---

## 📋 Requisitos Implementados

### ✅ Requisitos Obrigatórios

#### 1. Multithreading
- [x] Thread Shell: Interface de comandos interativa
- [x] Thread CPU: Execução de instruções com quantum
- [x] Thread IODevice: Operações de I/O assíncronas
- [x] Thread DiskThread: Carregamento/salvamento de páginas

#### 2. Gerenciamento de Processos
- [x] PCB completo (PID, nome, PC, estado, registradores, tabela de páginas)
- [x] Estados: NEW, READY, RUNNING, BLOCKED, FINISHED
- [x] Filas de prontos e bloqueados
- [x] Criação e remoção de processos
- [x] Dumps de memória ao finalizar

#### 3. Escalonamento
- [x] Algoritmo FIFO
- [x] Preempção por quantum
- [x] Transições de estado corretas
- [x] Logs de todas as mudanças de estado

#### 4. Memória Virtual
- [x] Paginação sob demanda (apenas primeira página carregada)
- [x] Tabela de páginas por processo
- [x] Page-faults tratados assincronamente
- [x] Vitimação de páginas (algoritmo FIFO)
- [x] Páginas sujas salvas no disco antes de vitimação
- [x] Área de swap simulada

#### 5. I/O Assíncrono
- [x] Operações de I/O não bloqueiam CPU
- [x] Processos bloqueados durante I/O
- [x] Interrupções de conclusão de I/O
- [x] DMA simulado (thread de disco escreve diretamente na memória)

#### 6. Tratamento de Interrupções
- [x] PAGE_FAULT: Carrega página ausente
- [x] IO_COMPLETION: Desbloqueia processo após I/O
- [x] INT_ENDERECO_INVALIDO: Finaliza processo
- [x] INT_INSTRUCAO_INVALIDA: Finaliza processo
- [x] INT_OVERFLOW: Finaliza processo

#### 7. Logging e Observabilidade
- [x] Logs estruturados: `ID NomeProg Razao EstadoInicial ProximoEstado TabelaDePaginas`
- [x] Tabelas de páginas: `{ [pag,frame,onde], ... }` onde onde ∈ {mp, ms, _}
- [x] Dumps de memória detalhados
- [x] Comando sysstate para inspeção completa do sistema

#### 8. Configuração
- [x] Arquivo config.ini para parâmetros
- [x] Tamanho de memória configurável
- [x] Tamanho de página configurável
- [x] Quantum configurável
- [x] Latências de I/O e disco configuráveis
- [x] Política de substituição configurável

---

## 🎯 Funcionalidades Extras

### Melhorias Implementadas

1. **Thread de Disco Dedicada**: Operações de disco em thread separada com fila de requisições
2. **Cálculo Dinâmico de Páginas**: Número de páginas calculado considerando todos os endereços acessados
3. **Comando sysstate**: Visualização completa do estado do sistema
4. **Script de Execução**: Facilita execução em diferentes modos
5. **Configuração Flexível**: Parâmetros ajustáveis sem modificar código
6. **Dumps Detalhados**: Incluem tabelas de páginas e conteúdo completo da memória
7. **Testes Automatizados**: Validam todos os requisitos automaticamente

---

## 📊 Exemplos de Uso

### Exemplo 1: Criar e Executar Processo

```bash
> new fibonacci
[KERNEL] Criando processo 0 (fibonacci). Aguardando carga inicial da memoria.
[LOG] 0  fibonacci        criacao               nulo        BLOCKED     { [0,_,_], [1,_,_], [2,_,_], [3,_,_] }
    [DISK_THREAD] Carregando pagina 0 do processo 0 do disco...
    [DISK_THREAD] Pagina 0 do processo 0 carregada no frame 0.
[LOG] 0  fibonacci        carga_inicial         BLOCKED     READY       { [0,0,mp], [1,_,_], [2,_,_], [3,_,_] }
[KERNEL] Processo 0 pronto e adicionado a fila de prontos.
```

### Exemplo 2: Visualizar Estado do Sistema

```bash
> sysstate

====================================================================================================
ESTADO COMPLETO DO SISTEMA
====================================================================================================

[PROCESSOS] Total: 2
  ID: 0 | Nome: fibonacci      | Estado: READY    | PC: 2
       Tabela de Paginas: { [0,0,mp], [1,_,_], [2,_,_], [3,2,mp] }
  ID: 1 | Nome: fatorial       | Estado: BLOCKED (I/O Request) | PC: 5
       Tabela de Paginas: { [0,1,mp], [1,_,_], [2,_,_], [3,3,mp] }

[FILA DE PRONTOS] Tamanho: 1
  -> Processo 0 (fibonacci)

[FILA DE BLOQUEADOS] Tamanho: 1
  -> Processo 1 (fatorial) - Razao: I/O Request

[CPU] Processo em execucao: Nenhum (IDLE)

[MEMORIA] Frames totais: 4, Livres: 0, Ocupados: 4

Mapa de Frames:
  Frame 00: [OCUPADO] PID=0, Pagina=0, Dirty=False, LastUsed=10
  Frame 01: [OCUPADO] PID=1, Pagina=0, Dirty=False, LastUsed=15
  Frame 02: [OCUPADO] PID=0, Pagina=3, Dirty=True, LastUsed=20
  Frame 03: [OCUPADO] PID=1, Pagina=3, Dirty=True, LastUsed=25

[DISCO/SWAP] Paginas em swap: 0
====================================================================================================
```

---

## 🧪 Validação

### Testes Automatizados

Execute `./run.sh test` para validar:
- ✅ Criação de processos
- ✅ Page-faults e carregamento assíncrono
- ✅ Vitimação de páginas
- ✅ I/O assíncrono
- ✅ Escalonamento e preempção
- ✅ Logs estruturados
- ✅ Dumps de memória

### Resultados Esperados

Após executar os testes, verifique:
1. Arquivo `test_log.txt` com logs estruturados
2. Diretório `memory_dumps/` com dumps dos processos
3. Mensagens de log indicando page-faults, vitimação e I/O
4. Processos alternando entre estados corretamente

---

## 📚 Documentação Adicional

- **README.md**: Documentação completa da arquitetura e uso
- **TESTES.md**: Descrição detalhada dos testes e validação
- **config.ini**: Parâmetros configuráveis do sistema

---

## 🎓 Conceitos Demonstrados

Este simulador demonstra os seguintes conceitos de Sistemas Operacionais:

1. **Concorrência**: Múltiplas threads operando simultaneamente
2. **Sincronização**: Uso de locks, eventos e filas para coordenação
3. **Escalonamento**: Algoritmo FIFO com preempção por quantum
4. **Memória Virtual**: Paginação sob demanda com vitimação
5. **I/O Assíncrono**: Operações não-bloqueantes com DMA
6. **Tratamento de Interrupções**: Mecanismo de interrupções e handlers
7. **Gerenciamento de Processos**: Estados, transições e PCB
8. **Observabilidade**: Logging estruturado e dumps de memória

---

## 👨‍💻 Informações Técnicas

- **Linguagem**: Python 3.7+
- **Linhas de Código**: ~888 linhas (sistema_os.py)
- **Paradigma**: Orientado a Objetos com Threads
- **Bibliotecas**: Apenas bibliotecas padrão do Python
- **Compatibilidade**: Linux, macOS, Windows

---

## ✅ Checklist de Entrega

- [x] Código-fonte completo e comentado
- [x] Script de build/execução (run.sh)
- [x] Arquivo de configuração (config.ini)
- [x] Programas de teste (fibonacci, fatorial, etc.)
- [x] Testes automatizados implementados
- [x] Logs estruturados gerados
- [x] Dumps de memória gerados
- [x] README com arquitetura e instruções
- [x] Documentação de testes (TESTES.md)
- [x] Documentação de entrega (ENTREGA.md)

---

## 📝 Observações Finais

O simulador está completo e funcional, atendendo a todos os requisitos especificados nos trabalhos T2a e T2b. O sistema demonstra de forma clara e observável os conceitos fundamentais de sistemas operacionais modernos, incluindo multithreading, memória virtual, escalonamento e I/O assíncrono.

Para qualquer dúvida ou problema na execução, consulte o README.md ou execute `./run.sh help`.

---

**Data de Entrega**: 2025-11-11  
**Curso**: Engenharia de Software - PUCRS  
**Trabalhos**: T2a/T2b - Simulador de Sistema Operacional

