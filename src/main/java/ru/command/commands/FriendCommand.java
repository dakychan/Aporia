package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import ru.Aporia;
import ru.command.Command;
import ru.files.FilesManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Command for managing friends list.
 */
public class FriendCommand implements Command {
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "friend";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Управление списком друзей";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return ChatUtils.INSTANCE.formatCommand("friend") + " add <имя> - Добавить друга\n" +
               ChatUtils.INSTANCE.formatCommand("friend") + " remove <имя> - Удалить друга\n" +
               ChatUtils.INSTANCE.formatCommand("friend") + " list - Показать всех друзей\n" +
               ChatUtils.INSTANCE.formatCommand("friend") + " clear - Очистить список друзей";
    }
    
    /**
     * Execute the friend command.
     * 
     * @param args Command arguments
     */
    @Override
    public void execute(String[] args) {
        FilesManager fm = Aporia.getFilesManager();
        
        if (args.length == 0) {
            List<String> friends = fm.loadFriends();
            if (friends.isEmpty()) {
                ChatUtils.INSTANCE.sendMessage("Список друзей пуст", ChatUtils.MessageType.WARNING);
                ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; remove ; list ; clear", ChatUtils.MessageType.WARNING);
            } else {
                listFriends(fm);
            }
            return;
        }
        
        if (args[0].equalsIgnoreCase("list")) {
            listFriends(fm);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            addFriend(fm, args[1]);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            removeFriend(fm, args[1]);
        } else if (args[0].equalsIgnoreCase("clear")) {
            clearFriends(fm);
        } else {
            ChatUtils.INSTANCE.sendMessage("Использование:", ChatUtils.MessageType.WARNING);
            for (String line : getUsage().split("\n")) {
                ChatUtils.INSTANCE.sendMessage(line, ChatUtils.MessageType.WARNING);
            }
        }
    }
    
    /**
     * List all friends.
     * 
     * @param fm Files manager
     */
    private void listFriends(FilesManager fm) {
        List<String> friends = fm.loadFriends();
        
        if (friends.isEmpty()) {
            ChatUtils.INSTANCE.sendMessage("Список друзей пуст", ChatUtils.MessageType.WARNING);
            return;
        }
        
        ChatUtils.INSTANCE.sendMessage("=== Друзья (" + friends.size() + ") ===", ChatUtils.MessageType.WARNING);
        friends.forEach(friend -> 
            ChatUtils.INSTANCE.sendMessage("- " + friend, ChatUtils.MessageType.WARNING)
        );
    }
    
    /**
     * Add a friend.
     * 
     * @param fm Files manager
     * @param username Friend username
     */
    private void addFriend(FilesManager fm, String username) {
        List<String> friends = new ArrayList<>(fm.loadFriends());
        
        if (friends.contains(username)) {
            ChatUtils.INSTANCE.sendMessage(username + " уже в списке друзей", ChatUtils.MessageType.ERROR);
            return;
        }
        
        friends.add(username);
        fm.saveFriends(friends);
        ChatUtils.INSTANCE.sendMessage("Друг добавлен: " + username, ChatUtils.MessageType.SUCCESS);
    }
    
    /**
     * Remove a friend.
     * 
     * @param fm Files manager
     * @param username Friend username
     */
    private void removeFriend(FilesManager fm, String username) {
        List<String> friends = new ArrayList<>(fm.loadFriends());
        
        if (!friends.contains(username)) {
            ChatUtils.INSTANCE.sendMessage(username + " не найден в списке друзей", ChatUtils.MessageType.ERROR);
            return;
        }
        
        friends.remove(username);
        fm.saveFriends(friends);
        ChatUtils.INSTANCE.sendMessage("Друг удалён: " + username, ChatUtils.MessageType.SUCCESS);
    }
    
    /**
     * Clear all friends.
     * 
     * @param fm Files manager
     */
    private void clearFriends(FilesManager fm) {
        fm.saveFriends(new ArrayList<>());
        ChatUtils.INSTANCE.sendMessage("Список друзей очищен", ChatUtils.MessageType.SUCCESS);
    }
}
