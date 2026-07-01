package so.aporia.module.settings

import java.util.function.Supplier

/**
 * Preview3DSetting — setting that provides a 3D preview popup of a module's visual effect.
 *
 * // Video not yet created — placeholder for future 3D preview integration
 */
class Preview3DSetting @JvmOverloads constructor(
    name: String,
    description: String,
    previewUrl: String = "",
    visible: Supplier<Boolean>? = null
) : Setting<Unit>(name, description, Unit, visible) {

    var isPreviewOpen = false
        private set

    fun togglePreview() {
        isPreviewOpen = !isPreviewOpen
    }

    fun openPreview() {
        isPreviewOpen = true
    }

    fun closePreview() {
        isPreviewOpen = false
    }
}
