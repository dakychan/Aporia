package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import ru.command.Command;

import java.util.Arrays;
import java.util.Map;

/**
 * Command for managing command aliases.
 */
public class AliasCommand implements Command {
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "alias";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Управление псевдонимами команд";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return ChatUtils.INSTANCE.formatCommand("alias") + " add <имя> <команда> - Создать псевдоним\n" +
               ChatUtils.INSTANCE.formatCommand("alias") + " list - Показать все псевдонимы\n" +
               ChatUtils.INSTANCE.formatCommand("alias") + " remove <имя> - Удалить псевдоним\n" +
               ChatUtils.INSTANCE.formatCommand("alias") + " clear - Очистить все псевдонимы";
    }
    
    /**
     * Execute the alias command.
     * 
     * @param args Command arguments
     */
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
            if (aliases.isEmpty()) {
                ChatUtils.INSTANCE.sendMessage("Псевдонимы не настроены", ChatUtils.MessageType.WARNING);
                ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; list ; remove ; clear", ChatUtils.MessageType.WARNING);
            } else {
                listAliases();
            }
            return;
        }
        
        String subcommand = args[0].toLowerCase();
        
        switch (subcommand) {
            case "list":
                listAliases();
                break;
            case "add":
                if (args.length >= 3) {
                    String alias = args[1];
                    String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    addAlias(alias, command);
                } else {
                    ChatUtils.INSTANCE.sendMessage("Использование: " + ChatUtils.INSTANCE.formatCommand("alias") + " add <имя> <команда>", ChatUtils.MessageType.ERROR);
                }
                break;
            case "remove":
                if (args.length >= 2) {
                    removeAlias(args[1]);
                } else {
                    ChatUtils.INSTANCE.sendMessage("Использование: " + ChatUtils.INSTANCE.formatCommand("alias") + " remove <имя>", ChatUtils.MessageType.ERROR);
                }
                break;
            case "clear":
                clearAliases();
                break;
            default:
                ChatUtils.INSTANCE.sendMessage("Неизвестная подкоманда: " + subcommand, ChatUtils.MessageType.ERROR);
                ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; list ; remove ; clear", ChatUtils.MessageType.WARNING);
                break;
        }
    }
    
    /**
     * List all aliases.
     */
    private void listAliases() {
        Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
        
        if (aliases.isEmpty()) {
            ChatUtils.INSTANCE.sendMessage("Псевдонимы не настроены", ChatUtils.MessageType.WARNING);
            return;
        }
        
        ChatUtils.INSTANCE.sendMessage("=== Псевдонимы команд ===", ChatUtils.MessageType.WARNING);
        aliases.forEach((alias, command) -> 
            ChatUtils.INSTANCE.sendMessage(alias + " -> " + command, ChatUtils.MessageType.WARNING)
        );
    }
    
    /**
     * Add a new alias.
     * 
     * @param alias The alias name
     * @param command The command to alias
     */
    private void addAlias(String alias, String command) {
        ChatUtils.INSTANCE.addAlias(alias, command);
        ChatUtils.INSTANCE.sendMessage("Псевдоним создан: " + alias + " -> " + command, ChatUtils.MessageType.SUCCESS);
    }
    
    /**
     * Remove an alias.
     * 
     * @param alias The alias to remove
     */
    private void removeAlias(String alias) {
        if (ChatUtils.INSTANCE.getAliases().containsKey(alias.toLowerCase())) {
            ChatUtils.INSTANCE.removeAlias(alias);
            ChatUtils.INSTANCE.sendMessage("Псевдоним удалён: " + alias, ChatUtils.MessageType.SUCCESS);
        } else {
            ChatUtils.INSTANCE.sendMessage("Псевдоним не найден: " + alias, ChatUtils.MessageType.ERROR);
        }
    }
    
    /**
     * Clear all aliases.
     */
    private void clearAliases() {
        Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
        if (aliases.isEmpty()) {
            ChatUtils.INSTANCE.sendMessage("Псевдонимы уже пусты", ChatUtils.MessageType.WARNING);
            return;
        }
        
        int count = aliases.size();
        aliases.keySet().forEach(alias -> ChatUtils.INSTANCE.removeAlias(alias));
        ChatUtils.INSTANCE.sendMessage("Удалено псевдонимов: " + count, ChatUtils.MessageType.SUCCESS);
    }
}
