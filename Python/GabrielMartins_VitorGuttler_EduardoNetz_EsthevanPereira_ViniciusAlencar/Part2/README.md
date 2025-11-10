# Simulador de Sistema Operacional Básico

Este projeto, desenvolvido para a disciplina de Sistemas Operacionais, é um simulador de um sistema computacional simples, implementado em Python. Ele modela os componentes fundamentais de hardware e software, permitindo a criação, gerenciamento e execução de múltiplos processos em um ambiente com memória paginada e escalonamento preemptivo.

O código é uma tradução e expansão de um projeto base originalmente fornecido em Java pelo Prof. Fernando Dotti.

## Funcionalidades Principais

- **Arquitetura de CPU Simulada:** Uma CPU com um conjunto de instruções (ISA) próprio, registradores, e capacidade de gerar interrupções.
- **Gerenciamento de Memória Paginada:** A memória física é dividida em frames, e os processos recebem um espaço de endereçamento lógico através de tabelas de páginas.
- **Gerenciamento de Processos:** Suporte completo ao ciclo de vida de um processo, incluindo criação, término e armazenamento de contexto em um Process Control Block (PCB).
- **Escalonador Preemptivo (Round-Robin):** Um escalonador que gerencia uma fila de processos prontos, concedendo a cada um uma fatia de tempo (quantum) de CPU.
- **Tratamento de Interrupções e System Calls:** O sistema é capaz de lidar com exceções de hardware (endereço inválido, instrução inválida, overflow) e chamadas de sistema para I/O (leitura e escrita).
- **Interface de Linha de Comando (CLI):** Uma interface interativa para carregar programas, gerenciar processos e inspecionar o estado do sistema.

## Arquitetura do Sistema

O simulador é dividido em duas camadas principais: Hardware (HW) e Software (SO), refletindo a separação de responsabilidades em um sistema computacional real.

### Camada de Hardware (HW)

- **`CPU`**: O cérebro da simulação. Executa o ciclo de instrução (busca, decodificação, execução). Contém:
    - Registradores (`reg`): Armazenamento rápido para operações.
    - Program Counter (`pc`): Aponta para a próxima instrução lógica a ser executada.
    - Instruction Register (`ir`): Armazena a instrução atual.
    - Lógica de tradução de endereços (MMU simulada) que converte endereços lógicos em físicos usando a tabela de páginas do processo.
- **`Memory`**: Representa a memória RAM como um array de `Word`.
- **`Word`**: A menor unidade de dados/instrução, contendo um `Opcode` e seus parâmetros.
- **`Opcode` e `Interrupts`**: Enumerações que definem o conjunto de instruções suportado pela CPU e os tipos de interrupções que ela pode gerar.

### Camada de Software (SO)

- **`GerenteMemoria`**: Responsável por alocar e desalocar frames de memória física. Ele mantém um mapa de frames livres e cria as tabelas de páginas para os novos processos.
- **`PCB (Process Control Block)`**: Uma estrutura de dados que armazena todo o contexto de um processo: seu ID, estado (Pronto, Executando, Terminado), PC, valores dos registradores e sua tabela de páginas.
- **`GerenteProcessos`**: Gerencia o ciclo de vida dos processos. É responsável por:
    - Criar novos processos (alocando memória e criando um PCB).
    - Manter a fila de processos prontos (`ready_queue`).
    - Terminar processos, liberando seus recursos.
- **`Escalonador`**: Implementa o algoritmo de escalonamento Round-Robin. Ele seleciona o próximo processo da fila de prontos, restaura seu contexto na CPU e o executa por um `quantum`.
- **`InterruptHandling` e `SysCallHandling`**: Contêm as rotinas de tratamento que são invocadas pela CPU quando uma interrupção ou uma chamada de sistema ocorre. Por exemplo, uma interrupção de endereço inválido faz com que o `InterruptHandling` marque o processo como terminado.
- **`Sistema`**: A classe principal que inicializa e integra todos os componentes de HW e SO. Ela também gerencia a interface de linha de comando (CLI), interpretando os comandos do usuário.

## Como Executar

Para iniciar o simulador, basta executar o script Python em um terminal.

```bash
python3 sistema_os.py
```

O sistema irá apresentar um prompt `>` aguardando comandos.

## Comandos Disponíveis

- `new <nomePrograma>`: Cria um novo processo a partir de um programa pré-definido.
- `rm <id>`: Remove um processo do sistema, liberando sua memória.
- `ps`: Lista todos os processos existentes e seus estados.
- `exec <id>`: Executa um único processo até que ele termine ou seja interrompido.
- `execall`: Inicia o escalonador para executar todos os processos na fila de prontos de forma concorrente.
- `dump <id>`: Exibe informações detalhadas de um processo, incluindo seu PCB e o conteúdo de sua memória lógica.
- `dumpm <inicio> <fim>`: Exibe o conteúdo de um intervalo da memória física.
- `traceon` / `traceoff`: Ativa ou desativa o modo de depuração, que mostra cada instrução sendo executada.
- `exit`: Encerra a execução do simulador.

## Programas de Exemplo

O simulador inclui vários programas para teste, como:

- `fatorial`, `fatorialV2`: Calcula o fatorial de um número.
- `fibonacci10`, `fibonacciREAD`: Gera a sequência de Fibonacci. A versão `READ` utiliza uma chamada de sistema para ler o tamanho da sequência do usuário.
- `PC`: Ordena um vetor de números usando o algoritmo Bubble Sort.

# Implementação da Parte 2 do Trabalho

Instruções para entrega no moodle:  
    Devido ao alto número de grupos, o professor pode ter que executar os trabalhos off-line,
    sem interação com o grupo, para proceder a avaliação.    Assim pede-se seguir estas orientações.
    Monte um diretório cujo nome seja a concatenação dos primeiros nomes dos integrantes do grupo.   
    Ex.: JoãoMariaJosé.
    Coloque seus fontes neste diretório, com toda sua estrutura de arquivos para compilar e executar.
    Escreva um arquivo README.pdf (ou txt) contendo os itens abaixo, e coloque neste diretório.
              Nomes dos integrantes. 
              Seção Implementação: 
                     Informe se seu programa implementa todas as características solicitadas.
                     E se há alguma restrição (situação que não está funcionando).
              Seção Testes
                     Para cada teste (cenário de execução) indique como executar e 
                     o resultado esperado em cada caso.
      Comprima este diretório com .ZIP e entregue na sala do moodle.   
      Por favor use *somente .ZIP*  e NÃO RAR.
   O objetivo é que ao abrir este .ZIP na máquina do professor, 
   ele terá uma pasta completa com seu trabalho, com tudo que é necessário para 
   executar ali dentro, seguindo o seu relatório.



Hey, add `libspnav` as a dependency. Otherwise, it will fail with:
'''bash
    CMake Error at /usr/share/cmake/Modules/FindPackageHandleStandardArgs.cmake:227 (message):  Could NOT find SPNAV (missing: SPNAV_LIBRARY SPNAV_INCLUDE_DIR) Call Stack (most recent call first):  /usr/share/cmake/Modules/FindPackageHandleStandardArgs.cmake:591 (_FPHSA_FAILURE_MESSAGE)  cmake/FindSPNAV.cmake:51 (find_package_handle_standard_args)  CMakeLists.txt:814 (find_package)
'''