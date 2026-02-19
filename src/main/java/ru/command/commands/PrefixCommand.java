package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import ru.command.Command;

/**
 * Command for changing the command prefix.
 */
public class PrefixCommand implements Command {
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "prefix";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Изменение префикса команд";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return ChatUtils.INSTANCE.formatCommand("prefix") + " <символ> - Установить новый префикс команд";
    }
    
    /**
     * Execute the prefix command.
     * 
     * @param args Command arguments
     */
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtils.INSTANCE.sendMessage("Текущий префикс: " + ChatUtils.INSTANCE.getPrefix(), ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Использование: " + getUsage(), ChatUtils.MessageType.WARNING);
            return;
        }
        
        if (args.length > 1) {
            ChatUtils.INSTANCE.sendMessage("Префикс должен быть одним символом", ChatUtils.MessageType.ERROR);
            ChatUtils.INSTANCE.sendMessage("Использование: " + getUsage(), ChatUtils.MessageType.WARNING);
            return;
        }
        
        String newPrefix = args[0];
        
        if (newPrefix.length() > 1) {
            ChatUtils.INSTANCE.sendMessage("Префикс должен быть одним символом", ChatUtils.MessageType.ERROR);
            return;
        }
        
        String oldPrefix = ChatUtils.INSTANCE.getPrefix();
        ChatUtils.INSTANCE.setPrefix(newPrefix);
        
        ChatUtils.INSTANCE.sendMessage("Префикс изменён с " + oldPrefix + " на " + newPrefix, ChatUtils.MessageType.SUCCESS);
    }
}
