package net.sf.rails.game;

import java.util.List;
import java.util.Set;

import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueBonus;
import net.sf.rails.algorithms.RevenueStaticModifier;



/**
 * An object of class Bonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>Currently, Bonus objects will be created in the following cases:
 * <br>1. when a SpecialBaseTokenLay containing a BonusToken
 * is exercised,
 * <br>2. when a private having a LocatedBonus special property is bought by
 * a public company,
 * <br>3. when a sellable bonus is bought from such a public company by another company.
 * @author VosE
 *
 */
public class Bonus implements Closeable, RevenueStaticModifier {

    private PublicCompany owner;
    private List<MapHex> locations = null;
    private String name;
    private int value;
    // TODO: What was the intention of those?
/*    private String removingObjectDesc = null;
    private Object removingObject = null;
*/
    
    public Bonus (PublicCompany owner,
            String name, int value, List<MapHex> locations) {
        this.owner = owner;
        this.name = name;
        this.value = value;
        this.locations = locations;
    
        // add them to the call list of the RevenueManager
        RailsRoot.getInstance().getRevenueManager().addStaticModifier(this);

    }
    public boolean isExecutionable() {
        return false;
    }

    public PublicCompany getOwner() {
        return owner;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getIdForView() {

        if (locations == null || locations.isEmpty()) {
            return name.substring(0, 2);
        }

        StringBuffer b = new StringBuffer();
        for (MapHex location : locations) {
            if (b.length() > 0) b.append(",");
            b.append(location.getId());
        }
        return b.toString();
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    /**
     * Remove the bonus
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */
    public void close() {
        RailsRoot.getInstance().getRevenueManager().removeStaticModifier(this);
    }

    
    public boolean equals (Bonus b) {
        return (b.name.equals(name))
               && b.value == value;
    }

    @Override
    public String toString() {
        return "Bonus "+name+" hex="
               + getIdForView() + " value=" + value;
    }

    public String getClosingInfo() {
        return toString();
    }

    /**
     * Add bonus value to revenue calculator
     */
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // 1. check operating company
        if (owner != revenueAdapter.getCompany()) return false;
        
        // 2. find vertices to hex
        boolean found = false;
        Set<NetworkVertex> bonusVertices = NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), locations);
        for (NetworkVertex bonusVertex:bonusVertices) {
            if (!bonusVertex.isStation()) continue;
            RevenueBonus bonus = new RevenueBonus(value, name);
            bonus.addVertex(bonusVertex);
            revenueAdapter.addRevenueBonus(bonus);
            found = true;
        }
        return found;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        if (name == null) return null;
        return "Bonus active = " + name;
    }
    
}
