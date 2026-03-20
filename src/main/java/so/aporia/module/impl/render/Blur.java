package so.aporia.module.impl.render;

import net.minecraft.client.Minecraft;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.utils.Logger;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.RenderHudEvent;
import so.aporia.utils.user.render.core.AporiaRenderer;

/**
 * Blur module — applies a light radial blur effect to the screen.
 * The blur is strongest at the center and fades out towards the edges.
 */
public final class Blur extends Module {

    private static Blur instance;

    /* Blur settings */
    private float blurRadius = 0.5f;
    private float blurStrength = 1.0f;  // Full strength for visible blur

    public Blur() {
        super("Blur", Category.VISUAL);
        instance = this;
        enable();
    }

    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
        Logger.info("Blur module enabled");
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        cleanup();
        Logger.info("Blur module disabled");
    }

    @EventHandler
    public void onRenderHud(RenderHudEvent e) {
        if (!isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        AporiaRenderer.INSTANCE.renderBlur(mc);
    }

    private void cleanup() {
        AporiaRenderer.INSTANCE.cleanupBlur();
    }

    public void setBlurRadius(float radius) {
        this.blurRadius = Math.max(0.1f, Math.min(1.0f, radius));
    }

    public void setBlurStrength(float strength) {
        this.blurStrength = Math.max(0.1f, Math.min(1.0f, strength));
    }

    public float getBlurRadius() {
        return blurRadius;
    }

    public float getBlurStrength() {
        return blurStrength;
    }
}
