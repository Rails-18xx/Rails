/*
 * Created on Sep 7, 2005
 * Author: Erik Vos
 */
package game;

import java.util.Map;

import org.w3c.dom.Element;

/**
 * @author Erik Vos
 */
public interface PhaseI extends ConfigurableComponentI {
	
	public boolean isTileColourAllowed (String tileColour);
	public Map getTileColours ();

	public int getIndex ();
	
	public String getName ();
	
    public boolean doPrivatesClose();

    public boolean isPrivateSellingAllowed();
    
    public int getNumberOfOperatingRounds ();

}
