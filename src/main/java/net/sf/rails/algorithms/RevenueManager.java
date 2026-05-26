package net.sf.rails.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.game.state.ArrayListState;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Coordinates and stores all elements related to revenue calculation,
 * which are permanent.
 * The conversion of Rails elements is in the responsibility of the RevenueAdapter.
 * For each GameManager instance only one RevenueManager is created.
 */
public class RevenueManager extends RailsManager implements Configurable {

    protected int specialRevenue;
    protected RailsRoot root;
    protected PhaseManager phaseManager;

    private static final Logger log = LoggerFactory.getLogger(RevenueManager.class);

    // Modifiers that are configurable
    private final HashSet<Configurable> configurableModifiers = new HashSet<>();

    // Variables to store modifiers (permanent)
    private final ArrayListState<NetworkGraphModifier> graphModifiers = new ArrayListState<>(this, "graphModifiers");
    private final ArrayListState<RevenueStaticModifier> staticModifiers = new ArrayListState<>(this, "staticModifiers");
    private final ArrayListState<RevenueDynamicModifier> dynamicModifiers = new ArrayListState<>(this, "dynamicModifiers");
    private RevenueCalculatorModifier calculatorModifier;

    // Variables that store the active modifier (per RevenueAdapter)
    private final ArrayList<RevenueStaticModifier> activeStaticModifiers = new ArrayList<>();
    private final ArrayList<RevenueDynamicModifier> activeDynamicModifiers = new ArrayList<>();
    // TODO: Still add that flag if the calculator is active
//    private boolean activeCalculator;

    /**
     * Used by Configure (via reflection) only
     */
    public RevenueManager(RailsRoot parent, String id) {
        super(parent, id);
        this.root = parent;
        this.phaseManager = root.getPhaseManager();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        // define modifiers
        List<Tag> modifierTags = tag.getChildren("Modifier");

        if (modifierTags != null) {
            for (Tag modifierTag : modifierTags) {
                // get classname
                String className = modifierTag.getAttributeAsString("class");
                if (className == null) {
                    throw new ConfigurationException(LocalText.getText("ComponentHasNoClass", "Modifier"));
                }
                // create modifier
                Object modifier;
                try {
                    modifier = Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException(LocalText.getText("ClassCannotBeInstantiated", className), e);
                }
                boolean isModifier = false;
                // add them to the revenueManager
                if (modifier instanceof NetworkGraphModifier) {
                    NetworkGraphModifier ngm = (NetworkGraphModifier) modifier;
                    ngm.setRoot(getRoot());
                    graphModifiers.add(ngm);
                    isModifier = true;
                    log.debug("Added as graph modifier = {}", className);
                }
                if (modifier instanceof RevenueStaticModifier) {
                    staticModifiers.add((RevenueStaticModifier) modifier);
                    isModifier = true;
                    log.debug("Added as static modifier = {}", className);
                }
                if (modifier instanceof RevenueDynamicModifier) {
                    dynamicModifiers.add((RevenueDynamicModifier) modifier);
                    isModifier = true;
                    log.debug("Added as dynamic modifier = {}", className);
                }
                if (modifier instanceof RevenueCalculatorModifier) {
                    if (calculatorModifier != null) {
                        throw new ConfigurationException(LocalText.getText(
                                "MoreThanOneCalculatorModifier", className));
                    }
                    calculatorModifier = (RevenueCalculatorModifier) modifier;
                    isModifier = true;
                    log.debug("Added as calculator modifier = {}", className);
                }
                if (!isModifier) {
                    throw new ConfigurationException(LocalText.getText(
                            "ClassIsNotAModifier", className));
                }
                if (isModifier && modifier instanceof Configurable) {
                    configurableModifiers.add((Configurable) modifier);
                }
            }
        }

    }

    public void finishConfiguration(RailsRoot parent)
            throws ConfigurationException {
        for (Configurable modifier : configurableModifiers) {
            modifier.finishConfiguration(parent);
        }
    }

    public void addStaticModifier(RevenueStaticModifier modifier) {
        staticModifiers.add(modifier);
        log.debug("Revenue Manager: Added static modifier {}", modifier);
    }

    public boolean removeStaticModifier(RevenueStaticModifier modifier) {
        boolean result = staticModifiers.remove(modifier);
        if (result) {
            log.debug("RevenueManager: Removed static modifier {}", modifier);
        } else {
            log.debug("RevenueManager: Cannot remove{}", modifier);
        }
        return result;
    }

    public void addGraphModifier(NetworkGraphModifier modifier) {
        graphModifiers.add(modifier);
        log.debug("Revenue Manager: Added graph modifier {}", modifier);
    }

    public boolean removeGraphModifier(NetworkGraphModifier modifier) {
        boolean result = graphModifiers.remove(modifier);
        if (result) {
            log.debug("RevenueManager: Removed graph modifier {}", modifier);
        } else {
            log.debug("RevenueManager: Cannot remove{}", modifier);
        }
        return result;
    }

    public void addDynamicModifier(RevenueDynamicModifier modifier) {
        dynamicModifiers.add(modifier);
        log.debug("Revenue Manager: Added dynamic modifier {}", modifier);
    }

    public boolean removeDynamicModifier(RevenueDynamicModifier modifier) {
        boolean result = dynamicModifiers.remove(modifier);
        if (result) {
            log.debug("RevenueManager: Removed dynamic modifier {}", modifier);
        } else {
            log.debug("RevenueManager: Cannot remove{}", modifier);
        }
        return result;
    }

    void activateMapGraphModifiers(NetworkGraph graph) {
        for (NetworkGraphModifier modifier : graphModifiers.view()) {
            modifier.modifyMapGraph(graph);
        }
    }

    void activateRouteGraphModifiers(NetworkGraph graph, PublicCompany company) {
        for (NetworkGraphModifier modifier : graphModifiers.view()) {
            modifier.modifyRouteGraph(graph, company);
        }
    }


    void initStaticModifiers(RevenueAdapter revenueAdapter) {
        activeStaticModifiers.clear();
        for (RevenueStaticModifier modifier : staticModifiers.view()) {
            if (modifier.modifyCalculator(revenueAdapter)) {
                activeStaticModifiers.add(modifier);
            }
        }
    }

    /**
     * @param revenueAdapter
     * @return true if there are active dynamic modifiers
     */
    boolean initDynamicModifiers(RevenueAdapter revenueAdapter) {
        activeDynamicModifiers.clear();
        for (RevenueDynamicModifier modifier : dynamicModifiers.view()) {
            if (modifier.prepareModifier(revenueAdapter)) {
                activeDynamicModifiers.add(modifier);
                log.debug("Modifier {} activated", modifier.getClass().getSimpleName());
            } else {
                log.debug("Modifier {} deactivated", modifier.getClass().getSimpleName());
            }
        }
        return !activeDynamicModifiers.isEmpty();
    }

    /**
     * @param revenueAdapter
     * @return revenue from active calculator
     */
    // FIXME: This does not fully cover all cases that needs the revenue from the calculator
    // EV: indeed, it is used in a different way in 1837, so beware!
    // See RunToCoalMineModifier.
    int revenueFromDynamicCalculator(RevenueAdapter revenueAdapter) {
        return calculatorModifier.calculateRevenue(revenueAdapter);

    }

    /**
     * Allows dynamic modifiers to adjust the optimal run
     *
     * @param optimalRun
     */
    void adjustOptimalRun(List<RevenueTrainRun> optimalRun) {
        // allow dynamic modifiers to change the optimal run
        for (RevenueDynamicModifier modifier : activeDynamicModifiers) {
            modifier.adjustOptimalRun(optimalRun);
        }
    }

    /**
     * @param run     the current run
     * @param optimal flag if this is the found optimal run
     * @return total value of dynamic modifiers
     */
    /* TODO Replace return value (throughout) with new Revenue object,
       that includes special revenue such as direct treasury income.
     */
    int evaluationValue(List<RevenueTrainRun> run, boolean optimal) {
        // this allows dynamic modifiers to change the optimal run
        // however this is forbidden outside the optimal run!
        int value = 0;
        // To prevent "concurrent modification" exceptions, make a copy first
        log.debug("Dynamic modifiers: {}", activeDynamicModifiers);
        ArrayList<RevenueDynamicModifier> adm = (ArrayList<RevenueDynamicModifier>) activeDynamicModifiers.clone();
        for (RevenueDynamicModifier modifier : adm) {
            value += modifier.evaluationValue(run, optimal);
            log.debug("Modifier {} evaluation cumulative value: {}", modifier, value);
        }
        if (calculatorModifier != null) {
            specialRevenue = calculatorModifier.getSpecialRevenue();
        }
        log.debug("Revenue: total={} special={}",value, specialRevenue);
        return value;
    }

    public int getSpecialRevenue () {
        return specialRevenue;
    }

    /**
     * @return total prediction value of dynamic modifiers
     */
    int predictionValue(List<RevenueTrainRun> run) {
        // do not change the optimal run!
        int value = 0;
        for (RevenueDynamicModifier modifier : activeDynamicModifiers) {
            value += modifier.predictionValue(run);
        }
        return value;
    }

    /**
     * @param revenueAdapter
     * @return pretty print output from all modifiers (both static and dynamic)
     */
    protected String prettyPrint(RevenueAdapter revenueAdapter) {
        StringBuilder prettyPrint = new StringBuilder();

        for (RevenueStaticModifier modifier : activeStaticModifiers) {
            String modifierText = modifier.prettyPrint(revenueAdapter);
            if (modifierText != null) {
                prettyPrint.append(modifierText).append("\n");
            }
        }

        for (RevenueDynamicModifier modifier : activeDynamicModifiers) {
            String modifierText = modifier.prettyPrint(revenueAdapter);
            if (modifierText != null) {
                prettyPrint.append(modifierText).append("\n");
            }
        }

        return prettyPrint.toString();
    }

    /*---------------------------
     * The below new section of RevenueManager provides a generic interface
     * to obtain the actual revenue values of stops of any kind.
     * Game-specific subclasses can provide special cases.
     *
     * These methods are used by
     * - NetworkVertex, to initialise stop values per train
     *   in getValueByTrain().
     * - Dynamic modifiers (currently 18VA only, TBD).
     *
     * The methods getBaseRevenue() and getExtraRevenue() can be
     * overridden in subclasses of RevenueManager (which is no longer final).
     * This allows to specify the actual revenue value of stops
     * per stop type, train type en company details, as needed.
     *
     * Method getExtraRevenue() was intended to return any extra values
     * that should be returned by predictionValue() in dynamic modifiers.
     * However, the flexibility of getBaseRevenue() should allow
     * getExtraRevenue to return zero at all times.
     *
     * Revenues are returned as objects of the new Revenue class,
     * which can carry both normal and special revenue values.
     * Special values include the direct-to-treasury amounts
     * of 1837 and 18VA (revenue from mines).
     *
     * Created 04/2023 by Erik Vos
     */


     /* 'Actual revenue' is the final value of a stop for a given
     * train and company. This is the value with which NetworkVertex
     * objects can b e initialized.
     *
     * @param stop The stop for which a revenue value is requested
     * @param train A specific train that may affect the revenue,
     * or null if the train does not matter
     * @param company A specific company that may affect the revenue,
     * or null if the company does not matter
     * @return A new Revenue object
     */
    public final Revenue getActualRevenue(Stop stop, Train train, PublicCompany company) {
        return getBaseRevenue (stop, train, company)
                .addRevenue (getExtraRevenue(stop, train, company));
    }

    public final int getActualAsInteger (Stop stop, Train train, PublicCompany company) {
        Revenue rev = getActualRevenue(stop, train, company);
        return rev.getNormalRevenue() + rev.getSpecialRevenue();
    }

    /** Same as getRevenue(), but intended to get
     * <i>additional</i> revenue vales, as are needed for
     * the predictionValue() methods in dynamic modifiers.
     *
     * This method may not be really needed anymore, as getActualRevenue
     * now provides the final value per NetworkVertex, rather than a loosely
     * predicted one.
     */
    public Revenue getExtraRevenue (Stop stop, Train train, PublicCompany company) {

        return new Revenue (0, 0);
    }

    public final int getExtraAsInteger (Stop stop, Train train, PublicCompany company) {
        Revenue rev = getExtraRevenue(stop, train, company);
        return rev.getNormalRevenue() + rev.getSpecialRevenue();
    }

    /** Return the revenue insofar it could be configured.
     * This probably represents the former initial NetworkVertex revenue,
     * and it still is the default for the new actual revenue.
     *
     * @param stop
     * @param train
     * @param company
     * @return
     */
    protected Revenue getBaseRevenue (Stop stop, Train train, PublicCompany company) {
        return new Revenue (stop.getValueForPhase(phaseManager.getCurrentPhase()),0);
    }
}
