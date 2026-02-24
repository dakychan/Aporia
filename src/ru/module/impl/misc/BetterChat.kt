package ru.module.impl.misc

import ru.module.Module

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
class BetterChat : Module("BetterChat", "Улучшенная система чата", C.MISC) {
    
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
        aporia.cc.chat.ChatUtils.notifyOnCopy = notifyOnCopy.value
        aporia.cc.chat.ChatUtils.useGradientPrefix = useGradient.value
        aporia.cc.chat.ChatUtils.useRandomGradient = randomGradient.value
        aporia.cc.chat.ChatUtils.antiSpamEnabled = antiSpam.value
    }
    
    override fun onDisable() {
        aporia.cc.chat.ChatUtils.notifyOnCopy = false
        aporia.cc.chat.ChatUtils.useGradientPrefix = false
        aporia.cc.chat.ChatUtils.useRandomGradient = false
        aporia.cc.chat.ChatUtils.antiSpamEnabled = false
    }
    
    override fun onTick() {
        /**
         * Update settings in real-time
         */
        if (isEnabled) {
            aporia.cc.chat.ChatUtils.notifyOnCopy = notifyOnCopy.value
            aporia.cc.chat.ChatUtils.useGradientPrefix = useGradient.value
            aporia.cc.chat.ChatUtils.useRandomGradient = randomGradient.value
            aporia.cc.chat.ChatUtils.antiSpamEnabled = antiSpam.value
        }
    }
}
