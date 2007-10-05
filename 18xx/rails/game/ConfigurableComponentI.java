/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ConfigurableComponentI.java,v 1.2 2007/10/05 22:02:27 evos Exp $ */
package rails.game;

import org.w3c.dom.Element;

/**
 * Interaface for rails.game components which can be configured from an XML element.
 */
public interface ConfigurableComponentI
{

	/**
	 * Instructs the component to configure itself from the provided XML
	 * element.
	 * 
	 * @param el
	 *            the XML element containing the configuration
	 * @throws ConfigurationException
	 */
	void configureFromXML(Element el) throws ConfigurationException;

}
