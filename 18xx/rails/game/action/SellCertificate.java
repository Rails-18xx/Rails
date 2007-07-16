/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/Attic/SellCertificate.java,v 1.4 2007/07/16 20:40:20 evos Exp $
 * 
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import rails.game.Bank;
import rails.game.PublicCertificateI;

/**
 * @author Erik Vos
 */
public class SellCertificate extends PossibleAction {
    
    private PublicCertificateI certificate;
    private int price;
    private int maximumNumber;
    
    private int numberSold = 0;

    /**
     * 
     */
    public SellCertificate(PublicCertificateI certificate, int price, int maximumNumber) {
        this.certificate = certificate;
        this.price = price;
        this.maximumNumber = maximumNumber;
    }
    
    public SellCertificate (PublicCertificateI certificate) {
    	this (certificate, 
    			certificate.getCompany().getCurrentPrice().getPrice(), 
    			1);
    }

    public SellCertificate (PublicCertificateI certificate, int maximumNumber) {
    	this (certificate, 
    			certificate.getCompany().getCurrentPrice().getPrice(), 
    			maximumNumber);
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
    
    public int getNumberSold() {
		return numberSold;
	}

	public void setNumberSold(int numberSold) {
		this.numberSold = numberSold;
	}

    public boolean equals (PossibleAction action) {
        if (!(action instanceof SellCertificate)) return false;
        SellCertificate a = (SellCertificate) action;
        return a.certificate == certificate
            && a.price == price
            && a.maximumNumber == maximumNumber;
    }

	public String toString() {
        return "SellCertificate "+ certificate.getName() 
        	+ " share=" + certificate.getShare()
        	+ "% price=" + Bank.format(certificate.getShares() * price);
    }
}
