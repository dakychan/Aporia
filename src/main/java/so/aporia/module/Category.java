package so.aporia.module;

/** Module categories for grouping in the UI. */
public enum Category {
    /* icon chars from categoryicons font */
    COMBAT ('a'),  /* combat */
    MOVE   ('c'),  /* world/globe -> move */
    VISUAL ('n'),  /* visual */
    PLAYER ('g'),  /* person */
    WORLD  ('v'),  /* world */
    MISC   ('m');  /* dots */

    /** Glyph character in the {@code caticons} font. */
    public final char icon;

    Category(char icon) { this.icon = icon; }
}
