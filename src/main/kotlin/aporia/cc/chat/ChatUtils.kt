package aporia.cc.chat

import net.minecraft.client.Minecraft
import net.minecraft.client.GuiMessage
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSink
import org.lwjgl.glfw.GLFW
import ru.command.Command
import ru.command.CommandRegistry

/**
 * Unified chat and command management system for Aporia.cc.
 * Handles command execution, message formatting, and chat copy functionality.
 */
object ChatUtils {
    
    /**
     * Message type enum for colored output.
     */
    enum class MessageType {
        /** Red color (§c) for errors */
        ERROR,
        /** Yellow color (§e) for warnings and info */
        WARNING,
        /** Green color (§a) for success messages */
        SUCCESS
    }
    
    /** Command registry */
    private val registry: CommandRegistry = CommandRegistry()
    
    /** Command prefix */
    private var prefix: String = "^"
    
    /** Command aliases */
    private val aliases: MutableMap<String, String> = mutableMapOf()
    
    /** Show notification when copying chat */
    @JvmField
    var notifyOnCopy: Boolean = true
    
    /** Highlight copied messages */
    @JvmField
    var highlightCopied: Boolean = true
    
    /** Use gradient prefix */
    @JvmField
    var useGradientPrefix: Boolean = true
    
    /** Use random gradient colors */
    @JvmField
    var useRandomGradient: Boolean = false
    
    /** Enable anti-spam message stacking */
    @JvmField
    var antiSpamEnabled: Boolean = true
    
    /**
     * Static gradient prefix for "Aporia.cc ->"
     * White to dark gray gradient using &#RRGGBB format
     */
    private val GRADIENT_PREFIX_STATIC = 
        "&#FFFFFFA" +      // A - white
        "&#EBEBEBp" +      // p
        "&#D6D6D6o" +      // o
        "&#C2C2C2r" +      // r
        "&#AEAEAEi" +      // i
        "&#999999a" +      // a
        "&#858585." +      // .
        "&#707070c" +      // c
        "&#5C5C5Cc" +      // c
        "&#474747 " +      // space
        "&#333333-" +      // -
        "&#1F1F1F>"        // >
    
    /**
     * Gradient color presets for random mode
     */
    private val GRADIENT_PRESETS = listOf(
        // Cyan to blue
        listOf(0x00FFFF, 0x0080FF, 0x0000FF),
        // Purple to pink
        listOf(0x8000FF, 0xFF00FF, 0xFF0080),
        // Green to cyan
        listOf(0x00FF00, 0x00FF80, 0x00FFFF),
        // Orange to red
        listOf(0xFF8000, 0xFF4000, 0xFF0000),
        // Yellow to orange
        listOf(0xFFFF00, 0xFFCC00, 0xFF8000),
        // Pink to purple
        listOf(0xFF80FF, 0xFF00FF, 0x8000FF)
    )
    
    /**
     * Initialize ChatUtils and register all commands.
     */
    fun initialize() {
        registerCommand(ru.command.commands.ConfigCommand())
        registerCommand(ru.command.commands.AliasCommand())
        registerCommand(ru.command.commands.FriendCommand())
        registerCommand(ru.command.commands.InfoCommand())
        registerCommand(ru.command.commands.PrefixCommand())
        registerCommand(ru.command.commands.HelpCommand(registry))
        
        ru.files.Logger.info("ChatUtils initialized with ${registry.getAllCommands().size} commands")
    }
    
    /**
     * Handle incoming chat message.
     * 
     * @param message The chat message
     * @return true if message was a command, false otherwise
     */
    fun handleChatMessage(message: String): Boolean {
        if (!message.startsWith(prefix)) {
            return false
        }
        
        var commandText = message.substring(prefix.length).trim()
        
        if (commandText.isEmpty()) {
            return true
        }
        
        commandText = expandAlias(commandText)
        
        val parts = commandText.split("\\s+".toRegex())
        val commandName = parts[0].lowercase()
        val args = parts.drop(1).toTypedArray()
        
        val command = registry.getCommand(commandName)
        
        if (command == null) {
            sendMessage("Неизвестная команда: $commandName", MessageType.ERROR)
            return true
        }
        
        try {
            command.execute(args)
        } catch (e: Exception) {
            sendMessage("Ошибка выполнения команды: ${e.message}", MessageType.ERROR)
            e.printStackTrace()
        }
        
        return true
    }
    
    /**
     * Send formatted message to player.
     * 
     * @param message The message text
     * @param type The message type
     */
    fun sendMessage(message: String, type: MessageType) {
        val formatted = formatMessage(message, type)
        val mc = Minecraft.getInstance()
        mc.player?.displayClientMessage(Component.literal(formatted), false)
    }
    
    /**
     * Register a command.
     * 
     * @param command The command to register
     */
    fun registerCommand(command: Command) {
        registry.register(command)
    }
    
    /**
     * Copy chat message to clipboard or open chat.
     * 
     * @param parts The message lines to copy
     * @param button The mouse button that was pressed
     */
    @JvmStatic
    fun copyMessage(parts: List<GuiMessage.Line>, button: Int) {
        val content = buildString {
            val visitor = FormattedCharSink { _, _, codePoint ->
                appendCodePoint(codePoint)
                true
            }
            for (line in parts) {
                line.content().accept(visitor)
            }
        }
        
        val mc = Minecraft.getInstance()
        
        if (isAnyPressed(GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT) 
            && button == GLFW.GLFW_MOUSE_BUTTON_1) {
            mc.keyboardHandler.clipboard = content
            if (notifyOnCopy) {
                sendMessage("Строка скопирована", MessageType.SUCCESS)
            }
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_2) {
            mc.setScreen(net.minecraft.client.gui.screens.ChatScreen(content, false))
        }
    }
    
    /**
     * Check if any of the given keys are currently pressed.
     * 
     * @param keys The GLFW key codes to check
     * @return true if any key is pressed
     */
    private fun isAnyPressed(vararg keys: Int): Boolean {
        val mc = Minecraft.getInstance()
        return keys.any { 
            GLFW.glfwGetKey(mc.window.handle(), it) == GLFW.GLFW_PRESS 
        }
    }
    
    /**
     * Format message with color code and prefix.
     * 
     * @param text The message text
     * @param type The message type
     * @return Formatted message string
     */
    private fun formatMessage(text: String, type: MessageType): String {
        val colorCode = when (type) {
            MessageType.ERROR -> "§c"
            MessageType.WARNING -> "§e"
            MessageType.SUCCESS -> "§a"
        }
        
        val prefixText = if (useGradientPrefix) {
            if (useRandomGradient) {
                generateRandomGradientPrefix()
            } else {
                GRADIENT_PREFIX_STATIC
            }
        } else {
            "§eAporia.cc ->"
        }
        
        return "$prefixText $colorCode$text"
    }
    
    /**
     * Generate random gradient prefix.
     * 
     * @return Gradient formatted prefix string
     */
    private fun generateRandomGradientPrefix(): String {
        val preset = GRADIENT_PRESETS.random()
        val text = "Aporia.cc ->"
        return applyGradient(text, preset)
    }
    
    /**
     * Apply gradient to text using &#RRGGBB format.
     * 
     * @param text The text to apply gradient to
     * @param colors List of RGB colors for gradient
     * @return Gradient formatted text
     */
    private fun applyGradient(text: String, colors: List<Int>): String {
        if (colors.isEmpty() || text.isEmpty()) return text
        
        val result = StringBuilder()
        val steps = text.length - 1
        
        for (i in text.indices) {
            val char = text[i]
            val progress = if (steps > 0) i.toFloat() / steps else 0f
            val color = interpolateColors(colors, progress)
            
            val hex = String.format("%06X", color)
            result.append("&#$hex$char")
        }
        
        return result.toString()
    }
    
    /**
     * Interpolate between multiple colors.
     * 
     * @param colors List of colors to interpolate
     * @param progress Progress from 0.0 to 1.0
     * @return Interpolated RGB color
     */
    private fun interpolateColors(colors: List<Int>, progress: Float): Int {
        if (colors.size == 1) return colors[0]
        
        val scaledProgress = progress * (colors.size - 1)
        val index = scaledProgress.toInt().coerceIn(0, colors.size - 2)
        val localProgress = scaledProgress - index
        
        val color1 = colors[index]
        val color2 = colors[index + 1]
        
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val r = (r1 + (r2 - r1) * localProgress).toInt()
        val g = (g1 + (g2 - g1) * localProgress).toInt()
        val b = (b1 + (b2 - b1) * localProgress).toInt()
        
        return (r shl 16) or (g shl 8) or b
    }
    
    /**
     * Expand command alias if exists.
     * 
     * @param input The command input
     * @return Expanded command or original input
     */
    private fun expandAlias(input: String): String {
        val parts = input.split("\\s+".toRegex(), limit = 2)
        val firstWord = parts[0].lowercase()
        
        if (aliases.containsKey(firstWord)) {
            val expansion = aliases[firstWord]!!
            return if (parts.size > 1) {
                "$expansion ${parts[1]}"
            } else {
                expansion
            }
        }
        
        return input
    }
    
    /**
     * Set command prefix.
     * 
     * @param newPrefix The new prefix
     */
    fun setPrefix(newPrefix: String) {
        prefix = newPrefix
    }
    
    /**
     * Get current command prefix.
     * 
     * @return The current prefix
     */
    fun getPrefix(): String {
        return prefix
    }
    
    /**
     * Add command alias.
     * 
     * @param alias The alias name
     * @param command The command to alias
     */
    fun addAlias(alias: String, command: String) {
        aliases[alias.lowercase()] = command
    }
    
    /**
     * Remove command alias.
     * 
     * @param alias The alias to remove
     */
    fun removeAlias(alias: String) {
        aliases.remove(alias.lowercase())
    }
    
    /**
     * Get all aliases.
     * 
     * @return Map of aliases
     */
    fun getAliases(): Map<String, String> {
        return aliases.toMap()
    }
    
    /**
     * Message stack entry for anti-spam system.
     * 
     * @property sender The message sender
     * @property message The message content
     * @property count Number of times this message has been sent
     * @property lastTime Timestamp of the last occurrence (milliseconds)
     */
    data class MessageStack(
        val sender: String,
        val message: String,
        var count: Int,
        var lastTime: Long
    )
    
    /**
     * Recent messages for stacking.
     */
    private val messageStacks: MutableList<MessageStack> = mutableListOf()
    
    /**
     * Maximum time between messages to stack (milliseconds).
     * Messages older than this will be removed from the stack.
     */
    private const val STACK_TIMEOUT = 5000L
    
    /**
     * Process incoming chat message for stacking.
     * 
     * This method checks if the message should be stacked with a previous
     * message from the same sender. If a match is found, the count is
     * incremented and a formatted message with the count is returned.
     * If no match is found, a new stack entry is created.
     * 
     * @param sender Message sender
     * @param message Message content
     * @return Formatted message with stack count, or null if this is a duplicate
     */
    fun processAntiSpam(sender: String, message: String): String? {
        if (!antiSpamEnabled) {
            return "$sender -> $message"
        }
        
        val currentTime = System.currentTimeMillis()
        
        messageStacks.removeIf { currentTime - it.lastTime > STACK_TIMEOUT }
        
        val existing = messageStacks.find { 
            it.sender == sender && it.message == message 
        }
        
        if (existing != null) {
            existing.count++
            existing.lastTime = currentTime
            return "$sender -> $message (x${existing.count})"
        } else {
            messageStacks.add(MessageStack(sender, message, 1, currentTime))
            return "$sender -> $message"
        }
    }
    
    /**
     * Clear all message stacks.
     * 
     * This can be used to reset the anti-spam system.
     */
    fun clearAntiSpam() {
        messageStacks.clear()
    }
    
    /**
     * Get the current number of active message stacks.
     * 
     * @return Number of active stacks
     */
    fun getStackCount(): Int {
        return messageStacks.size
    }
    
    /**
     * Get whether anti-spam is enabled.
     * 
     * @return true if anti-spam is enabled
     */
    fun getAntiSpamEnabled(): Boolean {
        return antiSpamEnabled
    }
}
