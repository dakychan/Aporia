package so.aporia.module.impl.render;

import net.minecraft.client.Minecraft;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.RenderHudEvent;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

/**
 * HUD watermark — renders client name, player nick, FPS and frame-time.
 * Layout mirrors: [APORIA]  [Nick  234FPS  0MS]
 */
public final class Hud extends Module {

    /* Layout */
    private static final int    PAD      = 6;
    private static final int    H        = 22;
    private static final int    MARGIN   = 4;
    private static final int    GAP      = 4;
    private static final float  FS       = 9f;

    /* Colors */
    private static final int C_BG        = ColorUtil.rgba(12,  18,  22,  200);
    private static final int C_ACCENT    = ColorUtil.rgba(80,  200, 200, 255); /* teal — client name */
    private static final int C_WHITE     = ColorUtil.rgba(255, 255, 255, 255);
    private static final int C_DIM       = ColorUtil.rgba(160, 160, 160, 255); /* FPS/MS labels */

    public Hud() {
        super("HUD", Category.VISUAL);
        enable();
    }

    @Override protected void onEnable()  { EventBus.INSTANCE.register(this); }
    @Override protected void onDisable() { EventBus.INSTANCE.unregister(this); }

    @EventHandler
    public void onRenderHud(RenderHudEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        AporiaRenderer r = AporiaRenderer.INSTANCE;

        String clientName = "APORIA";
        String nick       = mc.getUser().getName();
        int    fps        = mc.getFps();
        int    ms         = (int)(mc.getFrameTimeNs() / 1_000_000L);

        /* Measure text widths */
        float wClient = r.getTextWidth("bold",    clientName, FS);
        float wNick   = r.getTextWidth("regular", nick,       FS);
        float wFps    = r.getTextWidth("regular", fps + "",   FS);
        float wFpsLbl = r.getTextWidth("regular", "FPS",      FS);
        float wMs     = r.getTextWidth("regular", ms  + "",   FS);
        float wMsLbl  = r.getTextWidth("regular", "MS",       FS);

        /* Left pill: [APORIA] */
        float pillClientW = PAD*2 + wClient;
        /* Right pill: [nick  234FPS  0MS] */
        float pillInfoW   = PAD*2 + wNick + GAP*2 + wFps + wFpsLbl + GAP*2 + wMs + wMsLbl;

        float x = MARGIN;
        float y = MARGIN;

        /* Left pill */
        r.drawRect(x, y, pillClientW, H, 6, C_BG);
        r.drawText("bold", clientName, x + PAD, y + (H - FS) / 2f, FS, C_ACCENT);

        /* Right pill */
        float rx = x + pillClientW + GAP;
        r.drawRect(rx, y, pillInfoW, H, 6, C_BG);

        float tx = rx + PAD;
        float ty = y + (H - FS) / 2f;

        /* Nick */
        r.drawText("regular", nick, tx, ty, FS, C_WHITE);
        tx += wNick + GAP*2;

        /* 234FPS */
        r.drawText("regular", fps + "", tx, ty, FS, C_WHITE);
        tx += wFps;
        r.drawText("regular", "FPS", tx, ty, FS, C_DIM);
        tx += wFpsLbl + GAP*2;

        /* 0MS */
        r.drawText("regular", ms + "", tx, ty, FS, C_WHITE);
        tx += wMs;
        r.drawText("regular", "MS", tx, ty, FS, C_DIM);
    }
}
