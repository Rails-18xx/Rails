/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/BuyCertificate.java,v 1.1 2007/06/17 22:03:51 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.Bank;
import rails.game.Portfolio;
import rails.game.PublicCertificateI;

/**
 * @author Erik Vos
 */
public class BuyCertificate extends PossibleAction {
    
    protected PublicCertificateI certificate;
    protected Portfolio from;
    protected int price;
    protected int maximumNumber;
    
    protected int numberBought = 0;

    /**
     * Common constructor.
     */
    public BuyCertificate(PublicCertificateI certificate, Portfolio from,
    		int price, int maximumNumber) {
        this.certificate = certificate;
        this.from = from;
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

	public String toString() {
		StringBuffer text = new StringBuffer(); 
        text.append("BuyCertificate: ");
        if (maximumNumber > 1) {
        	text.append ("up to ").append(maximumNumber).append(" of");
        }
        text.append(certificate.getName())
        	.append(" from ").append(from.getName())
        	.append(" price=").append(Bank.format(certificate.getShares() * price));
        return text.toString();
    }
}
