# PUCRS - Escola Politécnica

## Disciplina: Sistemas Operacionais - Trabalho Prático 2b
### Projeto Concorrente - Memória Virtual
**Prof. Fernando Luís Dotti**

## 1. Antecedentes

Você deve contar com o sistema completo, usando paginação e fazendo IO.

## 2. Memória Virtual

Nesta fase vamos implementar Memória Virtual, com os seguintes aspectos:

- Ao criar o processo, carrega somente a primeira página em um frame;
- Quando um endereço lógico é utilizado e a página não está em memória, gera page-fault;

### Page-fault:

- Pede-se um quadro a mais para o GM - vide abaixo
- **[**]** Adiciona mapeamento página/quadro na tabela de páginas do processo
- Encaminha pedido para trazer página do processo ao dispositivo de IO (disco) específico para paginação
- Processo executando vai para estado bloqueado
- Escalona outro processo

### Pede quadro para gerente de memória

- Gerente de memória tenta achar quadro livre
- Se não achar, tem que vitimar uma página ocupando um quadro
  - Escolher página (escolher uma política simples)
  - Salvar página em disco
  - Encaminha pedido para salvar página em disco, possibilitando trazer novamente
  - Processo executando vai para estado bloqueado
  - Escalona outro processo

### Interrupções novas, informando:

**Fim de salvamento de página em disco, liberando quadro** (só ocorre se alguém pediu este quadro)
- Passa respectivo quadro para o processo demandante
- Continua em **[**]**

**Fim de carga de página em quadro alocado da memória**
- Passa respectivo processo de bloqueado para pronto (fim do tratamento de page-fault)

### Disco

Desta forma, passamos a ter mais um dispositivo, o Disco:

Contém tanto os programas armazenados como quadros de memória salvos em disco com estado da execução.

Podemos:
- trazer páginas específicas de programas específicos para um quadro de memória
- salvar/copiar páginas específicas, em quadros da memória, para disco (quando a página é vitimada)
- trazer páginas, anteriormente vitimadas, de volta para a memória, em quadro disponível.

### Extensão do esquema de paginação

Estendendo o esquema de paginação, agora uma página pode:

- Nunca ter sido carregada em memória, neste caso o conteúdo é a página do programa original;
- Já ter sido carregada anteriormente, e neste caso o conteúdo está em um quadro de memória ou em um espaço em disco armazenando uma cópia do quadro que esteve em memória.

A informação para diferenciar isto deve estar na tabela de páginas. Você pode implementar o espaço em disco para manter cópias de quadros da memória de forma parecida com o gerente de memória: ele aloca quadros e libera quadros.

Do ponto de vista de estado do processo, ele tem os mesmos 3: running, ready, e blocked.

## 3. Testes

Os mesmos testes anteriores devem funcionar. Deve ser possível saber os quadros utilizados por um processo e ver a mudança durante a execução.

## 4. Esquema

_(Referência a esquema visual presente no documento original)_