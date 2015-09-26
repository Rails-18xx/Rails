package net.sf.rails.game;

import java.util.List;

import rails.game.action.*;
import net.sf.rails.common.*;
import net.sf.rails.util.Util;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Model;


public abstract class StartRound extends Round {

    // FIXME: StartRounds do not set Priority Player
    
    // static at creation
    protected final StartPacket startPacket;
    protected final String variant;
    
    // static at start
    protected Player startPlayer;

    
    // The following have to be initialized by the sub-classes
    /**
     * Should the UI present bidding into and facilities? This value MUST be set
     * in the actual StartRound constructor.
     */
    protected final boolean hasBidding;

    /**
     * Should the UI show base prices? Not useful if the items are all equal, as
     * in 1841 and 18EU.
     */
    protected final boolean hasBasePrices;
    
    /**
     * Is buying allowed in the start round?  Not in the first start round of
     * 1880, for example, where everything is auctioned.
     */
    protected final boolean hasBuying;
    
    private String StartRoundName="Start of Initial StartRound";

    // dynamic variables
    protected final ArrayListState<StartItem> itemsToSell = ArrayListState.create(this, "itemsToSell");
    protected final IntegerState numPasses = IntegerState.create(this, "numPasses");

    protected StartRound(GameManager parent, String id, boolean hasBidding, boolean hasBasePrices, boolean hasBuying) {
        super (parent, id);
        this.hasBidding = hasBidding;
        this.hasBasePrices = hasBasePrices;
        this.hasBuying = hasBuying;
        
        this.startPacket = parent.getStartPacket();

        String variant =  GameOption.getValue(this, GameOption.VARIANT);
        this.variant = Util.valueWithDefault(variant, "");
        
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setActivePanel(GuiDef.Panel.START_ROUND);
    }
    
    protected StartRound(GameManager parent, String id) {
        // default case, set bidding, basePrices and buying all to true
        this(parent, id, true, true, true);
    }

    public void start() {
        for (StartItem item : startPacket.getItems()) {
            // New: we only include items that have not yet been sold
            // at the start of the current StartRound
            if (!item.isSold()) {
                itemsToSell.add(item);
            }
        }
        numPasses.set(0);
        
        // init current with priority player
        startPlayer = playerManager.setCurrentToPriorityPlayer();

        ReportBuffer.add(this, LocalText.getText("StartOfInitialRound"));
        ReportBuffer.add(this, LocalText.getText("HasPriority",
                startPlayer.getId()));
    }
    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;

        log.debug("Processing action " + action);

        if (action instanceof NullAction && 
                ((NullAction)action).getMode() == NullAction.Mode.PASS) {
            String playerName = action.getPlayerName();
            NullAction nullAction = (NullAction) action;
            result = pass(nullAction, playerName);

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
                    playerManager.setPriorityPlayerToNext();
                    result = true;
                } else {
                    result = buy(playerName, buyAction);
                }
            } else if (startItemAction instanceof BidStartItem) {
                result = bid(playerName, (BidStartItem) startItemAction);
            }
        } else {

            DisplayBuffer.add(this, LocalText.getText("UnexpectedAction",
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
        Player player = playerManager.getCurrentPlayer();
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
                                    Bank.format(this, sharePrice),
                                shareCompName );
                    break;
                }
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuyItem",
                    playerName,
                    item.getId(),
                    errMsg ));
            return false;
        }

        assignItem(player, item, price, sharePrice);

        // Set priority (only if the item was not auctioned)
        // ASSUMPTION: getting an item in auction mode never changes priority
        if (lastBid == 0) {
            playerManager.setPriorityPlayerToNext();
        }
        playerManager.setCurrentToNextPlayer();

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
        String priceText = Currency.toBank(player, price);
        ReportBuffer.add(this,LocalText.getText("BuysItemFor",
                player.getId(),
                primary.toText(),
                priceText ));
        primary.moveTo(player);
        checksOnBuying(primary, sharePrice);
        if (item.hasSecondary()) {
            Certificate extra = item.getSecondary();
            ReportBuffer.add(this,LocalText.getText("ALSO_GETS",
                    player.getId(),
                    extra.toText()));
            extra.moveTo(player);
            checksOnBuying(extra, sharePrice);
        }
        item.setSold(player, price);
    }

    protected void checksOnBuying(Certificate cert, int sharePrice) {
        if (cert instanceof PublicCertificate) {
            PublicCertificate pubCert = (PublicCertificate) cert;
            PublicCompany comp = pubCert.getCompany();
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
            if (comp.hasStarted()) comp.checkPresidency();  // Needed for 1835 BY
        }
    }

    /**
     * Process a player's pass.
     * @param action TODO
     * @param playerName The name of the current player (for checking purposes).
     */
    protected abstract boolean pass(NullAction action, String playerName);

    @Override
    protected void finishRound() {
        super.finishRound();
    }

    /*----- Setting up the UI for the next action -----*/

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
    
    public boolean hasBuying() {
        return hasBuying;
    }

    public boolean hasBasePrices() {
        return hasBasePrices;
    }

    public Model getBidModel(int privateIndex, Player player) {
        return (itemsToSell.get(privateIndex)).getBidForPlayerModel(player);
    }

    public Model getMinimumBidModel(int privateIndex) {
        return (itemsToSell.get(privateIndex)).getMinimumBidModel();
    }

    // TODO: Maybe this should be a subclass of a readableCashModel
    public Model getFreeCashModel(Player player) {
        return player.getFreeCashModel();
    }

    // TODO: Maybe this should be a subclass of a readableCashModel
    public Model getBlockedCashModel(Player player) {
        return player.getBlockedCashModel();
    }
    /**
     * @return the startRoundName
     */
    public String getStartRoundName() {
        return StartRoundName;
    }

    /**
     * @param startRoundName the startRoundName to set
     */
    public void setStartRoundName(String startRoundName) {
        StartRoundName = startRoundName;
    }

}
