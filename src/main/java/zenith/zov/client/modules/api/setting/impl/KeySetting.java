package zenith.zov.client.modules.api.setting.impl;


import com.google.gson.JsonObject;
import lombok.Getter;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.utility.render.display.Keyboard;

import java.util.function.Supplier;

@Getter
public class KeySetting extends Setting {


    private String nameKey;

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
        nameKey = Keyboard.getKeyName(keyCode);
    }

    private int keyCode;

    public KeySetting(String name, Supplier<Boolean> visible) {
        super(name);
        setVisible(visible);
        this.keyCode = -1;
        nameKey = Keyboard.getKeyName(keyCode);
    }
    public KeySetting(String name, int keyCode, Supplier<Boolean> visible) {
        super(name);
        setVisible(visible);
        this.keyCode = keyCode;
        nameKey = Keyboard.getKeyName(keyCode);
    }
    public KeySetting(String name, int keyCode) {
        super(name);

        this.keyCode = keyCode;
        nameKey = Keyboard.getKeyName(keyCode);
    }
    public KeySetting(String name) {
        super(name);
        this.keyCode = -1;
        this.nameKey = "";
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(String.valueOf(name), this.getKeyCode());

    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.setKeyCode(propertiesObject.get(String.valueOf(name)).getAsInt());

    }


}
