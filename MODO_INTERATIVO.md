# Sistema Operacional SISOP - Modo Comando

## Descrição

O sistema SISOP agora opera em **modo comando contínuo**, permitindo executar múltiplas funções através de comandos de texto. O sistema aguarda comandos do usuário e só encerra quando solicitado explicitamente.

## Como Executar

```bash
javac Sistema.java
java Sistema
```

## Interface de Comandos

O sistema utiliza uma interface baseada em comandos de texto, similar a um shell:

```
SISOP> help
SISOP> list
SISOP> load fatorialV2
SISOP> exec fatorialV2
SISOP> execAll
SISOP> dump 0 10
SISOP> quit
```

### Comandos Disponíveis

- **`list`** - Lista todos os programas disponíveis
- **`load <programa>`** - Carrega um programa na memória
  - Exemplo: `load fatorialV2`
- **`exec <programa>`** - Executa um programa específico
  - Exemplo: `exec fatorialV2`
- **`execAll`** - Executa todos os programas carregados com escalonamento
- **`dump <inicio> <fim>`** - Mostra dump da memória entre posições
  - Exemplo: `dump 0 20`
- **`help`** - Mostra lista de comandos disponíveis
- **`quit` ou `exit`** - Encerra o sistema

## Testes Automatizados

Para testar as funções do sistema programaticamente sem entrada manual:

```bash
javac TesteSistemaInterativo.java
java TesteSistemaInterativo
```

O teste automatizado verifica:
- ✅ Inicialização do sistema
- ✅ Execução de programas individuais (fatorialV2, fibonacci10, progMinimo, fatorial)
- ✅ Listagem de programas disponíveis
- ✅ Tratamento de erros (programa não encontrado)
- ✅ Parada do sistema (método stop())
- ✅ Múltiplas execuções sequenciais

## Programas Disponíveis

O sistema inclui uma biblioteca de programas prontos para execução:
- **fatorial** - Calcula fatorial de 7
- **fatorialV2** - Calcula fatorial de 5 com syscall
- **fibonacci10** - Gera série Fibonacci (10 elementos)
- **fibonacci10v2** - Fibonacci versão 2
- **progMinimo** - Programa mínimo de teste
- **fibonacciREAD** - Fibonacci com entrada
- **PB** - Teste de fatorial com condicionais
- **PC** - Bubble sort (ordenação)

## Exemplo de Uso

```bash
$ java Sistema

╔════════════════════════════════════════════╗
║  Bem-vindo ao Sistema Operacional SISOP   ║
║  Sistema em modo comando                   ║
╚════════════════════════════════════════════╝

Digite 'help' para ver comandos disponíveis.

SISOP> help

=== COMANDOS DISPONÍVEIS ===
list                - Lista programas disponíveis
load <programa>     - Carrega um programa na memória
exec <programa>     - Executa um programa
execAll             - Executa todos programas carregados
dump <inicio> <fim> - Mostra dump da memória
help                - Mostra esta ajuda
quit | exit         - Encerra o sistema
============================

SISOP> list

>>> Programas disponíveis:
  - fatorial      : Calcula fatorial de 7
  - fatorialV2    : Calcula fatorial de 5 com syscall
  - fibonacci10   : Gera série Fibonacci (10 elementos)
  ...

SISOP> load fatorialV2

>>> Carregando programa: fatorialV2 <<<
>>> Programa carregado na memória <<<
>>> Use 'exec fatorialV2' para executar <<<

SISOP> exec fatorialV2

>>> Executando programa: fatorialV2 <<<
---------------------------------- programa carregado na memoria
...
>>> Execução finalizada <<<

SISOP> execAll

>>> Executando todos os programas com escalonamento <<<
>>> Escalonando: progMinimo <<<
...
>>> Todos os programas foram executados <<<

SISOP> dump 0 5

>>> Dump da memória [0 - 5]:
0:  [ LDI, 0, -1, 5  ]
1:  [ STD, 0, -1, 19  ]
...

SISOP> quit

>>> Encerrando sistema... <<<
>>> Sistema encerrado com sucesso <<<
```

## Diferenças em Relação ao Sistema Original

### Sistema Original
- Executava um programa pré-definido e encerrava
- Não havia interação com o usuário
- Necessário recompilar para executar outro programa

### Sistema Novo (Modo Comando)
- ✅ **Execução contínua** - aguarda comandos do usuário
- ✅ **Interface de comandos** - similar a um shell
- ✅ **Múltiplas execuções** - execute quantos programas desejar
- ✅ **Comandos flexíveis** - exec, list, dump, help, quit
- ✅ **Encerramento controlado** - saia quando desejar (quit/exit)
- ✅ **Dump de memória** - visualize o estado da memória
- ✅ **Mensagens de log** - todas as mensagens preservadas

## Tratamento de Erros

O sistema inclui tratamento de erros para:
- Nomes de programas inválidos
- Comandos desconhecidos
- Parâmetros incorretos para dump
- Erros durante a execução dos programas

## Notas Técnicas

### Alterações Realizadas
1. **Sistema.java** - Adicionado menu interativo e loop de execução
2. **Programs.java** - Corrigido bug de comparação de strings (== para .equals())

### Compatibilidade
O sistema mantém total compatibilidade com:
- Todos os programas existentes
- Sistema de memória (Etapa 1)
- Sistema de processos (Etapa 2)
- Exemplos e testes existentes

## Atendimento ao Requisito

Este sistema atende ao requisito do problema:
> "Faça o sistema, como um programa constante, que pode executar as funções que temos modulares. Ou seja, ao invés do programa executar e encerrar, ele deve executar e aguardar chamadas das funções e encerrar on command."

O sistema agora:
- ✅ Opera como programa constante
- ✅ Executa funções modulares (programas disponíveis)
- ✅ Aguarda chamadas de funções (comandos de texto)
- ✅ Encerra apenas sob comando (quit/exit)
- ✅ Comandos inspirados em definição do trabalho (TrabalhoSO-A-DefinicaoHW.pages)
