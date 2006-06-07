package game;

public class PublicCertificate implements PublicCertificateI, Cloneable
{

	/** From which public company is this a certificate */
	protected PublicCompanyI company;
	/**
	 * Share percentage represented by this certificate
	 * 
	 * @deprecated
	 */
	protected int shares;
	/** President's certificate? */
	protected boolean president;
	/** Availability */
	protected boolean available;
	/** Current holder of the certificate */
	protected Portfolio portfolio;

	public PublicCertificate(int shares)
	{
		this(shares, false, true);
	}

	public PublicCertificate(int shares, boolean president)
	{
		this(shares, president, true);
	}

	public PublicCertificate(int shares, boolean president, boolean available)
	{
		this.shares = shares;
		this.president = president;
		this.available = available;
	}

	/**
	 * @return if Certificate is Available
	 */
	public boolean isAvailable()
	{
		return available;
	}

	/**
	 * @return Portfolio this certificate belongs to.
	 */
	public Portfolio getPortfolio()
	{
		return portfolio;
	}

	/**
	 * @return if this is a president's share
	 */
	public boolean isPresidentShare()
	{
		return president;
	}

	/**
	 * Get the number of shares that this certificate represents.
	 * 
	 * @return The number of shares.
	 */
	public int getShares()
	{
		return shares;
	}

	/**
	 * Get the percentage of ownership that this certificate represents. This is
	 * equal to the number of shares * the share unit.
	 * 
	 * @return The share percentage.
	 */
	public int getShare()
	{
		return shares * company.getShareUnit();
	}

	/**
	 * Get the current price of this certificate.
	 * 
	 * @return The current certificate price.
	 */
	public int getCertificatePrice()
	{
		if (company.getCurrentPrice() != null)
		{
			return company.getCurrentPrice().getPrice() * shares;
		}
		else
		{
			return 0;
		}
	}

	public String getName()
	{
		return company.getName() + " " + getShare() + "% "
				+ (president ? "president " : "") + "share";
	}

	/**
	 * @param b
	 */
	public void setAvailable(boolean b)
	{
		available = b;
	}

	/**
	 * @param portfolio
	 */
	public void setPortfolio(Portfolio portfolio)
	{
		this.portfolio = portfolio;
	}

	/**
	 * @param b
	 */
	public void setPresident(boolean b)
	{
		president = b;
	}

	/**
	 * @return
	 */
	public PublicCompanyI getCompany()
	{
		return company;
	}

	/**
	 * @param companyI
	 */
	public void setCompany(PublicCompanyI companyI)
	{
		company = companyI;
	}

	protected Object clone()
	{
		try
		{
			return super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			System.out.println("Cannot clone certificate:\n"
					+ e.getStackTrace());
			return null;
		}
	}

	public PublicCertificateI copy()
	{
		return (PublicCertificateI) this.clone();
	}
	
	/**
	 * Two certificates are "equal" if they both belong to the same company, 
	 * represent the same share percentage, and are not a preseident share. 
	 * @param cert public company certificate to compare with. 
	 * @return True if the certs are "equal" in the defined sense.
	 */
	public boolean equals (PublicCertificateI cert) {
	    return (cert != null
	            && getCompany() == cert.getCompany()
	            && isPresidentShare() == cert.isPresidentShare()
	            && getShares() == cert.getShares());
	}

	public String toString()
	{
		return "PublicCertificate: " + company.getName() + ", Shares: "
				+ shares;
	}
}
