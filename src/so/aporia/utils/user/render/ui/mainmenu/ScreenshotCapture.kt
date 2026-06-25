package so.aporia.utils.user.render.ui.mainmenu

import com.chaos.annotation.Obfuscate
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent

@Obfuscate
object ScreenshotCapture {

    private const val INTERVAL_MS = 20L * 60L * 1000L
    private var lastCapture = 0L
    private var captureScheduled = false
    private const val FILE_NAME = "_mainmenu_temp.png"

    fun start() {
        EventBus.register(this)
    }

    fun stop() {
        EventBus.unregister(this)
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        val mc = Minecraft.getInstance()
        if (mc.level == null) return
        val conn = mc.connection ?: return
        if (!conn.connection.isConnected) return
        val now = System.currentTimeMillis()
        if (now - lastCapture < INTERVAL_MS) return
        if (captureScheduled) return
        captureScheduled = true
        lastCapture = now

        val dir = mc.gameDirectory
        val targetFile = java.io.File(dir, FILE_NAME)

        try {
            net.minecraft.client.Screenshot.takeScreenshot(mc.mainRenderTarget) { nativeImage ->
                net.minecraft.util.Util.ioPool().execute {
                    try {
                        nativeImage.writeToFile(targetFile)
                    } catch (e: Exception) {
                        System.err.println("Failed to write mainmenu screenshot: ${e.message}")
                    } finally {
                        nativeImage.close()
                        captureScheduled = false
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Screenshot capture failed: ${e.message}")
            captureScheduled = false
        }
    }

    fun captureNow() {
        val mc = Minecraft.getInstance()
        if (mc.level == null) return
        val now = System.currentTimeMillis()
        lastCapture = now

        val dir = mc.gameDirectory
        val targetFile = java.io.File(dir, FILE_NAME)

        try {
            net.minecraft.client.Screenshot.takeScreenshot(mc.mainRenderTarget) { nativeImage ->
                try {
                    nativeImage.writeToFile(targetFile)
                } catch (e: Exception) {
                    System.err.println("Failed to write mainmenu screenshot: ${e.message}")
                } finally {
                    nativeImage.close()
                }
            }
        } catch (e: Exception) {
            System.err.println("Screenshot capture failed: ${e.message}")
        }
    }
}