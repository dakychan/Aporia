package zenith.zov.client.hud.elements.component;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.text.Text;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.client.modules.api.Module;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class NotifyComponent extends DraggableHudElement {

    private final Animation toggleAnimation = new Animation(200, Easing.QUAD_IN_OUT);

    
    private final Deque<BaseNotification> notifications = new ArrayDeque<>();

    public NotifyComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    
    public void addNotification(Module module, boolean enabled) {
        notifications.addLast(new ModuleNotification(module, enabled));
    }

    public void addTextNotification(String icon, Text text) {
        notifications.addLast(new TextNotification(icon, text));
    }

    
    @Override
    public void render(CustomDrawContext ctx) {
        width = 100;
        height = 18;

        long now = System.currentTimeMillis();
        Iterator<BaseNotification> iterator = notifications.iterator();


        toggleAnimation.update(mc.currentScreen instanceof ChatScreen && notifications.isEmpty());

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font textFont = Fonts.MEDIUM.getFont(6);
        Font iconFont = Fonts.ICONS.getFont(6);

        float baseX = x;
        float baseY = y;

                ctx.pushMatrix();
        ctx.getMatrices().translate(x + 50, y + 9);
        ctx.getMatrices().scale(toggleAnimation.getValue(), toggleAnimation.getValue());
        ctx.getMatrices().translate(-(x + 50), -(y + 9));

        float notificationHeight = 18f;
        DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, 90, notificationHeight, 21, BorderRadius.all(4), ColorRGBA.WHITE);

        ctx.drawRoundedRect(x, y, 90, notificationHeight, BorderRadius.all(4), theme.getForegroundColor());
        ctx.drawRoundedRect(x, y, 16, notificationHeight, BorderRadius.left(4, 4), theme.getForegroundLight());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, 90, notificationHeight, 0.1f,14,  theme.getColor(),BorderRadius.all(4));

        ctx.drawText(iconFont, "A", x + (16 - iconFont.width("A")) / 2f + 1f, y + (notificationHeight - iconFont.height()) / 2f, theme.getColor());
        ctx.drawText(textFont, "Пример уведомления", x + 16 + 4, y + (18 - textFont.height()) / 2f, theme.getWhite());

        baseY += (notificationHeight + 6) * toggleAnimation.getValue();
        ctx.popMatrix();

                for (BaseNotification n : notifications) {
            float gap = 6f;
            float offset = n.offsetAnimation.getValue() * (notificationHeight + gap);
            if (offset < 100) {
                n.render(ctx, baseX, baseY + offset, textFont, theme, notificationHeight, this);
            } else {
                n.timestamp = System.currentTimeMillis();
            }
        }

                int index = 0;
        while (iterator.hasNext()) {
            BaseNotification n = iterator.next();

            
            if (!n.fadingOut && now - n.timestamp > 1500) {
                n.fadingOut = true;
                n.alphaAnimation.update(0);
            }

            
            if (n.fadingOut && n.alphaAnimation.getValue() < 0.01f) {
                iterator.remove();
                continue;
            }

            if (!n.fadingOut) {
                if (n.offsetAnimation.isDone() && n.offsetAnimation.getValue() == 0) {
                    n.offsetAnimation.reset(index);
                }
                n.offsetAnimation.update(index);
                index++;
            }

            n.alphaAnimation.update(n.fadingOut ? 0 : 1);
        }
    }

    
    private static abstract class BaseNotification {
        long timestamp;
        boolean fadingOut = false;

        final Animation offsetAnimation = new Animation(300, Easing.QUAD_IN_OUT);
        final Animation alphaAnimation  = new Animation(300, Easing.QUAD_IN_OUT);

        abstract void render(CustomDrawContext ctx,
                             float x, float y,
                             Font textFont,
                             Theme theme,
                             float notificationHeight,
                             NotifyComponent parent);
    }

    
    private static class ModuleNotification extends BaseNotification {
        final Module module;
        final boolean enabled;

        ModuleNotification(Module module, boolean enabled) {
            this.module = module;
            this.enabled = enabled;
        }

        @Override
        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (timestamp == 0) timestamp = System.currentTimeMillis();

            float borderRadius = 4f;
            float iconBgWidth = 16f;

            
            ColorRGBA headerBg = theme.getForegroundLight();
            ColorRGBA rowBg = theme.getForegroundColor();
            ColorRGBA primary = enabled ? theme.getColor() : theme.getGrayLight();
            ColorRGBA textColor = enabled ? theme.getWhite() : theme.getGray();

            String moduleName = module.getName();
            String statusText = "  was " + (enabled ? "enabled" : "disabled");

            float moduleNameWidth = textFont.width(moduleName);
            float statusTextWidth = textFont.width(statusText);
            float width = iconBgWidth + 4 + moduleNameWidth + statusTextWidth + 4;

            parent.height = notificationHeight;
            parent.width = 100;

            
            x += (100 - width) / 2f;

            
            float alpha = alphaAnimation.getValue();
            float scale = alpha;

            Font iconFont = Fonts.ICONS.getFont(6);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x + width / 2f, y + notificationHeight / 2f);
            ctx.getMatrices().scale(scale, scale);
            ctx.getMatrices().translate(-(x + width / 2f), -(y + notificationHeight / 2f));

            
            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, notificationHeight, 21, BorderRadius.all(borderRadius), ColorRGBA.WHITE);
            ctx.drawRoundedRect(x, y, width, notificationHeight, BorderRadius.all(borderRadius), rowBg);
            ctx.drawRoundedRect(x, y, iconBgWidth, notificationHeight, BorderRadius.left(borderRadius, borderRadius), headerBg);

            
            String icon = module.getCategory().getIcon();
            float iconX = x + (iconBgWidth - iconFont.width(icon)) / 2f + 1f;
            float iconY = y + (notificationHeight - iconFont.height()) / 2f;
            ctx.drawText(iconFont, icon, iconX, iconY, primary);

            
            float textX = x + iconBgWidth + 4f;
            float textY = y + (notificationHeight - textFont.height()) / 2f;
            ctx.drawText(textFont, moduleName, textX, textY, primary);
            ctx.drawText(textFont, statusText, textX + moduleNameWidth, textY, textColor);

            ctx.getMatrices().popMatrix();
        }
    }

    
    private static class TextNotification extends BaseNotification {
        final String icon; 
        final Text text;   

        TextNotification(String icon, Text text) {
            this.icon = icon;
            this.text = text;
        }

        @Override
        void render(CustomDrawContext ctx, float x, float y, Font textFont, Theme theme, float notificationHeight, NotifyComponent parent) {
            if (timestamp == 0) timestamp = System.currentTimeMillis();

            float borderRadius = 4f;
            float iconBgWidth = 16f;

            
            ColorRGBA headerBg = theme.getForegroundLight();
            ColorRGBA rowBg = theme.getForegroundColor();
            ColorRGBA primary = theme.getColor();
            ColorRGBA textColor = theme.getWhite();


            float contentWidth = textFont.width(text);
            float width = iconBgWidth + 4 + contentWidth + 4;

            parent.height = notificationHeight;
            parent.width = Math.max(parent.width, 100);

            
            x += (100 - width) / 2f;

            
            float alpha = alphaAnimation.getValue();
            float scale = alpha;

            Font iconFont = Fonts.ICONS.getFont(6);

            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x + width / 2f, y + notificationHeight / 2f);
            ctx.getMatrices().scale(scale, scale);
            ctx.getMatrices().translate(-(x + width / 2f), -(y + notificationHeight / 2f));

            
            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, notificationHeight, 21, BorderRadius.all(borderRadius), ColorRGBA.WHITE);
            ctx.drawRoundedRect(x, y, width, notificationHeight, BorderRadius.all(borderRadius), rowBg);
            ctx.drawRoundedRect(x, y, iconBgWidth, notificationHeight, BorderRadius.left(borderRadius, borderRadius), headerBg);

            
            float iconX = x + (iconBgWidth - iconFont.width(icon)) / 2f + 1f;
            float iconY = y + (notificationHeight - iconFont.height()) / 2f;
            ctx.drawText(iconFont, icon, iconX, iconY, primary);

            
            float textX = x + iconBgWidth + 4f;
            float textY = y + (notificationHeight - textFont.height()) / 2f;
            ctx.drawText(textFont, text, textX, textY);

            ctx.getMatrices().popMatrix();
        }
    }
}


