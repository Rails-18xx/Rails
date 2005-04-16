/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/CertificateI.java,v 1.1 2005/04/16 22:51:22 evos Exp $
 * 
 * Created on 09-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

/**
 * @author Erik
 */
public interface CertificateI {

	/**
	 * @return
	 */
	public boolean isAvailable();

	/**
	 * @return
	 */
	public Portfolio getPortfolio();

	/**
	 * @return
	 */
	public boolean isPresident();

	/**
	 * @return
	 */
	public int getShare();

	/**
	 * @param b
	 */
	public void setAvailable(boolean b);

	/**
	 * @param portfolio
	 */
	public void setPortfolio(Portfolio portfolio);

	/**
	 * @param b
	 */
	public void setPresident(boolean b);

	/**
	 * @param i
	 */
	public void setShare(int i);
	/**
	 * @return
	 */
	public PublicCompanyI getCompany();

	/**
	 * @param companyI
	 */
	public void setCompany(PublicCompanyI companyI);
	
	/** Clone this certificate */
	public CertificateI copy();

}
