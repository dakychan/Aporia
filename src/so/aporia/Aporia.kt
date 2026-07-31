package so.aporia
import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import so.aporia.module.impl.render.Beautifully
import so.aporia.module.impl.render.Hud
import so.aporia.utils.events.impl.RenderHudEvent
import so.aporia.utils.files.impl.ConfigFile
import so.aporia.utils.user.input.KeybindManager
import so.aporia.utils.user.friend.FriendManager
import so.aporia.utils.user.render.ui.clickgui.QuestManager
import so.aporia.utils.user.render.font.FontRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.render3d.AporiaRenderer3D
import so.aporia.utils.user.render.ui.chat.ChatScreenRenderer
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.Minecraft
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class Aporia private constructor() : ResourceManagerReloadListener {

    @JvmField
    val fonts: FontRenderer = FontRenderer()

    private var configLoadDeferred = false

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
        if (mc.getNarrator() != null) {
            ConfigFile.load()
        } else {
            configLoadDeferred = true
        }
        locale.init()
        FriendManager.init()
        QuestManager.init()
        initializeDiscordRPC()
        r.fontFlush = Runnable { fonts.flushPipeline() }
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
            val avail = fonts.availableFamilies()
            val famIdx = clickGui.fontFamily.getSelectedIndex()
            fonts.currentFamily = if (famIdx in avail.indices) avail[famIdx] else "Default"
        }
    }

    fun updateFontFamilyOptions() {
        val clickGui = mm.get("ClickGui") as? so.aporia.module.impl.misc.ClickGui ?: return
        val avail = fonts.availableFamilies()
        clickGui.fontFamily.value(*avail.toTypedArray()).selected(avail.firstOrNull() ?: "Default")
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
    private var fontsInitialized = false

    fun render(gfx: GuiGraphicsExtractor, partialTick: Float) {
        if (!fontsInitialized) {
            fontsInitialized = true
            r.init()
            AporiaRenderer3D.INSTANCE.init()
            Fonts.register(fonts)
            fonts.initialize()
            r.fontFlush = Runnable { fonts.flushPipeline() }
        }
        if (configLoadDeferred && mc.getNarrator() != null) {
            configLoadDeferred = false
            ConfigFile.load()
        }
        @Suppress("DEPRECATION")
        val mem = Runtime.getRuntime()
        if (mem.freeMemory() < 32L * 1024L * 1024L && renderErrorCount == 0) {
            System.gc()
        }
        if (mem.freeMemory() < 16L * 1024L * 1024L) {
            if (renderErrorCount > 3) return
        }
        try {
            val beautifully = mm.get("Beautifully") as? Beautifully
            if (beautifully != null && beautifully.isEnabled) {
                beautifully.render(gfx, 0, 0, partialTick)
            }

            bus.post(RenderHudEvent(gfx, partialTick))
            r.flush()

            // Aporia Chat HUD (когда чат не в фокусе — иначе ChatScreenBackendApi открыт)
            val mc = Minecraft.getInstance()
            if (!mc.gui.hud.getChat().isChatFocused()) {
                if (Beautifully.isCustomChatEnabled()) {
                    ChatScreenRenderer.render(gfx, mc.font, mc.gui.hud.getChat(), mc.gui.hud.getGuiTicks())
                } else {
                    mc.gui.hud.getChat().extractRenderState(gfx, mc.font, mc.gui.hud.getGuiTicks(), 0, 0, ChatComponent.DisplayMode.BACKGROUND, false)
                }
            }

            val hud = mm.get("HUD") as? Hud
            if (hud != null && hud.isEnabled) {
                hud.render(gfx, 0, 0, partialTick)
            }

            r.flush()
            renderErrorCount = 0
        } catch (e: OutOfMemoryError) {
            renderErrorCount++
            so.aporia.utils.user.logger.Logger.error("OOM in render #$renderErrorCount")
            if (renderErrorCount > 5) throw e
            System.gc()
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            if (now - lastRenderError > 5000) {
                lastRenderError = now
                so.aporia.utils.user.logger.Logger.error("Render error: ${e.javaClass.name}: ${e.message}")
            }
        }
    }

    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        r.init()
        AporiaRenderer3D.INSTANCE.init()
        Fonts.register(fonts)
        fonts.initialize()
        r.fontFlush = Runnable { fonts.flushPipeline() }
        updateFontFamilyOptions()
        loadFontMode()
        fontsInitialized = true
    }

    companion object {
        @JvmField
        val INSTANCE = Aporia()

        @JvmField
        val FONTS: FontRenderer = INSTANCE.fonts
    }
}