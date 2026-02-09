package aporia.cc.utility.render.display;


import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.*;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import aporia.cc.utility.interfaces.IMinecraft;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.color.ColorUtil;
import aporia.cc.utility.render.display.shader.DrawUtil;


import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@UtilityClass
public class Render2DUtil implements IMinecraft {

    public static HashMap<GlowKey, GlowRect> glowCache = new HashMap<>();

    public static HashMap<Integer, GlowRect> shadowCache1 = new HashMap<>();
    final static Stack<Rectangle> clipStack = new Stack<>();


    public static void endScissor() {
        RenderSystem.disableScissorForRenderTypeDraws();
    }


    public static void setRectPoints(BufferBuilder bufferBuilder, Matrix4f matrix, float x, float y, float x1, float y1, Color c1, Color c2, Color c3, Color c4) {
        bufferBuilder.vertex(matrix, x, y1, 0.0F).color(c1.getRGB());
        bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(c2.getRGB());
        bufferBuilder.vertex(matrix, x1, y, 0.0F).color(c3.getRGB());
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(c4.getRGB());
    }

    public static boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX - width <= x && mouseY >= y && mouseY - height <= y;
    }

    public static void drawBlurredShadow(Matrix3x2fStack matrices, float x, float y, float width, float height, int blurRadius, Color color) {

//        width = width + blurRadius * 2;
//        height = height + blurRadius * 2;
//        x = x - blurRadius;
//        y = y - blurRadius;
//
//        int identifier = (int) ( blurRadius);
//        if (glowCache.containsKey(identifier)) {
//
//            DrawUtility.drawTexture(matrices, glowCache.get(identifier).id.getId(),x,y,width,height,new ColorRGBA(color));
//        } else {
//            BufferedImage original = new BufferedImage((int) width, (int) height, BufferedImage.TYPE_INT_ARGB);
//            Graphics g = original.getGraphics();
//            g.setColor(new Color(-1));
//            g.fillRect(blurRadius, blurRadius, (int) (width - blurRadius * 2), (int) (height - blurRadius * 2));
//            g.dispose();
//            GaussianFilter op = new GaussianFilter(blurRadius);
//            BufferedImage blurred = op.filter(original, null);
//            glowCache.put(identifier, new GlowRect(blurred));
//            return;
//        }

    }
    private final List<Quad> QUAD = new ArrayList<>();

    public void onRender(DrawContext context) {
        Matrix3x2fStack matrices = context.getMatrices();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        if (!QUAD.isEmpty()) {
            RenderLayer layer = RenderLayerUtil.positionColor();
            BufferBuilder buffer = RenderLayerUtil.begin(layer);
            QUAD.forEach(quad -> quad(matrix4f, buffer,quad.x,quad.y,quad.width,quad.height,quad.color));
            RenderLayerUtil.draw(layer, buffer);
            QUAD.clear();
        }
    }
    public void drawQuad(float x, float y, float width, float height, int color) {
        QUAD.add(new Quad(x, y, width, height, ColorUtil.multAlpha(color, 1.0f)));
    }

    public record Quad(float x, float y, float width, float height, int color) {}

    public void quad(Matrix4f matrix4f,BufferBuilder buffer, float x, float y, float width, float height) {
        buffer.vertex(matrix4f, x, y, 0);
        buffer.vertex(matrix4f,x, y + height, 0);
        buffer.vertex(matrix4f,x + width, y + height, 0);
        buffer.vertex(matrix4f,x + width, y, 0);
    }

    public void quad(Matrix4f matrix4f,BufferBuilder buffer, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix4f, x, y, 0).color(color);
        buffer.vertex(matrix4f,x, y + height, 0).color(color);
        buffer.vertex(matrix4f,x + width, y + height, 0).color(color);
        buffer.vertex(matrix4f,x + width, y, 0).color(color);
    }


    public void quad(Matrix4f matrix4f, float x, float y, float width, float height, int color) {
        RenderLayer layer = RenderLayerUtil.positionColor();
        BufferBuilder buffer = RenderLayerUtil.begin(layer);
        buffer.vertex(matrix4f, x, y + height, 0).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0).color(color);
        buffer.vertex(matrix4f, x + width, y, 0).color(color);
        buffer.vertex(matrix4f, x, y, 0).color(color);
        RenderLayerUtil.draw(layer, buffer);
    }


    public void quadTexture(Matrix4f matrix4f, BufferBuilder buffer, float x, float y, float width, float height, int color) {
        buffer.vertex(matrix4f, x, y + height, 0).texture(0, 0).color(color);
        buffer.vertex(matrix4f, x + width, y + height, 0).texture(0, 1).color(color);
        buffer.vertex(matrix4f, x + width, y, 0).texture(1, 1).color(color);
        buffer.vertex(matrix4f, x, y, 0).texture(1, 0).color(color);
    }

    record GlowKey(int width, int height, int blurRadius) {
    }

    private static GlowKey findSimilarKey(int width, int height, int blurRadius) {
        GlowKey closest = null;
        int minDiff = Integer.MAX_VALUE;

        for (GlowKey k : glowCache.keySet()) {
            int diff = Math.abs(k.width() - width)
                    + Math.abs(k.height() - height)
                    + Math.abs(k.blurRadius() - blurRadius);
            if (diff < minDiff) {
                minDiff = diff;
                closest = k;
            }
        }
        return closest;
    }

    private static final ExecutorService GLOW_GENERATOR = Executors.newSingleThreadExecutor();

    public static void drawGradientBlurredShadow(Matrix3x2fStack matrices, float x, float y, float width, float height, int blurRadius, Gradient gradient) {

        width = width + blurRadius * 2;
        height = height + blurRadius * 2;
        x = x - blurRadius;
        y = y - blurRadius;
        GlowKey existing = findSimilarKey((int) width, (int) height, (int) blurRadius);


        if (existing!=null && (int) (Math.abs(existing.width() - width)
                + Math.abs(existing.height() - height)
                + Math.abs(existing.blurRadius() - blurRadius)) <5) {
        } else {
            GlowKey key = new GlowKey((int) width, (int) height, (int) blurRadius);
           mc.execute(() -> {
                BufferedImage original = new BufferedImage(key.width(), key.height(), BufferedImage.TYPE_INT_ARGB);
                Graphics g = original.getGraphics();
                g.setColor(new Color(-1));
                g.fillRect(blurRadius, blurRadius, key.width() - blurRadius * 2, key.height() - blurRadius * 2);
                g.dispose();

                GaussianFilter op = new GaussianFilter(blurRadius);
                BufferedImage blurred = op.filter(original, null);

                glowCache.put(key, new GlowRect(blurred));
            });

        }
        GlowRect glowRect = glowCache.getOrDefault(existing,null);
        if(glowRect==null) {
            return;
        }
        glowRect.reset();
        DrawUtil.drawTexture(matrices, glowRect.id.getId(), x, y, width, height, gradient);
    }


    public static void registerBufferedImageTexture(Texture i, BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            byte[] bytes = baos.toByteArray();
            registerTexture(i, bytes);
        } catch (Exception ignored) {
        }
    }

    public static void registerTexture(Texture i, byte[] content) {
        try {
            ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
            data.flip();
            NativeImage image = NativeImage.read(data);
            mc.execute(() -> {
                NativeImageBackedTexture tex = new NativeImageBackedTexture(
                        () -> "Aporia_texture_" + i.getId(),
                        image
                );
                tex.setFilter(true, false);
                tex.setClamp(true);
                mc.getTextureManager().registerTexture(i.getId(), tex);
            });
        } catch (Exception ignored) {
        }
    }

    public static void renderTexture(Matrix3x2fStack matrices, Identifier texture, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;
        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);
        RenderLayer layer = RenderLayerUtil.guiTextured(texture);
        BufferBuilder buffer = RenderLayerUtil.begin(layer);
        buffer.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight);
        buffer.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight);
        buffer.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight);
        RenderLayerUtil.draw(layer, buffer);
    }

    public static void renderGradientTexture(Matrix3x2fStack matrices, Identifier texture, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
        RenderLayer layer = RenderLayerUtil.guiTextured(texture);
        BufferBuilder buffer = RenderLayerUtil.begin(layer);
        renderGradientTextureInternal(buffer, matrices, x0, y0, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, c1, c2, c3, c4);
        RenderLayerUtil.draw(layer, buffer);
    }

    public static void renderGradientTextureInternal(BufferBuilder buff, Matrix3x2fStack matrices, double x0, double y0, double width, double height, float u, float v, double regionWidth, double regionHeight, double textureWidth, double textureHeight, Color c1, Color c2, Color c3, Color c4) {
        double x1 = x0 + width;
        double y1 = y0 + height;
        double z = 0;
        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);
        buff.vertex(matrix, (float) x0, (float) y1, (float) z).texture((u) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight).color(c1.getRGB());
        buff.vertex(matrix, (float) x1, (float) y1, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v + (float) regionHeight) / (float) textureHeight).color(c2.getRGB());
        buff.vertex(matrix, (float) x1, (float) y0, (float) z).texture((u + (float) regionWidth) / (float) textureWidth, (v) / (float) textureHeight).color(c3.getRGB());
        buff.vertex(matrix, (float) x0, (float) y0, (float) z).texture((u) / (float) textureWidth, (v + 0.0F) / (float) textureHeight).color(c4.getRGB());
    }


    public static void setupRender() {
    }

    public static void drawTracerPointer(Matrix3x2fStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
//        switch (HudEditor.arrowsStyle.getValue()) {
//            case Default -> drawDefaultArrow(matrices, x, y, size, tracerWidth, downHeight, down, glow, color);
//            case New -> drawNewArrow(matrices, x, y, size + 8, new Color(color));
//        }
    }

    public static void drawNewArrow(Matrix3x2fStack matrices, float x, float y, float size, Color color) {
//        RenderSystem.setShaderTexture(0, TextureStorage.arrow);
//        setupRender();
//        RenderSystem.setShaderColor(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);
//        RenderSystem.disableDepthTest();
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        Matrix4f matrix = matrices.peek().getPositionMatrix();
//        RenderSystem.setShader(ShaderProgramKeys.getPositionTexProgram);
//        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
//        bufferBuilder.vertex(matrix, x - (size / 2f), y + size, 0).texture(0f, 1f);
//        bufferBuilder.vertex(matrix, x + size / 2f, y + size, 0).texture(1f, 1f);
//        bufferBuilder.vertex(matrix, x + size / 2f, y, 0).texture(1f, 0);
//        bufferBuilder.vertex(matrix, x - (size / 2f), y, 0).texture(0, 0);
//        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
//        RenderSystem.defaultBlendFunc();
//        RenderSystem.enableDepthTest();
//        endRender();
    }

    public static void drawDefaultArrow(Matrix3x2fStack matrices, float x, float y, float size, float tracerWidth, float downHeight, boolean down, boolean glow, int color) {
        if (glow)
            Render2DUtil.drawBlurredShadow(matrices, x - size * tracerWidth, y, (x + size * tracerWidth) - (x - size * tracerWidth), size, 10, Render2DUtil.injectAlpha(new Color(color), 140));

        matrices.pushMatrix();
        setupRender();
        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);

        RenderLayer layer = RenderLayerUtil.positionColor();
        BufferBuilder bufferBuilder = RenderLayerUtil.begin(layer);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
        bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
        color = Render2DUtil.darker(new Color(color), 0.8f).getRGB();
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0F).color(color);
        bufferBuilder.vertex(matrix, (x + size * tracerWidth), (y + size), 0.0F).color(color);
        bufferBuilder.vertex(matrix, x, y, 0.0F).color(color);

        if (down) {
            color = Render2DUtil.darker(new Color(color), 0.6f).getRGB();
            bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0F).color(color);
            bufferBuilder.vertex(matrix, (x + size * tracerWidth), (y + size), 0.0F).color(color);
            bufferBuilder.vertex(matrix, x, (y + size - downHeight), 0.0F).color(color);
            bufferBuilder.vertex(matrix, (x - size * tracerWidth), (y + size), 0.0F).color(color);
        }

        RenderLayerUtil.draw(layer, bufferBuilder);
        endRender();
        matrices.popMatrix();
    }


    public static void endRender() {
    }


    public static float scrollAnimate(float endPoint, float current, float speed) {
        boolean shouldContinueAnimation = endPoint > current;
        if (speed < 0.0f) {
            speed = 0.0f;
        } else if (speed > 1.0f) {
            speed = 1.0f;
        }

        float dif = Math.max(endPoint, current) - Math.min(endPoint, current);
        float factor = dif * speed;
        return current + (shouldContinueAnimation ? factor : -factor);
    }

    public static Color injectAlpha(final Color color, final int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MathHelper.clamp(alpha, 0, 255));
    }

    public static Color TwoColoreffect(Color cl1, Color cl2, double speed, double count) {
        int angle = (int) (((System.currentTimeMillis()) / speed + count) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return interpolateColorC(cl1, cl2, angle / 360f);
    }

    public static Color astolfo(boolean clickgui, int yOffset) {
        float speed = clickgui ? 35 * 100 : 30 * 100;
        float hue = (System.currentTimeMillis() % (int) speed) + yOffset;
        if (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5F) {
            hue = 0.5F - (hue - 0.5F);
        }
        hue += 0.5F;
        return Color.getHSBColor(hue, 0.4F, 1F);
    }

    public static Color rainbow(int delay, float saturation, float brightness) {
        double rainbow = Math.ceil((System.currentTimeMillis() + delay) / 16f);
        rainbow %= 360;
        return Color.getHSBColor((float) (rainbow / 360), saturation, brightness);
    }

    public static Color skyRainbow(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor((double) ((float) ((angle %= 360) / 360.0)) < 0.5 ? -((float) (angle / 360.0)) : (float) (angle / 360.0), 0.5F, 1.0F);
    }


    public static Color getAnalogousColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float degree = 0.84f;
        float newHueSubtracted = hsb[0] - degree;
        return new Color(Color.HSBtoRGB(newHueSubtracted, hsb[1], hsb[2]));
    }

    public static Color applyOpacity(Color color, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
    }

    public static int applyOpacity(int color_int, float opacity) {
        opacity = Math.min(1, Math.max(0, opacity));
        Color color = new Color(color_int);
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity)).getRGB();
    }

    public static Color darker(Color color, float factor) {
        return new Color(Math.max((int) (color.getRed() * factor), 0), Math.max((int) (color.getGreen() * factor), 0), Math.max((int) (color.getBlue() * factor), 0), color.getAlpha());
    }

    public static Color rainbow(int speed, int index, float saturation, float brightness, float opacity) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        float hue = angle / 360f;
        Color color = new Color(Color.HSBtoRGB(hue, saturation, brightness));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int) (opacity * 255))));
    }

    public static Color interpolateColorsBackAndForth(int speed, int index, Color start, Color end, boolean trueColor) {
        int angle = (int) (((System.currentTimeMillis()) / speed + index) % 360);
        angle = (angle >= 180 ? 360 - angle : angle) * 2;
        return trueColor ? interpolateColorHue(start, end, angle / 360f) : interpolateColorC(start, end, angle / 360f);
    }

    public static Color interpolateColorC(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));
        return new Color(interpolateInt(color1.getRed(), color2.getRed(), amount), interpolateInt(color1.getGreen(), color2.getGreen(), amount), interpolateInt(color1.getBlue(), color2.getBlue(), amount), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static Color interpolateColorHue(Color color1, Color color2, float amount) {
        amount = Math.min(1, Math.max(0, amount));

        float[] color1HSB = Color.RGBtoHSB(color1.getRed(), color1.getGreen(), color1.getBlue(), null);
        float[] color2HSB = Color.RGBtoHSB(color2.getRed(), color2.getGreen(), color2.getBlue(), null);

        Color resultColor = Color.getHSBColor(interpolateFloat(color1HSB[0], color2HSB[0], amount), interpolateFloat(color1HSB[1], color2HSB[1], amount), interpolateFloat(color1HSB[2], color2HSB[2], amount));

        return new Color(resultColor.getRed(), resultColor.getGreen(), resultColor.getBlue(), interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }

    public static double interpolate(double oldValue, double newValue, double interpolationValue) {
        return (oldValue + (newValue - oldValue) * interpolationValue);
    }

    public static float interpolateFloat(float oldValue, float newValue, double interpolationValue) {
        return (float) interpolate(oldValue, newValue, (float) interpolationValue);
    }

    public static int interpolateInt(int oldValue, int newValue, double interpolationValue) {
        return (int) interpolate(oldValue, newValue, (float) interpolationValue);
    }


    public static BufferBuilder preShaderDraw(Matrix3x2fStack matrices, float x, float y, float width, float height) {
        setupRender();
        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        setRectanglePoints(buffer, matrix, x, y, x + width, y + height);
        return buffer;
    }

    public static void setRectanglePoints(BufferBuilder buffer, Matrix4f matrix, float x, float y, float x1, float y1) {
        buffer.vertex(matrix, x, y, 0);
        buffer.vertex(matrix, x, y1, 0);
        buffer.vertex(matrix, x1, y1, 0);
        buffer.vertex(matrix, x1, y, 0);
    }

//    public static void drawOrbiz(MatrixStack matrices, float z, final double r, Color c) {
//        Matrix4f matrix = matrices.peek().getPositionMatrix();
//        setupRender();
//        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
//        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
//        for (int i = 0; i <= 20; i++) {
//            final float x2 = (float) (Math.sin(((i * 56.548656f) / 180f)) * r);
//            final float y2 = (float) (Math.cos(((i * 56.548656f) / 180f)) * r);
//            bufferBuilder.vertex(matrix, x2, y2, z).color(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, 0.4f);
//        }
//        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//        endRender();
//    }
//
//    public static void drawStar(MatrixStack matrices, Color c, float scale) {
//        setupRender();
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        RenderSystem.setShaderTexture(0, TextureStorage.star);
//        RenderSystem.setShaderColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
//        Render2DEngine.renderGradientTexture(matrices, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
//        endRender();
//    }
//
//    public static void drawHeart(MatrixStack matrices, Color c, float scale) {
//        setupRender();
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        RenderSystem.setShaderTexture(0, TextureStorage.heart);
//        RenderSystem.setShaderColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
//        Render2DEngine.renderGradientTexture(matrices, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
//        endRender();
//    }
//
//    public static void drawBloom(MatrixStack matrices, Color c, float scale) {
//        setupRender();
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        RenderSystem.setShaderTexture(0, TextureStorage.firefly);
//        RenderSystem.setShaderColor(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
//        Render2DEngine.renderGradientTexture(matrices, 0, 0, scale, scale, 0, 0, 128, 128, 128, 128, c, c, c, c);
//        endRender();
//    }
//
//    public static void drawBubble(MatrixStack matrices, float angle, float factor) {
//        setupRender();
//        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
//        RenderSystem.setShaderTexture(0, TextureStorage.bubble);
//        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
//        float scale = factor * 2f;
//        Render2DEngine.renderGradientTexture(matrices, -scale / 2, -scale / 2, scale, scale, 0, 0, 128, 128, 128, 128, applyOpacity(HudEditor.getColor(270), 1f - factor), applyOpacity(HudEditor.getColor(0), 1f - factor), applyOpacity(HudEditor.getColor(180), 1f - factor), applyOpacity(HudEditor.getColor(90), 1f - factor));
//        endRender();
//    }
//
//    public static void drawLine(float x, float y, float x1, float y1, int color) {
//        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
//        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
//        bufferBuilder.vertex(x, y, 0f).color(color);
//        bufferBuilder.vertex(x1, y1, 0f).color(color);
//        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());
//    }

    //http://www.java2s.com/example/java/2d-graphics/check-if-a-color-is-more-dark-than-light.html
    public static boolean isDark(Color color) {
        return isDark(color.getRed() / 255.0f, color.getGreen() / 255.0f, color.getBlue() / 255.0f);
    }

    public static boolean isDark(float r, float g, float b) {
        return colorDistance(r, g, b, 0f, 0f, 0f) < colorDistance(r, g, b, 1f, 1f, 1f);
    }

    public static float colorDistance(float r1, float g1, float b1, float r2, float g2, float b2) {
        float a = r2 - r1;
        float b = g2 - g1;
        float c = b2 - b1;
        return (float) Math.sqrt(a * a + b * b + c * c);
    }


    public static @NotNull Color getColor(@NotNull Color start, @NotNull Color end, float progress, boolean smooth) {
        if (!smooth)
            return progress >= 0.95 ? end : start;

        final int rDiff = end.getRed() - start.getRed();
        final int gDiff = end.getGreen() - start.getGreen();
        final int bDiff = end.getBlue() - start.getBlue();
        final int aDiff = end.getAlpha() - start.getAlpha();

        return new Color(
                fixColorValue(start.getRed() + (int) (rDiff * progress)),
                fixColorValue(start.getGreen() + (int) (gDiff * progress)),
                fixColorValue(start.getBlue() + (int) (bDiff * progress)),
                fixColorValue(start.getAlpha() + (int) (aDiff * progress)));
    }

    private static int fixColorValue(int colorVal) {
        return colorVal > 255 ? 255 : Math.max(colorVal, 0);
    }

    public static void endBuilding(RenderLayer layer, BufferBuilder bb) {
        RenderLayerUtil.draw(layer, bb);
    }

    public static class GlowRect {

        public final Texture id;
        private int ticksSinceUse = 0;

        public GlowRect(BufferedImage img) {
            this.id = new Texture("shadow_" + RandomStringUtils.randomAlphanumeric(8));
            Render2DUtil.registerBufferedImageTexture(this.id, img);
        }

        public void reset() {
            ticksSinceUse = 0;
        }

        public boolean tick() {

            return ++ticksSinceUse > 300;
        }

        public void destroy() {
            mc.getTextureManager().destroyTexture(id.getId());
        }
    }

    public static class GlowRect2 {
        public final int id;
        private int ticksSinceUse = 0;

        public GlowRect2(int id) {
            this.id = id;

        }

        public void reset() {
            ticksSinceUse = 0;
        }

        public boolean tick() {

            return ++ticksSinceUse > 4;
        }

    }

    public record Rectangle(float x, float y, float x1, float y1) {
        public boolean contains(double x, double y) {
            return x >= this.x && x <= x1 && y >= this.y && y <= y1;
        }
    }
}
