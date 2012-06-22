package rails.game;

import java.awt.Color;
import java.util.*;

import com.google.common.collect.ImmutableList;

import rails.common.GuiDef;
import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.action.SetDividend;
import rails.game.model.*;
import rails.game.special.*;
import rails.game.state.*;
import rails.util.*;

/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies encompass all 18xx company-like entities that lay
 * tracks and run trains. <p> Ownership of companies will always be performed by
 * holding certificates. Some minor company types may have only one certificate,
 * but this will still be the form in which ownership is expressed. <p> Company
 * shares may or may not have a price on the stock market.
 * 
 * FIXME: This mechanism has to be rewritten!
 * 
 * FIXME: Check if uninitialized states may cause trouble on undo
 */
public class PublicCompany extends Company implements CashOwner, PortfolioOwner, PortfolioHolder {

    public static final int CAPITALISE_FULL = 0;

    public static final int CAPITALISE_INCREMENTAL = 1;

    public static final int CAPITALISE_WHEN_BOUGHT = 2;

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
    protected final BooleanState hasReachedDestination = BooleanState.create(this, "hasReachedDestinations");

    /** Sequence number in the array of public companies - may not be useful */
    protected int publicNumber = -1; // For internal use

    protected int numberOfBaseTokens = 0;

    protected int baseTokensBuyCost = 0;
    /** An array of base token laying costs, per successive token */
    protected int[] baseTokenLayCost;
    protected String baseTokenLayCostMethod = "sequential";

    protected final BaseTokensModel baseTokens = BaseTokensModel.create(this, "baseTokens"); // Create after cloning ?
    protected final PortfolioModel portfolio = PortfolioModel.create(this);
    
    
    /**
     * Initial (par) share price, represented by a stock market location object
     */
    protected PriceModel parPrice;

    /** Current share price, represented by a stock market location object */
    protected PriceModel currentPrice;

    /** Company treasury, holding cash */
    protected final CashMoneyModel treasury = CashMoneyModel.create(this, "treasury", false);

    /** PresidentModel */
    protected final PresidentModel presidentModel = PresidentModel.create(this);

    /** Has the company started? */
    protected final BooleanState hasStarted = BooleanState.create(this, "hasStarted");

    /** Total bonus tokens amount */
    protected final BonusModel bonusValue = BonusModel.create(this, "bonusValue");

    /** Acquires Bonus objects */
    protected final ArrayListState<Bonus> bonuses = ArrayListState.create(this, "bonuses");

    /** Most recent revenue earned. */
    protected final CashMoneyModel lastRevenue = CashMoneyModel.create(this, "lastRevenue", false);

    /** Most recent payout decision. */
    protected final StringState lastRevenueAllocation = StringState.create(this, "lastRevenueAllocation");

    /** Is the company operational ("has it floated")? */
    protected final BooleanState hasFloated = BooleanState.create(this, "hasFloated");

    /** Has the company already operated? */
    protected final BooleanState hasOperated = BooleanState.create(this, "hasOperated");

    /** Are company shares buyable (i.e. before started)? */
    protected final BooleanState buyable = BooleanState.create(this, "buyable");

    /** In-game state.
     * <p> Will only be set false if the company is closed and cannot ever be reopened.
     * By default it will be set false if a company is closed. */
    // TODO: Check if there was some assumption to be null at some place
    protected final BooleanState inGameState = BooleanState.create(this, "inGameState", true);

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
    // init during finishConfig
    
    /**
     * Number of tiles laid. Only used where more tiles can be laid in the
     * company's first OR turn.
     * It is set to -1 as this flags a special state 
     * TODO: Check if this works
     */
    protected final IntegerState extraTiles = IntegerState.create(this, "extraTiles", -1);

    /* Spendings in the current operating turn */
    protected final CashMoneyModel privatesCostThisTurn = CashMoneyModel.create(this, "privatesCostThisTurn", false);

    protected final StringState tilesLaidThisTurn = StringState.create(this, "tilesLaidThisTurn");

    protected final CashMoneyModel tilesCostThisTurn = CashMoneyModel.create(this, "tilesCostThisTurn", false); 

    protected final StringState tokensLaidThisTurn = StringState.create(this, "tokenLaidThisTurn");

    protected final CashMoneyModel tokensCostThisTurn = CashMoneyModel.create(this, "tokensCostThisTurn", false);

    protected final CashMoneyModel trainsCostThisTurn = CashMoneyModel.create(this, "trainsCostThisTurn", false);

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
    protected final ArrayListState<PublicCertificate> certificates = ArrayListState.create(this, "certificates");
    /** Are the certificates available from the first SR? */
    boolean certsAreInitiallyAvailable = true;

    /** What percentage of ownership constitutes "one share" */
    protected IntegerState shareUnit = null; // configured see below

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

    protected PrivateCompany privateToCloseOnFirstTrain = null;

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
    protected IntegerState currentNumberOfLoans = null; // init during finishConfig
    protected int loanInterestPct = 0;
    protected int maxLoansPerRound = 0;
    protected CashMoneyModel currentLoanValue = null; // init during finishConfig

    protected BooleanState canSharePriceVary;

    protected GameManager gameManager;
    protected Bank bank;
    protected StockMarket stockMarket;
    protected MapManager mapManager;
    
    /** Rights */
    protected HashMapState<String, String> rights = null;
    // created in finishConfiguration

    /**
     * The constructor. The way this class is instantiated does not allow
     * arguments.
     * FIXME: This has to be rewritten
     */
    protected PublicCompany(Item parent, String id) {
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

        if (hasStockPrice) {
            parPrice = PriceModel.create(this, "ParPrice");
            currentPrice = PriceModel.create(this, "currentPrice");
            canSharePriceVary = BooleanState.create(this, "canSharePriceVary", true);
        }
    }
    
    
    /**
     * To configure all public companies from the &lt;PublicCompany&gt; XML
     * element
     */
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        longName = tag.getAttributeAsString("longname", getId());
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
            shareUnit = IntegerState.create(this, "shareUnit", shareUnitTag.getAttributeAsInteger("percentage", DEFAULT_SHARE_UNIT));
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
            trainLimit = trainsTag.getAttributeAsIntegerArray("limit", trainLimit);
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
                    firstTrainTag.getAttributeAsString("getId()");
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

        // FIXME: The certificates mechanism has to be adopted
        // to the new structure of init etc.
        // So this is not going to work as it is now
        int certIndex = 0;
        List<Tag> certificateTags = tag.getChildren("Certificate");
        if (certificateTags != null) {
            int shareTotal = 0;
            boolean gotPresident = false;
            PublicCertificate certificate;
            // Throw away
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
                                + getId()
                                + " cannot have multiple President shares");
                    gotPresident = true;
                }

                for (int k = 0; k < number; k++) {
                    certificate = new PublicCertificate(this, "certificate", shares, president,
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
    

    public void setIndex (int index) {
        publicNumber = index;
    }

    /**
     * Final initialisation, after all XML has been processed.
     */
    public void finishConfiguration(GameManager gameManager)
    throws ConfigurationException {

        this.gameManager = gameManager;
        bank = gameManager.getBank();
        stockMarket = gameManager.getStockMarket();
        mapManager = gameManager.getMapManager();

        if (turnsWithExtraTileLaysInit != null) {
            turnsWithExtraTileLays = new HashMap<String, IntegerState>();
            for (String colour : turnsWithExtraTileLaysInit.keySet()) {
                IntegerState tileLays = IntegerState.create
                        (this, "" + colour + "_ExtraTileTurns", turnsWithExtraTileLaysInit.get(colour));
                turnsWithExtraTileLays.put(colour, tileLays);
            }
        }

       if (maxNumberOfLoans != 0) {
            currentNumberOfLoans = IntegerState.create(this, "currentNumberOfLoans");
            currentLoanValue = CashMoneyModel.create(this, "currentLoanValue", false);
            currentLoanValue.setSuppressZero(true);
        }

        if (hasStockPrice && Util.hasValue(startSpace)) {
            parPrice.setPrice(stockMarket.getStockSpace(
                    startSpace));
            if (parPrice.getPrice() == null)
                throw new ConfigurationException("Invalid start space "
                        + startSpace + " for company "
                        + getId());
            currentPrice.setPrice(parPrice.getPrice());

        }

        if (shareUnit == null) {
            shareUnit = IntegerState.create(this, "shareUnit", DEFAULT_SHARE_UNIT);
        }

        // Give each certificate an unique Id
        PublicCertificate cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(getId(), i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable()
                    && this.certsAreInitiallyAvailable);
        }

        for (int i = 0; i < numberOfBaseTokens; i++) {
            BaseToken token =  BaseToken.create(this);
            baseTokens.addFreeToken(token);
        }
        if (homeHexNames != null) {
            homeHexes = new ArrayList<MapHex>(2);
            MapHex homeHex;
            for (String homeHexName : homeHexNames.split(",")) {
                homeHex = mapManager.getHex(homeHexName);
                if (homeHex == null) {
                    throw new ConfigurationException("Invalid home hex "
                            + homeHexName
                            + " for company " + getId());
                }
                homeHexes.add (homeHex);
                infoText += "<br>Home: " + homeHex.getInfo();
            }
        }

        if (destinationHexName != null) {
            destinationHex = mapManager.getHex(destinationHexName);
            if (destinationHex == null) {
                throw new ConfigurationException("Invalid destination hex "
                        + destinationHexName
                        + " for company " + getId());
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
        
        // Can companies acquire special rights (such as in 1830 Coalfields)?
        if (specialProperties != null) {
            for (SpecialProperty sp : specialProperties) {
                if (sp instanceof SpecialRight) {
                    gameManager.setGuiParameter (GuiDef.Parm.HAS_ANY_RIGHTS, true);
                    // Initialize rights here to prevent overhead if not used, 
                    // but if rights are used, the GUI needs it from the start.
                    if (rights == null) {
                        rights = HashMapState.create(this, "rights");
                    }
                    // TODO: This is only a workaround for the missing finishConfiguration of special properties (SFY)
                    sp.finishConfiguration(gameManager);
                }
            }
        }
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
    public boolean mayBuyTrainType (Train train) {
        return true;
    }

    public boolean mustHaveOperatedToTradeShares() {
        return mustHaveOperatedToTradeShares;
    }

    public void start(StockSpace startSpace) {

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
        StockSpace startSpace = stockMarket.getStartSpace(price);
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

    public void transferAssetsFrom(PublicCompany otherCompany) {

        if (otherCompany.getCash() > 0) {
            MoneyModel.cashMove(otherCompany, this, otherCompany.getCash());
        }
        portfolio.transferAssetsFrom(otherCompany.getPortfolioModel());
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
        // TODO: Is this still required?
        getPresident().getPortfolioModel().getShareModel(this).update();

        if (sharePriceUpOnFloating) {
            stockMarket.moveUp(this);
        }

        if (homeBaseTokensLayTime == WHEN_FLOATED) {
            layHomeBaseTokens();
        }

        if (initialTrainType != null) {
            TrainManager trainManager = gameManager.getTrainManager();
            TrainCertificateType type = trainManager.getCertTypeByName(initialTrainType);
            Train train = bank.getIpo().getTrainOfType(type);
            buyTrain(train, initialTrainCost);
            train.setTradeable(initialTrainTradeable);
            trainManager.checkTrainAvailability(train, bank.getIpo().getParent());
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

        PortfolioModel shareDestination;
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
        for (PublicCertificate cert : certificates.view()) {
            // TODO: Check if this is the correct condition, portfolioModel parent change Type?
            if (cert.getPortfolio().getParent() != shareDestination.getParent()) {
                // TODO: Could this be shortened?
                shareDestination.getCertificatesModel().getPortfolio().moveInto(cert);
            }
        }

        // Any trains go to the pool (from the 1856 rules)
        // TODO: Can this be simplified?
        Portfolio.moveAll(portfolio.getTrainsModel().getPortfolio(), bank.getPool().getTrainsModel().getPortfolio());

        // Any cash goes to the bank (from the 1856 rules)
        int cash = treasury.value();
        if (cash > 0) {
            treasury.setSuppressZero(true);
            MoneyModel.cashMoveToBank(this, cash);
        }

        lastRevenue.setSuppressZero(true);
        setLastRevenue(0);

        // TODO: Check if this works correctly
        // move all laid tokens to free tokens again
        PortfolioList.moveAll(
                baseTokens.getLaidTokens(), baseTokens.getFreeTokens());
        // close company on the stock market
        stockMarket.close(this);

    }

    /** Reinitialize a company, i.e. close it and make the shares available for a new company start.
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

    public BooleanState getInGameModel () {
        return inGameState;
    }

    public BooleanState getIsClosedModel () {
        return closedObject;
    }

    /**
     * Set the company par price. <p> <i>Note: this method should <b>not</b> be
     * used to start a company!</i> Use <code><b>start()</b></code> in
     * stead.
     *
     * @param spaceI
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
    public void setCurrentSpace(StockSpace price) {
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

    public int getCash() {
        return treasury.value();
    }
    
    public CashMoneyModel getCashModel() {
        return treasury;
    }

    public String getFormattedCash() {
        return treasury.getText();
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
     *
     */
    public void nameCertificates () {
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
     * Get the company President.
     *
     */
    // FIXME: This has to be redesigned
    // Relying on the ordering is not a good thing
    public Player getPresident() {
        if (hasStarted()) {
            Owner owner = certificates.get(0).getPortfolio().getOwner();
            if (owner instanceof Player) return (Player) owner;
        }
        return null;
    }

    public PresidentModel getPresidentModel() {
        return presidentModel;
    }

    public PublicCertificate getPresidentsShare () {
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
            stockMarket.payOut(this);
        }

    }

    public boolean paysOutToTreasury (PublicCertificate cert) {

        PortfolioHolder holder = cert.getPortfolio().getParent();
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
        PortfolioHolder owner;

        for (PublicCertificate cert : certificates.view()) {
            owner = cert.getPortfolio().getParent();
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
        return shareUnit.value();
    }

    public int getShareUnitsForSharePrice() {
        return shareUnitsForSharePrice;
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
        for (PublicCertificate cert : certificates.view()) {
            // TODO: Check if this is correct, it can be
            // that this links to a PortfolioModel
            if (cert.getPortfolio().getParent() instanceof Player) {
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
                    buyer.getPortfolioModel());
            ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                    buyer.getId(),
                    getId() ));
        }
    }

    /**
     * Check if the presidency has changed for a <b>selling</b> player.
     */
    public void checkPresidencyOnSale(Player seller) {

        if (seller != getPresident()) return;

        int presShare = seller.getPortfolioModel().getShare(this);
        int presIndex = seller.getIndex();
        Player player;
        int share;
        GameManager gmgr = GameManager.getInstance();

        for (int i = presIndex + 1; i < presIndex
        + gmgr.getNumberOfPlayers(); i++) {
            player = gmgr.getPlayerByIndex(i);
            share = player.getPortfolioModel().getShare(this);
            if (share > presShare) {
                // Presidency must be transferred
                seller.getPortfolioModel().swapPresidentCertificate(this,
                        player.getPortfolioModel());
                ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                        player.getId(),
                        getId() ));
            }
        }
    }

    /** A generic presidency check. Perhaps it can replace the above two methods. */
    public void checkPresidency () {

        Player president = getPresident();
        int presIndex = president.getIndex();
        int presShare = president.getPortfolioModel().getShare(this);

        GameManager gmgr = GameManager.getInstance();
        Player player;
        int share;

        for (int i = presIndex + 1; i < presIndex
        + gmgr.getNumberOfPlayers(); i++) {
            player = gmgr.getPlayerByIndex(i);
            share = player.getPortfolioModel().getShare(this);
            if (share > presShare) {
                // Hand presidency to the first player with a higher share
                president.getPortfolioModel().swapPresidentCertificate(this,
                        player.getPortfolioModel());
                ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                        player.getId(),
                        getId() ));
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
        return 100 / shareUnit.value();
    }

    /** Get the current maximum number of trains got a given limit index. 
     * @parm index The index of the train limit step as defined for the current phase. Values start at 0.
     * <p>N.B. the new style limit steps per phase start at 1, 
     * so one must be subtracted before calling this method.
     */
    protected int getTrainLimit(int index) {
        return trainLimit[Math.min(index, trainLimit.length - 1)];
    }

    public int getCurrentTrainLimit() {
        return getTrainLimit(gameManager.getCurrentPhase().getTrainLimitIndex());
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
        // FIXME: The type of owner can be tainted as it can link to portfolioModel
        if (train.getPortfolio().getOwner() instanceof PublicCompany) {
            PublicCompany previousOwner = (PublicCompany)train.getPortfolio().getOwner();
            //  adjust the money spent on trains field
            ((CashMoneyModel)previousOwner.getTrainsSpentThisTurnModel()).change(-price);
            // pay the money to the other company
            MoneyModel.cashMove(this, previousOwner, price);
        } else { // TODO: make this a serious test, no assumption
            // else it is from the bank
            MoneyModel.cashMoveToBank(this, price);
        }

        // increase own train costs
        trainsCostThisTurn.change(price);
        // move the train to here
        portfolio.getTrainsModel().getPortfolio().moveInto(train);
        // check if a private has to be closed on first train buy
        if (privateToCloseOnFirstTrain != null
                && !privateToCloseOnFirstTrain.isClosed()) {
            privateToCloseOnFirstTrain.setClosed();
        }
    }

    public Model getTrainsSpentThisTurnModel() {
        return trainsCostThisTurn;
    }

    public void buyPrivate(PrivateCompany privateCompany, PortfolioHolder from,
            int price) {

        if (from != bank.getIpo()) {
            // The initial buy is reported from StartRound. This message should also
            // move to elsewhere.
            ReportBuffer.add(LocalText.getText("BuysPrivateFromFor",
                    getId(),
                    privateCompany.getId(),
                    from.getId(),
                    Bank.format(price) ));
        }

        // Move the private certificate
        portfolio.getPrivatesOwnedModel().getPortfolio().moveInto(privateCompany);

        // Move the money
        if (price > 0) MoneyModel.cashMove(this, (CashOwner)from, price); // TODO: Remove the cast
        privatesCostThisTurn.change(price);

        // Move any special abilities to the portfolio, if configured so
        List<SpecialProperty> sps = privateCompany.getSpecialProperties();
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
                        ((SellBonusToken)sp).setSeller(this);
                        // Also note 1 has been used
                        ((SellBonusToken)sp).setExercised();
                    }
                    spsToMoveToGM.add(sp);
                }
            }
            for (SpecialProperty sp : spsToMoveHere) {
                sp.moveTo(portfolio);
            }
            for (SpecialProperty sp : spsToMoveToGM) {
                gameManager.addSpecialProperty(sp);
                log.debug("SP "+sp.getId()+" is now a common property");
            }
        }

    }

    public Model getPrivatesSpentThisTurnModel() {
        return privatesCostThisTurn;
    }

    public void layTile(MapHex hex, Tile tile, int orientation, int cost) {

        String tileLaid =
            "#" + tile.getExternalId() + "/" + hex.getId() + "/"
            + hex.getOrientationName(orientation);
        tilesLaidThisTurn.appendWithDelimiter(tileLaid, ", ");

        if (cost > 0) tilesCostThisTurn.change(cost);

        if (extraTiles != null && extraTiles.value() > 0) {
            extraTiles.add(-1);
        }
    }

    public void layTilenNoMapMode(int cost) {
        if (cost > 0) tilesCostThisTurn.change(cost);
        tilesLaidThisTurn.appendWithDelimiter(Bank.format(cost), ",");
    }

    public StringState getTilesLaidThisTurnModel() {
        return tilesLaidThisTurn;
    }

    public Model getTilesCostThisTurnModel() {
        return tilesCostThisTurn;
    }

    public void layBaseToken(MapHex hex, int cost) {

        String tokenLaid = hex.getId();
        tokensLaidThisTurn.appendWithDelimiter(tokenLaid, ", ");
        if (cost > 0) tokensCostThisTurn.change(cost);
    }

    public void layBaseTokennNoMapMode(int cost) {
        if (cost > 0) tokensCostThisTurn.change(cost);
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

    public boolean removeBonus (String name) {
        if (bonuses != null && !bonuses.isEmpty()) {
            for(Bonus bonus : bonuses.view()) {
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
        return baseTokens.getLaidTokens().size() > 0;
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
                    // No tracks, then it doesn't matter
                    homeCityNumber = 1;
                } else {
                    // Cover the case that there already is another token.
                    // Allowing this is optional for 1856 Hamilton (THB home)
                    List<Stop> stops = homeHex.getStops();
                    List<Stop> openStops = new ArrayList<Stop>();
                    for (Stop stop : stops) {
                        if (stop.hasTokenSlotsLeft()) openStops.add (stop);
                    }
                    if (openStops.size() == 1) {
                        // Just one spot: lay the home base there.
                        homeCityNumber = openStops.get(0).getNumber();
                    } else {
                        // ??  
                        // TODO Will player be asked??
                        return false;
                    }
                }
            }
            log.debug(getId() + " lays home base on " + homeHex.getId() + " city "
                    + homeCityNumber);
            homeHex.layBaseToken(this, homeCityNumber);
        }
        return true;
    }

    public BaseToken getFreeToken() {
        if (baseTokens.getFreeTokens().size() > 0) {
            return (BaseToken)baseTokens.getFreeTokens().items().get(0);
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
    // FIXME: Create a new Token model that distinguishs between laidBaseTokens and freeBaseTokens
    // not by the holder
    public boolean addToken(Token token) {
        boolean result = false;
/*        if (token instanceof BaseToken
                && laidBaseTokens.remove(token)
                // FIXME: This is plain wrong as we cannot add the token to the view list
                && Util.addToList(freeBaseTokens.view(), token, position)) {
            token.setHolder(this);
            // TODO: Is this still required?
            this.baseTokensModel.update();
        }
        */
        return result;

    }

    public ImmutableList<Token> getAllBaseTokens() {
        ImmutableList<Token> list = ImmutableList.copyOf(baseTokens.getFreeTokens());
        list.addAll(baseTokens.getLaidTokens().items());
        return list;
    }
    
    public ImmutableList<Token> getLaidBaseTokens() {
        return baseTokens.getLaidTokens().items();
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

    public boolean hasTokens() {
        return (baseTokens.nbAllTokens() > 0);
    }

    // TODO: Check if the rewritten part below really works
    public int getNumberOfTileLays(String tileColour) {

        if (extraTileLays == null) return 1;

        Map<String, Integer> phaseMap = extraTileLays.get(tileColour);
        if (phaseMap == null || phaseMap.isEmpty()) return 1;

        Phase phase = gameManager.getPhaseManager().getCurrentPhase();
        Integer ii = phaseMap.get(phase.getName());
        if (ii == null) return 1;

        int i = ii;
        if (i > 1) {
//            if (extraTiles == null && turnsWithExtraTileLays != null) {
//                extraTiles = turnsWithExtraTileLays.get(tileColour);
//            }
//            if (extraTiles != null) {
//                if (extraTiles.value() == 0) {
//                    extraTiles = null;
//                    return 1;
//                }
//            }
// the above is replaced by
            // FIXME: Check if the rewritten part below really works
            // There is some flagging going on with setting extraTiles to null
            if (turnsWithExtraTileLays != null && extraTiles.value() == -1) {
                extraTiles.set(turnsWithExtraTileLays.get(tileColour).value());
            }
            if (extraTiles.value() == 0) {
                extraTiles.set(-1);
                return 1;
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
        return currentNumberOfLoans.value();
    }

    public int getCurrentLoanValue () {
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

    public MoneyModel getLoanValueModel () {
        return currentLoanValue;
    }
    
    public State getRightsModel () {
        return rights;
    }

    public boolean canClose() {
        return canClose;
    }
    
    public void setRight (String nameOfRight, String value) {
        if (rights == null) {
            rights = HashMapState.create(this, "rights");
        }
        rights.put(nameOfRight, value);
    }
    
    public boolean hasRight (String nameOfRight) {
        return rights != null && rights.containsKey(nameOfRight);
    }
    
    public String getRight (String nameOfRight) {
        return rights != null ? rights.get(nameOfRight) : null;
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

    /** Extra codes to be added to the president's indicator in the Game Status window.
     * Normally nothing (see 1856 CGR for an exception). */
    public String getExtraShareMarks () {
        return "";
    }
    
    /** Does the company has a route?
     * Currently this is a stub that always returns true.
     */
    public boolean hasRoute() {
        return true;
    }

    // PortfolioOwner method
    public PortfolioModel getPortfolioModel() {
        return portfolio;
    }

}
