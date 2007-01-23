package rails.game;

/**
 * The superinterface of PrivateCompanyI and PublicCertificateI,
 * which allows objects implementating these interfaces to be
 * combined in start packets and other contexts where their
 * "certificateship" is of interest. 
 */
public interface Certificate {
    
	/**
	 * @return Portfolio
	 */
	public Portfolio getPortfolio();
	
	public String getName();

}
