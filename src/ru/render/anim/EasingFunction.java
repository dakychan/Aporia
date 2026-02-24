package ru.render.anim;

@FunctionalInterface
public interface EasingFunction {
   float ease(float var1);

   static EasingFunction identity() {
      return t -> t;
   }

   default EasingFunction compose(EasingFunction after) {
      return after == null ? this : t -> after.ease(this.ease(t));
   }
}
