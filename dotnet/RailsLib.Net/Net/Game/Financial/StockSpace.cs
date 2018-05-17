using GameLib.Net.Common;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Objects of this class represent a square on the StockMarket.
 */

namespace GameLib.Net.Game.Financial
{
    public class StockSpace : RailsModel, IComparable<StockSpace>
    {
        /*--- Class attributes ---*/
        /*--- Constants ---*/
        /** The getId() of the XML tag used to configure a stock space. */
        public static readonly string ELEMENT_ID = "StockSpace";

        /**
         * The getId() of the XML attribute for the stock price's getId() (like "A1" -
         * naming is like spreadsheet cells.
         */
        public static readonly string NAME_TAG = "name";

        /** The getId() of the XML attribute for the stock price. */
        public static readonly string PRICE_TAG = "price";

        /** The getId() of the XML attribute for the stock price type (optional). */
        public static readonly string TYPE_TAG = "type";

        /**
         * The getId() of the XML tag for the "startSpace" property. (indicating an
         * allowed PAR price)
         */
        public static readonly string START_SPACE_TAG = "StartSpace";

        /** The getId() of the XML tag for the "below ledge" property. */
        public static readonly string BELOW_LEDGE_TAG = "BelowLedge";

        /** The getId() of the XML tag for the "left of ledge" property. */
        public static readonly string LEFT_OF_LEDGE_TAG = "LeftOfLedge";

        /** The getId() of the XML tag for the "closes company" property. */
        public static readonly string CLOSES_COMPANY_TAG = "ClosesCompany";

        /** The getId() of the XML tag for the "game over" property. */
        public static readonly string GAME_OVER_TAG = "GameOver";

        /*--- Instance attributes ---*/
        private readonly int row;
        private readonly int column;
        private readonly int price;
        private readonly StockSpaceType type;
        private bool belowLedge = false; // For 1870
        private bool leftOfLedge = false; // For 1870
        private bool closesCompany = false;// For 1856 and other games
        private bool endsGame = false; // For 1841 and other games
        private bool start = false; // Company may start here


        /*--- State fields */
        private readonly ListState<PublicCompany> tokens;
        private readonly ListState<PublicCompany> fixedStartPrices;

        private static readonly Logger<StockSpace> log = new Logger<StockSpace>();

        /*--- Constructors ---*/
        private StockSpace(StockMarket parent, string id, int price, StockSpaceType type) : base(parent, id)
        {
            tokens = ListState<PublicCompany>.Create(this, "tokens");
            fixedStartPrices = ListState<PublicCompany>.Create(this, "fixedStartPrices");

            this.price = price;
            this.type = type;

            this.row = int.Parse(id.Substring(1)) - 1;
            this.column = (id.ToUpper()[0] - '@') - 1;
        }

        /**
         * @return fully initialized StockSpace
         */
        public static StockSpace Create(StockMarket parent, string id, int price, StockSpaceType type)
        {
            return new StockSpace(parent, id, price, type);
        }

        new public StockMarket Parent
        {
            get
            {
                return (StockMarket)base.Parent;
            }
        }

        // No constructors for the booleans. Use the setters.

        /*--- Token handling methods ---*/
        /**
         * Add a token at the end of the array (i.e. at the bottom of the pile)
         *
         * Always returns true;
         *
         * @param company The company object to add.
         */
        public bool AddToken(PublicCompany company)
        {
            log.Debug(company.Id + " price token added to " + Id);
            tokens.Add(company);
            return true;
        }

        public bool AddTokenAtStackPosition(PublicCompany company, int stackPosition)
        {
            log.Debug(company.Id + " price token added to " + Id + "  at stack position " + stackPosition);
            tokens.Add(stackPosition, company);
            return true;
        }

        /**
         * Remove a token from the pile.
         *
         * @param company The company object to remove.
         * @return False if the token was not found.
         */
        public bool RemoveToken(PublicCompany company)
        {
            log.Debug(company.Id + " price token removed from " + Id);
            return tokens.Remove(company);
        }

        public IReadOnlyCollection<PublicCompany> GetTokens()
        {
            return tokens.View();
        }

        /**
         * Find the stack position of a company token
         *
         * @return Stock position: 0 = top, increasing towards the bottom. -1 if not
         * found.
         */
        public int GetStackPosition(PublicCompany company)
        {
            return tokens.IndexOf(company);
        }

        /*----- Fixed start prices (e.g. 1835, to show in small print) -----*/
        public void AddFixedStartPrice(PublicCompany company)
        {
            fixedStartPrices.Add(company);
        }

        public IReadOnlyCollection<PublicCompany> GetFixedStartPrices()
        {
            return fixedStartPrices.View();
        }

        /*--- Getters ---*/
        /**
         * @return TRUE is the square is just above a ledge.
         */
        public bool IsBelowLedge
        {
            get
            {
                return belowLedge;
            }
            set
            {
                belowLedge = value;
            }
        }

        /**
         * @return TRUE if the square closes companies landing on it.
         */
        public bool ClosesCompany
        {
            get
            {
                return closesCompany;
            }
            set
            {
                closesCompany = value;
            }
        }

        /**
         * @return The square's colour.
         */
        public System.Drawing.Color Color
        {
            get
            {
                if (type != null)
                {
                    return type.Color;
                }
                else
                {
                    return System.Drawing.Color.White;
                }
            }
        }

        /**
         * @return TRUE if the rails.game ends if a company lands on this square.
         */
        public bool EndsGame
        {
            get
            {
                return endsGame;
            }
            set
            {
                endsGame = value;
            }
        }

        /**
         * @return The stock price associated with the square.
         */
        public int Price
        {
            get
            {
                return price;
            }
        }

        /**
         * @return
         */
        public int Column
        {
            get
            {
                return column;
            }
        }

        /**
         * @return
         */
        public StockSpaceType SpaceType
        {
            get
            {
                return type;
            }
        }

        /**
         * @return
         */
        public int Row
        {
            get
            {
                return row;
            }
        }

        /**
         * @return
         */
        public bool IsStart
        {
            get
            {
                return start;
            }
            set
            {
                start = value;
            }
        }

        /**
         * @return
         */
        public bool IsLeftOfLedge
        {
            get
            {
                return leftOfLedge;
            }
            set
            {
                leftOfLedge = value;
            }
        }

        /**
         * @return
         */
        public bool IsNoBuyLimit
        {
            get
            {
                return type != null && type.IsNoBuyLimit;
            }
        }

        /**
         * @return
         */
        public bool IsNoCertLimit
        {
            get
            {
                return type != null && type.IsNoCertLimit;
            }
        }

        /**
         * @return
         */
        public bool IsNoHoldLimit
        {
            get
            {
                return type != null && type.IsNoHoldLimit;
            }
        }

        /**
         * @return Returns if the space hasTokens.
         */
        public bool HasTokens
        {
            get
            {
                return tokens.Count != 0;
            }
        }

        override public string ToText()
        {
            return Bank.Format(Parent, price);
        }

        // Comparable method
        // TODO: Check if this is correct (price > column > row)
        public int CompareTo(StockSpace other)
        {
            int result;

            result = price.CompareTo(other.price);
            if (result != 0) return result;

            result = column.CompareTo(other.column);
            if (result != 0) return result;

            return row.CompareTo(other.row);

            //return ComparisonChain.start()
            //        .compare(price, other.price)
            //        .compare(column, other.column)
            //        .compare(row, other.row)
            //        .result();
        }

    }
}
