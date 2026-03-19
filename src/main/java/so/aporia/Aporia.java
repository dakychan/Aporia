package so.aporia;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import so.aporia.render.core.AporiaRenderer;
import so.aporia.render.font.FontRenderer;
import so.aporia.render.font.Fonts;

/**
 * Main entry point for Aporia.
 * Registered as a {@link ResourceManagerReloadListener} in {@code Minecraft.java}.
 */
public class Aporia implements ResourceManagerReloadListener {

    public static final Aporia INSTANCE = new Aporia();

    /** Shared font renderer — use this to draw text anywhere. */
    public static final FontRenderer FONTS = new FontRenderer();

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        AporiaRenderer.INSTANCE.init();
        Fonts.register(FONTS);
        FONTS.initialize();
    }
}
