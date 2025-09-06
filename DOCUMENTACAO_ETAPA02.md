# Guia "Para Idiotas": Sistema de Operação com Máquina Virtual (Etapa 02)

## Introdução - O Que É Este Projeto?

Imagine que você precisa construir um computador do zero, mas apenas no software. É exatamente isso que estamos fazendo! Este projeto é um **Sistema de Operação** (SO) completo que roda em uma **Máquina Virtual** (VM), tudo escrito em Java.

### Por Que Fazer Isso?

- **Aprender conceitos fundamentais**: Como um computador realmente funciona por dentro
- **Entender sistemas operacionais**: Como programas são carregados, executados e gerenciados
- **Experimentar com segurança**: Podemos "quebrar" nossa VM sem danificar o computador real

## Estrutura do Projeto - Como Está Organizado?

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

## Como Funciona? - Explicação Passo a Passo

### 1. O Hardware Virtual (pasta `hardware/`)

#### CPU.java - O Cérebro da Máquina
```java
// A CPU tem registradores (como gavetas para guardar números)
private int[] reg = new int[10];  // 10 registradores: R0, R1, R2... R9

// Tem um "Program Counter" que aponta para a próxima instrução
private int pc = 0;

// E executa um ciclo infinito:
while (!cpuStop) {
    // 1. Busca a instrução na memória
    ir = m[pc];
    
    // 2. Decodifica que tipo de instrução é
    switch (ir.opc) {
        case LDI:  // Carrega um número direto no registrador
            reg[ir.ra] = ir.p;
            break;
        case ADD:  // Soma dois registradores
            reg[ir.ra] = reg[ir.ra] + reg[ir.rb];
            break;
        // ... e assim por diante
    }
    
    // 3. Avança para próxima instrução
    pc++;
}
```

**Em linguagem simples**: A CPU é como uma pessoa muito obediente que só sabe fazer operações muito básicas (somar, subtrair, pular para outra instrução), mas faz isso muito rapidamente e sem errar.

#### Memory.java - A Memória RAM Virtual
```java
public class Memory {
    public Word[] pos;  // Array gigante de "palavras" de memória
    
    public Memory(int size) {
        pos = new Word[size];  // Cria a memória com o tamanho desejado
        // Inicializa todas as posições
        for (int i = 0; i < pos.length; i++) {
            pos[i] = new Word(Opcode.___, -1, -1, -1);  // Posição vazia
        }
    }
}
```

**Em linguagem simples**: A memória é como um armário gigante com milhares de gavetas numeradas. Cada gaveta pode guardar uma instrução ou um número.

#### Word.java - Uma "Palavra" de Memória
```java
public class Word {
    public Opcode opc;  // Que tipo de instrução é (LDI, ADD, etc.)
    public int ra;      // Primeiro registrador
    public int rb;      // Segundo registrador  
    public int p;       // Parâmetro/endereço
}
```

**Em linguagem simples**: Cada "palavra" é como um post-it que pode conter uma instrução ("some R1 + R2") ou apenas um número para armazenar dados.

### 2. O Gerenciador de Memória Inteligente (pasta `memory/`) - **NOVIDADE DA ETAPA 02!**

#### O Problema: Por Que Precisamos de um Gerenciador?

Antes, era assim:
```
Memória: [Programa1][Programa2][Programa3][Espaço Vazio]
```

Problemas:
- **Fragmentação**: Se Programa2 termina, fica um buraco no meio
- **Programas grandes**: E se um programa não cabe em um espaço contínuo?
- **Desperdício**: Memória desperdiçada entre programas

#### A Solução: Paginação!

Com paginação, dividimos tudo em "páginas" de tamanho fixo:

```
Memória Física:     [Frame0][Frame1][Frame2][Frame3][Frame4]...
Programa Lógico:    [Página0][Página1][Página2]

Mapeamento:
Página 0 → Frame 2
Página 1 → Frame 5  
Página 2 → Frame 1
```

#### GerenciadorMemoria.java - O Organizador Inteligente

```java
public class GerenciadorMemoria {
    private PosicaoDeMemoria[] memoria;      // Memória física real
    private boolean[] framesAlocados;        // Quais frames estão ocupados?
    private Map<Integer, String> frameOwner; // Quem é dono de cada frame?
    
    // Método principal: alocar memória para um programa
    public boolean aloca(int nroPalavras, int[] tabelaPaginas, String processoId) {
        // 1. Calcula quantas páginas precisa
        int paginasNecessarias = (nroPalavras + tamPg - 1) / tamPg;
        
        // 2. Procura frames livres (não precisam ser contíguos!)
        List<Integer> framesLivres = encontraFramesLivres();
        
        if (framesLivres.size() >= paginasNecessarias) {
            // 3. Aloca os frames encontrados
            for (int i = 0; i < paginasNecessarias; i++) {
                int frame = framesLivres.get(i);
                tabelaPaginas[i] = frame;
                framesAlocados[frame] = true;
                frameOwner.put(frame, processoId);
            }
            return true;  // Sucesso!
        }
        return false;  // Não há memória suficiente
    }
}
```

**Em linguagem simples**: É como um bibliotecário super organizado que:
1. Pega um livro muito grande (programa)
2. Divide em capítulos menores (páginas)  
3. Coloca cada capítulo em qualquer prateleira disponível (frames)
4. Mantém um índice de onde está cada capítulo (tabela de páginas)

#### Tradução de Endereços - A Mágica Por Trás

```java
public int traduzeEndereco(int enderecoLogico, int[] tabelaPaginas) {
    // O programa pensa que está no endereço 15
    int pagina = enderecoLogico / tamPg;        // Página 1 (se tamPg = 8)
    int deslocamento = enderecoLogico % tamPg;  // Posição 7 dentro da página
    int frame = tabelaPaginas[pagina];          // Frame real onde está a página
    int enderecoFisico = frame * tamFrame + deslocamento;
    
    return enderecoFisico;  // Endereço real na memória física
}
```

**Exemplo Prático**:
- Programa quer acessar endereço lógico 15
- Página = 15 ÷ 8 = 1 (segunda página)
- Deslocamento = 15 % 8 = 7 (sétima posição na página)  
- Se tabelaPaginas[1] = 5 (página está no frame 5)
- Endereço físico = 5 × 8 + 7 = 47

### 3. Os Programas (pasta `programs/`)

#### Programs.java - A Biblioteca de Software
Contém programas prontos como:
- **fatorial**: Calcula fatorial de um número
- **fibonacci**: Sequência de Fibonacci
- **progMinimo**: Programa mais simples possível
- **PC**: Bubble sort (ordenação)

```java
new Program("fatorial", new Word[] {
    new Word(Opcode.LDI, 0, -1, 7),     // R0 = 7 (calcular 7!)
    new Word(Opcode.LDI, 1, -1, 1),     // R1 = 1 (acumulador)
    new Word(Opcode.MULT, 1, 0, -1),    // R1 = R1 * R0
    new Word(Opcode.SUB, 0, 6, -1),     // R0 = R0 - 1
    new Word(Opcode.JMP, -1, -1, 2),    // Volta para multiplicação
    new Word(Opcode.STOP, -1, -1, -1)   // Para o programa
});
```

### 4. O Sistema Operacional (pasta `software/`)

#### SO.java - O Núcleo do Sistema
```java
public class SO {
    public HW hw;                    // Acesso ao hardware
    public Utilities utils;          // Funções auxiliares
    public InterruptHandling ih;     // Tratamento de interrupções
    public SysCallHandling sysCall;  // Chamadas de sistema
    
    public SO(HW _hw) {
        hw = _hw;
        utils = new Utilities(hw);
        ih = new InterruptHandling();
        sysCall = new SysCallHandling();
        
        // Conecta tudo
        hw.cpu.setAddressOfHandlers(ih, sysCall);
        hw.cpu.setUtilities(utils);
    }
}
```

#### Utilities.java - As Ferramentas Úteis
```java
public void loadAndExec(Word[] p) {
    loadProgram(p);                    // 1. Carrega programa na memória
    System.out.println("Programa carregado");
    dump(0, p.length);                 // 2. Mostra conteúdo da memória
    
    hw.cpu.setContext(0);              // 3. PC = 0 (começa do início)
    System.out.println("Iniciando execução");
    hw.cpu.run();                      // 4. CPU executa até STOP
    
    System.out.println("Memória após execução");
    dump(0, p.length);                 // 5. Mostra resultado final
}
```

## Etapa 02: O Que Foi Implementado?

### 1. Sistema de Testes Automatizados

#### TesteGerenciadorMemoria.java
- **Testa configuração padrão**: 1024 palavras, 128 frames de 8 palavras
- **Testa configuração customizada**: Diferentes tamanhos de memória
- **Testa tradução de endereços**: Verifica se endereço lógico → físico funciona
- **Testa fragmentação**: Simula programas sendo carregados e removidos
- **Testa limites**: Tentativas de alocar mais memória que disponível

#### TesteIntegracao.java  
- **Integração com programas existentes**: Carrega programas reais (factorial, fibonacci)
- **Múltiplos programas simultaneamente**: Vários programas na memória ao mesmo tempo
- **Verificação de consistência**: Dados permanecem íntegros após tradução

### 2. GitHub Actions - Testes Automáticos

Arquivo `.github/workflows/ci.yaml` configurado para:
1. **Compilar** todo o projeto Java
2. **Executar** o sistema básico (Sistema.java)
3. **Rodar** testes do gerenciador de memória
4. **Verificar** integração com sistemas existentes

A cada push/pull request, o GitHub automaticamente:
- ✅ Compila o código
- ✅ Executa todos os testes  
- ✅ Reporta se algo quebrou

### 3. Melhorias na Documentação

- **CHANGELOG.md atualizado**: Registro detalhado de todas as mudanças
- **README.md melhorado**: Instruções de compilação e execução
- **Este documento**: Explicação completa "para idiotas"

## Como Executar? - Guia Prático

### 1. Executar o Sistema Básico
```bash
javac Sistema.java     # Compila
java Sistema          # Executa (roda programa fatorial)
```

### 2. Executar Testes de Memória
```bash
javac memory/*.java                    # Compila testes
java memory.TesteGerenciadorMemoria   # Roda testes do gerenciador
java memory.TesteIntegracao           # Roda testes de integração
```

### 3. Ver Estatísticas Detalhadas
Os testes mostram:
- **Frames livres vs ocupados**
- **Mapa visual da memória**
- **Tradução de endereços passo-a-passo**
- **Detecção de erros e proteção**

## Conceitos Importantes Aprendidos

### 1. **Paginação**
- **Problema**: Fragmentação externa da memória
- **Solução**: Dividir memória em blocos de tamanho fixo
- **Vantagem**: Programas podem usar memória não-contígua

### 2. **Tradução de Endereços**
- **Endereço Lógico**: O que o programa "pensa" que está usando
- **Endereço Físico**: Onde realmente está na memória
- **MMU (Memory Management Unit)**: Quem faz a tradução

### 3. **Proteção de Memória**
- **Limites**: Programa não pode acessar memória de outros
- **Validação**: Verificação em tempo de execução
- **Segurança**: Isolamento entre processos

### 4. **Gerenciamento de Recursos**
- **Alocação**: Como distribuir memória limitada
- **Desalocação**: Como liberar memória quando não precisa mais
- **Estatísticas**: Monitoramento de uso e performance

## Próximos Passos (Etapas Futuras)

1. **Gerenciamento de Processos**: Múltiplos programas executando simultaneamente
2. **Escalonamento**: Decidir qual programa roda quando
3. **Sistema de Arquivos**: Como salvar/carregar dados
4. **Rede**: Comunicação entre VMs
5. **Interface Gráfica**: Interface visual para administração

## Conclusão

Este projeto implementa um **sistema operacional completo** desde o zero, incluindo:
- ✅ **Hardware virtual** (CPU + Memória)
- ✅ **Gerenciamento de memória** com paginação
- ✅ **Programas executáveis** (fatorial, fibonacci, etc.)
- ✅ **Sistema de testes** automatizados
- ✅ **CI/CD** com GitHub Actions

É uma base sólida para entender como computadores realmente funcionam por dentro!

---

**Para mais detalhes técnicos**, consulte:
- `CHANGELOG.md` - Histórico de mudanças
- `.github/instructions/.instructions.md` - Especificações técnicas  
- Código fonte com comentários detalhados