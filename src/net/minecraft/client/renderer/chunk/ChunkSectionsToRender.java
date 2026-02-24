package net.minecraft.client.renderer.chunk;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderPass.Draw;
import com.mojang.blaze3d.systems.RenderSystem.AutoStorageIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import java.util.EnumMap;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ChunkSectionsToRender(
   GpuTextureView textureView,
   EnumMap<ChunkSectionLayer, List<Draw<GpuBufferSlice[]>>> drawsPerLayer,
   int maxIndicesRequired,
   GpuBufferSlice[] chunkSectionInfos
) {
   public void renderGroup(ChunkSectionLayerGroup p_406533_, GpuSampler p_455406_) {
      AutoStorageIndexBuffer rendersystem$autostorageindexbuffer = RenderSystem.getSequentialBuffer(Mode.QUADS);
      GpuBuffer gpubuffer = this.maxIndicesRequired == 0 ? null : rendersystem$autostorageindexbuffer.getBuffer(this.maxIndicesRequired);
      IndexType vertexformat$indextype = this.maxIndicesRequired == 0 ? null : rendersystem$autostorageindexbuffer.type();
      ChunkSectionLayer[] achunksectionlayer = p_406533_.layers();
      Minecraft minecraft = Minecraft.getInstance();
      boolean flag = net.minecraft.SharedConstants.DEBUG_HOTKEYS && minecraft.wireframe;
      RenderTarget rendertarget = p_406533_.outputTarget();
      RenderPass renderpass = RenderSystem.getDevice()
         .createCommandEncoder()
         .createRenderPass(
            () -> "Section layers for " + p_406533_.label(),
            rendertarget.getColorTextureView(),
            OptionalInt.empty(),
            rendertarget.getDepthTextureView(),
            OptionalDouble.empty()
         );

      try {
         RenderSystem.bindDefaultUniforms(renderpass);
         renderpass.bindTexture(
            "Sampler2", minecraft.gameRenderer.lightTexture().getTextureView(), RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR)
         );

         for (ChunkSectionLayer chunksectionlayer : achunksectionlayer) {
            List<Draw<GpuBufferSlice[]>> list = this.drawsPerLayer.get(chunksectionlayer);
            if (!list.isEmpty()) {
               if (chunksectionlayer == ChunkSectionLayer.TRANSLUCENT) {
                  list = list.reversed();
               }

               renderpass.setPipeline(flag ? RenderPipelines.WIREFRAME : chunksectionlayer.pipeline());
               renderpass.bindTexture("Sampler0", this.textureView, p_455406_);
               renderpass.drawMultipleIndexed(list, gpubuffer, vertexformat$indextype, List.of("ChunkSection"), this.chunkSectionInfos);
            }
         }
      } catch (Throwable var17) {
         if (renderpass != null) {
            try {
               renderpass.close();
            } catch (Throwable var16) {
               var17.addSuppressed(var16);
            }
         }

         throw var17;
      }

      if (renderpass != null) {
         renderpass.close();
      }
   }
}
