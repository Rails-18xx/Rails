/*
 * Created on Sep 7, 2005
 * Author: Erik Vos
 */
package game;

import java.util.List;

import org.w3c.dom.Element;

/**
 * @author Erik Vos
 */
public interface PhaseI {
	
    /* Probably redundant */
	public List getAvailableTrainTypes();

	public void configureFromXML(Element el) throws ConfigurationException;
	
	public boolean isTileColourAllowed (String tileColour);

	public int getIndex ();
	
	public String getName ();
}
