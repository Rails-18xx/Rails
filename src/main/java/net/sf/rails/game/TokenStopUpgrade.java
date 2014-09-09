package net.sf.rails.game;

import java.util.Comparator;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import rails.game.action.LayBaseToken;
import rails.game.action.LayToken;

public class TokenStopUpgrade extends MapUpgrade {

    // static fields
    private final Stop stop;
    private final LayToken action;
    
    private TokenStopUpgrade(Stop stop, LayToken action) {
        this.stop = stop;
        this.action = action;
    }
    
    public static TokenStopUpgrade create(Stop stop, LayToken action) {
        return new TokenStopUpgrade(stop, action);
    }

    public LayToken getAction() {
        return action;
    }

    // MapUpgrade abstract methods
    @Override
    public boolean isValid() {
        return true;
    }
    
    @Override
    public Stop getLocation() {
        return stop;
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
    public Comparator<MapUpgrade> getComparator () {
        return new Comparator<MapUpgrade>() {
            
            @Override
            public int compare(MapUpgrade u1, MapUpgrade u2) {
                if (u1 instanceof TokenStopUpgrade && u2 instanceof TokenStopUpgrade) {
                    TokenStopUpgrade ts1 = (TokenStopUpgrade) u1;
                    TokenStopUpgrade ts2 = (TokenStopUpgrade) u2;

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
                            .compare(ts1.stop.getRelatedStation().getValue(), ts2.stop.getRelatedStation().getValue())
                            .compare(ts2.stop.getTokenSlotsLeft(), ts1.stop.getTokenSlotsLeft())
                            .compare(ts1.stop.getRelatedNumber(), ts2.stop.getRelatedNumber())
                            .result();
                }
                return 0;
            }
        };
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("stop", stop)
                .add("action", action)
                .toString();
    }
    

    
}
