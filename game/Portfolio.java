/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/Portfolio.java,v 1.2 2005/04/16 23:03:08 evos Exp $
 *
 * Created on 09-Apr-2005 by Erik Vos
 *
 * Change Log:
 */
package game;

import java.util.*;

/**
 * @author Erik
 */
public class Portfolio {

	/** Owned private companies */
	protected ArrayList privateCompanies = new ArrayList();

	/** Owned public company certificates */
	protected ArrayList certificates = new ArrayList();
	/** Owned public company certificates, organised in a HashMap per company */
	protected HashMap certPerCompany = new HashMap();

	/** Who owns the portfolio */
	protected CashHolder owner;
	/** Who receives the dividends (may differ from owner if that is the Bank) */
	protected boolean paysToCompany = false;

	/** Name of portfolio */
	protected String name;

	public Portfolio (String name, CashHolder holder, boolean paysToCompany) {
		this.name = name;
		this.owner = holder;
		this.paysToCompany = paysToCompany;
	}

	public Portfolio (String name, CashHolder holder) {
		this.name = name;
		this.owner = holder;
	}

	public void buyPrivate (PrivateCompanyI privateCompany, Portfolio from, int price) {

		Log.write(getName()+" buys "+privateCompany.getName()
			+" from "+from.getName()+" for "+price);

		// Move the private certificate
		from.removePrivate (privateCompany);
		this.addPrivate (privateCompany);
		privateCompany.setHolder (this);

		// Move the money
		Bank.transferCash(owner, from.owner, price);
	}

	public void buyCertificate (CertificateI certificate, Portfolio from, int price) {

		Log.write (getName()+" buys "+certificate.getShare()+"% of "
			+certificate.getCompany().getName()+" from "+from.getName()
			+ " for "+price);

		// Move the certificate
		from.removeCertificate (certificate);
		this.addCertificate (certificate);
		certificate.setPortfolio (this);

		// Move the money
		Bank.transferCash(owner, from.owner, price);
	}


	public void addPrivate (PrivateCompanyI privateCompany) {
		privateCompanies.add(privateCompany);
		privateCompany.setHolder(this);
	}

	public void addCertificate (CertificateI certificate) {
		certificates.add(certificate);
		String companyName = certificate.getCompany().getName();
		if (!certPerCompany.containsKey(companyName)) {
			certPerCompany.put(companyName, new ArrayList());
		}
		((ArrayList)certPerCompany.get(companyName)).add(certificate);
		certificate.setPortfolio(this);
	}

	public boolean removePrivate (PrivateCompanyI privateCompany) {
		for (int i=0; i<privateCompanies.size(); i++) {
			if (privateCompanies.get(i) == privateCompany) {
				privateCompanies.remove(i);
				return true;
			}
		}
		return false;
	}

	public void removeCertificate (CertificateI certificate) {
		for (int i=0; i<certificates.size(); i++) {
			if (certificates.get(i) == certificate) {
				certificates.remove(i);
			}
		}
		String companyName = certificate.getCompany().getName();
		ArrayList certs = (ArrayList)certPerCompany.get(companyName);
		for (int i=0; i<certs.size(); i++) {
			if (certs.get(i) == certificate) {
				certs.remove(i);
			}
		}

	}

	public List getPrivateCompanies() {
		return privateCompanies;
	}

	public List getCertificates() {
		return certificates;
	}

	public List getCertificatesPerCompany (String compName) {
		if (certPerCompany.containsKey(compName)) {
			return (List) certPerCompany.get(compName);
		} else {
			return new ArrayList();
		}
	}

	/** Find any certificate */
	public CertificateI findCertificate (PublicCompanyI company, boolean president) {
		String companyName = company.getName();
		if (!certPerCompany.containsKey(companyName)) return null;
		Iterator it = ((List)certPerCompany.get(companyName)).iterator();
		CertificateI cert;
		while (it.hasNext()) {
			 cert = (CertificateI) it.next();
			 if (cert.getCompany() == company && president == cert.isPresident()) {
			 	  return cert;
			 }
		}
		return null;
	}


	/**
	 * @return
	 */
	public CashHolder getBeneficiary(PublicCompanyI company) {
		if (paysToCompany) {
			return (CashHolder) company;
		} else {
			return owner;
		}
	}

	/**
	 * @return
	 */
	public CashHolder getOwner() {
		return owner;
	}

	/**
	 * @param object
	 */
	public void setOwner(CashHolder owner) {
		this.owner = owner;
	}

	/**
	 * @return
	 */
	public HashMap getCertPerCompany() {
		return certPerCompany;
	}

	/**
	 * @return
	 */
	public String getName() {
		return name;
	}

	public int countShares (PublicCompanyI company) {
		int share = 0;
		String name = company.getName();
		if (certPerCompany.containsKey(name)) {
			Iterator it = ((List)certPerCompany.get(name)).iterator();
			while (it.hasNext()) {
				share += ((CertificateI)it.next()).getShare();
			}
		}
		return share;
	}

}
