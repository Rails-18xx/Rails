package net.sf.rails.game.specific._1862;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import rails.game.action.*;
import net.sf.rails.common.*;
import net.sf.rails.game.Currency;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StartRound;

public class ParliamentRound extends StartRound {
    private ArrayList<ParliamentBiddableItem> biddables;
    
    ParliamentBiddableItem activeBiddable;
    
    private Set<Player> passedPlayers;
    private TreeSet<Player> biddingPlayers;
    private Set<Player> auctionWinningPlayers;
    private int numberOfPasses;
    
    public ParliamentRound(GameManager parent, String id, boolean hasBidding, boolean hasBasePrices, boolean hasBuying) {
        super (parent, id);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.START_ROUND);
    }
    
    
    public ParliamentRound(GameManager parent, String id) {
        super(parent, id);
    }

    public void start() {
        biddables = new ArrayList<ParliamentBiddableItem> ();        
        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            PublicCompany_1862 c = (PublicCompany_1862) company;
            if (c.isStartable()) {
                biddables.add(new ParliamentBiddableItem(this, "biddable_" + c.getId(), c));
            }
            // TODO: Remove this
            if (biddables.size() == 8) {
                break;
            }
        }
        
        passedPlayers = new TreeSet<Player>();
        biddingPlayers = new TreeSet<Player>();
        auctionWinningPlayers = new TreeSet<Player>();
        activeBiddable = null;
        numberOfPasses = 0;
        
        startPlayer = playerManager.setCurrentToPriorityPlayer();
        
        ReportBuffer.add(this, LocalText.getText("StartOfParliamentRound"));
        ReportBuffer.add(this, LocalText.getText("HasPriority", startPlayer.getId()));
        
        setPossibleActions(); 
    }

    public ArrayList<ParliamentBiddableItem> getBiddables() {
        return biddables;
    }
    
    public boolean setPossibleActions() {
        if (activeBiddable == null) {
            for (ParliamentBiddableItem biddable : biddables) {
                if (playerCanBidOn(biddable) == true) {
                    possibleActions.add(new BidParliamentAction(biddable));
                }
            }
            possibleActions.add(new NullAction(NullAction.PASS));
        } else if (onlyOneBidderLeft() == true) {
            possibleActions.add(new BuyParliamentAction(activeBiddable, biddingPlayers.first()));
        } else {
            if (playerCanBidOn(activeBiddable) == true) {
                possibleActions.add(new BidParliamentAction(activeBiddable));
            }
            possibleActions.add(new NullAction(NullAction.PASS));
        }
        
        return false;
    }
    
    private boolean playerCanBidOn(ParliamentBiddableItem biddable) {
        Player player = playerManager.getCurrentPlayer();
        int nextCashMinimum = (biddable.getMinParValue() * 3) + biddable.getCurrentBid() + biddable.getBidIncrement();
        if (player.getCash() < nextCashMinimum) {
            return false;
        } else if (biddable.hasBeenSold()) {
            return false;
        }
        
        return true;
    }
    
    private boolean onlyOneBidderLeft() {
        if ((passedPlayers.size() == (playerManager.getNumberOfPlayers() - 1)) && 
                (biddingPlayers.size() == 1)) {
            return true;
        }
        return false;
    }
         
    @Override
    public boolean process(PossibleAction action) {
        
        if (action instanceof BidParliamentAction) {
            return processBidParliamentAction((BidParliamentAction) action);    
        } else if (action instanceof NullAction) {
            return processNullAction();
        } else if (action instanceof BuyParliamentAction) {
            return processBuyParliamentAction((BuyParliamentAction) action);
        }
        
        return false;
    }
    
    private boolean processBidParliamentAction(BidParliamentAction action) {
        Player player = playerManager.getCurrentPlayer();
        ParliamentBiddableItem biddable = action.getBiddable();
        biddable.performBid(player, action.getActualBid());
        biddingPlayers.add(player);
        activeBiddable = biddable;
        biddable.setCurrentBid(action.getActualBid());
        
        ReportBuffer.add(this, LocalText.getText("BID_ITEM",
                player.getId(),
                Currency.format(this, action.getActualBid()),
                biddable.getCompany().getLongName()));        
        
        while (passedPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
        numberOfPasses = 0;
        return true;    
    }

    private boolean processNullAction() {
        Player player = playerManager.getCurrentPlayer();
        biddingPlayers.remove(player);
        passedPlayers.add(player);
        numberOfPasses++;
        
        ReportBuffer.add(this, LocalText.getText("PASSES", player.getId()));

        if (numberOfPasses == playerManager.getNumberOfPlayers()) {
            finishRound();
        } else {
            while (passedPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
        }
        return true;        
    }
    
    private boolean processBuyParliamentAction(BuyParliamentAction action) {
        Player player = playerManager.getCurrentPlayer();
        PublicCompany_1862 company = action.getBiddable().getCompany();
        PublicCertificate cert = company.getPresidentsShare();
        action.getBiddable().sold();

//      company.start(startSpace); 
        
        cert.moveTo(player);

        // If more than one certificate is bought at the same time, transfer
        // these too.
        for (int i = 3; i < action.getSharesPurchased(); i++) {
            cert = ipo.findCertificate(company, false);
            cert.moveTo(player);
        }

        // Pay for these shares
        Currency.wire(player, action.getParPrice()*action.getSharesPurchased() + 
                action.getBiddable().getCurrentBid(), bank);
        
        
        
        auctionWinningPlayers.add(player);
        
        ReportBuffer.add(this, LocalText.getText("WinsAuction",
                player.getId(), company.getLongName(), Currency.format(this, action.getBiddable().getCurrentBid())));
        ReportBuffer.add(this, LocalText.getText("START_COMPANY_LOG",
                player.getId(), company.getLongName(), Currency.format(this, action.getParPrice()),
                    Currency.format(this,  action.getParPrice() * action.getSharesPurchased()), action.getSharesPurchased(),
                    action.getSharesPurchased() * 10, LocalText.getText("BANK")));
        
        if (auctionWinningPlayers.size() == playerManager.getNumberOfPlayers()) {
            finishRound();
        } else {
            activeBiddable = null;
            biddingPlayers.clear();
            passedPlayers.clear();
            numberOfPasses = 0;
            playerManager.setCurrentPlayer(player);
            while (auctionWinningPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
        }
        return true;
    }
    
    
    // Needed for "StartRound"
    @Override
    protected boolean bid(String playerName, BidStartItem startItem) {
        return false;
    }

    // Needed for "StartRound"
    @Override
    protected boolean pass(NullAction action, String playerName) {
        return false;
    }




}
