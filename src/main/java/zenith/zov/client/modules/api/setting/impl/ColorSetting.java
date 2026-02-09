package zenith.zov.client.modules.api.setting.impl;


import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.utility.render.display.base.color.ColorRGBA;

import java.util.function.Supplier;

public class ColorSetting extends Setting {
    @Getter
    private ColorRGBA color;

    private final ColorGetter colorGetter;


    public ColorSetting(String name, ColorRGBA color, Supplier<Boolean> visible, ColorGetter colorGetter) {
        this(name,color,colorGetter);
        setVisible(visible);
    }

    public ColorSetting(String name, ColorRGBA color,ColorGetter colorGetter) {

        super(name);
        if(color==null){

            throw new RuntimeException(name+" color is null");
        }
        this.color = color;

        setColor(color);


        this.colorGetter = colorGetter;
    }
    public ColorSetting(String name, ColorRGBA color) {
         this(name,color,()->color);
    }
    public ColorSetting(String name, ColorGetter color) {
        this(name,color.getDefaultColor(),color);
    }
    public ColorSetting(String name, ColorRGBA color, Supplier<Boolean> visible) {
        this(name,color,visible,()->color);

    }



    public int getIntColor(){
        return color.getRGB();
    }

    public void setColor(int color) {
        this.color = new ColorRGBA(color);
    }
    public void setColor(ColorRGBA color) {
        this.color = color;
    }



    public void update() {

    }

    public void reset() {

        color = colorGetter.getDefaultColor();

    }
    public ColorRGBA getColor(float alpha){

        return color.mulAlpha(alpha);
    }

    @Override
    public void safe(JsonObject propertiesObject) {
        propertiesObject.addProperty(String.valueOf(name), ((ColorSetting) this).getIntColor());

    }

    @Override
    public void load(JsonObject propertiesObject) {
        this.setColor(propertiesObject.get(String.valueOf(name)).getAsInt());
    }


    public interface ColorGetter{
        ColorRGBA getDefaultColor();
    }
}
