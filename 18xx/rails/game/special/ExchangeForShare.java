/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/ExchangeForShare.java,v 1.16 2010/02/04 21:27:59 evos Exp $ */
package rails.game.special;

import rails.game.*;
import rails.util.*;

public class ExchangeForShare extends SpecialProperty {

    /** The public company of which a share can be obtained. */
    String publicCompanyName;
    PublicCompanyI publicCompany;
    /** The share size */
    int share;

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag swapTag = tag.getChild("ExchangeForShare");
        if (swapTag == null) {
            throw new ConfigurationException("<ExchangeForShare> tag missing");
        }

        publicCompanyName = swapTag.getAttributeAsString("company");
        if (!Util.hasValue(publicCompanyName))
            throw new ConfigurationException(
                    "ExchangeForShare: company name missing");
        share = swapTag.getAttributeAsInteger("share", 10);
    }

    public boolean isExecutionable() {

        return privateCompany.getPortfolio().getOwner() instanceof Player;
    }

    /**
     * @return Returns the privateCompany.
     */
    public PrivateCompanyI getPrivateCompany() {
        return privateCompany;
    }

    /**
     * @return Returns the publicCompanyName.
     */
    public String getPublicCompanyName() {
        return publicCompanyName;
    }

    /**
     * @return Returns the share.
     */
    public int getShare() {
        return share;
    }

    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return "Swap " + privateCompany.getName() + " for " + share
               + "% share of " + publicCompanyName;
    }

    @Override
    public String toMenu() {
        return LocalText.getText("SwapPrivateForCertificate",
                privateCompany.getName(),
                share,
                publicCompanyName );
    }
    
    public String getInfo() {
        return toMenu();
    }
}
