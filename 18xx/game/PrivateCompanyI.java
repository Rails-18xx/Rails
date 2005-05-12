/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PrivateCompanyI.java,v 1.4 2005/05/12 22:22:28 evos Exp $
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


	public void setHolder(Portfolio portfolio);
	
	public void payOut ();


}
