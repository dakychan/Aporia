package ru.ui.notify;

import java.util.ArrayList;
import java.util.List;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import ru.render.Animation;
import ru.render.EasingFunction;

public class Notify {

    // ===== NotificationType =====
    public enum NotificationType {
        INFO(RenderColor.of(60, 120, 245, 230)),
        MODULE(RenderColor.of(80, 200, 120, 230)),
        ERROR(RenderColor.of(245, 80, 80, 230));

        private final RenderColor color;

        NotificationType(RenderColor color) {
            this.color = color;
        }

        public RenderColor getColor() {
            return color;
        }
    }

    // ===== Notification =====
    public static class Notification {
        private final String message;
        private final NotificationType type;
        private final long createdTime;
        private final long duration;
        private final Animation slideAnimation;
        private final Animation progressAnimation;
        private boolean dismissed = false;
        private static final long SLIDE_DURATION_MS = 300;

        public Notification(String message, NotificationType type, long duration) {
            this.message = message;
            this.type = type;
            this.createdTime = System.currentTimeMillis();
            this.duration = duration;

            this.slideAnimation = new Animation(SLIDE_DURATION_MS / 1000.0f);
            this.progressAnimation = new Animation(duration / 1000.0f);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - createdTime >= duration;
        }

        public float getProgress() {
            long elapsed = System.currentTimeMillis() - createdTime;
            float normalizedTime = Math.min((float) elapsed / duration, 1.0f);
            return EasingFunction.LINEAR.apply(normalizedTime);
        }

        public float getSlideOffset() {
            long elapsed = System.currentTimeMillis() - createdTime;
            float normalizedTime = Math.min((float) elapsed / SLIDE_DURATION_MS, 1.0f);
            return EasingFunction.EASE_IN_OUT_CUBIC.apply(normalizedTime);
        }

        public void dismiss() {
            dismissed = true;
        }

        public boolean isDismissed() {
            return dismissed;
        }

        public String getMessage() {
            return message;
        }

        public NotificationType getType() {
            return type;
        }
    }

    // ===== NotificationMessages =====
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
            return String.format("Модуль %s %s", moduleName,
                    enabled ? "включен" : "выключен");
        }

        public static String moduleEnabled(String moduleName) {
            return String.format("%s был/была включена", moduleName);
        }

        public static String moduleToggledByUser(String moduleName, boolean enabled) {
            return String.format("Вы %s %s",
                    enabled ? "включили" : "выключили",
                    moduleName);
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
    }

    // ===== NotificationManager =====
    public static class Manager {
        private static Manager instance;
        private final List<Notification> activeNotifications = new ArrayList<>();
        private static final int MAX_NOTIFICATIONS = 5;
        private static final long DEFAULT_DURATION = 3000;

        private Manager() {}

        public void showNotification(String message, NotificationType type) {
            showNotification(message, type, DEFAULT_DURATION);
        }

        public void showNotification(String message, NotificationType type, long duration) {
            Notification notification = new Notification(message, type, duration);

            synchronized (activeNotifications) {
                activeNotifications.add(notification);

                while (activeNotifications.size() > MAX_NOTIFICATIONS) {
                    activeNotifications.remove(0);
                }
            }
        }

        public void update() {
            synchronized (activeNotifications) {
                activeNotifications.removeIf(n -> n.isExpired() || n.isDismissed());
            }
        }

        public List<Notification> getActiveNotifications() {
            synchronized (activeNotifications) {
                return new ArrayList<>(activeNotifications);
            }
        }

        public static Manager getInstance() {
            if (instance == null) {
                instance = new Manager();
            }
            return instance;
        }
    }
}