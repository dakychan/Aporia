package aporia.su.utils.chat.impl;

import aporia.su.utils.chat.Command;
import aporia.su.utils.chat.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class PrefixCommand extends Command {
    
    public PrefixCommand() {
        super("prefix", "Изменить префикс команд");
    }
    
    @Override
    public void execute(List<String> args) {
        Minecraft mc = Minecraft.getInstance();
        
        if (args.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§eТекущий префикс: §a" + CommandManager.INSTANCE.getPrefix()), false);
                mc.player.displayClientMessage(Component.literal("§7Использование: " + CommandManager.INSTANCE.getPrefix() + "prefix <новый префикс>"), false);
            }
            return;
        }
        
        String newPrefix = args.get(0);
        if (newPrefix.length() > 3) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§cПрефикс слишком длинный! Максимум 3 символа."), false);
            }
            return;
        }
        
        CommandManager.INSTANCE.setPrefix(newPrefix);
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§aПрефикс изменен на: §f" + newPrefix), false);
        }
    }
    
    @Override
    public String[] getCompletions(List<String> args) {
        if (args.isEmpty()) {
            return new String[]{"^", ".", ",", "!", "/", "-"};
        }
        return new String[0];
    }
}
