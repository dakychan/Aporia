package ru.module;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.event.impl.EventSystemImpl;
import ru.event.impl.ModuleToggleEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ModuleTest {
    private TestModule module;
    
    @BeforeEach
    void setUp() {
        EventSystemImpl.getInstance().clear();
        module = new TestModule("TestModule", Module.Category.MISC);
    }
    
    @Test
    void setEnabledFiresModuleToggleEvent() {
        AtomicBoolean eventFired = new AtomicBoolean(false);
        AtomicReference<Module> eventModule = new AtomicReference<>();
        AtomicReference<Boolean> eventEnabled = new AtomicReference<>();
        
        EventSystemImpl.getInstance().subscribe(ModuleToggleEvent.class, event -> {
            eventFired.set(true);
            eventModule.set(event.module);
            eventEnabled.set(event.enabled);
        });
        
        module.setEnabled(true);
        
        assertTrue(eventFired.get());
        assertEquals(module, eventModule.get());
        assertTrue(eventEnabled.get());
    }
    
    @Test
    void setEnabledFiresEventForBothEnableAndDisable() {
        AtomicBoolean enableEventFired = new AtomicBoolean(false);
        AtomicBoolean disableEventFired = new AtomicBoolean(false);
        
        EventSystemImpl.getInstance().subscribe(ModuleToggleEvent.class, event -> {
            if (event.enabled) {
                enableEventFired.set(true);
            } else {
                disableEventFired.set(true);
            }
        });
        
        module.setEnabled(true);
        assertTrue(enableEventFired.get());
        
        module.setEnabled(false);
        assertTrue(disableEventFired.get());
    }
    
    @Test
    void setEnabledTriggersNotificationOnStateChange() {
        module.setEnabled(true);
        
        Notify.Manager manager = Notify.Manager.getInstance();
        assertFalse(manager.getActiveNotifications().isEmpty());
        
        var notification = manager.getActiveNotifications().get(0);
        assertTrue(notification.getMessage().contains("TestModule"));
        assertTrue(notification.getMessage().contains("включен"));
        assertEquals(Notify.NotificationType.MODULE, notification.getType());
    }
    
    @Test
    void setEnabledDoesNotTriggerNotificationWhenStateUnchanged() {
        module.setEnabled(false);
        
        Notify.Manager manager = Notify.Manager.getInstance();
        int initialCount = manager.getActiveNotifications().size();
        
        module.setEnabled(false);
        
        assertEquals(initialCount, manager.getActiveNotifications().size());
    }
    
    @Test
    void setEnabledCallsOnEnableWhenEnabled() {
        module.setEnabled(true);
        assertTrue(module.onEnableCalled);
        assertFalse(module.onDisableCalled);
    }
    
    @Test
    void setEnabledCallsOnDisableWhenDisabled() {
        module.setEnabled(true);
        module.onEnableCalled = false;
        
        module.setEnabled(false);
        assertFalse(module.onEnableCalled);
        assertTrue(module.onDisableCalled);
    }
    
    @Test
    void toggleChangesEnabledState() {
        assertFalse(module.isEnabled());
        
        module.toggle();
        assertTrue(module.isEnabled());
        
        module.toggle();
        assertFalse(module.isEnabled());
    }
    
    @Test
    void notificationContainsCorrectModuleName() {
        module.setEnabled(true);
        
        var notification = Notify.Manager.getInstance().getActiveNotifications().get(0);
        assertTrue(notification.getMessage().contains("TestModule"));
    }
    
    @Test
    void notificationShowsCorrectStateForEnable() {
        module.setEnabled(true);
        
        var notification = Notify.Manager.getInstance().getActiveNotifications().get(0);
        assertTrue(notification.getMessage().contains("включен"));
    }
    
    @Test
    void notificationShowsCorrectStateForDisable() {
        module.setEnabled(true);
        Notify.Manager.getInstance().getActiveNotifications().clear();
        
        module.setEnabled(false);
        
        var notification = Notify.Manager.getInstance().getActiveNotifications().get(0);
        assertTrue(notification.getMessage().contains("выключен"));
    }
    
    static class TestModule extends Module {
        boolean onEnableCalled = false;
        boolean onDisableCalled = false;
        
        public TestModule(String name, Category category) {
            super(name, "Test description", category, -1);
        }
        
        @Override
        public void onEnable() {
            onEnableCalled = true;
        }
        
        @Override
        public void onDisable() {
            onDisableCalled = true;
        }
        
        @Override
        public void onTick() {
        }
    }
}
