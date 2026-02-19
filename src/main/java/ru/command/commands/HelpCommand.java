package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import ru.command.Command;
import ru.command.CommandRegistry;

/**
 * Help command that displays all available commands.
 */
public class HelpCommand implements Command {
    
    private final CommandRegistry registry;
    
    /**
     * Constructor.
     * 
     * @param registry The command registry
     */
    public HelpCommand(CommandRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "help";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Показать список команд";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return ChatUtils.INSTANCE.formatCommand("help") + " - Показать все команды";
    }
    
    /**
     * Execute the help command.
     * 
     * @param args Command arguments (unused)
     */
    @Override
    public void execute(String[] args) {
        ChatUtils.INSTANCE.sendMessage("=== Список команд ===", ChatUtils.MessageType.WARNING);
        
        for (Command cmd : registry.getAllCommands()) {
            String line = String.format("%s - %s", cmd.getName(), cmd.getDescription());
            ChatUtils.INSTANCE.sendMessage(line, ChatUtils.MessageType.WARNING);
        }
    }
}
