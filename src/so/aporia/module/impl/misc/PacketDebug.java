/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.module.impl.misc;

import net.minecraft.network.protocol.Packet;
import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.utils.events.EventBus;
import so.aporia.utils.events.EventHandler;
import so.aporia.utils.events.impl.PacketEvent;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.logger.Logger;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * PacketDebug — логирует ВСЕ пакеты в .log файл (append).
 * При каждом enable создаёт новый parse_<timestamp>.log
 */
public final class PacketDebug extends Module {

    public final BooleanSetting logInbound = new BooleanSetting(
        "Log Inbound", "Входящие пакеты", true
    );
    public final BooleanSetting logOutbound = new BooleanSetting(
        "Log Outbound", "Исходящие пакеты", true
    );

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private Path sessionFile;
    private long packetCount = 0;

    public PacketDebug() {
        super("PacketDebug", Category.MISC);
    }

    @Override
    protected void onEnable() {
        packetCount = 0;
        String ts = LocalDateTime.now().format(FILE_FMT);
        sessionFile = FilesManager.ROOT.resolve("packets/parse_" + ts + ".log");
        try {
            String header = "=== PacketDebug Session: " + ts + " ===\n\n";
            FilesManager.append(sessionFile, header);
            Logger.success("[PacketDebug] ENABLED → " + sessionFile.getFileName());
        } catch (Exception e) {
            Logger.error("[PacketDebug] init failed: " + e.getMessage());
            sessionFile = null;
            return;
        }
        EventBus.INSTANCE.register(this);
    }

    @Override
    protected void onDisable() {
        EventBus.INSTANCE.unregister(this);
        try {
            String footer = "\n=== Session ended (" + packetCount + " packets) ===\n";
            FilesManager.append(sessionFile, footer);
            Logger.success("[PacketDebug] DISABLED — logged " + packetCount + " packets");
        } catch (Exception e) {
            Logger.error("[PacketDebug] disable failed: " + e.getMessage());
        }
        sessionFile = null;
    }

    @EventHandler
    public void onPacket(PacketEvent event) {
        if (sessionFile == null) return;
        if (event.direction() == PacketEvent.Direction.INBOUND  && !logInbound.isEnabled())  return;
        if (event.direction() == PacketEvent.Direction.OUTBOUND && !logOutbound.isEnabled()) return;
        Packet<?> pkt = event.packet();
        String simple = pkt.getClass().getSimpleName();
        String dir = event.direction() == PacketEvent.Direction.INBOUND ? "IN " : "OUT";
        String time = LocalDateTime.now().format(TIME_FMT);
        String entry = '[' + time + "] [" + dir + "] " + simple + '\n'
            + "  class: " + pkt.getClass().getName() + '\n'
            + "  data : " + pkt + '\n'
            + "──────────────────────────────────────────\n";
        try {
            FilesManager.append(sessionFile, entry);
            packetCount++;
        } catch (Exception e) {
            Logger.error("[PacketDebug] write failed: " + e.getMessage());
        }
    }
}
