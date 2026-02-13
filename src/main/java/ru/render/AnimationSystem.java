package ru.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class AnimationSystem {
   private static final float MIN_DELTA_SECONDS = 1.0E-4F;
   private static final float MAX_DELTA_SECONDS = 0.06666667F;
   private static final AnimationSystem INSTANCE = new AnimationSystem();
   private final Object lock = new Object();
   private final List<Animated> animatedObjects = new ArrayList<>();
   private long lastTickNanos = System.nanoTime();
   private float lastDeltaSeconds = 0.0F;

   public AnimationSystem() {
   }

   public static AnimationSystem getInstance() {
      return INSTANCE;
   }

   public void tick() {
      long now = System.nanoTime();
      long elapsed = now - this.lastTickNanos;
      this.lastTickNanos = now;
      if (elapsed < 0L) {
         elapsed = 0L;
      }

      float delta = (float)elapsed / 1.0E9F;
      if (delta < 1.0E-4F) {
         delta = 1.0E-4F;
      } else if (delta > 0.06666667F) {
         delta = 0.06666667F;
      }

      this.lastDeltaSeconds = delta;
      synchronized (this.lock) {
         if (!this.animatedObjects.isEmpty()) {
            Iterator<Animated> iterator = this.animatedObjects.iterator();

            while (iterator.hasNext()) {
               Animated animated = iterator.next();
               boolean keepUpdating = animated.update(delta);
               if (!keepUpdating) {
                  iterator.remove();
               }
            }
         }
      }
   }

   public float getLastDeltaSeconds() {
      return this.lastDeltaSeconds;
   }

   public void ensureRegistered(Animated animated) {
      if (animated != null) {
         synchronized (this.lock) {
            if (!this.animatedObjects.contains(animated)) {
               this.animatedObjects.add(animated);
            }
         }
      }
   }

   public void unregister(Animated animated) {
      if (animated != null) {
         synchronized (this.lock) {
            this.animatedObjects.remove(animated);
         }
      }
   }

   public interface Animated {
      boolean update(float var1);
   }
}
