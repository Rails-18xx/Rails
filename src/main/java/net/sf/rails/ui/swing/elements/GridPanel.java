package net.sf.rails.ui.swing.elements;

import javax.swing.JPanel;

import net.java.dev.designgridlayout.DesignGridLayout;
import net.java.dev.designgridlayout.IRowCreator;

/**
 * A GridPanel defines a panel that contains a gridlayout
 */

public class GridPanel {
    
    private final JPanel panel;
    private final DesignGridLayout layout;
    
    public GridPanel(JPanel panel) {
        this.panel = panel;
        this.layout = new DesignGridLayout(panel);
    }
    
    public IRowCreator row() {
        return layout.row();
    }
    
    public JPanel panel() {
        return panel;
    }
    
}
