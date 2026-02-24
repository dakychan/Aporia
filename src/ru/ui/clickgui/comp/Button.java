package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class Button {
   private String name;
   private boolean enabled;
   private int x;
   private int y;
   private int width;
   private int height;
   private Animation hoverAnimation = new Animation();
   private Animation toggleAnimation = new Animation();
   private Runnable onClick;

   public Button(String name, boolean enabled) {
      this.name = name;
      this.enabled = enabled;
      this.height = 25;
   }

   public void setOnClick(Runnable onClick) {
      this.onClick = onClick;
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
      this.toggleAnimation.run(this.enabled ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
      this.toggleAnimation.update();
      float hoverProgress = this.hoverAnimation.get();
      float toggleProgress = this.toggleAnimation.get();
      RenderColor bgColor = RenderColor.of(
         (int)MathHelper.lerp(40.0F, 60.0F, toggleProgress),
         (int)MathHelper.lerp(40.0F, 120.0F, toggleProgress),
         (int)MathHelper.lerp(50.0F, 245.0F, toggleProgress),
         (int)MathHelper.lerp(180.0F, 230.0F, hoverProgress)
      );
      RectRenderer.drawRoundedRect((float)x, (float)y, (float)width, (float)this.height, 5.0F, bgColor);
      if (textRenderer != null) {
         textRenderer.drawText(x + 8, y + 8, 12.0F, this.name, RenderColor.WHITE);
      }

      if (textRenderer != null) {
         String status = this.enabled ? "Да" : "Нет";
         RenderColor statusColor = this.enabled ? RenderColor.of(80, 200, 120, 255) : RenderColor.of(180, 180, 190, 255);
         float textWidth = textRenderer.measureWidth(status, 12.0F);
         textRenderer.drawText(x + width - textWidth - 8.0F, y + 8, 12.0F, status, statusColor);
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button == 0 && this.isHovered(mouseX, mouseY)) {
         this.enabled = !this.enabled;
         if (this.onClick != null) {
            this.onClick.run();
         }

         return true;
      } else {
         return false;
      }
   }

   private boolean isHovered(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }
}
