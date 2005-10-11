/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Certificate.java,v 1.6 2005/10/11 17:35:29 wakko666 Exp $
 * 
 * Created on 06-May-2005
 * Change Log:
 */
package game;

/**
 * The superinterface of PrivateCompanyI and PublicCertificateI,
 * which allows objects implementating these interfaces to be
 * combined in start packets and other contexts where their
 * "certificateship" is of interest. 
 * @author Erik Vos
 */
public interface Certificate {
    
	/**
	 * @return Portfolio
	 */
	public Portfolio getPortfolio();
	
	public String getName();

}
