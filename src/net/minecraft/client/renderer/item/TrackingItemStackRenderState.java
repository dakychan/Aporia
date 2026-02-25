package net.minecraft.client.renderer.item;

import java.util.ArrayList;
import java.util.List;




public class TrackingItemStackRenderState extends ItemStackRenderState {
   private final List<Object> modelIdentityElements = new ArrayList<>();

   @Override
   public void appendModelIdentityElement(Object p_410809_) {
      this.modelIdentityElements.add(p_410809_);
   }

   public Object getModelIdentity() {
      return this.modelIdentityElements;
   }
}
