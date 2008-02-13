/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/specific/_18EU/StartRound_18EU.java,v 1.1 2008/02/13 20:04:25 evos Exp $ */
package rails.game.specific._18EU;

import java.util.List;

import rails.game.Bank;
import rails.game.DisplayBuffer;
import rails.game.GameManager;
import rails.game.Player;
import rails.game.ReportBuffer;
import rails.game.StartItem;
import rails.game.StartPacket;
import rails.game.StartRound;
import rails.game.action.BidStartItem;
import rails.game.action.BuyStartItem;
import rails.game.action.NullAction;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.game.state.State;
import rails.util.LocalText;


/**
 * Implements an 1835-style startpacket sale.
 */
public class StartRound_18EU extends StartRound
{

    public final static int SELECT_STEP = 0;
    public final static int OPEN_STEP = 1;
    public final static int BUY_STEP = 2;
    public final static int BID_STEP = 3;

	private final IntegerState currentStep
	    = new IntegerState ("CurrentStep", SELECT_STEP);

	private final State selectingPlayer
	    = new State ("SelectingPlayer", Player.class);

	private final IntegerState currentBuyPrice
	    = new IntegerState ("CurrentBuyPrice", 0);

	private final State currentAuctionItem
	    = new State ("CurrentAuctionItem", StartItem.class);

	/**
	 * Constructor, only to be used in dynamic instantiation.
	 */
	public StartRound_18EU()
	{
		super();
		hasBidding = true;
		hasBasePrices = false;
	}

	/**
	 * Start the 18EU-style start round.
	 *
	 * @param startPacket
	 *            The startpacket to be sold in this start round.
	 */
	@Override
    public void start(StartPacket startPacket)
	{
		super.start(startPacket);

		setStep(SELECT_STEP);

		setPossibleActions();
	}

	/**
	 * Get a list of items that may be bought immediately.
	 * <p>
	 * In an 1835-style auction this method will usually return several items.
	 *
	 * @return An array of start items that can be bought.
	 */
	//public StartItem[] getBuyableItems() {return null;}

	@Override
    public boolean setPossibleActions() {

		possibleActions.clear();
        boolean passAllowed = false;

        // Refresh player, may have been reset by Undo/Redo
        currentPlayer = GameManager.getCurrentPlayer();

		switch (getStep()) {
		case SELECT_STEP:
		    // In the selection step, all not yet sold items are buyable.
		    // The current player MUST select an item,
		    // and may then bid for it or pass.

		    selectingPlayer.set(getCurrentPlayer());
		    currentBuyPrice.set(100);

	        for (StartItem item : itemsToSell)
	        {
	            if (!item.isSold()) {
	                item.setStatus(StartItem.BIDDABLE);
	                item.setMinimumBid(item.getBasePrice());
	                BidStartItem possibleAction = new BidStartItem (
	                        item,
	                        item.getBasePrice(),
	                        startPacket.getModulus(),
	                        false, true);
	                possibleActions.add (possibleAction);
	                ///item.setStatus (StartItem.BIDDABLE);
	            }
	        }
	        break;

		case BUY_STEP:

	        possibleActions.add(new BuyStartItem(
	          (StartItem)currentAuctionItem.getObject(),
	          currentBuyPrice.intValue(),
	          true));
		    passAllowed = true;

		    break;

		case OPEN_STEP:
		case BID_STEP:

	        StartItem item = (StartItem)currentAuctionItem.getObject();
	        BidStartItem possibleAction = new BidStartItem(
                    item,
                    item.getMinimumBid(),
                    startPacket.getModulus(),
                    true);
            possibleActions.add(possibleAction);
		    passAllowed = true;

		    break;
		}

		if (passAllowed) possibleActions.add (new NullAction (NullAction.PASS));

		return true;
	}

	@Override
    public List<StartItem> getStartItems()
	{
	    return startPacket.getItems();
	}

    @Override
    protected boolean buy(String playerName, BuyStartItem boughtItem)
    {
        StartItem item = boughtItem.getStartItem();
        int status = boughtItem.getStatus();
        String errMsg = null;
        Player player = GameManager.getCurrentPlayer();
        int price = 0;

        while (true)
        {

            // Is the item buyable?
            if (status == StartItem.BUYABLE) {
                price = currentBuyPrice.intValue();
            } else {
                errMsg = LocalText.getText("NotForSale");
                break;
            }

            if (status == StartItem.BUYABLE
                    && player.getFreeCash() < price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null)
        {
            DisplayBuffer.add (LocalText.getText("CantBuyItem", new String[] {
                    playerName,
                    item.getName(),
                    errMsg
                }));
            return false;
        }

        MoveSet.start(false);

        assignItem(player, item, price, 0);
        setNextSelectingPlayer();
        setStep(SELECT_STEP);

        return true;

    }

	/**
	 * The current player bids on a given start item.
	 *
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 * @param itemName
	 *            The name of the start item on which the bid is placed.
	 * @param amount
	 *            The bid amount.
	 */
	@Override
    protected boolean bid(String playerName, BidStartItem bidItem)
	{

		StartItem item = bidItem.getStartItem();
		StartItem auctionedItem = (StartItem) currentAuctionItem.getObject();
		String errMsg = null;
		Player player = GameManager.getCurrentPlayer();
		int bidAmount = bidItem.getActualBid();

		while (true)
		{

			// If the bidAmount is -1, this is just a selection for auctioning
			if (bidAmount == -1) {
				if (!bidItem.isSelectForAuction()) {
					errMsg = LocalText.getText("NotBiddable");
					break;
				}
			} else {

				// If auctioning, must be the right item
				if ((getStep() == OPEN_STEP || getStep() == BID_STEP)
						&& !item.equals(auctionedItem)) {
					errMsg = LocalText.getText("WrongStartItem",
							new String[] {
								item.getName(),
								auctionedItem.getName()
					});
					break;
				}

				// Bid must be at least the minimum bid
				if (bidAmount < item.getMinimumBid())
				{
					errMsg = LocalText.getText("BidTooLow", Bank.format(item.getMinimumBid()));
					break;
				}

				// Bid must be a multiple of the modulus
				if (bidAmount % startPacket.getModulus() != 0) {
					errMsg = LocalText.getText("BidMustBeMultipleOf", new String[] {
							String.valueOf(bidAmount),
							String.valueOf(startPacket.getMinimumIncrement())}
					);
					break;
				}

				// Player must have enough cash
				if (bidAmount > player.getCash())
				{
					errMsg = LocalText.getText("BidTooHigh", Bank.format(bidAmount));
					break;
				}
			}

			break;
		}

		if (errMsg != null)
		{
			DisplayBuffer.add(LocalText.getText("InvalidBid", new String[]{
					playerName,
					item.getName(),
					errMsg
				}));
			return false;
		}

		MoveSet.start(false);

		if (getStep() == SELECT_STEP) {

			currentAuctionItem.set(item);
			item.setStatus(StartItem.AUCTIONED);
			if (bidAmount == -1) {
				setStep(OPEN_STEP);
			}

			ReportBuffer.add(LocalText.getText("SELECT_ITEM",
					new String[] {
						playerName,
						item.getName()
			}));
		}

		if (bidAmount > 0) {
			item.setBid(bidAmount, player);
			item.setMinimumBid(bidAmount + 5);
		    setStep(BID_STEP);

			ReportBuffer.add(LocalText.getText("BID_ITEM", new String[] {
					playerName,
					Bank.format(bidAmount),
					item.getName()
				}));
		}

		switch (getStep()) {
		case OPEN_STEP:
		case BUY_STEP:
		    setNextPlayer();
		    if (currentPlayer == selectingPlayer.getObject()) {
		        // All have passed, now lower the buy price
		        currentBuyPrice.add(-10);
		        setStep (BUY_STEP);
		        if (currentBuyPrice.intValue() == 0) {
		            // Forced buy
		            assignItem (currentPlayer, item, 0, 0);
		        }
		    }
		    break;

		case BID_STEP:
		    setNextBiddingPlayer();
		}

		return true;

	}

	/**
	 * Process a player's pass.
	 *
	 * @param playerName
	 *            The name of the current player (for checking purposes).
	 */
	@Override
    public boolean pass(String playerName)
	{
	    // All validations have already been done

		ReportBuffer.add(LocalText.getText("PASSES", playerName));

		MoveSet.start(false);

		StartItem auctionedItem = (StartItem) currentAuctionItem.getObject();

		switch (getStep()) {
        case OPEN_STEP:
        case BUY_STEP:
            setNextPlayer();
            if (currentPlayer == selectingPlayer.getObject()) {
                // All have passed, now lower the buy price
                currentBuyPrice.add(-10);
                auctionedItem.setMinimumBid(currentBuyPrice.intValue());
                ReportBuffer.add(LocalText.getText("ITEM_PRICE_REDUCED",
                        new String[] {
                            auctionedItem.getName(),
                            Bank.format(currentBuyPrice.intValue())
                    }));
                setStep (BUY_STEP);
                if (auctionedItem.getStatus() != StartItem.BUYABLE) {
                    auctionedItem.setStatus(StartItem.BUYABLE);
                }
                if (currentBuyPrice.intValue() == 0) {
                    // Forced buy
                    // Trick to make the zero buy price visible
                    auctionedItem.setBid(0, currentPlayer);

                    assignItem (currentPlayer, auctionedItem, 0, 0);
                    setStep(SELECT_STEP);
                    setNextSelectingPlayer();
                }
            }
            break;

		case BID_STEP:

			auctionedItem.setBid(-1, currentPlayer);

			// We are done if the next still bidding player
			// is equal to the current highest bidder.
		    setNextBiddingPlayer();
		    if (currentPlayer == auctionedItem.getBidder()) {
				// Finish bidding
			    assignItem (auctionedItem.getBidder(),
			            auctionedItem, auctionedItem.getBid(), 0);
			    setStep(SELECT_STEP);
                setNextSelectingPlayer();
			}
		}

		return true;
	}

	private void setNextBiddingPlayer () {

	    do {
	        setNextPlayer();
	    } while (((StartItem)currentAuctionItem.getObject()).getBid(currentPlayer) < 0);
	}

	private void setNextSelectingPlayer() {
        setPlayer ((Player)selectingPlayer.getObject());
        setNextPlayer();
	}

	public int getStep() {
        return currentStep.intValue();
    }

    public void setStep(int step) {
        if (step != currentStep.intValue()) {
            currentStep.set(step);
        }
    }

    @Override
    public String getHelp() {
	    return "1835 Start Round help text";
	}


}
