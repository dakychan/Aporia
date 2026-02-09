package aporia.cc.client.modules.impl.render;

import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.MultiBooleanSetting;

import java.util.List;

@ModuleAnnotation(name = "NoRender", category = Category.RENDER,description = "Убирает лишние элементы с экрана")
public final class NoRender extends Module {
    public static final NoRender INSTANCE = new NoRender();

    private final MultiBooleanSetting settings = MultiBooleanSetting.create("Убрать", List.of(
            "Огонь",
            "Плохие эффекты"
    ));


    private NoRender() {
    }


    public boolean isRemoveFire() {
        return this.isEnabled() && settings.isEnable(0);
    }
    public boolean isRemoveBadEffect() {
        return this.isEnabled() && settings.isEnable(1);
    }
}

