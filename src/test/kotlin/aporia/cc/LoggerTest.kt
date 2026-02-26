package aporia.cc

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Tests for Logger console and file output format.
 * Validates: Requirements 8.1, 8.2
 */
class LoggerTest {
    private val originalOut = System.out
    private val outputStream = ByteArrayOutputStream()
    private val logFile = Paths.get("logs", "aporia.log")
    
    @BeforeEach
    fun setUp() {
        System.setOut(PrintStream(outputStream))
        
        // Clean up log file before each test
        if (Files.exists(logFile)) {
            Files.delete(logFile)
        }
    }
    
    @AfterEach
    fun tearDown() {
        System.setOut(originalOut)
        
        // Clean up log file after each test
        if (Files.exists(logFile)) {
            Files.delete(logFile)
        }
    }
    
    /**
     * Test that console output follows the format: [HH:MM:SS] [LEVEL] message
     * Validates: Requirements 8.2
     */
    @Test
    fun testConsoleOutputFormat() {
        Logger.info("test message")
        
        val output = outputStream.toString()
        
        // Remove ANSI color codes for easier testing
        val cleanOutput = output.replace("\u001B\\[[0-9;]+m".toRegex(), "")
        
        // Verify format: [HH:MM:SS] [INFO] test message
        val pattern = "\\[\\d{2}:\\d{2}:\\d{2}\\] \\[INFO\\] test message".toRegex()
        assert(pattern.containsMatchIn(cleanOutput)) {
            "Console output does not match expected format. Got: $cleanOutput"
        }
    }
    
    /**
     * Test that file output follows the format: [YYYY-MM-DD HH:MM:SS] [LEVEL] message
     * Validates: Requirements 8.1
     */
    @Test
    fun testFileOutputFormat() {
        Logger.info("file test message")
        
        // Wait a bit to ensure file write completes
        Thread.sleep(100)
        
        assert(Files.exists(logFile)) {
            "Log file was not created"
        }
        
        val fileContent = Files.readString(logFile)
        
        // Verify format: [YYYY-MM-DD HH:MM:SS] [INFO] file test message
        val pattern = "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\] \\[INFO\\] file test message".toRegex()
        assert(pattern.containsMatchIn(fileContent)) {
            "File output does not match expected format. Got: $fileContent"
        }
    }
    
    /**
     * Test that file output does not contain ANSI color codes.
     * Validates: Requirements 8.1
     */
    @Test
    fun testFileOutputNoAnsiCodes() {
        Logger.info("color test")
        
        // Wait a bit to ensure file write completes
        Thread.sleep(100)
        
        val fileContent = Files.readString(logFile)
        
        // Verify no ANSI codes in file
        assert(!fileContent.contains("\u001B[")) {
            "File output should not contain ANSI color codes. Got: $fileContent"
        }
    }
    
    /**
     * Test that all log levels produce correct format.
     */
    @Test
    fun testAllLogLevels() {
        val levels = listOf(
            "INFO" to { Logger.info("info test") },
            "WARN" to { Logger.warn("warn test") },
            "ERROR" to { Logger.error("error test") },
            "DEBUG" to { Logger.debug("debug test") }
        )
        
        for ((levelName, logFunction) in levels) {
            outputStream.reset()
            logFunction()
            
            val output = outputStream.toString()
            val cleanOutput = output.replace("\u001B\\[[0-9;]+m".toRegex(), "")
            
            // Verify format includes the level
            assert(cleanOutput.contains("[$levelName]")) {
                "Output for $levelName does not contain level marker. Got: $cleanOutput"
            }
            
            // Verify timestamp format
            val timestampPattern = "\\[\\d{2}:\\d{2}:\\d{2}\\]".toRegex()
            assert(timestampPattern.containsMatchIn(cleanOutput)) {
                "Output for $levelName does not contain valid timestamp. Got: $cleanOutput"
            }
        }
    }
    
    /**
     * Test that all log levels write to file correctly.
     * Validates: Requirements 8.1
     */
    @Test
    fun testAllLogLevelsInFile() {
        Logger.info("info test")
        Logger.warn("warn test")
        Logger.error("error test")
        Logger.debug("debug test")
        
        // Wait a bit to ensure file writes complete
        Thread.sleep(100)
        
        val fileContent = Files.readString(logFile)
        
        // Verify all levels are in the file
        assert(fileContent.contains("[INFO] info test")) {
            "File does not contain INFO log"
        }
        assert(fileContent.contains("[WARN] warn test")) {
            "File does not contain WARN log"
        }
        assert(fileContent.contains("[ERROR] error test")) {
            "File does not contain ERROR log"
        }
        assert(fileContent.contains("[DEBUG] debug test")) {
            "File does not contain DEBUG log"
        }
    }
    
    /**
     * Test that ANSI colors are applied to log levels.
     */
    @Test
    fun testAnsiColorsApplied() {
        Logger.info("color test")
        
        val output = outputStream.toString()
        
        // Verify ANSI color codes are present
        assert(output.contains("\u001B[")) {
            "Output does not contain ANSI color codes"
        }
        
        // Verify reset code is present
        assert(output.contains("\u001B[0m")) {
            "Output does not contain ANSI reset code"
        }
    }
    
    /**
     * Test that exception logging prints stack trace to console.
     * Validates: Requirements 8.4
     */
    @Test
    fun testExceptionLoggingToConsole() {
        val exception = RuntimeException("Test exception")
        Logger.error("Error occurred", exception)
        
        val output = outputStream.toString()
        
        // Verify message is present
        assert(output.contains("Error occurred")) {
            "Console output does not contain error message"
        }
        
        // Verify exception class name is present
        assert(output.contains("RuntimeException")) {
            "Console output does not contain exception class name"
        }
        
        // Verify exception message is present
        assert(output.contains("Test exception")) {
            "Console output does not contain exception message"
        }
        
        // Verify stack trace is present (should contain "at " for stack frames)
        assert(output.contains("at ")) {
            "Console output does not contain stack trace"
        }
    }
    
    /**
     * Test that exception logging writes stack trace to file.
     * Validates: Requirements 8.4
     */
    @Test
    fun testExceptionLoggingToFile() {
        val exception = IllegalArgumentException("Invalid argument")
        Logger.warn("Warning with exception", exception)
        
        // Wait a bit to ensure file write completes
        Thread.sleep(100)
        
        val fileContent = Files.readString(logFile)
        
        // Verify message is present
        assert(fileContent.contains("Warning with exception")) {
            "File does not contain warning message"
        }
        
        // Verify exception class name is present
        assert(fileContent.contains("IllegalArgumentException")) {
            "File does not contain exception class name"
        }
        
        // Verify exception message is present
        assert(fileContent.contains("Invalid argument")) {
            "File does not contain exception message"
        }
        
        // Verify stack trace is present
        assert(fileContent.contains("at ")) {
            "File does not contain stack trace"
        }
    }
    
    /**
     * Test that all log levels support exception logging.
     * Validates: Requirements 8.4
     */
    @Test
    fun testAllLogLevelsWithException() {
        val exception = Exception("Test exception")
        
        val levels = listOf(
            "INFO" to { Logger.info("info with exception", exception) },
            "WARN" to { Logger.warn("warn with exception", exception) },
            "ERROR" to { Logger.error("error with exception", exception) },
            "DEBUG" to { Logger.debug("debug with exception", exception) }
        )
        
        for ((levelName, logFunction) in levels) {
            outputStream.reset()
            logFunction()
            
            val output = outputStream.toString()
            
            // Verify message and exception are both present
            assert(output.contains("$levelName".lowercase() + " with exception")) {
                "Output for $levelName does not contain message"
            }
            assert(output.contains("Test exception")) {
                "Output for $levelName does not contain exception message"
            }
        }
    }
    
    /**
     * Test that exception logging preserves timestamp format.
     * Validates: Requirements 8.1, 8.4
     */
    @Test
    fun testExceptionLoggingPreservesFormat() {
        val exception = Exception("Format test")
        Logger.info("Message with exception", exception)
        
        // Wait a bit to ensure file write completes
        Thread.sleep(100)
        
        val fileContent = Files.readString(logFile)
        
        // Verify timestamp format is preserved
        val timestampPattern = "\\[\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\]".toRegex()
        assert(timestampPattern.containsMatchIn(fileContent)) {
            "File output with exception does not contain valid timestamp. Got: $fileContent"
        }
        
        // Verify level marker is present
        assert(fileContent.contains("[INFO]")) {
            "File output with exception does not contain level marker"
        }
    }
}

