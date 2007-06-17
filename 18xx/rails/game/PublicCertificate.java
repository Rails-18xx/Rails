package rails.game;

import org.apache.log4j.Logger;

import rails.util.LocalText;


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
	
	/** A key identifying the certificate's unique type */
	protected String certTypeId;

	protected static Logger log = Logger.getLogger(PublicCertificate.class.getPackage().getName());

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
	//public boolean isAvailable()
	//{
	//	return available;
	//}

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

	/** Get the name of a certificate. 
	 The name is derived from the company name and the 
	 share percentage of this certificate.
	 If it is a 100% share (as occurs with e.g. 1835 minors),
	 only the company name is given.
	 If it is a president's share, that fact is mentioned.
	 */
	public String getName()
	{
		int share = getShare();
		if (share == 100) {
			/* Applies to shareless minors: just name the company */
			return company.getName();
		} else if (president){
			return LocalText.getText("PRES_CERT_NAME", new String[] {
					company.getName(),
					String.valueOf(getShare())
			});
		} else {
			return LocalText.getText("CERT_NAME", new String[] {
					company.getName(),
					String.valueOf(getShare())
			});
		}
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
		certTypeId = company.getName()+"_"+getShares()+"%";
		if (president) certTypeId += "_P";
	}
	
	public String getTypeId () {
		return certTypeId;
	}

	protected Object clone()
	{
		try
		{
			return super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			log.fatal ("Cannot clone certificate:", e);
			return null;
		}
	}

	public PublicCertificateI copy()
	{
		return (PublicCertificateI) this.clone();
	}
	
	/**
	 * Two certificates are "equal" if they both belong to the same company, 
	 * represent the same share percentage, and are not a president share. 
	 * @param cert Public company certificate to compare with. 
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
		return "PublicCertificate: " + getName();
	}
}
