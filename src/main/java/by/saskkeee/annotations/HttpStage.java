package by.saskkeee.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ЭТО ЧТО БЫ НЕ ВЗЛОМАЛИ
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface HttpStage {
    int stage();
}
