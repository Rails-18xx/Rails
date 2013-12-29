/**
 * 
 */
package rails.game.specific._1835;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.PublicCertificateI;
import rails.game.PublicCompany;


/**
 * @author Martin
 *
 */
public class PublicCompany_1835 extends PublicCompany {

    /**
     * 
     */
    public PublicCompany_1835() {
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see rails.game.PublicCompany#isSoldOut()
     */
    @Override
    public boolean isSoldOut() {
        CashHolder owner;
        String name;
        
       if (gameManager.getGameOption("PrussianReservedIgnored").equalsIgnoreCase("yes"))
        {
          for (PublicCertificateI cert : certificates) {
                owner = cert.getPortfolio().getOwner();
                name = cert.getPortfolio().getName();
                if ((owner instanceof Bank || owner == cert.getCompany()) && (!name.equalsIgnoreCase("unavailable"))) {
                    return false;
                }
            }
            return true;
       }
        return super.isSoldOut();
 }

    
}
