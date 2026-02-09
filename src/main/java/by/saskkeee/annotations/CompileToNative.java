package by.saskkeee.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Функцию @CompileToNative можно писать над классами или воидами не надо везде ее пихать ее надо пихать
 * над важными метадами этот метод является важным самое главное надо что бы над главным
 * классом чита был написан @CompileToNative это повышает уровень защиты этого класса например мейн меню
 * не очень важный класс, а вот киллаура уже важна
 */

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface CompileToNative {
    boolean duplicate() default false;
}
