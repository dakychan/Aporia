package anidumpproject.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Аннотация для маркировки главного класса приложения.
 * <p>
 * Используется для исключения метода из нативной обфускации,
 * чтобы сохранить точку входа доступной для JVM.
 * </p>
 * 
 * <h3>Применение:</h3>
 * <pre>{@code
 * @MainClass
 * public class MyApplication {
 *     public static void main(String[] args) {
 *         // Точка входа
 *     }
 * }
 * }</pre>
 * 
 * <h3>Особенности:</h3>
 * <ul>
 *   <li>Применяется только к методам и типам</li>
 *   <li>Сохраняется только на уровне CLASS (не в runtime)</li>
 *   <li>Предотвращает обфускацию критичных точек входа</li>
 *   <li>Используется обфускатором для пропуска метода</li>
 * </ul>
 * 
 * @author Aporia Team
 * @version 1.0
 * @since 1.0
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MainClass {
    
    /**
     * Описание главного класса или метода.
     * <p>
     * Используется для документирования назначения точки входа.
     * </p>
     * 
     * @return описание класса/метода
     */
    String value() default "";
    
    /**
     * Приоритет загрузки класса.
     * <p>
     * Определяет порядок инициализации при наличии нескольких главных классов.
     * Меньшее значение = более высокий приоритет.
     * </p>
     * 
     * @return приоритет (по умолчанию 0)
     */
    int priority() default 0;
}
