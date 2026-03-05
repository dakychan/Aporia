package aporia.su.util.user.network.api;

import aporia.su.mixin.client.ClientConnectionAccessor;
import aporia.su.mixin.client.IClientWorld;
import aporia.su.modules.impl.combat.aura.Angle;
import aporia.su.util.user.player.timer.TimerUtil;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.scoreboard.*;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.StringUtils;
import aporia.su.util.interfaces.IMinecraft;
import aporia.su.util.events.impl.player.PacketEvent;
import aporia.su.util.user.string.PlayerInteractionHelper;
import aporia.su.util.user.player.timer.StopWatch;

import java.nio.charset.StandardCharsets;
import java.util.UUID;


@Getter
@UtilityClass
public class Network implements IMinecraft {
    private final StopWatch pvpWatch = new StopWatch();
    public String server = "Vanilla";
    public float TPS = 20;
    public long timestamp;
    @Getter
    public int anarchy;
    @Getter
    public boolean pvpEnd;

    public void tick() {
        anarchy = getAnarchyMode();
        server = getServer();
        pvpEnd = inPvpEnd();
        if (inPvp()) pvpWatch.reset();
    }

    public void packet(PacketEvent e) {
        switch (e.getPacket()) {
            case WorldTimeUpdateS2CPacket time -> {
                long nanoTime = System.nanoTime();

                float maxTPS = 20;
                float rawTPS = maxTPS * (1e9f / (nanoTime - timestamp));

                TPS = MathHelper.clamp(rawTPS, 0, maxTPS);
                timestamp = nanoTime;
            }
            default -> {}
        }
    }

    public String getServer() {
        if (PlayerInteractionHelper.nullCheck() || mc.getNetworkHandler() == null || mc.getNetworkHandler().getServerInfo() == null || mc.getNetworkHandler().getBrand() == null) return "Vanilla";
        String serverIp = mc.getNetworkHandler().getServerInfo().address.toLowerCase();
        String brand = mc.getNetworkHandler().getBrand().toLowerCase();

        if (brand.contains("botfilter")) return "FunTime";
        else if (brand.contains("§6spooky§ccore")) return "SpookyTime";
        else if (serverIp.contains("funtime") || serverIp.contains("skytime") || serverIp.contains("space-times") || serverIp.contains("funsky")) return "CopyTime";
        else if (brand.contains("holyworld") || brand.contains("vk.com/idwok")) return "HolyWorld";
        else if (serverIp.contains("reallyworld")) return "ReallyWorld";
        else if (serverIp.contains("gulpvp")) return "GulPvP";
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
                        if (string != null && !string.isEmpty()) return Integer.parseInt(string.replace(" (1.20)", ""));
                    }
                }
            }
        }
        return -1;
    }

    public boolean isPvp() {
        return !pvpWatch.finished(500);
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
    public boolean isGulPvP() {return server.equals("GulPvP");}
    public boolean isHolyWorld() {return server.equals("HolyWorld");}
    public boolean isSpookyTime() {return server.equals("SpookyTime");}
    public boolean isAresMine() {return server.equals("aresmine");}
    public boolean isVanilla() {return server.equals("Vanilla");}

    @UtilityClass
    public static class NetworkUtility implements IMinecraft {
        private boolean shouldTriggerEvent = true;
        private boolean serverSprinting = false;

        @Getter
        private float tpsFactor = 0;
        private int received = 0;
        private long lastReceive = 0;
        private TimerUtil tpsTimer = new TimerUtil();

        public void pauseEvents() {
            shouldTriggerEvent = false;
        }

        public void resumeEvents() {
            shouldTriggerEvent = true;
        }

        public boolean shouldTriggerEvent() {
            return shouldTriggerEvent;
        }

        public void updateServerSprint(boolean sprint) {
            serverSprinting = sprint;
        }

        public boolean serverSprinting() {
            return serverSprinting;
        }

        public void sendWithoutEvent(Runnable runnable) {
            pauseEvents();
            runnable.run();
            resumeEvents();
        }

        public void sendWithoutEvent(Packet<?> packet) {
            pauseEvents();
            send(packet);
            resumeEvents();
        }

        public void send(Packet<?> packet) {
            if (mc.getNetworkHandler() == null) return;

            if (packet instanceof ClickSlotC2SPacket click) {
                mc.interactionManager.clickSlot(click.syncId(), click.slot(), click.button(), click.actionType(), mc.player);
            } else {
                mc.getNetworkHandler().sendPacket(packet);
            }
        }

        public void sendInputPacket(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
            PlayerInput input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
            mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(input));
        }

        public void sendOnlySneak(boolean sneak) {
            PlayerInput playerInput = mc.player.input.playerInput;
            sendInputPacket(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), playerInput.jump(), sneak, playerInput.sprint());
        }

        public void sendUse(Hand hand) {
            sendUse(hand, new Angle(mc.player.getYaw(), mc.player.getPitch()));
        }

        public void sendUse(Hand hand, Angle angle) {
            try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
                int i = pendingUpdateManager.getSequence();
                PlayerInteractItemC2SPacket packet = new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch());
                NetworkUtility.send(packet);
            }
        }

        public void sendUse(Hand hand, BlockHitResult hitResult) {
            try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
                int i = pendingUpdateManager.getSequence();
                PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(hand, hitResult, i);
                NetworkUtility.send(packet);
            }
        }

        public boolean is(String server) {
            return mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getServerInfo().address.contains(server);
        }

        public void handleCPacket(Packet<?> packet) {
            if (packet instanceof PlayerMoveC2SPacket e) {
                PlayerState.lastGround = e.isOnGround();
                PlayerState.lastVertical = mc.player.verticalCollision;
            }
        }

        public void handleSPacket(Packet<?> packet) {
            if (packet instanceof WorldTimeUpdateS2CPacket e) {
                lastReceive = System.currentTimeMillis();
            }
        }

        public void handlePacket(Packet<?> packet) {
            if (!(mc.getNetworkHandler() instanceof ClientPlayNetworkHandler net)) return;
            if (mc.isOnThread()) {
                ClientConnectionAccessor.handlePacket(packet, net);
            } else {
                mc.execute(() -> ClientConnectionAccessor.handlePacket(packet, net));
            }
        }

        @UtilityClass
        public class PlayerState {
            public boolean lastGround = false, lastVertical = false;
            public int lastTp = 0;
        }

        public UUID offlineUUID(String name) {
            return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        }
    }
}
