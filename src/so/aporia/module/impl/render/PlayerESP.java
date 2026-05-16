/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.phys.Vec3;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.WorldRenderEvent;

public final class PlayerESP extends Module {

    public final BooleanSetting nameTags = new BooleanSetting("NameTags", "Show player nametags", true);
    public final BooleanSetting boxes = new BooleanSetting("Boxes", "Show 3D boxes around players", true);
    public final BooleanSetting playerModel = new BooleanSetting("PlayerModel", "Render player model with skin", false);

    public PlayerESP() {
        super("PlayerESP", Category.VISUAL);
    }

    @Override
    protected void onEnable() {
        EventBus.INSTANCE.register(this);
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = e.poseStack();
        float pt = e.partialTick();
        Vec3 camPos = mc.gameRenderer.getMainCamera().position();

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof Player player)) continue;
            if (player == mc.player) continue;
            if (player.isInvisible()) continue;

            double x = Mth.lerp(pt, player.xo, player.getX()) - camPos.x;
            double y = Mth.lerp(pt, player.yo, player.getY()) - camPos.y;
            double z = Mth.lerp(pt, player.zo, player.getZ()) - camPos.z;

            poseStack.pushPose();
            poseStack.translate(x, y, z);

            if (boxes.isEnabled()) {
                renderBox(poseStack, player, buffers);
            }

            if (nameTags.isEnabled()) {
                renderNameTag(poseStack, player, buffers);
            }

            if (playerModel.isEnabled()) {
                renderPlayerModel(poseStack, player, buffers, pt);
            }

            poseStack.popPose();
        }

        buffers.endBatch();
    }

    private void renderBox(PoseStack poseStack, Player player, MultiBufferSource.BufferSource buffers) {
        double w = player.getBbWidth();
        double h = player.getBbHeight();
        float hw = (float) w / 2f;

        poseStack.pushPose();
        poseStack.translate(-hw, 0, -hw);

        int color = -1;
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.6f;

        float x0 = 0, y0 = 0, z0 = 0;
        float x1 = (float) w, y1 = (float) h, z1 = (float) w;

        float[][] lines = {
                {x0, y0, z0, x1, y0, z0}, {x0, y0, z1, x1, y0, z1},
                {x0, y1, z0, x1, y1, z0}, {x0, y1, z1, x1, y1, z1},
                {x0, y0, z0, x0, y1, z0}, {x1, y0, z0, x1, y1, z0},
                {x0, y0, z1, x0, y1, z1}, {x1, y0, z1, x1, y1, z1},
                {x0, y0, z0, x0, y0, z1}, {x1, y0, z0, x1, y0, z1},
                {x0, y1, z0, x0, y1, z1}, {x1, y1, z0, x1, y1, z1},
        };

        VertexConsumer vc = buffers.getBuffer(RenderTypes.lines());
        PoseStack.Pose last = poseStack.last();
        for (float[] line : lines) {
            vc.addVertex(last.pose(), line[0], line[1], line[2])
                    .setNormal(last, 0, 1, 0)
                    .setColor(r, g, b, a);
            vc.addVertex(last.pose(), line[3], line[4], line[5])
                    .setNormal(last, 0, 1, 0)
                    .setColor(r, g, b, a);
        }

        poseStack.popPose();
    }

    private void renderNameTag(PoseStack poseStack, Player player, MultiBufferSource.BufferSource buffers) {
        String name = player.getName().getString();
        double y = player.getBbHeight() + 0.3;

        poseStack.pushPose();
        poseStack.translate(0, y, 0);

        float tw = Minecraft.getInstance().font.width(name);
        poseStack.translate(-tw / 2f, 0, 0);

        Minecraft.getInstance().font.drawInBatch(
                name, 0, 0, -1, false,
                poseStack.last().pose(),
                buffers, Font.DisplayMode.NORMAL, 0, 15728880
        );

        poseStack.popPose();
    }

    private void renderPlayerModel(PoseStack poseStack, Player player, MultiBufferSource.BufferSource buffers, float partialTick) {
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        Minecraft mc = Minecraft.getInstance();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();

        // getPlayerRenderer возвращает AvatarRenderer<AbstractClientPlayer>
        AvatarRenderer<AbstractClientPlayer> avatarRenderer = dispatcher.getPlayerRenderer(clientPlayer);

        if (avatarRenderer != null) {
            AvatarRenderState state = avatarRenderer.createRenderState();
            avatarRenderer.extractRenderState(clientPlayer, state, partialTick);

            PlayerModel model = avatarRenderer.getModel();
            model.setupAnim(state);

            Identifier texture = state.skin.body().texturePath();

            poseStack.pushPose();
            poseStack.translate(0, player.getBbHeight() + 0.5, 0);
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - player.yBodyRot));
            poseStack.scale(0.5f, 0.5f, 0.5f);

            RenderType renderType = RenderTypes.entityCutoutNoCull(texture);
            VertexConsumer vc = buffers.getBuffer(renderType);
            model.renderToBuffer(poseStack, vc, 15728880, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }
}
