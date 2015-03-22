package net.sf.rails.game.model;

import java.awt.Color;

import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Item;

/**
 * Used to indicate if an player sold a share this round
 */
public class SoldThisRoundModel extends ColorModel {
    
    public static final Color SOLD_COLOR = Color.RED; 
    public static final int SOLD_ALPHA = 64;
    
    private final BooleanState state =  BooleanState.create(this, "state");

    private SoldThisRoundModel(Item parent, String id) {
        super(parent, id);
    }
    
    public static SoldThisRoundModel create(Player parent, PublicCompany company) {
        return new SoldThisRoundModel(parent, "SoldThisRoundModel_" + company.getId());
    }

    public boolean value() {
        return state.value();
    }
    
    public void set(boolean value) {
        state.set(value);
    }

    @Override
    public Color getBackground() {
        if (state.value()) {
            return new Color(SOLD_COLOR.getRed(), SOLD_COLOR.getGreen(), SOLD_COLOR.getBlue(), SOLD_ALPHA);
        } else {
            return null;
        }
    }

    @Override
    public Color getForeground() {
        return null;
    }
    
}
