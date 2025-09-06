# SISOP
T1 - SISOP

Estrutura Modularizada

```
SISOP
├── Sistema.java                # Main (instancia HW, SO, Programs) + run()
├── hardware/
│   ├── CPU.java
│   ├── HW.java
│   ├── Interrupts.java
│   ├── Memory.java
│   ├── Opcode.java
│   └── Word.java
├── memory/                     # Gerenciador de Memória com Paginação
│   ├── GerenciadorMemoria.java
│   ├── MemoryManagerBridge.java
│   ├── PosicaoDeMemoria.java
│   ├── TesteGerenciadorMemoria.java
│   └── TesteIntegracao.java
├── programs/
│   ├── Program.java
│   └── Programs.java
└── software/
    ├── InterruptHandling.java
    ├── SO.java
    ├── SysCallHandling.java
    └── Utilities.java
```


# TO-DO

[x] Gerencia de Memória
[ ] Gerencia de Processos
[ ] Escalonamento -> Round-Robin (definido pelo prof)

* Para o controle do tempo, vamos usar os ciclos de CPU.

Melhor metodologia para desenvolver o trabalho:

* Gerente de Memória -> Gerente de Processos -> Escalonamento

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