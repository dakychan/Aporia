package de.maxhenkel.voicechat.intercompatibility;

import com.mojang.blaze3d.platform.InputConstants;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.events.*;
import de.maxhenkel.voicechat.mixin.ConnectionAccessor;
import de.maxhenkel.voicechat.voice.client.ClientVoicechatConnection;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.Connection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.RepositorySource;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class FabricClientCompatibilityManager extends ClientCompatibilityManager {

    private static final Identifier VOICE_CHAT_ICON_LAYER = Identifier.fromNamespaceAndPath(Voicechat.MODID, "hud");
    private static final Identifier EARLY_JOIN = Identifier.fromNamespaceAndPath(Voicechat.MODID, "early_join");

    private static final Minecraft mc = Minecraft.getInstance();
    private final List<Runnable> clientTickListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> joinWorldListeners = new CopyOnWriteArrayList<>();

    public static void fireClientTick() {
        if (INSTANCE instanceof FabricClientCompatibilityManager manager) {
            for (Runnable listener : manager.clientTickListeners) {
                listener.run();
            }
        }
    }

    public static void fireJoinWorld() {
        if (INSTANCE instanceof FabricClientCompatibilityManager manager) {
            for (Runnable listener : manager.joinWorldListeners) {
                listener.run();
            }
        }
    }

    public static void onRenderVoiceChatLayer(GuiGraphics guiGraphics, DeltaTracker deltaTracker) {
        RenderEvents.RENDER_HUD.invoker().accept(guiGraphics);
    }

    @Override
    public void onRenderNamePlate(RenderNameplateEvent onRenderNamePlate) {
        RenderEvents.RENDER_NAMEPLATE.register(onRenderNamePlate);
    }

    @Override
    public void onRenderHUD(RenderHUDEvent onRenderHUD) {
        RenderEvents.RENDER_HUD.register(guiGraphics -> onRenderHUD.render(guiGraphics, mc.getDeltaTracker().getRealtimeDeltaTicks()));
    }

    @Override
    public void onKeyboardEvent(KeyboardEvent onKeyboardEvent) {
        InputEvents.KEYBOARD_KEY.register(onKeyboardEvent);
    }

    @Override
    public void onMouseEvent(MouseEvent onMouseEvent) {
        InputEvents.MOUSE_KEY.register(onMouseEvent);
    }

    @Override
    public void onClientTick(Runnable onClientTick) {
        clientTickListeners.add(onClientTick);
    }

//    @Override
//    public InputConstants.Key getBoundKeyOf(KeyMapping keyBinding) {
//        if (keyBinding == null) {
//            return InputConstants.UNKNOWN;
//        }
//        return keyBinding.key;
//    }
@Override
public InputConstants.Key getBoundKeyOf(KeyMapping keyBinding) {
    if (keyBinding == null) {
        return InputConstants.UNKNOWN;
    }
    return InputConstants.getKey(keyBinding.saveString());
}

    @Override
    public void onHandleKeyBinds(Runnable onHandleKeyBinds) {
        InputEvents.HANDLE_KEYBINDS.register(onHandleKeyBinds);
    }

    @Override
    public KeyMapping registerKeyBinding(KeyMapping keyBinding) {
        if (mc.options != null) {
            KeyMapping[] existing = mc.options.keyMappings;
            boolean alreadyRegistered = Arrays.stream(existing).anyMatch(mapping -> mapping == keyBinding);
            if (!alreadyRegistered) {
                KeyMapping[] updated = Arrays.copyOf(existing, existing.length + 1);
                updated[updated.length - 1] = keyBinding;
                mc.options.keyMappings = updated;
                KeyMapping.resetMapping();
            }
        }
        return keyBinding;
    }

    @Override
    public void emitVoiceChatConnectedEvent(ClientVoicechatConnection client) {
        ClientVoiceChatEvents.VOICECHAT_CONNECTED.invoker().accept(client);
    }

    @Override
    public void emitVoiceChatDisconnectedEvent() {
        ClientVoiceChatEvents.VOICECHAT_DISCONNECTED.invoker().run();
    }

    @Override
    public void onVoiceChatConnected(Consumer<ClientVoicechatConnection> onVoiceChatConnected) {
        ClientVoiceChatEvents.VOICECHAT_CONNECTED.register(onVoiceChatConnected);
    }

    @Override
    public void onVoiceChatDisconnected(Runnable onVoiceChatDisconnected) {
        ClientVoiceChatEvents.VOICECHAT_DISCONNECTED.register(onVoiceChatDisconnected);
    }

    @Override
    public void emitDisconnectedEvent() {
        ClientWorldEvents.DISCONNECT.invoker().run();
    }

    @Override
    public void onDisconnect(Runnable onDisconnect) {
        ClientWorldEvents.DISCONNECT.register(onDisconnect);
    }

    @Override
    public void onJoinWorld(Runnable onJoinWorld) {
        joinWorldListeners.add(onJoinWorld);
    }

    @Override
    public void onPublishServer(Consumer<Integer> onPublishServer) {
        PublishServerEvents.SERVER_PUBLISHED.register(onPublishServer);
    }

    @Override
    public SocketAddress getSocketAddress(Connection connection) {
        return ((ConnectionAccessor) connection).getChannel().remoteAddress();
    }

    @Override
    public void addResourcePackSource(RepositorySource repositorySource) {
        mc.getResourcePackRepository().addSource(repositorySource);
    }
}
