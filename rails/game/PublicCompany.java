/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCompany.java,v 1.30 2008/02/20 22:16:37 evos Exp $ */
package rails.game;

import java.awt.Color;
import java.util.*;

import rails.game.action.SetDividend;
import rails.game.model.*;
import rails.game.move.CashMove;
import rails.game.move.Moveable;
import rails.game.state.*;
import rails.util.*;

/**
 * This class provides an implementation of a (perhaps only basic) public
 * company. Public companies emcompass all 18xx company-like entities that lay
 * tracks and run trains.
 * <p>
 * Ownership of companies will always be performed by holding certificates. Some
 * minor company types may have only one certificate, but this will still be the
 * form in which ownership is expressed.
 * <p>
 * Company shares may or may not have a price on the stock market.
 */
public class PublicCompany extends Company implements PublicCompanyI {
    protected static final int DEFAULT_SHARE_UNIT = 10;

    protected static int numberOfPublicCompanies = 0;

    // Home base token lay times
    protected static final int WHEN_STARTED = 0;
    protected static final int WHEN_FLOATED = 1;
    protected static final int START_OF_FIRST_OR = 2;

    protected static final String[] tokenLayTimeNames = new String[] {
            "whenStarted", "whenFloated", "firstOR" };

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

    /** Home hex & city * */
    protected MapHex homeHex = null;

    protected Station homeStation = null;

    /** Destination hex * */
    protected MapHex destinationHex = null;

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

    /** Has the company started? */
    protected BooleanState hasStarted = null;

    /** Total bonus tokens amount */
    protected MoneyModel bonusTokensValue = null;

    /** Most recent revenue earned. */
    protected MoneyModel lastRevenue = null;

    /** Most recent payout decision. */
    protected StringState lastRevenueAllocation;

    /** Is the company operational ("has it floated")? */
    protected BooleanState hasFloated = null;

    /** Has the company already operated? */
    protected BooleanState hasOperated = null;

    /**
     * A map per tile colour. Each entry contains a map per phase, of which each
     * value is an Integer defining the number of allowed tile lays. Only
     * numbers deviating from 1 need be specified, the default is always 1.
     */
    protected Map<String, HashMap<String, Integer>> extraTileLays = null;
    /**
     * A map per tile colour, holding the number of turns that the tile lay number applies.
     * The default number is always 1.
     */
    protected Map<String, Integer> turnsWithExtraTileLaysInit = null;
    /** Copy of turnsWithExtraTileLaysInit, per company */
    protected Map<String, IntegerState> turnsWithExtraTileLays = null;
    /** Number of tiles laid. Only used where
     * more tiles can be laid in
     * the company's first OR turn.
     */
    protected IntegerState extraTiles = null;

    /** Is the company closed (or bankrupt)? */
    protected boolean closed = false;

    /* Spendings in the current operating turn */
    protected MoneyModel privatesCostThisTurn;

    protected StringState tilesLaidThisTurn;

    protected MoneyModel tilesCostThisTurn;

    protected StringState tokensLaidThisTurn;

    protected MoneyModel tokensCostThisTurn;

    protected MoneyModel trainsCostThisTurn;

    protected boolean canBuyStock = false;

    protected boolean canBuyPrivates = false;

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

    /** Privates and Certificates owned by the public company */
    protected Portfolio portfolio;

    /** What percentage of ownership constitutes "one share" */
    protected int shareUnit = DEFAULT_SHARE_UNIT;

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

    /** Initial train at floating time */
    protected String initialTrain = null;

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
    public void configureFromXML(Tag tag) throws ConfigurationException {

        longName = tag.getAttributeAsString("longname", name);

        /* Configure public company features */
        fgHexColour = tag.getAttributeAsString("fgColour", fgHexColour);
        fgColour = new Color(Integer.parseInt(fgHexColour, 16));

        bgHexColour = tag.getAttributeAsString("bgColour", bgHexColour);
        bgColour = new Color(Integer.parseInt(bgHexColour, 16));

        floatPerc = tag.getAttributeAsInteger("floatPerc", floatPerc);

        startSpace = tag.getAttributeAsString("startspace");

        fixedPrice = tag.getAttributeAsInteger("price", 0);

        numberOfBaseTokens = tag.getAttributeAsInteger("tokens", 1);

        Tag shareUnitTag = tag.getChild("ShareUnit");
        if (shareUnitTag != null) {
            shareUnit = shareUnitTag.getAttributeAsInteger("percentage",
                    shareUnit);
        }

        Tag homeBaseTag = tag.getChild("Home");
        if (homeBaseTag != null) {
            String homeHexName = homeBaseTag.getAttributeAsString("hex");
            MapHex hex = MapManager.getInstance().getHex(homeHexName);
            if (hex != null) {
                homeHex = hex;
                List<Station> stations = hex.getStations();
                int homeCity = homeBaseTag.getAttributeAsInteger("city", 1);
                if (stations != null && stations.size() > 0) {
                    homeStation = stations.get(Math.min(homeCity,
                            stations.size()) - 1);
                }
            } else {
                throw new ConfigurationException("Invalid home hex "
                        + homeHexName + " for company " + name);
            }
        }

        Tag destinationTag = tag.getChild("Destination");
        if (destinationTag != null) {
            String destHexName = destinationTag.getAttributeAsString("hex");
            MapHex hex = MapManager.getInstance().getHex(destHexName);
            if (hex != null) {
                destinationHex = hex;
            } else {
                throw new ConfigurationException("Invalid destination hex "
                        + destHexName + " for company " + name);
            }
        }

        Tag privateBuyTag = tag.getChild("CanBuyPrivates");
        if (privateBuyTag != null) {
            canBuyPrivates = true;
            GameManager.setCanAnyCompBuyPrivates(true);
            GameManager.setCompaniesCanBuyPrivates();

            String lower = privateBuyTag.getAttributeAsString("lowerPriceFactor");
            if (!Util.hasValue(lower))
                throw new ConfigurationException(
                        "Lower private price factor missing");
            lowerPrivatePriceFactor = Float.parseFloat(lower);

            String upper = privateBuyTag.getAttributeAsString("upperPriceFactor");
            if (!Util.hasValue(upper))
                throw new ConfigurationException(
                        "Upper private price factor missing");
            upperPrivatePriceFactor = Float.parseFloat(upper);
        }

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

            payoutMustExceedPriceToMove = payoutTag.getAttributeAsBoolean(
                    "mustExceedPriceToMove", false);
        }

        Tag ownSharesTag = tag.getChild("TreasuryCanHoldOwnShares");
        if (ownSharesTag != null) {
            canHoldOwnShares = true;
            treasuryPaysOut = true;
            GameManager.setCanAnyCompanyHoldShares(true);

            maxPercOfOwnShares = ownSharesTag.getAttributeAsInteger("maxPerc",
                    maxPercOfOwnShares);
        }

        Tag trainsTag = tag.getChild("Trains");
        if (trainsTag != null) {
            trainLimit = trainsTag.getAttributeAsIntegerArray("number");
            mustOwnATrain = trainsTag.getAttributeAsBoolean("mandatory",
                    mustOwnATrain);
            initialTrain = trainsTag.getAttributeAsString("initial");
        }

        Tag firstTrainTag = tag.getChild("FirstTrainCloses");
        if (firstTrainTag != null) {
            String typeName = firstTrainTag.getAttributeAsString("type",
                    "Private");
            if (typeName.equalsIgnoreCase("Private")) {
                privateToCloseOnFirstTrainName = firstTrainTag.getAttributeAsString("name");
            } else {
                throw new ConfigurationException(
                        "Only Privates can be closed on first train buy");
            }
        }

        Tag capitalisationTag = tag.getChild("Capitalisation");
        if (capitalisationTag != null) {
            String capType = capitalisationTag.getAttributeAsString("type",
                    "full");
            if (capType.equalsIgnoreCase("full")) {
                setCapitalisation(CAPITALISE_FULL);
            } else if (capType.equalsIgnoreCase("incremental")) {
                setCapitalisation(CAPITALISE_INCREMENTAL);
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

	            int validForTurns = numberTag.getAttributeAsInteger("occurrences", 0);

	            String[] colours = colourString.split(",");
	            HashMap<String, Integer> phaseMap;
	            /**
	             * TODO: should not be necessary to specify all phases separately
	             */
	            String[] phases = phaseString.split(",");
	            for (int i = 0; i < colours.length; i++) {
	                if (extraTileLays == null)
	                    extraTileLays = new HashMap<String, HashMap<String, Integer>>();
	                extraTileLays.put(colours[i],
	                        (phaseMap = new HashMap<String, Integer>()));
	                for (int k = 0; k < phases.length; k++) {
	                    phaseMap.put(phases[k], lays);
	                }
		            //phaseMap.put("turns", validForTurns);
	                if (validForTurns > 0) {
	                	if (turnsWithExtraTileLaysInit == null) {
	                		turnsWithExtraTileLaysInit = new HashMap<String, Integer>();
	                	}
	                	turnsWithExtraTileLaysInit.put (colours[i], validForTurns);
	                }
	            }
        	}
        }

        if (hasParPrice)
            GameManager.setHasAnyParPrice(true);

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

                boolean president = "President".equals(certificateTag.getAttributeAsString(
                        "type", ""));
                int number = certificateTag.getAttributeAsInteger("number", 1);

                if (president) {
                    if (number > 1 || gotPresident)
                        throw new ConfigurationException("Company type " + name
                                + " cannot have multiple President shares");
                    gotPresident = true;
                }

                for (int k = 0; k < number; k++) {
                    certificate = new PublicCertificate(shares, president);
                    addCertificate(certificate);
                    shareTotal += shares * shareUnit;
                }
            }
            if (shareTotal != 100)
                throw new ConfigurationException("Company type " + name
                        + " total shares is not 100%");
        }

		// BaseToken
		Tag baseTokenTag = tag.getChild("BaseTokens");
		if (baseTokenTag != null) {

			// Cost of laying a token
			Tag layCostTag = baseTokenTag.getChild("LayCost");
			if (layCostTag != null) {
				baseTokenLayCostMethod = layCostTag.getAttributeAsString("method", baseTokenLayCostMethod);
				// Must validate the cost method!

				baseTokenLayCost = layCostTag.getAttributeAsIntegerArray("cost");
			}

			/* Cost of buying a token (mutually exclusive with laying cost) */
			Tag buyCostTag = baseTokenTag.getChild("BuyCost");
			if (buyCostTag != null) {
				baseTokensBuyCost = floatTag.getAttributeAsInteger(
	                "initialTokenCost", 0);
			}

	        Tag tokenLayTimeTag = baseTokenTag.getChild("HomeBase");
	        if (tokenLayTimeTag != null) {
	        	// When is the home base laid?
	        	// Note: if not before, home tokens are in any case laid
	        	// at the start of the first OR
	            String layTimeString = tokenLayTimeTag.getAttributeAsString("lay");
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
            mustHaveOperatedToTradeShares = sellSharesTag.getAttributeAsBoolean(
                    "mustHaveOperated", mustHaveOperatedToTradeShares);
        }
    }

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);
        if (!name.equals(""))
            this.publicNumber = numberOfPublicCompanies++;

        this.portfolio = new Portfolio(name, this);
        treasury = new CashModel(this);
        lastRevenue = new MoneyModel(name + "_lastRevenue");
        lastRevenue.setOption(MoneyModel.SUPPRESS_INITIAL_ZERO);
        lastRevenueAllocation = new StringState(name + "_lastAllocation");
        baseTokensModel = new BaseTokensModel(this);

        hasStarted = new BooleanState(name + "_hasStarted", false);
        hasFloated = new BooleanState(name + "_hasFloated", false);
        hasOperated = new BooleanState(name + "_hasOperated", false);

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
        trainsCostThisTurn.setOption(MoneyModel.SUPPRESS_ZERO);
        bonusTokensValue = new MoneyModel(name + "_bonusValue");
        bonusTokensValue.setOption(MoneyModel.SUPPRESS_ZERO
                + MoneyModel.ADD_PLUS);
        if (hasStockPrice) {
            parPrice = new PriceModel(this, name + "_ParPrice");
            currentPrice = new PriceModel(this, name + "_CurrentPrice");
        }

        if (turnsWithExtraTileLaysInit != null) {
        	turnsWithExtraTileLays = new HashMap<String, IntegerState>();
        	for (String colour : turnsWithExtraTileLaysInit.keySet()) {
        		turnsWithExtraTileLays.put(colour,
        				new IntegerState (name+"_"+colour+"_ExtraTileTurns",
        						turnsWithExtraTileLaysInit.get(colour)));
        	}
        }

        PublicCompanyI dummyCompany = (PublicCompanyI) type.getDummyCompany();
        if (dummyCompany != null) {
            fgHexColour = dummyCompany.getHexFgColour();
            bgHexColour = dummyCompany.getHexBgColour();
        }

    }

    /**
     * Final initialisation, after all XML has been processed.
     */
    public void init2() throws ConfigurationException {
        if (hasStockPrice && Util.hasValue(startSpace)) {
            parPrice.setPrice(StockMarket.getInstance().getStockSpace(
                    startSpace));
            if (parPrice.getPrice() == null)
                throw new ConfigurationException("Invalid start space "
                        + startSpace + "for company " + name);
            currentPrice.setPrice(parPrice.getPrice());

        }

        // Give each certificate an unique Id
        for (int i = 0; i < certificates.size(); i++) {
            certificates.get(i).setUniqueId(name, i);
        }

        BaseToken token;
        for (int i = 0; i < numberOfBaseTokens; i++) {
            token = new BaseToken(this);
            allBaseTokens.add(token);
            freeBaseTokens.add(token);
        }

    }

    /** Reset turn objects */
    public void initTurn() {

        if (!hasLaidHomeBaseTokens())
            layHomeBaseTokens();

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
     * @return Returns the homeHex.
     */
    public MapHex getHomeHex() {
        return homeHex;
    }

    /**
     * @param homeHex
     *            The homeHex to set.
     */
    public void setHomeHex(MapHex homeHex) {
        this.homeHex = homeHex;
    }

    /**
     * @return Returns the homeStation.
     */
    public Station getHomeStation() {
        return homeStation;
    }

    /**
     * @param homeStation
     *            The homeStation to set.
     */
    public void setHomeStation(Station homeStation) {
        this.homeStation = homeStation;
    }

    /**
     * @return Returns the destinationHex.
     */
    public MapHex getDestinationHex() {
        return destinationHex;
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

    public boolean mustHaveOperatedToTradeShares() {
        return mustHaveOperatedToTradeShares;
    }

    public void start(StockSpaceI startSpace) {
        hasStarted.set(true);
        setParPrice(startSpace);
        // The current price is set via the Stock Market
        StockMarket.getInstance().start(this, startSpace);

        /* Final initialisations */
        if (Util.hasValue(privateToCloseOnFirstTrainName)) {
            privateToCloseOnFirstTrain = Game.getCompanyManager().getPrivateCompany(
                    privateToCloseOnFirstTrainName);
        }

        if (homeBaseTokensLayTime == WHEN_STARTED) {
            layHomeBaseTokens();
        }
   }

    public void start(int price) {
        StockSpaceI startSpace = StockMarket.getInstance().getStartSpace(price);
        if (startSpace == null) {
            log.error("Invalid start price " + Bank.format(price));
        } else {
            start(startSpace);
        }

    }

    /**
     * Start a company with a fixed par price.
     */
    public void start() {
        hasStarted.set(true);
        if (hasStockPrice && parPrice.getPrice() != null) {
            // setCurrentPrice (parPrice.getPrice());
            // The current price is set via the Stock Market
            StockMarket.getInstance().start(this, parPrice.getPrice());
        }

        if (homeBaseTokensLayTime == WHEN_STARTED) {
            layHomeBaseTokens();
        }

    }

    public void transferAssetsFrom (PublicCompanyI otherCompany) {

        new CashMove (otherCompany, this, otherCompany.getCash());
        portfolio.transferAssetsFrom (otherCompany.getPortfolio());
    }

    /**
     * @return Returns true is the company has started.
     */
    public boolean hasStarted() {
        return hasStarted.booleanValue();
    }

    /**
     * Float the company, put its initial cash in the treasury.
     */
    public void setFloated(boolean moveCash) {

        hasFloated.set(true);

        // Remove the "unfloated" indicator in GameStatus
        getPresident().getPortfolio().getShareModel(this).update();

        // In 18EU, cash has already been moved
        if (moveCash) {

	        int cash = 0;
	        if (hasStockPrice) {
	            int capFactor = 0;
	            if (capitalisation == CAPITALISE_FULL) {
	                capFactor = 100 / shareUnit;
	            } else if (capitalisation == CAPITALISE_INCREMENTAL) {
	                // TODO Should be: 100% - percentage still in IPO
	                capFactor = percentageOwnedByPlayers() / shareUnit;
	            }
	            int price = (hasParPrice ? getParPrice() : getCurrentPrice()).getPrice();
	            cash = capFactor * price;
	        } else {
	            cash = fixedPrice;
	        }

	        if (baseTokensBuyCost > 0)
	            cash -= baseTokensBuyCost;

	        new CashMove(Bank.getInstance(), this, cash);
	        ReportBuffer.add(LocalText.getText("FloatsWithCash",
	        		new String[] {
	        		name,
	                Bank.format(cash)
	                }));

	        if (capitalisation == CAPITALISE_INCREMENTAL && canHoldOwnShares) {
	            List<Certificate> moving = new ArrayList<Certificate>();
	            for (Certificate ipoCert : Bank.getIpo().getCertificatesPerCompany(
	                    name)) {
	                moving.add(ipoCert);
	            }
	            for (Certificate ipoCert : moving) {
	                ipoCert.moveTo(portfolio);
	            }
	        }
        } else {
        	ReportBuffer.add(LocalText.getText("Floats", name));
        }

        if (sharePriceUpOnFloating) {
            Game.getStockMarket().moveUp(this);
        }

        if (homeBaseTokensLayTime == WHEN_FLOATED) {
            layHomeBaseTokens();
        }

        if (initialTrain != null) {
            TrainI train = Bank.getIpo().getTrainOfType(initialTrain);
            buyTrain(train, 0);
    		TrainManager.get().checkTrainAvailability(train, Bank.getIpo());
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

    public void setOperated(boolean value) {
        hasOperated.set(value);
    }

    @Override
    public void setClosed() {
        super.setClosed();
        for (PublicCertificateI cert : certificates) {
            cert.moveTo(Bank.getScrapHeap());
        }
        lastRevenue.setOption(MoneyModel.SUPPRESS_ZERO);
        setLastRevenue(0);
        treasury.setOption(CashModel.SUPPRESS_ZERO);
        treasury.update();

        Util.moveObjects(laidBaseTokens, this);
    }
    /**
     * Set the company par price.
     * <p>
     * <i>Note: this method should <b>not</b> be used to start a company!</i>
     * Use <code><b>start()</b></code> in stead.
     *
     * @param spaceI
     */
    public void setParPrice(StockSpaceI space) {
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
     *         the stock chart.
     */
    public StockSpaceI getParPrice() {
        if (hasParPrice) {
            return parPrice != null ? parPrice.getPrice() : null;
        } else {
            return currentPrice != null ? currentPrice.getPrice() : null;
        }
    }

    /**
     * Set a new company price.
     *
     * @param price
     *            The StockSpace object that defines the new location on the
     *            stock market.
     */
    public void setCurrentPrice(StockSpaceI price) {
        if (price != null) {
            currentPrice.setPrice(price);
        }
    }

    public PriceModel getCurrentPriceModel() {
        return currentPrice;
    }

    public PriceModel getParPriceModel() {
        // Temporary fix to satisfy GameStatus window. Should be removed there.
        if (parPrice == null)
            return currentPrice;

        return parPrice;
    }

    /**
     * Get the current company share price.
     *
     * @return The StockSpace object that defines the current location on the
     *         stock market.
     */
    public StockSpaceI getCurrentPrice() {
        return currentPrice != null ? currentPrice.getPrice() : null;
    }

    /**
     * Add a given amount to the company treasury.
     *
     * @param amount
     *            The amount to add (may be negative).
     */
    public boolean addCash(int amount) {
        return treasury.addCash(amount);
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
    public static int getNumberOfPublicCompanies() {
        return numberOfPublicCompanies;
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
     *         share).
     */
    public List<PublicCertificateI> getCertificates() {
        return certificates;
    }

    /**
     * Assign a predefined list of certificates to this company. The list is
     * deep cloned.
     *
     * @param list
     *            ArrayList containing the certificates.
     */
    public void setCertificates(List<PublicCertificateI> list) {
        certificates = new ArrayList<PublicCertificateI>();
        PublicCertificateI cert2;
        for (PublicCertificateI cert : list) {
            cert2 = cert.copy();
            certificates.add(cert2);
            cert2.setCompany(this);
            // TODO Questionable if it should be put in IPO or in
            // Unavailable.
        }
    }

    /**
     * Add a certificate to the end of this company's list of certificates.
     *
     * @param certificate
     *            The certificate to add.
     */
    public void addCertificate(PublicCertificateI certificate) {
        if (certificates == null)
            certificates = new ArrayList<PublicCertificateI>();
        certificates.add(certificate);
        certificate.setCompany(this);
    }

    /**
     * Get the Portfolio of this company, containing all privates and
     * certificates owned..
     *
     * @return The Portfolio of this company.
     */
    public Portfolio getPortfolio() {
        return portfolio;
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

    public boolean isAvailable () {
    	Portfolio presLoc = certificates.get(0).getPortfolio();
    	return presLoc != Bank.getUnavailable()
    			&& presLoc != Bank.getScrapHeap();
    }

    /**
     * Store the last revenue earned by this company.
     *
     * @param i
     *            The last revenue amount.
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
     * Pay out a given amount of revenue (and store it). The amount is
     * distributed to all the certificate holders, or the "beneficiary" if
     * defined (e.g.: BankPool shares may pay to the company).
     *
     * @param The
     *            revenue amount.
     */
    /*
     * public void payOut(int amount) {
     *
     * distributePayout(amount); // Move the token if (hasStockPrice &&
     * (!payoutMustExceedPriceToMove || amount >=
     * currentPrice.getPrice().getPrice())) Game.getStockMarket().payOut(this); } /*
     *
     * /** Split a dividend. TODO Optional rounding down the payout
     *
     * @param amount
     */
    public void splitRevenue(int amount) {

        // setLastRevenue(amount);

        if (amount > 0) {
            // Withhold half of it
            // For now, hardcode the rule that payout is rounded up.
            int withheld = (amount / (2 * getNumberOfShares()))
                    * getNumberOfShares();
            new CashMove(null, this, withheld);
            ReportBuffer.add(name + " receives " + Bank.format(withheld));

            // Payout the remainder
            int payed = amount - withheld;
            payout(payed);
        }

    }

    /**
     * Distribute the dividend amongst the shareholders.
     *
     * @param amount
     */
    public void payout(int amount) {

        if (amount == 0)
            return;

        int part;
        Map<CashHolder, Integer> split = new HashMap<CashHolder, Integer>();

        for (PublicCertificateI cert : certificates) {
            CashHolder recipient = getBeneficiary(cert);
            part = amount * cert.getShares() * shareUnit / 100;
            // For reporting, we want to add up the amounts per recipient
            if (split.containsKey(recipient)) {
                part += (split.get(recipient)).intValue();
            }
            split.put(recipient, new Integer(part));
        }
        // Report and add the cash
        for (CashHolder recipient : split.keySet()) {
            if (recipient instanceof Bank)
                continue;
            part = (split.get(recipient)).intValue();
            ReportBuffer.add(recipient.getName() + " receives "
                    + Bank.format(part));
            new CashMove(null, recipient, part);
        }

        // Move the token
        if (hasStockPrice
                && (!payoutMustExceedPriceToMove || amount >= currentPrice.getPrice().getPrice())) {
            Game.getStockMarket().payOut(this);
        }

    }

    /** Who gets the per-share revenue? */
    protected CashHolder getBeneficiary(PublicCertificateI cert) {

        Portfolio holder = cert.getPortfolio();
        CashHolder beneficiary = holder.getOwner();
        // Special cases apply if the holder is the IPO or the Pool
        if (holder == Bank.getIpo() && ipoPaysOut || holder == Bank.getPool()
                && poolPaysOut) {
            beneficiary = this;
        }
        // log.debug("Cert "+cert.getName()+"owned by "+holder.getName()+"
        // benefits "+beneficiary.getName()
        // +" (IPO:"+ipoPaysOut+" Pool:"+poolPaysOut+")");
        return beneficiary;
    }

    /**
     * Withhold a given amount of revenue (and store it).
     *
     * @param The
     *            revenue amount.
     */
    public void withhold(int amount) {
        if (amount > 0)
            new CashMove(null, this, amount);
        // Move the token
        if (hasStockPrice)
            Game.getStockMarket().withhold(this);
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

    /**
     * Get the unit of share.
     *
     * @return The percentage of ownership that is called "one share".
     */
    public int getShareUnit() {
        return shareUnit;
    }

    @Override
    public String toString() {
        return name + ", " + publicNumber + " of " + numberOfPublicCompanies;
    }

    public static boolean startCompany(String playerName, String companyName,
            StockSpace startSpace) {
        // TODO: Should probably do error checking in case names aren't found.
        Player player = Game.getPlayerManager().getPlayerByName(playerName);
        PublicCompany company = (PublicCompany) Game.getCompanyManager().getPublicCompany(
                companyName);

        PublicCertificate cert = (PublicCertificate) company.certificates.get(0);

        if (player.getCash() >= (startSpace.getPrice() * (cert.getShare() / company.getShareUnit()))) {
            company.start(startSpace);
            int price = startSpace.getPrice()
                    * (cert.getShare() / company.getShareUnit());
            player.buyShare(cert, price);

            return true;
        } else
            return false;
    }

    /**
     * @return Returns the lowerPrivatePriceFactor.
     */
    public float getLowerPrivatePriceFactor() {
        return lowerPrivatePriceFactor;
    }

    /**
     * @return Returns the upperPrivatePriceFactor.
     */
    public float getUpperPrivatePriceFactor() {
        return upperPrivatePriceFactor;
    }

    public boolean hasStockPrice() {
        return hasStockPrice;
    }

    public boolean hasParPrice() {
        return hasParPrice;
    }

    public int getFixedPrice() {
        return fixedPrice;
    }

    public int percentageOwnedByPlayers() {
        int share = 0;
        for (PublicCertificateI cert : certificates) {
            if (cert.getPortfolio().getOwner() instanceof Player) {
                share += cert.getShare();
            }
        }
        return share;
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
     * @param buyer
     *            Player who has just bought a certificate.
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
            ReportBuffer.add(LocalText.getText("PresidencyIsTransferredTo",
                    new String[] { name, buyer.getName() }));
        }
    }

    /**
     * Check if the presidency has changed for a <b>selling</b> player.
     */
    public void checkPresidencyOnSale(Player seller) {

        if (seller != getPresident())
            return;

        int presShare = seller.getPortfolio().getShare(this);
        int presIndex = seller.getIndex();
        Player player;
        int share;

        for (int i = presIndex + 1; i < presIndex
                + GameManager.getNumberOfPlayers(); i++) {
            player = GameManager.getPlayer(i);
            share = player.getPortfolio().getShare(this);
            if (share > presShare) {
                // Presidency must be transferred
                seller.getPortfolio().swapPresidentCertificate(this,
                        player.getPortfolio());
                ReportBuffer.add(LocalText.getText("PresidencyIsTransferredTo",
                        new String[] { name, player.getName() }));
            }
        }
    }

    /** Only usable if the float percentage is fixed.
     * Games where the percentage varies must check this
     * in StockRound and possibly StartRound.
     */
    public void checkFlotation(boolean moveCash) {
        if (hasStarted() && !hasFloated()
                && (Bank.getIpo().getShare(this)
                		+ portfolio.getShare(this)) <= 100 - floatPerc) {
            // Float company (limit and capitalisation to be made
            // configurable)
            setFloated(moveCash);
        }
    }

    /**
     * @return Returns the capitalisation.
     */
    public int getCapitalisation() {
        return capitalisation;
    }

    /**
     * @param capitalisation
     *            The capitalisation to set.
     */
    public void setCapitalisation(int capitalisation) {
        log.debug("Capitalisation=" + capitalisation);
        this.capitalisation = capitalisation;
    }

    public int getNumberOfShares() {
        return 100 / shareUnit;
    }

    public int getTrainLimit(int phaseIndex) {
        return trainLimit[Math.min(phaseIndex, trainLimit.length - 1)];
    }

    public int getCurrentTrainLimit() {
        return getTrainLimit(GameManager.getCurrentPhase().getIndex());
    }

    public boolean mayBuyTrains() {

        return portfolio.getNumberOfTrains() < getCurrentTrainLimit();
    }

    /**
     * Must be called in stead of Portfolio.buyTrain if side-effects can occur.
     */
    public void buyTrain(TrainI train, int price) {
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
        portfolio.buyPrivate(privateCompany, from, price);

        privatesCostThisTurn.add(price);
    }

    public ModelObject getPrivatesSpentThisTurnModel() {
        return privatesCostThisTurn;
    }

    public void layTile(MapHex hex, TileI tile, int orientation, int cost) {

        String tileLaid = "#" + tile.getExternalId() + "/" + hex.getName()
                + "/" + MapHex.getOrientationName(orientation);
        tilesLaidThisTurn.appendWithDelimiter(tileLaid, ", ");

        if (cost > 0)
            tilesCostThisTurn.add(cost);

        if (extraTiles != null && extraTiles.intValue() > 0) {
            extraTiles.add(-1);
        }
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
        if (cost > 0)
            tokensCostThisTurn.add(cost);
    }

	/**
	 * Calculate the cost of laying a token. Currently hardcoded for the
	 * "sequence" method. The other token layong costing methods will be
	 * implemented later.
	 *
	 * @param index
	 *            The sequence number of the token that the company is laying.
	 * @return The cost of laying that token.
	 */
	public int getBaseTokenLayCost()
	{

		if (baseTokenLayCost == null) return 0;

		int index = getNumberOfLaidBaseTokens();

		if (index >= baseTokenLayCost.length)
		{
			index = baseTokenLayCost.length - 1;
		}
		else if (index < 0)
		{
			index = 0;
		}
		return baseTokenLayCost[index];
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

    public void layBonusToken(MapHex hex, int cost, BonusToken token) {
        // TODO for now we only add the bonus value.
        // We must be prepared though, that tokens may be removed later.
        bonusTokensValue.add(token.getValue());

    }

    public void removeBonusToken(BonusToken token) {
        bonusTokensValue.add(-token.getValue());
    }

    public MoneyModel getBonusTokensModel() {
        return bonusTokensValue;
    }

    public boolean hasLaidHomeBaseTokens() {
        return laidBaseTokens.size() > 0;
    }

    public boolean layHomeBaseTokens() {

        // TODO Assume for now that companies have only one home base.
        // This is not true in 1841!
        // TODO This does not yet cover cases where the user
        // has a choice, such in 1830 Erie.
    	if (hasLaidHomeBaseTokens()) return true;

        return homeHex.layBaseToken(this, homeStation);
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

    public boolean addToken(TokenI token) {

        boolean result = false;
        if (token instanceof BaseToken && laidBaseTokens.remove(token)) {
            token.setHolder(this);
            result = freeBaseTokens.add(token);
            this.baseTokensModel.update();
        }
        return result;

    }

    /*
     * public List<TokenI> getLaidBaseTokens() { return laidBaseTokens; }
     */

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

    public boolean addObject(Moveable object) {
        if (object instanceof TokenI) {
            return addToken((TokenI) object);
        } else {
            return false;
        }
    }

    public boolean removeObject(Moveable object) {
        if (object instanceof BaseToken) {
            return removeToken((TokenI) object);
        } else if (object instanceof BonusToken) {
            removeBonusToken((BonusToken) object);
            return true;
        } else {
            return false;
        }
    }

    public int getNumberOfTileLays(String tileColour) {

        if (extraTileLays == null)
            return 1;

        Map<String, Integer> phaseMap = extraTileLays.get(tileColour);
        if (phaseMap == null || phaseMap.isEmpty())
            return 1;

        PhaseI phase = PhaseManager.getInstance().getCurrentPhase();
        Integer ii = phaseMap.get(phase.getName());
        if (ii == null)
            return 1;

        int i = ii;
        if (i > 1) {
        	if (extraTiles == null
        				&& turnsWithExtraTileLays != null) {
        			extraTiles = turnsWithExtraTileLays.get(tileColour);
       		}
        	if (extraTiles != null) {
        		if (extraTiles.intValue() == 0)	{
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
}
