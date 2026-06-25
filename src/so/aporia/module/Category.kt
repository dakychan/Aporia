package so.aporia.module

import net.minecraft.resources.Identifier

enum class Category(@JvmField val icon: Char, textureName: String) {
    COMBAT('a', "combat.png"),
    MOVE('c', "move.png"),
    VISUAL('n', "visual.png"),
    PLAYER('g', "player.png"),
    WORLD('v', "world.png"),
    MISC('m', "misc.png");

    val texture: Identifier = Identifier.fromNamespaceAndPath("aporia", "texture/$textureName")
    fun icon(): Char = icon
}
