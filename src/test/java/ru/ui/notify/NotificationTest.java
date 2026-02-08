package ru.ui.notify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTest {

    @Test
    void testNotificationCreation() {
        Notification notification = new Notification("Test message", NotificationType.INFO, 3000);
        
        assertNotNull(notification);
        assertEquals("Test message", notification.getMessage());
        assertEquals(NotificationType.INFO, notification.getType());
        assertFalse(notification.isDismissed());
    }

    @Test
    void testNotificationIsNotExpiredImmediately() {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        assertFalse(notification.isExpired());
    }

    @Test
    void testNotificationIsExpiredAfterDuration() throws InterruptedException {
        Notification notification = new Notification("Test", NotificationType.INFO, 100);
        Thread.sleep(150);
        assertTrue(notification.isExpired());
    }

    @Test
    void testNotificationProgressStartsAtZero() {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        float progress = notification.getProgress();
        assertTrue(progress >= 0.0f && progress <= 0.1f);
    }

    @Test
    void testNotificationProgressIncreasesOverTime() throws InterruptedException {
        Notification notification = new Notification("Test", NotificationType.INFO, 1000);
        float initialProgress = notification.getProgress();
        Thread.sleep(200);
        float laterProgress = notification.getProgress();
        assertTrue(laterProgress > initialProgress);
    }

    @Test
    void testNotificationProgressDoesNotExceedOne() throws InterruptedException {
        Notification notification = new Notification("Test", NotificationType.INFO, 100);
        Thread.sleep(200);
        float progress = notification.getProgress();
        assertTrue(progress <= 1.0f);
    }

    @Test
    void testSlideOffsetStartsAtZero() {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        float offset = notification.getSlideOffset();
        assertTrue(offset >= 0.0f && offset <= 0.1f);
    }

    @Test
    void testSlideOffsetIncreasesOverTime() throws InterruptedException {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        float initialOffset = notification.getSlideOffset();
        Thread.sleep(100);
        float laterOffset = notification.getSlideOffset();
        assertTrue(laterOffset > initialOffset);
    }

    @Test
    void testSlideOffsetDoesNotExceedOne() throws InterruptedException {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        Thread.sleep(400);
        float offset = notification.getSlideOffset();
        assertTrue(offset <= 1.0f);
    }

    @Test
    void testDismissMarksNotificationAsDismissed() {
        Notification notification = new Notification("Test", NotificationType.INFO, 3000);
        assertFalse(notification.isDismissed());
        notification.dismiss();
        assertTrue(notification.isDismissed());
    }

    @Test
    void testNotificationWithDifferentTypes() {
        Notification info = new Notification("Info", NotificationType.INFO, 3000);
        Notification module = new Notification("Module", NotificationType.MODULE, 3000);
        Notification error = new Notification("Error", NotificationType.ERROR, 3000);
        
        assertEquals(NotificationType.INFO, info.getType());
        assertEquals(NotificationType.MODULE, module.getType());
        assertEquals(NotificationType.ERROR, error.getType());
    }

    @Test
    void testNotificationWithLongDuration() {
        Notification notification = new Notification("Test", NotificationType.INFO, 10000);
        assertFalse(notification.isExpired());
        float progress = notification.getProgress();
        assertTrue(progress < 0.1f);
    }

    @Test
    void testNotificationMessagePreserved() {
        String message = "This is a test notification with special characters: !@#$%";
        Notification notification = new Notification(message, NotificationType.INFO, 3000);
        assertEquals(message, notification.getMessage());
    }

    @Test
    void testMultipleNotificationsIndependent() throws InterruptedException {
        Notification n1 = new Notification("First", NotificationType.INFO, 1000);
        Thread.sleep(100);
        Notification n2 = new Notification("Second", NotificationType.MODULE, 1000);
        
        float progress1 = n1.getProgress();
        float progress2 = n2.getProgress();
        
        assertTrue(progress1 > progress2);
    }
}
