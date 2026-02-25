package cc.apr.command.commands;

import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import cc.apr.command.Command;

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
      return ChatUtils.INSTANCE.formatCommand("prefix") + " <символ> - Установить новый префикс команд";
   }

   @Override
   public void execute(String[] args) {
      if (args.length == 0) {
         ChatUtils.INSTANCE.sendMessage("Текущий префикс: " + ChatUtils.INSTANCE.getPrefix(), MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("Использование: " + this.getUsage(), MessageType.WARNING);
      } else if (args.length > 1) {
         ChatUtils.INSTANCE.sendMessage("Префикс должен быть одним символом", MessageType.ERROR);
         ChatUtils.INSTANCE.sendMessage("Использование: " + this.getUsage(), MessageType.WARNING);
      } else {
         String newPrefix = args[0];
         if (newPrefix.length() > 1) {
            ChatUtils.INSTANCE.sendMessage("Префикс должен быть одним символом", MessageType.ERROR);
         } else {
            String oldPrefix = ChatUtils.INSTANCE.getPrefix();
            ChatUtils.INSTANCE.setPrefix(newPrefix);
            ChatUtils.INSTANCE.sendMessage("Префикс изменён с " + oldPrefix + " на " + newPrefix, MessageType.SUCCESS);
         }
      }
   }
}
