package so.aporia.utils.events.impl;

/** Fired on keyboard key press/release/repeat. */
public final class KeyInputEvent {

    public enum Action { PRESS, RELEASE, REPEAT }

    private final int key;
    private final int scancode;
    private final int modifiers;
    private final Action action;
    private boolean cancelled;

    public KeyInputEvent(int key, int scancode, int modifiers, Action action) {
        this.key = key; this.scancode = scancode;
        this.modifiers = modifiers; this.action = action;
    }

    /** GLFW key code. */
    public int key()        { return key; }
    public int scancode()   { return scancode; }
    /** GLFW modifier bitmask (SHIFT=1, CTRL=2, ALT=4). */
    public int modifiers()  { return modifiers; }
    public Action action()  { return action; }

    public boolean isCtrl()  { return (modifiers & 2) != 0; }
    public boolean isShift() { return (modifiers & 1) != 0; }
    public boolean isAlt()   { return (modifiers & 4) != 0; }

    public void cancel()         { cancelled = true; }
    public boolean isCancelled() { return cancelled; }
}
