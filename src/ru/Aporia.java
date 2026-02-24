package ru;

import aporia.cc.Logger;
import aporia.cc.chat.ChatUtils;
import aporia.cc.user.UserData;
import aporia.cc.user.UserData.UserDataClass;
import com.ferra13671.cometrenderer.CometRenderer;
import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin;
import com.mojang.blaze3d.opengl.GlTexture;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuTexture;
import ru.files.FilesManager;
import ru.gui.GuiManager;
import ru.help.discord.DiscordManager;
import ru.input.api.KeyBindings;
import ru.input.impl.UnifiedInputHandler;
import ru.input.impl.bind.KeybindListener;
import ru.input.impl.bind.KeybindManager;
import ru.manager.OsManager;
import ru.module.Module;
import ru.module.ModuleManager;
import ru.module.impl.visuals.BlurTest;
import ru.module.impl.visuals.Interface;
import ru.render.BlurShader;
import ru.render.RectRenderer;
import ru.ui.hud.HudManager;
import ru.ui.notify.Notify;

public class Aporia {
   private static FilesManager filesManager;
   private static BlurShader blurShader;
   private static DiscordManager discordManager;

   public static DiscordManager getDiscordManager() {
      return discordManager;
   }

   public static void initRender() {
      CometRenderer.init();
      MinecraftPlugin.init(glGpuBuffer -> glGpuBuffer.getHandle(), () -> 1);
   }

   public static void onInit() {
      initFileSystem();
      initOsManager();
      UnifiedInputHandler.init();
      KeybindManager.getInstance().loadKeybinds();
      KeybindListener.init();
      Notify.Manager.getInstance();
      KeyBindings.register();
      setupKeyBindings();
      initUserData();
      initRenderingSystems();
      initCommandSystem();
      initDiscordManager();
   }

   private static void initOsManager() {
      OsManager.INSTANCE.initLogger();
   }

   private static void initRenderingSystems() {
      blurShader = new BlurShader();
      RectRenderer.init();
      RectRenderer.setSharedBlurShader(blurShader);
      HudManager.INSTANCE.initialize();
   }

   private static void initCommandSystem() {
      ChatUtils.INSTANCE.initialize();
   }

   private static void initDiscordManager() {
      discordManager = new DiscordManager();
   }

   private static void initFileSystem() {
      Logger.INSTANCE.initialize();
      filesManager = new FilesManager();
      filesManager.initialize();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
         try {
            UserDataClass userData = UserData.getUserData();
            filesManager.saveStats(userData);
            if (blurShader != null) {
               blurShader.cleanup();
            }
         } catch (Exception var1) {
            Logger.INSTANCE.error("Failed to save stats on shutdown: " + var1.getMessage(), var1);
         }
      }));
   }

   public static FilesManager getFilesManager() {
      return filesManager;
   }

   private static void setupKeyBindings() {
   }

   public static void render() {
      GuiManager.render();
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      if (plugin != null) {
         int width = plugin.getMainFramebufferWidth();
         int height = plugin.getMainFramebufferHeight();
         Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
         boolean interfaceEnabled = interfaceModule != null && interfaceModule.isEnabled();
         if (interfaceEnabled && blurShader != null) {
            blurShader.renderToFramebuffer(width, height, () -> {
               if (interfaceModule instanceof Interface) {
                  ((Interface)interfaceModule).render();
               }
            });
         } else if (interfaceEnabled && interfaceModule instanceof Interface) {
            ((Interface)interfaceModule).render();
         }
      }
   }

   public static void renderBlur() {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      if (plugin != null) {
         int width = plugin.getMainFramebufferWidth();
         int height = plugin.getMainFramebufferHeight();
         Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
         if (blurTestModule != null && blurTestModule.isEnabled() && blurTestModule instanceof BlurTest) {
            ((BlurTest)blurTestModule).onRender2D();
         }
      }
   }

   public static void captureScreenForBlur() {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      if (plugin != null) {
         int width = plugin.getMainFramebufferWidth();
         int height = plugin.getMainFramebufferHeight();
         Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
         if (blurTestModule != null && blurTestModule.isEnabled()) {
            RectRenderer.beginBlurBatch(width, height);
         }
      }
   }

   public static void captureScreenForNextFrame() {
      MinecraftPlugin plugin = MinecraftPlugin.getInstance();
      if (plugin != null) {
         int width = plugin.getMainFramebufferWidth();
         int height = plugin.getMainFramebufferHeight();
         Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
         if (blurTestModule != null && blurTestModule.isEnabled()) {
            RectRenderer.captureForNextFrame(width, height);
         }
      }
   }

   public static void captureScreenInFramePass(int width, int height) {
      Module blurTestModule = ModuleManager.getInstance().getModuleByName("BlurTest");
      if (blurTestModule != null && blurTestModule.isEnabled()) {
         System.out.println("[APORIA] Capturing screen in FramePass - width=" + width + ", height=" + height);
         RectRenderer.captureForNextFrame(width, height);
      }
   }

   public static void captureWorldOnlyTarget(RenderTarget worldTarget) {
      int width = worldTarget.width;
      int height = worldTarget.height;
      GpuTexture gpuTexture = worldTarget.getColorTexture();
      if (gpuTexture instanceof GlTexture) {
         int textureId = ((GlTexture)gpuTexture).glId();
         RectRenderer.setWorldOnlyTexture(textureId, width, height);
      }
   }

   public static boolean handleInterfaceClick(int mouseX, int mouseY, int button) {
      Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
      return interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface
         ? ((Interface)interfaceModule).handleMouseClick(mouseX, mouseY, button)
         : false;
   }

   public static void handleInterfaceDrag(int mouseX, int mouseY) {
      Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
      if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface) {
         ((Interface)interfaceModule).handleMouseDrag(mouseX, mouseY);
      }
   }

   public static void handleInterfaceRelease() {
      Module interfaceModule = ModuleManager.getInstance().getModuleByName("Interface");
      if (interfaceModule != null && interfaceModule.isEnabled() && interfaceModule instanceof Interface) {
         ((Interface)interfaceModule).handleMouseRelease();
      }
   }

   public static void initUserData() {
      UserDataClass userData = UserData.getUserData();
   }
}
