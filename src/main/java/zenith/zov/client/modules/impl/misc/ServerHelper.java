package zenith.zov.client.modules.impl.misc;


import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.BlockState;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.*;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import zenith.zov.Zenith;
import zenith.zov.base.events.impl.input.EventKey;
import zenith.zov.base.events.impl.render.EventRender2D;
import zenith.zov.base.events.impl.render.EventRender3D;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.client.modules.api.Category;
import zenith.zov.client.modules.api.Module;
import zenith.zov.client.modules.api.ModuleAnnotation;
import zenith.zov.client.modules.api.setting.Setting;
import zenith.zov.client.modules.api.setting.impl.BooleanSetting;
import zenith.zov.client.modules.api.setting.impl.KeySetting;
import zenith.zov.client.modules.impl.render.Predictions;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.game.player.PlayerInventoryUtil;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.math.ProjectionUtil;
import zenith.zov.utility.other.BooleanSettable;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.base.color.ColorUtil;
import zenith.zov.utility.render.display.shader.DrawUtil;
import zenith.zov.utility.render.level.Render3DUtil;


import java.util.*;
import java.util.List;

@ModuleAnnotation(name = "ServerHelper", category = Category.MISC, description = "")
public final class ServerHelper extends Module {

    private final Map<BlockPos, BlockState> blockStateMap = new HashMap<>();
    private final List<ServerEvent> serverEvents = new ArrayList<>();
    private final List<Structure> structures = new ArrayList<>();
    private final List<KeyBind> keyBindings = new ArrayList<>();
    

    private final Map<BlockPos, Boolean> trapCache = new HashMap<>();
    private final Map<BlockPos, Boolean> bigTrapCache = new HashMap<>();
    private static final long CACHE_DURATION = 1000;
    private final Map<BlockPos, Long> trapCacheTime = new HashMap<>();
    private final Map<BlockPos, Long> bigTrapCacheTime = new HashMap<>();

    public static final ServerHelper INSTANCE = new ServerHelper();

    private ServerHelper() {
        initialize();
    }

    private final BooleanSetting consumablesSetting = new BooleanSetting("Таймер расходников", true,()->Zenith.getInstance().getServerHandler().isCopyTime()||Zenith.getInstance().getServerHandler().isHolyWorld());

    private final BooleanSetting autoPointSetting = new BooleanSetting("Авто точка", true, Zenith.getInstance().getServerHandler()::isFunTime);


    public void initialize() {


        keyBindings.add(new KeyBind(Items.FIREWORK_STAR,
                new KeySetting("Анти флай", Zenith.getInstance().getServerHandler()::isReallyWorld),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.FLOWER_BANNER_PATTERN,
                new KeySetting("Опыт прокрутки", Zenith.getInstance().getServerHandler()::isReallyWorld),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.PRISMARINE_SHARD,
                new KeySetting("Взрывная трапка", Zenith.getInstance().getServerHandler()::isHolyWorld),
                5, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.POPPED_CHORUS_FRUIT,
                new KeySetting("Обыч трапка", Zenith.getInstance().getServerHandler()::isHolyWorld),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.NETHER_STAR,
                new KeySetting("Стан", Zenith.getInstance().getServerHandler()::isHolyWorld),
                30, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.FIRE_CHARGE,
                new KeySetting("Взрывная штука", Zenith.getInstance().getServerHandler()::isHolyWorld),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.SNOWBALL,
                new KeySetting("Снежок", () -> Zenith.getInstance().getServerHandler().isCopyTime()
                        || Zenith.getInstance().getServerHandler().isHolyWorld()),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.PHANTOM_MEMBRANE,
                new KeySetting("Божья аура", Zenith.getInstance().getServerHandler()::isCopyTime),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.NETHERITE_SCRAP,
                new KeySetting("Трапка", Zenith.getInstance().getServerHandler()::isCopyTime),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.DRIED_KELP,
                new KeySetting("Пласт", Zenith.getInstance().getServerHandler()::isCopyTime),
                0, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.SUGAR,
                new KeySetting("Явная пыль", Zenith.getInstance().getServerHandler()::isCopyTime),
                10, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.FIRE_CHARGE,
                new KeySetting("Огненный смерч", Zenith.getInstance().getServerHandler()::isCopyTime),
                10, new BooleanSettable()));

        keyBindings.add(new KeyBind(Items.ENDER_EYE,
                new KeySetting("Дезорент", Zenith.getInstance().getServerHandler()::isCopyTime),
                10, new BooleanSettable()));


    }


    @Override
    public List<Setting> getSettings() {
        ArrayList<Setting> settings = new ArrayList<>(List.of(consumablesSetting, autoPointSetting));
        settings.addAll(keyBindings.stream().map(KeyBind::setting).toList());
        return settings;
    }


    @EventTarget
    public void onKey(EventKey e) {
        if (e.getAction() == GLFW.GLFW_RELEASE) {
            keyBindings.stream().filter(bind -> e.is(bind.setting.getKeyCode()) && bind.setting.getVisible().get()).forEach(bind -> {
                if ( mc.currentScreen == null) PlayerInventoryUtil.swapAndUse(bind.item);

                bind.draw.setValue(false);
            });
            return;
        }
        if (mc.currentScreen != null) return;

        keyBindings.stream().filter(bind -> e.is(bind.setting.getKeyCode()) && bind.setting.getVisible().get() && PlayerInventoryUtil.getSlot(bind.item, slot -> slot.getStack().get(DataComponentTypes.CUSTOM_DATA) != null) != null).forEach(bind -> bind.draw.setValue(true));


    }


    @EventTarget
    public void onPacket(EventPacket e) {

        if(this.consumablesSetting.isEnabled()){
            if ( Zenith.getInstance().getServerHandler().isCopyTime()&&e.getPacket() instanceof ChunkDeltaUpdateS2CPacket chunkDelta) {
                chunkDelta.visitUpdates((pos, state) -> blockStateMap.put(pos.add(0, 0, 0), state));
                chunkDelta.visitUpdates((pos, state) -> {
                    Vec3d vec = pos.add(0, 0, 0).toCenterPos();
                    if (blockStateMap.size() > 50 && blockStateMap.size() < 600) {
                        if (isTrap(pos.up(2)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 15000);
                        else if (isBigTrap(pos.up(3)))
                            addStructure(Items.NETHERITE_SCRAP, vec, System.currentTimeMillis() + 30000);
                    }
                });
            }
            if (e.getPacket() instanceof PlaySoundS2CPacket soundS2CPacket) {
                if (Zenith.getInstance().getServerHandler().isHolyWorld() && soundS2CPacket.getSound().toString().equals("Reference{ResourceKey[minecraft:sound_event / minecraft:block.beacon.deactivate]=SoundEvent[location=minecraft:block.beacon.deactivate, fixedRange=Optional.empty]}")) {
                    addStructure(Items.NETHER_STAR, new Vec3d(soundS2CPacket.getX(), soundS2CPacket.getY(), soundS2CPacket.getZ()), System.currentTimeMillis() + 15000);
                }

            }
            if (Zenith.getInstance().getServerHandler().isHolyWorld() && e.getPacket() instanceof ParticleS2CPacket particleS2CPacket) {


                String particleType = particleS2CPacket.getParameters().getType().toString();

                if (particleType.contains("ExplosionSmokeParticle")) {
                    addStructure(Items.PRISMARINE_SHARD, new Vec3d(particleS2CPacket.getX(), particleS2CPacket.getY(), particleS2CPacket.getZ()), System.currentTimeMillis() + 11000);

                }
            }
        }
        if (e.getPacket() instanceof GameMessageS2CPacket gameMessage && autoPointSetting.isEnabled() && autoPointSetting.getVisible().get()) {
            Text content = gameMessage.content();
            String contentString = content.toString();
            String message = content.getString();
            String name = StringUtils.substringBetween(message, "|||   [", "]   ");
            if (name != null) {
                String position = StringUtils.substringBetween(contentString, "value='/gps ", "'");
                String lvl = StringUtils.substringBetween(message, "Уровень лута: ", "\n ║");
                String owner = StringUtils.substringBetween(message, "Призван игроком: ", "\n ║");
                if (position != null) {
                    String[] pose = position.split(" ");
                    Vec3d center = BlockPos.ofFloored(Integer.parseInt(pose[0]), Integer.parseInt(pose[1]), Integer.parseInt(pose[2])).toCenterPos();
                    switch (name) {
                        case "Мистический сундук" -> addEvent(name, lvl, owner, center, "overworld", 300, 0);
                        case "Вулкан" -> addEvent(name, lvl, owner, center, "overworld", 300, 120);
                        case "Метеоритный дождь", "Маяк убийца", "Мистический Алтарь" ->
                                addEvent(name, lvl, owner, center, "overworld", 360, 0);
                        case "Загадочный маяк" -> addEvent(name, lvl, owner, center, "overworld", 60, 180);
                    }
                } else {
                    switch (name) {
                        case "Сундук смерти" ->
                                addEvent(name, lvl, owner, BlockPos.ofFloored(-155, 64, 205).toCenterPos(), "lobby", 300, 0);
                        case "Адская резня" ->
                                addEvent(name, lvl, owner, BlockPos.ofFloored(48, 87, 73).toCenterPos(), "lobby", 180, 120);
                    }
                }
            }
        }
    }


    @EventTarget
    public void onWorldRender(EventRender3D e) {
        long currentTime = System.currentTimeMillis();
        if (currentTime % 5000 < 16) {
            cleanCache();
        }
        
        MatrixStack matrix = e.getMatrix();
        keyBindings.stream().filter(bind -> bind.draw.isValue()).forEach(bind -> {
            BlockPos playerPos = mc.player.getBlockPos();
            Vec3d smooth = MathUtil.interpolate(Vec3d.of(BlockPos.ofFloored(mc.player.lastX, mc.player.lastY, mc.player.lastZ)), Vec3d.of(playerPos)).subtract(Vec3d.of(playerPos));
            switch (bind.setting.getName()) {
                case "Трапка", "Обыч трапка" ->
                        drawItemCube(playerPos, smooth, 1.99F, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB());
                case "Дезорент", "Огненный смерч", "Явная пыль" ->
                        drawItemRadius(matrix, bind.distance, ColorUtil.LIGHT_RED);
                case "Взрывная штука" ->
                        drawItemRadius(matrix, 5, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB());
                case "Пласт" -> {
                    float yaw = MathHelper.wrapDegrees(mc.player.getYaw());
                    if (Math.abs(mc.player.getPitch()) > 60) {
                        BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
                        Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -157.5F || yaw >= 157.5F) {
                        BlockPos blockPos = playerPos.north(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -112.5F) {
                        drawSidePlast(playerPos.east(5).south().down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), -1, true);
                    } else if (yaw <= -67.5F) {
                        BlockPos blockPos = playerPos.east(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= -22.5F) {
                        drawSidePlast(playerPos.east(5).down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 1, false);
                    } else if (yaw >= -22.5 && yaw <= 22.5) {
                        BlockPos blockPos = playerPos.south(2).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= 67.5F) {
                        drawSidePlast(playerPos.west(4).down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 1, true);
                    } else if (yaw <= 112.5F) {
                        BlockPos blockPos = playerPos.west(3).up();
                        Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
                        Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
                        Render3DUtil.drawBox(new Box(pos1, pos2), Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), 3, true, true, true);
                    } else if (yaw <= 157.5F) {
                        drawSidePlast(playerPos.west(4).south().down(), smooth, Zenith.getInstance().getThemeManager().getClientColor(90).getRGB(), -1, false);
                    }
                }
                case "Взрывная трапка" -> drawItemCube(playerPos, smooth, 3.99F, ColorUtil.LIGHT_RED);
                case "Стан" -> drawItemCube(playerPos, smooth, 15.01F, ColorUtil.LIGHT_RED);
                case "Снежок" ->
                        Predictions.INSTANCE.drawPredictionInHand(matrix, List.of(Items.SNOWBALL.getDefaultStack()));
            }
        });
    }

    @EventTarget
    public void onDraw(EventRender2D e) {
        DrawContext context = e.getContext();

        structures.forEach(cons -> {
            double time = (cons.time - System.currentTimeMillis()) / 1000;
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(cons.vec);

            String text = MathUtil.round(time, 0.1F) + "с";
            Font font = Fonts.MEDIUM.getFont(10);
            float width = font.width(text);
            float posX = (float) (vec3d.x - width / 2);
            float posY = (float) vec3d.y;
            float padding = 2;

            if (ProjectionUtil.canSee(cons.vec) && cons.anarchy == Zenith.getInstance().getServerHandler().getAnarchy() && Zenith.getInstance().getServerHandler().getWorldType().equals(cons.world)) {
//                blur.render(ShapeProperties.create(matrix, posX - padding, posY - padding, width + padding * 2, 10)
//                        .round(1.5F).color(ColorUtil.HALF_BLACK).build());
                DrawUtil.drawBlurHud(e.getContext().getMatrices(),posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8,22,BorderRadius.all(4), ColorRGBA.WHITE);
                e.getContext().drawRoundedRect(posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8, BorderRadius.all(4), Zenith.getInstance().getThemeManager().getCurrentTheme().getForegroundLight());
                DrawUtil.drawRoundedCorner(e.getContext().getMatrices(),posX - 4, posY - 4, (16 * 0.8f + 4 + font.width(text) + 8), 16 * 0.8f + 8,0.1f,10,Zenith.getInstance().getThemeManager().getCurrentTheme().getColor(),BorderRadius.all(4));

                e.getContext().drawText(font, text, posX + 16 * 0.8f + 4, posY + 2.5f, Zenith.getInstance().getThemeManager().getCurrentTheme().getColor());

                e.getContext().getMatrices().pushMatrix();
                e.getContext().getMatrices().translate(posX, posY);
                e.getContext().getMatrices().scale(0.8f, 0.8f);
                e.getContext().drawItem(cons.item.getDefaultStack(), 0, 0);
                e.getContext().getMatrices().popMatrix();
            }
        });
        serverEvents.forEach(event -> {
            Vec3d vec3d = ProjectionUtil.worldSpaceToScreenSpace(event.vec);

            double timeOpen = (event.timeOpen - System.currentTimeMillis()) / 1000;
            double timeEnd = (event.timeEnd - System.currentTimeMillis()) / 1000;
            String distance = " [" + MathUtil.round(mc.getEntityRenderDispatcher().camera.getPos().distanceTo(event.vec), 0.1) + "m" + "]";
            String time = timeOpen > 0 ? ("До начала: " + MathUtil.round(timeOpen, timeOpen < 30 ? 0.1F : 1) + "с").replace(".0", "")
                    : timeEnd > 0 ? ("До конца: " + MathUtil.round(timeEnd, timeEnd < 30 ? 0.1F : 1) + "с").replace(".0", "")
                    : "Конец ивента!";

            if (ProjectionUtil.canSee(event.vec) && event.anarchy == Zenith.getInstance().getServerHandler().getAnarchy() && Zenith.getInstance().getServerHandler().getWorldType().equals(event.world)) {
                List<String> list = new ArrayList<>(Collections.singletonList(event.name + distance));
                if (event.owner != null) list.add("Призван: " + Formatting.GOLD + event.owner);
                list.add(time);
                if (event.lvl != null) list.add(event.lvl);
                //    draw(matrix, Fonts.getSize(14), list, vec3d);
            }
        });
        structures.removeIf(cons -> cons.time - System.currentTimeMillis() <= 0);
        serverEvents.removeIf(event -> event.timeEnd + 90000 - System.currentTimeMillis() <= 0);

    }

    private void drawItemCube(BlockPos playerPos, Vec3d smooth, float size, int color) {
        Box box = new Box(playerPos.up()).offset(smooth).contract(0, 0.2f, 0).expand(size);
        boolean inBox = mc.world.getPlayers().stream().anyMatch(ent -> ent != mc.player && box.intersects(ent.getBoundingBox()) && !Zenith.getInstance().getFriendManager().isFriend(ent.getGameProfile().getName()));
        Render3DUtil.drawBox(box, inBox ? Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() : color, 3, true, true, true);
    }

    private void drawItemRadius(MatrixStack matrix, float distance, int clr) {
        float playerHalfWidth = mc.player.getWidth() / 2;
        int color = validDistance(distance) ? Zenith.getInstance().getThemeManager().getCurrentTheme().getColor().getRGB() : clr;

        Vec3d pos = MathUtil.interpolate(mc.player).add(playerHalfWidth, 0.02, playerHalfWidth);
        Vec3d vec3d = pos.subtract(mc.getEntityRenderDispatcher().camera.getPos());
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Vec3d nextCosSin = MathUtil.cosSin(i + 1, size, distance);
            Render3DUtil.drawLine(vec3d.add(cosSin), vec3d.add(cosSin.x, cosSin.y + 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0), 1, true);
            Render3DUtil.drawLine(pos.add(cosSin), pos.add(nextCosSin), color, 2, true);
        }
        for (int i = 0, size = 90; i <= size; i++) {
            Vec3d cosSin = MathUtil.cosSin(i, size, distance);
            Render3DUtil.drawLine(vec3d.add(cosSin), vec3d.add(cosSin.x, cosSin.y - 2, cosSin.z), ColorUtil.multAlpha(color, 0.2F), ColorUtil.multAlpha(color, 0), 1, true);
        }
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
    }

    private void draw(MatrixStack matrix, Font font, List<String> list, Vec3d vec3d) {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            String string = list.get(i);
            float width = font.width(string);
            float posX = (float) (vec3d.x - width / 2);

            offsetY += 10;
        }
    }

    public void drawSidePlast(BlockPos blockPos, Vec3d smooth, int color, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        float width = 2;
        int quadColor = ColorUtil.multAlpha(color, 0.15F);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawHorizontalLines(vec3d, color, width, i, ff);
        drawVerticalLines(vec3d, color, width, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawHorizontalQuads(vec3d, quadColor, i, ff);
        drawVerticalQuads(vec3d, quadColor, i, ff);
    }

    private void drawHorizontalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Vec3d current = vec3d;
        
        Render3DUtil.drawLine(current, current = current.add(x, 0, 0), color, width, true);
        
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(current, current = current.add(0, 0, i), color, width, true);
            Render3DUtil.drawLine(current, current = current.add(x, 0, 0), color, width, true);
        }
        
        Render3DUtil.drawLine(current, current = current.add(0, 0, i), color, width, true);
        Render3DUtil.drawLine(current, current = current.add(x * -2, 0, 0), color, width, true);
        
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(current, current = current.add(0, 0, i * -1), color, width, true);
            Render3DUtil.drawLine(current, current = current.add(x * -1, 0, 0), color, width, true);
        }
        
        Render3DUtil.drawLine(current, current.add(0, 0, i * -2), color, width, true);
    }

    private void drawVerticalLines(Vec3d vec3d, int color, float width, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawLine(vec3d, vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x, 0, i), vec3d.add(0, 5, 0), color, width, true);
        }
        Render3DUtil.drawLine(vec3d = vec3d.add(0, 0, i), vec3d.add(0, 5, 0), color, width, true);
        Render3DUtil.drawLine(vec3d = vec3d.add(x * -2, 0, 0), vec3d.add(0, 5, 0), color, width, true);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawLine(vec3d = vec3d.add(x * -1, 0, i * -1), vec3d.add(0, 5, 0), color, width, true);
        }
    }

    private void drawHorizontalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        vec3d = vec3d.add(0, 1e-3, 0);
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        for (int f = 0; f < 3; f++)
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i * 2), vec3d.add(0, 0, i * 2), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 0, i), vec3d.add(0, 0, i), color, true);
    }

    private void drawVerticalQuads(Vec3d vec3d, int color, int i, boolean ff) {
        float x = ff ? i : -i;
        Render3DUtil.drawQuad(vec3d, vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        for (int f = 0; f < 4; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x, 0, 0), vec3d.add(x, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x, 0, 0), vec3d.add(0, 0, i), vec3d.add(0, 5, i), vec3d.add(0, 5, 0), color, true);
        Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i), vec3d.add(x * -2, 0, 0), vec3d.add(x * -2, 5, 0), vec3d.add(0, 5, 0), color, true);
        vec3d = vec3d.add(x * -1, 0, 0);
        for (int f = 0; f < 3; f++) {
            Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -1), vec3d.add(0, 5, i * -1), vec3d.add(0, 5, 0), color, true);
            Render3DUtil.drawQuad(vec3d = vec3d.add(0, 0, i * -1), vec3d.add(x * -1, 0, 0), vec3d.add(x * -1, 5, 0), vec3d.add(0, 5, 0), color, true);
        }
        Render3DUtil.drawQuad(vec3d = vec3d.add(x * -1, 0, 0), vec3d.add(0, 0, i * -2), vec3d.add(0, 5, i * -2), vec3d.add(0, 5, 0), color, true);
    }

    private void addEvent(String name, String lvl, String owner, Vec3d vec3d, String world, int timeOpen, int timeLoot) {
        if (serverEvents.stream().noneMatch(server -> server.vec.equals(vec3d))) {
            long open = System.currentTimeMillis() + timeOpen * 1000L;
            long loot = open + timeLoot * 1000L;
            serverEvents.add(new ServerEvent(name, lvl, owner, vec3d, world, Zenith.getInstance().getServerHandler().getAnarchy(), open, loot));
        }
    }

    private void addStructure(Item item, Vec3d vec, double time) {
        if (structures.stream().noneMatch(str -> str.vec.equals(vec))) {
            structures.add(new Structure(item, vec, Zenith.getInstance().getServerHandler().getWorldType(), Zenith.getInstance().getServerHandler().getAnarchy(), time));
        }
    }

    private Vector4f getRound(Font font, List<String> list, int i, float width) {
        if (i == 0) {
            float next = font.width(list.get(i + 1));
            return next >= width ? new Vector4f(2, 0, 2, 0) : new Vector4f(2);
        }
        if (i == list.size() - 1) {
            float prev = font.width(list.get(i - 1));
            return prev >= width ? new Vector4f(0, 2, 0, 2) : new Vector4f(2);
        }
        float prev = font.width(list.get(i - 1));
        float next = font.width(list.get(i + 1));
        return prev >= width ? next >= width ? new Vector4f() : new Vector4f(0, 2, 0, 2) : new Vector4f(2);
    }

    private boolean validDistance(float dist) {
        return dist == 0 || mc.world.getPlayers().stream().anyMatch(p -> p != mc.player && !Zenith.getInstance().getFriendManager().isFriend(p.getGameProfile().getName()) && mc.player.distanceTo(p) <= dist);
    }

    private boolean isTrap(BlockPos center) {
        long currentTime = System.currentTimeMillis();
        if (trapCacheTime.containsKey(center) && currentTime - trapCacheTime.get(center) < CACHE_DURATION) {
            return trapCache.get(center);
        }

        boolean result = checkTrap(center);
        trapCache.put(center, result);
        trapCacheTime.put(center, currentTime);
        return result;
    }

    private boolean checkTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 2)) {
            if (MathUtil.getDistance(pos.toCenterPos(), center.toCenterPos()) < 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(2).north().east()) && !pos.equals(center.up(2).north().west()) && !pos.equals(center.up(2).south().east()) && !pos.equals(center.up(2).south().west())) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    private boolean isBigTrap(BlockPos center) {
        long currentTime = System.currentTimeMillis();
        if (bigTrapCacheTime.containsKey(center) && currentTime - bigTrapCacheTime.get(center) < CACHE_DURATION) {
            return bigTrapCache.get(center);
        }

        boolean result = checkBigTrap(center);
        bigTrapCache.put(center, result);
        bigTrapCacheTime.put(center, currentTime);
        return result;
    }

    private boolean checkBigTrap(BlockPos center) {
        int inconsistencies = 0;
        for (BlockPos pos : PlayerIntersectionUtil.getCube(center, 3)) {
            if (Math.abs(pos.getX() - center.getX()) <= 2 && Math.abs(pos.getY() - center.getY()) <= 2 && Math.abs(pos.getZ() - center.getZ()) <= 2) {
                BlockState state = blockStateMap.get(pos);
                if (state != null && !state.isAir()) inconsistencies++;
            } else if (!pos.equals(center.up(3))) {
                BlockState state = blockStateMap.get(pos);
                if (state == null || state.isAir()) inconsistencies++;
            }
            if (inconsistencies > 1) return false;
        }
        return true;
    }

    private static boolean isSolid(BlockState state) {
        return state != null && !state.isAir();
    }
    
    private void cleanCache() {
        long currentTime = System.currentTimeMillis();
        
        trapCacheTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > CACHE_DURATION);
        trapCache.entrySet().removeIf(entry -> !trapCacheTime.containsKey(entry.getKey()));
        
        bigTrapCacheTime.entrySet().removeIf(entry -> currentTime - entry.getValue() > CACHE_DURATION);
        bigTrapCache.entrySet().removeIf(entry -> !bigTrapCacheTime.containsKey(entry.getKey()));
        
        if (trapCache.size() > 1000) {
            trapCache.clear();
            trapCacheTime.clear();
        }
        if (bigTrapCache.size() > 1000) {
            bigTrapCache.clear();
            bigTrapCacheTime.clear();
        }
    }

    private boolean matchesMask(BlockPos center,
                                byte[][][] mask,
                                int radius,
                                int tolerance) {
        int inconsistencies = 0;
        BlockPos.Mutable m = new BlockPos.Mutable();

        for (int dy = -radius; dy <= radius; dy++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    byte req = mask[dy + radius][dz + radius][dx + radius];
                    if (req == -1) continue;

                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState st = blockStateMap.get(m);
                    boolean solid = isSolid(st);


                    if ((req == 1 && !solid) || (req == 0 && solid)) {
                        if (++inconsistencies > tolerance) return false;
                    }
                }
            }
        }
        return true;
    }

    public record KeyBind(Item item, KeySetting setting, float distance, BooleanSettable draw) {
    }

    public record Structure(Item item, Vec3d vec, String world, int anarchy, double time) {
    }

    public record ServerEvent(String name, String lvl, String owner, Vec3d vec, String world, int anarchy,
                              double timeOpen, double timeEnd) {
    }
}


