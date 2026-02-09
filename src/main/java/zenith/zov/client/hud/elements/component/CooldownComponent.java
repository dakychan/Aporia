package zenith.zov.client.hud.elements.component;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.CooldownUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.potion.Potions;
import net.minecraft.registry.Registries;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.math.MathUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class CooldownComponent extends DraggableHudElement {

    private final Map<String, CooldownModule> keyModules = new LinkedHashMap<>();
    private final Animation animationWidth = new Animation(200, 100, Easing.QUAD_IN_OUT);
    private final Animation animationScale = new Animation(200, 0, Easing.QUAD_IN_OUT);

    private final Animation animationVisible = new Animation(200,0,Easing.QUAD_IN_OUT);

    private final Map<String, TimedCooldown> customCooldowns = new LinkedHashMap<>();
    
    private final Map<String, TimedCooldown> vanillaCooldowns = new LinkedHashMap<>();

    public CooldownComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
        EventManager.register(this);
    }

    

    @EventTarget
    public void onPacket(EventPacket e) {
        if (mc == null || mc.world == null) return;

        if (e.getPacket() instanceof CooldownUpdateS2CPacket pkt) {
            Item item = Registries.ITEM.get(pkt.cooldownGroup());
            int ticks = pkt.cooldown();
            String id = "vanilla:" + item.getTranslationKey();

            if (ticks == 0) {
                vanillaCooldowns.remove(id);
                keyModules.remove(id);
                return;
            }

            long nowTicks = mc.world.getTime();
            vanillaCooldowns.put(id, TimedCooldown.fromTicks(
                    id,
                    item.getName().getString(),
                    item.getDefaultStack(),
                    nowTicks,
                    ticks
            ));
        } else if (e.getPacket() instanceof PlayerRespawnS2CPacket) {
            vanillaCooldowns.clear();
            customCooldowns.clear();
            keyModules.clear();
        }
    }

    

    @EventTarget
    public void onCustomCooldown(EventPacket e) {
        if (mc == null || mc.player == null) return;

        ItemStack itemStack = mc.player.getActiveItem();
        if (itemStack == null) return;

        if (mc.player.getItemUseTime() >= itemStack.getMaxUseTime(mc.player)) {
            PotionContentsComponent data = itemStack.get(DataComponentTypes.POTION_CONTENTS);
            if (data != null) {
                if (data.getColor() == 33461 || data.getColor() == -515037) {
                    ItemStack medikStak = Items.POTION.getDefaultStack();
                    medikStak.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                            Optional.of(Potions.SWIFTNESS), Optional.of(data.getColor()), List.of(), Optional.empty()
                    ));
                    if (data.getColor() == -515037) {
                        
                        addCustomCooldown(medikStak, "Исцел", 10_000);
                    }else {

                            addCustomCooldown(medikStak, "Исцел", 10_000);
                        
                    }
                }
            }
        }
    }

    

    
    public void addCustomCooldown(ItemStack stack, long durationMs) {
        String id = "custom:" + stack.getItem().getTranslationKey();
        addCustomCooldownReal(id, stack.getItem().getName().getString(), stack, durationMs);
    }

    
    public void addCustomCooldown(ItemStack stack, String displayName, long durationMs) {
        addCustomCooldownReal("custom:" + displayName, displayName, stack, durationMs);
    }

    
    public void addCustomCooldownReal(String id, String displayName, ItemStack iconStack, long durationMs) {
        if (mc == null) return;
        if (keyModules.containsKey(id)) return;
        long nowNs = System.nanoTime();
        customCooldowns.put(id, TimedCooldown.fromReal(
                id, displayName, iconStack, nowNs, Math.max(1L, durationMs) * 1_000_000L
        ));
    }

    
    public void addCustomCooldownTicks(String id, String displayName, ItemStack iconStack, long durationTicks) {
        if (mc == null || mc.world == null) return;
        if (keyModules.containsKey(id)) return;
        long nowTicks = mc.world.getTime();
        customCooldowns.put(id, TimedCooldown.fromTicks(
                id, displayName, iconStack, nowTicks, Math.max(1L, durationTicks)
        ));
    }

    

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc == null) return;

        Font iconFont = Fonts.ICONS.getFont(6);

        
        long nowTicks = mc != null && mc.world != null ? mc.world.getTime() : 0L;
        for (TimedCooldown tc : vanillaCooldowns.values()) {
            if (tc.isFinished(nowTicks)) continue;

            keyModules.computeIfAbsent(tc.id, k ->
                    new CooldownModule(
                            tc.icon,
                            tc.displayName,
                            tc.getProgress(nowTicks),
                            () -> tc.getProgress(mc.world != null ? mc.world.getTime() : nowTicks),
                            () -> tc.isFinished(mc.world != null ? mc.world.getTime() : nowTicks),
                            () -> tc.getRemainingSeconds(mc.world != null ? mc.world.getTime() : nowTicks)
                    )
            );
        }

        
        for (TimedCooldown tc : customCooldowns.values()) {
            if (tc.base == TimeBase.TICKS) {
                if (tc.isFinished(nowTicks)) continue;
                keyModules.computeIfAbsent(tc.id, k ->
                        new CooldownModule(
                                tc.icon,
                                tc.displayName,
                                tc.getProgress(nowTicks),
                                () -> tc.getProgress(mc.world != null ? mc.world.getTime() : nowTicks),
                                () -> tc.isFinished(mc.world != null ? mc.world.getTime() : nowTicks),
                                () -> tc.getRemainingSeconds(mc.world != null ? mc.world.getTime() : nowTicks)
                        )
                );
            } else {
                
                if (tc.isFinished(0)) continue;
                keyModules.computeIfAbsent(tc.id, k ->
                        new CooldownModule(
                                tc.icon,
                                tc.displayName,
                                tc.getProgress(0),
                                () -> tc.getProgress(0),
                                () -> tc.isFinished(0),
                                () -> tc.getRemainingSeconds(0)
                        )
                );
            }
        }

        
        if (keyModules.isEmpty()) {
            animationScale.update(0);
        } else {
            boolean singleAndHidden = keyModules.size() == 1 && keyModules.values().iterator().next().animation.getTargetValue() == 0;
            animationScale.update(singleAndHidden ? 0 : 1);
        }

        float x = this.x;
        float y = this.y;

        float height = (float) (18 + keyModules.values().stream().mapToDouble(CooldownModule::getHeight).sum());
        float width = (float) keyModules.values().stream().mapToDouble(CooldownModule::updateWidth).max().orElse(100);
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

            ctx.drawText(iconFont, "N", x + 8, y + (18 - iconFont.height()) / 2, theme.getColor());
            ctx.drawText(iconFont, "M", x + width - 8 - iconFont.width("M"), y + (18 - iconFont.height()) / 2, theme.getWhiteGray());

            Font font = Fonts.MEDIUM.getFont(6);
            ctx.drawText(font, "Cooldown", x + 8 + 8 + 2, y + (18 - font.height()) / 2, theme.getWhite());

        }
        if(animationVisible.getValue()==1){
            float kmY = y + 18;
            int i = 0;

            for (CooldownModule km : keyModules.values()) {
                km.render(ctx, x, kmY, width, i);
                kmY += km.getHeight();
                i++;
            }
        }

        
        keyModules.entrySet().removeIf(entry -> entry.getValue().isDelete());

        
        if (mc != null && mc.world != null) {
            long t = mc.world.getTime();
            vanillaCooldowns.entrySet().removeIf(e -> e.getValue().isFinished(t));
            customCooldowns.entrySet().removeIf(e -> e.getValue().base == TimeBase.TICKS ? e.getValue().isFinished(t) : e.getValue().isFinished(0));
        }

        ctx.drawRoundedBorder(x, y, width, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, height, 0.1f,
                Math.min(20, Math.max(12, height / 2.5f)), theme.getColor(), BorderRadius.all(4));
        ctx.popMatrix();

    }

    

    private class CooldownModule {
        private final Animation animation = new Animation(150, 0.01f, Easing.QUAD_IN_OUT);
        private final Animation animationColor = new Animation(200, Easing.QUAD_IN_OUT);
        private final Animation animationProgress;

        private final ItemStack iconStack;
        private final String displayName;

        private final Supplier<Float> progressSupplier;           
        private final BooleanSupplier finishedSupplier;           
        private final Supplier<Integer> remainingSecondsSupplier; 

        public CooldownModule(ItemStack iconStack,
                              String displayName,
                              float initialProgress,
                              Supplier<Float> progressSupplier,
                              BooleanSupplier finishedSupplier,
                              Supplier<Integer> remainingSecondsSupplier) {
            this.iconStack = iconStack;
            this.displayName = displayName;
            this.progressSupplier = progressSupplier;
            this.finishedSupplier = finishedSupplier;
            this.remainingSecondsSupplier = remainingSecondsSupplier;
            this.animationProgress = new Animation(200, initialProgress, Easing.LINEAR);
        }

        public float updateWidth() {
            float width = 120;

            String timeStr = formatTime(remainingSecondsSupplier.get());
            float moduleTextWidth = Fonts.MEDIUM.getWidth(displayName, 6);
            float timeTextWidth = Fonts.MEDIUM.getWidth(timeStr, 6);

            float rightPad = 8 + timeTextWidth;
            float widthText = width - (rightPad + 10 + 5 + 10);
            if (widthText < 8 + moduleTextWidth + 8) {
                float deltaWidth = moduleTextWidth + 8 + 8 - widthText;
                width += deltaWidth;
            }
            return width;
        }

        public float getHeight() {
            return (float) (18 * animation.getValue());
        }

        public void render(CustomDrawContext ctx, float x, float y, float width, int i) {
            float progress = updateProgressAndGet();

            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font font = Fonts.MEDIUM.getFont(6);

            animation.update(progress > 0 ? 1 : 0);

            ctx.pushMatrix();
            ctx.getMatrices().translate(x + width / 2, y + 18 / 2f);
            float deltaAnim = animation.getValue();
            ctx.getMatrices().scale(deltaAnim, deltaAnim);
            ctx.getMatrices().translate(-(x + width / 2), -(y + 18 / 2f));

            animationColor.update(i % 2 == 0 ? 1 : 0);
            ColorRGBA backgroundColor = theme.getForegroundLight().mix(theme.getForegroundColor(), animationColor.getValue());

            ctx.drawRoundedRect(x, y, width, 18, i == keyModules.size() - 1 ? BorderRadius.bottom(4, 4) : BorderRadius.ZERO, backgroundColor);
            {
                ctx.pushMatrix();
                ctx.getMatrices().translate(x + 6, y + (18 - 8) / 2f);
                ctx.getMatrices().scale(0.5f, 0.5f);
                ctx.drawItem(iconStack, 0, 0);
                ctx.popMatrix();
            }

            ctx.drawText(font, displayName, x + 8 + 8 + 8, y + (18 - font.height()) / 2, theme.getWhite());

            String timeStr = formatTime(remainingSecondsSupplier.get());
            float timeWidth = Fonts.MEDIUM.getWidth(timeStr, 6);
            ctx.drawText(font, timeStr, x + width - timeWidth - 8, y + (18 - font.height()) / 2, theme.getColor());

            ctx.popMatrix();
        }

        public boolean isDelete() {
            return animation.getValue() == 0 && finishedSupplier.getAsBoolean();
        }

        private float updateProgressAndGet() {
            float p = progressSupplier.get();
            animationProgress.update(p);
            return MathUtil.round(animationProgress.getValue());
        }

        private String formatTime(int totalSeconds) {
            if (totalSeconds < 0) totalSeconds = 0;
            int minutes = totalSeconds / 60;
            int seconds = totalSeconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    

    private enum TimeBase { TICKS, REAL }

    private static class TimedCooldown {
        final String id;
        final String displayName;
        final ItemStack icon;
        final TimeBase base;

        
        
        final long start;
        final long duration;

        private TimedCooldown(String id, String displayName, ItemStack icon, TimeBase base, long start, long duration) {
            this.id = id;
            this.displayName = displayName;
            this.icon = icon;
            this.base = base;
            this.start = start;
            this.duration = duration;
        }

        
        static TimedCooldown fromTicks(String id, String name, ItemStack icon, long startTicks, long durationTicks) {
            return new TimedCooldown(id, name, icon, TimeBase.TICKS, startTicks, durationTicks);
        }
        static TimedCooldown fromReal(String id, String name, ItemStack icon, long startNs, long durationNs) {
            return new TimedCooldown(id, name, icon, TimeBase.REAL, startNs, durationNs);
        }

        
        float getProgress(long nowTick) {
            long remain = switch (base) {
                case TICKS -> Math.max(0L, (start + duration) - nowTick);
                case REAL  -> Math.max(0L, (start + duration) - System.nanoTime());
            };
            return duration <= 0 ? 0f : (float) remain / (float) duration;
        }

        boolean isFinished(long nowTick) {
            return switch (base) {
                case TICKS -> nowTick >= (start + duration);
                case REAL  -> System.nanoTime() >= (start + duration);
            };
        }

        int getRemainingSeconds(long nowTick) {
            long remain = switch (base) {
                case TICKS -> Math.max(0L, (start + duration) - nowTick);            
                case REAL  -> Math.max(0L, (start + duration) - System.nanoTime());  
            };
            if (base == TimeBase.TICKS) {
                
                return (int) (remain / 20L);
            } else {
                
                return (int) (remain / 1_000_000_000L);
            }
        }
    }
}

