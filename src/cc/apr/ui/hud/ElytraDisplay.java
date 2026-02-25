package cc.apr.ui.hud;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RectColors;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.ferra13671.cometrenderer.plugins.minecraft.drawer.impl.RoundedRectDrawer;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.utils.math.MathUtils;

public class ElytraDisplay {
   private final int x;
   private final int y;
   private final int width;
   private final int height;

   public ElytraDisplay(int x, int y, int width, int height) {
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
         textRenderer.drawText(this.x + 10, this.y + 10, 14.0F, "Elytra Flight", RenderColor.WHITE);
         if (!player.isFlyingVehicle()) {
            textRenderer.drawText(this.x + 10, this.y + 35, 12.0F, "Not flying", RenderColor.of(150, 150, 150, 200));
         } else {
            Vec3 position = player.position();
            Vec3 velocity = player.getDeltaMovement();
            float pitch = player.getXRot();
            float yaw = player.getYRot();
            List<Vec3> trajectory = MathUtils.calculateElytraTrajectory(position, velocity, pitch, yaw);
            double maxDistance = MathUtils.calculateElytraMaxDistance(position, velocity);
            int textY = this.y + 35;
            textRenderer.drawText(this.x + 10, textY, 12.0F, String.format("Distance: %.1f blocks", maxDistance), RenderColor.of(200, 200, 200, 255));
            textY += 18;
            textRenderer.drawText(this.x + 10, textY, 12.0F, String.format("Velocity: %.2f m/s", velocity.length()), RenderColor.of(200, 200, 200, 255));
            textY += 18;
            textRenderer.drawText(this.x + 10, textY, 12.0F, String.format("Points: %d", trajectory.size()), RenderColor.of(200, 200, 200, 255));
            if (!trajectory.isEmpty()) {
               this.renderTrajectory(context, trajectory);
            }
         }
      }
   }

   public void renderTrajectory(GuiGraphics context, List<Vec3> trajectory) {
      if (trajectory != null && !trajectory.isEmpty()) {
         int graphX = this.x + 10;
         int graphY = this.y + this.height - 40;
         int graphWidth = this.width - 20;
         int graphHeight = 30;
         this.renderRect(graphX, graphY, graphWidth, graphHeight, 3.0F, RenderColor.of(20, 20, 25, 200));
         double minY = Double.MAX_VALUE;
         double maxY = Double.MIN_VALUE;

         for (Vec3 point : trajectory) {
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
         }

         double yRange = maxY - minY;
         if (yRange < 0.1) {
            yRange = 1.0;
         }

         for (int i = 0; i < trajectory.size() - 1; i++) {
            Vec3 point = trajectory.get(i);
            float pointX = graphX + (float)(i * graphWidth) / trajectory.size();
            float normalizedY = (float)((point.y - minY) / yRange);
            float pointY = graphY + graphHeight - normalizedY * graphHeight;
            float progress = (float)i / trajectory.size();
            int r = (int)(progress * 255.0F);
            int g = (int)((1.0F - progress) * 255.0F);
            this.renderRect((int)pointX, (int)pointY, 2, 2, 1.0F, RenderColor.of(r, g, 50, 255));
         }
      }
   }

   private void renderRect(int x, int y, int w, int h, float radius, RenderColor color) {
      new RoundedRectDrawer().rectSized(x, y, w, h, radius, RectColors.oneColor(color)).build().tryDraw().close();
   }
}
