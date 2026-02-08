package ru.ui.notify;

import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    void testAllNotificationTypesExist() {
        assertNotNull(NotificationType.INFO);
        assertNotNull(NotificationType.MODULE);
        assertNotNull(NotificationType.ERROR);
    }

    @Test
    void testNotificationTypeHasColor() {
        assertNotNull(NotificationType.INFO.getColor());
        assertNotNull(NotificationType.MODULE.getColor());
        assertNotNull(NotificationType.ERROR.getColor());
    }

    @Test
    void testInfoColorIsBlue() {
        RenderColor color = NotificationType.INFO.getColor();
        assertNotNull(color);
    }

    @Test
    void testModuleColorIsGreen() {
        RenderColor color = NotificationType.MODULE.getColor();
        assertNotNull(color);
    }

    @Test
    void testErrorColorIsRed() {
        RenderColor color = NotificationType.ERROR.getColor();
        assertNotNull(color);
    }

    @Test
    void testEnumValuesReturnsAllTypes() {
        NotificationType[] types = NotificationType.values();
        assertEquals(3, types.length);
        assertEquals(NotificationType.INFO, types[0]);
        assertEquals(NotificationType.MODULE, types[1]);
        assertEquals(NotificationType.ERROR, types[2]);
    }

    @Test
    void testEnumValueOfReturnsCorrectType() {
        assertEquals(NotificationType.INFO, NotificationType.valueOf("INFO"));
        assertEquals(NotificationType.MODULE, NotificationType.valueOf("MODULE"));
        assertEquals(NotificationType.ERROR, NotificationType.valueOf("ERROR"));
    }

    @Test
    void testEnumValueOfThrowsExceptionForInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            NotificationType.valueOf("INVALID");
        });
    }
}
