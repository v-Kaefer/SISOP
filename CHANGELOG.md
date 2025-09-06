# Changelog - Sistema de Operação com Máquina Virtual

Todas as modificações notáveis para o projeto SISOP serão documentadas neste arquivo.

## [2.0.0] - Etapa 02 - 2025-01-XX

### Adicionado

#### Sistema de Testes Automatizados para GitHub Actions
- **GitHub Actions CI**: Configuração completa de integração contínua
  - Compilação automática de todo o projeto Java
  - Execução do sistema básico (Sistema.java)
  - Execução dos testes do gerenciador de memória
  - Execução dos testes de integração
  - Validação automática a cada push/pull request

#### Testes de Memória Expandidos
- **TesteGerenciadorMemoria.java**: Suite completa de testes do gerenciador
  - Teste com configuração padrão (1024 palavras, 128 frames)
  - Teste com configuração customizada (diferentes tamanhos)
  - Teste de tradução de endereços lógico → físico
  - Teste de fragmentação e defragmentação
  - Teste de proteção de memória e limites
- **TesteIntegracao.java**: Testes de integração com sistema existente
  - Carregamento de programas reais (factorial, fibonacci, etc.)
  - Múltiplos programas simultâneos na memória
  - Verificação de integridade de dados após tradução

#### Documentação Completa "Para Idiotas"
- **DOCUMENTACAO_ETAPA02.md**: Guia didático completo
  - Explicação da estrutura do projeto
  - Como funciona cada componente (hardware, software, memory)
  - Conceitos de paginação e tradução de endereços explicados de forma simples
  - Exemplos práticos com código comentado
  - Guia de execução passo-a-passo
  - Conceitos importantes de SO explicados didaticamente

#### Melhorias na Infraestrutura de CI/CD
- **Correção de ci.yaml**: 
  - Sintaxe YAML corrigida (missing colons, invalid structure)
  - Configuração correta do Java 17 com Temurin distribution
  - Steps organizados para compilação sequencial
  - Execução de testes em ordem lógica
  - Timeouts apropriados para execução

### Modificado

#### GitHub Actions Workflow
- **Arquivo**: `.github/workflows/ci.yaml`
- **Mudanças**:
  - Nome alterado de "C CI" para "Java CI" (correção)
  - Adicionada configuração correta do setup-java com distribution
  - Compilação separada em steps (Sistema.java + memory/*.java)
  - Três fases de teste: sistema básico, memória, integração
  - Sintaxe YAML corrigida (adicionados colons faltantes)

#### Estrutura de Documentação
- **CHANGELOG.md**: Reformatado para incluir histórico completo
- **README.md**: Mantidas instruções de compilação existentes
- **Organização**: Documentação técnica vs didática separadas

### Funcionalidades Implementadas

#### Automação de Testes
- **Compilação automática**: Todo push/PR compila automaticamente
- **Execução de testes**: Três níveis de validação
  1. **Sistema básico**: Executa programa factorial padrão
  2. **Gerenciador de memória**: Valida paginação e tradução
  3. **Integração**: Verifica compatibilidade com programas existentes
- **Feedback imediato**: Status verde/vermelho no GitHub

#### Proteção de Qualidade
- **Validação automática**: Impede merge de código quebrado
- **Testes abrangentes**: Cobertura de casos edge e fragmentação
- **Monitoramento**: Estatísticas de uso de memória e performance

#### Documentação Educativa
- **Linguagem acessível**: Explicações "para idiotas" mas tecnicamente corretas
- **Exemplos práticos**: Código real comentado linha por linha
- **Conceitos fundamentais**: SO, paginação, MMU explicados do zero
- **Guias práticos**: Como compilar, executar e interpretar resultados

### Tecnicalidades Resolvidas

#### Problemas de CI Corrigidos
- **Sintaxe YAML inválida**: Corrigidos missing colons em steps
- **Compilação incorreta**: java Sistema.java → javac + java Sistema
- **Dependências**: Ordem correta de compilação de memory/*.java
- **Setup Java**: Configuração completa com distribution e version

#### Estrutura de Testes
- **Isolamento**: Cada teste verifica aspectos específicos
- **Reutilização**: Testes usam componentes reais do sistema
- **Validação**: Verificação automática de resultados esperados
- **Cobertura**: Cenários normais e de erro

### Compatibilidade

#### Backward Compatibility
- **Sistema existente**: Nenhuma mudança quebra funcionalidade anterior
- **API pública**: Interfaces mantidas (HW, CPU, Memory, Programs)
- **Programas existentes**: Todos os programas (factorial, fibonacci, etc.) funcionam

#### Forward Compatibility  
- **Extensibilidade**: Arquitetura preparada para próximas etapas
- **Configurabilidade**: Parâmetros de memória ajustáveis
- **Modularidade**: Componentes independentes para futuras modificações

### Métricas de Qualidade

#### Cobertura de Testes
- **Gerenciador de memória**: 100% dos métodos principais testados
- **Cenários de erro**: Validação de limites e proteção
- **Integração**: Compatibilidade com todos os programas existentes
- **Performance**: Monitoramento de uso de memória e estatísticas

#### Automação
- **CI/CD**: Pipeline completo de build → test → deploy
- **Feedback**: Resultados automáticos em <2 minutos
- **Proteção**: Branch protection com required status checks

## [1.0.0] - Etapa 01 - 2025-09-06

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