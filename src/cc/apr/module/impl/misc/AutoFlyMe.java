package cc.apr.module.impl.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import cc.apr.module.Module;

public class AutoFlyMe extends Module {
   private long lastSpacePress = 0L;
   private int currentRepeat = 0;
   private boolean isExecuting = false;
   private boolean wasFalling = false;
   private double lastY = 0.0;
   private long lastFallCommandTime = 0L;
   private static final long FALL_COMMAND_COOLDOWN = 500L;
   private boolean receivedCantFlyMessage = false;
   private long cantFlyMessageTime = 0L;
   private static final long CANT_FLY_RETRY_DELAY = 100L;
   private final Module.NumberSetting timeDelay = new Module.NumberSetting("Задержка", 1.0, 0.1, 2.0, 0.1);
   private final Module.NumberSetting repeatCount = new Module.NumberSetting("Повторы", 1.0, 1.0, 5.0, 1.0);
   private final Module.BooleanSetting useFlyme = new Module.BooleanSetting("Использовать /flyme", false);
   private final Module.BooleanSetting onFall = new Module.BooleanSetting("При падении", true);
   private final Module.ModeSetting mode = new Module.ModeSetting("Режим", "Авто", "Авто", "Ручной", "Смешанный");

   public AutoFlyMe() {
      super("AutoFlyMe", "Автоматически прописывает /fly или /flyme при падении", Module.C.MISC, -1);
      this.addSetting(this.timeDelay);
      this.addSetting(this.repeatCount);
      this.addSetting(this.useFlyme);
      this.addSetting(this.onFall);
      this.addSetting(this.mode);
   }

   @Override
   public void onEnable() {
      this.lastSpacePress = 0L;
      this.currentRepeat = 0;
      this.isExecuting = false;
      this.wasFalling = false;
      this.receivedCantFlyMessage = false;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         this.lastY = mc.player.getY();
      }
   }

   @Override
   public void onDisable() {
      this.isExecuting = false;
      this.currentRepeat = 0;
      this.wasFalling = false;
      this.receivedCantFlyMessage = false;
   }

   @Override
   public void onTick() {
      if (this.isEnabled()) {
         Minecraft mc = Minecraft.getInstance();
         if (mc.player != null && mc.level != null) {
            if (this.onFall.getValue()) {
               this.checkFalling(mc);
            }

            if (mc.options.keyJump.isDefault() && !this.isExecuting) {
               this.isExecuting = true;
               this.lastSpacePress = System.currentTimeMillis();
               this.currentRepeat = 0;
            }

            if (this.isExecuting && this.currentRepeat < this.repeatCount.getValue().intValue()) {
               long elapsed = System.currentTimeMillis() - this.lastSpacePress;
               long delayMs = (long)(this.timeDelay.getValue() * 1000.0);
               if (elapsed >= delayMs * this.currentRepeat) {
                  this.executeCommand();
                  this.currentRepeat++;
                  if (this.currentRepeat >= this.repeatCount.getValue().intValue()) {
                     this.isExecuting = false;
                  }
               }
            }

            if (this.receivedCantFlyMessage) {
               long elapsed = System.currentTimeMillis() - this.cantFlyMessageTime;
               if (elapsed >= 100L) {
                  if (this.isFalling(mc)) {
                     this.executeCommand();
                     this.cantFlyMessageTime = System.currentTimeMillis();
                  } else {
                     this.receivedCantFlyMessage = false;
                  }
               }
            }
         }
      }
   }

   private void checkFalling(Minecraft mc) {
      boolean isFalling = this.isFalling(mc);
      if (isFalling && !this.wasFalling) {
         long currentTime = System.currentTimeMillis();
         if (currentTime - this.lastFallCommandTime >= 500L) {
            this.executeCommand();
            this.lastFallCommandTime = currentTime;
         }
      }

      this.wasFalling = isFalling;
      this.lastY = mc.player.getY();
   }

   private boolean isFalling(Minecraft mc) {
      return mc.player == null ? false : mc.player.getDeltaMovement().y < -0.1 && !mc.player.onGround() && !mc.player.getAbilities().flying;
   }

   private void executeCommand() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null) {
         String command = this.useFlyme.getValue() ? "flyme" : "fly";
         mc.player.connection.sendCommand(command);
      }
   }

   public void onChatMessage(Component message) {
      String text = message.getString().toLowerCase();
      if (text.contains("вы не можете летать") || text.contains("you cannot fly") || text.contains("you can't fly") || text.contains("flying is not enabled")) {
         this.receivedCantFlyMessage = true;
         this.cantFlyMessageTime = System.currentTimeMillis();
      }
   }
}
