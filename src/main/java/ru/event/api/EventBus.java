package ru.event.api;

import java.util.function.Consumer;

public interface EventBus {

    <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener);

    <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener);

    <T extends Event> void fire(T event);
}
