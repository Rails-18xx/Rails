/**
 * 
 */
package rails.game.specific._1837;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.google.common.base.Objects;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.MapManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.Stop;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
import rails.game.specific._1880.SetupNewPublicDetails_1880;

/**
 * @author martin
 *
 */
public class SetHomeHexLocation extends StartItemAction {

    private static final long serialVersionUID = 1L;
    protected transient MapHex selectedHomeHex = null;
    protected String selectedHomeHexName = null;
    transient protected PublicCompany company;
    protected String companyName;
    /**
     * @param startItem
     * @param price 
     * @param player 
     */
    public SetHomeHexLocation(StartItem startItem,
            PublicCompany company, Player player, int price) {
        super(startItem);
        startItem.setSold(player, price);
        this.company = company;
        this.companyName = company.getId();
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
    
    private void readObject(ObjectInputStream in) throws IOException,
    ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
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
        SetHomeHexLocation action = (SetHomeHexLocation)pa; 
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
                .addToString("company", company)
                .addToStringOnlyActed("selectedHomeHex", selectedHomeHex)
                .toString()
        ;
    }

    public Object getCompanyName() {
        return companyName;
    }

    public void setHomeHex(String string) {
        MapManager mapManager = getRoot().getMapManager();
        MapHex homeHex=mapManager.getHex(string);
        selectedHomeHex = homeHex;
        selectedHomeHexName = homeHex.getId();
    }
}
