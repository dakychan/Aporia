/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

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
