package rails.game;

import java.util.ArrayList;
import java.util.List;

import rails.common.DisplayBuffer;
import rails.common.GuiDef;
import rails.common.LocalText;
import rails.common.parser.GameOption;
import rails.game.action.*;
import rails.game.model.AbstractModel;
import rails.game.model.Model;
import rails.game.model.MoneyModel;
import rails.game.state.ArrayListState;
import rails.game.state.GenericState;
import rails.game.state.IntegerState;

public abstract class StartRound extends Round {

    protected StartPacket startPacket = null;
    protected int[] itemIndex;
    protected ArrayListState<StartItem> itemsToSell = null;
    protected GenericState<StartItem> auctionItemState =
            new GenericState<StartItem>(this, "AuctionItem");
    protected IntegerState numPasses = new IntegerState(this, "StartRoundPasses");
    protected int numPlayers;
    protected String variant;
    protected Player currentPlayer;
    protected Player startPlayer;

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
    public StartRound(GameManagerI gameManager) {

        super (gameManager);
        this.startPacket = gameManager.getStartPacket();

        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.START_ROUND);
    }

    /**
     * Start the start round.
     *
     * @param startPacket The startpacket to be sold in this start round.
     */
    public void start() {

        this.variant = gameManager.getGameOption(GameOption.VARIANT);
        if (variant == null) variant = "";
        numPlayers = gameManager.getNumberOfPlayers();

        itemsToSell = new ArrayListState<StartItem>(this, "itemsToSell");
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

        setCurrentPlayerIndex(gameManager.getPriorityPlayer().getIndex());
        currentPlayer = getCurrentPlayer();
        startPlayer = currentPlayer;

        ReportBuffer.add(LocalText.getText("StartOfInitialRound"));
        ReportBuffer.add(LocalText.getText("HasPriority",
                getCurrentPlayer().getId()));
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
        
        startPacketChecks();

        if (startPacket.areAllSold()) {
            /*
             * If the complete start packet has been sold, start a Stock round,
             */
            possibleActions.clear();
            finishRound();
        }

        return result;
    }

    /** Stub to allow start packet cleanups in subclasses */
    protected void startPacketChecks() {
        return;
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
        int lastBid = item.getBid();
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
                if ((stockMarket.getStartSpace(sharePrice)) == null) {
                    errMsg =
                            LocalText.getText("InvalidStartPrice",
                                    Bank.format(sharePrice),
                                    shareCompName );
                    break;
                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantBuyItem",
                    playerName,
                    item.getName(),
                    errMsg ));
            return false;
        }

        changeStack.start(false);

        assignItem(player, item, price, sharePrice);

        // Set priority (only if the item was not auctioned)
        // ASSUMPTION: getting an item in auction mode never changes priority
        if (lastBid == 0) {
            gameManager.setPriorityPlayer();
        }
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
        ReportBuffer.add(LocalText.getText("BuysItemFor",
                player.getId(),
                primary.getId(),
                Bank.format(price) ));
        pay (player, bank, price);
        transferCertificate (primary, player.getPortfolio());
        checksOnBuying(primary, sharePrice);
        if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            ReportBuffer.add(LocalText.getText("ALSO_GETS",
                    player.getId(),
                    extra.getId() ));
            transferCertificate (extra, player.getPortfolio());
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
                    } else if (comp.getIPOPrice() != 0) {
                        // Company has a known start price
                        comp.start();
                    } else {
                        log.error("No start price for " + comp.getId());
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

    @Override
    protected void finishRound() {
        super.finishRound();
    }

        /*----- Setting up the UI for the next action -----*/

   /**
     * Get the currentPlayer index in the player list (starting at 0).
     *
     * @return The index of the current Player.
     * @see GameManager.getCurrentPlayerIndex().
     */
    @Override
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
     * Get the current list of start items.
     *
     * @return An array of start items, possibly empry.
     */

    public List<StartItem> getStartItems() {

        return itemsToSell.view();
    }

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

    public Model<String> getBidModel(int privateIndex, int playerIndex) {
        return (itemsToSell.get(privateIndex)).getBidForPlayerModel(playerIndex);
    }

    public Model<String> getMinimumBidModel(int privateIndex) {
        return (itemsToSell.get(privateIndex)).getMinimumBidModel();
    }

    public Model<String> getFreeCashModel(int playerIndex) {
        return gameManager.getPlayerByIndex(playerIndex).getFreeCashModel();
    }

    public Model<String> getBlockedCashModel(int playerIndex) {
        return gameManager.getPlayerByIndex(playerIndex).getBlockedCashModel();
    }

}
