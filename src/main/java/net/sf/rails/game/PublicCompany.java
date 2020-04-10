package net.sf.rails.game;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.*;
import net.sf.rails.game.special.SellBonusToken;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.*;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.SetDividend;

import java.awt.*;
import java.util.List;
import java.util.*;


/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies encompass all 18xx company-like entities that lay
 * tracks and run trains. <p> Ownership of companies will always be performed by
 * holding certificates. Some minor company types may have only one certificate,
 * but this will still be the form in which ownership is expressed. <p> Company
 * shares may or may not have a price on the stock market.
 */
public class PublicCompany extends RailsAbstractItem implements Company, RailsMoneyOwner, PortfolioOwner, Comparable<PublicCompany> {

    private static final Logger log = LoggerFactory.getLogger(PublicCompany.class);

    public static final int CAPITALISE_FULL = 0;

    public static final int CAPITALISE_INCREMENTAL = 1;

    public static final int CAPITALISE_WHEN_BOUGHT = 2;

    protected static final int DEFAULT_SHARE_UNIT = 10;

    protected static int numberOfPublicCompanies = 0;

    // Home base & price token lay times
    protected static final int WHEN_STARTED = 0;
    protected static final int WHEN_FLOATED = 1;
    protected static final int START_OF_FIRST_OR = 2; // Only applies to home base tokens

    // Base token lay cost calculation methods
    public static final String BASE_COST_SEQUENCE = "sequence";
    public static final String BASE_COST_DISTANCE = "distance";

    protected static final String[] tokenLayTimeNames = new String[]{"whenStarted", "whenFloated", "firstOR"};

    protected int homeBaseTokensLayTime = START_OF_FIRST_OR;


    /**
     * Foreground (i.e. text) colour of the company tokens (if pictures are not
     * used)
     */
    protected Color fgColour;

    /**
     * Hexadecimal representation (RRGGBB) of the foreground colour.
     */
    protected String fgHexColour = "FFFFFF";

    /**
     * Background colour of the company tokens
     */
    protected Color bgColour;

    /**
     * Hexadecimal representation (RRGGBB) of the background colour.
     */
    protected String bgHexColour = "000000";

    /**
     * Home hex & city *
     * Two home hexes is supported, but only if:<br>
     * 1. The locations are fixed (i.e. configured by XML), and<br>
     * 2. Any station (city) numbers are equal for the two home stations.
     * There is no provision yet for two home hexes having different tile station numbers.
     */
    protected String homeHexNames = null;
    protected List<MapHex> homeHexes = null;
    protected int homeCityNumber = 1;

    /**
     * Destination hex *
     */
    protected String destinationHexName = null;
    protected MapHex destinationHex = null;
    protected final BooleanState hasReachedDestination = new BooleanState(this, "hasReachedDestinations");

    /**
     * Sequence number in the array of public companies - may not be useful
     */
    protected int publicNumber = -1; // For internal use

    protected int numberOfBaseTokens = 0;

    protected int baseTokensBuyCost = 0;
    /**
     * An array of base token laying costs, per successive token
     */
    protected List<Integer> baseTokenLayCost;
    protected String baseTokenLayCostMethod = "sequential";

    protected final BaseTokensModel baseTokens = BaseTokensModel.create(this, "baseTokens"); // Create after cloning ?
    protected final PortfolioModel portfolio = PortfolioModel.create(this);


    /**
     * Initial (par) share price, represented by a stock market location object
     */
    protected PriceModel parPrice;

    /**
     * Current share price, represented by a stock market location object
     */
    protected PriceModel currentPrice;

    /**
     * Company treasury, holding cash
     */
    protected final PurseMoneyModel treasury =
            PurseMoneyModel.create(this, "treasury", false);

    /**
     * PresidentModel
     */
    protected final PresidentModel presidentModel = PresidentModel.create(this);

    /**
     * Has the company started?
     */
    protected final BooleanState hasStarted = new BooleanState(this, "hasStarted");

    /**
     * Total bonus tokens amount
     */
    protected final BonusModel bonusValue = BonusModel.create(this, "bonusValue");

    /**
     * Acquires Bonus objects
     */
    protected final ArrayListState<Bonus> bonuses = new ArrayListState(this, "bonuses");

    /**
     * Most recent revenue earned.
     */
    protected final CountingMoneyModel lastRevenue = CountingMoneyModel.create(this, "lastRevenue", false);

    public final CountingMoneyModel directIncomeRevenue = CountingMoneyModel.create(this, "directIncome", false);

    /**
     * Most recent Direct Company Treasury income earned.
     */
    protected final CountingMoneyModel lastDirectIncome = CountingMoneyModel.create(this, "lastDirectIncome", false);

    /**
     * Most recent payout decision.
     */
    protected final StringState lastRevenueAllocation = StringState.create(this, "lastRevenueAllocation");

    /**
     * Is the company operational ("has it floated")?
     */
    protected final BooleanState hasFloated = new BooleanState(this, "hasFloated");

    /**
     * Has the company already operated?
     */
    protected final BooleanState hasOperated = new BooleanState(this, "hasOperated");

    /**
     * Are company shares buyable (i.e. before started)?
     */
    protected final BooleanState buyable = new BooleanState(this, "buyable");

    /**
     * In-game state.
     * <p> Will only be set false if the company is closed and cannot ever be reopened.
     * By default it will be set false if a company is closed.
     */
    // TODO: Check if there was some assumption to be null at some place
    protected final BooleanState inGameState = new BooleanState(this, "inGameState", true);

    // TODO: the extra turn model has to be rewritten (it is not fully undo proof)

    /**
     * Stores the number of turns with extraLays
     */
    protected Map<String, IntegerState> turnsWithExtraTileLays = null;

    /**
     * This receives the current value of turnsWithExtraTileLays
     */
    protected GenericState<IntegerState> extraTiles = new GenericState<>(this, "extraTiles");

    /* Spendings in the current operating turn */
    protected final CountingMoneyModel privatesCostThisTurn = CountingMoneyModel.create(this, "privatesCostThisTurn", false);

    protected final StringState tilesLaidThisTurn = StringState.create(this, "tilesLaidThisTurn");

    protected final CountingMoneyModel tilesCostThisTurn = CountingMoneyModel.create(this, "tilesCostThisTurn", false);

    protected final StringState tokensLaidThisTurn = StringState.create(this, "tokenLaidThisTurn");

    protected final CountingMoneyModel tokensCostThisTurn = CountingMoneyModel.create(this, "tokensCostThisTurn", false);

    protected final CountingMoneyModel trainsCostThisTurn = CountingMoneyModel.create(this, "trainsCostThisTurn", false);

    protected boolean canBuyStock = false;

    protected boolean canBuyPrivates = false;

    protected boolean canUseSpecialProperties = false;

    /**
     * Can a company be restarted once it is closed?
     */
    protected boolean canBeRestarted = false;

    /**
     * Minimum price for buying privates, to be multiplied by the original price
     */
    protected float lowerPrivatePriceFactor;

    /**
     * Maximum price for buying privates, to be multiplied by the original price
     */
    protected float upperPrivatePriceFactor;

    protected boolean ipoPaysOut = false;

    protected boolean poolPaysOut = false;

    protected boolean treasuryPaysOut = false;

    protected boolean canHoldOwnShares = false;

    protected int maxPercOfOwnShares = 0;

    protected boolean mayTradeShares = false;

    protected boolean mustHaveOperatedToTradeShares = false;

    protected List<Tag> certificateTags = null;

    /**
     * The certificates of this company (minimum 1)
     */
    protected final ArrayListState<PublicCertificate> certificates = new ArrayListState<>(this, "ownCertificates");
    /**
     * Are the certificates available from the first SR?
     */
    protected boolean certsAreInitiallyAvailable = true;

    /**
     * What percentage of ownership constitutes "one share"
     */
    protected IntegerState shareUnit = IntegerState.create(this, "shareUnit", DEFAULT_SHARE_UNIT);

    /**
     * What number of share units relates to the share price
     * (normally 1, but 2 for 1835 Prussian)
     */
    protected int shareUnitsForSharePrice = 1;

    /**
     * At what percentage sold does the company float
     */
    protected int floatPerc = 0;

    /**
     * Share price movement on floating (1851: up)
     */
    protected boolean sharePriceUpOnFloating = false;

    /**
     * Does the company have a stock price (minors often don't)
     */
    protected boolean hasStockPrice = true;

    /**
     * Does the company have a par price?
     */
    protected boolean hasParPrice = true;

    protected boolean splitAllowed = false;

    /**
     * Is the revenue always split (typical for non-share minors)
     */
    protected boolean splitAlways = false;

    /**
     * Must payout exceed stock price to move token right?
     */
    protected boolean payoutMustExceedPriceToMove = false;

    /**
     * Multiple certificates those that represent more than one nominal share unit (except president share)
     */
    protected boolean hasMultipleCertificates = false;

    /*---- variables needed during initialisation -----*/
    protected String startSpace = null;

    protected int dropPriceToken = WHEN_STARTED;

    protected int capitalisation = CAPITALISE_FULL;

    /**
     * Fixed price (for a 1835-style minor)
     */
    protected int fixedPrice = 0;

    /**
     * Train limit per phase (index)
     */
    protected List<Integer> trainLimit;

    /**
     * Private to close if first train is bought
     */
    protected String privateToCloseOnFirstTrainName = null;

    protected PrivateCompany privateToCloseOnFirstTrain = null;

    /**
     * Must the company own a train
     */
    protected boolean mustOwnATrain = true;

    protected boolean mustTradeTrainsAtFixedPrice = false;

    /**
     * Can the company price token go down to a "Close" square?
     * 1856 CGR cannot.
     */
    protected boolean canClose = true;

    /**
     * Initial train at floating time
     */
    protected String initialTrainType = null;
    protected int initialTrainCost = 0;
    protected boolean initialTrainTradeable = true;

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
    private String longName;
    private String alias;
    private CompanyType type;
    private String infoText = "";
    private String parentInfoText = "";
    private final BooleanState closed = new BooleanState(this, "closed", false);

    /**
     * Relation to a later to be founded National/Regional Major Company
     */
    private String relatedPublicCompany = null;

    private String foundingStartCompany = null;

    /**
     * Used by Configure (via reflection) only
     */
    public PublicCompany(RailsItem parent, String id) {
        this(parent, id, true);
    }

    public PublicCompany(RailsItem parent, String id, boolean hasStockPrice) {
        super(parent, id);
        lastRevenue.setSuppressInitialZero(true);

        /* Spendings in the current operating turn */
        privatesCostThisTurn.setSuppressZero(true);
        tilesCostThisTurn.setSuppressZero(true);
        tokensCostThisTurn.setSuppressZero(true);
        trainsCostThisTurn.setSuppressZero(true);
        trainsCostThisTurn.setDisplayNegative(true);

        // Bonuses
        bonusValue.setBonuses(bonuses);

        this.hasStockPrice = hasStockPrice;

        if (hasStockPrice) {
            parPrice = PriceModel.create(this, "ParPrice", false);
            currentPrice = PriceModel.create(this, "currentPrice", true);
            canSharePriceVary = new BooleanState(this, "canSharePriceVary", true);
        }
    }


    /**
     * To configure all public companies from the &lt;PublicCompany&gt; XML
     * element
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {

        longName = tag.getAttributeAsString("longname", getId());
        infoText = "<html>" + longName;

        alias = tag.getAttributeAsString("alias", alias);

        /* Configure public company features */
        fgHexColour = tag.getAttributeAsString("fgColour", fgHexColour);
        fgColour = Util.parseColour(fgHexColour);

        bgHexColour = tag.getAttributeAsString("bgColour", bgHexColour);
        bgColour = Util.parseColour(bgHexColour);

        floatPerc = tag.getAttributeAsInteger("floatPerc", floatPerc);

        relatedPublicCompany = tag.getAttributeAsString("relatedCompany", relatedPublicCompany);

        foundingStartCompany = tag.getAttributeAsString("foundingCompany", foundingStartCompany);

        startSpace = tag.getAttributeAsString("startspace");
        // Set the default price token drop time.
        // Currently, no exceptions exist, so this value isn't changed anywhere yet.
        // Any (future) games with exceptions to these defaults will require a separate XML attribute.
        // Known games to have exceptions: 1837.
        dropPriceToken = startSpace != null ? WHEN_FLOATED : WHEN_STARTED;

        fixedPrice = tag.getAttributeAsInteger("price", 0);

        numberOfBaseTokens = tag.getAttributeAsInteger("tokens", 1);

        certsAreInitiallyAvailable
                = tag.getAttributeAsBoolean("available", certsAreInitiallyAvailable);

        canBeRestarted = tag.getAttributeAsBoolean("restartable", canBeRestarted);

        Tag shareUnitTag = tag.getChild("ShareUnit");
        if (shareUnitTag != null) {
            shareUnit.set(shareUnitTag.getAttributeAsInteger("percentage", DEFAULT_SHARE_UNIT));
            shareUnitsForSharePrice
                    = shareUnitTag.getAttributeAsInteger("sharePriceUnits", shareUnitsForSharePrice);
        }

        Tag homeBaseTag = tag.getChild("Home");
        if (homeBaseTag != null) {
            homeHexNames = homeBaseTag.getAttributeAsString("hex");
            homeCityNumber = homeBaseTag.getAttributeAsInteger("city", 1);
        }

        Tag destinationTag = tag.getChild("Destination");
        if (destinationTag != null) {
            destinationHexName = destinationTag.getAttributeAsString("hex");
        }

        Tag privateBuyTag = tag.getChild("CanBuyPrivates");
        if (privateBuyTag != null) {
            canBuyPrivates = true;
        }

        Tag canUseSpecTag = tag.getChild("CanUseSpecialProperties");
        if (canUseSpecTag != null) canUseSpecialProperties = true;

        // Extra info text(usually related to extra-share special properties)
        Tag infoTag = tag.getChild("Info");
        if (infoTag != null) {
            String infoKey = infoTag.getAttributeAsString("key");
            String[] infoParms = infoTag.getAttributeAsString("parm", "").split(",");
            infoText += "<br>" + LocalText.getText(infoKey, (Object[]) infoParms);
        }

        // Special properties (as in the 1835 black minors)
        parentInfoText += SpecialProperty.configure(this, tag);

        poolPaysOut = poolPaysOut || tag.getChild("PoolPaysOut") != null;

        ipoPaysOut = ipoPaysOut || tag.getChild("IPOPaysOut") != null;

        Tag floatTag = tag.getChild("Float");
        if (floatTag != null) {
            floatPerc = floatTag.getAttributeAsInteger("percentage", floatPerc);
            String sharePriceAttr = floatTag.getAttributeAsString("price");
            if (Util.hasValue(sharePriceAttr)) {
                sharePriceUpOnFloating = sharePriceAttr.equalsIgnoreCase("up");
            }
        }

        Tag priceTag = tag.getChild("StockPrice");
        if (priceTag != null) {
            hasStockPrice = priceTag.getAttributeAsBoolean("market", true);
            hasParPrice = priceTag.getAttributeAsBoolean("par", hasStockPrice);
        }

        Tag payoutTag = tag.getChild("Payout");
        if (payoutTag != null) {
            String split = payoutTag.getAttributeAsString("split", "no");
            splitAlways = split.equalsIgnoreCase("always");
            splitAllowed = split.equalsIgnoreCase("allowed");

            payoutMustExceedPriceToMove =
                    payoutTag.getAttributeAsBoolean("mustExceedPriceToMove",
                            false);
        }

        Tag ownSharesTag = tag.getChild("TreasuryCanHoldOwnShares");
        if (ownSharesTag != null) {
            canHoldOwnShares = true;
            treasuryPaysOut = true;

            maxPercOfOwnShares =
                    ownSharesTag.getAttributeAsInteger("maxPerc",
                            maxPercOfOwnShares);
        }

        Tag trainsTag = tag.getChild("Trains");
        if (trainsTag != null) {
            trainLimit = trainsTag.getAttributeAsIntegerList("limit");
            mustOwnATrain =
                    trainsTag.getAttributeAsBoolean("mandatory", mustOwnATrain);
        }

        Tag initialTrainTag = tag.getChild("InitialTrain");
        if (initialTrainTag != null) {
            initialTrainType = initialTrainTag.getAttributeAsString("type");
            initialTrainCost = initialTrainTag.getAttributeAsInteger("cost",
                    initialTrainCost);
            initialTrainTradeable = initialTrainTag.getAttributeAsBoolean("tradeable",
                    initialTrainTradeable);
        }

        Tag firstTrainTag = tag.getChild("FirstTrainCloses");
        if (firstTrainTag != null) {
            String typeName =
                    firstTrainTag.getAttributeAsString("type", "Private");
            if (typeName.equalsIgnoreCase("Private")) {
                privateToCloseOnFirstTrainName =
                        firstTrainTag.getAttributeAsString("name");
            } else {
                throw new ConfigurationException(
                        "Only Privates can be closed on first train buy");
            }
        }

        Tag capitalisationTag = tag.getChild("Capitalisation");
        if (capitalisationTag != null) {
            String capType =
                    capitalisationTag.getAttributeAsString("type", "full");
            if (capType.equalsIgnoreCase("full")) {
                setCapitalisation(CAPITALISE_FULL);
            } else if (capType.equalsIgnoreCase("incremental")) {
                setCapitalisation(CAPITALISE_INCREMENTAL);
            } else if (capType.equalsIgnoreCase("whenBought")) {
                setCapitalisation(CAPITALISE_WHEN_BOUGHT);
            } else {
                throw new ConfigurationException(
                        "Invalid capitalisation type: " + capType);
            }
        }


        // TODO: Check if this still works correctly
        // The certificate init was moved to the finishConfig phase
        // as PublicCompany is configured twice
        List<Tag> certTags = tag.getChildren("Certificate");
        if (certTags != null) certificateTags = certTags;

        // BaseToken
        Tag baseTokenTag = tag.getChild("BaseTokens");
        if (baseTokenTag != null) {

            // Cost of laying a token
            Tag layCostTag = baseTokenTag.getChild("LayCost");
            if (layCostTag != null) {
                baseTokenLayCostMethod =
                        layCostTag.getAttributeAsString("method",
                                baseTokenLayCostMethod);
                if (baseTokenLayCostMethod.equalsIgnoreCase(BASE_COST_SEQUENCE)) {
                    baseTokenLayCostMethod = BASE_COST_SEQUENCE;
                } else if (baseTokenLayCostMethod.equalsIgnoreCase(BASE_COST_DISTANCE)) {
                    baseTokenLayCostMethod = BASE_COST_DISTANCE;
                } else {
                    throw new ConfigurationException(
                            "Invalid base token lay cost calculation method: "
                                    + baseTokenLayCostMethod);
                }

                baseTokenLayCost =
                        layCostTag.getAttributeAsIntegerList("cost");
            }

            /* Cost of buying a token (mutually exclusive with laying cost) */
            Tag buyCostTag = baseTokenTag.getChild("BuyCost");
            if (buyCostTag != null) {
                baseTokensBuyCost =
                        buyCostTag.getAttributeAsInteger("initialTokenCost", 0);
            }

            Tag tokenLayTimeTag = baseTokenTag.getChild("HomeBase");
            if (tokenLayTimeTag != null) {
                // When is the home base laid?
                // Note: if not before, home tokens are in any case laid
                // at the start of the first OR
                String layTimeString =
                        tokenLayTimeTag.getAttributeAsString("lay");
                if (Util.hasValue(layTimeString)) {
                    for (int i = 0; i < tokenLayTimeNames.length; i++) {
                        if (tokenLayTimeNames[i].equalsIgnoreCase(layTimeString)) {
                            homeBaseTokensLayTime = i;
                            break;
                        }
                    }
                }
            }
        }

        Tag sellSharesTag = tag.getChild("TradeShares");
        if (sellSharesTag != null) {
            mayTradeShares = true;
            mustHaveOperatedToTradeShares =
                    sellSharesTag.getAttributeAsBoolean("mustHaveOperated",
                            mustHaveOperatedToTradeShares);
        }

        Tag loansTag = tag.getChild("Loans");
        if (loansTag != null) {
            maxNumberOfLoans = loansTag.getAttributeAsInteger("number", -1);
            // Note: -1 means undefined, to be handled in the code
            // (for instance: 1856).
            valuePerLoan = loansTag.getAttributeAsInteger("value", 0);
            loanInterestPct = loansTag.getAttributeAsInteger("interest", 0);
            maxLoansPerRound = loansTag.getAttributeAsInteger("perRound", -1);
        }

        Tag optionsTag = tag.getChild("Options");
        if (optionsTag != null) {
            mustTradeTrainsAtFixedPrice = optionsTag.getAttributeAsBoolean
                    ("mustTradeTrainsAtFixedPrice", mustTradeTrainsAtFixedPrice);
            canClose = optionsTag.getAttributeAsBoolean("canClose", canClose);
        }

    }


    public void setIndex(int index) {
        publicNumber = index;
    }

    /**
     * Final initialisation, after all XML has been processed.
     */
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {

        if (maxNumberOfLoans != 0) {
            currentNumberOfLoans = IntegerState.create(this, "currentNumberOfLoans");
            currentLoanValue = CountingMoneyModel.create(this, "currentLoanValue", false);
            currentLoanValue.setSuppressZero(true);
        }

        if (hasStockPrice && Util.hasValue(startSpace)) {
            parPrice.setPrice(getRoot().getStockMarket().getStockSpace(
                    startSpace));
            if (parPrice.getPrice() == null)
                throw new ConfigurationException("Invalid start space "
                        + startSpace + " for company "
                        + getId());
            currentPrice.setPrice(parPrice.getPrice());

        }

        int certIndex = 0;
        if (certificateTags != null) {
            int shareTotal = 0;
            boolean gotPresident = false;
            PublicCertificate certificate;
            // Throw away
            // the per-type
            // specification

            // TODO: Move this to PublicCertificate class, as it belongs there
            for (Tag certificateTag : certificateTags) {
                int shares = certificateTag.getAttributeAsInteger("shares", 1);

                boolean president =
                        "President".equals(certificateTag.getAttributeAsString(
                                "type", ""));
                int number = certificateTag.getAttributeAsInteger("number", 1);

                boolean certIsInitiallyAvailable
                        = certificateTag.getAttributeAsBoolean("available",
                        certsAreInitiallyAvailable);

                float certificateCount = certificateTag.getAttributeAsFloat("certificateCount", 1.0f);

                if (president) {
                    if (number > 1 || gotPresident)
                        throw new ConfigurationException(
                                "Company type "
                                        + getId()
                                        + " cannot have multiple President shares");
                    gotPresident = true;
                }

                for (int k = 0; k < number; k++) {
                    certificate = new PublicCertificate(this, "cert_" + certIndex, shares, president,
                            certIsInitiallyAvailable, certificateCount, certIndex++);
                    certificates.add(certificate);
                    shareTotal += shares * shareUnit.value();
                }
            }
            if (shareTotal != 100)
                throw new ConfigurationException("Company type " + getId()
                        + " total shares is not 100%");
        }

        nameCertificates();

        // Give each certificate an unique Id
        PublicCertificate cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(getId(), i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable() && this.certsAreInitiallyAvailable);
        }

        Set<BaseToken> newTokens = Sets.newHashSet();
        for (int i = 0; i < numberOfBaseTokens; i++) {
            BaseToken token = BaseToken.create(this);
            newTokens.add(token);
        }
        baseTokens.initTokens(newTokens);

        if (homeHexNames != null) {
            homeHexes = new ArrayList<MapHex>(2);
            MapHex homeHex;
            for (String homeHexName : homeHexNames.split(",")) {
                homeHex = getRoot().getMapManager().getHex(homeHexName);
                if (homeHex == null) {
                    throw new ConfigurationException("Invalid home hex "
                            + homeHexName
                            + " for company " + getId());
                }
                homeHexes.add(homeHex);
                infoText += "<br>Home: " + homeHex.toText();
            }
        }

        if (destinationHexName != null) {
            destinationHex = getRoot().getMapManager().getHex(destinationHexName);
            if (destinationHex == null) {
                throw new ConfigurationException("Invalid destination hex "
                        + destinationHexName
                        + " for company " + getId());
            }
            infoText += "<br>Destination: " + destinationHex.toText();
        }

        if (Util.hasValue(privateToCloseOnFirstTrainName)) {
            privateToCloseOnFirstTrain =
                    getRoot().getCompanyManager().getPrivateCompany(
                            privateToCloseOnFirstTrainName);
        }

        if (trainLimit != null) {
            infoText += "<br>" + LocalText.getText("CompInfoMaxTrains",
                    Util.joinWithDelimiter(trainLimit, ", "));

        }

        infoText += parentInfoText;
        parentInfoText = "";

        // Can companies acquire special rightsModel (such as in 1830 Coalfields)?
        // TODO: Can this be simplified?
        if (portfolio.hasSpecialProperties()) {
            for (SpecialProperty sp : portfolio.getPersistentSpecialProperties()) {
                if (sp instanceof SpecialRight) {
                    getRoot().getGameManager().setGuiParameter(GuiDef.Parm.HAS_ANY_RIGHTS, true);
                    // Initialize rightsModel here to prevent overhead if not used,
                    // but if rightsModel are used, the GUI needs it from the start.
                    if (rightsModel == null) {
                        rightsModel = RightsModel.create(this, "rightsModel");
                    }
                    // TODO: This is only a workaround for the missing finishConfiguration of special properties (SFY)
                    sp.finishConfiguration(root);
                }
            }
        }

        // finish Configuration of portfolio
        portfolio.finishConfiguration();

        // set multipleCertificates
        for (PublicCertificate c : certificates) {
            if (!c.isPresidentShare() && c.getShares() != 1) {
                hasMultipleCertificates = true;
            }
        }
    }

    /**
     * Used in finalizing configuration
     */
    public void addExtraTileLayTurnsInfo(String colour, int turns) {
        if (turnsWithExtraTileLays == null) {
            turnsWithExtraTileLays = new HashMap<String, IntegerState>();
        }
        IntegerState tileLays = IntegerState.create
                (this, "" + colour + "_ExtraTileTurns", turns);
        turnsWithExtraTileLays.put(colour, tileLays);
    }

    /**
     * Reset turn objects
     */
    public void initTurn() {

        if (!hasLaidHomeBaseTokens()) layHomeBaseTokens();

        privatesCostThisTurn.set(0);
        tilesLaidThisTurn.set("");
        tilesCostThisTurn.set(0);
        tokensLaidThisTurn.set("");
        tokensCostThisTurn.set(0);
        trainsCostThisTurn.set(0);
    }

    /**
     * Return the company token background colour.
     *
     * @return Color object
     */
    public Color getBgColour() {
        return bgColour;
    }

    /**
     * Return the company token background colour.
     *
     * @return Hexadecimal string RRGGBB.
     */
    public String getHexBgColour() {
        return bgHexColour;
    }

    /**
     * Return the company token foreground colour.
     *
     * @return Color object.
     */
    public Color getFgColour() {
        return fgColour;
    }

    /**
     * Return the company token foreground colour.
     *
     * @return Hexadecimal string RRGGBB.
     */
    public String getHexFgColour() {
        return fgHexColour;
    }

    /**
     * Return the company's Home hexes (usually one).
     *
     * @return Returns the homeHex.
     */
    public List<MapHex> getHomeHexes() {
        return homeHexes;
    }

    /**
     * Set a non-fixed company home hex.
     * Only covers setting <i>one</i> home hex.
     * Having <i>two</i> home hexes is currently only supported if the locations are preconfigured.
     *
     * @param homeHex The homeHex to set.
     */
    public void setHomeHex(MapHex homeHex) {
        homeHexes = new ArrayList<MapHex>(1);
        homeHexes.add(homeHex);
    }

    /**
     * @return Returns the homeStation.
     */
    public int getHomeCityNumber() {
        return homeCityNumber;
    }

    /**
     * @param number The homeStation to set.
     */
    public void setHomeCityNumber(int number) {
        this.homeCityNumber = number;
    }

    /**
     * @return Returns the destinationHex.
     */
    public MapHex getDestinationHex() {
        return destinationHex;
    }

    public boolean hasDestination() {
        return destinationHex != null;
    }

    public boolean hasReachedDestination() {
        return hasReachedDestination != null &&
                hasReachedDestination.value();
    }

    public void setReachedDestination(boolean value) {
        hasReachedDestination.set(value);
    }

    /**
     * @return
     */
    public boolean canBuyStock() {
        return canBuyStock;
    }

    public boolean mayTradeShares() {
        return mayTradeShares;
    }

    /**
     * Stub that allows exclusions such as that 1856 CGR may not buy a 4
     */
    public boolean mayBuyTrainType(Train train) {
        return true;
    }

    public boolean mustHaveOperatedToTradeShares() {
        return mustHaveOperatedToTradeShares;
    }

    public void start(StockSpace startSpace) {

        hasStarted.set(true);
        if (hasStockPrice) buyable.set(true);

        // In case of a restart: undo closing
        if (closed.value()) closed.set(false);

        if (startSpace != null) {
            setParSpace(startSpace);
            setCurrentSpace(startSpace);

            // Drop the current price token, if allowed at this point
            if (dropPriceToken == WHEN_STARTED) {
                getRoot().getStockMarket().start(this, startSpace);
            }
        }


        if (homeBaseTokensLayTime == WHEN_STARTED) {
            layHomeBaseTokens();
        }
    }

    public void start(int price) {
        StockSpace startSpace = getRoot().getStockMarket().getStartSpace(price);
        if (startSpace == null) {
            log.error("Invalid start price " + Bank.format(this, price)); // TODO: Do this nicer
        } else {
            start(startSpace);
        }
    }

    /**
     * Start a company.
     */
    public void start() {
        start(getStartSpace());
    }

    public void transferAssetsFrom(PublicCompany otherCompany) {

        if (otherCompany.getCash() > 0) {
            Currency.wireAll(otherCompany, this);
        }
        portfolio.transferAssetsFrom(otherCompany.getPortfolioModel());
    }

    /**
     * @return Returns true is the company has started.
     */
    public boolean hasStarted() {
        return hasStarted.value();
    }

    /**
     * Make company shares buyable. Only useful where shares become
     * buyable before the company has started (e.g. 1835 Prussian).
     */
    public void setBuyable(boolean buyable) {
        this.buyable.set(buyable);
    }

    public boolean isBuyable() {
        return buyable.value();
    }

    /**
     * Float the company, put its initial cash in the treasury.
     */
    public void setFloated() {

        hasFloated.set(true);
        // In case of a restart
        if (hasOperated.value()) hasOperated.set(false);

        // Remove the "unfloated" indicator in GameStatus
        // FIXME: Is this still required?
        // getPresident().getPortfolioModel().getShareModel(this).update();

        if (sharePriceUpOnFloating) {
            getRoot().getStockMarket().moveUp(this);
        }

        // Drop the current price token, if allowed at this point
        if (dropPriceToken == WHEN_FLOATED) {
            getRoot().getStockMarket().start(this, getCurrentSpace());
        }

        if (homeBaseTokensLayTime == WHEN_FLOATED) {
            layHomeBaseTokens();
        }

        if (initialTrainType != null) {
            TrainManager trainManager = getRoot().getTrainManager();
            TrainCertificateType type = trainManager.getCertTypeByName(initialTrainType);
            Train train = getRoot().getBank().getIpo().getPortfolioModel().getTrainOfType(type);
            buyTrain(train, initialTrainCost);
            train.setTradeable(initialTrainTradeable);
            trainManager.checkTrainAvailability(train, getRoot().getBank().getIpo());
        }
    }

    /**
     * Has the company already floated?
     *
     * @return true if the company has floated.
     */
    public boolean hasFloated() {
        return hasFloated.value();
    }

    public BooleanState getFloatedModel() {
        return hasFloated;
    }

    /**
     * Has the company already operated?
     *
     * @return true if the company has operated.
     */
    public boolean hasOperated() {
        return hasOperated.value();
    }

    public void setOperated() {
        hasOperated.set(true);
    }

    /**
     * Reinitialize a company, i.e. close it and make the shares available for a new company start.
     * IMplemented rules are now as in 18EU.
     * TODO Will see later if this is generic enough.
     */
    protected void reinitialise() {
        hasStarted.set(false);
        hasFloated.set(false);
        hasOperated.set(false);
        if (parPrice != null && fixedPrice <= 0) parPrice.setPrice(null);
        if (currentPrice != null) currentPrice.setPrice(null);
    }

    public BooleanState getInGameModel() {
        return inGameState;
    }

    public BooleanState getIsClosedModel() {
        return closed;
    }

    /**
     * Set the company par price. <p> <i>Note: this method should <b>not</b> be
     * used to start a company!</i> Use <code><b>start()</b></code> in
     * stead.
     *
     * @param space
     */
    public void setParSpace(StockSpace space) {
        if (hasStockPrice) {
            if (space != null) {
                parPrice.setPrice(space);
            }
        }
    }

    /**
     * Get the company par (initial) price.
     *
     * @return StockSpace object, which defines the company start position on
     * the stock chart.
     */
    public StockSpace getStartSpace() {
        if (hasParPrice) {
            return parPrice != null ? parPrice.getPrice() : null;
        } else {
            return currentPrice != null ? currentPrice.getPrice() : null;
        }
    }

    public int getIPOPrice() {
        if (hasParPrice) {
            if (getStartSpace() != null) {
                return getStartSpace().getPrice();
            } else {
                return 0;
            }
        } else {
            return getMarketPrice();
        }
    }

    public int getMarketPrice() {
        if (getCurrentSpace() != null) {
            return getCurrentSpace().getPrice();
        } else {
            return 0;
        }
    }

    /**
     * Return the price per share at game end.
     * Normally, it is equal to the market price,
     * but in some games (e.g. 1856) deductions may apply.
     *
     * @return
     */
    public int getGameEndPrice() {
        return getMarketPrice() / getShareUnitsForSharePrice();
    }

    /**
     * Set a new company price.
     *
     * @param price The StockSpace object that defines the new location on the
     *              stock market.
     */
    public void setCurrentSpace(StockSpace price) {
        if (price != null && price != getCurrentSpace()) {
            currentPrice.setPrice(price);
        }
    }

    public PriceModel getCurrentPriceModel() {
        return currentPrice;
    }

    public PriceModel getParPriceModel() {
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
    public StockSpace getCurrentSpace() {
        return currentPrice != null ? currentPrice.getPrice() : null;
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

    public PurseMoneyModel getPurseMoneyModel() {
        return treasury;
    }

    public String getFormattedCash() {
        return treasury.toText();
    }

    public Model getText() {
        return treasury;
    }

    /**
     * @return
     */
    public int getPublicNumber() {
        return publicNumber;
    }

    /**
     * Get a list of this company's certificates.
     *
     * @return ArrayList containing the certificates (item 0 is the President's
     * share).
     */
    public List<PublicCertificate> getCertificates() {
        return certificates.view();
    }

    /**
     * Backlink the certificates to this company,
     * and give each one a type getId().
     */
    public void nameCertificates() {
        for (PublicCertificate cert : certificates.view()) {
            cert.setCompany(this);
        }
    }

    /**
     * Get the percentage of shares that must be sold to float the company.
     *
     * @return The float percentage.
     */
    public int getFloatPercentage() {
        return floatPerc;
    }

    /**
     * Determine sold percentage for floating purposes
     */
    public int getSoldPercentage() {
        int soldPercentage = 0;
        for (PublicCertificate cert : certificates.view()) {
            if (certCountsAsSold(cert)) {
                soldPercentage += cert.getShare();
            }
        }
        return soldPercentage;
    }

    /**
     * Can be subclassed for games with special rules
     */
    protected boolean certCountsAsSold(PublicCertificate cert) {
        Owner owner = cert.getOwner();
        return owner instanceof Player || owner == getRoot().getBank().getPool();
    }

    /**
     * Get the company President.
     */
    // FIXME: This has to be redesigned
    // Relying on the ordering is not a good thing
    public Player getPresident() {
        if (hasStarted()) {
            Owner owner = certificates.get(0).getOwner();
            if (owner instanceof Player) return (Player) owner;
        }
        return null;
    }

    public PresidentModel getPresidentModel() {
        return presidentModel;
    }

    public PublicCertificate getPresidentsShare() {
        return certificates.get(0);
    }

    /**
     * Store the last revenue earned by this company.
     *
     * @param i The last revenue amount.
     */
    public void setLastRevenue(int i) {
        lastRevenue.set(i);
    }

    /**
     * Get the last revenue earned by this company.
     *
     * @return The last revenue amount.
     */
    public int getLastRevenue() {
        return lastRevenue.value();
    }

    public Model getLastRevenueModel() {
        return lastRevenue;
    }

    /**
     * Last revenue allocation (payout, split, withhold)
     */
    public void setLastRevenueAllocation(int allocation) {
        if (allocation >= 0 && allocation < SetDividend.NUM_OPTIONS) {
            lastRevenueAllocation.set(LocalText.getText(SetDividend.getAllocationNameKey(allocation)));
        } else {
            lastRevenueAllocation.set("");
        }
    }

    public String getlastRevenueAllocationText() {
        return lastRevenueAllocation.value();
    }

    public StringState getLastRevenueAllocationModel() {
        return lastRevenueAllocation;
    }

    /**
     * Determine if the price token must be moved after a dividend payout.
     *
     * @param amount
     */
    public void payout(int amount) {

        if (amount == 0) return;

        // Move the token
        if (hasStockPrice
                && (!payoutMustExceedPriceToMove
                || amount >= currentPrice.getPrice().getPrice())) {
            getRoot().getStockMarket().payOut(this);
        }

    }

    public boolean paysOutToTreasury(PublicCertificate cert) {

        Owner owner = cert.getOwner();
        if (owner == getRoot().getBank().getIpo() && ipoPaysOut
                || owner == getRoot().getBank().getPool() && poolPaysOut) {
            return true;
        }
        return false;
    }

    /**
     * Determine if the price token must be moved after a withheld dividend.
     *
     * @param amount The revenue amount.
     */
    public void withhold(int amount) {
        if (hasStockPrice) getRoot().getStockMarket().withhold(this);
    }

    /**
     * Is the company completely sold out? This method should return true only
     * if the share price should move up at the end of a stock round. Since 1851
     * (jan 2008) interpreted as: no share is owned either by the Bank or by the
     * company's own Treasury.
     *
     * @return true if the share price can move up.
     */
    public boolean isSoldOut() {
        Owner owner;

        for (PublicCertificate cert : certificates.view()) {
            owner = cert.getOwner();
            if (owner instanceof BankPortfolio || owner == cert.getCompany()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return
     */
    public boolean canBuyPrivates() {
        return canBuyPrivates;
    }

    public boolean canUseSpecialProperties() {
        return canUseSpecialProperties;
    }

    /**
     * Get the unit of share.
     *
     * @return The percentage of ownership that is called "one share".
     */
    public int getShareUnit() {
        return shareUnit.value();
    }

    public int getShareUnitsForSharePrice() {
        return shareUnitsForSharePrice;
    }

    /**
     * @reeturn true if company has Multiple certificates, representing more than one nominal share unit (except president share)
     */
    public boolean hasMultipleCertificates() {
        return hasMultipleCertificates;
    }

    @Override
    public String toString() {
        return getId();
    }

    public boolean hasStockPrice() {
        return hasStockPrice;
    }

    public boolean hasParPrice() {
        return hasParPrice;
    }

    public boolean canSharePriceVary() {
        return canSharePriceVary.value();
    }

    public int getFixedPrice() {
        return fixedPrice;
    }

    public int getBaseTokensBuyCost() {
        return baseTokensBuyCost;
    }

    public int sharesOwnedByPlayers() {
        int shares = 0;
        for (PublicCertificate cert : certificates.view()) {
            if (cert.getOwner() instanceof Player) {
                shares += cert.getShares();
            }
        }
        return shares;
    }

    public boolean canHoldOwnShares() {
        return canHoldOwnShares;
    }

    /**
     * @return Returns the splitAllowed.
     */
    public boolean isSplitAllowed() {
        return splitAllowed;
    }

    /**
     * @return Returns the splitAlways.
     */
    public boolean isSplitAlways() {
        return splitAlways;
    }

    /**
     * Check if the presidency has changed for a <b>buying</b> player.
     *
     * @param buyer Player who has just bought a certificate.
     */
    public void checkPresidencyOnBuy(Player buyer) {

        if (!hasStarted() || buyer == getPresident() || certificates.size() < 2)
            return;
        Player pres = getPresident();
        int presShare = pres.getPortfolioModel().getShare(this);
        int buyerShare = buyer.getPortfolioModel().getShare(this);
        if (buyerShare > presShare) {
            pres.getPortfolioModel().swapPresidentCertificate(this,
                    buyer.getPortfolioModel(), 0);
            ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                    buyer.getId(),
                    getId()));
        }
    }

    public void checkPresidency() {

        // check if there is a new potential president
        int presidentShareNumber = getPresident().getPortfolioModel().getShareNumber(this) + 1;
        Player nextPotentialPresident = findNextPotentialPresident(presidentShareNumber);

        // no change, return
        if (nextPotentialPresident == null) {
            return;
        }

        // otherwise Hand presidency to the player with the highest share
        getPresident().getPortfolioModel().swapPresidentCertificate(this, nextPotentialPresident.getPortfolioModel(), 2);
        ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                nextPotentialPresident.getId(),
                getId()));
    }

    public Player findPlayerToDump() {
        return findNextPotentialPresident(getPresidentsShare().getShares());
    }

    public Player findNextPotentialPresident(int minimumShareNumber) {

        int requiredShareNumber = minimumShareNumber;
        Player potentialDirector = null;

        for (Player nextPlayer : getRoot().getPlayerManager().getNextPlayersAfter(getPresident(), false, false)) {
            int nextPlayerShareNumber = nextPlayer.getPortfolioModel().getShareNumber(this);
            if (nextPlayerShareNumber >= requiredShareNumber) {
                potentialDirector = nextPlayer;
                requiredShareNumber = nextPlayerShareNumber + 1;
            }
        }
        return potentialDirector;
    }

    /**
     * @return Returns the capitalisation.
     */
    public int getCapitalisation() {
        return capitalisation;
    }

    /**
     * @param capitalisation The capitalisation to set.
     */
    public void setCapitalisation(int capitalisation) {
        log.debug("Capitalisation=" + capitalisation);
        this.capitalisation = capitalisation;
    }

    public int getNumberOfShares() {
        return 100 / shareUnit.value();
    }

    /**
     * Get the current maximum number of trains got a given limit index.
     *
     * @parm index The index of the train limit step as defined for the current phase. Values start at 0.
     * <p>N.B. the new style limit steps per phase start at 1,
     * so one must be subtracted before calling this method.
     */
    protected int getTrainLimit(int index) {
        return trainLimit.get(Math.min(index, trainLimit.size() - 1));
    }

    public int getCurrentTrainLimit() {
        return getTrainLimit(getRoot().getGameManager().getCurrentPhase().getTrainLimitIndex());
    }

    public int getNumberOfTrains() {
        return portfolio.getNumberOfTrains();
    }

    public boolean canRunTrains() {
        return portfolio.getNumberOfTrains() > 0;
    }

    /**
     * Must be called in stead of Portfolio.buyTrain if side-effects can occur.
     */
    public void buyTrain(Train train, int price) {

        // check first if it is bought from another company
        if (train.getOwner() instanceof PublicCompany) {
            PublicCompany previousOwner = (PublicCompany) train.getOwner();
            //  adjust the money spent on trains field
            previousOwner.getTrainsSpentThisTurnModel().change(-price);
            // pay the money to the other company
            Currency.wire(this, price, previousOwner);
        } else { // TODO: make this a serious test, no assumption
            // else it is from the bank
            Currency.toBank(this, price);
        }

        // increase own train costs
        trainsCostThisTurn.change(price);
        // move the train to here
        portfolio.getTrainsModel().getPortfolio().add(train);
        // check if a private has to be closed on first train buy
        if (privateToCloseOnFirstTrain != null
                && !privateToCloseOnFirstTrain.isClosed()) {
            privateToCloseOnFirstTrain.setClosed();
        }
    }

    public CountingMoneyModel getTrainsSpentThisTurnModel() {
        return trainsCostThisTurn;
    }

    public void buyPrivate(PrivateCompany privateCompany, Owner from,
                           int price) {

        if (from != getRoot().getBank().getIpo()) {
            // The initial buy is reported from StartRound. This message should also
            // move to elsewhere.
            ReportBuffer.add(this, LocalText.getText("BuysPrivateFromFor",
                    getId(),
                    privateCompany.getId(),
                    from.getId(),
                    Bank.format(this, price)));
        }

        // Move the private certificate
        portfolio.getPrivatesOwnedModel().getPortfolio().add(privateCompany);

        // Move the money
        if (price > 0) {
            Currency.wire(this, price, (MoneyOwner) from); // TODO: Remove the cast
        }
        privatesCostThisTurn.change(price);

        // Move any special abilities to the portfolio, if configured so
        Set<SpecialProperty> sps = privateCompany.getSpecialProperties();
        if (sps != null) {
            // Need intermediate List to avoid ConcurrentModificationException
            List<SpecialProperty> spsToMoveHere =
                    new ArrayList<SpecialProperty>(2);
            List<SpecialProperty> spsToMoveToGM =
                    new ArrayList<SpecialProperty>(2);
            for (SpecialProperty sp : sps) {
                if (sp.getTransferText().equalsIgnoreCase("toCompany")) {
                    spsToMoveHere.add(sp);
                } else if (sp.getTransferText().equalsIgnoreCase("toGameManager")) {
                    // This must be SellBonusToken - remember the owner!
                    if (sp instanceof SellBonusToken) {
                        // TODO: Check if this works correctly
                        ((SellBonusToken) sp).setSeller(this);
                        // Also note 1 has been used
                        ((SellBonusToken) sp).setExercised();
                    }
                    spsToMoveToGM.add(sp);
                }
            }
            for (SpecialProperty sp : spsToMoveHere) {
                sp.moveTo(portfolio.getParent());
            }
            for (SpecialProperty sp : spsToMoveToGM) {
                getRoot().getGameManager().addSpecialProperty(sp);
                log.debug("SP " + sp.getId() + " is now a common property");
            }
        }

    }

    public Model getPrivatesSpentThisTurnModel() {
        return privatesCostThisTurn;
    }

    public void layTile(MapHex hex, Tile tile, int orientation, int cost) {

        String tileLaid =
                "#" + tile.toText() + "/" + hex.getId() + "/"
                        + hex.getOrientationName(orientation);
        tilesLaidThisTurn.append(tileLaid, ", ");

        if (cost > 0) tilesCostThisTurn.change(cost);

    }

    public void layTilenNoMapMode(int cost) {
        if (cost > 0) tilesCostThisTurn.change(cost);
        tilesLaidThisTurn.append(Bank.format(this, cost), ",");
    }

    public StringState getTilesLaidThisTurnModel() {
        return tilesLaidThisTurn;
    }

    public Model getTilesCostThisTurnModel() {
        return tilesCostThisTurn;
    }

    public void layBaseToken(MapHex hex, int cost) {

        String tokenLaid = hex.getId();
        tokensLaidThisTurn.append(tokenLaid, ", ");
        if (cost > 0) tokensCostThisTurn.change(cost);
    }

    public void layBaseTokennNoMapMode(int cost) {
        if (cost > 0) tokensCostThisTurn.change(cost);
        tokensLaidThisTurn.append(Bank.format(this, cost), ",");
    }

    /**
     * Calculate the cost of laying a token, given the hex where
     * the token is laid. This only makes a difference for de "distance" method.
     *
     * @param hex The hex where the token is to be laid.
     * @return The cost of laying that token.
     */
    public int getBaseTokenLayCost(MapHex hex) {

        if (baseTokenLayCost == null) return 0;

        if (baseTokenLayCostMethod.equals(BASE_COST_SEQUENCE)) {
            int index = getNumberOfLaidBaseTokens();

            if (index >= baseTokenLayCost.size()) {
                index = baseTokenLayCost.size() - 1;
            } else if (index < 0) {
                index = 0;
            }
            return baseTokenLayCost.get(index);
        } else if (baseTokenLayCostMethod.equals(BASE_COST_DISTANCE)) {
            if (hex == null) {
                return baseTokenLayCost.get(0);
            } else {
                // WARNING: no provision yet for multiple home hexes.
                return getRoot().getMapManager().getHexDistance(homeHexes.get(0), hex) * baseTokenLayCost.get(0);
            }
        } else {
            return 0;
        }
    }

    /**
     * Return all possible token lay costs to be incurred for the
     * company's next token lay. For the distance method it will be a full list
     */
    public Set<Integer> getBaseTokenLayCosts() {

        if (baseTokenLayCostMethod.equals(BASE_COST_SEQUENCE)) {
            return ImmutableSet.of(getBaseTokenLayCost(null));
        } else if (baseTokenLayCostMethod.equals(BASE_COST_DISTANCE)) {
            // WARNING: no provision yet for multiple home hexes.
            Collection<Integer> distances = getRoot().getMapManager().getCityDistances(homeHexes.get(0));
            ImmutableSet.Builder<Integer> costs = ImmutableSet.builder();
            for (int distance : distances) {
                costs.add(distance * baseTokenLayCost.get(0));
            }
            return costs.build();
        } else {
            return ImmutableSet.of(0);
        }

    }

    public StringState getTokensLaidThisTurnModel() {
        return tokensLaidThisTurn;
    }

    public MoneyModel getTokensCostThisTurnModel() {
        return tokensCostThisTurn;
    }

    public BaseTokensModel getBaseTokensModel() {
        return baseTokens;
    }

    public boolean addBonus(Bonus bonus) {
        bonuses.add(bonus);
        return true;
    }

    public boolean removeBonus(Bonus bonus) {
        bonus.close(); // close the bonus
        // TODO: add bonusValue as dependence to bonuses
        bonuses.remove(bonus);
        return true;
    }

    public boolean removeBonus(String name) {
        if (bonuses != null && !bonuses.isEmpty()) {
            for (Bonus bonus : bonuses.view()) {
                if (bonus.getName().equals(name)) return removeBonus(bonus);
            }
        }
        return false;
    }

    public List<Bonus> getBonuses() {
        return bonuses.view();
    }

    public BonusModel getBonusTokensModel() {
        return bonusValue;
    }

    public boolean hasLaidHomeBaseTokens() {
        return baseTokens.nbLaidTokens() > 0;
    }

    // Return value is not used
    public boolean layHomeBaseTokens() {

        if (hasLaidHomeBaseTokens()) return true;

        for (MapHex homeHex : homeHexes) {
            if (homeCityNumber == 0) {
                // This applies to cases like 1830 Erie and 1856 THB.
                // On a trackless tile it does not matter, but if
                // the tile has track (such as the green OO tile),
                // the player must select a city.
                if (homeHex.getCurrentTile().hasNoStationTracks()) {
                    // No tracks, then it doesn't matter
                    homeCityNumber = 1;
                } else {
                    // Cover the case that there already is another token.
                    // Allowing this is optional for 1856 Hamilton (THB home)
                    Set<Stop> stops = homeHex.getStops();
                    List<Stop> openStops = new ArrayList<Stop>();
                    for (Stop stop : stops) {
                        if (stop.hasTokenSlotsLeft()) openStops.add(stop);
                    }
                    if (openStops.size() == 1) {
                        // Just one spot: lay the home base there.
                        homeCityNumber = openStops.get(0).getRelatedNumber();
                    } else {
                        // ??
                        // TODO Will player be asked??
                        return false;
                    }
                }
            }
            log.debug(getId() + " lays home base on " + homeHex.getId() + " city "
                    + homeCityNumber);
            homeHex.layBaseToken(this, homeHex.getRelatedStop(homeCityNumber));
        }
        return true;
    }

    /**
     * @return the next (free) token to lay, null if none is available
     */
    public BaseToken getNextBaseToken() {
        return baseTokens.getNextToken();
    }

    public ImmutableSet<BaseToken> getAllBaseTokens() {
        return baseTokens.getAllTokens();
    }

    public ImmutableSet<BaseToken> getLaidBaseTokens() {
        return baseTokens.getLaidTokens();
    }

    public int getNumberOfBaseTokens() {
        return baseTokens.nbAllTokens();
    }

    public int getNumberOfFreeBaseTokens() {
        return baseTokens.nbFreeTokens();
    }

    public int getNumberOfLaidBaseTokens() {
        return baseTokens.nbLaidTokens();
    }

    public boolean hasBaseTokens() {
        return (baseTokens.nbAllTokens() > 0);
    }

    public int getNumberOfTileLays(String tileColour) {

        Phase phase = getRoot().getPhaseManager().getCurrentPhase();

        // Get the number of tile lays from Phase
        int tileLays = phase.getTileLaysPerColour(getType().getId(), tileColour);

        // standard cases: 0 and 1, return
        if (tileLays <= 1) return tileLays;

        // More than one tile lay allowed.
        // Check if there is a limitation on the number of turns that this is valid.
        if (turnsWithExtraTileLays != null) {
            extraTiles.set(turnsWithExtraTileLays.get(tileColour));
        }

        // check if extraTiles is defined
        if (extraTiles.value() != null) {
            // the value is zero already, thus no extra tiles
            if (extraTiles.value().value() == 0) {
                return 1;
            } else {
                // reduce the number of turns by one
                extraTiles.value().add(-1);
            }
        }
        return tileLays;
    }

    public boolean mustOwnATrain() {
        return mustOwnATrain;
    }

    public boolean mustTradeTrainsAtFixedPrice() {
        return mustTradeTrainsAtFixedPrice;
    }

    public int getCurrentNumberOfLoans() {
        return currentNumberOfLoans.value();
    }

    public int getCurrentLoanValue() {
        return getCurrentNumberOfLoans() * getValuePerLoan();
    }

    public void addLoans(int number) {
        currentNumberOfLoans.add(number);
        currentLoanValue.change(number * getValuePerLoan());
    }

    public int getLoanInterestPct() {
        return loanInterestPct;
    }

    public int getMaxNumberOfLoans() {
        return maxNumberOfLoans;
    }

    public boolean canLoan() {
        return maxNumberOfLoans != 0;
    }

    public int getMaxLoansPerRound() {
        return maxLoansPerRound;
    }

    public int getValuePerLoan() {
        return valuePerLoan;
    }

    public MoneyModel getLoanValueModel() {
        return currentLoanValue;
    }

    public Observable getRightsModel() {
        return rightsModel;
    }

    public boolean canClose() {
        return canClose;
    }

    public void setRight(SpecialRight right) {
        if (rightsModel == null) {
            rightsModel = RightsModel.create(this, "RightsModel");
        }
        rightsModel.add(right);
    }

    public boolean hasRight(SpecialRight right) {
        return rightsModel.contains(right);
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Cannot clone company " + getId());
            return null;
        }

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

        return clone;
    }

    /**
     * Extra codes to be added to the president's indicator in the Game Status window.
     * Normally nothing (see 1856 CGR for an exception).
     */
    public String getExtraShareMarks() {
        return "";
    }

    /**
     * Does the company has a route?
     * Currently this is a stub that always returns true.
     */
    public boolean hasRoute() {
        return true;
    }

    // Owner method
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }

    // Company methods
    public void initType(CompanyType type) {
        this.type = type;
    }

    public CompanyType getType() {
        return type;
    }

    public boolean isClosed() {
        return closed.value();
    }

    public void setClosed() {
        closed.set(true);

        PortfolioOwner shareDestination;
        // If applicable, prepare for a restart
        if (canBeRestarted) {
            if (certsAreInitiallyAvailable) {
                shareDestination = getRoot().getBank().getIpo();
            } else {
                shareDestination = getRoot().getBank().getUnavailable();
            }
            reinitialise();
        } else {
            shareDestination = getRoot().getBank().getScrapHeap();
            inGameState.set(false);
        }

        // Dispose of the certificates
        for (PublicCertificate cert : certificates) {
            if (cert.getOwner() != shareDestination) {
                cert.moveTo(shareDestination);
            }
        }

        // Any trains go to the pool (from the 1856 rules)
        portfolio.getTrainsModel().getPortfolio().moveAll(getRoot().getBank().getPool());

        // Any cash goes to the bank (from the 1856 rules)
        int cash = treasury.value();
        if (cash > 0) {
            treasury.setSuppressZero(true);
            Currency.toBank(this, cash);
        }

        lastRevenue.setSuppressZero(true);
        setLastRevenue(0);

        // move all laid tokens to free tokens again
        Portfolio.moveAll(baseTokens.getLaidTokens(), this);

        // close company on the stock market
        getRoot().getStockMarket().close(this);

    }

    public String getLongName() {
        return longName;
    }

    public String getAlias() {
        return alias;
    }

    public String getInfoText() {
        return infoText;
    }

    public ImmutableSet<SpecialProperty> getSpecialProperties() {
        return portfolio.getPersistentSpecialProperties();
    }

    // MoneyOwner interface
    public Purse getPurse() {
        return treasury.getPurse();
    }

    public int getCash() {
        return getPurse().value();
    }

    // Comparable interface
    public int compareTo(PublicCompany other) {
        return this.getId().compareTo(other.getId());
    }

    public void setRelatedNationalCompany(String companyName) {
        this.relatedPublicCompany = companyName;
    }

    public String getRelatedNationalCompany() {
        return relatedPublicCompany;
    }


    public boolean isRelatedToNational(String nationalInFounding) {
        if (this.getRelatedNationalCompany().equals(nationalInFounding)) {
            return true;
        }
        return false;
    }

    /**
     * @return the foundingStartCompany
     */
    public String getFoundingStartCompany() {

        return foundingStartCompany;
    }

    /**
     * @param foundingCompany the foundingStartCompany to set
     */
    public void setStartingMinor(String foundingCompany) {
        this.foundingStartCompany = foundingCompany;
    }


    public Model getLastDirectIncomeModel() {
        return lastDirectIncome;
    }

    /**
     * Store the last direct Income earned by this company.
     *
     * @param i The last revenue amount.
     */
    public void setLastDirectIncome(int i) {
        lastDirectIncome.set(i);
    }

    /**
     * Get the last directIncome earned by this company.
     *
     * @return The last revenue amount.
     */
    public int getLastDirectIncome() {
        return lastDirectIncome.value();
    }

    public void setDirectIncomeRevenue(int directIncome) {
        this.directIncomeRevenue.set(directIncome);
    }

    public int getDirectIncomeRevenue() {
        return directIncomeRevenue.value();
    }

}
