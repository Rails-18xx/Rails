package rails.common.parser;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.common.parser.XMLTags;
import rails.game.RailsRoot;

public class ComponentManager {

    private static final Logger log = LoggerFactory.getLogger(ComponentManager.class);

    private final Map<String, Configurable> mComponentMap = Maps.newHashMap();
    
    public ComponentManager() {}
    
    public void start(RailsRoot root, Tag tag) throws ConfigurationException {
        List<Tag> componentTags = tag.getChildren(XMLTags.COMPONENT_ELEMENT_ID);
        for (Tag componentTag : componentTags) {
            String compName = componentTag.getAttributeAsString("name");
            log.debug("Found component " + compName);
            Configurable component = configureComponent(root, componentTag);
            // feedback to RailsRoot
            root.setComponent(component);
        }
    }

    private Configurable configureComponent(RailsRoot root, Tag componentTag)
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

        // Now construct the component: Parent is a RailsRoot
        Configurable component = Configure.create(Configurable.class, clazz, RailsRoot.class, root, name);

        // Configure the component, from a file, or the embedded XML.
        Tag configElement = componentTag;
        if (file != null) {
            String directory =  "data/" + root.getGameName();
            configElement = Tag.findTopTagInFile(file, directory, name, root.getGameOptions());
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
        
        return component;
    }

}
