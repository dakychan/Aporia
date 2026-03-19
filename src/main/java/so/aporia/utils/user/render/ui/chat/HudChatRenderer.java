package so.aporia.utils.user.render.ui.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.animation.MessageAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** HUD chat renderer — physics-animated, uses active {@link AporiaChatScreen.WinCfg}. */
public final class HudChatRenderer {

    private static final int FADE_START = 160;
    private static final int FADE_TICKS = 40;

    static final Map<Long, MessageAnim> anims = new HashMap<>();

    private HudChatRenderer() {}

    public static void render(GuiGraphics gfx, Font font, ChatComponent chat, int guiTicks) {
        /* Always render Main window (wins[0]) in HUD — filtered windows are screen-only */
        AporiaChatScreen.WinCfg c = AporiaChatScreen.WinMgr.I.wins.get(0);
        List<GuiMessage.Line> lines = new java.util.ArrayList<>(c.lines);
        if (lines.isEmpty()) return;

        int screenH = gfx.guiHeight();
        int boxH    = c.h;
        int boxY    = screenH - c.bottomY - boxH;
        int maxL    = c.maxLines();
        int count   = Math.min(lines.size(), maxL);

        float maxAlpha = 0f;
        float[] alphas  = new float[count];
        float[] offsets = new float[count];

        for (int i = 0; i < count; i++) {
            GuiMessage.Line line = lines.get(i);
            MessageAnim anim = anims.computeIfAbsent(AporiaChatScreen.lineKey(line), k -> new MessageAnim(0f));
            float target = vanillaAlpha(line, guiTicks);
            anim.setAlphaTarget(target);
            anim.tick();
            alphas[i]  = anim.alpha();
            offsets[i] = anim.slideX();
            if (alphas[i] > maxAlpha) maxAlpha = alphas[i];
        }

        anims.entrySet().removeIf(e -> e.getValue().isDead());
        if (maxAlpha < 0.01f) return;

        /* Dynamic background — shrinks when fewer messages than max */
        int actualH  = Math.min(boxH, count * AporiaChatScreen.LINE_H + AporiaChatScreen.BOX_PAD * 2);
        int actualBoxY = boxY + boxH - actualH;
        AporiaRenderer.INSTANCE.drawRect(c.x, actualBoxY, c.w, actualH, AporiaChatScreen.RADIUS,
            ColorUtil.rgba(0, 0, 0, (int)(150 * maxAlpha)));

        int textX = c.x + AporiaChatScreen.BOX_PAD;
        for (int i = 0; i < count; i++) {
            if (alphas[i] < 0.01f) continue;
            int lineY = actualBoxY + AporiaChatScreen.BOX_PAD + (count-1-i) * AporiaChatScreen.LINE_H;
            int tx    = textX + (int) offsets[i];
            gfx.drawString(font, lines.get(i).content(), tx, lineY,
                ColorUtil.rgba(255, 255, 255, (int)(255 * alphas[i])), false);
        }
    }

    private static float vanillaAlpha(GuiMessage.Line line, int guiTicks) {
        int age = guiTicks - line.addedTime();
        if (age > FADE_START + FADE_TICKS) return 0f;
        if (age < FADE_START) return 1f;
        return 1f - Easing.cubicIn((float)(age - FADE_START) / FADE_TICKS);
    }
}
