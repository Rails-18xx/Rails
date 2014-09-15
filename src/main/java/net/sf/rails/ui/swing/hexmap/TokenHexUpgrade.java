package net.sf.rails.ui.swing.hexmap;

import java.util.Comparator;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import rails.game.action.LayBaseToken;
import rails.game.action.LayToken;

public class TokenHexUpgrade extends HexUpgrade {

    // static fields
    private final LayToken action;
    
    private TokenHexUpgrade(MapHex hex, LayToken action) {
        super(hex);
        this.action = action;
    }
    
    public static TokenHexUpgrade create(MapHex hex, LayToken action) {
        return new TokenHexUpgrade(hex, action);
    }

    public LayToken getAction() {
        return action;
    }
    
    // FIXME
    public Stop getSelectedStop() {
        return null;
    }

    // HexUpgrade abstract methods
    @Override
    public boolean isValid() {
        return true;
    }
    
    @Override
    public int getCompareId() {
        return 1;
    }
    
    /**
     * Sorting is based on the following: First special Tokens, then type id of action (see there),
     * followed by valuable stations, with less token slots left
     */
    @Override
    public Comparator<HexUpgrade> getComparator () {
        return new Comparator<HexUpgrade>() {
            
            @Override
            public int compare(HexUpgrade u1, HexUpgrade u2) {
                if (u1 instanceof TokenHexUpgrade && u2 instanceof TokenHexUpgrade) {
                    TokenHexUpgrade ts1 = (TokenHexUpgrade) u1;
                    TokenHexUpgrade ts2 = (TokenHexUpgrade) u2;

                    boolean base1 = ts1.action instanceof LayBaseToken;
                    boolean base2 = ts2.action instanceof LayBaseToken;

                    int type1 = 0, type2 = 0;
                    if (base1) {
                        type1 = ((LayBaseToken)ts1.action).getType();
                    }
                    if (base2) {
                        type2 = ((LayBaseToken)ts2.action).getType();
                    }
                    
                    return ComparisonChain.start()
                            .compare(base1, base2)
                            .compare(type2, type1)
//                            .compare(ts1.stop.getRelatedStation().getValue(), ts2.stop.getRelatedStation().getValue())
//                            .compare(ts2.stop.getTokenSlotsLeft(), ts1.stop.getTokenSlotsLeft())
//                            .compare(ts1.stop.getRelatedNumber(), ts2.stop.getRelatedNumber())
                            .result();
                }
                return 0;
            }
        };
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
//                .add("stop", stop)
                .add("action", action)
                .toString();
    }
    

    
}
