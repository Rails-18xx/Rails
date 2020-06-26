package net.sf.rails.game.model;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Model;

import java.awt.*;

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
