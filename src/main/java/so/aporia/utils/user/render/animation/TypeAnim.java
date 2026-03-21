package so.aporia.utils.user.render.animation;

import java.util.Random;

/**
 * TypeAnim — анимация "взлома сейфа" для текста.
 *
 * Каждый символ целевой строки "подбирается" по одному:
 *  - пока символ не найден, на его месте мелькают случайные символы из алфавита
 *  - как только символ "угадан" — он фиксируется
 *  - лишние символы старой строки медленно стираются справа
 *
 * Использование:
 * <pre>{@code
 * TypeAnim anim = new TypeAnim(60, 120); // msPerChar, shuffleMs
 * anim.setTarget("Агрессивный");
 * // каждый кадр:
 * String display = anim.update();
 * renderer.drawText(font, display, x, y, size, color);
 * }</pre>
 */
public final class TypeAnim {
    private static final char[] ALPHABET;
    static {
        String src = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                   + "0123456789!@#$%&*?<>€£¥¢§©®™°±×÷≠≤≥∞≈∑√∫∆∂∏←↑→↓↔↕↵↖↗↘↙"
                   + "АБВГДЕЖЗИКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдежзиклмнопрстуфхцчшщъыьэюя";
        ALPHABET = src.toCharArray();
    }

    private final long msPerChar;
    private final long shuffleMs;

    private String current  = "";
    private String target   = "";

    private long   startMs  = 0;
    private boolean running = false;

    private final Random rng = new Random();

    public TypeAnim(long msPerChar, long shuffleMs) {
        this.msPerChar = msPerChar;
        this.shuffleMs = shuffleMs;
    }

    /** Устанавливает новую целевую строку и запускает анимацию. */
    public void setTarget(String newTarget) {
        if (newTarget.equals(target) && running) return;
        current = target; // начинаем со старого значения
        target  = newTarget;
        startMs = System.currentTimeMillis();
        running = true;
    }

    /** Мгновенно устанавливает строку без анимации. */
    public void snap(String value) {
        target  = value;
        current = value;
        running = false;
    }

    /** Возвращает true пока анимация идёт. */
    public boolean isRunning() { return running; }

    /**
     * Вызывать каждый кадр. Возвращает строку для отображения.
     * Строка содержит уже зафиксированные символы + мелькающие + остатки старой строки.
     */
    public String update() {
        if (!running) return target;
        long elapsed = System.currentTimeMillis() - startMs;
        int fixed = (int)(elapsed / msPerChar);
        if (fixed >= target.length()) {
            running = false;
            current = target;
            return target;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fixed; i++) {
            sb.append(target.charAt(i));
        }
        long charElapsed = elapsed - (long)fixed * msPerChar;
        float shuffleT = Math.min(1f, (float)charElapsed / shuffleMs);
        long shuffleInterval = Math.max(16, (long)(80 * (1f - shuffleT * 0.7f)));
        char glitch = ALPHABET[(int)((System.currentTimeMillis() / shuffleInterval) % ALPHABET.length)];
        sb.append(glitch);
        int oldLen = current.length();
        int newLen = target.length();
        int erased = (int)(elapsed / (msPerChar * 0.6));
        int oldTail = Math.max(0, oldLen - fixed - 1 - erased);
        int oldStart = fixed + 1;
        for (int i = 0; i < oldTail && oldStart + i < oldLen; i++) {
            sb.append(current.charAt(oldStart + i));
        }
        return sb.toString();
    }
    public String getTarget() { return target; }
}
