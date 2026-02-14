package ru.command.commands;

import ru.Aporia;
import ru.command.Command;
import ru.command.CommandManager;
import ru.files.FilesManager;

import java.util.ArrayList;
import java.util.List;

public class FriendCommand implements Command {
    
    @Override
    public String getName() {
        return "friend";
    }
    
    @Override
    public String getDescription() {
        return "Управление списком друзей";
    }
    
    @Override
    public String getUsage() {
        return ".friend add <имя> - Добавить друга\n" +
               ".friend remove <имя> - Удалить друга\n" +
               ".friend list - Показать всех друзей\n" +
               ".friend clear - Очистить список друзей";
    }
    
    @Override
    public void execute(String[] args) {
        FilesManager fm = Aporia.getFilesManager();
        
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("list"))) {
            listFriends(fm);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            addFriend(fm, args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            removeFriend(fm, args[1]);
        } else if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            clearFriends(fm);
        } else {
            CommandManager.sendChatMessage("§cИспользование:");
            for (String line : getUsage().split("\n")) {
                CommandManager.sendChatMessage("§e" + line);
            }
        }
    }
    
    private void listFriends(FilesManager fm) {
        List<String> friends = fm.loadFriends();
        
        if (friends.isEmpty()) {
            CommandManager.sendChatMessage("§eСписок друзей пуст");
            return;
        }
        
        CommandManager.sendChatMessage("§e=== Друзья (" + friends.size() + ") ===");
        friends.forEach(friend -> 
            CommandManager.sendChatMessage("§7- §f" + friend)
        );
    }
    
    private void addFriend(FilesManager fm, String username) {
        List<String> friends = new ArrayList<>(fm.loadFriends());
        
        if (friends.contains(username)) {
            CommandManager.sendChatMessage("§c" + username + " уже в списке друзей");
            return;
        }
        
        friends.add(username);
        fm.saveFriends(friends);
        CommandManager.sendChatMessage("§aДруг добавлен: §f" + username);
    }
    
    private void removeFriend(FilesManager fm, String username) {
        List<String> friends = new ArrayList<>(fm.loadFriends());
        
        if (!friends.contains(username)) {
            CommandManager.sendChatMessage("§c" + username + " не найден в списке друзей");
            return;
        }
        
        friends.remove(username);
        fm.saveFriends(friends);
        CommandManager.sendChatMessage("§aДруг удалён: §f" + username);
    }
    
    private void clearFriends(FilesManager fm) {
        fm.saveFriends(new ArrayList<>());
        CommandManager.sendChatMessage("§aСписок друзей очищен");
    }
}
