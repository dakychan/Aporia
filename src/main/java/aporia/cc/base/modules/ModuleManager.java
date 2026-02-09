package aporia.cc.base.modules;

import aporia.cc.client.modules.impl.combat.AntiBot;
import aporia.cc.client.modules.impl.combat.Aura;
import aporia.cc.client.modules.impl.combat.AutoSwap;
import aporia.cc.client.modules.impl.combat.AutoTotem;
import aporia.cc.client.modules.impl.misc.*;
import aporia.cc.client.modules.impl.movement.*;
import aporia.cc.client.modules.impl.render.*;
import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;


import aporia.cc.base.events.impl.input.EventKey;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.impl.combat.*;
import aporia.cc.client.modules.impl.misc.*;
import aporia.cc.client.modules.impl.movement.*;
import aporia.cc.client.modules.impl.player.FastBreak;
import aporia.cc.client.modules.impl.player.NoDelay;
import aporia.cc.client.modules.impl.render.*;
import aporia.cc.utility.interfaces.IMinecraft;
import aporia.cc.client.modules.impl.player.AutoTool;
import aporia.cc.client.modules.impl.player.AutoArmor;
import aporia.cc.client.modules.impl.player.Blink;


import java.util.*;

@Getter
public final class ModuleManager implements IMinecraft {

    private final List<Module> modules = new ArrayList<>();

    public ModuleManager() {
        init();
        EventManager.register(this);
    }

    private void init() {
        registerCombat();
        registerMovement();
        registerRender();
        registerPlayer();
        registerMisc();
    }

    private void registerCombat() {

        registerModule(AntiBot.INSTANCE);
        registerModule(Aura.INSTANCE);
        registerModule(AutoSwap.INSTANCE);

        registerModule(AutoTotem.INSTANCE);
    }

    private void registerMovement() {

        registerModule(AutoSprint.INSTANCE);
        registerModule(ElytraBooster.INSTANCE);
        registerModule(ElytraRecast.INSTANCE);
        registerModule(GuiWalk.INSTANCE);
        registerModule(NoSlow.INSTANCE);
    }

    private void registerRender() {
        registerModule(Interface.INSTANCE);
        registerModule(AntiInvisible.INSTANCE);

        registerModule(Menu.INSTANCE);
        registerModule(NoRender.INSTANCE);
        registerModule(Predictions.INSTANCE);
        registerModule(BlockESP.INSTANCE);
        registerModule(SwingAnimation.INSTANCE);
        registerModule(Crosshair.INSTANCE);
        registerModule(ViewModel.INSTANCE);
        registerModule(WorldTweaks.INSTANCE);
        registerModule(EntityESP.INSTANCE);
    }

    private void registerPlayer() {
        registerModule(AutoTool.INSTANCE);
        registerModule(AutoArmor.INSTANCE);
        registerModule(Blink.INSTANCE);
        registerModule(NoDelay.INSTANCE);
        registerModule(FastBreak.INSTANCE);
    }

    private void registerMisc() {
        registerModule(ServerHelper.INSTANCE);
        registerModule(ElytraHelper.INSTANCE);
        registerModule(ItemScroller.INSTANCE);
        registerModule(ClickAction.INSTANCE);
        registerModule(FreeCam.INSTANCE);
        registerModule(CameraTweaks.INSTANCE);
        registerModule(AutoAuth.INSTANCE);
        registerModule(AutoDuels.INSTANCE);
        registerModule(AHHelper.INSTANCE);
        registerModule(AutoSbor.INSTANCE);
        registerModule(NoInteract.INSTANCE);
        registerModule(AutoAccept.INSTANCE);
        registerModule(AutoRespawn.INSTANCE);
        registerModule(NameProtect.INSTANCE);
        registerModule(ClanUpgrade.INSTANCE);
    }

    private void registerModule(Module module) {
        modules.add(module);
    }


    public Module getModule(String name) {
        return modules.stream()
                .filter(module -> module.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public Set<Module> getActiveModules() {
        Set<Module> active = new HashSet<>();
        for (Module module : modules) {
            if (module.isEnabled()) active.add(module);
        }
        return active;
    }


    @EventTarget
    public void onKey(EventKey event) {

        if (mc.currentScreen != null || event.getAction() != GLFW.GLFW_PRESS) return;

        for (Module module : modules) {
            if (module.getKeyCode() == event.getKeyCode()
                    && module.getKeyCode() != GLFW.GLFW_KEY_UNKNOWN) {
                module.toggle();
            }
        }
    }
}

