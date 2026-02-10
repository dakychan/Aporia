package ru.gui;

public class GuiManager {
    private static GuiScreen currentScreen = null;

    public static void openScreen(GuiScreen screen) {
        currentScreen = screen;
    }

    public static void closeScreen() {
        currentScreen = null;
    }

    public static GuiScreen getCurrentScreen() {
        return currentScreen;
    }

    public static boolean isScreenOpen() {
        return currentScreen != null;
    }

    public static void render() {
        if (currentScreen != null) {
            currentScreen.render();
        }
    }
}
