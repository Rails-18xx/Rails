package net.sf.rails.ui.swing.core;

import net.sf.rails.game.state.Item;

/**
 * TableCoordinate is used as an identifier is used to index column or rows in a grid table
 */
public class TableCoordinate {
    
    private final String id;
    private boolean visible;

    private TableCoordinate(String id) {
        this.id = id;
        this.visible = true;
    }
    
    public String getId() {
        return id;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public TableCoordinate setVisible(boolean visible) {
        this.visible = visible;
        return this;
    }
    
    public TableCoordinate show() {
        return setVisible(true);
    }
    
    public TableCoordinate hide() {
        return setVisible(false);
    }
    
    public static TableCoordinate from(Object column) {
        return new TableCoordinate(column.toString());
    }
    
    public static TableCoordinate from(Item item) {
        return new TableCoordinate(item.getFullURI());
    }
    
}
