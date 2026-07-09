package so.aporia.utils.imports
import aporia.cc.OsManager
import net.minecraft.client.Minecraft
import so.aporia.Aporia
import so.aporia.module.ModuleManager
import so.aporia.utils.events.EventBus
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.friend.FriendManager
import so.aporia.utils.user.locale.LocaleManager
import so.aporia.utils.user.logger.Logger
import so.aporia.utils.user.render.color.ColorUtil
import so.aporia.utils.user.render.core.AporiaRenderer
import so.aporia.utils.user.render.font.Fonts
import so.aporia.utils.user.render.theme.ThemeManager
import com.chaos.annotation.ChaosNative
val r get() = AporiaRenderer.INSTANCE
val mc get() = Minecraft.getInstance()
val logger get() = Logger
val fonts get() = Aporia.FONTS
val locale get() = LocaleManager.INSTANCE
val theme get() = ThemeManager.INSTANCE.active()
val files get() = FilesManager
val os get() = OsManager
val bus get() = EventBus
val mm get() = ModuleManager
val colorUtil get() = ColorUtil
val fm get() = FriendManager