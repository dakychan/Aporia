package net.minecraft.client.gui.narration;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.gui.components.TabOrderedElement;




public interface NarratableEntry extends TabOrderedElement, NarrationSupplier {
   NarratableEntry.NarrationPriority narrationPriority();

   default boolean isActive() {
      return true;
   }

   default Collection<? extends NarratableEntry> getNarratables() {
      return List.of(this);
   }

   
   public static enum NarrationPriority {
      NONE,
      HOVERED,
      FOCUSED;

      public boolean isTerminal() {
         return this == FOCUSED;
      }
   }
}
