package net.sf.rails.ui.swing.core;

/**
 * RowColId is used as an identifier for either row or column of a grid table
 */
public class RowColId {
    
    private final String id;
    private boolean visible;

    public RowColId(String id) {
        this.id = id;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
}
