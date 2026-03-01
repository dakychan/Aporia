package aporia.su.manager;

import lombok.Getter;
import aporia.su.client.draggables.HudManager;
import aporia.su.command.CommandManager;
import aporia.su.events.api.EventManager;
import aporia.su.modules.impl.combat.aura.attack.StrikerConstructor;
import aporia.su.modules.module.*;
import aporia.su.screens.clickgui.ClickGui;
import aporia.su.util.config.ConfigSystem;
import aporia.su.util.config.impl.bind.BindConfig;
import aporia.su.util.config.impl.blockesp.BlockESPConfig;
import aporia.su.util.config.impl.drag.DragConfig;
import aporia.su.util.config.impl.friend.FriendConfig;
import aporia.su.util.config.impl.prefix.PrefixConfig;
import aporia.su.util.config.impl.proxy.ProxyConfig;
import aporia.su.util.config.impl.staff.StaffConfig;
import aporia.su.util.modules.ModuleProvider;
import aporia.su.util.modules.ModuleSwitcher;
import aporia.su.util.render.shader.RenderCore;
import aporia.su.util.render.shader.Scissor;
import aporia.su.util.render.font.FontInitializer;
import aporia.su.util.repository.macro.MacroRepository;
import aporia.su.util.repository.way.WayRepository;
import aporia.su.util.tps.TPSCalculate;

/**
 *  © 2026 Copyright Aporia.cc 2.0
 *        All Rights Reserved ®
 */

@Getter
public class Manager {
    public StrikerConstructor attackPerpetrator = new StrikerConstructor();
    private EventManager eventManager;
    private RenderCore renderCore;
    private Scissor scissor;
    private ModuleProvider moduleProvider;
    private ModuleRepository moduleRepository;
    private ModuleSwitcher moduleSwitcher;
    private ClickGui clickgui;
    private ConfigSystem configSystem;
    private CommandManager commandManager;
    private TPSCalculate tpsCalculate;
    private HudManager hudManager = new HudManager();

    public void init() {
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
        configSystem = new ConfigSystem();
        configSystem.init();
        commandManager = new CommandManager();
        commandManager.init();
    }
}