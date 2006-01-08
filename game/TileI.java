/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TileI.java,v 1.9 2006/01/08 19:32:56 evos Exp $
 * 
 * Created on 23-Oct-2005
 * Change Log:
 */
package game;

import java.util.*;

import org.w3c.dom.Element;

/**
 * @author Erik Vos
 */
public interface TileI
{

	public void configureFromXML(Element se, Element te)
			throws ConfigurationException;

	public String getColour();

	/**
	 * @return Returns the id.
	 */
	public int getId();

	/**
	 * @return Returns the name.
	 */
	public String getName();

	public boolean hasTracks(int sideNumber);

	public boolean isUpgradeable();

	public boolean isLayableNow();

	public List getUpgrades(MapHex hex);
	public List getValidUpgrades (MapHex hex, PhaseI phase);

	public String getUpgradesString(MapHex hex);

	public boolean hasStations();

	public List getStations();
	
	public int getNumStations();

	public boolean lay (MapHex hex);
	public boolean remove (MapHex hex);
	public int countFreeTiles ();
	public int getAmount ();
}
