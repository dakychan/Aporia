package so.aporia.module.impl.player

import com.chaos.annotation.Obfuscate
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.utils.imports.*

@Obfuscate
class NoPush : Module("NoPush", Category.PLAYER) {

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
    }

    companion object {
        private var instance: NoPush? = null

        @JvmStatic
        fun isNoPushActive(): Boolean {
            return instance != null && instance!!.isEnabled
        }
    }

    init {
        instance = this
    }
}
