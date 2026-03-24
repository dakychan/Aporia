package so.aporia.utils.events.impl;

import net.minecraft.network.chat.Component;

/**
 * Fired when a chat message is received from the server.
 */
public final class ChatMessageEvent {

    private final Component message;
    private final String plainText;

    public ChatMessageEvent(Component message) {
        this.message = message;
        this.plainText = message.getString();
    }

    public Component message()   { return message; }
    public String   plainText()  { return plainText; }
}
