package net.minecraft.client.renderer.texture;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;




public class SimpleTexture extends ReloadableTexture {
   public SimpleTexture(Identifier p_457931_) {
      super(p_457931_);
   }

   @Override
   public TextureContents loadContents(ResourceManager p_376679_) throws IOException {
      return TextureContents.load(p_376679_, this.resourceId());
   }
}
