package zenith.zov.base.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import zenith.zov.Zenith;
import zenith.zov.base.theme.Theme;
import zenith.zov.base.theme.ThemeManager;
import zenith.zov.client.modules.api.Module;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.io.File;
import java.io.IOException;

@Getter
public class Config {
    private final String name;
    private final File file;

    public Config(String name) {
        this.name = name;
        this.file = new File(ConfigManager.configDirectory, name + "." + Zenith.NAME.toLowerCase());

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public JsonObject save() {
        try {
            JsonObject root = new JsonObject();

            
            JsonObject modulesObject = new JsonObject();
            for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
                modulesObject.add(module.getName(), module.save());
            }
            root.add("Modules", modulesObject);

            
            ThemeManager themeManager = Zenith.getInstance().getThemeManager();

            JsonObject themeObject = new JsonObject();
            themeObject.addProperty("selected", themeManager.getCurrentTheme().getName());
            themeObject.addProperty("columns", Zenith.getInstance().getMenuScreen().getColumns());

            
            JsonObject items = new JsonObject();
            for (Theme t : themeManager.getThemes()) {
                items.add(t.getName(), serializeTheme(t));
            }
            themeObject.add("items", items);

            root.add("Theme", themeObject);

            return root;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }

    public void load(JsonObject object) {
        
        if (object.has("Theme")) {
            JsonObject themeObject = object.getAsJsonObject("Theme");

            
            if (themeObject.has("selected")) {
                String selected = themeObject.get("selected").getAsString();
                Zenith.getInstance().getThemeManager().switchThemeByName(selected);
            }
            if (themeObject.has("columns")) {
                int columns = themeObject.get("columns").getAsInt();
                Zenith.getInstance().getMenuScreen().setColumns(columns);
            }

            ThemeManager themeManager = Zenith.getInstance().getThemeManager();

            
            if (themeObject.has("items")) {
                JsonObject items = themeObject.getAsJsonObject("items");
                for (Theme t : themeManager.getThemes()) {
                    if (items.has(t.getName())) {
                        JsonObject tObj = items.getAsJsonObject(t.getName());
                        applyThemeFromJson(t, tObj);
                    }
                }
            }

            
        }

        
        if (object.has("Modules")) {
            try {
                JsonObject modulesObject = object.getAsJsonObject("Modules");
                for (Module module : Zenith.getInstance().getModuleManager().getModules()) {
                    module.load(modulesObject.getAsJsonObject(module.getName()));
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    

    private JsonObject serializeTheme(Theme theme) {
        JsonObject o = new JsonObject();
        putColor(o, "color", theme.getColor());
        putColor(o, "secondColor", theme.getSecondColor());
        putColor(o, "friendColor", theme.getFriendColor());

        putColor(o, "gray", theme.getGray());
        putColor(o, "grayLight", theme.getGrayLight());
        putColor(o, "foregroundLight", theme.getForegroundLight());
        putColor(o, "whiteGray", theme.getWhiteGray());
        putColor(o, "foregroundGray", theme.getForegroundGray());
        putColor(o, "foregroundLightStroke", theme.getForegroundLightStroke());
        putColor(o, "foregroundColor", theme.getForegroundColor());
        putColor(o, "foregroundStroke", theme.getForegroundStroke());
        putColor(o, "foregroundDark", theme.getForegroundDark());
        putColor(o, "white", theme.getWhite());
        putColor(o, "backgroundColor", theme.getBackgroundColor());

        o.addProperty("glow", theme.isGlow());
        o.addProperty("blur", theme.isBlur());
        o.addProperty("corners", theme.isCorners());
        return o;
    }

    private void applyThemeFromJson(Theme theme, JsonObject obj) {
        
        ColorRGBA val;
        if ((val = getColor(obj, "color")) != null) theme.setColor(val);
        if ((val = getColor(obj, "secondColor")) != null) theme.setSecondColor(val);
        if ((val = getColor(obj, "friendColor")) != null) theme.setFriendColor(val);
        if ((val = getColor(obj, "gray")) != null) theme.setGray(val);
        if ((val = getColor(obj, "grayLight")) != null) theme.setGrayLight(val);
        if ((val = getColor(obj, "foregroundLight")) != null) theme.setForegroundLight(val);
        if ((val = getColor(obj, "whiteGray")) != null) theme.setWhiteGray(val);
        if ((val = getColor(obj, "foregroundGray")) != null) theme.setForegroundGray(val);
        if ((val = getColor(obj, "foregroundLightStroke")) != null) theme.setForegroundLightStroke(val);
        if ((val = getColor(obj, "foregroundColor")) != null) theme.setForegroundColor(val);
        if ((val = getColor(obj, "foregroundStroke")) != null) theme.setForegroundStroke(val);
        if ((val = getColor(obj, "foregroundDark")) != null) theme.setForegroundDark(val);
        if ((val = getColor(obj, "white")) != null) theme.setWhite(val);
        if ((val = getColor(obj, "backgroundColor")) != null) theme.setBackgroundColor(val);

        
        if (obj.has("glow")) theme.setGlow(getBool(obj, "glow", false));
        if (obj.has("blur")) theme.setBlur(getBool(obj, "blur", false));
        if (obj.has("corners")) theme.setCorners(getBool(obj, "corners", false));
    }

    private boolean hasAnyLegacyThemeFields(JsonObject o) {
        
        return o.has("color") || o.has("secondColor") || o.has("backgroundColor");
    }

    private void putColor(JsonObject obj, String key, ColorRGBA color) {
        if (color != null) obj.addProperty(key, color.getRGB());
    }

    private ColorRGBA getColor(JsonObject obj, String key) {
        return obj.has(key) ? new ColorRGBA(obj.get(key).getAsInt()) : null;
    }

    private boolean getBool(JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        return (el == null || el.isJsonNull()) ? def : el.getAsBoolean();
    }
}
