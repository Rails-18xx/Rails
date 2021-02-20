package net.sf.rails.game.specific._18Chesapeake;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.sf.rails.common.GameData;
import net.sf.rails.common.GameInfo;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.model.CountingMoneyModel;
import net.sf.rails.game.state.BooleanState;

public class StartItem_18Chesapeake extends StartItem {

    public StartItem_18Chesapeake(RailsItem parent, String id, String type, int index, boolean president) {
        super(parent, id, type, index, president);
        // TODO Auto-generated constructor stub
    }


    /**
     * @param name      The Company name of the primary certificate. This name will
     *                  also become the name of the start item itself.
     * @param type      The CompanyType name of the primary certificate.
     * @param president True if the primary certificate is the president's
     *                  share.
     * @return a fully intialized StartItem
     */
    public static StartItem_18Chesapeake create(RailsItem parent, String name, String type, int price, boolean reduceable, int index, boolean president) {
        StartItem_18Chesapeake item = new StartItem_18Chesapeake(parent, name, type, index, president);
        item.initBasePrice(price);
        item.setReducePrice(reduceable);
        return item;
    }


    @Override
    protected void assignStartSubItem(GameManager gameManager, BankPortfolio ipo, BankPortfolio unavailable, CompanyManager compMgr, String name2, boolean president2) {

    Company company2;

        if (name2.equalsIgnoreCase("random")) {
            //Randomization for 18Chesapeake
            List<PublicCompany> rList18CH;
            rList18CH = compMgr.getAllPublicCompanies();
            Random randomStart = gameManager.getRandomGenerator();

            String rname = rList18CH.get(randomStart.nextInt(6)).getId();

            company2 = compMgr.getCompany(type2, rname);
        } else {
            company2 = compMgr.getCompany(type2, name2);
        }
        secondary =
                ipo.getPortfolioModel().findCertificate((PublicCompany) company2,
                        president2);
        PublicCertificate pubcert2 = (PublicCertificate) secondary;
        if (pubcert2.getOwner() != unavailable) {
            pubcert2.moveTo(unavailable);
        }
    }


    @Override
    public void init(GameManager gameManager) {
        super.init(gameManager);
    }

}
