package ru.module.impl.visuals;

import net.minecraft.client.Minecraft;
import ru.module.Module;
import ru.ui.clickgui.ClickGuiScreen;

public class ClickGui extends Module {
    
    public ClickGui() {
        super("ClickGui", "Гуи чита", C.VISUALS, 96);
    }
    
    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
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
