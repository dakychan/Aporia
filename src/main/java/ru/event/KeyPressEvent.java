package ru.event;

import ru.input.Keyboard;

/**
 * Событие нажатия клавиши
 */
public class KeyPressEvent extends Event {
    public final Keyboard key;
    public final int keyCode;

    public KeyPressEvent(Keyboard key, int keyCode) {
        this.key = key;
        this.keyCode = keyCode;
    }
}
