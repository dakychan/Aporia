package so.aporia.utils.events.impl;

import net.minecraft.network.chat.Component;

/**
 * Fired before a chat message is displayed.
 * Can be cancelled to hide the message from chat.
 */
public final class ChatHideEvent {

    private final Component message;
    private final String plainText;
    private boolean cancelled;

    public ChatHideEvent(Component message) {
        this.message = message;
        this.plainText = message.getString();
    }

    public Component message()   { return message; }
    public String   plainText()  { return plainText; }

    public void    cancel()        { cancelled = true; }
    public boolean isCancelled()   { return cancelled; }
}
