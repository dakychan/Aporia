package cc.apr.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import java.util.List;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import cc.apr.render.MsdfFont;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.render.RectRenderer;
import cc.apr.ui.clickgui.ClickGuiScreen;

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
      if (context != null) {
         Notify.Manager manager = Notify.Manager.getInstance();
         manager.update();
         List<Notify.Notification> notifications = manager.getActiveNotifications();
         if (!notifications.isEmpty()) {
            Minecraft client = Minecraft.getInstance();
            boolean isClickGuiOpen = client.screen instanceof ClickGuiScreen;
            if (isClickGuiOpen) {
               notifications = notifications.stream().filter(nx -> nx.getType() != Notify.NotificationType.MODULE).collect(Collectors.toList());
               if (notifications.isEmpty()) {
                  return;
               }
            }

            ensureInitialized();
            MinecraftPlugin plugin = MinecraftPlugin.getInstance();
            plugin.bindMainFramebuffer(true);
            int fbWidth = plugin.getMainFramebufferWidth();
            int x = (fbWidth - 220) / 2;
            int y = 20;

            for (Notify.Notification n : notifications) {
               float slide = n.getSlideOffset();
               int currentY = y + (int)((1.0F - slide) * -28.0F);
               renderNotification(n, x, currentY);
               y += 36;
            }
         }
      }
   }

   private static void renderNotification(Notify.Notification n, int x, int y) {
      String message = n.getMessage();
      float fontSize = 11.0F;
      if (textRenderer != null) {
         float textWidth = textRenderer.measureWidth(message, fontSize);
         int iconSpace = 32;
         int padding = 28;
         int dynamicWidth = (int)(textWidth + padding + iconSpace);
         dynamicWidth = Math.max(120, Math.min(dynamicWidth, 250));
         MinecraftPlugin plugin = MinecraftPlugin.getInstance();
         int fbWidth = plugin.getMainFramebufferWidth();
         int centeredX = (fbWidth - dynamicWidth) / 2;
         RectRenderer.drawRoundedRect((float)centeredX, (float)y, (float)dynamicWidth, 28.0F, 14.0F, BG_COLOR);
         int iconX = centeredX + 8;
         int iconY = y + 6;
         float textStartX = centeredX + iconSpace;
         float textX = textStartX + (dynamicWidth - iconSpace - textWidth) / 2.0F;
         float textY = y + (28.0F - fontSize) / 2.0F + 5.0F;
         textRenderer.drawText(textX, textY, fontSize, message, RenderColor.WHITE);
      } else {
         RectRenderer.drawRoundedRect((float)x, (float)y, 220.0F, 28.0F, 14.0F, BG_COLOR);
      }
   }

   private static void ensureInitialized() {
      if (!initialized) {
         try {
            MsdfFont font = new MsdfFont("assets/aporia/fonts/Inter_Medium.json", "assets/aporia/fonts/Inter_Medium.png");
            textRenderer = new MsdfTextRenderer(font);
            initialized = true;
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }
   }
}
