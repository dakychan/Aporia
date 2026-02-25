package cc.apr.ui.clickgui;

import aporia.cc.Logger;
import aporia.cc.files.ModuleConfig;
import aporia.cc.user.UserData;
import aporia.cc.user.UserData.UserDataClass;
import cc.apr.Aporia;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.ferra13671.cometrenderer.plugins.minecraft.RenderColor;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import aporia.cc.files.FilesManager;
import cc.apr.help.discord.DiscordManager;
import cc.apr.input.api.KeyboardKeys;
import cc.apr.input.api.bind.Keybind;
import cc.apr.input.impl.bind.KeybindManager;
import aporia.cc.core.manager.OsManager;
import cc.apr.module.Module;
import cc.apr.module.ModuleManager;
import cc.apr.module.impl.visuals.Interface;
import cc.apr.render.BlurShader;
import cc.apr.render.IconFont;
import cc.apr.render.MsdfFont;
import cc.apr.render.MsdfTextRenderer;
import cc.apr.render.RectRenderer;
import cc.apr.ui.clickgui.comp.MultiSetting;
import cc.apr.ui.clickgui.comp.Slider;
import cc.apr.ui.notify.Notify;
import cc.apr.utils.Lang;

public class ClickGuiScreen extends Screen {
   private static MsdfFont font;
   private static MsdfTextRenderer textRenderer;
   private static boolean initialized = false;
   private boolean textOnlyMode = false;
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
   private final Map<Module.Category, ClickGuiScreen.CategoryPanel> categoryPanels = new HashMap<>();
   private ClickGuiScreen.CategoryPanel draggingPanel = null;
   private int dragOffsetX = 0;
   private int dragOffsetY = 0;
   private static final int CATEGORY_WIDTH = 175;
   private static final int CATEGORY_SPACING = 15;
   private static final int MODULE_HEIGHT = 32;
   private static final int MODULE_SPACING = 8;
   private static final int HEADER_HEIGHT = 38;
   private static final int SETTING_SPACING = 35;
   private static final int SETTINGS_PADDING = 20;
   private static final float ANIMATION_SPEED = 0.15F;
   private static final float DESCRIPTION_Y_OFFSET = 20.0F;
   private static final float DESCRIPTION_DASH_SPACING = 8.0F;
   private BlurShader blurShader;
   private boolean panelsPositioned = false;
   private Identifier avatarTexture = null;
   private String cachedUID = null;
   private static final int AVATAR_SIZE = 64;
   private static final int AVATAR_X_OFFSET = 300;
   private static final int AVATAR_Y_OFFSET = 150;
   private static final int TEXT_OFFSET_X = 10;
   private static final int TEXT_LINE_HEIGHT = 24;

   public void setTextOnlyMode(boolean enabled) {
      this.textOnlyMode = enabled;
   }

   public boolean isTextOnlyMode() {
      return this.textOnlyMode;
   }

   public ClickGuiScreen(int width, int height) {
      super(Component.literal("Click GUI"));
      initFont();
      IconFont.init();
      Lang.load();
      this.initializeCategoryPanels();
      this.loadPanelPositions();
      this.loadModuleKeybinds();
      this.blurShader = new BlurShader();
   }

   private static void initFont() {
      if (!initialized) {
         try {
            font = new MsdfFont("assets/aporia/fonts/Inter_Medium.json", "assets/aporia/fonts/Inter_Medium.png");
            textRenderer = new MsdfTextRenderer(font);
            initialized = true;
         } catch (Exception var1) {
            var1.printStackTrace();
         }
      }
   }

   private void initializeCategoryPanels() {
      Module.Category[] categories = Module.Category.values();
      int startX = 100;
      int startY = 50;
      int categoryX = startX;

      for (Module.Category category : categories) {
         ClickGuiScreen.CategoryPanel panel = new ClickGuiScreen.CategoryPanel(category, categoryX, startY, 175, 385);
         this.categoryPanels.put(category, panel);
         categoryX += 190;
      }
   }

   private void loadModuleKeybinds() {
      this.moduleKeybinds.clear();

      for (Module module : ModuleManager.getInstance().getModules()) {
         String keybindId = "module." + module.getName().toLowerCase() + ".toggle";
         Keybind keybind = KeybindManager.getInstance().getKeybind(keybindId);
         if (keybind != null) {
            int keyCode = keybind.getKeyCode();
            if (keyCode > 0) {
               this.moduleKeybinds.put(module, keyCode);
            }
         }
      }
   }

   private void centerPanels(int fbWidth, int fbHeight) {
      if (!this.panelsPositioned) {
         Module.Category[] categories = Module.Category.values();
         int totalWidth = 175 * categories.length + 15 * (categories.length - 1);
         int panelHeight = 385;
         int startX = (fbWidth - totalWidth) / 2;
         int startY = (fbHeight - panelHeight) / 2;
         int categoryX = startX;

         for (Module.Category category : categories) {
            ClickGuiScreen.CategoryPanel panel = this.categoryPanels.get(category);
            if (panel != null) {
               panel.setPosition(categoryX, startY);
               categoryX += 190;
            }
         }

         this.panelsPositioned = true;
      }
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (initialized) {
         Module clickGuiModule = ModuleManager.getInstance().getModuleByName("ClickGui");
         if (clickGuiModule != null) {
            for (Module.Setting<?> setting : clickGuiModule.getSettings()) {
               if (setting.getName().equals("Text Only") && setting instanceof Module.BooleanSetting) {
                  this.textOnlyMode = ((Module.BooleanSetting)setting).getValue();
                  break;
               }
            }
         }

         MinecraftPlugin plugin = MinecraftPlugin.getInstance();
         plugin.bindMainFramebuffer(true);
         int fbWidth = plugin.getMainFramebufferWidth();
         int fbHeight = plugin.getMainFramebufferHeight();
         double scale = (double)fbWidth / this.width;
         int fbMouseX = (int)(mouseX * scale);
         int fbMouseY = (int)(mouseY * scale);
         this.centerPanels(fbWidth, fbHeight);
         this.hoveredModule = null;
         this.updateSettingsAnimations();

         for (ClickGuiScreen.CategoryPanel panel : this.categoryPanels.values()) {
            this.renderCategory(panel, fbMouseX, fbMouseY);
         }

         if (this.hoveredModule != null) {
            this.renderModuleDescription(fbMouseX, fbMouseY);
         }

         this.renderDiscordAvatar(fbWidth, fbHeight);
      }
   }

   private void renderCategory(ClickGuiScreen.CategoryPanel panel, int mouseX, int mouseY) {
      int x = panel.getX();
      int y = panel.getY();
      int width = panel.getWidth();
      int height = panel.getHeight();
      this.renderRectWithBlur(x, y, width, height, 8.0F, RenderColor.of(20, 20, 25, 230), 3.0F);
      this.renderRectWithBlur(x, y, width, 38.0F, 8.0F, RenderColor.of(30, 30, 38, 255), 3.0F);
      if (IconFont.isInitialized()) {
         MsdfTextRenderer iconRenderer = IconFont.getRenderer();
         String icon = IconFont.getIcon(panel.getCategory());
         if (iconRenderer != null) {
            iconRenderer.drawText(x + 10, y + 21, 18.0F, icon, RenderColor.WHITE);
         }
      }

      if (textRenderer != null) {
         textRenderer.drawText(x + 35, y + 21, 16.0F, panel.getCategory().getDisplayName(), RenderColor.WHITE);
      }

      List<Module> modules = ModuleManager.getInstance().getModulesByCategory(panel.getCategory());
      int moduleY = y + 38 + 5;

      for (Module module : modules) {
         if (moduleY + 32 > y + height - 5) {
            break;
         }

         float settingsProgress = this.settingsAnimations.getOrDefault(module, 0.0F);
         int maxSettingsHeight = this.calculateSettingsHeight(module);
         int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
         this.renderModule(module, x + 5, moduleY, width - 10, 32, mouseX, mouseY);
         if (settingsProgress > 0.01F) {
            this.renderModuleSettings(module, x + 5, moduleY + 32, width - 10, settingsHeight, settingsProgress, mouseX, mouseY);
         }

         moduleY += 40 + settingsHeight;
      }
   }

   private void renderModule(Module module, int x, int y, int width, int height, int mouseX, int mouseY) {
      boolean isExpanded = this.expandedModules.contains(module);
      float settingsProgress = this.settingsAnimations.getOrDefault(module, 0.0F);
      int maxSettingsHeight = this.calculateSettingsHeight(module);
      int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
      boolean isHovered = mouseX >= x
         && mouseX <= x + width
         && mouseY >= y
         && mouseY <= y + height
         && (!isExpanded || !(settingsProgress > 0.01F) || mouseY <= y + height);
      boolean isEnabled = module.isEnabled();
      if (isHovered) {
         this.hoveredModule = module;
      }

      RenderColor bgColor;
      RenderColor textColor;
      if (this.textOnlyMode) {
         bgColor = isHovered ? RenderColor.of(60, 60, 70, 220) : RenderColor.of(50, 50, 60, 200);
         textColor = isEnabled ? RenderColor.of(80, 200, 120, 255) : RenderColor.of(150, 150, 160, 255);
      } else if (isEnabled) {
         bgColor = isHovered ? RenderColor.of(70, 130, 255, 255) : RenderColor.of(60, 120, 245, 230);
         textColor = RenderColor.WHITE;
      } else {
         bgColor = isHovered ? RenderColor.of(60, 60, 70, 220) : RenderColor.of(50, 50, 60, 200);
         textColor = RenderColor.of(180, 180, 190, 255);
      }

      float radius = 5.0F;
      this.renderRectWithBlur(x, y, width, height, radius, bgColor, 2.0F);
      if (textRenderer != null) {
         String displayText = module == this.bindingModule ? "Биндим на..." : module.getName();
         textRenderer.drawText(x + 8, y + 20, 15.0F, displayText, textColor);
         if (this.moduleKeybinds.containsKey(module)) {
            int keyCode = this.moduleKeybinds.get(module);
            String keyName = KeyboardKeys.getKeyName(keyCode);
            if (keyName == null || keyName.equals("NONE") || keyName.isEmpty()) {
               Logger.INSTANCE.warn("Unknown key code for module " + module.getName() + ": " + keyCode);
               keyName = "???";
            }

            float keyNameWidth = textRenderer.measureWidth(keyName, 13.0F);
            float keyNameX = x + width - keyNameWidth - 8.0F;
            RenderColor keyColor = RenderColor.of(150, 150, 160, 255);
            textRenderer.drawText(keyNameX, y + 20, 13.0F, keyName, keyColor);
         }
      }
   }

   private void renderModuleSettings(Module module, int x, int y, int width, int height, float alpha, int fbMouseX, int fbMouseY) {
      if (height > 0) {
         int alphaValue = (int)(200.0F * alpha);
         RenderColor bgColor = RenderColor.of(35, 35, 45, alphaValue);
         this.renderRectWithBlur(x, y, width, height, 5.0F, bgColor, 2.0F);
         if (!(alpha < 0.3F)) {
            List<Module.Setting<?>> settings = module.getSettings();
            if (settings.isEmpty()) {
               if (textRenderer != null) {
                  textRenderer.drawText(x + 10, y + 15, 13.0F, "Нет настроек", RenderColor.of(150, 150, 160, (int)(255.0F * alpha)));
               }
            } else {
               int settingY = y + 10;
               int maxY = y + height;

               for (Module.Setting<?> setting : settings) {
                  if (settingY > maxY - 25) {
                     break;
                  }

                  if (setting instanceof Module.NumberSetting numSetting) {
                     String sliderId = module.getName() + "." + setting.getName();
                     Slider slider = this.sliderCache
                        .computeIfAbsent(
                           sliderId,
                           k -> new Slider(setting.getName(), numSetting.getValue().floatValue(), (float)numSetting.getMin(), (float)numSetting.getMax())
                        );
                     if (!slider.isDragging()) {
                        slider.setValue(numSetting.getValue().floatValue());
                     }

                     slider.render(x + 10, settingY, width - 20, textRenderer, fbMouseX, fbMouseY);
                     settingY += 35;
                  } else {
                     if (textRenderer != null) {
                        textRenderer.drawText(x + 10, settingY + 8, 12.0F, setting.getName(), RenderColor.of(200, 200, 210, (int)(255.0F * alpha)));
                     }

                     if (setting instanceof Module.BooleanSetting boolSetting) {
                        String value = boolSetting.getValue() ? "Да" : "Нет";
                        RenderColor valueColor = boolSetting.getValue()
                           ? RenderColor.of(80, 200, 120, (int)(255.0F * alpha))
                           : RenderColor.of(180, 180, 190, (int)(255.0F * alpha));
                        if (textRenderer != null) {
                           float valueWidth = textRenderer.measureWidth(value, 12.0F);
                           textRenderer.drawText(x + width - valueWidth - 10.0F, settingY + 8, 12.0F, value, valueColor);
                        }
                     } else if (setting instanceof Module.ModeSetting modeSetting) {
                        String value = modeSetting.getValue();
                        if (textRenderer != null) {
                           float valueWidth = textRenderer.measureWidth(value, 12.0F);
                           textRenderer.drawText(
                              x + width - valueWidth - 10.0F, settingY + 8, 12.0F, value, RenderColor.of(100, 150, 255, (int)(255.0F * alpha))
                           );
                        }
                     } else if (setting instanceof Interface.MultiSetting multiSettingData) {
                        String cacheKey = module.getName() + "_" + setting.getName();
                        MultiSetting multiSetting = this.multiSettingCache.get(cacheKey);
                        if (multiSetting == null) {
                           multiSetting = new MultiSetting(setting.getName(), multiSettingData.getOptions(), multiSettingData.getValue());
                           this.multiSettingCache.put(cacheKey, multiSetting);
                        }

                        int multiSettingHeight = multiSetting.getHeight();
                        if (settingY + multiSettingHeight <= maxY) {
                           multiSetting.render(x + 10, settingY, width - 20, textRenderer, fbMouseX, fbMouseY, alpha);
                        }

                        settingY += multiSettingHeight;
                     } else if (setting instanceof Module.StringSetting strSetting) {
                        String value = strSetting.getValue();
                        if (value.length() > 15) {
                           value = value.substring(0, 12) + "...";
                        }

                        if (textRenderer != null) {
                           float valueWidth = textRenderer.measureWidth(value, 12.0F);
                           textRenderer.drawText(
                              x + width - valueWidth - 10.0F, settingY + 8, 12.0F, value, RenderColor.of(180, 180, 190, (int)(255.0F * alpha))
                           );
                        }
                     }

                     settingY += 35;
                  }
               }
            }
         }
      }
   }

   private int calculateSettingsHeight(Module module) {
      List<Module.Setting<?>> settings = module.getSettings();
      if (settings.isEmpty()) {
         return 40;
      } else {
         int totalHeight = 20;

         for (Module.Setting<?> setting : settings) {
            if (setting instanceof Interface.MultiSetting multiSettingData) {
               String cacheKey = module.getName() + "_" + setting.getName();
               MultiSetting multiSetting = this.multiSettingCache.get(cacheKey);
               if (multiSetting == null) {
                  multiSetting = new MultiSetting(setting.getName(), multiSettingData.getOptions(), multiSettingData.getValue());
                  this.multiSettingCache.put(cacheKey, multiSetting);
               }

               totalHeight += multiSetting.getHeight();
            } else {
               totalHeight += 35;
            }
         }

         return totalHeight;
      }
   }

   private void updateSettingsAnimations() {
      for (Module module : ModuleManager.getInstance().getModules()) {
         float current = this.settingsAnimations.getOrDefault(module, 0.0F);
         float target = this.expandedModules.contains(module) ? 1.0F : 0.0F;
         if (Math.abs(current - target) > 0.01F) {
            float newValue = current + (target - current) * 0.15F;
            this.settingsAnimations.put(module, newValue);
         } else {
            this.settingsAnimations.put(module, target);
         }
      }
   }

   private void renderModuleDescription(int mouseX, int mouseY) {
      if (textRenderer != null && this.hoveredModule != null) {
         String moduleName = this.hoveredModule.getName();
         String description = Lang.getModuleDescription(moduleName);
         if (description.equals("module." + moduleName.toLowerCase() + ".description")) {
            description = this.hoveredModule.getDescription();
         }

         if (description != null && !description.isEmpty()) {
            String fullText = moduleName + " - " + description;
            MinecraftPlugin plugin = MinecraftPlugin.getInstance();
            int fbWidth = plugin.getMainFramebufferWidth();
            float nameWidth = textRenderer.measureWidth(moduleName, 30.0F);
            float dashWidth = textRenderer.measureWidth(" - ", 26.0F);
            float descWidth = textRenderer.measureWidth(description, 26.0F);
            float totalWidth = nameWidth + dashWidth + descWidth;
            int tooltipX = (int)((fbWidth - totalWidth) / 2.0F);
            int tooltipY = 60;
            textRenderer.drawText(tooltipX, tooltipY, 30.0F, moduleName, RenderColor.WHITE);
            float offsetX = tooltipX + nameWidth + 8.0F;
            textRenderer.drawText(offsetX, tooltipY + 4, 26.0F, " - " + description, RenderColor.of(200, 200, 210, 255));
         }
      }
   }

   private void renderRect(float x, float y, float w, float h, float radius, RenderColor color) {
      RectRenderer.drawRoundedRect(x, y, w, h, radius, color);
   }

   private void renderRectWithBlur(float x, float y, float w, float h, float radius, RenderColor color, float blurAmount) {
      RectRenderer.drawRectangleWithBlur(x, y, w, h, color, radius, blurAmount);
   }

   private boolean handleSettingClick(Module module, int mouseX, int mouseY) {
      if (!this.expandedModules.contains(module)) {
         return false;
      } else {
         float settingsProgress = this.settingsAnimations.getOrDefault(module, 0.0F);
         if (settingsProgress < 0.3F) {
            return false;
         } else {
            ClickGuiScreen.CategoryPanel panel = null;

            for (ClickGuiScreen.CategoryPanel p : this.categoryPanels.values()) {
               if (p.getCategory() == module.getCategory()) {
                  panel = p;
                  break;
               }
            }

            if (panel == null) {
               return false;
            } else {
               List<Module> modules = ModuleManager.getInstance().getModulesByCategory(module.getCategory());
               int moduleY = panel.getY() + 38 + 5;
               int moduleX = panel.getX() + 5;
               int moduleWidth = panel.getWidth() - 10;

               for (Module m : modules) {
                  if (m == module) {
                     int settingsY = moduleY + 32;
                     int maxSettingsHeight = this.calculateSettingsHeight(module);
                     int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
                     if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth && mouseY >= settingsY && mouseY <= settingsY + settingsHeight) {
                        List<Module.Setting<?>> settings = module.getSettings();
                        int settingY = settingsY + 10;

                        for (Module.Setting<?> setting : settings) {
                           if (settingY > settingsY + settingsHeight - 25) {
                              break;
                           }

                           if (setting instanceof Module.NumberSetting) {
                              String sliderId = module.getName() + "." + setting.getName();
                              Slider slider = this.sliderCache.get(sliderId);
                              if (slider != null && slider.mouseClicked(mouseX, mouseY, 0)) {
                                 this.anyComponentDragging = true;
                                 this.currentlyDraggingSlider = slider;
                                 return true;
                              }

                              settingY += 35;
                           } else if (setting instanceof Interface.MultiSetting multiSettingData) {
                              String cacheKey = module.getName() + "_" + setting.getName();
                              MultiSetting multiSetting = this.multiSettingCache.get(cacheKey);
                              if (multiSetting == null) {
                                 multiSetting = new MultiSetting(setting.getName(), multiSettingData.getOptions(), multiSettingData.getValue());
                                 this.multiSettingCache.put(cacheKey, multiSetting);
                              }

                              if (multiSetting.mouseClicked(mouseX, mouseY, 0)) {
                                 return true;
                              }

                              settingY += multiSetting.getHeight();
                           } else if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth && mouseY >= settingY && mouseY <= settingY + 35) {
                              if (setting instanceof Module.BooleanSetting boolSetting) {
                                 boolSetting.setValue(!boolSetting.getValue());
                                 return true;
                              }

                              if (setting instanceof Module.ModeSetting modeSetting) {
                                 modeSetting.cycle();
                                 return true;
                              }

                              settingY += 35;
                           } else {
                              settingY += 35;
                           }
                        }
                     }

                     return false;
                  }

                  float progress = this.settingsAnimations.getOrDefault(m, 0.0F);
                  int maxSettingsHeight = this.calculateSettingsHeight(m);
                  int settingsHeight = (int)(maxSettingsHeight * progress);
                  moduleY += 40 + settingsHeight;
               }

               return false;
            }
         }
      }
   }

   public boolean keyPressed(KeyEvent input) {
      if (this.bindingModule != null) {
         int keyCode = input.key();
         if (keyCode == 256) {
            this.bindingModule = null;
            Notify.Manager.getInstance().showNotification("Привязка клавиши отменена", Notify.NotificationType.INFO);
            return true;
         } else {
            KeyboardKeys key = KeyboardKeys.findByKeyCode(keyCode);
            if (key != KeyboardKeys.KEY_NONE && keyCode > 0) {
               String keybindId = "module." + this.bindingModule.getName().toLowerCase() + ".toggle";
               KeybindManager.getInstance().updateKeybind(keybindId, keyCode);
               this.moduleKeybinds.put(this.bindingModule, keyCode);
               String keyName = KeyboardKeys.getKeyName(keyCode);
               if (keyName == null || keyName.equals("NONE") || keyName.isEmpty()) {
                  Logger.INSTANCE.warn("Unknown key code assigned to module " + this.bindingModule.getName() + ": " + keyCode);
                  keyName = "???";
               }

               Notify.Manager.getInstance()
                  .showNotification("Модуль " + this.bindingModule.getName() + " привязан к " + keyName, Notify.NotificationType.MODULE);
               this.bindingModule = null;
               return true;
            } else {
               this.bindingModule = null;
               Notify.Manager.getInstance().showNotification("Недопустимая клавиша для привязки", Notify.NotificationType.ERROR);
               return true;
            }
         }
      } else if (input.key() == 256) {
         this.onClose();
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseClicked(MouseButtonEvent click, boolean bl) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      double scale = (double)plugin.getMainFramebufferWidth() / this.width;
      int mouseX = (int)(click.x() * scale);
      int mouseY = (int)(click.y() * scale);
      int button = click.button();
      if (button == 0) {
         boolean handledOutsideClick = false;

         for (MultiSetting multiSetting : this.multiSettingCache.values()) {
            if (multiSetting.isExpanded() && multiSetting.isClickOutside(mouseX, mouseY)) {
               multiSetting.collapse();
               handledOutsideClick = true;
            }
         }

         if (handledOutsideClick) {
            return true;
         }

         if (this.anyComponentDragging) {
            return true;
         }

         if (cc.apr.Aporia.handleInterfaceClick(mouseX, mouseY, button)) {
            return true;
         }

         for (ClickGuiScreen.CategoryPanel panel : this.categoryPanels.values()) {
            if (panel.isHeaderHovered(mouseX, mouseY)) {
               this.draggingPanel = panel;
               this.dragOffsetX = mouseX - panel.getX();
               this.dragOffsetY = mouseY - panel.getY();
               panel.setDragging(true);
               this.anyComponentDragging = true;
               return true;
            }
         }

         for (ClickGuiScreen.CategoryPanel panelx : this.categoryPanels.values()) {
            for (Module module : ModuleManager.getInstance().getModulesByCategory(panelx.getCategory())) {
               if (this.handleSettingClick(module, mouseX, mouseY)) {
                  return true;
               }
            }
         }

         for (ClickGuiScreen.CategoryPanel panelx : this.categoryPanels.values()) {
            Module modulex = panelx.getHoveredModule(mouseX, mouseY);
            if (modulex != null) {
               modulex.toggle(true);
               return true;
            }
         }
      } else if (button == 1) {
         if (this.anyComponentDragging) {
            return true;
         }

         for (ClickGuiScreen.CategoryPanel panelxx : this.categoryPanels.values()) {
            Module modulex = panelxx.getHoveredModule(mouseX, mouseY);
            if (modulex != null) {
               if (this.expandedModules.contains(modulex)) {
                  this.expandedModules.remove(modulex);
               } else {
                  this.expandedModules.add(modulex);
               }

               return true;
            }
         }
      } else if (button == 2) {
         if (this.anyComponentDragging) {
            return true;
         }

         for (ClickGuiScreen.CategoryPanel panelxxx : this.categoryPanels.values()) {
            Module modulex = panelxxx.getHoveredModule(mouseX, mouseY);
            if (modulex != null) {
               this.bindingModule = modulex;
               return true;
            }
         }
      }

      return super.mouseClicked(click, bl);
   }

   public boolean mouseReleased(MouseButtonEvent mouseButtonEvent) {
      cc.apr.Aporia.handleInterfaceRelease();
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      double scale = (double)plugin.getMainFramebufferWidth() / this.width;
      int fbMouseX = (int)(mouseButtonEvent.x() * scale);
      int fbMouseY = (int)(mouseButtonEvent.y() * scale);
      if (this.currentlyDraggingSlider != null) {
         String sliderId = null;

         for (Entry<String, Slider> entry : this.sliderCache.entrySet()) {
            if (entry.getValue() == this.currentlyDraggingSlider) {
               sliderId = entry.getKey();
               break;
            }
         }

         if (sliderId != null) {
            this.syncSliderToSetting(this.currentlyDraggingSlider, sliderId);
         }
      }

      for (Slider slider : this.sliderCache.values()) {
         slider.mouseReleased(fbMouseX, fbMouseY, mouseButtonEvent.button());
      }

      this.currentlyDraggingSlider = null;
      if (mouseButtonEvent.button() == 0 && this.draggingPanel != null) {
         this.draggingPanel.setDragging(false);
         this.draggingPanel = null;
         this.anyComponentDragging = false;
         this.savePanelPositions();
         return true;
      } else {
         this.anyComponentDragging = false;
         return super.mouseReleased(mouseButtonEvent);
      }
   }

   private void syncSliderToSetting(Slider slider, String sliderId) {
      if (sliderId != null) {
         String[] parts = sliderId.split("\\.", 2);
         if (parts.length == 2) {
            String moduleName = parts[0];
            String settingName = parts[1];
            Module module = ModuleManager.getInstance().getModuleByName(moduleName);
            if (module != null) {
               for (Module.Setting<?> setting : module.getSettings()) {
                  if (setting.getName().equals(settingName) && setting instanceof Module.NumberSetting) {
                     ((Module.NumberSetting)setting).setValue((double)slider.getValue());
                     break;
                  }
               }
            }
         }
      }
   }

   public boolean mouseDragged(MouseButtonEvent mouseButtonEvent, double dragX, double dragY) {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      double scale = (double)plugin.getMainFramebufferWidth() / this.width;
      int fbMouseX = (int)(mouseButtonEvent.x() * scale);
      int fbMouseY = (int)(mouseButtonEvent.y() * scale);
      cc.apr.Aporia.handleInterfaceDrag(fbMouseX, fbMouseY);
      if (this.currentlyDraggingSlider != null) {
         float oldValue = this.currentlyDraggingSlider.getValue();
         this.currentlyDraggingSlider.mouseDragged(fbMouseX, fbMouseY);
         if (Math.abs(this.currentlyDraggingSlider.getValue() - oldValue) > 0.01F) {
            String sliderId = null;

            for (Entry<String, Slider> entry : this.sliderCache.entrySet()) {
               if (entry.getValue() == this.currentlyDraggingSlider) {
                  sliderId = entry.getKey();
                  break;
               }
            }

            if (sliderId != null) {
               this.syncSliderToSetting(this.currentlyDraggingSlider, sliderId);
            }
         }
      }

      if (this.draggingPanel != null && mouseButtonEvent.button() == 0) {
         int scaledMouseX = (int)(mouseButtonEvent.x() * scale);
         int scaledMouseY = (int)(mouseButtonEvent.y() * scale);
         this.draggingPanel.setPosition(scaledMouseX - this.dragOffsetX, scaledMouseY - this.dragOffsetY);
         return true;
      } else {
         return super.mouseDragged(mouseButtonEvent, dragX, dragY);
      }
   }

   public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
   }

   public void onClose() {
      Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
      if (interfaceModule instanceof Interface) {
         ((Interface)interfaceModule).handleMouseRelease();
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
         FilesManager filesManager = Aporia.getFilesManager();
         if (filesManager != null) {
            Map<String, ModuleConfig> configs = new HashMap<>();
            Map<String, ModuleConfig> existingConfigs = filesManager.loadConfig();
            if (existingConfigs != null) {
               configs.putAll(existingConfigs);
            }

            Map<String, String> guiSettings = new HashMap<>();

            for (ClickGuiScreen.CategoryPanel panel : this.categoryPanels.values()) {
               String categoryName = panel.getCategory().name();
               guiSettings.put("Panel." + categoryName + ".X", String.valueOf(panel.getX()));
               guiSettings.put("Panel." + categoryName + ".Y", String.valueOf(panel.getY()));
            }

            Module clickGuiModule = ModuleManager.getInstance().getModuleByName("ClickGui");
            boolean enabled = clickGuiModule != null && clickGuiModule.isEnabled();
            configs.put("ClickGui", new ModuleConfig(enabled, guiSettings));
            filesManager.saveConfig(configs);
         }
      } catch (Exception var8) {
         var8.printStackTrace();
      }
   }

   private void loadPanelPositions() {
      try {
         FilesManager filesManager = cc.apr.Aporia.getFilesManager();
         if (filesManager != null) {
            Map<String, ModuleConfig> configs = filesManager.loadConfig();
            if (configs != null && configs.containsKey("ClickGui")) {
               ModuleConfig guiConfig = configs.get("ClickGui");
               Map<String, String> settings = guiConfig.getSettings();

               for (ClickGuiScreen.CategoryPanel panel : this.categoryPanels.values()) {
                  String categoryName = panel.getCategory().name();
                  String xKey = "Panel." + categoryName + ".X";
                  String yKey = "Panel." + categoryName + ".Y";
                  if (settings.containsKey(xKey) && settings.containsKey(yKey)) {
                     try {
                        int x = Integer.parseInt(settings.get(xKey));
                        int y = Integer.parseInt(settings.get(yKey));
                        panel.setPosition(x, y);
                     } catch (NumberFormatException var12) {
                     }
                  }
               }
            }
         }
      } catch (Exception var13) {
         var13.printStackTrace();
      }
   }

   private void renderDiscordAvatar(int fbWidth, int fbHeight) {
      DiscordManager discordManager = cc.apr.Aporia.getDiscordManager();
      if (discordManager != null && discordManager.isRunning()) {
         if (this.avatarTexture == null) {
            this.loadDiscordAvatar();
         }

         int x = fbWidth - 300;
         int y = fbHeight - 150;
         if (textRenderer != null) {
            String username = System.getProperty("user.name", "Unknown");
            String uid = this.getDiscordUID();
            String version = "1.0.0";
            int textX = x + 64 + 10;
            textRenderer.drawText(textX, y, 28.0F, "Aporia.cc " + version, RenderColor.WHITE);
            textRenderer.drawText(textX, y + 24, 24.0F, username, RenderColor.of(170, 170, 170, 255));
            textRenderer.drawText(textX, y + 48, 24.0F, "UID: " + uid, RenderColor.of(136, 136, 136, 255));
         }
      }
   }

   private void loadDiscordAvatar() {
      try {
         DiscordManager discordManager = cc.apr.Aporia.getDiscordManager();
         if (discordManager == null) {
            return;
         }

         DiscordManager.DiscordInfo info = discordManager.getInfo();
         if (info == null || info.avatarUrl().isEmpty()) {
            return;
         }
      } catch (Exception var3) {
         this.avatarTexture = null;
      }
   }

   private String getDiscordUID() {
      if (this.cachedUID != null) {
         return this.cachedUID;
      } else {
         try {
            Path statsFile = OsManager.INSTANCE.getCacheDirectory().resolve("Stats.json");
            if (!Files.exists(statsFile)) {
               UserDataClass userData = UserData.getUserData();
               this.cachedUID = userData.getUuid();
               return this.cachedUID;
            } else {
               JsonObject json = JsonParser.parseReader(new FileReader(statsFile.toFile())).getAsJsonObject();
               JsonElement uuidElem = json.get("uuid");
               JsonElement uidElem = json.get("uid");
               this.cachedUID = uuidElem != null ? uuidElem.getAsString() : (uidElem != null ? uidElem.getAsString() : "Unknown");
               return this.cachedUID;
            }
         } catch (Exception var5) {
            Logger.INSTANCE.error("Failed to load UID from Stats.json", var5);
            UserDataClass userData = UserData.getUserData();
            this.cachedUID = userData.getUuid();
            return this.cachedUID;
         }
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
         return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + 38;
      }

      public Module getHoveredModule(int mouseX, int mouseY) {
         if (!this.isInBounds(mouseX, mouseY)) {
            return null;
         } else {
            List<Module> modules = ModuleManager.getInstance().getModulesByCategory(this.category);
            int moduleY = this.y + 38 + 5;
            int moduleX = this.x + 5;
            int moduleWidth = this.width - 10;

            for (Module module : modules) {
               if (mouseX >= moduleX && mouseX <= moduleX + moduleWidth && mouseY >= moduleY && mouseY <= moduleY + 32) {
                  return module;
               }

               float settingsProgress = ClickGuiScreen.this.settingsAnimations.getOrDefault(module, 0.0F);
               int maxSettingsHeight = ClickGuiScreen.this.calculateSettingsHeight(module);
               int settingsHeight = (int)(maxSettingsHeight * settingsProgress);
               moduleY += 40 + settingsHeight;
            }

            return null;
         }
      }

      public boolean isInBounds(int mouseX, int mouseY) {
         return mouseX >= this.x && mouseX <= this.x + this.width && mouseY >= this.y && mouseY <= this.y + this.height;
      }

      public void setPosition(int x, int y) {
         this.x = x;
         this.y = y;
      }

      public Module.Category getCategory() {
         return this.category;
      }

      public int getX() {
         return this.x;
      }

      public int getY() {
         return this.y;
      }

      public int getWidth() {
         return this.width;
      }

      public int getHeight() {
         return this.height;
      }

      public boolean isDragging() {
         return this.dragging;
      }

      public void setDragging(boolean dragging) {
         this.dragging = dragging;
      }
   }
}
