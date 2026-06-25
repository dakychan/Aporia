package so.aporia.utils.user.render.core

enum class RenderLayer(val priority: Int) {
    WORLD(0),
    GUI(1),
    HUD(2),
    CLICKGUI(3),
    OVERLAY(4);

    companion object {
        @JvmStatic
        @OptIn(ExperimentalStdlibApi::class)
        fun sorted(): Array<RenderLayer> = entries.toTypedArray()
    }
}
