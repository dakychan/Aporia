package so.aporia.utils.events.impl

import net.minecraft.network.chat.Component

class ChatMessageEvent(val message: Component) {
    val plainText: String = message.string

    fun message(): Component = message
    fun plainText(): String = plainText
}
