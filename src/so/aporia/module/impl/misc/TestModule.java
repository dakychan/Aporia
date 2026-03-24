package so.aporia.module.impl.misc;

import so.aporia.module.Category;
import so.aporia.module.Module;
import so.aporia.module.settings.BindSetting;
import so.aporia.module.settings.BooleanSetting;
import so.aporia.module.settings.ButtonSetting;
import so.aporia.module.settings.MultiSelectSetting;
import so.aporia.module.settings.SelectSetting;
import so.aporia.module.settings.TextSetting;
import so.aporia.utils.user.logger.Logger;

/** Test module showcasing all setting types. */
public final class TestModule extends Module {
    public final TextSetting username = new TextSetting(
        "Username", "Введи ник", "Player123"
    );

    public final BooleanSetting enabled2 = new BooleanSetting(
        "Включить фичу", "Включает тестовую фичу", false
    );

    public final SelectSetting mode = new SelectSetting("Режим", "Выбор режима")
        .value("Авто", "Ручной", "Агрессивный", "Тихий")
        .selected("Авто");

    public final MultiSelectSetting targets = new MultiSelectSetting(
        "Цели", "Выбери несколько целей"
    ).options("Игроки", "Мобы", "Животные", "Боссы");

    public final BindSetting hotkey = new BindSetting(
        "Хоткей", "Клавиша активации"
    );

    public final ButtonSetting action = new ButtonSetting(
        "Выполнить", "Запускает действие",
        () -> Logger.info("[TestModule] Action triggered! mode=" + mode.get()
            + " user=" + username.get())
    );

    public TestModule() {
        super("TestModule", Category.MISC);
    }
}
