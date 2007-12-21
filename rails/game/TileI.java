/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TileI.java,v 1.6 2007/12/21 21:18:12 evos Exp $ */
package rails.game;

import java.util.*;

import rails.util.Tag;

public interface TileI
{

	public void configureFromXML(Tag se, Tag te)
	throws ConfigurationException;

	public String getColour();

	/**
	 * @return Returns the id.
	 */
	public int getId();
	public int getExternalId();
	public int getPictureId();

	/**
	 * @return Returns the name.
	 */
	public String getName();

	public boolean hasTracks(int sideNumber);

	public boolean isUpgradeable();

	public boolean isLayableNow();

	public List<TileI> getUpgrades(MapHex hex);
	public List<TileI> getValidUpgrades (MapHex hex, PhaseI phase);

	public String getUpgradesString(MapHex hex);

	public boolean hasStations();

	public List getStations();
	
	public int getNumStations();

	public boolean lay (MapHex hex);
	public boolean remove (MapHex hex);
	public int countFreeTiles ();
	public int getQuantity ();
}
