package ru.input.api.bind;

public class Keybind {
    private final String id;
    private int keyCode;
    private final Runnable action;

    public Keybind(String id, int keyCode, Runnable action) {
        this.id = id;
        this.keyCode = keyCode;
        this.action = action;
    }

    public void execute() {
        if (action != null) {
            action.run();
        }
    }

    public void setKeyCode(int keyCode) {
        this.keyCode = keyCode;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public String getId() {
        return id;
    }

    public Runnable getAction() {
        return action;
    }

    @Override
    public String toString() {
        return "Keybind{id='" + id + "', keyCode=" + keyCode + "}";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Keybind keybind = (Keybind) obj;
        return id.equals(keybind.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
