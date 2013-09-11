/**
 * 
 */
package rails.game.specific._1880;

import java.util.List;
import java.util.Vector;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;

/**
 * @author Martin
 * 
 */
public class StartRound_Sequential extends StartRound {

    private Player startingPlayer;
    private StartItem currentItem;
    private final List<Player> passedPlayers = new Vector<Player>();

    /**
     * @param gameManager
     */
    public StartRound_Sequential(GameManagerI gameManager) {
        super(gameManager);
        hasBasePrices = false;
        hasBidding = true;
        hasBuying = false;
    }

    @Override
    public void start(StartPacket startPacket) {
        super.start(startPacket);

        // crude fix for StartItem hardcoded SetMinimumbid ignoring the initial
        // value out of the XMLs....
        for (StartItem item : startPacket.getItems()) {
            item.setMinimumBid(item.getBasePrice());
        }

        startingPlayer = getCurrentPlayer();
        setPossibleActions();
    }

    private void updateBiddingItem() {
        StartItem firstUnsoldItem = startPacket.getFirstUnsoldItem();
        if (firstUnsoldItem == null) {
            finishRound();
        } else if (currentItem != firstUnsoldItem) {
            if (currentItem != null) {
                setNextStartingPlayer();
            }
            currentItem = firstUnsoldItem;
            currentItem.setStatus(StartItem.BIDDABLE);
            passedPlayers.clear();
        }        
    }

    private void setNextBiddingPlayer() {
        int currentPlayerIndex = getCurrentPlayerIndex();
        int numberOfPlayers = gameManager.getNumberOfPlayers();

        for (int i = (currentPlayerIndex + 1); i < (currentPlayerIndex + numberOfPlayers); i++) {
            if (passedPlayers.contains(gameManager.getPlayerByIndex(i)) == false) {
                setCurrentPlayerIndex(i);
                break;
            }
        }
    }

    private void setNextStartingPlayer() {
        Player nextPlayer =
                gameManager.getPlayerByIndex(startingPlayer.getIndex() + 1);
        startingPlayer = nextPlayer;
        setCurrentPlayerIndex(nextPlayer.getIndex());
    }

    @Override
    public boolean setPossibleActions() {
        updateBiddingItem();

        if (currentPlayer == startPlayer) {
            ReportBuffer.add("");
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
                gameManager.getPlayerManager().getPlayerByName(playerName);
        int bidAmount = bidItem.getActualBid();

        if (validateBid(playerName, bidItem) != true) {
            return false;
        }

        if (currentItem.getBid(player) > 0) {
            player.unblockCash(currentItem.getBid(player));
        }
        currentItem.setBid(bidAmount, player);
        player.blockCash(bidAmount);

        moveStack.start(false);

        ReportBuffer.add(LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(bidAmount),
                bidItem.getStartItem().getName(),
                Bank.format(player.getFreeCash())));        
        
        if ((passedPlayers.size() == (gameManager.getNumberOfPlayers() - 1))
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
            if (!playerName.equals(getCurrentPlayer().getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, getCurrentPlayer().getName());
                break;
            }

            // Is the current item correct?
            if (currentItem != bidItem.getStartItem()) {
                errMsg = LocalText.getText("WrongItem", playerName, getCurrentPlayer().getName());
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
                errMsg = LocalText.getText("BidTooHigh", Bank.format(bidItem.getActualBid()));
                break;
            }

        break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("InvalidBid",
                    playerName,
                    currentItem.getName(),
                    errMsg ));
            return false;
        }

        return true;
    }

    @Override
    protected boolean pass(String playerName) {
        Player player = gameManager.getPlayerManager().getPlayerByName(playerName);

        if (validatePass(playerName) != true) {
            return false;
        }

        if (currentItem.getBid(player) > 0) {
            player.unblockCash(currentItem.getBid(player));
        }
        currentItem.setBid(-1, player);
        passedPlayers.add(player);
        
        moveStack.start(false);
        ReportBuffer.add(LocalText.getText("PASSES", playerName));
                
        if (passedPlayers.size() == gameManager.getNumberOfPlayers()) {
        // All players have passed - reduce price or run an operating round
            ReportBuffer.add(LocalText.getText("ALL_PASSED"));
            if (currentItem.getNoBidsReaction() == StartItem.NoBidsReaction.REDUCE_AND_REBID) {
                currentItem.reduceBasePriceBy(5); // TODO: Make not 5
                // If the price was reduced to 0, assign the company to the starting bidder instead
                if (currentItem.getBasePrice() == 0) {
                    assignItem((Player) startingPlayer, currentItem, 0);
                } else {
                    ReportBuffer.add(LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                                    currentItem.getName(),
                                    Bank.format(startPacket.getFirstItem().getBasePrice()) ));
                    currentItem.setMinimumBid(currentItem.getBasePrice());
                    passedPlayers.clear();
                    setNextBiddingPlayer();
                }
            } else {
                passedPlayers.clear();
                finishRound(); // TODO: Can this work?  How will it know to not start second start round?
            }
        } else if ((passedPlayers.size() == (gameManager.getNumberOfPlayers() - 1))
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
        if (!playerName.equals(getCurrentPlayer().getName())) {
                DisplayBuffer.add(LocalText.getText("InvalidPass", playerName,
                        LocalText.getText("WrongPlayer", playerName, getCurrentPlayer().getName())));
                return false;
            }
        return true;
    }

    protected void assignItem(Player player, StartItem item, int price) {
        Certificate primary = item.getPrimary();
        pay(player, bank, price);
        transferCertificate(primary, player.getPortfolio());
        item.setSold(player, price);
        ReportBuffer.add(LocalText.getText("BuysItemFor",
                player.getName(),
                primary.getName(),
                Bank.format(price) ));
        itemAssigned(player, item, price);
    }
    
    protected void itemAssigned(Player player, StartItem item, int price) {
    }
}
