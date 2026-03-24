package so.aporia.module.settings;

import java.util.function.Supplier;

/**
 * Keybind setting - binds a key to an action.
 */
public class BindSetting extends Setting<Integer> {
    
    public BindSetting(String name, String description) {
        this(name, description, -1, null);
    }
    
    public BindSetting(String name, String description, int defaultKey) {
        this(name, description, defaultKey, null);
    }
    
    public BindSetting(String name, String description, int defaultKey, Supplier<Boolean> visible) {
        super(name, description, defaultKey, visible);
    }
    
    public int getKey() { return value; }
    public void setKey(int key) { this.value = key; }
    public boolean isBound() { return value != -1; }
}
