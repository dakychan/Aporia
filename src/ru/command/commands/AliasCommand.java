package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import java.util.Arrays;
import java.util.Map;
import ru.command.Command;

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
      return ChatUtils.INSTANCE.formatCommand("alias")
         + " add <имя> <команда> - Создать псевдоним\n"
         + ChatUtils.INSTANCE.formatCommand("alias")
         + " list - Показать все псевдонимы\n"
         + ChatUtils.INSTANCE.formatCommand("alias")
         + " remove <имя> - Удалить псевдоним\n"
         + ChatUtils.INSTANCE.formatCommand("alias")
         + " clear - Очистить все псевдонимы";
   }

   @Override
   public void execute(String[] args) {
      if (args.length == 0) {
         Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
         if (aliases.isEmpty()) {
            ChatUtils.INSTANCE.sendMessage("Псевдонимы не настроены", MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; list ; remove ; clear", MessageType.WARNING);
         } else {
            this.listAliases();
         }
      } else {
         String subcommand = args[0].toLowerCase();
         switch (subcommand) {
            case "list":
               this.listAliases();
               break;
            case "add":
               if (args.length >= 3) {
                  String alias = args[1];
                  String command = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                  this.addAlias(alias, command);
               } else {
                  ChatUtils.INSTANCE.sendMessage("Использование: " + ChatUtils.INSTANCE.formatCommand("alias") + " add <имя> <команда>", MessageType.ERROR);
               }
               break;
            case "remove":
               if (args.length >= 2) {
                  this.removeAlias(args[1]);
               } else {
                  ChatUtils.INSTANCE.sendMessage("Использование: " + ChatUtils.INSTANCE.formatCommand("alias") + " remove <имя>", MessageType.ERROR);
               }
               break;
            case "clear":
               this.clearAliases();
               break;
            default:
               ChatUtils.INSTANCE.sendMessage("Неизвестная подкоманда: " + subcommand, MessageType.ERROR);
               ChatUtils.INSTANCE.sendMessage("Попробуйте: add ; list ; remove ; clear", MessageType.WARNING);
         }
      }
   }

   private void listAliases() {
      Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
      if (aliases.isEmpty()) {
         ChatUtils.INSTANCE.sendMessage("Псевдонимы не настроены", MessageType.WARNING);
      } else {
         ChatUtils.INSTANCE.sendMessage("=== Псевдонимы команд ===", MessageType.WARNING);
         aliases.forEach((alias, command) -> ChatUtils.INSTANCE.sendMessage(alias + " -> " + command, MessageType.WARNING));
      }
   }

   private void addAlias(String alias, String command) {
      ChatUtils.INSTANCE.addAlias(alias, command);
      ChatUtils.INSTANCE.sendMessage("Псевдоним создан: " + alias + " -> " + command, MessageType.SUCCESS);
   }

   private void removeAlias(String alias) {
      if (ChatUtils.INSTANCE.getAliases().containsKey(alias.toLowerCase())) {
         ChatUtils.INSTANCE.removeAlias(alias);
         ChatUtils.INSTANCE.sendMessage("Псевдоним удалён: " + alias, MessageType.SUCCESS);
      } else {
         ChatUtils.INSTANCE.sendMessage("Псевдоним не найден: " + alias, MessageType.ERROR);
      }
   }

   private void clearAliases() {
      Map<String, String> aliases = ChatUtils.INSTANCE.getAliases();
      if (aliases.isEmpty()) {
         ChatUtils.INSTANCE.sendMessage("Псевдонимы уже пусты", MessageType.WARNING);
      } else {
         int count = aliases.size();
         aliases.keySet().forEach(alias -> ChatUtils.INSTANCE.removeAlias(alias));
         ChatUtils.INSTANCE.sendMessage("Удалено псевдонимов: " + count, MessageType.SUCCESS);
      }
   }
}
