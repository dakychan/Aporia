package net.minecraft.client.data.models;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface ItemModelOutput {
    default void accept(Item p_375406_, ItemModel.Unbaked p_376490_) {
        this.accept(p_375406_, p_376490_, ClientItem.Properties.DEFAULT);
    }

    void accept(Item p_458974_, ItemModel.Unbaked p_451423_, ClientItem.Properties p_458087_);

    void copy(Item p_376683_, Item p_376863_);
}