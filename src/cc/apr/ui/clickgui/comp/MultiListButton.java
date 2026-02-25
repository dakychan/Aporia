package cc.apr.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import java.util.ArrayList;
import java.util.List;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.render.RectRenderer;
import cc.apr.render.anim.Animation;
import cc.apr.render.anim.Easings;
import cc.apr.utils.math.MathHelper;

public class MultiListButton {
   private String name;
   private List<String> options;
   private List<Boolean> selected;
   private boolean expanded = false;
   private int x;
   private int y;
   private int width;
   private int height;
   private Animation expandAnimation = new Animation();
   private Animation hoverAnimation = new Animation();
   private int hoveredIndex = -1;
   private static final String ARROW_DOWN = "V";

   public MultiListButton(String name, String... options) {
      this.name = name;
      this.options = new ArrayList<>();
      this.selected = new ArrayList<>();

      for (String option : options) {
         this.options.add(option);
         this.selected.add(false);
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
         String headerText = this.expanded ? this.name : "Нажми, чтобы открыть";
         textRenderer.drawText(x + 8, y + 8, 12.0F, headerText, RenderColor.WHITE);
         float arrowRotation = expandProgress * 180.0F;
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

               boolean isSelected = this.selected.get(i);
               RenderColor optionColor = RenderColor.of(
                  isSelected ? 60 : (hovered ? 50 : 40),
                  isSelected ? 120 : (hovered ? 50 : 40),
                  isSelected ? 245 : (hovered ? 60 : 50),
                  (int)(itemAlpha * (isSelected ? 200 : 160))
               );
               RectRenderer.drawRoundedRect((float)(x + 5), (float)optionY, (float)(width - 10), 20.0F, 4.0F, optionColor);
               RenderColor checkColor = isSelected
                  ? RenderColor.of(80, 200, 120, (int)(itemAlpha * 255.0F))
                  : RenderColor.of(100, 100, 110, (int)(itemAlpha * 200.0F));
               RectRenderer.drawRoundedRect((float)(x + 10), (float)(optionY + 5), 10.0F, 10.0F, 2.0F, checkColor);
               if (textRenderer != null) {
                  RenderColor textColor = RenderColor.of(255, 255, 255, (int)(itemAlpha * 255.0F));
                  textRenderer.drawText(x + 25, optionY + 5, 11.0F, this.options.get(i), textColor);
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
         this.selected.set(this.hoveredIndex, !this.selected.get(this.hoveredIndex));
         return true;
      } else {
         return false;
      }
   }

   public int getHeight() {
      return this.height;
   }

   public List<Boolean> getSelected() {
      return new ArrayList<>(this.selected);
   }

   public void setSelected(int index, boolean value) {
      if (index >= 0 && index < this.selected.size()) {
         this.selected.set(index, value);
      }
   }
}
