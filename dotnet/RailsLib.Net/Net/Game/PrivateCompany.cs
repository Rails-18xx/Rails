using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

// FIXME: Move static field numberOfPrivateCompanies to CompanyManager

namespace GameLib.Net.Game
{
    public class PrivateCompany : RailsOwnableItem<PrivateCompany>, ICompany, ICertificate, ICloseable, ITriggerable
    {
        private static Logger<PrivateCompany> log = new Logger<PrivateCompany>();

        public const string TYPE_TAG = "Private";
        public const string REVENUE = "revenue";
        //used by getUpperPrice and getLowerPrice to signal no limit
        public const int NO_PRICE_LIMIT = -1;


        // FIXME: See above, this has to be fixed
        protected static int numberOfPrivateCompanies = 0;
        protected int privateNumber; // For internal use

        protected int basePrice = 0;
        // list of revenue sfy 1889
        protected List<int> revenue;
        protected string auctionType;

        // Closing conditions
        protected int closingPhase;
        // Closing when special properties are used
        protected bool closeIfAllExercised = false; // all exercised => closing
        protected bool closeIfAnyExercised = false; // any exercised => closing
        protected bool closeAtEndOfTurn = false; // closing at end of OR turn, E.g. 1856 W&SR

        // Prevent closing conditions sfy 1889
        protected List<string> preventClosingConditions = new List<string>();
        // Close at start of phase
        protected string closeAtPhaseName = null;
        // Manual close possible
        protected bool closeManually = false;

        protected string blockedHexesString = null;
        protected List<MapHex> blockedHexes = null;

        // Maximum and minimum prices the private can be sold in for.
        protected int upperPrice = NO_PRICE_LIMIT;
        protected int lowerPrice = NO_PRICE_LIMIT;

        // Maximum and minimum price factor (used to set upperPrice and lowerPrice)
        protected float lowerPriceFactor = NO_PRICE_LIMIT;
        protected float upperPriceFactor = NO_PRICE_LIMIT;

        // Maximum and minimum prices the private can be sold to a player for.
        protected int upperPlayerPrice = NO_PRICE_LIMIT;
        protected int lowerPlayerPrice = NO_PRICE_LIMIT;

        // Maximum and minimum price factor when selling to another player
        protected float lowerPlayerPriceFactor = NO_PRICE_LIMIT;
        protected float upperPlayerPriceFactor = NO_PRICE_LIMIT;

        // Can the private be bought by companies / players (when held by a player)
        protected bool tradeableToCompany = true;
        protected bool tradeableToPlayer = false;

        private PortfolioSet<SpecialProperty> specialProperties;

        // used for Company interface
        private string longName;
#pragma warning disable 649
        private string alias;
#pragma warning restore 649
        private CompanyType type;
        private string infoText = "";
        private string parentInfoText = "";
        private BooleanState closed;

        // used for Certificate interface
        private float certificateCount = 1.0f;

        /**
         * Used by Configure (via reflection) only
         */
        public PrivateCompany(IRailsItem parent, string id) : base(parent, id)
        {
            specialProperties = PortfolioSet<SpecialProperty>.Create(this, "specialProperties");
            closed = BooleanState.Create(this, "closed", false);

            this.privateNumber = numberOfPrivateCompanies++;
        }

        public void Triggered(Observable observable, Change change)
        {
            // if newOwner is a (public) company then unblock
            if (Owner is ICompany)
            {
                UnblockHexes();
            }
        }
        /**
         * @see net.sf.rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {
            /* Configure private company features */
            try
            {

                longName = tag.GetAttributeAsString("longname", Id);
                infoText = "<html>" + longName;
                basePrice = tag.GetAttributeAsInteger("basePrice", 0);

                // sfy 1889 changed to IntegerArray
                revenue = new List<int>(tag.GetAttributeAsIntegerList("revenue"));

                // pld: adding revenue to info text
                infoText += "<br>Revenue: ";
                for (int i = 0; i < revenue.Count; i++)
                {
                    infoText += (Bank.Format(this, revenue[i]));
                    if (i < revenue.Count - 1) { infoText += ", "; };
                }

                Tag certificateTag = tag.GetChild("Certificate");
                if (certificateTag != null)
                {
                    certificateCount = certificateTag.GetAttributeAsFloat("certificateCount", 1.0f);
                }

                // Blocked hexes (until bought by a company)
                Tag blockedTag = tag.GetChild("Blocking");
                if (blockedTag != null)
                {
                    blockedHexesString = blockedTag.GetAttributeAsString("hex");
                    infoText += "<br>Blocking: " + blockedHexesString;

                    // add triggerable to unblock
                    this.TriggeredOnOwnerChange(this);
                    //            new Triggerable()
                    //            {
                    //                public void triggered(Observable observable, Change change)
                    //    {
                    //        // if newOwner is a (public) company then unblock
                    //        if (getOwner() instanceof Company) {
                    //        PrivateCompany.this.unblockHexes();
                    //    }
                    //}
                    //}
                    //        );
                }

                // Extra info text(usually related to extra-share special properties)
                Tag infoTag = tag.GetChild("Info");
                if (infoTag != null)
                {
                    string infoKey = infoTag.GetAttributeAsString("key");
                    string[] infoParms = infoTag.GetAttributeAsString("parm", "").Split(',');
                    infoText += "<br>" + LocalText.GetText(infoKey, (object[])infoParms);
                }


                // SpecialProperties
                parentInfoText += SpecialProperty.Configure(this, tag);

                // Closing conditions
                // Currently only used to handle closure following laying
                // tiles and/or tokens because of special properties.
                // Other cases are currently handled elsewhere.
                Tag closureTag = tag.GetChild("ClosingConditions");

                if (closureTag != null)
                {

                    Tag spTag = closureTag.GetChild("SpecialProperties");

                    if (spTag != null)
                    {

                        string ifAttribute = spTag.GetAttributeAsString("condition");
                        if (ifAttribute != null)
                        {
                            closeIfAllExercised = ifAttribute.Equals("ifExercised", StringComparison.OrdinalIgnoreCase)
                            || ifAttribute.Equals("ifAllExercised", StringComparison.OrdinalIgnoreCase);
                            closeIfAnyExercised = ifAttribute.Equals("ifAnyExercised", StringComparison.OrdinalIgnoreCase);
                        }
                        string whenAttribute = spTag.GetAttributeAsString("when");
                        if (whenAttribute != null)
                        {
                            closeAtEndOfTurn = whenAttribute.Equals("endOfORTurn", StringComparison.OrdinalIgnoreCase);
                        }
                    }

                    /* conditions that prevent closing */
                    List<Tag> preventTags = closureTag.GetChildren("PreventClosing");
                    if (preventTags != null)
                    {
                        foreach (Tag preventTag in preventTags)
                        {
                            string conditionText = preventTag.GetAttributeAsString("condition");
                            if (conditionText != null)
                            {
                                preventClosingConditions.Add(conditionText);
                            }
                        }
                    }

                    /* allow manual closure */
                    Tag manualTag = closureTag.GetChild("CloseManually");
                    if (manualTag != null)
                    {
                        closeManually = true;
                    }

                    // Close at start of phase
                    Tag closeTag = closureTag.GetChild("Phase");
                    if (closeTag != null)
                    {
                        closeAtPhaseName = closeTag.GetText();
                    }
                }

                // start: br
                // Reads the Tradeable tags
                List<Tag> tradeableTags = tag.GetChildren("Tradeable");
                if (tradeableTags != null)
                {
                    foreach (Tag tradeableTag in tradeableTags)
                    {

                        if (tradeableTag.HasAttribute("toCompany"))
                        {
                            tradeableToCompany = tradeableTag.GetAttributeAsBoolean("toCompany");

                            if (tradeableToCompany)
                            {
                                upperPrice =
                                    tradeableTag.GetAttributeAsInteger("upperPrice", upperPrice);
                                lowerPrice =
                                    tradeableTag.GetAttributeAsInteger("lowerPrice", lowerPrice);
                                lowerPriceFactor =
                                    tradeableTag.GetAttributeAsFloat("lowerPriceFactor", lowerPriceFactor);
                                upperPriceFactor =
                                    tradeableTag.GetAttributeAsFloat("upperPriceFactor", upperPriceFactor);
                            }
                        }

                        if (tradeableTag.HasAttribute("toPlayer"))
                        {
                            tradeableToPlayer = tradeableTag.GetAttributeAsBoolean("toPlayer");

                            if (tradeableToPlayer)
                            {
                                upperPlayerPrice =
                                    tradeableTag.GetAttributeAsInteger("upperPrice", upperPlayerPrice);
                                lowerPlayerPrice =
                                    tradeableTag.GetAttributeAsInteger("lowerPrice", lowerPlayerPrice);
                                lowerPlayerPriceFactor =
                                    tradeableTag.GetAttributeAsFloat("lowerPriceFactor", lowerPlayerPriceFactor);
                                upperPlayerPriceFactor =
                                    tradeableTag.GetAttributeAsFloat("upperPriceFactor", upperPlayerPriceFactor);
                            }
                        }
                    }
                }
                //end: br

            }
            catch (Exception e)
            {
                throw new ConfigurationException("Configuration error for Private "
                        + Id + " " + e.Message);
            }
        }

        public void FinishConfiguration(RailsRoot root)
        {

            foreach (SpecialProperty sp in specialProperties)
            {
                sp.FinishConfiguration(root);
            }

            if (!string.IsNullOrEmpty(blockedHexesString))
            {
                MapManager mapManager = root.MapManager;
                blockedHexes = new List<MapHex>();
                foreach (string hexName in blockedHexesString.Split(','))
                {
                    MapHex hex = mapManager.GetHex(hexName);
                    blockedHexes.Add(hex);
                    hex.SetBlockingPrivateCompany(this);
                }
            }

            infoText += parentInfoText;
            parentInfoText = "";

            if (!string.IsNullOrEmpty(closeAtPhaseName))
            {
                Phase closingPhase = root.PhaseManager.GetPhaseByName(closeAtPhaseName);
                if (closingPhase != null)
                {
                    closingPhase.AddObjectToClose(this);
                }
            }

            // start: br
            //if {upper,lower}PriceFactor is set but {upper,lower}Price is not, calculate the right value
            if (upperPrice == NO_PRICE_LIMIT && upperPriceFactor != NO_PRICE_LIMIT)
            {

                if (basePrice == 0)
                {
                    throw new ConfigurationException("Configuration error for Private "
                            + Id + ": upperPriceFactor needs basePrice to be set");
                }

                upperPrice = (int)(basePrice * upperPriceFactor + 0.5f);
            }
            if (lowerPrice == NO_PRICE_LIMIT && lowerPriceFactor != NO_PRICE_LIMIT)
            {

                if (basePrice == 0)
                {
                    throw new ConfigurationException("Configuration error for Private "
                            + Id + ": lowerPriceFactor needs basePrice to be set");
                }

                lowerPrice = (int)(basePrice * lowerPriceFactor + 0.5f);
            }
            // end: br
        }


        /**
         * @return Private Company Number
         */
        public int PrivateNumber
        {
            get
            {
                return privateNumber;
            }
        }

        /**
         * @return Base Price
         */
        public int BasePrice
        {
            get
            {
                return basePrice;
            }
        }

        /**
         * @return Revenue
         */
        public List<int> Revenue
        {
            get
            {
                return revenue;
            }
        }

        //  start: sfy 1889: new method
        public int GetRevenueByPhase(Phase phase)
        {
            if (phase != null)
            {
                return revenue[Math.Min(revenue.Count, phase.PrivatesRevenueStep) - 1];
            }
            else
            {
                return 0;
            }
        }
        // end: sfy 1889

        /**
         * @return Phase this Private closes
         */
        public int ClosingPhase
        {
            get
            {
                return closingPhase;
            }
        }

        public void SetClosed()
        {

            if (IsClosed()) return;
            //        if (!isCloseable()) return;  /* moved hat to call in closeAllPrivates, to allow other closing actions */

            closed.Set(true);

            UnblockHexes();

            MoveTo(GetRoot.Bank.ScrapHeap);

            ReportBuffer.Add(this, LocalText.GetText("PrivateCloses", Id));

            // For 1856: buyable tokens still owned by the private will now
            // become commonly buyable, i.e. owned by GameManager.
            // (Note: all such tokens will be made buyable from the Bank too,
            // this is done in OperatingRound_1856).
            List<SellBonusToken> moveToGM = new List<SellBonusToken>(4);
            foreach (SpecialProperty sp in specialProperties)
            {
                if (sp is SellBonusToken)
                {
                    moveToGM.Add((SellBonusToken)sp);
                }
            }
            foreach (SellBonusToken sbt in moveToGM)
            {
                GetRoot.GameManager.CommonSpecialPropertiesPortfolio.Add(sbt);
                log.Debug("SP " + sbt.Id + " is now a common property");
            }
        }

        /* start sfy 1889 */
        public bool IsCloseable()
        {

            if ((preventClosingConditions == null) || preventClosingConditions.Count == 0) return true;

            if (preventClosingConditions.Contains("doesNotClose"))
            {
                log.Debug("Private Company " + Id + " does not close (unconditional).");
                return false;
            }
            if (preventClosingConditions.Contains("ifOwnedByPlayer")
                    && Owner is Player)
            {
                log.Debug("Private Company " + Id + " does not close, as it is owned by a player.");
                return false;
            }
            return true;
        }

        public List<string> GetPreventClosingConditions()
        {
            return preventClosingConditions;
        }
        /* end sfy 1889 */

        /**
         * @param i
         */
        public void SetClosingPhase(int i)
        {
            closingPhase = i;
        }

        public void UnblockHexes()
        {
            if (blockedHexes != null)
            {
                foreach (MapHex hex in blockedHexes)
                {
                    hex.SetBlockingPrivateCompany(null);
                }
            }
        }

        override public string ToString()
        {
            return "Private: " + Id;
        }


        public object Clone()
        {
            return MemberwiseClone();
            //object clone = null;
            //try
            //{
            //    clone = super.clone();
            //}
            //catch (CloneNotSupportedException e)
            //{
            //    log.error("Cannot clone company " + getId());
            //    return null;
            //}
            //
            //return clone;
        }

        public List<MapHex> BlockedHexes
        {
            get
            {
                return blockedHexes;
            }
        }

        public bool ClosesIfAllExercised
        {
            get
            {
                return closeIfAllExercised;
            }
        }

        public bool ClosesIfAnyExercised
        {
            get
            {
                return closeIfAnyExercised;
            }
        }

        public bool ClosesAtEndOfTurn
        {
            get
            {
                return closeAtEndOfTurn;
            }
        }

        public bool ClosesManually
        {
            get
            {
                return closeManually;
            }
        }

        public void CheckClosingIfExercised(bool endOfTurn)
        {

            if (IsClosed() || endOfTurn != closeAtEndOfTurn) return;

            if (closeIfAllExercised)
            {
                foreach (SpecialProperty sp in specialProperties)
                {
                    if (!sp.IsExercised()) return;
                }
                log.Debug("CloseIfAll: closing " + Id);
                SetClosed();

            }
            else if (closeIfAnyExercised)
            {
                foreach (SpecialProperty sp in specialProperties)
                {
                    if (sp.IsExercised())
                    {
                        log.Debug("CloseIfAny: closing " + Id);
                        SetClosed();
                        return;
                    }
                }
            }
        }

        public string GetClosingInfo()
        {
            return null;
        }

        public void Close()
        {
            SetClosed();
        }

        /**
         * @return Returns the upperPrice that the company can be sold in for.
         */
        public int GetUpperPrice()
        {
            return GetUpperPrice(false);
        }

        public int GetUpperPrice(bool saleToPlayer)
        {
            if (saleToPlayer)
            {
                return upperPlayerPrice;
            }

            return upperPrice;
        }

        /**
         * @return Returns the lowerPrice that the company can be sold in for.
         */
        public int GetLowerPrice()
        {
            return GetLowerPrice(false);
        }

        public int GetLowerPrice(bool saleToPlayer)
        {
            if (saleToPlayer)
            {
                return lowerPlayerPrice;
            }

            return lowerPrice;
        }

        /**
         * @return Returns whether or not the company can be bought by a company
         */
        public bool TradeableToCompany
        {
            get
            {
                return tradeableToCompany;
            }
        }

        /**
         * @return Returns whether or not the company can be bought by a player (from another player)
         */
        public bool TradeableToPlayer
        {
            get
            {
                return tradeableToPlayer;
            }
        }

        /**
         * Do we have any special properties?
         *
         * @return Boolean
         */
        public bool HasSpecialProperties
        {
            get
            {
                return !specialProperties.IsEmpty;
            }
        }

        // Company methods
        public void InitType(CompanyType type)
        {
            this.type = type;
        }

        public CompanyType CompanyType
        {
            get
            {
                return type;
            }
        }

        public bool IsClosed()
        {
            return closed.Value;
        }

        public string LongName
        {
            get
            {
                return longName;
            }
        }

        public string Alias
        {
            get
            {
                return alias;
            }
        }

        public string InfoText
        {
            get
            {
                return infoText;
            }
        }

        public IReadOnlyCollection<SpecialProperty> GetSpecialProperties()
        {
            return specialProperties.Items;
        }

        // RailsItem methods
        new public IRailsItem Parent
        {
            get
            {
                return (IRailsItem)base.Parent;
            }
        }

        new public RailsRoot GetRoot
        {
            get
            {
                return (RailsRoot)base.GetRoot;
            }
        }

        // Certificate Interface

        public float CertificateCount
        {
            get
            {
                return certificateCount;
            }
            set
            {
                certificateCount = value;
            }
        }

        public bool BlockedForTileLays(MapHex mapHex)
        {
            if (blockedHexes.Contains(mapHex))
            {
                return true;
            }
            return false;
        }

        // Item interface
        override public string ToText()
        {
            return Id;
        }
    }
}
