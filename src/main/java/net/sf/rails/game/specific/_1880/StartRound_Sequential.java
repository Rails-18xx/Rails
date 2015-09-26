/**
 * 
 */
package net.sf.rails.game.specific._1880;


import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import rails.game.action.*;

/**
 * @author Martin
 * 
 * Rails 2.0: OK
 * 
 */
public class StartRound_Sequential extends StartRound {

    private final GenericState<Player> startingPlayer =
            GenericState.create(this, "startingPlayer");
    private final GenericState<StartItem> currentItem =
            GenericState.create(this, "currentItem");
    private final ArrayListState<Player> passedPlayers = 
            ArrayListState.create(this, "passedPlayers");

    public StartRound_Sequential(GameManager parent, String id) {
        super(parent, id, true, false, false);
        // bidding yes, but no base prices and buying
    }

    @Override
    public void start() {
        super.start();

        // crude fix for StartItem hardcoded SetMinimumbid ignoring the initial
        // value out of the XMLs....
        // FIXME: Is this undo proof?
        for (StartItem item : startPacket.getItems()) {
            item.setMinimumBid(item.getBasePrice());
        }

        startingPlayer.set(playerManager.getCurrentPlayer());
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
        for (Player p: playerManager.getNextPlayers(false)) {
            if (!passedPlayers.contains(p)) {
                // TODO: What happens if all players have passed?
                playerManager.setCurrentPlayer(p);
                break;
            }
        }
    }

    private void setNextStartingPlayer() {
        Player nextPlayer = playerManager.getNextPlayerAfter(startingPlayer.value());
        startingPlayer.set(nextPlayer);
        playerManager.setCurrentPlayer(nextPlayer);
    }

    @Override
    public boolean setPossibleActions() {
        updateBiddingItem();

        if (playerManager.getCurrentPlayer() == startPlayer) {
            ReportBuffer.add(this,"");
        }

        if (currentItem != null) {
            if (playerManager.getCurrentPlayer().getCash() >= currentItem.value().getMinimumBid()) {
                possibleActions.add(new BidStartItem(currentItem.value(),
                        currentItem.value().getMinimumBid(), startPacket.getModulus(), true));
            }
            possibleActions.add(new NullAction(NullAction.Mode.PASS));
        }
        
        return true;
    }
    
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {
        Player player = playerManager.getPlayerByName(playerName);
        int bidAmount = bidItem.getActualBid();

        if (validateBid(playerName, bidItem) != true) {
            return false;
        }

        if (currentItem.value().getBid(player) > 0) {
            player.unblockCash(currentItem.value().getBid(player));
        }
             
        currentItem.value().setBid(bidAmount, player);
        player.blockCash(bidAmount);

        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(this,bidAmount),
                bidItem.getStartItem().getId(),
                Bank.format(this,player.getFreeCash())));        
        
        if ((passedPlayers.size() == (getRoot().getPlayerManager().getNumberOfPlayers() - 1))
            && (currentItem.value().getBidder() != null)) {
        // One player has bid, everyone else has passed.
            assignItem(currentItem.value().getBidder(), currentItem.value(),
                    currentItem.value().getBid());
        } else {
            setNextBiddingPlayer();
        }
        return true;
    }
    
    private boolean validateBid(String playerName, BidStartItem bidItem) {
        String errMsg = null;

        while (true) {
            // Check player
            if (!playerName.equals(playerManager.getCurrentPlayer().getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, playerManager.getCurrentPlayer().getId());
                break;
            }

            // Is the current item correct?
            if (currentItem.value() != bidItem.getStartItem()) {
                errMsg = LocalText.getText("WrongItem", playerName, playerManager.getCurrentPlayer().getId());
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
            if (bidItem.getActualBid() < currentItem.value().getMinimumBid()) {
                errMsg = LocalText.getText("BidTooLow", "" + currentItem.value().getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidItem.getActualBid() % startPacket.getModulus() != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf", bidItem.getActualBid(),
                        startPacket.getMinimumIncrement());
                break;
            }

        // Has the buyer enough cash?
            if (bidItem.getActualBid() > playerManager.getCurrentPlayer().getCash()) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(this, bidItem.getActualBid()));
                break;
            }

        break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    playerName,
                    currentItem.value().getId(),
                    errMsg ));
            return false;
        }

        return true;
    }

    @Override
    protected boolean pass(NullAction action, String playerName) {
        Player player = playerManager.getPlayerByName(playerName);

        if (validatePass(playerName) != true) {
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));
        
        if (currentItem.value().getBid(player) > 0) {
            player.unblockCash(currentItem.value().getBid(player));
        }

        currentItem.value().setPass(player);
        passedPlayers.add(player);
        
        if (passedPlayers.size() == playerManager.getNumberOfPlayers()) {
        // All players have passed - reduce price or run an operating round
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            if (currentItem.value().getNoBidsReaction() == StartItem.NoBidsReaction.REDUCE_AND_REBID) {
                currentItem.value().reduceBasePriceBy(5); // TODO: Make not 5
                // If the price was reduced to 0, assign the company to the starting bidder instead
                if (currentItem.value().getBasePrice() == 0) {
                    assignItem((Player) startingPlayer.value(), currentItem.value(), 0);
                } else {
                    ReportBuffer.add(this, LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                                    currentItem.value().getId(),
                                    Bank.format(this, startPacket.getFirstItem().getBasePrice()) ));
                    currentItem.value().setMinimumBid(currentItem.value().getBasePrice());
                    passedPlayers.clear();
                    setNextBiddingPlayer();
                }
            } else {
                gameManager.reportAllPlayersPassed();
                passedPlayers.clear();
                finishRound(); // TODO: Can this work?  How will it know to not start second start round?
            }
        } else if ((passedPlayers.size() == (getRoot().getPlayerManager().getNumberOfPlayers() - 1))
            && (currentItem.value().getBidder() != null)) {
        // One player has bid, everyone else has passed.
            assignItem(currentItem.value().getBidder(), currentItem.value(), currentItem.value().getBid()); // Could re-assign starting player...
        } else {
        // There are other players yet to bid
            setNextBiddingPlayer();     
        }
        return true;
    }

    private boolean validatePass(String playerName) {
        if (!playerName.equals(playerManager.getCurrentPlayer().getId())) {
                DisplayBuffer.add(this, LocalText.getText("InvalidPass", playerName,
                        LocalText.getText("WrongPlayer", playerName, playerManager.getCurrentPlayer().getId())));
                return false;
            }
        return true;
    }

    protected void assignItem(Player player, StartItem item, int price) {
        Certificate primary = item.getPrimary();
        Currency.toBank(player, price);
        primary.moveTo(player);
        item.setSold(player, price);
        ReportBuffer.add(this, LocalText.getText("BuysItemFor",
                player.getId(),
                primary.toText(),
                Bank.format(this, price) ));
        itemAssigned(player, item, price);
    }
    
    protected void itemAssigned(Player player, StartItem item, int price) {
    }

}
