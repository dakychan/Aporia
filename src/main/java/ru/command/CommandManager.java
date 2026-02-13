package ru.command;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

public class CommandManager {
    private static CommandManager instance;
    
    private final CommandRegistry registry;
    private String prefix = ".";
    private final Map<String, String> aliases = new HashMap<>();
    
    private CommandManager() {
        this.registry = new CommandRegistry();
    }
    
    public static CommandManager getInstance() {
        if (instance == null) {
            instance = new CommandManager();
        }
        return instance;
    }
    
    public void initialize() {
        registerCommand(new ru.command.commands.ConfigCommand());
        registerCommand(new ru.command.commands.AliasCommand());
        registerCommand(new ru.command.commands.FriendCommand());
        registerCommand(new ru.command.commands.InfoCommand());
        registerCommand(new ru.command.commands.PrefixCommand());
        
        ru.files.Logger.INSTANCE.info("CommandManager initialized with " + registry.getAllCommands().size() + " commands");
    }
    
    public boolean handleChatMessage(String message) {
        if (!message.startsWith(prefix)) {
            return false;
        }
        
        String commandText = message.substring(prefix.length()).trim();
        
        if (commandText.isEmpty()) {
            return true;
        }
        
        commandText = expandAlias(commandText);
        
        String[] parts = commandText.split("\\s+");
        String commandName = parts[0].toLowerCase();
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        Command command = registry.getCommand(commandName);
        
        if (command == null) {
            sendChatMessage("§cНеизвестная команда: " + commandName);
            return true;
        }
        
        try {
            command.execute(args);
        } catch (Exception e) {
            sendChatMessage("§cОшибка выполнения команды: " + e.getMessage());
            e.printStackTrace();
        }
        
        return true;
    }
    
    public void registerCommand(Command command) {
        registry.register(command);
    }
    
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }
    
    public String getPrefix() {
        return prefix;
    }
    
    public void addAlias(String alias, String command) {
        aliases.put(alias.toLowerCase(), command);
    }
    
    public void removeAlias(String alias) {
        aliases.remove(alias.toLowerCase());
    }
    
    public Map<String, String> getAliases() {
        return new HashMap<>(aliases);
    }
    
    private String expandAlias(String input) {
        String[] parts = input.split("\\s+", 2);
        String firstWord = parts[0].toLowerCase();
        
        if (aliases.containsKey(firstWord)) {
            String expansion = aliases.get(firstWord);
            if (parts.length > 1) {
                return expansion + " " + parts[1];
            }
            return expansion;
        }
        
        return input;
    }
    
    public static void sendChatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), false);
        }
    }
}
