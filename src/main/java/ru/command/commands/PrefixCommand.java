package ru.command.commands;

import ru.command.Command;
import ru.command.CommandManager;

public class PrefixCommand implements Command {
    
    @Override
    public String getName() {
        return "prefix";
    }
    
    @Override
    public String getDescription() {
        return "Изменение префикса команд";
    }
    
    @Override
    public String getUsage() {
        return ".prefix <символ> - Установить новый префикс команд";
    }
    
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager cm = CommandManager.getInstance();
            CommandManager.sendChatMessage("§eТекущий префикс: §f" + cm.getPrefix());
            CommandManager.sendChatMessage("§eИспользование: " + getUsage());
            return;
        }
        
        if (args.length > 1) {
            CommandManager.sendChatMessage("§cПрефикс должен быть одним символом");
            CommandManager.sendChatMessage("§eИспользование: " + getUsage());
            return;
        }
        
        String newPrefix = args[0];
        
        if (newPrefix.length() > 1) {
            CommandManager.sendChatMessage("§cПрефикс должен быть одним символом");
            return;
        }
        
        CommandManager cm = CommandManager.getInstance();
        String oldPrefix = cm.getPrefix();
        cm.setPrefix(newPrefix);
        
        CommandManager.sendChatMessage("§aПрефикс изменён с §f" + oldPrefix + " §aна §f" + newPrefix);
    }
}
