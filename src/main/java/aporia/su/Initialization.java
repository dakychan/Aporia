package aporia.su;

import aporia.su.util.files.FilesManager;
import aporia.su.util.user.render.screens.hud.api.HudManager;
import aporia.su.util.user.chat.command.CommandManager;
import aporia.su.util.events.api.EventManager;
import aporia.su.modules.impl.combat.aura.attack.StrikerConstructor;
import aporia.su.modules.module.ModuleRepository;
import aporia.su.util.user.render.screens.clickgui.ClickGui;
import aporia.su.util.files.impl.BindConfig;
import aporia.su.util.files.impl.BlockESPConfig;
import aporia.su.util.files.impl.DragConfig;
import aporia.su.util.files.impl.FriendConfig;
import aporia.su.util.files.impl.PrefixConfig;
import aporia.su.util.files.impl.ProxyConfig;
import aporia.su.util.files.impl.StaffConfig;
import aporia.su.modules.wtf.ModuleProvider;
import aporia.su.modules.wtf.ModuleSwitcher;
import aporia.su.util.user.render.font.FontInitializer;
import aporia.su.util.user.render.shader.RenderCore;
import aporia.su.util.user.render.shader.Scissor;
import aporia.su.util.user.repository.macro.MacroRepository;
import aporia.su.util.user.repository.way.WayRepository;
import aporia.su.util.user.player.tps.TPSCalculate;
import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;

public class Initialization implements ClientModInitializer {

    @Getter
    private static Initialization instance;

    @Getter
    private Manager manager;

    @Override
    public void onInitializeClient() {

    }

    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
    }

    @Getter
    public static class Manager {
        public StrikerConstructor attackPerpetrator = new StrikerConstructor();
        private EventManager eventManager;
        private RenderCore renderCore;
        private Scissor scissor;
        private ModuleProvider moduleProvider;
        private ModuleRepository moduleRepository;
        private ModuleSwitcher moduleSwitcher;
        private ClickGui clickgui;
        private FilesManager.ConfigManager configManager;
        private CommandManager commandManager;
        private TPSCalculate tpsCalculate;
        private HudManager hudManager = new HudManager();

        public void init() {
            /** Инициализируем файловую систему */
            FilesManager.initialize();
            
            /** Загружаем конфиги */
            MacroRepository.getInstance().init();
            WayRepository.getInstance().init();
            BlockESPConfig.getInstance().load();
            FriendConfig.getInstance().load();
            PrefixConfig.getInstance().load();
            StaffConfig.getInstance().load();
            ProxyConfig.getInstance().load();
            DragConfig.getInstance().load();
            BindConfig.getInstance();
            FontInitializer.register();

            tpsCalculate = new TPSCalculate();
            clickgui = new ClickGui();
            eventManager = new EventManager();
            renderCore = new RenderCore();
            scissor = new Scissor();
            hudManager = new HudManager();
            hudManager.initElements();
            moduleRepository = new ModuleRepository();
            moduleRepository.setup();
            moduleProvider = new ModuleProvider(moduleRepository.modules());
            moduleSwitcher = new ModuleSwitcher(moduleRepository.modules(), eventManager);
            
            /** Инициализируем встроенный конфиг менеджер */
            configManager = FilesManager.getConfigManager();
            configManager.initialize(moduleRepository);
            
            commandManager = new CommandManager();
            commandManager.init();
        }
    }
}