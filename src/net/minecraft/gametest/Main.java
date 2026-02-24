package net.minecraft.gametest;

import net.minecraft.gametest.framework.GameTestMainUtil;
import net.minecraft.obfuscate.DontObfuscate;

public class Main {
   @DontObfuscate
   public static void main(String[] p_393417_) throws Exception {
      net.minecraft.SharedConstants.tryDetectVersion();
      GameTestMainUtil.runGameTestServer(p_393417_, p_393535_ -> {});
   }
}
