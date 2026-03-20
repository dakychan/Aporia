package so.aporia;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import so.aporia.module.ModuleManager;
import so.aporia.module.impl.render.Blur;
import so.aporia.module.impl.render.Hud;
import so.aporia.utils.Logger;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.impl.RenderHudEvent;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.input.KeybindManager;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.utils.user.render.font.FontRenderer;
import so.aporia.utils.user.render.font.Fonts;

/**
 * Main entry point for Aporia.
 * Registered as a {@link ResourceManagerReloadListener} in {@code Minecraft.java}.
 */
public class Aporia implements ResourceManagerReloadListener {

    public static final Aporia INSTANCE = new Aporia();

    /** Shared font renderer — use this to draw text anywhere. */
    public static final FontRenderer FONTS = new FontRenderer();

    private Aporia() {
        Logger.info("Starting Aporia...");
        /* Init filesystem — создаёт ~/.apr и загружает все файлы */
        try {
            FilesManager.init();
            Logger.success("FilesManager initialized");
        } catch (Exception e) {
            Logger.error("FilesManager init failed: " + e.getMessage());
        }
        /* Init keybind manager — registers itself on EventBus */
        KeybindManager.INSTANCE.toString();
        /* Register all modules once at startup */
        ModuleManager.INSTANCE.registerAll(
            new Hud(),
            new Blur()
        );
        Logger.success("Aporia initialized");
    }

    /**
     * Called from {@code GameRenderer} each frame after {@code GuiGraphics} is available.
     * Fires {@link RenderHudEvent} so all render modules can draw.
     */
    public void render(GuiGraphics gfx, float partialTick) {
        EventBus.INSTANCE.post(new RenderHudEvent(gfx, partialTick));
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        AporiaRenderer.INSTANCE.init();
        Fonts.register(FONTS);
        FONTS.initialize();
    }
}
