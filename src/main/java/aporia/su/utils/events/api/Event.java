package aporia.su.utils.events.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Базовый класс для всех событий в системе.
 * Поддерживает механизм отмены события.
 * 
 * @author Aporia
 */
public abstract class Event {
    private boolean cancelled = false;
    
    /**
     * @return true если событие было отменено
     */
    public boolean isCancelled() {
        return cancelled;
    }
    
    /**
     * Устанавливает статус отмены события.
     * 
     * @param cancelled true для отмены события
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    /**
     * Аннотация для пометки методов как обработчиков событий.
     * Метод должен принимать один параметр - событие, которое он обрабатывает.
     * 
     * Пример использования:
     * <pre>
     * {@code
     * @EventHandler
     * public void onTabComplete(TabCompleteEvent event) {
     *     // обработка события
     * }
     * }
     * </pre>
     * 
     * @author Aporia
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface EventHandler {
        /**
         * Приоритет выполнения обработчика.
         * Меньшее значение = выше приоритет (выполнится раньше).
         * 
         * @return приоритет обработчика (по умолчанию 0)
         */
        int priority() default 0;
    }
}
