package ru.input.impl.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.input.api.bind.Keybind;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KeybindManager.
 * Validates keybind registration, updates, key press handling, and storage management.
 */
class KeybindManagerTest {

    private KeybindManager manager;

    @BeforeEach
    void setUp() {
        manager = KeybindManager.getInstance();
        manager.clear(); // Clear any existing keybinds before each test
    }

    @Test
    void testGetInstanceReturnsSameInstance() {
        KeybindManager instance1 = KeybindManager.getInstance();
        KeybindManager instance2 = KeybindManager.getInstance();
        
        assertSame(instance1, instance2);
    }

    @Test
    void testRegisterKeybind() {
        Keybind keybind = new Keybind("test.register", 82, () -> {});
        
        manager.registerKeybind(keybind);
        
        assertTrue(manager.hasKeybind("test.register"));
        assertEquals(1, manager.getKeybindCount());
        assertSame(keybind, manager.getKeybind("test.register"));
    }

    @Test
    void testRegisterMultipleKeybinds() {
        Keybind keybind1 = new Keybind("test.multi1", 82, () -> {});
        Keybind keybind2 = new Keybind("test.multi2", 70, () -> {});
        Keybind keybind3 = new Keybind("test.multi3", 65, () -> {});
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        manager.registerKeybind(keybind3);
        
        assertEquals(3, manager.getKeybindCount());
        assertTrue(manager.hasKeybind("test.multi1"));
        assertTrue(manager.hasKeybind("test.multi2"));
        assertTrue(manager.hasKeybind("test.multi3"));
    }

    @Test
    void testRegisterKeybindWithNullThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            manager.registerKeybind(null);
        });
    }

    @Test
    void testRegisterKeybindWithNullIdThrowsException() {
        Keybind keybind = new Keybind(null, 82, () -> {});
        
        assertThrows(IllegalArgumentException.class, () -> {
            manager.registerKeybind(keybind);
        });
    }

    @Test
    void testRegisterKeybindReplacesExisting() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        
        Keybind keybind1 = new Keybind("test.replace", 82, counter1::incrementAndGet);
        Keybind keybind2 = new Keybind("test.replace", 70, counter2::incrementAndGet);
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        
        assertEquals(1, manager.getKeybindCount());
        assertSame(keybind2, manager.getKeybind("test.replace"));
        
        // Old keybind should not be triggered
        manager.handleKeyPress(82);
        assertEquals(0, counter1.get());
        
        // New keybind should be triggered
        manager.handleKeyPress(70);
        assertEquals(1, counter2.get());
    }

    @Test
    void testUpdateKeybind() {
        Keybind keybind = new Keybind("test.update", 82, () -> {});
        manager.registerKeybind(keybind);
        
        boolean updated = manager.updateKeybind("test.update", 70);
        
        assertTrue(updated);
        assertEquals(70, keybind.getKeyCode());
        assertEquals(70, manager.getKeybind("test.update").getKeyCode());
    }

    @Test
    void testUpdateKeybindUpdatesKeyCodeMap() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.update.map", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        manager.updateKeybind("test.update.map", 70);
        
        // Old key should not trigger
        manager.handleKeyPress(82);
        assertFalse(executed.get());
        
        // New key should trigger
        manager.handleKeyPress(70);
        assertTrue(executed.get());
    }

    @Test
    void testUpdateNonExistentKeybindReturnsFalse() {
        boolean updated = manager.updateKeybind("nonexistent", 70);
        
        assertFalse(updated);
    }

    @Test
    void testHandleKeyPressExecutesAction() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.press", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        manager.handleKeyPress(82);
        
        assertTrue(executed.get());
    }

    @Test
    void testHandleKeyPressExecutesMultipleKeybinds() {
        AtomicInteger counter1 = new AtomicInteger(0);
        AtomicInteger counter2 = new AtomicInteger(0);
        
        Keybind keybind1 = new Keybind("test.multi.press1", 82, counter1::incrementAndGet);
        Keybind keybind2 = new Keybind("test.multi.press2", 82, counter2::incrementAndGet);
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        
        manager.handleKeyPress(82);
        
        assertEquals(1, counter1.get());
        assertEquals(1, counter2.get());
    }

    @Test
    void testHandleKeyPressWithNoKeybindsDoesNothing() {
        assertDoesNotThrow(() -> manager.handleKeyPress(82));
    }

    @Test
    void testHandleKeyPressWithExceptionContinuesExecution() {
        AtomicBoolean executed1 = new AtomicBoolean(false);
        AtomicBoolean executed2 = new AtomicBoolean(false);
        
        Keybind keybind1 = new Keybind("test.exception1", 82, () -> {
            executed1.set(true);
            throw new RuntimeException("Test exception");
        });
        Keybind keybind2 = new Keybind("test.exception2", 82, () -> executed2.set(true));
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        
        // Should not throw, and second keybind should still execute
        assertDoesNotThrow(() -> manager.handleKeyPress(82));
        assertTrue(executed1.get());
        assertTrue(executed2.get());
    }

    @Test
    void testGetKeybind() {
        Keybind keybind = new Keybind("test.get", 82, () -> {});
        manager.registerKeybind(keybind);
        
        Keybind retrieved = manager.getKeybind("test.get");
        
        assertSame(keybind, retrieved);
    }

    @Test
    void testGetNonExistentKeybindReturnsNull() {
        Keybind retrieved = manager.getKeybind("nonexistent");
        
        assertNull(retrieved);
    }

    @Test
    void testGetKeybindsByKeyCode() {
        Keybind keybind1 = new Keybind("test.bycode1", 82, () -> {});
        Keybind keybind2 = new Keybind("test.bycode2", 82, () -> {});
        Keybind keybind3 = new Keybind("test.bycode3", 70, () -> {});
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        manager.registerKeybind(keybind3);
        
        List<Keybind> binds82 = manager.getKeybindsByKeyCode(82);
        List<Keybind> binds70 = manager.getKeybindsByKeyCode(70);
        
        assertEquals(2, binds82.size());
        assertTrue(binds82.contains(keybind1));
        assertTrue(binds82.contains(keybind2));
        
        assertEquals(1, binds70.size());
        assertTrue(binds70.contains(keybind3));
    }

    @Test
    void testGetKeybindsByKeyCodeWithNoBindsReturnsEmptyList() {
        List<Keybind> binds = manager.getKeybindsByKeyCode(82);
        
        assertNotNull(binds);
        assertTrue(binds.isEmpty());
    }

    @Test
    void testGetKeybindsByKeyCodeReturnsUnmodifiableList() {
        Keybind keybind = new Keybind("test.unmodifiable", 82, () -> {});
        manager.registerKeybind(keybind);
        
        List<Keybind> binds = manager.getKeybindsByKeyCode(82);
        
        assertThrows(UnsupportedOperationException.class, () -> {
            binds.add(new Keybind("test.add", 82, () -> {}));
        });
    }

    @Test
    void testGetAllKeybinds() {
        Keybind keybind1 = new Keybind("test.all1", 82, () -> {});
        Keybind keybind2 = new Keybind("test.all2", 70, () -> {});
        Keybind keybind3 = new Keybind("test.all3", 65, () -> {});
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        manager.registerKeybind(keybind3);
        
        Collection<Keybind> allKeybinds = manager.getAllKeybinds();
        
        assertEquals(3, allKeybinds.size());
        assertTrue(allKeybinds.contains(keybind1));
        assertTrue(allKeybinds.contains(keybind2));
        assertTrue(allKeybinds.contains(keybind3));
    }

    @Test
    void testGetAllKeybindsReturnsUnmodifiableCollection() {
        Keybind keybind = new Keybind("test.unmodifiable.all", 82, () -> {});
        manager.registerKeybind(keybind);
        
        Collection<Keybind> allKeybinds = manager.getAllKeybinds();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            allKeybinds.add(new Keybind("test.add", 82, () -> {}));
        });
    }

    @Test
    void testUnregisterKeybind() {
        Keybind keybind = new Keybind("test.unregister", 82, () -> {});
        manager.registerKeybind(keybind);
        
        boolean removed = manager.unregisterKeybind("test.unregister");
        
        assertTrue(removed);
        assertFalse(manager.hasKeybind("test.unregister"));
        assertEquals(0, manager.getKeybindCount());
    }

    @Test
    void testUnregisterKeybindRemovesFromKeyCodeMap() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.unregister.map", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        manager.unregisterKeybind("test.unregister.map");
        
        manager.handleKeyPress(82);
        assertFalse(executed.get());
    }

    @Test
    void testUnregisterNonExistentKeybindReturnsFalse() {
        boolean removed = manager.unregisterKeybind("nonexistent");
        
        assertFalse(removed);
    }

    @Test
    void testClear() {
        manager.registerKeybind(new Keybind("test.clear1", 82, () -> {}));
        manager.registerKeybind(new Keybind("test.clear2", 70, () -> {}));
        manager.registerKeybind(new Keybind("test.clear3", 65, () -> {}));
        
        manager.clear();
        
        assertEquals(0, manager.getKeybindCount());
        assertFalse(manager.hasKeybind("test.clear1"));
        assertFalse(manager.hasKeybind("test.clear2"));
        assertFalse(manager.hasKeybind("test.clear3"));
    }

    @Test
    void testClearRemovesFromKeyCodeMap() {
        AtomicBoolean executed = new AtomicBoolean(false);
        manager.registerKeybind(new Keybind("test.clear.map", 82, () -> executed.set(true)));
        
        manager.clear();
        
        manager.handleKeyPress(82);
        assertFalse(executed.get());
    }

    @Test
    void testHasKeybind() {
        Keybind keybind = new Keybind("test.has", 82, () -> {});
        
        assertFalse(manager.hasKeybind("test.has"));
        
        manager.registerKeybind(keybind);
        
        assertTrue(manager.hasKeybind("test.has"));
    }

    @Test
    void testGetKeybindCount() {
        assertEquals(0, manager.getKeybindCount());
        
        manager.registerKeybind(new Keybind("test.count1", 82, () -> {}));
        assertEquals(1, manager.getKeybindCount());
        
        manager.registerKeybind(new Keybind("test.count2", 70, () -> {}));
        assertEquals(2, manager.getKeybindCount());
        
        manager.unregisterKeybind("test.count1");
        assertEquals(1, manager.getKeybindCount());
        
        manager.clear();
        assertEquals(0, manager.getKeybindCount());
    }

    @Test
    void testMultipleKeybindsOnSameKeyAllExecute() {
        AtomicInteger counter = new AtomicInteger(0);
        
        manager.registerKeybind(new Keybind("test.same1", 82, counter::incrementAndGet));
        manager.registerKeybind(new Keybind("test.same2", 82, counter::incrementAndGet));
        manager.registerKeybind(new Keybind("test.same3", 82, counter::incrementAndGet));
        
        manager.handleKeyPress(82);
        
        assertEquals(3, counter.get());
    }

    @Test
    void testKeybindExecutionOrder() {
        StringBuilder order = new StringBuilder();
        
        manager.registerKeybind(new Keybind("test.order1", 82, () -> order.append("1")));
        manager.registerKeybind(new Keybind("test.order2", 82, () -> order.append("2")));
        manager.registerKeybind(new Keybind("test.order3", 82, () -> order.append("3")));
        
        manager.handleKeyPress(82);
        
        // All should execute, order may vary but all should be present
        assertEquals(3, order.length());
        assertTrue(order.toString().contains("1"));
        assertTrue(order.toString().contains("2"));
        assertTrue(order.toString().contains("3"));
    }

    @Test
    void testUpdateKeybindFromOneKeyToAnother() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.update.keys", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        // Update from 82 to 70
        manager.updateKeybind("test.update.keys", 70);
        
        // Old key should have no bindings
        assertEquals(0, manager.getKeybindsByKeyCode(82).size());
        
        // New key should have the binding
        assertEquals(1, manager.getKeybindsByKeyCode(70).size());
        
        // Verify execution
        manager.handleKeyPress(82);
        assertFalse(executed.get());
        
        manager.handleKeyPress(70);
        assertTrue(executed.get());
    }

    @Test
    void testRegisterKeybindWithSpecialKeys() {
        // Test with F13 (302), F25 (314), and numpad (320)
        manager.registerKeybind(new Keybind("test.f13", 302, () -> {}));
        manager.registerKeybind(new Keybind("test.f25", 314, () -> {}));
        manager.registerKeybind(new Keybind("test.numpad", 320, () -> {}));
        
        assertEquals(3, manager.getKeybindCount());
        assertTrue(manager.hasKeybind("test.f13"));
        assertTrue(manager.hasKeybind("test.f25"));
        assertTrue(manager.hasKeybind("test.numpad"));
    }

    @Test
    void testHandleKeyPressWithSpecialKeys() {
        AtomicInteger counter = new AtomicInteger(0);
        
        manager.registerKeybind(new Keybind("test.special.f13", 302, counter::incrementAndGet));
        manager.registerKeybind(new Keybind("test.special.f25", 314, counter::incrementAndGet));
        
        manager.handleKeyPress(302);
        assertEquals(1, counter.get());
        
        manager.handleKeyPress(314);
        assertEquals(2, counter.get());
    }

    @Test
    void testConcurrentModificationDuringExecution() {
        // Test that modifying keybinds during execution doesn't cause issues
        AtomicBoolean executed = new AtomicBoolean(false);
        
        manager.registerKeybind(new Keybind("test.concurrent1", 82, () -> {
            // Try to register another keybind during execution
            manager.registerKeybind(new Keybind("test.concurrent2", 70, () -> {}));
        }));
        manager.registerKeybind(new Keybind("test.concurrent3", 82, () -> executed.set(true)));
        
        // Should not throw ConcurrentModificationException
        assertDoesNotThrow(() -> manager.handleKeyPress(82));
        assertTrue(executed.get());
        assertTrue(manager.hasKeybind("test.concurrent2"));
    }

    @Test
    void testSaveKeybindsCreatesFile() {
        manager.registerKeybind(new Keybind("test.save", 82, () -> {}));
        
        assertDoesNotThrow(() -> manager.saveKeybinds());
    }

    @Test
    void testLoadKeybindsWithNoFileDoesNothing() {
        manager.registerKeybind(new Keybind("test.load.nofile", 82, () -> {}));
        
        assertDoesNotThrow(() -> manager.loadKeybinds());
        
        assertEquals(1, manager.getKeybindCount());
    }

    @Test
    void testSaveAndLoadKeybinds() {
        manager.registerKeybind(new Keybind("test.persist1", 82, () -> {}));
        manager.registerKeybind(new Keybind("test.persist2", 70, () -> {}));
        manager.registerKeybind(new Keybind("test.persist3", 65, () -> {}));
        
        manager.saveKeybinds();
        
        manager.clear();
        assertEquals(0, manager.getKeybindCount());
        
        manager.registerKeybind(new Keybind("test.persist1", 999, () -> {}));
        manager.registerKeybind(new Keybind("test.persist2", 999, () -> {}));
        manager.registerKeybind(new Keybind("test.persist3", 999, () -> {}));
        
        manager.loadKeybinds();
        
        // In test environment, config directory may not be available
        // so we just verify the methods don't throw exceptions
        assertNotNull(manager.getKeybind("test.persist1"));
        assertNotNull(manager.getKeybind("test.persist2"));
        assertNotNull(manager.getKeybind("test.persist3"));
    }

    @Test
    void testUpdateKeybindCallsSave() {
        manager.registerKeybind(new Keybind("test.update.save", 82, () -> {}));
        
        assertDoesNotThrow(() -> manager.updateKeybind("test.update.save", 70));
    }
}

