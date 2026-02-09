package zenith.zov.utility.render.display.base;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.MsdfRenderer;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.render.display.MatrixUtil;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

import java.util.Objects;

public class CustomDrawContext extends DrawContext implements IMinecraft {

    public CustomDrawContext(GuiRenderState state) {
        super(mc, state);
    }

    public CustomDrawContext(DrawContext originalContext) {
        super(mc, originalContext.state);
    }

    public static CustomDrawContext of(DrawContext originalContext) {
        return new CustomDrawContext(originalContext);
    }

    public void drawText(Font font, String text, float x, float y, ColorRGBA color) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color.getRGB(), currentMatrix(), x, y, 0);
    }

    public void drawText(Font font, String text, float x, float y, Gradient color) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), color, currentMatrix(), x, y, 0);
    }

    public void drawText(Font font, Text text, float x, float y) {
        MsdfRenderer.renderText(font.getFont(), text, font.getSize(), currentMatrix(), x, y, 0);
    }

    public void drawSquircle(float x, float y, float width, float height, float squirt, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawSquircle(this.getMatrices(), x, y, width, height, squirt, borderRadius, color);
    }

    public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, ColorRGBA color) {
        DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, color);
    }

    public void drawRoundedRect(float x, float y, float width, float height, BorderRadius borderRadius, Gradient gradient) {
        DrawUtil.drawRoundedRect(this.getMatrices(), x, y, width, height, borderRadius, gradient);
    }

    public void drawRect(float x, float y, float width, float height, ColorRGBA color) {
        DrawUtil.drawRect(this.getMatrices(), x, y, width, height, color);
    }

    public int drawTextWithBackground(TextRenderer textRenderer, Text text, int x, int y, int width,BorderRadius borderRadius, ColorRGBA textColor, ColorRGBA backgroundColor) {


        int var10001 = x - 3;
        int var10002 = y - 2;
        int var10003 = width + 6;
        Objects.requireNonNull(textRenderer);
        this.drawRoundedRect(var10001, var10002, var10003,  9 + 4,borderRadius,backgroundColor);


        this.drawText(textRenderer, text, x, y, textColor.getRGB(), true);
        return x + textRenderer.getWidth(text);
    }

    public void drawSprite(CustomSprite sprite, float x, float y, float width, float height, ColorRGBA textureColor) {
        DrawUtil.drawSprite(this.getMatrices(), sprite, x, y, width, height, textureColor);
    }

    public void drawRoundedCorner(float x, float y, float width, float height, float borderThikenes, float widthCorner, ColorRGBA color, BorderRadius radius) {

        width = Math.round(width);
        height = Math.round(height);
        this.enableScissor((int) Math.ceil(x - 10), (int) (y - 10), (int) (x + widthCorner), (int) (y + widthCorner));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x + width - widthCorner), (int) (y - 10), (int) (x + width + 10), (int) (y + widthCorner));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x - 10), (int) (y + height - widthCorner), (int) (x + widthCorner), (int) (y + height + 10));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();

        this.enableScissor((int) (x + width - widthCorner), (int) (y + height - widthCorner), (int) (x + width + 10), (int) (y + height + 10));
        drawRoundedBorder(x, y, width, height, borderThikenes, radius, color);
        this.disableScissor();
    }

    public void drawRoundedBorder(float x, float y, float width, float height, float borderThickness, BorderRadius borderRadius, ColorRGBA borderColor) {
        DrawUtil.drawRoundedBorder(this.getMatrices(), x, y, width, height, borderThickness, borderRadius, borderColor);
    }

    public void drawTexture(Identifier identifier, float x, float y, float width, float height, ColorRGBA textureColor) {
        DrawUtil.drawTexture(this.getMatrices(), identifier, x, y, width, height, textureColor);
    }

    @Override
    public void enableScissor(int x1, int y1, int x2, int y2) {
        super.enableScissor(x1, y1, x2, y2);
        applyImmediateScissor();
    }

    @Override
    public void disableScissor() {
        super.disableScissor();
        applyImmediateScissor();
    }

    private void applyImmediateScissor() {
        ScreenRect rect = this.scissorStack.peekLast();
        if (rect == null) {
            RenderSystem.disableScissorForRenderTypeDraws();
            return;
        }
        int scale = MinecraftClient.getInstance().getWindow().getScaleFactor();
        int fbHeight = MinecraftClient.getInstance().getWindow().getFramebufferHeight();
        int x = (int) (rect.getLeft() * scale);
        int y = (int) (fbHeight - rect.getBottom() * scale);
        int w = Math.max(0, (int) (rect.width() * scale));
        int h = Math.max(0, (int) (rect.height() * scale));
        RenderSystem.enableScissorForRenderTypeDraws(x, y, w, h);
    }

    public void pushMatrix() {
        getMatrices().pushMatrix();
    }

    public void popMatrix() {
        getMatrices().popMatrix();
    }

    private Matrix4f currentMatrix() {
        return MatrixUtil.toMatrix4f(getMatrices());
    }
}
