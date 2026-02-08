package ru.ui.notify;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    private static NotificationManager instance;
    private final List<Notification> activeNotifications = new ArrayList<>();
    private static final int MAX_NOTIFICATIONS = 5;
    private static final long DEFAULT_DURATION = 3000;

    private NotificationManager() {}

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

    public static NotificationManager getInstance() {
        if (instance == null) {
            instance = new NotificationManager();
        }
        return instance;
    }
}
