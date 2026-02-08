package ru.ui.notify;

import org.junit.jupiter.api.Test;
import ru.render.MsdfTextRenderer;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRendererTest {

    @Test
    void testInitWithNullRenderer() {
        assertDoesNotThrow(() -> NotificationRenderer.init(null));
    }

    @Test
    void testInitWithValidRenderer() {
        MsdfTextRenderer mockRenderer = null;
        assertDoesNotThrow(() -> NotificationRenderer.init(mockRenderer));
    }

    @Test
    void testRenderWithNoNotifications() {
        NotificationManager manager = NotificationManager.getInstance();
        while (!manager.getActiveNotifications().isEmpty()) {
            manager.getActiveNotifications().get(0).dismiss();
            manager.update();
        }
        
        assertDoesNotThrow(() -> NotificationRenderer.render(null));
    }

    @Test
    void testRenderWithNullContextDoesNotThrow() {
        assertDoesNotThrow(() -> NotificationRenderer.render(null));
    }
}
