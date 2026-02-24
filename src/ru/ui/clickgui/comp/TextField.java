package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class TextField {
   private String name;
   private String text = "";
   private String placeholder = "";
   private boolean focused = false;
   private int x;
   private int y;
   private int width;
   private int height;
   private int cursorPosition = 0;
   private long lastBlinkTime = 0L;
   private boolean cursorVisible = true;
   private Animation focusAnimation = new Animation();
   private Animation hoverAnimation = new Animation();

   public TextField(String name, String placeholder) {
      this.name = name;
      this.placeholder = placeholder;
      this.height = 45;
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
      this.focusAnimation.run(this.focused ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
      this.focusAnimation.update();
      float hoverProgress = this.hoverAnimation.get();
      float focusProgress = this.focusAnimation.get();
      RenderColor bgColor = RenderColor.of(
         (int)MathHelper.lerp(40.0F, 50.0F, Math.max(hoverProgress, focusProgress)),
         (int)MathHelper.lerp(40.0F, 50.0F, Math.max(hoverProgress, focusProgress)),
         (int)MathHelper.lerp(50.0F, 60.0F, Math.max(hoverProgress, focusProgress)),
         180
      );
      RectRenderer.drawRoundedRect((float)x, (float)y, (float)width, (float)this.height, 5.0F, bgColor);
      if (textRenderer != null) {
         textRenderer.drawText(x + 8, y + 6, 11.0F, this.name, RenderColor.of(180, 180, 190, 255));
      }

      int fieldY = y + 22;
      int fieldHeight = 18;
      RenderColor fieldColor = RenderColor.of(30, 30, 38, 200);
      RectRenderer.drawRoundedRect((float)(x + 5), (float)fieldY, (float)(width - 10), (float)fieldHeight, 4.0F, fieldColor);
      if (focusProgress > 0.01F) {
         RenderColor borderColor = RenderColor.of(60, 120, 245, (int)(focusProgress * 200.0F));
         RectRenderer.drawRoundedRect((float)(x + 5), (float)fieldY, (float)(width - 10), (float)fieldHeight, 4.0F, borderColor);
         RectRenderer.drawRoundedRect((float)(x + 6), (float)(fieldY + 1), (float)(width - 12), (float)(fieldHeight - 2), 3.0F, fieldColor);
      }

      if (textRenderer != null) {
         String displayText = this.text.isEmpty() ? this.placeholder : this.text;
         RenderColor textColor = this.text.isEmpty() ? RenderColor.of(100, 100, 110, 200) : RenderColor.WHITE;
         float maxWidth = width - 20;
         float textWidth = textRenderer.measureWidth(displayText, 11.0F);
         if (textWidth > maxWidth) {
            int startIndex = Math.max(0, displayText.length() - 20);
            displayText = displayText.substring(startIndex);
         }

         textRenderer.drawText(x + 10, fieldY + 4, 11.0F, displayText, textColor);
         if (this.focused && this.cursorVisible) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastBlinkTime > 500L) {
               this.cursorVisible = !this.cursorVisible;
               this.lastBlinkTime = currentTime;
            }

            float cursorX = x + 10 + textRenderer.measureWidth(this.text.substring(0, Math.min(this.cursorPosition, this.text.length())), 11.0F);
            RenderColor cursorColor = RenderColor.WHITE;
            RectRenderer.drawRoundedRect(cursorX, (float)(fieldY + 3), 1.0F, 12.0F, 0.5F, cursorColor);
         } else if (!this.focused) {
            this.lastBlinkTime = System.currentTimeMillis();
            this.cursorVisible = true;
         }
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button == 0) {
         boolean wasInside = this.isHovered(mouseX, mouseY);
         this.focused = wasInside;
         if (this.focused) {
            this.cursorPosition = this.text.length();
         }

         return wasInside;
      } else {
         return false;
      }
   }

   public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
      if (!this.focused) {
         return false;
      } else if (keyCode == 259) {
         if (!this.text.isEmpty() && this.cursorPosition > 0) {
            this.text = this.text.substring(0, this.cursorPosition - 1) + this.text.substring(this.cursorPosition);
            this.cursorPosition--;
         }

         return true;
      } else if (keyCode == 261) {
         if (this.cursorPosition < this.text.length()) {
            this.text = this.text.substring(0, this.cursorPosition) + this.text.substring(this.cursorPosition + 1);
         }

         return true;
      } else if (keyCode == 263) {
         if (this.cursorPosition > 0) {
            this.cursorPosition--;
         }

         return true;
      } else if (keyCode == 262) {
         if (this.cursorPosition < this.text.length()) {
            this.cursorPosition++;
         }

         return true;
      } else if (keyCode == 268) {
         this.cursorPosition = 0;
         return true;
      } else if (keyCode == 269) {
         this.cursorPosition = this.text.length();
         return true;
      } else if (keyCode == 257) {
         this.focused = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean charTyped(char chr, int modifiers) {
      if (!this.focused) {
         return false;
      } else if (chr >= ' ' && chr != 127) {
         this.text = this.text.substring(0, this.cursorPosition) + chr + this.text.substring(this.cursorPosition);
         this.cursorPosition++;
         return true;
      } else {
         return false;
      }
   }

   private boolean isHovered(int mouseX, int mouseY) {
      return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
   }

   public String getText() {
      return this.text;
   }

   public void setText(String text) {
      this.text = text;
      this.cursorPosition = Math.min(this.cursorPosition, text.length());
   }

   public boolean isFocused() {
      return this.focused;
   }

   public void setFocused(boolean focused) {
      this.focused = focused;
   }
}
