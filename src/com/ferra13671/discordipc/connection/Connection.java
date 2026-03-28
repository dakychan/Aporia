package com.ferra13671.discordipc.connection;

import com.ferra13671.discordipc.connection.impl.UnixConnection;
import com.ferra13671.discordipc.connection.impl.WinConnection;
import com.ferra13671.discordipc.connection.packet.C2SPacket;
import com.ferra13671.discordipc.connection.packet.S2CPacket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public abstract class Connection {
    private final static String[] UNIX_TEMP_PATHS = { "XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP" };

    protected final Consumer<S2CPacket> callback;

    public Connection(Consumer<S2CPacket> onPacketCallback) {
        this.callback = onPacketCallback;
    }

    public static Connection open(Consumer<S2CPacket> callback) {
        String os = System.getProperty("os.name").toLowerCase();

        // Windows
        if (os.contains("win")) {
            for (int i = 0; i < 10; i++) {
                try {
                    return new WinConnection("\\\\.\\pipe\\discord-ipc-" + i, callback);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        // Unix
        else {
            String name = null;

            for (String tempPath : UNIX_TEMP_PATHS) {
                name = System.getenv(tempPath);
                if (name != null) break;
            }

            if (name == null) name = "/tmp";
            name += "/discord-ipc-";

            for (int i = 0; i < 10; i++) {
                try {
                    return new UnixConnection(name + i, callback);
                } catch (IOException ignored) {}
            }
        }

        return null;
    }

    public void write(C2SPacket packet) {
        write(C2SPacket.writeToBuffer(packet));
    }

    protected abstract void write(ByteBuffer buffer);

    public abstract void close();
}
