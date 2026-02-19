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
        "&#FFFFFFA" +
        "&#EBEBEBp" +
        "&#D6D6D6o" +
        "&#C2C2C2r" +
        "&#AEAEAEi" +
        "&#999999a" +
        "&#858585." +
        "&#707070c" +
        "&#5C5C5Cc" +
        "&#474747 " +
        "&#333333-" +
        "&#1F1F1F>"
    
    /**
     * Gradient color presets for random mode
     */
    private val GRADIENT_PRESETS = listOf(
        listOf(0x00FFFF, 0x0080FF, 0x0000FF),
        listOf(0x8000FF, 0xFF00FF, 0xFF0080),
        listOf(0x00FF00, 0x00FF80, 0x00FFFF),
        listOf(0xFF8000, 0xFF4000, 0xFF0000),
        listOf(0xFFFF00, 0xFFCC00, 0xFF8000),
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
        val withCommand = message.replace("%command", formatCommand(""))
        val formatted = formatMessage(withCommand, type)
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
        
        return "§fAporia.cc -> $colorCode$text"
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
     * Apply gradient to text using Minecraft color codes §.
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
            
            val minecraftColor = rgbToMinecraftColor(color)
            result.append("§$minecraftColor$char")
        }
        
        return result.toString()
    }
    
    /**
     * Convert RGB color to nearest Minecraft color code.
     * 
     * @param rgb RGB color value
     * @return Minecraft color code (0-9, a-f)
     */
    private fun rgbToMinecraftColor(rgb: Int): Char {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        
        val minecraftColors = mapOf(
            0x000000 to '0',
            0x0000AA to '1',
            0x00AA00 to '2',
            0x00AAAA to '3',
            0xAA0000 to '4',
            0xAA00AA to '5',
            0xFFAA00 to '6',
            0xAAAAAA to '7',
            0x555555 to '8',
            0x5555FF to '9',
            0x55FF55 to 'a',
            0x55FFFF to 'b',
            0xFF5555 to 'c',
            0xFF55FF to 'd',
            0xFFFF55 to 'e',
            0xFFFFFF to 'f'
        )
        
        var minDistance = Int.MAX_VALUE
        var closestColor = 'f'
        
        for ((mcRgb, code) in minecraftColors) {
            val mcR = (mcRgb shr 16) and 0xFF
            val mcG = (mcRgb shr 8) and 0xFF
            val mcB = mcRgb and 0xFF
            
            val distance = (r - mcR) * (r - mcR) + 
                          (g - mcG) * (g - mcG) + 
                          (b - mcB) * (b - mcB)
            
            if (distance < minDistance) {
                minDistance = distance
                closestColor = code
            }
        }
        
        return closestColor
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
     * Format command syntax with current prefix.
     * 
     * @param command Command name without prefix
     * @return Formatted command string with current prefix
     */
    fun formatCommand(command: String): String {
        return "$prefix$command"
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
     * Message stack entry for anti-spam system with smart detection.
     * 
     * @property sender The message sender
     * @property message The message content
     * @property count Number of times this message has been sent
     * @property lastTime Timestamp of the last occurrence (milliseconds)
     * @property lastIndex Index of the last message in chat history
     */
    data class MessageStack(
        val sender: String,
        val message: String,
        var count: Int,
        var lastTime: Long,
        var lastIndex: Int
    )
    
    /**
     * Recent messages for stacking.
     */
    private val messageStacks: MutableList<MessageStack> = mutableListOf()
    
    /**
     * Global message counter for tracking positions.
     */
    private var messageCounter: Int = 0
    
    /**
     * Maximum time between messages to stack (milliseconds).
     * Messages older than this will be removed from the stack.
     */
    private const val STACK_TIMEOUT = 5000L
    
    /**
     * Minimum delay between messages to consider as spam (milliseconds).
     */
    private const val SPAM_DELAY_THRESHOLD = 2000L
    
    /**
     * Minimum number of repetitions to consider as spam.
     */
    private const val SPAM_COUNT_THRESHOLD = 3
    
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
