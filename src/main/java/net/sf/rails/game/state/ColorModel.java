package net.sf.rails.game.state;

import java.awt.Color;

/**
 * ColourModel defines colors for UI components
 */

public abstract class ColorModel extends Model {

    protected ColorModel(Item parent, String id) {
        super(parent, id);
    }
    
    public abstract Color getBackground();
    
    public abstract Color getForeground();
    
}
