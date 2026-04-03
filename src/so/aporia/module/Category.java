package so.aporia.module;

import net.minecraft.resources.Identifier;

/** Module categories for grouping in the UI. */
public enum Category {
    COMBAT ('a', "combat.png"),
    MOVE   ('c', "move.png"),
    VISUAL ('n', "visual.png"),
    PLAYER ('g', "player.png"),
    WORLD  ('v', "world.png"),
    MISC   ('m', "misc.png");

    /** Glyph character in the {@code caticons} font. */
    public final char icon;
    
    /** Texture identifier for the category icon. */
    public final Identifier texture;

    Category(char icon, String textureName) {
        this.icon = icon;
        this.texture = Identifier.fromNamespaceAndPath("aporia", "texture/" + textureName);
    }
}
