package so.aporia.module.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Dropdown/select setting with multiple options.
 */
public class SelectSetting extends Setting<String> {
    
    private final List<String> options = new ArrayList<>();
    
    public SelectSetting(String name, String description) {
        this(name, description, null);
    }
    
    public SelectSetting(String name, String description, Supplier<Boolean> visible) {
        super(name, description, "", visible);
    }
    
    public SelectSetting value(String... values) {
        for (String v : values) options.add(v);
        if (value.isEmpty() && !options.isEmpty()) {
            value = options.get(0);
        }
        return this;
    }
    
    public SelectSetting selected(String selected) {
        if (options.contains(selected)) {
            value = selected;
        }
        return this;
    }
    
    public boolean isSelected(String option) {
        return value.equals(option);
    }
    
    public List<String> getOptions() { return options; }
    
    public int getSelectedIndex() { return options.indexOf(value); }
    
    public void setSelectedIndex(int index) {
        if (index >= 0 && index < options.size()) {
            value = options.get(index);
        }
    }
}
