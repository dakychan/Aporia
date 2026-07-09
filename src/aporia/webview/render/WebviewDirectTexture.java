package aporia.webview.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;

import com.chaos.annotation.ChaosNative;

@ChaosNative
public class WebviewDirectTexture extends AbstractTexture {

    {
        this.sampler = RenderSystem.getSamplerCache()
            .getSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE,
                FilterMode.LINEAR, FilterMode.LINEAR, false);
    }

    public void setView(GpuTextureView view) {
        this.textureView = view;
        this.texture = view.texture();
    }

    @Override
    public void close() {
        this.textureView = null;
        this.texture = null;
    }
}
