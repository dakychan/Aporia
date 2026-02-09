package aporia.cc.base.font;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.*;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import aporia.cc.utility.render.display.base.Gradient;
import aporia.cc.utility.render.display.base.color.ColorRGBA;
import aporia.cc.utility.render.display.RenderLayerUtil;
import aporia.cc.utility.render.display.shader.GlProgram;
import aporia.cc.Aporia;

import java.util.List;

@UtilityClass
public class MsdfRenderer {

    private GlProgram msdfProgram;

    public void init() {
        if (msdfProgram == null) {
            msdfProgram = new GlProgram(Aporia.id("msdf_font/data"), VertexFormats.POSITION_TEXTURE_COLOR);
        }
    }

    private void prepareShader(MsdfFont font, float thickness, float smoothness, boolean enableFadeout,
            float fadeoutStart, float fadeoutEnd, float maxWidth, float textPosX) {
        init();
        if (maxWidth <= 0.0f) {
            maxWidth = 1.0f;
        }
        msdfProgram.use();
        var texture = MinecraftClient.getInstance()
                .getTextureManager()
                .getTexture(font.getAtlasIdentifier());
        texture.setFilter(true, true);
        RenderSystem.setShaderTexture(0, texture.getGlTextureView());
        msdfProgram.findUniform("Range").set(font.getAtlas().range());
        msdfProgram.findUniform("Thickness").set(thickness);
        msdfProgram.findUniform("Smoothness").set(smoothness, smoothness);
        msdfProgram.findUniform("Outline").set(0);
        msdfProgram.findUniform("OutlineThickness").set(0.0f);
        msdfProgram.findUniform("OutlineColor").set(0.0f, 0.0f, 0.0f, 0.0f);
        msdfProgram.findUniform("EnableFadeout").set(enableFadeout ? 1 : 0);
        msdfProgram.findUniform("FadeoutStart").set(fadeoutStart);
        msdfProgram.findUniform("FadeoutEnd").set(fadeoutEnd);
        msdfProgram.findUniform("MaxWidth").set(maxWidth);
        msdfProgram.findUniform("TextPosX").set(textPosX);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z) {
        renderText(font, text, size, color, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth) {

        float thickness = 0.10f;
        float smoothness = 0.34f;
        float spacing = 0;
        float textPosX = x - 0.75F;
        prepareShader(font, thickness, smoothness, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth, textPosX);
        // NameProtect nameProtectModule =
        // Rockstar.getInstance().getModuleManager().getModule(NameProtect.class);
        //
        // if (nameProtectModule.isEnabled()) {
        // text = nameProtectModule.patchName(text);
        // }

        // if (Batching.getActive() != null) {
        // // Для батчинга пока оставляем стандартную отрисовку
        // // TODO: Реализовать fadeout для батчинга
        // font.applyGlyphs(
        // matrix,
        // Batching.getActive().getBuilder(),
        // text,
        // size,
        // thickness * 0.5f * size,
        // spacing,
        // // Так называемый рокстарвский MAGIC VALUE
        // x - 0.75F, // небольшой оффсет чтобы мы всегда были внутри краев
        // y + (size * 0.7F),
        // z,
        // color
        // );
        // return;
        // }

        RenderLayer layer = RenderLayerUtil.guiTextured(font.getAtlasIdentifier());
        BufferBuilder builder = RenderLayerUtil.begin(layer);
        font.applyGlyphs(
                matrix,
                builder,
                text,
                size,
                thickness * 0.5f * size,
                spacing,
                // Так называемый рокстарвский MAGIC VALUE
                x - 0.75F, // небольшой оффсет чтобы мы всегда были внутри краев
                y + (size * 0.7F),
                z,
                color);
        RenderLayerUtil.draw(layer, builder);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            int color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd) {
        float maxWidth = font.getWidth(text, size) * 2.0F;
        renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z) {
        renderText(font, text, size, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F);
    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth) {
        float thickness = 0.10f;
        float smoothness = 0.34f;
        float spacing = 0;
        float textPosX = x - 0.75F;
        prepareShader(font, thickness, smoothness, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth, textPosX);
        List<FormattedTextProcessor.TextSegment> segments = FormattedTextProcessor.processText(text,
                ColorRGBA.WHITE.getRGB());

        float currentX = x;

        RenderLayer layer = RenderLayerUtil.guiTextured(font.getAtlasIdentifier());
        BufferBuilder builder = RenderLayerUtil.begin(layer);

        for (FormattedTextProcessor.TextSegment segment : segments) {

            font.applyGlyphs(
                    matrix,
                    builder,
                    segment.text(),
                    size,
                    thickness * 0.5f * size,
                    spacing - 0.3F,
                    // Так называемый рокстарвский MAGIC VALUE
                    currentX - 0.75F, // небольшой оффсет чтобы мы всегда были внутри краев
                    y + (size * 0.7F),
                    z,
                    segment.color());

            currentX += font.getWidth(segment.text(), size);
        }
        RenderLayerUtil.draw(layer, builder);

    }

    public void renderText(
            MsdfFont font,
            Text text,
            float size,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd) {
        float maxWidth = font.getTextWidth(text, size) * 2.0F;
        renderText(font, text, size, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);

    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            Gradient color,
            Matrix4f matrix,
            float x,
            float y,
            float z) {
        renderText(font, text, size, color, matrix, x, y, z, false, 0.0f, 1.0f, 0.0F);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            Gradient color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd,
            float maxWidth) {
        text = text.replace("і", "i").replace("І", "I");
        float thickness = 0.10f;
        float smoothness = 0.34f;
        float spacing = 0;
        float textPosX = x - 0.75F;
        prepareShader(font, thickness, smoothness, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth, textPosX);
        // NameProtect nameProtectModule =
        // Rockstar.getInstance().getModuleManager().getModule(NameProtect.class);
        //
        // if (nameProtectModule.isEnabled()) {
        // text = nameProtectModule.patchName(text);
        // }

        // if (Batching.getActive() != null) {
        // // Для батчинга пока оставляем стандартную отрисовку
        // // TODO: Реализовать fadeout для батчинга
        // font.applyGlyphs(
        // matrix,
        // Batching.getActive().getBuilder(),
        // text,
        // size,
        // thickness * 0.5f * size,
        // spacing,
        // // Так называемый рокстарвский MAGIC VALUE
        // x - 0.75F, // небольшой оффсет чтобы мы всегда были внутри краев
        // y + (size * 0.7F),
        // z,
        // color
        // );
        // return;
        // }

        RenderLayer layer = RenderLayerUtil.guiTextured(font.getAtlasIdentifier());
        BufferBuilder builder = RenderLayerUtil.begin(layer);
        font.applyGlyphs(
                matrix,
                builder,
                text,
                size,
                thickness * 0.5f * size,
                spacing,
                // Так называемый рокстарвский MAGIC VALUE
                x - 0.75F, // небольшой оффсет чтобы мы всегда были внутри краев
                y + (size * 0.7F),
                z,
                color);
        RenderLayerUtil.draw(layer, builder);
    }

    public void renderText(
            MsdfFont font,
            String text,
            float size,
            Gradient color,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            boolean enableFadeout,
            float fadeoutStart,
            float fadeoutEnd) {
        float maxWidth = font.getWidth(text, size) * 2.0F;
        renderText(font, text, size, color, matrix, x, y, z, enableFadeout, fadeoutStart, fadeoutEnd, maxWidth);
    }

}
