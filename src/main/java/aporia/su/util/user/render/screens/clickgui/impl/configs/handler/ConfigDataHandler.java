package aporia.su.util.user.render.screens.clickgui.impl.configs.handler;

import aporia.cc.OsManager;
import aporia.su.util.files.FilesManager;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConfigDataHandler {

    private final List<String> configs = new ArrayList<>();
    private final ConfigAnimationHandler animationHandler;

    private String selectedConfig = null;
    private boolean isCreating = false;
    private String newConfigName = "";

    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private float scrollTopFade = 0f;
    private float scrollBottomFade = 0f;

    public ConfigDataHandler(ConfigAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void refreshConfigs() {
        List<String> oldConfigs = new ArrayList<>(configs);
        configs.clear();
        try {
            Path configDir = OsManager.mainDirectory.resolve("configs");
            if (Files.exists(configDir)) {
                Files.list(configDir)
                        .filter(path -> path.toString().endsWith(".apr"))
                        .forEach(path -> {
                            String name = path.getFileName().toString();
                            String configName = name.substring(0, name.length() - 4);
                            if (!configName.equalsIgnoreCase("config")) {
                                configs.add(configName);
                            }
                        });
            }
        } catch (IOException ignored) {}

        for (String config : configs) {
            if (!oldConfigs.contains(config)) {
                animationHandler.getItemAppearAnimations().put(config, 0f);
            }
        }
    }

    public void updateScroll(float deltaTime) {
        scrollOffset += (targetScrollOffset - scrollOffset) * 12f * deltaTime;
    }

    public void updateScrollFades(float visibleHeight) {
        float contentHeight = configs.size() * 27f;

        if (contentHeight <= visibleHeight) {
            scrollTopFade = 0f;
            scrollBottomFade = 0f;
            return;
        }

        float maxScroll = contentHeight - visibleHeight;
        scrollTopFade = (float) Math.min(1f, -scrollOffset / 20f);
        scrollBottomFade = (float) Math.min(1f, (maxScroll + scrollOffset) / 20f);
    }

    public void handleScroll(double vertical, float visibleHeight) {
        float contentHeight = configs.size() * 27f;
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        targetScrollOffset += vertical * 25;
        targetScrollOffset = Math.max(-maxScroll, Math.min(0, targetScrollOffset));
    }

    public boolean saveConfig(String name) {
        if (name.equalsIgnoreCase("config")) {
            return false;
        }

        try {
            Path configDir = OsManager.mainDirectory.resolve("configs");
            Path newConfig = configDir.resolve(name + ".apr");

            if (Files.exists(newConfig)) {
                return false;
            }

            FilesManager.getConfigManager().save();
            Path currentConfig = FilesManager.getFilePath(configDir, "config", FilesManager.FileFormat.APR);
            Files.copy(currentConfig, newConfig);
            refreshConfigs();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean loadConfig(String name) {
        try {
            Path configDir = OsManager.mainDirectory.resolve("configs");
            Path configFile = configDir.resolve(name + ".apr");
            return Files.exists(configFile);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean refreshConfig(String name) {
        try {
            Path configDir = OsManager.mainDirectory.resolve("configs");
            Path configFile = configDir.resolve(name + ".apr");

            if (!Files.exists(configFile)) {
                return false;
            }

            FilesManager.getConfigManager().save();
            Files.deleteIfExists(configFile);
            Path currentConfig = FilesManager.getFilePath(configDir, "config", FilesManager.FileFormat.APR);
            Files.copy(currentConfig, configFile);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteConfig(String name) {
        try {
            Path configDir = OsManager.mainDirectory.resolve("configs");
            Path configFile = configDir.resolve(name + ".apr");

            if (Files.exists(configFile)) {
                Files.delete(configFile);
                if (name.equals(selectedConfig)) {
                    selectedConfig = null;
                }
                refreshConfigs();
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public void toggleCreating() {
        isCreating = !isCreating;
        if (!isCreating) {
            newConfigName = "";
        }
    }

    public void appendChar(char chr) {
        if (newConfigName.length() < 32 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            newConfigName += chr;
        }
    }

    public void removeLastChar() {
        if (!newConfigName.isEmpty()) {
            newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
        }
    }

    public void clearNewConfigName() {
        newConfigName = "";
    }
}