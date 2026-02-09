package zenith.zov.base.autobuy.enchantes.container;

import zenith.zov.base.autobuy.enchantes.Enchant;
import zenith.zov.base.autobuy.enchantes.custom.EnchantCustom;
import zenith.zov.base.autobuy.enchantes.minecraft.EnchantVanilla;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnchantContainer {
    public static final Map<String, String> ENCHANT_MAP = new HashMap<>();

    static {
        ENCHANT_MAP.put("oxidation", "Окисление");
        ENCHANT_MAP.put("detection", "Детекция");
        ENCHANT_MAP.put("poison", "Яд");
        ENCHANT_MAP.put("vampirism", "Вампиризм");
        ENCHANT_MAP.put("skilled", "Опытный");
        ENCHANT_MAP.put("smelting", "Автоплавка");
        ENCHANT_MAP.put("magnet", "Магнит");
        ENCHANT_MAP.put("pinger", "Пингер");
        ENCHANT_MAP.put("web", "Паутина");
        ENCHANT_MAP.put("buldozing", "Бульдозер");
        ENCHANT_MAP.put("pulling", "Притягивание");
        ENCHANT_MAP.put("stupor", "Ступор");
        ENCHANT_MAP.put("demolishing", "Разрушение");
        ENCHANT_MAP.put("returning", "Возврат");
        ENCHANT_MAP.put("scout", "Скаут");

        ENCHANT_MAP.put("minecraft:protection", "Защита");
        ENCHANT_MAP.put("minecraft:fire_protection", "Огнеупорность");
        ENCHANT_MAP.put("minecraft:feather_falling", "Невесомость (Падение)");
        ENCHANT_MAP.put("minecraft:blast_protection", "Взрывоустойчивость");
        ENCHANT_MAP.put("minecraft:projectile_protection", "Защита от снарядов");
        ENCHANT_MAP.put("minecraft:thorns", "Шипы");
        ENCHANT_MAP.put("minecraft:soul_speed", "Скорость души");

        ENCHANT_MAP.put("minecraft:respiration", "Подводное дыхание");
        ENCHANT_MAP.put("minecraft:depth_strider", "Подводник (Подводная ходьба)");
        ENCHANT_MAP.put("minecraft:aqua_affinity", "Подводник (Ускорение добычи под водой)");
        ENCHANT_MAP.put("minecraft:frost_walker", "Ледоход");

        ENCHANT_MAP.put("minecraft:sharpness", "Острота");
        ENCHANT_MAP.put("minecraft:smite", "Небесная кара");
        ENCHANT_MAP.put("minecraft:bane_of_arthropods", "Бич членистоногих");
        ENCHANT_MAP.put("minecraft:knockback", "Отбрасывание");
        ENCHANT_MAP.put("minecraft:fire_aspect", "Заговор огня");
        ENCHANT_MAP.put("minecraft:looting", "Добыча");
        ENCHANT_MAP.put("minecraft:sweeping_edge", "Разящий клинок");

        ENCHANT_MAP.put("minecraft:efficiency", "Эффективность");
        ENCHANT_MAP.put("minecraft:silk_touch", "Шёлковое касание");
        ENCHANT_MAP.put("minecraft:unbreaking", "Прочность");
        ENCHANT_MAP.put("minecraft:fortune", "Удача");
        ENCHANT_MAP.put("minecraft:mending", "Починка");
        ENCHANT_MAP.put("minecraft:impaling", "Пронзатель");

        ENCHANT_MAP.put("minecraft:power", "Сила");
        ENCHANT_MAP.put("minecraft:punch", "Откидывание (Удар стрелой)");
        ENCHANT_MAP.put("minecraft:flame", "Воспламенение");
        ENCHANT_MAP.put("minecraft:infinity", "Бесконечность");
        ENCHANT_MAP.put("minecraft:piercing", "Точность");
        ENCHANT_MAP.put("minecraft:multishot", "Многострельность");
        ENCHANT_MAP.put("minecraft:quick_charge", "Быстрая перезарядка");

        ENCHANT_MAP.put("minecraft:riptide", "Замедление");
        ENCHANT_MAP.put("minecraft:loyalty", "Верность");
        ENCHANT_MAP.put("minecraft:channeling", "Громовержец");

        ENCHANT_MAP.put("minecraft:luck_of_the_sea", "Везучий рыбак");
        ENCHANT_MAP.put("minecraft:lure", "Приманка");

        ENCHANT_MAP.put("minecraft:binding_curse", "Проклятие несъёмности");
        ENCHANT_MAP.put("minecraft:vanishing_curse", "Проклятие утраты");
    }


    public static List<Enchant> parse(String input) {
        List<Enchant> enchants = new ArrayList<>();
        String trimmed = input.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }

        Pattern pattern = Pattern.compile(
                "(EnchantCustom|EnchantVanilla) \\[checked=([^,]+), level=(\\d+)]"
        );

        Matcher matcher = pattern.matcher(trimmed);
        while (matcher.find()) {
            String className = matcher.group(1);
            String checked = matcher.group(2);
            int level = Integer.parseInt(matcher.group(3));

            String name = ENCHANT_MAP.getOrDefault(checked,checked);

            Enchant enchant;
            if (className.equals("EnchantCustom")) {
                enchant = new EnchantCustom(name, checked, level);
            } else {
                enchant = new EnchantVanilla(name, checked, level);
            }

            enchants.add(enchant);
        }

        return enchants;
    }
}
