package zenith.zov.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.client.modules.api.setting.Setting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Supplier;

@Getter
public class ModeSetting extends Setting {

    private final List<Value> values = new ArrayList<>();
    @Setter
    private Value value;
    private boolean expanded;

    public ModeSetting(String name, String... modes) {
        super(name);
        for (String mode : modes) {
            if(mode.isEmpty()) continue;
            new Value(this, mode);
        }
        if (!values.isEmpty()) {
            value = values.getFirst();
        }

    }

    public ModeSetting(String name, Supplier<Boolean> visible, String... modes) {
        super(name);
        for (String mode : modes) {
            if(mode.isEmpty()) continue;
            new Value(this, mode);
        }
        if (!values.isEmpty()) {
            value = values.getFirst();
        }
        setVisible(visible);
    }

    public void set(String mode) {
        values.stream()
                .filter(v -> v.getName().equals(mode))
                .findFirst()
                .ifPresent(v -> this.value = v);
    }

    public String get() {
        return value != null ? value.getName() : "";
    }

    public boolean is(String mode) {
        return value != null && value.getName().equals(mode);
    }

    public boolean is(Value otherValue) {
        return this.value == otherValue;
    }

    public Value getRandomEnabledElement() {
        List<Value> selectedValues = values.stream()
                .filter(Value::isSelected)
                .toList();

        if (!selectedValues.isEmpty()) {
            return selectedValues.get(new Random().nextInt(selectedValues.size()));
        }
        return null;
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(String.valueOf(name), get());

    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.set(propertiesObject.get(String.valueOf(name)).getAsString());
    }

    @Getter
    public static class Value {
        private final ModeSetting parent;
        private final String name;
        private final String description;

        public Value(ModeSetting parent, String name) {
            this.parent = parent;
            this.name = name;
            this.description = "";
            if(parent.values.isEmpty()){
                this.select();
            }
            parent.values.add(this);
        }

        public Value(ModeSetting parent, String name, String description) {
            this.parent = parent;
            this.name = name;
            this.description = description;
            if(parent.values.isEmpty()){
                this.select();
            }
            parent.values.add(this);
        }

        public Value select() {
            parent.setValue(this);
            return this;
        }

        public boolean isSelected() {
            return parent.getValue() == this;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) return true;
            if (obj == null || obj.getClass() != this.getClass()) return false;
            var that = (Value) obj;
            return Objects.equals(this.parent, that.parent) &&
                    Objects.equals(this.name, that.name) &&
                    Objects.equals(this.description, that.description);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, name, description);
        }
    }
}
