//package zenith.zov.client.modules.api.setting.impl;
//
//
//import lombok.Getter;
//import lombok.Setter;
//import net.minecraft.client.gui.screens.ChatScreen;
//import org.joml.Vector2f;
//@Getter
//@Setter
//public class DragSetting extends Setting {
//
//
//    private float x, y;
//
//    private float width, height;
//
//
//    public DragSetting(String name, float x, float y, float width, float height) {
//        super(name);
//        this.x = x;
//        this.y = y;
//        this.width = width;
//        this.height = height;
//
//    }
//
//    public void set(MatrixStack matrixStack, float x, float y) {
//        if(!( mc.screen instanceof ChatScreen)){
//            return;
//        }
//        Vector2f nerest  = sirius.getDragManager().getNearest(this.getName(), x, y);
//        SheetCode x0 = new SheetCode(nerest.x,0);
//        SheetCode y0 = new SheetCode(nerest.y,0);
//
//        Vector2f nerest2 = sirius.getDragManager().getNearest(this.getName(), x + width, y + height);
//        SheetCode x1 = new SheetCode(nerest2.x,-width);
//        SheetCode y1 = new SheetCode(nerest2.y,-height);
//        Vector2f nerest3 = sirius.getDragManager().getNearest(this.getName(), x + width / 2, y + height / 2);
//        SheetCode x2 = new SheetCode(nerest3.x,-width/2);
//        SheetCode y2 = new SheetCode(nerest3.y,-height/2);
//
//        this.x = x;
//        this.y = y;
//        SheetCode x3 = getValue(x0,x1,x2);
//        SheetCode y3 = getValue(y0,y1,y2);
//        renderXLine(matrixStack,x3);
//        renderYLine(matrixStack,y3);
//
//        update();
//
//    }
//    private SheetCode getValue(SheetCode value , SheetCode value2,SheetCode value3) {
//        if(value.pos!=-1){
//            return value;
//        }
//        if(value2.pos!=-1){
//            return value2;
//        }
//
//        return value3;
//
//    }
//
//
//    private void renderYLine(MatrixStack matrixStack, SheetCode nearest) {
//        if(nearest.pos==-1)return;
//        sirius.getDragManager().addRunnable(()-> {
//        RenderUtil.renderRect(matrixStack.last().pose(), 0, nearest.pos, window.getScreenWidth(), 2, -1, 0, 1);
//        });
//        float nexY = nearest.pos + nearest.offset;
//        if(!ImGui.isMouseDragging(0)){
//            this.y = nexY;
//        }
//    }
//    private void renderXLine(MatrixStack matrixStack, SheetCode nearest) {
//        if(nearest.pos==-1)return;
//        sirius.getDragManager().addRunnable(()-> {
//            RenderUtil.renderRect(matrixStack.last().pose(), nearest.pos, 0, 2, window.getScreenHeight(), -1, 0, 1);
//        });
//        float nexX = nearest.pos + nearest.offset;
//        if(!ImGui.isMouseDragging(0)){
//            this.x = nexX;
//        }
//    }
//    public void set(float x, float y) {
//        if(!( mc.screen instanceof ChatScreen)){
//            return;
//        }
//        this.x = x;
//        this.y = y;
//
//    }
//
//
//    public void update(){
//        if(this.x < 0){
//            this.x = 0;
//        }
//        if(this.y < 0){
//            this.y = 0;
//        }
//
//            float widthScreen = ModuleLinkContainer.hud.getScaledWidth();
//            float heightScreen = ModuleLinkContainer.hud.getScaledHeight();
//            if (this.x + width > widthScreen) {
//                this.x = widthScreen - width;
//            }
//            if (this.y + height > heightScreen) {
//                this.y = heightScreen - height;
//            }
//
//    }
//
//    @Getter
//    @Setter
//    class SheetCode{
//        private float pos;
//        private float offset;
//
//        public SheetCode(float pos, float offset) {
//            this.pos = pos;
//            this.offset = offset;
//        }
//    }
//}