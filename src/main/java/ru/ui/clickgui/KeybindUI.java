package ru.ui.clickgui;

import ru.event.impl.EventSystemImpl;
import ru.event.impl.KeyPressEvent;
import ru.ui.notify.NotificationManager;
import ru.ui.notify.NotificationMessages;
import ru.ui.notify.NotificationType;

import java.util.function.Consumer;

public class KeybindUI {
    private static boolean waitingForKey = false;
    private static String bindingTarget = null;
    private static Consumer<Integer> bindCallback = null;
    private static Consumer<KeyPressEvent> keyPressListener = null;
    
    public static void startBinding(String moduleName, Consumer<Integer> callback) {
        startBinding(moduleName, null, callback);
    }
    
    public static void startBinding(String moduleName, String settingName, Consumer<Integer> callback) {
        waitingForKey = true;
        bindingTarget = settingName != null ? moduleName + ", " + settingName : moduleName;
        bindCallback = callback;
        
        String message = settingName != null ?
            NotificationMessages.bindingModuleSetting(moduleName, settingName) :
            NotificationMessages.bindingModule(moduleName);
        
        NotificationManager.getInstance().showNotification(
            message,
            NotificationType.INFO,
            10000
        );
        
        keyPressListener = KeybindUI::handleKeyPress;
        EventSystemImpl.getInstance().subscribe(KeyPressEvent.class, keyPressListener);
    }
    
    private static void handleKeyPress(KeyPressEvent event) {
        if (!waitingForKey) return;
        
        NotificationManager.getInstance().showNotification(
            NotificationMessages.keyPressed(event.keyName),
            NotificationType.INFO,
            1000
        );
        
        if (bindCallback != null) {
            bindCallback.accept(event.keyCode);
        }
        
        waitingForKey = false;
        bindingTarget = null;
        bindCallback = null;
        
        if (keyPressListener != null) {
            EventSystemImpl.getInstance().unsubscribe(KeyPressEvent.class, keyPressListener);
            keyPressListener = null;
        }
    }
    
    public static boolean isWaitingForKey() {
        return waitingForKey;
    }
}
