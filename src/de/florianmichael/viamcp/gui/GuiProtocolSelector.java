package de.florianmichael.viamcp.gui;

import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import de.florianmichael.vialoadingbase.ViaLoadingBase;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class GuiProtocolSelector extends Screen {

    private final Screen parent;
    private VersionList list;

    public GuiProtocolSelector(Screen parent) {
        super(Component.literal("ViaMCP"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.list = new VersionList(this.minecraft, this.width, this.height, 32, 18);
        this.addWidget(this.list);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, p_ -> this.minecraft.setScreen(this.parent)).bounds(this.width / 2 - 100, this.height - 26, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        this.list.render(gfx, mouseX, mouseY, partialTick);
        gfx.drawCenteredString(this.font, "ViaMCP - Protocol Version", this.width / 2, 12, 0xFFFFFF);
        ProtocolVersion target = ViaLoadingBase.getInstance().getTargetVersion();
        gfx.drawCenteredString(this.font, "Current: " + target.getName() + " (" + target.getVersion() + ")", this.width / 2, 22, 0x55FF55);
        super.render(gfx, mouseX, mouseY, partialTick);
    }

    public static class VersionList extends ObjectSelectionList<VersionList.VersionEntry> {

        public VersionList(net.minecraft.client.Minecraft mc, int width, int height, int y0, int entryHeight) {
            super(mc, width, height, y0, entryHeight);
            List<ProtocolVersion> versions = new ArrayList<>(ViaLoadingBase.PROTOCOLS);
            for (ProtocolVersion v : versions) {
                VersionEntry entry = new VersionEntry(v);
                this.addEntry(entry);
                if (ViaLoadingBase.getInstance().getTargetVersion().equals(v)) {
                    this.setSelected(entry);
                }
            }
            if (this.getSelected() != null) {
                this.centerScrollOn(this.getSelected());
            }
        }

        @Override
        public int getRowWidth() {
            return super.getRowWidth() + 50;
        }

        public static class VersionEntry extends ObjectSelectionList.Entry<VersionEntry> {
            private final ProtocolVersion version;

            public VersionEntry(ProtocolVersion version) {
                this.version = version;
            }

            @Override
            public void renderContent(GuiGraphics gfx, int index, int y, boolean hovered, float partialTick) {
                boolean selected = ViaLoadingBase.getInstance().getTargetVersion().equals(this.version);
                int color = selected ? 0x55FF55 : (hovered ? 0xFFFF55 : 0xAAAAAA);
                String text = this.version.getName() + " (" + this.version.getVersion() + ")";
                gfx.drawString(net.minecraft.client.Minecraft.getInstance().font, text, 4, y, color);
            }

            @Override
            public Component getNarration() {
                return Component.literal(this.version.getName());
            }

            @Override
            public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
                ViaLoadingBase.getInstance().reload(this.version);
                return true;
            }
        }
    }
}
