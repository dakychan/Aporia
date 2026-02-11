package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.ui.clickgui.ClickGuiScreen;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Минимальный рендерер нотификаций
 */
public class NotifyRenderer {
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    
    private static final int NOTIFICATION_WIDTH = 250;
    private static final int NOTIFICATION_HEIGHT = 40;
    private static final int NOTIFICATION_SPACING = 8;
    private static final int START_Y = 20;
    
    private static final RenderColor BG_COLOR = RenderColor.of(20, 20, 25, 230);
    
    public static void render(DrawContext context) {
        if (context == null) return;
        
        Notify.Manager manager = Notify.Manager.getInstance();
        manager.update();
        
        List<Notify.Notification> notifications = manager.getActiveNotifications();
        if (notifications.isEmpty()) return;
        
        // Проверяем, открыто ли ClickGui
        MinecraftClient client = MinecraftClient.getInstance();
        boolean isClickGuiOpen = client.currentScreen instanceof ClickGuiScreen;
        
        // Если ClickGui открыто, фильтруем нотификации о модулях
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
        // Фон - закругленный со всех 4 сторон
        RectRenderer.drawRoundedRect(x, y, NOTIFICATION_WIDTH, NOTIFICATION_HEIGHT, 8, BG_COLOR);
        
        // Текст по центру
        if (textRenderer != null) {
            textRenderer.drawText(x + 10, y + 16, 11, n.getMessage(), RenderColor.WHITE);
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
