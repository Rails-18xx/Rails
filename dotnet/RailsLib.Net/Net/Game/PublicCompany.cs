using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies encompass all 18xx company-like entities that lay
 * tracks and run trains. <p> Ownership of companies will always be performed by
 * holding certificates. Some minor company types may have only one certificate,
 * but this will still be the form in which ownership is expressed. <p> Company
 * shares may or may not have a price on the stock market.
 * 
 */

namespace GameLib.Net.Game
{
    public class PublicCompany : RailsAbstractItem, ICompany, IRailsMoneyOwner, IPortfolioOwner, IComparable<PublicCompany>, IComparable
    {
        private static Logger<PublicCompany> log = new Logger<PublicCompany>();

        public const int CAPITALIZE_FULL = 0;

        public const int CAPITALIZE_INCREMENTAL = 1;

        public const int CAPITALIZE_WHEN_BOUGHT = 2;

        protected const int DEFAULT_SHARE_UNIT = 10;

        protected static int numberOfPublicCompanies = 0;

        // Home base & price token lay times
        protected const int WHEN_STARTED = 0;
        protected const int WHEN_FLOATED = 1;
        protected const int START_OF_FIRST_OR = 2; // Only applies to home base tokens

        // Base token lay cost calculation methods
        public const string BASE_COST_SEQUENCE = "sequence";
        public const string BASE_COST_DISTANCE = "distance";

        protected readonly string[] tokenLayTimeNames =
            new string[] { "whenStarted", "whenFloated", "firstOR" };

        protected int homeBaseTokensLayTime = START_OF_FIRST_OR;

        /**
         * Foreground (i.e. text) color of the company tokens (if pictures are not
         * used)
         */
        protected Color fgColor;

        /** Hexadecimal representation (RRGGBB) of the foreground color. */
        protected string fgHexColor = "FFFFFF";

        /** Background color of the company tokens */
        protected Color bgColor;

        /** Hexadecimal representation (RRGGBB) of the background color. */
        protected string bgHexColor = "000000";

        /** Home hex & city *
         * Two home hexes is supported, but only if:<br>
         * 1. The locations are fixed (i.e. configured by XML), and<br>
         * 2. Any station (city) numbers are equal for the two home stations.
         * There is no provision yet for two home hexes having different tile station numbers. */
        protected string homeHexNames = null;
        protected List<MapHex> homeHexes = null;
        protected int homeCityNumber = 1;

        /** Destination hex * */
        protected string destinationHexName = null;
        protected MapHex destinationHex = null;
        protected BooleanState hasReachedDestination;

        /** Sequence number in the array of public companies - may not be useful */
        protected int publicNumber = -1; // For internal use

        protected int numberOfBaseTokens = 0;

        protected int baseTokensBuyCost = 0;
        /** An array of base token laying costs, per successive token */
        protected List<int> baseTokenLayCost;
        protected string baseTokenLayCostMethod = "sequential";

        protected BaseTokensModel baseTokens;
        protected PortfolioModel portfolio;


        /**
         * Initial (par) share price, represented by a stock market location object
         */
        protected PriceModel parPrice;

        /** Current share price, represented by a stock market location object */
        protected PriceModel currentPrice;

        /** Company treasury, holding cash */
        protected PurseMoneyModel treasury;

        /** PresidentModel */
        protected PresidentModel presidentModel;

        /** Has the company started? */
        protected BooleanState hasStarted;

        /** Total bonus tokens amount */
        protected BonusModel bonusValue;

        /** Acquires Bonus objects */
        protected ListState<Bonus> bonuses;

        /** Most recent revenue earned. */
        protected CountingMoneyModel lastRevenue;

        /** Most recent payout decision. */
        protected StringState lastRevenueAllocation;

        /** Is the company operational ("has it floated")? */
        protected BooleanState hasFloated;

        /** Has the company already operated? */
        protected BooleanState hasOperated;

        /** Are company shares buyable (i.e. before started)? */
        protected BooleanState buyable;

        /** In-game state.
         * <p> Will only be set false if the company is closed and cannot ever be reopened.
         * By default it will be set false if a company is closed. */
        // TODO: Check if there was some assumption to be null at some place
        protected BooleanState inGameState;

        // TODO: the extra turn model has to be rewritten (it is not fully undo proof)

        /** Stores the number of turns with extraLays */
        protected Dictionary<string, IntegerState> turnsWithExtraTileLays = null;

        /** This receives the current value of turnsWithExtraTileLays  */
        protected GenericState<IntegerState> extraTiles;

        /* Spendings in the current operating turn */
        protected CountingMoneyModel privatesCostThisTurn;

        protected StringState tilesLaidThisTurn;

        protected CountingMoneyModel tilesCostThisTurn;

        protected StringState tokensLaidThisTurn;

        protected CountingMoneyModel tokensCostThisTurn;

        protected CountingMoneyModel trainsCostThisTurn;

        protected bool canBuyStock = false;

        protected bool canBuyPrivates = false;

        protected bool canUseSpecialProperties = false;

        /** Can a company be restarted once it is closed? */
        protected bool canBeRestarted = false;

        /**
         * Minimum price for buying privates, to be multiplied by the original price
         */
        protected float lowerPrivatePriceFactor;

        /**
         * Maximum price for buying privates, to be multiplied by the original price
         */
        protected float upperPrivatePriceFactor;

        protected bool ipoPaysOut = false;

        protected bool poolPaysOut = false;

        protected bool treasuryPaysOut = false;

        protected bool canHoldOwnShares = false;

        protected int maxPercOfOwnShares = 0;

        protected bool mayTradeShares = false;

        protected bool mustHaveOperatedToTradeShares = false;

        protected List<Tag> certificateTags = null;

        /** The certificates of this company (minimum 1) */
        protected ListState<PublicCertificate> certificates;
        /** Are the certificates available from the first SR? */
        bool certsAreInitiallyAvailable = true;

        /** What percentage of ownership constitutes "one share" */
        protected IntegerState shareUnit;

        /** What number of share units relates to the share price
         * (normally 1, but 2 for 1835 Prussian)
         */
        protected int shareUnitsForSharePrice = 1;

        /** At what percentage sold does the company float */
        protected int floatPerc = 0;

        /** Share price movement on floating (1851: up) */
        protected bool sharePriceUpOnFloating = false;

        /** Does the company have a stock price (minors often don't) */
        protected bool hasStockPrice = true;

        /** Does the company have a par price? */
        protected bool hasParPrice = true;

        protected bool splitAllowed = false;

        /** Is the revenue always split (typical for non-share minors) */
        protected bool splitAlways = false;

        /** Must payout exceed stock price to move token right? */
        protected bool payoutMustExceedPriceToMove = false;

        /** Multiple certificates those that represent more than one nominal share unit (except president share) */
        protected bool hasMultipleCertificates = false;

        /*---- variables needed during initialization -----*/
        protected string startSpace = null;

        protected int dropPriceToken = WHEN_STARTED;

        protected int capitalization = CAPITALIZE_FULL;

        /** Fixed price (for a 1835-style minor) */
        protected int fixedPrice = 0;

        /** Train limit per phase (index) */
        protected List<int> trainLimit;

        /** Private to close if first train is bought */
        protected string privateToCloseOnFirstTrainName = null;

        protected PrivateCompany privateToCloseOnFirstTrain = null;

        /** Must the company own a train */
        protected bool mustOwnATrain = true;

        protected bool mustTradeTrainsAtFixedPrice = false;

        /** Can the company price token go down to a "Close" square?
         * 1856 CGR cannot.
         */
        protected bool canClose = true;

        /** Initial train at floating time */
        protected string initialTrainType = null;
        protected int initialTrainCost = 0;
        protected bool initialTrainTradeable = true;

        /* Loans */
        protected int maxNumberOfLoans = 0;
        protected int valuePerLoan = 0;
        protected IntegerState currentNumberOfLoans = null; // init during finishConfig
        protected int loanInterestPct = 0;
        protected int maxLoansPerRound = 0;
        protected CountingMoneyModel currentLoanValue = null; // init during finishConfig

        protected BooleanState canSharePriceVary;

        protected RightsModel rightsModel = null; // init if required
                                                  // created in finishConfiguration

        // used for Company interface
        private string longName;
        private string alias;
        private CompanyType type;
        private string infoText = "";
        private string parentInfoText = "";
        private BooleanState closed;

        /**
         *  Relation to a later to be founded National/Regional Major Company 
         *  */
        private string relatedPublicCompany = null;

        private string foundingStartCompany = null;

        /**
         * Used by Configure (via reflection) only
         */
        public PublicCompany(IRailsItem parent, string id) : base(parent, id)
        {
            hasReachedDestination = BooleanState.Create(this, "hasReachedDestinations");
            baseTokens = BaseTokensModel.Create(this, "baseTokens"); // Create after cloning ?
            portfolio = PortfolioModel.Create(this);
            treasury = PurseMoneyModel.Create(this, "treasury", false);
            presidentModel = PresidentModel.Create(this);
            hasStarted = BooleanState.Create(this, "hasStarted");
            bonusValue = BonusModel.Create(this, "bonusValue");
            bonuses = ListState<Bonus>.Create(this, "bonuses");
            lastRevenue = CountingMoneyModel.Create(this, "lastRevenue", false);
            lastRevenueAllocation = StringState.Create(this, "lastRevenueAllocation");
            hasFloated = BooleanState.Create(this, "hasFloated");
            hasOperated = BooleanState.Create(this, "hasOperated");
            buyable = BooleanState.Create(this, "buyable");
            inGameState = BooleanState.Create(this, "inGameState", true);
            extraTiles = GenericState<IntegerState>.Create(this, "extraTiles");
            privatesCostThisTurn = CountingMoneyModel.Create(this, "privatesCostThisTurn", false);
            tilesLaidThisTurn = StringState.Create(this, "tilesLaidThisTurn");
            tilesCostThisTurn = CountingMoneyModel.Create(this, "tilesCostThisTurn", false);
            tokensLaidThisTurn = StringState.Create(this, "tokenLaidThisTurn");
            tokensCostThisTurn = CountingMoneyModel.Create(this, "tokensCostThisTurn", false);
            trainsCostThisTurn = CountingMoneyModel.Create(this, "trainsCostThisTurn", false);
            certificates = ListState<PublicCertificate>.Create(this, "ownCertificates");
            shareUnit = IntegerState.Create(this, "shareUnit", DEFAULT_SHARE_UNIT);
            closed = BooleanState.Create(this, "closed", false);

            lastRevenue.SetSuppressInitialZero(true);

            /* Spendings in the current operating turn */
            privatesCostThisTurn.SetSuppressZero(true);
            tilesCostThisTurn.SetSuppressZero(true);
            tokensCostThisTurn.SetSuppressZero(true);
            trainsCostThisTurn.SetSuppressZero(true);
            trainsCostThisTurn.SetDisplayNegative(true);

            // Bonuses
            bonusValue.SetBonuses(bonuses);

            if (hasStockPrice)
            {
                parPrice = PriceModel.Create(this, "ParPrice", false);
                currentPrice = PriceModel.Create(this, "currentPrice", true);
                canSharePriceVary = BooleanState.Create(this, "canSharePriceVary", true);
            }
        }


        /**
         * To configure all public companies from the &lt;PublicCompany&gt; XML
         * element
         */
        public void ConfigureFromXML(Tag tag)
        {

            longName = tag.GetAttributeAsString("longname", Id);
            infoText = "<html>" + longName;

            alias = tag.GetAttributeAsString("alias", alias);

            /* Configure public company features */
            fgHexColor = tag.GetAttributeAsString("fgColor", fgHexColor);
            fgColor = Util.Util.ParseColor(fgHexColor);

            bgHexColor = tag.GetAttributeAsString("bgColour", bgHexColor);
            bgColor = Util.Util.ParseColor(bgHexColor);

            floatPerc = tag.GetAttributeAsInteger("floatPerc", floatPerc);

            relatedPublicCompany = tag.GetAttributeAsString("relatedCompany", relatedPublicCompany);

            foundingStartCompany = tag.GetAttributeAsString("foundingCompany", foundingStartCompany);

            startSpace = tag.GetAttributeAsString("startspace");
            // Set the default price token drop time.
            // Currently, no exceptions exist, so this value isn't changed anywhere yet.
            // Any (future) games with exceptions to these defaults will require a separate XML attribute.
            // Known games to have exceptions: 1837.
            dropPriceToken = startSpace != null ? WHEN_FLOATED : WHEN_STARTED;

            fixedPrice = tag.GetAttributeAsInteger("price", 0);

            numberOfBaseTokens = tag.GetAttributeAsInteger("tokens", 1);

            certsAreInitiallyAvailable = tag.GetAttributeAsBoolean("available", certsAreInitiallyAvailable);

            canBeRestarted = tag.GetAttributeAsBoolean("restartable", canBeRestarted);

            Tag shareUnitTag = tag.GetChild("ShareUnit");
            if (shareUnitTag != null)
            {
                shareUnit.Set(shareUnitTag.GetAttributeAsInteger("percentage", DEFAULT_SHARE_UNIT));
                shareUnitsForSharePrice = shareUnitTag.GetAttributeAsInteger("sharePriceUnits", shareUnitsForSharePrice);
            }

            Tag homeBaseTag = tag.GetChild("Home");
            if (homeBaseTag != null)
            {
                homeHexNames = homeBaseTag.GetAttributeAsString("hex");
                homeCityNumber = homeBaseTag.GetAttributeAsInteger("city", 1);
            }

            Tag destinationTag = tag.GetChild("Destination");
            if (destinationTag != null)
            {
                destinationHexName = destinationTag.GetAttributeAsString("hex");
            }

            Tag privateBuyTag = tag.GetChild("CanBuyPrivates");
            if (privateBuyTag != null)
            {
                canBuyPrivates = true;
            }

            Tag canUseSpecTag = tag.GetChild("CanUseSpecialProperties");
            if (canUseSpecTag != null) canUseSpecialProperties = true;

            // Extra info text(usually related to extra-share special properties)
            Tag infoTag = tag.GetChild("Info");
            if (infoTag != null)
            {
                string infoKey = infoTag.GetAttributeAsString("key");
                string[] infoParms = infoTag.GetAttributeAsString("parm", "").Split(',');
                infoText += "<br>" + LocalText.GetText(infoKey, (object[])infoParms);
            }

            // Special properties (as in the 1835 black minors)
            parentInfoText += SpecialProperty.Configure(this, tag);

            poolPaysOut = poolPaysOut || tag.GetChild("PoolPaysOut") != null;

            ipoPaysOut = ipoPaysOut || tag.GetChild("IPOPaysOut") != null;

            Tag floatTag = tag.GetChild("Float");
            if (floatTag != null)
            {
                floatPerc = floatTag.GetAttributeAsInteger("percentage", floatPerc);
                string sharePriceAttr = floatTag.GetAttributeAsString("price");
                if (!string.IsNullOrEmpty(sharePriceAttr))
                {
                    sharePriceUpOnFloating = sharePriceAttr.Equals("up", StringComparison.OrdinalIgnoreCase);
                }
            }

            Tag priceTag = tag.GetChild("StockPrice");
            if (priceTag != null)
            {
                hasStockPrice = priceTag.GetAttributeAsBoolean("market", true);
                hasParPrice = priceTag.GetAttributeAsBoolean("par", hasStockPrice);
            }

            Tag payoutTag = tag.GetChild("Payout");
            if (payoutTag != null)
            {
                string split = payoutTag.GetAttributeAsString("split", "no");
                splitAlways = split.Equals("always", StringComparison.OrdinalIgnoreCase);
                splitAllowed = split.Equals("allowed", StringComparison.OrdinalIgnoreCase);

                payoutMustExceedPriceToMove = payoutTag.GetAttributeAsBoolean("mustExceedPriceToMove", false);
            }

            Tag ownSharesTag = tag.GetChild("TreasuryCanHoldOwnShares");
            if (ownSharesTag != null)
            {
                canHoldOwnShares = true;
                treasuryPaysOut = true;

                maxPercOfOwnShares = ownSharesTag.GetAttributeAsInteger("maxPerc", maxPercOfOwnShares);
            }

            Tag trainsTag = tag.GetChild("Trains");
            if (trainsTag != null)
            {
                trainLimit = new List<int>(trainsTag.GetAttributeAsIntegerList("limit"));
                mustOwnATrain = trainsTag.GetAttributeAsBoolean("mandatory", mustOwnATrain);
            }

            Tag initialTrainTag = tag.GetChild("InitialTrain");
            if (initialTrainTag != null)
            {
                initialTrainType = initialTrainTag.GetAttributeAsString("type");
                initialTrainCost = initialTrainTag.GetAttributeAsInteger("cost", initialTrainCost);
                initialTrainTradeable = initialTrainTag.GetAttributeAsBoolean("tradeable", initialTrainTradeable);
            }

            Tag firstTrainTag = tag.GetChild("FirstTrainCloses");
            if (firstTrainTag != null)
            {
                string typeName = firstTrainTag.GetAttributeAsString("type", "Private");
                if (typeName.Equals("Private", StringComparison.OrdinalIgnoreCase))
                {
                    privateToCloseOnFirstTrainName = firstTrainTag.GetAttributeAsString("name");
                }
                else
                {
                    throw new ConfigurationException("Only Privates can be closed on first train buy");
                }
            }

            Tag capitalizationTag = tag.GetChild("Capitalization");
            if (capitalizationTag != null)
            {
                string capType = capitalizationTag.GetAttributeAsString("type", "full");
                if (capType.Equals("full", StringComparison.OrdinalIgnoreCase))
                {
                    Capitalization = CAPITALIZE_FULL;
                }
                else if (capType.Equals("incremental", StringComparison.OrdinalIgnoreCase))
                {
                    Capitalization = CAPITALIZE_INCREMENTAL;
                }
                else if (capType.Equals("whenBought", StringComparison.OrdinalIgnoreCase))
                {
                    Capitalization = CAPITALIZE_WHEN_BOUGHT;
                }
                else
                {
                    throw new ConfigurationException("Invalid capitalization type: " + capType);
                }
            }


            // TODO: Check if this still works correctly
            // The certificate init was moved to the finishConfig phase
            // as PublicCompany is configured twice
            List<Tag> certTags = tag.GetChildren("Certificate");
            if (certTags != null) certificateTags = certTags;

            // BaseToken
            Tag baseTokenTag = tag.GetChild("BaseTokens");
            if (baseTokenTag != null)
            {

                // Cost of laying a token
                Tag layCostTag = baseTokenTag.GetChild("LayCost");
                if (layCostTag != null)
                {
                    baseTokenLayCostMethod = layCostTag.GetAttributeAsString("method", baseTokenLayCostMethod);
                    if (baseTokenLayCostMethod.Equals(BASE_COST_SEQUENCE, StringComparison.OrdinalIgnoreCase))
                    {
                        baseTokenLayCostMethod = BASE_COST_SEQUENCE;
                    }
                    else if (baseTokenLayCostMethod.Equals(BASE_COST_DISTANCE, StringComparison.OrdinalIgnoreCase))
                    {
                        baseTokenLayCostMethod = BASE_COST_DISTANCE;
                    }
                    else
                    {
                        throw new ConfigurationException(
                                "Invalid base token lay cost calculation method: "
                                + baseTokenLayCostMethod);
                    }

                    baseTokenLayCost = new List<int>(layCostTag.GetAttributeAsIntegerList("cost"));
                }

                /* Cost of buying a token (mutually exclusive with laying cost) */
                Tag buyCostTag = baseTokenTag.GetChild("BuyCost");
                if (buyCostTag != null)
                {
                    baseTokensBuyCost =
                        buyCostTag.GetAttributeAsInteger("initialTokenCost", 0);
                }

                Tag tokenLayTimeTag = baseTokenTag.GetChild("HomeBase");
                if (tokenLayTimeTag != null)
                {
                    // When is the home base laid?
                    // Note: if not before, home tokens are in any case laid
                    // at the start of the first OR
                    string layTimeString =
                        tokenLayTimeTag.GetAttributeAsString("lay");
                    if (!string.IsNullOrEmpty(layTimeString))
                    {
                        for (int i = 0; i < tokenLayTimeNames.Length; i++)
                        {
                            if (tokenLayTimeNames[i].Equals(layTimeString, StringComparison.OrdinalIgnoreCase))
                            {
                                homeBaseTokensLayTime = i;
                                break;
                            }
                        }
                    }
                }
            }

            Tag sellSharesTag = tag.GetChild("TradeShares");
            if (sellSharesTag != null)
            {
                mayTradeShares = true;
                mustHaveOperatedToTradeShares =
                    sellSharesTag.GetAttributeAsBoolean("mustHaveOperated", mustHaveOperatedToTradeShares);
            }

            Tag loansTag = tag.GetChild("Loans");
            if (loansTag != null)
            {
                maxNumberOfLoans = loansTag.GetAttributeAsInteger("number", -1);
                // Note: -1 means undefined, to be handled in the code
                // (for instance: 1856).
                valuePerLoan = loansTag.GetAttributeAsInteger("value", 0);
                loanInterestPct = loansTag.GetAttributeAsInteger("interest", 0);
                maxLoansPerRound = loansTag.GetAttributeAsInteger("perRound", -1);
            }

            Tag optionsTag = tag.GetChild("Options");
            if (optionsTag != null)
            {
                mustTradeTrainsAtFixedPrice = optionsTag.GetAttributeAsBoolean(
                    "mustTradeTrainsAtFixedPrice", mustTradeTrainsAtFixedPrice);
                canClose = optionsTag.GetAttributeAsBoolean("canClose", canClose);
            }
        }


        public void SetIndex(int index)
        {
            publicNumber = index;
        }

        /**
         * Final initialization, after all XML has been processed.
         */
        public void FinishConfiguration(RailsRoot root)
        {

            if (maxNumberOfLoans != 0)
            {
                currentNumberOfLoans = IntegerState.Create(this, "currentNumberOfLoans");
                currentLoanValue = CountingMoneyModel.Create(this, "currentLoanValue", false);
                currentLoanValue.SetSuppressZero(true);
            }

            if (hasStockPrice && !string.IsNullOrEmpty(startSpace))
            {
                parPrice.SetPrice(GetRoot.StockMarket.GetStockSpace(startSpace));
                if (parPrice.GetPrice() == null)
                    throw new ConfigurationException("Invalid start space "
                            + startSpace + " for company "
                            + Id);
                currentPrice.SetPrice(parPrice.GetPrice());
            }

            int certIndex = 0;
            if (certificateTags != null)
            {
                int shareTotal = 0;
                bool gotPresident = false;
                PublicCertificate certificate;
                // Throw away
                // the per-type
                // specification

                // TODO: Move this to PublicCertificate class, as it belongs there
                foreach (Tag certificateTag in certificateTags)
                {
                    int shares = certificateTag.GetAttributeAsInteger("shares", 1);

                    bool president = "President".Equals(certificateTag.GetAttributeAsString("type", ""));
                    int number = certificateTag.GetAttributeAsInteger("number", 1);

                    bool certIsInitiallyAvailable
                        = certificateTag.GetAttributeAsBoolean("available", certsAreInitiallyAvailable);

                    float certificateCount = certificateTag.GetAttributeAsFloat("certificateCount", 1.0f);

                    if (president)
                    {
                        if (number > 1 || gotPresident)
                            throw new ConfigurationException(
                                    "Company type "
                                    + Id
                                    + " cannot have multiple President shares");
                        gotPresident = true;
                    }

                    for (int k = 0; k < number; k++)
                    {
                        certificate = new PublicCertificate(this, "cert_" + certIndex, shares, president,
                                certIsInitiallyAvailable, certificateCount, certIndex++);
                        certificates.Add(certificate);
                        shareTotal += shares * shareUnit.Value;
                    }
                }
                if (shareTotal != 100)
                {
                    throw new ConfigurationException("Company type " + Id + " total shares is not 100%");
                }
            }

            NameCertificates();

            // Give each certificate an unique Id
            PublicCertificate cert;
            for (int i = 0; i < certificates.Count; i++)
            {
                cert = certificates.Get(i);
                cert.SetUniqueId(Id, i);
                cert.IsInitiallyAvailable = cert.IsInitiallyAvailable && this.certsAreInitiallyAvailable;
            }

            List<BaseToken> newTokens = new List<BaseToken>();
            for (int i = 0; i < numberOfBaseTokens; i++)
            {
                BaseToken token = BaseToken.Create(this);
                newTokens.Add(token);
            }
            baseTokens.InitTokens(newTokens);

            if (homeHexNames != null)
            {
                homeHexes = new List<MapHex>(2);
                MapHex homeHex;
                foreach (string homeHexName in homeHexNames.Split(','))
                {
                    homeHex = GetRoot.MapManager.GetHex(homeHexName);
                    if (homeHex == null)
                    {
                        throw new ConfigurationException("Invalid home hex "
                                + homeHexName
                                + " for company " + Id);
                    }
                    homeHexes.Add(homeHex);
                    infoText += "<br>Home: " + homeHex.ToText();
                }
            }

            if (destinationHexName != null)
            {
                destinationHex = GetRoot.MapManager.GetHex(destinationHexName);
                if (destinationHex == null)
                {
                    throw new ConfigurationException("Invalid destination hex "
                            + destinationHexName
                            + " for company " + Id);
                }
                infoText += "<br>Destination: " + destinationHex.ToText();
            }

            if (!string.IsNullOrEmpty(privateToCloseOnFirstTrainName))
            {
                privateToCloseOnFirstTrain = GetRoot.CompanyManager.GetPrivateCompany(privateToCloseOnFirstTrainName);
            }

            if (trainLimit != null)
            {
                infoText += "<br>" + LocalText.GetText("CompInfoMaxTrains",
                        string.Join(", ", trainLimit));// Util.joinWithDelimiter(trainLimit, ", "));

            }

            infoText += parentInfoText;
            parentInfoText = "";

            // Can companies acquire special rightsModel (such as in 1830 Coalfields)?
            // TODO: Can this be simplified?
            if (portfolio.HasSpecialProperties)
            {
                foreach (SpecialProperty sp in portfolio.GetPersistentSpecialProperties())
                {
                    if (sp is SpecialRight)
                    {
                        GetRoot.GameManager.SetGuiParameter(GuiDef.Parm.HAS_ANY_RIGHTS, true);
                        // Initialize rightsModel here to prevent overhead if not used,
                        // but if rightsModel are used, the GUI needs it from the start.
                        if (rightsModel == null)
                        {
                            rightsModel = RightsModel.Create(this, "rightsModel");
                        }
                        // TODO: This is only a workaround for the missing finishConfiguration of special properties (SFY)
                        sp.FinishConfiguration(root);
                    }
                }
            }

            // finish Configuration of portfolio
            portfolio.FinishConfiguration();

            // set multipleCertificates
            foreach (PublicCertificate c in certificates)
            {
                if (!c.IsPresidentShare && c.GetShares() != 1)
                {
                    hasMultipleCertificates = true;
                }
            }
        }

        /** Used in finalizing configuration */
        public void AddExtraTileLayTurnsInfo(string color, int turns)
        {
            if (turnsWithExtraTileLays == null)
            {
                turnsWithExtraTileLays = new Dictionary<string, IntegerState>();
            }
            IntegerState tileLays = IntegerState.Create
                    (this, "" + color + "_ExtraTileTurns", turns);
            turnsWithExtraTileLays[color] = tileLays;
        }

        /** Reset turn objects */
        public void InitTurn()
        {

            if (!HasLaidHomeBaseTokens) LayHomeBaseTokens();

            privatesCostThisTurn.Set(0);
            tilesLaidThisTurn.Set("");
            tilesCostThisTurn.Set(0);
            tokensLaidThisTurn.Set("");
            tokensCostThisTurn.Set(0);
            trainsCostThisTurn.Set(0);
        }

        /**
         * Return the company token background color.
         *
         * @return Color object
         */
        public Color BgColor
        {
            get
            {
                return bgColor;
            }
        }

        /**
         * Return the company token background color.
         *
         * @return Hexadecimal string RRGGBB.
         */
        public string HexBgColor
        {
            get
            {
                return bgHexColor;
            }
        }

        /**
         * Return the company token foreground color.
         *
         * @return Color object.
         */
        public Color FgColor
        {
            get
            {
                return fgColor;
            }
        }

        /**
         * Return the company token foreground color.
         *
         * @return Hexadecimal string RRGGBB.
         */
        public string HexFgColor
        {
            get
            {
                return fgHexColor;
            }
        }

        /**
         * Return the company's Home hexes (usually one).
         * @return Returns the homeHex.
         */
        public List<MapHex> GetHomeHexes()
        {
            return homeHexes;
        }

        /**
         * Set a non-fixed company home hex.
         * Only covers setting <i>one</i> home hex.
         * Having <i>two</i> home hexes is currently only supported if the locations are preconfigured.
         * @param homeHex The homeHex to set.
         */
        public void SetHomeHex(MapHex homeHex)
        {
            homeHexes = new List<MapHex>(1);
            homeHexes.Add(homeHex);
        }

        /**
         * @return Returns the homeStation.
         */
        public int HomeCityNumber
        {
            get
            {
                return homeCityNumber;
            }
            /**
             * @param homeStation The homeStation to set.
             */
            set
            {
                homeCityNumber = value;
            }
        }

        /**
         * @return Returns the destinationHex.
         */
        public MapHex DestinationHex
        {
            get
            {
                return destinationHex;
            }
        }

        public bool HasDestination
        {
            get
            {
                return destinationHex != null;
            }
        }

        public bool HasReachedDestination()
        {
            return hasReachedDestination != null &&
            hasReachedDestination.Value;
        }

        public void SetReachedDestination(bool value)
        {
            hasReachedDestination.Set(value);
        }

        /**
         * @return
         */
        public bool CanBuyStock
        {
            get
            {
                return canBuyStock;
            }
        }

        public bool MayTradeShares
        {
            get
            {
                return mayTradeShares;
            }
        }

        /** Stub that allows exclusions such as that 1856 CGR may not buy a 4 */
        public bool MayBuyTrainType(Train train)
        {
            return true;
        }

        public bool MustHaveOperatedToTradeShares
        {
            get
            {
                return mustHaveOperatedToTradeShares;
            }
        }

        public void Start(StockSpace startSpace)
        {

            hasStarted.Set(true);
            if (hasStockPrice) buyable.Set(true);

            // In case of a restart: undo closing
            if (closed.Value) closed.Set(false);

            if (startSpace != null)
            {
                SetParSpace(startSpace);
                SetCurrentSpace(startSpace);

                // Drop the current price token, if allowed at this point
                if (dropPriceToken == WHEN_STARTED)
                {
                    GetRoot.StockMarket.Start(this, startSpace);
                }
            }


            if (homeBaseTokensLayTime == WHEN_STARTED)
            {
                LayHomeBaseTokens();
            }
        }

        public void Start(int price)
        {
            StockSpace startSpace = GetRoot.StockMarket.GetStartSpace(price);
            if (startSpace == null)
            {
                log.Error("Invalid start price " + Bank.Format(this, price)); // TODO: Do this nicer
            }
            else
            {
                Start(startSpace);
            }
        }

        /**
         * Start a company.
         */
        public void Start()
        {
            Start(GetStartSpace());
        }

        public void TransferAssetsFrom(PublicCompany otherCompany)
        {

            if (otherCompany.Cash > 0)
            {
                Currency.WireAll(otherCompany, this);
            }
            portfolio.TransferAssetsFrom(otherCompany.PortfolioModel);
        }

        /**
         * @return Returns true is the company has started.
         */
        public bool HasStarted()
        {
            return hasStarted.Value;
        }

        /** Make company shares buyable. Only useful where shares become
         * buyable before the company has started (e.g. 1835 Prussian).
         * */
        public void SetBuyable(bool buyable)
        {
            this.buyable.Set(buyable);
        }

        public bool IsBuyable()
        {
            return buyable.Value;
        }

        /**
         * Float the company, put its initial cash in the treasury.
         */
        public void SetFloated()
        {

            hasFloated.Set(true);
            // In case of a restart
            if (hasOperated.Value) hasOperated.Set(false);

            // Remove the "unfloated" indicator in GameStatus
            // FIXME: Is this still required?
            // getPresident().getPortfolioModel().getShareModel(this).update();

            if (sharePriceUpOnFloating)
            {
                GetRoot.StockMarket.MoveUp(this);
            }

            // Drop the current price token, if allowed at this point
            if (dropPriceToken == WHEN_FLOATED)
            {
                GetRoot.StockMarket.Start(this, GetCurrentSpace());
            }

            if (homeBaseTokensLayTime == WHEN_FLOATED)
            {
                LayHomeBaseTokens();
            }

            if (initialTrainType != null)
            {
                TrainManager trainManager = GetRoot.TrainManager;
                TrainCertificateType type = trainManager.GetCertTypeByName(initialTrainType);
                Train train = GetRoot.Bank.Ipo.PortfolioModel.GetTrainOfType(type);
                BuyTrain(train, initialTrainCost);
                train.IsTradeable = initialTrainTradeable;
                trainManager.CheckTrainAvailability(train, GetRoot.Bank.Ipo);
            }
        }

        /**
         * Has the company already floated?
         *
         * @return true if the company has floated.
         */
        public bool HasFloated()
        {
            return hasFloated.Value;
        }

        public BooleanState GetFloatedModel()
        {
            return hasFloated;
        }

        /**
         * Has the company already operated?
         *
         * @return true if the company has operated.
         */
        public bool HasOperated()
        {
            return hasOperated.Value;
        }

        public void SetOperated()
        {
            hasOperated.Set(true);
        }

        /** Reinitialize a company, i.e. close it and make the shares available for a new company start.
         * IMplemented rules are now as in 18EU.
         * TODO Will see later if this is generic enough.
         *
         */
        protected void Reinitialize()
        {
            hasStarted.Set(false);
            hasFloated.Set(false);
            hasOperated.Set(false);
            if (parPrice != null && fixedPrice <= 0) parPrice.SetPrice(null);
            if (currentPrice != null) currentPrice.SetPrice(null);
        }

        public BooleanState GetInGameModel()
        {
            return inGameState;
        }

        public BooleanState GetIsClosedModel()
        {
            return closed;
        }

        /**
         * Set the company par price. <p> <i>Note: this method should <b>not</b> be
         * used to start a company!</i> Use <code><b>start()</b></code> in
         * stead.
         *
         * @param spaceI
         */
        public void SetParSpace(StockSpace space)
        {
            if (hasStockPrice)
            {
                if (space != null)
                {
                    parPrice.SetPrice(space);
                }
            }
        }

        /**
         * Get the company par (initial) price.
         *
         * @return StockSpace object, which defines the company start position on
         * the stock chart.
         */
        public StockSpace GetStartSpace()
        {
            if (hasParPrice)
            {
                return parPrice != null ? parPrice.GetPrice() : null;
            }
            else
            {
                return currentPrice != null ? currentPrice.GetPrice() : null;
            }
        }

        public int GetIPOPrice()
        {
            if (hasParPrice)
            {
                if (GetStartSpace() != null)
                {
                    return GetStartSpace().Price;
                }
                else
                {
                    return 0;
                }
            }
            else
            {
                return GetMarketPrice();
            }
        }

        public int GetMarketPrice()
        {
            if (GetCurrentSpace() != null)
            {
                return GetCurrentSpace().Price;
            }
            else
            {
                return 0;
            }
        }

        /** Return the price per share at game end.
         * Normally, it is equal to the market price,
         * but in some games (e.g. 1856) deductions may apply.
         * @return
         */
        public int GetGameEndPrice()
        {
            return GetMarketPrice() / ShareUnitsForSharePrice;
        }

        /**
         * Set a new company price.
         *
         * @param price The StockSpace object that defines the new location on the
         * stock market.
         */
        public void SetCurrentSpace(StockSpace price)
        {
            if (price != null && price != GetCurrentSpace())
            {
                currentPrice.SetPrice(price);
            }
        }

        public PriceModel GetCurrentPriceModel()
        {
            return currentPrice;
        }

        public PriceModel GetParPriceModel()
        {
            // Temporary fix to satisfy GameStatus window. Should be removed there.
            if (parPrice == null) return currentPrice;

            return parPrice;
        }

        /**
         * Get the current company share price.
         *
         * @return The StockSpace object that defines the current location on the
         * stock market.
         */
        public StockSpace GetCurrentSpace()
        {
            return currentPrice != null ? currentPrice.GetPrice() : null;
        }

        // TODO: Compare StockMarket processMove methods and check what can replace the code below
        //    public void updatePlayersWorth() {
        //
        //        Map<Player, Boolean> done = new HashMap<Player, Boolean>(8);
        //        Player owner;
        //        for (PublicCertificate cert : certificates.view()) {
        //            if (cert.getPortfolio() instanceof PortfolioModel // FIXME: What kind of condition is this, was cert.getHolder()
        //                    && cert.getHolder().getOwner() instanceof Player) {
        //                owner = (Player)cert.getHolder().getOwner();
        //                if (!done.containsKey(owner)) {
        //                    owner.updateWorth();
        //                    done.put(owner, true);
        //                }
        //            }
        //        }
        //    }

        public PurseMoneyModel GetPurseMoneyModel()
        {
            return treasury;
        }

        public string GetFormattedCash()
        {
            return treasury.ToText();
        }

        public RailsModel GetText()
        {
            return treasury;
        }

        /**
         * @return
         */
        public int PublicNumber
        {
            get
            {
                return publicNumber;
            }
        }

        /**
         * Get a list of this company's certificates.
         *
         * @return ArrayList containing the certificates (item 0 is the President's
         * share).
         */
        public List<PublicCertificate> GetCertificates()
        {
            return new List<PublicCertificate>(certificates.View());
        }

        /**
         * Backlink the certificates to this company,
         * and give each one a type getId().
         *
         */
        public void NameCertificates()
        {
            foreach (PublicCertificate cert in certificates.View())
            {
                cert.Company = this;
            }
        }

        /**
         * Get the percentage of shares that must be sold to float the company.
         *
         * @return The float percentage.
         */
        public int FloatPercentage
        {
            get
            {
                return floatPerc;
            }
        }

        /** Determine sold percentage for floating purposes */
        public int GetSoldPercentage()
        {
            int soldPercentage = 0;
            foreach (PublicCertificate cert in certificates.View())
            {
                if (CertCountsAsSold(cert))
                {
                    soldPercentage += cert.Share;
                }
            }
            return soldPercentage;
        }

        /** Can be subclassed for games with special rules */
        protected bool CertCountsAsSold(PublicCertificate cert)
        {
            IOwner owner = cert.Owner;
            return owner is Player || owner == GetRoot.Bank.Pool;
        }

        /**
         * Get the company President.
         *
         */
        // FIXME: This has to be redesigned
        // Relying on the ordering is not a good thing
        public Player GetPresident()
        {
            if (HasStarted())
            {
                IOwner owner = certificates.Get(0).Owner;
                if (owner is Player) return (Player)owner;
            }
            return null;
        }

        public PresidentModel GetPresidentModel()
        {
            return presidentModel;
        }

        public PublicCertificate GetPresidentsShare()
        {
            return certificates.Get(0);
        }

        /**
         * Store the last revenue earned by this company.
         *
         * @param i The last revenue amount.
         */
        public void SetLastRevenue(int i)
        {
            lastRevenue.Set(i);
        }

        /**
         * Get the last revenue earned by this company.
         *
         * @return The last revenue amount.
         */
        public int GetLastRevenue()
        {
            return lastRevenue.Value;
        }

        public RailsModel GetLastRevenueModel()
        {
            return lastRevenue;
        }

        /** Last revenue allocation (payout, split, withhold) */
        public void SetLastRevenueAllocation(int allocation)
        {
            if (allocation >= 0 && allocation < SetDividend.NUM_OPTIONS)
            {
                lastRevenueAllocation.Set(LocalText.GetText(SetDividend.GetAllocationNameKey(allocation)));
            }
            else
            {
                lastRevenueAllocation.Set("");
            }
        }

        public string GetLastRevenueAllocationText()
        {
            return lastRevenueAllocation.Value;
        }

        public StringState GetLastRevenueAllocationModel()
        {
            return lastRevenueAllocation;
        }

        /**
         * Determine if the price token must be moved after a dividend payout.
         *
         * @param amount
         */
        public void Payout(int amount)
        {

            if (amount == 0) return;

            // Move the token
            if (hasStockPrice
                    && (!payoutMustExceedPriceToMove
                            || amount >= currentPrice.GetPrice().Price))
            {
                GetRoot.StockMarket.PayOut(this);
            }
        }

        public bool PaysOutToTreasury(PublicCertificate cert)
        {

            IOwner owner = cert.Owner;
            if (owner == GetRoot.Bank.Ipo && ipoPaysOut
                    || owner == GetRoot.Bank.Pool && poolPaysOut)
            {
                return true;
            }
            return false;
        }

        /**
         * Determine if the price token must be moved after a withheld dividend.
         *
         * @param The revenue amount.
         */
        public void Withhold(int amount)
        {
            if (hasStockPrice) GetRoot.StockMarket.Withhold(this);
        }

        /**
         * Is the company completely sold out? This method should return true only
         * if the share price should move up at the end of a stock round. Since 1851
         * (jan 2008) interpreted as: no share is owned either by the Bank or by the
         * company's own Treasury.
         *
         * @return true if the share price can move up.
         */
        public bool IsSoldOut()
        {
            IOwner owner;

            foreach (PublicCertificate cert in certificates.View())
            {
                owner = cert.Owner;
                if (owner is BankPortfolio || owner == cert.Company)
                {
                    return false;
                }
            }
            return true;
        }

        /**
         * @return
         */
        public bool CanBuyPrivates
        {
            get
            {
                return canBuyPrivates;
            }
        }

        public bool CanUseSpecialProperties
        {
            get
            {
                return canUseSpecialProperties;
            }
        }

        /**
         * Get the unit of share.
         *
         * @return The percentage of ownership that is called "one share".
         */
        public int GetShareUnit()
        {
            return shareUnit.Value;
        }

        public int ShareUnitsForSharePrice
        {
            get
            {
                return shareUnitsForSharePrice;
            }
        }

        /** @return true if company has Multiple certificates, representing more than one nominal share unit (except president share) 
         */
        public bool HasMultipleCertificates
        {
            get
            {
                return hasMultipleCertificates;
            }
        }

        override public string ToString()
        {
            return Id;
        }

        public bool HasStockPrice
        {
            get
            {
                return hasStockPrice;
            }
        }

        public bool HasParPrice
        {
            get
            {
                return hasParPrice;
            }
        }

        public bool CanSharePriceVary()
        {
            return canSharePriceVary.Value;
        }

        public int FixedPrice
        {
            get
            {
                return fixedPrice;
            }
        }

        public int BaseTokensBuyCost
        {
            get
            {
                return baseTokensBuyCost;
            }
        }

        public int SharesOwnedByPlayers()
        {
            int shares = 0;
            foreach (PublicCertificate cert in certificates.View())
            {
                if (cert.Owner is Player)
                {
                    shares += cert.GetShares();
                }
            }
            return shares;
        }

        public bool CanHoldOwnShares
        {
            get
            {
                return canHoldOwnShares;
            }
        }

        /**
         * @return Returns the splitAllowed.
         */
        public bool IsSplitAllowed
        {
            get
            {
                return splitAllowed;
            }
        }

        /**
         * @return Returns the splitAlways.
         */
        public bool IsSplitAlways
        {
            get
            {
                return splitAlways;
            }
        }

        /**
         * Check if the presidency has changed for a <b>buying</b> player.
         *
         * @param buyer Player who has just bought a certificate.
         */
        public void CheckPresidencyOnBuy(Player buyer)
        {

            if (!HasStarted() || buyer == GetPresident() || certificates.Count < 2)
                return;
            Player pres = GetPresident();
            int presShare = pres.PortfolioModel.GetShare(this);
            int buyerShare = buyer.PortfolioModel.GetShare(this);
            if (buyerShare > presShare)
            {
                pres.PortfolioModel.SwapPresidentCertificate(this,
                        buyer.PortfolioModel, 0);
                ReportBuffer.Add(this, LocalText.GetText("IS_NOW_PRES_OF", buyer.Id, Id));
            }
        }

        public void CheckPresidency()
        {
            // check if there is a new potential president
            int presidentShareNumber = GetPresident().PortfolioModel.GetShareNumber(this) + 1;
            Player nextPotentialPresident = FindNextPotentialPresident(presidentShareNumber);

            // no change, return
            if (nextPotentialPresident == null)
            {
                return;
            }

            // otherwise Hand presidency to the player with the highest share
            GetPresident().PortfolioModel.SwapPresidentCertificate(this, nextPotentialPresident.PortfolioModel, 2);
            ReportBuffer.Add(this, LocalText.GetText("IS_NOW_PRES_OF", nextPotentialPresident.Id, Id));
        }

        public Player FindPlayerToDump()
        {
            return FindNextPotentialPresident(GetPresidentsShare().GetShares());
        }

        public Player FindNextPotentialPresident(int minimumShareNumber)
        {
            int requiredShareNumber = minimumShareNumber;
            Player potentialDirector = null;

            foreach (Player nextPlayer in GetRoot.PlayerManager.GetNextPlayersAfter(GetPresident(), false, false))
            {
                int nextPlayerShareNumber = nextPlayer.PortfolioModel.GetShareNumber(this);
                if (nextPlayerShareNumber >= requiredShareNumber)
                {
                    potentialDirector = nextPlayer;
                    requiredShareNumber = nextPlayerShareNumber + 1;
                }
            }
            return potentialDirector;
        }

        /**
         * @return Returns the capitalization.
         */
        public int Capitalization
        {
            get
            {
                return capitalization;
            }
            set
            {
                log.Debug("Capitalization=" + capitalization);
                this.capitalization = value;
            }
        }

        public int GetNumberOfShares()
        {
            return 100 / shareUnit.Value;
        }

        /** Get the current maximum number of trains got a given limit index.
         * @parm index The index of the train limit step as defined for the current phase. Values start at 0.
         * <p>N.B. the new style limit steps per phase start at 1,
         * so one must be subtracted before calling this method.
         */
        protected int GetTrainLimit(int index)
        {
            return trainLimit[Math.Min(index, trainLimit.Count - 1)];
        }

        public int GetCurrentTrainLimit()
        {
            return GetTrainLimit(GetRoot.GameManager.CurrentPhase.TrainLimitIndex);
        }

        public int GetNumberOfTrains()
        {
            return portfolio.NumberOfTrains;
        }

        public bool CanRunTrains()
        {
            return portfolio.NumberOfTrains > 0;
        }

        /**
         * Must be called in stead of Portfolio.buyTrain if side-effects can occur.
         */
        public void BuyTrain(Train train, int price)
        {

            // check first if it is bought from another company
            if (train.Owner is PublicCompany)
            {
                PublicCompany previousOwner = (PublicCompany)train.Owner;
                //  adjust the money spent on trains field
                previousOwner.GetTrainsSpentThisTurnModel().Change(-price);
                // pay the money to the other company
                Currency.Wire(this, price, previousOwner);
            }
            else
            { // TODO: make this a serious test, no assumption
              // else it is from the bank
                Currency.ToBank(this, price);
            }

            // increase own train costs
            trainsCostThisTurn.Change(price);
            // move the train to here
            portfolio.TrainsModel.Portfolio.Add(train);
            // check if a private has to be closed on first train buy
            if (privateToCloseOnFirstTrain != null
                    && !privateToCloseOnFirstTrain.IsClosed())
            {
                privateToCloseOnFirstTrain.SetClosed();
            }
        }

        public CountingMoneyModel GetTrainsSpentThisTurnModel()
        {
            return trainsCostThisTurn;
        }

        public void BuyPrivate(PrivateCompany privateCompany, IOwner from, int price)
        {
            if (from != GetRoot.Bank.Ipo)
            {
                // The initial buy is reported from StartRound. This message should also
                // move to elsewhere.
                ReportBuffer.Add(this, LocalText.GetText("BuysPrivateFromFor",
                        Id,
                        privateCompany.Id,
                        from.Id,
                        Bank.Format(this, price)));
            }

            // Move the private certificate
            portfolio.PrivatesOwnedModel.Portfolio.Add(privateCompany);

            // Move the money
            if (price > 0)
            {
                Currency.Wire(this, price, (IMoneyOwner)from); // TODO: Remove the cast
            }
            privatesCostThisTurn.Change(price);

            // Move any special abilities to the portfolio, if configured so
            IEnumerable<SpecialProperty> sps = privateCompany.GetSpecialProperties();
            if (sps != null)
            {
                // Need intermediate List to avoid ConcurrentModificationException
                List<SpecialProperty> spsToMoveHere = new List<SpecialProperty>(2);
                List<SpecialProperty> spsToMoveToGM = new List<SpecialProperty>(2);
                foreach (SpecialProperty sp in sps)
                {
                    if (sp.TransferText.Equals("toCompany", StringComparison.OrdinalIgnoreCase))
                    {
                        spsToMoveHere.Add(sp);
                    }
                    else if (sp.TransferText.Equals("toGameManager", StringComparison.OrdinalIgnoreCase))
                    {
                        // This must be SellBonusToken - remember the owner!
                        if (sp is SellBonusToken)
                        {
                            // TODO: Check if this works correctly
                            ((SellBonusToken)sp).SetSeller(this);
                            // Also note 1 has been used
                            ((SellBonusToken)sp).SetExercised();
                        }
                        spsToMoveToGM.Add(sp);
                    }
                }
                foreach (SpecialProperty sp in spsToMoveHere)
                {
                    sp.MoveTo(portfolio.Parent);
                }
                foreach (SpecialProperty sp in spsToMoveToGM)
                {
                    GetRoot.GameManager.AddSpecialProperty(sp);
                    log.Debug("SP " + sp.Id + " is now a common property");
                }
            }
        }

        public RailsModel GetPrivatesSpentThisTurnModel()
        {
            return privatesCostThisTurn;
        }

        public void LayTile(MapHex hex, Tile tile, int orientation, int cost)
        {
            string tileLaid =
                "#" + tile.ToText() + "/" + hex.Id + "/"
                + hex.GetOrientationName(orientation);
            tilesLaidThisTurn.Append(tileLaid, ", ");

            if (cost > 0) tilesCostThisTurn.Change(cost);

        }

        public void LayTilenNoMapMode(int cost)
        {
            if (cost > 0) tilesCostThisTurn.Change(cost);
            tilesLaidThisTurn.Append(Bank.Format(this, cost), ",");
        }

        public StringState GetTilesLaidThisTurnModel()
        {
            return tilesLaidThisTurn;
        }

        public RailsModel GetTilesCostThisTurnModel()
        {
            return tilesCostThisTurn;
        }

        public void LayBaseToken(MapHex hex, int cost)
        {

            string tokenLaid = hex.Id;
            tokensLaidThisTurn.Append(tokenLaid, ", ");
            if (cost > 0) tokensCostThisTurn.Change(cost);
        }

        public void LayBaseTokennNoMapMode(int cost)
        {
            if (cost > 0) tokensCostThisTurn.Change(cost);
            tokensLaidThisTurn.Append(Bank.Format(this, cost), ",");
        }

        /**
         * Calculate the cost of laying a token, given the hex where
         * the token is laid. This only makes a difference for the "distance" method.
         * @param hex The hex where the token is to be laid.
         * @return The cost of laying that token.
         */
        public int GetBaseTokenLayCost(MapHex hex)
        {

            if (baseTokenLayCost == null) return 0;

            if (baseTokenLayCostMethod.Equals(BASE_COST_SEQUENCE))
            {
                int index = GetNumberOfLaidBaseTokens();

                if (index >= baseTokenLayCost.Count)
                {
                    index = baseTokenLayCost.Count - 1;
                }
                else if (index < 0)
                {
                    index = 0;
                }
                return baseTokenLayCost[index];
            }
            else if (baseTokenLayCostMethod.Equals(BASE_COST_DISTANCE))
            {
                if (hex == null)
                {
                    return baseTokenLayCost[0];
                }
                else
                {
                    // WARNING: no provision yet for multiple home hexes.
                    return GetRoot.MapManager.GetHexDistance(homeHexes[0], hex) * baseTokenLayCost[0];
                }
            }
            else
            {
                return 0;
            }
        }

        /** Return all possible token lay costs to be incurred for the
         * company's next token lay. For the distance method it will be a full list
         */
        public HashSet<int> GetBaseTokenLayCosts()
        {
            var ret = new HashSet<int>();
            if (baseTokenLayCostMethod.Equals(BASE_COST_SEQUENCE))
            {
                ret.Add(GetBaseTokenLayCost(null));
                return ret;
                //return ImmutableSet.of(getBaseTokenLayCost(null));
            }
            else if (baseTokenLayCostMethod.Equals(BASE_COST_DISTANCE))
            {
                // WARNING: no provision yet for multiple home hexes.
                var distances = GetRoot.MapManager.GetCityDistances(homeHexes[0]);
                //ImmutableSet.Builder<Integer> costs = ImmutableSet.builder();
                //HashSet<int> costs = new HashSet<int>();
                foreach (int distance in distances)
                {
                    ret.Add(distance * baseTokenLayCost[0]);
                }
                return ret;
            }
            else
            {
                ret.Add(0);
                return ret;
            }
        }

        public StringState GetTokensLaidThisTurnModel()
        {
            return tokensLaidThisTurn;
        }

        public MoneyModel GetTokensCostThisTurnModel()
        {
            return tokensCostThisTurn;
        }

        public BaseTokensModel GetBaseTokensModel()
        {
            return baseTokens;
        }

        public bool AddBonus(Bonus bonus)
        {
            bonuses.Add(bonus);
            return true;
        }

        public bool RemoveBonus(Bonus bonus)
        {
            bonus.Close(); // close the bonus
                           // TODO: add bonusValue as dependence to bonuses
            bonuses.Remove(bonus);
            return true;
        }

        public bool RemoveBonus(string name)
        {
            if (bonuses != null && !bonuses.IsEmpty)
            {
                foreach (Bonus bonus in bonuses.View())
                {
                    if (bonus.Name.Equals(name)) return RemoveBonus(bonus);
                }
            }
            return false;
        }

        public List<Bonus> GetBonuses()
        {
            return new List<Bonus>(bonuses.View());
        }

        public BonusModel GetBonusTokensModel()
        {
            return bonusValue;
        }

        public bool HasLaidHomeBaseTokens
        {
            get
            {
                return baseTokens.NbLaidTokens > 0;
            }
        }

        // Return value is not used
        public bool LayHomeBaseTokens()
        {

            if (HasLaidHomeBaseTokens) return true;

            foreach (MapHex homeHex in homeHexes)
            {
                if (homeCityNumber == 0)
                {
                    // This applies to cases like 1830 Erie and 1856 THB.
                    // On a trackless tile it does not matter, but if
                    // the tile has track (such as the green OO tile),
                    // the player must select a city.
                    if (homeHex.CurrentTile.HasNoStationTracks)
                    {
                        // No tracks, then it doesn't matter
                        homeCityNumber = 1;
                    }
                    else
                    {
                        // Cover the case that there already is another token.
                        // Allowing this is optional for 1856 Hamilton (THB home)
                        /*Set<Stop>*/
                        var stops = homeHex.Stops;
                        List<Stop> openStops = new List<Stop>();
                        foreach (Stop stop in stops)
                        {
                            if (stop.HasTokenSlotsLeft) openStops.Add(stop);
                        }
                        if (openStops.Count == 1)
                        {
                            // Just one spot: lay the home base there.
                            homeCityNumber = openStops[0].GetRelatedNumber();
                        }
                        else
                        {
                            // ??
                            // TODO Will player be asked??
                            return false;
                        }
                    }
                }
                log.Debug(Id + " lays home base on " + homeHex.Id + " city "
                        + homeCityNumber);
                homeHex.LayBaseToken(this, homeHex.GetRelatedStop(homeCityNumber));
            }
            return true;
        }

        /**
         * @return the next (free) token to lay, null if none is available
         */
        public BaseToken GetNextBaseToken()
        {
            return baseTokens.GetNextToken();
        }

        public IReadOnlyCollection<BaseToken> GetAllBaseTokens()
        {
            return baseTokens.GetAllTokens();
        }

        public IReadOnlyCollection<BaseToken> GetLaidBaseTokens()
        {
            return baseTokens.GetLaidTokens();
        }

        public int GetNumberOfBaseTokens()
        {
            return baseTokens.NbAllTokens;
        }

        public int GetNumberOfFreeBaseTokens()
        {
            return baseTokens.NbFreeTokens;
        }

        public int GetNumberOfLaidBaseTokens()
        {
            return baseTokens.NbLaidTokens;
        }

        public bool HasBaseTokens
        {
            get
            {
                return (baseTokens.NbAllTokens > 0);
            }
        }

        public int GetNumberOfTileLays(string tileColour)
        {

            Phase phase = GetRoot.PhaseManager.GetCurrentPhase();

            // Get the number of tile lays from Phase
            int tileLays = phase.GetTileLaysPerColour(CompanyType.Id, tileColour);

            // standard cases: 0 and 1, return
            if (tileLays <= 1) return tileLays;

            // More than one tile lay allowed.
            // Check if there is a limitation on the number of turns that this is valid.
            if (turnsWithExtraTileLays != null)
            {
                extraTiles.Set(turnsWithExtraTileLays[tileColour]);
            }

            // check if extraTiles is defined
            if (extraTiles.Value != null)
            {
                // the value is zero already, thus no extra tiles
                if (extraTiles.Value.Value == 0)
                {
                    return 1;
                }
                else
                {
                    // reduce the number of turns by one
                    extraTiles.Value.Add(-1);
                }
            }
            return tileLays;
        }

        public bool MustOwnATrain
        {
            get
            {
                return mustOwnATrain;
            }
        }

        public bool MustTradeTrainsAtFixedPrice
        {
            get
            {
                return mustTradeTrainsAtFixedPrice;
            }
        }

        public int GetCurrentNumberOfLoans()
        {
            return currentNumberOfLoans.Value;
        }

        public int GetCurrentLoanValue()
        {
            return GetCurrentNumberOfLoans() * ValuePerLoan;
        }

        public void AddLoans(int number)
        {
            currentNumberOfLoans.Add(number);
            currentLoanValue.Change(number * ValuePerLoan);
        }

        public int LoanInterestPct
        {
            get
            {
                return loanInterestPct;
            }
        }

        public int MaxNumberOfLoans
        {
            get
            {
                return maxNumberOfLoans;
            }
        }

        public bool CanLoan
        {
            get
            {
                return maxNumberOfLoans != 0;
            }
        }

        public int MaxLoansPerRound
        {
            get
            {
                return maxLoansPerRound;
            }
        }

        public int ValuePerLoan
        {
            get
            {
                return valuePerLoan;
            }
        }

        public MoneyModel GetLoanValueModel()
        {
            return currentLoanValue;
        }

        public Observable GetRightsModel()
        {
            return rightsModel;
        }

        public bool CanClose
        {
            get
            {
                return canClose;
            }
        }

        public void SetRight(SpecialRight right)
        {
            if (rightsModel == null)
            {
                rightsModel = RightsModel.Create(this, "RightsModel");
            }
            rightsModel.Add(right);
        }

        public bool HasRight(SpecialRight right)
        {
            return rightsModel.Contains(right);
        }

        public object Clone()
        {
            return MemberwiseClone();

            //Object clone = null;
            //try
            //{
            //    clone = super.clone();
            //}
            //catch (CloneNotSupportedException e)
            //{
            //    log.error("Cannot clone company " + getId());
            //    return null;
            //}

            /*
             * Add the certificates, if defined with the CompanyType and absent in
             * the Company specification
             */

            // FIXME: In the following the cloning has to be moved to the portfolio

            /*        if (certificates != null) {
                        ((PublicCompany) clone).setCertificates(certificates.view());
                    }
                    if (specialProperties != null) {
                        ((PublicCompany) clone).specialProperties = new HolderModel<SpecialProperty>(this, "specialProperties");
                    }
             */

            //return clone;
        }

        /** Extra codes to be added to the president's indicator in the Game Status window.
         * Normally nothing (see 1856 CGR for an exception). */
        public string GetExtraShareMarks()
        {
            return "";
        }

        /** Does the company has a route?
         * Currently this is a stub that always returns true.
         */
        public bool HasRoute
        {
            get
            {
                return true;
            }
        }

        // Owner method
        public PortfolioModel PortfolioModel
        {
            get
            {
                return portfolio;
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

        public void SetClosed()
        {
            closed.Set(true);

            IPortfolioOwner shareDestination;
            // If applicable, prepare for a restart
            if (canBeRestarted)
            {
                if (certsAreInitiallyAvailable)
                {
                    shareDestination = GetRoot.Bank.Ipo;
                }
                else
                {
                    shareDestination = GetRoot.Bank.Unavailable;
                }
                Reinitialize();
            }
            else
            {
                shareDestination = GetRoot.Bank.ScrapHeap;
                inGameState.Set(false);
            }

            // Dispose of the certificates
            foreach (PublicCertificate cert in certificates)
            {
                if (cert.Owner != shareDestination)
                {
                    cert.MoveTo(shareDestination);
                }
            }

            // Any trains go to the pool (from the 1856 rules)
            portfolio.TrainsModel.Portfolio.MoveAll(GetRoot.Bank.Pool);

            // Any cash goes to the bank (from the 1856 rules)
            int cash = treasury.Value;
            if (cash > 0)
            {
                treasury.SetSuppressZero(true);
                Currency.ToBank(this, cash);
            }

            lastRevenue.SetSuppressZero(true);
            SetLastRevenue(0);

            // move all laid tokens to free tokens again
            Portfolio.MoveAll(baseTokens.GetLaidTokens(), this);

            // close company on the stock market
            GetRoot.StockMarket.Close(this);

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
            return portfolio.GetPersistentSpecialProperties();
        }

        // MoneyOwner interface
        public Purse Purse
        {
            get
            {
                return treasury.Purse;
            }
        }

        public int Cash
        {
            get
            {
                return Purse.Value();
            }
        }

        // Comparable interface
        public int CompareTo(PublicCompany other)
        {
            if (other == null) return 1;
            return this.Id.CompareTo(other.Id);
        }

        public int CompareTo(object obj)
        {
            if (obj == null) return 1;
            if (!(obj is IRailsItem)) throw new ArgumentException();
            return Id.CompareTo(((IRailsItem)obj).Id);
        }


        public string RelatedNationalCompany
        {
            get
            {
                return relatedPublicCompany;
            }
            set
            {
                relatedPublicCompany = value;
            }
        }


        public bool IsRelatedToNational(string nationalInFounding)
        {
            if (this.RelatedNationalCompany.Equals(nationalInFounding))
            {
                return true;
            }
            return false;
        }

        /**
         * @return the foundingStartCompany
         */
        public string FoundingStartCompany
        {
            get
            {
                return foundingStartCompany;
            }
        }

        /**
         * @param foundingStartCompany the foundingStartCompany to set
         */
        public void SetStartingMinor(string foundingCompany)
        {
            this.foundingStartCompany = foundingCompany;
        }
    }
}
