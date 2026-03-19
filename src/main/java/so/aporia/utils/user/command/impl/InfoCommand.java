package so.aporia.utils.user.command.impl;

import net.minecraft.client.Minecraft;
import so.aporia.utils.user.command.Command;
import so.aporia.utils.user.command.CommandManager;

public class InfoCommand implements Command {

    @Override
    public String name() { return "info"; }

    @Override
    public String description() { return "Shows mod and player info"; }

    @Override
    public void execute(String[] args) {
        Minecraft mc = Minecraft.getInstance();
        CommandManager.chat("§6--- Aporia Info ---");
        CommandManager.chat("§eVersion: §f1.0.0");
        CommandManager.chat("§eMinecraft: §f" + mc.getLaunchedVersion());
        if (mc.player != null) {
            CommandManager.chat("§ePlayer: §f" + mc.player.getName().getString());
            CommandManager.chat("§ePosition: §f" +
                String.format("%.1f, %.1f, %.1f",
                    mc.player.getX(), mc.player.getY(), mc.player.getZ()));
        }
    }
}
