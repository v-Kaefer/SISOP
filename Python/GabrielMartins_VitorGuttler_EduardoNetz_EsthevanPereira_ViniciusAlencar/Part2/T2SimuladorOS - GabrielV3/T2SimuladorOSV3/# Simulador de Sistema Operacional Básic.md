# Simulador de Sistema Operacional Concorrente com Memória Virtual

Este projeto é um simulador avançado de um sistema operacional, desenvolvido em Python, que implementa conceitos essenciais de concorrência e gerenciamento de memória virtual. O sistema opera em um modelo multithreaded, permitindo a sobreposição de operações de I/O com o processamento da CPU e gerenciando a memória de forma dinâmica através de paginação por demanda.

## Principais Conceitos Implementados

  * **Arquitetura Concorrente (Multithreading)**: O sistema é construído sobre três threads principais que operam em paralelo:

    1.  **Thread do Shell**: A interface do usuário, sempre reativa para aceitar comandos.
    2.  **Thread da CPU**: O processador que executa as instruções dos processos.
    3.  **Thread do Dispositivo de I/O**: Simula um hardware lento que processa requisições de I/O de forma assíncrona.

  * **Modelo de Três Estados de Processo**: Os processos transitam ativamente entre os estados `READY` (pronto para executar), `RUNNING` (em execução na CPU) e `BLOCKED` (aguardando I/O ou uma página da memória).

  * **Memória Virtual com Paginação por Demanda**:

      * **Carregamento Sob Demanda**: Ao serem criados, os processos carregam apenas sua primeira página para a memória. As demais são trazidas do "disco" somente quando são referenciadas pela primeira vez.
      * **Tratamento de *Page Fault***: Um acesso a uma página não presente na memória gera uma interrupção de *page fault*, que bloqueia o processo e aciona o SO para carregar a página necessária.

  * **Substituição de Página (Vitimação)**:

      * **Algoritmo FIFO**: Quando ocorre um *page fault* e a memória está cheia, o sistema utiliza o algoritmo FIFO (First-In, First-Out) para escolher uma página "vítima" a ser removida.
      * **Dirty Bit**: A tabela de páginas rastreia se uma página foi modificada (`dirty_bit`). Se uma página "suja" é vitimada, ela é salva na área de swap do disco antes de o frame ser reutilizado.

  * **I/O Assíncrono com Interrupções**: Um processo que solicita uma operação de I/O é imediatamente bloqueado, liberando a CPU. Ao término da operação, o dispositivo de I/O gera uma interrupção, e o SO move o processo de volta para a fila de prontos.

## Como Executar

1.  **Pré-requisitos**: Python 3 instalado.
2.  **Salvar o Código**: Salve o código-fonte em um arquivo chamado `sistema_os.py`.
3.  **Executar no Terminal**:
    ```sh
    python3 sistema_os.py
    ```
    O sistema iniciará, e o prompt interativo `>` aparecerá, pronto para receber comandos.

## Comandos do Shell Interativo

| Comando | Argumentos | Descrição |
| :--- | :--- | :--- |
| `new` | `<nomePrograma>` | Cria um novo processo a partir de um programa pré-definido. |
| `ps` | (nenhum) | Lista todos os processos existentes e seus estados (`READY`, `RUNNING`, `BLOCKED`). |
| `rm` | `<id>` | Remove um processo do sistema e libera todos os seus recursos. |
| `exec` | `<id>` | Prioriza um processo, movendo-o para o início da fila de prontos para ser o próximo a ser escalonado. |
| `execall` | (nenhum) | Garante que o escalonador seja ativado para tentar executar um processo, caso a CPU esteja ociosa. |
| `dump` | `<id>` | Exibe o PCB completo de um processo, incluindo sua tabela de páginas detalhada. |
| `dumpm` | `<inicio> <fim>` | Exibe o conteúdo da memória física real (frames) no intervalo especificado. |
| `stats` | (nenhum) | Mostra estatísticas sobre a quantidade de processos em cada estado. |
| `memstat`| (nenhum) | Exibe um mapa de uso dos frames da memória, mostrando quais estão livres e quais estão ocupados por qual processo/página. |
| `traceon` | (nenhum) | Ativa o modo de debug, que imprime cada instrução executada pela CPU. |
| `traceoff`| (nenhum) | Desativa o modo de debug. |
| `exit` | (nenhum) | Encerra o simulador de forma limpa, finalizando todas as threads. |

## Programas Disponíveis para Teste

  * **`fatorialV2`**: Um programa que executa uma `SYSCALL` de I/O no início, ideal para testar o bloqueio por I/O.
  * **`progMinimo`**: Um programa muito curto, útil para testes rápidos de criação e finalização.
  * **`PB`**: Um programa que usa intensivamente a CPU para calcular um fatorial.
  * **`PC`**: Um programa que implementa o Bubble Sort. Por ser maior e acessar diferentes partes da memória, é excelente para provocar *page faults* e vitimação.

## Roteiro de Teste Sugerido

Para observar o sistema em ação, use a configuração de memória pequena (`mem_size=64`, `tam_pg=16`) definida no código.

1.  **Teste de Concorrência e I/O**:

      * Crie um processo de I/O: `> new fatorialV2`
      * Imediatamente, crie um processo de CPU: `> new PC`
      * Use `ps` repetidamente. Observe o processo `fatorialV2` (ID 0) ir para `BLOCKED (I/O Request)`, enquanto o processo `PC` (ID 1) continua executando. Depois, veja o processo 0 voltar para `READY`.

2.  **Teste de Page Fault e Vitimação**:

      * Crie um processo grande: `> new PC`
      * Use `dump 0`. Apenas a página 0 estará com `Valido=True`.
      * Deixe o sistema rodar. O log mostrará a ocorrência de *page faults* para carregar novas páginas.
      * Use `memstat` para ver os 4 frames da memória serem preenchidos.
      * Ative o `traceon`. Deixe o sistema rodar até que uma instrução de escrita (`STD` ou `STX`) ocorra.
      * Use `dump 0` para confirmar que o `dirty_bit` da página correspondente foi ativado.
      * Continue a execução. Quando o processo precisar de uma 5ª página, o log mostrará a sequência completa: **Vitimacao** -\> **Salvamento da página suja no disco** -\> **Carregamento da nova página**.
