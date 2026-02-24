package ru.event.api;

import java.util.function.Consumer;

public interface EventBus {
   <T extends Event> void subscribe(Class<T> var1, Consumer<T> var2);

   <T extends Event> void unsubscribe(Class<T> var1, Consumer<T> var2);

   <T extends Event> void fire(T var1);
}
