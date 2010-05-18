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

    public RevenueManager() {
        staticModifiers = new HashSet<RevenueStaticModifier>(); 
    }
    
    public void configureFromXML(Tag tag) throws ConfigurationException {
        
        // define static modifiers
        List<Tag> modifierTags = tag.getChildren("StaticModifier");
        
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

    public void finishConfiguration(GameManagerI parent)
            throws ConfigurationException {
        for (RevenueStaticModifier modifier:staticModifiers) {
            if (modifier instanceof ConfigurableComponentI) {
                ((ConfigurableComponentI)modifier).finishConfiguration(parent);
            }
        }
    }
    
    void callStaticModifiers(RevenueAdapter revenueAdapter) {
        for (RevenueStaticModifier modifier:staticModifiers) {
            modifier.modifyCalculator(revenueAdapter);
        }
        
    }

}
