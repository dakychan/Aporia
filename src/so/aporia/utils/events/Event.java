/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Generic event container. Listeners are called in registration order.
 * @param <T> event data type
 */
public final class Event<T> {

    private final List<Consumer<T>> listeners = new ArrayList<>();

    /** Register a listener directly. */
    public void register(Consumer<T> listener) {
        listeners.add(listener);
    }

    /** Fire the event — calls all registered listeners. */
    public void fire(T event) {
        for (Consumer<T> l : listeners) l.accept(event);
    }
}
