package ru.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import ru.module.Module;

/**
 * Test module that sends "By Aporia.cc" in chat every 5 seconds.
 */
public class TestChat extends Module {
    private long lastMessageTime = 0;
    private static final long MESSAGE_INTERVAL = 5000; // 5 seconds in milliseconds
    
    public TestChat() {
        super("TestChat", Category.MISC);
    }
    
    @Override
    public void onEnable() {
        lastMessageTime = System.currentTimeMillis();
    }
    
    @Override
    public void onDisable() {
        // Nothing to do
    }
    
    @Override
    public void onTick() {
        if (!isEnabled()) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime >= MESSAGE_INTERVAL) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("By Aporia.cc"), false);
            }
            lastMessageTime = currentTime;
        }
    }
}
