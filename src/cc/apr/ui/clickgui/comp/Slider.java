package cc.apr.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.render.RectRenderer;
import cc.apr.render.anim.Animation;
import cc.apr.render.anim.Easings;
import cc.apr.utils.math.MathHelper;

public class Slider {
   private String name;
   private float value;
   private float min;
   private float max;
   private int x;
   private int y;
   private int width;
   private int height;
   private boolean dragging = false;
   private Animation hoverAnimation = new Animation();

   public Slider(String name, float value, float min, float max) {
      this.name = name;
      this.value = value;
      this.min = min;
      this.max = max;
      this.height = 25;
   }

   public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      plugin.bindMainFramebuffer(true);
      if (!this.dragging) {
         this.x = x;
         this.y = y;
         this.width = width;
      }

      boolean hovered = this.isHovered(mouseX, mouseY);
      this.hoverAnimation.run(hovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
      this.hoverAnimation.update();
      float hoverProgress = this.hoverAnimation.get();
      if (textRenderer != null) {
         textRenderer.drawText(x, y + 6, 12.0F, this.name, RenderColor.WHITE);
      }

      if (textRenderer != null) {
         String valueText = String.format("%.1f", this.value);
         float textWidth = textRenderer.measureWidth(valueText, 12.0F);
         textRenderer.drawText(x + width - textWidth, y + 6, 12.0F, valueText, RenderColor.of(180, 180, 190, 255));
      }

      RenderColor trackColor = RenderColor.of(40, 40, 50, 255);
      RectRenderer.drawRoundedRect((float)x, (float)(y + this.height - 8), (float)width, 6.0F, 3.0F, trackColor);
      float percentage = (this.value - this.min) / (this.max - this.min);
      float barWidth = width * percentage;
      RenderColor barColor = RenderColor.of(60, 120, 245, 255);
      if (barWidth > 2.0F) {
         RectRenderer.drawRoundedRect((float)x, (float)(y + this.height - 8), barWidth, 6.0F, 3.0F, barColor);
      }

      float handleX = x + barWidth - 6.0F;
      float handleY = y + this.height - 11;
      RenderColor handleColor = !hovered && !this.dragging ? RenderColor.of(80, 140, 255, 255) : RenderColor.of(100, 160, 255, 255);
      RectRenderer.drawRoundedRect(handleX, handleY, 12.0F, 12.0F, 6.0F, handleColor);
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         boolean circleHit = this.isCircleHovered(mouseX, mouseY);
         boolean lineHit = this.isLineHovered(mouseX, mouseY);
         if (!circleHit && !lineHit) {
            return false;
         } else {
            this.dragging = true;
            this.updateValue(mouseX);
            return true;
         }
      }
   }

   public boolean mouseReleased(int mouseX, int mouseY, int button) {
      if (button == 0 && this.dragging) {
         this.dragging = false;
         return true;
      } else {
         return false;
      }
   }

   public void mouseDragged(int mouseX, int mouseY) {
      if (this.dragging) {
         this.updateValue(mouseX);
      }
   }

   private void updateValue(int mouseX) {
      float percentage = MathHelper.clamp((float)(mouseX - this.x) / this.width, 0.0F, 1.0F);
      float newValue = this.min + percentage * (this.max - this.min);
      newValue = (float)MathHelper.round(newValue, 1);
      if (Math.abs(newValue - this.value) > 0.01F) {
         this.value = newValue;
      }
   }

   private boolean isCircleHovered(int mouseX, int mouseY) {
      float percentage = (this.value - this.min) / (this.max - this.min);
      float barWidth = this.width * percentage;
      float handleX = this.x + barWidth - 6.0F + 6.0F;
      float handleY = this.y + this.height - 11 + 6;
      float handleRadius = 6.0F;
      float dx = mouseX - handleX;
      float dy = mouseY - handleY;
      float distanceSquared = dx * dx + dy * dy;
      return distanceSquared <= handleRadius * handleRadius;
   }

   private boolean isLineHovered(int mouseX, int mouseY) {
      int lineY = this.y + this.height - 8;
      int lineHeight = 6;
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= lineY && mouseY <= lineY + lineHeight;
   }

   private boolean isHovered(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public float getValue() {
      return this.value;
   }

   public void setValue(float value) {
      if (!this.dragging) {
         this.value = MathHelper.clamp(value, this.min, this.max);
      }
   }

   public boolean isDragging() {
      return this.dragging;
   }

   public void setBounds(int x, int y, int width) {
      this.x = x;
      this.y = y;
      this.width = width;
   }
}
