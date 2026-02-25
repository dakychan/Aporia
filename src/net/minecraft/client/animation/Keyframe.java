package net.minecraft.client.animation;



import org.joml.Vector3fc;


public record Keyframe(float timestamp, Vector3fc preTarget, Vector3fc postTarget, AnimationChannel.Interpolation interpolation) {
   public Keyframe(float p_426228_, Vector3fc p_424077_, AnimationChannel.Interpolation p_429750_) {
      this(p_426228_, p_424077_, p_424077_, p_429750_);
   }
}
