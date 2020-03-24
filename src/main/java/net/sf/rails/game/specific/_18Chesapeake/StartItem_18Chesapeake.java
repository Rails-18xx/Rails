package net.sf.rails.game.specific._18Chesapeake;

import java.util.Collections;
import java.util.List;

import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
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
     * Initialisation, to be called after all XML parsing has completed, and
     * after IPO initialisation.
     */
	@Override
    public void init(GameManager gameManager) {

        List<Player> players = getRoot().getPlayerManager().getPlayers();
        for (Player p: players) {
            // TODO: Check if this is correct or that it should be initialized with zero
            CountingMoneyModel bid = CountingMoneyModel.create(this, "bidBy_" + p.getId(), false);
            bid.setSuppressZero(true);
            bids.put(p, bid);
            active.put(p, BooleanState.create(this, "active_" + p.getId()));
        }
        // TODO Leave this for now, but it should be done
        // in the game-specific StartRound class
        minimumBid.set(basePrice.value() + 5);

        BankPortfolio ipo = getRoot().getBank().getIpo();
        BankPortfolio unavailable = getRoot().getBank().getUnavailable();

        CompanyManager compMgr = getRoot().getCompanyManager();

        Company company = compMgr.getCompany(type, getId());
        if (company instanceof PrivateCompany) {
            primary = (Certificate) company;
        } else {
            primary = ipo.getPortfolioModel().findCertificate((PublicCompany) company, president);
            // Move the certificate to the "unavailable" pool.
            PublicCertificate pubcert = (PublicCertificate) primary;
            if (pubcert.getOwner() == null
                || pubcert.getOwner() != unavailable.getParent()) {
                pubcert.moveTo(unavailable);
            }
        }

        // Check if there is another certificate
        if (name2 != null) {
        	//Randomization for 18Chesapeake
        	List<PublicCompany> rList18CH;
        	rList18CH = compMgr.getAllPublicCompanies();
        	 Collections.shuffle(rList18CH);
        	String rname = rList18CH.get(0).getAlias();
            Company company2 = compMgr.getCompany(type2,rname);

                secondary =
                        ipo.getPortfolioModel().findCertificate((PublicCompany) company2,
                                president2);
                // Move the certificate to the "unavailable" pool.
                // FIXME: This is still an issue to resolve  ???
                PublicCertificate pubcert2 = (PublicCertificate) secondary;
                if (pubcert2.getOwner() != unavailable) {
                    pubcert2.moveTo(unavailable);
                }
        }

    }
	

}
