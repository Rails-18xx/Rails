package net.sf.rails.ui.swing.hexmap;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.NavigableSet;
import java.util.Set;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Stop;

import com.google.common.base.Objects;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import rails.game.action.LayBaseToken;
import rails.game.action.LayToken;

public class TokenHexUpgrade extends HexUpgrade {

    public enum Invalids implements HexUpgrade.Invalids {
        HEX_BLOCKED, HEX_RESERVED;

        @Override
        public String toString() {
            return LocalText.getText("TOKEN_" + this.name());
        }
        
    }

    // static fields
    private final LayToken action;
    private final ImmutableSet<Stop> stops;

    // validation fields
    private final NavigableSet<Stop> allowed = Sets.newTreeSet();
    private final EnumSet<Invalids> invalids = EnumSet.noneOf(Invalids.class);

    // ui fields
    private Stop selectedStop;
    
    private TokenHexUpgrade(GUIHex hex, Collection<Stop> stops, LayToken action) {
        super(hex);
        this.action = action;
        this.stops = ImmutableSet.copyOf(stops);
    }
    
    public static TokenHexUpgrade create(GUIHex hex, Collection<Stop> stops, LayToken action) {
        return new TokenHexUpgrade(hex, stops, action);
    }

    public LayToken getAction() {
        return action;
    }
    
    public Set<Stop> getStops() {
        return stops;
    }
    
    public Stop getSelectedStop() {
        return selectedStop;
    }
    
    private boolean validate() {
        invalids.clear();
        allowed.addAll(stops);
        
        // laying home hex is always allowed
        if (!hex.getHex().isHomeFor(action.getCompany())) {
            if (hexBlocked()) {
                invalids.add(Invalids.HEX_BLOCKED);
            }
            if (hexReserved()) {
                invalids.add(Invalids.HEX_RESERVED);
            }
        }

        selectedStop = allowed.first();
        
        return invalids.isEmpty();
    }
    
    public boolean hexBlocked() {
        return hex.getHex().getBlockedForTokenLays() == MapHex.BlockedToken.ALWAYS;
    }
    
    public boolean hexReserved() {
        for (Stop stop:stops) {
            if (hex.getHex().isBlockedForReservedHomes(stop)) {
                allowed.remove(stop);
            }
        }
        return allowed.isEmpty();
    }

    // HexUpgrade abstract methods
    
    @Override
    public boolean hasSingleSelection() {
        return allowed.size() == 1;
    }

    @Override
    public void firstSelection() {
        selectedStop = allowed.first();
    }
    
    @Override
    public void nextSelection() {
        Stop next = allowed.higher(selectedStop);
        if (next == null) {
            selectedStop =  allowed.first();
        } else {
            selectedStop = next;
        }
    }

    @Override
    public Set<HexUpgrade.Invalids> getInvalids() {
        return ImmutableSet.<HexUpgrade.Invalids>copyOf(invalids);
    }
    
    @Override
    public boolean isValid() {
        return invalids.isEmpty();
    }

    @Override
    public String getToolTip() {
        // TODO: Add text
        return LocalText.getText("TokenHexUpgrade.ToolTip");
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
                            .result();
                }
                return 0;
            }
        };
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("stops", stops)
                .add("action", action)
                .toString();
    }

    /**
     * sets both validation and visibility for upgrades
     */
    public static void validates(TokenHexUpgrade upgrade) {
        if (upgrade.validate()) {
            upgrade.setVisible(true);
        }
    }
}
