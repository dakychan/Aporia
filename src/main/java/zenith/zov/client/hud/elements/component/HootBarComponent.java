package zenith.zov.client.hud.elements.component;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.profiler.Profilers;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.mixin.accessors.DrawContextAccessor;
import zenith.zov.utility.mixin.accessors.InGameHudAccessor;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

public class HootBarComponent extends DraggableHudElement {
    List<HotBarSlot> slots = new ArrayList<>();

    public HootBarComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);
        float itemSize = 24;
        width = itemSize * 9;
        height = itemSize;
        for (int i = 0; i < 9; i++) {
            slots.add(new HotBarSlot(i));
        }
    }

    @Override
    public void render(CustomDrawContext ctx) {

        x= (ctx.getScaledWindowWidth()-width )/2;
        float posX = getX();
        float posY = getY();

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();

        if (hasCreativeInventory()) {
            renderHeldItemTooltip(ctx,posY-35);
            renderOverlayMessage(ctx,mc.getRenderTickCounter(),posY-35-9);
            Font font = Fonts.MEDIUM.getFont(7);
            int k = (int) (this.mc.player.experienceLevel);
            ctx.drawText(font, String.valueOf(k), posX + width / 2 - font.width(String.valueOf(k)) / 2, posY - 15 + font.height() / 2, ColorRGBA.GREEN);

            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(posX, posY, width, 24, BorderRadius.all(4), theme.getForegroundColor());
            ItemStack offHand = mc.player.getOffHandStack();
            if(!offHand.isEmpty()) {
                float offHandX=posX - height - 12;
                float offHandY=posY;
                DrawUtil.drawBlurHud(ctx.getMatrices(),offHandX, offHandY, height, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

                ctx.drawRoundedRect(offHandX, offHandY, height, height, BorderRadius.all(4), theme.getForegroundColor());

                ctx.drawRoundedBorder(offHandX, offHandY, height, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());

                DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - height - 12, posY, height, height, 0.1f, 15f, theme.getColor(), BorderRadius.all(4));
                ctx.pushMatrix();
                ctx.getMatrices().translate(offHandX + (24 - 12.8f) / 2f, offHandY + (24 - 12.8f) / 2f);
                ctx.getMatrices().scale(0.8f, 0.8f);
                ctx.drawItem(offHand, 0, 0);

                ((DrawContextAccessor) ctx).callDrawItemBar(offHand, 0, 0);
                ((DrawContextAccessor) ctx).callDrawCooldownProgress(offHand, 0, 0);


                ctx.popMatrix();

                if (offHand.getCount() > 1) {
                    String countText = "x" + String.valueOf(offHand.getCount());
                    float countWidth = font.width(countText);
                    float countX = offHandX + 24 - countWidth - 1f;
                    float countY = offHandY + 24 - font.height() - 3;

                    ctx.drawText(font, countText, countX, countY,  theme.getGray());
                }
            }

            float xSlot = posX;
            for (HotBarSlot slot : slots) {
                slot.render(ctx, xSlot, posY, theme);
                xSlot += height;
            }


            ctx.drawRoundedBorder(x, y, width, 24, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());



            DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, 24, 0.1f, 15f, theme.getColor(), BorderRadius.all(4));
            return;
        }

        if(mc.interactionManager.hasStatusBars()) {
            ctx.pushMatrix();
            ctx.getMatrices().translate(-(ctx.getScaledWindowWidth() / 2 - 91), -(ctx.getScaledWindowHeight() - 39));
            ctx.getMatrices().scale(1, 1);
            ctx.getMatrices().translate(posX, 0);


            ctx.getMatrices().translate(0, posY - 15);

            if (!hasCreativeInventory()) {
                ((InGameHudAccessor) mc.inGameHud).invokeRenderStatusBars(ctx);
            }

            ctx.popMatrix();

            renderHeldItemTooltip(ctx,posY-35);
            renderOverlayMessage(ctx,mc.getRenderTickCounter(),posY-35-9);
            Font font = Fonts.MEDIUM.getFont(7);
            int k = (int) (this.mc.player.experienceLevel);
            ctx.drawText(font, String.valueOf(k), posX + width / 2 - font.width(String.valueOf(k)) / 2, posY - 15 + font.height() / 2, ColorRGBA.GREEN);

            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(posX, posY, width, 24, BorderRadius.all(4), theme.getForegroundColor());
            ItemStack offHand = mc.player.getOffHandStack();
            if(!offHand.isEmpty()) {
                float offHandX=posX - height - 12;
                float offHandY=posY;
                DrawUtil.drawBlurHud(ctx.getMatrices(),offHandX, offHandY, height, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

                ctx.drawRoundedRect(offHandX, offHandY, height, height, BorderRadius.all(4), theme.getForegroundColor());

                ctx.drawRoundedBorder(offHandX, offHandY, height, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());

                DrawUtil.drawRoundedCorner(ctx.getMatrices(), posX - height - 12, posY, height, height, 0.1f, 15f, theme.getColor(), BorderRadius.all(4));
                ctx.pushMatrix();
                ctx.getMatrices().translate(offHandX + (24 - 12.8f) / 2f, offHandY + (24 - 12.8f) / 2f);
                ctx.getMatrices().scale(0.8f, 0.8f);
                ctx.drawItem(offHand, 0, 0);

                ((DrawContextAccessor) ctx).callDrawItemBar(offHand, 0, 0);
                ((DrawContextAccessor) ctx).callDrawCooldownProgress(offHand, 0, 0);


                ctx.popMatrix();

                if (offHand.getCount() > 1) {
                    String countText = "x" + String.valueOf(offHand.getCount());
                    float countWidth = font.width(countText);
                    float countX = offHandX + 24 - countWidth - 1f;
                    float countY = offHandY + 24 - font.height() - 3;

                    ctx.drawText(font, countText, countX, countY,  theme.getGray());
                }
            }

            float xSlot = posX;
            for (HotBarSlot slot : slots) {
                slot.render(ctx, xSlot, posY, theme);
                xSlot += height;
            }


            ctx.drawRoundedBorder(x, y, width, 24, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());



            DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, 24, 0.1f, 15f, theme.getColor(), BorderRadius.all(4));

        }
    }


    class HotBarSlot {
        private final Animation animationEnable = new Animation(150, 0, Easing.QUAD_IN_OUT);
        private final BorderRadius borderRadius;
        private final int index;

        public HotBarSlot(int index) {
            borderRadius = index == 0 ? BorderRadius.left(4, 4) : index == 8 ? BorderRadius.right(4, 4) : BorderRadius.ZERO;
            this.index = index;
        }

        public void render(CustomDrawContext ctx, float x, float y, Theme theme) {
            animationEnable.setDuration(80);
            Font font = Fonts.MEDIUM.getFont(6);
            animationEnable.update(index == mc.player.getInventory().getSelectedSlot() ? 1 : 0);
            ColorRGBA bgColor = (index % 2 != 0 ? ColorRGBA.TRANSPARENT : theme.getForegroundLight()).mix(theme.getColor(), animationEnable.getValue());
            ColorRGBA textColor = theme.getGray().mix(theme.getWhite(), animationEnable.getValue());
            ItemStack stack = mc.player.getInventory().getMainStacks().get(index);
            ctx.drawRoundedRect(x, y, 24, 24, borderRadius, bgColor);
            ctx.pushMatrix();
            ctx.getMatrices().translate(x + (24 - 12.8f) / 2f, y + (24 - 12.8f) / 2f);
            ctx.getMatrices().scale(0.8f, 0.8f);
            ctx.drawItem(stack, 0, 0);

            ((DrawContextAccessor) ctx).callDrawItemBar(stack, 0, 0);
            ((DrawContextAccessor) ctx).callDrawCooldownProgress(stack, 0, 0);


            ctx.popMatrix();
            ctx.drawText(font, String.valueOf(index + 1), x + 2, y + 2, textColor);
            if (stack.getCount() > 1) {
                String countText = "x" + String.valueOf(stack.getCount());
                float countWidth = font.width(countText);
                float countX = x + 24 - countWidth - 1;
                float countY = y + 24 - font.height() - 3;

                ctx.drawText(font, countText, countX, countY, textColor);
            }
        }

    }

    private void renderHeldItemTooltip(CustomDrawContext context,float y) {
        Profilers.get().push("selectedItemName");

        if (mc.inGameHud.heldItemTooltipFade > 0 && !mc.inGameHud.currentStack.isEmpty()) {
            MutableText mutableText = Text.empty().append(mc.inGameHud.currentStack.getName()).formatted(mc.inGameHud.currentStack.getRarity().getFormatting());
            if (mc.inGameHud.currentStack.contains(DataComponentTypes.CUSTOM_NAME)) {
                mutableText.formatted(Formatting.ITALIC);
            }

            int i = mc.textRenderer.getWidth(mutableText);
            int j = (context.getScaledWindowWidth() - i) / 2;
            int k = (int) y;
            if (!mc.interactionManager.hasStatusBars() || hasCreativeInventory()) {
                k += 14;
            }

            int l = (int)((float)mc.inGameHud.heldItemTooltipFade * 256.0F / 10.0F);
            if (l > 255) {
                l = 255;
            }

            if (l > 0) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate(j, k);

                Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
                context.drawTextWithBackground(mc.inGameHud.getTextRenderer(), mutableText, 0, 0, i, ColorHelper.withAlpha(l, Colors.WHITE) );

                context.getMatrices().popMatrix();
            }
        }

        Profilers.get().pop();
    }
    public final void renderOverlayMessage(CustomDrawContext context, RenderTickCounter tickCounter,float y) {
        TextRenderer textRenderer = mc.inGameHud.getTextRenderer();
        if (mc.inGameHud.overlayMessage != null && mc.inGameHud.overlayRemaining > 0) {
            Profilers.get().push("overlayMessage");
            float f = (float)mc.inGameHud.overlayRemaining - tickCounter.getTickProgress(false);
            int i = (int)(f * 255.0F / 20.0F);
            if (i > 255) {
                i = 255;
            }

            if (i > 8) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((float)(context.getScaledWindowWidth() / 2), y);
                int j;
                if (mc.inGameHud.overlayTinted) {
                    j = MathHelper.hsvToArgb(f / 50.0F, 0.7F, 0.6F, i);
                } else {
                    j = ColorHelper.withAlpha(i, -1);
                }

                int k = textRenderer.getWidth(mc.inGameHud.overlayMessage);

                context.getMatrices().translate(-k / 2f, -4);
                context.drawTextWithBackground(textRenderer, mc.inGameHud.overlayMessage, 0, 0, k,j);

                context.getMatrices().popMatrix();
            }

            Profilers.get().pop();
        }
    }

    @Override
    protected void renderXLine(CustomDrawContext ctx, SheetCode nearest) {

    }

    private boolean hasCreativeInventory() {
        return mc.interactionManager != null
                && mc.interactionManager.getCurrentGameMode() != null
                && mc.interactionManager.getCurrentGameMode().isCreative();
    }
}


