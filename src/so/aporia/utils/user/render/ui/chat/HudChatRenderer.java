/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.render.ui.chat;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import so.aporia.utils.user.render.animation.Easing;
import so.aporia.utils.user.render.animation.MessageAnim;
import so.aporia.utils.user.render.color.ColorUtil;
import so.aporia.utils.user.render.core.AporiaRenderer;
import so.aporia.module.impl.render.Beautifully;

import java.util.HashMap;
import java.util.Map;

/**
 * HUD-side chat renderer. Draws the main chat window ({@link AporiaChatScreen.WinCfg} index 0)
 * while no chat screen is open. Messages fade out using vanilla timing and slide in via
 * physics animations tracked in {@link #anims}.
 */
public final class HudChatRenderer {

    private static final int FADE_START             = 160;
    private static final int FADE_TICKS             = 40;
    private static final int ANIM_CLEANUP_INTERVAL  = 20;

    static final Map<Long, MessageAnim> anims = new HashMap<>();
    private static int cleanupTimer = 0;

    private HudChatRenderer() {}

    /** Called every frame from the HUD render hook. */
    public static void render(GuiGraphics gfx, Font font, ChatComponent chat, int guiTicks) {
        if (!Beautifully.isCustomChatEnabled()) return;

        AporiaChatScreen.WinCfg c = AporiaChatScreen.WinMgr.I.wins.get(0);
        if (c.lines.isEmpty()) return;

        int screenH = gfx.guiHeight();
        int boxH    = c.h;
        int boxY    = screenH - c.bottomY - boxH;
        int maxL    = c.maxLines();
        int count   = Math.min(c.lines.size(), maxL);

        float   maxAlpha = 0f;
        float[] alphas   = new float[count];
        float[] offsets  = new float[count];

        int i = 0;
        for (GuiMessage.Line line : c.lines) {
            if (i >= count) break;
            long key  = AporiaChatScreen.lineKey(line);
            MessageAnim anim = anims.computeIfAbsent(key, k -> new MessageAnim(0f));
            anim.setAlphaTarget(vanillaAlpha(line, guiTicks));
            anim.tick();
            alphas[i]  = anim.alpha();
            offsets[i] = anim.slideX();
            if (alphas[i] > maxAlpha) maxAlpha = alphas[i];
            i++;
        }

        if (++cleanupTimer >= ANIM_CLEANUP_INTERVAL) {
            cleanupTimer = 0;
            anims.entrySet().removeIf(e -> e.getValue().isDead());
        }

        if (maxAlpha < 0.01f) return;

        int actualH    = Math.min(boxH, count * AporiaChatScreen.LINE_H + AporiaChatScreen.BOX_PAD * 2);
        int actualBoxY = boxY + boxH - actualH;
        int bgColor = ColorUtil.rgba(0, 0, 0, (int)(150 * maxAlpha));
        if (Beautifully.isBlurEnabled()) {
            AporiaRenderer.INSTANCE.drawRectBlurred(c.x, actualBoxY, c.w, actualH, AporiaChatScreen.RADIUS, bgColor);
        } else {
            AporiaRenderer.INSTANCE.drawRect(c.x, actualBoxY, c.w, actualH, AporiaChatScreen.RADIUS, bgColor);
        }

        int textX = c.x + AporiaChatScreen.BOX_PAD;
        for (int j = 0; j < count; j++) {
            if (alphas[j] < 0.01f) continue;
            int lineY = actualBoxY + AporiaChatScreen.BOX_PAD + (count - 1 - j) * AporiaChatScreen.LINE_H;
            int tx    = textX + (int) offsets[j];
            int col   = ColorUtil.rgba(255, 255, 255, (int)(255 * alphas[j]));
            GuiMessage.Line line = getLine(c, j);
            if (line != null) gfx.drawString(font, line.content(), tx, lineY, col, false);
        }
    }

    private static GuiMessage.Line getLine(AporiaChatScreen.WinCfg c, int idx) {
        int k = 0;
        for (GuiMessage.Line l : c.lines) {
            if (k++ == idx) return l;
        }
        return null;
    }

    /** Computes vanilla-style fade alpha based on message age in ticks. */
    static float vanillaAlpha(GuiMessage.Line line, int guiTicks) {
        int age = guiTicks - line.addedTime();
        if (age > FADE_START + FADE_TICKS) return 0f;
        if (age < FADE_START) return 1f;
        return 1f - Easing.cubicIn((float)(age - FADE_START) / FADE_TICKS);
    }
}
