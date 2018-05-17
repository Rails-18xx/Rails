using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Implements an 1830-style initial auction.
 */

namespace GameLib.Net.Game
{
    public class StartRound_1830 : StartRound
    {
        protected int bidIncrement;

        private GenericState<StartItem> auctionItemState;

        /**
         * Constructed via Configure
         */
        public StartRound_1830(GameManager parent, String id) : base(parent, id)
        {
            auctionItemState = GenericState<StartItem>.Create(this, "auctionItemState");
            bidIncrement = startPacket.Modulus;
        }

        override public void Start()
        {
            base.Start();
            auctionItemState.Set(null);
            SetPossibleActions();
        }

        override public bool Process(PossibleAction action)
        {
            if (!base.Process(action)) return false;

            // Assign any further items that have been bid exactly once
            // and don't need any further player intervention, such
            // as setting a start price
            StartItem item;
            while ((item = startPacket.GetFirstUnsoldItem()) != null
                    && item.GetBidders() == 1 && item.NeedsPriceSetting() == null)
            {
                AssignItem(item.GetBidder(), item, item.GetBid(), 0);

                // Check if this has exhausted the start packet
                if (startPacket.AreAllSold())
                {
                    FinishRound();
                    break;
                }
            }
            return true;
        }

        override public bool SetPossibleActions()
        {

            bool passAllowed = true;

            possibleActions.Clear();

            if (playerManager.CurrentPlayer == startPlayer) ReportBuffer.Add(this, "");

            // FIXME: Rails 2.0 Could be an infinite loop if there if no player has enough money to buy an item
            while (possibleActions.IsEmpty)
            {
                Player currentPlayer = playerManager.CurrentPlayer;

                foreach (StartItem item in itemsToSell.View())
                {

                    if (item.IsSold)
                    {
                        // Don't include
                    }
                    else if (item.GetStatus() == StartItem.AUCTIONED)
                    {

                        if (currentPlayer.FreeCash
                            + item.GetBid(currentPlayer) >= item.GetMinimumBid())
                        {
                            BidStartItem possibleAction =
                                    new BidStartItem(item,
                                            item.GetMinimumBid(),
                                            startPacket.Modulus, true);
                            possibleActions.Add(possibleAction);
                            break; // No more actions
                        }
                        else
                        {
                            // Can't bid: Autopass
                            numPasses.Add(1);
                            break;
                        }
                    }
                    else if (item.GetStatus() == StartItem.NEEDS_SHARE_PRICE)
                    {
                        /* This status is set in buy() if a share price is missing */
                        playerManager.SetCurrentPlayer(item.GetBidder());
                        possibleActions.Add(new BuyStartItem(item, item.GetBid(), false, true));
                        passAllowed = false;
                        break; // No more actions
                    }
                    else if (item == startPacket.GetFirstUnsoldItem())
                    {
                        if (item.GetBidders() == 1)
                        {
                            // Bid upon by one player.
                            // If we need a share price, ask for it.
                            PublicCompany comp = item.NeedsPriceSetting();
                            if (comp != null)
                            {
                                playerManager.SetCurrentPlayer(item.GetBidder());
                                item.SetStatus(StartItem.NEEDS_SHARE_PRICE);
                                BuyStartItem newItem =
                                        new BuyStartItem(item, item.GetBasePrice(),
                                                true, true);
                                possibleActions.Add(newItem);
                                break; // No more actions possible!
                            }
                            else
                            {
                                // ERROR, this should have been detected in process()!
                                log.Error("??? Wrong place to assign item " + item.Id);
                                AssignItem(item.GetBidder(), item, item.GetBid(), 0);
                            }
                        }
                        else if (item.GetBidders() > 1)
                        {
                            ReportBuffer.Add(this, LocalText.GetText("TO_AUCTION",
                                    item.Id));
                            // Start left of the currently highest bidder
                            if (item.GetStatus() != StartItem.AUCTIONED)
                            {
                                SetNextBiddingPlayer(item, item.GetBidder());
                                currentPlayer = playerManager.CurrentPlayer;
                                item.SetStatus(StartItem.AUCTIONED);
                                auctionItemState.Set(item);
                            }
                            if (currentPlayer.FreeCash
                                + item.GetBid(currentPlayer) >= item.GetMinimumBid())
                            {
                                BidStartItem possibleAction =
                                        new BidStartItem(item,
                                                item.GetMinimumBid(),
                                                startPacket.Modulus, true);
                                possibleActions.Add(possibleAction);
                            }
                            break; // No more possible actions!
                        }
                        else
                        {
                            item.SetStatus(StartItem.BUYABLE);
                            if (currentPlayer.FreeCash >= item.GetBasePrice())
                            {
                                possibleActions.Add(new BuyStartItem(item,
                                        item.GetBasePrice(), false));
                            }
                        }
                    }
                    else
                    {
                        item.SetStatus(StartItem.BIDDABLE);
                        if (currentPlayer.FreeCash
                            + item.GetBid(currentPlayer) >= item.GetMinimumBid())
                        {
                            BidStartItem possibleAction =
                                    new BidStartItem(item, item.GetMinimumBid(),
                                            startPacket.Modulus, false);
                            possibleActions.Add(possibleAction);
                        }
                    }

                }

                if (possibleActions.IsEmpty)
                {
                    numPasses.Add(1);
                    if (auctionItemState.Value == null)
                    {
                        playerManager.SetCurrentToNextPlayer();
                    }
                    else
                    {
                        SetNextBiddingPlayer(auctionItemState.Value);
                    }
                }
            }

            if (passAllowed)
            {
                possibleActions.Add(new NullAction(NullAction.Modes.PASS));
            }

            return true;
        }

        /*----- moveStack methods -----*/
        /**
         * The current player bids on a given start item.
         *
         * @param playerName The name of the current player (for checking purposes).
         * @param itemName The name of the start item on which the bid is placed.
         * @param amount The bid amount.
         */
        override protected bool Bid(string playerName, BidStartItem bidItem)
        {
            StartItem item = bidItem.StartItem;
            String errMsg = null;
            Player player = playerManager.CurrentPlayer;
            int previousBid = 0;
            int bidAmount = bidItem.ActualBid;

            while (true)
            {

                // Check player
                if (!playerName.Equals(player.Id))
                {
                    errMsg = LocalText.GetText("WrongPlayer", playerName, player.Id);
                    break;
                }
                // Check item
                bool validItem = false;
                foreach (StartItemAction activeItem in possibleActions.GetActionType<StartItemAction>())
                {
                    if (bidItem.EqualsAsOption(activeItem))
                    {
                        validItem = true;
                        break;
                    }

                }
                if (!validItem)
                {
                    errMsg = LocalText.GetText("ActionNotAllowed",
                                    bidItem.ToString());
                    break;
                }

                // Is the item buyable?
                if (bidItem.GetStatus() != StartItem.BIDDABLE
                    && bidItem.GetStatus() != StartItem.AUCTIONED)
                {
                    errMsg = LocalText.GetText("NotForSale");
                    break;
                }

                // Bid must be at least 5 above last bid
                if (bidAmount < item.GetMinimumBid())
                {
                    errMsg = LocalText.GetText("BidTooLow", ""
                                                           + item.GetMinimumBid());
                    break;
                }

                // Bid must be a multiple of the modulus
                if (bidAmount % startPacket.Modulus != 0)
                {
                    errMsg = LocalText.GetText("BidMustBeMultipleOf",
                                    bidAmount,
                                    startPacket.MinimumIncrement);
                    break;
                }

                // Has the buyer enough cash?
                previousBid = item.GetBid(player);
                int available = player.FreeCash + previousBid;
                if (bidAmount > available)
                {
                    errMsg = LocalText.GetText("BidTooHigh", Bank.Format(this, available));
                    break;
                }

                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("InvalidBid",
                        playerName,
                        item.Id,
                        errMsg));
                return false;
            }



            item.SetBid(bidAmount, player);
            if (previousBid > 0) player.UnblockCash(previousBid);
            player.BlockCash(bidAmount);
            ReportBuffer.Add(this, LocalText.GetText("BID_ITEM_LOG",
                    playerName,
                    Bank.Format(this, bidAmount),
                    item.Id,
                    Bank.Format(this, player.FreeCash)));

            if (bidItem.GetStatus() != StartItem.AUCTIONED)
            {
                playerManager.SetPriorityPlayerToNext();
                playerManager.SetCurrentToNextPlayer();
            }
            else
            {
                SetNextBiddingPlayer(item);
            }
            numPasses.Set(0);

            return true;

        }

        override protected bool Buy(string playerName, BuyStartItem boughtItem)
        {
            bool result = base.Buy(playerName, boughtItem);
            auctionItemState.Set(null);
            return result;
        }


        /**
         * Process a player's pass.
         * @param playerName The name of the current player (for checking purposes).
         */
        override protected bool Pass(NullAction action, string playerName)
        {

            string errMsg = null;
            Player player = playerManager.CurrentPlayer;
            StartItem auctionItem = auctionItemState.Value;

            while (true)
            {

                // Check player
                if (!playerName.Equals(player.Id))
                {
                    errMsg = LocalText.GetText("WrongPlayer", playerName, player.Id);
                    break;
                }
                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("InvalidPass",
                        playerName,
                        errMsg));
                return false;
            }

            ReportBuffer.Add(this, LocalText.GetText("PASSES", playerName));

            numPasses.Add(1);
            if (auctionItem != null)
            {

                if (numPasses.Value >= auctionItem.GetBidders() - 1)
                {
                    // All but the highest bidder have passed.
                    int price = auctionItem.GetBid();

                    log.Debug("Highest bidder is "
                              + auctionItem.GetBidder().Id);
                    if (auctionItem.NeedsPriceSetting() != null)
                    {
                        auctionItem.SetStatus(StartItem.NEEDS_SHARE_PRICE);
                    }
                    else
                    {
                        AssignItem(auctionItem.GetBidder(), auctionItem, price, 0);
                    }
                    auctionItemState.Set(null);
                    numPasses.Set(0);
                    // Next turn goes to priority holder
                    playerManager.SetCurrentToPriorityPlayer(); // EV - Added to fix bug 2989440
                }
                else
                {
                    // More than one left: find next bidder

                    if (GameOption.GetAsBoolean(this, "LeaveAuctionOnPass"))
                    {
                        // Game option: player to leave auction after a pass (default no).
                        player.UnblockCash(auctionItem.GetBid(player));
                        auctionItem.SetPass(player);
                    }
                    SetNextBiddingPlayer(auctionItem);
                }

            }
            else
            {

                if (numPasses.Value >= playerManager.NumberOfPlayers)
                {
                    // All players have passed.
                    gameManager.ReportAllPlayersPassed();
                    // It the first item has not been sold yet, reduce its price by 5.
                    if (startPacket.GetFirstItem() == startPacket.GetFirstUnsoldItem() || startPacket.GetFirstUnsoldItem().Reduceable)
                    {
                        startPacket.GetFirstUnsoldItem().ReduceBasePriceBy(5);
                        ReportBuffer.Add(this, LocalText.GetText(
                                "ITEM_PRICE_REDUCED",
                                        startPacket.GetFirstUnsoldItem().Id,
                                        Bank.Format(this, startPacket.GetFirstUnsoldItem().GetBasePrice())));
                        numPasses.Set(0);
                        if (startPacket.GetFirstUnsoldItem().GetBasePrice() == 0)
                        {
                            GetRoot.PlayerManager.SetCurrentToNextPlayer();
                            AssignItem(playerManager.CurrentPlayer,
                                    startPacket.GetFirstUnsoldItem(), 0, 0);
                            GetRoot.PlayerManager.SetPriorityPlayerToNext();
                            GetRoot.PlayerManager.SetCurrentToNextPlayer();
                        }
                        else
                        {
                            //BR: If the first item's price is reduced, but not to 0, 
                            //    we still need to advance to the next player
                            playerManager.SetCurrentToNextPlayer();
                        }
                    }
                    else
                    {
                        numPasses.Set(0);
                        //gameManager.nextRound(this);
                        FinishRound();
                    }
                }
                else
                {
                    playerManager.SetCurrentToNextPlayer();
                }
            }

            return true;
        }


        private void SetNextBiddingPlayer(StartItem item, Player biddingPlayer)
        {
            foreach (Player player in playerManager.GetNextPlayersAfter(biddingPlayer, false, false))
            {
                if (item.IsActive(player))
                {
                    playerManager.SetCurrentPlayer(player);
                    break;
                }
            }
        }

        private void SetNextBiddingPlayer(StartItem item)
        {
            SetNextBiddingPlayer(item, playerManager.CurrentPlayer);
        }

    }
}
