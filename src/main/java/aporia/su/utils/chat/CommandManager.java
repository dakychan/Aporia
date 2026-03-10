package aporia.su.utils.chat;

import java.util.*;
import java.util.stream.Collectors;

public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();
    
    private final Map<String, Command> commands = new HashMap<>();
    private String prefix = "^";
    
    private CommandManager() {
        registerCommand(new AliasCommand());
        registerCommand(new PrefixCommand());
        registerCommand(new InfoCommand());
    }
    
    public void registerCommand(Command command) {
        commands.put(command.getName(), command);
        for (String alias : command.getAliases()) {
            commands.put(alias, command);
        }
    }
    
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
    
    public List<Command> getAllCommands() {
        return commands.values().stream().distinct().collect(Collectors.toList());
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
}
