package ru.ui.clickgui.comp;

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import java.util.List;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;

public class MultiSetting {
   private String name;
   private List<String> options;
   private List<String> selectedOptions;
   private int x;
   private int y;
   private int width;
   private int height;
   private MsdfTextRenderer textRenderer;
   private static final int HEADER_HEIGHT = 20;
   private static final int BUTTON_HEIGHT = 18;
   private static final int BUTTON_SPACING = 4;
   private static final int PADDING = 6;

   public MultiSetting(String name, List<String> options, List<String> selectedOptions) {
      this.name = name;
      this.options = options;
      this.selectedOptions = selectedOptions;
      this.height = 44;
   }

   public int getHeight() {
      int totalRows = 1;
      int currentRowWidth = 0;
      int maxWidth = this.width > 0 ? this.width : 150;

      for (String option : this.options) {
         float textWidth = 40.0F;
         if (this.textRenderer != null) {
            try {
               textWidth = this.textRenderer.measureWidth(option, 9.0F);
            } catch (Exception var8) {
               textWidth = option.length() * 6;
            }
         }

         int buttonWidth = (int)(textWidth + 12.0F);
         if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
            totalRows++;
            currentRowWidth = 0;
         }

         currentRowWidth += buttonWidth + 4;
      }

      return 20 + totalRows * 22 + 6;
   }

   public void render(int x, int y, int width, MsdfTextRenderer textRenderer, int mouseX, int mouseY, float alpha) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      plugin.bindMainFramebuffer(true);
      this.x = x;
      this.y = y;
      this.width = width;
      this.textRenderer = textRenderer;
      alpha = 1.0F;
      if (textRenderer != null) {
         textRenderer.drawText(x, y + 6, 10.0F, this.name, RenderColor.of(220, 220, 230, 255));
      }

      int buttonX = x;
      int buttonY = y + 20;
      int maxWidth = width;
      int currentRowWidth = 0;

      for (String option : this.options) {
         boolean isSelected = this.selectedOptions.contains(option);
         float textWidth = textRenderer != null ? textRenderer.measureWidth(option, 9.0F) : 40.0F;
         int buttonWidth = (int)(textWidth + 12.0F);
         if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
            buttonX = x;
            buttonY += 22;
            currentRowWidth = 0;
         }

         boolean hovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 18;
         RenderColor bgColor;
         if (isSelected) {
            bgColor = hovered ? RenderColor.of(70, 130, 255, 255) : RenderColor.of(60, 120, 245, 230);
         } else {
            bgColor = hovered ? RenderColor.of(60, 60, 70, 220) : RenderColor.of(50, 50, 60, 200);
         }

         RectRenderer.drawRoundedRect((float)buttonX, (float)buttonY, (float)buttonWidth, 18.0F, 3.0F, bgColor);
         if (textRenderer != null) {
            RenderColor textColor = isSelected ? RenderColor.WHITE : RenderColor.of(180, 180, 190, 255);
            textRenderer.drawText(buttonX + 6, buttonY + 8, 9.0F, option, textColor);
         }

         buttonX += buttonWidth + 4;
         currentRowWidth += buttonWidth + 4;
      }
   }

   public boolean mouseClicked(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      } else {
         int buttonX = this.x;
         int buttonY = this.y + 20;
         int maxWidth = this.width;
         int currentRowWidth = 0;

         for (String option : this.options) {
            float textWidth = 40.0F;
            if (this.textRenderer != null) {
               try {
                  textWidth = this.textRenderer.measureWidth(option, 9.0F);
               } catch (Exception var12) {
                  textWidth = option.length() * 6;
               }
            }

            int buttonWidth = (int)(textWidth + 12.0F);
            if (currentRowWidth + buttonWidth > maxWidth && currentRowWidth > 0) {
               buttonX = this.x;
               buttonY += 22;
               currentRowWidth = 0;
            }

            if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + 18) {
               this.toggleOption(option);
               return true;
            }

            buttonX += buttonWidth + 4;
            currentRowWidth += buttonWidth + 4;
         }

         return false;
      }
   }

   private void toggleOption(String option) {
      if (this.selectedOptions.contains(option)) {
         this.selectedOptions.remove(option);
      } else {
         this.selectedOptions.add(option);
      }
   }

   public void setBounds(int x, int y, int width) {
      this.x = x;
      this.y = y;
      this.width = width;
   }

   public boolean isExpanded() {
      return false;
   }

   public void setExpanded(boolean expanded) {
   }

   public boolean isClickOutside(int mouseX, int mouseY) {
      return false;
   }

   public void collapse() {
   }
}
