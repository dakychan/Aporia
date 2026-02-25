package cc.apr.module.impl.misc

import aporia.cc.chat.ChatUtils
import cc.apr.module.Module

/**
 * BetterChat module - улучшенная система чата.
 *
 * Функции:
 * - Shift + ЛКМ: копирование сообщения в буфер обмена
 * - ПКМ: вставка сообщения в поле ввода чата
 * - Уведомление при копировании (опционально)
 * - Градиентный префикс (статичный или рандомный)
 * - Анти-спам: стекирование одинаковых сообщений
 */
class  BetterChat : Module("BetterChat", "Улучшенная система чата", C.MISC) {

    private val notifyOnCopy: BooleanSetting
    private val useGradient: BooleanSetting
    private val randomGradient: BooleanSetting
    private val antiSpam: BooleanSetting

    init {
        notifyOnCopy = BooleanSetting("Уведомление", true)
        useGradient = BooleanSetting("Градиент", true)
        randomGradient = BooleanSetting("Рандом градиент", false)
        antiSpam = BooleanSetting("Анти-спам", true)

        addSetting(notifyOnCopy)
        addSetting(useGradient)
        addSetting(randomGradient)
        addSetting(antiSpam)
    }

    override fun onEnable() {
        ChatUtils.notifyOnCopy = notifyOnCopy.value
        ChatUtils.useGradientPrefix = useGradient.value
        ChatUtils.useRandomGradient = randomGradient.value
        ChatUtils.antiSpamEnabled = antiSpam.value
    }

    override fun onDisable() {
        ChatUtils.notifyOnCopy = false
        ChatUtils.useGradientPrefix = false
        ChatUtils.useRandomGradient = false
        ChatUtils.antiSpamEnabled = false
    }

    override fun onTick() {
        /**
         * Update settings in real-time
         */
        if (isEnabled) {
            ChatUtils.notifyOnCopy = notifyOnCopy.value
            ChatUtils.useGradientPrefix = useGradient.value
            ChatUtils.useRandomGradient = randomGradient.value
            ChatUtils.antiSpamEnabled = antiSpam.value
        }
    }
}