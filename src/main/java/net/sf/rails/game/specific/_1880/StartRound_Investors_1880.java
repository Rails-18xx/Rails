/**
 * 
 */
package net.sf.rails.game.specific._1880;

import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.IntegerState;

/**
 * @author Martin
 * 
 * Rails 2.0: Ok
 * 
 */
public class StartRound_Investors_1880 extends StartRound {

    private final IntegerState investorsPurchased = 
            IntegerState.create(this, "investorsPurchased");

    public StartRound_Investors_1880(GameManager gameManager, String id) {
        super(gameManager, id, false, true, true);
        // no bidding involved
    }

    @Override
    public void start() {
        super.start();

        for (StartItem item : startPacket.getItems()) {
            item.setStatus(StartItem.BUYABLE);
        }
        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {
        for (StartItem item : startPacket.getItems()) {
            if (item.isSold() == false) {
                possibleActions.add(new BuyStartItem(item, 0, false));
            }
        }
        return true;
    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        Player player = getRoot().getPlayerManager().getPlayerByName(playerName);
        PublicCertificate certificate = (PublicCertificate) boughtItem.getStartItem().getPrimary();
        Investor_1880 investor = (Investor_1880) certificate.getCompany();
                
        if (validateBuy(playerName, boughtItem) == false) {
            return false;
        }

        certificate.moveTo(player);
        boughtItem.getStartItem().setSold(player, 0);
        investor.start();
        investor.setFloated();
        
        
        playerManager.setCurrentToNextPlayer();
        
        ReportBuffer.add(this, LocalText.getText("ChoosesInvestor",
                player.getId(),
                investor.getId() ));
        
        // If this player is the owner of the BCR, link it to this investor
        PublicCompany_1880 bcr =(PublicCompany_1880) companyManager.getPublicCompany("BCR");
        if (bcr.getPresident() == player) {
            PublicCertificate bcrCertificate = ipo.findCertificate(bcr, 1, false);
            bcrCertificate.moveTo(investor.getPortfolioModel());
            investor.setLinkedCompany(bcr);
        }        
        
        investorsPurchased.add(1);
        if (investorsPurchased.value() == playerManager.getNumberOfPlayers()) {
            for (StartItem item : startPacket.getItems()) {
                if (item.isSold() == false) {
                    item.setStatus(StartItem.SOLD);
                }
            }
        }
        return true;
    }
    
    
    private boolean validateBuy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();

        if (item.getStatus() != StartItem.BUYABLE) {
            DisplayBuffer.add(this,LocalText.getText("CantBuyItem",
                    playerName,
                    item.getId(),
                    LocalText.getText("NotForSale") ));
            return false;
        }
        return true;
    }

    @Override
    protected void finishRound() {
        for (Investor_1880 investor : Investor_1880.getInvestors(companyManager)) {
            if (investor.getPresident() == null) {
                investor.setClosed();
            }
        }

        super.finishRound();
    }

    // Should never be called...
    @Override
    protected boolean bid(String playerName, BidStartItem startItem) {
        return false;
    }

    // Should never be called...
    @Override
    protected boolean pass(NullAction action, String playerName) {
        return false;
    }

}
