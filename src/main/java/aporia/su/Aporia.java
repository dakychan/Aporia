package aporia.su;

import aporia.su.utils.chat.CommandManager;
import aporia.su.utils.events.api.Event.EventHandler;
import aporia.su.utils.events.api.EventManager;
import aporia.su.utils.events.impl.TabCompleteEvent;

/**
 * Класс инициализации системы Aporia.
 * Регистрирует команды и обработчики событий при запуске клиента.
 * 
 * @author Aporia
 */
public class Aporia {
    /**
     * Инициализирует систему команд и регистрирует обработчики событий.
     * Вызывается автоматически при запуске Minecraft клиента.
     */
    public static void init() {
        System.out.println("[Aporia] Command system initialized with prefix: " + CommandManager.INSTANCE.getPrefix());
        EventManager.registerListener(new Aporia());
    }
    
    /**
     * Обработчик события автодополнения.
     * Предоставляет автодополнения для команд с нашим префиксом.
     * 
     * @param event событие автодополнения
     */
    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String prefix = event.getPrefix();
        if (prefix.startsWith(CommandManager.INSTANCE.getPrefix())) {
            String[] completions = CommandManager.INSTANCE.getCompletions(prefix);
            if (completions.length > 0) {
                event.setCompletions(completions);
            }
        }
    }
}
