package so.aporia.module.impl.combat

import net.minecraft.network.protocol.game.*
import net.minecraft.world.phys.Vec3
import so.aporia.module.Category
import so.aporia.module.Module
import so.aporia.module.settings.BooleanSetting
import so.aporia.module.settings.SliderSetting
import so.aporia.module.settings.SelectSetting
import so.aporia.utils.events.EventHandler
import so.aporia.utils.events.impl.PacketEvent
import so.aporia.utils.events.impl.TickEvent
import net.minecraft.client.Minecraft
import so.aporia.utils.imports.*
import com.chaos.annotation.ChaosNative
@ChaosNative
class God : Module("God", Category.COMBAT) {
    companion object {
        @JvmField val mc = Minecraft.getInstance()
    }

    val mode = SelectSetting("Mode", "God mode logic")
        .value("Sustain", "Clip", "Auto")
        .selected("Auto")
    
    val minHealth = SliderSetting("Min Health", "Trigger Clip/Desync HP", 6.0, 1.0, 19.0, 1.0)
    val antiDeath = BooleanSetting("Anti Death", "Force restore HP on 0", true)
    val antiKnockback = BooleanSetting("Anti Knockback", "Cancel velocity on damage", true)

    private var spoofHealth = -1f
    private var tickCounter = 0
    private var clipCooldown = 0

    override val settings = listOf(mode, minHealth, antiDeath, antiKnockback)

    override fun onEnable() {
        bus.register(this)
    }

    override fun onDisable() {
        bus.unregister(this)
        tickCounter = 0
        clipCooldown = 0
    }

    @EventHandler
    fun onTick(event: TickEvent) {
        if (mc.player == null || mc.level == null) return

        tickCounter++
        if (mc.player!!.health > spoofHealth || spoofHealth <= 0) {
            spoofHealth = mc.player!!.health
        }

        // Если клиент почему-то решил что мы мертвы (например после закрытия экрана смерти)
        if (mc.player!!.health <= 0 && antiDeath.isEnabled) {
            mc.player!!.setHealth(if (spoofHealth > 0) spoofHealth else 20.0f)
        }

        if (clipCooldown > 0) {
            clipCooldown--
            return
        }

        // Автоматический клип, если ХП просело ниже настройке
        if ((mode.isSelected("Clip") || mode.isSelected("Auto")) && mc.player!!.health <= minHealth.getFloat() && clipCooldown == 0) {
            clip(-15.0)
            clipCooldown = 40 // Кулдаун 2 секунды, чтобы не спамить и не кикнуло
        }

        // Sustain: Спамим фейковые пакеты пола, чтобы сбить синхронизацию позиции с сервером.
        // Сервер не знает точно, где мы стоим, и не может нанести точный урон от сущностей.
        if (mode.isSelected("Sustain") || mode.isSelected("Auto")) {
            if (tickCounter % 2 == 0) {
                sendDesync()
            }
        }
    }

    @EventHandler
    fun onPacketReceive(event: PacketEvent) {
        if (mc.player == null) return
        if (event.direction != PacketEvent.Direction.INBOUND) return

        val packet = event.packet
        when (packet) {
            is ClientboundDamageEventPacket -> {
                if (packet.entityId == mc.player!!.id) {
                    event.cancel() // Отменяем сам факт получения урона на клиенте

                    if (mode.isSelected("Clip") || mode.isSelected("Auto")) {
                        if (clipCooldown == 0) {
                            clip(-15.0)
                            clipCooldown = 40
                        }
                    }
                }
            }
            is ClientboundHurtAnimationPacket -> {
                if (packet.id == mc.player!!.id) event.cancel() // Убираем красный экран
            }
            // САМОЕ ГЛАВНОЕ: Отмена отбрасывания. Без этого тебя выкинет из блока при клипе.
            is ClientboundSetEntityMotionPacket -> {
                if (packet.id == mc.player!!.id && antiKnockback.isEnabled) {
                    event.cancel()
                }
            }
            is ClientboundPlayerCombatKillPacket -> {
                if (packet.playerId == mc.player!!.id) {
                    event.cancel() // Блокируем экран смерти
                    if (antiDeath.isEnabled) {
                        mc.player!!.setHealth(if (spoofHealth > 0) spoofHealth else 20.0f)
                    }
                }
            }
            is ClientboundSetHealthPacket -> {
                val newHealth = packet.health

                if (newHealth <= 0 && antiDeath.isEnabled) {
                    // Сервер говорит: "Ты помер". Мы говорим: "Нет"
                    event.cancel()
                    mc.player!!.setHealth(if (spoofHealth > 0) spoofHealth else 20.0f)
                } else if (newHealth < spoofHealth && newHealth > 0) {
                    // Сервер пытается снизить ХП. Мы игнорируем и оставляем старое.
                    event.cancel()
                    mc.player!!.setHealth(spoofHealth)
                } else if (newHealth >= spoofHealth) {
                    // Если ХП выросло (например, от зелий или аппетита) - обновляем наше спуфенное значение
                    spoofHealth = newHealth
                }
            }
        }
    }

    private fun clip(offset: Double) {
        if (mc.player == null) return
        val p = mc.player!!

        val x = p.x
        val y = p.y
        val z = p.z

        // Быстрый спам в текущую позицию, чтобы сервер её зафиксировал
        for (i in 0 until 5) {
            p.connection.send(ServerboundMovePlayerPacket.Pos(x, y, z, false, false))
        }

        // Резкий клип вниз
        for (i in 0 until 5) {
            p.connection.send(ServerboundMovePlayerPacket.Pos(x, y + offset, z, false, false))
        }

        p.setPos(x, y + offset, z)
    }

    private fun sendDesync() {
        if (mc.player == null) return
        val p = mc.player!!
        // Спамим инвертированным состоянием onGround. 
        // Это заставляет античит думать, что мы летим/прыгаем, сбивая точность хитбоксов.
        p.connection.send(ServerboundMovePlayerPacket.StatusOnly(!p.onGround(), false))
    }
}