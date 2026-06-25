package so.aporia.module.impl.misc

import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventBus
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.user.logger.Logger

@Obfuscate
class TestModule : Module("TestModule", Category.MISC) {

    val username = so.aporia.module.settings.TextSetting("Username", "Введи ник", "Player123")
    val enabled2 = BooleanSetting("Включить фичу", "Включает тестовую фичу", false)
    val mode = so.aporia.module.settings.SelectSetting("Режим", "Выбор режима")
        .value("Авто", "Ручной", "Агрессивный", "Тихий").selected("Авто")
    val targets = so.aporia.module.settings.MultiSelectSetting("Цели", "Выбери несколько целей")
        .options("Игроки", "Мобы", "Животные", "Боссы")
    val hotkey = so.aporia.module.settings.BindSetting("Хоткей", "Клавиша активации")
    val action = so.aporia.module.settings.ButtonSetting("Выполнить", "Запускает действие",
        Runnable { Logger.info("[TestModule] Action triggered! mode=${mode.get()} user=${username.get()}") })

    override fun onEnable() { EventBus.register(this) }
    override fun onDisable() { EventBus.unregister(this) }
}
