package rails.game.action;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import net.sf.rails.game.*;
import net.sf.rails.util.RailsObjects;

import com.google.common.base.Objects;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This new class is intended to replace ExchangeTokens as used in 1856.
 * It is more versatile, and has been developed for 1826
 * at the Etat or SNCF formation. There the president may
 * elect to replace tokens of all companies which have been
 * absorbed, with a maximum of 2 resp. 3 per company.
 */
public class ExchangeTokens2 extends PossibleAction {

    // Server settings
    /**
     * The company of which tokens may be laid
     */
    private transient PublicCompany newCompany;
    private String newCompanyName;
    /**
     * A multimap with all eligible stops for token (re)placement,
     * for all eligible companies.
     */
    private transient List<Location> locations;
    /**
     * To serialize, one string will hold all values.
     * Format: "Company1:stop1,stop2;Company2:stop3" etc.
     */
    private String locationsAsString;
    /**
     * The minimum number to exchange per company, normally 0
     */
    private int minNumberToExchange;
    /**
     * The maximum number to exchange *per company*; 0 = unlimited
     */
    private int maxNumberToExchange;
    /**
     * False: the numbers to exchange are counted over all old companies,
     * True: the numbers to exchange are counted per old company.
     */
    private boolean exchangeCountPerCompany;

    // Client settings: in Location only

    // Other initializations
    //private transient CompanyManager companyManager;
    //private transient MapManager mapManager;

    public static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ExchangeTokens2.class);

    public ExchangeTokens2(RailsRoot root,
                          PublicCompany newCompany,
                          int minNumberToExchange,
                          int maxNumberToExchange,
                          boolean exchangeCountPerCompany) {

        super(root);
        this.newCompany = newCompany;
        this.newCompanyName = newCompany.getId();
        this.minNumberToExchange = minNumberToExchange;
        this.maxNumberToExchange = maxNumberToExchange;
        this.exchangeCountPerCompany = exchangeCountPerCompany;

        //companyManager = root.getCompanyManager();
        //mapManager = root.getMapManager();

        locations = new ArrayList<>();
    }

    /*public void setExchangedTokens2(List<ExchangeableToken> exchangedTokens) {
        for (ExchangeableToken t : exchangedTokens) {
            t.setSelected(true);
        }
    }*/
    public void addStop (PublicCompany oldCompany, Stop stop) {
        locations.add (new Location(oldCompany, newCompany, stop));
        build();
    }

    public PublicCompany getNewCompany() {
        return newCompany;
    }

    /* not used
    public void setNewCompany(PublicCompany newCompany) {
        this.newCompany = newCompany;
    }*/

    private void build() {
        locationsAsString = buildLocationsString(this);
    }

    public void setLocations(List<Location> locations) {
        this.locations = locations;
        build();
    }

    public List<Location> getLocations() {
        return locations;
    }

    public int getMaxNumberToExchange() {
        return maxNumberToExchange;
    }

    public int getMinNumberToExchange() {
        return minNumberToExchange;
    }

    public boolean isExchangeCountPerCompany() {
        return exchangeCountPerCompany;
    }

    public String getLocationsAsString() {
        return locationsAsString;
    }

    public void clearSelections() {
        for (Location location : locations) {
            location.setSelected(false);
        }
        build();
    }

    /*public List<ExchangeableToken> getTokensToExchange() {
        return tokensToExchange;
    }*/

    public String buildLocationsString(ExchangeTokens2 action) {
        List<String> cities = new ArrayList<>();
        if (locations == null) locations = new ArrayList<>();
        for (Location location : locations) {
            cities.add(location.toString());
        }
        return Util.join(cities, ";");
    }

    public List<Location> parseLocationsString (String locationsString) {
        List<Location> locations = new ArrayList<>();
        String[] parts1, parts2;
        CompanyManager cmgr = getCompanyManager();
        MapManager mmgr = root.getMapManager();
        for (String s : locationsString.split(";")) {
            parts1 = s.split(":");
            PublicCompany oldCompany = cmgr.getPublicCompany(parts1[0]);
            for (String st : parts1[1].split(",")) {
                parts2 = st.split("/");
                MapHex hex = mmgr.getHex(parts2[0]);
                int stopNumber = Integer.parseInt(parts2[1]);
                boolean selected = (parts2.length > 2 && parts2[2].equals("+"));
                for (Stop stop : hex.getStops()) {
                    if (stop.getNumber() == stopNumber) {
                        Location location = new Location (oldCompany, stop, selected);
                        locations.add(location);
                        break;
                    }
                }
            }
        }
        return locations;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        newCompany = getCompanyManager().getPublicCompany(newCompanyName);
        locations = parseLocationsString (locationsAsString);
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        ExchangeTokens2 action = (ExchangeTokens2)pa;
        boolean result = Objects.equal(this.newCompany, action.newCompany)
                && Objects.equal(this.minNumberToExchange, action.minNumberToExchange)
                && Objects.equal(this.maxNumberToExchange, action.maxNumberToExchange)
                && Objects.equal(this.exchangeCountPerCompany, action.exchangeCountPerCompany)
                ;
        if (asOption) return result;

        // check asAction attributes
        return result && validate (action.getLocationsAsString());
    }


    private boolean validate (String actionLocationsString ) {
        for (String locationString : actionLocationsString.split(";")) {
            if (!this.locationsAsString.contains(locationString)) return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                        .addToString("newCompany", newCompany)
                        .addToString("tokensToExchange", locations)
                        .addToString("minNumberToExchange", minNumberToExchange)
                        .addToString("maxNumberToExchange", maxNumberToExchange)
                        .addToString("exchangeCountPerCompany", exchangeCountPerCompany)
                        .toString()
                ;
    }


    public class Location {
        private transient PublicCompany oldCompany;
        private String oldCompanyName;
        private transient Stop stop;
        private String stopComposedId;
        private boolean selected;

        private Location(PublicCompany oldCompany, PublicCompany newCompany, Stop stop) {
            this(oldCompany, stop, false);
        }

        private Location(PublicCompany oldCompany, Stop stop, boolean selected) {
            this.oldCompany = oldCompany;
            oldCompanyName = oldCompany.getId();
            this.stop = stop;
            stopComposedId = stop.getStopComposedId();
            this.selected = selected;
        }

        public PublicCompany getOldCompany() {
            return oldCompany;
        }

        public String getOldCompanyName() {
            return oldCompanyName;
        }

        public Stop getStop() {
            return stop;
        }

        public String getStopComposedId() {
            return stopComposedId;
        }

        public boolean isSelected() {
            return selected;
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
        }

        public void setSelected() { selected = true; build(); }

        protected boolean equalsAs(Location loc, boolean asOption) {
            // identity always true
            if (loc == this) return true;

            // check asOption attributes
            return Objects.equal(this.oldCompanyName, loc.oldCompanyName)
                    && Objects.equal(this.stopComposedId,loc.stopComposedId)
                    ;
            // no asAction attributes to be checked
        }

        public String toString() {
            return oldCompanyName + ":" + stopComposedId +
                    (selected ? "/+" : "");
        }

    }

}
