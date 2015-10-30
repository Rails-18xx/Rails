package rails.game.specific._18EU;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.action.StartCompany;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Stop;
import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;


/**
 * Rails 2.0: Updated equals and toString methods
 */
public class StartCompany_18EU extends StartCompany {

    // Server settings
    // Before phase 5: select a minor to merge
    protected transient List<PublicCompany> minorsToMerge = null;
    protected String minorsToMergeNames = null;
    // From phase 5: select a Home station
    protected boolean requestStartSpaces = false;
    protected transient List<Stop> availableHomeStations = null;
    protected String availableHomeStationNames = null;

    // Client settings
    // Before phase 5: selected minor to merge
    protected transient PublicCompany chosenMinor = null;
    protected String chosenMinorName = null;
    // From phase 5: selected Home station
    protected transient Stop selectedHomeStation = null;
    protected String selectedHomeStationName = null;

    public static final long serialVersionUID = 1L;

    public StartCompany_18EU(PublicCompany company, int[] prices) {
        super(company, prices, 1);
    }

    public void setMinorsToMerge(List<PublicCompany> minors) {

        minorsToMerge = minors;

        if (minorsToMerge != null) {
            StringBuffer b = new StringBuffer();
            for (PublicCompany minor : minorsToMerge) {
                if (b.length() > 0) b.append(",");
                b.append(minor.getId());
            }
            minorsToMergeNames = b.toString();
        }
    }

    public void setAvailableHomeStations(List<Stop> stations) {
        availableHomeStations = stations;

        if (availableHomeStations != null) {
            StringBuffer b = new StringBuffer();
            for (Stop station : availableHomeStations) {
                if (b.length() > 0) b.append(",");
                b.append(station.getSpecificId());
            }
            availableHomeStationNames = b.toString();
        }
    }

    public List<Stop> getAvailableHomeStations() {
        return availableHomeStations;
    }

    public List<PublicCompany> getMinorsToMerge() {
        return minorsToMerge;
    }

    public PublicCompany getChosenMinor() {
        return chosenMinor;
    }

    public void setChosenMinor(PublicCompany chosenMinor) {
        this.chosenMinor = chosenMinor;
        this.chosenMinorName = chosenMinor.getId();
    }

    public Stop getSelectedHomeStation() {
        // use delayed selectedHomeStation initialization
        // as not all cities are defined immediately
        if (selectedHomeStation == null && selectedHomeStationName != null) {
            MapManager mapManager = getRoot().getMapManager();
            String[] parts = parseStationName (selectedHomeStationName);
            MapHex hex = mapManager.getHex(parts[0]);
            int stationId = Integer.parseInt(parts[1]);
            selectedHomeStation = hex.getRelatedStop(stationId);
        }

        return selectedHomeStation;
    }

    public void setHomeStation(Stop homeStation) {
        selectedHomeStation = homeStation;
        selectedHomeStationName = homeStation.getSpecificId();
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        StartCompany_18EU action = (StartCompany_18EU)pa; 
        boolean options =  
                Objects.equal(this.requestStartSpaces, action.requestStartSpaces)
                // availableHomeStations does not work correctly, as here sometimes the station number deviate
               // && RailsObjects.elementEquals(this.availableHomeStations, action.availableHomeStations)
                && RailsObjects.elementEquals(this.minorsToMerge, action.minorsToMerge)
        ;
        
        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
                && Objects.equal(this.chosenMinor, action.chosenMinor)
                && Objects.equal(this.selectedHomeStation, action.selectedHomeStation)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("minorsToMerge", minorsToMerge)
                    .addToString("requestStartSpaces", requestStartSpaces)
                    .addToString("availableHomeStations", availableHomeStations)
                    .addToStringOnlyActed("chosenMinor", chosenMinor)
                    .addToStringOnlyActed("selectedHomeStation", selectedHomeStation)
                    .toString()
        ;

    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        in.defaultReadObject();

        CompanyManager cmgr = getCompanyManager();
        if (minorsToMergeNames != null) {
            minorsToMerge = new ArrayList<PublicCompany>();
            for (String name : minorsToMergeNames.split(",")) {
                minorsToMerge.add(cmgr.getPublicCompany(name));
            }
        }
        if (chosenMinorName != null) {
            chosenMinor = cmgr.getPublicCompany(chosenMinorName);
        }

        MapManager mapManager = RailsRoot.getInstance().getMapManager();
        if (availableHomeStationNames != null) {
            availableHomeStations = new ArrayList<Stop>();
            for (String cityName : availableHomeStationNames.split(",")) {
                String[] parts = parseStationName (cityName);
                MapHex hex = mapManager.getHex(parts[0]);
                int stationId = Integer.parseInt(parts[1]);
                availableHomeStations.add (hex.getRelatedStop(stationId));
            }
        }
        // selectedHomeStation is delayed
    }

    private String[] parseStationName (String name) {

        if (name.contains(" on ")) {
            // Old style
            String[] parts = name.split(" ");
            return new String[] {parts[4], parts[1]};
        } else {
            // New style
            return name.split("/");
        }
    }

}
