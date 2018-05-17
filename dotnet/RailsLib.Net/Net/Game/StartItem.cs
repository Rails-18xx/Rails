using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Each object of this class represents a "start packet item", which consist of
 * one or two certificates. The whole start packet must be bought before the
 * stock rounds can start. <p> During XML parsing, only the certificate name and
 * other attributes are saved. The certificate objects are linked to in the
 * later initialization step.
 */

namespace GameLib.Net.Game
{
    public class StartItem : RailsAbstractItem
    {
        // Fixed properties
        protected ICertificate primary = null;
        protected ICertificate secondary = null;
        protected CountingMoneyModel basePrice;
        protected bool reduceable;
        protected int row = 0;
        protected int column = 0;
        protected int index;

        // Bids
        protected GenericState<Player> lastBidder;
        protected Dictionary<Player, CountingMoneyModel> bids = new Dictionary<Player, CountingMoneyModel>();
        protected CountingMoneyModel minimumBid;
        protected Dictionary<Player, BooleanState> active = new Dictionary<Player, BooleanState>();

        /**
         * Status of the start item (buyable? biddable?) regardless whether the
         * current player has the amount of (unblocked) cash to buy it or to bid on
         * it.
         */
        protected IntegerState status;

        public const int UNAVAILABLE = 0;
        public const int BIDDABLE = 1;
        public const int BUYABLE = 2;
        public const int SELECTABLE = 3;
        public const int AUCTIONED = 4;
        public const int NEEDS_SHARE_PRICE = 5; // TODO No longer used (may
                                                // not be true after
                                                // bidding), needs code
                                                // cleanup
        public const int SOLD = 6;

        public static string[] statusName =
            new string[] { "Unavailable", "Biddable", "Buyable", "Selectable",
                    "Auctioned", "NeedingSharePrice", "Sold" };

        // For initialization purposes only
        protected string type = null;
        protected bool president = false;
        protected string name2 = null;
        protected string type2 = null;
        protected bool president2 = false;

        public enum NoBidsReactions
        {
            REDUCE_AND_REBID,
            RUN_OPERATING_ROUND
        };

        protected NoBidsReactions noBidsReaction = NoBidsReactions.RUN_OPERATING_ROUND;

        protected static Logger<StartItem> log = new Logger<StartItem>();

        /**
         * The constructor, taking the properties of the "primary" (often the only)
         * certificate. The parameters are only stored, real initialization is done
         * by the init() method.
         */
        protected StartItem(IRailsItem parent, string id, string type, int index, bool president) : base(parent, id)
        {
            basePrice = CountingMoneyModel.Create(this, "basePrice", false);
            lastBidder = GenericState<Player>.Create(this, "lastBidder");
            minimumBid = CountingMoneyModel.Create(this, "minimumBid", false);
            status = IntegerState.Create(this, "status");

            this.type = type;
            this.index = index;
            this.president = president;

            minimumBid.SetSuppressZero(true);
        }

        /** 
         * @param name The Company name of the primary certificate. This name will
         * also become the name of the start item itself.
         * @param type The CompanyType name of the primary certificate.
         * @param president True if the primary certificate is the president's
         * share.
         * @return a fully initialized StartItem 
         */
        public static StartItem Create(IRailsItem parent, string name, string type, int price, bool reduceable, int index, bool president)
        {
            StartItem item = new StartItem(parent, name, type, index, president);
            item.InitBasePrice(price);
            item.SetReducePrice(reduceable);
            return item;
        }

        protected void InitBasePrice(int basePrice)
        {
            this.basePrice.Set(basePrice);
        }

        protected void SetReducePrice(bool reduceable)
        {
            this.reduceable = reduceable;
        }

        /**
         * Add a secondary certificate, that "comes with" the primary certificate.
         *
         * @param name2 The Company name of the secondary certificate.
         * @param type2 The CompanyType name of the secondary certificate.
         * @param president2 True if the secondary certificate is the president's
         * share.
         */
        public void SetSecondary(string name2, string type2, bool president2)
        {
            this.name2 = name2;
            this.type2 = type2;
            this.president2 = president2;
        }

        /**
         * Add a secondary certificate, that "comes with" the primary certificate
         * after initialization.
         *
         * @param secondary The secondary certificate.
         */
        public void SetSecondary(ICertificate secondary)
        {
            this.secondary = secondary;
        }

        /**
         * Initialization, to be called after all XML parsing has completed, and
         * after IPO initialization.
         */
        public void Init(GameManager gameManager)
        {

            IReadOnlyCollection<Player> players = GetRoot.PlayerManager.Players;
            foreach (Player p in players)
            {
                // TODO: Check if this is correct or that it should be initialized with zero
                CountingMoneyModel bid = CountingMoneyModel.Create(this, "bidBy_" + p.Id, false);
                bid.SetSuppressZero(true);
                bids[p] = bid;
                active[p] = BooleanState.Create(this, "active_" + p.Id);
            }
            // TODO Leave this for now, but it should be done
            // in the game-specific StartRound class
            minimumBid.Set(basePrice.Value + 5);

            BankPortfolio ipo = GetRoot.Bank.Ipo;
            BankPortfolio unavailable = GetRoot.Bank.Unavailable;

            CompanyManager compMgr = GetRoot.CompanyManager;

            ICompany company = compMgr.GetCompany(type, Id);
            if (company is PrivateCompany)
            {
                primary = (ICertificate)company;
            }
            else
            {
                primary = ipo.PortfolioModel.FindCertificate((PublicCompany)company, president);
                // Move the certificate to the "unavailable" pool.
                PublicCertificate pubcert = (PublicCertificate)primary;
                if (pubcert.Owner == null
                    || pubcert.Owner != unavailable.Parent)
                {
                    pubcert.MoveTo(unavailable);
                }
            }

            // Check if there is another certificate
            if (name2 != null)
            {

                ICompany company2 = compMgr.GetCompany(type2, name2);
                if (company2 is PrivateCompany)
                {
                    secondary = (ICertificate)company2;
                }
                else
                {
                    secondary =
                            ipo.PortfolioModel.FindCertificate((PublicCompany)company2, president2);
                    // Move the certificate to the "unavailable" pool.
                    // FIXME: This is still an issue to resolve  ???
                    PublicCertificate pubcert2 = (PublicCertificate)secondary;
                    if (pubcert2.Owner != unavailable)
                    {
                        pubcert2.MoveTo(unavailable);
                    }
                }
            }

        }

        public int Index
        {
            get
            {
                return index;
            }
        }


        /**
         * Get the row number.
         *
         * @see setRow()
         * @return The row number. Default 0.
         */
        public int Row
        {
            get
            {
                return row;
            }
            /**
     * Set the start packet row. <p> Applies to games like 1835 where start
     * items are organized and become available in rows.
     *
     * @param row
     */
            set
            {
                row = value;
            }
        }

        /**
         * Get the column number.
         *
         * @see setColumn()
         * @return The column number. Default 0.
         */
        public int Column
        {
            get
            {
                return column;
            }
            set
            {
                column = value;
            }
        }

        /**
         * Get the primary (or only) certificate.
         *
         * @return The primary certificate object.
         */
        public ICertificate Primary
        {
            get
            {
                return primary;
            }
        }

        /**
         * Check if there is a secondary certificate.
         *
         * @return True if there is a secondary certificate.
         */
        public bool HasSecondary
        {
            get
            {
                return secondary != null;
            }
        }

        /**
         * Get the secondary certificate.
         *
         * @return The secondary certificate object, or null if it does not exist.
         */
        public ICertificate Secondary
        {
            get
            {
                return secondary;
            }
        }

        /**
         * Get the start item base price.
         *
         * @return The base price.
         */
        public int GetBasePrice()
        {
            return basePrice.Value;
        }

        public bool Reduceable
        {
            get
            {
                return reduceable;
            }
        }

        public void ReduceBasePriceBy(int amount)
        {
            basePrice.Change(-amount);
        }


        /**
         * Register a bid. <p> This method does <b>not</b> check off the amount of
         * money that a player has available for bidding.
         *
         * @param amount The bid amount.
         * @param bidder The bidding player.
         */
        public void SetBid(int amount, Player bidder)
        {
            CountingMoneyModel bid = bids[bidder];
            bid.Set(amount);
            bid.SetSuppressZero(false);
            active[bidder].Set(true);
            lastBidder.Set(bidder);
            minimumBid.Set(amount + 5);
        }

        /**
         * Get the currently highest bid amount.
         *
         * @return The bid amount (0 if there have been no bids yet).
         */
        public int GetBid()
        {
            if (lastBidder.Value == null)
            {
                return 0;
            }
            else
            {
                return bids[lastBidder.Value].Value;
            }
        }

        /**
         * Get the highest bid done so far by a particular player.
         *
         * @param player The name of the player.
         * @return The bid amount for this player (default 0).
         */
        public int GetBid(Player player)
        {
            return bids[player].Value;
        }

        /**
         * Return the total number of players that has done bids so far on this
         * item.
         *
         * @return The number of bidders.
         */
        public int GetBidders()
        {
            int bidders = 0;
            foreach (Player bidder in active.Keys)
            {
                if (active[bidder].Value == true)
                {
                    bidders++;
                }
            }
            return bidders;
        }

        /**
         * Get the highest bidder so far.
         *
         * @return The player object that did the highest bid.
         */
        public Player GetBidder()
        {
            return lastBidder.Value;
        }

        public void SetPass(Player player)
        {
            active[player].Set(false);
            CountingMoneyModel bid = bids[player];
            bid.Set(0);
            bid.SetSuppressZero(true);
        }

        /**
         * Get the minimum allowed next bid. TODO 5 should be configurable.
         *
         * @return Minimum bid
         */
        public int GetMinimumBid()
        {
            return minimumBid.Value;
        }

        public void SetMinimumBid(int value)
        {
            minimumBid.Set(value);
        }

        /**
         * Check if a player has done any bids on this start item.
         *
         * @param playerName The name of the player.
         * @return True if this player is active for this startItem
         */
        public bool IsActive(Player player)
        {
            return active[player].Value;
        }


        /**
         * Set all players to active on this start item.  Used when 
         * players who did not place a bid are still allowed to
         * participate in an auction (e.g. 1862)
         */
        public void SetAllActive()
        {
            foreach (Player p in active.Keys)
            {
                active[p].Set(true);
            }
        }

        /**
         * Check if the start item has been sold.
         *
         * @return True if this item has been sold.
         */
        public bool IsSold
        {
            get
            {
                return status.Value == SOLD;
            }
        }

        /**
         * Set the start item sold status.
         *
         * @param sold The new sold status (usually true).
         */
        public void SetSold(Player player, int buyPrice)
        {
            status.Set(SOLD);


            lastBidder.Set(player);

            // For display purposes, set all lower bids to zero
            foreach (Player p in bids.Keys)
            {
                CountingMoneyModel bid = bids[p];
                // Unblock any bid money
                if (bid.Value > 0)
                {
                    p.UnblockCash(bid.Value);
                    if (p != player)
                    {
                        bid.Set(0);
                        bid.SetSuppressZero(true);
                    }
                    active[p].Set(false);
                }
            }
            // for winning bidder set bid to buyprice
            bids[player].Set(buyPrice);
            minimumBid.Set(0);
        }

        /**
         * This method indicates if there is a company for which a par price must be
         * set when this start item is bought. The UI can use this to ask for the
         * price immediately, so bypassing the extra "price asking" intermediate
         * step.
         *
         * @return A public company for which a price must be set.
         */
        public PublicCompany NeedsPriceSetting()
        {
            PublicCompany company;

            if ((company = CheckNeedForPriceSetting(primary)) != null)
            {
                return company;
            }
            else if (secondary != null
                     && ((company = CheckNeedForPriceSetting(secondary)) != null))
            {
                return company;
            }

            return null;
        }

        /**
         * If a start item component a President's certificate that needs price
         * setting, return the name of the company for which the price must be set.
         *
         * @param certificate
         * @return Name of public company, or null
         */
        protected PublicCompany CheckNeedForPriceSetting(ICertificate certificate)
        {

            if (!(certificate is PublicCertificate)) return null;

            PublicCertificate publicCert = (PublicCertificate)certificate;

            if (!publicCert.IsPresidentShare) return null;

            PublicCompany company = publicCert.Company;

            if (!company.HasStockPrice) return null;

            if (company.GetIPOPrice() != 0) return null;

            return company;

        }

        public int GetStatus()
        {
            return status.Value;
        }

        public IntegerState StatusModel
        {
            get
            {
                return status;
            }
        }

        public string GetStatusName()
        {
            return statusName[status.Value];
        }

        public void SetStatus(int status)
        {
            this.status.Set(status);
        }

        public State.Model BasePriceModel
        {
            get
            {
                return basePrice;
            }
        }

        public State.Model GetBidForPlayerModel(Player player)
        {
            return bids[player];
        }

        public State.Model MinimumBidModel
        {
            get
            {
                return minimumBid;
            }
        }

        public string StartItemType
        {
            get
            {
                return type;
            }
        }

        public NoBidsReactions NoBidsReaction
        {
            get
            {
                return noBidsReaction;
            }
            set
            {
                noBidsReaction = value;
            }
        }

        public string GetText()
        {
            return ToString();
        }
    }
}
