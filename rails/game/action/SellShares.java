/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/SellShares.java,v 1.2 2007/06/18 19:53:43 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.Bank;
import rails.game.Game;
import rails.game.PublicCompanyI;

/**
 * @author Erik Vos
 */
public class SellShares extends PossibleAction {
    
    private final String companyName;
    private final PublicCompanyI company;
    private final int shareUnit;
    private final int shareUnits;
    private final int share;
    private final int price;
    private final int maximumNumber;
    
    private int numberSold = 0;

    /**
     * 
     */
    public SellShares(String companyName, int shareUnits,
    		int maximumNumber, int price) {
        this.companyName = companyName;
        this.shareUnits = shareUnits;
        this.price = price;
        this.maximumNumber = maximumNumber;
        
        company = Game.getCompanyManager().getPublicCompany(companyName);
        shareUnit = company.getShareUnit();
        share = shareUnits * shareUnit;
    }
    
    /**
     * @return Returns the maximumNumber.
     */
    public int getMaximumNumber() {
        return maximumNumber;
    }
    /**
     * @return Returns the price.
     */
    public int getPrice() {
        return price;
    }
    /**
     * @return Returns the companyName.
     */
    public String getCompanyName() {
        return companyName;
    }
    
    public PublicCompanyI getCompany () {
    	return Game.getCompanyManager().getPublicCompany(companyName);
    }
    
    public int getShareUnits() {
		return shareUnits;
	}

    public int getShareUnit() {
		return shareUnit;
	}

	public int getShare() {
		return share;
	}

	public int getNumberSold() {
		return numberSold;
	}

	public void setNumberSold(int numberSold) {
		this.numberSold = numberSold;
	}
	
	public boolean equals (PossibleAction action) {
		if (!(action instanceof SellShares)) return false;
		SellShares a = (SellShares) action;
		return a.getCompanyName().equals(companyName)
			&& a.getShareUnits() ==  shareUnits
			&& a.getMaximumNumber() == maximumNumber
			&& a.getPrice() == price;
	}

	public String toString() {
        return "SellShares: "
        	+ (numberSold > 0 ? numberSold : "max " + maximumNumber)
        	+ " of " +share + "% " + companyName 
         	+ " at " + Bank.format(shareUnits * price) + " apiece";
    }
}
