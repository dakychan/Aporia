package zenith.zov.client.hud.elements.component;

import net.minecraft.client.gui.screen.ChatScreen;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.render.display.Keyboard;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.client.modules.api.Module;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.*;

public class KeybindsComponent extends DraggableHudElement {

    private final LinkedHashSet<KeyModule> keyModules = new LinkedHashSet<>();
    private final Animation animationWidth = new Animation(200,100,Easing.QUAD_IN_OUT);
    private final Animation animationScale = new Animation(200,0,Easing.QUAD_IN_OUT);
    private final Animation animationVisible = new Animation(200,0,Easing.QUAD_IN_OUT);
    public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }

    @Override
    public void render(CustomDrawContext ctx) {
        Font iconFont =Fonts.ICONS.getFont(6);

        Zenith.getInstance().getModuleManager().getActiveModules().forEach(module -> {
            if (module.getKeyCode() != -1&&!keyModules.contains(new KeyModule(module))) {
                keyModules.addLast(new KeyModule(module));
            }
        });
        keyModules.removeIf(km -> (!km.module.isEnabled() || km.module.getKeyCode() == -1) && km.isDelete());
        if(keyModules.isEmpty()) {
           animationScale.update( 0);
        }else {
            animationScale.update(keyModules.size()==1&&keyModules.getFirst().animation.getTargetValue()==0?0:1);
        }

        float x = this.x;
        float y = this.y;
        float thickness = 1.5f, height = (float) (18  + keyModules.stream().mapToDouble(key -> key.getHeight()).sum());


        float width = (float) keyModules.stream().mapToDouble(KeyModule::updateWidth).max().orElse(100);
        width = animationWidth.update(width);
        this.width = width;
        this.height = height;
        animationVisible.update(mc.currentScreen instanceof ChatScreen || !keyModules.isEmpty());
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ctx.pushMatrix();
      {

            ctx.getMatrices().translate(x + width / 2, y + height / 2);
            ctx.getMatrices().scale(animationVisible.getValue(), animationVisible.getValue());
            ctx.getMatrices().translate(-(x + width / 2), -(y + height / 2));


            BorderRadius radius6 = BorderRadius.all(6);
            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(x, y, width, height, radius6, theme.getForegroundLight());

            ctx.drawText(iconFont, "L", x + 8, y + (18 - iconFont.height()) / 2, theme.getColor());
            ctx.drawText(iconFont, "M", x + width - 8 - iconFont.width("M"), y + (18 - iconFont.height()) / 2, theme.getWhiteGray());

            Font font = Fonts.MEDIUM.getFont(6);
            ctx.drawText(font, "Keybinds", x + 8 + 8 + 2, y + (18 - font.height()) / 2, theme.getWhite());

        }
        //   Fonts.INTER.render( "Keybinds", 12, x + 10, y + 8, 0.05f, 0.4f, 0, GuiConfig.textColor);


      //  ctx.drawRoundedBorder(x + width - 50, y, 17, 17, 0.5f, radius6, color8);
        //  Fonts.ICON.render( "o", 12, x+width-50+8, y + 10, 0.05f, 0.4f, 0, themeManager.getCurrentTheme().getStart().getRGB());

        if(animationVisible.getValue()==1){
            float kmY = y + 18;

            int i = 0;
            ctx.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));
            for (KeyModule km : keyModules) {
                km.render(ctx, x, kmY, width, i);


                kmY += km.getHeight();
                i++;
            }
            ctx.disableScissor();
        }
        ctx.drawRoundedBorder(x, y, width, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, height, 0.1f, Math.min(20, Math.max(12, height / 2.5f)), theme.getColor(), BorderRadius.all(4));

        ctx.popMatrix();

    }



    class KeyModule implements Comparable<KeyModule> {
        private final Animation animation = new Animation(150,Easing.QUAD_IN_OUT);
        private final Animation animationColor = new Animation(200,Easing.QUAD_IN_OUT);

        private final Module module;
        private float width;

        public KeyModule(Module module) {
            this.module = module;
        }

        public float updateWidth() {

            float width = 100;
            float moduleTextWidth = Fonts.MEDIUM.getWidth(module.getName(), 6);
            float sizeBind = Math.round(Fonts.MEDIUM.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6));

            float keyTextWidth = 8+8+8 + sizeBind ;
            float widthText = width - (keyTextWidth  + 8 );
            if (widthText < 8 + moduleTextWidth + 8) {


                float deltaWidth = moduleTextWidth + 8 + 8 - widthText;
                width += deltaWidth;


            }
            return width;
        }

        public float getHeight() {
            return (float) (18 * animation.getValue());
        }

        public void render(CustomDrawContext ctx, float x, float y, float width,int i) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font iconFont =Fonts.ICONS.getFont(6);
            Font font =Fonts.MEDIUM.getFont(6);
            animation.update((!module.isEnabled() || module.getKeyCode() == -1) ? 0 : 1);
            ctx.pushMatrix();
            ctx.getMatrices().translate(x + width / 2, y+18/2f);
            float deltaANim =animation.getValue();

                ctx.getMatrices().scale((float) deltaANim, deltaANim);

            ctx.getMatrices().translate(-(x + width / 2), -(y+18/2f));

            float sizeBind = Math.round(Fonts.MEDIUM.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6));

            float keyTextWidth =8 + sizeBind ;
            float widthText = width - (keyTextWidth + 10 + 5 + 10);
            animationColor.update(i%2==0?1:0);
            ColorRGBA backgroundColor = theme.getForegroundLight().mix(theme.getForegroundColor(),animationColor.getValue());

            ctx.drawRoundedRect(x, y, width, 18,i==keyModules.size()-1?BorderRadius.bottom(4,4):BorderRadius.ZERO,backgroundColor);

            ctx.drawText(iconFont,module.getCategory().getIcon(), x + 8, y +(18-iconFont.height())/2, theme.getColor());

            ctx.drawText(font,module.getName(), x + 8+8+8, y +(18-font.height())/2, theme.getWhite());

            ctx.drawText(Fonts.BOLD.getFont(8),".", x + 8+8+2, y +4, theme.getWhiteGray());



//            int color16 = themeManager.getStart(0.16f);
//            RenderUtil.renderRect( x, y, keyTextWidth, 30,color16, 6, 1);
//            RenderUtil.renderBorder(  x, y, keyTextWidth, 30, color16, 6, 0.5f, 1, 1);
            ctx.drawText(font, Keyboard.getKeyName(module.getKeyCode()), x +width-keyTextWidth, y +(18-font.height())/2, theme.getColor());
            ctx.popMatrix();
//            int color16 = themeManager.getStart(0.16f);
//            RenderUtil.renderRect( x+width-50, y, 30, 30,color16, 6, 1);
//            RenderUtil.renderBorder(  x+width-50, y, 30, 30, color16, 6, 0.5f, 1, 1);
//            Fonts.ICON.render( "o", 12, x+width-50+8, y + 10, 0.05f, 0.4f, 0, color.getIntColor());

        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            KeyModule keyModule = (KeyModule) obj;
            return Objects.equals(module.getName(), keyModule.module.getName());
        }

        public boolean isDelete() {

            return animation.getValue() == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(module);
        }


        @Override
        public int compareTo(KeyModule o) {
            return module.getName().compareTo(o.module.getName());
        }
    }
}

