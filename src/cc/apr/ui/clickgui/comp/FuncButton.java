package cc.apr.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.render.RectRenderer;
import cc.apr.render.anim.Animation;
import cc.apr.render.anim.Easings;
import cc.apr.utils.math.MathHelper;

public class FuncButton {
   private String name;
   private int x;
   private int y;
   private int width;
   private int height;
   private Animation hoverAnimation = new Animation();
   private Runnable action;

   public FuncButton(String name, Runnable action) {
      this.name = name;
      this.action = action;
      this.height = 25;
   }

   public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      plugin.bindMainFramebuffer(true);
      this.x = x;
      this.y = y;
      this.width = width;
      boolean hovered = this.isHovered(mouseX, mouseY);
      this.hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
      this.hoverAnimation.update();
      float hoverProgress = this.hoverAnimation.get();
      RenderColor bgColor = RenderColor.of(
         (int)MathHelper.lerp(50.0F, 70.0F, hoverProgress),
         (int)MathHelper.lerp(50.0F, 70.0F, hoverProgress),
         (int)MathHelper.lerp(60.0F, 80.0F, hoverProgress),
         (int)MathHelper.lerp(180.0F, 220.0F, hoverProgress)
      );
      RectRenderer.drawRoundedRect((float)x, (float)y, (float)width, (float)this.height, 5.0F, bgColor);
      if (textRenderer != null) {
         float textWidth = textRenderer.measureWidth(this.name, 12.0F);
         textRenderer.drawText(x + (width - textWidth) / 2.0F, y + 8, 12.0F, this.name, RenderColor.WHITE);
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button == 0 && this.isHovered(mouseX, mouseY)) {
         if (this.action != null) {
            this.action.run();
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isHovered(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }
}
