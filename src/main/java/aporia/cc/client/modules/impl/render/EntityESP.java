package aporia.cc.client.modules.impl.render;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import aporia.cc.Aporia;
import com.darkmagician6.eventapi.EventTarget;
import aporia.cc.base.events.impl.render.EventRender2D;
import aporia.cc.base.events.impl.render.EventRender3D;
import aporia.cc.base.font.Font;
import aporia.cc.base.font.Fonts;
import aporia.cc.base.theme.Theme;
import aporia.cc.client.modules.api.Category;
import aporia.cc.client.modules.api.Module;
import aporia.cc.client.modules.api.ModuleAnnotation;
import aporia.cc.client.modules.api.setting.impl.BooleanSetting;
import aporia.cc.client.modules.api.setting.impl.ModeSetting;
import aporia.cc.client.modules.api.setting.impl.MultiBooleanSetting;
import aporia.cc.client.modules.api.setting.impl.NumberSetting;
import aporia.cc.client.modules.impl.misc.NameProtect;
import aporia.cc.utility.game.other.TextUtil;
import aporia.cc.utility.game.player.PlayerIntersectionUtil;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.math.ProjectionUtil;
import aporia.cc.utility.render.display.Render2DUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.CustomDrawContext;
import aporia.cc.utility.render.display.base.color.ColorRGBA;
import aporia.cc.utility.render.display.shader.DrawUtil;
import aporia.cc.utility.render.level.Render3DUtil;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

@ModuleAnnotation(name = "EntityESP", category = Category.RENDER, description = "ESP")
public final class EntityESP extends Module {
    public static final EntityESP INSTANCE = new EntityESP();

    private final MultiBooleanSetting elements = MultiBooleanSetting.create("Элементы",
            asList("Ники", "Предметы", "Броню","Треугольники","Боксы","Руки"));

    private final ModeSetting textMode = new ModeSetting("Шрифт");
    private final ModeSetting.Value mojo = new ModeSetting.Value(textMode, "Minecraft");
    private final ModeSetting.Value Aporia = new ModeSetting.Value(textMode, "Aporia").select();

    private final NumberSetting scale = new NumberSetting("Размер", 1.0f, 0.5f, 2.0f, 0.1f);
    private final BooleanSetting blur = new BooleanSetting("Блюр", "Требует хорошего пк", false, Interface.INSTANCE::isBlur);
    private final BooleanSetting glow = new BooleanSetting("Свечение", "Нужен мощный пк", false, Interface.INSTANCE::isGlow);

    private EntityESP() {

    }

    public boolean isRenderName() {
        return this.isEnabled()&&this.elements.isEnable(0) ;
    }

    private boolean isElementEnabled(String elementName) {
        return elements.isEnable(elementName);
    }

    private boolean isUiOverlayActive() {
        return mc.currentScreen != null || Menu.INSTANCE.isEnabled();
    }

    @EventTarget
    public void onHudRender(EventRender3D event) {
        if (mc.player == null || mc.world == null || isUiOverlayActive()) return;
        if(!elements.isEnable(4)) return;
        for (Entity ent : mc.world.getPlayers()) {

            if (ent instanceof PlayerEntity entity) {
                if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) continue;


                Render3DUtil.drawBox((entity.getBoundingBox().offset(MathUtil.interpolate(entity).subtract(entity.getPos()))), Aporia.getInstance().getThemeManager().getClientColor(180).getRGB(), 1f);
            }
        }
    }

    @EventTarget
    public void onHudRender(EventRender2D event) {
        if (mc.player == null || mc.world == null || mc.getEntityRenderDispatcher().camera == null) return;
        if (isUiOverlayActive()) return;
        Render2DUtil.onRender(event.getContext());
        try {
            CustomDrawContext context = event.getContext();

            for (Entity ent : mc.world.getEntities()) {

                if (ent instanceof PlayerEntity entity) {
                    if ( this.elements.isEnable(5)) {
                        if (elements.isEnable(5)) {

                            Vector4d vec4d = ProjectionUtil.getVector4D(entity);

                            drawHands(event.getContext(), entity, vec4d);
                        }
                    }
                    if ( this.elements.isEnable(0)) renderNameTag(context, entity, event.getTickDelta());

                    if (elements.isEnable(3)) renderEntityBox(context, entity, event.getTickDelta());

                }
                if (ent instanceof ItemEntity entity) {
                    if ( this.elements.isEnable(1)) renderItemESP(context, entity, event.getTickDelta());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
        }
    }
    private void drawHands(CustomDrawContext context, PlayerEntity player, Vector4d vec) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) return;

        if(ProjectionUtil.canSee(vec) ) return;
        double posY = vec.w; 
        final float centerX = (float) ProjectionUtil.centerX(vec);

        for (ItemStack stack : List.of(player.getMainHandStack(), player.getOffHandStack())) {
            if (stack == null || stack.isEmpty()) continue;

            MutableText text = stack.getName().copy();


            if(text.getSiblings().isEmpty()) {
                text = text.copy().setStyle(Style.EMPTY.withColor(Aporia.getInstance().getThemeManager().getCurrentTheme().getWhite().getRGB()));
            }
            if (!text.getString().isEmpty() ) {
                text = TextUtil.replaceLastChar(text, "");

            }

            if (stack.getCount() > 1) {
                text = text.copy().append(Text.of(" x" + stack.getCount()).copy().setStyle(Style.EMPTY.withColor(Aporia.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB())));
                ;
            }




                Font font = Fonts.MEDIUM.getFont(8 * scale.getCurrent());
            posY += (font.height() / 2.0f) + 12 + 2;



            float textX = centerX - (font.width(text) / 2.0f);

            Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();

            ColorRGBA rectColor = Aporia.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())
                    ? theme.getColor().withAlpha(theme.getForegroundLight().getAlpha())
                    : theme.getForegroundLight();

            float actualTextWidth = font.width(text) * scale.getCurrent();
            float rectWidth = actualTextWidth + 16 * scale.getCurrent();
            float rectHeight = 12 * scale.getCurrent();
            float rectX = textX - 8 * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, (float) posY, rectWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX, (float) posY, rectWidth, rectHeight, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, (float) posY, rectWidth, rectHeight, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            context.pushMatrix();
            context.getMatrices().translate(textX, (float) posY + 3 * scale.getCurrent());
            context.drawText(font, text, 0, 0);
            context.popMatrix();
        }
    }
    private void renderNameTag(CustomDrawContext context, PlayerEntity player, float tickDelta) {
        if (player == mc.player && mc.options.getPerspective().isFirstPerson()) return;

        double x = interpolate(player.lastX, player.getX(), tickDelta);
        double y = interpolate(player.lastY, player.getY(), tickDelta);
        double z = interpolate(player.lastZ, player.getZ(), tickDelta);
        Vec3d projected = ProjectionUtil.worldSpaceToScreenSpace(new Vec3d(x, y + getPlayerHeight(player), z));

        if (projected.z <= 0 || projected.z >= 1) return;
        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();

        ColorRGBA rectColor = Aporia.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())
                ? theme.getColor().withAlpha(theme.getForegroundLight().getAlpha())
                : theme.getForegroundLight();
        float posX = 0;
        float posY = 0;
        float textWidth = 0;
        if (mojo.isSelected()) {
            Text playerName = (player == mc.player ||
                    (NameProtect.getCustomName() != null && Aporia.getInstance().getFriendManager().isFriend(player.getNameForScoreboard())))
                    ? Text.of(NameProtect.getCustomName(player.getNameForScoreboard()))
                    : player.getDisplayName();
            if (playerName == null) return;

            playerName = playerName.getString().contains(player.getGameProfile().getName()) ? TextUtil.truncateAfterSubstring(playerName, player.getGameProfile().getName(), false) : TextUtil.truncateAfterSecondSpace(playerName, false);

            playerName = playerName.copy().append(Text.of("  " + PlayerIntersectionUtil.getHealthString(player)).copy().getWithStyle(Style.EMPTY.withColor(getHealthColorRGBA(PlayerIntersectionUtil.getHealth(player)).getRGB())).get(0));


            textWidth = mc.textRenderer.getWidth(playerName);
            posX = (float) projected.x - textWidth / 2;
            posY = (float) projected.y - getPlayerHeight(player);

            context.pushMatrix();
            context.getMatrices().translate(posX + textWidth / 2, posY + 6.5f);
            context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent());
            context.getMatrices().translate(-posX - textWidth / 2, -posY - 6.5f);


            float actualTextWidth = mc.textRenderer.getWidth(playerName) * scale.getCurrent();
            float rectWidth = actualTextWidth + 12 * scale.getCurrent();
            float rectHeight = 12 * scale.getCurrent();
            float rectX = posX - 6 * scale.getCurrent();
            float rectY = posY - 13f * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, rectY, rectWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, rectY, rectWidth, rectHeight, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            
            context.pushMatrix();
            context.getMatrices().translate(posX, posY - 13f * scale.getCurrent() + 2 * scale.getCurrent());
            context.drawText(Fonts.MEDIUM.getFont(10 * scale.getCurrent()), Text.of(playerName), 0, 0);

            context.popMatrix();
        } else {
            Text playerName = (player == mc.player ||
                    (NameProtect.getCustomName() != null && Aporia.getInstance().getFriendManager().isFriend(player.getNameForScoreboard())))
                    ? Text.of(NameProtect.getCustomName(player.getGameProfile().getName()))
                    : player.getDisplayName();
            if (playerName == null) return;
            playerName = playerName.getString().contains(player.getGameProfile().getName()) ? TextUtil.truncateAfterSubstring(playerName, player.getGameProfile().getName(), false) : TextUtil.truncateAfterSecondSpace(playerName, false);
            Text finalText = Text.of("");
            for (Text text: playerName.getSiblings()){

                finalText =finalText.copy().append(text.copy().setStyle(text.getStyle().getColor()==null?Style.EMPTY.withColor(theme.getWhite().getRGB()):text.getStyle()));
            }
            playerName = finalText;
            float hp = PlayerIntersectionUtil.getHealth(player);
            String health = PlayerIntersectionUtil.getHealthString(hp);
            Font font = Fonts.MEDIUM.getFont(8 * scale.getCurrent());

            textWidth = font.width(playerName) + 8 + font.width(health);
            posX = (float) projected.x - textWidth / 2;
            posY = (float) projected.y - getPlayerHeight(player);

            context.pushMatrix();
            context.getMatrices().translate(posX + textWidth / 2, posY + 6.5f);
            context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent());
            context.getMatrices().translate(-posX - textWidth / 2, -posY - 6.5f);


            float actualNameWidth = font.width(playerName) * scale.getCurrent();
            float actualHealthWidth = font.width(health) * scale.getCurrent();
            float actualTotalWidth = actualNameWidth + 8 * scale.getCurrent() + actualHealthWidth;
            float rectWidth2 = actualTotalWidth + 16 * scale.getCurrent();
            float rectHeight2 = 12 * scale.getCurrent();
            float rectX2 = posX - 8 * scale.getCurrent();
            float rectY2 = posY - 13f * scale.getCurrent();
            
            DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX2, rectY2, rectWidth2, rectHeight2, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());

            context.drawRoundedRect(rectX2, rectY2, rectWidth2, rectHeight2, BorderRadius.all(2), rectColor);
            DrawUtil.drawRoundedCorner(context.getMatrices(), rectX2, rectY2, rectWidth2, rectHeight2, 0.1f, 9, theme.getColor(), BorderRadius.all(2));

            
            context.pushMatrix();
            context.getMatrices().translate(posX, posY - 13f * scale.getCurrent() + 3 * scale.getCurrent());
            context.drawText(font, playerName, 0, 0);
            context.drawText(font, health, actualNameWidth + 8 * scale.getCurrent(), 0, getHealthColorRGBA(hp));

            context.popMatrix();
        }

        


        if ( this.elements.isEnable(2)) {
            ArrayList<ItemStack> stacks = new ArrayList<>();
            stacks.add(player.getOffHandStack());
            stacks.addAll(List.of(
                    player.getEquippedStack(EquipmentSlot.HEAD),
                    player.getEquippedStack(EquipmentSlot.CHEST),
                    player.getEquippedStack(EquipmentSlot.LEGS),
                    player.getEquippedStack(EquipmentSlot.FEET)
            ));
            stacks.add(player.getMainHandStack());

            int nonEmpty = (int) stacks.stream().filter(stack -> !stack.isEmpty()).count();
            if (nonEmpty > 0) {
                float totalWidth = nonEmpty * 18 * scale.getCurrent();
                float centerX = posX + textWidth / 2;
                float rectX = centerX - totalWidth / 2 - 4 * scale.getCurrent();
                float rectY = posY - 35 * scale.getCurrent();
                float rectHeight = 18 * scale.getCurrent();
                
                DrawUtil.drawBlurHudBooleanCheck(context.getMatrices(), rectX, rectY, totalWidth, rectHeight, 22, BorderRadius.all(2), ColorRGBA.WHITE, blur.isEnabled() && blur.isVisible(), glow.isEnabled() && glow.isVisible());
                context.drawRoundedRect(rectX, rectY, totalWidth, rectHeight, BorderRadius.all(2), rectColor);

                DrawUtil.drawRoundedCorner(context.getMatrices(), rectX, rectY, totalWidth, rectHeight, 0.1f, 10f, theme.getColor(), BorderRadius.all(2));

                float currentOffset = 0;
                for (ItemStack stack : stacks) {
                    if (!stack.isEmpty()) {
                        context.pushMatrix();
                        context.getMatrices().translate(centerX - totalWidth / 2 + currentOffset, rectY);
                        context.getMatrices().scale(scale.getCurrent(), scale.getCurrent());
                        context.drawItem(stack, 0, 1);
                        context.popMatrix();
                        currentOffset += 18 * scale.getCurrent();
                    }
                }
            }
        }



        context.popMatrix();
    }

    private void renderItemESP(CustomDrawContext context, ItemEntity ent, float tickDelta) {

        Vec3d[] corners = getPoints(ent, tickDelta);
        Vector4d screenBB = null;

        for (Vec3d v : corners) {
            Vec3d p = ProjectionUtil.worldSpaceToScreenSpace(v);
            if (p.z > 0 && p.z < 1) {
                if (screenBB == null) screenBB = new Vector4d(p.x, p.y, p.x, p.y);
                screenBB.x = Math.min(screenBB.x, p.x);
                screenBB.y = Math.min(screenBB.y, p.y);
                screenBB.z = Math.max(screenBB.z, p.x);
                screenBB.w = Math.max(screenBB.w, p.y);
            }
        }
        if (screenBB == null) return;


        Text label = ent.getStack().getName().copy();
        if(label.getSiblings().isEmpty()) {
            label = label.copy().setStyle(Style.EMPTY.withColor(Aporia.getInstance().getThemeManager().getCurrentTheme().getWhite().getRGB()));
        }
        if (!label.getString().isEmpty() && label.getString().charAt(label.getString().length() - 1) == ' ') {
            label = TextUtil.replaceLastChar(label, "");

        }

        if (ent.getStack().getCount() > 1) {
            label = label.copy().append(Text.of(" x" + ent.getStack().getCount()).copy().setStyle(Style.EMPTY.withColor(Aporia.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB())));
            ;
        }


        Theme theme = Aporia.getInstance().getThemeManager().getCurrentTheme();


        Font font = Fonts.MEDIUM.getFont(10 * scale.getCurrent());

        float padX = 8f * scale.getCurrent();
        float padY = 12f * scale.getCurrent();
        float textW = font.width(label) * scale.getCurrent();
        float rectW = textW + padX * 2 + 16 * 0.7f * scale.getCurrent() + padX;
        float textH = 10f * scale.getCurrent();


        float boxMinX = (float) screenBB.x;
        float boxMaxX = (float) screenBB.z;
        float boxMinY = (float) screenBB.y;

        float posX = (boxMinX + (boxMaxX - boxMinX) / 2f) - (rectW / 2f);
        float posY = boxMinY - 13f;

        float rectX = posX - padX;
        float rectY = posY;

        float rectH = padY;


        context.pushMatrix();
        context.getMatrices().translate(rectX + rectW / 2f, rectY + 6.5f);
        context.getMatrices().scale(0.9f * scale.getCurrent(), 0.9f * scale.getCurrent());
        context.getMatrices().translate(-(rectX + rectW / 2f), -(rectY + 6.5f));


        DrawUtil.drawBlurHudBooleanCheck(
                context.getMatrices(),
                rectX, rectY,
                rectW, rectH,
                22,
                BorderRadius.all(2),
                ColorRGBA.WHITE,
                blur.isEnabled() && blur.isVisible(),
                glow.isEnabled() && glow.isVisible()
        );

        ColorRGBA rectColor = theme.getForegroundLight();
        context.drawRoundedRect(rectX, rectY, rectW, rectH, BorderRadius.all(2), rectColor);
        DrawUtil.drawRoundedCorner(
                context.getMatrices(),
                rectX, rectY,
                rectW, rectH,
                0.1f, 9f,
                theme.getColor(),
                BorderRadius.all(2)
        );


        context.pushMatrix();
        context.getMatrices().translate(posX + 16 * 0.7f * scale.getCurrent() + 8 * scale.getCurrent(), rectY + 2f * scale.getCurrent());
        context.drawText(font, label, 0, 0);
        context.popMatrix();
        float itemX = rectX + padX;
        float itemY = rectY;
        context.pushMatrix();
        context.getMatrices().translate(itemX, itemY);
        context.getMatrices().scale(0.7f * scale.getCurrent(), 0.7f * scale.getCurrent());
        context.drawItem(ent.getStack(), 0, 0);

        context.popMatrix();
        context.popMatrix();
    }

    private void renderEntityBox(CustomDrawContext context, Entity entity, float tickDelta) {
        if (entity == mc.player && mc.options.getPerspective().isFirstPerson()) return;
        Vector4d vec4d = ProjectionUtil.getVector4D(entity);
        if (ProjectionUtil.canSee(vec4d)) return;
        drawFlatBox((entity instanceof PlayerEntity player && Aporia.getInstance().getFriendManager().isFriend(player.getGameProfile().getName())), vec4d);

    }

    private void drawFlatBox(boolean friend, Vector4d vec) {
        int client1 = !friend ? Aporia.getInstance().getThemeManager().getClientColor(0).getRGB() : Aporia.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client2 = !friend ? Aporia.getInstance().getThemeManager().getClientColor(90).getRGB() : Aporia.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client3 = !friend ? Aporia.getInstance().getThemeManager().getClientColor(180).getRGB() : Aporia.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();
        int client4 = !friend ? Aporia.getInstance().getThemeManager().getClientColor(270).getRGB() : Aporia.getInstance().getThemeManager().getCurrentTheme().getFriendColor().getRGB();

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float baseSize = (endPosX - posX) / 3;
        float size = baseSize * scale.getCurrent();
        float thickness = 2f * scale.getCurrent();
        
        float offsetX = (baseSize - size) / 2;
        float offsetY = (baseSize - size) / 2;

        Render2DUtil.drawQuad(posX - thickness + offsetX, posY - thickness + offsetY, size, thickness, client1);
        Render2DUtil.drawQuad(posX - thickness + offsetX, posY + offsetY, thickness, size + thickness, client1);

        Render2DUtil.drawQuad(posX - thickness + offsetX, endPosY - size - thickness + offsetY, thickness, size, client2);
        Render2DUtil.drawQuad(posX - thickness + offsetX, endPosY - thickness + offsetY, thickness, thickness, client2);

        Render2DUtil.drawQuad(endPosX - size + thickness * 2 + offsetX, posY - thickness + offsetY, size, thickness, client3);
        Render2DUtil.drawQuad(endPosX + thickness + offsetX, posY + offsetY, thickness, size + thickness, client3);

        Render2DUtil.drawQuad(endPosX + thickness + offsetX, endPosY - size - thickness + offsetY, thickness, size, client4);
        Render2DUtil.drawQuad(endPosX - size + thickness * 2 + offsetX, endPosY - thickness + offsetY, thickness, thickness, client4);
    }

    private float getPlayerHeight(PlayerEntity player) {
        return (float) player.getBoundingBox().getLengthY() + 0.2f;
    }

    private double interpolate(double prev, double current, float delta) {
        return prev + (current - prev) * delta;
    }


    private Vec3d[] getPoints(Entity entity, float tickDelta) {
        double x = entity.lastX + (entity.getX() - entity.lastX) * tickDelta;
        double y = entity.lastY + (entity.getY() - entity.lastY) * tickDelta;
        double z = entity.lastZ + (entity.getZ() - entity.lastZ) * tickDelta;

        Box box = entity.getBoundingBox();
        Box shifted = new Box(
                box.minX - entity.getX() + x - 0.05,
                box.minY - entity.getY() + y,
                box.minZ - entity.getZ() + z - 0.05,
                box.maxX - entity.getX() + x + 0.05,
                box.maxY - entity.getY() + y + 0.15,
                box.maxZ - entity.getZ() + z + 0.05
        );

        return new Vec3d[]{
                new Vec3d(shifted.minX, shifted.minY, shifted.minZ),
                new Vec3d(shifted.minX, shifted.maxY, shifted.minZ),
                new Vec3d(shifted.maxX, shifted.minY, shifted.minZ),
                new Vec3d(shifted.maxX, shifted.maxY, shifted.minZ),
                new Vec3d(shifted.minX, shifted.minY, shifted.maxZ),
                new Vec3d(shifted.minX, shifted.maxY, shifted.maxZ),
                new Vec3d(shifted.maxX, shifted.minY, shifted.maxZ),
                new Vec3d(shifted.maxX, shifted.maxY, shifted.maxZ)
        };
    }

    private ColorRGBA getHealthColorRGBA(float health) {
        if (health <= 7) return new ColorRGBA(255, 0, 0, 255);
        if (health <= 15) return new ColorRGBA(255, 255, 0, 255);
        return new ColorRGBA(0, 255, 0, 255);

    }


}


