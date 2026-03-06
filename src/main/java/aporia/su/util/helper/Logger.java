package aporia.su.util.helper;

/**
 *  © 2026 Copyright Aporia.cc 2.0
 *        All Rights Reserved ®
 */

public class Logger {

    private static final String RESET = "\u001B[0m";
    private static final String GREEN_BG = "\u001B[42m";
    private static final String RED_BG = "\u001B[41m";
    private static final String YELLOW_BG = "\u001B[43m";
    private static final String BLUE_BG = "\u001B[44m";
    private static final String BLACK = "\u001B[30m";
    private static final String WHITE = "\u001B[97m";
    private static final String BOLD = "\u001B[1m";

    private static final String PREFIX = "[Aporia] ";

    public static void success(String message) {
        System.out.println(GREEN_BG + BLACK + BOLD + " " + PREFIX + message + " " + RESET);
    }

    public static void error(String message) {
        System.err.println(RED_BG + WHITE + BOLD + " " + PREFIX + message + " " + RESET);
    }

    public static void warn(String message) {
        System.out.println(YELLOW_BG + BLACK + BOLD + " " + PREFIX + message + " " + RESET);
    }

    public static void info(String message) {
        System.out.println(BLUE_BG + WHITE + BOLD + " " + PREFIX + message + " " + RESET);
    }

    public static void debug(String message) {
        if (Boolean.getBoolean("aporia.debug")) {
            System.out.println("[DEBUG] " + PREFIX + message);
        }
    }
}
