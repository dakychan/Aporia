package ru.ui.clickgui;

import aporia.cc.user.UserData;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.google.gson.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import aporia.cc.Logger;
import ru.files.PathResolver;
import ru.input.api.bind.Keybind;
import ru.input.impl.bind.KeybindManager;
import ru.module.Module;
import ru.module.ModuleManager;
import ru.render.BlurShader;
import ru.render.IconFont;
import ru.render.MsdfFont;
import ru.render.MsdfTextRenderer;
import ru.render.RectRenderer;
import ru.ui.clickgui.comp.Slider;
import ru.ui.clickgui.comp.MultiSetting;
import ru.ui.notify.Notify;
import ru.util.Lang;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ClickGuiScreen extends Screen {
    private static MsdfFont font;
    private static MsdfTextRenderer textRenderer;
    private static boolean initialized = false;
    
    /**
     * Text-only mode setting.
     */
    private boolean textOnlyMode = false;
    
    /**
     * Set text-only display mode.
     * 
     * @param enabled true to enable text-only mode
     */
    public void setTextOnlyMode(boolean enabled) {
        this.textOnlyMode = enabled;
    }
    
    /**
     * Get whether text-only mode is enabled.
     * 
     * @return true if text-only mode is enabled
     */
    public boolean isTextOnlyMode() {
        return this.textOnlyMode;
    }
    
    private Module hoveredModule = null;
    private final Set<Module> expandedModules = new HashSet<>();
    private final Map<Module, Float> settingsAnimations = new HashMap<>();
    private final Map<String, Slider> sliderCache = new HashMap<>();
    private final Map<String, MultiSetting> multiSettingCache = new HashMap<>();
    private final Set<Module.Setting<?>> expandedMultiSettings = new HashSet<>();

    private Module bindingModule = null;
    private Map<Module, Integer> moduleKeybinds = new HashMap<>();

    private boolean anyComponentDragging = false;
    private Slider currentlyDraggingSlider = null;
    
    private final Map<Module.Category, CategoryPanel> categoryPanels = new HashMap<>();
    private CategoryPanel draggingPanel = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    
    private static final int CATEGORY_WIDTH = 175;
    private static final int CATEGORY_SPACING = 15;
    private static final int MODULE_HEIGHT = 32;
    private static final int MODULE_SPACING = 8;
    private static final int HEADER_HEIGHT = 38;
    private static final int SETTING_SPACING = 35;
    private static final int SETTINGS_PADDING = 20;
    private static final float ANIMATION_SPEED = 0.15f;
    private static final float DESCRIPTION_Y_OFFSET = 20f;
    private static final float DESCRIPTION_DASH_SPACING = 8f;
    
    private BlurShader blurShader;

    public ClickGuiScreen(int width, int height) {
        super(Component.literal("Click GUI"));
        initFont();
        IconFont.init();
        Lang.load();
        initializeCategoryPanels();
        loadPanelPositions();
        loadModuleKeybinds();
        blurShader = new BlurShader();
    }

    private static void initFont() {
        if (initialized) return;
        try {
            font = new MsdfFont(
                "assets/aporia/fonts/Inter_Medium.json",
                "assets/aporia/fonts/Inter_Medium.png"
            );
            textRenderer = new MsdfTextRenderer(font);
            initialized = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void initializeCategoryPanels() {
        Module.Category[] categories = Module.Category.values();

        int startX = 100;
        int startY = 50;
        int categoryX = startX;
        
        for (Module.Category category : categories) {
            CategoryPanel panel = new CategoryPanel(category, categoryX, startY, CATEGORY_WIDTH, 385);
            categoryPanels.put(category, panel);
            categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
        }
    }
    
    /**
     * Load module keybinds from KeybindManager and populate the cache.
     * This method queries all registered modules and retrieves their keybind assignments.
     */
    private void loadModuleKeybinds() {
        moduleKeybinds.clear();

        for (Module module : ModuleManager.getInstance().getModules()) {
            String keybindId = "module." + module.getName().toLowerCase() + ".toggle";
            Keybind keybind = KeybindManager.getInstance().getKeybind(keybindId);
            
            if (keybind != null) {
                int keyCode = keybind.getKeyCode();
                if (keyCode > 0) {
                    moduleKeybinds.put(module, keyCode);
                }
            }
        }
    }
    
    private boolean panelsPositioned = false;
    
    private void centerPanels(int fbWidth, int fbHeight) {
        if (panelsPositioned) return;
        
        Module.Category[] categories = Module.Category.values();
        int totalWidth = (CATEGORY_WIDTH * categories.length) + (CATEGORY_SPACING * (categories.length - 1));
        int panelHeight = 385;

        int startX = (fbWidth - totalWidth) / 2;
        int startY = (fbHeight - panelHeight) / 2;
        int categoryX = startX;
        
        for (Module.Category category : categories) {
            CategoryPanel panel = categoryPanels.get(category);
            if (panel != null) {
                panel.setPosition(categoryX, startY);
                categoryX += CATEGORY_WIDTH + CATEGORY_SPACING;
            }
        }
        
        panelsPositioned = true;
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!initialized) return;
        
        /**
         * Update text-only mode from ClickGui module settings.
         */
        Module clickGuiModule = ModuleManager.getInstance().getModuleByName("ClickGui");
        if (clickGuiModule != null) {
            for (Module.Setting<?> setting : clickGuiModule.getSettings()) {
                if (setting.getName().equals("Text Only") && setting instanceof Module.BooleanSetting) {
                    this.textOnlyMode = ((Module.BooleanSetting) setting).getValue();
                    break;
                }
            }
        }

        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        plugin.bindMainFramebuffer(true);

        int fbWidth = plugin.getMainFramebufferWidth();
        int fbHeight = plugin.getMainFramebufferHeight();

        double scale = (double) fbWidth / this.width;
        int fbMouseX = (int)(mouseX * scale);
        int fbMouseY = (int)(mouseY * scale);

        centerPanels(fbWidth, fbHeight);
        
        if (blurShader != null) {
            blurShader.apply(fbWidth, fbHeight);
        }
        
        hoveredModule = null;

        updateSettingsAnimations();

        for (CategoryPanel panel : categoryPanels.values()) {
            renderCategory(panel, fbMouseX, fbMouseY);
        }
        
        if (hoveredModule != null) {
            renderModuleDescription(fbMouseX, fbMouseY);
        }
        
        renderDiscordAvatar(fbWidth, fbHeight);
    }
    
    private void renderCategory(CategoryPanel panel, int mouseX, int mouseY) {
        int x = panel.getX();
        int y = panel.getY();
        int width = panel.getWidth();
        int height = panel.getHeight();

        renderRectWithBlur(x, y, width, height, 8, RenderColor.of(20, 20, 25, 230), 3f);

        renderRectWithBlur(x, y, width, HEADER_HEIGHT, 8, RenderColor.of(30, 30, 38, 255), 3f);

        if (IconFont.isInitialized()) {
            MsdfTextRenderer iconRenderer = IconFont.getRenderer();
            String icon = IconFont.getIcon(panel.getCategory());
            if (iconRenderer != null) {
                iconRenderer.drawText(x + 10, y + 21, 18, icon, RenderColor.WHITE);
            }
        }

        if (textRenderer != null) {
            textRenderer.drawText(x + 35, y + 21, 16, 
                panel.getCategory().getDisplayName(), RenderColor.WHITE);
        }

        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(panel.getCategory());
        int moduleY = y + HEADER_HEIGHT + 5;
        
        for (Module module : modules) {
            if (moduleY + MODULE_HEIGHT > y + height - 5) {
                break;
            }

            float settingsProgress = settingsAnimations.getOrDefault(module, 0f);
            int maxSettingsHeight = calculateSettingsHeight(module);
            int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
            
            renderModule(module, x + 5, moduleY, width - 10, 
                MODULE_HEIGHT, mouseX, mouseY);

            if (settingsProgress > 0.01f) {
                renderModuleSettings(module, x + 5, moduleY + MODULE_HEIGHT, 
                    width - 10, settingsHeight, settingsProgress, mouseX, mouseY);
            }
            
            moduleY += MODULE_HEIGHT + MODULE_SPACING + settingsHeight;
        }
    }
    
    private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY) {
        boolean isExpanded = expandedModules.contains(module);
        float settingsProgress = settingsAnimations.getOrDefault(module, 0f);
        int maxSettingsHeight = calculateSettingsHeight(module);
        int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
        
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height &&
                           !(isExpanded && settingsProgress > 0.01f && mouseY > y + height);
        boolean isEnabled = module.isEnabled();
        
        if (isHovered) {
            hoveredModule = module;
        }
        
        RenderColor bgColor;
        RenderColor textColor;
        
        if (textOnlyMode) {
            bgColor = isHovered ? RenderColor.of(60, 60, 70, 220) : RenderColor.of(50, 50, 60, 200);
            textColor = isEnabled ? RenderColor.of(80, 200, 120, 255) : RenderColor.of(150, 150, 160, 255);
        } else {
            if (isEnabled) {
                bgColor = isHovered ? RenderColor.of(70, 130, 255, 255) : RenderColor.of(60, 120, 245, 230);
                textColor = RenderColor.WHITE;
            } else {
                bgColor = isHovered ? RenderColor.of(60, 60, 70, 220) : RenderColor.of(50, 50, 60, 200);
                textColor = RenderColor.of(180, 180, 190, 255);
            }
        }

        float radius = 5;
        renderRectWithBlur(x, y, width, height, radius, bgColor, 2f);

        if (textRenderer != null) {
            String displayText = (module == bindingModule) ? "Биндим на..." : module.getName();
            textRenderer.drawText(x + 8, y + 20, 15, displayText, textColor);

            if (moduleKeybinds.containsKey(module)) {
                int keyCode = moduleKeybinds.get(module);
                String keyName = ru.input.api.KeyboardKeys.getKeyName(keyCode);

                if (keyName == null || keyName.equals("NONE") || keyName.isEmpty()) {
                    Logger.INSTANCE.warn("Unknown key code for module " + module.getName() + ": " + keyCode);
                    keyName = "???";
                }

                float keyNameWidth = textRenderer.measureWidth(keyName, 13);
                float keyNameX = x + width - keyNameWidth - 8;

                RenderColor keyColor = RenderColor.of(150, 150, 160, 255);
                textRenderer.drawText(keyNameX, y + 20, 13, keyName, keyColor);
            }
        }
    }
    
    private void renderModuleSettings(Module module, int x, int y, int width, int height, float alpha, int fbMouseX, int fbMouseY) {
        if (height <= 0) return;

        int alphaValue = (int)(200 * alpha);
        RenderColor bgColor = RenderColor.of(35, 35, 45, alphaValue);
        renderRectWithBlur(x, y, width, height, 5, bgColor, 2f);

        if (alpha < 0.3f) return;
        
        List<Module.Setting<?>> settings = module.getSettings();
        
        if (settings.isEmpty()) {
            if (textRenderer != null) {
                textRenderer.drawText(x + 10, y + 15, 13, 
                    "Нет настроек", RenderColor.of(150, 150, 160, (int)(255 * alpha)));
            }
            return;
        }
        
        int settingY = y + 10;
        
        for (Module.Setting<?> setting : settings) {
            if (settingY > y + height - 25) break;
            
            if (setting instanceof Module.NumberSetting) {
                Module.NumberSetting numSetting = (Module.NumberSetting) setting;
                String sliderId = module.getName() + "." + setting.getName();
                
                Slider slider = sliderCache.computeIfAbsent(sliderId, k -> 
                    new Slider(setting.getName(), 
                        numSetting.getValue().floatValue(), 
                        (float)numSetting.getMin(), 
                        (float)numSetting.getMax())
                );

                if (!slider.isDragging()) {
                    slider.setValue(numSetting.getValue().floatValue());
                }
                
                slider.render(x + 10, settingY, width - 20, textRenderer, fbMouseX, fbMouseY);
                
                settingY += SETTING_SPACING;
            } else {
                if (textRenderer != null) {
                    textRenderer.drawText(x + 10, settingY + 8, 12, 
                        setting.getName(), RenderColor.of(200, 200, 210, (int)(255 * alpha)));
                }
                
                if (setting instanceof Module.BooleanSetting) {
                    Module.BooleanSetting boolSetting = (Module.BooleanSetting) setting;
                    String value = boolSetting.getValue() ? "Да" : "Нет";
                    RenderColor valueColor = boolSetting.getValue() 
                        ? RenderColor.of(80, 200, 120, (int)(255 * alpha))
                        : RenderColor.of(180, 180, 190, (int)(255 * alpha));
                    
                    if (textRenderer != null) {
                        float valueWidth = textRenderer.measureWidth(value, 12);
                        textRenderer.drawText(x + width - valueWidth - 10, settingY + 8, 12, value, valueColor);
                    }
                } else if (setting instanceof Module.ModeSetting) {
                    Module.ModeSetting modeSetting = (Module.ModeSetting) setting;
                    String value = modeSetting.getValue();
                    
                    if (textRenderer != null) {
                        float valueWidth = textRenderer.measureWidth(value, 12);
                        textRenderer.drawText(x + width - valueWidth - 10, settingY + 8, 12, 
                            value, RenderColor.of(100, 150, 255, (int)(255 * alpha)));
                    }
                } else if (setting instanceof ru.module.impl.visuals.Interface.MultiSetting) {
                    ru.module.impl.visuals.Interface.MultiSetting multiSettingData = 
                        (ru.module.impl.visuals.Interface.MultiSetting) setting;

                    String cacheKey = module.getName() + "_" + setting.getName();
                    MultiSetting multiSetting = multiSettingCache.get(cacheKey);
                    if (multiSetting == null) {
                        multiSetting = new MultiSetting(
                            setting.getName(),
                            multiSettingData.getOptions(),
                            multiSettingData.getValue()
                        );
                        multiSettingCache.put(cacheKey, multiSetting);
                    }

                    multiSetting.render(x + 10, settingY, width - 20, textRenderer, fbMouseX, fbMouseY, alpha);
                    settingY += multiSetting.getHeight();
                } else if (setting instanceof Module.StringSetting) {
                    Module.StringSetting strSetting = (Module.StringSetting) setting;
                    String value = strSetting.getValue();
                    if (value.length() > 15) {
                        value = value.substring(0, 12) + "...";
                    }
                    
                    if (textRenderer != null) {
                        float valueWidth = textRenderer.measureWidth(value, 12);
                        textRenderer.drawText(x + width - valueWidth - 10, settingY + 8, 12, 
                            value, RenderColor.of(180, 180, 190, (int)(255 * alpha)));
                    }
                }
                
                settingY += SETTING_SPACING;
            }
        }
    }
    
    private int calculateSettingsHeight(Module module) {
        List<Module.Setting<?>> settings = module.getSettings();
        if (settings.isEmpty()) {
            return 40;
        }
        
        int totalHeight = SETTINGS_PADDING;
        
        for (Module.Setting<?> setting : settings) {
            if (setting instanceof ru.module.impl.visuals.Interface.MultiSetting) {
                ru.module.impl.visuals.Interface.MultiSetting multiSettingData = 
                    (ru.module.impl.visuals.Interface.MultiSetting) setting;
                
                String cacheKey = module.getName() + "_" + setting.getName();
                MultiSetting multiSetting = multiSettingCache.get(cacheKey);
                if (multiSetting == null) {
                    multiSetting = new MultiSetting(
                        setting.getName(),
                        multiSettingData.getOptions(),
                        multiSettingData.getValue()
                    );
                    multiSettingCache.put(cacheKey, multiSetting);
                }
                
                totalHeight += multiSetting.getHeight();
            } else {
                totalHeight += SETTING_SPACING;
            }
        }
        
        return totalHeight;
    }
    
    private void updateSettingsAnimations() {
        for (Module module : ModuleManager.getInstance().getModules()) {
            float current = settingsAnimations.getOrDefault(module, 0f);
            float target = expandedModules.contains(module) ? 1f : 0f;
            
            if (Math.abs(current - target) > 0.01f) {
                float newValue = current + (target - current) * ANIMATION_SPEED;
                settingsAnimations.put(module, newValue);
            } else {
                settingsAnimations.put(module, target);
            }
        }
    }
    
    private void renderModuleDescription(int mouseX, int mouseY) {
        if (textRenderer == null || hoveredModule == null) return;
        
        String moduleName = hoveredModule.getName();
        String description = Lang.getModuleDescription(moduleName);
       
        if (description.equals("module." + moduleName.toLowerCase() + ".description")) {
            description = hoveredModule.getDescription();
        }

        if (description == null || description.isEmpty()) return;

        String fullText = moduleName + " - " + description;
      
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        int fbWidth = plugin.getMainFramebufferWidth();
      
        float nameWidth = textRenderer.measureWidth(moduleName, 30);
        float dashWidth = textRenderer.measureWidth(" - ", 26);
        float descWidth = textRenderer.measureWidth(description, 26);
        float totalWidth = nameWidth + dashWidth + descWidth;
        
        int tooltipX = (int)((fbWidth - totalWidth) / 2);
        int tooltipY = 60;
    
        textRenderer.drawText(tooltipX, tooltipY, 30, 
            moduleName, RenderColor.WHITE);
   
        float offsetX = tooltipX + nameWidth + DESCRIPTION_DASH_SPACING;
        textRenderer.drawText(offsetX, tooltipY + 4, 26, 
            " - " + description, RenderColor.of(200, 200, 210, 255));
    }

    private void renderRect(float x, float y, float w, float h, float radius, RenderColor color) {
        RectRenderer.drawRoundedRect(x, y, w, h, radius, color);
    }
    
    private void renderRectWithBlur(float x, float y, float w, float h, float radius, RenderColor color, float blurAmount) {
        RectRenderer.drawRectangleWithBlur(x, y, w, h, color, radius, blurAmount);
    }
    
    private boolean handleSettingClick(Module module, int mouseX, int mouseY) {
        if (!expandedModules.contains(module)) return false;
        
        float settingsProgress = settingsAnimations.getOrDefault(module, 0f);
        if (settingsProgress < 0.3f) return false;
        
        CategoryPanel panel = null;
        for (CategoryPanel p : categoryPanels.values()) {
            if (p.getCategory() == module.getCategory()) {
                panel = p;
                break;
            }
        }
        
        if (panel == null) return false;
        
        List<Module> modules = ModuleManager.getInstance().getModulesByCategory(module.getCategory());
        int moduleY = panel.getY() + HEADER_HEIGHT + 5;
        int moduleX = panel.getX() + 5;
        int moduleWidth = panel.getWidth() - 10;
        
        for (Module m : modules) {
            if (m == module) {
                int settingsY = moduleY + MODULE_HEIGHT;
                int maxSettingsHeight = calculateSettingsHeight(module);
                int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
                
                if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth &&
                    mouseY >= settingsY && mouseY <= settingsY + settingsHeight) {
                    
                    List<Module.Setting<?>> settings = module.getSettings();
                    int settingY = settingsY + 10;
                    
                    for (Module.Setting<?> setting : settings) {
                        if (settingY > settingsY + settingsHeight - 25) break;
                        
                        if (setting instanceof Module.NumberSetting) {
                            String sliderId = module.getName() + "." + setting.getName();
                            Slider slider = sliderCache.get(sliderId);
                            if (slider != null) {
                                if (slider.mouseClicked(mouseX, mouseY, 0)) {
                                    anyComponentDragging = true;
                                    currentlyDraggingSlider = slider;
                                    return true;
                                }
                            }
                            settingY += SETTING_SPACING;
                        } else if (setting instanceof ru.module.impl.visuals.Interface.MultiSetting) {
                            ru.module.impl.visuals.Interface.MultiSetting multiSettingData = 
                                (ru.module.impl.visuals.Interface.MultiSetting) setting;
                            
                            String cacheKey = module.getName() + "_" + setting.getName();
                            MultiSetting multiSetting = multiSettingCache.get(cacheKey);
                            if (multiSetting == null) {
                                multiSetting = new MultiSetting(
                                    setting.getName(),
                                    multiSettingData.getOptions(),
                                    multiSettingData.getValue()
                                );
                                multiSettingCache.put(cacheKey, multiSetting);
                            }
                            
                            if (multiSetting.mouseClicked(mouseX, mouseY, 0)) {
                                return true;
                            }
                            
                            settingY += multiSetting.getHeight();
                        } else if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth &&
                            mouseY >= settingY && mouseY <= settingY + SETTING_SPACING) {
                            
                            if (setting instanceof Module.BooleanSetting) {
                                Module.BooleanSetting boolSetting = (Module.BooleanSetting) setting;
                                boolSetting.setValue(!boolSetting.getValue());
                                return true;
                            } else if (setting instanceof Module.ModeSetting) {
                                Module.ModeSetting modeSetting = (Module.ModeSetting) setting;
                                modeSetting.cycle();
                                return true;
                            }
                            
                            settingY += SETTING_SPACING;
                        } else {
                            settingY += SETTING_SPACING;
                        }
                    }
                }
                
                return false;
            }
            
            float progress = settingsAnimations.getOrDefault(m, 0f);
            int maxSettingsHeight = calculateSettingsHeight(m);
            int settingsHeight = (int)(maxSettingsHeight * progress);
            moduleY += MODULE_HEIGHT + MODULE_SPACING + settingsHeight;
        }
        
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (bindingModule != null) {
            int keyCode = input.key();

            if (keyCode == 256) {
                bindingModule = null;
                Notify.Manager.getInstance().showNotification(
                    "Привязка клавиши отменена",
                    Notify.NotificationType.INFO
                );
                return true;
            }

            ru.input.api.KeyboardKeys key = ru.input.api.KeyboardKeys.findByKeyCode(keyCode);
            if (key == ru.input.api.KeyboardKeys.KEY_NONE || keyCode <= 0) {
                bindingModule = null;
                Notify.Manager.getInstance().showNotification(
                    "Недопустимая клавиша для привязки",
                    Notify.NotificationType.ERROR
                );
                return true;
            }

            String keybindId = "module." + bindingModule.getName().toLowerCase() + ".toggle";
            KeybindManager.getInstance().updateKeybind(keybindId, keyCode);

            moduleKeybinds.put(bindingModule, keyCode);

            String keyName = ru.input.api.KeyboardKeys.getKeyName(keyCode);

            if (keyName == null || keyName.equals("NONE") || keyName.isEmpty()) {
                Logger.INSTANCE.warn("Unknown key code assigned to module " + bindingModule.getName() + ": " + keyCode);
                keyName = "???";
            }
            
            Notify.Manager.getInstance().showNotification(
                "Модуль " + bindingModule.getName() + " привязан к " + keyName,
                Notify.NotificationType.MODULE
            );

            bindingModule = null;
            return true;
        }
        
        if (input.key() == 256) {
            this.onClose();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        double scale = (double) plugin.getMainFramebufferWidth() / this.width;

        int mouseX = (int)(click.x() * scale);
        int mouseY = (int)(click.y() * scale);
        int button = click.button();

        if (button == 0) {
            boolean handledOutsideClick = false;
            for (MultiSetting multiSetting : multiSettingCache.values()) {
                if (multiSetting.isExpanded() && multiSetting.isClickOutside(mouseX, mouseY)) {
                    multiSetting.collapse();
                    handledOutsideClick = true;
                }
            }

            if (handledOutsideClick) {
                return true;
            }

            if (anyComponentDragging) {
                return true;
            }
            
            if (ru.Aporia.handleInterfaceClick(mouseX, mouseY, button)) {
                return true;
            }

            for (CategoryPanel panel : categoryPanels.values()) {
                if (panel.isHeaderHovered(mouseX, mouseY)) {
                    draggingPanel = panel;
                    dragOffsetX = mouseX - panel.getX();
                    dragOffsetY = mouseY - panel.getY();
                    panel.setDragging(true);
                    anyComponentDragging = true;
                    return true;
                }
            }

            for (CategoryPanel panel : categoryPanels.values()) {
                List<Module> modules = ModuleManager.getInstance().getModulesByCategory(panel.getCategory());
                for (Module module : modules) {
                    if (handleSettingClick(module, mouseX, mouseY)) {
                        return true;
                    }
                }
            }

            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(mouseX, mouseY);
                if (module != null) {
                    module.toggle(true);
                    return true;
                }
            }
        } else if (button == 1) {
            if (anyComponentDragging) {
                return true;
            }
            
            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(mouseX, mouseY);
                if (module != null) {
                    if (expandedModules.contains(module)) {
                        expandedModules.remove(module);
                    } else {
                        expandedModules.add(module);
                    }
                    return true;
                }
            }
        } else if (button == 2) {
            if (anyComponentDragging) {
                return true;
            }
            
            for (CategoryPanel panel : categoryPanels.values()) {
                Module module = panel.getHoveredModule(mouseX, mouseY);
                if (module != null) {
                    bindingModule = module;
                    return true;
                }
            }
        }

        return super.mouseClicked(click, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
        ru.Aporia.handleInterfaceRelease();
        
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        double scale = (double) plugin.getMainFramebufferWidth() / this.width;
        int fbMouseX = (int)(mouseButtonEvent.x() * scale);
        int fbMouseY = (int)(mouseButtonEvent.y() * scale);
        
        /**
         * Sync the currently dragging slider to its setting before releasing.
         * This ensures the final value is saved to the module setting.
         */
        if (currentlyDraggingSlider != null) {
            String sliderId = null;
            for (Map.Entry<String, Slider> entry : sliderCache.entrySet()) {
                if (entry.getValue() == currentlyDraggingSlider) {
                    sliderId = entry.getKey();
                    break;
                }
            }
            
            if (sliderId != null) {
                syncSliderToSetting(currentlyDraggingSlider, sliderId);
            }
        }

        for (Slider slider : sliderCache.values()) {
            slider.mouseReleased(fbMouseX, fbMouseY, mouseButtonEvent.button());
        }

        currentlyDraggingSlider = null;
        
        if (mouseButtonEvent.button() == 0 && draggingPanel != null) {
            draggingPanel.setDragging(false);
            draggingPanel = null;
            anyComponentDragging = false;
            savePanelPositions();
            return true;
        }

        anyComponentDragging = false;
        
        return super.mouseReleased(mouseButtonEvent);
    }

    /**
     * Synchronize slider value with module setting.
     * Updates the module setting to match the current slider value.
     * 
     * @param slider The slider to sync
     * @param sliderId The slider identifier in format "ModuleName.SettingName"
     */
    private void syncSliderToSetting(Slider slider, String sliderId) {
        if (sliderId == null) return;
        
        String[] parts = sliderId.split("\\.", 2);
        if (parts.length != 2) return;
        
        String moduleName = parts[0];
        String settingName = parts[1];
        
        Module module = ModuleManager.getInstance().getModuleByName(moduleName);
        if (module == null) return;
        
        for (Module.Setting<?> setting : module.getSettings()) {
            if (setting.getName().equals(settingName) && setting instanceof Module.NumberSetting) {
                ((Module.NumberSetting) setting).setValue((double) slider.getValue());
                break;
            }
        }
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
        MinecraftPlugin plugin = MinecraftPlugin.getInstance();
        double scale = (double) plugin.getMainFramebufferWidth() / this.width;
        int fbMouseX = (int)(mouseButtonEvent.x() * scale);
        int fbMouseY = (int)(mouseButtonEvent.y() * scale);

        ru.Aporia.handleInterfaceDrag(fbMouseX, fbMouseY);
        
        /**
         * Only update the slider that is currently being dragged.
         * Sync value to module setting immediately when value changes.
         */
        if (currentlyDraggingSlider != null) {
            float oldValue = currentlyDraggingSlider.getValue();
            currentlyDraggingSlider.mouseDragged(fbMouseX, fbMouseY);
            
            /**
             * If value changed, update the module setting immediately.
             */
            if (Math.abs(currentlyDraggingSlider.getValue() - oldValue) > 0.01f) {
                /**
                 * Find the slider ID from cache.
                 */
                String sliderId = null;
                for (Map.Entry<String, Slider> entry : sliderCache.entrySet()) {
                    if (entry.getValue() == currentlyDraggingSlider) {
                        sliderId = entry.getKey();
                        break;
                    }
                }
                
                /**
                 * Sync slider value to module setting.
                 */
                if (sliderId != null) {
                    syncSliderToSetting(currentlyDraggingSlider, sliderId);
                }
            }
        }
        
        if (draggingPanel != null && mouseButtonEvent.button() == 0) {
            int scaledMouseX = (int)(mouseButtonEvent.x() * scale);
            int scaledMouseY = (int)(mouseButtonEvent.y() * scale);

            draggingPanel.setPosition(
                    scaledMouseX - dragOffsetX,
                    scaledMouseY - dragOffsetY
            );
            return true;
        }
        return super.mouseDragged(mouseButtonEvent, dragX, dragY);
    }

    @Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics context, int mouseX, int mouseY, float delta) {
    } 

    @Override
    public void onClose() {
        Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
        if (interfaceModule instanceof ru.module.impl.visuals.Interface) {
            ((ru.module.impl.visuals.Interface) interfaceModule).handleMouseRelease();
        }

        ModuleManager.getInstance().saveConfig();
        
        super.onClose();
    }

    public boolean shouldCloseOnEsc() {
        return true;
    }
    
    public boolean shouldPause() {
        return false;
    }
    
    private void savePanelPositions() {
        try {
            ru.files.FilesManager filesManager = ru.Aporia.getFilesManager();
            if (filesManager != null) {
                Map<String, aporia.cc.files.ModuleConfig> configs = new HashMap<>();

                Map<String, aporia.cc.files.ModuleConfig> existingConfigs = filesManager.loadConfig();
                if (existingConfigs != null) {
                    configs.putAll(existingConfigs);
                }

                Map<String, String> guiSettings = new HashMap<>();
                for (CategoryPanel panel : categoryPanels.values()) {
                    String categoryName = panel.getCategory().name();
                    guiSettings.put("Panel." + categoryName + ".X", String.valueOf(panel.getX()));
                    guiSettings.put("Panel." + categoryName + ".Y", String.valueOf(panel.getY()));
                }

                Module clickGuiModule = ModuleManager.getInstance().getModuleByName("ClickGui");
                boolean enabled = clickGuiModule != null && clickGuiModule.isEnabled();
                
                configs.put("ClickGui", new aporia.cc.files.ModuleConfig(enabled, guiSettings));
                
                filesManager.saveConfig(configs);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void loadPanelPositions() {
        try {
            ru.files.FilesManager filesManager = ru.Aporia.getFilesManager();
            if (filesManager != null) {
                Map<String, aporia.cc.files.ModuleConfig> configs = filesManager.loadConfig();
                if (configs != null && configs.containsKey("ClickGui")) {
                    aporia.cc.files.ModuleConfig guiConfig = configs.get("ClickGui");
                    Map<String, String> settings = guiConfig.getSettings();
                    
                    for (CategoryPanel panel : categoryPanels.values()) {
                        String categoryName = panel.getCategory().name();
                        String xKey = "Panel." + categoryName + ".X";
                        String yKey = "Panel." + categoryName + ".Y";
                        
                        if (settings.containsKey(xKey) && settings.containsKey(yKey)) {
                            try {
                                int x = Integer.parseInt(settings.get(xKey));
                                int y = Integer.parseInt(settings.get(yKey));
                                panel.setPosition(x, y);
                            } catch (NumberFormatException e) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    class CategoryPanel {
        private final Module.Category category;
        private int x;
        private int y;
        private final int width;
        private int height;
        private boolean dragging;
        
        public CategoryPanel(Module.Category category, int x, int y, int width, int height) {
            this.category = category;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.dragging = false;
        }
        
        public boolean isHeaderHovered(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + HEADER_HEIGHT;
        }
        
        public Module getHoveredModule(int mouseX, int mouseY) {
            if (!isInBounds(mouseX, mouseY)) return null;
            
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(category);
            int moduleY = y + HEADER_HEIGHT + 5;
            int moduleX = x + 5;
            int moduleWidth = width - 10;
            
            for (Module module : modules) {
                if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth &&
                    mouseY >= moduleY && mouseY <= moduleY + MODULE_HEIGHT) {
                    return module;
                }
                
                float settingsProgress = settingsAnimations.getOrDefault(module, 0f);
                int maxSettingsHeight = calculateSettingsHeight(module);
                int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
                
                moduleY += MODULE_HEIGHT + MODULE_SPACING + settingsHeight;
            }
            
            return null;
        }
        
        public boolean isInBounds(int mouseX, int mouseY) {
            return mouseX >= x && mouseX <= x + width &&
                   mouseY >= y && mouseY <= y + height;
        }
        
        public void setPosition(int x, int y) {
            this.x = x;
            this.y = y;
        }
        
        public Module.Category getCategory() {
            return category;
        }
        
        public int getX() {
            return x;
        }
        
        public int getY() {
            return y;
        }
        
        public int getWidth() {
            return width;
        }
        
        public int getHeight() {
            return height;
        }
        
        public boolean isDragging() {
            return dragging;
        }
        
        public void setDragging(boolean dragging) {
            this.dragging = dragging;
        }
    }
    
    /**
     * Discord avatar display fields
     */
    private net.minecraft.resources.Identifier avatarTexture = null;
    private String cachedUID = null;
    private static final int AVATAR_SIZE = 64;
    private static final int AVATAR_X_OFFSET = 300;
    private static final int AVATAR_Y_OFFSET = 150;
    private static final int TEXT_OFFSET_X = 10;
    private static final int TEXT_LINE_HEIGHT = 24;
    
    /**
     * Render Discord avatar display in bottom-right corner.
     * 
     * @param fbWidth Framebuffer width
     * @param fbHeight Framebuffer height
     */
    private void renderDiscordAvatar(int fbWidth, int fbHeight) {
        ru.help.discord.DiscordManager discordManager = ru.Aporia.getDiscordManager();
        
        if (discordManager == null || !discordManager.isRunning()) {
            return;
        }
        
        if (avatarTexture == null) {
            loadDiscordAvatar();
        }
        
        int x = fbWidth - AVATAR_X_OFFSET;
        int y = fbHeight - AVATAR_Y_OFFSET;
        
        if (textRenderer != null) {
            String username = System.getProperty("user.name", "Unknown");
            String uid = getDiscordUID();
            String version = "1.0.0";
            
            int textX = x + AVATAR_SIZE + TEXT_OFFSET_X;
            textRenderer.drawText(textX, y, 28, "Aporia.cc " + version, RenderColor.WHITE);
            textRenderer.drawText(textX, y + TEXT_LINE_HEIGHT, 24, username, RenderColor.of(170, 170, 170, 255));
            textRenderer.drawText(textX, y + TEXT_LINE_HEIGHT * 2, 24, "UID: " + uid, RenderColor.of(136, 136, 136, 255));
        }
    }
    
    /**
     * Load Discord avatar texture.
     */
    private void loadDiscordAvatar() {
        try {
            ru.help.discord.DiscordManager discordManager = ru.Aporia.getDiscordManager();
            if (discordManager == null) {
                return;
            }
            
            ru.help.discord.DiscordManager.DiscordInfo info = discordManager.getInfo();
            if (info == null || info.avatarUrl().isEmpty()) {
                return;
            }
            
           /* net.minecraft.client.renderer.texture.DynamicTexture texture = ru.files.avatar.BufferUtil.getHeadFromURL(info.avatarUrl());
            if (texture != null) {
                avatarTexture = ru.files.avatar.BufferUtil.registerDynamicTexture("discord-avatar-", texture);
            } */
        } catch (Exception e) {
            avatarTexture = null;
        }
    }
    
    /**
     * Get user UID from Stats.json or UserData.
     * 
     * @return User UID or "Unknown"
     */
    private String getDiscordUID() {
        if (cachedUID != null) {
            return cachedUID;
        }
        
        try {
            Path statsFile = PathResolver.INSTANCE.getCacheDirectory().resolve("Stats.json");
            if (!Files.exists(statsFile)) {
                UserData.UserDataClass userData = UserData.getUserData();
                cachedUID = userData.getUuid();
                return cachedUID;
            }
            
            JsonObject json = JsonParser.parseReader(
                new FileReader(statsFile.toFile())
            ).getAsJsonObject();
            
            JsonElement uuidElem = json.get("uuid");
            JsonElement uidElem = json.get("uid");
            
            cachedUID = uuidElem != null ? uuidElem.getAsString() : 
                       (uidElem != null ? uidElem.getAsString() : "Unknown");
            return cachedUID;
        } catch (Exception e) {
            Logger.INSTANCE.error("Failed to load UID from Stats.json", e);
            UserData.UserDataClass userData = UserData.getUserData();
            cachedUID = userData.getUuid();
            return cachedUID;
        }
    }
}
