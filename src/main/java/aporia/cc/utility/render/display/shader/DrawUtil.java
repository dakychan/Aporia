package aporia.cc.utility.render.display.shader;

import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec2f;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import aporia.cc.Aporia;
import aporia.cc.client.modules.impl.render.Interface;
import aporia.cc.utility.interfaces.IWindow;
import aporia.cc.utility.math.MathUtil;
import aporia.cc.utility.render.display.MatrixUtil;
import aporia.cc.utility.render.display.Render2DUtil;
import aporia.cc.utility.render.display.RenderLayerUtil;
import aporia.cc.utility.render.display.base.BorderRadius;
import aporia.cc.utility.render.display.base.CustomSprite;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.color.ColorRGBA;



@UtilityClass
public class DrawUtil implements IWindow {

    public static final float DEFAULT_SMOOTHNESS = 0.8f;

    public GlProgram rectangleProgram;
    private GlProgram squircleProgram;
    private GlProgram roundedTextureProgram;
    private GlProgram squircleTextureProgram;
    private GlProgram borderProgram;
    private GlProgram figmaBorderProgram;
    private GlProgram loadingProgram;
    private GlProgram gradientRectangleProgram;
    public BlurProgram blurProgram;

    private final CustomRenderTarget buffer = new CustomRenderTarget(false);
    private static ProjectionMatrix2 guiProjection;
    private static int guiDepth;
    private static final ThreadLocal<Vector3f> GUI_MODEL_OFFSET = new ThreadLocal<>();

    public void initializeShaders() {
        rectangleProgram = new GlProgram(Aporia.id("rectangle/data"), VertexFormats.POSITION_COLOR);
        squircleProgram = new GlProgram(Aporia.id("squircle/data"), VertexFormats.POSITION_COLOR);
        squircleTextureProgram = new GlProgram(Aporia.id("squircle_texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
        roundedTextureProgram = new GlProgram(Aporia.id("texture/data"), VertexFormats.POSITION_TEXTURE_COLOR);
        borderProgram = new GlProgram(Aporia.id("border/data"), VertexFormats.POSITION_COLOR);
        figmaBorderProgram = new GlProgram(Aporia.id("corner/data"), VertexFormats.POSITION_COLOR);

        loadingProgram = new GlProgram(Aporia.id("loading/data"), VertexFormats.POSITION_COLOR);
        gradientRectangleProgram = new GlProgram(Aporia.id("gradient_rectangle/data"), VertexFormats.POSITION_COLOR);
        blurProgram = new BlurProgram();
        blurProgram.initShaders();
    }

    public void updateBuffer() {
        buffer.setup();
        mc.getFramebuffer().drawBlit(buffer.getColorAttachmentView());
    }

    public void beginGui() {
        if (guiDepth > 0) {
            guiDepth++;
            return;
        }
        ProjectionMatrix2 projection = getGuiProjection();
        if (projection == null) {
            return;
        }
        guiDepth = 1;
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(
                projection.set(mc.getWindow().getScaledWidth(), mc.getWindow().getScaledHeight()),
                ProjectionType.ORTHOGRAPHIC
        );
        GUI_MODEL_OFFSET.set(new Vector3f(RenderSystem.getModelOffset()));
        RenderSystem.setModelOffset(0.0f, 0.0f, 0.0f);
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
    }

    public void endGui() {
        if (guiDepth <= 0) {
            guiDepth = 0;
            return;
        }
        guiDepth--;
        if (guiDepth > 0) {
            return;
        }
        RenderSystem.getModelViewStack().popMatrix();
        Vector3f modelOffset = GUI_MODEL_OFFSET.get();
        if (modelOffset != null) {
            RenderSystem.setModelOffset(modelOffset.x, modelOffset.y, modelOffset.z);
            GUI_MODEL_OFFSET.remove();
        }
        RenderSystem.restoreProjectionMatrix();
    }

    private static ProjectionMatrix2 getGuiProjection() {
        if (guiProjection != null) {
            return guiProjection;
        }
        try {
            guiProjection = new ProjectionMatrix2("Aporia_gui", -1000.0f, 1000.0f, true);
        } catch (IllegalStateException ignored) {
            return null;
        }
        return guiProjection;
    }

    public void drawLine(Matrix3x2fStack matrices, Vec2f from, Vec2f to, ColorRGBA color) {
        matrices.pushMatrix();
        try {
            Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
            RenderLayerUtil.setCurrentLayer(RenderLayerUtil.positionColor());
            RenderSystem.lineWidth(1);

            drawSetup();

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            builder.vertex(matrix4f, from.x, from.y, 0).color(color.getRGB());
            builder.vertex(matrix4f, to.x, to.y, 0).color(color.getRGB());
            RenderLayerUtil.drawCurrent(builder);

            drawEnd();

        } finally {
            RenderSystem.lineWidth(1.0f);
            matrices.popMatrix();
        }
    }

    public void drawBezier(Matrix3x2fStack matrices, Vec2f p0, Vec2f p1, Vec2f p2, Vec2f p3, ColorRGBA color, int resolution) {
        matrices.pushMatrix();
        try {
            Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
            RenderLayerUtil.setCurrentLayer(RenderLayerUtil.positionColor());
            RenderSystem.lineWidth(1);

            drawSetup();

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);
            for (int i = 0; i <= resolution; i++) {
                float t = (float) i / resolution;
                float x = (float) MathUtil.cubicBezier(t, p0.x, p1.x, p2.x, p3.x);
                float y = (float) MathUtil.cubicBezier(t, p0.y, p1.y, p2.y, p3.y);
                builder.vertex(matrix4f, x, y, 0).color(color.getRGB());
            }
            RenderLayerUtil.drawCurrent(builder);

            drawEnd();

        } finally {
            RenderSystem.lineWidth(1.0f);
            matrices.popMatrix();
        }
    }

    private float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;

        return (uu * u * p0) + (3 * uu * t * p1) + (3 * u * tt * p2) + (tt * t * p3);
    }

    public void drawRect(Matrix3x2fStack matrices, float x, float y, float width, float height, ColorRGBA color) {
        matrices.pushMatrix();

        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.positionColor());

        drawSetup();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, x, y + height, 0).color(color.getRGB());
        builder.vertex(matrix4f, x + width, y + height, 0).color(color.getRGB());
        builder.vertex(matrix4f, x + width, y, 0).color(color.getRGB());
        builder.vertex(matrix4f, x, y, 0).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawSquircle(Matrix3x2fStack matrices, float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        squircleProgram.use();
        squircleProgram.findUniform("Size").set(width, height);
        squircleProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius() * squirt / 2F,
                borderRadius.bottomLeftRadius() * squirt / 2F,
                borderRadius.topRightRadius() * squirt / 2F,
                borderRadius.bottomRightRadius() * squirt / 2F
        );
        squircleProgram.findUniform("Smoothness").set(smoothness);
        squircleProgram.findUniform("CornerSmoothness").set(squirt);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawLoadingRect(Matrix3x2fStack matrices, float x, float y, float width, float height, float progress, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        loadingProgram.use();
        loadingProgram.findUniform("Size").set(width, height);
        loadingProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        loadingProgram.findUniform("Smoothness").set(smoothness);
        loadingProgram.findUniform("Progress").set(progress);
        loadingProgram.findUniform("StripeWidth").set(0f);
        loadingProgram.findUniform("Fade").set(0.5f);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        rectangleProgram.use();
        rectangleProgram.findUniform("Size").set(width, height);
        rectangleProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        rectangleProgram.findUniform("Smoothness").set(smoothness);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color1, ColorRGBA color2, ColorRGBA color3, ColorRGBA color4) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        gradientRectangleProgram.use();
        gradientRectangleProgram.findUniform("Size").set(width, height);
        gradientRectangleProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        gradientRectangleProgram.findUniform("Smoothness").set(smoothness);

        gradientRectangleProgram.findUniform("TopLeftColor").set(
                color1.getRed() / 255.0f,
                color1.getGreen() / 255.0f,
                color1.getBlue() / 255.0f,
                color1.getAlpha() / 255.0f
        );
        gradientRectangleProgram.findUniform("BottomLeftColor").set(
                color2.getRed() / 255.0f,
                color2.getGreen() / 255.0f,
                color2.getBlue() / 255.0f,
                color2.getAlpha() / 255.0f
        );
        gradientRectangleProgram.findUniform("BottomRightColor").set(
                color3.getRed() / 255.0f,
                color3.getGreen() / 255.0f,
                color3.getBlue() / 255.0f,
                color3.getAlpha() / 255.0f
        );
        gradientRectangleProgram.findUniform("TopRightColor").set(
                color4.getRed() / 255.0f,
                color4.getGreen() / 255.0f,
                color4.getBlue() / 255.0f,
                color4.getAlpha() / 255.0f
        );

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

        // color1 - верхний левый
        // color2 - нижний левый
        // color3 - нижний правый
        // color4 - верхний правый
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color1.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color2.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color3.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color4.getRGB());

        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawRoundedRect(Matrix3x2fStack matrices, float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
        drawRoundedRect(matrices, x, y, width, height, borderRadius,
                gradient.getTopLeftColor(),
                gradient.getBottomLeftColor(),
                gradient.getBottomRightColor(),
                gradient.getTopRightColor());
    }


    /**
     * Немного криво, но работает
     */
    public void drawRoundedBorder(Matrix3x2fStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float internalSmoothness = DEFAULT_SMOOTHNESS, externalSmoothness = 1.0F;

        borderProgram.use();
        borderProgram.findUniform("Size").set(width, height);
        borderProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        borderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
        borderProgram.findUniform("Thickness").set(borderThickness);

        drawSetup();

        float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
        float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }


    public void drawRoundedCorner(Matrix3x2fStack matrices, float x, float y, float width, float height, float borderThikenes, float delta, ColorRGBA color, BorderRadius radius) {
        if(!Interface.INSTANCE.isCorners()) return;
        //знаю это пиздец но я проебался с тем что сразу не сделал метод у DragHud
        x-=0.3f;
        y-=0.3f;
        width+=0.3f*2;
        height+=0.3f*2;
        drawRoundedCornerOnly(matrices, x, y, delta, delta, borderThikenes, radius, color, 0);
        drawRoundedCornerOnly(matrices, x + width - delta, y, delta, delta, borderThikenes, radius, color, 1);

        drawRoundedCornerOnly(matrices, x, y + height - delta, delta, delta, borderThikenes, radius, color, 2);
        drawRoundedCornerOnly(matrices, x + width - delta, y + height - delta, delta, delta, borderThikenes, radius, color, 3);

    }

    public void drawRoundedCornerOnly(Matrix3x2fStack matrices, float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor, float cornerIdex) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float internalSmoothness = DEFAULT_SMOOTHNESS, externalSmoothness = 1.0F;

        figmaBorderProgram.use();
        figmaBorderProgram.findUniform("Size").set(width, height);
        figmaBorderProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        figmaBorderProgram.findUniform("Smoothness").set(internalSmoothness, externalSmoothness);
        figmaBorderProgram.findUniform("Thickness").set(borderThickness);
        figmaBorderProgram.findUniform("CornerIndex").set(cornerIdex);

        drawSetup();

        float horizontalPadding = -externalSmoothness / 2.0F + externalSmoothness * 2.0F;
        float verticalPadding = externalSmoothness / 2.0F + externalSmoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(borderColor.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(borderColor.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawTexture(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
        matrices.pushMatrix();

        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));

        drawSetup();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getRGB());
        builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getRGB());
        builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getRGB());
        builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();

        // сбрасываем текстуру
        matrices.popMatrix();
    }
    public void drawTexture(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, Gradient textureColor) {
        matrices.pushMatrix();

        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));

        drawSetup();

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, x, y, 0.0F).texture(0.0F, 0.0F).color(textureColor.getTopLeftColor().getRGB());
        builder.vertex(matrix4f, x, y + height, 0.0F).texture(0.0F, 1.0F).color(textureColor.getBottomLeftColor().getRGB());
        builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(1.0F, 1.0F).color(textureColor.getBottomRightColor().getRGB());
        builder.vertex(matrix4f, x + width, y, 0.0F).texture(1.0F, 0.0F).color(textureColor.getTopRightColor().getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();

        // сбрасываем текстуру
        matrices.popMatrix();
    }

    public void drawTexture(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, float u1, float u2, float v1, float v2, ColorRGBA clor) {
        matrices.pushMatrix();
        int color = clor.getRGB();

        // багчинг пошол нахуй
        // не

        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        float x2 = x + width;
        float y2 = y + height;

        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, x, y, 0.0F).texture(u1, v1).color(color);
        builder.vertex(matrix4f, x, y2, 0.0F).texture(u1, v2).color(color);
        builder.vertex(matrix4f, x2, y2, 0.0F).texture(u2, v2).color(color);
        builder.vertex(matrix4f, x2, y, 0.0F).texture(u2, v1).color(color);
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();

        // сбрасываем текстуру
        matrices.popMatrix();
    }

    public void drawSprite(Matrix3x2fStack matrices, CustomSprite sprite, float x, float y, float width, float height, ColorRGBA color) {
        drawTexture(matrices, sprite.getTexture(), x, y, width, height, 0, 1, 0, 1, color);
    }

    public void drawRoundedTexture(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius) {
        drawRoundedTexture(matrices, identifier, x, y, width, height, borderRadius, ColorRGBA.WHITE);
    }

    public void drawRoundedTexture(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        roundedTextureProgram.use();
        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));

        roundedTextureProgram.findUniform("Size").set(width, height);
        roundedTextureProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        roundedTextureProgram.findUniform("Smoothness").set(smoothness);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(0.0F, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(0.0F, 1.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(1.0F, 1.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(1.0F, 0.0F).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);
        drawEnd();

        // сбрасываем текстуру
        matrices.popMatrix();
    }

    /**
     * Объясняю как работает:
     * Это по сути тот же самый квадрат с закругленными краями, но с более размытыми краями, что как
     * раз и создает нужный нам эффект "тени"
     */
    public void drawShadow(Matrix3x2fStack matrices, float x, float y, float width, float height, float softness, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        rectangleProgram.use();
        rectangleProgram.findUniform("Size").set(width, height);
        rectangleProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius() * 3,
                borderRadius.bottomLeftRadius() * 3,
                borderRadius.topRightRadius() * 3,
                borderRadius.bottomRightRadius() * 3
        );
        rectangleProgram.findUniform("Smoothness").set(softness);

        drawSetup();

        float horizontalPadding = -softness / 2.0F + softness * 2.0F;
        float verticalPadding = softness / 2.0F + softness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();
        matrices.popMatrix();
    }

    public void drawBlur(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurRadius, float squirt, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = 0.03f;

        blurRadius /= 22.5f;

        if (blurRadius <= 0) return;

        blurProgram.setBlurRadius(2);
        squircleTextureProgram.use();
        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTexturedNoTexture());
        RenderSystem.setShaderTexture(0, BlurProgram.getTexture());
        squircleTextureProgram.findUniform("Size").set(width, height);
        squircleTextureProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius() * squirt / 2F,
                borderRadius.bottomLeftRadius() * squirt / 2F,
                borderRadius.topRightRadius() * squirt / 2F,
                borderRadius.bottomRightRadius() * squirt / 2F
        );
        squircleTextureProgram.findUniform("Smoothness").set(0.1f);
        squircleTextureProgram.findUniform("CornerSmoothness").set(squirt);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float u = adjustedX / screenWidth;
        float v = (screenHeight - adjustedY - adjustedHeight) / screenHeight;
        float texWidth = adjustedWidth / screenWidth;
        float texHeight = adjustedHeight / screenHeight;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u, v + texHeight).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u, v).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u + texWidth, v).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);

        drawEnd();

        matrices.popMatrix();
    }


    public void drawBlurHud(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
        drawBlurHudBooleanCheck(matrices,x,y,width,height,blurRadius,borderRadius,color,Interface.INSTANCE.isBlur(),Interface.INSTANCE.isGlow());
    }
    public void drawBlurHudBooleanCheck(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color,boolean blur,boolean glow) {
        if(blur) {

            matrices.pushMatrix();
            Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

            blurRadius /= 22.5f;

            if (blurRadius <= 0) return;

            blurProgram.setBlurRadius(2);
            roundedTextureProgram.use();
            RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTexturedNoTexture());
            RenderSystem.setShaderTexture(0, BlurProgram.getTexture());

            roundedTextureProgram.findUniform("Size").set(width, height);
            roundedTextureProgram.findUniform("Radius").set(
                    borderRadius.topLeftRadius(),
                    borderRadius.bottomLeftRadius(),
                    borderRadius.topRightRadius(),
                    borderRadius.bottomRightRadius()
            );
            roundedTextureProgram.findUniform("Smoothness").set(0.01f);

            drawSetup();

            int screenWidth = mc.getWindow().getScaledWidth();
            int screenHeight = mc.getWindow().getScaledHeight();

            float u = x / screenWidth;
            float v = (screenHeight - y - height) / screenHeight;
            float texWidth = width / screenWidth;
            float texHeight = height / screenHeight;

            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
            builder.vertex(matrix4f, x, y, 0.0F).texture(u, v + texHeight).color(color.getRGB());
            builder.vertex(matrix4f, x, y + height, 0.0F).texture(u, v).color(color.getRGB());
            builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(u + texWidth, v).color(color.getRGB());
            builder.vertex(matrix4f, x + width, y, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
            RenderLayerUtil.drawCurrent(builder);
            drawEnd();

            // сбрасываем текстуру
            matrices.popMatrix();
        }
        if(glow){
            drawGlow(matrices, x, y, width, height,Interface.INSTANCE.getGlowRadius());
        }
    }
    public static void drawGlow(Matrix3x2fStack Matrix3x2fStack, float x, float y, float width, float height,int glowRadius) {

        Render2DUtil.drawGradientBlurredShadow(Matrix3x2fStack,x,y,width,height,glowRadius, Aporia.getInstance().getThemeManager().getClientColor());


    }
    public void drawBlur(Matrix3x2fStack matrices, float x, float y, float width, float height, float blurRadius, BorderRadius borderRadius, ColorRGBA color) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);

        blurRadius /= 22.5f;

        if (blurRadius <= 0) return;

        blurProgram.setBlurRadius(2);
        roundedTextureProgram.use();
        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTexturedNoTexture());
        RenderSystem.setShaderTexture(0, BlurProgram.getTexture());

        roundedTextureProgram.findUniform("Size").set(width, height);
        roundedTextureProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        roundedTextureProgram.findUniform("Smoothness").set(0.01f);

        drawSetup();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        float u = x / screenWidth;
        float v = (screenHeight - y - height) / screenHeight;
        float texWidth = width / screenWidth;
        float texHeight = height / screenHeight;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        builder.vertex(matrix4f, x, y, 0.0F).texture(u, v + texHeight).color(color.getRGB());
        builder.vertex(matrix4f, x, y + height, 0.0F).texture(u, v).color(color.getRGB());
        builder.vertex(matrix4f, x + width, y + height, 0.0F).texture(u + texWidth, v).color(color.getRGB());
        builder.vertex(matrix4f, x + width, y, 0.0F).texture(u + texWidth, v + texHeight).color(color.getRGB());
        RenderLayerUtil.drawCurrent(builder);
        drawEnd();

        // сбрасываем текстуру
        matrices.popMatrix();

    }

    public void drawImage(Matrix3x2fStack matrices, BufferBuilder builder, double x, double y, double z, double width, double height, ColorRGBA color) {
        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);

        builder.vertex(matrix, (float) x, (float) (y + height), (float) z).texture(0, 1).color(color.getRGB());
        builder.vertex(matrix, (float) (x + width), (float) (y + height), (float) z).texture(1, 1).color(color.getRGB());
        builder.vertex(matrix, (float) (x + width), (float) y, (float) z).texture(1, 0).color(color.getRGB());
        builder.vertex(matrix, (float) x, (float) y, (float) z).texture(0, 0).color(color.getRGB());
    }

    public void drawImage(Matrix3x2fStack matrices, Identifier identifier, double x, double y, double z, double width, double height, ColorRGBA color) {
        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));
        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        Matrix4f matrix = MatrixUtil.toMatrix4f(matrices);

        builder.vertex(matrix, (float) x, (float) (y + height), (float) z).texture(0, 1).color(color.getRGB());
        builder.vertex(matrix, (float) (x + width), (float) (y + height), (float) z).texture(1, 1).color(color.getRGB());
        builder.vertex(matrix, (float) (x + width), (float) y, (float) z).texture(1, 0).color(color.getRGB());
        builder.vertex(matrix, (float) x, (float) y, (float) z).texture(0, 0).color(color.getRGB());

        RenderLayerUtil.drawCurrent(builder);
    }

    public void drawPlayerHeadWithRoundedShader(Matrix3x2fStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
        drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color,
                8.0f / 64.0f,   // u1 - левый край головы
                8.0f / 64.0f,   // v1 - верхний край головы
                16.0f / 64.0f,  // u2 - правый край головы
                16.0f / 64.0f   // v2 - нижний край головы
        );
    }

    private void drawPlayerHatLayerWithRoundedShader(Matrix3x2fStack matrices, Identifier skinTexture, float x, float y, float size, BorderRadius borderRadius, ColorRGBA color) {
        // Включаем блендинг для прозрачности hat layer
        drawRoundedTextureWithUV(matrices, skinTexture, x, y, size, size, borderRadius, color,
                40.0f / 64.0f,  // u1 - левый край hat layer
                8.0f / 64.0f,   // v1 - верхний край hat layer
                48.0f / 64.0f,  // u2 - правый край hat layer
                16.0f / 64.0f   // v2 - нижний край hat layer
        );

    }

    public void drawRoundedTextureWithUV(Matrix3x2fStack matrices, Identifier identifier, float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color, float u1, float v1, float u2, float v2) {
        matrices.pushMatrix();
        Matrix4f matrix4f = MatrixUtil.toMatrix4f(matrices);
        float smoothness = DEFAULT_SMOOTHNESS;

        roundedTextureProgram.use();
        RenderLayerUtil.setCurrentLayer(RenderLayerUtil.guiTextured(identifier));

        roundedTextureProgram.findUniform("Size").set(width, height);
        roundedTextureProgram.findUniform("Radius").set(
                borderRadius.topLeftRadius(),
                borderRadius.bottomLeftRadius(),
                borderRadius.topRightRadius(),
                borderRadius.bottomRightRadius()
        );
        roundedTextureProgram.findUniform("Smoothness").set(smoothness);

        drawSetup();

        float horizontalPadding = -smoothness / 2.0F + smoothness * 2.0F;
        float verticalPadding = smoothness / 2.0F + smoothness;
        float adjustedX = x - horizontalPadding / 2.0F;
        float adjustedY = y - verticalPadding / 2.0F;
        float adjustedWidth = width + horizontalPadding;
        float adjustedHeight = height + verticalPadding;

        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        // Используем переданный цвет вместо жестко закодированного белого
        builder.vertex(matrix4f, adjustedX, adjustedY, 0.0F).texture(u1, v1).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX, adjustedY + adjustedHeight, 0.0F).texture(u1, v2).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY + adjustedHeight, 0.0F).texture(u2, v2).color(color.getRGB());
        builder.vertex(matrix4f, adjustedX + adjustedWidth, adjustedY, 0.0F).texture(u2, v1).color(color.getRGB());

        RenderLayerUtil.drawCurrent(builder);
        drawEnd();

        matrices.popMatrix();
    }


    public void drawSetup() {
    }

    public void drawEnd() {
    }

    record HeadUV(float u1, float v1, float uSize, float vSize) {
    }
}





