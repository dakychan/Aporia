package ru.module;

import net.jqwik.api.*;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.ModuleToggleEvent;
import ru.ui.notify.NotificationManager;
import ru.ui.notify.NotificationType;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModulePropertyTest {
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void moduleToggleTriggersNotificationWithCorrectContent(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category,
            @ForAll boolean enableState) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        NotificationManager manager = NotificationManager.getInstance();
        int initialNotificationCount = manager.getActiveNotifications().size();
        
        module.setEnabled(enableState);
        
        var notifications = manager.getActiveNotifications();
        int newNotificationCount = notifications.size();
        
        if (enableState) {
            assertTrue(newNotificationCount > initialNotificationCount, 
                "Notification should be triggered when enabling");
            
            var notification = notifications.get(notifications.size() - 1);
            assertEquals(NotificationType.MODULE, notification.getType());
            assertTrue(notification.getMessage().contains(moduleName));
            assertTrue(notification.getMessage().contains("включен"));
        } else {
            assertEquals(initialNotificationCount, newNotificationCount, 
                "No notification should be triggered when state doesn't change (false -> false)");
        }
    }
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void moduleToggleFiresExactlyOneEvent(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category,
            @ForAll boolean enableState) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        AtomicInteger eventCount = new AtomicInteger(0);
        EventSystemImpl.getInstance().subscribe(ModuleToggleEvent.class, event -> {
            eventCount.incrementAndGet();
        });
        
        module.setEnabled(enableState);
        
        assertEquals(1, eventCount.get(), "Exactly one ModuleToggleEvent should be fired");
    }
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void moduleToggleEventContainsCorrectData(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category,
            @ForAll boolean enableState) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        AtomicReference<ModuleToggleEvent> capturedEvent = new AtomicReference<>();
        EventSystemImpl.getInstance().subscribe(ModuleToggleEvent.class, capturedEvent::set);
        
        module.setEnabled(enableState);
        
        assertNotNull(capturedEvent.get());
        assertEquals(module, capturedEvent.get().module);
        assertEquals(enableState, capturedEvent.get().enabled);
    }
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void noNotificationWhenStateUnchanged(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        module.setEnabled(false);
        NotificationManager manager = NotificationManager.getInstance();
        int initialCount = manager.getActiveNotifications().size();
        
        module.setEnabled(false);
        
        assertEquals(initialCount, manager.getActiveNotifications().size());
    }
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void multipleTogglesProduceMultipleNotifications(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category,
            @ForAll("toggleCounts") int toggleCount) {
        
        EventSystemImpl.getInstance().clear();
        NotificationManager.getInstance().getActiveNotifications().clear();
        TestModule module = new TestModule(moduleName, category);
        
        for (int i = 0; i < toggleCount; i++) {
            module.toggle();
        }
        
        NotificationManager manager = NotificationManager.getInstance();
        assertTrue(manager.getActiveNotifications().size() >= toggleCount);
    }
    
    @Provide
    Arbitrary<String> moduleNames() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(3)
                .ofMaxLength(20);
    }
    
    @Provide
    Arbitrary<Module.Category> categories() {
        return Arbitraries.of(Module.Category.values());
    }
    
    @Provide
    Arbitrary<Integer> toggleCounts() {
        return Arbitraries.integers().between(1, 5);
    }
    
    static class TestModule extends Module {
        public TestModule(String name, Category category) {
            super(name, category);
        }
        
        @Override
        public void onEnable() {
        }
        
        @Override
        public void onDisable() {
        }
        
        @Override
        public void onTick() {
        }
    }
}
