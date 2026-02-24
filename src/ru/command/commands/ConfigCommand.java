package ru.command.commands;

import aporia.cc.Logger;
import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import ru.command.Command;
import ru.module.ModuleManager;

public class ConfigCommand implements Command {
   @Override
   public String getName() {
      return "config";
   }

   @Override
   public String getDescription() {
      return "Управление конфигурацией клиента";
   }

   @Override
   public String getUsage() {
      return ChatUtils.INSTANCE.formatCommand("config") + " save - Сохранить текущую конфигурацию";
   }

   @Override
   public void execute(String[] args) {
      if (args.length == 0) {
         ChatUtils.INSTANCE.sendMessage("Конфигурация не изменена", MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("Попробуйте: save", MessageType.WARNING);
      } else {
         if (args[0].equalsIgnoreCase("save")) {
            try {
               this.saveConfiguration();
               ChatUtils.INSTANCE.sendMessage("Конфигурация успешно сохранена", MessageType.SUCCESS);
            } catch (Exception var3) {
               ChatUtils.INSTANCE.sendMessage("Ошибка при сохранении конфигурации: " + var3.getMessage(), MessageType.ERROR);
               Logger.INSTANCE.error("Failed to save configuration", var3);
            }
         } else {
            ChatUtils.INSTANCE.sendMessage("Неизвестная подкоманда: " + args[0], MessageType.ERROR);
            ChatUtils.INSTANCE.sendMessage("Использование: " + this.getUsage(), MessageType.WARNING);
         }
      }
   }

   private void saveConfiguration() {
      ModuleManager.getInstance().saveConfig();
   }
}
