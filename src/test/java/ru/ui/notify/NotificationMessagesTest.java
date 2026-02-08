package ru.ui.notify;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NotificationMessagesTest {

    @Test
    void testPlayerBrokeShieldSubstitutesPlayerName() {
        String message = NotificationMessages.playerBrokeShield("TestPlayer");
        assertTrue(message.contains("TestPlayer"));
        assertTrue(message.contains("пробил щит"));
    }

    @Test
    void testPlayerBrokeShieldWithSpecialCharacters() {
        String message = NotificationMessages.playerBrokeShield("Player_123");
        assertTrue(message.contains("Player_123"));
    }

    @Test
    void testModeratorJoinedSpectatorReturnsCorrectMessage() {
        String message = NotificationMessages.moderatorJoinedSpectator();
        assertEquals("Модератор зашел в спек!", message);
    }

    @Test
    void testPotionsRunningOutReturnsCorrectMessage() {
        String message = NotificationMessages.potionsRunningOut();
        assertEquals("Зелия заканчиваются!", message);
    }

    @Test
    void testModuleToggledWhenEnabled() {
        String message = NotificationMessages.moduleToggled("KillAura", true);
        assertTrue(message.contains("KillAura"));
        assertTrue(message.contains("включен"));
    }

    @Test
    void testModuleToggledWhenDisabled() {
        String message = NotificationMessages.moduleToggled("Fly", false);
        assertTrue(message.contains("Fly"));
        assertTrue(message.contains("выключен"));
    }

    @Test
    void testModuleEnabledSubstitutesModuleName() {
        String message = NotificationMessages.moduleEnabled("Speed");
        assertTrue(message.contains("Speed"));
        assertTrue(message.contains("был/была включена"));
    }

    @Test
    void testModuleToggledByUserWhenEnabled() {
        String message = NotificationMessages.moduleToggledByUser("AutoArmor", true);
        assertTrue(message.contains("AutoArmor"));
        assertTrue(message.contains("включили"));
    }

    @Test
    void testModuleToggledByUserWhenDisabled() {
        String message = NotificationMessages.moduleToggledByUser("NoFall", false);
        assertTrue(message.contains("NoFall"));
        assertTrue(message.contains("выключили"));
    }

    @Test
    void testModuleEnableFailedSubstitutesModuleName() {
        String message = NotificationMessages.moduleEnableFailed("BrokenModule");
        assertTrue(message.contains("BrokenModule"));
        assertTrue(message.contains("Не удалось включить модуль"));
    }

    @Test
    void testFolderCreationFailedReturnsCorrectMessage() {
        String message = NotificationMessages.folderCreationFailed();
        assertEquals("Не смог создать папки", message);
    }

    @Test
    void testModuleActivationFailedReturnsCorrectMessage() {
        String message = NotificationMessages.moduleActivationFailed();
        assertEquals("Не удалось активировать / деактивировать модуль", message);
    }

    @Test
    void testBindingModuleSubstitutesModuleName() {
        String message = NotificationMessages.bindingModule("Sprint");
        assertTrue(message.contains("Sprint"));
        assertTrue(message.contains("Биндим"));
        assertTrue(message.contains("на .."));
    }

    @Test
    void testBindingModuleSettingSubstitutesBothNames() {
        String message = NotificationMessages.bindingModuleSetting("KillAura", "Range");
        assertTrue(message.contains("KillAura"));
        assertTrue(message.contains("Range"));
        assertTrue(message.contains("Биндим"));
        assertTrue(message.contains("на .."));
    }

    @Test
    void testKeyPressedSubstitutesKeyName() {
        String message = NotificationMessages.keyPressed("F");
        assertTrue(message.contains("F"));
        assertTrue(message.contains("Нажата клавиша"));
    }

    @Test
    void testKeyPressedWithSpecialKey() {
        String message = NotificationMessages.keyPressed("F13");
        assertTrue(message.contains("F13"));
    }

    @Test
    void testAllInfoMethodsReturnNonEmptyStrings() {
        assertFalse(NotificationMessages.playerBrokeShield("Player").isEmpty());
        assertFalse(NotificationMessages.moderatorJoinedSpectator().isEmpty());
        assertFalse(NotificationMessages.potionsRunningOut().isEmpty());
    }

    @Test
    void testAllModuleMethodsReturnNonEmptyStrings() {
        assertFalse(NotificationMessages.moduleToggled("Module", true).isEmpty());
        assertFalse(NotificationMessages.moduleEnabled("Module").isEmpty());
        assertFalse(NotificationMessages.moduleToggledByUser("Module", false).isEmpty());
    }

    @Test
    void testAllErrorMethodsReturnNonEmptyStrings() {
        assertFalse(NotificationMessages.moduleEnableFailed("Module").isEmpty());
        assertFalse(NotificationMessages.folderCreationFailed().isEmpty());
        assertFalse(NotificationMessages.moduleActivationFailed().isEmpty());
    }

    @Test
    void testAllKeybindMethodsReturnNonEmptyStrings() {
        assertFalse(NotificationMessages.bindingModule("Module").isEmpty());
        assertFalse(NotificationMessages.bindingModuleSetting("Module", "Setting").isEmpty());
        assertFalse(NotificationMessages.keyPressed("Key").isEmpty());
    }

    @Test
    void testModuleToggledConsistentFormat() {
        String enabled = NotificationMessages.moduleToggled("Test", true);
        String disabled = NotificationMessages.moduleToggled("Test", false);
        
        assertTrue(enabled.startsWith("Модуль Test"));
        assertTrue(disabled.startsWith("Модуль Test"));
    }

    @Test
    void testBindingModuleSettingContainsComma() {
        String message = NotificationMessages.bindingModuleSetting("Module", "Setting");
        assertTrue(message.contains(","));
    }
}
