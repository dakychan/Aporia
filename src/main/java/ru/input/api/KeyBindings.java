package ru.input.api;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static KeyMapping RENDER_TEST_KEY;

    public static void register() {
        RENDER_TEST_KEY = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.sorray.render_test",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_GRAVE_ACCENT,
                        KeyMapping.Category.MISC
                )
        );
    }
}
