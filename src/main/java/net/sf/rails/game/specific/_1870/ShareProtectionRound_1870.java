// --- START FIX ---
package net.sf.rails.game.specific._1870;

import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Round;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.StringState;
import rails.game.action.BuyCertificate;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;


public class ShareProtectionRound_1870 extends Round {

    private final StringState protectedCompanyId = StringState.create(this, "protectedCompanyId");
    private final StringState sellerName = StringState.create(this, "sellerName");
    private final IntegerState sharesSold = IntegerState.create(this, "sharesSold");

    public ShareProtectionRound_1870(GameManager gameManager, String id) {
        super(gameManager, id);
    }

    public void setProtectionContext(PublicCompany company, Player seller, int sold) {
        this.protectedCompanyId.set(company.getId());
        this.sellerName.set(seller.getName());
        this.sharesSold.set(sold);
    }

    @Override
    public boolean setPossibleActions() {
        boolean result = super.setPossibleActions();
        PublicCompany company = getRoot().getCompanyManager().getPublicCompany(protectedCompanyId.value());
        
        if (company != null) {
            Player president = company.getPresident();
            if (president != null) {
                int price = company.getCurrentSpace().getPrice() / company.getShareUnitsForSharePrice();
                // Buy from Bank Pool: (Company, Size per cert, From Owner, Price, Quantity)
                BuyCertificate buy = new BuyCertificate(company, company.getShareUnit(), pool.getParent(), price, sharesSold.value());
                possibleActions.add(buy);
            }
        }
        
        // Action 2: Pass (Let it drop)
        possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
        return result;
    }

    @Override
    public boolean process(PossibleAction action) {
        PublicCompany company = getRoot().getCompanyManager().getPublicCompany(protectedCompanyId.value());
        Player president = company.getPresident();
        Round interrupted = (Round) getRoot().getGameManager().getInterruptedRound();
        StockRound_1870 sr = (interrupted instanceof StockRound_1870) ? (StockRound_1870) interrupted : null;

        if (action instanceof BuyCertificate) {
            BuyCertificate buyAction = (BuyCertificate) action;
            int totalCost = buyAction.getNumberBought() * buyAction.getPrice();
            
            ReportBuffer.add(this, "=> SHARE CATCH: " + president.getName() + " protects the share price of " + company.getId() + " by buying the " + sharesSold.value() + " sold share(s) for " + getRoot().getBank().getCurrency().format(totalCost) + ".");
            ReportBuffer.add(this, "=> " + company.getId() + " share price remains at " + getRoot().getBank().getCurrency().format(buyAction.getPrice()) + ".");
            
            int count = sharesSold.value();
            Iterable<PublicCertificate> poolCerts = pool.getCertificates(company);
            for (PublicCertificate cert : poolCerts) {
                if (count > 0 && !cert.isPresidentShare()) {
                    cert.moveTo(president);
                    count--;
                }
            }
            
            Currency.wire(president, totalCost, getRoot().getBank());
            
            if (sr != null) {
                sr.setNextPlayerAfterProtection(president);
            }
            
            finishRound();
            return true;
            
        } else if (action instanceof NullAction && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            ReportBuffer.add(this, "=> DECLINED: " + president.getName() + " declines to protect " + company.getId() + ".");
            ReportBuffer.add(this, "=> The share price of " + company.getId() + " drops normally.");
            
            if (sr != null) {
                sr.processPassedProtection(company, sharesSold.value());
            }
            
            finishRound();
            return true;
        }
        return super.process(action);
    }

    @Override
    public String getRoundName() {
        return "Share Protection";
    }

    public void start() {
        setPossibleActions();
    }
}
// --- END FIX ---