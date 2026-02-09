package zenith.zov.client.hud.elements.component;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.utility.math.Timer;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.render.display.Render2DUtil;
import zenith.zov.utility.render.display.Texture;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;


import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicInfoComponent extends DraggableHudElement {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 43, 150, false);
    private final Identifier artwork = Zenith.id("icons/avatarmusic.png");
    private final Timer lastMedia = new Timer();
    public IMediaSession session;

    private final Animation exit = new Animation(200, 0, Easing.QUAD_IN_OUT);
    private final Animation fadeAnimation;
    private final Animation progressAnimation;


    public MusicInfoComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);

        this.fadeAnimation = new Animation(300, Easing.CUBIC_OUT);
        this.progressAnimation = new Animation(200, Easing.CUBIC_OUT);
    }

    public void tick() {
        if (mc.player.age % 5 == 0) {
            executorService.execute(() -> {

                IMediaSession currentSession = session = MediaPlayerInfo.Instance.getMediaSessions().stream().max(Comparator.comparing(s -> s.getMedia().getPlaying())).orElse(null);
                if (currentSession != null) {
                    MediaInfo info = currentSession.getMedia();
                    if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
                        if (mediaInfo.getTitle().equals("Название Трека") || !Arrays.equals(mediaInfo.getArtworkPng(),info.getArtworkPng())) {
                            Render2DUtil.registerTexture(new Texture(artwork), info.getArtworkPng());
                        }
                        mediaInfo = info;
                        lastMedia.reset();
                    }
                }

            });
        }
    }


    @Override
    public void render(CustomDrawContext ctx) {

        try {
            exit.update(!lastMedia.finished(2000) || mc.currentScreen instanceof ChatScreen);
            if (exit.getValue() == 0) {
                return;
            }
            Font titleFont = Fonts.MEDIUM.getFont(6);
            Font artistFont = Fonts.MEDIUM.getFont(5);
            Font timeFont = Fonts.MEDIUM.getFont(6);
            float coverBoxSize = 32;
            float infoBoxWidth = 75;
            float padding = 6f;
            float borderRadius = 4f;
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA bgLeft = theme.getForegroundLight();
            ColorRGBA bgRight = theme.getForegroundColor();
            ColorRGBA titleColor = theme.getWhite();
            ColorRGBA artistColor = theme.getGrayLight();
            ColorRGBA timeColor = theme.getColor();
            ColorRGBA progressBg = theme.getForegroundStroke();
            ColorRGBA progressFill = theme.getColor();

            this.width = coverBoxSize + infoBoxWidth;
            this.height = coverBoxSize;
            ctx.pushMatrix();
            {
                float scaleX = x + width / 2;
                float scaleY = y + height / 2;
                ctx.getMatrices().translate(scaleX, scaleY);
                ctx.getMatrices().scale(exit.getValue(), exit.getValue());
                ctx.getMatrices().translate(-scaleX, -scaleY);
            }
            DrawUtil.drawBlurHud(ctx.getMatrices(), x, y, width, height, 21, BorderRadius.all(4), ColorRGBA.WHITE);

            ctx.drawRoundedRect(x, y, coverBoxSize, height, BorderRadius.left(borderRadius, borderRadius), bgLeft);
            ctx.drawRoundedRect(x + coverBoxSize, y, infoBoxWidth, height, BorderRadius.right(borderRadius, borderRadius), bgRight);
            ctx.drawRoundedBorder(x, y, coverBoxSize + infoBoxWidth, height, 0.1f, BorderRadius.all(4), theme.getForegroundStroke());

            DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y, coverBoxSize + infoBoxWidth, height, 0.1f, 15f, theme.getColor(), BorderRadius.all(4));


            float imagePadding = 4f;
            float imageSize = coverBoxSize - imagePadding * 2f;
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(0, 0);
            if (artwork != null) {
                DrawUtil.drawRoundedTexture(ctx.getMatrices(), artwork, x + imagePadding, y + imagePadding, imageSize, imageSize, BorderRadius.all(2));
            }
            ctx.getMatrices().popMatrix();


            float rightX = x + coverBoxSize;
            float rightContentX = rightX + padding;
            float rightContentWidth = infoBoxWidth - padding * 2;
            float sliderY = y + height - 7f;
            float progress = mediaInfo.getDuration() > 0 ? (float) mediaInfo.getPosition() / mediaInfo.getDuration() : 0f;

            progressAnimation.update(progress);
            float animatedProgress = progressAnimation.getValue();

            float alpha = fadeAnimation.getValue();
            ColorRGBA bgLeftAlpha = bgLeft.mulAlpha(alpha);
            ColorRGBA bgRightAlpha = bgRight.mulAlpha(alpha);


            ctx.drawRoundedRect(x, y, coverBoxSize, height, BorderRadius.left(borderRadius, borderRadius), bgLeftAlpha);
            ctx.drawRoundedRect(x + coverBoxSize, y, infoBoxWidth, height, BorderRadius.right(borderRadius, borderRadius), bgRightAlpha);

            ctx.drawRoundedRect(rightContentX, sliderY, rightContentWidth, 2f, BorderRadius.all(0.2f), progressBg);
            ctx.drawRoundedRect(rightContentX, sliderY, rightContentWidth * Math.min(1f, animatedProgress), 2f, BorderRadius.all(0.2f), progressFill);

            float titleY = y + 8f;
            float artistY = titleY + titleFont.height() + 2f;
            String timeString = formatTime(mediaInfo.getPosition());
            float timeWidth = timeFont.width(timeString);
            float maxTextWidth = rightContentWidth - timeWidth - 2f;

            String title = mediaInfo.getTitle();
            String artist = mediaInfo.getArtist();

            renderScrollingText(ctx, titleFont, title, rightContentX, titleY, titleColor, maxTextWidth);

            if (artist != null && !artist.isEmpty()) {
                renderScrollingText(ctx, artistFont, artist, rightContentX, artistY + 1.5f, artistColor, maxTextWidth);
            }

            ctx.drawText(timeFont, timeString, rightContentX + 3f + rightContentWidth - timeWidth, titleY, timeColor);

            ctx.popMatrix();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //honey pasting inc.
    private void renderScrollingText(CustomDrawContext ctx, Font font, String text, float x, float y, ColorRGBA color, float maxWidth) {
        float textW = font.width(text);
        float scroll = 0f;

        if (textW > maxWidth) {
            float scrollMax = textW - maxWidth;
            if (scrollMax < 0) scrollMax = 0;

            float pauseDuration = 1000f;
            float scrollDuration = 4000f;
            float totalCycle = pauseDuration + scrollDuration + pauseDuration + scrollDuration;

            long now = System.currentTimeMillis();
            float timeInCycle = now % (long) totalCycle;

            if (timeInCycle < pauseDuration) {
                scroll = 0f;
            } else if (timeInCycle < pauseDuration + scrollDuration) {
                float t = (timeInCycle - pauseDuration) / scrollDuration;
                scroll = t * scrollMax;
            } else if (timeInCycle < pauseDuration + scrollDuration + pauseDuration) {
                scroll = scrollMax;
            } else {
                float t = (timeInCycle - pauseDuration - scrollDuration - pauseDuration) / scrollDuration;
                scroll = scrollMax * (1f - t);
            }
        }

        ctx.enableScissor((int) Math.ceil(x - 1), (int) Math.ceil(y - 1), (int) Math.ceil((x - 1) + maxWidth + 2), (int) Math.ceil((y - 1) + font.height() + 5));
        ctx.drawText(font, text.toLowerCase(), x - scroll, y, color);
        ctx.disableScissor();
    }


    private String formatTime(long ms) {
        long minutes = ms / 60;
        long seconds = ms % 60;
        return String.format("%d:%02d", minutes, seconds);
    }


}


