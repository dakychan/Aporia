package aporia.webview.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.texture.AbstractTexture;

public class WebviewDirectTexture extends AbstractTexture {

    public void setView(GpuTextureView view) {
        this.textureView = view;
    }

    @Override
    public void close() {
        this.textureView = null;
        this.texture = null;
    }
}
