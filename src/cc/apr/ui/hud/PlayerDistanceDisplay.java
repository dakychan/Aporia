package cc.apr.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.utils.math.MathUtils;

public class PlayerDistanceDisplay {
   private final int x;
   private final int y;
   private final int width;
   private final int height;

   public PlayerDistanceDisplay(int x, int y, int width, int height) {
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
         textRenderer.drawText(this.x + 10, this.y + 10, 14.0F, "Player Distances", RenderColor.WHITE);
         Map<AbstractClientPlayer, Double> distances = MathUtils.calculateDistancesToPlayers(player);
         if (distances.isEmpty()) {
            textRenderer.drawText(this.x + 10, this.y + 35, 12.0F, "No players nearby", RenderColor.of(150, 150, 150, 200));
         } else {
            List<Entry<AbstractClientPlayer, Double>> sortedDistances = new ArrayList<>(distances.entrySet());
            sortedDistances.sort(Entry.comparingByValue());
            int textY = this.y + 35;
            int maxPlayers = Math.min(5, sortedDistances.size());

            for (int i = 0; i < maxPlayers; i++) {
               Entry<AbstractClientPlayer, Double> entry = sortedDistances.get(i);
               AbstractClientPlayer otherPlayer = entry.getKey();
               double distance = entry.getValue();
               String playerName = otherPlayer.getName().getString();
               if (playerName.length() > 12) {
                  playerName = playerName.substring(0, 12) + "...";
               }

               String distanceText = String.format("%s: %.1f blocks", playerName, distance);
               RenderColor color;
               if (distance < 20.0) {
                  color = RenderColor.of(100, 255, 100, 255);
               } else if (distance < 50.0) {
                  color = RenderColor.of(255, 255, 100, 255);
               } else {
                  color = RenderColor.of(255, 100, 100, 255);
               }

               textRenderer.drawText(this.x + 10, textY, 12.0F, distanceText, color);
               textY += 18;
            }

            if (sortedDistances.size() > 5) {
               textRenderer.drawText(
                  this.x + 10, textY, 11.0F, String.format("+ %d more players", sortedDistances.size() - 5), RenderColor.of(150, 150, 150, 200)
               );
            }
         }
      }
   }

   private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
      new RoundedRectDrawer().rectSized(x, y, w, h, radius, RectColors.oneColor(color)).build().tryDraw().close();
   }
}
