package ru.input.api.bind;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class KeybindTest {

    @Test
    void testKeybindCreation() {
        Runnable action = () -> {};
        Keybind keybind = new Keybind("test.keybind", 82, action);
        assertNotNull(keybind);
        assertEquals("test.keybind", keybind.getId());
        assertEquals(82, keybind.getKeyCode());
        assertEquals(action, keybind.getAction());
    }

    @Test
    void testKeybindExecutesAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);
        Keybind keybind = new Keybind("test.execute", 82, action);
        assertFalse(executed.get());
        keybind.execute();
        assertTrue(executed.get());
    }

    @Test
    void testKeybindExecutesActionMultipleTimes() {
        AtomicInteger counter = new AtomicInteger(0);
        Runnable action = counter::incrementAndGet;
        Keybind keybind = new Keybind("test.multiple", 82, action);
        assertEquals(0, counter.get());
        keybind.execute();
        assertEquals(1, counter.get());
        keybind.execute();
        assertEquals(2, counter.get());
        keybind.execute();
        assertEquals(3, counter.get());
    }

    @Test
    void testKeybindExecuteWithNullActionDoesNotThrow() {
        Keybind keybind = new Keybind("test.null", 82, null);
        assertDoesNotThrow(keybind::execute);
    }

    @Test
    void testSetKeyCodeUpdatesKeyCode() {
        Keybind keybind = new Keybind("test.rebind", 82, () -> {});
        assertEquals(82, keybind.getKeyCode());
        keybind.setKeyCode(70);
        assertEquals(70, keybind.getKeyCode());
    }

    @Test
    void testSetKeyCodeMultipleTimes() {
        Keybind keybind = new Keybind("test.rebind.multiple", 82, () -> {});
        assertEquals(82, keybind.getKeyCode());
        keybind.setKeyCode(70);
        assertEquals(70, keybind.getKeyCode());
        keybind.setKeyCode(65);
        assertEquals(65, keybind.getKeyCode());
        keybind.setKeyCode(32);
        assertEquals(32, keybind.getKeyCode());
    }

    @Test
    void testSetKeyCodeDoesNotAffectAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Runnable action = () -> executed.set(true);
        Keybind keybind = new Keybind("test.rebind.action", 82, action);
        keybind.setKeyCode(70);
        assertFalse(executed.get());
        keybind.execute();
        assertTrue(executed.get());
    }

    @Test
    void testSetKeyCodeDoesNotAffectId() {
        Keybind keybind = new Keybind("test.rebind.id", 82, () -> {});
        assertEquals("test.rebind.id", keybind.getId());
        keybind.setKeyCode(70);
        assertEquals("test.rebind.id", keybind.getId());
    }

    @Test
    void testToStringContainsIdAndKeyCode() {
        Keybind keybind = new Keybind("test.tostring", 82, () -> {});
        String str = keybind.toString();
        assertTrue(str.contains("test.tostring"));
        assertTrue(str.contains("82"));
    }

    @Test
    void testEqualsWithSameId() {
        Keybind keybind1 = new Keybind("test.equals", 82, () -> {});
        Keybind keybind2 = new Keybind("test.equals", 70, () -> {});
        assertEquals(keybind1, keybind2);
    }

    @Test
    void testEqualsWithDifferentId() {
        Keybind keybind1 = new Keybind("test.equals1", 82, () -> {});
        Keybind keybind2 = new Keybind("test.equals2", 82, () -> {});
        assertNotEquals(keybind1, keybind2);
    }

    @Test
    void testEqualsWithSameInstance() {
        Keybind keybind = new Keybind("test.equals.same", 82, () -> {});
        assertEquals(keybind, keybind);
    }

    @Test
    void testEqualsWithNull() {
        Keybind keybind = new Keybind("test.equals.null", 82, () -> {});
        assertNotEquals(keybind, null);
    }

    @Test
    void testEqualsWithDifferentClass() {
        Keybind keybind = new Keybind("test.equals.class", 82, () -> {});
        assertNotEquals(keybind, "not a keybind");
    }

    @Test
    void testHashCodeConsistentWithEquals() {
        Keybind keybind1 = new Keybind("test.hashcode", 82, () -> {});
        Keybind keybind2 = new Keybind("test.hashcode", 70, () -> {});
        assertEquals(keybind1, keybind2);
        assertEquals(keybind1.hashCode(), keybind2.hashCode());
    }

    @Test
    void testHashCodeDifferentForDifferentIds() {
        Keybind keybind1 = new Keybind("test.hashcode1", 82, () -> {});
        Keybind keybind2 = new Keybind("test.hashcode2", 82, () -> {});
        assertNotEquals(keybind1.hashCode(), keybind2.hashCode());
    }

    @Test
    void testKeybindWithSpecialKeys() {
        Keybind keybind1 = new Keybind("test.f13", 302, () -> {});
        assertEquals(302, keybind1.getKeyCode());
        Keybind keybind2 = new Keybind("test.f25", 314, () -> {});
        assertEquals(314, keybind2.getKeyCode());
        Keybind keybind3 = new Keybind("test.numpad", 320, () -> {});
        assertEquals(320, keybind3.getKeyCode());
    }

    @Test
    void testKeybindWithNegativeKeyCode() {
        Keybind keybind = new Keybind("test.none", -1, () -> {});
        assertEquals(-1, keybind.getKeyCode());
    }

    @Test
    void testKeybindIdCannotBeNull() {
        assertThrows(NullPointerException.class, () -> {
            Keybind keybind = new Keybind(null, 82, () -> {});
            keybind.getId().length();
        });
    }

    @Test
    void testMultipleKeybindsWithSameKeyCode() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        Keybind keybind1 = new Keybind("test.multi1", 82, counter1::incrementAndGet);
        Keybind keybind2 = new Keybind("test.multi2", 82, counter2::incrementAndGet);
        keybind1.execute();
        assertEquals(1, counter1.get());
        assertEquals(0, counter2.get());
        keybind2.execute();
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    void testKeybindActionCanAccessExternalState() {
        StringBuilder builder = new StringBuilder();
        Runnable action = () -> builder.append("executed");
        Keybind keybind = new Keybind("test.external", 82, action);
        assertEquals("", builder.toString());
        keybind.execute();
        assertEquals("executed", builder.toString());
    }
}
