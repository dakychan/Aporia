package so.aporia.utils.user.command;

import aporia.cc.PanicSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import so.aporia.utils.user.command.impl.HelpCommand;
import so.aporia.utils.user.command.impl.InfoCommand;
import so.aporia.utils.user.command.impl.PanicCommand;
import so.aporia.utils.user.command.impl.PrefixCommand;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Central registry and dispatcher for all Aporia client-side commands.
 * <p>
 * Центральный реестр и диспетчер всех клиентских команд Aporia.
 */
public class CommandManager {

    /**
     * Singleton instance.
     * <p>
     * Экземпляр синглтона.
     */
    public static final CommandManager INSTANCE = new CommandManager();

    /**
     * Current command prefix. Can be changed at runtime via {@code .prefix}.
     * <p>
     * Текущий префикс команд. Можно изменить во время выполнения через {@code .prefix}.
     */
    public static String PREFIX = ".";

    private final Map<String, Command> commands = new HashMap<>();

    public CommandManager() {
        register(new HelpCommand());
        register(new InfoCommand());
        register(new PanicCommand());
        register(new PrefixCommand());
    }

    /**
     * Registers a command by its {@link Command#name()}.
     * <p>
     * Регистрирует команду по её {@link Command#name()}.
     *
     * @param cmd command to register / команда для регистрации
     */
    public void register(Command cmd) {
        commands.put(cmd.name().toLowerCase(), cmd);
    }

    /**
     * Returns all registered commands.
     * <p>
     * Возвращает все зарегистрированные команды.
     */
    public Collection<Command> getAll() {
        return commands.values();
    }

    /**
     * Attempts to handle a chat message as a client command.
     * <p>
     * While {@link PanicSystem} is active, all prefix-matching messages are
     * passed through to the server as normal chat instead of being consumed.
     * <p>
     * Пытается обработать сообщение чата как клиентскую команду.
     * <p>
     * Пока {@link PanicSystem} активен, все сообщения с префиксом отправляются
     * на сервер как обычный чат вместо того чтобы быть потреблёнными.
     *
     * @param message raw chat input / входящее сообщение чата
     * @return {@code true} if the message was consumed and must NOT be sent to the server
     */
    public boolean handle(String message) {
        if (!message.startsWith(PREFIX)) return false;

        if (PanicSystem.INSTANCE.isPanicked()) return false;

        String[] parts = message.substring(PREFIX.length()).trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return true;

        Command cmd = commands.get(parts[0].toLowerCase());
        if (cmd == null) {
            chat("§cUnknown command: " + parts[0] + ". Use " + PREFIX + "help");
        } else {
            cmd.execute(parts);
        }
        return true;
    }

    /**
     * Displays a client-side only message in chat.
     * Silently suppressed while {@link PanicSystem} is active.
     * <p>
     * Показывает сообщение только на клиенте в чате.
     * Молча подавляется пока {@link PanicSystem} активен.
     *
     * @param text message text, supports §-color codes / текст сообщения, поддерживает §-коды цветов
     */
    public static void chat(String text) {
        if (PanicSystem.INSTANCE.isPanicked()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }
}
