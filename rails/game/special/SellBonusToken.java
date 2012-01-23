package rails.game.special;

import java.util.List;

import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.GameManager;
import rails.game.MapHex;
import rails.game.model.Owner;
import rails.game.state.GenericState;
import rails.game.state.IntegerState;
import rails.util.Util;

public class SellBonusToken extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;
    private GenericState<Owner> seller = null;
    private String name;
    private int price;
    private int value;
    private int maxNumberToSell;
    private IntegerState numberSold;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        Tag sellBonusTokenTag = tag.getChild("SellBonusToken");
        if (sellBonusTokenTag == null) {
            throw new ConfigurationException("<SellBonusToken> tag missing");
        }

        locationCodes = sellBonusTokenTag.getAttributeAsString("location");
        if (!Util.hasValue(locationCodes))
            throw new ConfigurationException("SellBonusToken: location missing");

        name = sellBonusTokenTag.getAttributeAsString("name");

        value = sellBonusTokenTag.getAttributeAsInteger("value", 0);
        if (value <= 0)
            throw new ConfigurationException("Value invalid ["+value+"] or missing");

        price = sellBonusTokenTag.getAttributeAsInteger("price", 0);
        if (price <= 0)
            throw new ConfigurationException("Price invalid ["+price+"] or missing");

        maxNumberToSell = sellBonusTokenTag.getAttributeAsInteger("amount", 1);

        seller = GenericState.create(this, "SellerOf_"+name+"_Bonus");
        
        numberSold = IntegerState.create(this, "Bonus_"+name+"_sold", 0);
    }

    @Override
    public void finishConfiguration (GameManager gameManager) 
    throws ConfigurationException {
        
        locations = gameManager.getMapManager().parseLocations(locationCodes);
        
        
    }

     @Override
    public void setExercised () {
        numberSold.add(1);
    }
     
    public void makeResellable () {
        numberSold.add(-1);
    }
    
    @Override
    public boolean isExercised () {
        return maxNumberToSell >= 0 && numberSold.intValue() >= maxNumberToSell;
    }
    
    public boolean isExecutionable() {
        return true;
    }

    public List<MapHex> getLocations() {
        return locations;
    }

    public String getLocationNameString() {
        return locationCodes;
    }

    public String getId() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getValue() {
        return value;
    }

    public Owner getSeller() {
        return seller.get();
    }

    public void setSeller(Owner seller) {
        this.seller.set(seller);
    }

    @Override
    public String toString() {
        return "SellBonusToken comp=" + originalCompany.getId() + " hex="
               + locationCodes + " value=" + value + " price=" + price
               + " max="+maxNumberToSell+" sold="+numberSold.intValue();
    }

}
