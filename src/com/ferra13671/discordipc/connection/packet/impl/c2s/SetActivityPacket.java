package com.ferra13671.discordipc.connection.packet.impl.c2s;

import com.ferra13671.discordipc.activity.ActivityInfo;
import com.ferra13671.discordipc.connection.packet.C2SPacket;
import com.ferra13671.discordipc.connection.packet.opcode.Opcode;
import com.google.gson.JsonObject;

public class SetActivityPacket implements C2SPacket {
    private final int pid;
    private final ActivityInfo activityInfo;

    public SetActivityPacket(int pid, ActivityInfo activityInfo) {
        this.pid = pid;
        this.activityInfo = activityInfo;
    }

    @Override
    public Opcode getOpcode() {
        return Opcode.Frame;
    }

    @Override
    public JsonObject toJson() {
        JsonObject argsObject = new JsonObject();
        argsObject.addProperty("pid", this.pid);
        argsObject.add("activity", this.activityInfo.toJson());

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("cmd", "SET_ACTIVITY");
        jsonObject.add("args", argsObject);

        return jsonObject;
    }
}
