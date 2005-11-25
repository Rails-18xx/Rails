/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PrivateCompanyI.java,v 1.7 2005/11/25 22:38:25 evos Exp $
 * 
 * Created on 19mar2005 by Erik Vos
 * Changes:
 *
 */
package game;

import java.util.List;

/**
 * @author Erik Vos
 */
public interface PrivateCompanyI extends CompanyI, Certificate
{

	public static final String TYPE_TAG = "Private";
	//public static final String BASE_PRICE = "basePrice";
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

	public List getSpecialProperties();
}
