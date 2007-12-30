/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyCertificate.java,v 1.6 2007/12/30 14:25:12 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.Bank;
import rails.game.Portfolio;
import rails.game.PublicCertificate;
import rails.game.PublicCertificateI;

/**
 * @author Erik Vos
 */
public class BuyCertificate extends PossibleAction {
    
    // Server-side settings
    transient protected PublicCertificateI certificate;
    protected String certUniqueId;
    transient protected Portfolio from;
    protected String fromName;
    protected int price;
    protected int maximumNumber;
    
    // Client-side settings
    protected int numberBought = 0;

    public static final long serialVersionUID = 1L;
    
    /**
     * Common constructor.
     */
    public BuyCertificate(PublicCertificateI certificate, Portfolio from,
    		int price, int maximumNumber) {
        this.certificate = certificate;
        this.certUniqueId = certificate.getUniqueId(); // TODO: Must be replaced by a unique Id!
        this.from = from;
        this.fromName = from.getName();
        this.price = price;
        this.maximumNumber = maximumNumber;
    }
    
    /** Buy a certificate from some portfolio at the current price */
    public BuyCertificate (PublicCertificateI certificate, Portfolio from) {
    	this (certificate, from, certificate.getCertificatePrice(),	1);
    }

    /** Buy a certificate from some portfolio at a given price */
    public BuyCertificate (PublicCertificateI certificate, Portfolio from,
    		int price) {
    	this (certificate, from, price,	1);
    }
    
    /** Required for deserialization */
    private BuyCertificate() {}

    /**
     * @return Returns the maximumNumber.
     */
    public int getMaximumNumber() {
        return maximumNumber;
    }
    /**
     * @return Returns the price.
     */
    public int getPrice() {
        return price;
    }
    
    /**
     * @return Returns the certificate.
     */
    public PublicCertificateI getCertificate() {
        return certificate;
    }
    
    public int getNumberBought() {
		return numberBought;
	}

	public void setNumberBought(int numberBought) {
		this.numberBought = numberBought;
	}
    
    public boolean equals (PossibleAction action) {
        if (!(action instanceof BuyCertificate)) return false;
        BuyCertificate a = (BuyCertificate) action;
        return a.certificate == certificate
            && a.from == from
            && a.price == price
            && a.maximumNumber == maximumNumber;
    }

	public String toString() {
		StringBuffer text = new StringBuffer(); 
        text.append("BuyCertificate: ");
        if (numberBought > 1) {
        	text.append (numberBought).append(" of ");
        } else if (numberBought == 0 && maximumNumber > 1) {
        	text.append ("up to ").append(maximumNumber).append(" of ");
        }
        text.append(certificate.getName())
        	.append(" from ").append(from.getName())
        	.append(" price=").append(Bank.format(certificate.getShares() * price));
        return text.toString();
    }
	
	private void readObject (ObjectInputStream in) 
			throws IOException, ClassNotFoundException {
		
		in.defaultReadObject();
		certificate = PublicCertificate.getByUniqueId (certUniqueId);
		from = Portfolio.getByName(fromName);
		
	}
}
