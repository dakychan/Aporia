package ru.command.commands;

import aporia.cc.chat.ChatUtils;
import ru.command.Command;

/**
 * Command for managing client configuration.
 */
public class ConfigCommand implements Command {
    
    /**
     * Get command name.
     * 
     * @return Command name
     */
    @Override
    public String getName() {
        return "config";
    }
    
    /**
     * Get command description.
     * 
     * @return Command description
     */
    @Override
    public String getDescription() {
        return "Управление конфигурацией клиента";
    }
    
    /**
     * Get command usage.
     * 
     * @return Command usage string
     */
    @Override
    public String getUsage() {
        return "^config save - Сохранить текущую конфигурацию";
    }
    
    /**
     * Execute the config command.
     * 
     * @param args Command arguments
     */
    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            ChatUtils.INSTANCE.sendMessage("Конфигурация не изменена", ChatUtils.MessageType.WARNING);
            ChatUtils.INSTANCE.sendMessage("Попробуйте: save", ChatUtils.MessageType.WARNING);
            return;
        }
        
        if (args[0].equalsIgnoreCase("save")) {
            try {
                saveConfiguration();
                ChatUtils.INSTANCE.sendMessage("Конфигурация успешно сохранена", ChatUtils.MessageType.SUCCESS);
            } catch (Exception e) {
                ChatUtils.INSTANCE.sendMessage("Ошибка при сохранении конфигурации: " + e.getMessage(), ChatUtils.MessageType.ERROR);
                ru.files.Logger.INSTANCE.error("Failed to save configuration", e);
            }
        } else {
            ChatUtils.INSTANCE.sendMessage("Неизвестная подкоманда: " + args[0], ChatUtils.MessageType.ERROR);
            ChatUtils.INSTANCE.sendMessage("Использование: " + getUsage(), ChatUtils.MessageType.WARNING);
        }
    }
    
    /**
     * Save the current configuration.
     */
    private void saveConfiguration() {
        ru.module.ModuleManager.getInstance().saveConfig();
    }
}
