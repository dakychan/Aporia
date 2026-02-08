package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.DrawContext;
import ru.render.MsdfTextRenderer;

import java.util.List;

public class NotificationRenderer {
    private static final int NOTIFICATION_WIDTH = 300;
    private static final int NOTIFICATION_HEIGHT = 50;
    private static final int NOTIFICATION_SPACING = 10;
    private static final int PROGRESS_LINE_HEIGHT = 2;
    private static final int START_Y = 20;
    
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    
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
    
    public static void render(DrawContext context) {
        if (context == null) return;
        MinecraftPlugin.getInstance().bindMainFramebuffer(true);
        ensureInitialized();
        
        NotificationManager manager = NotificationManager.getInstance();
        manager.update();
        
        List<Notification> notifications = manager.getActiveNotifications();
        if (notifications.isEmpty()) return;
        
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        if (plugin == null) return;
        
        plugin.bindMainFramebuffer(true);
        int windowWidth = plugin.getMainFramebufferWidth();
        
        int x = (windowWidth - NOTIFICATION_WIDTH) / 2;
        int y = START_Y;
        
        for (Notification notification : notifications) {
            float slideOffset = notification.getSlideOffset();
            int currentY = y + (int)((1.0f - slideOffset) * -NOTIFICATION_HEIGHT);
            
            renderNotification(notification, x, currentY);
            
            y += NOTIFICATION_HEIGHT + NOTIFICATION_SPACING;
        }
    }
    
    private static void renderNotification(Notification notification, int x, int y) {
        NotificationType type = notification.getType();
        
        new RoundedRectDrawer()
            .rectSized(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 8, 
                      RectColors.oneColor(type.getColor()))
            .build()
            .tryDraw()
            .close();
        
        if (textRenderer != null) {
            textRenderer.drawText(
                x + 10, 
                y + (NOTIFICATION_HEIGHT / 2) - 6, 
                12, 
                notification.getMessage(), 
                RenderColor.WHITE
            );
        }
        
        float progress = notification.getProgress();
        int progressWidth = (int)(NOTIFICATION_WIDTH * progress);
        
        new RoundedRectDrawer()
            .rectSized(x, y + NOTIFICATION_HEIGHT - PROGRESS_LINE_HEIGHT, 
                      progressWidth, PROGRESS_LINE_HEIGHT, 1,
                      RectColors.oneColor(RenderColor.of(255, 255, 255, 180)))
            .build()
            .tryDraw()
            .close();
    }
}
