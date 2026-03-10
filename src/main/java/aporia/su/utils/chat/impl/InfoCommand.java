package aporia.su.utils.chat.impl;

import aporia.su.utils.chat.Command;
import aporia.su.utils.chat.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Команда для отображения информации о системе команд.
 * Показывает текущий префикс и список всех доступных команд с их описаниями.
 * 
 * Алиасы: help, ?
 * 
 * @author Aporia
 */
public class InfoCommand extends Command {
    
    public InfoCommand() {
        super("info", "Информация о системе команд", List.of("help", "?"));
    }
    
    @Override
    public void execute(List<String> args) {
        Minecraft mc = Minecraft.getInstance();
        
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§8§m                                    "), false);
            mc.player.displayClientMessage(Component.literal("§6§lAporia §fCommand System"), false);
            mc.player.displayClientMessage(Component.literal("§7Префикс: §a" + CommandManager.INSTANCE.getPrefix()), false);
            mc.player.displayClientMessage(Component.literal(""), false);
            mc.player.displayClientMessage(Component.literal("§eДоступные команды:"), false);
            
            for (Command cmd : CommandManager.INSTANCE.getAllCommands()) {
                String aliasText = "";
                if (!cmd.getAliases().isEmpty()) {
                    aliasText = " §7(" + String.join(", ", cmd.getAliases()) + ")";
                }
                mc.player.displayClientMessage(Component.literal(
                    "§a" + CommandManager.INSTANCE.getPrefix() + cmd.getName() + aliasText + " §f- §7" + cmd.getDescription()
                ), false);
            }
            
            mc.player.displayClientMessage(Component.literal("§8§m                                    "), false);
        }
    }
}
