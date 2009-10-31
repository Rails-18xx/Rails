/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SellBonusToken.java,v 1.2 2009/10/31 17:08:26 evos Exp $ */
package rails.game.special;

import java.util.List;

import rails.game.*;
import rails.game.state.State;
import rails.util.Tag;
import rails.util.Util;

public class SellBonusToken extends SpecialProperty {

    private String locationCodes = null;
    private List<MapHex> locations = null;
    //private PublicCompanyI seller = null;
    private State seller = null;
    private String name;
    private int price;
    private int value;
    private int maxNumberToSell;
    private int numberSold = 0;

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

        maxNumberToSell = sellBonusTokenTag.getAttributeAsInteger("number", 1);

        seller = new State ("SellerOf_"+name+"_Bonus", CashHolder.class);
    }

    public void finishConfiguration (GameManager gameManager) 
    throws ConfigurationException {
        
        locations = gameManager.getMapManager().parseLocations(locationCodes);
    }

     @Override
	public void setExercised () {
    	numberSold++;
    	if (maxNumberToSell >= 0 && numberSold >= maxNumberToSell) {
    		super.setExercised();
    	}
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
		return (CashHolder) seller.getObject();
	}

	public void setSeller(CashHolder seller) {
		this.seller.set(seller);
	}

	@Override
	public String toString() {
        return "SellBonusToken comp=" + privateCompany.getName() + " hex="
               + locationCodes + " value=" + value + " price=" + price;
    }

}
