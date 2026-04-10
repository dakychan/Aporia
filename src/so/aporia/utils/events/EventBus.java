/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central event bus. Auto-discovers {@link EventHandler}-annotated methods via reflection.
 * <p>
 * Центральная шина событий. Автоматически находит методы с аннотацией {@link EventHandler}.
 * <p>
 * Usage:
 * <pre>{@code
 * EventBus.INSTANCE.register(this);
 * EventBus.INSTANCE.post(new MouseScrollEvent(...));
 * }</pre>
 */
public final class EventBus {

    public static final EventBus INSTANCE = new EventBus();

    /**
     * eventClass → list of (instance, method) pairs.
     * <p>
     * eventClass → список пар (instance, method).
     */
    private final Map<Class<?>, List<Listener>> listeners = new HashMap<>();

    private EventBus() {}

    /**
     * Scans {@code obj} for {@link EventHandler}-annotated methods and registers them.
     * Each method must have exactly one parameter (the event type).
     * <p>
     * Сканирует {@code obj} на наличие методов с аннотацией {@link EventHandler} и регистрирует их.
     * Каждый метод должен иметь ровно один параметр (тип события).
     */
    public void register(Object obj) {
        for (Method m : obj.getClass().getMethods()) {
            if (!m.isAnnotationPresent(EventHandler.class)) continue;
            if (m.getParameterCount() != 1) continue;
            Class<?> eventType = m.getParameterTypes()[0];
            listeners.computeIfAbsent(eventType, k -> new ArrayList<>())
                     .add(new Listener(obj, m));
        }
    }

    /**
     * Unregisters all handlers belonging to {@code obj}.
     * <p>
     * Отписывает все обработчики принадлежащие {@code obj}.
     */
    public void unregister(Object obj) {
        listeners.values().forEach(list -> list.removeIf(l -> l.instance == obj));
    }

    /**
     * Posts an event — calls all matching handlers.
     * <p>
     * Отправляет событие — вызывает все подходящие обработчики.
     */
    public void post(Object event) {
        List<Listener> list = listeners.get(event.getClass());
        if (list == null) {
            return;
        }
        for (Listener l : list) {
            try { l.method.invoke(l.instance, event); }
            catch (Exception e) { e.printStackTrace(); }
        }
    }

    private record Listener(Object instance, Method method) {}
}
