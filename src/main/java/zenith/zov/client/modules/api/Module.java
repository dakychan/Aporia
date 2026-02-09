package zenith.zov.client.modules.api;


import com.darkmagician6.eventapi.EventManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import zenith.zov.Zenith;

import zenith.zov.base.events.impl.other.EventModuleToggle;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.client.modules.api.setting.impl.*;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import zenith.zov.utility.interfaces.IClient;
import zenith.zov.utility.interfaces.IMinecraft;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class Module implements IClient,Comparable<Module> {
    protected ModuleAnnotation info = this.getClass().getAnnotation(ModuleAnnotation.class);


    private String name;

    private final Category category;

    private volatile boolean enabled;

    private int keyCode;


    protected Module() {
        name = info.name();
        category = info.category();
        enabled = false;
        keyCode = -1;
    }


    public void setToggled(boolean state) {
        if (state) {
            this.onEnable();
        } else {
            this.onDisable();
        }
        this.enabled = state;
    }

    public void toggle() {
        this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
        } else {
            this.onDisable();
        }
    }


    public void onEnable() {
        EventManager.register(this);
        EventManager.call(new EventModuleToggle(this,enabled));

    }

    public void onDisable() {
        EventManager.unregister(this);
        EventManager.call(new EventModuleToggle(this,enabled));
    }

    public List<Setting> getSettings() {
        return Arrays.stream(this.getClass().getDeclaredFields()).map(field -> {
            try {
                field.setAccessible(true);
                return field.get(this);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        }).filter(field -> field instanceof Setting).map(field -> (Setting) field).collect(Collectors.toList());
    }

    public JsonObject save() {

        JsonObject object = new JsonObject();
        object.addProperty("enabled", this.enabled);
        object.addProperty("keyCode", this.keyCode);
        JsonObject propertiesObject = new JsonObject();

        for (Setting setting : getSettings()) {
           setting.safe(propertiesObject);

        }
        object.add("Settings", propertiesObject);
        return object;
    }

    public void load(JsonObject object) {
        try {

            if (object != null) {
                if (object.has("enabled")) {
                    boolean enable = object.get("enabled").getAsBoolean();
                    if (enable &&!this.isEnabled()) {
                        this.toggle();
                    }
                    if(!enable &&this.isEnabled()) {
                        this.toggle();
                    }
                }

                if (object.has("keyCode")) {
                    keyCode = (object.get("keyCode").getAsInt());
                }

                for (Setting setting : getSettings()) {
                    String valueOf = setting.getName();
                    JsonObject propertiesObject = object.getAsJsonObject("Settings");
                    if (propertiesObject == null)
                        continue;
                    if (!propertiesObject.has(valueOf))
                        continue;
                    setting.load(propertiesObject);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int compareTo(@NotNull Module o) {
        return o.getName().compareTo(this.name);
    }
}
