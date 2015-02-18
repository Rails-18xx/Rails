package rails.game.action;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

import net.sf.rails.game.MapHex;
import net.sf.rails.game.special.SpecialBaseTokenLay;
import net.sf.rails.game.special.SpecialBonusTokenLay;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.util.RailsObjects;

/**
 * Rails 2.0: Updated equals and toString methods
 */
public abstract class LayToken extends PossibleORAction {

    /*--- Preconditions ---*/

    /** Where to lay a token (null means anywhere) */
    transient protected List<MapHex> locations = null;
    protected String locationNames;

    /**
     * Special property that will be fulfilled by this token lay. If null, this
     * is a normal token lay.
     */
    transient protected SpecialProperty specialProperty = null;
    protected int specialPropertyId;

    /*--- Postconditions ---*/

    /** The map hex on which the token is laid */
    transient protected MapHex chosenHex = null;
    protected String chosenHexName;

    public static final long serialVersionUID = 1L;

    /**
     * Allow laying a base token on a given location.
     */
    public LayToken(List<MapHex> locations) {
        this.locations = locations;
        if (locations != null) {
            this.locations = locations;
            buildLocationNameString();
        }

    }

    public LayToken(SpecialBaseTokenLay specialProperty) {
        this.locations = specialProperty.getLocations();
        if (locations != null) buildLocationNameString();
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
    }
    
    public LayToken(SpecialBonusTokenLay specialProperty) {
        this.locations = specialProperty.getLocations();
        if (locations != null) buildLocationNameString();
        this.specialProperty = specialProperty;
        this.specialPropertyId = specialProperty.getUniqueId();
    }

    public LayToken (MapHex hex) {
    	this.locations = new ArrayList<MapHex>(1);
    	locations.add(hex);
        buildLocationNameString();
    }

    /**
     * @return Returns the chosenHex.
     */
    public MapHex getChosenHex() {
        return chosenHex;
    }

    /**
     * @param chosenHex The chosenHex to set.
     */
    public void setChosenHex(MapHex chosenHex) {
        this.chosenHex = chosenHex;
        this.chosenHexName = chosenHex.getId();
    }

    /**
     * @return Returns the specialProperty.
     */
    public abstract SpecialProperty getSpecialProperty();

    /**
     * @param specialProperty The specialProperty to set.
     */
    public void setSpecialProperty(SpecialBaseTokenLay specialProperty) {
        this.specialProperty = specialProperty;
        // TODO this.specialPropertyUniqueId = specialProperty.getUniqueId();
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationNames;
    }

    private void buildLocationNameString() {
        StringBuffer b = new StringBuffer();
        for (MapHex hex : locations) {
            if (b.length() > 0) b.append(",");
            b.append(hex.getId());
        }
        locationNames = b.toString();
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false; 

        // check asOption attributes
        LayToken action = (LayToken)pa; 
        boolean options = (Objects.equal(this.locations, action.locations) || this.locations == null && action.locations.isEmpty())
                && Objects.equal(this.specialProperty, action.specialProperty)
        ;

        // finish if asOptions check
        if (asOption) return options;
        
        // check asAction attributes
        return options
            && Objects.equal(this.chosenHex, action.chosenHex)
        ;
    }

    @Override
    public String toString() {
        return super.toString() + 
                RailsObjects.stringHelper(this)
                    .addToString("locations", locations)
                    .addToString("specialProperty", specialProperty)
                    .addToStringOnlyActed("chosenHex", chosenHex)
                    .toString()
        ;
    }
}
