/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Certificate.java,v 1.5 2005/05/12 22:22:28 evos Exp $
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
	 * @return
	 */
	public Portfolio getPortfolio();
	
	public String getName();

}
