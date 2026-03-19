package so.aporia.render.ui.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import so.aporia.render.animation.Easing;
import so.aporia.render.animation.MessageAnim;
import so.aporia.render.color.ColorUtil;
import so.aporia.render.core.AporiaRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HUD chat renderer — replaces vanilla fade-out chat.
 * Each line gets a {@link MessageAnim} for spring-based alpha + slide-in.
 */
public final class HudChatRenderer {

    static final int LEFT       = AporiaChatScreen.LEFT;
    static final int BOT_PAD    = AporiaChatScreen.BOTTOM_PAD;
    static final int INPUT_H    = AporiaChatScreen.INPUT_H;
    static final int GAP        = AporiaChatScreen.GAP;
    static final int BOX_PAD    = AporiaChatScreen.BOX_PAD;
    static final int LINE_H     = AporiaChatScreen.LINE_H;
    static final int MAX_LINES  = AporiaChatScreen.MAX_LINES;
    static final int RADIUS     = AporiaChatScreen.RADIUS;

    /** Ticks a message stays fully visible before fading. */
    private static final int FADE_START = 160;
    /** Ticks over which vanilla alpha decays. */
    private static final int FADE_TICKS = 40;

    static final Map<Long, MessageAnim> anims = new HashMap<>();

    private HudChatRenderer() {}

    public static void render(GuiGraphics gfx, Font font, ChatComponent chat, int guiTicks) {
        List<GuiMessage.Line> lines = chat.getTrimmedMessages();
        if (lines.isEmpty()) return;

        int fullW   = ChatComponent.getWidth(net.minecraft.client.Minecraft.getInstance().options.chatWidth().get());
        int screenH = gfx.guiHeight();
        int inputY  = screenH - BOT_PAD - INPUT_H;
        int boxH    = MAX_LINES * LINE_H + BOX_PAD * 2;
        int boxY    = inputY - GAP - boxH;
        int count   = Math.min(lines.size(), MAX_LINES);

        /* Tick all anims and drive alpha targets from vanilla fade logic. */
        float maxAlpha = 0f;
        float[] alphas  = new float[count];
        float[] offsets = new float[count];

        for (int i = 0; i < count; i++) {
            GuiMessage.Line line = lines.get(i);
            long key = lineKey(line);

            MessageAnim anim = anims.computeIfAbsent(key, k -> new MessageAnim(0f));
            float target = vanillaAlpha(line, guiTicks);
            anim.setAlphaTarget(target);
            anim.tick();

            alphas[i]  = anim.alpha();
            offsets[i] = anim.slideX();
            if (alphas[i] > maxAlpha) maxAlpha = alphas[i];
        }

        /* Evict dead entries to avoid unbounded growth. */
        anims.entrySet().removeIf(e -> e.getValue().isDead());

        if (maxAlpha < 0.01f) return;

        /* Background box. */
        int bgAlpha = (int)(150 * maxAlpha);
        AporiaRenderer.INSTANCE.drawRect(LEFT, boxY, fullW, boxH, RADIUS,
            ColorUtil.rgba(0, 0, 0, bgAlpha));

        /* Message lines — newest at bottom (index 0). */
        int textX = LEFT + BOX_PAD;
        for (int i = 0; i < count; i++) {
            if (alphas[i] < 0.01f) continue;
            int lineY = boxY + BOX_PAD + (MAX_LINES - 1 - i) * LINE_H;
            int tx    = textX + (int) offsets[i];
            int color = ColorUtil.rgba(255, 255, 255, (int)(255 * alphas[i]));
            gfx.drawString(font, lines.get(i).content(), tx, lineY, color, false);
        }
    }

    /** Vanilla fade: 1.0 while fresh, cubic-out decay after FADE_START ticks. */
    private static float vanillaAlpha(GuiMessage.Line line, int guiTicks) {
        int age = guiTicks - line.addedTime();
        if (age > FADE_START + FADE_TICKS) return 0f;
        if (age < FADE_START) return 1f;
        float t = (float)(age - FADE_START) / FADE_TICKS;
        return 1f - Easing.cubicIn(t);
    }

    private static long lineKey(GuiMessage.Line line) {
        return AporiaChatScreen.lineKey(line);
    }
}
