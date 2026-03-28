package com.ferra13671.discordipc.connection.packet.impl.s2c;

import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.opcode.Opcode;
import com.google.gson.JsonObject;

public record ErrorPacket(int code, String message) implements S2CPacket {

    public ErrorPacket(JsonObject jsonObject) {
        this(
                jsonObject.getAsJsonObject("data").get("code").getAsInt(),
                jsonObject.getAsJsonObject("data").get("message").getAsString()
        );
    }

    @Override
    public Opcode getOpcode() {
        return Opcode.Frame;
    }

    public static boolean is(JsonObject jsonObject) {
        return jsonObject.has("evt") && jsonObject.get("evt").getAsString().equals("ERROR");
    }
}
