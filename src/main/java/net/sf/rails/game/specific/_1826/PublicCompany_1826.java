package net.sf.rails.game.specific._1826;

import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.PublicCertificate;

public class PublicCompany_1826 extends PublicCompany {

    public PublicCompany_1826(RailsItem parent, String id) {
        super (parent, id, true);
    }

    public PublicCompany_1826(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id, hasStockPrice);
    }

    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        super.finishConfiguration(root);

        // 5-share companies have an initial share unit of 20%
        if (getType().equals("Public") && !getId().equals("B")) {
            shareUnit.set(20);
            for (PublicCertificate cert : certificates) {

            }
        }
    }
}