package ru.module;

import ru.event.impl.EventSystemImpl;
import ru.event.impl.ModuleToggleEvent;
import ru.ui.notify.Notify;

/**
 * Base class for all modules in the client.
 */
public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final int defaultBind;
    private boolean enabled;
    
    // Алиас для удобства
    public static class C {
        public static final Category COMBAT = Category.COMBAT;
        public static final Category MOVEMENT = Category.MOVEMENT;
        public static final Category VISUALS = Category.VISUALS;
        public static final Category PLAYER = Category.PLAYER;
        public static final Category MISC = Category.MISC;
    }
    
    public enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        VISUALS("Visuals"),
        PLAYER("Player"),
        MISC("Miscellaneous");
        
        private final String displayName;
        
        Category(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    public Module(String name, String description, Category category) {
        this(name, description, category, -1);
    }
    
    public Module(String name, String description, Category category, int defaultBind) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultBind = defaultBind;
        this.enabled = false;
    }
    
    public void toggle() {
        setEnabled(!enabled);
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return; // Предотвращаем двойной вызов
        
        this.enabled = enabled;
        
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        
        EventSystemImpl.getInstance().fire(new ModuleToggleEvent(this, enabled));
        
        // Показываем нотификацию
        ru.ui.notify.Notify.Manager.getInstance().showNotification(
            name + (enabled ? " включен" : " выключен"),
            ru.ui.notify.Notify.NotificationType.MODULE
        );
    }
    
    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onTick();
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public int getDefaultBind() {
        return defaultBind;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
