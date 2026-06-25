package so.aporia.module.settings

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SettingCategory(val name: String, val hierarchy: Int = 0)
