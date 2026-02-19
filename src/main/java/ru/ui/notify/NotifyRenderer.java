package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.ui.clickgui.ClickGuiScreen;

import java.util.List;
import java.util.stream.Collectors;

public class NotifyRenderer {
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    
    private static final int NOTIFICATION_WIDTH = 220;
    private static final int NOTIFICATION_HEIGHT = 28;
    private static final int NOTIFICATION_SPACING = 8;
    private static final int START_Y = 20;
    private static final int BORDER_RADIUS = 14;
    private static final int ICON_SIZE = 16;
    private static final int ICON_PADDING = 8;
    
    private static final RenderColor BG_COLOR = RenderColor.of(20, 20, 25, 230);
    
    public static void render(GuiGraphics context) {
        if (context == null) return;
        
        Notify.Manager manager = Notify.Manager.getInstance();
        manager.update();
        
        List<Notify.Notification> notifications = manager.getActiveNotifications();
        if (notifications.isEmpty()) return;

        Minecraft client = Minecraft.getInstance();
        boolean isClickGuiOpen = client.screen instanceof ClickGuiScreen;

        if (isClickGuiOpen) {
            notifications = notifications.stream()
                .filter(n -> n.getType() != Notify.NotificationType.MODULE)
                .collect(Collectors.toList());
            
            if (notifications.isEmpty()) return;
        }
        
        ensureInitialized();
        
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);
        
        int fbWidth = plugin.getMainFramebufferWidth();
        int x = (fbWidth - NOTIFICATION_WIDTH) / 2;
        int y = START_Y;
        
        for (Notify.Notification n : notifications) {
            float slide = n.getSlideOffset();
            int currentY = y + (int)((1.0f - slide) * -NOTIFICATION_HEIGHT);
            
            renderNotification(n, x, currentY);
            y += NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }
    }
    
    private static void renderNotification(Notify.Notification n, int x, int y) {
        String message = n.getMessage();
        float fontSize = 11f; 
        
        if (textRenderer != null) {
            float textWidth = textRenderer.measureWidth(message, fontSize);
            int iconSpace = ICON_SIZE + ICON_PADDING * 2;
            int padding = 28;
            int dynamicWidth = (int)(textWidth + padding + iconSpace);
       
            dynamicWidth = Math.max(120, Math.min(dynamicWidth, 250));
            
            MinecraftPlugin plugin = MinecraftPlugin.getInstance();
            int fbWidth = plugin.getMainFramebufferWidth();
            int centeredX = (fbWidth - dynamicWidth) / 2;
       
            RectRenderer.drawRoundedRect(centeredX, y, dynamicWidth, NOTIFICATION_HEIGHT, BORDER_RADIUS, BG_COLOR);
        
            // Icon space (left side)
            int iconX = centeredX + ICON_PADDING;
            int iconY = y + (NOTIFICATION_HEIGHT - ICON_SIZE) / 2;
            // TODO: Render icon here when available at (iconX, iconY) with size ICON_SIZE
         
            float textStartX = centeredX + iconSpace;
            float textX = textStartX + (dynamicWidth - iconSpace - textWidth) / 2;
            float textY = y + (NOTIFICATION_HEIGHT - fontSize) / 2 + 5;
            textRenderer.drawText(textX, textY, fontSize, message, RenderColor.WHITE);
        } else {
            RectRenderer.drawRoundedRect(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, BORDER_RADIUS, BG_COLOR);
        }
    }
    
    private static void ensureInitialized() {
        if (initialized) return;
        try {
            ru.render.MsdfFont font = new ru.render.MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            );
            textRenderer = new MsdfTextRenderer(font);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
