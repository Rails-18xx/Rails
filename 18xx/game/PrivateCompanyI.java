/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PrivateCompanyI.java,v 1.3 2005/05/06 15:55:29 evos Exp $
 * 
 * Created on 19mar2005 by Erik Vos
 * Changes:
 *
 */
package game;

/**
 * @author Erik Vos
 */
public interface PrivateCompanyI extends CompanyI, Certificate {
	
	public static final String TYPE_TAG = "Private";
	
	public static final String BASE_PRICE = "basePrice";
	
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
	 * @return
	 */
	public Portfolio getHolder();

	public void setHolder(Portfolio portfolio);
	
	public void payOut ();


}
