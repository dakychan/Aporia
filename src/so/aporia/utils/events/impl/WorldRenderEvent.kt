package so.aporia.utils.events.impl
import com.mojang.blaze3d.vertex.PoseStack
import com.chaos.annotation.ChaosNative
@ChaosNative
class WorldRenderEvent(val poseStack: PoseStack, val partialTick: Float)