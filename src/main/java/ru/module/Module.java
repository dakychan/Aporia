package ru.module;

/**
 * Base class for all modules in the client.
 */
public abstract class Module {
    private final String name;
    private final Category category;
    private boolean enabled;
    
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
    
    public Module(String name, Category category) {
        this.name = name;
        this.category = category;
        this.enabled = false;
    }
    
    public void toggle() {
        setEnabled(!enabled);
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
    }
    
    public abstract void onEnable();
    public abstract void onDisable();
    public abstract void onTick();
    
    public String getName() {
        return name;
    }
    
    public Category getCategory() {
        return category;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
}
