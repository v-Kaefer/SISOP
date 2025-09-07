# SISOP
T1 - SISOP

Estrutura Modularizada

```
SISOP/
├── Sistema.java                   # O "main" - onde tudo começa
├── hardware/                      # Componentes do computador virtual
│   ├── CPU.java                  # Processador virtual  
│   ├── Memory.java               # Memória RAM virtual
│   ├── Word.java                 # Uma "palavra" de memória
│   ├── Opcode.java               # Conjunto de instruções da CPU
│   └── HW.java                   # Hardware completo (CPU + Memória)
├── memory/                        # Gerenciador de memória inteligente (NOVO!)
│   ├── GerenciadorMemoria.java   # Sistema de paginação
│   ├── PosicaoDeMemoria.java     # Representação de instrução/dado
│   ├── MemoryManagerBridge.java  # Ponte entre sistemas
│   ├── TesteGerenciadorMemoria.java   # Testes completos
│   └── TesteIntegracao.java      # Testes de integração
├── programs/                      # Programas que rodam na VM
│   ├── Programs.java             # Biblioteca de programas
│   └── Program.java              # Estrutura de um programa
└── software/                      # Sistema operacional da VM
    ├── SO.java                   # Núcleo do sistema operacional
    ├── Utilities.java            # Funções auxiliares
    ├── InterruptHandling.java    # Tratamento de interrupções
    └── SysCallHandling.java      # Chamadas de sistema
```

# TO-DO

* [X] Gerencia de Memória (Etapa 01) - COMPLETA
* [~] Gerencia de Processos (Etapa 02) - FUNDAÇÕES IMPLEMENTADAS
* [ ] Escalonamento -> Round-Robin (Etapa 03) - PLANEJADO

* Para o controle do tempo, vamos usar os ciclos de CPU.

Melhor metodologia para desenvolver o trabalho:

* Gerente de Memória -> Gerente de Processos -> Escalonamento

## Documentação

- **[DOCUMENTACAO_ETAPA02.md](DOCUMENTACAO_ETAPA02.md)** - Guia completo do sistema com foco em gerenciamento de memória
- **[DOCUMENTACAO_GERENCIA_PROCESSOS.md](DOCUMENTACAO_GERENCIA_PROCESSOS.md)** - Estado atual da implementação de gerência de processos

# Para compilar e rodar:

Na pasta SISOP, execute:

`javac Sistema.java`

Para rodar, execute:

`java Sistema`

## Gerenciador de Memória

O sistema implementa um Gerenciador de Memória com paginação completo, localizado no diretório `memory/`.

### Para testar o Gerenciador de Memória:

**Teste básico:**
```bash
javac -cp . memory/*.java
java -cp . memory.TesteGerenciadorMemoria
```

**Teste de integração com programas existentes:**
```bash
java -cp . memory.TesteIntegracao
```

### Características do Gerenciador:

- **Paginação**: Memória dividida em frames/páginas de tamanho configurável
- **Alocação dinâmica**: Alocação e desalocação de frames para processos
- **Tradução de endereços**: Conversão de endereços lógicos para físicos
- **Proteção de memória**: Verificação de acesso a áreas não alocadas
- **Estatísticas**: Monitoramento de uso da memória
- **Fragmentação**: Tratamento adequado de fragmentação externa
- **Bridge de integração**: Compatibilidade com o sistema existente

## Exemplos Práticos

### Exemplo de Gerência de Processos
```bash
javac examples/*.java
java examples.ExemploGerenciaProcessos
```

Este exemplo demonstra o estado atual da implementação de gerência de processos, mostrando:
- Alocação simultânea de memória para múltiplos processos
- Configuração de contexto da CPU
- Limitações atuais do sistema