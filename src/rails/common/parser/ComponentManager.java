/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ComponentManager.java,v 1.19 2010/05/18 04:12:23 stefanfrey Exp $ */
package rails.common.parser;

import java.lang.reflect.Constructor;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.parser.XMLTags;
import rails.game.state.Context;

/**
 * ComponentManage - an implementation of ComponentManagerI, which handles the
 * creation and configuration of rails.game components, and acts as a discovery
 * point for other components to find them.
 */
public class ComponentManager {

    private String gameName;

    private List<Tag> componentTags;

    protected Logger log = LoggerFactory.getLogger(ComponentManager.class.getPackage().getName());
    protected List<String> directories = new ArrayList<String>();
    
    private Map<String, ConfigurableComponent> mComponentMap =
            new HashMap<String, ConfigurableComponent>();
    
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
        ConfigurableComponent component;
        try {
            Class<? extends ConfigurableComponent> compClass;
            compClass =
                    Class.forName(clazz).asSubclass(
                            ConfigurableComponent.class);
            Constructor<? extends ConfigurableComponent> compCons =
                compClass.getConstructor(new Class[0]);
            component = compCons.newInstance(new Object[0]);
        } catch (Exception ex) {
            // There are MANY things that could go wrong here.
            // They all just mean that the configuration and code
            // do not combine to make a well-formed system.
            // Debugging aided by chaining the caught exception.
            throw new ConfigurationException(LocalText.getText(
                    "ComponentHasNoClass", clazz), ex);

        }

        // Configure the component, from a file, or the embedded XML.
        Tag configElement = componentTag;
        if (file != null) {
            directories.add("data/" + gameName);
            configElement = Tag.findTopTagInFile(file, directories, name);
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
    public ConfigurableComponent findComponent(String componentName) throws ConfigurationException {
        ConfigurableComponent comp = mComponentMap.get(componentName);
        
        //FIXME: Revenue Manager is currently optional.
        if (comp == null && componentName != "RevenueManager") {
            throw new ConfigurationException("No XML element found for component named: " + componentName);
        }
        
        return comp;
    }
}
