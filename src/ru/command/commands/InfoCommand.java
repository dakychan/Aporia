package ru.command.commands;

import aporia.cc.Logger;
import aporia.cc.chat.ChatUtils;
import aporia.cc.chat.ChatUtils.MessageType;
import aporia.cc.user.UserData;
import aporia.cc.user.UserData.UserDataClass;
import ru.command.Command;

public class InfoCommand implements Command {
   @Override
   public String getName() {
      return "info";
   }

   @Override
   public String getDescription() {
      return "Отображение информации о клиенте";
   }

   @Override
   public String getUsage() {
      return ChatUtils.INSTANCE.formatCommand("info") + " - Показать UID, username и HWID";
   }

   @Override
   public void execute(String[] args) {
      try {
         UserDataClass userData = ru.Aporia.getFilesManager().loadStats();
         if (userData == null) {
            userData = UserData.getUserData();
            ru.Aporia.getFilesManager().saveStats(userData);
         }

         ChatUtils.INSTANCE.sendMessage("=== Информация о клиенте ===", MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("UID: " + (userData.getUuid() != null ? userData.getUuid() : "N/A"), MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("Username: " + (userData.getUsername() != null ? userData.getUsername() : "N/A"), MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("HWID: " + (userData.getHardwareId() != null ? userData.getHardwareId() : "N/A"), MessageType.WARNING);
         ChatUtils.INSTANCE.sendMessage("Role: " + (userData.getRole() != null ? userData.getRole().toString() : "N/A"), MessageType.WARNING);
      } catch (Exception var3) {
         ChatUtils.INSTANCE.sendMessage("Не удалось загрузить данные пользователя: " + var3.getMessage(), MessageType.ERROR);
         Logger.INSTANCE.error("Failed to load user data", var3);
      }
   }
}
