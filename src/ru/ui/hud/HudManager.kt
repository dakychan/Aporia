package ru.ui.hud

import com.ferra13671.cometrenderer.plugins.minecraft.MinecraftPlugin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import aporia.cc.Logger
import ru.manager.OsManager
import ru.manager.OsManager.DirectoryType
import java.nio.file.Files
import java.nio.file.Path

/**
 * Единый менеджер HUD компонентов.
 * Объединяет управление компонентами и их базовую функциональность.
 */
object HudManager {

    private val gson = Gson()
    private val configPath: Path = OsManager.getFile(DirectoryType.CONFIG, "HudConfig.json")
    private val components = mutableListOf<HudComponent>()
    private var nextZIndex = 0

    /**
     * HUD компонент (open для наследования).
     */
    open class HudComponent(val name: String) {
        var x: Float = 0f
        var y: Float = 0f
        var width: Float = 0f
        var height: Float = 0f
        private var zIndex: Int = 0

        var isDragging = false
        private var dragOffsetX = 0f
        private var dragOffsetY = 0f

        /**
         * Функция рендеринга (задаётся при создании).
         */
        open var renderCallback: ((MinecraftPlugin) -> Unit)? = null

        /**
         * Функция обновления размера (опционально).
         */
        open var updateSizeCallback: ((MinecraftPlugin) -> Unit)? = null

        fun getZIndex(): Int = zIndex

        fun setZIndex(value: Int) {
            zIndex = value
        }

        open fun render(plugin: MinecraftPlugin) {
            renderCallback?.invoke(plugin)
        }

        open fun updateSize(plugin: MinecraftPlugin) {
            updateSizeCallback?.invoke(plugin)
        }

        fun isHovered(mouseX: Int, mouseY: Int): Boolean {
            return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height
        }

        fun startDrag(mouseX: Int, mouseY: Int) {
            isDragging = true
            dragOffsetX = mouseX - x
            dragOffsetY = mouseY - y
        }

        fun drag(mouseX: Int, mouseY: Int, fbWidth: Int, fbHeight: Int) {
            if (isDragging) {
                x = (mouseX - dragOffsetX).coerceIn(0f, (fbWidth - width).coerceAtLeast(0f))
                y = (mouseY - dragOffsetY).coerceIn(0f, (fbHeight - height).coerceAtLeast(0f))
            }
        }

        fun stopDrag() {
            isDragging = false
        }
    }

    /**
     * Инициализировать HUD менеджер.
     */
    fun initialize() {
        loadConfig()
    }

    /**
     * Добавить компонент.
     */
    fun addComponent(component: HudComponent) {
        if (!components.contains(component)) {
            components.add(component)
            component.setZIndex(nextZIndex++)
        }
    }

    /**
     * Удалить компонент.
     */
    fun removeComponent(component: HudComponent) {
        components.remove(component)
    }

    /**
     * Получить все компоненты.
     */
    fun getComponents(): List<HudComponent> = components.toList()

    /**
     * Переместить компонент на передний план.
     */
    fun bringToFront(component: HudComponent) {
        if (components.contains(component)) {
            nextZIndex++
            component.setZIndex(nextZIndex)
        }
    }

    /**
     * Рендеринг всех компонентов (по z-index).
     */
    fun render(plugin: MinecraftPlugin) {
        val sorted = components.sortedBy { it.getZIndex() }
        for (component in sorted) {
            component.render(plugin)
        }
    }

    /**
     * Обработка клика мыши.
     */
    fun handleMouseClick(mouseX: Int, mouseY: Int, button: Int): HudComponent? {
        if (button != 0) return null // Только ЛКМ

        // Проверяем с конца (верхние компоненты primero)
        for (component in components.asReversed()) {
            if (component.isHovered(mouseX, mouseY)) {
                bringToFront(component)
                component.startDrag(mouseX, mouseY)
                return component
            }
        }

        return null
    }

    /**
     * Обработка перетаскивания.
     */
    fun handleMouseDrag(mouseX: Int, mouseY: Int, fbWidth: Int, fbHeight: Int) {
        for (component in components) {
            if (component.isDragging) {
                component.drag(mouseX, mouseY, fbWidth, fbHeight)
            }
        }
    }

    /**
     * Обработка отпускания мыши.
     */
    fun handleMouseRelease() {
        for (component in components) {
            component.stopDrag()
        }
        saveConfig()
    }

    /**
     * Сохранить конфигурацию.
     */
    fun saveConfig() {
        try {
            val configData = components.map { component ->
                mapOf(
                    "name" to component.name,
                    "x" to component.x,
                    "y" to component.y,
                    "zIndex" to component.getZIndex()
                )
            }

            val json = gson.toJson(configData)
            Files.writeString(configPath, json)
        } catch (e: Exception) {
            Logger.error("Failed to save HUD configuration", e)
        }
    }

    /**
     * Загрузить конфигурацию.
     */
    fun loadConfig() {
        try {
            if (!Files.exists(configPath)) {
                return
            }

            val json = Files.readString(configPath)
            val type = object : TypeToken<List<Map<String, Any>>>() {}.type
            val configData: List<Map<String, Any>> = gson.fromJson(json, type)

            for (config in configData) {
                val name = config["name"] as String
                val component = components.find { it.name == name }
                if (component != null) {
                    component.x = (config["x"] as Number).toFloat()
                    component.y = (config["y"] as Number).toFloat()
                    component.setZIndex((config["zIndex"] as Number).toInt())
                }
            }
        } catch (e: Exception) {
            Logger.error("Failed to load HUD configuration", e)
        }
    }

    /**
     * Создать новый HUD компонент.
     * 
     * @param name Имя компонента
     * @param render Функция рендеринга
     * @param updateSize Функция обновления размера (опционально)
     */
    fun createComponent(
        name: String,
        render: (MinecraftPlugin) -> Unit,
        updateSize: ((MinecraftPlugin) -> Unit)? = null
    ): HudComponent {
        return HudComponent(name).apply {
            renderCallback = render
            updateSizeCallback = updateSize
        }
    }
}
