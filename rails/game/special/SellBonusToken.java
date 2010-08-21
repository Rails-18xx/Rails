/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SellBonusToken.java,v 1.8 2010/05/18 21:36:12 stefanfrey Exp $ */
package rails.game.special;

import java.util.List;

import rails.game.*;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.util.Tag;
import rails.util.Util;

public class SellBonusToken extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;
    private State seller = null;
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

        seller = new State ("SellerOf_"+name+"_Bonus", CashHolder.class);
        
        numberSold = new IntegerState ("Bonus_"+name+"_sold", 0);
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager) 
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

    public String getName() {
        return name;
    }

    public int getPrice() {
        return price;
    }

    public int getValue() {
        return value;
    }

    public CashHolder getSeller() {
        return (CashHolder) seller.get();
    }

    public void setSeller(CashHolder seller) {
        this.seller.set(seller);
    }

    @Override
    public String toString() {
        return "SellBonusToken comp=" + originalCompany.getName() + " hex="
               + locationCodes + " value=" + value + " price=" + price
               + " max="+maxNumberToSell+" sold="+numberSold.intValue();
    }

}
