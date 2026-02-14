package ru.module;

import ru.event.impl.EventSystemImpl;
import ru.event.impl.ModuleToggleEvent;
import ru.ui.notify.Notify;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    private final String name;
    private final String description;
    private final Category category;
    private final int defaultBind;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

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
    
    public static abstract class Setting<T> {
        private final String name;
        private T value;
        
        public Setting(String name, T defaultValue) {
            this.name = name;
            this.value = defaultValue;
        }
        
        public String getName() {
            return name;
        }
        
        public T getValue() {
            return value;
        }
        
        public void setValue(T value) {
            this.value = value;
        }
    }
    
    public static class BooleanSetting extends Setting<Boolean> {
        public BooleanSetting(String name, boolean defaultValue) {
            super(name, defaultValue);
        }
    }
    
    public static class NumberSetting extends Setting<Double> {
        private final double min;
        private final double max;
        private final double step;
        
        public NumberSetting(String name, double defaultValue, double min, double max, double step) {
            super(name, defaultValue);
            this.min = min;
            this.max = max;
            this.step = step;
        }
        
        @Override
        public void setValue(Double value) {
            super.setValue(Math.max(min, Math.min(max, value)));
        }
        
        public double getMin() {
            return min;
        }
        
        public double getMax() {
            return max;
        }
        
        public double getStep() {
            return step;
        }
    }
    
    public static class StringSetting extends Setting<String> {
        public StringSetting(String name, String defaultValue) {
            super(name, defaultValue);
        }
    }
    
    public static class ModeSetting extends Setting<String> {
        private final String[] modes;
        
        public ModeSetting(String name, String defaultValue, String... modes) {
            super(name, defaultValue);
            this.modes = modes;
        }
        
        public String[] getModes() {
            return modes;
        }
        
        public void cycle() {
            String current = getValue();
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equals(current)) {
                    setValue(modes[(i + 1) % modes.length]);
                    return;
                }
            }
            if (modes.length > 0) {
                setValue(modes[0]);
            }
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
    
    protected void addSetting(Setting<?> setting) {
        settings.add(setting);
    }
    
    public List<Setting<?>> getSettings() {
        return settings;
    }
    
    public void toggle() {
        setEnabled(!enabled);
    }
    
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        
        this.enabled = enabled;
        
        if (enabled) {
            onEnable();
        } else {
            onDisable();
        }
        
        EventSystemImpl.getInstance().fire(new ModuleToggleEvent(this, enabled));

        Notify.Manager.getInstance().showNotification(
            name + (enabled ? " включен" : " выключен"),
            Notify.NotificationType.MODULE
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
