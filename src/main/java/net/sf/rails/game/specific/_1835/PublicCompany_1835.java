package net.sf.rails.game.specific._1835;

import net.sf.rails.common.GameOption;
import net.sf.rails.game.Bank;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.state.Owner;
/**
 * 
 */
public class PublicCompany_1835 extends PublicCompany {


    /**
     * @author Martin
     *
     */
    public PublicCompany_1835(RailsItem parent, String Id) {
        super(parent, Id);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see rails.game.PublicCompany#isSoldOut()
     */
    @Override
    public boolean isSoldOut() {
        Owner owner;
        String name;

        if ("yes".equalsIgnoreCase(GameOption.getValue(this, "PrussianReservedIgnored"))) {
            for (PublicCertificate cert : certificates.view()) {
                owner = cert.getOwner();
                name = cert.getName();
                if ((owner instanceof Bank || owner == cert.getCompany()) && (!name.equalsIgnoreCase("unavailable"))) {
                    return false;
                }
            }
            return true;
        }
        return super.isSoldOut();
    }

}
