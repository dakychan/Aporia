package zenith.zov.utility.game.other;

import net.minecraft.network.packet.Packet;
import zenith.zov.utility.interfaces.IMinecraft;

import java.util.ArrayList;
import java.util.List;
@Deprecated
public class NetworkUtils implements IMinecraft {

    private static final List<Packet<?>> silentPackets = new ArrayList<>();

    public static void sendSilentPacket(Packet<?> packet) {
        silentPackets.add(packet);
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static void sendPacket(Packet<?> packet) {
        mc.getNetworkHandler().sendPacket(packet);
    }

    public static List<Packet<?>> getSilentPackets() {
        return silentPackets;
    }

    public static void clearSilentPackets() {
        silentPackets.clear();
    }
} 