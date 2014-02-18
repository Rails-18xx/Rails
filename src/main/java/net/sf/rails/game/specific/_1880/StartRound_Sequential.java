/**
 * 
 */
package net.sf.rails.game.specific._1880;

import java.util.List;
import java.util.Vector;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Certificate;
import net.sf.rails.game.Currency;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PlayerManager;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartPacket;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.GenericState;
import rails.game.action.*;

/**
 * @author Martin
 * 
 */
public class StartRound_Sequential extends StartRound {

    private final GenericState<Player> startingPlayer =
            GenericState.create(this, "startingPlayer");
    private final GenericState<StartItem> currentItem =
            GenericState.create(this, "currentItem");
    private final ArrayListState<Player> passedPlayers = 
            ArrayListState.create(this, "passedPlayers");

    /**
     * @param gameManager
     */
    public StartRound_Sequential(GameManager parent, String id) {
        super(parent, id);
        hasBasePrices = false;
        hasBidding = true;
        hasBuying = false;
    }

    @Override
    public void start(StartPacket startPacket) {
        super.start(startPacket);

        // crude fix for StartItem hardcoded SetMinimumbid ignoring the initial
        // value out of the XMLs....
        // FIXME: Is this undo proof?
        for (StartItem item : startPacket.getItems()) {
            item.setMinimumBid(item.getBasePrice());
        }

        startingPlayer.set(getRoot().getPlayerManager().getCurrentPlayer());
        setPossibleActions();
    }

    private void updateBiddingItem() {
        StartItem firstUnsoldItem = startPacket.getFirstUnsoldItem();
        if (firstUnsoldItem == null) {
            finishRound();
        } else if (currentItem.value() != firstUnsoldItem) {
            if (currentItem.value() != null) {
                setNextStartingPlayer();
            }
            currentItem.set(firstUnsoldItem);
            currentItem.value().setStatus(StartItem.BIDDABLE);
            passedPlayers.clear();
        }        
    }

    private void setNextBiddingPlayer() {
        
        List<Player> nextPlayers = getRoot().getPlayerManager().getPlayersAfterCurrent();
        for (Player p: nextPlayers) {
            if (!passedPlayers.contains(p)) {
                // TODO: What happens if all players have passed?
                getRoot().getPlayerManager().setCurrentPlayer(p);
                break;
            }
        }
    }

    private void setNextStartingPlayer() {
        PlayerManager pm = getRoot().getPlayerManager();
        Player nextPlayer = pm.getNextPlayerAfter(startingPlayer.value());
        startingPlayer.set(nextPlayer);
        pm.setCurrentPlayer(nextPlayer);
    }

    @Override
    public boolean setPossibleActions() {
        updateBiddingItem();

        if (currentPlayer == startPlayer) {
            ReportBuffer.add(this,"");
        }

        if (currentItem != null) {
            if (getCurrentPlayer().getCash() >= currentItem.getMinimumBid()) {
                possibleActions.add(new BidStartItem(currentItem,
                        currentItem.getMinimumBid(), startPacket.getModulus(), true));
            }
            possibleActions.add(new NullAction(NullAction.PASS));
        }
        
        return true;
    }
    
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {
        Player player =
                getRoot().getPlayerManager().getPlayerByName(playerName);
        int bidAmount = bidItem.getActualBid();

        if (validateBid(playerName, bidItem) != true) {
            return false;
        }

        if (currentItem.getBid(player) > 0) {
            player.unblockCash(currentItem.getBid(player));
        }
             
        currentItem.setBid(bidAmount, player);
        player.blockCash(bidAmount);

        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Currency.format(this,bidAmount),
                bidItem.getStartItem().getName(),
                Currency.format(this,player.getFreeCash())));        
        
        if ((passedPlayers.size() == (getRoot().getPlayerManager().getNumberOfPlayers() - 1))
            && (currentItem.getBidder() != null)) {
        // One player has bid, everyone else has passed.
            assignItem(currentItem.getBidder(), currentItem,
                    currentItem.getBid());
        } else {
            setNextBiddingPlayer();
        }
        return true;
    }
    
    private boolean validateBid(String playerName, BidStartItem bidItem) {
        String errMsg = null;

        while (true) {
            // Check player
            if (!playerName.equals(getCurrentPlayer().getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, getCurrentPlayer().getId());
                break;
            }

            // Is the current item correct?
            if (currentItem != bidItem.getStartItem()) {
                errMsg = LocalText.getText("WrongItem", playerName, getCurrentPlayer().getId());
                break;
            }
            
            // Check item
            boolean validItem = false;
            for (StartItemAction activeItem : possibleActions.getType(StartItemAction.class)) {
                if (bidItem.equalsAsOption(activeItem)) {
                    validItem = true;
                    break;
                }
            }
            
            if (!validItem) {
                errMsg = LocalText.getText("ActionNotAllowed", bidItem.toString());
                break;
            }

            // Is the item buyable?
            if (bidItem.getStatus() != StartItem.BIDDABLE) {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            // Bid must be at least 5 above last bid
            if (bidItem.getActualBid() < currentItem.getMinimumBid()) {
                errMsg = LocalText.getText("BidTooLow", "" + currentItem.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidItem.getActualBid() % startPacket.getModulus() != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf", bidItem.getActualBid(),
                        startPacket.getMinimumIncrement());
                break;
            }

        // Has the buyer enough cash?
            if (bidItem.getActualBid() > getCurrentPlayer().getCash()) {
                errMsg = LocalText.getText("BidTooHigh", Currency.format(this, bidItem.getActualBid()));
                break;
            }

        break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    playerName,
                    currentItem.getName(),
                    errMsg ));
            return false;
        }

        return true;
    }

    @Override
    protected boolean pass(NullAction action, String playerName) {
        Player player = getRoot().getPlayerManager().getPlayerByName(playerName);

        if (validatePass(playerName) != true) {
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));
        
        if (currentItem.getBid(player) > 0) {
            player.unblockCash(currentItem.getBid(player));
        }

        currentItem.setBid(-1, player);
        passedPlayers.add(player);
        
        if (passedPlayers.size() == getRoot().getPlayerManager().getNumberOfPlayers()) {
        // All players have passed - reduce price or run an operating round
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            if (currentItem.getNoBidsReaction() == StartItem.NoBidsReaction.REDUCE_AND_REBID) {
                currentItem.reduceBasePriceBy(5); // TODO: Make not 5
                // If the price was reduced to 0, assign the company to the starting bidder instead
                if (currentItem.getBasePrice() == 0) {
                    assignItem((Player) startingPlayer, currentItem, 0);
                } else {
                    ReportBuffer.add(this, LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                                    currentItem.getName(),
                                    Currency.format(this, startPacket.getFirstItem().getBasePrice()) ));
                    currentItem.setMinimumBid(currentItem.getBasePrice());
                    passedPlayers.clear();
                    setNextBiddingPlayer();
                }
            } else {
                passedPlayers.clear();
                finishRound(); // TODO: Can this work?  How will it know to not start second start round?
            }
        } else if ((passedPlayers.size() == (getRoot().getPlayerManager().getNumberOfPlayers() - 1))
            && (currentItem.getBidder() != null)) {
        // One player has bid, everyone else has passed.
            assignItem(currentItem.getBidder(), currentItem, currentItem.getBid()); // Could re-assign starting player...
        } else {
        // There are other players yet to bid
            setNextBiddingPlayer();     
        }
        return true;
    }

    private boolean validatePass(String playerName) {
        if (!playerName.equals(getCurrentPlayer().getId())) {
                DisplayBuffer.add(this, LocalText.getText("InvalidPass", playerName,
                        LocalText.getText("WrongPlayer", playerName, getCurrentPlayer().getId())));
                return false;
            }
        return true;
    }

    protected void assignItem(Player player, StartItem item, int price) {
        Certificate primary = item.getPrimary();
        Currency.toBank(player, price);
        transferCertificate(primary, player.getPortfolioModel());
        item.setSold(player, price);
        ReportBuffer.add(this, LocalText.getText("BuysItemFor",
                player.getId(),
                primary.getName(),
                Currency.format(this, price) ));
        itemAssigned(player, item, price);
    }
    
    protected void itemAssigned(Player player, StartItem item, int price) {
    }

}
