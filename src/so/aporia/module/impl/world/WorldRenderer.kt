package so.aporia.module.impl.world

import net.minecraft.client.Minecraft
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.NumberSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.logger.Logger

class WorldRenderer : Module("WorldRenderer", Category.WORLD) {

    val customTime = BooleanSetting("Custom Time", "Override client-side world time", false)
    val time = NumberSetting("Time", "Custom client time (0-24000)", 6000.0, 0.0, 24000.0, 100.0) { customTime.isEnabled }
    val timeSpeed = NumberSetting("Time Speed", "Multiplier for time progression", 1.0, 0.0, 100.0, 0.5) { customTime.isEnabled }

    val stars = BooleanSetting("Custom Stars", "Override star density", false)
    val starDensity = NumberSetting("Star Density", "Number of stars visible (0.0 - 2.0)", 1.0, 0.0, 2.0, 0.05) { stars.isEnabled }

    val customFog = BooleanSetting("Custom Fog", "Override fog settings", false)
    val fogDistance = NumberSetting("Fog Distance", "Fog render distance (0.0 - 1.0)", 0.5, 0.0, 1.0, 0.01) { customFog.isEnabled }
    val fogRed = NumberSetting("Fog Red", "Fog color red (0-255)", 128.0, 0.0, 255.0, 1.0) { customFog.isEnabled }
    val fogGreen = NumberSetting("Fog Green", "Fog color green (0-255)", 128.0, 0.0, 255.0, 1.0) { customFog.isEnabled }
    val fogBlue = NumberSetting("Fog Blue", "Fog color blue (0-255)", 128.0, 0.0, 255.0, 1.0) { customFog.isEnabled }

    private val mc = Minecraft.getInstance()
    private var baseDayTime = -1L

    override fun onEnable() {
        EventBus.register(this)
        Logger.info("WorldRenderer enabled")
    }

    override fun onDisable() {
        EventBus.unregister(this)
        Logger.info("WorldRenderer disabled")
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

        active = isEnabled
        starDensityMod = if (isEnabled && stars.isEnabled) starDensity.getFloat() else 1f
        customFogActive = isEnabled && customFog.isEnabled
        if (customFogActive) {
            fogDist = fogDistance.getFloat()
            fogColorR = fogRed.getFloat()
            fogColorG = fogGreen.getFloat()
            fogColorB = fogBlue.getFloat()
        }
    }

    companion object {
        @JvmField var active = false
        @JvmField var starDensityMod = 1.0f
        @JvmField var customFogActive = false
        @JvmField var fogDist = 0.5f
        @JvmField var fogColorR = 128f
        @JvmField var fogColorG = 128f
        @JvmField var fogColorB = 128f
    }
}
