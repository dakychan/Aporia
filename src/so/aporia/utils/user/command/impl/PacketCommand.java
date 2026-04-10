/*
 * Copyright (c) 2025-2026 BEVoid Project
 * Distributed under the BEVoid Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils.user.command.impl;

import so.aporia.module.ModuleManager;
import so.aporia.module.impl.misc.PacketDebug;
import so.aporia.utils.files.FilesManager;
import so.aporia.utils.user.command.Command;
import so.aporia.utils.user.command.CommandManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * .packet log   — показать список файлов логов
 * .packet save  — сохранить текущий лог в буфер обмена (или вывести путь)
 */
public class PacketCommand implements Command {

    @Override
    public String name() { return "packet"; }

    @Override
    public String description() { return "Packet debug: .packet log | .packet save"; }

    @Override
    public void execute(String[] args) {
        if (args.length < 2) {
            CommandManager.chat("§cUsage: .packet log | .packet save");
            return;
        }

        String subCmd = args[1].toLowerCase();

        if (subCmd.equals("log")) {
            handleLog();
        } else if (subCmd.equals("save")) {
            handleSave();
        } else {
            CommandManager.chat("§cUnknown subcommand: " + subCmd);
        }
    }

    private void handleLog() {
        Path packetsDir = FilesManager.ROOT.resolve("packets");
        if (!Files.exists(packetsDir)) {
            CommandManager.chat("§cNo packet logs found");
            return;
        }

        try {
            List<Path> logs = Files.list(packetsDir)
                .filter(p -> p.getFileName().toString().startsWith("parse_") && p.getFileName().toString().endsWith(".log"))
                .sorted((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()))
                .collect(Collectors.toList());

            if (logs.isEmpty()) {
                CommandManager.chat("§cNo packet logs found");
                return;
            }

            CommandManager.chat("§6--- Packet Logs ---");
            for (int i = 0; i < Math.min(logs.size(), 10); i++) {
                Path log = logs.get(i);
                long size = Files.size(log);
                String sizeStr = formatSize(size);
                CommandManager.chat("§e" + (i + 1) + ". §f" + log.getFileName() + " §7(" + sizeStr + ")");
            }
            if (logs.size() > 10) {
                CommandManager.chat("§7... and " + (logs.size() - 10) + " more");
            }
        } catch (Exception e) {
            CommandManager.chat("§cError: " + e.getMessage());
        }
    }

    private void handleSave() {
        PacketDebug module = (PacketDebug) ModuleManager.INSTANCE.get("PacketDebug");
        if (module == null || !module.isEnabled()) {
            CommandManager.chat("§cPacketDebug module not enabled");
            return;
        }

        Path packetsDir = FilesManager.ROOT.resolve("packets");
        if (!Files.exists(packetsDir)) {
            CommandManager.chat("§cNo packet logs found");
            return;
        }

        try {
            Path latest = Files.list(packetsDir)
                .filter(p -> p.getFileName().toString().startsWith("parse_") && p.getFileName().toString().endsWith(".log"))
                .max((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()))
                .orElse(null);

            if (latest == null) {
                CommandManager.chat("§cNo packet logs found");
                return;
            }

            String content = FilesManager.readText(latest);
            CommandManager.chat("§6--- Latest Packet Log ---");
            CommandManager.chat("§eFile: §f" + latest.getFileName());
            CommandManager.chat("§eSize: §f" + formatSize(Files.size(latest)));
            CommandManager.chat("§eLines: §f" + content.split("\n").length);
            CommandManager.chat("§eLocation: §f" + latest.toAbsolutePath());
        } catch (Exception e) {
            CommandManager.chat("§cError: " + e.getMessage());
        }
    }

    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
