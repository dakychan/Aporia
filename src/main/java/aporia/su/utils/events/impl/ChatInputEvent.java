package aporia.su.utils.events.impl;

import aporia.su.utils.events.api.Event;

/**
 * Событие ввода сообщения в чат.
 * Вызывается когда игрок отправляет сообщение в чат.
 * Позволяет перехватить сообщение до отправки.
 *
 * @author Aporia
 */
public class ChatInputEvent extends Event {
    private String message;
    private boolean isCommand;

    /**
     * Создает событие ввода сообщения.
     *
     * @param message текст сообщения
     */
    public ChatInputEvent(String message) {
        this.message = message;
        this.isCommand = message.startsWith("/");
    }

    /**
     * @return текст сообщения
     */
    public String getMessage() {
        return message;
    }

    /**
     * Устанавливает новый текст сообщения.
     *
     * @param message новый текст
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return true если сообщение является командой (начинается с /)
     */
    public boolean isCommand() {
        return isCommand;
    }

    /**
     * @return true если сообщение является командой Aporia (начинается с префикса)
     */
    public boolean isAporiaCommand() {
        return !message.startsWith("/");
    }
}
