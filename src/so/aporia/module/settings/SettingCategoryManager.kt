package so.aporia.module.settings

object SettingCategoryManager {
    private val current = ThreadLocal<Pair<String, Int>?>()

    fun push(name: String, hierarchy: Int) { current.set(name to hierarchy) }
    fun pop() { current.set(null) }
    fun current(): Pair<String, Int>? = current.get()
}
