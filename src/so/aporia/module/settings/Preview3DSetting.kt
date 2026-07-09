package so.aporia.module.settings
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.theme.ThemeManager.Theme
import java.util.function.Supplier
import com.chaos.annotation.ChaosNative
/**
 * Preview3DSetting — setting that provides a 3D preview popup of a module's visual effect.
 *
 * // Video not yet created — placeholder for future 3D preview integration
 */
@ChaosNative
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

    override fun draw(r: AporiaRenderer, x: Float, y: Float, w: Float, theme: Theme, mouseX: Float, mouseY: Float, isDropOpen: Boolean) {
        val label = "Preview"
        val bw = r.getTextWidth("regular", label, 8f) + 12f
        val bx = x + w - 8f - bw
        r.drawRect(bx, y + 1f, bw, 14f - 3f, 2f, 0x4D50C8C8.toInt())
        r.drawText("regular", label, bx + 4f, y + (14f - 8f) / 2f - 1f, 8f, 0xFF55CCCC.toInt())
    }
}