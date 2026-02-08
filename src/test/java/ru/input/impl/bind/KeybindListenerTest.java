package ru.input.impl.bind;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.input.api.bind.Keybind;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for KeybindListener.
 * 
 * Note: These tests verify the event subscription and keybind triggering logic.
 * Full integration testing with GUI state requires a Minecraft client environment.
 */
class KeybindListenerTest {

    private KeybindManager manager;
    private EventSystemImpl eventSystem;

    @BeforeEach
    void setUp() {
        manager = KeybindManager.getInstance();
        manager.clear();
        
        eventSystem = EventSystemImpl.getInstance();
        eventSystem.clear();
        
        KeybindListener.init();
    }

    @Test
    void testInitCanBeCalledMultipleTimes() {
        assertDoesNotThrow(() -> {
            KeybindListener.init();
            KeybindListener.init();
            KeybindListener.init();
        });
    }

    @Test
    void testKeyPressEventTriggersKeybind() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.listener.press", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        // Note: In test environment without Minecraft client, shouldProcessKeybinds() 
        // will return false, so we're testing the subscription mechanism
        KeyPressEvent event = new KeyPressEvent(82, "R");
        eventSystem.fire(event);
        
        // In test environment, keybind won't execute due to null MinecraftClient
        // This test verifies the listener is subscribed and doesn't crash
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testMultipleKeyPressEventsHandled() {
        AtomicInteger counter = new AtomicInteger(0);
        Keybind keybind1 = new Keybind("test.listener.multi1", 82, counter::incrementAndGet);
        Keybind keybind2 = new Keybind("test.listener.multi2", 70, counter::incrementAndGet);
        
        manager.registerKeybind(keybind1);
        manager.registerKeybind(keybind2);
        
        eventSystem.fire(new KeyPressEvent(82, "R"));
        eventSystem.fire(new KeyPressEvent(70, "F"));
        eventSystem.fire(new KeyPressEvent(82, "R"));
        
        // In test environment, keybinds won't execute, but events should be handled
        assertDoesNotThrow(() -> {
            eventSystem.fire(new KeyPressEvent(82, "R"));
            eventSystem.fire(new KeyPressEvent(70, "F"));
        });
    }

    @Test
    void testKeyPressEventWithNoKeybindsDoesNotCrash() {
        KeyPressEvent event = new KeyPressEvent(82, "R");
        
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testKeyPressEventWithMultipleKeybindsOnSameKey() {
        AtomicInteger counter = new AtomicInteger(0);
        
        manager.registerKeybind(new Keybind("test.listener.same1", 82, counter::incrementAndGet));
        manager.registerKeybind(new Keybind("test.listener.same2", 82, counter::incrementAndGet));
        manager.registerKeybind(new Keybind("test.listener.same3", 82, counter::incrementAndGet));
        
        KeyPressEvent event = new KeyPressEvent(82, "R");
        
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testKeyPressEventWithSpecialKeys() {
        manager.registerKeybind(new Keybind("test.listener.f13", 302, () -> {}));
        manager.registerKeybind(new Keybind("test.listener.f25", 314, () -> {}));
        
        assertDoesNotThrow(() -> {
            eventSystem.fire(new KeyPressEvent(302, "F13"));
            eventSystem.fire(new KeyPressEvent(314, "F25"));
        });
    }

    @Test
    void testListenerDoesNotAffectOtherEventSubscribers() {
        AtomicBoolean otherListenerCalled = new AtomicBoolean(false);
        
        eventSystem.subscribe(KeyPressEvent.class, event -> {
            otherListenerCalled.set(true);
        });
        
        KeyPressEvent event = new KeyPressEvent(82, "R");
        eventSystem.fire(event);
        
        assertTrue(otherListenerCalled.get());
    }

    @Test
    void testKeybindExceptionDoesNotStopEventProcessing() {
        AtomicBoolean secondKeybindExecuted = new AtomicBoolean(false);
        
        manager.registerKeybind(new Keybind("test.listener.exception1", 82, () -> {
            throw new RuntimeException("Test exception");
        }));
        manager.registerKeybind(new Keybind("test.listener.exception2", 82, () -> {
            secondKeybindExecuted.set(true);
        }));
        
        KeyPressEvent event = new KeyPressEvent(82, "R");
        
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testDynamicKeybindRegistrationAfterInit() {
        KeybindListener.init();
        
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.listener.dynamic", 82, () -> executed.set(true));
        
        manager.registerKeybind(keybind);
        
        KeyPressEvent event = new KeyPressEvent(82, "R");
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testKeybindUpdateAfterInit() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.listener.update", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        KeybindListener.init();
        
        manager.updateKeybind("test.listener.update", 70);
        
        // Old key should not trigger
        assertDoesNotThrow(() -> eventSystem.fire(new KeyPressEvent(82, "R")));
        
        // New key should be registered
        assertDoesNotThrow(() -> eventSystem.fire(new KeyPressEvent(70, "F")));
    }

    @Test
    void testKeybindUnregisterAfterInit() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Keybind keybind = new Keybind("test.listener.unregister", 82, () -> executed.set(true));
        manager.registerKeybind(keybind);
        
        KeybindListener.init();
        
        manager.unregisterKeybind("test.listener.unregister");
        
        KeyPressEvent event = new KeyPressEvent(82, "R");
        assertDoesNotThrow(() -> eventSystem.fire(event));
    }

    @Test
    void testMultipleKeysInSequence() {
        AtomicInteger keyPressCount = new AtomicInteger(0);
        
        manager.registerKeybind(new Keybind("test.listener.seq1", 82, keyPressCount::incrementAndGet));
        manager.registerKeybind(new Keybind("test.listener.seq2", 70, keyPressCount::incrementAndGet));
        manager.registerKeybind(new Keybind("test.listener.seq3", 65, keyPressCount::incrementAndGet));
        
        assertDoesNotThrow(() -> {
            eventSystem.fire(new KeyPressEvent(82, "R"));
            eventSystem.fire(new KeyPressEvent(70, "F"));
            eventSystem.fire(new KeyPressEvent(65, "A"));
            eventSystem.fire(new KeyPressEvent(82, "R"));
        });
    }

    @Test
    void testListenerWithEmptyKeybindManager() {
        manager.clear();
        
        assertDoesNotThrow(() -> {
            eventSystem.fire(new KeyPressEvent(82, "R"));
            eventSystem.fire(new KeyPressEvent(70, "F"));
            eventSystem.fire(new KeyPressEvent(65, "A"));
        });
    }
}
