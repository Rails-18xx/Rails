using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    abstract public class StartRound : Round, ICreatable
    {
        // FIXME: StartRounds do not set Priority Player

        // static at creation
        protected StartPacket startPacket;
        protected string variant;

        // static at start
        protected Player startPlayer;


        // The following have to be initialized by the sub-classes
        /**
         * Should the UI present bidding into and facilities? This value MUST be set
         * in the actual StartRound constructor.
         */
        protected bool hasBidding;

        /**
         * Should the UI show base prices? Not useful if the items are all equal, as
         * in 1841 and 18EU.
         */
        protected bool hasBasePrices;

        /**
         * Is buying allowed in the start round?  Not in the first start round of
         * 1880, for example, where everything is auctioned.
         */
        protected bool hasBuying;

        private string StartRoundName = "Start of Initial StartRound";

        // dynamic variables
        protected ListState<StartItem> itemsToSell;
        protected IntegerState numPasses;

        protected StartRound(GameManager parent, string id, bool hasBidding, bool hasBasePrices, bool hasBuying) : base(parent, id)
        {
            itemsToSell = ListState<StartItem>.Create(this, "itemsToSell");
            numPasses = IntegerState.Create(this, "numPasses");

            this.hasBidding = hasBidding;
            this.hasBasePrices = hasBasePrices;
            this.hasBuying = hasBuying;

            this.startPacket = parent.GetStartPacket();

            string variant = GameOption.GetValue(this, GameOption.VARIANT);
            this.variant = Util.Util.ValueWithDefault(variant, "");

            guiHints.SetVisibilityHint(GuiDef.Panel.STATUS, true);
            guiHints.SetVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.SetVisibilityHint(GuiDef.Panel.MAP, true);
            guiHints.ActivePanel = GuiDef.Panel.START_ROUND;
        }

        protected StartRound(GameManager parent, string id) : this(parent, id, true, true, true)
        {
            // default case, set bidding, basePrices and buying all to true
        }

        virtual public void Start()
        {
            foreach (StartItem item in startPacket.Items)
            {
                // New: we only include items that have not yet been sold
                // at the start of the current StartRound
                if (!item.IsSold)
                {
                    itemsToSell.Add(item);
                }
            }
            numPasses.Set(0);

            // init current with priority player
            startPlayer = playerManager.SetCurrentToPriorityPlayer();

            ReportBuffer.Add(this, LocalText.GetText("StartOfInitialRound"));
            ReportBuffer.Add(this, LocalText.GetText("HasPriority",
                    startPlayer.Id));
        }

        override public bool Process(PossibleAction action)
        {

            bool result = false;

            log.Debug("Processing action " + action);

            if (action is NullAction &&
                    ((NullAction)action).Mode == NullAction.Modes.PASS)
            {
                string playerName = action.PlayerName;
                NullAction nullAction = (NullAction)action;
                result = Pass(nullAction, playerName);

            }
            else if (action is StartItemAction)
            {
                StartItemAction startItemAction = (StartItemAction)action;
                string playerName = action.PlayerName;

                log.Debug("Item details: " + startItemAction.ToString());

                if (startItemAction is BuyStartItem)
                {
                    BuyStartItem buyAction = (BuyStartItem)startItemAction;
                    if (buyAction.HasSharePriceToSet
                            && buyAction.AssociatedSharePrice == 0)
                    {
                        // We still need a share price for this item
                        startItemAction.StartItem.SetStatus(
                                StartItem.NEEDS_SHARE_PRICE);
                        // We must set the priority player, though
                        playerManager.SetPriorityPlayerToNext();
                        result = true;
                    }
                    else
                    {
                        result = Buy(playerName, buyAction);
                    }
                }
                else if (startItemAction is BidStartItem)
                {
                    result = Bid(playerName, (BidStartItem)startItemAction);
                }
            }
            else
            {

                DisplayBuffer.Add(this, LocalText.GetText("UnexpectedAction",
                        action.ToString()));
            }

            StartPacketChecks();

            if (startPacket.AreAllSold())
            {
                /*
                 * If the complete start packet has been sold, start a Stock round,
                 */
                possibleActions.Clear();
                FinishRound();
            }

            return result;
        }

        /** Stub to allow start packet cleanups in subclasses */
        virtual protected void StartPacketChecks()
        {
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
        protected abstract bool Bid(string playerName, BidStartItem startItem);

        /**
         * Buy a start item against the base price.
         *
         * @param playerName Name of the buying player.
         * @param itemName Name of the bought start item.
         * @param sharePrice If nonzero: share price if item contains a President's
         * share
         * @return False in case of any errors.
         */

        virtual protected bool Buy(string playerName, BuyStartItem boughtItem)
        {
            StartItem item = boughtItem.StartItem;
            int lastBid = item.GetBid();
            string errMsg = null;
            Player player = playerManager.CurrentPlayer;
            int price = 0;
            int sharePrice = 0;
            string shareCompName = "";

            while (true)
            {
                if (!boughtItem.SetSharePriceOnly)
                {
                    if (item.GetStatus() != StartItem.BUYABLE)
                    {
                        errMsg = LocalText.GetText("NotForSale");
                        break;
                    }

                    price = item.GetBasePrice();
                    if (item.GetBid() > price) price = item.GetBid();

                    if (player.FreeCash < price)
                    {
                        errMsg = LocalText.GetText("NoMoney");
                        break;
                    }
                }
                else
                {
                    price = item.GetBid();
                }

                if (boughtItem.HasSharePriceToSet)
                {
                    shareCompName = boughtItem.CompanyToSetPriceFor;
                    sharePrice = boughtItem.AssociatedSharePrice;
                    if (sharePrice == 0)
                    {
                        errMsg =
                            LocalText.GetText("NoSharePriceSet", shareCompName);
                        break;
                    }
                    if ((stockMarket.GetStartSpace(sharePrice)) == null)
                    {
                        errMsg =
                            LocalText.GetText("InvalidStartPrice",
                                        Bank.Format(this, sharePrice),
                                    shareCompName);
                        break;
                    }
                }
                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CantBuyItem",
                        playerName,
                        item.Id,
                        errMsg));
                return false;
            }

            AssignItem(player, item, price, sharePrice);

            // Set priority (only if the item was not auctioned)
            // ASSUMPTION: getting an item in auction mode never changes priority
            if (lastBid == 0)
            {
                playerManager.SetPriorityPlayerToNext();
            }
            playerManager.SetCurrentToNextPlayer();

            numPasses.Set(0);

            return true;

        }

        /**
         * This method executes the start item buy action.
         *
         * @param player Buying player.
         * @param item Start item being bought.
         * @param price Buy price.
         */
        protected void AssignItem(Player player, StartItem item, int price,
                int sharePrice)
        {
            ICertificate primary = item.Primary;
            string priceText = Currency.ToBank(player, price);
            ReportBuffer.Add(this, LocalText.GetText("BuysItemFor",
                    player.Id,
                    primary.ToText(),
                    priceText));
            primary.MoveTo(player);
            ChecksOnBuying(primary, sharePrice);
            if (item.HasSecondary)
            {
                ICertificate extra = item.Secondary;
                ReportBuffer.Add(this, LocalText.GetText("ALSO_GETS",
                        player.Id,
                        extra.ToText()));
                extra.MoveTo(player);
                ChecksOnBuying(extra, sharePrice);
            }
            item.SetSold(player, price);
        }

        protected void ChecksOnBuying(ICertificate cert, int sharePrice)
        {
            if (cert is PublicCertificate)
            {
                PublicCertificate pubCert = (PublicCertificate)cert;
                PublicCompany comp = pubCert.Company;
                // Start the company, look for a fixed start price
                if (!comp.HasStarted())
                {
                    if (!comp.HasStockPrice)
                    {
                        comp.Start();
                    }
                    else if (pubCert.IsPresidentShare)
                    {
                        /* Company to be started. Check if it has a start price */
                        if (sharePrice > 0)
                        {
                            // User has told us the start price
                            comp.Start(sharePrice);
                        }
                        else if (comp.GetIPOPrice() != 0)
                        {
                            // Company has a known start price
                            comp.Start();
                        }
                        else
                        {
                            log.Error("No start price for " + comp.Id);
                        }
                    }
                }
                if (comp.HasStarted() && !comp.HasFloated())
                {
                    CheckFlotation(comp);
                }
                if (comp.HasStarted()) comp.CheckPresidency();  // Needed for 1835 BY
            }
        }

        /**
         * Process a player's pass.
         * @param action TODO
         * @param playerName The name of the current player (for checking purposes).
         */
        protected abstract bool Pass(NullAction action, string playerName);

        override protected void FinishRound()
        {
            base.FinishRound();
        }

        /*----- Setting up the UI for the next action -----*/

        /**
         * Get the current list of start items.
         *
         * @return An array of start items, possibly empty.
         */

        public List<StartItem> GetStartItems()
        {

            return new List<StartItem>(itemsToSell.View());
        }

        /**
         * Get a list of items that the current player may bid upon.
         *
         * @return An array of start items, possibly empty.
         */

        public StartPacket StartPacket
        {
            get
            {
                return startPacket;
            }
        }

        public bool HasBidding
        {
            get
            {
                return hasBidding;
            }
        }

        public bool HasBuying
        {
            get
            {
                return hasBuying;
            }
        }

        public bool HasBasePrices
        {
            get
            {
                return hasBasePrices;
            }
        }

        public State.Model GetBidModel(int privateIndex, Player player)
        {
            return (itemsToSell.Get(privateIndex)).GetBidForPlayerModel(player);
        }

        public State.Model GetMinimumBidModel(int privateIndex)
        {
            return (itemsToSell.Get(privateIndex)).MinimumBidModel;
        }

        // TODO: Maybe this should be a subclass of a readableCashModel
        public State.Model GetFreeCashModel(Player player)
        {
            return player.FreeCashModel;
        }

        // TODO: Maybe this should be a subclass of a readableCashModel
        public State.Model GetBlockedCashModel(Player player)
        {
            return player.BlockedCashModel;
        }
        /**
         * @return the startRoundName
         */
        public string GetStartRoundName()
        {
            return StartRoundName;
        }

        /**
         * @param startRoundName the startRoundName to set
         */
        public void SetStartRoundName(string startRoundName)
        {
            StartRoundName = startRoundName;
        }
    }
}
