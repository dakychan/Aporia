package zenith.zov.base.notify;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;


import net.minecraft.text.Text;
import zenith.zov.base.events.impl.other.EventModuleToggle;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.hud.elements.component.NotifyComponent;

public class NotifyManager {
    
    private static NotifyManager instance;
    private NotifyComponent notifyComponent;
    
    public NotifyManager() {
        EventManager.register(this);
    }
    
    public static NotifyManager getInstance() {
        if (instance == null) {
            instance = new NotifyManager();
        }
        return instance;
    }
    
    public void setNotifyComponent(NotifyComponent component) {
        this.notifyComponent = component;
    }
    
    @EventTarget
    public void onModuleToggle(EventModuleToggle event) {
        if (notifyComponent != null) {
            notifyComponent.addNotification(event.getModule(), event.isEnabled());
        }
    }
    
    public void addNotification(Module module, boolean enabled) {
        if (notifyComponent != null) {
            notifyComponent.addNotification(module, enabled);
        }
    }
    public void addNotification(String icon,Text module) {
        if (notifyComponent != null) {
            notifyComponent.addTextNotification(icon,module);
        }
    }
} 