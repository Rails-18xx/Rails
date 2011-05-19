/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCompany.java,v 1.100 2010/06/17 21:35:54 evos Exp $ */
package rails.game;

import java.awt.Color;
import java.util.*;

import rails.game.action.SetDividend;
import rails.game.model.*;
import rails.game.move.*;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialPropertyI;
import rails.game.state.*;
import rails.util.*;

/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies encompass all 18xx company-like entities that lay
 * tracks and run trains. <p> Ownership of companies will always be performed by
 * holding certificates. Some minor company types may have only one certificate,
 * but this will still be the form in which ownership is expressed. <p> Company
 * shares may or may not have a price on the stock market.
 */
public class PublicCompany extends Company implements PublicCompanyI {

    protected static final int DEFAULT_SHARE_UNIT = 10;

    protected static int numberOfPublicCompanies = 0;

    // Home base token lay times
    protected static final int WHEN_STARTED = 0;
    protected static final int WHEN_FLOATED = 1;
    protected static final int START_OF_FIRST_OR = 2;

    // Base token lay cost calculation methods
    public static final String BASE_COST_SEQUENCE = "sequence";
    public static final String BASE_COST_DISTANCE = "distance";

    protected static final String[] tokenLayTimeNames =
        new String[] { "whenStarted", "whenFloated", "firstOR" };

    protected int homeBaseTokensLayTime = START_OF_FIRST_OR;

    /**
     * Foreground (i.e. text) colour of the company tokens (if pictures are not
     * used)
     */
    protected Color fgColour;

    /** Hexadecimal representation (RRGGBB) of the foreground colour. */
    protected String fgHexColour = "FFFFFF";

    /** Background colour of the company tokens */
    protected Color bgColour;

    /** Hexadecimal representation (RRGGBB) of the background colour. */
    protected String bgHexColour = "000000";

    /** Home hex & city * 
     * Two home hexes is supported, but only if:<br>
     * 1. The locations are fixed (i.e. configured by XML), and<br>
     * 2. Any station (city) numbers are equal for the two home stations.
     * There is no provision yet for two home hexes having different tile station numbers. */
    protected String homeHexNames = null;
    protected List<MapHex> homeHexes = null;
    protected int homeCityNumber = 1;
    protected boolean homeAllCitiesBlocked = false;

    /** Destination hex * */
    protected String destinationHexName = null;
    protected MapHex destinationHex = null;
    protected BooleanState hasReachedDestination = null;

    /** Sequence number in the array of public companies - may not be useful */
    protected int publicNumber = -1; // For internal use

    protected List<TokenI> allBaseTokens;

    protected List<TokenI> freeBaseTokens;

    protected List<TokenI> laidBaseTokens;

    protected int numberOfBaseTokens = 0;

    protected int baseTokensBuyCost = 0;
    /** An array of base token laying costs, per successive token */
    protected int[] baseTokenLayCost;
    protected String baseTokenLayCostMethod = "sequential";

    protected BaseTokensModel baseTokensModel; // Create after cloning

    /**
     * Initial (par) share price, represented by a stock market location object
     */
    protected PriceModel parPrice = null;

    /** Current share price, represented by a stock market location object */
    protected PriceModel currentPrice = null;

    /** Company treasury, holding cash */
    protected CashModel treasury = null;

    /** PresidentModel */
    protected PresidentModel presidentModel = null;

    /** Has the company started? */
    protected BooleanState hasStarted = null;

    /** Total bonus tokens amount */
    protected BonusModel bonusValue = null;

    /** Acquires Bonus objects */
    protected List<Bonus> bonuses = null;

    /** Most recent revenue earned. */
    protected MoneyModel lastRevenue = null;

    /** Most recent payout decision. */
    protected StringState lastRevenueAllocation;

    /** Is the company operational ("has it floated")? */
    protected BooleanState hasFloated = null;

    /** Has the company already operated? */
    protected BooleanState hasOperated = null;

    /** Are company shares buyable (i.e. before started)? */
    protected BooleanState buyable = null;

    /** In-game state.
     * <p> Will only be set false if the company is closed and cannot ever be reopened.
     * By default it will be set false if a company is closed. */
    protected BooleanState inGameState = null;

    /**
     * A map per tile colour. Each entry contains a map per phase, of which each
     * value is an Integer defining the number of allowed tile lays. Only
     * numbers deviating from 1 need be specified, the default is always 1.
     */
    protected Map<String, HashMap<String, Integer>> extraTileLays = null;
    /**
     * A map per tile colour, holding the number of turns that the tile lay
     * number applies. The default number is always 1.
     */
    protected Map<String, Integer> turnsWithExtraTileLaysInit = null;
    /** Copy of turnsWithExtraTileLaysInit, per company */
    protected Map<String, IntegerState> turnsWithExtraTileLays = null;
    /**
     * Number of tiles laid. Only used where more tiles can be laid in the
     * company's first OR turn.
     */
    protected IntegerState extraTiles = null;

    /* Spendings in the current operating turn */
    protected MoneyModel privatesCostThisTurn;

    protected StringState tilesLaidThisTurn;

    protected MoneyModel tilesCostThisTurn;

    protected StringState tokensLaidThisTurn;

    protected MoneyModel tokensCostThisTurn;

    protected MoneyModel trainsCostThisTurn;

    protected boolean canBuyStock = false;

    protected boolean canBuyPrivates = false;

    protected boolean canUseSpecialProperties = false;

    /** Can a company be restarted once it is closed? */
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

    /** The certificates of this company (minimum 1) */
    protected ArrayList<PublicCertificateI> certificates;
    /** Are the certificates available from the first SR? */
    boolean certsAreInitiallyAvailable = true;

    /** What percentage of ownership constitutes "one share" */
    protected IntegerState shareUnit;

    /** What number of share units relates to the share price
     * (normally 1, but 2 for 1835 Prussian)
     */
    protected int shareUnitsForSharePrice = 1;

    /** At what percentage sold does the company float */
    protected int floatPerc = 0;

    /** Share price movement on floating (1851: up) */
    protected boolean sharePriceUpOnFloating = false;

    /** Does the company have a stock price (minors often don't) */
    protected boolean hasStockPrice = true;

    /** Does the company have a par price? */
    protected boolean hasParPrice = true;

    protected boolean splitAllowed = false;

    /** Is the revenue always split (typical for non-share minors) */
    protected boolean splitAlways = false;

    /** Must payout exceed stock price to move token right? */
    protected boolean payoutMustExceedPriceToMove = false;

    /*---- variables needed during initialisation -----*/
    protected String startSpace = null;

    protected int capitalisation = CAPITALISE_FULL;

    /** Fixed price (for a 1835-style minor) */
    protected int fixedPrice = 0;

    /** Train limit per phase (index) */
    protected int[] trainLimit = new int[0];

    /** Private to close if first train is bought */
    protected String privateToCloseOnFirstTrainName = null;

    protected PrivateCompanyI privateToCloseOnFirstTrain = null;

    /** Must the company own a train */
    protected boolean mustOwnATrain = true;

    protected boolean mustTradeTrainsAtFixedPrice = false;

    /** Can the company price token go down to a "Close" square?
     * 1856 CGR cannot.
     */
    protected boolean canClose = true;

    /** Initial train at floating time */
    protected String initialTrainType = null;
    protected int initialTrainCost = 0;
    protected boolean initialTrainTradeable = true;

    /* Loans */
    protected int maxNumberOfLoans = 0;
    protected int valuePerLoan = 0;
    protected IntegerState currentNumberOfLoans = null;
    protected int loanInterestPct = 0;
    protected int maxLoansPerRound = 0;
    protected MoneyModel currentLoanValue = null;

    protected BooleanState canSharePriceVary = null;

    protected GameManagerI gameManager;
    protected Bank bank;
    protected StockMarketI stockMarket;
    protected MapManager mapManager;

    /**
     * The constructor. The way this class is instantiated does not allow
     * arguments.
     */
    public PublicCompany() {
        super();
    }

    /**
     * To configure all public companies from the &lt;PublicCompany&gt; XML
     * element
     */
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        longName = tag.getAttributeAsString("longname", name);
        infoText = "<html>"+longName;

        alias = tag.getAttributeAsString("alias", alias);

        /* Configure public company features */
        fgHexColour = tag.getAttributeAsString("fgColour", fgHexColour);
        fgColour = Util.parseColour(fgHexColour);

        bgHexColour = tag.getAttributeAsString("bgColour", bgHexColour);
        bgColour = Util.parseColour(bgHexColour);

        floatPerc = tag.getAttributeAsInteger("floatPerc", floatPerc);

        startSpace = tag.getAttributeAsString("startspace");

        fixedPrice = tag.getAttributeAsInteger("price", 0);

        numberOfBaseTokens = tag.getAttributeAsInteger("tokens", 1);

        certsAreInitiallyAvailable
        = tag.getAttributeAsBoolean("available", certsAreInitiallyAvailable);

        canBeRestarted = tag.getAttributeAsBoolean("restartable", canBeRestarted);

        Tag shareUnitTag = tag.getChild("ShareUnit");
        if (shareUnitTag != null) {
            shareUnit = new IntegerState (name+"_ShareUnit",
                    shareUnitTag.getAttributeAsInteger("percentage", DEFAULT_SHARE_UNIT));
            shareUnitsForSharePrice
            = shareUnitTag.getAttributeAsInteger("sharePriceUnits", shareUnitsForSharePrice);
        }

        Tag homeBaseTag = tag.getChild("Home");
        if (homeBaseTag != null) {
            homeHexNames = homeBaseTag.getAttributeAsString("hex");
            homeCityNumber = homeBaseTag.getAttributeAsInteger("city", 1);
            homeAllCitiesBlocked = homeBaseTag.getAttributeAsBoolean("allCitiesBlocked", false);
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
            infoText += "<br>"+LocalText.getText(infoKey, (Object[])infoParms);
        }

        // Special properties (as in the 1835 black minors)
        super.configureFromXML(tag);

        // TODO Normally set in the default train type. May be wrong place.
        // Ridiculous to reparse with each train type.
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
            trainLimit = trainsTag.getAttributeAsIntegerArray("limit");
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

        Tag tileLaysTag = tag.getChild("TileLays");
        if (tileLaysTag != null) {

            for (Tag numberTag : tileLaysTag.getChildren("Number")) {

                String colourString = numberTag.getAttributeAsString("colour");
                if (colourString == null)
                    throw new ConfigurationException(
                    "No colour entry for NumberOfTileLays");
                String phaseString = numberTag.getAttributeAsString("phase");
                if (phaseString == null)
                    throw new ConfigurationException(
                    "No phase entry for NumberOfTileLays");
                int number = numberTag.getAttributeAsInteger("number");
                Integer lays = new Integer(number);

                int validForTurns =
                    numberTag.getAttributeAsInteger("occurrences", 0);

                String[] colours = colourString.split(",");
                HashMap<String, Integer> phaseMap;
                /**
                 * TODO: should not be necessary to specify all phases
                 * separately
                 */
                String[] phases = phaseString.split(",");
                for (int i = 0; i < colours.length; i++) {
                    if (extraTileLays == null)
                        extraTileLays =
                            new HashMap<String, HashMap<String, Integer>>();
                    extraTileLays.put(colours[i], (phaseMap =
                        new HashMap<String, Integer>()));
                    for (int k = 0; k < phases.length; k++) {
                        phaseMap.put(phases[k], lays);
                    }
                    if (validForTurns > 0) {
                        if (turnsWithExtraTileLaysInit == null) {
                            turnsWithExtraTileLaysInit =
                                new HashMap<String, Integer>();
                        }
                        turnsWithExtraTileLaysInit.put(colours[i],
                                validForTurns);
                    }
                }
            }
        }

        int certIndex = 0;
        List<Tag> certificateTags = tag.getChildren("Certificate");
        if (certificateTags != null) {
            int shareTotal = 0;
            boolean gotPresident = false;
            PublicCertificateI certificate;
            certificates = new ArrayList<PublicCertificateI>(); // Throw away
            // the per-type
            // specification

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
                                + name
                                + " cannot have multiple President shares");
                    gotPresident = true;
                }

                for (int k = 0; k < number; k++) {
                    certificate = new PublicCertificate(shares, president,
                            certIsInitiallyAvailable, certificateCount, certIndex++);
                    addCertificate(certificate);
                    shareTotal += shares * shareUnit.intValue();
                }
            }
            if (shareTotal != 100)
                throw new ConfigurationException("Company type " + name
                        + " total shares is not 100%");
        }
        nameCertificates();

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
                    layCostTag.getAttributeAsIntegerArray("cost");
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

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);

        inGameState = new BooleanState(name + "_InGame", true);

        this.portfolio = new Portfolio(name, this);
        treasury = new CashModel(this);
        lastRevenue = new MoneyModel(name + "_lastRevenue");
        lastRevenue.setOption(MoneyModel.SUPPRESS_INITIAL_ZERO);
        lastRevenueAllocation = new StringState(name + "_lastAllocation");
        baseTokensModel = new BaseTokensModel(this);
        presidentModel = new PresidentModel(this);

        hasStarted = new BooleanState(name + "_hasStarted", false);
        hasFloated = new BooleanState(name + "_hasFloated", false);
        hasOperated = new BooleanState(name + "_hasOperated", false);
        buyable = new BooleanState(name + "_isBuyable", false);

        allBaseTokens = new ArrayList<TokenI>();
        freeBaseTokens = new ArrayList<TokenI>();
        laidBaseTokens = new ArrayList<TokenI>();

        /* Spendings in the current operating turn */
        privatesCostThisTurn = new MoneyModel(name + "_spentOnPrivates");
        privatesCostThisTurn.setOption(MoneyModel.SUPPRESS_ZERO);
        tilesLaidThisTurn = new StringState(name + "_tilesLaid");
        tilesCostThisTurn = new MoneyModel(name + "_spentOnTiles");
        tilesCostThisTurn.setOption(MoneyModel.SUPPRESS_ZERO);
        tokensLaidThisTurn = new StringState(name + "_tokensLaid");
        tokensCostThisTurn = new MoneyModel(name + "_spentOnTokens");
        tokensCostThisTurn.setOption(MoneyModel.SUPPRESS_ZERO);
        trainsCostThisTurn = new MoneyModel(name + "_spentOnTrains");
        trainsCostThisTurn.setOption(MoneyModel.SUPPRESS_ZERO|MoneyModel.ALLOW_NEGATIVE);
        bonusValue = new BonusModel(name + "_bonusValue");

        if (hasStockPrice) {
            parPrice = new PriceModel(this, name + "_ParPrice");
            currentPrice = new PriceModel(this, name + "_CurrentPrice");
            canSharePriceVary = new BooleanState (name+"_CanSharePriceVary", true);
        }

        if (turnsWithExtraTileLaysInit != null) {
            turnsWithExtraTileLays = new HashMap<String, IntegerState>();
            for (String colour : turnsWithExtraTileLaysInit.keySet()) {
                turnsWithExtraTileLays.put(colour, new IntegerState(
                        name + "_" + colour + "_ExtraTileTurns",
                        turnsWithExtraTileLaysInit.get(colour)));
            }
        }

        PublicCompanyI dummyCompany = (PublicCompanyI) type.getDummyCompany();
        if (dummyCompany != null) {
            fgHexColour = dummyCompany.getHexFgColour();
            bgHexColour = dummyCompany.getHexBgColour();
        }

        if (maxNumberOfLoans != 0) {
            currentNumberOfLoans = new IntegerState (name+"_Loans", 0);
            currentLoanValue = new MoneyModel (name+"_LoanValue", 0);
            currentLoanValue.setOption(MoneyModel.SUPPRESS_ZERO);
        }

    }

    public void setIndex (int index) {
        publicNumber = index;
    }

    /**
     * Final initialisation, after all XML has been processed.
     */
    public void finishConfiguration(GameManagerI gameManager)
    throws ConfigurationException {

        this.gameManager = gameManager;
        bank = gameManager.getBank();
        stockMarket = gameManager.getStockMarket();
        mapManager = gameManager.getMapManager();

        if (hasStockPrice && Util.hasValue(startSpace)) {
            parPrice.setPrice(stockMarket.getStockSpace(
                    startSpace));
            if (parPrice.getPrice() == null)
                throw new ConfigurationException("Invalid start space "
                        + startSpace + " for company "
                        + name);
            currentPrice.setPrice(parPrice.getPrice());

        }

        if (shareUnit == null) {
            shareUnit = new IntegerState (name+"_ShareUnit", DEFAULT_SHARE_UNIT);
        }

        // Give each certificate an unique Id
        PublicCertificateI cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(name, i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable()
                    && this.certsAreInitiallyAvailable);
        }

        BaseToken token;
        for (int i = 0; i < numberOfBaseTokens; i++) {
            token = new BaseToken(this);
            allBaseTokens.add(token);
            freeBaseTokens.add(token);
        }

        if (homeHexNames != null) {
            homeHexes = new ArrayList<MapHex>(2);
            MapHex homeHex;
            for (String homeHexName : homeHexNames.split(",")) {
                homeHex = mapManager.getHex(homeHexName);
                if (homeHex == null) {
                    throw new ConfigurationException("Invalid home hex "
                            + homeHexName
                            + " for company " + name);
                }
                homeHexes.add (homeHex);
                infoText += "<br>Home: " + homeHex.getInfo();
            }
        }

        if (destinationHexName != null) {
            destinationHex = mapManager.getHex(destinationHexName);
            if (destinationHex != null) {
                hasReachedDestination = new BooleanState (name+"_reachedDestination", false);
            } else {
                throw new ConfigurationException("Invalid destination hex "
                        + destinationHexName
                        + " for company " + name);
            }
            infoText += "<br>Destination: "+destinationHex.getInfo();
        }

        if (Util.hasValue(privateToCloseOnFirstTrainName)) {
            privateToCloseOnFirstTrain =
                gameManager.getCompanyManager().getPrivateCompany(
                        privateToCloseOnFirstTrainName);
        }

        infoText += parentInfoText;
        parentInfoText = "";
    }

    /** Reset turn objects */
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
     * @return Returns the homeHex.
     */
    public List<MapHex> getHomeHexes() {
        return homeHexes;
    }

    /**
     * Set a non-fixed company home hex.
     * Only covers setting <i>one</i> home hex. 
     * Having <i>two</i> home hexes is currently only supported if the locations are preconfigured.
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
     * @param homeStation The homeStation to set.
     */
    public void setHomeCityNumber(int number) {
        this.homeCityNumber = number;
    }

    /**
     * @return true -> requires an open slot in each city of the hex, false -> one slot on the hex
     *
     */
    public boolean isHomeBlockedForAllCities() {
        return homeAllCitiesBlocked;
    }


    /**
     * @return Returns the destinationHex.
     */
    public MapHex getDestinationHex() {
        return destinationHex;
    }

    public boolean hasDestination () {
        return destinationHex != null;
    }

    public boolean hasReachedDestination() {
        return hasReachedDestination != null &&
        hasReachedDestination.booleanValue();
    }

    public void setReachedDestination (boolean value) {
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

    /** Stub that allows exclusions such as that 1856 CGR may not buy a 4 */
    public boolean mayBuyTrainType (TrainI train) {
        return true;
    }

    public boolean mustHaveOperatedToTradeShares() {
        return mustHaveOperatedToTradeShares;
    }

    public void start(StockSpaceI startSpace) {

        hasStarted.set(true);
        if (hasStockPrice) buyable.set(true);

        // In case of a restart: undo closing
        if (closedObject.booleanValue()) closedObject.set(false);

        if (startSpace != null) {
            setParSpace(startSpace);
            //  The current price is set via the Stock Market
            stockMarket.start(this, startSpace);
        }


        if (homeBaseTokensLayTime == WHEN_STARTED) {
            layHomeBaseTokens();
        }
    }

    public void start(int price) {
        StockSpaceI startSpace = stockMarket.getStartSpace(price);
        if (startSpace == null) {
            log.error("Invalid start price " + Bank.format(price));
        } else {
            start(startSpace);
        }
    }

    /**
     * Start a company.
     */
    public void start() {
        start (getStartSpace());
    }

    public void transferAssetsFrom(PublicCompanyI otherCompany) {

        if (otherCompany.getCash() > 0) {
            new CashMove(otherCompany, this, otherCompany.getCash());
        }
        portfolio.transferAssetsFrom(otherCompany.getPortfolio());
    }

    /**
     * @return Returns true is the company has started.
     */
    public boolean hasStarted() {
        return hasStarted.booleanValue();
    }

    /** Make company shares buyable. Only useful where shares become
     * buyable before the company has started (e.g. 1835 Prussian).
     * */
    public void setBuyable(boolean buyable) {
        this.buyable.set(buyable);
    }

    public boolean isBuyable() {
        return buyable.booleanValue();
    }

    /**
     * Float the company, put its initial cash in the treasury.
     */
    public void setFloated() {

        hasFloated.set(true);
        // In case of a restart
        if (hasOperated.booleanValue()) hasOperated.set(false);

        // Remove the "unfloated" indicator in GameStatus
        getPresident().getPortfolio().getShareModel(this).update();

        if (sharePriceUpOnFloating) {
            stockMarket.moveUp(this);
        }

        if (homeBaseTokensLayTime == WHEN_FLOATED) {
            layHomeBaseTokens();
        }

        if (initialTrainType != null) {
            TrainManager trainManager = gameManager.getTrainManager();
            TrainTypeI type = trainManager.getTypeByName(initialTrainType);
            TrainI train = bank.getIpo().getTrainOfType(type);
            buyTrain(train, initialTrainCost);
            train.setTradeable(initialTrainTradeable);
            trainManager.checkTrainAvailability(train, bank.getIpo());
        }
    }

    /**
     * Has the company already floated?
     *
     * @return true if the company has floated.
     */
    public boolean hasFloated() {
        return hasFloated.booleanValue();
    }

    /**
     * Has the company already operated?
     *
     * @return true if the company has operated.
     */
    public boolean hasOperated() {
        return hasOperated.booleanValue();
    }

    public void setOperated() {
        hasOperated.set(true);
    }

    @Override
    public void setClosed() {
        super.setClosed();

        Portfolio shareDestination;
        // If applicable, prepare for a restart
        if (canBeRestarted) {
            if (certsAreInitiallyAvailable) {
                shareDestination = bank.getIpo();
            } else {
                shareDestination = bank.getUnavailable();
            }
            reinitialise();
        } else {
            shareDestination = bank.getScrapHeap();
            inGameState.set(false);
        }

        // Dispose of the certificates
        for (PublicCertificateI cert : certificates) {
            if (cert.getHolder() != shareDestination) {
                cert.moveTo(shareDestination);
            }
        }

        // Any trains go to the pool (from the 1856 rules)
        Util.moveObjects(portfolio.getTrainList(), bank.getPool());

        // Any cash goes to the bank (from the 1856 rules)
        int cash = treasury.getCash();
        if (cash > 0) new CashMove (this, bank, cash);

        lastRevenue.setOption(MoneyModel.SUPPRESS_ZERO);
        setLastRevenue(0);
        treasury.setOption(CashModel.SUPPRESS_ZERO);
        treasury.update();

        Util.moveObjects(laidBaseTokens, this);
        stockMarket.close(this);

    }

    /** Reinitialise a company, i.e. close it and make the shares available for a new company start.
     * IMplemented rules are now as in 18EU.
     * TODO Will see later if this is generic enough.
     *
     */
    protected void reinitialise () {
        hasStarted.set(false);
        hasFloated.set(false);
        hasOperated.set(false);
        if (parPrice != null && fixedPrice <= 0) parPrice.setPrice(null);
        if (currentPrice != null) currentPrice.setPrice(null);
    }

    public ModelObject getInGameModel () {
        return inGameState;
    }

    public ModelObject getIsClosedModel () {
        return closedObject;
    }

    /**
     * Set the company par price. <p> <i>Note: this method should <b>not</b> be
     * used to start a company!</i> Use <code><b>start()</b></code> in
     * stead.
     *
     * @param spaceI
     */
    public void setParSpace(StockSpaceI space) {
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
    public StockSpaceI getStartSpace() {
        if (hasParPrice) {
            return parPrice != null ? parPrice.getPrice() : null;
        } else {
            return currentPrice != null ? currentPrice.getPrice() : null;
        }
    }

    public int getIPOPrice () {
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

    public int getMarketPrice () {
        if (getCurrentSpace() != null) {
            return getCurrentSpace().getPrice();
        } else {
            return 0;
        }
    }

    /** Return the price per share at game end.
     * Normally, it is equal to the market price,
     * but in some games (e.g. 1856) deductions may apply.
     * @return
     */
    public int getGameEndPrice() {
        return getMarketPrice();
    }

    /**
     * Set a new company price.
     *
     * @param price The StockSpace object that defines the new location on the
     * stock market.
     */
    public void setCurrentSpace(StockSpaceI price) {
        if (price != null) {
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
    public StockSpaceI getCurrentSpace() {
        return currentPrice != null ? currentPrice.getPrice() : null;
    }

    public void updatePlayersWorth() {

        Map<Player, Boolean> done = new HashMap<Player, Boolean>(8);
        Player owner;
        for (PublicCertificateI cert : certificates) {
            if (cert.getHolder() instanceof Portfolio
                    && ((Portfolio)cert.getHolder()).getOwner() instanceof Player) {
                owner = (Player)((Portfolio)cert.getHolder()).getOwner();
                if (!done.containsKey(owner)) {
                    owner.updateWorth();
                    done.put(owner, true);
                }
            }
        }
    }

    /**
     * Add a given amount to the company treasury.
     *
     * @param amount The amount to add (may be negative).
     */
    public void addCash(int amount) {
        treasury.addCash(amount);
    }

    /**
     * Get the current company treasury.
     *
     * @return The current cash amount.
     */
    public int getCash() {
        return treasury.getCash();
    }

    public String getFormattedCash() {
        return treasury.toString();
    }

    public ModelObject getCashModel() {
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
    public List<PublicCertificateI> getCertificates() {
        return certificates;
    }

    /**
     * Assign a predefined list of certificates to this company. The list is
     * deep cloned.
     *
     * @param list ArrayList containing the certificates.
     */
    public void setCertificates(List<PublicCertificateI> list) {
        certificates = new ArrayList<PublicCertificateI>();
        for (PublicCertificateI cert : list) {
            certificates.add(new PublicCertificate(cert));
        }
    }

    /**
     * Backlink the certificates to this company,
     * and give each one a type name.
     *
     */
    public void nameCertificates () {
        for (PublicCertificateI cert : certificates) {
            cert.setCompany(this);
        }
    }

    /**
     * Add a certificate to the end of this company's list of certificates.
     *
     * @param certificate The certificate to add.
     */
    public void addCertificate(PublicCertificateI certificate) {
        if (certificates == null)
            certificates = new ArrayList<PublicCertificateI>();
        certificates.add(certificate);
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
     * Get the company President.
     *
     */
    public Player getPresident() {
        if (hasStarted()) {
            CashHolder owner = certificates.get(0).getPortfolio().getOwner();
            if (owner instanceof Player) return (Player) owner;
        }
        return null;
    }

    public PresidentModel getPresidentModel() {
        return presidentModel;
    }

    public PublicCertificateI getPresidentsShare () {
        return certificates.get(0);
    }

    public boolean isAvailable() {
        Portfolio presLoc = certificates.get(0).getPortfolio();
        return presLoc != bank.getUnavailable()
        && presLoc != bank.getScrapHeap();
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
        return lastRevenue.intValue();
    }

    public ModelObject getLastRevenueModel() {
        return lastRevenue;
    }

    /** Last revenue allocation (payout, split, withhold) */
    public void setLastRevenueAllocation(int allocation) {
        if (allocation >= 0 && allocation < SetDividend.NUM_OPTIONS) {
            lastRevenueAllocation.set(LocalText.getText(SetDividend.getAllocationNameKey(allocation)));
        } else {
            lastRevenueAllocation.set("");
        }
    }

    public String getlastRevenueAllocationText() {
        return lastRevenueAllocation.stringValue();
    }

    public ModelObject getLastRevenueAllocationModel() {
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
            stockMarket.payOut(this);
        }

    }

    public boolean paysOutToTreasury (PublicCertificateI cert) {

        Portfolio holder = cert.getPortfolio();
        if (holder == bank.getIpo() && ipoPaysOut
                || holder == bank.getPool() && poolPaysOut) {
            return true;
        }
        return false;
    }

    /**
     * Determine if the price token must be moved after a withheld dividend.
     *
     * @param The revenue amount.
     */
    public void withhold(int amount) {
        if (hasStockPrice) stockMarket.withhold(this);
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
        CashHolder owner;

        for (PublicCertificateI cert : certificates) {
            owner = cert.getPortfolio().getOwner();
            if (owner instanceof Bank || owner == cert.getCompany()) {
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
        return shareUnit.intValue();
    }

    public int getShareUnitsForSharePrice() {
        return shareUnitsForSharePrice;
    }

    @Override
    public String toString() {
        return name;
    }

    public boolean hasStockPrice() {
        return hasStockPrice;
    }

    public boolean hasParPrice() {
        return hasParPrice;
    }

    public boolean canSharePriceVary() {
        return canSharePriceVary.booleanValue();
    }

    public int getFixedPrice() {
        return fixedPrice;
    }

    public int getBaseTokensBuyCost() {
        return baseTokensBuyCost;
    }

    public int sharesOwnedByPlayers() {
        int shares = 0;
        for (PublicCertificateI cert : certificates) {
            if (cert.getPortfolio().getOwner() instanceof Player) {
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
        int presShare = pres.getPortfolio().getShare(this);
        int buyerShare = buyer.getPortfolio().getShare(this);
        if (buyerShare > presShare) {
            pres.getPortfolio().swapPresidentCertificate(this,
                    buyer.getPortfolio());
            ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                    buyer.getName(),
                    name ));
        }
    }

    /**
     * Check if the presidency has changed for a <b>selling</b> player.
     */
    public void checkPresidencyOnSale(Player seller) {

        if (seller != getPresident()) return;

        int presShare = seller.getPortfolio().getShare(this);
        int presIndex = seller.getIndex();
        Player player;
        int share;
        GameManagerI gmgr = GameManager.getInstance();

        for (int i = presIndex + 1; i < presIndex
        + gmgr.getNumberOfPlayers(); i++) {
            player = gmgr.getPlayerByIndex(i);
            share = player.getPortfolio().getShare(this);
            if (share > presShare) {
                // Presidency must be transferred
                seller.getPortfolio().swapPresidentCertificate(this,
                        player.getPortfolio());
                ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                        player.getName(),
                        name ));
            }
        }
    }

    /** A generic presidency check. Perhaps it can replace the above two methods. */
    public void checkPresidency () {

        Player president = getPresident();
        int presIndex = president.getIndex();
        int presShare = president.getPortfolio().getShare(this);

        GameManagerI gmgr = GameManager.getInstance();
        Player player;
        int share;

        for (int i = presIndex + 1; i < presIndex
        + gmgr.getNumberOfPlayers(); i++) {
            player = gmgr.getPlayerByIndex(i);
            share = player.getPortfolio().getShare(this);
            if (share > presShare) {
                // Hand presidency to the first player with a higher share
                president.getPortfolio().swapPresidentCertificate(this,
                        player.getPortfolio());
                ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                        player.getName(),
                        name ));
                return;
            }
        }

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
        return 100 / shareUnit.intValue();
    }

    public int getTrainLimit(int phaseIndex) {
        return trainLimit[Math.min(phaseIndex, trainLimit.length - 1)];
    }

    public int getCurrentTrainLimit() {
        return getTrainLimit(GameManager.getInstance().getCurrentPhase().getIndex());
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
    public void buyTrain(TrainI train, int price) {
        if (train.getOwner() instanceof PublicCompanyI) {
            ((MoneyModel)((PublicCompanyI)train.getOwner()).getTrainsSpentThisTurnModel()).add(-price);
        }
        portfolio.buyTrain(train, price);
        trainsCostThisTurn.add(price);
        if (privateToCloseOnFirstTrain != null
                && !privateToCloseOnFirstTrain.isClosed()) {
            privateToCloseOnFirstTrain.setClosed();
        }
    }

    public ModelObject getTrainsSpentThisTurnModel() {
        return trainsCostThisTurn;
    }

    public void buyPrivate(PrivateCompanyI privateCompany, Portfolio from,
            int price) {

        if (from != bank.getIpo()) {
            // The initial buy is reported from StartRound. This message should also
            // move to elsewhere.
            ReportBuffer.add(LocalText.getText("BuysPrivateFromFor",
                    name,
                    privateCompany.getName(),
                    from.getName(),
                    Bank.format(price) ));
        }

        // Move the private certificate
        privateCompany.moveTo(portfolio);

        // Move the money
        if (price > 0) new CashMove(this, from.owner, price);
        privatesCostThisTurn.add(price);

        // Move any special abilities to the portfolio, if configured so
        List<SpecialPropertyI> sps = privateCompany.getSpecialProperties();
        if (sps != null) {
            // Need intermediate List to avoid ConcurrentModificationException
            List<SpecialPropertyI> spsToMoveHere =
                new ArrayList<SpecialPropertyI>(2);
            List<SpecialPropertyI> spsToMoveToGM =
                new ArrayList<SpecialPropertyI>(2);
            for (SpecialPropertyI sp : sps) {
                if (sp.getTransferText().equalsIgnoreCase("toCompany")) {
                    spsToMoveHere.add(sp);
                } else if (sp.getTransferText().equalsIgnoreCase("toGameManager")) {
                    // This must be SellBonusToken - remember the owner!
                    if (sp instanceof SellBonusToken) {
                        ((SellBonusToken)sp).setSeller(this);
                        // Also note 1 has been used
                        ((SellBonusToken)sp).setExercised();
                    }
                    spsToMoveToGM.add(sp);
                }
            }
            for (SpecialPropertyI sp : spsToMoveHere) {
                sp.moveTo(portfolio);
            }
            for (SpecialPropertyI sp : spsToMoveToGM) {
                sp.moveTo(gameManager);
                log.debug("SP "+sp.getName()+" is now a common property");
            }
        }

    }

    public ModelObject getPrivatesSpentThisTurnModel() {
        return privatesCostThisTurn;
    }

    public void layTile(MapHex hex, TileI tile, int orientation, int cost) {

        String tileLaid =
            "#" + tile.getExternalId() + "/" + hex.getName() + "/"
            + hex.getOrientationName(orientation);
        tilesLaidThisTurn.appendWithDelimiter(tileLaid, ", ");

        if (cost > 0) tilesCostThisTurn.add(cost);

        if (extraTiles != null && extraTiles.intValue() > 0) {
            extraTiles.add(-1);
        }
    }

    public void layTileInNoMapMode(int cost) {
        if (cost > 0) tilesCostThisTurn.add(cost);
        tilesLaidThisTurn.appendWithDelimiter(Bank.format(cost), ",");
    }

    public ModelObject getTilesLaidThisTurnModel() {
        return tilesLaidThisTurn;
    }

    public ModelObject getTilesCostThisTurnModel() {
        return tilesCostThisTurn;
    }

    public void layBaseToken(MapHex hex, int cost) {

        String tokenLaid = hex.getName();
        tokensLaidThisTurn.appendWithDelimiter(tokenLaid, ", ");
        if (cost > 0) tokensCostThisTurn.add(cost);
    }

    public void layBaseTokenInNoMapMode(int cost) {
        if (cost > 0) tokensCostThisTurn.add(cost);
        tokensLaidThisTurn.appendWithDelimiter(Bank.format(cost), ",");
    }

    /**
     * Calculate the cost of laying a token, given the hex where
     * the token is laid. This only makes a difference for de "distance" method.
     * @param hex The hex where the token is to be laid.
     * @return The cost of laying that token.
     */
    public int getBaseTokenLayCost(MapHex hex) {

        if (baseTokenLayCost == null) return 0;

        if (baseTokenLayCostMethod.equals(BASE_COST_SEQUENCE)) {
            int index = getNumberOfLaidBaseTokens();

            if (index >= baseTokenLayCost.length) {
                index = baseTokenLayCost.length - 1;
            } else if (index < 0) {
                index = 0;
            }
            return baseTokenLayCost[index];
        } else if (baseTokenLayCostMethod.equals(BASE_COST_DISTANCE)) {
            if (hex == null) {
                return baseTokenLayCost[0];
            } else {
                // WARNING: no provision yet for multiple home hexes.
                return mapManager.getHexDistance(homeHexes.get(0), hex) * baseTokenLayCost[0];
            }
        } else {
            return 0;
        }
    }

    /** Return all possible token lay costs to be incurred for the
     * company's next token lay. In the "distance" method, this will be an array.
     * @return
     */
    public int[] getBaseTokenLayCosts () {

        if (baseTokenLayCostMethod.equals(BASE_COST_SEQUENCE)) {
            return new int[] {getBaseTokenLayCost(null)};
        } else if (baseTokenLayCostMethod.equals(BASE_COST_DISTANCE)) {
            // WARNING: no provision yet for multiple home hexes.
            int[] distances = mapManager.getCityDistances(homeHexes.get(0));
            int[] costs = new int[distances.length];
            int i = 0;
            for (int distance : distances) {
                costs[i++] = distance * baseTokenLayCost[0];
            }
            return costs;
        } else {
            return new int[] {0};
        }

    }

    public ModelObject getTokensLaidThisTurnModel() {
        return tokensLaidThisTurn;
    }

    public ModelObject getTokensCostThisTurnModel() {
        return tokensCostThisTurn;
    }

    public BaseTokensModel getBaseTokensModel() {
        return baseTokensModel;
    }

    public boolean addBonus(Bonus bonus) {
        if (bonuses == null) {
            bonuses = new ArrayList<Bonus>(2);
            bonusValue.set(bonuses);
        }
        new AddToList<Bonus> (bonuses, bonus, name+"_Bonuses", bonusValue);
        return true;
    }

    public boolean removeBonus(Bonus bonus) {
        bonus.close(); // close the bonus
        new RemoveFromList<Bonus> (bonuses, bonus, name+"_Bonuses", bonusValue);
        return true;
    }

    public boolean removeBonus (String name) {
        if (bonuses != null && !bonuses.isEmpty()) {
            for(Bonus bonus : bonuses) {
                if (bonus.getName().equals(name)) return removeBonus(bonus);
            }
        }
        return false;
    }

    public List<Bonus> getBonuses() {
        return bonuses;
    }

    public BonusModel getBonusTokensModel() {
        return bonusValue;
    }

    public boolean hasLaidHomeBaseTokens() {
        return laidBaseTokens.size() > 0;
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
                Map<Integer, List<Track>> tracks
                = homeHex.getCurrentTile().getTracksPerStationMap();
                if (tracks == null || tracks.isEmpty()) {
                    homeCityNumber = 1;
                } else {
                    return false;
                }
            }
            log.debug(name + " lays home base on " + homeHex.getName() + " city "
                    + homeCityNumber);
            homeHex.layBaseToken(this, homeCityNumber);
        }
        return true;
    }

    public BaseToken getFreeToken() {
        if (freeBaseTokens.size() > 0) {
            return (BaseToken) freeBaseTokens.get(0);
        } else {
            return null;
        }
    }

    /**
     * Add a base token to the company charter. This method is called when a
     * base token is removed from a map hex. This may happen because of an Undo
     * action. In some games tokens can be taken back for more "regular" reasons
     * as well. The token is removed from the company laid token list and added
     * to the free token list.
     */

    public boolean addToken(TokenI token, int position) {

        boolean result = false;
        if (token instanceof BaseToken
                && laidBaseTokens.remove(token)
                && Util.addToList(freeBaseTokens, token, position)) {
            token.setHolder(this);
            this.baseTokensModel.update();
        }
        return result;

    }

    public List<TokenI> getTokens() {
        return allBaseTokens;
    }

    public int getNumberOfBaseTokens() {
        return allBaseTokens.size();
    }

    public int getNumberOfFreeBaseTokens() {
        return freeBaseTokens.size();
    }

    public int getNumberOfLaidBaseTokens() {
        return laidBaseTokens.size();
    }

    public boolean hasTokens() {
        return (allBaseTokens.size() > 0);
    }

    /**
     * Remove a base token from the company charter. This method is called when
     * a base token is laid on a map hex. The token is removed from the company
     * free token list and added to the laid token list. In other words: lay a
     * base token
     */
    public boolean removeToken(TokenI token) {

        boolean result = false;
        if (token instanceof BaseToken && freeBaseTokens.remove(token)) {
            result = laidBaseTokens.add(token);
            this.baseTokensModel.update();
        }
        return result;

    }

    public boolean addObject(Moveable object, int[] position) {
        if (object instanceof TokenI) {
            return addToken((TokenI) object, position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }

    public boolean removeObject(Moveable object) {
        if (object instanceof BaseToken) {
            return removeToken((TokenI) object);
        } else {
            return false;
        }
    }

    public int[] getListIndex (Moveable object) {
        if (object instanceof BaseToken) {
            return new int[] {freeBaseTokens.indexOf(object)};
        } else {
            return Moveable.AT_END;
        }
    }

    public int getNumberOfTileLays(String tileColour) {

        if (extraTileLays == null) return 1;

        Map<String, Integer> phaseMap = extraTileLays.get(tileColour);
        if (phaseMap == null || phaseMap.isEmpty()) return 1;

        PhaseI phase = gameManager.getPhaseManager().getCurrentPhase();
        Integer ii = phaseMap.get(phase.getName());
        if (ii == null) return 1;

        int i = ii;
        if (i > 1) {
            if (extraTiles == null && turnsWithExtraTileLays != null) {
                extraTiles = turnsWithExtraTileLays.get(tileColour);
            }
            if (extraTiles != null) {
                if (extraTiles.intValue() == 0) {
                    extraTiles = null;
                    return 1;
                }
            }
        }
        return i;
    }

    public boolean mustOwnATrain() {
        return mustOwnATrain;
    }

    public boolean mustTradeTrainsAtFixedPrice() {
        return mustTradeTrainsAtFixedPrice;
    }

    public int getCurrentNumberOfLoans() {
        return currentNumberOfLoans.intValue();
    }

    public int getCurrentLoanValue () {
        return getCurrentNumberOfLoans() * getValuePerLoan();
    }

    public void addLoans(int number) {
        currentNumberOfLoans.add(number);
        currentLoanValue.add(number * getValuePerLoan());
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

    public MoneyModel getLoanValueModel () {
        return currentLoanValue;
    }

    public boolean canClose() {
        return canClose;
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.fatal("Cannot clone company " + name);
            return null;
        }

        /*
         * Add the certificates, if defined with the CompanyType and absent in
         * the Company specification
         */
        if (certificates != null) {
            ((PublicCompanyI) clone).setCertificates(certificates);
        }

        return clone;
    }

    /** Extra codes to be added to the president's indicator in the Game Status window.
     * Normally nothing (see 1856 CGR for an exception). */
    public String getExtraShareMarks () {
        return "";
    }
    
}
