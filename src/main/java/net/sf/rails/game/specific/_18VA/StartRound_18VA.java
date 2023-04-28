package net.sf.rails.game.specific._18VA;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.Certificate;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.MoneyOwner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import java.util.Set;

/**
 * Implements an 1830-style initial auction.
 */
public class StartRound_18VA extends StartRound {
    private static final Logger log = LoggerFactory.getLogger(net.sf.rails.game.specific._18VA.StartRound_18VA.class);

    protected final int bidIncrement;
    protected Player firstPasser = null;

    public static final String boName = "B&O";
    public final PublicCompany bo = companyManager.getPublicCompany(boName);
    Player boBuyer;
    StartItem boItem;

    private final GenericState<StartItem> auctionItemState =
            new GenericState<>(this, "auctionItemState");

    /**
     * Constructed via Configure
     */
    public StartRound_18VA(GameManager parent, String id) {
        super(parent, id);
        bidIncrement = startPacket.getModulus();
    }

    @Override
    public void start() {
        super.start();
        auctionItemState.set(null);
        setPossibleActions();
    }

    /*
    public boolean process(PossibleAction action) {

        if (!super.process(action)) return false;

        if (action instanceof BuyStartItem && action.getPlayer().equals(boBuyer)) {
            BuyStartItem bsi = (BuyStartItem) action;
            int price = bsi.getAssociatedSharePrice();
            PublicCompany bo = ((PublicCertificate)bsi.getStartItem().getPrimary()).getCompany();
            log.info("B&O share price = {}", price);
        }
        return true;
    }*/

    /**
     * In 1826, starting the B&O involves two steps:
     * 1. The certificate, as a part of the Start Packet, is bought
     * but not yet started.
     * In this step, the action attribute 'companyNeedingSharePrice'
     * is set to null, so no price request happens.
     * This needs no further action here.
     * 2. Only if the whole Start Packet has been sold, the price is
     * requested and set. The B&O starts when this is complete.
     * In this step, the B&O is started after setting the price.
     */
    @Override
    protected void checksOnBuying(Certificate cert, int sharePrice) {
        if (cert instanceof PublicCertificate) {
            PublicCertificate pubCert = (PublicCertificate) cert;
            PublicCompany comp = pubCert.getCompany();
            if (!comp.equals(bo)) return; // Impossible

            // Step 1
            if (sharePrice == 0) return;
            // Step 2
            comp.start(sharePrice);
        }
    }

    @Override
    public boolean setPossibleActions() {

        boolean passAllowed = true;

        possibleActions.clear();

        if (playerManager.getCurrentPlayer() == startPlayer) ReportBuffer.add(this, "");

        // FIXME: Rails 2.0 Could be an infinite loop if there if no player has enough money to buy an item
        while (possibleActions.isEmpty()) {
            Player currentPlayer = playerManager.getCurrentPlayer();

            for (StartItem item : itemsToSell.view()) {

                if (item.isSold()) {
                    // Don't include
                } else if (item.getStatus() == StartItem.NEEDS_SHARE_PRICE) {
                    /* B&O */
                    playerManager.setCurrentPlayer(item.getBidder());
                    possibleActions.add(new BuyStartItem(item, item.getBid(), false, true));
                    passAllowed = false;
                    break; // No more actions
                } else {
                    int currentBid = item.getBid();
                    item.setMinimumBid(currentBid > 0 ? currentBid + bidIncrement : item.getBasePrice());
                    item.setStatus(StartItem.BIDDABLE);
                    if (currentPlayer.getFreeCash()
                            + item.getBid(currentPlayer) >= item.getMinimumBid()) {
                        BidStartItem possibleAction =
                                new BidStartItem(item, item.getMinimumBid(),
                                        startPacket.getModulus(), false);
                        possibleActions.add(possibleAction);
                    }
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

        if (passAllowed) {
            possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
        }

        return true;
    }

    /*----- moveStack methods -----*/

    /**
     * The current player bids on a given start item.
     *
     * @param playerName The name of the current player (for checking purposes).
     * @param bidItem    The start item on which the bid is placed.
     */
    @Override
    protected boolean bid(String playerName, BidStartItem bidItem) {

        StartItem item = bidItem.getStartItem();
        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();
        int previousBid = 0;
        int bidAmount = bidItem.getActualBid();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getId());
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
                errMsg = LocalText.getText("ActionNotAllowed",
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
                errMsg = LocalText.getText("BidTooLow", ""
                        + item.getMinimumBid());
                break;
            }

            // Bid must be a multiple of the modulus
            if (bidAmount % startPacket.getModulus() != 0) {
                errMsg = LocalText.getText("BidMustBeMultipleOf",
                        bidAmount,
                        startPacket.getMinimumIncrement());
                break;
            }

            // Has the buyer enough cash?
            previousBid = item.getBid(player);
            int available = player.getFreeCash() + previousBid;
            if (bidAmount > available) {
                errMsg = LocalText.getText("BidTooHigh", Bank.format(this, available));
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidBid",
                    playerName,
                    item.getId(),
                    errMsg));
            return false;
        }


        item.setBid(bidAmount, player);
        if (previousBid > 0) player.unblockCash(previousBid);
        player.blockCash(bidAmount);
        ReportBuffer.add(this, LocalText.getText("BID_ITEM_LOG",
                playerName,
                Bank.format(this, bidAmount),
                item.getId(),
                Bank.format(this, player.getFreeCash())));

        playerManager.setCurrentToNextPlayer();

        numPasses.set(0);

        return true;

    }

    /**
     * Process a player's pass.
     *
     * @param playerName The name of the current player (for checking purposes).
     */
    @Override
    protected boolean pass(NullAction action, String playerName) {

        String errMsg = null;
        Player player = playerManager.getCurrentPlayer();

        while (true) {

            // Check player
            if (!playerName.equals(player.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, player.getId());
                break;
            }
            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("InvalidPass",
                    playerName,
                    errMsg));
            return false;
        }

        ReportBuffer.add(this, LocalText.getText("PASSES", playerName));

        numPasses.add(1);
        if (numPasses.value() == 1) firstPasser = player;

        if (numPasses.value() >= playerManager.getNumberOfPlayers()) {
            // All players have passed.
            gameManager.reportAllPlayersPassed();
            playerManager.setPriorityPlayer(firstPasser);

            // Assign all biddable items that have at least one bid
            for (StartItem item : startPacket.getUnsoldItems()) {
                log.debug("Unsold item: {} bid={}", item, item.getBid());
                player = item.getBidder();
                if (item.getBidder() != null) {
                    int price = item.getBid();
                    assignItem(player, item, price, 0);

                }
            }
            if (!startPacket.getUnsoldItems().isEmpty()) {
                // If any item has not been bid upon, reduce its price by 10.
                for (StartItem item : startPacket.getUnsoldItems()) {
                    item.reduceBasePriceBy(10);
                    ReportBuffer.add(this, LocalText.getText(
                            "ITEM_PRICE_REDUCED",
                            startPacket.getFirstUnsoldItem().getId(),
                            Bank.format(this, startPacket.getFirstUnsoldItem().getBasePrice())));
                    numPasses.set(0);
                    if (item.getBasePrice() == 0) {
                        // Assign it to the priority holder
                        assignItem(playerManager.getPriorityPlayer(), item, 0, 0);
                        playerManager.setPriorityPlayerToNext();
                    }
                }
            }
            if (startPacket.getUnsoldItems().isEmpty()) {
                // Finish start round. B&O owner must now set the share price.
                if (bo.hasFloated()) {
                    finishRound();
                } else {
                    playerManager.setCurrentPlayer(boBuyer);
                    boItem.setStatus(StartItem.NEEDS_SHARE_PRICE);
                }
            }
        } else {
            playerManager.setCurrentToNextPlayer();
        }

        return true;
    }


    private void setNextBiddingPlayer(StartItem item, Player biddingPlayer) {
        for (Player player : playerManager.getNextPlayersAfter(biddingPlayer, false, false)) {
            if (item.isActive(player)) {
                playerManager.setCurrentPlayer(player);
                break;
            }
        }
    }

    private void setNextBiddingPlayer(StartItem item) {
        setNextBiddingPlayer(item, playerManager.getCurrentPlayer());
    }

    /** See Javadoc of checksOnBuying() */
    protected void assignItem(Player player, StartItem item, int price,
                              int sharePrice) {

        if (item.getDisplayName().equals(boName)) {
            if (sharePrice == 0) {
                // Step 1
                boBuyer = player;
                boItem = item;
                super.assignItem(player, item, price, sharePrice);
                //item.setStatus(StartItem.NEEDS_SHARE_PRICE);
                log.debug ("B&O step 1 done");
            } else {
                // Step 2
                bo.start(sharePrice);
                Currency.fromBank(2 * sharePrice, bo);
                bo.setFloated();
                boBuyer = null;
                item.setStatus(StartItem.SOLD);
                log.debug ("B&O step 2 is done");
            }
        } else {
            super.assignItem(player, item, price, sharePrice);
            log.debug ("Item {} assigned to {}", item, player);
        }
    }
}




