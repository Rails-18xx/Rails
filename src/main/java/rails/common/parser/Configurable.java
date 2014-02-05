package rails.common.parser;

import rails.game.RailsRoot;
import rails.game.state.Creatable;

/**
 * Interface for rails.game components which can be configured from an XML
 * element.
 */
public interface Configurable extends Creatable {
    
    /**
     * Instructs the component to configure itself from the provided XML
     * element.
     * 
     * @param element the XML element containing the configuration
     * @throws ConfigurationException
     */
    void configureFromXML(Tag tag) throws ConfigurationException;
    
    /**
     * This method is intended to be called for each configurable
     * component, to perforn any initialisation activities that
     * require any other components to be initialised first. 
     * This includes creating any required relationships to other 
     * configured components and objects. 
     * <p>This method should be called where necessary after all 
     * XML file parsing has completed, so that all objects that
     * need to be related to do exist. 
     * @param parent The 'parent' configurable component is passed to allow 
     * the 'child' to access any other object without the need to resort to
     * static calls where possible. 
     */
    void finishConfiguration (RailsRoot parent)
    throws ConfigurationException;

}
