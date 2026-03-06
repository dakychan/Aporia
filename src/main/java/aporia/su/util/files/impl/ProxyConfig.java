package aporia.su.util.files.impl;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import aporia.su.util.helper.Logger;
import aporia.su.util.user.network.proxy.Proxy;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

/**
 * Конфиг для прокси настроек.
 */
public class ProxyConfig {
    private static ProxyConfig instance;
    private static final String CONFIG_NAME = "proxy";
    private static final Path CONFIG_DIR = OsManager.mainDirectory.resolve("configs").resolve("proxy");

    @Getter @Setter
    private boolean proxyEnabled = false;

    @Getter @Setter
    private Proxy defaultProxy = new Proxy();

    @Getter @Setter
    private Proxy lastUsedProxy = new Proxy();

    private ProxyConfig() {}

    public static ProxyConfig getInstance() {
        if (instance == null) {
            instance = new ProxyConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("proxyEnabled", proxyEnabled);

            JsonObject defaultProxyJson = new JsonObject();
            defaultProxyJson.addProperty("ipPort", defaultProxy.ipPort);
            defaultProxyJson.addProperty("type", defaultProxy.type.name());
            defaultProxyJson.addProperty("username", defaultProxy.username);
            defaultProxyJson.addProperty("password", defaultProxy.password);
            root.add("defaultProxy", defaultProxyJson);

            JsonObject lastUsedProxyJson = new JsonObject();
            lastUsedProxyJson.addProperty("ipPort", lastUsedProxy.ipPort);
            lastUsedProxyJson.addProperty("type", lastUsedProxy.type.name());
            lastUsedProxyJson.addProperty("username", lastUsedProxy.username);
            lastUsedProxyJson.addProperty("password", lastUsedProxy.password);
            root.add("lastUsedProxy", lastUsedProxyJson);

            FilesManager.createFile(
                CONFIG_DIR,
                FilesManager.FileFormat.APR,
                CONFIG_NAME,
                root.toString(),
                FilesManager.CheckMode.ALWAYS
            );
        } catch (Exception e) {
            Logger.error("ProxyConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            Path configPath = FilesManager.getFilePath(CONFIG_DIR, CONFIG_NAME, FilesManager.FileFormat.APR);
            
            if (!FilesManager.exists(configPath)) {
                save();
                return;
            }

            String json = FilesManager.readFile(configPath);
            if (json == null || json.isEmpty()) {
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("proxyEnabled")) {
                proxyEnabled = root.get("proxyEnabled").getAsBoolean();
            }

            if (root.has("defaultProxy")) {
                defaultProxy = parseProxy(root.getAsJsonObject("defaultProxy"));
            }

            if (root.has("lastUsedProxy")) {
                lastUsedProxy = parseProxy(root.getAsJsonObject("lastUsedProxy"));
            }

            Logger.success("ProxyConfig loaded!");
        } catch (Exception e) {
            Logger.error("ProxyConfig: Load failed! " + e.getMessage());
        }
    }

    private Proxy parseProxy(JsonObject json) {
        Proxy proxy = new Proxy();

        if (json.has("ipPort")) {
            proxy.ipPort = json.get("ipPort").getAsString();
        }
        if (json.has("type")) {
            try {
                proxy.type = Proxy.ProxyType.valueOf(json.get("type").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }
        if (json.has("username")) {
            proxy.username = json.get("username").getAsString();
        }
        if (json.has("password")) {
            proxy.password = json.get("password").getAsString();
        }

        return proxy;
    }

    public void setDefaultProxyAndSave(Proxy proxy) {
        this.defaultProxy = proxy;
        save();
    }

    public void setProxyEnabledAndSave(boolean enabled) {
        this.proxyEnabled = enabled;
        save();
    }

    public void setLastUsedProxyAndSave(Proxy proxy) {
        this.lastUsedProxy = proxy;
        save();
    }
}
