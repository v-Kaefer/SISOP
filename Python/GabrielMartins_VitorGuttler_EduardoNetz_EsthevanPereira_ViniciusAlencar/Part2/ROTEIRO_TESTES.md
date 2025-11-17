# Roteiro de Testes - Sistema Operacional Python

## Testes Executados

### Teste 1: nop > start > new fatorial

**Sequência**:
```
new nop
start
new fatorial
ps
stats
exit
```

**Resultado**:
- ✅ Processo 0 (nop) criado com sucesso
- ✅ Sistema iniciado em modo T2a
- ✅ Processo 1 (fatorial) criado após start
- ✅ ps mostra 2 processos, ambos em READY
- ✅ Sistema funcionou conforme esperado

**Por que funcionou**: O sistema permite adicionar processos após o start. Ambos ficam na fila READY aguardando escalonamento.

---

### Teste 2: new fatorial > start > nop > new fatorial  

**Sequência**:
```
new fatorial
start
new nop
new fatorial
ps
stats
exit
```

**Resultado**:
- ✅ Processo 0 (fatorial) criado antes do start
- ✅ Sistema iniciado
- ✅ Processo 1 (nop) adicionado após start
- ✅ Processo 2 (fatorial) adicionado após start
- ✅ ps mostra 3 processos ativos
- ✅ Sistema funcionou conforme esperado

**Por que funcionou**: Sistema permite adicionar múltiplos processos em qualquer ordem.

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

**Resultado**:
- ✅ Processo 0 (fatorial) criado - usa 1 frame [0]
- ✅ Processo 1 (fibonacci10) criado - usa 2 frames [1, 2]
- ✅ Sistema iniciado
- ✅ ps mostra ambos processos em READY
- ✅ Sistema funcionou conforme esperado

**Por que funcionou**: Sistema aloca frames automaticamente para cada processo.

---

### Teste 4: start > new fatorial (fila vazia)

**Sequência**:
```
start
new fatorial
ps
exit
```

**Resultado**:
- ✅ Sistema iniciado com fila vazia
- ✅ Processo 0 (fatorial) adicionado depois
- ✅ ps mostra processo criado em READY
- ✅ Sistema funcionou conforme esperado

**Por que funcionou**: CPU thread aguarda processos mesmo quando fila está vazia. Adicionar processo após start funciona.

---

## Plano de Correção

Não há correções necessárias. Todos os testes passaram conforme esperado.

**Conclusão**: O sistema permite criar processos antes ou depois do start, em qualquer ordem. A fila de prontos gerencia corretamente os processos independente da sequência de comandos.
