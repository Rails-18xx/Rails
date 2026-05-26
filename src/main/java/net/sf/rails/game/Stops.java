package net.sf.rails.game;

import java.util.HashMap;
import java.util.Map;

/**
 * Stops is a class intended to provide a central place
 * for calculation of revenues per stop, possibly dependent
 * on phase, tokening and train type.
 *
 * It was created out of frustration with the existing train run
 * calculation code, that made handling train run revenues in 18VA
 * a matter of building a very complex dynamic modifier.
 *
 * Another factor was the mine revenue in 1837 and 18VA, which
 * wholly or partly went directly to the company treasury,
 * completely separate from normal revenue. This has led to
 * add another class called Revenue, which can transport both
 * normal and special revenues, and is extendable.
 *
 * This class is a default version, that returns the fixed
 * stop values that are sufficient in most games, an exception
 * being the phase-dependent offmap revenues.
 *
 * Game-specific subclasses can be created to handle the kind
 * of special cases that are so abundant in 18VA.
 *
 * The main usage of the methods of this class is expected to be
 * by NetworkVertex and dynamic modifier objects.
 *
 * The results can be obtained in two ways:
 * - as TOTAL REVENUE, which is the total revenue of a stop;
 * - as EXTRA REVENUE, which is the correction to the standard
 * revenue that is configured for tiles and offmap hexes.
 * That correction (default zero) must be returned by the method
 * predictionValue() of any dynamic modifier.
 *
 * Created 04/2023 by Erik Vos
 */
public class Stops {

    protected GameManager gameManager;
    protected PhaseManager phaseManager;
    protected MapManager mapManager;

    protected static Map<RailsRoot, Stops> instances = new HashMap<>();

    protected Stops (RailsRoot root) {
        gameManager = root.getGameManager();
        phaseManager = root.getPhaseManager();
        mapManager = root.getMapManager();
    }

    protected static Stops getInstance (RailsRoot root) {
        if (!instances.containsKey(root)) {
            instances.put(root, new Stops(root));
        }
        return instances.get(root);
    }

    public static Revenue getValue(Stop stop) {
        return getInstance (stop.getRoot()).getRevenue(stop, null, null);
    }

    public static Revenue getValueForTrain(Stop stop, Train train) {
        return getInstance (stop.getRoot()).getRevenue(stop, train, null);
    }

    public static Revenue getValueForTrainAndCompany (Stop stop, Train train, PublicCompany company) {
        return getInstance (stop.getRoot()).getRevenue(stop, train, company);
    }

    public static Revenue getExtraValue(Stop stop) {
        return getInstance (stop.getRoot()).getExtraRevenue(stop, null, null);
    }

    public static Revenue getExtraValueForTrain(Stop stop, Train train) {
        return getInstance (stop.getRoot()).getExtraRevenue(stop, train, null);
    }

    public static Revenue getExtraValueForTrainAndCompany (Stop stop, Train train, PublicCompany company) {
        return getInstance (stop.getRoot()).getExtraRevenue(stop, train, company);
    }

    public static int getTotalRevenue (Stop stop, Train train, PublicCompany company) {
        Revenue revenue = getValueForTrainAndCompany (stop, train, company);
        return revenue.getNormalRevenue() + revenue.getSpecialRevenue();
    }

    public static int getTotalExtraRevenue (Stop stop, Train train, PublicCompany company) {
        Revenue revenue = getExtraValueForTrainAndCompany (stop, train, company);
        return revenue.getNormalRevenue() + revenue.getSpecialRevenue();
    }


    /* This default method can be overridden for any game
     * in a game-specific subclass
     *
     * @param stop The stop for which a revenue value is requested
     * @param train A specific train that may affect the revenue,
     * or null if the train does not matter
     * @param company A specific company that may affect the revenue,
     * or null if the company does not matter
     * @return A new Revenue object
     */
    protected Revenue getRevenue (Stop stop, Train train, PublicCompany company) {
         //if (stop.getType() == Stop.Type.OFFMAP) {
            return new Revenue (
                    stop.getValueForPhase(phaseManager.getCurrentPhase()),
                    0);
        //} else {
        //    return new Revenue (stop.getValue(),
        //            0);
        //}

    }

    /** Same as getRevenue(), but intended to get
     * <i>additional</i> revenue vales, as are needed for
     * the predictionValue() methods in dynamic modifiers.
     */
    protected Revenue getExtraRevenue (Stop stop, Train train, PublicCompany company) {
        return new Revenue (0, 0);
    }

}
