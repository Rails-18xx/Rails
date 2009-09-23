/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/Bonus.java,v 1.1 2009/09/23 21:38:57 evos Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

/**
 * An object of class Bonus represent extra income for the owning company,
 * usually connected to certain map locations.
 * <p>Currently, Bonus objects will be created in the following cases:
 * <br>1. when a SpecialTokenLay containing a BonusToken
 * is exercised,
 * <br>2. when a private having a LocatedBonus special property is bought by
 * a public company,
 * <br>3. when a sellable bonus is bought from such a public company by another company.
 * @author VosE
 *
 */
public class Bonus implements Closeable {

	private PublicCompanyI owner;
    private String locationCodes = null;
    private List<MapHex> locations = null;
    private String name;
    private int value;
    private String removingObjectDesc = null;
    private Object removingObject = null;

    public Bonus (PublicCompanyI owner,
    		String name, int value, String locationCodes) {
    	this.owner = owner;
    	this.name = name;
    	this.value = value;
    	this.locationCodes = locationCodes;
   		parseLocations();
    }

    public boolean isExecutionable() {
        return false;
    }

    public PublicCompanyI getOwner() {
		return owner;
	}

	public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    private void parseLocations () {

        MapManager mmgr = MapManager.getInstance();
        MapHex hex;
        locations = new ArrayList<MapHex>();
        for (String hexName : locationCodes.split(",")) {
            hex = mmgr.getHex(hexName);
            if (hex != null) locations.add(hex);
        }
    }

    /**
     * Remove the token.
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */
    public void close() {

        owner.removeBonus(name);
    }

    /**
     * Prepare the bonus token for removal, if so configured.
     * The only case currently implemented to trigger removal
     * is the start of a given phase.
     */
    public void prepareForRemoval (PhaseManager phaseManager) {

        if (removingObjectDesc == null) return;

        if (removingObject == null) {
	        String[] spec = removingObjectDesc.split(":");
	        if (spec[0].equalsIgnoreCase("Phase")) {
	            removingObject =
	                    phaseManager.getPhaseByName(spec[1]);
	        }
        }

        if (removingObject instanceof Phase) {
            ((Phase) removingObject).addObjectToClose(this);
        }
    }

	@Override
	public String toString() {
        return "Bonus "+name+" hex="
               + locationCodes + " value=" + value;
    }

}
