package com.ferra13671.discordipc.connection.packet;

import com.google.gson.JsonObject;

import java.nio.ByteBuffer;
import java.util.UUID;

public interface C2SPacket extends Packet {

    JsonObject toJson();

    static ByteBuffer writeToBuffer(C2SPacket packet) {
        JsonObject packetObject = packet.toJson();

        packetObject.addProperty("nonce", UUID.randomUUID().toString());

        byte[] d = packetObject.toString().getBytes();
        ByteBuffer packetBuf = ByteBuffer.allocate(d.length + 8);
        packetBuf.putInt(Integer.reverseBytes(packet.getOpcode().ordinal()));
        packetBuf.putInt(Integer.reverseBytes(d.length));
        packetBuf.put(d);

        packetBuf.rewind();
        return packetBuf;
    }
}
