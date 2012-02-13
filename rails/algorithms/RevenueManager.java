package rails.algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;


import rails.common.LocalText;
import rails.common.parser.ConfigurableComponentI;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.GameManager;
import rails.game.state.AbstractItem;
import rails.game.state.ArrayListState;

/**
 * Coordinates and stores all elements related to revenue calulcation,
 * which are permanent.
 * The conversion of Rails elements is in the responsibility of the RevenueAdapter.
 * For each GameManager instance only one RevenueManager is created.
 * 
 * @author freystef
 *
 */

public final class RevenueManager extends AbstractItem implements ConfigurableComponentI {

    protected static Logger log =
        Logger.getLogger(RevenueManager.class.getPackage().getName());

    private final HashSet<ConfigurableComponentI> configurableModifiers;
    
    private final ArrayListState<NetworkGraphModifier> graphModifiers;
    private final ArrayListState<RevenueStaticModifier> staticModifiers;
    private final ArrayListState<RevenueDynamicModifier> dynamicModifiers;
    
    private final ArrayList<RevenueStaticModifier> activeStaticModifiers;
    private final ArrayList<RevenueDynamicModifier> activeDynamicModifiers;
    private RevenueDynamicModifier activeCalculator;

    public RevenueManager() {
        graphModifiers = ArrayListState.create(this, "NetworkGraphModifiers"); 
        staticModifiers = ArrayListState.create(this, "RevenueStaticModifiers"); 
        dynamicModifiers = ArrayListState.create(this, "RevenueDynamicModifiers");
        configurableModifiers = new HashSet<ConfigurableComponentI>();
        
        activeStaticModifiers = new ArrayList<RevenueStaticModifier>();
        activeDynamicModifiers = new ArrayList<RevenueDynamicModifier>();
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        // define modifiers
        List<Tag> modifierTags = tag.getChildren("Modifier");
        
        if (modifierTags != null) {
            for (Tag modifierTag:modifierTags) {
                // get classname
                String className = modifierTag.getAttributeAsString("class");
                if (className == null) {
                    throw new ConfigurationException(LocalText.getText(
                            "ComponentHasNoClass", "Modifier"));
                }
                // create modifier
                Object modifier;
                try {
                    modifier = Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException(LocalText.getText(
                            "ClassCannotBeInstantiated", className), e);
                }
                boolean isModifier = false;
                // add them to the revenueManager
                if (modifier instanceof NetworkGraphModifier) {
                    graphModifiers.add((NetworkGraphModifier)modifier);
                    isModifier = true;
                    log.info("Added as graph modifier = " + className);
                }
                if (modifier instanceof RevenueStaticModifier) {
                    staticModifiers.add((RevenueStaticModifier)modifier);
                    isModifier = true;
                    log.info("Added as static modifier = " + className);
                }
                if (modifier instanceof RevenueDynamicModifier) {
                    dynamicModifiers.add((RevenueDynamicModifier)modifier);
                    isModifier = true;
                    log.info("Added as dynamic modifier = " + className);
                }
                if (!isModifier) {
                    throw new ConfigurationException(LocalText.getText(
                            "ClassIsNotAModifier", className));
                }
                if (isModifier && modifier instanceof ConfigurableComponentI) {
                    configurableModifiers.add((ConfigurableComponentI)modifier);
                }
            }
        }

    }

    public void finishConfiguration(GameManager parent)
            throws ConfigurationException {
        for (ConfigurableComponentI modifier:configurableModifiers) {
                modifier.finishConfiguration(parent);
        }
    }
    
    public void addStaticModifier(RevenueStaticModifier modifier) {
        staticModifiers.add(modifier);
        log.info("Revenue Manager: Added static modifier " + modifier);
    }
    
    public boolean removeStaticModifier(RevenueStaticModifier modifier) {
        boolean result = staticModifiers.remove(modifier);
        if (result) {
            log.info("RevenueManager: Removed static modifier " + modifier);
        } else {
            log.info("RevenueManager: Cannot remove" + modifier);
        }
        return result;
    }

    public void addGraphModifier(NetworkGraphModifier modifier) {
        graphModifiers.add(modifier);
        log.info("Revenue Manager: Added graph modifier " + modifier);
    }
    
    public boolean removeGraphModifier(NetworkGraphModifier modifier) {
        boolean result = graphModifiers.remove(modifier);
        if (result) {
            log.info("RevenueManager: Removed graph modifier " + modifier);
        } else {
            log.info("RevenueManager: Cannot remove" + modifier);
        }
        return result;
    }

    public void addDynamicModifier(RevenueDynamicModifier modifier) {
        dynamicModifiers.add(modifier);
        log.info("Revenue Manager: Added dynamic modifier " + modifier);
    }
    
    public boolean removeDynamicModifier(RevenueDynamicModifier modifier) {
        boolean result = dynamicModifiers.remove(modifier);
        if (result) {
            log.info("RevenueManager: Removed dynamic modifier " + modifier);
        } else {
            log.info("RevenueManager: Cannot remove" + modifier);
        }
        return result;
    }

    void initGraphModifiers(NetworkGraphBuilder graphBuilder) {
        for (NetworkGraphModifier modifier:graphModifiers.view()) {
            modifier.modifyGraph(graphBuilder);
        }
    }
    
    void initStaticModifiers(RevenueAdapter revenueAdapter) {
        activeStaticModifiers.clear();
        for (RevenueStaticModifier modifier:staticModifiers.view()) {
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
        for (RevenueDynamicModifier modifier:dynamicModifiers.view()) {
            if (modifier.prepareModifier(revenueAdapter))
                activeDynamicModifiers.add(modifier);
        }
        return !activeDynamicModifiers.isEmpty();
    }
    
    /**
     * @return true if one of the dynamic modifiers is an calculator of its own
     */
    boolean hasDynamicCalculator() {
        for (RevenueDynamicModifier modifier:activeDynamicModifiers) {
            if (modifier.providesOwnCalculateRevenue()) {
                activeCalculator = modifier;
                return true;
            }
        }
        return false;
    }
    
    /**
     * @param revenueAdapter
     * @return revenue from active calculator
     */
    int revenueFromDynamicCalculator(RevenueAdapter revenueAdapter) {
        return activeCalculator.calculateRevenue(revenueAdapter);
        
    }

    /**
     * Allows dynamic modifiers to adjust the optimal run 
     * @param optimalRun
     */
    void adjustOptimalRun(List<RevenueTrainRun> optimalRun) {
        // allow dynamic modifiers to change the optimal run
        for (RevenueDynamicModifier modifier:activeDynamicModifiers) {
            modifier.adjustOptimalRun(optimalRun);
        }
    }

    /**
     * @param run the current run
     * @param optimal flag if this is the found optimal run
     * @return total value of dynamic modifiers
     */
    int evaluationValue(List<RevenueTrainRun> run, boolean optimal) {
         // allow dynamic modifiers to change the optimal run
         int value = 0;
         for (RevenueDynamicModifier modifier:activeDynamicModifiers) {
             value += modifier.evaluationValue(run, optimal);
         }
         return value;
     }

    /**
     * @return total prediction value of dynamic modifiers
     */
    int predictionValue() {
         int value = 0;
         for (RevenueDynamicModifier modifier:activeDynamicModifiers) {
             value += modifier.predictionValue();
         }
         return value;
     }
    
    /**
     * 
     * @param revenueAdapter
     * @return pretty print output from all modifiers (both static and dynamic)
     */
    String prettyPrint(RevenueAdapter revenueAdapter) {
        StringBuffer prettyPrint = new StringBuffer();
        
        for (RevenueStaticModifier modifier:activeStaticModifiers) {
            String modifierText = modifier.prettyPrint(revenueAdapter);
            if (modifierText != null) {
                prettyPrint.append(modifierText + "\n");
            }
        }

        for (RevenueDynamicModifier modifier:activeDynamicModifiers) {
            String modifierText = modifier.prettyPrint(revenueAdapter);
            if (modifierText != null) {
                prettyPrint.append(modifierText + "\n");
            }
        }
        
        return prettyPrint.toString();
    }
    
    
}
