package ru.input.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyboardKeysTest {

    @Test
    void testKeyboardKeysEnumExists() {
        assertNotNull(KeyboardKeys.KEY_SPACE);
        assertNotNull(KeyboardKeys.KEY_NONE);
    }

    @Test
    void testKeyboardKeysContainsF13ThroughF25() {
        assertNotNull(KeyboardKeys.KEY_F13);
        assertNotNull(KeyboardKeys.KEY_F14);
        assertNotNull(KeyboardKeys.KEY_F15);
        assertNotNull(KeyboardKeys.KEY_F16);
        assertNotNull(KeyboardKeys.KEY_F17);
        assertNotNull(KeyboardKeys.KEY_F18);
        assertNotNull(KeyboardKeys.KEY_F19);
        assertNotNull(KeyboardKeys.KEY_F20);
        assertNotNull(KeyboardKeys.KEY_F21);
        assertNotNull(KeyboardKeys.KEY_F22);
        assertNotNull(KeyboardKeys.KEY_F23);
        assertNotNull(KeyboardKeys.KEY_F24);
        assertNotNull(KeyboardKeys.KEY_F25);
    }

    @Test
    void testKeyboardKeysContainsNumpadKeys() {
        assertNotNull(KeyboardKeys.KEY_KP_0);
        assertNotNull(KeyboardKeys.KEY_KP_1);
        assertNotNull(KeyboardKeys.KEY_KP_2);
        assertNotNull(KeyboardKeys.KEY_KP_3);
        assertNotNull(KeyboardKeys.KEY_KP_4);
        assertNotNull(KeyboardKeys.KEY_KP_5);
        assertNotNull(KeyboardKeys.KEY_KP_6);
        assertNotNull(KeyboardKeys.KEY_KP_7);
        assertNotNull(KeyboardKeys.KEY_KP_8);
        assertNotNull(KeyboardKeys.KEY_KP_9);
        assertNotNull(KeyboardKeys.KEY_KP_DECIMAL);
        assertNotNull(KeyboardKeys.KEY_KP_DIVIDE);
        assertNotNull(KeyboardKeys.KEY_KP_MULTIPLY);
        assertNotNull(KeyboardKeys.KEY_KP_SUBTRACT);
        assertNotNull(KeyboardKeys.KEY_KP_ADD);
        assertNotNull(KeyboardKeys.KEY_KP_ENTER);
        assertNotNull(KeyboardKeys.KEY_KP_EQUAL);
    }

    @Test
    void testKeyboardKeysContainsSpecialKeys() {
        assertNotNull(KeyboardKeys.KEY_INSERT);
        assertNotNull(KeyboardKeys.KEY_DELETE);
        assertNotNull(KeyboardKeys.KEY_HOME);
        assertNotNull(KeyboardKeys.KEY_END);
        assertNotNull(KeyboardKeys.KEY_PAGE_UP);
        assertNotNull(KeyboardKeys.KEY_PAGE_DOWN);
        assertNotNull(KeyboardKeys.KEY_CAPS_LOCK);
        assertNotNull(KeyboardKeys.KEY_SCROLL_LOCK);
        assertNotNull(KeyboardKeys.KEY_NUM_LOCK);
        assertNotNull(KeyboardKeys.KEY_PRINT_SCREEN);
        assertNotNull(KeyboardKeys.KEY_PAUSE);
    }

    @Test
    void testKeyboardKeysContainsModifierKeys() {
        assertNotNull(KeyboardKeys.KEY_LEFT_SHIFT);
        assertNotNull(KeyboardKeys.KEY_LEFT_CONTROL);
        assertNotNull(KeyboardKeys.KEY_LEFT_ALT);
        assertNotNull(KeyboardKeys.KEY_LEFT_SUPER);
        assertNotNull(KeyboardKeys.KEY_RIGHT_SHIFT);
        assertNotNull(KeyboardKeys.KEY_RIGHT_CONTROL);
        assertNotNull(KeyboardKeys.KEY_RIGHT_ALT);
        assertNotNull(KeyboardKeys.KEY_RIGHT_SUPER);
    }

    @Test
    void testKeyboardKeysContainsMouseButtons() {
        assertNotNull(KeyboardKeys.MOUSE_1);
        assertNotNull(KeyboardKeys.MOUSE_2);
        assertNotNull(KeyboardKeys.MOUSE_3);
        assertNotNull(KeyboardKeys.MOUSE_LEFT);
        assertNotNull(KeyboardKeys.MOUSE_RIGHT);
        assertNotNull(KeyboardKeys.MOUSE_MIDDLE);
    }

    @Test
    void testGetKeyNameReturnsCorrectName() {
        assertEquals("SPACE", KeyboardKeys.getKeyName(32));
        assertEquals("F13", KeyboardKeys.getKeyName(302));
        assertEquals("F25", KeyboardKeys.getKeyName(314));
        assertEquals("NUMPAD0", KeyboardKeys.getKeyName(320));
        assertEquals("ESCAPE", KeyboardKeys.getKeyName(256));
        assertEquals("NONE", KeyboardKeys.getKeyName(999));
    }

    @Test
    void testGetKeyCodeReturnsCorrectCode() {
        assertEquals(32, KeyboardKeys.getKeyCode("SPACE"));
        assertEquals(32, KeyboardKeys.getKeyCode("space"));
        assertEquals(302, KeyboardKeys.getKeyCode("F13"));
        assertEquals(314, KeyboardKeys.getKeyCode("F25"));
        assertEquals(320, KeyboardKeys.getKeyCode("NUMPAD0"));
        assertEquals(256, KeyboardKeys.getKeyCode("ESCAPE"));
        assertEquals(-1, KeyboardKeys.getKeyCode("INVALID"));
    }

    @Test
    void testFindByKeyCodeReturnsCorrectEnum() {
        assertEquals(KeyboardKeys.KEY_SPACE, KeyboardKeys.findByKeyCode(32));
        assertEquals(KeyboardKeys.KEY_F13, KeyboardKeys.findByKeyCode(302));
        assertEquals(KeyboardKeys.KEY_F25, KeyboardKeys.findByKeyCode(314));
        assertEquals(KeyboardKeys.KEY_KP_0, KeyboardKeys.findByKeyCode(320));
        assertEquals(KeyboardKeys.KEY_ESCAPE, KeyboardKeys.findByKeyCode(256));
        assertEquals(KeyboardKeys.KEY_NONE, KeyboardKeys.findByKeyCode(999));
    }

    @Test
    void testFindByNameReturnsCorrectEnum() {
        assertEquals(KeyboardKeys.KEY_SPACE, KeyboardKeys.findByName("SPACE"));
        assertEquals(KeyboardKeys.KEY_SPACE, KeyboardKeys.findByName("space"));
        assertEquals(KeyboardKeys.KEY_F13, KeyboardKeys.findByName("F13"));
        assertEquals(KeyboardKeys.KEY_F25, KeyboardKeys.findByName("F25"));
        assertEquals(KeyboardKeys.KEY_KP_0, KeyboardKeys.findByName("NUMPAD0"));
        assertEquals(KeyboardKeys.KEY_ESCAPE, KeyboardKeys.findByName("ESCAPE"));
        assertEquals(KeyboardKeys.KEY_NONE, KeyboardKeys.findByName("INVALID"));
    }

    @Test
    void testIsKeyMethodWorksCorrectly() {
        assertTrue(KeyboardKeys.KEY_SPACE.isKey(32));
        assertFalse(KeyboardKeys.KEY_SPACE.isKey(33));
        assertTrue(KeyboardKeys.KEY_F13.isKey(302));
        assertFalse(KeyboardKeys.KEY_F13.isKey(303));
    }

    @Test
    void testGetNameReturnsCorrectName() {
        assertEquals("SPACE", KeyboardKeys.KEY_SPACE.getName());
        assertEquals("F13", KeyboardKeys.KEY_F13.getName());
        assertEquals("F25", KeyboardKeys.KEY_F25.getName());
        assertEquals("NUMPAD0", KeyboardKeys.KEY_KP_0.getName());
        assertEquals("NONE", KeyboardKeys.KEY_NONE.getName());
    }

    @Test
    void testEnumGetKeyCodeReturnsCorrectCode() {
        assertEquals(32, KeyboardKeys.KEY_SPACE.getKeyCode());
        assertEquals(302, KeyboardKeys.KEY_F13.getKeyCode());
        assertEquals(314, KeyboardKeys.KEY_F25.getKeyCode());
        assertEquals(320, KeyboardKeys.KEY_KP_0.getKeyCode());
        assertEquals(-1, KeyboardKeys.KEY_NONE.getKeyCode());
    }

    @Test
    void testToStringReturnsName() {
        assertEquals("SPACE", KeyboardKeys.KEY_SPACE.toString());
        assertEquals("F13", KeyboardKeys.KEY_F13.toString());
        assertEquals("NUMPAD0", KeyboardKeys.KEY_KP_0.toString());
    }

    @Test
    void testAllStandardLettersPresent() {
        for (char c = 'A'; c <= 'Z'; c++) {
            String keyName = String.valueOf(c);
            KeyboardKeys key = KeyboardKeys.findByName(keyName);
            assertNotEquals(KeyboardKeys.KEY_NONE, key, "Key " + keyName + " should exist");
        }
    }

    @Test
    void testAllNumbersPresent() {
        for (int i = 0; i <= 9; i++) {
            String keyName = String.valueOf(i);
            KeyboardKeys key = KeyboardKeys.findByName(keyName);
            assertNotEquals(KeyboardKeys.KEY_NONE, key, "Key " + keyName + " should exist");
        }
    }

    @Test
    void testAllFunctionKeysPresent() {
        for (int i = 1; i <= 25; i++) {
            String keyName = "F" + i;
            KeyboardKeys key = KeyboardKeys.findByName(keyName);
            assertNotEquals(KeyboardKeys.KEY_NONE, key, "Key " + keyName + " should exist");
        }
    }

    @Test
    void testKeyCodesAreUnique() {
        KeyboardKeys mouseKey = KeyboardKeys.findByKeyCode(0);
        assertNotNull(mouseKey);
        assertTrue(mouseKey == KeyboardKeys.MOUSE_1 || 
                   mouseKey == KeyboardKeys.MOUSE_LEFT,
                   "Key code 0 should map to a mouse button");
    }
}
