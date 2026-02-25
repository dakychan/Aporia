package cc.apr.module.impl.visuals;

import net.minecraft.client.Minecraft;
import cc.apr.module.Module;
import cc.apr.ui.clickgui.ClickGuiScreen;

public class ClickGui extends Module {
   private final Module.BooleanSetting textOnlyMode = new Module.BooleanSetting("Text Only", false);

   public ClickGui() {
      super("ClickGui", "Гуи чита", Module.C.VISUALS, 96);
      this.addSetting(this.textOnlyMode);
   }

   @Override
   public void onEnable() {
      Minecraft mc = Minecraft.getInstance();
      ClickGuiScreen screen = new ClickGuiScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight());
      screen.setTextOnlyMode(this.textOnlyMode.getValue());
      mc.setScreen(screen);
      this.setEnabled(false);
   }

   @Override
   public void onDisable() {
   }

   @Override
   public void onTick() {
      if (Minecraft.getInstance().screen instanceof ClickGuiScreen) {
         ClickGuiScreen screen = (ClickGuiScreen)Minecraft.getInstance().screen;
         screen.setTextOnlyMode(this.textOnlyMode.getValue());
      }
   }
}
