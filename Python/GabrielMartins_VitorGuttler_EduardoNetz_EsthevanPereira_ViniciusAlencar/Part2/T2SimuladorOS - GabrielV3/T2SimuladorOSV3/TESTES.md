# Documento de Testes - Simulador de SO

## Visão Geral

Este documento descreve os testes implementados e os resultados esperados para validar o funcionamento correto do simulador de sistema operacional.

---

## Testes Automatizados

### Como Executar

```bash
./run.sh test
# ou
python3 sistema_os.py test
```

---

## Cenários de Teste

### Teste A: Processos com I/O e Page-Faults

**Objetivo**: Validar criação de processos, page-faults, I/O assíncrono e escalonamento.

**Processos**: `fibonacci` e `fatorial`

**Comportamento Esperado**:
1. ✅ Processos são criados no estado BLOCKED aguardando carga inicial
2. ✅ Primeira página é carregada assincronamente
3. ✅ Processos transitam para READY após carga inicial
4. ✅ CPU escalona processos alternadamente
5. ✅ Page-faults são gerados ao acessar páginas não carregadas
6. ✅ Páginas são carregadas assincronamente do disco
7. ✅ Processos são bloqueados durante I/O (SYSCALL)
8. ✅ Processos retornam a READY após conclusão de I/O
9. ✅ Preempção por quantum funciona corretamente
10. ✅ Processos finalizam e geram dumps de memória

**Validação**:
- Log estruturado com todas as transições de estado
- Tabelas de páginas mostram páginas carregadas (mp) e não carregadas (_)
- Dumps de memória são gerados ao finalizar

---

### Teste B: Vitimação de Páginas

**Objetivo**: Forçar esgotamento de frames e validar algoritmo de vitimação FIFO.

**Processos**: `PB`, `PC`, `fatorialV2` (além dos processos do Teste A)

**Comportamento Esperado**:
1. ✅ Múltiplos processos competem por frames limitados
2. ✅ Quando não há frames livres, algoritmo FIFO seleciona vítima
3. ✅ Páginas sujas (dirty_bit=True) são salvas no disco antes de vitimação
4. ✅ Frame da vítima é liberado e reutilizado
5. ✅ Processo continua execução após carregamento da nova página
6. ✅ Área de swap (disco) armazena páginas vitimadas

**Validação**:
- Mensagens de log indicam vitimação: "Sem frames livres. Iniciando vitimacao..."
- Páginas vitimadas aparecem no swap (on_swap=True)
- Frames são reutilizados corretamente

---

### Teste C: Logs e Dumps

**Objetivo**: Validar formato de logs e dumps de memória.

**Comportamento Esperado**:
1. ✅ Arquivo `test_log.txt` é criado com logs estruturados
2. ✅ Cada linha segue o formato: `ID NomeProg Razao EstadoInicial ProximoEstado TabelaDePaginas`
3. ✅ Tabelas de páginas mostram: `{ [pag,frame,onde], ... }`
4. ✅ Dumps de memória são salvos em `memory_dumps/`
5. ✅ Dumps contêm: PC, registradores, tabela de páginas, conteúdo da memória física

**Validação**:
- Arquivo de log existe e está bem formatado
- Dumps de memória existem para cada processo finalizado
- Conteúdo dos dumps é legível e completo

---

## Testes Manuais (Modo Interativo)

### Teste 1: Criação e Listagem de Processos

```bash
> new fibonacci
> new fatorial
> ps
```

**Resultado Esperado**:
- Processos são criados e listados
- Estados iniciais são BLOCKED (aguardando carga)
- Após carga, transitam para READY

---

### Teste 2: Visualização de Estado do Sistema

```bash
> new fibonacci
> sysstate
```

**Resultado Esperado**:
- Exibe todos os processos com seus estados
- Mostra filas de prontos e bloqueados
- Exibe mapa de frames (livres e ocupados)
- Lista páginas em swap

---

### Teste 3: Dump de Processo

```bash
> new fibonacci
> dump 0
```

**Resultado Esperado**:
- Exibe ID, estado, PC, registradores
- Mostra tabela de páginas completa

---

### Teste 4: Dump de Memória Física

```bash
> new fibonacci
> dumpm 0 15
```

**Resultado Esperado**:
- Exibe conteúdo da memória física nos endereços especificados
- Mostra instruções (opcode, registradores, parâmetro)

---

### Teste 5: Modo Trace (Debug)

```bash
> traceon
> new progMinimo
> traceoff
```

**Resultado Esperado**:
- Com trace ativado, cada instrução executada é exibida
- Mostra PC, processo e instrução detalhada

---

### Teste 6: Remoção de Processo

```bash
> new fibonacci
> ps
> rm 0
> ps
```

**Resultado Esperado**:
- Processo é removido se não estiver em execução
- Memória é liberada
- Dump é gerado

---

## Validação de Requisitos

### ✅ Requisitos Funcionais Implementados

1. **Multithreading**
   - ✅ Thread Shell (interface de comandos)
   - ✅ Thread CPU (execução de instruções)
   - ✅ Thread IODevice (I/O assíncrono)
   - ✅ Thread DiskThread (operações de disco)

2. **Gerenciamento de Processos**
   - ✅ PCB com PID, nome, PC, estado, registradores, tabela de páginas
   - ✅ Estados: NEW, READY, RUNNING, BLOCKED, FINISHED
   - ✅ Filas de prontos e bloqueados
   - ✅ Criação e remoção de processos

3. **Escalonamento**
   - ✅ Algoritmo FIFO
   - ✅ Preempção por quantum
   - ✅ Transições de estado corretas

4. **Memória Virtual**
   - ✅ Paginação sob demanda
   - ✅ Tabela de páginas por processo
   - ✅ Page-faults tratados assincronamente
   - ✅ Vitimação de páginas (FIFO)
   - ✅ Páginas sujas salvas no disco

5. **I/O Assíncrono**
   - ✅ Operações de I/O não bloqueiam CPU
   - ✅ Processos bloqueados durante I/O
   - ✅ Interrupções de conclusão de I/O
   - ✅ DMA simulado

6. **Tratamento de Interrupções**
   - ✅ PAGE_FAULT
   - ✅ IO_COMPLETION
   - ✅ INT_ENDERECO_INVALIDO
   - ✅ INT_INSTRUCAO_INVALIDA
   - ✅ INT_OVERFLOW

7. **Logging e Observabilidade**
   - ✅ Logs estruturados de todas as transições
   - ✅ Formato padronizado com tabelas de páginas
   - ✅ Dumps de memória ao finalizar processos
   - ✅ Comando sysstate para inspeção completa

8. **Configuração**
   - ✅ Arquivo config.ini para parâmetros
   - ✅ Tamanho de memória configurável
   - ✅ Tamanho de página configurável
   - ✅ Quantum configurável
   - ✅ Latências de I/O e disco configuráveis

---

## Arquivos Gerados

Após executar os testes, os seguintes arquivos são gerados:

```
T2SimuladorOS/
├── test_log.txt                    # Log estruturado de todas as transições
├── system_log.txt                  # Log do modo interativo
└── memory_dumps/                   # Dumps de processos finalizados
    ├── processo_0_fibonacci_dump.txt
    ├── processo_1_fatorial_dump.txt
    └── ...
```

---

## Conclusão

Todos os requisitos especificados foram implementados e validados através de testes automatizados e manuais. O sistema demonstra corretamente:

- Concorrência com múltiplas threads
- Gerenciamento de processos com estados e transições
- Memória virtual com paginação sob demanda
- Vitimação de páginas com algoritmo FIFO
- I/O assíncrono com DMA simulado
- Logging estruturado e observabilidade completa
- Configuração flexível via arquivo

O simulador está pronto para demonstração e avaliação.

