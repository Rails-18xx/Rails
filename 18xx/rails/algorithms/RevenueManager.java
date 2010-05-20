package rails.algorithms;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;


import rails.game.ConfigurableComponentI;
import rails.game.ConfigurationException;
import rails.game.GameManagerI;
import rails.util.LocalText;
import rails.util.Tag;

/**
 * Coordinates and stores all elements related to revenue calulcation,
 * which are permanent.
 * The conversion of Rails elements is in the responsibility of the RevenueAdapter.
 * For each GameManager instance only one RevenueManager is created.
 * 
 * @author freystef
 *
 */

public final class RevenueManager implements ConfigurableComponentI {

    protected static Logger log =
        Logger.getLogger(RevenueManager.class.getPackage().getName());

    private Set<RevenueStaticModifier> staticModifiers;
    private Set<RevenueDynamicModifier> dynamicModifiers;

    public RevenueManager() {
        staticModifiers = new HashSet<RevenueStaticModifier>(); 
        dynamicModifiers = new HashSet<RevenueDynamicModifier>(); 
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        // define static modifiers
        List<Tag> modifierTags = tag.getChildren("StaticModifier");
        
        if (modifierTags != null) {
            for (Tag modifierTag:modifierTags) {
                // get classname
                String className = modifierTag.getAttributeAsString("class");
                if (className == null) {
                    throw new ConfigurationException(LocalText.getText(
                            "ComponentHasNoClass", "StaticModifier"));
                }
                // create modifier
                RevenueStaticModifier modifier;
                try {
                    modifier = (RevenueStaticModifier) Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException(LocalText.getText(
                            "ClassCannotBeInstantiated", className), e);
                }
                // add them to the revenueManager
                staticModifiers.add(modifier);
                log.info("Added modifier " + className);
            }
        }

        // define dynamic modifiers
        modifierTags = tag.getChildren("DynamicModifier");
        
        if (modifierTags != null) {
            for (Tag modifierTag:modifierTags) {
                // get classname
                String className = modifierTag.getAttributeAsString("class");
                if (className == null) {
                    throw new ConfigurationException(LocalText.getText(
                            "ComponentHasNoClass", "DynamicModifier"));
                }
                // create modifier
                RevenueDynamicModifier modifier;
                try {
                    modifier = (RevenueDynamicModifier) Class.forName(className).newInstance();
                } catch (Exception e) {
                    throw new ConfigurationException(LocalText.getText(
                            "ClassCannotBeInstantiated", className), e);
                }
                // add them to the revenueManager
                dynamicModifiers.add(modifier);
                log.info("Added modifier " + className);
            }
        }
    }

    public void finishConfiguration(GameManagerI parent)
            throws ConfigurationException {
        for (RevenueStaticModifier modifier:staticModifiers) {
            if (modifier instanceof ConfigurableComponentI) {
                ((ConfigurableComponentI)modifier).finishConfiguration(parent);
            }
        }
    }
    
    public void addStaticModifier(RevenueStaticModifier modifier) {
        staticModifiers.add(modifier);
        log.info("Revenue Manager: Added modifier " + modifier);
    }
    
    public boolean removeStaticModifier(RevenueStaticModifier modifier) {
        boolean result = staticModifiers.remove(modifier);
        if (result) {
            log.info("RevenueManager: Removed modifier " + modifier);
        } else {
            log.info("RevenueManager: Cannot remove" + modifier);
        }
        return result;
    }

    Set<RevenueStaticModifier> getStaticModifiers() {
        return staticModifiers;
    }

    Set<RevenueDynamicModifier> getDynamicModifiers() {
        return dynamicModifiers;
    }
    
    void callStaticModifiers(RevenueAdapter revenueAdapter) {
        for (RevenueStaticModifier modifier:staticModifiers) {
            modifier.modifyCalculator(revenueAdapter);
        }
    }

    Set<RevenueDynamicModifier> callDynamicModifiers(RevenueAdapter revenueAdapter) {
        Set<RevenueDynamicModifier> activeModifiers = new HashSet<RevenueDynamicModifier>();
        for (RevenueDynamicModifier modifier:dynamicModifiers) {
            if (modifier.prepareModifier(revenueAdapter))
                activeModifiers.add(modifier);
        }
        return activeModifiers;
    }

}
