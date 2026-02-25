package cc.apr.command.commands;

import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import java.util.ArrayList;
import java.util.List;
import cc.apr.command.Command;
import aporia.cc.files.FilesManager;

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
      return ChatUtils.INSTANCE.formatCommand("friend")
         + " add <имя> - Добавить друга\n"
         + ChatUtils.INSTANCE.formatCommand("friend")
         + " remove <имя> - Удалить друга\n"
         + ChatUtils.INSTANCE.formatCommand("friend")
         + " list - Показать всех друзей\n"
         + ChatUtils.INSTANCE.formatCommand("friend")
         + " clear - Очистить список друзей";
   }

   @Override
   public void execute(String[] args) {
      FilesManager fm = cc.apr.Aporia.getFilesManager();
      if (args.length == 0) {
         List<String> friends = fm.loadFriends();
         if (friends.isEmpty()) {
            ChatUtils.INSTANCE.sendMessage("Список друзей пуст", MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; remove ; list ; clear", MessageType.WARNING);
         } else {
            this.listFriends(fm);
         }
      } else {
         if (args[0].equalsIgnoreCase("list")) {
            this.listFriends(fm);
         } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            this.addFriend(fm, args[1]);
         } else if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
            this.removeFriend(fm, args[1]);
         } else if (args[0].equalsIgnoreCase("clear")) {
            this.clearFriends(fm);
         } else {
            ChatUtils.INSTANCE.sendMessage("Использование:", MessageType.WARNING);

            for (String line : this.getUsage().split("\n")) {
               ChatUtils.INSTANCE.sendMessage(line, MessageType.WARNING);
            }
         }
      }
   }

   private void listFriends(FilesManager fm) {
      List<String> friends = fm.loadFriends();
      if (friends.isEmpty()) {
         ChatUtils.INSTANCE.sendMessage("Список друзей пуст", MessageType.WARNING);
      } else {
         ChatUtils.INSTANCE.sendMessage("=== Друзья (" + friends.size() + ") ===", MessageType.WARNING);
         friends.forEach(friend -> ChatUtils.INSTANCE.sendMessage("- " + friend, MessageType.WARNING));
      }
   }

   private void addFriend(FilesManager fm, String username) {
      List<String> friends = new ArrayList<>(fm.loadFriends());
      if (friends.contains(username)) {
         ChatUtils.INSTANCE.sendMessage(username + " уже в списке друзей", MessageType.ERROR);
      } else {
         friends.add(username);
         fm.saveFriends(friends);
         ChatUtils.INSTANCE.sendMessage("Друг добавлен: " + username, MessageType.SUCCESS);
      }
   }

   private void removeFriend(FilesManager fm, String username) {
      List<String> friends = new ArrayList<>(fm.loadFriends());
      if (!friends.contains(username)) {
         ChatUtils.INSTANCE.sendMessage(username + " не найден в списке друзей", MessageType.ERROR);
      } else {
         friends.remove(username);
         fm.saveFriends(friends);
         ChatUtils.INSTANCE.sendMessage("Друг удалён: " + username, MessageType.SUCCESS);
      }
   }

   private void clearFriends(FilesManager fm) {
      fm.saveFriends(new ArrayList<>());
      ChatUtils.INSTANCE.sendMessage("Список друзей очищен", MessageType.SUCCESS);
   }
}
