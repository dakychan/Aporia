package ru.event.impl;

import ru.event.api.Event;
import ru.input.api.KeyboardKeys;

/**
 * Событие отпускания клавиши
 */
public class KeyReleaseEvent extends Event {
    public final KeyboardKeys key;
    public final int keyCode;
    public final String keyName;

    public KeyReleaseEvent(KeyboardKeys key, int keyCode) {
        this.key = key;
        this.keyCode = keyCode;
        this.keyName = key.getName();
    }
    
    public KeyReleaseEvent(int keyCode, String keyName) {
        this.key = KeyboardKeys.findByKeyCode(keyCode);
        this.keyCode = keyCode;
        this.keyName = keyName;
    }
}
