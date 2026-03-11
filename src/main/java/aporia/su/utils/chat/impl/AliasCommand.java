package aporia.su.utils.chat.impl;

import aporia.su.utils.chat.Command;
import aporia.su.utils.chat.CommandManager;
import aporia.su.utils.chat.GradientText;
import dev.aporia.FilesManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Map;

/**
 * Команда для управления алиасами.
 * Реализует интерактивное создание алиасов через машину состояний.
 *
 * Использование:
 * - ^alias add - интерактивный режим (2 шага)
 * - ^alias list - показать все алиасы
 * - ^alias remove <имя> - удалить алиас
 */
public class AliasCommand extends Command {

    private static final int STEP_COMMAND = 1;
    private static final int STEP_ALIAS_NAME = 2;
    private static final int TOTAL_STEPS = 2;

    private final FilesManager.AliasManager aliasManager;

    public AliasCommand() {
        super("alias", "Создание сокращений для команд");
        this.aliasManager = FilesManager.aliases();
    }

    @Override
    public void execute(List<String> args) {
        Minecraft mc = Minecraft.getInstance();

        if (args.isEmpty()) {
            sendHelp(mc);
            return;
        }

        String subCommand = args.get(0).toLowerCase();
        switch (subCommand) {
            case "add" -> startInteractiveSession(mc);
            case "list" -> listAliases(mc);
            case "remove" -> removeAlias(mc, args);
            default -> executeAlias(mc, subCommand);
        }
    }

    @Override
    public int getExpectedSteps() {
        return TOTAL_STEPS;
    }

    @Override
    public String getStepPrompt(int currentStep) {
        return switch (currentStep) {
            case STEP_COMMAND -> "§bВведи команду, которую хочешь сократить (например: /gamemode creative или ^tp):";
            case STEP_ALIAS_NAME -> "§bТеперь введи имя алиаса (например: gmc):";
            default -> "";
        };
    }

    @Override
    public void onStepComplete(int currentStep, List<String> buffer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        String input = buffer.isEmpty() ? "" : buffer.getLast();

        switch (currentStep) {
            case STEP_COMMAND -> {
                mc.player.displayClientMessage(Component.literal("§eКоманда сохранена: §f" + input), false);
                mc.player.displayClientMessage(Component.literal(getStepPrompt(STEP_ALIAS_NAME)), false);
            }
        }
    }

    @Override
    public void executeWithBuffer(List<String> buffer) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || buffer.size() < TOTAL_STEPS) return;

        String command = buffer.get(0).trim();
        String aliasName = buffer.get(1).trim().split(" ")[0];

        if (aliasName.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("§cИмя алиаса не может быть пустым!"), false);
            return;
        }

        if (command.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("§cКоманда не может быть пустой!"), false);
            return;
        }

        aliasManager.addAlias(aliasName, command);

        String commandType = command.startsWith("/") ? "Minecraft" : "Aporia";
        mc.player.displayClientMessage(
            GradientText.createPrefix().append(
                Component.literal(" §aАлиас §f'" + aliasName + "' §7-> §f'" + command + "' §aсохранен! (§f" + commandType + "§a)")
            ), false
        );
    }

    private void startInteractiveSession(Minecraft mc) {
        if (mc.player == null) return;

        CommandManager.INSTANCE.startWaiting(this);
        mc.player.displayClientMessage(Component.literal("§e=== Создание алиаса ==="), false);
        mc.player.displayClientMessage(Component.literal(getStepPrompt(STEP_COMMAND)), false);
    }

    private void listAliases(Minecraft mc) {
        if (mc.player == null) return;

        Map<String, String> allAliases = aliasManager.getAllAliases();
        if (allAliases.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("§eСписок алиасов пуст."), false);
            return;
        }

        mc.player.displayClientMessage(
            GradientText.createPrefix().append(Component.literal(" §fСписок алиасов:")), false
        );

        allAliases.forEach((name, cmd) -> {
            String type = cmd.startsWith("/") ? "§9MC" : "§aAporia";
            mc.player.displayClientMessage(
                Component.literal(" §8• §a" + name + " §7-> §f" + cmd + " §8[" + type + "§8]"), false
            );
        });
    }

    private void removeAlias(Minecraft mc, List<String> args) {
        if (mc.player == null) return;

        if (args.size() < 2) {
            mc.player.displayClientMessage(Component.literal("§cУкажи имя алиаса: §7alias remove <имя>"), false);
            return;
        }

        String name = args.get(1);
        if (aliasManager.hasAlias(name)) {
            aliasManager.removeAlias(name);
            mc.player.displayClientMessage(Component.literal("§aАлиас '" + name + "' удален."), false);
        } else {
            mc.player.displayClientMessage(Component.literal("§cАлиас '" + name + "' не найден."), false);
        }
    }

    private void executeAlias(Minecraft mc, String aliasName) {
        if (mc.player == null) return;

        String command = aliasManager.getCommand(aliasName);
        if (command == null) {
            mc.player.displayClientMessage(
                Component.literal("§cАлиас '" + aliasName + "' не найден. Используй §7^alias list"), false
            );
            return;
        }

        if (command.startsWith("/")) {
            // Алиас на команду Minecraft
            mc.player.connection.sendCommand(command.substring(1));
        } else if (command.startsWith(CommandManager.INSTANCE.getPrefix())) {
            // Алиас на команду Aporia
            CommandManager.INSTANCE.executeCommand(command);
        } else {
            // Обычное сообщение или команда без префикса
            mc.player.connection.sendChat(command);
        }
    }

    private void sendHelp(Minecraft mc) {
        if (mc.player == null) return;

        String p = CommandManager.INSTANCE.getPrefix();
        mc.player.displayClientMessage(Component.literal("§eИспользование:"), false);
        mc.player.displayClientMessage(Component.literal("§7" + p + "alias add §f- Создать алиас (интерактивно)"), false);
        mc.player.displayClientMessage(Component.literal("§7" + p + "alias list §f- Список алиасов"), false);
        mc.player.displayClientMessage(Component.literal("§7" + p + "alias remove <имя> §f- Удалить алиас"), false);
    }

    @Override
    public String[] getCompletions(List<String> args) {
        if (args.isEmpty()) {
            return new String[]{"add", "list", "remove"};
        }

        if (args.size() == 1) {
            return new String[]{"add", "list", "remove"};
        }

        if (args.size() == 2 && args.get(0).equalsIgnoreCase("remove")) {
            return aliasManager.getAllAliases().keySet().toArray(new String[0]);
        }

        return new String[0];
    }
}
