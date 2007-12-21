/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompanyI.java,v 1.4 2007/12/21 21:18:12 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.move.MoveableHolderI;
import rails.game.special.SpecialPropertyI;

public interface PrivateCompanyI 
extends CompanyI, Certificate, MoveableHolderI
{

	public static final String TYPE_TAG = "Private";
	public static final String REVENUE = "revenue";

	/**
	 * @return
	 */
	public int getPrivateNumber();

	/**
	 * @return
	 */
	public int getBasePrice();

	/**
	 * @return
	 */
	public int getRevenue();

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties();

    public List<MapHex> getBlockedHexes();

    public void setHolder(Portfolio portfolio);

	public void payOut();

}
