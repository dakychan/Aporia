package ru.event.api;

/**
 * Базовый класс для событий с поддержкой отмены
 */
public abstract class Event {
    private boolean cancelled = false;
    
    /**
     * Проверяет, отменено ли событие
     * @return true если событие отменено
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Устанавливает статус отмены события
     * @param cancelled true для отмены события
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
