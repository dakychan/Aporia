package ru.event.api;

import java.util.function.Consumer;

/**
 * Интерфейс шины событий для подписки, отписки и отправки событий
 */
public interface EventBus {
    /**
     * Подписывается на события определенного типа
     * @param eventClass класс события
     * @param listener обработчик события
     * @param <T> тип события
     */
    <T extends Event> void subscribe(Class<T> eventClass, Consumer<T> listener);
    
    /**
     * Отписывается от событий определенного типа
     * @param eventClass класс события
     * @param listener обработчик события
     * @param <T> тип события
     */
    <T extends Event> void unsubscribe(Class<T> eventClass, Consumer<T> listener);
    
    /**
     * Отправляет событие всем подписчикам
     * @param event событие для отправки
     * @param <T> тип события
     */
    <T extends Event> void fire(T event);
}
