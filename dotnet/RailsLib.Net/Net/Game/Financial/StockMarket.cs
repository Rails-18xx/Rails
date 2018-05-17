using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Model;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

namespace GameLib.Net.Game.Financial
{
    public class StockMarket : RailsManager, IConfigurable
    {
        /**
 *  This is the name by which the CompanyManager should be registered with
 * the ComponentManager.
 */
        public static readonly string COMPONENT_NAME = "StockMarket";

        public static readonly string DEFAULT = "default";

        protected Dictionary<string, StockSpaceType> stockSpaceTypes;
        protected Dictionary<string, StockSpace> stockChartSpaces;
        protected SortedSet<StockSpace> startSpaces;

        protected StockMarketModel marketModel;


        protected StockSpace[,] stockChart;
        protected int numRows = 0;
        protected int numCols = 0;
        protected StockSpaceType defaultType;

        /* Game-specific flags */
        protected bool upOrDownRight = false;
        /* Sold out and at top: go down or right (1870) */

        // TODO: There used to be a BooleanState gameOver, did this have a function?
        /**
         * Used by Configure (via reflection) only
         */
        public StockMarket(RailsRoot parent, string id) : base(parent, id)
        {
            stockSpaceTypes = new Dictionary<string, StockSpaceType>();
            stockChartSpaces = new Dictionary<string, StockSpace>();
            startSpaces = new SortedSet<StockSpace>();
            marketModel = StockMarketModel.Create(this);
        }

        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {

            // Define a default stockspace type with colour white
            defaultType = new StockSpaceType(DEFAULT, StockSpaceType.WHITE);
            stockSpaceTypes[DEFAULT] = defaultType;

            /* Read and configure the stock market space types */
            List<Tag> typeTags = tag.GetChildren(StockSpaceType.ELEMENT_ID);

            if (typeTags != null)
            {
                foreach (Tag typeTag in typeTags)
                {
                    /* Extract the attributes of the Stock space type */
                    string name = typeTag.GetAttributeAsString(StockSpaceType.NAME_TAG);
                    if (name == null)
                    {
                        throw new ConfigurationException(LocalText.GetText("UnnamedStockSpaceType"));
                    }
                    string color = typeTag.GetAttributeAsString(StockSpaceType.COLOR_TAG);

                    /* Check for duplicates */
                    if (stockSpaceTypes.ContainsKey(name))
                    {
                        throw new ConfigurationException(LocalText.GetText("StockSpaceTypeConfiguredTwice", name));
                    }

                    /* Create the type */
                    StockSpaceType spaceType = new StockSpaceType(name, color);
                    stockSpaceTypes[name] = spaceType;

                    // Check the stock space type flags
                    spaceType.IsNoBuyLimit = typeTag.GetChild(StockSpaceType.NO_BUY_LIMIT_TAG) != null;
                    spaceType.IsNoCertLimit = typeTag.GetChild(StockSpaceType.NO_CERT_LIMIT_TAG) != null;
                    spaceType.IsNoHoldLimit = typeTag.GetChild(StockSpaceType.NO_HOLD_LIMIT_TAG) != null;
                }
            }

            /* Read and configure the stock market spaces */
            List<Tag> spaceTags = tag.GetChildren(StockSpace.ELEMENT_ID);
            StockSpaceType type;
            int row, col;
            foreach (Tag spaceTag in spaceTags)
            {
                type = null;

                // Extract the attributes of the Stock space
                string name = spaceTag.GetAttributeAsString(StockSpace.NAME_TAG);
                if (name == null)
                {
                    throw new ConfigurationException(LocalText.GetText("UnnamedStockSpace"));
                }
                string price = spaceTag.GetAttributeAsString(StockSpace.PRICE_TAG);
                if (price == null)
                {
                    throw new ConfigurationException(LocalText.GetText("StockSpaceHasNoPrice", name));
                }
                string typeName = spaceTag.GetAttributeAsString(StockSpace.TYPE_TAG);
                if (typeName != null)
                {
                    if (!stockSpaceTypes.ContainsKey(typeName))
                    {
                        throw new ConfigurationException(LocalText.GetText("StockSpaceTypeUndefined", type));
                    }
                    type = stockSpaceTypes[typeName];
                }
                if (type == null) type = defaultType;

                if (stockChartSpaces.ContainsKey(name))
                {
                    throw new ConfigurationException(LocalText.GetText("StockSpacesConfiguredTwice", name));
                }

                StockSpace space = StockSpace.Create(this, name, int.Parse(price), type);
                stockChartSpaces[name] = space;

                row = int.Parse(name.Substring(1));
                col = (name.ToUpper()[0] - '@');
                if (row > numRows) numRows = row;
                if (col > numCols) numCols = col;

                // Loop through the stock space flags
                if (spaceTag.GetChild(StockSpace.START_SPACE_TAG) != null)
                {
                    space.IsStart = true;
                    startSpaces.Add(space);
                }
                space.ClosesCompany = spaceTag.GetChild(StockSpace.CLOSES_COMPANY_TAG) != null;
                space.EndsGame = spaceTag.GetChild(StockSpace.GAME_OVER_TAG) != null;
                space.IsBelowLedge = spaceTag.GetChild(StockSpace.BELOW_LEDGE_TAG) != null;
                space.IsLeftOfLedge = spaceTag.GetChild(StockSpace.LEFT_OF_LEDGE_TAG) != null;

            }

            stockChart = new StockSpace[numRows, numCols];
            foreach (StockSpace space in stockChartSpaces.Values)
            {
                stockChart[space.Row, space.Column] = space;
            }

            upOrDownRight = tag.GetChild("UpOrDownRight") != null;
        }

        /**
         * Final initializations, to be called after all XML processing is complete.
         * The purpose is to register fixed company start prices.
         */
        public void FinishConfiguration(RailsRoot root)
        {

            foreach (PublicCompany comp in root.CompanyManager.GetAllPublicCompanies())
            {
                if (!comp.HasStarted() && comp.GetStartSpace() != null)
                {
                    comp.GetStartSpace().AddFixedStartPrice(comp);
                }
            }
        }

        /**
         * @return
         */
        public StockSpace[,] GetStockChart()
        {
            return stockChart;
        }

        public StockSpace GetStockSpace(int row, int col)
        {
            if (row >= 0 && row < numRows && col >= 0 && col < numCols)
            {
                return stockChart[row, col];
            }
            else
            {
                return null;
            }
        }

        public StockSpace GetStockSpace(string name)
        {
            return stockChartSpaces[name];
        }

        /*--- Actions ---*/

        public void Start(PublicCompany company, StockSpace price)
        {
            PrepareMove(company, null, price);
            // make marketModel updating on company price model
            company.GetCurrentPriceModel().AddModel(marketModel);
        }

        public void PayOut(PublicCompany company)
        {
            MoveRightOrUp(company);
        }

        public void Withhold(PublicCompany company)
        {
            MoveLeftOrDown(company);
        }

        public void Sell(PublicCompany company, int numberOfSpaces)
        {
            MoveDown(company, numberOfSpaces);
        }

        public void SoldOut(PublicCompany company)
        {
            MoveUp(company);
        }

        public void MoveUp(PublicCompany company)
        {
            StockSpace oldsquare = company.GetCurrentSpace();
            StockSpace newsquare = oldsquare;
            int row = oldsquare.Row;
            int col = oldsquare.Column;
            if (row > 0)
            {
                newsquare = GetStockSpace(row - 1, col);
            }
            else if (upOrDownRight && col < numCols - 1)
            {
                newsquare = GetStockSpace(row + 1, col + 1);
            }
            if (newsquare != null) PrepareMove(company, oldsquare, newsquare);
        }

        public void Close(PublicCompany company)
        {
            PrepareMove(company, company.GetCurrentSpace(), null);
        }

        protected void MoveDown(PublicCompany company, int numberOfSpaces)
        {
            StockSpace oldsquare = company.GetCurrentSpace();
            StockSpace newsquare = oldsquare;
            int row = oldsquare.Row;
            int col = oldsquare.Column;

            /* Drop the indicated number of rows */
            int newrow = row + numberOfSpaces;

            /* Don't drop below the bottom of the chart */
            while (newrow >= numRows || GetStockSpace(newrow, col) == null)
                newrow--;

            // Check if company may enter a "Closed" area
            while (GetStockSpace(newrow, col).ClosesCompany && !company.CanClose)
                newrow--;

            /*
             * If marker landed just below a ledge, and NOT because it was bounced
             * by the bottom of the chart, it will stay just above the ledge.
             */
            if (GetStockSpace(newrow, col).IsBelowLedge
                    && newrow == row + numberOfSpaces) newrow--;

            if (newrow > row)
            {
                newsquare = GetStockSpace(newrow, col);
            }
            if (newsquare != oldsquare)
            {
                PrepareMove(company, oldsquare, newsquare);
            }
        }

        protected void MoveRightOrUp(PublicCompany company)
        {
            /* Ignore the amount for now */
            StockSpace oldsquare = company.GetCurrentSpace();
            StockSpace newsquare = oldsquare;
            int row = oldsquare.Row;
            int col = oldsquare.Column;
            if (col < numCols - 1 && !oldsquare.IsLeftOfLedge
                    && (newsquare = GetStockSpace(row, col + 1)) != null) { }
            else if (row > 0
                    && (newsquare = GetStockSpace(row - 1, col)) != null) { }
            PrepareMove(company, oldsquare, newsquare);
        }

        protected void MoveLeftOrDown(PublicCompany company)
        {
            StockSpace oldsquare = company.GetCurrentSpace();
            StockSpace newsquare = oldsquare;
            int row = oldsquare.Row;
            int col = oldsquare.Column;
            if (col > 0 && (newsquare = GetStockSpace(row, col - 1)) != null) { }
            else if (row < numRows - 1 &&
                    (newsquare = GetStockSpace(row + 1, col)) != null) { }
            else
            {
                newsquare = oldsquare;
            }
            PrepareMove(company, oldsquare, newsquare);
        }

        protected void PrepareMove(PublicCompany company, StockSpace from, StockSpace to)
        {
            // To be written to a log file in the future.
            if (from != null && from == to)
            {
                ReportBuffer.Add(this, LocalText.GetText("PRICE_STAYS_LOG",
                        company.Id,
                        Bank.Format(this, from.Price),
                        from.Id));
                return;
            }
            else if (from == null && to != null)
            {
                ;
            }
            else if (from != null && to != null)
            {
                ReportBuffer.Add(this, LocalText.GetText("PRICE_MOVES_LOG",
                        company.Id,
                        Bank.Format(this, from.Price),
                        from.Id,
                        Bank.Format(this, to.Price),
                        to.Id));

                /* Check for rails.game closure */
                if (to.EndsGame)
                {
                    ReportBuffer.Add(this, LocalText.GetText("GAME_OVER"));
                    GetRoot.GameManager.RegisterMaxedSharePrice(company, to);
                }

            }
            company.SetCurrentSpace(to);

            if (to != null)
            {
                to.AddToken(company);
            }
            if (from != null)
            {
                from.RemoveToken(company);
            }
        }

        // FIXME: The StockSpace changes have to update the players worth
        // thus link the state of company space to the players worth

        /** 
         * Return start prices as list of prices
         */
        [Obsolete]
        public List<int> GetStartPrices()
        {
            List<int> prices = new List<int>();
            foreach (StockSpace space in startSpaces)
            {
                prices.Add(space.Price);
            }
            return prices;
        }

        /**
         * Return start prices as an sorted set of stockspaces
         */
        public IReadOnlyCollection<StockSpace> getStartSpaces()
        {
            return new ReadOnlyCollection<StockSpace>(new List<StockSpace>(startSpaces));// ImmutableSortedSet.copyOf(startSpaces);
        }

        [Obsolete]
        public StockSpace GetStartSpace(int price)
        {
            foreach (StockSpace space in startSpaces)
            {
                if (space.Price == price) return space;
            }
            return null;
        }

        /**
         * @return number of columns
         */
        public int NumberOfColumns
        {
            get
            {
                return numCols;
            }
        }

        /**
         * @return number of rows
         */
        public int NumberOfRows
        {
            get
            {
                return numRows;
            }
        }

        public StockMarketModel MarketModel
        {
            get
            {
                return marketModel;
            }
        }

    }
}
