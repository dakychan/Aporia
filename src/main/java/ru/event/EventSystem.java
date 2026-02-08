package ru.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Система событий
 */
public class EventSystem {
    private static final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();

    public static <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    public static <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventClass);
        if (list != null) {
            list.remove(listener);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> void fire(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> listener : list) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    public static void clear() {
        listeners.clear();
    }
}
