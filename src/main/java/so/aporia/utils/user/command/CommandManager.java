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
 */
public class CommandManager {

    /** Singleton instance. */
    public static final CommandManager INSTANCE = new CommandManager();

    /** Current command prefix. Can be changed at runtime via {@code .prefix}. */
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
     *
     * @param cmd command to register
     */
    public void register(Command cmd) {
        commands.put(cmd.name().toLowerCase(), cmd);
    }

    /**
     * @return all registered commands
     */
    public Collection<Command> getAll() {
        return commands.values();
    }

    /**
     * Attempts to handle a chat message as a client command.
     * <p>
     * While {@link PanicSystem} is active, all prefix-matching messages are
     * passed through to the server as normal chat instead of being consumed.
     *
     * @param message raw chat input
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
     *
     * @param text message text, supports §-color codes
     */
    public static void chat(String text) {
        if (PanicSystem.INSTANCE.isPanicked()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(text), false);
        }
    }
}
