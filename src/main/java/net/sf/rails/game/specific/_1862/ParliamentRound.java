package net.sf.rails.game.specific._1862;

import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import rails.game.action.*;
import net.sf.rails.common.*;
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
        Player player = playerManager.getCurrentPlayer();
        
        if (action instanceof BidParliamentAction) {
            BidParliamentAction bpa = (BidParliamentAction) action;
            ParliamentBiddableItem biddable = bpa.getBiddable();
            biddable.performBid(player, bpa.getActualBid());
            biddingPlayers.add(player);
            activeBiddable = biddable;
            biddable.setCurrentBid(bpa.getActualBid());
            while (passedPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
            numberOfPasses = 0;
            return true;    
        } else if (action instanceof NullAction) {
            biddingPlayers.remove(player);
            passedPlayers.add(player);
            numberOfPasses++;
            if (numberOfPasses == playerManager.getNumberOfPlayers()) {
                finishRound();
                return true;
            } else {
                while (passedPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
                return true;
            }
        } else if (action instanceof BuyParliamentAction) {
            BuyParliamentAction bpa = (BuyParliamentAction) action;
            PublicCompany_1862 company = bpa.getBiddable().getCompany();
            PublicCertificate cert = company.getPresidentsShare();
            bpa.getBiddable().sold();

//          company.start(startSpace); 
            
            cert.moveTo(player);

            // If more than one certificate is bought at the same time, transfer
            // these too.
            for (int i = 3; i < bpa.getSharesPurchased(); i++) {
                cert = ipo.findCertificate(company, false);
                cert.moveTo(player);
            }

            // Pay for these shares
//            String costText = Currency.wire(player, bpa.getParPrice()*bpa.getSharesPurchased() + 
//                    bpa.getBiddable().getCurrentBid(), bank);
            
            auctionWinningPlayers.add(player);
            if (auctionWinningPlayers.size() == playerManager.getNumberOfPlayers()) {
                finishRound();
                return true;
            } else {
                activeBiddable = null;
                biddingPlayers.clear();
                passedPlayers.clear();
                numberOfPasses = 0;
                playerManager.setCurrentPlayer(player);
                while (auctionWinningPlayers.contains(playerManager.setCurrentToNextPlayer()) == true) {}
                return true;
            }
        }
        
        return false;
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
