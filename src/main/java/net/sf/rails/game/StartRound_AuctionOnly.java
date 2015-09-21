package net.sf.rails.game;

import java.util.SortedSet;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.ArrayListState;
import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import rails.game.action.StartItemAction;

/**
 * Implements a start round where there are only auctions (no buying), e.g. the
 * parliament round in 1862.
 */

public abstract class StartRound_AuctionOnly extends StartRound {
    private final ArrayListState<Player> auctionWinners = 
            ArrayListState.create(this, "auctionWinners");

    protected StartRound_AuctionOnly(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    public void start() {
        super.start();
        auctionWinners.clear();
        setPossibleActions();
    }

    @Override
    public boolean setPossibleActions() {
        Player currentPlayer = playerManager.getCurrentPlayer();

        possibleActions.clear();

        if (currentPlayer == startPlayer) ReportBuffer.add(this, "");

        if (currentAuctionItem() != null) {
            // This item is currently up for bid
            StartItem currentItem = currentAuctionItem();
            if (playerCanBid(currentPlayer, currentItem)) {
                BidStartItem possibleAction =
                        new BidStartItem(currentItem,
                                currentItem.getMinimumBid(),
                                startPacket.getModulus(), true);
                possibleActions.add(possibleAction);
            }
            possibleActions.add(new NullAction(NullAction.Mode.PASS));
        } else if (currentSharePriceItem() != null) {
            // Item sold, but needs share price
            StartItem currentItem = currentSharePriceItem();
            playerManager.setCurrentPlayer(currentItem.getBidder());
            BuyStartItem bsi =
                    new BuyStartItem(currentItem, currentItem.getBid(), false,
                            true);
            
            bsi.setStartSpaces(getStartSpaces(currentItem.getBidder().getFreeCash()));
            possibleActions.add(bsi);
        } else {
            // Nothing is currently up for auction
            boolean atLeastOneBiddable = false;
            for (StartItem item : itemsToSell.view()) {
                if (item.isSold() == false) {
                    item.setAllActive();
                    item.setStatus(StartItem.BIDDABLE);
                    if (playerCanBid(currentPlayer, item)) {
                        atLeastOneBiddable = true;
                        BidStartItem possibleAction =
                                new BidStartItem(item, item.getMinimumBid(),
                                        startPacket.getModulus(), false);
                        possibleActions.add(possibleAction);
                    }
                }
            }
            if (atLeastOneBiddable == true) {
                possibleActions.add(new NullAction(NullAction.Mode.PASS));
            }
        }

        if (possibleActions.isEmpty()) {
            numPasses.add(1); 
            setNextBiddingPlayer();
        }
        return true;

    }

    private StartItem currentAuctionItem() {
        for (StartItem item : itemsToSell.view()) {
            if (item.getStatus() == StartItem.AUCTIONED) {
                return item;
            }
        }
        return null;
    }

    private StartItem currentSharePriceItem() {
        for (StartItem item : itemsToSell.view()) {
            if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                return item;
            }
        }
        return null;
    }

    protected boolean playerCanBid(Player currentPlayer, StartItem item) {
        if (currentPlayer.getFreeCash() + item.getBid(currentPlayer) >= item.getMinimumBid()) {
            return true;
        }
        return false;
    }

    /*
     * The possible start spaces allowed for a company.
     */
    protected abstract SortedSet<String> getStartSpaces(int i);


    @Override
    protected boolean bid(String playerName, BidStartItem startItem) {
        if (validateBid(playerName, startItem) == false) {
            return false;
        }

        StartItem item = startItem.getStartItem();
        Player player = playerManager.getCurrentPlayer();
        int bidAmount = startItem.getActualBid();
        int previousBid = item.getBid(player);

        item.setBid(bidAmount, player);
        if (item.getStatus() != StartItem.AUCTIONED) {
            item.setStatus(StartItem.AUCTIONED);
        }

        if (previousBid > 0) {
            player.unblockCash(previousBid);
        }

        player.blockCash(bidAmount);

        ReportBuffer.add(
                this,
                LocalText.getText("BID_ITEM_LOG", playerName,
                        Bank.format(this, bidAmount), item.getId(),
                        Bank.format(this, player.getFreeCash())));

        setNextBiddingPlayer(item);
        numPasses.set(0);

        return true;
    }

    private boolean validateBid(String playerName, BidStartItem bidItem) {
        StartItem item = bidItem.getStartItem();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int previousBid = 0;
        int bidAmount = bidItem.getActualBid();

        while (true) {
            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg =
                        LocalText.getText("WrongPlayer", playerName,
                                player.getId());
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
                errMsg =
                        LocalText.getText("ActionNotAllowed",
                                bidItem.toString());
                break;
            }

            // Is the item buyable?
            if (bidItem.getStatus() != StartItem.BIDDABLE
                && bidItem.getStatus() != StartItem.AUCTIONED) {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            // Bid must be at least 5 above last bid
            if (bidAmount < item.getMinimumBid()) {
                errMsg =
                        LocalText.getText("BidTooLow",
                                "" + item.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidAmount % startPacket.getModulus() != 0) {
                errMsg =
                        LocalText.getText("BidMustBeMultipleOf", bidAmount,
                                startPacket.getMinimumIncrement());
                break;
            }

            // Has the buyer enough cash?
            previousBid = item.getBid(player);
            int available = player.getFreeCash() + previousBid;
            if (bidAmount > available) {
                errMsg =
                        LocalText.getText("BidTooHigh",
                                Bank.format(this, available));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid", playerName,
                    item.getId(), errMsg));
            return false;
        }
        return true;
    }

    @Override
    protected boolean pass(NullAction action, String playerName) {
        if (validatePass(action, playerName) == false) {
            return false;
        }

        Player player = playerManager.getCurrentPlayer();
        numPasses.add(1);

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));
        if (currentAuctionItem() != null) {
            // An item is currently up for bid
            StartItem auctionItem = currentAuctionItem();
            if (auctionItem.getBidders() == 2) {
                // Only one bidder is left after this pass
                int price = auctionItem.getBid();
                log.debug("Highest bidder is "
                          + auctionItem.getBidder().getId());
                if (auctionItem.needsPriceSetting() != null) {
                    auctionItem.setStatus(StartItem.NEEDS_SHARE_PRICE);
                } else {
                    assignItem(auctionItem.getBidder(), auctionItem, price, 0);
                }
                numPasses.set(0);
//                playerManager.setCurrentToPriorityPlayer(); // TODO: I think this can be removed.
            } else {
                player.unblockCash(auctionItem.getBid(player));
                auctionItem.setPass(player);
                setNextBiddingPlayer(auctionItem);
            }
        } else { 
            // Nothing is up for bid
            if ((numPasses.value() + auctionWinners.size()) == playerManager.getNumberOfPlayers()) {
                finishRound();
            } else {
                setNextBiddingPlayer();
            }
        }
        return true;
    }

    private boolean validatePass(NullAction action, String playerName) {
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg =
                        LocalText.getText("WrongPlayer", playerName,
                                player.getId());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this,
                    LocalText.getText("InvalidPass", playerName, errMsg));
            return false;
        }

        return true;
    }
    

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        boolean result = super.buy(playerName, boughtItem);
        if (result == true) {
            Player player = playerManager.getPlayerByName(playerName);
            auctionWinners.add(player);
            if (auctionWinners.size() == playerManager.getNumberOfPlayers()) {
                finishRound();
            }
            playerManager.setCurrentPlayer(player);
            while (auctionWinners.contains(playerManager.getCurrentPlayer())) {
                playerManager.setCurrentToNextPlayer();
            }
        }
        return result;
    }

    private void setNextBiddingPlayer(StartItem item, Player biddingPlayer) {
        for (Player player : playerManager.getNextPlayersAfter(biddingPlayer,
                false, false)) {
            if (item.isActive(player)) {
                playerManager.setCurrentPlayer(player);
                break;
            }
        }
    }

    private void setNextBiddingPlayer(StartItem item) {
        setNextBiddingPlayer(item, playerManager.getCurrentPlayer());
    }
    
    private void setNextBiddingPlayer() {
        playerManager.setCurrentToNextPlayer();
        while (auctionWinners.contains(playerManager.getCurrentPlayer())) {
            playerManager.setCurrentToNextPlayer();
        }
    }

}
