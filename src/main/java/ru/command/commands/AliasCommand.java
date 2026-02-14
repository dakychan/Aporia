package ru.command.commands;

import ru.command.Command;
import ru.command.CommandManager;

import java.util.Arrays;
import java.util.Map;

public class AliasCommand implements Command {
    
    @Override
    public String getName() {
        return "alias";
    }
    
    @Override
    public String getDescription() {
        return "Управление псевдонимами команд";
    }
    
    @Override
    public String getUsage() {
        return ".alias <имя> <команда> - Создать псевдоним\n" +
               ".alias list - Показать все псевдонимы\n" +
               ".alias remove <имя> - Удалить псевдоним";
    }
    
    @Override
    public void execute(String[] args) {
        CommandManager cm = CommandManager.getInstance();
        
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            listAliases(cm);
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("remove")) {
            removeAlias(cm, args[1]);
        } else if (args.length >= 2) {
            String alias = args[0];
            String command = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            addAlias(cm, alias, command);
        } else {
            CommandManager.sendChatMessage("§cИспользование:");
            for (String line : getUsage().split("\n")) {
                CommandManager.sendChatMessage("§e" + line);
            }
        }
    }
    
    private void listAliases(CommandManager cm) {
        Map<String, String> aliases = cm.getAliases();
        
        if (aliases.isEmpty()) {
            CommandManager.sendChatMessage("§eПсевдонимы не настроены");
            return;
        }
        
        CommandManager.sendChatMessage("§e=== Псевдонимы команд ===");
        aliases.forEach((alias, command) -> 
            CommandManager.sendChatMessage("§7" + alias + " §f-> §a" + command)
        );
    }
    
    private void addAlias(CommandManager cm, String alias, String command) {
        cm.addAlias(alias, command);
        CommandManager.sendChatMessage("§aПсевдоним создан: §f" + alias + " §7-> §a" + command);
    }
    
    private void removeAlias(CommandManager cm, String alias) {
        if (cm.getAliases().containsKey(alias.toLowerCase())) {
            cm.removeAlias(alias);
            CommandManager.sendChatMessage("§aПсевдоним удалён: §f" + alias);
        } else {
            CommandManager.sendChatMessage("§cПсевдоним не найден: " + alias);
        }
    }
}
