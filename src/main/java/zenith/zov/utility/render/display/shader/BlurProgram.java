package zenith.zov.utility.render.display.shader;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormats;
import zenith.zov.Zenith;
import zenith.zov.utility.interfaces.IWindow;
import zenith.zov.utility.math.Timer;
import zenith.zov.utility.render.display.RenderLayerUtil;
import zenith.zov.utility.render.display.shader.impl.KawaseBlurProgram;

public class BlurProgram implements IWindow {

    public static final Supplier<CustomRenderTarget> CACHE = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());
    public static final Supplier<CustomRenderTarget> BUFFER = Suppliers.memoize(() -> new CustomRenderTarget(false).setLinear());

    private static KawaseBlurProgram kawaseDownProgram;
    private static KawaseBlurProgram kawaseUpProgram;

    private final Timer timer = new Timer();

    public void initShaders() {
        kawaseDownProgram = new KawaseBlurProgram(Zenith.id("kawase_down/data"));
        kawaseUpProgram = new KawaseBlurProgram(Zenith.id("kawase_up/data"));
    }

    public void draw() {
        if (!timer.finished(25)) return;
        timer.reset();
    }

    private void drawQuad(float x, float y, float width, float height) {
        int color = -1;
        BufferBuilder builder = RenderLayerUtil.begin(RenderLayerUtil.guiTexturedNoTexture());
        builder.vertex(x, y, 0F).texture(0, 1).color(color);
        builder.vertex(x, y + height, 0F).texture(0, 0).color(color);
        builder.vertex(x + width, y + height, 0F).texture(1, 0).color(color);
        builder.vertex(x + width, y, 0F).texture(1, 1).color(color);

        RenderLayerUtil.draw(RenderLayerUtil.guiTexturedNoTexture(), builder);
    }

    public static GpuTextureView getTexture() {
        return BUFFER.get().getColorAttachmentView();
    }

    public void setBlurRadius(float blurRadius) {
        kawaseDownProgram.updateUniforms(blurRadius);
        kawaseUpProgram.updateUniforms(blurRadius);
    }

}
