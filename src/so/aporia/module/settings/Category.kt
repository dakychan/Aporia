package so.aporia.module.settings
import com.chaos.annotation.ChaosNative
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SettingCategory(val name: String, val hierarchy: Int = 0)