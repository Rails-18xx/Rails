package rails.common.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.common.parser.XMLTags;
import rails.game.state.Configurable;
import rails.game.state.Configure;
import rails.game.state.Context;

public class ComponentManager {

    final static Logger log = LoggerFactory.getLogger(ComponentManager.class);

    private String gameName;

    private List<Tag> componentTags;
    
    private Map<String, Configurable> mComponentMap =
            new HashMap<String, Configurable>();
    
    public ComponentManager(Context context, String gameName, Tag tag, Map<String, String> gameOptions)
            throws ConfigurationException {
        this.gameName = gameName;

        componentTags = tag.getChildren(XMLTags.COMPONENT_ELEMENT_ID);
        for (Tag component : componentTags) {
            String compName = component.getAttributeAsString("name");
            log.debug("Found component " + compName);
            configureComponent(context, component);
            component.setGameOptions(gameOptions);
        }
    }

    private void configureComponent(Context context, Tag componentTag)
            throws ConfigurationException {

        // Extract the attributes of the Component
        String name = componentTag.getAttributeAsString(XMLTags.NAME_ATTR);
        if (name == null) {
            throw new ConfigurationException(
                    LocalText.getText("UnnamedComponent"));
        }
        String clazz = componentTag.getAttributeAsString(XMLTags.CLASS_ATTR);
        if (clazz == null) {
            throw new ConfigurationException(LocalText.getText(
                    "ComponentHasNoClass", name));
        }
        String file = componentTag.getAttributeAsString(XMLTags.FILE_ATTR);

        // Only one component per name.
        if (mComponentMap.get(name) != null) {
            throw new ConfigurationException(LocalText.getText(
                    "ComponentConfiguredTwice", name));
        }

        // Now construct the component
        Configurable component = Configure.create(Configurable.class, clazz, context, name);

        // Configure the component, from a file, or the embedded XML.
        Tag configElement = componentTag;
        if (file != null) {
            String directory =  "data/" + gameName;
            configElement = Tag.findTopTagInFile(file, directory, name);
            configElement.setGameOptions(componentTag.getGameOptions());
        }

        try {
            component.configureFromXML(configElement);
        } catch (ConfigurationException e) {
            // Temporarily allow components to be incompletely configured.
            log.warn(LocalText.getText("AcceptingConfigFailure"), e);
        }

        // Add it to the map of known components.
        mComponentMap.put(name, component);
        log.debug(LocalText.getText("ComponentInitAs", name, clazz ));

    }

    /**
     * Returns the configured parameter with the given name.
     *
     * @param componentName the of the component sought.
     * @return the component sought, or null if it has not been configured.
     */
    public Configurable findComponent(String componentName) throws ConfigurationException {
        Configurable comp = mComponentMap.get(componentName);
        
        //FIXME: Revenue Manager is currently optional.
        if (comp == null && componentName != "RevenueManager") {
            throw new ConfigurationException("No XML element found for component named: " + componentName);
        }
        
        return comp;
    }
}
