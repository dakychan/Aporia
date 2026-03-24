package aporia.cc;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/**
 * Handles the panic mode — hides all client features and clears chat history
 * until the player types their own username to restore.
 */
public class PanicSystem {

    /** Singleton instance. */
    public static final PanicSystem INSTANCE = new PanicSystem();

    private boolean panicked = false;

    /** Username required to restore from panic mode. Set on {@link #panic()}. */
    private String restoreKey = null;

    /**
     * @return {@code true} if panic mode is currently active
     */
    public boolean isPanicked() {
        return panicked;
    }

    /**
     * Activates panic mode:
     * <ul>
     *   <li>Clears all client-side chat messages</li>
     *   <li>Shows a restore hint with the player's username</li>
     *   <li>Suppresses all further client-side output</li>
     * </ul>
     */
    public void panic() {
        if (panicked) return;
        panicked = true;

        Minecraft mc = Minecraft.getInstance();
        restoreKey = mc.getUser().getName();

        mc.gui.getChat().clearMessages(false);

        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.literal("§7Bye bye! To restore the client, type \"§f" + restoreKey + "§7\""),
                false
            );
        }
    }

    /**
     * Deactivates panic mode and notifies the player.
     */
    public void restore() {
        panicked = false;
        restoreKey = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§aAporia restored."), false);
        }
    }

    /**
     * Must be called from {@code ChatScreen.handleChatInput} before any other processing.
     * <p>
     * If panic mode is active and the message matches the restore key,
     * the client is restored silently — the message is never added to history
     * and never sent to the server.
     *
     * @param message raw normalized chat input
     * @return {@code true} if the message was consumed and must NOT be processed further
     */
    public boolean handleChat(String message) {
        if (!panicked) return false;

        if (restoreKey != null && message.equals(restoreKey)) {
            restore();
            return true;
        }

        return false;
    }
}
