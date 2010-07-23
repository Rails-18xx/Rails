package rails.game.specific._18EU;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.*;
import rails.game.action.StartCompany;

public class StartCompany_18EU extends StartCompany {

    // Server settings
    // Before phase 5: select a minor to merge
    protected transient List<PublicCompanyI> minorsToMerge = null;
    protected String minorsToMergeNames = null;
    // From phase 5: select a Home station
    protected boolean requestStartSpaces = false;
    protected transient List<City> availableHomeStations = null;
    protected String availableHomeStationNames = null;

    // Client settings
    // Before phase 5: selected minor to merge
    protected transient PublicCompanyI chosenMinor = null;
    protected String chosenMinorName = null;
    // From phase 5: selected Home station
    protected transient City selectedHomeStation = null;
    protected String selectedHomeStationName = null;

    public static final long serialVersionUID = 1L;

    public StartCompany_18EU(PublicCompanyI company, int[] prices) {
        super(company, prices, 1);
    }

    public void setMinorsToMerge(List<PublicCompanyI> minors) {

        minorsToMerge = minors;

        if (minorsToMerge != null) {
            StringBuffer b = new StringBuffer();
            for (PublicCompanyI minor : minorsToMerge) {
                if (b.length() > 0) b.append(",");
                b.append(minor.getName());
            }
            minorsToMergeNames = b.toString();
        }
    }

    public void setAvailableHomeStations(List<City> stations) {
        availableHomeStations = stations;

        if (availableHomeStations != null) {
            StringBuffer b = new StringBuffer();
            for (City station : availableHomeStations) {
                if (b.length() > 0) b.append(",");
                b.append(station.getName());
            }
            availableHomeStationNames = b.toString();
        }
    }

    public List<City> getAvailableHomeStations() {
        return availableHomeStations;
    }

    public List<PublicCompanyI> getMinorsToMerge() {
        return minorsToMerge;
    }

    public PublicCompanyI getChosenMinor() {
        return chosenMinor;
    }

    public void setChosenMinor(PublicCompanyI chosenMinor) {
        this.chosenMinor = chosenMinor;
        this.chosenMinorName = chosenMinor.getName();
    }

    public City getSelectedHomeStation() {
        // use delayed selectedHomeStation initialization
        // as not all cities are defined immediately
        if (selectedHomeStation == null && selectedHomeStationName != null) {
            MapManager mapManager = GameManager.getInstance().getMapManager();
            String[] parts = parseStationName (selectedHomeStationName);
            MapHex hex = mapManager.getHex(parts[0]);
            selectedHomeStation = hex.getCity(Integer.parseInt(parts[1]));
        }
        
        return selectedHomeStation;
    }

    public void setHomeStation(City homeStation) {
        selectedHomeStation = homeStation;
        selectedHomeStationName = homeStation.getName();
    }

    @Override
    public String toString() {
        StringBuffer text = new StringBuffer(super.toString());
        if (minorsToMergeNames != null) {
            text.append(" minors=").append(minorsToMergeNames);
        }
        if (chosenMinorName != null) {
            text.append(" merged minor=" + chosenMinorName);
        }
        if (availableHomeStationNames != null) {
            text.append(" stations=" + availableHomeStationNames);
        }
        if (selectedHomeStationName != null) {
            text.append(" home station=" + selectedHomeStationName);
        }
        return text.toString();
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        CompanyManagerI cmgr = getCompanyManager();
        if (minorsToMergeNames != null) {
            minorsToMerge = new ArrayList<PublicCompanyI>();
            for (String name : minorsToMergeNames.split(",")) {
                minorsToMerge.add(cmgr.getPublicCompany(name));
            }
        }
        if (chosenMinorName != null) {
            chosenMinor = cmgr.getPublicCompany(chosenMinorName);
        }

        MapManager mapManager = GameManager.getInstance().getMapManager();
        if (availableHomeStationNames != null) {
            availableHomeStations = new ArrayList<City>();
            for (String cityName : availableHomeStationNames.split(",")) {
                String[] parts = parseStationName (cityName);
                MapHex hex = mapManager.getHex(parts[0]);
                availableHomeStations.add (hex.getCity(Integer.parseInt(parts[1])));
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
