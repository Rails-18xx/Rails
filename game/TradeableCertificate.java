/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/Attic/TradeableCertificate.java,v 1.1 2006/05/30 21:50:38 evos Exp $
 * 
 * Created on 20-May-2006
 * Change Log:
 */
package game;

/**
 * @author Erik Vos
 */
public class TradeableCertificate {

    private PublicCertificateI cert;
    private int price = 0;
    private int number = 0;
    
    public TradeableCertificate (PublicCertificateI cert, int price) {
        this.cert = cert;
        this.price = price;
    }
    
     
    public PublicCertificateI getCert() {
        return cert;
    }
    
    public int getPrice () {
        return price;
    }
    
    public int getNumber () {
        return number;
    }
    
     
    public Portfolio getPortfolio () {
        return cert.getPortfolio();
    }
    
    public CashHolder getOwner () {
        return cert.getPortfolio().getOwner();
    }
    

}
