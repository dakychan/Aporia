package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;



@FunctionalInterface

public interface UnbakedGeometry {
   UnbakedGeometry EMPTY = (p_396370_, p_395488_, p_393569_, p_397842_) -> QuadCollection.EMPTY;

   QuadCollection bake(TextureSlots var1, ModelBaker var2, ModelState var3, ModelDebugName var4);
}
