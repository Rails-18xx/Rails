/**
 * 
 */
package rails.game.specific._1880;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
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
    public StartRound_Investors_1880(GameManagerI gameManager) {
        super(gameManager);
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
                possibleActions.add(new BuyStartItem_1880(item, 0, false));
            }
        }
        return true;
    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        Player player = gameManager.getPlayerManager().getPlayerByName(playerName);
        PublicCertificate certificate = (PublicCertificate) boughtItem.getStartItem().getPrimary();
        Investor_1880 investor = (Investor_1880) certificate.getCompany();
                
        if (validateBuy(playerName, boughtItem) == false) {
            return false;
        }

        moveStack.start(false);

        transferCertificate(certificate, player.getPortfolio());
        boughtItem.getStartItem().setSold(player, 0);
        investor.start();
        investor.setFloated();
        
        setCurrentPlayerIndex(getCurrentPlayerIndex() + 1);
        
        ReportBuffer.add(LocalText.getText("ChoosesInvestor",
                player.getName(),
                investor.getName() ));
        
        // If this player is the owner of the BCR, link it to this investor
        PublicCompany_1880 bcr =(PublicCompany_1880) companyManager.getPublicCompany("BCR");
        if (bcr.getPresident() == player) {
            PublicCertificateI bcrCertificate = ipo.findCertificate(bcr, 1, false);
            bcrCertificate.moveTo(investor.getPortfolio());
            investor.setLinkedCompany(bcr);
        }        
        
        investorsPurchased += 1;
        if (investorsPurchased == gameManager.getNumberOfPlayers()) {
            finishRound();            
        }
        return true;
    }
    
    
    private boolean validateBuy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();

        if (item.getStatus() != StartItem.BUYABLE) {
            DisplayBuffer.add(LocalText.getText("CantBuyItem",
                    playerName,
                    item.getName(),
                    LocalText.getText("NotForSale") ));
            return false;
        }
        return true;
    }

    @Override
    protected void finishRound() {
        for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
            if (company instanceof Investor_1880) {
                if (company.getPresident() == null) {
                    company.setClosed();
                }
            }
        }
        for (StartItem item : startPacket.getItems()) {
            if (item.isSold() == false) {
                item.setStatus(StartItem.SOLD);
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
    protected boolean pass(String playerName) {
        // TODO Auto-generated method stub
        return false;
    }

}
