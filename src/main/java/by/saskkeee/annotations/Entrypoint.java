package by.saskkeee.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Функцию Entrypoint можно писать над классами или воидами
 * да все короче иди нахуй
 * СОСИ СОСИ ЛООХ
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Entrypoint {
}
