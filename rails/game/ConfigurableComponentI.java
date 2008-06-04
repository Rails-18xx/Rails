/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ConfigurableComponentI.java,v 1.4 2008/06/04 19:00:31 evos Exp $ */
package rails.game;

import rails.util.Tag;

/**
 * Interaface for rails.game components which can be configured from an XML
 * element.
 */
public interface ConfigurableComponentI {

    /**
     * Instructs the component to configure itself from the provided XML
     * element.
     * 
     * @param el the XML element containing the configuration
     * @throws ConfigurationException
     */
    void configureFromXML(Tag tag) throws ConfigurationException;

}
