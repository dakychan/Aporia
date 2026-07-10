package net.minecraft.client;

import net.minecraft.obfuscate.DontObfuscate;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientBrandRetriever {
    public static final String VANILLA_NAME = "aporia";

    @DontObfuscate
    public static String getClientModName() {
        return "aporia";
    }
}