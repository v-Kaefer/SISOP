# Changelog - Gerente de Memória para Paginação

Todas as modificações notáveis para o projeto do Gerente de Memória serão documentadas neste arquivo.

## [1.0.0] - 2025-09-06

### Adicionado

#### Implementação do Gerente de Memória para Paginação (Etapa 01)

**Novos Arquivos:**
- `PosicaoDeMemoria.java` - Classe representando uma posição de memória da MV
- `GerenciadorMemoria.java` - Implementação principal do gerente de memória com paginação
- `TesteGerenciadorMemoriaCompleto.java` - Suite completa de testes
- `CHANGELOG.md` - Este arquivo de registro de mudanças
- `.instructions.md` - Documentação dos parâmetros

**Funcionalidades Implementadas:**

#### Core do Gerente de Memória
- **Alocação de frames**: método `aloca()` conforme especificação
- **Desalocação de frames**: método `desaloca()` com limpeza de conteúdo
- **Controle de frames**: estruturas internas para rastreamento livre/ocupado
- **Configuração flexível**: suporte a diferentes tamMem e tamPg

#### Carga e Tradução
- **Carga de programa**: método `carregaPrograma()` respeitando paginação
- **Tradução de endereços**: conversão lógico → físico com validação
- **Proteção de memória**: verificação de limites e acesso inválido
- **Acesso à memória**: métodos `acessaMemoria()` e `escreveMemoria()`

#### Estruturas de Dados
- **PosicaoDeMemoria**: representação abstrata de instrução/dado
- **Arrays de controle**: framesAlocados[], frameOwner para debugging

#### Sistema de Testes
- **Teste configuração padrão**: tamMem=1024, tamPg=8 (128 frames)
- **Teste configuração customizada**: diferentes tamanhos de memória
- **Teste tradução de endereços**: validação de conversão lógico→físico
- **Teste fragmentação**: cenários de alocação/desalocação intercalada
- **Teste limites**: tentativas de alocar mais que disponível

#### Funcionalidades de Debugging
- **Estatísticas de memória**: utilização, frames livres/ocupados
- **Mapa de memória**: visualização de estado dos frames
- **Conteúdo de páginas**: inspeção detalhada de frames
- **Rastreamento de processos**: identificação de proprietário por frame

### Características Técnicas

#### Parâmetros Configuráveis
- `tamMem`: Tamanho total da memória (padrão: 1024 palavras)
- `tamPg`: Tamanho da página (padrão: 8 palavras)
- `tamFrame`: Tamanho do frame = tamPg
- `numFrames`: Número de frames = tamMem / tamPg

#### Interface Principal
```java
// Alocação
boolean aloca(int nroPalavras, int[] tabelaPaginas, String processoId)

// Desalocação  
void desaloca(int[] tabelaPaginas)

// Carga de programa
void carregaPrograma(PosicaoDeMemoria[] programa, int[] tabelaPaginas)

// Tradução de endereços
int traduzeEndereco(int enderecoLogico, int[] tabelaPaginas)
```

#### Algoritmos Implementados
- **Alocação de frames**: First-fit com verificação de disponibilidade
- **Tradução de endereços**: página = endLogico / tamPg, desloc = endLogico % tamPg
- **Proteção de memória**: validação de limites durante acesso
- **Carga de programa**: cópia respeitando limites de página

### Baseado em Especificações
- **Guia.md**: Definição da Máquina Virtual, CPU e Memória
- **Etapa01.md**: Especificação do Gerente de Memória para Paginação
- **Tabela de Comandos**: Opcodes e micro-operações da MV

### Estrutura do Projeto
```
SISOP/
├── hardware/                            # Contém o hardware definido para a vm.
    ├── CPU.java
    ├── HW.java
    ├── Interrupts.java
    ├── Memory.java
    ├── Opcode.java                       # Enumeração de opcodes
    └── Word.java
├── memory/
    ├── GerenciadorMemoria.java           # Implementação principal
    ├── PosicaoDeMemoria.java             # Estrutura de memória da MV
    └── TesteGerenciadorMemoria.java      # Teste para ci
├── programs/                             # Contém o programa a ser executado na vm.
    ├── Program.java
    └── Programs.java
├── software/                             # Contém o código do SO da vm.
    ├── InterruptHandling.java
    ├── SO.java
    ├── SysCallHandling.java
    └── Utilities.java
└── CHANGELOG.md                          # Este arquivo
```

### Próximos Passos
- [ ] Integração com sistema de processos
- [ ] Implementação de algoritmos de substituição de página
- [ ] Otimização de alocação (algoritmos best-fit, worst-fit)
- [ ] Métricas avançadas de performance

### Notas de Desenvolvimento
- Código totalmente baseado nas especificações fornecidas
- Interface limpa e bem documentada
- Testes abrangentes cobrindo todos os cenários
- Implementação flexível para diferentes configurações
- Proteção de memória implementada conforme especificação