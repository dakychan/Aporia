package com.ferra13671.discordipc.connection.packet.impl;

import com.ferra13671.discordipc.connection.packet.S2CPacket;
import com.ferra13671.discordipc.connection.packet.opcode.Opcode;

public class RawPacket implements S2CPacket {

    @Override
    public Opcode getOpcode() {
        return null;
    }
}
