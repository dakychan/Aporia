package com.ferra13671.discordipc.connection.packet.impl.s2c;

import com.ferra13671.discordipc.IPCUser;
import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.opcode.Opcode;
import com.google.gson.JsonObject;

public record DispatchPacket(IPCUser discordUser) implements S2CPacket {

    public DispatchPacket(JsonObject jsonObject) {
        this(IPCUser.fromJson(jsonObject.getAsJsonObject("data").getAsJsonObject("user")));
    }

    @Override
    public Opcode getOpcode() {
        return Opcode.Frame;
    }

    public static boolean is(JsonObject jsonObject) {
        return jsonObject.has("cmd") && jsonObject.get("cmd").getAsString().equals("DISPATCH");
    }
}
