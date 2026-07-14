package de.maxhenkel.voicechat.net;

import de.maxhenkel.voicechat.Voicechat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPayloadRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FabricNetManager extends ClientServerNetManager {

    private final Set<Identifier> packets = new HashSet<>();
    private final Map<Identifier, Channel<?>> serverChannels = new HashMap<>();
    private final Map<Identifier, ClientServerChannel<?>> clientChannels = new HashMap<>();

    public Set<Identifier> getPackets() {
        return packets;
    }

    @Override
    public <T extends Packet<T>> Channel<T> registerReceiver(Class<T> packetType, boolean toClient, boolean toServer) {
        ClientServerChannel<T> channel = new ClientServerChannel<>();
        try {
            T dummyPacket = packetType.getDeclaredConstructor().newInstance();
            CustomPacketPayload.Type<T> type = dummyPacket.type();
            packets.add(type.id());

            StreamCodec<FriendlyByteBuf, T> serverCodec = new StreamCodec<>() {
                @Override
                public void encode(FriendlyByteBuf buf, T packet) {
                    packet.toBytes(buf);
                }

                @Override
                public T decode(FriendlyByteBuf buf) {
                    try {
                        T packet = packetType.getDeclaredConstructor().newInstance();
                        packet.fromBytes(buf);
                        return packet;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            StreamCodec<RegistryFriendlyByteBuf, T> clientCodec = new StreamCodec<>() {
                @Override
                public void encode(RegistryFriendlyByteBuf buf, T packet) {
                    packet.toBytes(buf);
                }

                @Override
                public T decode(RegistryFriendlyByteBuf buf) {
                    try {
                        T packet = packetType.getDeclaredConstructor().newInstance();
                        packet.fromBytes(buf);
                        return packet;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            if (toServer) {
                CustomPayloadRegistry.registerServerbound(type, serverCodec);
                serverChannels.put(type.id(), channel);
            }
            if (toClient) {
                CustomPayloadRegistry.registerClientbound(type, clientCodec);
                clientChannels.put(type.id(), channel);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
        return channel;
    }

    @Override
    protected void sendToServerInternal(Packet<?> packet) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(packet));
        }
    }

    @Override
    public void sendToClient(Packet<?> packet, ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(packet));
    }

    public boolean handleServerPayload(ServerPlayer player, CustomPacketPayload payload) {
        if (!(payload instanceof Packet<?> packet)) {
            return false;
        }
        Channel<?> channel = serverChannels.get(packet.type().id());
        if (channel == null) {
            return false;
        }
        if (Voicechat.SERVER != null && !Voicechat.SERVER.isCompatible(player) && !(packet instanceof RequestSecretPacket)) {
            return true;
        }
        handleServerPacket(channel, player, packet);
        return true;
    }

    public boolean handleClientPayload(LocalPlayer player, CustomPacketPayload payload) {
        if (!(payload instanceof Packet<?> packet)) {
            return false;
        }
        ClientServerChannel<?> channel = clientChannels.get(packet.type().id());
        if (channel == null) {
            return false;
        }
        handleClientPacket(channel, player, packet);
        return true;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Packet<T>> void handleServerPacket(Channel<?> channel, ServerPlayer player, Packet<?> packet) {
        ((Channel<T>) channel).onServerPacket(player, (T) packet);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Packet<T>> void handleClientPacket(ClientServerChannel<?> channel, LocalPlayer player, Packet<?> packet) {
        ((ClientServerChannel<T>) channel).onClientPacket(player, (T) packet);
    }
}
