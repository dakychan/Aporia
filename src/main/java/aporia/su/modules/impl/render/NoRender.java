package aporia.su.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import aporia.su.modules.module.ModuleStructure;
import aporia.su.modules.module.category.ModuleCategory;
import aporia.su.modules.module.setting.implement.MultiSelectSetting;
import aporia.su.util.Instance;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoRender extends ModuleStructure {

    public static NoRender getInstance() {
        return Instance.get(NoRender.class);
    }

    public MultiSelectSetting modeSetting = new MultiSelectSetting("Элементы", "Выберите элементы для игнорирования")
            .value("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea", "Scoreboard", "BossBar")
            .selected("Fire", "Bad Effects", "Block Overlay", "Darkness", "Damage", "Nausea");

    public NoRender() {
        super("NoRender", "No Render", ModuleCategory.RENDER);
        settings(modeSetting);
    }
}