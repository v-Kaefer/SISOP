# PUCRS - Escola Politécnica

## Disciplina: Sistemas Operacionais - Trabalho Prático
### Gerente de Processos
**Prof. Fernando Luís Dotti**

## 1. Gerente de Processos

### 2.1 Funcionalidade

O GP é um módulo do SO e é responsável por:

#### Criar um processo, dado um programa passado como parâmetro

```
boolean criaProcesso(programa)
    verifica tamanho do programa
    pede alocação de memória ao Gerente de Memória
    se nao tem memória, retorna negativo
    Cria PCB
    Seta partição usada no pcb
    Carrega o programa
    Seta demais parâmetros do PCB (id, pc=0, etc)
    Coloca PCB na fila de prontos
    Retorna true
```

#### Desaloca um processo

```
desalocaProcesso(id)
    desaloca toda memória do processo com id
    retira de qualquer fila que esteja
    desaloca pcb
```

### 2.2 Estruturas, filas, processo rodando

Nesta fase de evolução do nosso sistema precisamos das seguintes estruturas:

**PCB:** Você deve criar uma estrutura de descrição do processo com as informações necessárias para a gerência dele no seu sistema. Esta estrutura é o Process Control Block. Todo processo tem um PCB próprio.

**Running/rodando:** No nosso sistema, apenas um processo está rodando em um determinado momento. Existe como variável do SO um ponteiro rodando/running que identifica o PCB do processo executando.

**Ready/aptos:** Da mesma forma, temos uma lista de processos aptos/ready que podem rodar. Trata-se de uma lista de PCBs.

### 2.3 Funcionamento/Testes

Agora você dispõe de um sistema que pode ter vários processos em memória. Para demonstrar o funcionamento, você deve ter um sistema iterativo: ele fica esperando comandos. A cada comando, o sistema reage e volta a esperar o próximo comando.

#### Os comandos possíveis são:

**`new <nomeDePrograma>`** - cria um processo na memória. Pede ao GM para alocar memória. Cria PCB, seta partição ou tabela de páginas do processo no PCB, etc. coloca processo em uma lista de processos (prontos). Esta chamada retorna um identificador único do processo no sistema (ex.: 1, 2, 3 …)

**`rm <id>`** - retira o processo id do sistema, tenha ele executado ou não

**`ps`** - lista todos processos existentes

**`dump <id>`** - lista o conteúdo do PCB e o conteúdo da memória do processo com id

**`dumpM <inicio, fim>`** - lista a memória entre posições início e fim, independente do processo

**`exec <id>`** - executa o processo com id fornecido. se não houver processo, retorna erro.

**`traceOn`** - liga modo de execução em que CPU print cada instrução executada

**`traceOff`** - desliga o modo acima

**`exit`** - sai do sistema

---

**Nota:** Você pode criar outros nomes para os comandos. Desde que façam o descrito.