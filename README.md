# SISOP - Virtual Machine Simulator

**T1 - SISOP** - Operating Systems Course Project  
*PUCRS - Escola Politécnica*

[![CI/CD Pipeline](https://github.com/v-Kaefer/SISOP/actions/workflows/ci.yml/badge.svg)](https://github.com/v-Kaefer/SISOP/actions/workflows/ci.yml)

## 🚀 Overview

This project implements a virtual machine simulator for educational purposes in operating systems concepts. The simulator includes:

- **Hardware Layer**: Memory, CPU, and instruction set architecture
- **Operating System Layer**: Interrupt handling and system call management  
- **Built-in Programs**: Factorial, Fibonacci, sorting algorithms, and more
- **Comprehensive Testing**: Automated validation of all components

## 🏗️ Architecture

### Core Components

- **`Sistema.java`**: Main virtual machine implementation containing:
  - `Memory`: Virtual memory management
  - `CPU`: Instruction execution engine with opcodes
  - `Utilities`: Memory loading and debugging tools
  - `Programs`: Built-in program library
  - `InterruptHandling`: System interrupt management
  - `SysCallHandling`: System call processing

- **`SistemaTest.java`**: Comprehensive test suite validating:
  - System initialization and memory allocation
  - CPU execution and program loading
  - End-to-end program execution (factorial calculation)
  - System call handling and termination

## 🧪 Testing & Quality Assurance

### Automated CI/CD Pipeline

The project includes a comprehensive GitHub Actions CI/CD pipeline that automatically:

#### 🌟 **Multi-Java Version Support**
- Tests on Java 11, 17, and 21 for maximum compatibility
- Ensures forward and backward compatibility

#### ⚡ **Fast Feedback Loop**  
- Immediate compilation error detection
- Fail-fast approach for quick developer feedback

#### 🧪 **Comprehensive Testing**
- **Unit Tests**: 6 focused tests covering core functionality
- **Integration Tests**: Full program execution validation
- **Quality Gates**: Code formatting and security checks

#### 📊 **Detailed Reporting**
- Test results and coverage metrics
- Build artifacts and test reports
- Comprehensive project analysis

#### 🛡️ **Quality Assurance**
- Static code analysis and formatting checks
- Security vulnerability scanning
- File structure validation

### Running Tests Locally

```bash
# Quick test (manual)
javac *.java && java SistemaTest

# Comprehensive test (using provided script)
./test-runner.sh
```

### Pipeline Features

| Feature | Description | Status |
|---------|-------------|--------|
| **Multi-Java Testing** | Java 11, 17, 21 compatibility | ✅ |
| **Build Validation** | Compilation across all versions | ✅ |
| **Unit Testing** | Core component validation | ✅ |
| **Integration Testing** | End-to-end execution tests | ✅ |
| **Quality Gates** | Code formatting and security | ✅ |
| **Artifact Collection** | Build outputs and test reports | ✅ |
| **Dependency Caching** | Faster build performance | ✅ |
| **Security Scanning** | Basic vulnerability detection | ✅ |

## 🚀 Quick Start

### Prerequisites
- Java 11+ (tested on 11, 17, 21)
- Git

### Running the Virtual Machine

```bash
# Clone the repository
git clone https://github.com/v-Kaefer/SISOP.git
cd SISOP

# Compile and run
javac Sistema.java
java Sistema
```

### Running Tests

```bash
# Compile test suite
javac *.java

# Run all tests
java SistemaTest

# Run comprehensive local testing
./test-runner.sh
```

## 📊 Supported Programs

The virtual machine includes several built-in programs:

- **`fatorialV2`**: Calculates factorial with optimized implementation
- **`fatorial`**: Basic factorial calculation
- **`fibonacci10`**: Fibonacci sequence generation
- **`progMinimo`**: Minimal program example
- **`PC`**: Bubble sort implementation
- **Additional programs**: Various algorithm implementations

## 🔧 Development Workflow

### Continuous Integration

Every push and pull request automatically triggers:

1. **Build Stage**: Compilation validation across Java versions
2. **Test Stage**: Unit and integration test execution  
3. **Quality Stage**: Code analysis and formatting checks
4. **Reporting Stage**: Artifact collection and detailed reports

### Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes with tests
4. Push and create a pull request
5. Automated pipeline validates your changes

## 📈 Pipeline Status

The CI/CD pipeline provides:

- ✅ **Automated Quality Gates**: Prevents broken code from merging
- 📊 **Comprehensive Reporting**: Detailed test results and metrics  
- 🔄 **Continuous Validation**: Every change is automatically tested
- 🛡️ **Security Scanning**: Basic vulnerability detection
- 📦 **Artifact Management**: Build outputs and test reports preserved

## 🎯 Future Enhancements

- 📊 **Code Coverage**: Integration with JaCoCo for detailed metrics
- 🔍 **Advanced Static Analysis**: SpotBugs, PMD, or SonarQube integration
- 📝 **Documentation**: Automated JavaDoc generation
- 🐳 **Containerization**: Docker-based testing environments
- 🌐 **Performance Testing**: Benchmark tests for VM performance

---

**Course**: Sistemas Operacionais - PUCRS  
**Professor**: Fernando Dotti  
**Project**: Virtual Machine Simulator with Comprehensive CI/CD Pipeline
