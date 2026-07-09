package so.aporia.module.settings
import so.aporia.utils.user.render.animation.SpringSimulator
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.util.function.Supplier
import kotlin.math.roundToInt
import com.chaos.annotation.ChaosNative
@ChaosNative
class SliderSetting @JvmOverloads constructor(
    name: String,
    description: String,
    defaultValue: Double,
    val min: Double,
    val max: Double,
    val step: Double,
    visible: Supplier<Boolean>? = null
) : Setting<Double>(name, description, defaultValue, visible) {

    private val fracSpring = SpringSimulator(160f, 20f, ((defaultValue - min) / (max - min)).toFloat().coerceIn(0f, 1f))
    private var lastTick = System.currentTimeMillis()

    var editing = false
    var editBuffer: String? = null
    val valueBounds = FloatArray(4)

    fun get(): Double = value
    fun getFloat(): Float = value.toFloat()
    fun getInt(): Int = value.toInt()

    fun increase() { value = (value + step).coerceAtMost(max) }
    fun decrease() { value = (value - step).coerceAtLeast(min) }

    fun setValue(v: Double) {
        value = v.coerceIn(min, max)
    }

    override fun displayHeight(dropOpen: Boolean): Float = 22f

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val now = System.currentTimeMillis()
        fracSpring.update((now - lastTick) / 1000f)
        lastTick = now

        val targetFrac = ((get() - min) / (max - min)).toFloat().coerceIn(0f, 1f)
        fracSpring.setTarget(targetFrac)
        val frac = fracSpring.value()

        r.drawText("regular", name, x + 8f, y + 1f, 9f, theme.guiSettingText)

        val txt = if (editing) editBuffer ?: "%.1f".format(get()) else "%.1f".format(get())
        val fs = 8f
        val tw = r.getTextWidth("regular", txt, fs)
        val valX = x + w - 8f - tw - 8f
        val valY = y + 1f

        valueBounds[0] = valX
        valueBounds[1] = valY
        valueBounds[2] = tw + 12f
        valueBounds[3] = 10f

        val overValue = mouseX >= valueBounds[0] && mouseX < valueBounds[0] + valueBounds[2] &&
                        mouseY >= valueBounds[1] && mouseY < valueBounds[1] + valueBounds[3]
        if (editing || overValue) {
            r.drawRect(valX - 2f, valY - 1f, tw + 14f, 10f + 2f, 3f, 0x30FFFFFF.toInt())
        }
        val display = if (editing && (System.currentTimeMillis() / 500L) % 2 == 0L) "$txt|" else txt
        r.drawText("regular", display, valX + 4f, valY, fs, theme.guiSettingValue)

        val pw = w - 14f
        val by = y + 13f
        r.drawRect(x + 10f, by, pw, 2f, 1f, 0x44FFFFFF.toInt())
        r.drawRect(x + 10f, by, pw * frac, 2f, 1f, theme.guiSettingValue)
        val cx = x + 10f + pw * frac
        r.drawCircle(cx, by + 1f, 3f, -0x1)
        r.drawCircle(cx, by + 1f, 2f, theme.guiSettingValue)
    }
}