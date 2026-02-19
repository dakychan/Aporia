package aporia.cc

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Independent logging system with clean output.
 * Bypasses Minecraft's logging wrapper for direct console output.
 */
object Logger {
    /**
     * Log levels with ANSI color codes.
     */
    enum class Level(val color: String, val prefix: String) {
        DEBUG("\u001B[36m", "[DEBUG]"),
        INFO("\u001B[32m", "[INFO]"),
        WARN("\u001B[33m", "[WARN]"),
        ERROR("\u001B[31m", "[ERROR]")
    }
    
    private const val RESET = "\u001B[0m"
    
    /**
     * Log file path. Using temporary path until OsManager is implemented.
     * TODO: Replace with OsManager.mainDirectory.resolve("aporia.log") when task 9 is complete
     */
    private val logFile: Path = Paths.get("logs", "aporia.log")
    
    init {
        /**
         * Ensure log directory exists.
         */
        try {
            Files.createDirectories(logFile.parent)
        } catch (e: IOException) {
            System.err.println("Failed to create log directory: ${e.message}")
        }
    }
    
    /**
     * Initialize logger and display ASCII logo.
     */
    fun initialize() {
        val logo = """
            
     █████╗ ██████╗  ██████╗ ██████╗ ██╗ █████╗     ██████╗ ██████╗
    ██╔══██╗██╔══██╗██╔═══██╗██╔══██╗██║██╔══██╗   ██╔════╝██╔════╝
    ███████║██████╔╝██║   ██║██████╔╝██║███████║   ██║     ██║     
    ██╔══██║██╔═══╝ ██║   ██║██╔══██╗██║██╔══██║   ██║     ██║     
    ██║  ██║██║     ╚██████╔╝██║  ██║██║██║  ██║██╗╚██████╗╚██████╗
    ╚═╝  ╚═╝╚═╝      ╚═════╝ ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝╚═╝ ╚═════╝ ╚═════╝
            
        """.trimIndent()
        
        System.out.println(logo)
    }
    
    /**
     * Log message with specified level.
     * Outputs directly to System.out bypassing Minecraft's logger.
     * Also writes to log file with full timestamp.
     * 
     * @param level Log level
     * @param message Message text
     * @param throwable Optional exception to log with stack trace
     */
    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        /**
         * Console output with time-only timestamp.
         */
        val timestamp = LocalTime.now()
            .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
        val consoleMessage = "${level.color}[$timestamp] ${level.prefix}$RESET $message"
        System.out.println(consoleMessage)
        
        /**
         * Print stack trace to console if exception provided.
         */
        if (throwable != null) {
            throwable.printStackTrace(System.out)
        }
        
        /**
         * File output with full date-time timestamp.
         */
        val fileTimestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        val fileMessage = buildString {
            append("[$fileTimestamp] ${level.prefix} $message")
            if (throwable != null) {
                append("\n")
                append(throwable.stackTraceToString())
            }
        }
        
        writeToFile(fileMessage)
    }
    
    /**
     * Write log entry to file.
     * 
     * @param message Log message
     */
    private fun writeToFile(message: String) {
        try {
            val logEntry = "$message\n"
            Files.writeString(logFile, logEntry, 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            System.err.println("Failed to write to log file: ${e.message}")
        }
    }
    
    /**
     * Log info level message.
     * 
     * @param message Message text
     */
    fun info(message: String) {
        log(Level.INFO, message)
    }
    
    /**
     * Log info level message with exception.
     * 
     * @param message Message text
     * @param throwable Exception to log
     */
    fun info(message: String, throwable: Throwable) {
        log(Level.INFO, message, throwable)
    }
    
    /**
     * Log warning level message.
     * 
     * @param message Message text
     */
    fun warn(message: String) {
        log(Level.WARN, message)
    }
    
    /**
     * Log warning level message with exception.
     * 
     * @param message Message text
     * @param throwable Exception to log
     */
    fun warn(message: String, throwable: Throwable) {
        log(Level.WARN, message, throwable)
    }
    
    /**
     * Log error level message.
     * 
     * @param message Message text
     */
    fun error(message: String) {
        log(Level.ERROR, message)
    }
    
    /**
     * Log error level message with exception.
     * 
     * @param message Message text
     * @param throwable Exception to log
     */
    fun error(message: String, throwable: Throwable) {
        log(Level.ERROR, message, throwable)
    }
    
    /**
     * Log debug level message.
     * 
     * @param message Message text
     */
    fun debug(message: String) {
        log(Level.DEBUG, message)
    }
    
    /**
     * Log debug level message with exception.
     * 
     * @param message Message text
     * @param throwable Exception to log
     */
    fun debug(message: String, throwable: Throwable) {
        log(Level.DEBUG, message, throwable)
    }
}
