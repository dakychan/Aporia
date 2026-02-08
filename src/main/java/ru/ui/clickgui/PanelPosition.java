package ru.ui.clickgui;

public class PanelPosition {
    private String category;
    private int x;
    private int y;
    
    public PanelPosition() {
    }
    
    public PanelPosition(String category, int x, int y) {
        this.category = category;
        this.x = x;
        this.y = y;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getX() {
        return x;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getY() {
        return y;
    }
    
    public void setY(int y) {
        this.y = y;
    }
}
