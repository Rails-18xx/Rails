package net.sf.rails.game;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.rails.algorithms.*;
import net.sf.rails.game.state.Observable;

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
 * Changed 8/2020 by EV:
 * This modifier can now also be dynamic in case the bonus is only given once per revenue run.
 * In that case, the bonus is not assigned to the city, but added at the final evaluation
 * provided that at least one train has reached that city. This applies to 18Scan Kiruna.
 */
public class Bonus implements Closeable, RevenueStaticModifier, RevenueDynamicModifier {

    private PublicCompany owner;
    private List<MapHex> locations;
    private String name;
    private int value;
    private boolean dynamic;

    private Set<NetworkVertex> bonusVertices;
    private NetworkVertex bonusVertex;

    public Bonus (PublicCompany owner,
            String name, int value, List<MapHex> locations) {
        this(owner, name, value, locations, false);
    }

    public Bonus (PublicCompany owner,
                  String name, int value, List<MapHex> locations, boolean onlyOnce) {
        this.owner = owner;
        this.name = name;
        this.value = value;
        this.locations = locations;
        this.dynamic = onlyOnce;

        // add them to the call list of the RevenueManager
        if (dynamic) {
            owner.getRoot().getRevenueManager().addDynamicModifier(this);
        } else {
            owner.getRoot().getRevenueManager().addStaticModifier(this);
        }
    }

    // Unused (and a misformed name too)
    //public boolean isExecutionable() {
    //     return false;
    //}

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

        StringBuilder b = new StringBuilder();
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
     * See prepareForRemove().
     */
    @Override
    public void close() {
        if (dynamic) {
            owner.getRoot().getRevenueManager().removeDynamicModifier(this);
        } else {
            owner.getRoot().getRevenueManager().removeStaticModifier(this);
        }
    }


    public boolean equals (Object obj) {
        if ( !(obj instanceof Bonus) ) {
            return false;
        }
        Bonus b = (Bonus) obj;
        return (b.name.equals(name)) && b.value == value;
    }

    @Override
    public String toString() {
        return "Bonus "+name+" hex=" + getIdForView() + " value=" + value;
    }

    @Override
    public String getClosingInfo() {
        return toString();
    }

    /**
     * Add bonus value to revenue calculator
     * Part of static modifier
     */
    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        if (dynamic) return false;

        // 1. check operating company
        if (owner != revenueAdapter.getCompany()) return false;

        // 2. find vertices to hex
        boolean found = false;
        bonusVertices = NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), locations);

        // 3. First add the bonus to any stations
        for (NetworkVertex bonusVertex:bonusVertices) {
            if (bonusVertex.isStation()) {
                RevenueBonus bonus = new RevenueBonus(value, name);
                bonus.addVertex(bonusVertex);
                revenueAdapter.addRevenueBonus(bonus);
                this.bonusVertex = bonusVertex;
                found = true;

            }
        }

        if (found) return true;

        // 4. OK, then it's plain track, e.g. a Ferry.
        // Assume for now that we always have simple track with only 2 exits.
        // Pick just one exit, it doesn't matter which one, then return.
        for (NetworkVertex bonusVertex:bonusVertices) {
            if (!bonusVertex.isStation()) {
                RevenueBonus bonus = new RevenueBonus(value, name);
                bonus.addVertex(bonusVertex);
                revenueAdapter.addRevenueBonus(bonus);
                this.bonusVertex = bonusVertex;
                found = true;
                break;  // Only one!
            }
        }
        return found;
    }

    /**
     * First of four methods incorporating the dynamic modifier
     * @param revenueAdapter The calling class
     * @return True if a bonus location was found in the route graph
     */
    public boolean prepareModifier(RevenueAdapter revenueAdapter) {
        if (!dynamic) return false;

        // 1. check operating company
        if (owner != revenueAdapter.getCompany()) return false;

        // 2. find vertices to hex
        boolean found = false;
        bonusVertices = NetworkVertex.getVerticesByHexes(revenueAdapter.getVertices(), locations);

        // 3. Find the station vertex to enable a later check for train arrival
        for (NetworkVertex bonusVertex:bonusVertices) {
            if (bonusVertex.isStation()) {
                this.bonusVertex = bonusVertex;
                found=true;
             }
        }
        return found;
    }

    @Override
    public int predictionValue(List<RevenueTrainRun> runs) {
        return value;
    }

    @Override
    public int evaluationValue(List<RevenueTrainRun> runs, boolean optimalRuns) {
        int hits = 0;
        if (runs.size() == 0) return 0;
        for (RevenueTrainRun run:runs) {
            if (run.getUniqueVertices().contains(bonusVertex)) {
                hits++;
            }
        }
        return hits > 0 ? value : 0;
    }

    @Override
    public void adjustOptimalRun(List<RevenueTrainRun> optimalRuns) {
        // do nothing here (all is done by changing the evaluation value)
    }


    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        if (name == null) return null;
        return "Bonus active = " + name;
    }

}
