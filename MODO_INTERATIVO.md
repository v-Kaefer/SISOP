# Modo Interativo do Sistema SISOP

Este documento descreve o sistema de comandos interativo implementado no SISOP.

## Visão Geral

O sistema agora opera em modo contínuo, aguardando comandos do usuário até que seja explicitamente encerrado. Suporta dois modos de operação:

1. **Modo Legacy**: Comandos `load` e `exec` por nome de programa (compatibilidade)
2. **Modo Process Manager**: Comandos `new`, `ps`, `rm`, `exec` por ID (conforme especificação)

## Comandos Disponíveis

### Gerenciamento de Programas (Legacy)

- **`list`** - Lista todos os programas disponíveis
- **`load <programa>`** - Carrega um programa na memória (modo compatibilidade)
  - Exemplo: `load fatorialV2`

### Gerenciamento de Processos

- **`new <programa>`** - Cria um processo com ID único
  - Exemplo: `new fatorialV2`
  - Retorna um ID único para o processo
  
- **`rm <id>`** - Remove processo por ID
  - Exemplo: `rm 1`
  
- **`ps`** - Lista todos os processos no sistema
  - Mostra: ID, Programa, Status

### Execução

- **`exec <programa|id>`** - Executa programa ou processo
  - Por nome (legacy): `exec fatorialV2`
  - Por ID (process manager): `exec 1`
  - Se o parâmetro for numérico, trata como ID
  - Se não for numérico, trata como nome de programa

- **`execAll`** - Executa todos os programas carregados com escalonamento

### Dump e Debug

- **`dump <id>`** - Dump completo do processo
  - Exemplo: `dump 1`
  - Mostra: Nome, Status, Tamanho, Conteúdo do programa

- **`dumpM <inicio> <fim>`** - Dump da memória entre posições
  - Exemplo: `dumpM 0 20`

- **`traceOn`** - Liga modo trace
  - Cada instrução executada será exibida

- **`traceOff`** - Desliga modo trace

### Sistema

- **`help`** - Mostra lista de comandos disponíveis
- **`quit` ou `exit`** - Encerra o sistema

## Exemplos de Uso

### Modo Process Manager (Novo)

```
SISOP> new progMinimo
>>> Processo criado com ID: 1 <<<

SISOP> new fatorialV2
>>> Processo criado com ID: 2 <<<

SISOP> ps
>>> Processos no sistema:
ID	Programa		Status
----------------------------------------
1	progMinimo		Criado
2	fatorialV2		Criado

SISOP> exec 1
>>> Executando processo ID 1: progMinimo <<<
[executa programa]

SISOP> dump 1
>>> Dump do Processo ID 1 <<<
[mostra informações completas]

SISOP> rm 2
>>> Processo 2 (fatorialV2) removido <<<

SISOP> quit
```

### Modo Legacy (Compatibilidade)

```
SISOP> load progMinimo
>>> Carregando programa: progMinimo <<<

SISOP> exec progMinimo
>>> Executando programa: progMinimo <<<
[executa programa]

SISOP> quit
```

### Modo Debug com Trace

```
SISOP> traceOn
>>> Modo TRACE ativado <<<

SISOP> new progMinimo
>>> Processo criado com ID: 1 <<<

SISOP> exec 1
>>> Executando processo ID 1: progMinimo <<<
>>> Modo TRACE ativado <<<
[mostra cada instrução executada]

SISOP> traceOff
>>> Modo TRACE desativado <<<
```

## Testes Automatizados

### Executar Testes

```bash
javac TesteSistemaInterativo.java
java TesteSistemaInterativo
```

### Resultados Esperados

Todos os 9 testes devem passar:
- ✅ Inicialização do Sistema
- ✅ Executar FatorialV2
- ✅ Executar Fibonacci10
- ✅ Executar ProgMinimo
- ✅ Executar Programa por Nome (fatorial)
- ✅ Listar Programas Disponíveis
- ✅ Programa Não Encontrado (tratamento de erro)
- ✅ Stop do Sistema
- ✅ Múltiplas Execuções Sequenciais

## Notas de Implementação

### Compatibilidade

- ✅ Comandos legacy (`load`, `exec <programa>`) mantidos
- ✅ Novos comandos process manager implementados
- ✅ Sistema detecta automaticamente se `exec` recebe ID ou nome
- ✅ Ambos os modos podem ser usados na mesma sessão

### Requisitos Atendidos

Conforme **Enunciado_do_Trabalho_Gerente_de_Processos.md**:
- ✅ `new <programa>` - cria processo com ID único
- ✅ `rm <id>` - remove processo por ID
- ✅ `ps` - lista todos processos
- ✅ `dump <id>` - dump do processo
- ✅ `dumpM <inicio, fim>` - dump da memória
- ✅ `exec <id>` - executa por ID
- ✅ `traceOn/traceOff` - modo trace
- ✅ `exit` - sai do sistema

Conforme **Enunciado_do_Trabalho_Escalonamento.md**:
- ✅ `load` - carrega programas
- ✅ `execAll` - executa todos com escalonamento

## Mensagens de Log Preservadas

Todas as mensagens de log originais são mantidas:
- `">>> Executando programa: X <<<"`
- `"---------------------------------- programa carregado na memoria"`
- `"---------------------------------- inicia execucao"`
- Dumps de registradores e memória
- `">>> Execução finalizada <<<"`
