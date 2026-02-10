package ru.module.impl.visuals;

import net.minecraft.client.MinecraftClient;
import ru.module.Module;
import ru.ui.clickgui.ClickGuiScreen;

public class ClickGui extends Module {
    
    public ClickGui() {
        super("ClickGui", "Гуи чита", C.VISUALS, 96); // GRAVE key
    }
    
    @Override
    public void onEnable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.setScreen(new ClickGuiScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight()));
        setEnabled(false);
    }
    
    @Override
    public void onDisable() {
    }
    
    @Override
    public void onTick() {
    }
}
