package aporia.cc.utility.render.display.base;

import lombok.Getter;
import net.minecraft.util.Identifier;
import aporia.cc.Aporia;

@Getter
public class CustomSprite {

    private final Identifier texture;

    public CustomSprite(String path) {
        if (path.contains(":")) {
            this.texture = Identifier.of(path);
        } else if (path.contains("/")) {
            this.texture = Aporia.id(path);
        } else {
            this.texture = Aporia.id("icons/category/" + path);
        }
    }
}

