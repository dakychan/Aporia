package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.ChatHideEvent
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import java.util.regex.Pattern

@Obfuscate
class ServerHelper : Module("ServerHelper", Category.MISC) {

    val autoFlyMe = BooleanSetting("AutoFlyMe", "Автоматически активирует /flyme при падении", true)
    val mathResolver = BooleanSetting("MathResolver", "Автоматически решает математические капчи", true)
    val hideFlyMessages = BooleanSetting("Hide Fly Messages", "Скрывать сообщения о флае", true)
    val hideCaptcha = BooleanSetting("Hide Captcha", "Скрывать капчи из чата", true)

    private var isFalling = false
    private var ticksWithoutGround = 0
    private var ticksSinceLastCommand = 0
    private var captchaStartTime = 0L
    private var captchaSolvedByUs = false

    override fun onEnable() {
        EventBus.register(this)
        isFalling = false
        ticksWithoutGround = 0
        ticksSinceLastCommand = COMMAND_COOLDOWN
    }

    override fun onDisable() {
        EventBus.unregister(this)
        isFalling = false
        ticksWithoutGround = 0
        ticksSinceLastCommand = 0
    }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        val pkt = event.packet
        if (pkt is ClientboundSystemChatPacket) {
            val text = pkt.content.string
            if (hideFlyMessages.isEnabled &&
                (text.contains("Вы успешно включили себе флай") || text.contains("Вы успешно выключили себе флай"))
            ) { event.cancel(); return }
            if (mathResolver.isEnabled && text.contains("Решите")) {
                captchaStartTime = System.nanoTime()
                captchaSolvedByUs = false
                solveCaptcha(text)
                event.cancel()
            }
            if (mathResolver.isEnabled && captchaSolvedByUs) {
                if (CAPTCHA_SOLVED.matcher(text).find()) { event.cancel(); captchaSolvedByUs = false }
            }
        }
    }

    @EventHandler
    fun onChatHide(event: ChatHideEvent) {
        val text = event.plainText
        if (hideFlyMessages.isEnabled &&
            (text.contains("Вы успешно включили себе флай") || text.contains("Вы успешно выключили себе флай"))
        ) event.cancel()
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (!autoFlyMe.isEnabled) return
        ticksSinceLastCommand++
        val mc = Minecraft.getInstance()
        val player = mc.player ?: run { isFalling = false; ticksWithoutGround = 0; return }
        if (player.abilities.flying || player.onGround()) { isFalling = false; ticksWithoutGround = 0; return }
        ticksWithoutGround++
        if (ticksWithoutGround >= 3 && player.deltaMovement.y < -0.08) {
            if (!isFalling) { isFalling = true; ticksSinceLastCommand = COMMAND_COOLDOWN }
        }
        if (isFalling && ticksSinceLastCommand >= COMMAND_COOLDOWN) {
            player.connection?.sendCommand("flyme")
            ticksSinceLastCommand = 0
        }
    }

    private fun solveCaptcha(text: String) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        var answer = -1L
        val simpleMatcher = SIMPLE_MATH.matcher(text)
        if (simpleMatcher.find()) {
            try { answer = simpleMatcher.group(1).toLong() + simpleMatcher.group(2).toLong() } catch (_: Exception) { return }
        } else {
            val complexMatcher = COMPLEX_MATH.matcher(text)
            if (complexMatcher.find()) {
                try { answer = Math.round(evalExpression(complexMatcher.group(1).trim())) } catch (_: Exception) { return }
            }
        }
        if (answer >= 0 && player.connection != null) {
            player.connection.sendChat(answer.toString())
            captchaSolvedByUs = true
            val durationNanos = System.nanoTime() - captchaStartTime
            mc.execute {
                val msg = "§6Aporia.cc §f→ §a${player.name.string} решил капчу за §e${durationNanos} ns§a!"
                player.displayClientMessage(Component.literal(msg), false)
            }
        }
    }

    private fun evalExpression(expr: String): Double {
        val e = expr.replace("\\s+".toRegex(), "")
        val pos = intArrayOf(0)
        return eval(e, pos)
    }

    private fun eval(expr: String, pos: IntArray): Double {
        var result = parseTerm(expr, pos)
        while (pos[0] < expr.length) {
            val op = expr[pos[0]]
            if (op != '+' && op != '-') break
            pos[0]++
            val right = parseTerm(expr, pos)
            result = if (op == '+') result + right else result - right
        }
        return result
    }

    private fun parseTerm(expr: String, pos: IntArray): Double {
        var result = parseFactor(expr, pos)
        while (pos[0] < expr.length) {
            val op = expr[pos[0]]
            if (op != '*' && op != '/') break
            pos[0]++
            val right = parseFactor(expr, pos)
            result = if (op == '*') result * right else result / right
        }
        return result
    }

    private fun parseFactor(expr: String, pos: IntArray): Double {
        if (pos[0] < expr.length && expr[pos[0]] == '(') {
            pos[0]++
            val result = eval(expr, pos)
            if (pos[0] < expr.length) pos[0]++
            return result
        }
        val start = pos[0]
        while (pos[0] < expr.length && expr[pos[0]].isDigit()) pos[0]++
        if (pos[0] == start) throw Exception("Invalid number")
        return expr.substring(start, pos[0]).toDouble()
    }

    companion object {
        private const val COMMAND_COOLDOWN = 2
        private val SIMPLE_MATH = Pattern.compile(".*Решите\\s*:?\\s*(\\d+)\\s*\\+\\s*(\\d+).*")
        private val COMPLEX_MATH = Pattern.compile(".*Решите\\s*:?\\s*([\\d+\\-*/()\\s]+).*")
        private val CAPTCHA_SOLVED = Pattern.compile("(\\w+)\\s+первым\\s+решил\\s+пример\\s+и\\s+победил")
    }
}
