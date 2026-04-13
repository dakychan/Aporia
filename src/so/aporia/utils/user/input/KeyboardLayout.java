/*
 * Copyright (c) 2025-2026 Aporia.cc Project
 * Distributed under the Aporia.cc Software License Agreement v1.0
 * See LICENSE and COPYRIGHT files in the project root for full text.
 */

package so.aporia.utils;

/**
 * Converts text typed in the wrong keyboard layout (RU↔EN).
 * "Мустерыwорлд" → "MusteryWorld" and vice-versa.
 */
public final class KeyboardLayout {

    /* EN → RU mapping (same physical key) */
    private static final String EN = "qwertyuiop[]asdfghjkl;'zxcvbnm,." +
                                     "QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>";
    private static final String RU = "йцукенгшщзхъфывапролджэячсмитьбю" +
                                     "ЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";

    private KeyboardLayout() {}

    /** Converts every character as if typed on the opposite layout. */
    public static String convert(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int idx = RU.indexOf(c);
            if (idx >= 0) { sb.append(EN.charAt(idx)); continue; }
            idx = EN.indexOf(c);
            if (idx >= 0) { sb.append(RU.charAt(idx)); continue; }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * Returns both the original query and its layout-converted variant.
     * Search against both to handle mistyped layout.
     */
    public static String[] both(String query) {
        return new String[]{ query, convert(query) };
    }
}
