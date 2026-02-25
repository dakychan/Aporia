package cc.apr.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.utils.math.MathUtils;

public class ConsumableTimerDisplay {
   private final int x;
   private final int y;
   private final int width;
   private final int height;

   public ConsumableTimerDisplay(int x, int y, int width, int height) {
      this.x = x;
      this.y = y;
      this.width = width;
      this.height = height;
   }

   public void render(GuiGraphics context, LocalPlayer player, MsdfTextRenderer textRenderer) {
      if (player != null && textRenderer != null) {
         MinecraftPlugin plugin = MinecraftPlugin.getInstance();
         plugin.bindMainFramebuffer(true);
         this.renderRect(this.x, this.y, this.width, this.height, 5.0F, RenderColor.of(35, 35, 45, 180));
         textRenderer.drawText(this.x + 10, this.y + 10, 14.0F, "Consumable Timer", RenderColor.WHITE);
         if (!player.isUsingItem()) {
            textRenderer.drawText(this.x + 10, this.y + 35, 12.0F, "Not consuming", RenderColor.of(150, 150, 150, 200));
         } else {
            ItemStack activeItem = player.getActiveItem();
            int remainingTime = MathUtils.getRemainingTime(player);
            float progress = MathUtils.getConsumptionProgress(player);
            String itemName = activeItem.getItemName().getString();
            if (itemName.length() > 15) {
               itemName = itemName.substring(0, 15) + "...";
            }

            int textY = this.y + 35;
            textRenderer.drawText(this.x + 10, textY, 12.0F, itemName, RenderColor.of(200, 200, 200, 255));
            textY += 18;
            float remainingSeconds = remainingTime / 20.0F;
            textRenderer.drawText(this.x + 10, textY, 12.0F, String.format("Time left: %.1fs", remainingSeconds), RenderColor.of(200, 200, 200, 255));
            textY += 18;
            textRenderer.drawText(this.x + 10, textY, 12.0F, String.format("Progress: %.0f%%", progress * 100.0F), RenderColor.of(200, 200, 200, 255));
            this.renderProgressBar(context, progress);
         }
      }
   }

   public void renderProgressBar(GuiGraphics context, float progress) {
      if (progress < 0.0F) {
         progress = 0.0F;
      }

      if (progress > 1.0F) {
         progress = 1.0F;
      }

      int barX = this.x + 10;
      int barY = this.y + this.height - 25;
      int barWidth = this.width - 20;
      int barHeight = 15;
      this.renderRect(barX, barY, barWidth, barHeight, 3.0F, RenderColor.of(20, 20, 25, 200));
      int fillWidth = (int)(barWidth * progress);
      if (fillWidth > 0) {
         int r = (int)((1.0F - progress) * 255.0F);
         int g = (int)(progress * 255.0F);
         this.renderRect(barX, barY, fillWidth, barHeight, 3.0F, RenderColor.of(r, g, 50, 255));
      }
   }

   private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
      new RoundedRectDrawer().rectSized(x, y, w, h, radius, RectColors.oneColor(color)).build().tryDraw().close();
   }
}
