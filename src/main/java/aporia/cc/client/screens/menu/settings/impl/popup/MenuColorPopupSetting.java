package aporia.cc.client.screens.menu.settings.impl.popup;

import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.ChangeRect;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.UIContext;
import net.minecraft.client.util.math.Vector2f;
import net.minecraft.util.math.MathHelper;
import aporia.cc.Aporia;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.setting.impl.ColorSetting;
import aporia.cc.client.screens.menu.settings.api.MenuPopupSetting;
import aporia.cc.utility.game.other.MouseButton;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.TextBox;
import Aporia.zov.utility.render.display.base.*;
import aporia.cc.utility.render.display.base.color.ColorRGBA;
import aporia.cc.utility.render.display.base.color.ColorUtil;
import aporia.cc.utility.render.display.shader.DrawUtil;

import java.awt.*;
import java.util.Objects;

import static aporia.cc.utility.interfaces.IMinecraft.mc;

public class MenuColorPopupSetting extends MenuPopupSetting {
    private boolean open;
    private float hue;
    private float saturation;
    private float brightness;
    private int alpha;

    private boolean afocused;
    private boolean hfocused;
    private boolean sbfocused;


    private ColorSetting setting;
    private final TextBox colorString;

    public MenuColorPopupSetting(ChangeRect rect, ColorSetting setting) {

        super(rect);

        this.setting = setting;

        colorString = new TextBox(new Vector2f(0,0),Fonts.MEDIUM.getFont(7),"color",78);
        colorString.setCharFilter(TextBox.CharFilter.ENGLISH_NUMBERS);
        colorString.setMaxLength(6);
        updatePos();
        animationScale.update(1);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(colorString.keyPressed(keyCode, scanCode, modifiers)){
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return colorString.charTyped(chr, modifiers);
    }

    @Override
    public void render(UIContext ctx, float mouseX, float mouseY, float alphas, Theme theme) {
        animationScale.update();
        alphas = 1;
        ctx.pushMatrix();
        ctx.getMatrices().translate(bounds.getX(),bounds.getY()+bounds.getHeight()/2);
        ctx.getMatrices().scale(animationScale.getValue(),animationScale.getValue());
        ctx.getMatrices().translate(-bounds.getX(),-(bounds.getY()+bounds.getHeight()/2));

        ctx.drawRoundedRect(bounds.getX(),bounds.getY(),bounds.getWidth(),bounds.getHeight(), BorderRadius.all(4),theme.getForegroundColor().mulAlpha(alphas));
        ctx.drawRoundedRect(bounds.getX(),bounds.getY(),bounds.getWidth(),18, BorderRadius.top(4,4),theme.getForegroundLight().mulAlpha(alphas));



        if (mc.currentScreen == null) {
            afocused = false;
            hfocused = false;
            sbfocused = false;
        }
        float x = bounds.getX();
        float y = bounds.getY();
        float width = bounds.getWidth();
        float height = bounds.getHeight();

        float padding = 5;
        float colorX = padding+x;
        float colorY = padding+y+18;
        float colorWidth = width-padding*2;
        float colorHeight = 48;
        Font iconFont =Fonts.ICONS.getFont(6);
        ctx.drawText(iconFont,"V", x + padding, y +(18-iconFont.height())/2, theme.getColor().mulAlpha(alphas));
        ctx.drawText(iconFont,"M", x +width- padding-iconFont.width("M"), y +(18-iconFont.height())/2, theme.getWhiteGray().mulAlpha(alphas));

        Font font = Fonts.MEDIUM.getFont(7);
        ctx.drawText(font,setting.getName(), x + 8+8, y +(18-font.height())/2, theme.getWhite().mulAlpha(alphas));

        bounds.setWidth(Math.max(96,font.width(setting.getName())+30));

        bounds.setHeight(18+padding+colorHeight+padding+6+padding+6+padding+18+padding);

//            RenderUtil.renderRect(matrix, x, y, 180, 234, GuiConfig.bg, 6, 1);
//            RenderUtil.renderBorder(matrix, x, y, 180, 234, GuiConfig.bg, 6, 0.5f, 1, 1);

//        Fonts.INTER.render(matrix, setting.getName(), 12, x, y + 10, 0.05f, 0.3f, 0, GuiConfig.colorTextNotEnable);





        float spos = (float) (((colorX) + (colorWidth)) - ((colorWidth) - ((colorWidth) * saturation)));
        float bpos = (colorY + (colorHeight - (colorHeight * brightness)));
        float hpos = colorWidth * hue;
        float apos = colorWidth * alpha / 255;

        ColorRGBA colorA = new ColorRGBA(Color.getHSBColor(hue, 0.0F, 1.0F)).mulAlpha(alphas), colorB =  new ColorRGBA(Color.getHSBColor(hue, 1.0F, 1.0F)).mulAlpha(alphas);

        ColorRGBA colorC =new ColorRGBA( new Color(0, 0, 0, 0)), colorD = new ColorRGBA(new Color(0, 0, 0));
        //bg для колора
        ctx.drawRoundedRect( (colorX), colorY, colorWidth, colorHeight,BorderRadius.all(4), Gradient.of(colorA,colorA,colorB,colorB));
        ctx.drawRoundedRect( (colorX), colorY, colorWidth, colorHeight,BorderRadius.all(4),Gradient.of(colorC, colorD, colorC, colorD));
        ctx.drawRoundedBorder( (colorX), colorY, colorWidth, colorHeight,0.1f,BorderRadius.all(4),ColorRGBA.WHITE.mulAlpha(alphas));

        ctx.drawRoundedRect( spos -2, bpos - 2, 6, 6,BorderRadius.all(2),ColorRGBA.WHITE.mulAlpha(alphas));
        BorderRadius round = BorderRadius.all(1);
//        //хуе лгбт
        DrawUtil.drawRoundedTexture(ctx.getMatrices(),Aporia.id("icons/sliderhue.png"), colorX, colorY + colorHeight + padding, colorWidth, 4,round,ColorRGBA.WHITE.mulAlpha(alphas));
//        //ползунок

        ctx.drawRoundedRect(  colorX + hpos - 2, colorY + colorHeight + padding-1, 6, 6,BorderRadius.all(2),ColorRGBA.WHITE.mulAlpha(alphas));


//        //алфа

        DrawUtil.drawRoundedTexture(ctx.getMatrices(),Aporia.id("icons/slidertransparent.png"), colorX, colorY + colorHeight + padding+6+padding, colorWidth, 4,round,ColorRGBA.WHITE.mulAlpha(alphas));
        ColorRGBA fullAlpha =setting.getColor().withAlpha(255).mulAlpha(alphas);
        ctx.drawRoundedRect(colorX, colorY + colorHeight + padding+6+padding, colorWidth, 4,round,Gradient.of(ColorRGBA.TRANSPARENT,ColorRGBA.TRANSPARENT,fullAlpha,fullAlpha));

        ctx.drawRoundedRect(  colorX + apos - 2, colorY + colorHeight +6+padding+ padding-1, 6, 6,BorderRadius.all(2),ColorRGBA.WHITE.mulAlpha(alphas));



        //text

        ctx.drawRoundedRect(colorX, colorY + colorHeight + padding+6+padding+6+padding, colorWidth, 14,round,theme.getForegroundLight().mulAlpha(alphas));
        ctx.pushMatrix();
        //  ctx.getMatrices().translate(colorX, colorY + colorHeight + padding+6+padding+6+padding);
        ctx.drawText(font,"#",colorX+padding, colorY + colorHeight + padding+6+padding+6+padding+4,theme.getGray());
        this.colorString.render( ctx, colorX+padding+font.width("#")+1f, colorY + colorHeight + padding+6+padding+6+padding+4.5f,theme.getWhite().mulAlpha(alphas),theme.getGray().mulAlpha(alphas));
        this.colorString.setWidth( colorWidth-20);

        ctx.popMatrix();
        Color value = Color.getHSBColor(hue, saturation, brightness);
        if (sbfocused) {
            saturation = (float) (MathHelper.clamp(mouseX - (colorX), 0f, colorWidth) / (colorWidth));
            brightness = (float) ((colorHeight - MathHelper.clamp((mouseY - colorY), 0, colorHeight)) / colorHeight);
            saturation = MathHelper.clamp(saturation, 0f, 1f);
            brightness = MathHelper.clamp(brightness, 0f, 1f);
            value = Color.getHSBColor(hue, saturation, brightness);
            setColor(new ColorRGBA(value.getRed(), value.getGreen(), value.getBlue(), alpha));
        }

        if (hfocused) {
            hue = (float) (MathHelper.clamp(mouseX - (colorX), 0f, colorWidth) / (colorWidth));
            hue = MathHelper.clamp(hue, 0f, 1f);

            value = Color.getHSBColor(hue, saturation, brightness);

            setColor(new ColorRGBA(value.getRed(), value.getGreen(), value.getBlue(), alpha));
        }
//
        if (afocused) {
            alpha = (int) (MathHelper.clamp((mouseX - x) / colorWidth, 0f, 1f) * 255);

            setColor(new ColorRGBA(value.getRed(), value.getGreen(), value.getBlue(), alpha));

        }
        if(colorString.isSelected()){
            setColor(ColorUtil.hexToRgb(colorString.getText(),setting.getColor()));
            updatePos();
        }else {
            colorString.setText(ColorUtil.colorToHex(setting.getColor()));
            colorString.setCursor(6);
        }




//        //text
//        RenderUtil.renderRect(matrix, x, y + height + 18 + 5 + 8 + 10, 126, 26, GuiConfig.bg, 6, 1);
//        RenderUtil.renderBorder(matrix, x, y + height + 18 + 5 + 8 + 10, 126, 26, GuiConfig.bg, 6, 0.5f, 1, 1);
//        int color16 = themeManager.getStart(0.16f);
//        RenderUtil.renderRect(matrix, x + 126 + 5, y + height + 18 + 5 + 8 + 10, 26, 26, color16, 6, 1);
//        RenderUtil.renderBorder(matrix, x + 126 + 5, y + height + 18 + 5 + 8 + 10, 26, 26, color16, 6, 0.5f, 1, 1);
//        Fonts.ICON.render(matrix, "5", 12, x + 126 + 5+6, y + height + 18 + 5 + 8 + 10+6.4f, 0.05f, 0.4f, 0, themeManager.getStart(1f));
//        if(MathUtil.isHovered(mouseX,mouseY,x + 126 + 5, y + height + 18 + 5 + 8 + 10, 26, 26,xMax,yMax) &&ImGui.isMouseClicked(0)){
//            this.setting.reset();
//        }
        ctx.popMatrix();
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, MouseButton button) {

        colorString.onMouseClicked(mouseX,mouseY,button);
         float x = bounds.getX();
        float y = bounds.getY();
        float width = bounds.getWidth();
        float height = bounds.getHeight();

        float padding = 5;
        float colorX = padding+x;
        float colorY = padding+y+18;
        float colorWidth = width-padding*2;
        float colorHeight = 48;
        if(MathUtil.isHovered(mouseX,mouseY,colorString.getPosition().getX(),colorString.getPosition().getY(),colorWidth,14)){
            colorString.setSelected(true);
        }

            if ((MathUtil.isHovered(mouseX, mouseY, colorX, colorY, colorWidth, colorHeight))) {
            if (!(hfocused || afocused)) {
                sbfocused = true;
            }
            return;
        }

        if ((MathUtil.isHovered(mouseX, mouseY, colorX, colorY + colorHeight + padding, colorWidth, 6) )  ) {
            if (!(sbfocused || afocused)) {
                hfocused = true;
            }
            return;
        }


        if ((MathUtil.isHovered(mouseX, mouseY, colorX, colorY + colorHeight + padding+6+padding , width, 6)  )) {
            if (!(hfocused || sbfocused)) {
                afocused = true;
            }
            return;
        }
        Font iconFont = Fonts.ICONS.getFont(6);
        if(MathUtil.isHovered(mouseX,mouseY,x +width- padding-iconFont.width("M"), y +(18-iconFont.height())/2,iconFont.width("M"),4)){
            animationScale.update(0);
        }
    }

    @Override
    public float getWidth() {
        return 0;
    }

    @Override
    public float getHeight() {
        return 0;
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    private void updatePos() {
        float[] hsb = Color.RGBtoHSB(setting.getColor().getRed(), setting.getColor().getGreen(), setting.getColor().getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        brightness = hsb[2];
        alpha = setting.getColor().getAlpha();

    }

    private void setColor(ColorRGBA color) {
        setting.setColor(color);

    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof MenuColorPopupSetting that) {
            return setting == that.setting;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(setting);
    }
    public void onMouseReleased(double mouseX, double mouseY, MouseButton button) {
        hfocused = false;
        sbfocused = false;
        afocused = false;

    }
}

