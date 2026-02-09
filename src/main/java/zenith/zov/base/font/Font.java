package zenith.zov.base.font;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.minecraft.text.Text;

@Getter
@AllArgsConstructor
public class Font {

    private MsdfFont font;
    private float size;

    public float height() {
        // Так называемый рокстарвский MAGIC VALUE
        return size * 0.7F;
    }

    public float width(String text) {
        return font.getWidth(text, size);
    }

    public float width(Text text) {
        return font.getTextWidth(text, size);
    }

}