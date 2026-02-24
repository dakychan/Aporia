package ru.render;

import net.minecraft.client.gui.GuiGraphics;

public class GuiRenderer {
   private final BlurShader blurShader;
   private final AnimationSystem animationSystem;
   private boolean initialized = false;

   public GuiRenderer() {
      this.blurShader = new BlurShader();
      this.animationSystem = new AnimationSystem();
   }

   public void initialize() {
      if (!this.initialized) {
         this.initialized = true;
      }
   }

   public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
      if (!this.initialized) {
         this.initialize();
      }

      this.animationSystem.tick();
   }

   public void cleanup() {
      this.blurShader.cleanup();
      this.initialized = false;
   }

   public BlurShader getBlurShader() {
      return this.blurShader;
   }

   public AnimationSystem getAnimationSystem() {
      return this.animationSystem;
   }

   public boolean isInitialized() {
      return this.initialized;
   }
}
