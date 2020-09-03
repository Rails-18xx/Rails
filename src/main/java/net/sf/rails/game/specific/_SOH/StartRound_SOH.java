package net.sf.rails.game.specific._SOH;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.IntegerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_SOH extends StartRound {
    private static final Logger log = LoggerFactory.getLogger(StartRound_SOH.class);

    protected final int bidIncrement;

    private final GenericState<StartItem> auctionItemState =
            new GenericState<>(this, "auctionItemState");

    enum Procedure {
        AUCTION,  // Randomly pick one private per player, auction these.
        DEAL,     // Randomly deal one private per player
        SELECT    // Players each select any one private (intended for testing)
    }

    private int numberOfPlayers;
    private List<Player> players;

    private List<StartItem> allStartItems;
    private IntegerState currentItemIndex
            = IntegerState.create (this, "auctionedItemIndex");

    private Procedure procedure;

    private List<StartItem> pickedPrivatesAsList;
    private Map<Integer, StartItem> pickedPrivatesAsMap;

    /**
     * Constructed via Configure
     */
    public StartRound_SOH(GameManager parent, String id) {
        // Super must be called first thing, but we cannot use it any further
        // because its constructor parameters would greatly differ per procedure.
        super(parent, id);
        bidIncrement = startPacket.getModulus();
    }

    // ---------------
    // 0. COMMON STUFF
    // ---------------

    @Override
    public void start() {
        //super.start();

        procedure = Procedure.valueOf(GameOption.getValue(
                this, "StartRound").toUpperCase());

        players = playerManager.getPlayers();
        numberOfPlayers = players.size();
        startPlayer = playerManager.setCurrentToPriorityPlayer();

        allStartItems = startPacket.getItems();

        if (procedure == Procedure.AUCTION) {
            initAuction();
        } else if (procedure == Procedure.DEAL) {
            initDeal();
        } else if (procedure == Procedure.SELECT) {
            initSelection();
        }
    }

    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;
        Player currentPlayer = playerManager.getCurrentPlayer();

        // Check player
        String playerName = action.getPlayerName();
           if (!playerName.equals(currentPlayer.getId())) {
                String errMsg = LocalText.getText(
                        "WrongPlayer", playerName, currentPlayer.getId());
                log.error (errMsg);
                DisplayBuffer.add(this, errMsg);
            return false;
        }

        if (procedure == Procedure.AUCTION) {
            if (!processAuctionActions(action)) return false;
        } else if (procedure == Procedure.DEAL) {
            if (!processDealActions(action)) return false;
        } else if (procedure == Procedure.SELECT) {
            if (!processSelectionActions(action)) return false;
        }

        return true;
    }

    public boolean setPossibleActions () {

        possibleActions.clear();
        boolean result = false;

        if (procedure == Procedure.AUCTION) {
            result = setAuctionActions();
        } else if (procedure == Procedure.DEAL) {
            result = setDealActions();
        } else if (procedure == Procedure.SELECT) {
            result = setSelectionActions();
        }
        return result;
    }

    private void pickPrivates (int number) {

        Random generator = gameManager.getRandomGenerator();
        List<StartItem> items = new ArrayList<>(allStartItems);
        if (procedure == Procedure.DEAL) {
            pickedPrivatesAsList = new ArrayList<>();
        } else if (procedure == Procedure.AUCTION) {
            pickedPrivatesAsMap = new HashMap<>();
        }
        StartItem item;
        int n = items.size();
        int k, privateNumber;
        for (int i=0; i<number; i++, n--) {
            k = generator.nextInt(n);
            item = items.get(k);
            privateNumber = allStartItems.indexOf(item) + 1;
            if (procedure == Procedure.DEAL) {
                pickedPrivatesAsList.add(item);
            } else if (procedure == Procedure.AUCTION) {
                pickedPrivatesAsMap.put(privateNumber, item);
            }
            items.remove(k);
            log.info ("Picked private {} {}", k, item.getId());
        }
    }

    private void checkKO (Player player, StartItem item, boolean display) {
        // Buyer of 7 KO also gets an NRS share.
        if (item.getPrimary().getId().equalsIgnoreCase("7 KO")) {
            PublicCompany nrs = companyManager.getPublicCompany("NRS");
            bank.getIpo().getPortfolioModel().findCertificate(nrs, false).moveTo(player);
            String report = LocalText.getText("AlsoGetsShare",
                    player.getId(), nrs.getShareUnit(), nrs.getId());
            ReportBuffer.add(this, report);
            if (display) DisplayBuffer.add (this, report);
        }
    }


    // ---------------------
    // 1. AUCTION PROCESSING
    // ---------------------

    private StartItem auctionedItem;

    /**
     * Randomly select as many privates as there are players,
     * and auction each private in numerical order.
     */
    private void initAuction() {

        // Select as many privates as there are players
        pickPrivates(numberOfPlayers);

        // The picked items will be auctioned in numerical order,
        // which is why the item names start with their official number.
        StartItem item;
        for (int itemNo : pickedPrivatesAsMap.keySet().stream().sorted().collect(Collectors.toList())) {
            item = pickedPrivatesAsMap.get(itemNo);
            itemsToSell.add(item);
        }
        currentItemIndex.set(0);
        ReportBuffer.add (this, LocalText.getText(
                "HasPriority", playerManager.getPriorityPlayer().getId()));

        initItemAuction();
        setAuctionActions();
    }

    private void initItemAuction() {

        auctionedItem = itemsToSell.get(currentItemIndex.value());
        // Set the minimum initial bid, which in SOH is the base price.
                // Note, that this overrides the minimum initial bid as set in StartItem
                // (see StartItem.init() and .setBid() and the TODO in .init() )
        auctionedItem.setMinimumBid(auctionedItem.getBasePrice()
                // Note: in this game the below term is zero,
                // but it is added here as a reference how
                // the minimum initial bid should be calculated in other games.
                + startPacket.getMinimumInitialIncrement());

        numPasses.set(0);
        auctionItemState.set(auctionedItem);
        auctionedItem.setStatus(StartItem.AUCTIONED);

        // Set all players to active
        auctionedItem.setAllActive();
    }

    public boolean processAuctionActions(PossibleAction action) {

        boolean result;

        if (action instanceof BidStartItem) {

            return bid (action.getPlayerName(), (BidStartItem) action);

        } else if (action instanceof NullAction
                && ((NullAction) action).getMode() == NullAction.Mode.PASS) {

            result = pass ((NullAction) action, action.getPlayerName());
            
            if (!result) {
                finishRound();
            }

        } else {
            return super.process (action);
        }
        return true;
    }

    /** The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param bidItem The start item on which the bid is placed.
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        Player currentPlayer = playerManager.getCurrentPlayer();
        String errMsg = null;
        int previousBid = 0;
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // Check item
            if (!item.equals(auctionedItem)) {
                errMsg = LocalText.getText("WrongStartItem",
                        item.getPrimary().getId(),
                        auctionedItem.getPrimary().getId());
                break;
            }

            // Is the item buyable?
            if (bidItem.getStatus() != StartItem.BIDDABLE
                    && bidItem.getStatus() != StartItem.AUCTIONED) {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            // Bid must be at least the minimum bid
            if (bidAmount < item.getMinimumBid()) {
                errMsg = LocalText.getText("BidTooLow",
                        "" + item.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
                    // Note: this check is redundant here, because in this game
                    // the modulus is 1. Again it's included as an example for other games.
            if (bidAmount % startPacket.getModulus() != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf",
                        bidAmount,
                        startPacket.getModulus());
                break;
            }

            // Has the buyer enough cash?
            previousBid = item.getBid(currentPlayer);
            int available = currentPlayer.getFreeCash() + previousBid;
            if (bidAmount > available) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(this, available));
                break;
            }

            break;
        }

        if (errMsg != null) {
            log.error (errMsg);
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    playerName,
                    item.getId(),
                    errMsg ));
            return false;
        }

        item.setBid(bidAmount, currentPlayer);
        if (previousBid > 0) currentPlayer.unblockCash(previousBid);
        currentPlayer.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(this, bidAmount),
                item.getId(),
                Bank.format(this, currentPlayer.getFreeCash()) ));

        // Set next bidding player
        setNextBiddingPlayer(item);
        numPasses.set(0);
        item.setMinimumBid(bidAmount + startPacket.getMinimumIncrement());

        return true;

    }

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem) {

        StartItem item = boughtItem.getStartItem();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int price = item.getBasePrice();

        while (true) {
            if (player.getFreeCash() < price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }
            break;
        }


        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName,
                    item.getId(),
                    errMsg));
            return false;
        }

        assignItem(player, item, price, 0);

        // Set priority (only if the item was not auctioned)
        // ASSUMPTION: getting an item in auction mode never changes priority
        auctionItemState.set(null);

        checkKO(player, item, true);

        return true;
    }


    /**
     * Process a player's pass.
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

        Player player = playerManager.getCurrentPlayer();
        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

        if (procedure == Procedure.SELECT) {
            if (setNextSelectingPlayer()) {
                setSelectionActions();
            }
            return true;
        }

        // The 'number of passes' is set but not used in this StartRound version.
        // It has been replaced by the number of still actively bidding players,
        // which turned out to be much easier to use in determining the auction stage.
        // TODO: sort out if this can be an element of a future StartRound refactoring.
        numPasses.add(1);

        // Passing player becomes inactive for this item
        auctionedItem.setPass(player);
        if (auctionedItem.getBid(player) > 0) {
            player.unblockCash(auctionedItem.getBid(player));
        }

        // Find how many players are still active in bidding
        int activePlayers = auctionedItem.getActiveBidders();

        if (activePlayers == 0) {
            // All players have passed, discard this item
            auctionedItem.getPrimary().moveTo(bank.getScrapHeap());
            ReportBuffer.add(this, LocalText.getText("ALL_PASSED"));
            ReportBuffer.add(this, LocalText.getText("PrivateIsDiscarded", auctionedItem.getId()));

        } else if (activePlayers == 1 && auctionedItem.getBidder() != null) {
            // Only one active player is left (all but one have passed)
            // and has actually done a bid.
            int price = auctionedItem.getBid();
            Player buyer = auctionedItem.getBidder();

            log.debug("Highest bidder on {} is {} with {}", auctionedItem.getId(),
                    buyer.getId(), price);
            assignItem(buyer, auctionedItem, price, 0);
            // Buyer of 7 KO also gets an NRS share.
            checkKO (buyer, auctionedItem, false);

        } else {
             // Bidding still ongoing, find the next player who can still bid
            setNextBiddingPlayer(auctionedItem);

            return true;
        }

        // Auction of one item has finished
        auctionItemState.set(null);

        // Set priority
        playerManager.setPriorityPlayer(
                playerManager.setCurrentToNextPlayerAfter(
                        playerManager.getPriorityPlayer()));
        ReportBuffer.add (this, LocalText.getText(
                "HasPriority", playerManager.getPriorityPlayer().getId()));

        // Find next item to auction, if any
        return setNextAuctionItem();
    }

    /**
     * Find next active bidder, i.e. one that has not yet passed on this item
     * @param item The currently auctioned item
     */
    private void setNextBiddingPlayer (StartItem item) {
        Player nextPlayer;
        for (nextPlayer = playerManager.getNextPlayer ();
             !item.isActive (nextPlayer);
             nextPlayer = playerManager.getNextPlayerAfter (nextPlayer)) {
            numPasses.add(1);
        }
        playerManager.setCurrentPlayer(nextPlayer);

    }

    private boolean setNextSelectingPlayer () {

        Player nextPlayer = playerManager.getNextPlayer ();
        if (nextPlayer == startPlayer) {
            return false;
        } else {
            playerManager.setCurrentPlayer(nextPlayer);
            return true;
        }

    }

    private boolean setNextAuctionItem() {
        currentItemIndex.add(1);
        if (currentItemIndex.value() < numberOfPlayers) {
            initItemAuction();
            return true;
        }
        return false;
    }

    public boolean setAuctionActions() {

        if (playerManager.getCurrentPlayer() == startPlayer) ReportBuffer.add(this, "");

        // FIXME: Rails 2.0 Could be an infinite loop if there if no player has enough money to buy an item
        while (possibleActions.isEmpty()) {
            Player currentPlayer = playerManager.getCurrentPlayer();

            if (auctionedItem.getStatus() == StartItem.AUCTIONED) {

                if (currentPlayer.getFreeCash()
                        + auctionedItem.getBid(currentPlayer) >= auctionedItem.getMinimumBid()) {
                    BidStartItem possibleAction =
                            new BidStartItem(auctionedItem,
                                    auctionedItem.getMinimumBid(),
                                    bidIncrement, true);
                    possibleActions.add(possibleAction);
                    break; // No more actions
                } else {
                    // Can't bid: Autopass
                    numPasses.add(1);
                    break;
                }
            }

            if (possibleActions.isEmpty()) {
                numPasses.add(1);
                if (auctionItemState.value() == null) {
                    playerManager.setCurrentToNextPlayer();
                } else {
                    setNextBiddingPlayer(auctionItemState.value());
                }
            }
        }

        possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));

        return true;
    }

    // ------------------
    // 2. DEAL PROCESSING
    // ------------------


    /**
     * Randomly deal one private to each player.
     */
    private void initDeal() {

        pickPrivates(numberOfPlayers);
        for (StartItem item : pickedPrivatesAsList) {
            itemsToSell.add(item);
        }

        setDealActions();

        currentItemIndex.set(0);

    }

    private boolean processDealActions (PossibleAction action) {

        if (action instanceof BuyStartItem) {
            BuyStartItem buy = (BuyStartItem) action;
            Player player = buy.getPlayer();
            StartItem item = buy.getStartItem();
            assignItem(player, item, item.getBasePrice(), 0);
            log.info("Player {} gets private {}", player, item.getPrimary());

            checkKO(player, item, true);

            if (setNextSelectingPlayer()) {
                currentItemIndex.add(1);
            } else {
                finishRound();
            }

        } else {
            log.error ("Invalid action {}", action);
            return false;
        }

        return true;
    }

    private boolean setDealActions () {

        int index = currentItemIndex.value();
        StartItem item = itemsToSell.get(index);

        item.setStatus(StartItem.BUYABLE);
        possibleActions.add(new BuyStartItem (item, item.getBasePrice(), true));

        return true;
    }

    // -----------------------
    // 3. SELECTION PROCESSING
    // -----------------------

    /**
     * Allow each player to buy one private for face value.
     */
    private void initSelection () {

        hasBidding = Bidding.NO;

        for (StartItem item : allStartItems) {
             itemsToSell.add (item);
             item.setStatus(StartItem.BUYABLE);
             item.setAllActive();
        }
        setSelectionActions();
    }

    private boolean processSelectionActions (PossibleAction action) {

        boolean result;

        if (action instanceof BuyStartItem) {
            result = buy (action.getPlayerName(), (BuyStartItem) action);
        } else if (action instanceof NullAction
                && ((NullAction) action).getMode() == NullAction.Mode.PASS) {
            result = pass ((NullAction) action, action.getPlayerName());
        } else {
            result = super.process (action);
        }

        Player nextPlayer = playerManager.getNextPlayer();
        if (nextPlayer == startPlayer) {
            finishRound();
        } else {
            playerManager.setCurrentPlayer(nextPlayer);
        }

        return result;
    }

    private boolean setSelectionActions () {

        for (StartItem item : allStartItems) {
            if (!item.isSold()) {
                possibleActions.add(new BuyStartItem (item, item.getBasePrice(), false));
            }
        }

        possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));

        return true;
    }
}
