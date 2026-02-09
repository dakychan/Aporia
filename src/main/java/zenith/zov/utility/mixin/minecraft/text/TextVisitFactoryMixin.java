package zenith.zov.utility.mixin.minecraft.text;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import zenith.zov.client.modules.impl.misc.NameProtect;

@Mixin(TextVisitFactory.class)
public class TextVisitFactoryMixin {
    
    @ModifyArg(
        at = @At(
            value = "INVOKE", 
            target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", 
            ordinal = 0
        ), 
        method = "visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", 
        index = 0
    )
    private static String adjustText(String text) {
        return protect(text);
    }

    @Unique
    private static String protect(String string) {
        return NameProtect.getCustomName(string);
    }
}


