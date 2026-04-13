/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

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
