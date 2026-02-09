package zenith.zov.client.hud.elements.component;

import net.minecraft.item.ItemStack;
import zenith.zov.Zenith;
import zenith.zov.base.animations.base.Animation;
import zenith.zov.base.animations.base.Easing;
import zenith.zov.base.font.Font;
import zenith.zov.base.font.Fonts;
import zenith.zov.base.theme.Theme;
import zenith.zov.client.hud.elements.draggable.DraggableHudElement;
import zenith.zov.utility.mixin.accessors.DrawContextAccessor;
import zenith.zov.utility.render.display.base.BorderRadius;
import zenith.zov.utility.render.display.base.CustomDrawContext;
import zenith.zov.utility.render.display.base.color.ColorRGBA;
import zenith.zov.utility.render.display.shader.DrawUtil;

public class InventoryComponent extends DraggableHudElement {

    private final Animation toggleAnimation = new Animation(300, Easing.SINE_IN_OUT);
    private final Animation inventoryChangeAnimation = new Animation(150, Easing.SINE_IN_OUT);
    private String lastInventoryHash = "";
    private float lastWidth = 0f;
    private float lastHeight = 0f;

    public InventoryComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, Align align) {
        super(name,initialX, initialY,windowWidth,windowHeight,offsetX,offsetY,align);

    }

    @Override
    public void render(CustomDrawContext ctx) {
        if (mc.player == null) {
            toggleAnimation.update(0);
            if (toggleAnimation.getValue() > 0.01f) {
                renderInventory(ctx, toggleAnimation.getValue());
            }
            return;
        }

        toggleAnimation.update(1);
        if (toggleAnimation.getValue() <= 0.01f) return;

        String currentInventoryHash = "";
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            currentInventoryHash += stack.getItem().toString() + stack.getCount();
        }
        
        if (!currentInventoryHash.equals(lastInventoryHash)) {
            inventoryChangeAnimation.update(0);
            lastInventoryHash = currentInventoryHash;
        }
        inventoryChangeAnimation.update(1);

        renderInventory(ctx, toggleAnimation.getValue() * inventoryChangeAnimation.getValue());
    }

    private void renderInventory(CustomDrawContext ctx, float animationValue) {
        if (mc.player == null) {
            Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();
            ColorRGBA bgColor = theme.getForegroundColor();
            
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(x + lastWidth / 2f, y + lastHeight / 2f);
            ctx.getMatrices().scale(animationValue, animationValue);
            ctx.getMatrices().translate(-(x + lastWidth / 2f), -(y + lastHeight / 2f));
            
            ctx.drawRoundedRect(x, y, lastWidth, lastHeight, BorderRadius.all(4f), bgColor);
            
            ctx.getMatrices().popMatrix();
            return;
        }

        Font countFont = Fonts.MEDIUM.getFont(6);
        float slotSize = 20;
        float borderRadius = 4f;
        Theme theme = Zenith.getInstance().getThemeManager().getCurrentTheme();


        ColorRGBA graySlotColor = theme.getForegroundColor();
        ColorRGBA themeSlotColor = theme.getForegroundLight();

        int columns = 9;
        int rows = 3;
        float gridWidth = columns * slotSize;
        float gridHeight = rows * slotSize;

        this.width = gridWidth;
        this.height = gridHeight;
        
        lastWidth = width;
        lastHeight = height;

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x + width / 2f, y + height / 2f);
        ctx.getMatrices().scale(animationValue, animationValue);
        ctx.getMatrices().translate(-(x + width / 2f), -(y + height / 2f));
        DrawUtil.drawBlurHud(ctx.getMatrices(),x, y, width,height,21,BorderRadius.all(4),ColorRGBA.WHITE);

        //    ctx.drawRoundedRect(x,y,width,height, BorderRadius.all(4f), theme.getForegroundStroke());
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                int slotIndex = 9 + row * 9 + col;
                ItemStack stack = mc.player.getInventory().getStack(slotIndex);
                
                float slotX = (x + col * slotSize);
                float slotY =(y + row * slotSize);
                
                ColorRGBA slotColor = ((row + col) % 2 == 0) ? graySlotColor : themeSlotColor;
                float round = 4f;
                BorderRadius radius = col==0&&row==0?BorderRadius.top(round,0):col==8&&row==0?BorderRadius.top(0,round):col==0&&row==2?BorderRadius.bottom(round,0):col==8&&row==2?BorderRadius.bottom(0,round):BorderRadius.ZERO;
                ctx.drawRoundedRect(slotX, slotY, slotSize, slotSize,radius, slotColor);
                
                if (!stack.isEmpty()) {
                    ctx.pushMatrix();
                    ctx.getMatrices().translate(slotX + (slotSize - 12.8f) / 2f, slotY + (slotSize - 12.8f) / 2f);
                    ctx.getMatrices().scale(0.8f, 0.8f);

                    ctx.drawItem(stack, 0,0);

                    ((DrawContextAccessor) ctx).callDrawItemBar(stack,0,0);
                    ((DrawContextAccessor) ctx).callDrawCooldownProgress(stack,0,0);
                    ctx.popMatrix();

                }
            }
        }
      //  ctx.drawRoundedBorder(x, y, gridWidth, gridHeight,0.1f, BorderRadius.all(4f), theme.getForegroundStroke());
        ctx.drawRoundedBorder(x, y,gridWidth,gridHeight,0.1f,BorderRadius.all(4),theme.getForegroundStroke());

        DrawUtil.drawRoundedCorner(ctx.getMatrices(), x, y,gridWidth,gridHeight,0.1f,20f,theme.getColor(),BorderRadius.all(4));

        ctx.getMatrices().popMatrix();
    }



}


