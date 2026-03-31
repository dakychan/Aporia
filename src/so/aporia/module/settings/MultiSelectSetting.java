package so.aporia.module.settings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** Multi-select setting — multiple options can be toggled on/off. */
public class MultiSelectSetting extends Setting<Set<String>> {

    private final List<String> options = new ArrayList<>();

    public MultiSelectSetting(String name, String description) {
        this(name, description, null);
    }

    public MultiSelectSetting(String name, String description, Supplier<Boolean> visible) {
        super(name, description, new HashSet<>(), visible);
    }

    public MultiSelectSetting options(String... opts) {
        for (String o : opts) options.add(o);
        return this;
    }

    public List<String> getOptions() { return options; }

    public boolean isSelected(String option) { return value.contains(option); }

    public void toggle(String option) {
        if (value.contains(option)) value.remove(option);
        else value.add(option);
    }
    
    public List<String> getSelected() { return new ArrayList<>(value); }
    
    public void setSelected(List<String> selected) {
        value.clear();
        value.addAll(selected);
    }
}
