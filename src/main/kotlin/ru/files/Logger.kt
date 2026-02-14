package ru.files

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logger {
    
    enum class Level(val color: String, val prefix: String) {
        DEBUG("\u001B[36m", "[DEBUG]"),
        INFO("\u001B[32m", "[INFO]"),
        WARN("\u001B[33m", "[WARN]"),
        ERROR("\u001B[31m", "[ERROR]")
    }
    
    private const val RESET = "\u001B[0m"
    private const val MAGENTA = "\u001B[35m"
    private const val ASCII_BANNER = """
    █████╗ ██████╗  ██████╗ ██████╗ ██╗ █████╗     ██████╗ ██████╗
   ██╔══██╗██╔══██╗██╔═══██╗██╔══██╗██║██╔══██╗   ██╔════╝██╔════╝
   ███████║██████╔╝██║   ██║██████╔╝██║███████║   ██║     ██║     
   ██╔══██║██╔═══╝ ██║   ██║██╔══██╗██║██╔══██║   ██║     ██║     
   ██║  ██║██║     ╚██████╔╝██║  ██║██║██║  ██║██╗╚██████╗╚██████╗
   ╚═╝  ╚═╝╚═╝      ╚═════╝ ╚═╝  ╚═╝╚═╝╚═╝  ╚═╝╚═╝ ╚═════╝ ╚═════╝
"""
    
    private val logFile: Path = PathResolver.mainDirectory.resolve("aporia.log")
    private var initialized = false
    
    fun initialize() {
        if (!initialized) {
            printBanner()
            initialized = true
            ensureLogFileExists()
        }
    }
    
    private fun printBanner() {
        println("$MAGENTA$ASCII_BANNER$RESET")
        println("${MAGENTA}loading...$RESET")
    }
    
    private fun ensureLogFileExists() {
        try {
            if (!Files.exists(logFile)) {
                Files.createFile(logFile)
            }
        } catch (e: IOException) {
            System.err.println("Failed to create log file: ${e.message}")
        }
    }
    
    fun debug(message: String) {
        log(Level.DEBUG, message)
    }
    
    fun info(message: String) {
        log(Level.INFO, message)
    }
    
    fun warn(message: String) {
        log(Level.WARN, message)
    }
    
    fun error(message: String) {
        log(Level.ERROR, message)
    }
    
    fun error(message: String, throwable: Throwable) {
        log(Level.ERROR, message, throwable)
    }
    
    private fun log(level: Level, message: String, throwable: Throwable? = null) {
        val consoleMessage = "${level.color}${level.prefix}$RESET $message"
        println(consoleMessage)
        
        if (throwable != null) {
            throwable.printStackTrace()
        }
        
        val fileMessage = buildString {
            append("${level.prefix} $message")
            if (throwable != null) {
                append("\n")
                append(throwable.stackTraceToString())
            }
        }
        
        writeToFile(fileMessage)
    }
    
    private fun writeToFile(message: String) {
        try {
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val logEntry = "[$timestamp] $message\n"
            Files.writeString(logFile, logEntry, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        } catch (e: IOException) {
            System.err.println("Failed to write to log file: ${e.message}")
        }
    }
}
