package aporia.su.utils.chat.impl;

import aporia.su.utils.chat.Command;
import aporia.su.utils.chat.CommandManager;
import aporia.su.utils.chat.GradientText;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Команда для управления алиасами (сокращениями) других команд.
 * Позволяет создавать короткие имена для длинных команд.
 * 
 * Использование:
 * - alias add <имя> - создать новый алиас (интерактивно)
 * - alias list - показать все алиасы
 * - alias remove <имя> - удалить алиас
 * - alias <имя> - выполнить алиас
 * 
 * @author Aporia
 */
public class AliasCommand extends Command {
    private final Map<String, String> aliases = new HashMap<>();
    private String awaitingCommand = null;
    private String awaitingAlias = null;
    
    public AliasCommand() {
        super("alias", "Управление алиасами команд");
    }
    
    @Override
    public void execute(List<String> args) {
        Minecraft mc = Minecraft.getInstance();
        
        // Обработка второго шага создания алиаса (ввод команды)
        if (awaitingCommand != null) {
            String command = String.join(" ", args);
            aliases.put(awaitingCommand, command);
            if (mc.player != null) {
                mc.player.displayClientMessage(GradientText.createPrefix().append(Component.literal(" §7Алиас §a'" + awaitingCommand + "' §7-> §a'" + command + "' §7создан!")), false);
            }
            awaitingCommand = null;
            return;
        }
        
        // Обработка первого шага создания алиаса (ввод имени)
        if (awaitingAlias != null) {
            awaitingCommand = awaitingAlias;
            awaitingAlias = null;
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§eВведите команду для алиаса:"), false);
            }
            return;
        }
        
        if (args.isEmpty()) {
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§cИспользование: " + CommandManager.INSTANCE.getPrefix() + "alias add <имя>"), false);
            }
            return;
        }
        
        String subCommand = args.get(0).toLowerCase();
        
        if (subCommand.equals("add")) {
            if (args.size() < 2) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§cУкажите имя алиаса!"), false);
                }
                return;
            }
            awaitingAlias = args.get(1);
            if (mc.player != null) {
                mc.player.displayClientMessage(Component.literal("§eВведите команду для алиаса '" + args.get(1) + "':"), false);
            }
        } else if (subCommand.equals("list")) {
            if (aliases.isEmpty()) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§eАлиасы не созданы"), false);
                }
            } else {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§aАлиасы:"), false);
                    for (Map.Entry<String, String> entry : aliases.entrySet()) {
                        mc.player.displayClientMessage(Component.literal("§7" + entry.getKey() + " §f-> §7" + entry.getValue()), false);
                    }
                }
            }
        } else if (subCommand.equals("remove")) {
            if (args.size() < 2) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§cУкажите имя алиаса!"), false);
                }
                return;
            }
            if (aliases.remove(args.get(1)) != null) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§aАлиас '" + args.get(1) + "' удален!"), false);
                }
            } else {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§cАлиас не найден!"), false);
                }
            }
        } else {
            // Попытка выполнить алиас
            String aliasName = args.get(0);
            String command = aliases.get(aliasName);
            if (command != null) {
                if (mc.player != null) {
                    mc.player.connection.sendCommand(command);
                }
            } else {
                if (mc.player != null) {
                    mc.player.displayClientMessage(Component.literal("§cАлиас '" + aliasName + "' не найден!"), false);
                }
            }
        }
    }
    
    @Override
    public String[] getCompletions(List<String> args) {
        if (args.size() == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("add");
            completions.add("list");
            completions.add("remove");
            completions.addAll(aliases.keySet());
            return completions.toArray(new String[0]);
        } else if (args.size() == 2 && args.get(0).equalsIgnoreCase("remove")) {
            return aliases.keySet().toArray(new String[0]);
        }
        return new String[0];
    }
}
