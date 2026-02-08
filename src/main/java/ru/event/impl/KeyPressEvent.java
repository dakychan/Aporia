package ru.event.impl;

import ru.event.api.Event;
import ru.input.api.KeyboardKeys;

/**
 * Событие нажатия клавиши
 */
public class KeyPressEvent extends Event {
    public final KeyboardKeys key;
    public final int keyCode;
    public final String keyName;

    public KeyPressEvent(KeyboardKeys key, int keyCode) {
        this.key = key;
        this.keyCode = keyCode;
        this.keyName = key.getName();
    }
    
    public KeyPressEvent(int keyCode, String keyName) {
        this.key = KeyboardKeys.findByKeyCode(keyCode);
        this.keyCode = keyCode;
        this.keyName = keyName;
    }
}
