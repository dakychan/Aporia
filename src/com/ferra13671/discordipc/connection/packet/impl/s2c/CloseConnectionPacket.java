package com.ferra13671.discordipc.connection.packet.impl.s2c;

import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.opcode.Opcode;

public record CloseConnectionPacket(int code, String message) implements S2CPacket {

    @Override
    public Opcode getOpcode() {
        return Opcode.Close;
    }
}
