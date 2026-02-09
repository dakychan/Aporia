package zenith.zov.client.modules.impl.misc;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.slot.Slot;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.impl.ColorSetting;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

@Getter
@ModuleAnnotation(name = "AH Helper", category = Category.MISC, description = "помощник в поиске дешевых предметов")
public final class AHHelper extends Module {

    public static final AHHelper INSTANCE = new AHHelper();
    private AHHelper() {}

    private final ColorSetting cheapSlotColor = new ColorSetting("Цвет дешевого", new ColorRGBA(64, 255, 64, 140));
    private final ColorSetting goodSlotColor = new ColorSetting("Цвет выгодного", new ColorRGBA(255, 255, 64, 140));



    public void renderCheat(DrawContext context, Slot slot){


        context.fill(slot.x,slot.y,slot.x+16,slot.y+16,cheapSlotColor.getIntColor());
    }
    public void renderGood(DrawContext context, Slot slot){
        context.fill(slot.x,slot.y,slot.x+16,slot.y+16,goodSlotColor.getIntColor());
    }


}


