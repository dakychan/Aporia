package ru.input;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyBinding RENDER_TEST_KEY;

    public static void register() {
        RENDER_TEST_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyBinding(
                        "key.sorray.render_test",
                        InputUtil.Type.KEYSYM,
                        GLFW.GLFW_KEY_GRAVE_ACCENT,
                        KeyBinding.Category.MISC
                )
        );
    }
}
