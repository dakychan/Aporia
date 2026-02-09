package zenith.zov.client.modules.api.setting.impl;


import com.google.gson.JsonObject;
import lombok.Getter;
import zenith.zov.client.modules.api.setting.Setting;

import java.util.function.Supplier;
@Getter
public class NumberSetting extends Setting {
    private final String description;
    private float current;
    private final float min, max, increment;

    private Test edit;
    public NumberSetting(String name, float value, float min, float max, float increment,Test edit) {
        super(name);
        this.min = min;
        this.max = max;
        this.current = value;
        this.increment = increment;
        this.edit = edit;
        this.description ="";
    }
    public NumberSetting(String name, float value, float min, float max, float increment,String description) {
        super(name);
        this.min = min;
        this.max = max;
        this.current = value;
        this.increment = increment;

        this.description =description;
    }
    public NumberSetting(String name, float value, float min, float max, float increment) {
        super(name);
        this.min = min;
        this.max = max;
        this.current = value;
        this.increment = increment;

        this.description ="";
    }
    public NumberSetting(String name, float value, float min, float max, float increment, Supplier<Boolean> visible) {
        super(name);
        this.min = min;
        this.max = max;
        this.current = value;
        this.increment = increment;
        setVisible(visible);
        this.description ="";
    }

    public void setCurrent(float current) {
        float old = this.current;
        this.current = current;
        if(edit!=null) {
            edit.apply(old,current);
        }

    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(String.valueOf(name), ((NumberSetting) this).getCurrent());

    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.setCurrent((propertiesObject.get(String.valueOf(name)).getAsFloat()));
    }


    public interface Test{
        void apply(float oldValue,float newValue);
    }

}

