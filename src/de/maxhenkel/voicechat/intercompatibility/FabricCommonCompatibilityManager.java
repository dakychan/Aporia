package de.maxhenkel.voicechat.intercompatibility;

import com.mojang.brigadier.CommandDispatcher;
import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.events.PlayerEvents;
import de.maxhenkel.voicechat.events.ServerVoiceChatEvents;
import de.maxhenkel.voicechat.events.VanishEvents;
import de.maxhenkel.voicechat.net.FabricNetManager;
import de.maxhenkel.voicechat.net.NetManager;
import de.maxhenkel.voicechat.permission.FabricPermissionManager;
import de.maxhenkel.voicechat.permission.PermissionManager;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class FabricCommonCompatibilityManager extends CommonCompatibilityManager {
    private volatile MinecraftServer currentServer;
    private final List<Consumer<MinecraftServer>> serverStartingListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<MinecraftServer>> serverStoppingListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<CommandDispatcher<CommandSourceStack>>> commandRegistrationListeners = new CopyOnWriteArrayList<>();

    public static void setServer(MinecraftServer server) {
        if (INSTANCE instanceof FabricCommonCompatibilityManager manager) {
            manager.currentServer = server;
        }
    }
    public static void fireServerStarting(MinecraftServer server) {
        if (INSTANCE instanceof FabricCommonCompatibilityManager manager) {
            manager.currentServer = server;
            for (Consumer<MinecraftServer> listener : manager.serverStartingListeners) {
                listener.accept(server);
            }
        }
    }

    public static void fireServerStopping(MinecraftServer server) {
        if (INSTANCE instanceof FabricCommonCompatibilityManager manager) {
            for (Consumer<MinecraftServer> listener : manager.serverStoppingListeners) {
                listener.accept(server);
            }
        }
    }

    public static void fireRegisterServerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (INSTANCE instanceof FabricCommonCompatibilityManager manager) {
            for (Consumer<CommandDispatcher<CommandSourceStack>> listener : manager.commandRegistrationListeners) {
                listener.accept(dispatcher);
            }
        }
    }
    @Override
    public String getModVersion() {
        return Voicechat.VERSION;
    }
    @Override
    public String getModName() {
        return Voicechat.MOD_NAME;
    }

    @Override
    public Path getGameDirectory() {
        return Path.of(".");
    }

    @Override
    public void emitServerVoiceChatConnectedEvent(ServerPlayer player) {
        ServerVoiceChatEvents.VOICECHAT_CONNECTED.invoker().accept(player);
    }

    @Override
    public void emitServerVoiceChatDisconnectedEvent(UUID clientID) {
        ServerVoiceChatEvents.VOICECHAT_DISCONNECTED.invoker().accept(clientID);
    }

    @Override
    public void emitPlayerCompatibilityCheckSucceeded(ServerPlayer player) {
        ServerVoiceChatEvents.VOICECHAT_COMPATIBILITY_CHECK_SUCCEEDED.invoker().accept(player);
    }

    @Override
    public void onServerVoiceChatConnected(Consumer<ServerPlayer> onVoiceChatConnected) {
        ServerVoiceChatEvents.VOICECHAT_CONNECTED.register(onVoiceChatConnected);
    }

    @Override
    public void onServerVoiceChatDisconnected(Consumer<UUID> onVoiceChatDisconnected) {
        ServerVoiceChatEvents.VOICECHAT_DISCONNECTED.register(onVoiceChatDisconnected);
    }


    @Override
    public void onServerStarting(Consumer<MinecraftServer> onServerStarting) {
        serverStartingListeners.add(onServerStarting);
    }

    @Override
    public void onServerStopping(Consumer<MinecraftServer> onServerStopping) {
        serverStoppingListeners.add(onServerStopping);
    }

    @Override
    public void onPlayerLoggedIn(Consumer<ServerPlayer> onPlayerLoggedIn) {
        PlayerEvents.PLAYER_LOGGED_IN.register(onPlayerLoggedIn);
    }

    @Override
    public void onPlayerLoggedOut(Consumer<ServerPlayer> onPlayerLoggedOut) {
        PlayerEvents.PLAYER_LOGGED_OUT.register(onPlayerLoggedOut);
    }

    @Override
    public void onPlayerHide(BiConsumer<ServerPlayer, ServerPlayer> onPlayerHide) {
        VanishEvents.ON_VANISH.register(onPlayerHide);
    }

    @Override
    public void onPlayerShow(BiConsumer<ServerPlayer, ServerPlayer> onPlayerShow) {
        VanishEvents.ON_UNVANISH.register(onPlayerShow);
    }

    @Override
    public void onPlayerCompatibilityCheckSucceeded(Consumer<ServerPlayer> onPlayerCompatibilityCheckSucceeded) {
        ServerVoiceChatEvents.VOICECHAT_COMPATIBILITY_CHECK_SUCCEEDED.register(onPlayerCompatibilityCheckSucceeded);
    }
    @Override
    public void onRegisterServerCommands(Consumer<CommandDispatcher<CommandSourceStack>> onRegisterServerCommands) {
        commandRegistrationListeners.add(onRegisterServerCommands);
    }

    private FabricNetManager netManager;

    @Override
    public NetManager getNetManager() {
        if (netManager == null) {
            netManager = new FabricNetManager();
        }
        return netManager;
    }

    @Override
    public boolean isDevEnvironment() {
        return SharedConstants.IS_RUNNING_IN_IDE;
    }

    @Override
    public boolean isDedicatedServer() {
        return currentServer != null && currentServer.isDedicatedServer();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return true;
    }

    @Override
    public List<VoicechatPlugin> loadPlugins() {
        List<VoicechatPlugin> plugins = new ArrayList<>();
        ServiceLoader.load(VoicechatPlugin.class, Voicechat.class.getClassLoader()).forEach(plugins::add);
        return plugins;
    }

    @Override
    public PermissionManager createPermissionManager() {
        return new FabricPermissionManager();
    }

    @Override
    public boolean canSee(ServerPlayer player, ServerPlayer other) {
        return true;
    }

}
