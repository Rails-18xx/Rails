/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Certificate.java,v 1.1 2005/04/16 22:51:22 evos Exp $
 * 
 * Created on 09-Apr-2005 by Erik Vos
 * 
 * Change Log:
 */
package game;

/**
 * @author Erik
 */
public class Certificate implements CertificateI, Cloneable {
	
	/** From which public company is this a certificate */
	protected PublicCompanyI company;
	/** Share percentage represented by this certificate */
	protected int share;
	/** President's certificate? */
	protected boolean president;
	/** Availability */
	protected boolean available;
	/** Holder of the certificate */
	protected Portfolio portfolio;
	
	public Certificate (int share) {
		this (share, false, true); 
	}
	
	public Certificate (int share, boolean president) {
		this (share, president, true);
	}
	
	public Certificate (int share, boolean president, boolean available) {
		this.share = share;
		this.president = president;
		this.available = available;
	}
	
	

	/**
	 * @return
	 */
	public boolean isAvailable() {
		return available;
	}

	/**
	 * @return
	 */
	public Portfolio getPortfolio() {
		return portfolio;
	}

	/**
	 * @return
	 */
	public boolean isPresident() {
		return president;
	}

	/**
	 * @return
	 */
	public int getShare() {
		return share;
	}

	/**
	 * @param b
	 */
	public void setAvailable(boolean b) {
		available = b;
	}

	/**
	 * @param portfolio
	 */
	public void setPortfolio(Portfolio portfolio) {
		this.portfolio = portfolio;
	}

	/**
	 * @param b
	 */
	public void setPresident(boolean b) {
		president = b;
	}

	/**
	 * @param i
	 */
	public void setShare(int i) {
		share = i;
	}

	/**
	 * @return
	 */
	public PublicCompanyI getCompany() {
		return company;
	}

	/**
	 * @param companyI
	 */
	public void setCompany(PublicCompanyI companyI) {
		company = companyI;
	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			System.out.println("Cannot clone certificate:\n" + e.getStackTrace());
			return null;
		}
	}
	
	public CertificateI copy() {
		return (CertificateI) this.clone();
	}
}
