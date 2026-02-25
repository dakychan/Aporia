package cc.apr.input.api;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.KeyMapping.Category;

public class KeyBindings {
   public static KeyMapping RENDER_TEST_KEY;

   public static void register() {
      RENDER_TEST_KEY = new KeyMapping("key.sorray.render_test", Type.KEYSYM, 96, Category.MISC);
   }
}
