package aporia.su.utils.chat;

import aporia.su.utils.chat.impl.AliasCommand;
import aporia.su.utils.chat.impl.InfoCommand;
import aporia.su.utils.chat.impl.PrefixCommand;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Менеджер команд - центральная система управления всеми командами.
 * Singleton класс, который регистрирует, выполняет команды и предоставляет автодополнение.
 * 
 * @author Aporia
 */
public class CommandManager {
    /** Единственный экземпляр менеджера команд */
    public static final CommandManager INSTANCE = new CommandManager();
    
    private final Map<String, Command> commands = new HashMap<>();
    private String prefix = "^";
    
    /**
     * Приватный конструктор для Singleton паттерна.
     * Регистрирует встроенные команды.
     */
    private CommandManager() {
        registerCommand(new AliasCommand());
        registerCommand(new PrefixCommand());
        registerCommand(new InfoCommand());
    }
    
    /**
     * Регистрирует команду в системе.
     * Команда будет доступна по своему имени и всем алиасам.
     * 
     * @param command команда для регистрации
     */
    public void registerCommand(Command command) {
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }
    
    /**
     * Выполняет команду из строки ввода.
     * 
     * @param input полная строка ввода (с префиксом)
     * @return true если команда была найдена и выполнена, false иначе
     */
    public boolean executeCommand(String input) {
        if (!input.startsWith(prefix)) {
            return false;
        }
        
        String[] parts = input.substring(prefix.length()).split(" ");
        if (parts.length == 0) {
            return false;
        }
        
        String commandName = parts[0].toLowerCase();
        Command command = commands.get(commandName);
        if (command == null) {
            return false;
        }
        
        List<String> args = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            args.add(parts[i]);
        }
        
        command.execute(args);
        return true;
    }
    
    /**
     * Получает список автодополнений для текущего ввода.
     * 
     * @param input текущая строка ввода (с префиксом)
     * @return массив возможных автодополнений
     */
    public String[] getCompletions(String input) {
        if (!input.startsWith(prefix)) {
            return new String[0];
        }
        
        String text = input.substring(prefix.length());
        String[] parts = text.split(" ", -1);
        
        if (parts.length == 1) {
            String search = parts[0].toLowerCase();
            return commands.keySet().stream()
                .filter(cmd -> cmd.startsWith(search))
                .distinct()
                .toArray(String[]::new);
        } else {
            String commandName = parts[0].toLowerCase();
            Command command = commands.get(commandName);
            if (command == null) {
                return new String[0];
            }
            
            List<String> args = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                args.add(parts[i]);
            }
            
            return command.getCompletions(args);
        }
    }
    
    /**
     * @return список всех зарегистрированных команд (без дубликатов алиасов)
     */
    public List<Command> getAllCommands() {
        return commands.values().stream().distinct().collect(Collectors.toList());
    }
    
    /**
     * @return текущий префикс команд
     */
    public String getPrefix() {
        return prefix;
    }
    
    /**
     * Устанавливает новый префикс для команд.
     * 
     * @param prefix новый префикс
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
