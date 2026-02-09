package zenith.zov.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.client.modules.api.setting.Setting;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Getter
public class MultiBooleanSetting extends Setting {
    private final List<Value> booleanSettings;

    public MultiBooleanSetting(String name) {
        super(name);
        this.booleanSettings = new ArrayList<>();

    }

    public MultiBooleanSetting(String name, Value... settings) {
        super(name);
        this.booleanSettings = new ArrayList<>(Arrays.asList(settings));

    }

    public Value getValueByName(String settingName) {
        return booleanSettings.stream()
                .filter(s -> s.getName().equalsIgnoreCase(settingName))
                .findFirst()
                .orElse(null);
    }

    public static MultiBooleanSetting create(String name, List<String> values) {
        Value[] booleanSettings = values.stream()
                .map(value -> new Value(value, true))
                .toArray(Value[]::new);
        return new MultiBooleanSetting(name, booleanSettings);
    }

    public Value get(int index) {
        return booleanSettings.get(index);
    }

    public boolean isEnable(String name) {
        Value setting = getValueByName(name);
        return setting != null && setting.isEnabled();
    }

    public boolean isEnable(int index) {
        if(index>=getBooleanSettings().size()){
            return false;
        }
        Value setting = get(index);
        return setting != null && setting.isEnabled();
    }

    public List<Value> getSelectedValues() {
        return booleanSettings.stream()
                .filter(Value::isEnabled)
                .collect(Collectors.toList());
    }

    public List<String> getSelectedNames() {
        return booleanSettings.stream()
                .filter(Value::isEnabled)
                .map(Value::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        StringBuilder builder = new StringBuilder();
        int j = 0;
        for (Value s : ((MultiBooleanSetting) this).getBooleanSettings()) {
            if (((MultiBooleanSetting) this).getValueByName(s.getName()).isEnabled())
                builder.append(s.getName()).append("\n");
            j++;
        }
        propertiesObject.addProperty(this.getName(), builder.toString());
    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.getBooleanSettings().forEach(booleanSetting -> booleanSetting.setEnabled(false));

        String[] strs = propertiesObject.get(String.valueOf(name)).getAsString().split("\n");
        for (String str : strs) {
            Value booleanSetting = this.getValueByName(str);
            if (booleanSetting != null) {
                this.getValueByName(str).setEnabled(true);
            }
        }
    }

    @Getter
    @Setter
    public static class Value {
        private boolean enabled;
        private final String name;

        public Value(String name, boolean state) {
            this.enabled = state;
            this.name = name;

        }

        public Value(MultiBooleanSetting parent, String name, boolean state) {
            this.enabled = state;
            this.name = name;
            parent.booleanSettings.add(this);

        }

        public static Value of(String name, boolean state) {
            return new Value(name, state);
        }

        public static Value of(String name) {
            return new Value(name, true);
        }

        public void toggle() {
            enabled = !enabled;
        }


    }
}
