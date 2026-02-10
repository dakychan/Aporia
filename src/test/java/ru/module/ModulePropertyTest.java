package ru.module;

import net.jqwik.api.*;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.ModuleToggleEvent;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModulePropertyTest {
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void moduleToggleTriggersNotificationOnStateChange(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        module.setEnabled(true);
        
        Notify.Manager manager = Notify.Manager.getInstance();
        var notifications = manager.getActiveNotifications();
        
        boolean foundNotification = notifications.stream()
                .anyMatch(n -> n.getMessage().contains(moduleName) && 
                              n.getMessage().contains("включен") &&
                              n.getType() == Notify.NotificationType.MODULE);
        
        assertTrue(foundNotification, "Notification should be triggered when enabling module");
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
        
        Notify.Manager manager = Notify.Manager.getInstance();
        
        long notificationsBefore = manager.getActiveNotifications().stream()
                .filter(n -> n.getMessage().contains(moduleName))
                .count();
        
        module.setEnabled(false);
        
        long notificationsAfter = manager.getActiveNotifications().stream()
                .filter(n -> n.getMessage().contains(moduleName))
                .count();
        
        assertEquals(notificationsBefore, notificationsAfter, 
            "No new notification should be added when state doesn't change");
    }
    
    @Property
    @Tag("Feature: client-refactoring-and-notifications, Property 12: Module Toggle Notification")
    void multipleTogglesProduceMultipleNotifications(
            @ForAll("moduleNames") String moduleName,
            @ForAll("categories") Module.Category category,
            @ForAll("toggleCounts") int toggleCount) {
        
        EventSystemImpl.getInstance().clear();
        TestModule module = new TestModule(moduleName, category);
        
        for (int i = 0; i < toggleCount; i++) {
            module.toggle();
        }
        
        Notify.Manager manager = Notify.Manager.getInstance();
        long moduleNotifications = manager.getActiveNotifications().stream()
                .filter(n -> n.getMessage().contains(moduleName))
                .count();
        
        assertTrue(moduleNotifications >= Math.min(toggleCount, 5), 
            "Should have notifications for toggles (up to MAX_NOTIFICATIONS limit)");
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
            super(name, "Test description", category, -1);
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
