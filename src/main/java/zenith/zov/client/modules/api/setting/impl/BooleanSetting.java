package zenith.zov.client.modules.api.setting.impl;


import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.client.modules.api.setting.Setting;

import java.util.function.Supplier;


public class BooleanSetting extends Setting {

    @Getter
    @Setter
    private boolean enabled;
    @Getter
    private final String description;

    public BooleanSetting(String name, boolean state) {
        super(name);
        this.enabled = state;

        description = "";
    }
    public BooleanSetting(String name, String description,boolean state) {
        super(name);
        this.enabled = state;

        this.description = description;
    }
    public BooleanSetting(String name, String description,boolean state,Supplier<Boolean> supplier) {
        super(name);
        this.enabled = state;
        setVisible(supplier);
        this.description = description;
    }
    public BooleanSetting(String name, boolean state, Supplier<Boolean> visible) {
        super(name);
        this.enabled = state;
        setVisible(visible);
        description = "";
    }

    public static BooleanSetting of(String name, boolean state) {
        return new BooleanSetting(name,state);
    }
    public static BooleanSetting of(String name) {
        return new BooleanSetting(name,true);
    }

    public void toggle(){
        enabled = !enabled;
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(String.valueOf(name),isEnabled());

    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.setEnabled((propertiesObject.get(String.valueOf(name)).getAsBoolean()));

    }
}

