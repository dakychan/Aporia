package zenith.zov.client.hud.elements.component;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.*;

public class PotionsComponent extends DraggableHudElement {

    private final Animation animationWidth = new Animation(200, 100, Easing.QUAD_IN_OUT);
    private final Animation animationScale = new Animation(200, 0, Easing.QUAD_IN_OUT);
    private final Map<String, PotionModule> modules = new LinkedHashMap<>();
    private final Animation animationVisible = new Animation(200,0,Easing.QUAD_IN_OUT);

    public PotionsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }
    Set<String> currentKeys = new HashSet<>();
    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) return;
        currentKeys.clear();
        List<StatusEffectInstance> effects = new ArrayList<>(mc.player.getActiveStatusEffects().values());


        for (StatusEffectInstance eff : effects) {
            String key = eff.getEffectType().value().getTranslationKey() + eff.getAmplifier();
            currentKeys.add(key);
            if (!modules.containsKey(key)) {
                modules.put(key, new PotionModule(eff));
            }
        }


        modules.values().removeIf(PotionModule::isDelete);

        boolean hidden = modules.isEmpty();
        animationScale.update(hidden ? 1 : 0);

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font iconFont = Fonts.ICONS.getFont(6);
        Font font = Fonts.MEDIUM.getFont(6);

        float x = this.x;
        float y = this.y;

        float height = 18 + (float) modules.values().stream().mapToDouble(PotionModule::getHeight).sum();
        float width = (float) modules.values().stream().mapToDouble(PotionModule::updateWidth).max().orElse(100);
        width = animationWidth.update(width);

        this.width = width;
        this.height = height;

        ctx.pushMatrix();


        BorderRadius radius = new BorderRadius(4, 4, 4, 4);

        animationVisible.update(mc.currentScreen instanceof ChatScreen || !modules.isEmpty());


    {

            ctx.getMatrices().translate(x + width / 2, y + height / 2);
            ctx.getMatrices().scale(animationVisible.getValue(), animationVisible.getValue());
            ctx.getMatrices().translate(-(x + width / 2), -(y + height / 2));

            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(x, y, width, height, radius, theme.getForegroundLight());
            ctx.drawText(iconFont, "O", x + 8, y + (18 - iconFont.height()) / 2, theme.getColor());
            ctx.drawText(iconFont, "M", x + width - 8 - iconFont.width("M"), y + (18 - iconFont.height()) / 2, theme.getWhiteGray());
            ctx.drawText(font, "Potions", x + 8 + 8 + 2, y + (18 - font.height()) / 2, theme.getWhite());
        }
        if(animationVisible.getValue()==1){
            float offsetY = y + 18;
            int index = 0;
            for (PotionModule module : modules.values()) {
                module.render(ctx, x, offsetY, width, index);
                offsetY += module.getHeight();
                index++;
            }
        }
        ctx.drawRoundedBorder(x, y,width,height,0.1f,BorderRadius.all(4),theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y,width,height,0.1f,Math.min(20,Math.max(12,height/2.5f)),theme.getColor(),BorderRadius.all(4));

        ctx.popMatrix();
    }

    private class PotionModule {
        private final Animation animation = new Animation(150, 0.01f,Easing.QUAD_IN_OUT);
        private final Animation animationColor = new Animation(200, Easing.QUAD_IN_OUT);
        private  StatusEffectInstance effect;

        public PotionModule(StatusEffectInstance effect) {
            this.effect = effect;

        }

        public float updateWidth() {
            Font font = Fonts.MEDIUM.getFont(6);

            String name = I18n.translate(effect.getEffectType().value().getTranslationKey());
            String amp = getAmplifierText(effect.getAmplifier());
            String duration = formatDuration(effect.getDuration());

            float width = 100;
            float moduleTextWidth = 8+8+8+font.width(name+" "+amp);

            float keyTextWidth = font.width(duration);
            float widthText = width - (keyTextWidth  + 8 );
            if (widthText < 8 + moduleTextWidth + 8) {


                float deltaWidth = moduleTextWidth + 8 + 8 - widthText;
                width += deltaWidth;


            }

            return width;
        }


        public float getHeight() {
            return 18 * animation.getValue();
        }

        public void render(CustomDrawContext ctx, float x, float y, float width, int i) {

            String key = effect.getTranslationKey() + effect.getAmplifier();
            effect =  new ArrayList<>(mc.player.getActiveStatusEffects().values()).stream().filter(e ->{
                return( e.getTranslationKey() + e.getAmplifier() ).equals(key);
            }).findAny().orElse(effect);

            animation.update(currentKeys.contains(key) ? 1 : 0);
            animationColor.update(i % 2 == 0 ? 1 : 0);

            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font font = Fonts.MEDIUM.getFont(6);
            ColorRGBA background = theme.getForegroundLight().mix(theme.getForegroundColor(), animationColor.getValue());

            ctx.pushMatrix();
            ctx.getMatrices().translate(x + width / 2, y + 9);
            ctx.getMatrices().scale(animation.getValue(), animation.getValue());
            ctx.getMatrices().translate(-(x + width / 2), -(y + 9));

            ctx.drawRoundedRect(x, y, width, 18, (i == modules.size() - 1) ? BorderRadius.bottom(4, 4) : BorderRadius.ZERO, background);


            Identifier icon = getEffectIcon(effect.getEffectType().value());
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, icon, (int) (x + 8), (int) (y + 6), 0.0f, 0.0f, 6, 6, 18, 18);
            String name = I18n.translate(effect.getEffectType().value().getTranslationKey());
            String amp = getAmplifierText(effect.getAmplifier());

            ctx.drawText(Fonts.BOLD.getFont(8),".", x + 8+8+2, y +4, theme.getWhiteGray());

            ctx.drawText(font, name , x + 8 + 8 + 8, y + (18 - font.height()) / 2, theme.getWhite());

            ctx.drawText(font, amp , x + 8 + 8 + 8+font.width(name+" "), y + (18 - font.height()) / 2, theme.getGrayLight());

            String duration = formatDuration(effect.getDuration());
            ctx.drawText(font, duration, x + width - 8 - font.width(duration), y + (18 - font.height()) / 2, theme.getColor());

            ctx.popMatrix();
        }
        public boolean isDelete() {

            return animation.getValue() == 0;
        }
    }

    private String getAmplifierText(int amplifier) {

        return String.valueOf(amplifier+1);
    }


    private String formatDuration(int durationTicks) {
        int totalSeconds = durationTicks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    private Identifier getEffectIcon(StatusEffect effect) {
        String id = effect.getTranslationKey().replace("effect.minecraft.", "").replace("effect.", "");
        return  Identifier.of("minecraft", "textures/mob_effect/" + id + ".png");
    }

}

