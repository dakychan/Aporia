package net.minecraft.client.resources.model;

import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@FunctionalInterface
@OnlyIn(Dist.CLIENT)
public interface UnbakedGeometry {
    UnbakedGeometry EMPTY = (p_396370_, p_395488_, p_393569_, p_397842_) -> QuadCollection.EMPTY;

    QuadCollection bake(TextureSlots p_391189_, ModelBaker p_391275_, ModelState p_397070_, ModelDebugName p_391995_);
}