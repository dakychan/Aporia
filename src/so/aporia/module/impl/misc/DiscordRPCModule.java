/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.misc;

import com.chaos.annotation.Obfuscate;
import com.ferra13671.discordipc.DiscordIPC;
import com.ferra13671.discordipc.IPCUser;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.utils.user.logger.Logger;
import so.aporia.utils.user.render.core.AporiaRenderer;
import com.ferra13671.discordipc.activity.ActivityInfo;

import com.chaos.annotation.ChaosNative;

@Obfuscate
public final class DiscordRPCModule extends Module {
    private static final long APP_ID = 1471901603287142421L;
    
    private DiscordIPC ipc;
    private IPCUser discordUser;
    private Identifier avatarId;

    public DiscordRPCModule() {
        super("Discord RPC", Category.MISC);
    }

    @Override
    protected void onEnable() {
        try {
            ipc = new DiscordIPC();
            ipc.setOnReady(() -> {
                discordUser = ipc.getUser();
                updateActivity();
            });
            ipc.setOnError((code, message) -> {
                Logger.error("Discord RPC error: " + message);
            });

            ipc.start(APP_ID);
        } catch (Exception e) {
            ipc = null;
        }
    }

    public void loadAvatarOnRenderThread() {
        if (avatarId != null || discordUser == null) return;
        
        try {
            var userAvatar = discordUser.getAvatarImage();
            if (userAvatar == null || userAvatar.inputStream() == null) {
                return;
            }
            
            avatarId = AporiaRenderer.INSTANCE.loadImage(userAvatar.inputStream());
        } catch (Exception e) {
            Logger.warn("Failed to load Discord avatar: " + e.getMessage());
        }
    }

    @Override
    protected void onDisable() {
        if (ipc != null) {
            ipc.stop();
            ipc = null;
        }
        discordUser = null;
        avatarId = null;
    }

    private void updateActivity() {
        if (ipc == null || !ipc.isConnected()) return;

        Minecraft mc = Minecraft.getInstance();
        String state = mc.getConnection() != null ? "Playing on server" : "In main menu";

        ipc.updateActivity(info -> new ActivityInfo(
                "Aporia Cheat",
                state,
                null,
                null,
                null,
                null,
                System.currentTimeMillis() / 1000,
                null,
                null,
                null
        ));
    }

    public IPCUser getDiscordUser() {
        return discordUser;
    }

    public Identifier getAvatarId() {
        return avatarId;
    }

    public DiscordIPC getIPC() {
        return ipc;
    }

    @Override
    public java.util.List<so.aporia.module.settings.Setting<?>> getSettings() {
        return java.util.Collections.emptyList();
    }
}
