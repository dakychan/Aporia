package aporia.su.utils.events.impl;

import aporia.su.utils.events.api.Event;

/**
 * Событие автодополнения в чате.
 * Вызывается когда пользователь вводит текст в чат и система запрашивает автодополнения.
 * Позволяет модифицировать или предоставить собственные варианты автодополнения.
 * 
 * @author Aporia
 */
public class TabCompleteEvent extends Event {
    private final String prefix;
    /** Массив автодополнений, которые будут показаны пользователю */
    public String[] completions;
    
    /**
     * Создает событие автодополнения.
     * 
     * @param prefix текущий текст в чате (до позиции курсора)
     */
    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
    }
    
    /**
     * @return текущий текст в чате
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Устанавливает массив автодополнений для отображения.
     * 
     * @param completions массив строк для автодополнения
     */
    public void setCompletions(String[] completions) {
        this.completions = completions;
    }
}
