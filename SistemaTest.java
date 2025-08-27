import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringWriter;
import java.io.PrintWriter;

/**
 * Basic test suite for Sistema virtual machine simulator
 * Tests core functionality including hardware initialization, program execution, and system calls
 */
public class SistemaTest {
    
    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;
    
    public static void main(String[] args) {
        System.out.println("=== SISOP Virtual Machine Test Suite ===\n");
        
        // Run all tests
        testSystemInitialization();
        testMemoryAllocation();
        testCPUExecution();
        testProgramLoading();
        testFactorialProgram();
        testSysCallHandling();
        
        // Print summary
        System.out.println("\n=== Test Results Summary ===");
        System.out.println("Total tests run: " + testsRun);
        System.out.println("Tests passed: " + testsPassed);
        System.out.println("Tests failed: " + testsFailed);
        
        if (testsFailed > 0) {
            System.out.println("TESTS FAILED");
            System.exit(1);
        } else {
            System.out.println("ALL TESTS PASSED");
            System.exit(0);
        }
    }
    
    private static void testSystemInitialization() {
        test("System Initialization", () -> {
            Sistema sistema = new Sistema(1024);
            assertNotNull(sistema, "Sistema should be initialized");
            return true;
        });
    }
    
    private static void testMemoryAllocation() {
        test("Memory Allocation", () -> {
            Sistema sistema = new Sistema(512);
            assertNotNull(sistema, "Sistema with 512 memory words should be initialized");
            return true;
        });
    }
    
    private static void testCPUExecution() {
        test("CPU Execution Setup", () -> {
            Sistema sistema = new Sistema(1024);
            // Test that we can access the hardware components
            assertNotNull(sistema, "Sistema should be properly initialized with CPU");
            return true;
        });
    }
    
    private static void testProgramLoading() {
        test("Program Loading", () -> {
            Sistema sistema = new Sistema(1024);
            Sistema.Programs programs = sistema.new Programs();
            
            // Test retrieving a known program
            Sistema.Word[] fatorialProgram = programs.retrieveProgram("fatorialV2");
            assertNotNull(fatorialProgram, "Should be able to retrieve fatorialV2 program");
            assertTrue(fatorialProgram.length > 0, "Program should have instructions");
            
            return true;
        });
    }
    
    private static void testFactorialProgram() {
        test("Factorial Program Execution", () -> {
            // Capture output to verify execution
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PrintStream originalOut = System.out;
            System.setOut(new PrintStream(outputStream));
            
            try {
                Sistema sistema = new Sistema(1024);
                sistema.run();  // This runs the fatorialV2 program
                
                String output = outputStream.toString();
                assertTrue(output.contains("OUT:   120"), "Factorial of 5 should be 120");
                assertTrue(output.contains("SYSCALL STOP"), "Program should terminate with SYSCALL STOP");
                
                return true;
            } finally {
                System.setOut(originalOut);
            }
        });
    }
    
    private static void testSysCallHandling() {
        test("System Call Handling", () -> {
            // Test that system can handle syscalls without crashing
            Sistema sistema = new Sistema(1024);
            Sistema.SysCallHandling sysCall = sistema.new SysCallHandling(sistema.new HW(1024));
            
            // Test stop syscall
            sysCall.stop();  // Should not throw exception
            
            return true;
        });
    }
    
    // Test utility methods
    private static void test(String testName, TestRunner runner) {
        testsRun++;
        System.out.print("Testing " + testName + "... ");
        
        try {
            boolean result = runner.run();
            if (result) {
                System.out.println("PASSED");
                testsPassed++;
            } else {
                System.out.println("FAILED");
                testsFailed++;
            }
        } catch (Exception e) {
            System.out.println("FAILED (Exception: " + e.getMessage() + ")");
            testsFailed++;
            e.printStackTrace();
        }
    }
    
    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
    
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("Assertion failed: " + message);
        }
    }
    
    @FunctionalInterface
    private interface TestRunner {
        boolean run() throws Exception;
    }
}