package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import java.util.ArrayList;
import java.util.List;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.render.anim.Animation;
import ru.render.anim.Easings;
import ru.util.math.MathHelper;

public class SingleListButton {
   private String name;
   private List<String> options;
   private int selectedIndex = 0;
   private boolean expanded = false;
   private int x;
   private int y;
   private int width;
   private int height;
   private Animation expandAnimation = new Animation();
   private Animation hoverAnimation = new Animation();
   private int hoveredIndex = -1;
   private static final String ARROW_DOWN = "V";

   public SingleListButton(String name, String... options) {
      this.name = name;
      this.options = new ArrayList<>();

      for (String option : options) {
         this.options.add(option);
      }

      this.height = 25;
   }

   public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      plugin.bindMainFramebuffer(true);
      this.x = x;
      this.y = y;
      this.width = width;
      this.expandAnimation.run(this.expanded ? 1.0 : 0.0, 0.3, Easings.CUBIC_OUT, false);
      this.expandAnimation.update();
      float expandProgress = this.expandAnimation.get();
      boolean headerHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 25;
      this.hoverAnimation.run(headerHovered ? 1.0 : 0.0, 0.2, Easings.CUBIC_OUT, false);
      this.hoverAnimation.update();
      float hoverProgress = this.hoverAnimation.get();
      RenderColor headerColor = RenderColor.of(
         (int)MathHelper.lerp(50.0F, 60.0F, hoverProgress),
         (int)MathHelper.lerp(50.0F, 60.0F, hoverProgress),
         (int)MathHelper.lerp(60.0F, 70.0F, hoverProgress),
         200
      );
      RectRenderer.drawRoundedRect((float)x, (float)y, (float)width, 25.0F, 5.0F, headerColor);
      if (textRenderer != null) {
         textRenderer.drawText(x + 8, y + 8, 12.0F, this.name, RenderColor.WHITE);
         String selectedText = this.options.get(this.selectedIndex);
         float selectedWidth = textRenderer.measureWidth(selectedText, 11.0F);
         textRenderer.drawText(x + width - selectedWidth - 25.0F, y + 8, 11.0F, selectedText, RenderColor.of(180, 180, 190, 255));
         textRenderer.drawText(x + width - 20, y + 8, 12.0F, "V", RenderColor.of(180, 180, 190, 255));
      }

      if (expandProgress > 0.01F) {
         int listHeight = (int)(this.options.size() * 22 * expandProgress);
         int optionY = y + 27;
         this.hoveredIndex = -1;

         for (int i = 0; i < this.options.size(); i++) {
            float itemAlpha = Math.min(1.0F, expandProgress * 2.0F - i * 0.1F);
            if (!(itemAlpha <= 0.0F)) {
               boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= optionY && mouseY <= optionY + 20;
               if (hovered && this.expanded) {
                  this.hoveredIndex = i;
               }

               boolean isSelected = i == this.selectedIndex;
               RenderColor optionColor = RenderColor.of(
                  isSelected ? 60 : (hovered ? 50 : 40),
                  isSelected ? 120 : (hovered ? 50 : 40),
                  isSelected ? 245 : (hovered ? 60 : 50),
                  (int)(itemAlpha * (isSelected ? 200 : 160))
               );
               RectRenderer.drawRoundedRect((float)(x + 5), (float)optionY, (float)(width - 10), 20.0F, 4.0F, optionColor);
               if (isSelected) {
                  RenderColor indicatorColor = RenderColor.of(80, 200, 120, (int)(itemAlpha * 255.0F));
                  RectRenderer.drawRoundedRect((float)(x + 8), (float)(optionY + 7), 3.0F, 6.0F, 1.5F, indicatorColor);
               }

               if (textRenderer != null) {
                  RenderColor textColor = RenderColor.of(255, 255, 255, (int)(itemAlpha * 255.0F));
                  textRenderer.drawText(x + 15, optionY + 5, 11.0F, this.options.get(i), textColor);
               }

               optionY += 22;
            }
         }

         this.height = 25 + listHeight + 2;
      } else {
         this.height = 25;
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      } else if (mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 25) {
         this.expanded = !this.expanded;
         return true;
      } else if (this.expanded && this.hoveredIndex >= 0) {
         this.selectedIndex = this.hoveredIndex;
         this.expanded = false;
         return true;
      } else {
         return false;
      }
   }

   public int getHeight() {
      return this.height;
   }

   public int getSelectedIndex() {
      return this.selectedIndex;
   }

   public String getSelectedValue() {
      return this.options.get(this.selectedIndex);
   }

   public void setSelectedIndex(int index) {
      if (index >= 0 && index < this.options.size()) {
         this.selectedIndex = index;
      }
   }
}
