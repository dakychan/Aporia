package so.aporia.utils.user.logger

import com.chaos.annotation.Obfuscate

@Obfuscate
object Logger {

    private const val RESET = "\u001B[0m"
    private const val GREEN_BG = "\u001B[42m"
    private const val RED_BG = "\u001B[41m"
    private const val YELLOW_BG = "\u001B[43m"
    private const val BLUE_BG = "\u001B[44m"
    private const val BLACK = "\u001B[30m"
    private const val WHITE = "\u001B[97m"
    private const val BOLD = "\u001B[1m"
    private const val PREFIX = "[Aporia] "

    @JvmStatic
    fun success(message: String) {
        println(GREEN_BG + BLACK + BOLD + " " + PREFIX + message + " " + RESET)
    }

    @JvmStatic
    fun error(message: String) {
        System.err.println(RED_BG + WHITE + BOLD + " " + PREFIX + message + " " + RESET)
    }

    @JvmStatic
    fun warn(message: String) {
        println(YELLOW_BG + BLACK + BOLD + " " + PREFIX + message + " " + RESET)
    }

    @JvmStatic
    fun info(message: String) {
        println(BLUE_BG + WHITE + BOLD + " " + PREFIX + message + " " + RESET)
    }

    @JvmStatic
    fun debug(message: String) {
        if (System.getProperty("aporia.debug") == "true") {
            println("[DEBUG] " + PREFIX + message)
        }
    }
}
