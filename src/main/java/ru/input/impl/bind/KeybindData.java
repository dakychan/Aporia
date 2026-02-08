package ru.input.impl.bind;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class KeybindData {
    private String id;
    private int keyCode;
    private String keyName;

    public KeybindData() {
    }

    public KeybindData(String id, int keyCode, String keyName) {
        this.id = id;
        this.keyCode = keyCode;
        this.keyName = keyName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }
}
