package aporia.cc;

import by.saskkeee.annotations.CompileToNative;
import by.saskkeee.annotations.Entrypoint;
import by.saskkeee.annotations.vmprotect.CompileType;
import by.saskkeee.annotations.vmprotect.VMProtect;
import lombok.Getter;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import aporia.cc.base.autobuy.AutoBuyManager;

import aporia.cc.base.comand.CommandManager;
import aporia.cc.base.config.ConfigManager;
import aporia.cc.base.filemanager.impl.FriendManager;
import aporia.cc.base.filemanager.impl.StaffManager;
import aporia.cc.base.modules.ModuleManager;
import aporia.cc.base.request.ScriptManager;
import aporia.cc.base.rotation.RotationManager;
import aporia.cc.base.rotation.deeplearnig.DeepLearningManager;
import aporia.cc.base.theme.ThemeManager;
import aporia.cc.client.screens.autobuy.items.AutoInventoryBuyScreen;
import aporia.cc.client.screens.menu.MenuScreen;
import aporia.cc.client.screens.panels.PanelsScreen;
import aporia.cc.utility.game.server.ServerHandler;
import aporia.cc.base.notify.NotifyManager;
import aporia.cc.base.font.MsdfRenderer;
import aporia.cc.base.repository.RCTRepository;
import aporia.cc.utility.render.display.shader.DrawUtil;
import aporia.cc.utility.render.display.shader.GlProgram;

import java.io.File;

@Getter
@Entrypoint
public enum Aporia {
    INSTANCE;

    public static final String NAME = "Aporia", VER = "2.0", TYPE = "DEV";
    private static final String MOD_ID = NAME.toLowerCase();
    public static final File DIRECTORY = new File(MinecraftClient.getInstance().runDirectory, Aporia.NAME);

    private ModuleManager moduleManager;

    private ThemeManager themeManager;
    private MenuScreen menuScreen;
    private PanelsScreen panelsScreen;
    private ScriptManager scriptManager;
    private AutoInventoryBuyScreen autoInventoryBuyScreen;
    private ServerHandler serverHandler;
    private FriendManager friendManager;
    private StaffManager staffManager;
    private DeepLearningManager deepLearningManager;
    private RotationManager rotationManager;
    private AutoBuyManager autoBuyManager;

    private NotifyManager notifyManager;
    private CommandManager commandManager;
    private ConfigManager configManager;
    private RCTRepository rctRepository;

    @CompileToNative
    @VMProtect(type = CompileType.ULTRA)
    public void init() {

        Runtime.getRuntime().addShutdownHook(new Thread(() -> Aporia.getInstance().shutdown()));


        friendManager = new FriendManager();
        staffManager = new StaffManager();
        notifyManager = new NotifyManager();
        serverHandler = new ServerHandler();
        rctRepository = new RCTRepository();
        themeManager = new ThemeManager();
        moduleManager = new ModuleManager();




        deepLearningManager = new DeepLearningManager();
        rotationManager = new RotationManager();
        autoBuyManager = new AutoBuyManager();
        commandManager = new CommandManager();
        scriptManager = new ScriptManager();
        menuScreen = new MenuScreen();
        panelsScreen = new PanelsScreen();


        configManager = new ConfigManager(); //???? ?????????????? ?????????? ?????????????????? ????????????
        menuScreen.initialize(); //???????????? ????????????????????????

        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Aporia.id("after_shader_load");
            }

            @Override
            public void reload(ResourceManager manager) {
                GlProgram.loadAndSetupPrograms();
            }
        });
        DrawUtil.initializeShaders();
        MsdfRenderer.init();

    }

    public void shutdown() {
        friendManager.save();
        staffManager.save();
        configManager.save();
    }

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static Aporia getInstance() {
        return INSTANCE;
    }

    public RCTRepository getRCTRepository() {
        return rctRepository;
    }

}





