package zenith.zov.client.hud.elements.component;

import com.mojang.authlib.GameProfile;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.game.other.TextUtil;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.*;

public class StaffComponent extends DraggableHudElement {

    private final Animation animationWidth = new Animation(200, 100, Easing.QUAD_IN_OUT);
    private final Animation animationScale = new Animation(200, 0, Easing.QUAD_IN_OUT);

    private final Animation animationVisible = new Animation(200,0,Easing.QUAD_IN_OUT);

    private final Map<String, StaffModule> modules = new LinkedHashMap<>();

    
    private final Set<String> staffPrefix = Set.of(
            "helper","ᴀдмин","moder","staff","admin","curator","стажёр", "сотрудник", "помощник", "админ", "модер"
    );

    
    private final Map<String, Identifier> skinTextureCache = new HashMap<>();
    private long lastStaffUpdate = 0, lastSkinCacheClear = 0;

    
    private final Set<String> currentStaffKeys = new HashSet<>();

    public StaffComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
    }

    @Override
    public void render(CustomDrawContext ctx) {
        long currentTime = System.currentTimeMillis();

        if (currentTime - lastStaffUpdate > 1000 && mc.getNetworkHandler() != null) {
            updateStaffList(); 
            lastStaffUpdate = currentTime;
        }

        if (currentTime - lastSkinCacheClear > 30000) {
            skinTextureCache.clear();
            lastSkinCacheClear = currentTime;
        }

        
        modules.entrySet().removeIf(entry -> entry.getValue().isDelete());

        boolean hidden = modules.size() == 1 && modules.values().iterator().next().animation.getTargetValue() == 0;
        animationScale.update(hidden ? 0 : 1);

        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
        Font iconFont = Fonts.ICONS.getFont(6);
        Font font = Fonts.MEDIUM.getFont(6);

        float x = this.x;
        float y = this.y;
        float height = (float) (18 + modules.values().stream().mapToDouble(StaffModule::getHeight).sum());

        float width = (float) modules.values().stream().mapToDouble(StaffModule::updateWidth).max().orElse(100);
        width = animationWidth.update(width);

        this.width = width;
        this.height = height;

        ctx.pushMatrix();

        float borderAnim = animationScale.getValue() * 4;
        BorderRadius radius = new BorderRadius(4, 4, borderAnim, borderAnim);
        animationVisible.update(mc.currentScreen instanceof ChatScreen || !modules.isEmpty());


        {

            ctx.getMatrices().translate(x + width / 2, y + height / 2);
            ctx.getMatrices().scale(animationVisible.getValue(), animationVisible.getValue());
            ctx.getMatrices().translate(-(x + width / 2), -(y + height / 2));

            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(x, y, width, height, radius, theme.getForegroundLight());
            ctx.drawText(iconFont, "P", x + 8, y + (18 - iconFont.height()) / 2, theme.getColor());
            ctx.drawText(iconFont, "M", x + width - 8 - iconFont.width("M"), y + (18 - iconFont.height()) / 2, theme.getWhiteGray());
            ctx.drawText(font, "Staffs", x + 8 + 8 + 2, y + (18 - font.height()) / 2, theme.getWhite());
        }
       if(animationVisible.getValue()==1) {
            float offsetY = y + 18;
            int index = 0;
            for (Map.Entry<String, StaffModule> entry : modules.entrySet()) {
                StaffModule module = entry.getValue();
                module.render(ctx, x, offsetY, width, index, currentStaffKeys.contains(entry.getKey()));
                offsetY += module.getHeight();
                index++;
            }
        }
        ctx.drawRoundedBorder(x, y, width, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());
        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, width, height, 0.1f,
                Math.min(20, Math.max(12, height / 2.5f)), theme.getColor(), BorderRadius.all(4));

        ctx.popMatrix();
    }

    
    private void updateStaffList() {
        if (mc.getNetworkHandler() == null) return;

        currentStaffKeys.clear(); 

        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            GameProfile profile = entry.getProfile();
            Text displayName = entry.getDisplayName();
            if (displayName == null || profile == null) continue;

            String display = displayName.getString();
            String name = profile.getName();

            
            String prefix = display.replace(name, "").trim();

            
            if (prefix.length() < 2) continue;
            if (!containsAnyKeyword(prefix)) continue;

            
            String key = display;

            Status status = (entry.getGameMode() == GameMode.SPECTATOR) ? Status.VANISHED : Status.NONE;
            displayName =  displayName.getString().contains( profile.getName())?TextUtil.truncateAfterSubstring(displayName, profile.getName(),false):TextUtil.truncateAfterSecondSpace(displayName,false);
            Text finalDisplayName = displayName;
            modules.computeIfAbsent(key, k -> new StaffModule(finalDisplayName, key, name, status));

            currentStaffKeys.add(key);
        }

        
        
    }


    public boolean containsAnyKeyword(String text) {
        String lower = text.toLowerCase(Locale.US);
        for (String keyword : this.staffPrefix) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    public static class Staff {
        private Text prefix;
        private String name;
        private boolean isSpec;
        private Status status;
    }

    private class StaffModule {
        private final Animation animation = new Animation(150, 0.01f, Easing.QUAD_IN_OUT);
        private final Animation animationColor = new Animation(200, Easing.QUAD_IN_OUT);

        private final Text displayNameText;
        private final String key;
        private final String name;
        private final Status status;
        private final long appearTime;

        public StaffModule(Text displayNameText, String key, String name, Status status) {
            this.displayNameText = displayNameText;
            this.key = key;
            this.name = name;
            this.status = status;
            this.appearTime = System.currentTimeMillis();
        }

        public float updateWidth() {
            float width = 100;
            Font font = Fonts.MEDIUM.getFont(6);
            String time = formatTime(System.currentTimeMillis() - appearTime);

            float leftTextWidth = 8 + 8 + 8 + font.width(displayNameText);
            float rightTextWidth = font.width(time) + 8;
            if (width - (leftTextWidth + rightTextWidth) < 8) {
                width += (leftTextWidth + rightTextWidth + 8) - width;
            }
            return width;
        }

        public float getHeight() {
            return (float) (18 * animation.getValue());
        }

        public void render(CustomDrawContext ctx, float x, float y, float width, int i, boolean present) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            Font font = Fonts.MEDIUM.getFont(6);


            animation.update(present ? 1 : 0);

            ctx.pushMatrix();
            ctx.getMatrices().translate(x + width / 2, y + 9);
            ctx.getMatrices().scale(animation.getValue(), animation.getValue());
            ctx.getMatrices().translate(-(x + width / 2), -(y + 9));

            animationColor.update(i % 2 == 0 ? 1 : 0);
            ColorRGBA background = theme.getForegroundLight().mix(theme.getForegroundColor(), animationColor.getValue());
            ctx.drawRoundedRect(x, y, width, 18, (i == modules.size() - 1) ? BorderRadius.bottom(4, 4) : BorderRadius.ZERO, background);

            Identifier skinTexture = skinTextureCache.get(name);
            if (skinTexture == null && mc.getNetworkHandler() != null) {
                PlayerListEntry player = mc.getNetworkHandler().getPlayerList().stream()
                        .filter(p -> p.getProfile() != null && name.equals(p.getProfile().getName()))
                        .findFirst().orElse(null);
                if (player != null && player.getSkinTextures() != null) {
                    skinTexture = player.getSkinTextures().texture();
                    skinTextureCache.put(name, skinTexture);
                }
            }
            if (skinTexture == null) skinTexture = DefaultSkinHelper.getSteve().texture();

            DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, x + 6, y + 6, 6, BorderRadius.all(1.6f), ColorRGBA.WHITE);
            ctx.drawText(Fonts.BOLD.getFont(8), ".", x + 8 + 8 + 2, y + 4, theme.getWhiteGray());

             ctx.drawText(font, displayNameText, x + 8 + 8 + 8, y + (18 - font.height()) / 2);

            String timeText = formatTime(System.currentTimeMillis() - appearTime);
            ctx.drawText(font, timeText, x + width - 8 - font.width(timeText), y + (18 - font.height()) / 2, theme.getColor());

            ctx.popMatrix();
        }

        public boolean isDelete() {
            return animation.getValue() == 0;
        }
    }

    public enum Status {
        NONE, VANISHED
    }

    private String formatTime(long ms) {
        long minutes = ms / 60000;
        long seconds = (ms % 60000) / 1000;
        return String.format("%d:%02d", minutes, seconds);
    }
}

