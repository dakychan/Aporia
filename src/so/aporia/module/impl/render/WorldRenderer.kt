package so.aporia.module.impl.render

import so.aporia.utils.imports.*
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.ColorSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent

class WorldRenderer : Module("WorldRenderer", Category.VISUAL) {

    val customTime = BooleanSetting("Custom Time", "Override client-side world time", false)
    val time = NumberSetting("Time", "Custom client time (0-24000)", 6000.0, 0.0, 24000.0, 100.0) { customTime.isEnabled }
    val timeSpeed = NumberSetting("Time Speed", "Multiplier for time progression", 1.0, 0.0, 100.0, 0.5) { customTime.isEnabled }
    val brightness = NumberSetting("Brightness", "Gamma brightness", 1.0, 0.0, 2.0, 0.05)

    val stars = BooleanSetting("Custom Stars", "Override star density", false)
    val starDensity = NumberSetting("Star Density", "Star count multiplier (0.0 - 10.0)", 1.0, 0.0, 10.0, 0.1) { stars.isEnabled }

    val customFog = BooleanSetting("Custom Fog", "Override fog settings", false)
    val fogColor = ColorSetting("Fog Color", "Fog color", ColorSetting.fromRGB(128, 128, 128)) { customFog.isEnabled }
    val fogStart = NumberSetting("Fog Start", "Fog start distance (0.0 - 1.0)", 0.0, 0.0, 1.0, 0.01) { customFog.isEnabled }
    val fogEnd = NumberSetting("Fog End", "Fog end distance (0.0 - 1.0)", 0.5, 0.0, 1.0, 0.01) { customFog.isEnabled }
    val fogDensity = NumberSetting("Fog Density", "Fog density multiplier (0.0 - 5.0)", 1.0, 0.0, 5.0, 0.05) { customFog.isEnabled }

    private var baseDayTime = -1L

    override fun onEnable() {
        bus.register(this)
        logger.info("WorldRenderer enabled")
    }

    override fun onDisable() {
        bus.unregister(this)
        mc.options.gamma().set(1.0)
        logger.info("WorldRenderer disabled")
    }

    @EventHandler
    fun onTick(e: TickEvent) {
        val level = mc.level ?: return
        if (customTime.isEnabled) {
            if (baseDayTime < 0) baseDayTime = level.dayTime
            if (timeSpeed.getFloat() <= 0f) {
                level.levelData.dayTime = time.get().toLong().coerceIn(0, 24000)
            } else {
                val speed = timeSpeed.getFloat()
                baseDayTime = (baseDayTime + speed).toLong()
                val offset = time.get().toLong() - (baseDayTime % 24000)
                level.levelData.dayTime = (baseDayTime + offset) % 24000
            }
        } else {
            if (baseDayTime >= 0) baseDayTime = -1
        }

        mc.options.gamma().set(brightness.get())

        active = isEnabled
        starDensityMod = if (isEnabled && stars.isEnabled) starDensity.getFloat() else 1f
        val fog = isEnabled && customFog.isEnabled
        customFogActive = fog
        if (fog) {
            fogStartDist = fogStart.getFloat()
            fogEndDist = fogEnd.getFloat()
            fogDensityMod = fogDensity.getFloat()
            fogColorR = fogColor.getR().toFloat()
            fogColorG = fogColor.getG().toFloat()
            fogColorB = fogColor.getB().toFloat()
        }
    }

    companion object {
        @JvmField var active = false
        @JvmField var starDensityMod = 1.0f  // TODO: needs star mixin hook
        @JvmField var customFogActive = false  // TODO: needs fog mixin hook
        @JvmField var fogStartDist = 0.0f  // TODO: needs fog mixin hook
        @JvmField var fogEndDist = 0.5f  // TODO: needs fog mixin hook
        @JvmField var fogDensityMod = 1.0f  // TODO: needs fog mixin hook
        @JvmField var fogColorR = 128f  // TODO: needs fog mixin hook
        @JvmField var fogColorG = 128f  // TODO: needs fog mixin hook
        @JvmField var fogColorB = 128f  // TODO: needs fog mixin hook
    }
}
