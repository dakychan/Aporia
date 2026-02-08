package ru.gui;

/**
 * Базовый класс для GUI экранов
 */
public abstract class GuiScreen {
    protected int width;
    protected int height;
    protected double mouseX;
    protected double mouseY;

    public GuiScreen(int width, int height) {
        this.width = width;
        this.height = height;
        this.mouseX = 0;
        this.mouseY = 0;
    }

    public abstract void render();

    public void onKeyPressed(int keyCode) {
    }

    public void onKeyReleased(int keyCode) {
    }

    public void onMouseMove(double x, double y) {
        this.mouseX = x;
        this.mouseY = y;
    }

    public void onMouseClick(double x, double y, int button) {
    }

    public void onMouseRelease(double x, double y, int button) {
    }

    public void close() {
        GuiManager.closeScreen();
    }
}
