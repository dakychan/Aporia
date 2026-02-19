package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import aporia.cc.user.UserData;
import ru.Aporia;
import ru.command.Command;

/**
 * Command for displaying client information.
 */
public class InfoCommand implements Command {
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "info";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Отображение информации о клиенте";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return ChatUtils.INSTANCE.formatCommand("info") + " - Показать UID, username и HWID";
    }
    
    /**
     * Execute the info command.
     * 
     * @param args Command arguments (unused)
     */
    @Override
    public void execute(String[] args) {
        try {
            UserData.UserDataClass userData = Aporia.getFilesManager().loadStats();
            
            if (userData == null) {
                userData = UserData.INSTANCE.getUserData();
                Aporia.getFilesManager().saveStats(userData);
            }
            
            ChatUtils.INSTANCE.sendMessage("=== Информация о клиенте ===", ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("UID: " + (userData.getUuid() != null ? userData.getUuid() : "N/A"), ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Username: " + (userData.getUsername() != null ? userData.getUsername() : "N/A"), ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("HWID: " + (userData.getHardwareId() != null ? userData.getHardwareId() : "N/A"), ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Role: " + (userData.getRole() != null ? userData.getRole().toString() : "N/A"), ChatUtils.MessageType.WARNING);
            
        } catch (Exception e) {
            ChatUtils.INSTANCE.sendMessage("Не удалось загрузить данные пользователя: " + e.getMessage(), ChatUtils.MessageType.ERROR);
            aporia.cc.Logger.INSTANCE.error("Failed to load user data", e);
        }
    }
}
