package ru.command.commands;

import ru.Aporia;
import ru.command.Command;
import ru.command.CommandManager;

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
        return ".config save - Сохранить текущую конфигурацию";
    }
    
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.sendChatMessage("§cИспользование: " + getUsage());
            return;
        }
        
        if (args[0].equalsIgnoreCase("save")) {
            try {
                saveConfiguration();
                CommandManager.sendChatMessage("§aКонфигурация успешно сохранена");
            } catch (Exception e) {
                CommandManager.sendChatMessage("§cОшибка при сохранении конфигурации: " + e.getMessage());
                ru.files.Logger.INSTANCE.error("Failed to save configuration", e);
            }
        } else {
            CommandManager.sendChatMessage("§cНеизвестная подкоманда: " + args[0]);
            CommandManager.sendChatMessage("§eИспользование: " + getUsage());
        }
    }
    
    private void saveConfiguration() {
        ru.module.ModuleManager.getInstance().saveConfig();
    }
}
