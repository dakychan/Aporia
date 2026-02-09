package zenith.zov.utility.game.server;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import lombok.Getter;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;

import zenith.zov.base.events.impl.player.EventUpdate;
import zenith.zov.base.events.impl.server.EventPacket;
import zenith.zov.utility.game.player.PlayerIntersectionUtil;
import zenith.zov.utility.interfaces.IMinecraft;
import zenith.zov.utility.math.Timer;

import java.util.Objects;

@Getter
public class ServerHandler implements IMinecraft {

    private final Timer pvpWatch = new Timer();
    private String server = "Vanilla";
    private float TPS = 20;
    private long timestamp;
    private boolean serverSprint;
    private int anarchy;

    private boolean pvpEnd;
    public ServerHandler() {
        EventManager.register(this);
    }
    @EventTarget
    public void tick(EventUpdate eventUpdate) {
        anarchy = getAnarchyMode();
        server = updateServer();
        pvpEnd = inPvpEnd();
        if (inPvp()) pvpWatch.reset();
    }
    @EventTarget
    public void packet(EventPacket e) {
        if (e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            long nanoTime = System.nanoTime();

            float maxTPS = 20;
            float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));

            TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
            timestamp = nanoTime;
        }
    }

    @EventTarget
    public void onPacket(EventPacket e) {
        if (e.getPacket() instanceof ClientCommandC2SPacket command) {
            if (command.getMode().equals(ClientCommandC2SPacket.Mode.START_SPRINTING)) {
                e.setCancelled(serverSprint);
                serverSprint = true;
            } else if (command.getMode().equals(ClientCommandC2SPacket.Mode.STOP_SPRINTING)) {
                e.setCancelled(!serverSprint);
                serverSprint = false;
            }
        }

    }
    private String updateServer() {
        if (PlayerIntersectionUtil.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld")||brand.contains("leaf") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        return "Vanilla";
    }

    private int getAnarchyMode() {
        Scoreboard scoreboard = mc.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        switch (server) {
            case "FunTime" -> {
                if (objective != null) {
                    String[] string = objective.getDisplayName().getString().split("-");
                    if (string.length > 1) return Integer.parseInt(string[1]);
                }
            }
            case "HolyWorld" -> {
                for (ScoreboardEntry scoreboardEntry : scoreboard.getScoreboardEntries(objective)) {
                    String text = Team.decorateName(scoreboard.getScoreHolderTeam(scoreboardEntry.owner()), scoreboardEntry.name()).getString();
                    if (!text.isEmpty()) {
                        String string = StringUtils.substringBetween(text, "#", " -◆-");
                        if (string != null && !string.isEmpty()) return Integer.parseInt(string);
                    }
                }
            }
        }
        return -1;
    }

    public boolean isPvp() {
        return !pvpWatch.finished(250);
    }

    private boolean inPvp() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase()).anyMatch(s -> s.contains("pvp") || s.contains("пвп"));
    }

    private boolean inPvpEnd() {
        return mc.inGameHud.getBossBarHud().bossBars.values().stream().map(c -> c.getName().getString().toLowerCase())
                .anyMatch(s -> (s.contains("pvp") || s.contains("пвп")) && (s.contains("0") || s.contains("1")));
    }

    public String getWorldType() {
        return mc.world.getRegistryKey().getValue().getPath();
    }

    public boolean isCopyTime() {return server.equals("CopyTime") || server.equals("SpookyTime") || server.equals("FunTime");}
    public boolean isFunTime() {return server.equals("FunTime");}
    public boolean isReallyWorld() {return server.equals("ReallyWorld");}
    public boolean isHolyWorld() {return server.equals("HolyWorld");}
    public boolean isVanilla() {return server.equals("Vanilla");}
}
