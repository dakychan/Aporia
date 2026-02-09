package aporia.cc.client.modules.impl.misc;

import aporia.cc.Aporia;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;

// ООО<<МИНЦЕТ ПАСТИНГ INC>>ООО
@ModuleAnnotation(name = "NameProtect", category = Category.MISC, description = "Защищает имена игроков")
public final class NameProtect extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    
    private NameProtect() {
    }

    private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public static String getCustomName() {
        Module module = NameProtect.INSTANCE;
        return module != null && module.isEnabled() ? "AporiaDLC" : mc.player.getNameForScoreboard();
    }

    public static String getCustomName(String originalName) {
        Module module = NameProtect.INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "AporiaDLC");
        }

        if (module instanceof NameProtect nameProtect && nameProtect.hideFriends.isEnabled()) {
            var friends = Aporia.getInstance().getFriendManager().getItems();
            for (String friend : friends) {
                if (originalName.contains(friend)) {
                    return originalName.replace(friend, "AporiaDLC");
                }
            }
        }

        return originalName;
    }
}
