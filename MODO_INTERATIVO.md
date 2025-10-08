# Sistema Operacional SISOP - Modo Interativo

## Descrição

O sistema SISOP agora opera em **modo interativo contínuo**, permitindo executar múltiplas funções sem encerrar o programa. O sistema aguarda comandos do usuário e só encerra quando solicitado explicitamente.

## Como Executar

```bash
javac Sistema.java
java Sistema
```

## Menu Principal

O sistema apresenta o seguinte menu:

```
===============================================
           SISTEMA OPERACIONAL SISOP          
===============================================
1. Listar programas disponíveis
2. Executar programa
3. Executar fatorialV2 (demonstração)
4. Executar fibonacci10 (demonstração)
5. Executar progMinimo (demonstração)
0. Sair
===============================================
```

## Opções Disponíveis

### 1. Listar programas disponíveis
Exibe todos os programas disponíveis na biblioteca do sistema:
- **fatorial** - Calcula fatorial de 7
- **fatorialV2** - Calcula fatorial de 5 com syscall
- **fibonacci10** - Gera série Fibonacci (10 elementos)
- **fibonacci10v2** - Fibonacci versão 2
- **progMinimo** - Programa mínimo de teste
- **fibonacciREAD** - Fibonacci com entrada
- **PB** - Teste de fatorial com condicionais
- **PC** - Bubble sort (ordenação)

### 2. Executar programa
Permite executar qualquer programa disponível digitando seu nome.

**Exemplo:**
```
Escolha uma opção: 2
Digite o nome do programa para executar:
Nome: fibonacci10
```

### 3. Executar fatorialV2 (demonstração)
Executa diretamente o programa fatorialV2, que calcula 5! = 120.

### 4. Executar fibonacci10 (demonstração)
Executa diretamente o programa fibonacci10, que gera a sequência de Fibonacci.

### 5. Executar progMinimo (demonstração)
Executa o programa mínimo de teste.

### 0. Sair
Encerra o sistema de forma controlada.

## Exemplo de Uso

```bash
$ java Sistema

╔════════════════════════════════════════════╗
║  Bem-vindo ao Sistema Operacional SISOP   ║
║  Sistema em modo interativo                ║
╚════════════════════════════════════════════╝

===============================================
           SISTEMA OPERACIONAL SISOP          
===============================================
1. Listar programas disponíveis
2. Executar programa
3. Executar fatorialV2 (demonstração)
4. Executar fibonacci10 (demonstração)
5. Executar progMinimo (demonstração)
0. Sair
===============================================
Escolha uma opção: 1

=== PROGRAMAS DISPONÍVEIS ===
1. fatorial - Calcula fatorial de 7
2. fatorialV2 - Calcula fatorial de 5 com syscall
...

Escolha uma opção: 3

>>> Executando programa: fatorialV2 <<<
...
>>> Execução finalizada <<<

Escolha uma opção: 0

>>> Encerrando sistema... <<<
Sistema encerrado com sucesso.
```

## Diferenças em Relação ao Sistema Original

### Sistema Original
- Executava um programa pré-definido e encerrava
- Não havia interação com o usuário
- Necessário recompilar para executar outro programa

### Sistema Novo (Modo Interativo)
- ✅ **Execução contínua** - aguarda comandos do usuário
- ✅ **Menu interativo** - interface amigável
- ✅ **Múltiplas execuções** - execute quantos programas desejar
- ✅ **Seleção dinâmica** - escolha o programa em tempo de execução
- ✅ **Encerramento controlado** - saia quando desejar (opção 0)

## Tratamento de Erros

O sistema inclui tratamento de erros para:
- Nomes de programas inválidos
- Entrada de dados incorreta
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
- ✅ Aguarda chamadas de funções (menu interativo)
- ✅ Encerra apenas sob comando (opção 0)
