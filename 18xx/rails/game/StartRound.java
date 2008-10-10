package rails.game;

import java.util.ArrayList;
import java.util.List;

import rails.game.action.*;
import rails.game.model.ModelObject;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.util.LocalText;

public abstract class StartRound extends Round implements StartRoundI {

    protected StartPacket startPacket = null;
    protected int[] itemIndex;
    protected List<StartItem> itemsToSell = null;
    protected State auctionItemState =
            new State("AuctionItem", StartItem.class);
    protected IntegerState numPasses = new IntegerState("StartRoundPasses");
    protected int numPlayers;
    protected String variant;
    //protected GameManager gameMgr;
    protected Player currentPlayer;

    /**
     * Should the UI present bidding into and facilities? This value MUST be set
     * in the actual StartRound constructor.
     */
    protected boolean hasBidding;

    /**
     * Should the UI show base prices? Not useful if the items are all equal, as
     * in 1841 and 18EU.
     */
    protected boolean hasBasePrices = true;

    /** A company in need for a par price. */
    PublicCompanyI companyNeedingPrice = null;

    /*----- Initialisation -----*/
    /**
     * Will be created dynamically.
     * 
     */
    public StartRound() {}

    /**
     * Start the start round.
     * 
     * @param startPacket The startpacket to be sold in this start round.
     */
    public void start(StartPacket startPacket) {

        this.startPacket = startPacket;
        this.variant = Game.getGameOption(GameManager.VARIANT_KEY);
        if (variant == null) variant = "";
        numPlayers = gameManager.getNumberOfPlayers();

        itemsToSell = new ArrayList<StartItem>();
        itemIndex = new int[startPacket.getItems().size()];
        int index = 0;

        for (StartItem item : startPacket.getItems()) {

            // New: we only include items that have not yet been sold
            // at the start of the current StartRound
            if (!item.isSold()) {
                itemsToSell.add(item);
                itemIndex[index++] = item.getIndex();
            }
        }
        numPasses.set(0);
        auctionItemState.set(null);

        //gameMgr = GameManager.getInstance();
        gameManager.setRound(this);
        setCurrentPlayerIndex(gameManager.getPriorityPlayer().getIndex());
        currentPlayer = getCurrentPlayer();

        ReportBuffer.add("");
        ReportBuffer.add(LocalText.getText("StartOfInitialRound"));
        ReportBuffer.add(LocalText.getText("HasPriority",
                getCurrentPlayer().getName()));
    }

    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;

        log.debug("Processing action " + action);

        if (action instanceof NullAction) {

            String playerName = action.getPlayerName();
            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case NullAction.PASS:
                result = pass(playerName);
                break;
            }

        } else if (action instanceof StartItemAction) {

            StartItemAction startItemAction = (StartItemAction) action;
            String playerName = action.getPlayerName();

            log.debug("Item details: " + startItemAction.toString());

            if (startItemAction instanceof BuyStartItem) {

                BuyStartItem buyAction = (BuyStartItem) startItemAction;
                if (buyAction.hasSharePriceToSet()
                    && buyAction.getAssociatedSharePrice() == 0) {
                    // We still need a share price for this item
                    startItemAction.getStartItem().setStatus(
                            StartItem.NEEDS_SHARE_PRICE);
                    // We must set the priority player, though
                    gameManager.setPriorityPlayer();
                    result = true;
                } else {
                    result = buy(playerName, buyAction);
                }
            } else if (startItemAction instanceof BidStartItem) {
                result = bid(playerName, (BidStartItem) startItemAction);
            }
        } else {

            DisplayBuffer.add(LocalText.getText("UnexpectedAction",
                    action.toString()));
        }

        if (startPacket.areAllSold()) {
            /*
             * If the complete start packet has been sold, start a Stock round,
             */
            possibleActions.clear();
            gameManager.nextRound(this);
        } else if (!setPossibleActions()) {
            /*
             * If nobody can do anything, keep executing Operating and Start
             * rounds until someone has got enough money to buy one of the
             * remaining items. The game mechanism ensures that this will
             * ultimately be possible.
             */
            gameManager.nextRound(this);
        }

        return result;
    }

    /*----- Processing player actions -----*/

    /**
     * The current player bids on a given start item.
     * 
     * @param playerName The name of the current player (for checking purposes).
     * @param itemName The name of the start item on which the bid is placed.
     * @param amount The bid amount.
     */
    protected abstract boolean bid(String playerName, BidStartItem startItem);

    /**
     * Buy a start item against the base price.
     * 
     * @param playerName Name of the buying player.
     * @param itemName Name of the bought start item.
     * @param sharePrice If nonzero: share price if item contains a President's
     * share
     * @return False in case of any errors.
     */

    protected boolean buy(String playerName, BuyStartItem boughtItem) {
        StartItem item = boughtItem.getStartItem();
        String errMsg = null;
        Player player = getCurrentPlayer();
        int price = 0;
        int sharePrice = 0;
        String shareCompName = "";

        while (true) {
            if (!boughtItem.setSharePriceOnly()) {
                if (item.getStatus() != StartItem.BUYABLE) {
                    errMsg = LocalText.getText("NotForSale");
                    break;
                }

                price = item.getBasePrice();
                if (item.getBid() > price) price = item.getBid();

                if (player.getFreeCash() < price) {
                    errMsg = LocalText.getText("NoMoney");
                    break;
                }
            } else {
                price = item.getBid();
            }

            if (boughtItem.hasSharePriceToSet()) {
                shareCompName = boughtItem.getCompanyToSetPriceFor();
                sharePrice = boughtItem.getAssociatedSharePrice();
                if (sharePrice == 0) {
                    errMsg =
                            LocalText.getText("NoSharePriceSet", shareCompName);
                    break;
                }
                if ((StockMarket.getInstance().getStartSpace(sharePrice)) == null) {
                    errMsg =
                            LocalText.getText("InvalidStartPrice",
                                    new String[] { Bank.format(sharePrice),
                                            shareCompName });
                    break;
                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantBuyItem", new String[] {
                    playerName, item.getName(), errMsg }));
            return false;
        }

        MoveSet.start(false);

        assignItem(player, item, price, sharePrice);

        // Set priority
        gameManager.setPriorityPlayer();
        setNextPlayer();

        auctionItemState.set(null);
        numPasses.set(0);

        return true;

    }

    /**
     * This method executes the start item buy action.
     * 
     * @param player Buying player.
     * @param item Start item being bought.
     * @param price Buy price.
     */
    protected void assignItem(Player player, StartItem item, int price,
            int sharePrice) {
        Certificate primary = item.getPrimary();
        ReportBuffer.add(LocalText.getText("BuysItemFor", new String[] {
                player.getName(), primary.getName(), Bank.format(price) }));
        player.buy(primary, price);
        checksOnBuying(primary, sharePrice);
        if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            ReportBuffer.add(LocalText.getText("ALSO_GETS", new String[] {
                    player.getName(), extra.getName() }));
            player.buy(extra, 0);
            checksOnBuying(extra, sharePrice);
        }
        item.setSold(player, price);
    }

    protected void checksOnBuying(Certificate cert, int sharePrice) {
        if (cert instanceof PublicCertificateI) {
            PublicCertificateI pubCert = (PublicCertificateI) cert;
            PublicCompanyI comp = pubCert.getCompany();
            // Start the company, look for a fixed start price
            if (!comp.hasStarted()) {
                if (!comp.hasStockPrice()) {
                    comp.start();
                } else if (pubCert.isPresidentShare()) {
                    /* Company to be started. Check if it has a start price */
                    if (sharePrice > 0) {
                        // User has told us the start price
                        comp.start(sharePrice);
                    } else if (comp.getParPrice() != null) {
                        // Company has a fixed start price
                        comp.start();
                    } else {
                        log.error("No start price for " + comp.getName());
                    }
                }
            }
            if (comp.hasStarted() && !comp.hasFloated()) {
                checkFlotation(comp);
            }

        }
    }

    /**
     * Process a player's pass.
     * 
     * @param playerName The name of the current player (for checking purposes).
     */
    protected abstract boolean pass(String playerName);

    /*----- Setting up the UI for the next action -----*/

   /**
     * Get the currentPlayer index in the player list (starting at 0).
     * 
     * @return The index of the current Player.
     * @see GameManager.getCurrentPlayerIndex().
     */
    public int getCurrentPlayerIndex() {
        return gameManager.getCurrentPlayerIndex();
    }

    protected void setPriorityPlayer() {
        setCurrentPlayer(gameManager.getPriorityPlayer());
        currentPlayer = getCurrentPlayer();
    }

    protected void setPlayer(Player player) {
        setCurrentPlayer(player);
        currentPlayer = player;
    }

    protected void setNextPlayer() {
        setCurrentPlayerIndex(getCurrentPlayerIndex() + 1);
        currentPlayer = getCurrentPlayer();
    }

    /**
     * Get a list of items that may be bought immediately.
     * 
     * @return An array of start items, possibly empry.
     */

    public abstract List<StartItem> getStartItems();

    /**
     * Get a list of items that the current player may bid upon.
     * 
     * @return An array of start items, possibly empty.
     */

    public StartPacket getStartPacket() {
        return startPacket;
    }

    public boolean hasBidding() {
        return hasBidding;
    }

    public boolean hasBasePrices() {
        return hasBasePrices;
    }

    public ModelObject getBidModel(int privateIndex, int playerIndex) {
        return (itemsToSell.get(privateIndex)).getBidForPlayerModel(playerIndex);
    }

    public ModelObject getMinimumBidModel(int privateIndex) {
        return (itemsToSell.get(privateIndex)).getMinimumBidModel();
    }

    public ModelObject getFreeCashModel(int playerIndex) {
        return gameManager.getPlayerByIndex(playerIndex).getFreeCashModel();
    }

    public ModelObject getBlockedCashModel(int playerIndex) {
        return gameManager.getPlayerByIndex(playerIndex).getBlockedCashModel();
    }

}
