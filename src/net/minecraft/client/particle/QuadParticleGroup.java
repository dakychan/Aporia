package net.minecraft.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.QuadParticleRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class QuadParticleGroup extends ParticleGroup<SingleQuadParticle> {
   private final ParticleRenderType particleType;
   final QuadParticleRenderState particleTypeRenderState = new QuadParticleRenderState();

   public QuadParticleGroup(ParticleEngine p_422302_, ParticleRenderType p_427417_) {
      super(p_422302_);
      this.particleType = p_427417_;
   }

   @Override
   public ParticleGroupRenderState extractRenderState(Frustum p_426251_, Camera p_431723_, float p_428753_) {
      for (SingleQuadParticle singlequadparticle : this.particles) {
         if (p_426251_.pointInFrustum(singlequadparticle.x, singlequadparticle.y, singlequadparticle.z)) {
            try {
               singlequadparticle.extract(this.particleTypeRenderState, p_431723_, p_428753_);
            } catch (Throwable var9) {
               net.minecraft.CrashReport crashreport = net.minecraft.CrashReport.forThrowable(var9, "Rendering Particle");
               net.minecraft.CrashReportCategory crashreportcategory = crashreport.addCategory("Particle being rendered");
               crashreportcategory.setDetail("Particle", singlequadparticle::toString);
               crashreportcategory.setDetail("Particle Type", this.particleType::toString);
               throw new net.minecraft.ReportedException(crashreport);
            }
         }
      }

      return this.particleTypeRenderState;
   }
}
