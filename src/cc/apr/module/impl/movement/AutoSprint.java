package cc.apr.module.impl.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import cc.apr.module.Module;

public class AutoSprint extends Module {
   private final Module.BooleanSetting onlyForward = new Module.BooleanSetting("Только вперед", true);
   private final Module.BooleanSetting keepSprint = new Module.BooleanSetting("Сохранять спринт", false);
   private final Module.ModeSetting mode = new Module.ModeSetting("Режим", "Всегда", "Всегда", "При движении", "Умный");
   private final Module.NumberSetting delay = new Module.NumberSetting("Задержка", 0.0, 0.0, 10.0, 0.5);

   public AutoSprint() {
      super("AutoSprint", "Автоматически активирует спринт", Module.C.MOVEMENT);
      this.addSetting(this.onlyForward);
      this.addSetting(this.keepSprint);
      this.addSetting(this.mode);
      this.addSetting(this.delay);
   }

   @Override
   public void onEnable() {
   }

   @Override
   public void onDisable() {
      if (!this.keepSprint.getValue()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         if (player != null) {
            player.setSprinting(false);
         }
      }
   }

   @Override
   public void onTick() {
      Minecraft mc = Minecraft.getInstance();
      LocalPlayer player = mc.player;
      if (player != null) {
         boolean movingForward = mc.options.keyUp.isDown();
         if (this.onlyForward.getValue()) {
            if (movingForward && !player.isSprinting()) {
               player.setSprinting(true);
            }
         } else {
            boolean moving = movingForward || mc.options.keyLeft.isDown() || mc.options.keyRight.isDown() || mc.options.keyDown.isDown();
            if (moving && !player.isSprinting()) {
               player.setSprinting(true);
            }
         }
      }
   }
}
