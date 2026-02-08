package ru.input.api;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public enum Keyboard {
    KEY_GRAVE("GRAVE", 96),
    KEY_ESCAPE("ESCAPE", 256),
    KEY_ENTER("ENTER", 257),
    KEY_TAB("TAB", 258),
    KEY_BACKSPACE("BACKSPACE", 259),
    KEY_DELETE("DELETE", 261),
    KEY_UP("UP", 265),
    KEY_DOWN("DOWN", 264),
    KEY_LEFT("LEFT", 263),
    KEY_RIGHT("RIGHT", 262),
    KEY_A("A", 65),
    KEY_B("B", 66),
    KEY_C("C", 67),
    KEY_D("D", 68),
    KEY_E("E", 69),
    KEY_F("F", 70),
    KEY_G("G", 71),
    KEY_H("H", 72),
    KEY_I("I", 73),
    KEY_J("J", 74),
    KEY_K("K", 75),
    KEY_L("L", 76),
    KEY_M("M", 77),
    KEY_N("N", 78),
    KEY_O("O", 79),
    KEY_P("P", 80),
    KEY_Q("Q", 81),
    KEY_R("R", 82),
    KEY_S("S", 83),
    KEY_T("T", 84),
    KEY_U("U", 85),
    KEY_V("V", 86),
    KEY_W("W", 87),
    KEY_X("X", 88),
    KEY_Y("Y", 89),
    KEY_Z("Z", 90),
    KEY_NONE("NONE", -1);

    private final String name;
    private final int key;
    private static final Map<Integer, Keyboard> KEY_CODE_MAP = new HashMap<>();
    private static final Map<String, Keyboard> KEY_NAME_MAP = new HashMap<>();

    Keyboard(String name, int key) {
        this.name = name;
        this.key = key;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public static String keyName(int keyCode) {
        Keyboard key = KEY_CODE_MAP.getOrDefault(keyCode, KEY_NONE);
        return key.name;
    }

    public static int keyCode(String keyName) {
        Keyboard key = KEY_NAME_MAP.getOrDefault(keyName.toLowerCase(), KEY_NONE);
        return key.key;
    }

    public static Keyboard findByKeyCode(int keyCode) {
        return KEY_CODE_MAP.getOrDefault(keyCode, KEY_NONE);
    }

    public boolean isKey(int keyCode) {
        return keyCode == this.key;
    }

    public String getName() {
        return this.name;
    }

    public int getKey() {
        return this.key;
    }

    static {
        for (Keyboard key : values()) {
            KEY_CODE_MAP.put(key.key, key);
            KEY_NAME_MAP.put(key.name.toLowerCase(), key);
        }
    }
}
