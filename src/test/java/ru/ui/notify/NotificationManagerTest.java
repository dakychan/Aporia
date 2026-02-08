package ru.ui.notify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NotificationManagerTest {

    private NotificationManager manager;

    @BeforeEach
    void setUp() {
        manager = NotificationManager.getInstance();
        while (!manager.getActiveNotifications().isEmpty()) {
            manager.getActiveNotifications().get(0).dismiss();
            manager.update();
        }
    }

    @Test
    void testGetInstanceReturnsSingleton() {
        NotificationManager instance1 = NotificationManager.getInstance();
        NotificationManager instance2 = NotificationManager.getInstance();
        assertSame(instance1, instance2);
    }

    @Test
    void testShowNotificationAddsToActiveList() {
        manager.showNotification("Test", NotificationType.INFO);
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(1, active.size());
        assertEquals("Test", active.get(0).getMessage());
    }

    @Test
    void testShowNotificationWithDefaultDuration() {
        manager.showNotification("Test", NotificationType.INFO);
        List<Notification> active = manager.getActiveNotifications();
        assertFalse(active.isEmpty());
    }

    @Test
    void testShowNotificationWithCustomDuration() {
        manager.showNotification("Test", NotificationType.MODULE, 5000);
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(1, active.size());
    }

    @Test
    void testMultipleNotificationsStacked() {
        manager.showNotification("First", NotificationType.INFO);
        manager.showNotification("Second", NotificationType.MODULE);
        manager.showNotification("Third", NotificationType.ERROR);
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(3, active.size());
    }

    @Test
    void testMaxNotificationsLimit() {
        for (int i = 0; i < 10; i++) {
            manager.showNotification("Notification " + i, NotificationType.INFO);
        }
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(5, active.size());
    }

    @Test
    void testMaxNotificationsRemovesOldest() {
        for (int i = 0; i < 7; i++) {
            manager.showNotification("Notification " + i, NotificationType.INFO);
        }
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(5, active.size());
        assertEquals("Notification 2", active.get(0).getMessage());
        assertEquals("Notification 6", active.get(4).getMessage());
    }

    @Test
    void testUpdateRemovesExpiredNotifications() throws InterruptedException {
        manager.showNotification("Short", NotificationType.INFO, 100);
        manager.showNotification("Long", NotificationType.INFO, 5000);
        
        Thread.sleep(150);
        manager.update();
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(1, active.size());
        assertEquals("Long", active.get(0).getMessage());
    }

    @Test
    void testUpdateRemovesDismissedNotifications() {
        manager.showNotification("Test1", NotificationType.INFO);
        manager.showNotification("Test2", NotificationType.INFO);
        
        List<Notification> active = manager.getActiveNotifications();
        active.get(0).dismiss();
        
        manager.update();
        
        List<Notification> afterUpdate = manager.getActiveNotifications();
        assertEquals(1, afterUpdate.size());
        assertEquals("Test2", afterUpdate.get(0).getMessage());
    }

    @Test
    void testUpdateWithNoNotifications() {
        manager.update();
        List<Notification> active = manager.getActiveNotifications();
        assertTrue(active.isEmpty());
    }

    @Test
    void testGetActiveNotificationsReturnsDefensiveCopy() {
        manager.showNotification("Test", NotificationType.INFO);
        List<Notification> active1 = manager.getActiveNotifications();
        List<Notification> active2 = manager.getActiveNotifications();
        
        assertNotSame(active1, active2);
        assertEquals(active1.size(), active2.size());
    }

    @Test
    void testNotificationTypesPreserved() {
        manager.showNotification("Info", NotificationType.INFO);
        manager.showNotification("Module", NotificationType.MODULE);
        manager.showNotification("Error", NotificationType.ERROR);
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(NotificationType.INFO, active.get(0).getType());
        assertEquals(NotificationType.MODULE, active.get(1).getType());
        assertEquals(NotificationType.ERROR, active.get(2).getType());
    }

    @Test
    void testEmptyActiveNotificationsInitially() {
        List<Notification> active = manager.getActiveNotifications();
        assertTrue(active.isEmpty());
    }

    @Test
    void testConcurrentNotificationAddition() {
        for (int i = 0; i < 3; i++) {
            manager.showNotification("Concurrent " + i, NotificationType.INFO);
        }
        
        List<Notification> active = manager.getActiveNotifications();
        assertEquals(3, active.size());
    }

    @Test
    void testUpdateMultipleTimes() throws InterruptedException {
        manager.showNotification("Test", NotificationType.INFO, 100);
        
        manager.update();
        assertFalse(manager.getActiveNotifications().isEmpty());
        
        Thread.sleep(150);
        manager.update();
        assertTrue(manager.getActiveNotifications().isEmpty());
    }
}
