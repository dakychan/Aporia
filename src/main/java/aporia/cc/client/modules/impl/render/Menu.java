package aporia.cc.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import org.lwjgl.glfw.GLFW;
import aporia.cc.Aporia;

import aporia.cc.base.events.impl.render.EventRenderScreen;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.utility.render.display.base.UIContext;

@ModuleAnnotation(name = "Menu", category = Category.RENDER, description = "Меню чита")
public final class Menu extends Module {
    public static final Menu INSTANCE = new Menu();

    private Menu() {
        this.setKeyCode(GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    @Override
    public void onEnable() {
        if (mc.world == null) {
            this.setEnabled(false);
            return;
        }

        if (mc.currentScreen == Aporia.getInstance().getMenuScreen()) return;
        mc.setScreen(Aporia.getInstance().getMenuScreen());

        super.onEnable();
    }

    @Override
    public void onDisable() {


        super.onDisable();

    }


    @Override
    public void setKeyCode(int keyCode) {
        if(keyCode == -1) return;
        super.setKeyCode(keyCode);
    }

    @EventTarget
    public void render2d(EventRenderScreen eventRender2D){
        UIContext uiContext =eventRender2D.getContext();
        Aporia.getInstance().getMenuScreen().renderTop(uiContext, uiContext.getMouseX(), uiContext.getMouseY());
        if (Aporia.getInstance().getMenuScreen().isFinish()) {
            this.toggle();
        }
    }

}
