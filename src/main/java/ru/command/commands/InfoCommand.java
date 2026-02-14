package ru.command.commands;

import aporia.cc.user.UserData;
import ru.Aporia;
import ru.command.Command;
import ru.command.CommandManager;

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
        return ".info - Показать UID, username и HWID";
    }
    
    @Override
    public void execute(String[] args) {
        try {
            UserData.UserDataClass userData = Aporia.getFilesManager().loadStats();
            
            if (userData == null) {
                userData = UserData.INSTANCE.getUserData();
                Aporia.getFilesManager().saveStats(userData);
            }
            
            CommandManager.sendChatMessage("§e=== Информация о клиенте ===");
            CommandManager.sendChatMessage("§7UID: §f" + userData.getUuid());
            CommandManager.sendChatMessage("§7Username: §f" + userData.getUsername());
            CommandManager.sendChatMessage("§7HWID: §f" + userData.getHardwareId());
            CommandManager.sendChatMessage("§7Role: §f" + userData.getRole());
            
        } catch (Exception e) {
            CommandManager.sendChatMessage("§cНе удалось загрузить данные пользователя: " + e.getMessage());
            ru.files.Logger.INSTANCE.error("Failed to load user data", e);
        }
    }
}
