package so.aporia.module;

/** Module categories for grouping in the UI. */
public enum Category {
    COMBAT ('a'),
    MOVE   ('c'),
    VISUAL ('n'),
    PLAYER ('g'),
    WORLD  ('v'),
    MISC   ('m');

    /** Glyph character in the {@code caticons} font. */
    public final char icon;

    Category(char icon) { this.icon = icon; }
}
