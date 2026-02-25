package net.minecraft.client.renderer;

import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;




public record MaterialMapper(Identifier sheet, String prefix) {
   public Material apply(Identifier p_457005_) {
      return new Material(this.sheet, p_457005_.withPrefix(this.prefix + "/"));
   }

   public Material defaultNamespaceApply(String p_395591_) {
      return this.apply(Identifier.withDefaultNamespace(p_395591_));
   }
}
