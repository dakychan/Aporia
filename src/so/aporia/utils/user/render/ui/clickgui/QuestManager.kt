package so.aporia.utils.user.render.ui.clickgui

import so.aporia.utils.imports.*
import com.chaos.annotation.Obfuscate
import net.minecraft.core.component.DataComponents
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.TickEvent
import so.aporia.utils.events.impl.ChatMessageEvent
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.files.FilesManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * QuestManager — игровая система квестов внутри ClickGui.
 *
 * Квесты бывают следующих типов:
 *  - KILL_PLAYERS   — убить N игроков (детект по входящим kill-сообщениям + Aura-hук)
 *  - WALK_BLOCKS    — пройти N блоков (Euclidean distance per tick, фильтр телепортов)
 *  - EAT_FOOD       — съесть N еды (хук из AutoGapple/AutoTotem или ручной [notifyAte])
 *  - MINE_BLOCKS    — сломать N блоков (по исходящему ServerboundPlayerActionPacket + ручной [notifyBlockMined])
 *  - DEATHS         — умереть N раз (детект в [onTick] по mc.player?.isDeadOrDying)
 *  - CHAT_MESSAGES  — отправить N сообщений в чат (исходящий ServerboundChatPacket + [notifyChatSent])
 *  - SURVIVE_TICKS  — выжить N тиков (накапливается раз в секунду тика)
 *
 * Прогресс сохраняется в `~/.apr/quests/quests.apr` через [FilesManager.writeApr]/[readApr].
 * Сохранение триггерится:
 *  - после каждого прогресса (но не чаще, чем раз в [SAVE_DEBOUNCE_MS] мс — дебаунс);
 *  - при [reset]/[resetAll];
 *  - при выгрузке через [save] (можно дёрнуть вручную).
 *
 * Создан как singleton (object).
 */
@Obfuscate
object QuestManager {

    /* ============ Типы и модели ============ */

    enum class QuestType(val displayName: String) {
        KILL_PLAYERS("Kill Players"),
        WALK_BLOCKS("Walk Blocks"),
        EAT_FOOD("Eat Food"),
        MINE_BLOCKS("Mine Blocks"),
        DEATHS("Deaths"),
        CHAT_MESSAGES("Chat Messages"),
        SURVIVE_TICKS("Survive Ticks")
    }

    data class Quest(
        val id: String,
        val type: QuestType,
        val description: String,
        val target: Int,
        val reward: String? = null
    ) {
        val progress: Int get() = QuestManager.progress(this)
        val completed: Boolean get() = progress >= target
        val percent: Float
            get() = (progress.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    /** Структура, которую сериализуем в `quests.apr`. Gson-friendly. */
    class SavedQuest {
        @JvmField var id: String = ""
        @JvmField var type: String = ""
        @JvmField var progress: Int = 0
        @JvmField var target: Int = 0
    }

    class SaveFile {
        @JvmField var version: Int = 1
        @JvmField val quests = ArrayList<SavedQuest>()
    }

    /* ============ Состояние ============ */

    private val activeQuests = ConcurrentHashMap<String, Quest>()
    private val progress = ConcurrentHashMap<String, Int>()

    /** Дельта-счётчик выжитых тиков (накапливаем ровно по тикам, не по монотонному аптайму). */
    private val surviveTickCounter = AtomicInteger(0)

    /** Последняя позиция игрока для WALK_BLOCKS. */
    private var lastPos: Triple<Double, Double, Double>? = null

    /** Последний known-state "жив". Для детекта перехода жив→мёртв. */
    private var lastAlive: Boolean = true

    /** Дебаунс записи на диск, мс. */
    private val SAVE_DEBOUNCE_MS = 1500L
    @Volatile private var lastSaveAt: Long = 0L

    /* ============ Пути ============ */

    private val SAVE_FILE get() = files.ROOT.resolve("quests").resolve("quests.apr")

    /* ============ Публичная API ============ */

    fun init() {
        registerDefaultQuests()
        load()
        bus.register(this)
        logger.info("QuestManager initialized: ${activeQuests.size} quests, ${progress.size} progress entries")
    }

    private fun registerDefaultQuests() {
        register(Quest("welcome_kill", QuestType.KILL_PLAYERS, "Убей 3 игроков", 3, "Aura unlocked"))
        register(Quest("welcome_walk", QuestType.WALK_BLOCKS, "Пройди 1000 блоков", 1000))
        register(Quest("welcome_food", QuestType.EAT_FOOD, "Съешь 5 еды", 5))
        register(Quest("welcome_chat", QuestType.CHAT_MESSAGES, "Отправь 10 сообщений в чат", 10))
        register(Quest("welcome_survive", QuestType.SURVIVE_TICKS, "Выживи 24000 тиков (20 мин)", 24000, "Trust +1"))
    }

    fun register(quest: Quest) {
        activeQuests[quest.id] = quest
        // Если в сейве прогресса нет — стартуем с 0.
        progress.computeIfAbsent(quest.id) { 0 }
    }

    fun unregister(id: String) {
        activeQuests.remove(id)
        progress.remove(id)
        scheduleSave()
    }

    fun getAll(): List<Quest> = activeQuests.values.toList()

    fun progress(quest: Quest): Int = progress[quest.id] ?: 0

    /**
     * Атомарный инкремент прогресса. Не дёргается из публичного API модулей напрямую —
     * используйте типизированные хуки ниже; этот метод открыт для тестов/админ-команд.
     */
    fun increment(questId: String, delta: Int = 1) {
        val q = activeQuests[questId] ?: return
        // Атомарно: прочитать → изменить → записать в один шаг.
        val newValue = progress.compute(questId) { _, current ->
            (current ?: 0) + delta
        } ?: 0

        // Не считаем пропуск completed→true повторно (на каждом инкременте не спамим).
        val justCompleted = newValue == q.target
        if (justCompleted) {
            logger.success("Quest '${q.description}' completed!")
            q.reward?.let { logger.info("Reward: $it") }
        }
        scheduleSave()
    }

    fun reset(questId: String) {
        progress[questId] = 0
        scheduleSave(force = true)
    }

    fun resetAll() {
        // Не трогаем activeQuests, чтобы не потерять список квестов.
        val ids = progress.keys.toList()
        ids.forEach { progress[it] = 0 }
        surviveTickCounter.set(0)
        lastPos = null
        lastAlive = mc.player?.isAlive ?: true
        scheduleSave(force = true)
    }

    /* ===== Тип-специфичные хуки (для модулей/событий) ===== */

    /** Aura / module → после успешной атаки по игроку. */
    fun notifyKill() {
        activeQuests.values.firstOrNull { it.type == QuestType.KILL_PLAYERS }?.let { q ->
            increment(q.id, 1)
        }
    }

    /** AutoGapple / AutoTotem / прочее → когда использовали предмет-еду. */
    fun notifyAte() {
        activeQuests.values.firstOrNull { it.type == QuestType.EAT_FOOD }?.let { q ->
            increment(q.id, 1)
        }
    }

    /** break-модули / сам QuestManager по ServerboundPlayerActionPacket → после успешного слома блока. */
    fun notifyBlockMined() {
        activeQuests.values.firstOrNull { it.type == QuestType.MINE_BLOCKS }?.let { q ->
            increment(q.id, 1)
        }
    }

    /** Исходящее сообщение чата. */
    fun notifyChatSent() {
        activeQuests.values.firstOrNull { it.type == QuestType.CHAT_MESSAGES }?.let { q ->
            increment(q.id, 1)
        }
    }

    /* ===== Сохранение / загрузка ===== */

    /** Принудительное сохранение (например, из команды или shutdown-хук). */
    fun save() {
        saveNow()
    }

    private fun scheduleSave(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && now - lastSaveAt < SAVE_DEBOUNCE_MS) return
        lastSaveAt = now
        saveNow()
    }

    private fun saveNow() {
        try {
            val data = SaveFile()
            // Для каждого активного квеста сохраняем прогресс.
            for ((id, quest) in activeQuests) {
                val s = SavedQuest()
                s.id = id
                s.type = quest.type.name
                s.progress = progress[id] ?: 0
                s.target = quest.target
                data.quests.add(s)
            }
            // Плюс — орфаны: прогресс по id, которого больше нет в реестре (на случай, если сейв
            // захочется восстановить после обновления без потери истории). Не критично, но честно.
            for ((id, p) in progress) {
                if (activeQuests.containsKey(id)) continue
                val s = SavedQuest()
                s.id = id
                s.type = "UNKNOWN"
                s.progress = p
                s.target = 0
                data.quests.add(s)
            }
            FilesManager.writeApr(SAVE_FILE, FilesManager.GSON.toJson(data))
        } catch (e: Exception) {
            logger.warn("QuestManager: save failed: ${e.message}")
        }
    }

    private fun load() {
        try {
            if (!FilesManager.exists(SAVE_FILE)) return
            val json = FilesManager.readApr(SAVE_FILE)
            val data = FilesManager.GSON.fromJson(json, SaveFile::class.java) ?: return
            for (s in data.quests) {
                if (s.id.isEmpty()) continue
                // Восстанавливаем только те, что есть в реестре.
                if (activeQuests.containsKey(s.id)) {
                    progress[s.id] = s.progress.coerceAtLeast(0)
                } else {
                    // На случай, если квест добавили в реестр после сейва — попробуем восстановить
                    // по типу, если такой тип сейчас есть. Это редкий случай, но стоит копейки.
                    val type = runCatching { QuestType.valueOf(s.type) }.getOrNull() ?: continue
                    if (s.target > 0) {
                        register(Quest(s.id, type, s.id, s.target))
                        progress[s.id] = s.progress.coerceAtLeast(0)
                    }
                }
            }
            logger.info("QuestManager: loaded ${data.quests.size} entries from ${SAVE_FILE}")
        } catch (e: Exception) {
            logger.warn("QuestManager: load failed: ${e.message}")
        }
    }

    /* ===== Event-driven прогресс ===== */

    /** Тот же набор regex'ов, что в AutoEZ — это самый надёжный детектор реальных PvP-киллов. */
    private val KILL_CHAT_RU = Regex("""Вы\s+убили\s+([^\s.!?]+)""", RegexOption.IGNORE_CASE)
    private val KILL_CHAT_EN = Regex("""You\s+killed\s+([^\s.!?]+)""", RegexOption.IGNORE_CASE)

    @EventHandler
    fun onTick(e: TickEvent) {
        val player = mc.player ?: run {
            // Вне мира — обнуляем state, чтоб не было ложных киллов/смертей.
            lastPos = null
            return
        }
        surviveTickCounter.incrementAndGet()

        /* SURVIVE_TICKS — раз в секунду тика (20 тиков) прибавляем 20 к счётчику квеста,
           чтобы единицы были "тики", а не "секунды". Дельта, не абсолют — переживает рестарт. */
        if (surviveTickCounter.get() % 20 == 0) {
            activeQuests.values.firstOrNull { it.type == QuestType.SURVIVE_TICKS }?.let { q ->
                increment(q.id, 20)
            }
        }

        /* WALK_BLOCKS — Euclidean distance per tick, фильтруем телепорты и стационар. */
        val curPos = Triple(player.x, player.y, player.z)
        val prev = lastPos
        if (prev != null) {
            val dx = curPos.first - prev.first
            val dy = curPos.second - prev.second
            val dz = curPos.third - prev.third
            val d = Math.sqrt(dx * dx + dy * dy + dz * dz)
            if (d in 0.05..10.0) {
                activeQuests.values.firstOrNull { it.type == QuestType.WALK_BLOCKS }?.let { q ->
                    val toAdd = d.toInt()
                    if (toAdd > 0) increment(q.id, toAdd)
                }
            }
        }
        lastPos = curPos

        /* DEATHS — ловим переход жив→мёртв.
           PlayerDeathEvent в репе не диспатчится ни одним модулем (см. grep), поэтому
           собственный детектор внутри onTick надёжнее. */
        val nowAlive = player.isAlive
        if (lastAlive && !nowAlive) {
            activeQuests.values.firstOrNull { it.type == QuestType.DEATHS }?.let { q ->
                increment(q.id, 1)
            }
        }
        lastAlive = nowAlive
    }

    /**
     * Входящее сообщение чата — парсим "X killed Y" / "Вы убили Y" и считаем прогресс квеста.
     * AutoEZ уже использует этот же подход; делаем независимо, чтобы прогресс не зависел от
     * включения AutoEZ.
     */
    @EventHandler
    fun onChat(e: ChatMessageEvent) {
        if (mc.player == null) return
        val text = e.plainText ?: return
        val m = KILL_CHAT_RU.find(text) ?: KILL_CHAT_EN.find(text) ?: return
        // Если в чате про нас (нас убили) — пропускаем, нас интересуют только наши киллы.
        // Регэксп и так матчит "X killed Y" где X — игрок, отправляющий действие. Поскольку
        // сообщение приходит от сервера, "X" — это ник атакующего (т.е. мы).
        notifyKill()
    }

    /**
     * Outbound-пакеты: ловим чат-сообщения и ломание блоков.
     * На этих пакетах QuestManager независим от того, включены ли модули чата/break'а.
     */
    @EventHandler
    fun onPacket(e: PacketEvent) {
        if (e.direction() != PacketEvent.Direction.OUTBOUND) return
        val pkt = e.packet

        when (pkt) {
            is ServerboundChatPacket -> notifyChatSent()
            is ServerboundUseItemPacket -> {
                // Использование предмета правой рукой — обычно еда (золотое яблоко, хлеб, стейк…).
                // Это "еда" если предмет в используемой руке — пищевой.
                val hand = pkt.hand
                val itemStack = mc.player?.getItemInHand(hand) ?: return
                if (itemStack.has(DataComponents.FOOD)) notifyAte()
            }
            is ServerboundPlayerActionPacket -> {
                if (pkt.action == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) {
                    notifyBlockMined()
                }
            }
        }
    }

    /**
     * Ручной хук для Aura — вызывать после успешной атаки по игроку.
     * Это дополнение к in-chat-детекту: модуль Aura вызывает его синхронно, иначе игрок
     * может убить и сразу уйти до того, как сервер пришлёт в чат "You killed ...".
     */
    fun onPlayerKilled() = notifyKill()
}
