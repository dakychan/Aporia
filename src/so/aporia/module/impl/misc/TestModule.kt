package so.aporia.module.impl.misc
import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
class TestModule : Module("TestModule", Category.MISC) {

    val username = so.aporia.module.settings.TextSetting("Username", "Введи ник", "Player123")
    val enabled2 = BooleanSetting("Включить фичу", "Включает тестовую фичу", false)
    val mode = so.aporia.module.settings.SelectSetting("Режим", "Выбор режима")
        .value("Авто", "Ручной", "Агрессивный", "Тихий").selected("Авто")
    val targets = so.aporia.module.settings.MultiSelectSetting("Цели", "Выбери несколько целей")
        .options("Игроки", "Мобы", "Животные", "Боссы")
    val hotkey = so.aporia.module.settings.BindSetting("Хоткей", "Клавиша активации")
    val action = so.aporia.module.settings.ButtonSetting("Выполнить", "Запускает действие",
        Runnable { logger.info("[TestModule] Action triggered! mode=${mode.get()} user=${username.get()}") })

    override fun onEnable() { bus.register(this) }
    override fun onDisable() { bus.unregister(this) }

    override val settings = listOf(username, enabled2, mode, targets, hotkey, action)
}