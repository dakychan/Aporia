package aporia.language
import com.chaos.annotation.ChaosNative
/**
 * База для языковой системы Aporia.
 * Пока заглушка — будет расширяться позже.
 */
abstract class LanguageBase {
    abstract val code: String
    abstract val displayName: String

    abstract fun get(key: String): String?
    fun get(key: String, vararg args: Any?): String {
        val template = get(key) ?: key
        return template.format(*args)
    }
}