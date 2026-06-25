package so.aporia

import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import so.aporia.module.ModuleManager
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.Hud
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.input.KeybindManager
import so.aporia.utils.user.friend.FriendManager
import so.aporia.utils.user.locale.LocaleManager
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.core.DefaultLibraries
import so.aporia.utils.user.render.core.DefaultSnippets
import so.aporia.utils.user.render.core.DrawBatch
import so.aporia.utils.user.render.theme.ThemeManager
import so.aporia.utils.user.render.font.FontRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.render3d.AporiaRenderer3D
import so.aporia.utils.user.render.ui.mainmenu.ScreenshotCapture

@Obfuscate
class Aporia private constructor() : ResourceManagerReloadListener {

    @JvmField
    val fonts: FontRenderer = FontRenderer()

    init {
        Logger.info("Starting Aporia...")
        try {
            FilesManager.init()
            Logger.success("FilesManager initialized")
        } catch (e: Exception) {
            Logger.error("FilesManager init failed: ${e.message}")
        }
        KeybindManager.toString()
        ScreenshotCapture.start()
        ModuleManager.toString()
        LocaleManager.INSTANCE.init()
        FriendManager.init()
        ThemeManager.INSTANCE.init()
        initializeDiscordRPC()
        Logger.success("Aporia initialized")
    }

    private fun initializeDiscordRPC() {
        try {
            val discordRPC = ModuleManager.get("Discord RPC")
            discordRPC?.enable()
        } catch (e: Exception) {
            Logger.error("Discord RPC initialization failed: ${e.message}")
        }
    }

    fun render(gfx: GuiGraphics, partialTick: Float) {
        val beautifully = ModuleManager.get("Beautifully") as? Beautifully
        if (beautifully != null && beautifully.isEnabled) {
            beautifully.render(gfx, 0, 0, partialTick)
        }

        // NameTags (3D world-space) FIRST — под HUD
        EventBus.post(RenderHudEvent(gfx, partialTick))
        DrawBatch.INSTANCE.flush()
        AporiaRenderer.INSTANCE.flush()

        // HUD (DynamicIsland, InfoPanel) SECOND — поверх неймтегов
        val hud = ModuleManager.get("HUD") as? Hud
        if (hud != null && hud.isEnabled) {
            hud.render(gfx, 0, 0, partialTick)
        }

        DrawBatch.INSTANCE.flush()
        AporiaRenderer.INSTANCE.flush()
    }

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        AporiaRenderer.INSTANCE.init()
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
