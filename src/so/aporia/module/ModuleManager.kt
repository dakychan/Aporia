package so.aporia.module

object ModuleManager {

    private val modules = mutableListOf<Module>()

    init {
        registerAll(
            so.aporia.module.impl.render.Hud(),
            so.aporia.module.impl.render.Beautifully(),
            so.aporia.module.impl.render.PlayerESP(),
            so.aporia.module.impl.render.NameTags(),
            so.aporia.module.impl.misc.ClickGui(),
            so.aporia.module.impl.misc.ServerHelper(),
            so.aporia.module.impl.misc.AutoConfig(),
            so.aporia.module.impl.misc.AutoEZ(),
            so.aporia.module.impl.misc.TestModule(),
            so.aporia.module.impl.misc.PacketDebug(),
            so.aporia.module.impl.misc.Disabler(),
            so.aporia.module.impl.misc.DiscordRPCModule(),
            so.aporia.module.impl.combat.AutoGapple(),
            so.aporia.module.impl.combat.AutoTotem(),
            so.aporia.module.impl.combat.God(),
            so.aporia.module.impl.player.NoPush(),
            so.aporia.module.impl.player.ElytraHelper(),
            so.aporia.module.impl.world.MiddleClick(),
            so.aporia.module.impl.move.Speed(),
            so.aporia.module.impl.move.AutoSprint(),
            so.aporia.module.impl.move.Velocity(),
            so.aporia.module.impl.move.Flight(),
            so.aporia.module.impl.combat.Criticals(),
            so.aporia.module.impl.combat.TPAura(),
            so.aporia.module.impl.combat.Aura(),
            so.aporia.module.impl.combat.SpearTarget(),
            so.aporia.module.impl.combat.NoFriendDamage(),
            so.aporia.module.impl.combat.ElytraTarget(),
            so.aporia.module.impl.render.NoRender(),
            so.aporia.module.impl.render.PlayerBlur(),
            so.aporia.module.impl.world.WorldRenderer()
        )
    }

    @JvmStatic
    fun register(module: Module) {
        modules.add(module)
    }

    @JvmStatic
    fun registerAll(vararg mods: Module) {
        for (m in mods) register(m)
    }

    @JvmStatic
    fun getAll(): List<Module> = modules.toList()

    @JvmStatic
    fun getByCategory(category: Category): List<Module> {
        return modules.filter { it.category == category }
    }

    @JvmStatic
    fun get(name: String): Module? {
        return modules.firstOrNull { it.name.equals(name, ignoreCase = true) }
    }
}
