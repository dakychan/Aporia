package ru.input;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Система событий для клавиатуры
 */
public class KeyboardEventSystem {
    private static final Map<Integer, Consumer<KeyboardEvent>> keyListeners = new HashMap<>();

    public static void registerKeyListener(int keyCode, Consumer<KeyboardEvent> listener) {
        keyListeners.put(keyCode, listener);
    }

    public static void unregisterKeyListener(int keyCode) {
        keyListeners.remove(keyCode);
    }

    public static void fireKeyEvent(int keyCode, KeyboardEvent.Type type) {
        Consumer<KeyboardEvent> listener = keyListeners.get(keyCode);
        if (listener != null) {
            listener.accept(new KeyboardEvent(keyCode, type));
        }
    }

    public static class KeyboardEvent {
        public enum Type {
            PRESSED, RELEASED
        }

        public final int keyCode;
        public final Type type;

        public KeyboardEvent(int keyCode, Type type) {
            this.keyCode = keyCode;
            this.type = type;
        }
    }
}
