package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import java.util.ArrayList;
import java.util.List;
import ru.render.anim.Animation;
import ru.render.anim.Easings;

public class Notify {
   public static class Manager {
      private static Notify.Manager instance;
      private final List<Notify.Notification> notificationQueue = new ArrayList<>();
      private Notify.Notification activeNotification = null;
      private static final long DEFAULT_DURATION = 3000L;

      private Manager() {
      }

      public void showNotification(String message, Notify.NotificationType type) {
         this.showNotification(message, type, 3000L);
      }

      public void showNotification(String message, Notify.NotificationType type, long duration) {
         Notify.Notification notification = new Notify.Notification(message, type, duration);
         synchronized (this.notificationQueue) {
            if (type == Notify.NotificationType.MODULE) {
               String moduleName = this.extractModuleName(message);
               this.notificationQueue.removeIf(n -> n.getType() == Notify.NotificationType.MODULE && this.extractModuleName(n.getMessage()).equals(moduleName));
               if (this.activeNotification != null
                  && this.activeNotification.getType() == Notify.NotificationType.MODULE
                  && this.extractModuleName(this.activeNotification.getMessage()).equals(moduleName)) {
                  this.activeNotification = notification;
                  return;
               }
            }

            this.notificationQueue.add(notification);
         }
      }

      private String extractModuleName(String message) {
         if (message.contains(" включен")) {
            return message.substring(0, message.indexOf(" включен"));
         } else {
            return message.contains(" выключен") ? message.substring(0, message.indexOf(" выключен")) : message;
         }
      }

      public void update() {
         synchronized (this.notificationQueue) {
            if (this.activeNotification != null && (this.activeNotification.isExpired() || this.activeNotification.isDismissed())) {
               this.activeNotification = null;
            }

            if (this.activeNotification == null && !this.notificationQueue.isEmpty()) {
               this.activeNotification = this.notificationQueue.remove(0);
            }
         }
      }

      public List<Notify.Notification> getActiveNotifications() {
         synchronized (this.notificationQueue) {
            List<Notify.Notification> result = new ArrayList<>();
            if (this.activeNotification != null) {
               result.add(this.activeNotification);
            }

            return result;
         }
      }

      public static Notify.Manager getInstance() {
         if (instance == null) {
            instance = new Notify.Manager();
         }

         return instance;
      }
   }

   public static class Messages {
      public static String playerBrokeShield(String playerName) {
         return String.format("Игрок %s пробил щит!", playerName);
      }

      public static String moderatorJoinedSpectator() {
         return "Модератор зашел в спек!";
      }

      public static String potionsRunningOut() {
         return "Зелия заканчиваются!";
      }

      public static String moduleToggled(String moduleName, boolean enabled) {
         return String.format("Модуль %s %s", moduleName, enabled ? "включен" : "выключен");
      }

      public static String moduleEnabled(String moduleName) {
         return String.format("%s был/была включена", moduleName);
      }

      public static String moduleToggledByUser(String moduleName, boolean enabled) {
         return String.format("Вы %s %s", enabled ? "включили" : "выключили", moduleName);
      }

      public static String moduleEnableFailed(String moduleName) {
         return String.format("Не удалось включить модуль %s", moduleName);
      }

      public static String folderCreationFailed() {
         return "Не смог создать папки";
      }

      public static String moduleActivationFailed() {
         return "Не удалось активировать / деактивировать модуль";
      }

      public static String bindingModule(String moduleName) {
         return String.format("Биндим %s на ..", moduleName);
      }

      public static String bindingModuleSetting(String moduleName, String settingName) {
         return String.format("Биндим %s, %s на ..", moduleName, settingName);
      }

      public static String keyPressed(String keyName) {
         return String.format("Нажата клавиша: %s", keyName);
      }

      public static String chatMention() {
         return "Вас упомянули в чате !";
      }
   }

   public static class Notification {
      private final String message;
      private final Notify.NotificationType type;
      private final long createdTime;
      private final long duration;
      private final Animation slideAnimation;
      private final Animation progressAnimation;
      private boolean dismissed = false;
      private static final long SLIDE_DURATION_MS = 300L;

      public Notification(String message, Notify.NotificationType type, long duration) {
         this.message = message;
         this.type = type;
         this.createdTime = System.currentTimeMillis();
         this.duration = duration;
         this.slideAnimation = new Animation();
         this.progressAnimation = new Animation();
      }

      public boolean isExpired() {
         return System.currentTimeMillis() - this.createdTime >= this.duration;
      }

      public float getProgress() {
         long elapsed = System.currentTimeMillis() - this.createdTime;
         float normalizedTime = Math.min((float)elapsed / (float)this.duration, 1.0F);
         return (float)Easings.LINEAR.ease(normalizedTime);
      }

      public float getSlideOffset() {
         long elapsed = System.currentTimeMillis() - this.createdTime;
         float normalizedTime = Math.min((float)elapsed / 300.0F, 1.0F);
         return (float)Easings.CUBIC_IN_OUT.ease(normalizedTime);
      }

      public void dismiss() {
         this.dismissed = true;
      }

      public boolean isDismissed() {
         return this.dismissed;
      }

      public String getMessage() {
         return this.message;
      }

      public Notify.NotificationType getType() {
         return this.type;
      }
   }

   public static enum NotificationType {
      INFO(RenderColor.of(60, 120, 245, 230)),
      MODULE(RenderColor.of(80, 200, 120, 230)),
      ERROR(RenderColor.of(245, 80, 80, 230)),
      MENTION(RenderColor.of(255, 200, 50, 230));

      private final RenderColor color;

      private NotificationType(RenderColor color) {
         this.color = color;
      }

      public RenderColor getColor() {
         return this.color;
      }
   }
}
