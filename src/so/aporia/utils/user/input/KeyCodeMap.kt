package so.aporia.utils.user.input

import com.chaos.annotation.Obfuscate

@Obfuscate
object KeyCodeMap {

    private val SCANCODE_TO_NAME = mutableMapOf<Int, String>()
    private val NAME_TO_SCANCODE = mutableMapOf<String, Int>()

    init {
        // Ряд Esc и цифр
        addKey(1, "ESC")
        addKey(2, "1"); addKey(3, "2"); addKey(4, "3"); addKey(5, "4")
        addKey(6, "5"); addKey(7, "6"); addKey(8, "7"); addKey(9, "8")
        addKey(10, "9"); addKey(11, "0")
        addKey(12, "MINUS"); addKey(13, "EQUALS"); addKey(14, "BACKSPACE")
        
        // Буквы и табы
        addKey(15, "TAB")
        addKey(16, "Q"); addKey(17, "W"); addKey(18, "E"); addKey(19, "R")
        addKey(20, "T"); addKey(21, "Y"); addKey(22, "U"); addKey(23, "I")
        addKey(24, "O"); addKey(25, "P")
        addKey(26, "LBRACKET"); addKey(27, "RBRACKET"); addKey(28, "ENTER")
        
        // Модификаторы левые и средний ряд
        addKey(29, "LCTRL")
        addKey(30, "A"); addKey(31, "S"); addKey(32, "D"); addKey(33, "F")
        addKey(34, "G"); addKey(35, "H"); addKey(36, "J"); addKey(37, "K")
        addKey(38, "L")
        addKey(39, "SEMICOLON"); addKey(40, "APOSTROPHE"); addKey(41, "GRAVE")
        
        // Нижний ряд
        addKey(42, "LSHIFT"); addKey(43, "BACKSLASH")
        addKey(44, "Z"); addKey(45, "X"); addKey(46, "C"); addKey(47, "V")
        addKey(48, "B"); addKey(49, "N"); addKey(50, "M")
        addKey(51, "COMMA"); addKey(52, "PERIOD"); addKey(53, "SLASH")
        addKey(54, "RSHIFT")
        
        // Системные и правые модификаторы
        addKey(55, "MULTIPLY"); addKey(56, "LALT")
        addKey(57, "SPACE"); addKey(58, "CAPSLOCK")
        
        // Функциональные клавиши
        addKey(59, "F1"); addKey(60, "F2"); addKey(61, "F3"); addKey(62, "F4")
        addKey(63, "F5"); addKey(64, "F6"); addKey(65, "F7"); addKey(66, "F8")
        addKey(67, "F9"); addKey(68, "F10")
        addKey(87, "F11"); addKey(88, "F12")
        addKey(100, "F13"); addKey(101, "F14"); addKey(102, "F15")
        
        // Блок стрелок и навигации (стандартный LWJGL / расширенный набор)
        addKey(69, "NUMLOCK"); addKey(70, "SCROLL")
        addKey(71, "HOME"); addKey(72, "UP"); addKey(73, "PAGEUP")
        addKey(74, "SUBTRACT"); addKey(75, "LEFT"); addKey(76, "CLEAR")
        addKey(77, "RIGHT"); addKey(78, "ADD"); addKey(79, "END")
        addKey(80, "DOWN"); addKey(81, "PAGEDOWN"); addKey(82, "INSERT")
        addKey(83, "DELETE")
        
        // Нампад (Numpad) цифровой блок
        addKey(89, "NUMPAD0"); addKey(90, "NUMPAD1"); addKey(91, "NUMPAD2")
        addKey(92, "NUMPAD3"); addKey(93, "NUMPAD4"); addKey(94, "NUMPAD5")
        addKey(95, "NUMPAD6"); addKey(96, "NUMPAD7"); addKey(97, "NUMPAD8")
        addKey(98, "NUMPAD9")
        addKey(99, "NUMPADPERIOD")
        addKey(156, "NUMPADENTER")
        addKey(181, "DIVIDE") // Нампад слэш /
        
        // Дополнительные системные кнопки ОС
        addKey(157, "RCTRL")
        addKey(184, "RALT")
        addKey(197, "PAUSE")
        addKey(199, "HOME_EXT")
        addKey(200, "UP_EXT")
        addKey(201, "PAGEUP_EXT")
        addKey(203, "LEFT_EXT")
        addKey(205, "RIGHT_EXT")
        addKey(207, "END_EXT")
        addKey(208, "DOWN_EXT")
        addKey(209, "PAGEDOWN_EXT")
        addKey(210, "INSERT_EXT")
        addKey(211, "DELETE_EXT")
        addKey(219, "LWIN") // Левая кнопка Windows
        addKey(220, "RWIN") // Правая кнопка Windows
        addKey(221, "APPS") // Кнопка контекстного меню (рядом с правым Win)
        addKey(256, "PRINTSCREEN")

        // === МЫШКА В ВИРТУАЛЬНОМ ДИАПАЗОНЕ (500+) ===
        addKey(500, "LBUTTON") // Левая (Button 0)
        addKey(501, "RBUTTON") // Правая (Button 1)
        addKey(502, "MBUTTON") // Колесико (Button 2)
        addKey(503, "MOUSE4")  // Задняя боковая (Button 3 в LWJGL)
        addKey(504, "MOUSE5")  // Передняя боковая (Button 4 в LWJGL)
        addKey(505, "MOUSE6")  // Доп. кнопка мыши 6
        addKey(506, "MOUSE7")  // Доп. кнопка мыши 7
        addKey(507, "MOUSE8")  // Доп. кнопка мыши 8
    }

    private fun addKey(scancode: Int, name: String) {
        SCANCODE_TO_NAME[scancode] = name
        NAME_TO_SCANCODE[name] = scancode
    }

    @JvmStatic
    fun getName(scancode: Int): String = SCANCODE_TO_NAME.getOrDefault(scancode, "UNKNOWN")

    @JvmStatic
    fun getScancode(name: String): Int = NAME_TO_SCANCODE.getOrDefault(name.uppercase(), -1)

    @JvmStatic
    fun isKey(scancode: Int, name: String): Boolean = getScancode(name) == scancode
}
