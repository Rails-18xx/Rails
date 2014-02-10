package net.sf.rails.game;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;

import rails.game.action.LayBaseToken;
import rails.game.action.LayToken;

public class TokenStopUpgrade extends MapUpgrade implements Comparable<TokenStopUpgrade> {

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

    // Location class methods
    @Override
    public Stop getLocation() {
        return stop;
    }
    
    /**
     * Sorting is based on the following: First special Tokens, then type id of action (see there),
     * followed by valuable stations, with less token slots left
     */
    public int compareTo(TokenStopUpgrade other) {
        Station thisStation = this.stop.getRelatedStation();
        Station otherStation = other.stop.getRelatedStation();
        
        boolean thisBase = this.action instanceof LayBaseToken;
        boolean otherBase = other.action instanceof LayBaseToken;
        
        int thisType = 0, otherType = 0;
        if (thisBase) {
            thisType = ((LayBaseToken)this.action).getType();
        }
        if (otherBase) {
            otherType = ((LayBaseToken)other.action).getType();
        }
        
        return ComparisonChain.start()
                .compare(thisBase, otherBase)
                .compare(otherType, thisType)
                .compare(thisStation.getValue(), otherStation.getValue())
                .compare(other.stop.getTokenSlotsLeft(), this.stop.getTokenSlotsLeft())
                .result();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("stop", stop)
                .add("action", action)
                .toString();
    }
    

    
}
