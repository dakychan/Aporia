package zenith.zov.utility.render.display.base;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;

@Getter
public class UIContext extends CustomDrawContext {

    private final int mouseX, mouseY;
    private final float delta;

    protected UIContext(DrawContext originalContext, int mouseX, int mouseY, float delta) {
        super(originalContext);
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.delta = delta;
    }

    public static UIContext of(DrawContext originalContext, int mouseX, int mouseY, float delta) {
        return new UIContext(originalContext, mouseX, mouseY, delta);
    }

}