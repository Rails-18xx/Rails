package game;

public interface PublicCertificateI extends Certificate
{

	/**
	 * @return if company is available
	 */
	public boolean isAvailable();

	/**
	 * @return if this certificate is a president's share
	 */
	public boolean isPresidentShare();

	/**
	 * Get the number of shares that this certificate represents.
	 * 
	 * @return The number of shares.
	 */
	public int getShares();

	/**
	 * Get the percentage of ownership that this certificate represents. This is
	 * equal to the number of shares * the share unit.
	 * 
	 * @return The share percentage.
	 */
	public int getShare();

	/**
	 * Get the current price of this certificate.
	 * 
	 * @return The current certificate price.
	 */
	public int getCertificatePrice();

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
	 * @return
	 */
	public PublicCompanyI getCompany();

	/**
	 * @param companyI
	 */
	public void setCompany(PublicCompanyI companyI);

	/** Clone this certificate */
	public PublicCertificateI copy();
	
	/** Compare certificates 
	 * 
	 * @param cert Another publoc certificate.
	 * @return TRUE if the certificates are of the same company and
	 * represent the same number of shares.
	 */
	public boolean equals (PublicCertificateI cert);

}
