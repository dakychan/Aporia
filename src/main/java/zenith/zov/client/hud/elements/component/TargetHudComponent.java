package zenith.zov.client.hud.elements.component;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.client.modules.impl.combat.Aura;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.server.ServerHandler;
import zenith.zov.utility.mixin.accessors.DrawContextAccessor;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.List;

import static java.lang.Math.round;

public class TargetHudComponent extends DraggableHudElement {

    private final Animation healthAnimation = new Animation(200, Easing.LINEAR);
    private final Animation gappleAnimation = new Animation(200, Easing.LINEAR);
    private final Animation toggleAnimation = new Animation(200, Easing.QUAD_IN_OUT);
    private final Animation targetSwitchAnimation = new Animation(150, Easing.SINE_IN_OUT);
    private LivingEntity target;

    private String lastTargetName = "";

    public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }

    @Override
    public void render(CustomDrawContext ctx) {
        this.width=145f;
        this.height=40f;
        Aura aura = Aura.INSTANCE;

        LivingEntity target = (mc.currentScreen instanceof ChatScreen) ? mc.player :aura.getTarget();
        setTarget(target);



        if (toggleAnimation.getValue() == 0 ||this.target ==null) return;

        String currentTargetName = this.target.getName().getString();


        renderTargetHud(ctx,  this.target, toggleAnimation.getValue() );

    }

    private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {

        float posX = x, posY = y;
        float width = 145f, height =40f;
        float headSize = 24f, padding = 5f;
        float fontSize = 7.5f;

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA bgLeft = theme.getForegroundLight();
        ColorRGBA bgRight = theme.getForegroundColor();

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);

        ctx.getMatrices().scale(0.8f, 0.8f);
        ctx.getMatrices().translate(-x, -y);
        ctx.getMatrices().translate(posX + width / 2f, posY + height / 2f);
        ctx.getMatrices().scale(animation, animation);
        ctx.getMatrices().translate(-(posX + width / 2f), -(posY + height / 2f));
      //  ctx.enableScissor((int) (posX-1+width*(1-animation)), (int) (posY-1+height*(1-animation)), (int) (posX+2+width*animation), (int) (+posY+height*animation+2));
        DrawUtil.drawBlurHud(ctx.getMatrices(),x, y, width,height,21,BorderRadius.all(4),ColorRGBA.WHITE);

        ctx.drawRoundedRect(posX, posY, headSize + padding*2 , height, BorderRadius.left(4f, 4f), bgLeft);
        ctx.drawRoundedRect(posX + headSize + padding * 2, posY, width - (headSize + padding * 2 ), height, BorderRadius.right(4f, 4f), bgRight);



        float hp = round(PlayerIntersectionUtil.getHealth(target));
        float maxHp =Math.max(20,hp);
        float gapple = Math.max(0,hp-20);

        float healthPercent = hp / maxHp;
        float gapplePercent = gapple / maxHp;
        float barFullWidth = (width - padding * 2 - headSize-padding*2);

        float animatedHealth = healthAnimation.update(barFullWidth * healthPercent);
        float animatedGapple = gappleAnimation.update(barFullWidth * gapplePercent);

        float headX = posX + padding;
        float headY = posY + (height - headSize) / 2f;

        // лицо
//        ctx.drawRoundedRect(posX , posY , headSize+padding , height,
//                BorderRadius.all(1.5f), new ColorRGBA(255, 255, 255, 7));

        if (target instanceof PlayerEntity player) {
            DrawUtil.drawPlayerHeadWithRoundedShader(
                    ctx.getMatrices(),
                    ((AbstractClientPlayerEntity) player).getSkinTextures().texture(),
                    headX, headY, headSize,
                    BorderRadius.all(0.5f), ColorRGBA.WHITE
            );
        } else {
            Font qFont = Fonts.MEDIUM.getFont(12);
            ctx.drawText(qFont, "?", headX + (headSize-qFont.width("?")) / 2f,
                    headY + headSize / 2f - qFont.height() / 2f, ColorRGBA.WHITE);
        }

        // хп
        Font nameFont = Fonts.MEDIUM.getFont(fontSize);
        String name = target.getName().getString();

        float maxNameWidth = width - (headSize + padding * 2 + 20f) - 30f;
        String displayName = name;
        if (nameFont.width(name) > maxNameWidth) {
            while (nameFont.width(displayName + "...") > maxNameWidth && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        ctx.drawText(nameFont, displayName, headX + headSize + padding *2, headY, theme.getWhite());

        String hpText = (int) round(hp) + "hp";
        Font hpFont = Fonts.MEDIUM.getFont(fontSize);
        ctx.drawText(hpFont, hpText, posX + width - padding - hpFont.width(hpText), headY, new ColorRGBA(181, 162, 255));

        if (target instanceof PlayerEntity player) {
            drawArmor(ctx, player, headX + headSize + padding *2, headY+fontSize, posX+width-(headX + headSize + padding *2), padding, fontSize);
        }

        // хпбар
        float barX = headX  + headSize + padding*2;
        float barY = headY+headSize-2.5f;
        ColorRGBA barBg = bgRight.darker(0.2f);

        ctx.drawRoundedRect(barX, barY, barFullWidth, 2.5f, BorderRadius.all(0.5f), barBg);
        ctx.drawRoundedRect(barX, barY, animatedHealth, 2.5f, BorderRadius.all(0.5f), new ColorRGBA(181, 162, 255));
        ctx.drawRoundedBorder(posX,posY,width, height,0.01f,BorderRadius.all(4),theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(),posX,posY,width, height,0.01f,20f,theme.getColor(),BorderRadius.all(4));


//        ctx.disableScissor();
        ctx.getMatrices().popMatrix();

        this.width = width*0.8f;
        this.height = height*0.8f;
    }

    private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float headSize, float padding, float fontSize) {
        float width = 145;

        float boxSizeItem = 10;
        float paddingItem = 4;
        float iconX = posX ;
        float iconY = posY +1;


        Font xFont = Fonts.ICONS.getFont(5f);
        ItemStack[] items = {
                player.getMainHandStack(),
                player.getOffHandStack(),
                player.getEquippedStack(EquipmentSlot.HEAD),
                player.getEquippedStack(EquipmentSlot.CHEST),
                player.getEquippedStack(EquipmentSlot.LEGS),
                player.getEquippedStack(EquipmentSlot.FEET)

        };
        Font font = Fonts.MEDIUM.getFont(5);
        for (ItemStack stack : items) {

            if (!stack.isEmpty()) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(iconX + (boxSizeItem - 9.6f) / 2f, iconY + (boxSizeItem - 9.6f) / 2f);
                ctx.getMatrices().scale(0.6f, 0.6f);
                ctx.drawItem(stack, 0,0);
                ((DrawContextAccessor) ctx).callDrawItemBar(stack,0,0);
                ((DrawContextAccessor) ctx).callDrawCooldownProgress(stack,0,0);
                ctx.getMatrices().popMatrix();



            } else {
                ctx.drawText(xFont, "M", iconX + (boxSizeItem-xFont.width("X"))/2, iconY + (boxSizeItem-xFont.height())/2, Zenith.getInstance().getThemeManager().getCurrentTheme().getGrayLight());
            }
            iconX += boxSizeItem + paddingItem;
        }
    }


    public void setTarget(LivingEntity target) {
        if (target == null) {
            toggleAnimation.update(0);
            if (toggleAnimation.getValue() == 0) {

                this.target = null;
            }
        } else {
            if(target!=this.target) {

                toggleAnimation.update(0);
                if(toggleAnimation.getValue()==0){
                    this.target = target;
                }
            }else {
                toggleAnimation.update(1);
            }



        }



    }
}


