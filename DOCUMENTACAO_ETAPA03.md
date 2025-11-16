# DOCUMENTACAO ETAPA 3 - SINCRONIZAÇÃO ENTRE PROCESSOS

## Visão Geral

A **Etapa 3** implementa mecanismos de sincronização entre processos, permitindo que múltiplos processos coordenem o acesso a recursos compartilhados de forma segura e eficiente. Esta etapa introduz **Semáforos** e **Mutex** como primitivas fundamentais de sincronização.

## Motivação

Em sistemas com múltiplos processos executando concorrentemente, surge a necessidade de:
- **Coordenar acesso a recursos compartilhados** (memória, dispositivos, etc.)
- **Evitar condições de corrida** (race conditions)
- **Garantir exclusão mútua** em seções críticas
- **Sincronizar execução** entre processos (produtor-consumidor, leitores-escritores, etc.)

Sem sincronização adequada, processos concorrentes podem:
- Sobrescrever dados uns dos outros
- Ler dados inconsistentes
- Causar deadlocks e livelocks
- Produzir resultados não determinísticos

## Primitivas de Sincronização Implementadas

### 1. Semáforo (`Semaforo.java`)

Um **semáforo** é uma variável inteira com duas operações atômicas:
- **down()** (também chamada P ou wait): Decrementa o contador. Se ficar negativo, bloqueia o processo.
- **up()** (também chamada V ou signal): Incrementa o contador e acorda um processo bloqueado.

#### Características do Semáforo

```java
public class Semaforo {
    private int valor;                           // Contador de recursos
    private Queue<ProcessControlBlock> filaEspera; // Processos bloqueados
    
    public Semaforo(String nome, int valorInicial);
    public boolean down(ProcessControlBlock processo);
    public ProcessControlBlock up();
    public boolean tryDown(); // Versão não-bloqueante
}
```

#### Tipos de Semáforos

1. **Semáforo Binário** (valor inicial = 1)
   - Similar a um mutex
   - Garante exclusão mútua
   - Valor sempre 0 ou 1

2. **Semáforo Contador** (valor inicial = N)
   - Controla N recursos idênticos
   - Permite N processos simultâneos
   - Útil para pools de recursos

#### Operações

##### down() - Solicitar Recurso
```java
boolean down(ProcessControlBlock processo)
```
1. Decrementa o valor do semáforo
2. Se valor < 0:
   - Adiciona processo à fila de espera
   - Muda estado do processo para WAITING
   - Retorna false (processo bloqueado)
3. Se valor >= 0:
   - Processo obtém o recurso
   - Retorna true (sucesso)

##### up() - Liberar Recurso
```java
ProcessControlBlock up()
```
1. Incrementa o valor do semáforo
2. Se há processos na fila de espera:
   - Remove primeiro processo da fila (FIFO)
   - Muda estado para READY
   - Retorna o processo acordado
3. Caso contrário:
   - Retorna null

##### tryDown() - Tentativa Não-Bloqueante
```java
boolean tryDown()
```
- Tenta obter recurso sem bloquear
- Retorna true se obteve, false caso contrário
- Útil para evitar deadlocks

#### Estatísticas do Semáforo

Cada semáforo mantém:
- `valor`: Contador atual de recursos
- `totalOperacoesDown`: Total de chamadas down()
- `totalOperacoesUp`: Total de chamadas up()
- `totalBloqueios`: Quantos processos foram bloqueados
- `filaEspera`: Lista de processos bloqueados

### 2. Mutex (`Mutex.java`)

Um **Mutex** (Mutual Exclusion) é um mecanismo de exclusão mútua que garante que apenas um processo pode acessar uma seção crítica por vez.

#### Características do Mutex

```java
public class Mutex {
    private boolean disponivel;                // Estado do mutex
    private ProcessControlBlock proprietario;  // Quem possui o mutex
    private Semaforo semaforo;                 // Implementação interna
    
    public Mutex(String nome);
    public boolean lock(ProcessControlBlock processo);
    public boolean unlock(ProcessControlBlock processo);
    public boolean tryLock(ProcessControlBlock processo);
}
```

#### Diferenças entre Mutex e Semáforo Binário

| Aspecto | Mutex | Semáforo Binário |
|---------|-------|------------------|
| Ownership | Sim (apenas quem fez lock pode unlock) | Não (qualquer processo pode fazer up) |
| Uso | Proteção de seção crítica | Sincronização geral |
| Lock recursivo | Detectado e evitado | Não se aplica |
| Transferência | Transfere para próximo processo | Apenas sinaliza disponibilidade |

#### Operações

##### lock() - Adquirir Mutex
```java
boolean lock(ProcessControlBlock processo)
```
1. Verifica se processo já possui o mutex (evita lock recursivo)
2. Tenta adquirir usando semáforo interno
3. Se sucesso:
   - Define processo como proprietário
   - Marca mutex como não disponível
   - Retorna true
4. Se falha:
   - Processo é bloqueado (estado WAITING)
   - Retorna false

##### unlock() - Liberar Mutex
```java
boolean unlock(ProcessControlBlock processo)
```
1. Verifica se processo é o proprietário
2. Se não é proprietário:
   - Retorna false (erro)
3. Se é proprietário:
   - Libera semáforo interno (up)
   - Se há processo aguardando:
     - Transfere ownership para ele
   - Caso contrário:
     - Marca mutex como disponível
   - Retorna true

##### tryLock() - Tentativa Não-Bloqueante
```java
boolean tryLock(ProcessControlBlock processo)
```
- Tenta adquirir mutex sem bloquear
- Útil para evitar espera
- Retorna true se adquiriu, false caso contrário

#### Estatísticas do Mutex

Cada mutex mantém:
- `disponivel`: Se está livre
- `proprietario`: Processo atual (null se livre)
- `totalLocks`: Total de aquisições
- `totalUnlocks`: Total de liberações
- `totalTentativasFalhadas`: tryLock() que falharam

## Integração com o Sistema

### Estados de Processo

Os processos agora podem estar em estado **WAITING** quando bloqueados em semáforos/mutex:

```
NEW -> READY -> RUNNING -> WAITING (bloqueado em semáforo/mutex)
                    ↑           ↓
                    └─────────┘ (acordado quando recurso disponível)
```

### Fluxo de Sincronização

```
Processo A                Semáforo/Mutex              Processo B
    |                           |                          |
    |-- down()/lock() -------->|                          |
    |<----- sucesso ------------|                          |
    |                           |                          |
    | (seção crítica)           |                          |
    |                           |                          |
    |                           |<---- down()/lock() ------|
    |                           |                          |
    |                           |---- bloqueia ----------->|
    |                           | (estado = WAITING)       |
    |                           |                          |
    |--- up()/unlock() -------->|                          |
    |                           |                          |
    |                           |---- acorda ------------->|
    |                           | (estado = READY)         |
```

## Problemas Clássicos Implementados

### 1. Produtor-Consumidor

**Cenário**: Buffer compartilhado de tamanho limitado, produtores adicionam itens, consumidores removem itens.

**Sincronização necessária**:
- Produtor não pode adicionar se buffer cheio
- Consumidor não pode remover se buffer vazio
- Acesso ao buffer deve ser mutuamente exclusivo

**Solução com Semáforos**:
```java
Semaforo espacosVazios = new Semaforo("Vazios", N);  // N = tamanho do buffer
Semaforo itensCheios = new Semaforo("Cheios", 0);    // Inicialmente vazio
Mutex mutexBuffer = new Mutex("Buffer");              // Exclusão mútua

// Produtor
void produzir(item) {
    espacosVazios.down();      // Aguarda espaço vazio
    mutexBuffer.lock();        // Entra na seção crítica
    buffer.add(item);          // Adiciona item
    mutexBuffer.unlock();      // Sai da seção crítica
    itensCheios.up();          // Sinaliza item disponível
}

// Consumidor
void consumir() {
    itensCheios.down();        // Aguarda item disponível
    mutexBuffer.lock();        // Entra na seção crítica
    item = buffer.remove();    // Remove item
    mutexBuffer.unlock();      // Sai da seção crítica
    espacosVazios.up();        // Sinaliza espaço disponível
}
```

**Ver**: `exemplos/ExemploProdutorConsumidor.java`

### 2. Seção Crítica com Mutex

**Cenário**: Múltiplos processos acessam contador compartilhado.

**Problema sem sincronização**:
```
Processo A                Processo B
read contador (0)
                          read contador (0)
contador = 0 + 1
                          contador = 0 + 1
write contador (1)
                          write contador (1)
Resultado: 1 (deveria ser 2!)
```

**Solução com Mutex**:
```java
Mutex mutex = new Mutex("ContadorMutex");

void incrementar() {
    mutex.lock();           // Entra na seção crítica
    int temp = contador;
    temp = temp + 1;
    contador = temp;
    mutex.unlock();         // Sai da seção crítica
}
```

**Ver**: `exemplos/ExemploSecaoCritica.java`

## Testes Implementados

### Suite de Testes (`software/TesteSincronizacao.java`)

#### Teste 1: Semáforo Básico
- Cria semáforo com valor inicial 2
- down() duas vezes com sucesso
- down() terceira vez bloqueia processo
- up() acorda processo bloqueado
- **Resultado**: ✓ PASSOU

#### Teste 2: Mutex Básico
- Primeiro lock() tem sucesso
- Segundo lock() bloqueia processo
- unlock() transfere ownership
- unlock() por não-proprietário falha
- **Resultado**: ✓ PASSOU

#### Teste 3: Operações Try
- tryDown() com sucesso e falha
- tryLock() com sucesso e falha
- Processos não são bloqueados
- **Resultado**: ✓ PASSOU

#### Teste 4: Semáforo Contador
- Semáforo com valor inicial 3 (pool de recursos)
- Primeiros 3 processos obtêm recursos
- 4º e 5º processos bloqueiam
- Liberação acorda processos na ordem FIFO
- **Resultado**: ✓ PASSOU

#### Teste 5: Exclusão Mútua
- Múltiplos processos tentam acessar seção crítica
- Apenas um por vez consegue
- Fila de espera funciona corretamente (FIFO)
- **Resultado**: ✓ PASSOU

### Executando os Testes

```bash
# Compilar
javac software/TesteSincronizacao.java

# Executar
java software.TesteSincronizacao

# Resultado esperado
RESULTADO FINAL: 5/5 testes passaram
✓ TODOS OS TESTES PASSARAM!
```

## Exemplos Práticos

### Exemplo 1: Produtor-Consumidor

```bash
javac exemplos/ExemploProdutorConsumidor.java
java exemplos.ExemploProdutorConsumidor
```

**Demonstra**:
- Buffer compartilhado de tamanho 3
- 2 produtores e 2 consumidores
- Bloqueio quando buffer cheio/vazio
- Sincronização com semáforos contadores e mutex

### Exemplo 2: Proteção de Seção Crítica

```bash
javac exemplos/ExemploSecaoCritica.java
java exemplos.ExemploSecaoCritica
```

**Demonstra**:
- Contador compartilhado
- Múltiplos escritores
- Exclusão mútua com mutex
- tryLock() para tentativas não-bloqueantes

## Arquitetura Atualizada

```
SISOP/
├── software/
│   ├── Semaforo.java           ← NOVO (Etapa 3)
│   ├── Mutex.java              ← NOVO (Etapa 3)
│   ├── TesteSincronizacao.java ← NOVO (Etapa 3)
│   ├── ProcessState.java       (estados: WAITING suportado)
│   ├── ProcessControlBlock.java
│   ├── ProcessManager.java
│   └── ...
├── exemplos/
│   ├── ExemploProdutorConsumidor.java  ← NOVO (Etapa 3)
│   ├── ExemploSecaoCritica.java        ← NOVO (Etapa 3)
│   └── ...
```

## Conceitos de Sistemas Operacionais Implementados

### 1. Semáforos (Dijkstra, 1965)
- Primitiva clássica de sincronização
- Operações atômicas P (down) e V (up)
- Base para implementar outros mecanismos

### 2. Mutex (Exclusão Mútua)
- Garante acesso exclusivo a recursos
- Propriedade de ownership
- Previne condições de corrida

### 3. Estados de Processo
- WAITING: Processo bloqueado aguardando recurso
- Transições: RUNNING → WAITING → READY

### 4. Filas de Espera
- FIFO (First In, First Out)
- Justiça: processos acordados na ordem de chegada

### 5. Sincronização
- Coordenação entre processos concorrentes
- Garantias de consistência
- Prevenção de deadlocks (parcial)

## Complexidade e Performance

### Operações de Semáforo
- `down()`: O(1) - verificação e possível bloqueio
- `up()`: O(1) - incremento e possível despertar
- `tryDown()`: O(1) - tentativa não-bloqueante

### Operações de Mutex
- `lock()`: O(1) - usa semáforo internamente
- `unlock()`: O(1) - libera e transfere ownership
- `tryLock()`: O(1) - tentativa não-bloqueante

### Memória
- Semáforo: ~200 bytes (nome, valor, fila, estatísticas)
- Mutex: ~250 bytes (semáforo interno + ownership)

## Limitações e Melhorias Futuras

### Limitações Atuais
1. **Sem integração completa com ProcessManager**
   - Processos bloqueados não são reescalonados automaticamente
   - Necessário simular acordar processos manualmente

2. **Sem prevenção de deadlock**
   - Sistema não detecta nem previne deadlocks
   - Responsabilidade do programador

3. **Sem prioridades**
   - Fila de espera é FIFO pura
   - Não há inversão de prioridade

### Melhorias Futuras (Etapa 4+)
1. **Integração com ProcessManager**
   - Processos bloqueados automaticamente saem da CPU
   - Processos acordados automaticamente entram na fila de prontos

2. **Detecção de Deadlock**
   - Algoritmo do Banqueiro
   - Grafo de alocação de recursos

3. **Variáveis de Condição**
   - wait() e signal() em condições específicas
   - Mais flexível que semáforos

4. **Read-Write Locks**
   - Múltiplos leitores simultâneos
   - Escritor exclusivo

## Uso Recomendado

### Quando usar Semáforo
- Controlar acesso a N recursos idênticos
- Sincronização entre processos (sinalização)
- Produtor-consumidor, leitores-escritores

### Quando usar Mutex
- Proteger seção crítica
- Garantir exclusão mútua
- Quando ownership é importante

### Padrão Comum
```java
// Proteger seção crítica
mutex.lock(processo);
try {
    // código da seção crítica
} finally {
    mutex.unlock(processo);
}

// Controlar pool de recursos
if (semaforo.tryDown()) {
    try {
        // usar recurso
    } finally {
        semaforo.up();
    }
}
```

## Referências e Leituras Adicionais

1. **Dijkstra, E. W.** (1965). "Cooperating sequential processes"
   - Introdução aos semáforos

2. **Silberschatz, Galvin, Gagne** - "Operating System Concepts"
   - Capítulos sobre sincronização de processos

3. **Tanenbaum, Bos** - "Modern Operating Systems"
   - Problemas clássicos de sincronização

## Conclusão

A **Etapa 3** completa a implementação das primitivas fundamentais de sincronização no SISOP. Com semáforos e mutex, o sistema agora pode:

✓ Coordenar acesso a recursos compartilhados  
✓ Garantir exclusão mútua em seções críticas  
✓ Resolver problemas clássicos (produtor-consumidor, etc.)  
✓ Evitar condições de corrida  
✓ Fornecer base para sincronização complexa  

O sistema está pronto para próximas etapas que podem incluir:
- Sistema de arquivos (Etapa 4)
- Interface gráfica (Etapa 5)
- Algoritmos avançados de escalonamento
- Gerenciamento de memória virtual

---

**Documentação criada em**: Novembro 2024  
**Etapa**: 3/5 - Sincronização entre Processos  
**Status**: ✓ COMPLETA
