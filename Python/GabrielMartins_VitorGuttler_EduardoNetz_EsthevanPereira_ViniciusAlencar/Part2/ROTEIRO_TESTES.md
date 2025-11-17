# Roteiro de Testes - Sistema Operacional Python (com Memória Virtual T2b)

## Configuração dos Testes

**Memória Virtual**: ✅ ATIVADA (T2b)
- `USE_VIRTUAL_MEMORY = True`
- Memória: 512 palavras (8 frames × 64 palavras)
- Tamanho de página: 16 palavras
- Quantum: 5 instruções

## Bugs Corrigidos (Atualização 2025-11-17)

### Bug #3: GerenteMemoria.desaloca com dict em T2b
**Sintoma**: `TypeError: '<=' not supported between instances of 'int' and 'dict'`

**Causa**: Método `desaloca()` tentava comparar dict diretamente ao iterar sobre tabela de páginas T2b

**Correção**:
```python
for entry in tabela_paginas:
    if isinstance(entry, dict):
        if entry.get('state') == 'IN_MEMORY':
            frame = entry['frame']
        else:
            continue
    else:
        frame = entry
```

### Bug #4: dump_processo com dict em T2b
**Sintoma**: `TypeError` ao tentar multiplicar dict por int no dump

**Causa**: dump_processo tratava frame como int sem verificar se era dict

**Correção**: Adicionada verificação `isinstance(entry, dict)` para extrair frame corretamente

---

## Testes Executados

### Teste 1: Execução Básica de Processo (T2b)

**Sequência**:
```
new fatorial
start
ps
stats
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Processo 0 (fatorial) criado - Página 0 no frame 0 (lazy loading)
- ✅ Sistema iniciado em modo T2b (Memória Virtual)
- ✅ Thread Disk Device iniciada (gerenciamento de paginação)
- ✅ ps mostra processo com estrutura de página T2b:
  - `[{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'fatorial'}]`
- ✅ Processo executa até STOP e termina

**Por que funcionou**: 
- Memória virtual permite lazy loading (apenas primeira página carregada)
- Sistema gerencia page table com dicionários indicando estado da página
- Disk device thread disponível para page faults

**Observações T2b**:
- Programas carregados no disco antes de iniciar sistema
- Apenas primeira página de cada processo carregada na memória
- Demais páginas marcadas como NEVER_LOADED

---

### Teste 2: Múltiplos Processos com Escalonamento (T2a/T2b)

**Sequência**:
```
new fatorial
new fibonacci10
start
new progMinimo
ps
stats
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Processo 0 (fatorial) criado - Frame 0
- ✅ Processo 1 (fibonacci10) criado - Frame 1
- ✅ Sistema iniciado em modo T2b
- ✅ Processo 2 (progMinimo) adicionado após start - Frame 2
- ✅ ps mostra processos ativos
- ✅ Escalonamento Round-Robin funciona corretamente

**Por que funcionou**: Sistema permite adicionar múltiplos processos em qualquer ordem com memória virtual.

---

### Teste 3: Múltiplos new > start

**Sequência**:
```
new fatorial
new fibonacci10
start
ps
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Processo 0 (fatorial) criado - 1 página (frame 0)
- ✅ Processo 1 (fibonacci10) criado - Primeira página em frame 1
  - fibonacci10 tem múltiplas páginas, mas apenas página 0 carregada
  - Demais páginas serão carregadas sob demanda (page fault)
- ✅ Sistema iniciado
- ✅ ps mostra ambos processos

**Por que funcionou**: Lazy loading T2b permite criar processos grandes alocando apenas primeira página.

**Observação importante**: Se fibonacci10 acessar páginas além da primeira durante execução, ocorrerá page fault e Disk Device carregará a página necessária.

---

### Teste 4: start > new fatorial (fila vazia)

**Sequência**:
```
start
new fatorial
ps
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Sistema iniciado com fila vazia
- ✅ CPU thread, I/O Device e Disk Device iniciados
- ✅ Processo 0 (fatorial) adicionado depois - Frame 0
- ✅ ps mostra processo criado em READY
- ✅ Sistema funcionou conforme esperado

**Por que funcionou**: CPU thread aguarda processos mesmo quando fila está vazia (T2b mantém mesma sincronização do T2a).

---

### Teste 5: Execução Completa com Finalização (Bug Fix Verification)

**Sequência**:
```
new fatorial
start
(aguardar execução completa)
ps
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Processo 0 (fatorial) criado
- ✅ Sistema iniciado
- ✅ Fatorial executa até SYSCALL STOP
- ✅ Processo finaliza com mensagem "[FINALIZAÇÃO] Processo 0 FINALIZOU"
- ✅ **SEM EXCEÇÕES** (bug #3 corrigido)
- ✅ Memória desalocada corretamente
- ✅ ps mostra "Nenhum processo no sistema"

**Por que funcionou**: Correção do método `desaloca()` para tratar dicts corretamente em T2b.

---

### Teste 6: Comando dump com T2b (Bug Fix Verification)

**Sequência**:
```
new fatorial
dump 0
start
exit
```

**Resultado (T2b - Memória Virtual)**:
- ✅ Processo criado
- ✅ dump 0 mostra informações corretas:
  ```
  Estado: READY
  Tabela de Páginas: [{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'fatorial'}]
  Página 0: Lógico 0-15 → Frame 0 → Físico 0-15 (Estado: IN_MEMORY)
  ```
- ✅ **SEM EXCEÇÕES** (bug #4 corrigido)
- ✅ Conteúdo da memória exibido corretamente

**Por que funcionou**: Correção do método `dump_processo()` para extrair frame de dicts em T2b.

---

## Diferenças T2a vs T2b Observadas nos Testes

### T2a (Memória Completa - 1024 palavras)
```
[CRIAÇÃO] Processo 0 criado (Frames: [0, 1, 2], Estado: READY)
```
- Todo programa carregado em múltiplos frames
- Tabela de páginas com lista de inteiros: `[0, 1, 2]`

### T2b (Memória Virtual - 512 palavras)
```
[CRIAÇÃO T2b] Processo 0 criado (Página 0 no frame 0, demais NEVER_LOADED)
```
- Apenas primeira página carregada (lazy loading)
- Tabela de páginas com lista de dicionários:
  ```python
  [{'state': 'IN_MEMORY', 'frame': 0, 'disk_location': 'programa'}]
  ```
- Disk Device thread ativa para carregar páginas sob demanda
- Programas pré-carregados no disco virtual

---

## Funcionalidades T2b Verificadas

### ✅ Lazy Loading
- Apenas primeira página carregada ao criar processo
- Reduz uso de memória inicial
- Permite criar mais processos simultaneamente

### ✅ Disk Device Thread
- Thread separada para gerenciar paginação
- Mensagens: `[DISK] Programa 'X' carregado no disco`
- Pronto para processar page faults

### ✅ Estados de Página
- `IN_MEMORY`: Página está em frame da memória
- `NEVER_LOADED`: Página nunca foi carregada (lazy)
- `SWAPPED`: Página foi vitimada para disco (não testado aqui)

### ✅ Estrutura de Page Table Estendida
- Cada entrada contém:
  - `state`: Estado da página
  - `frame`: Frame físico (se IN_MEMORY)
  - `disk_location`: Localização no disco

### ✅ Desalocação de Memória (T2b)
- Sistema corretamente libera frames ao finalizar processos
- Trata estruturas dict e int corretamente
- Ignora páginas não carregadas em memória

### ✅ Comando dump (T2b)
- Exibe corretamente informações de processos T2b
- Mostra estado de cada página
- Lista apenas páginas carregadas em memória

### ⏳ Page Fault (não ocorreu nos testes)
- Testes rápidos não acessaram páginas além da primeira
- Para testar: executar fibonacci10 ou PC por tempo suficiente
- Esperado: Interrupção INT_PAGE_FAULT, carregamento assíncrono

### ⏳ Vitimização (não ocorreu nos testes)
- Memória de 512 palavras (8 frames) não foi totalmente preenchida
- Para testar: criar muitos processos grandes
- Esperado: Política FIFO escolhe vítima, salva em disco, libera frame

---

## Bugs Corrigidos Durante Testes

### Bug #1: PCB.id com dict em T2b
**Sintoma**: `TypeError: '<' not supported between instances of 'dict' and 'dict'`

**Causa**: `self.id = page_table[0]` atribuía dict inteiro como ID

**Correção**:
```python
if isinstance(page_table[0], dict):
    self.id = page_table[0]['frame']
else:
    self.id = page_table[0]
```

### Bug #2: list_all_processes com dict em T2b
**Sintoma**: `TypeError: unsupported operand type(s) for *: 'dict' and 'int'`

**Causa**: Tentativa de multiplicar dict por inteiro no cálculo de endereço

**Correção**:
```python
if isinstance(pcb.page_table[0], dict):
    primeiro_frame = pcb.page_table[0]['frame']
else:
    primeiro_frame = pcb.page_table[0]
```

### Bug #3: GerenteMemoria.desaloca com dict em T2b ⭐ NOVO
**Sintoma**: `TypeError: '<=' not supported between instances of 'int' and 'dict'`

**Causa**: Método iterava sobre tabela de páginas assumindo valores int

**Correção**:
```python
for entry in tabela_paginas:
    if isinstance(entry, dict):
        if entry.get('state') == 'IN_MEMORY':
            frame = entry['frame']
        else:
            continue
    else:
        frame = entry
```

### Bug #4: dump_processo com dict em T2b ⭐ NOVO
**Sintoma**: `TypeError` ao multiplicar dict por int

**Causa**: Não extraía frame do dict antes de fazer cálculos

**Correção**: Verificação `isinstance(entry, dict)` adicionada em 2 locais

---

## Plano de Correção

✅ **Todos bugs corrigidos** - Sistema funcionando completamente com memória virtual

**Resumo das correções**:
1. ✅ PCB.__init__ - Trata dict vs int
2. ✅ list_all_processes - Extrai frame de dict
3. ✅ GerenteMemoria.desaloca - Trata dict na desalocação
4. ✅ dump_processo - Extrai frame de dict para dump

---

## Próximos Testes Sugeridos (Não Executados)

### Teste 7: Forçar Page Fault
```
new fibonacci10
start
(aguardar execução completa para acessar segunda página)
```
**Esperado**: Page fault ao acessar página 1, carregamento assíncrono, processo bloqueia temporariamente

### Teste 8: Forçar Vitimização
```
(reduzir memória para 256 palavras - 4 frames)
new PC
new fibonacci10
new fatorial
new progMinimo
start
```
**Esperado**: Memória cheia, vitimização FIFO, salvamento em disco, page faults múltiplos

---

## Conclusão

**Sistema T2b funciona completamente** para os cenários testados:
- ✅ Lazy loading implementado
- ✅ Disk Device operacional
- ✅ Estrutura de página estendida funcionando
- ✅ Compatibilidade com comandos shell mantida
- ✅ **4 bugs de incompatibilidade dict/int corrigidos**
- ✅ Processos finalizam corretamente
- ✅ Memória desalocada sem erros
- ✅ Comando dump funciona em T2b

**Todos os 6 testes passaram com memória virtual ativada.**

---

**Data**: 2025-11-17  
**Versão**: T2b (Memória Virtual Ativada)  
**Configuração**: `USE_VIRTUAL_MEMORY = True`  
**Última Atualização**: Bugs #3 e #4 corrigidos
