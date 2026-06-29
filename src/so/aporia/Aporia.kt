package so.aporia

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.Hud
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.user.input.KeybindManager
import so.aporia.utils.user.friend.FriendManager
import so.aporia.utils.user.render.core.DefaultLibraries
import so.aporia.utils.user.render.core.DefaultSnippets
import so.aporia.utils.user.render.core.DrawBatch
import so.aporia.utils.user.render.theme.ThemeManager
import so.aporia.utils.user.render.font.FontRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.render3d.AporiaRenderer3D


@Obfuscate
class Aporia private constructor() : ResourceManagerReloadListener {

    @JvmField
    val fonts: FontRenderer = FontRenderer()

    init {
        logger.info("Starting Aporia...")
        try {
            files.init()
            logger.success("FilesManager initialized")
        } catch (e: Exception) {
            logger.error("FilesManager init failed: ${e.message}")
        }
        KeybindManager.toString()
        mm.toString()
        locale.init()
        FriendManager.init()
        ThemeManager.INSTANCE.init()
        initializeDiscordRPC()
        loadFontMode()
        logger.success("Aporia initialized")
    }

    private fun loadFontMode() {
        val clickGui = mm.get("ClickGui") as? so.aporia.module.impl.misc.ClickGui
        if (clickGui != null) {
            val idx = clickGui.fontRendererMode.getSelectedIndex()
            fonts.currentMode = when (idx) {
                0 -> so.aporia.utils.user.render.font.FontMode.MSDF
                1 -> so.aporia.utils.user.render.font.FontMode.TTF
                2 -> so.aporia.utils.user.render.font.FontMode.OTF
                else -> so.aporia.utils.user.render.font.FontMode.MSDF
            }
            val famIdx = clickGui.fontFamily.getSelectedIndex()
            fonts.currentFamily = if (famIdx == 1) "Inter" else "Default"
        }
    }

    private fun initializeDiscordRPC() {
        try {
            val discordRPC = mm.get("Discord RPC")
            discordRPC?.enable()
        } catch (e: Exception) {
            logger.error("Discord RPC initialization failed: ${e.message}")
        }
    }

    private var lastRenderError = 0L
    private var renderErrorCount = 0

    fun render(gfx: GuiGraphics, partialTick: Float) {
        val mem = Runtime.getRuntime()
        if (mem.freeMemory() < 32L * 1024L * 1024L) {
            System.gc()
            if (mem.freeMemory() < 16L * 1024L * 1024L) return
        }
        try {
            val beautifully = mm.get("Beautifully") as? Beautifully
            if (beautifully != null && beautifully.isEnabled) {
                beautifully.render(gfx, 0, 0, partialTick)
            }

            bus.post(RenderHudEvent(gfx, partialTick))
            DrawBatch.INSTANCE.flush()
            r.flush()

            val hud = mm.get("HUD") as? Hud
            if (hud != null && hud.isEnabled) {
                hud.render(gfx, 0, 0, partialTick)
            }

            DrawBatch.INSTANCE.flush()
            r.flush()
            renderErrorCount = 0
        } catch (e: OutOfMemoryError) {
            renderErrorCount++
            System.err.println("[Aporia] OOM in render #$renderErrorCount")
            if (renderErrorCount > 5) throw e
            System.gc()
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            if (now - lastRenderError > 5000) {
                lastRenderError = now
                System.err.println("[Aporia] Render error: ${e.javaClass.name}: ${e.message}")
            }
        }
    }

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        r.init()
        AporiaRenderer3D.INSTANCE.init()
        DefaultSnippets.registerAll()
        DefaultLibraries.registerAll()
        Fonts.register(fonts)
    }

    companion object {
        @JvmField
        val INSTANCE = Aporia()

        @JvmField
        val FONTS: FontRenderer = INSTANCE.fonts
    }
}
