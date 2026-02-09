package zenith.zov.client.modules.api.setting.impl;

import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.client.modules.api.setting.Setting;

@Getter
@Setter
public class ButtonSetting extends Setting {
    private Runnable runnable;

    public ButtonSetting(String name, Runnable runnable) {
        super(name);
        this.runnable = runnable;
    }

    public void toggle() {
        runnable.run();
    }

    @Override
    public void safe(JsonObject propertiesObject) {

    }

    @Override
    public void load(JsonObject propertiesObject) {

    }
}
