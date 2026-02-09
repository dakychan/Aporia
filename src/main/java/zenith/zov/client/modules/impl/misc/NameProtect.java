package zenith.zov.client.modules.impl.misc;

import zenith.zov.Zenith;
import com.darkmagician6.eventapi.EventTarget;
import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;

import java.util.List;

// ООО<<МИНЦЕТ ПАСТИНГ INC>>ООО
@ModuleAnnotation(name = "NameProtect", category = Category.MISC, description = "Защищает имена игроков")
public final class NameProtect extends Module {
    public static final NameProtect INSTANCE = new NameProtect();
    
    private NameProtect() {
    }

    private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);

    public static String getCustomName() {
        Module module = NameProtect.INSTANCE;
        return module != null && module.isEnabled() ? "ZENITHDLC" : mc.player.getNameForScoreboard();
    }

    public static String getCustomName(String originalName) {
        Module module = NameProtect.INSTANCE;
        if (module == null || !module.isEnabled() || mc.player == null) {
            return originalName;
        }

        String me = mc.player.getNameForScoreboard();
        if (originalName.contains(me)) {
            return originalName.replace(me, "ZENITHDLC");
        }

        if (module instanceof NameProtect nameProtect && nameProtect.hideFriends.isEnabled()) {
            var friends = Zenith.getInstance().getFriendManager().getItems();
            for (String friend : friends) {
                if (originalName.contains(friend)) {
                    return originalName.replace(friend, "ZENITHDLC");
                }
            }
        }

        return originalName;
    }
}
