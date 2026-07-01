package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.ColorSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent

/**
 * WorldRenderer — overrides client-side world visuals.
 *
 *  - **Time**: переписывает world `dayTime` (клиент-сайд, без влияния на сервер).
 *  - **Brightness**: устанавливает `mc.options.gamma` напрямую. При выключении —
 *    восстанавливает сохранённое значение.
 *  - **Fog**: флаг качается в [fogActive] — mixin на FogRenderer подхватывает
 *    значения start/end/density/color и применяет (см. WorldFogMixin).
 *  - **Stars**: флаг качается в [starDensityOverride] — mixin на StarField
 *    пересчитывает плотность звёзд (см. StarRendererMixin).
 */
class WorldRenderer : Module("WorldRenderer", Category.VISUAL) {

    /* ===== Custom Time ===== */
    val customTime = BooleanSetting("Custom Time", "Override client-side world time", false)
    val time = NumberSetting("Time", "Custom client time (0-24000)", 6000.0, 0.0, 24000.0, 100.0) { customTime.isEnabled }
    val timeSpeed = NumberSetting(
        "Time Speed", "Multiplier for time progression (0 = freeze)",
        1.0, 0.0, 100.0, 0.5
    ) { customTime.isEnabled }

    /* ===== Brightness ===== */
    // Brightness теперь — это GAMMA, диапазон соответствует ванильному слайдеру.
    // При выключении модуля восстанавливаем то, что было до включения.
    val brightness = NumberSetting(
        "Brightness", "Gamma (1.0 = vanilla, >1.0 = brighter world, <1.0 = darker)",
        1.0, 0.0, 2.0, 0.05
    )

    /* ===== Stars ===== */
    val stars = BooleanSetting("Custom Stars", "Override star density", false)
    val starDensity = NumberSetting(
        "Star Density", "Multiplier for star count (1.0 = vanilla)",
        1.0, 0.0, 10.0, 0.1
    ) { stars.isEnabled }

    /* ===== Fog ===== */
    val customFog = BooleanSetting("Custom Fog", "Override fog settings", false)
    val fogColor = ColorSetting(
        "Fog Color", "Fog color", ColorSetting.fromRGB(128, 128, 128)
    ) { customFog.isEnabled }
    val fogStart = NumberSetting(
        "Fog Start", "0.0 = camera, 1.0 = far render distance",
        0.0, 0.0, 1.0, 0.01
    ) { customFog.isEnabled }
    val fogEnd = NumberSetting(
        "Fog End", "0.0 = camera, 1.0 = far render distance",
        1.0, 0.0, 1.0, 0.01
    ) { customFog.isEnabled }
    val fogDensity = NumberSetting(
        "Fog Density", "Density multiplier (vanilla = 1.0)",
        1.0, 0.0, 5.0, 0.05
    ) { customFog.isEnabled }

    private var baseDayTime = -1L
    private var savedGamma = 1.0
    private var savedGammaValid = false

    override fun onEnable() {
        bus.register(this)

        // Сохраняем текущее значение гаммы пользователя, чтобы корректно
        // восстановить при выключении.
        runCatching {
            savedGamma = mc.options.gamma().get()
            savedGammaValid = true
        }

        logger.info("WorldRenderer enabled")
    }

    override fun onDisable() {
        bus.unregister(this)

        // Восстанавливаем гамму: если пользователь менял её до того, как включил
        // модуль — отдадим обратно, иначе оставим vanilla 1.0.
        runCatching {
            mc.options.gamma().set(if (savedGammaValid) savedGamma else 1.0)
        }

        // Сбрасываем состояния смешиваний fog/stars/daytime
        customFogActive = false
        starDensityOverride = -1f
        active = false

        logger.info("WorldRenderer disabled")
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        val level = mc.level ?: run {
            active = false
            customFogActive = false
            starDensityOverride = -1f
            return
        }

        /* ===== Brightness: всегда применяется, пока модуль включён ===== */
        runCatching {
            val target = brightness.get().coerceIn(0.0, 2.0)
            if (target != mc.options.gamma().get()) {
                mc.options.gamma().set(target)
            }
        }

        /* ===== Custom Time ===== */
        if (customTime.isEnabled) {
            if (baseDayTime < 0) baseDayTime = level.dayTime
            val data = level.levelData
            val speed = timeSpeed.getFloat()
            if (speed <= 0f) {
                // Фриз — фиксируем на выставленном значении.
                val t = time.get().toLong().coerceIn(0L, 24000L)
                if (data.dayTime != t) data.setDayTime(t)
            } else {
                // Обычная прокрутка: ориентируемся на время в ванильном мире и
                // добавляем смещение через слайдер.
                baseDayTime = (baseDayTime + speed.toLong()).coerceAtLeast(0)
                val targetTime = time.get().toLong().coerceIn(0L, 24000L)
                val offset = targetTime - (baseDayTime % 24000L)
                val finalTime = ((baseDayTime + offset) % 24000L + 24000L) % 24000L
                if (data.dayTime != finalTime) data.setDayTime(finalTime)
            }
        } else {
            if (baseDayTime >= 0) baseDayTime = -1
        }

        /* ===== Stars (через static-флаги, mixin подхватит) ===== */
        if (isEnabled && stars.isEnabled) {
            starDensityOverride = starDensity.getFloat().coerceIn(0f, 10f)
        } else {
            starDensityOverride = -1f
        }

        /* ===== Fog ===== */
        val fog = isEnabled && customFog.isEnabled
        customFogActive = fog
        if (fog) {
            fogStartDist = fogStart.getFloat().coerceIn(0f, 1f)
            fogEndDist = fogEnd.getFloat().coerceIn(0f, 1f)
            fogDensityMod = fogDensity.getFloat().coerceIn(0f, 5f)
            fogColorR = fogColor.getR().toFloat().coerceIn(0f, 255f)
            fogColorG = fogColor.getG().toFloat().coerceIn(0f, 255f)
            fogColorB = fogColor.getB().toFloat().coerceIn(0f, 255f)
        }

        active = isEnabled
    }

    companion object {
        /** Включён ли модуль в принципе. */
        @JvmField var active = false

        /** Включён ли кастомный fog. -1f = mixin не трогает дефолт. */
        @JvmField var customFogActive = false

        /** Плотность звёзд (mixin в StarField). -1f = использовать ванильную. */
        @JvmField var starDensityOverride = -1f

        @JvmField var fogStartDist = 0.0f
        @JvmField var fogEndDist = 1.0f
        @JvmField var fogDensityMod = 1.0f
        @JvmField var fogColorR = 128f
        @JvmField var fogColorG = 128f
        @JvmField var fogColorB = 128f
    }
}
