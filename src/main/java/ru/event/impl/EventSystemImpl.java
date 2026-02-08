package ru.event.impl;

import ru.event.api.Event;
import ru.event.api.EventBus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Реализация системы событий
 */
public class EventSystemImpl implements EventBus {
    private static final Map<Class<?>, List<Consumer<?>>> listeners = new HashMap<>();
    private static EventSystemImpl instance;

    private EventSystemImpl() {
    }

    /**
     * Получает единственный экземпляр системы событий
     * @return экземпляр EventSystemImpl
     */
    public static EventSystemImpl getInstance() {
        if (instance == null) {
            instance = new EventSystemImpl();
        }
        return instance;
    }

    @Override
    public <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener) {
        listeners.computeIfAbsent(eventClass, k -> new ArrayList<>()).add(listener);
    }

    @Override
    public <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener) {
        List<Consumer<?>> list = listeners.get(eventClass);
        if (list != null) {
            list.remove(listener);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Event> void fire(T event) {
        List<Consumer<?>> list = listeners.get(event.getClass());
        if (list != null) {
            for (Consumer<?> listener : list) {
                ((Consumer<T>) listener).accept(event);
            }
        }
    }

    /**
     * Очищает все подписки на события
     */
    public void clear() {
        listeners.clear();
    }
}
