package ru.ui.notify;

public class NotificationMessages {
    public static String playerBrokeShield(String playerName) {
        return String.format("Игрок %s пробил щит!", playerName);
    }
    
    public static String moderatorJoinedSpectator() {
        return "Модератор зашел в спек!";
    }
    
    public static String potionsRunningOut() {
        return "Зелия заканчиваются!";
    }
    
    public static String moduleToggled(String moduleName, boolean enabled) {
        return String.format("Модуль %s %s", moduleName, 
                           enabled ? "включен" : "выключен");
    }
    
    public static String moduleEnabled(String moduleName) {
        return String.format("%s был/была включена", moduleName);
    }
    
    public static String moduleToggledByUser(String moduleName, boolean enabled) {
        return String.format("Вы %s %s", 
                           enabled ? "включили" : "выключили", 
                           moduleName);
    }
    
    public static String moduleEnableFailed(String moduleName) {
        return String.format("Не удалось включить модуль %s", moduleName);
    }
    
    public static String folderCreationFailed() {
        return "Не смог создать папки";
    }
    
    public static String moduleActivationFailed() {
        return "Не удалось активировать / деактивировать модуль";
    }
    
    public static String bindingModule(String moduleName) {
        return String.format("Биндим %s на ..", moduleName);
    }
    
    public static String bindingModuleSetting(String moduleName, String settingName) {
        return String.format("Биндим %s, %s на ..", moduleName, settingName);
    }
    
    public static String keyPressed(String keyName) {
        return String.format("Нажата клавиша: %s", keyName);
    }
}
