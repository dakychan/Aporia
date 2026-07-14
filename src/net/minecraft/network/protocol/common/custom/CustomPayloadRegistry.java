package net.minecraft.network.protocol.common.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CustomPayloadRegistry {

    private static final Map<Identifier, StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> SERVERBOUND = new ConcurrentHashMap<>();
    private static final Map<Identifier, StreamCodec<? super RegistryFriendlyByteBuf, ? extends CustomPacketPayload>> CLIENTBOUND = new ConcurrentHashMap<>();
    private static final Map<Identifier, StreamCodec<? super FriendlyByteBuf, ? extends CustomPacketPayload>> CLIENTBOUND_CONFIG = new ConcurrentHashMap<>();

    static {
        registerServerbound(BrandPayload.TYPE, BrandPayload.STREAM_CODEC);
        registerClientbound(BrandPayload.TYPE, BrandPayload.STREAM_CODEC);
        registerClientboundConfig(BrandPayload.TYPE, BrandPayload.STREAM_CODEC);
    }

    private CustomPayloadRegistry() {
    }

    public static <T extends CustomPacketPayload> void registerServerbound(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        SERVERBOUND.put(type.id(), codec);
    }

    public static <T extends CustomPacketPayload> void registerClientbound(CustomPacketPayload.Type<T> type, StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        CLIENTBOUND.put(type.id(), codec);
    }

    public static <T extends CustomPacketPayload> void registerClientboundConfig(CustomPacketPayload.Type<T> type, StreamCodec<? super FriendlyByteBuf, T> codec) {
        CLIENTBOUND_CONFIG.put(type.id(), codec);
    }

    public static StreamCodec<FriendlyByteBuf, CustomPacketPayload> serverboundCodec(int maxPayloadSize) {
        return new StreamCodec<>() {
            @Override
            public void encode(FriendlyByteBuf buf, CustomPacketPayload payload) {
                write(buf, payload, SERVERBOUND);
            }

            @Override
            public CustomPacketPayload decode(FriendlyByteBuf buf) {
                return read(buf, SERVERBOUND, maxPayloadSize);
            }
        };
    }

    public static StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload> clientboundCodec(int maxPayloadSize) {
        return new StreamCodec<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buf, CustomPacketPayload payload) {
                write(buf, payload, CLIENTBOUND);
            }

            @Override
            public CustomPacketPayload decode(RegistryFriendlyByteBuf buf) {
                return read(buf, CLIENTBOUND, maxPayloadSize);
            }
        };
    }

    public static StreamCodec<FriendlyByteBuf, CustomPacketPayload> clientboundConfigCodec(int maxPayloadSize) {
        return new StreamCodec<>() {
            @Override
            public void encode(FriendlyByteBuf buf, CustomPacketPayload payload) {
                write(buf, payload, CLIENTBOUND_CONFIG);
            }

            @Override
            public CustomPacketPayload decode(FriendlyByteBuf buf) {
                return read(buf, CLIENTBOUND_CONFIG, maxPayloadSize);
            }
        };
    }

    private static <B extends FriendlyByteBuf> void write(B buf, CustomPacketPayload payload, Map<Identifier, StreamCodec<? super B, ? extends CustomPacketPayload>> map) {
        buf.writeIdentifier(payload.type().id());
        StreamCodec<? super B, ? extends CustomPacketPayload> codec = map.get(payload.type().id());
        if (codec == null) {
            throw new IllegalArgumentException("Unknown custom payload type: " + payload.type().id());
        }
        encode(codec, buf, payload);
    }

    private static <B extends FriendlyByteBuf> CustomPacketPayload read(
            B buf,
            Map<Identifier, StreamCodec<? super B, ? extends CustomPacketPayload>> map,
            int maxPayloadSize
    ) {
        Identifier id = buf.readIdentifier();
        StreamCodec<? super B, ? extends CustomPacketPayload> codec = map.get(id);
        if (codec == null) {
            return DiscardedPayload.codec(id, maxPayloadSize).decode(buf);
        }
        return decode(codec, buf);
    }

    @SuppressWarnings("unchecked")
    private static <B extends FriendlyByteBuf, T extends CustomPacketPayload> void encode(StreamCodec<? super B, ? extends CustomPacketPayload> codec, B buf, CustomPacketPayload payload) {
        ((StreamCodec<B, T>) codec).encode(buf, (T) payload);
    }

    @SuppressWarnings("unchecked")
    private static <B extends FriendlyByteBuf, T extends CustomPacketPayload> CustomPacketPayload decode(StreamCodec<? super B, ? extends CustomPacketPayload> codec, B buf) {
        return ((StreamCodec<B, T>) codec).decode(buf);
    }
}
