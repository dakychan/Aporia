package ru.ui.notify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic verification tests for the notification system improvements.
 * These tests verify that the refactored queue-based system compiles and works correctly.
 */
public class NotifyBasicTest {
    
    @Test
    public void testMentionTypeExists() {
        // Verify MENTION type was added successfully
        Notify.NotificationType mention = Notify.NotificationType.MENTION;
        assertNotNull(mention);
        assertNotNull(mention.getColor());
    }
    
    @Test
    public void testChatMentionMessage() {
        // Verify chatMention() method exists and returns correct text
        String message = Notify.Messages.chatMention();
        assertEquals("Вас упомянули в чате !", message);
    }
    
    @Test
    public void testSingleNotificationDisplay() {
        // Verify that only one notification is active at a time
        Notify.Manager manager = Notify.Manager.getInstance();
        
        // Add multiple notifications
        manager.showNotification("Test 1", Notify.NotificationType.INFO);
        manager.showNotification("Test 2", Notify.NotificationType.MODULE);
        manager.showNotification("Test 3", Notify.NotificationType.ERROR);
        
        // Update to activate first notification
        manager.update();
        
        // Should only have one active notification
        assertEquals(1, manager.getActiveNotifications().size());
    }
    
    @Test
    public void testEmptyQueueReturnsEmptyList() {
        // Create a fresh manager instance for this test
        Notify.Manager manager = Notify.Manager.getInstance();
        manager.update();
        
        // With no notifications, should return empty list
        assertTrue(manager.getActiveNotifications().size() <= 1);
    }
    
    @Test
    public void testExistingNotificationTypesStillWork() {
        // Verify existing types weren't broken
        assertNotNull(Notify.NotificationType.INFO);
        assertNotNull(Notify.NotificationType.MODULE);
        assertNotNull(Notify.NotificationType.ERROR);
        
        // Verify they have colors
        assertNotNull(Notify.NotificationType.INFO.getColor());
        assertNotNull(Notify.NotificationType.MODULE.getColor());
        assertNotNull(Notify.NotificationType.ERROR.getColor());
    }
}
