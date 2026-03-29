package so.aporia.utils.user.input;

import java.util.HashMap;
import java.util.Map;

public class KeyCodeMap {
    private static final Map<Integer, String> SCANCODE_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> NAME_TO_SCANCODE = new HashMap<>();

    static {
        addKey(1, "ESC");
        addKey(2, "1");
        addKey(3, "2");
        addKey(4, "3");
        addKey(5, "4");
        addKey(6, "5");
        addKey(7, "6");
        addKey(8, "7");
        addKey(9, "8");
        addKey(10, "9");
        addKey(11, "0");
        addKey(12, "MINUS");
        addKey(13, "EQUALS");
        addKey(14, "BACKSPACE");
        addKey(15, "TAB");
        addKey(16, "Q");
        addKey(17, "W");
        addKey(18, "E");
        addKey(19, "R");
        addKey(20, "T");
        addKey(21, "Y");
        addKey(22, "U");
        addKey(23, "I");
        addKey(24, "O");
        addKey(25, "P");
        addKey(26, "LBRACKET");
        addKey(27, "RBRACKET");
        addKey(28, "ENTER");
        addKey(29, "LCTRL");
        addKey(30, "A");
        addKey(31, "S");
        addKey(32, "D");
        addKey(33, "F");
        addKey(34, "G");
        addKey(35, "H");
        addKey(36, "J");
        addKey(37, "K");
        addKey(38, "L");
        addKey(39, "SEMICOLON");
        addKey(40, "APOSTROPHE");
        addKey(41, "GRAVE");
        addKey(42, "LSHIFT");
        addKey(43, "BACKSLASH");
        addKey(44, "Z");
        addKey(45, "X");
        addKey(46, "C");
        addKey(47, "V");
        addKey(48, "B");
        addKey(49, "N");
        addKey(50, "M");
        addKey(51, "COMMA");
        addKey(52, "PERIOD");
        addKey(53, "SLASH");
        addKey(54, "RSHIFT");
        addKey(55, "MULTIPLY");
        addKey(56, "LALT");
        addKey(57, "SPACE");
        addKey(58, "CAPSLOCK");
        addKey(59, "F1");
        addKey(60, "F2");
        addKey(61, "F3");
        addKey(62, "F4");
        addKey(63, "F5");
        addKey(64, "F6");
        addKey(65, "F7");
        addKey(66, "F8");
        addKey(67, "F9");
        addKey(68, "F10");
        addKey(69, "NUMLOCK");
        addKey(70, "SCROLL");
        addKey(71, "HOME");
        addKey(72, "UP");
        addKey(73, "PAGEUP");
        addKey(74, "SUBTRACT");
        addKey(75, "LEFT");
        addKey(76, "CLEAR");
        addKey(77, "RIGHT");
        addKey(78, "ADD");
        addKey(79, "END");
        addKey(80, "DOWN");
        addKey(81, "PAGEDOWN");
        addKey(82, "INSERT");
        addKey(83, "DELETE");
        addKey(87, "F11");
        addKey(88, "F12");
    }

    private static void addKey(int scancode, String name) {
        SCANCODE_TO_NAME.put(scancode, name);
        NAME_TO_SCANCODE.put(name, scancode);
    }

    public static String getName(int scancode) {
        return SCANCODE_TO_NAME.getOrDefault(scancode, "UNKNOWN");
    }

    public static int getScancode(String name) {
        return NAME_TO_SCANCODE.getOrDefault(name, -1);
    }

    public static boolean isKey(int scancode, String name) {
        return getScancode(name) == scancode;
    }
}
