package anidumpproject.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для runtime обфускации.
 * <p>
 * Маркирует классы для обфускации с ежедневной ротацией маппингов.
 * Маппинги меняются каждый день в 00:00 UTC.
 * </p>
 * 
 * <h3>Уровни обфускации:</h3>
 * <ul>
 *   <li><b>NONE</b> - Не обфусцировать (по умолчанию)</li>
 *   <li><b>LIGHT</b> - Легкая обфускация (короткие имена)</li>
 *   <li><b>MEDIUM</b> - Средняя обфускация (каша из символов)</li>
 *   <li><b>HEAVY</b> - Тяжелая обфускация (полная каша)</li>
 *   <li><b>EXTREME</b> - Экстремальная (максимальная каша)</li>
 * </ul>
 * 
 * <h3>Примеры:</h3>
 * <pre>{@code
 * // Легкая обфускация
 * @Obfuscate(level = Level.LIGHT)
 * public class MyModule {
 *     // aporia.su.modules.MyModule -> aporia.su.Ab
 * }
 * 
 * // Тяжелая обфускация
 * @Obfuscate(level = Level.HEAVY)
 * public class LicenseCheck {
 *     // aporia.su.LicenseCheck -> a.b.c.D3f$
 * }
 * 
 * // Не обфусцировать
 * @Obfuscate(level = Level.NONE)
 * public class PublicAPI {
 *     // Остается как есть
 * }
 * }</pre>
 * 
 * <h3>Особенности:</h3>
 * <ul>
 *   <li>Маппинги генерируются на основе даты UTC</li>
 *   <li>Каждый день новые имена классов</li>
 *   <li>В памяти только обфусцированные имена</li>
 *   <li>Дамп памяти бесполезен без маппингов</li>
 *   <li>Миксины не обфусцируются автоматически</li>
 * </ul>
 * 
 * <h3>Стратегия "Горящая бумага":</h3>
 * <p>
 * Маппинги существуют только во время билда и сохраняются в файл
 * с датой. В runtime маппингов нет - только каша в памяти.
 * </p>
 * 
 * @author protect3ed
 * @version 1.0
 * @since 2.0
 * @see Native
 * @see MainClass
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Obfuscate {
    
    /**
     * Уровень обфускации.
     * 
     * @return уровень (по умолчанию NONE)
     */
    Level level() default Level.NONE;
    
    /**
     * Комментарий для разработчика.
     * 
     * @return комментарий
     */
    String comment() default "";
    
    /**
     * Уровни обфускации.
     */
    enum Level {
        /**
         * Не обфусцировать.
         * <p>
         * Класс остается с оригинальным именем.
         * </p>
         */
        NONE,
        
        /**
         * Легкая обфускация - короткие имена (2 символа).
         * <p>
         * Пример: MyClass -> Ab
         * </p>
         * <ul>
         *   <li>Длина: 2 символа</li>
         *   <li>Читаемость: Низкая</li>
         *   <li>Защита: Базовая</li>
         * </ul>
         */
        LIGHT,
        
        /**
         * Средняя обфускация - каша из символов (3 символа).
         * <p>
         * Пример: MyClass -> a$B
         * </p>
         * <ul>
         *   <li>Длина: 3 символа</li>
         *   <li>Читаемость: Очень низкая</li>
         *   <li>Защита: Средняя</li>
         * </ul>
         */
        MEDIUM,
        
        /**
         * Тяжелая обфускация - полная каша (4 символа).
         * <p>
         * Пример: MyClass -> D3f$
         * </p>
         * <ul>
         *   <li>Длина: 4 символа</li>
         *   <li>Читаемость: Нулевая</li>
         *   <li>Защита: Высокая</li>
         * </ul>
         */
        HEAVY,
        
        /**
         * Экстремальная обфускация - максимальная каша (5+ символов).
         * <p>
         * Пример: MyClass -> _$aB9
         * </p>
         * <ul>
         *   <li>Длина: 5+ символов</li>
         *   <li>Читаемость: Невозможна</li>
         *   <li>Защита: Максимальная</li>
         * </ul>
         */
        EXTREME
    }
}
