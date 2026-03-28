package com.ferra13671.discordipc.connection.packet;

import com.ferra13671.discordipc.connection.packet.opcode.Opcode;

public interface Packet {

    Opcode getOpcode();
}
