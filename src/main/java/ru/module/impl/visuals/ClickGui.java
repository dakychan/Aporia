package ru.module.impl.visuals;

import net.minecraft.client.Minecraft;
import ru.module.Module;
import ru.ui.clickgui.ClickGuiScreen;

public class ClickGui extends Module {
    
    /**
     * Text-only mode setting.
     */
    private final BooleanSetting textOnlyMode;
    
    public ClickGui() {
        super("ClickGui", "Гуи чита", C.VISUALS, 96);
        
        textOnlyMode = new BooleanSetting("Text Only", false);
        addSetting(textOnlyMode);
    }
    
    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        ClickGuiScreen screen = new ClickGuiScreen(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        screen.setTextOnlyMode(textOnlyMode.getValue());
        mc.setScreen(screen);
        setEnabled(false);
    }
    
    @Override
    public void onDisable() {
    }
    
    @Override
    public void onTick() {
        /**
         * Update ClickGui with text-only mode setting.
         */
        if (Minecraft.getInstance().screen instanceof ClickGuiScreen) {
            ClickGuiScreen screen = (ClickGuiScreen) Minecraft.getInstance().screen;
            screen.setTextOnlyMode(textOnlyMode.getValue());
        }
    }
}
