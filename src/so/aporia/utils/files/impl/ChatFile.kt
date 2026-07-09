package so.aporia.utils.files.impl
import com.chaos.annotation.Obfuscate
import so.aporia.utils.files.FilesManager
import so.aporia.utils.user.render.ui.chat.AporiaChatScreen.WinCfg
import so.aporia.utils.user.render.ui.chat.AporiaChatScreen.WinMgr
import java.util.ArrayList
import com.chaos.annotation.ChaosNative
@Obfuscate
@ChaosNative
object ChatFile {

    private val FILE = FilesManager.ROOT.resolve("chat.apr")

    class WinData {
        @JvmField var name = "Main"
        @JvmField var x = -1
        @JvmField var bottomY = -1
        @JvmField var w = 320
        @JvmField var h = 120
        @JvmField var draggable = false
        @JvmField var searchOnOpen = false
        @JvmField var showOnlyFilter = false
        @JvmField var showOnlyServer = false
        @JvmField var filterWords = ""
        @JvmField var msgPrefix = ""
        @JvmField var msgSuffix = ""
        @JvmField var prefixTriggers = ""
        @JvmField var prefixEnabled = true
        @JvmField var selfColor = -0x52271A
        @JvmField var scrollOffset = 0
    }

    class ChatData {
        @JvmField var active = 0
        @JvmField val wins = ArrayList<WinData>()
    }

    private val GSON = com.google.gson.GsonBuilder().setPrettyPrinting().create()

    @JvmStatic
    @Throws(Exception::class)
    fun save() {
        val data = ChatData()
        data.active = WinMgr.I.active
        for (c in WinMgr.I.wins) {
            val d = WinData()
            d.name = c.name
            d.x = c.x
            d.bottomY = c.bottomY
            d.w = c.w
            d.h = c.h
            d.draggable = c.draggable
            d.searchOnOpen = c.searchOnOpen
            d.showOnlyFilter = c.showOnlyFilter
            d.showOnlyServer = c.showOnlyServer
            d.filterWords = c.filterWords
            d.msgPrefix = c.msgPrefix
            d.msgSuffix = c.msgSuffix
            d.prefixTriggers = c.prefixTriggers
            d.prefixEnabled = c.prefixEnabled
            d.selfColor = c.selfColor
            d.scrollOffset = c.scrollOffset
            data.wins.add(d)
        }
        val json = GSON.toJson(data)
        FilesManager.writeApr(FILE, json)
    }

    @JvmStatic
    @Throws(Exception::class)
    fun load() {
        if (!FilesManager.exists(FILE)) return
        val json = FilesManager.readApr(FILE)
        val data = try { GSON.fromJson(json, ChatData::class.java) } catch (_: Exception) { null }
        if (data == null || data.wins.isEmpty()) return
        WinMgr.I.wins.clear()
        for (d in data.wins) {
            val c = WinCfg(d.name, d.x, d.bottomY, d.w, d.h, d.draggable)
            c.searchOnOpen = d.searchOnOpen
            c.showOnlyFilter = d.showOnlyFilter
            c.showOnlyServer = d.showOnlyServer
            c.filterWords = d.filterWords
            c.msgPrefix = d.msgPrefix
            c.msgSuffix = d.msgSuffix
            c.prefixTriggers = d.prefixTriggers
            c.prefixEnabled = d.prefixEnabled
            c.selfColor = d.selfColor
            c.scrollOffset = d.scrollOffset
            WinMgr.I.wins.add(c)
        }
        WinMgr.I.active = data.active.coerceIn(0, WinMgr.I.wins.size - 1)
    }
}