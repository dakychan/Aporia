package zenith.zov.utility.render.display.base;

import lombok.Getter;
import net.minecraft.util.Identifier;
import zenith.zov.Zenith;

@Getter
public class CustomSprite {

    private final Identifier texture;

    public CustomSprite(String path) {
        if (path.contains(":")) {
            this.texture = Identifier.of(path);
        } else if (path.contains("/")) {
            this.texture = Zenith.id(path);
        } else {
            this.texture = Zenith.id("icons/category/" + path);
        }
    }
}