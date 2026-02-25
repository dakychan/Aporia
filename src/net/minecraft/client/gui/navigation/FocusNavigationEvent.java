package net.minecraft.client.gui.navigation;





public interface FocusNavigationEvent {
   ScreenDirection getVerticalDirectionForInitialFocus();

   
   public record ArrowNavigation(ScreenDirection direction) implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.direction.getAxis() == ScreenAxis.VERTICAL ? this.direction : ScreenDirection.DOWN;
      }
   }

   
   public static class InitialFocus implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return ScreenDirection.DOWN;
      }
   }

   
   public record TabNavigation(boolean forward) implements FocusNavigationEvent {
      @Override
      public ScreenDirection getVerticalDirectionForInitialFocus() {
         return this.forward ? ScreenDirection.DOWN : ScreenDirection.UP;
      }
   }
}
