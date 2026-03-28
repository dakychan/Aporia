package com.ferra13671.discordipc.connection.packet.opcode;

import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.impl.RawPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.CloseConnectionPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.DispatchPacket;
import com.ferra13671.discordipc.connection.packet.impl.s2c.ErrorPacket;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;

import java.util.function.Function;

@AllArgsConstructor
public enum Opcode {
    Handshake(jsonObject -> {
        throw new UnsupportedOperationException("Cannot create C2S packet from json.");
    }),
    Frame(jsonObject -> {
        if (DispatchPacket.is(jsonObject))
            return new DispatchPacket(jsonObject);

        if (ErrorPacket.is(jsonObject))
            return new ErrorPacket(jsonObject);

        throw new UnsupportedOperationException("Cannot create C2S packet from json.");
    }),
    Close(jsonObject ->
            new CloseConnectionPacket(jsonObject.get("code").getAsInt(), jsonObject.get("message").getAsString())
    ),
    Ping(jsonObject -> new RawPacket()),
    Pong(jsonObject -> new RawPacket());

    public final Function<JsonObject, S2CPacket> toPacketFunction;

    private static final Opcode[] VALUES = values();

    public static Opcode valueOf(int i) {
        return VALUES[i];
    }
}
