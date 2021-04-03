package rails.game.specific._1837;

import com.google.common.base.Objects;
import net.sf.rails.game.*;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import rails.game.action.StartItemAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author martin
 *
 */
public class SetHomeHexLocation2 extends PossibleORAction {

    private static final long serialVersionUID = 1L;

    /*--- Server-side settings ---*/
    protected transient PublicCompany company;
    protected String companyName;
    protected transient List<MapHex> potentialHexes;
    protected String potentialHexNames;

    /*--- Client-side settings ---*/
    protected transient MapHex selectedHomeHex = null;
    protected String selectedHomeHexName = null;

    /**
     * @param root
     * @param company
     * @param hexNames
     */
    public SetHomeHexLocation2(RailsRoot root, PublicCompany company,
                               String hexNames) {
        super(root);
        this.company = company;
        this.companyName = company.getId();
        this.potentialHexNames = hexNames;
        this.potentialHexes = new ArrayList<>(2);
        for (String hexName : hexNames.split(",")) {
            potentialHexes.add (root.getMapManager().getHex(hexName));
        }
    }

    public void setHomeHex(MapHex homeHex) {
        selectedHomeHex = homeHex;
        selectedHomeHexName = homeHex.getId();
    }
    public MapHex getSelectedHomeHex() {
        // use delayed selectedHomeStation initialization
        // as not all cities are defined immediately
        if (selectedHomeHex == null && selectedHomeHexName != null) {
            MapManager mapManager = getRoot().getMapManager();
            String[] parts = parseStationName (selectedHomeHexName);
            MapHex hex = mapManager.getHex(parts[0]);
           // int stationId = Integer.parseInt(parts[1]);
            selectedHomeHex = hex;
        }

        return selectedHomeHex;
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

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        if (in instanceof GameLoader.RailsObjectInputStream) {
            if (Util.hasValue(companyName))
                company = getCompanyManager().getPublicCompany(companyName);
        }
    }

    public void applyRailsRoot(RailsRoot root) {
        super.applyRailsRoot(root);

        if (Util.hasValue(companyName)) {
            company = getCompanyManager().getPublicCompany(companyName);
        }
    }

    public PublicCompany getCompany() {
        return company;
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        SetHomeHexLocation2 action = (SetHomeHexLocation2)pa;
        boolean options = Objects.equal(this.company, action.company);
        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
                && Objects.equal(this.selectedHomeHex, action.selectedHomeHex);
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                //.addToString("company", company) // Already done in super
                .addToString("possibleHomeHexes", potentialHexNames)
                .addToStringOnlyActed("selectedHomeHex", selectedHomeHexName)
                .toString()
        ;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setHomeHex(String string) {
        MapManager mapManager = getRoot().getMapManager();
        MapHex homeHex=mapManager.getHex(string);
        selectedHomeHex = homeHex;
        selectedHomeHexName = homeHex.getId();
    }

    public List<MapHex> getPotentialHexes() {
        return potentialHexes;
    }
}
