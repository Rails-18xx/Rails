/**
 * 
 */
package net.sf.rails.game.specific._1880;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartPacket;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.state.ChangeStack;
import rails.game.*;
import rails.game.action.*;

/**
 * @author Martin
 * 
 */
public class StartRound_Investors_1880 extends StartRound {

    int investorsPurchased = 0;

    /**
     * @param gameManager
     */
    public StartRound_Investors_1880(GameManager gameManager, String id) {
        super(gameManager, id);
        hasBasePrices = true;
        hasBidding = false;
        hasBuying = true;
    }

    @Override
    public void start(StartPacket startPacket) {
        super.start(startPacket);

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

        transferCertificate(certificate, player.getPortfolioModel());
        boughtItem.getStartItem().setSold(player, 0);
        investor.start();
        investor.setFloated();
        
        setCurrentPlayerIndex(getCurrentPlayerIndex() + 1);
        
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
        
        investorsPurchased += 1;
        if (investorsPurchased == getRoot().getPlayerManager().getNumberOfPlayers()) {
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
                    item.getName(),
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
