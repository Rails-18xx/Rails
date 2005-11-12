/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TileI.java,v 1.4 2005/11/12 13:44:08 evos Exp $
 * 
 * Created on 23-Oct-2005
 * Change Log:
 */
package game;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Element;

/**
 * @author Erik Vos
 */
public interface TileI {
    
    public void configureFromXML (Element se, Element te)
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
    
    public boolean isUpgradeable ();
    public boolean isUpgradeableNow ();

    public List getUpgrades (MapHex hex);
    
    public String getUpgradesString (MapHex hex);

    public boolean hasStations ();
    
    public List getStations();

}
