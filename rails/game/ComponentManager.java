/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ComponentManager.java,v 1.19 2010/05/18 04:12:23 stefanfrey Exp $ */
package rails.game;

import java.lang.reflect.Constructor;
import java.util.*;

import org.apache.log4j.Logger;

import rails.util.LocalText;
import rails.util.Tag;

/**
 * ComponentManage - an implementation of ComponentManagerI, which handles the
 * creation and configuration of rails.game components, and acts as a discovery
 * point for other components to find them.
 */
public class ComponentManager {

    private String gameName;

    /** The name of the XML tag used to configure the ComponentManager. */
    public static final String ELEMENT_ID = "ComponentManager";

    /** The name of the XML tag used to configure a component. */
    public static final String COMPONENT_ELEMENT_ID = "Component";

    /** The name of the XML attribute for the component's name. */
    public static final String COMPONENT_NAME_TAG = "name";

    /** The name of the XML attribute for the component's class. */
    public static final String COMPONENT_CLASS_TAG = "class";

    /** The name of the XML attribute for the component's configuration file. */
    public static final String COMPONENT_FILE_TAG = "file";

    private List<Tag> componentTags;
    private Map<String, String> gameOptions;

    protected static Logger log =
            Logger.getLogger(ComponentManager.class.getPackage().getName());
//    protected static List<String> directories = new ArrayList<String>();
    protected List<String> directories = new ArrayList<String>();

    public static synchronized ComponentManager configureInstance(String gameName, Tag tag,
            Map<String, String> gameOptions)
            throws ConfigurationException {
        return new ComponentManager(gameName, tag, gameOptions);
    }

    private ComponentManager(String gameName, Tag tag, Map<String, String> gameOptions)
            throws ConfigurationException {

        this.gameOptions = gameOptions;
        this.gameName = gameName;

        componentTags = tag.getChildren(COMPONENT_ELEMENT_ID);
        for (Tag component : componentTags) {
            String compName = component.getAttributeAsString("name");
            log.debug("Found component " + compName);
            if (compName.equalsIgnoreCase(GameManager.GM_NAME)) {
                configureComponent(component);
                break;
            }
        }
    }

    public synchronized void finishPreparation() throws ConfigurationException {

        for (Tag componentTag : componentTags) {
            componentTag.setGameOptions(gameOptions);
            String compName = componentTag.getAttributeAsString("name");
            if (compName.equalsIgnoreCase(GameManager.GM_NAME)) continue;
            log.debug("Found component " + compName);
            configureComponent(componentTag);
        }
    }

    private void configureComponent(Tag componentTag)
            throws ConfigurationException {

        // Extract the attributes of the Component
        String name = componentTag.getAttributeAsString(COMPONENT_NAME_TAG);
        if (name == null) {
            throw new ConfigurationException(
                    LocalText.getText("UnnamedComponent"));
        }
        String clazz = componentTag.getAttributeAsString(COMPONENT_CLASS_TAG);
        if (clazz == null) {
            throw new ConfigurationException(LocalText.getText(
                    "ComponentHasNoClass", name));
        }
        String file = componentTag.getAttributeAsString(COMPONENT_FILE_TAG);

        // Only one component per name.
        if (mComponentMap.get(name) != null) {
            throw new ConfigurationException(LocalText.getText(
                    "ComponentConfiguredTwice", name));
        }

        // Now construct the component
        ConfigurableComponentI component;
        try {
            Class<? extends ConfigurableComponentI> compClass;
            compClass =
                    Class.forName(clazz).asSubclass(
                            ConfigurableComponentI.class);
            Constructor<? extends ConfigurableComponentI> compCons =
                    compClass.getConstructor(new Class[0]);
            component = compCons.newInstance(new Object[0]);
        } catch (Exception ex) {
            // Not great to catch Exception, but there are MANY things that
            // could go wrong
            // here, and they all just mean that the configuration and code
            // do not between
            // them make a well-formed system. Debugging aided by chaining
            // the caught exception.
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
    public ConfigurableComponentI findComponent(String componentName) {
        return mComponentMap.get(componentName);
    }

    private Map<String, ConfigurableComponentI> mComponentMap =
            new HashMap<String, ConfigurableComponentI>();

}
