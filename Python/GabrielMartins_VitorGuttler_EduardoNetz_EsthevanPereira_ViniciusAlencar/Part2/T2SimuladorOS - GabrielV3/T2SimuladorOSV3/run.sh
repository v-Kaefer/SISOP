#!/bin/bash

# Script de execução do Simulador de Sistema Operacional
# PUCRS - Engenharia de Software - T2

echo "============================================"
echo "Simulador de Sistema Operacional - PUCRS T2"
echo "============================================"
echo ""

# Verifica se Python 3 está instalado
if ! command -v python3 &> /dev/null; then
    echo "ERRO: Python 3 não está instalado."
    echo "Por favor, instale Python 3 para executar o simulador."
    exit 1
fi

# Verifica a versão do Python
PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
echo "Python detectado: $PYTHON_VERSION"
echo ""

# Verifica se o arquivo principal existe
if [ ! -f "sistema_os.py" ]; then
    echo "ERRO: Arquivo sistema_os.py não encontrado."
    echo "Certifique-se de estar no diretório correto."
    exit 1
fi

# Verifica o modo de execução
if [ "$1" == "test" ]; then
    echo "Executando em MODO DE TESTE AUTOMATIZADO..."
    echo "Os testes serão executados automaticamente."
    echo ""
    python3 sistema_os.py test
elif [ "$1" == "help" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo "Uso: ./run.sh [modo]"
    echo ""
    echo "Modos disponíveis:"
    echo "  (nenhum)  - Executa em modo interativo (shell)"
    echo "  test      - Executa testes automatizados"
    echo "  help      - Exibe esta mensagem de ajuda"
    echo ""
    echo "Comandos do shell interativo:"
    echo "  new <prog>       - Cria um novo processo"
    echo "  ps               - Lista todos os processos"
    echo "  rm <id>          - Remove um processo"
    echo "  dump <id>        - Exibe informações de um processo"
    echo "  dumpm <ini> <fim> - Exibe conteúdo da memória física"
    echo "  sysstate         - Exibe estado completo do sistema"
    echo "  traceon          - Ativa modo trace (debug)"
    echo "  traceoff         - Desativa modo trace"
    echo "  exit             - Encerra o simulador"
    echo ""
    echo "Programas disponíveis:"
    echo "  fibonacci, fatorial, fatorialV2, PB, PC, progMinimo"
    echo ""
else
    echo "Executando em MODO INTERATIVO..."
    echo "Digite 'help' no shell para ver os comandos disponíveis."
    echo ""
    python3 sistema_os.py
fi

echo ""
echo "Simulador encerrado."

