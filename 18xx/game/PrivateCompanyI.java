/*
 * $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/PrivateCompanyI.java,v 1.1 2005/03/20 23:52:23 wakko666 Exp $
 * 
 * Created on 19mar2005 by Erik Vos
 * Changes:
 *
 */
package game;

/**
 * @author Erik Vos
 */
public interface PrivateCompanyI extends CompanyI {
	
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

}
