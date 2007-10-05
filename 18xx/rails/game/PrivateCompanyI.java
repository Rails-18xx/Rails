/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompanyI.java,v 1.3 2007/10/05 22:02:28 evos Exp $ */
package rails.game;

import java.util.List;

import rails.game.special.SpecialPropertyI;

public interface PrivateCompanyI extends CompanyI, Certificate
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

	public void setHolder(Portfolio portfolio);

	public void payOut();

	public List<SpecialPropertyI> getSpecialProperties();
}
