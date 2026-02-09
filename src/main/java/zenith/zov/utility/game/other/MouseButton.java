package zenith.zov.utility.game.other;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum MouseButton {

    LEFT(0),
    RIGHT(1),
    MIDDLE(2),
    BUTTON_4(3),    // Боковая 1 (Назад)
    BUTTON_5(4),   // Боковая 2 (Вперёд)
    BUTTON_6(5),  // Доп. кнопка 1
    BUTTON_7(6); // Доп. кнопка 2

    private final int buttonIndex;

    public static MouseButton fromButtonIndex(int index) {
        for (MouseButton button : MouseButton.values()) {
            if (button.getButtonIndex() == index) {
                return button;
            }
        }
        return MouseButton.LEFT;
    }
}