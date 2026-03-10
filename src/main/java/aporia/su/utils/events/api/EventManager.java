package aporia.su.utils.events.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventManager {
    private static final Map<Class<? extends Event>, List<Consumer<? extends Event>>> listeners = new HashMap<>();
    
    @SuppressWarnings("unchecked")
    public static <T extends Event> void register(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }
    
    @SuppressWarnings("unchecked")
    public static <T extends Event> void callEvent(T event) {
        List<Consumer<? extends Event>> eventListeners = listeners.get(event.getClass());
        if (eventListeners != null) {
            for (Consumer<? extends Event> listener : eventListeners) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }
}
