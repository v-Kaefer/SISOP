# Instruções para Geração do PDF

## Arquivo Criado

✅ **README.html** - Documento consolidado em HTML pronto para conversão em PDF

## Como Converter para PDF

### Opção 1: Navegador (Mais Simples)

1. Abra `README.html` no seu navegador (Chrome, Firefox, Edge)
2. Pressione `Ctrl+P` (Windows/Linux) ou `Cmd+P` (Mac)
3. Selecione "Salvar como PDF" como destino
4. Ajuste margens se necessário (sugestão: padrão)
5. Clique em "Salvar"

### Opção 2: Linha de Comando

**Com chromium/chrome**:
```bash
chromium-browser --headless --disable-gpu --print-to-pdf=README.pdf README.html
```

**Com wkhtmltopdf**:
```bash
wkhtmltopdf README.html README.pdf
```

**Com pandoc**:
```bash
pandoc README.md -o README.pdf --pdf-engine=pdflatex
```

### Opção 3: Online

1. Acesse: https://www.sejda.com/html-to-pdf
2. Faça upload de `README.html`
3. Baixe o PDF gerado

## Conteúdo do Documento

O arquivo `README.md` (344 linhas) contém:

1. **Implementação** - Características T1, T2a, T2b
2. **Como Executar** - Comandos e requisitos
3. **Comandos Disponíveis** - Tabela de referência
4. **Programas** - Lista de programas testáveis
5. **Testes** (11 cenários) - Comandos, resultados esperados, logs
6. **Resumo dos Testes** - Tabela com status
7. **Bugs Corrigidos** - 4 bugs T2b documentados
8. **Arquivos do Projeto** - Estrutura do diretório
9. **Configuração** - Como alternar T2a/T2b

## Nota

O documento foi criado consolidando README_ENTREGA.md (331 linhas) e ROTEIRO_TESTES.md (368 linhas) em um único arquivo de 344 linhas, atendendo ao limite de 1000 linhas solicitado.

**Verificação**: Todos os testes foram executados e resultados documentados conforme comportamento real do sistema.
