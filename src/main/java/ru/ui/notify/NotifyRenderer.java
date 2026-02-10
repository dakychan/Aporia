package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import net.minecraft.client.gui.DrawContext;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;

import java.util.List;

/**
 * Рендерер нотификаций
 */
public class NotifyRenderer {
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    
    private static final int NOTIFICATION_WIDTH = 250;
    private static final int NOTIFICATION_HEIGHT = 40;
    private static final int NOTIFICATION_SPACING = 8;
    private static final int PROGRESS_LINE_HEIGHT = 1;
    private static final int START_Y = 20;
    
    private static final RenderColor BG_COLOR = RenderColor.of(20, 20, 25, 230);
    private static final RenderColor PROGRESS_COLOR = RenderColor.of(60, 120, 245, 200);
    
    public static void render(DrawContext context) {
        if (context == null) return;
        
        Notify.Manager manager = Notify.Manager.getInstance();
        manager.update();
        
        List<Notify.Notification> notifications = manager.getActiveNotifications();
        if (notifications.isEmpty()) return;
        
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
        // Фон
        RectRenderer.drawRoundedRect(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 6, BG_COLOR);
        
        // Текст
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 16, 11, n.getMessage(), RenderColor.WHITE);
        }
        
        // Полоска прогресса
        float progress = n.getProgress();
        int progressWidth = (int)(NOTIFICATION_WIDTH * progress);
        RectRenderer.drawRoundedRect(
            x, y + NOTIFICATION_HEIGHT - PROGRESS_LINE_HEIGHT, 
            progressWidth, PROGRESS_LINE_HEIGHT, 0, PROGRESS_COLOR
        );
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
