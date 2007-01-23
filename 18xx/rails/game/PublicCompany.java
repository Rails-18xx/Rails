package rails.game;


import java.awt.Color;
import java.util.*;

import org.w3c.dom.*;

import rails.game.model.BaseTokensModel;
import rails.game.model.CashModel;
import rails.game.model.ModelObject;
import rails.game.model.MoneyModel;
import rails.game.model.PriceModel;
import rails.game.move.CashMove;
import rails.game.move.MoveSet;
import rails.game.move.StateChange;
import rails.game.state.StateObject;
import rails.util.LocalText;
import rails.util.Util;
import rails.util.XmlUtils;


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
public class PublicCompany extends Company implements PublicCompanyI
{
	protected static final int DEFAULT_SHARE_UNIT = 10;

	protected static int numberOfPublicCompanies = 0;

	/**
	 * Foreground (i.e. text) colour of the company tokens (if pictures are not
	 * used)
	 */
	protected Color fgColour;

	/** Hexadecimal representation (RRGGBB) of the foreground colour. */
	protected String fgHexColour;

	/** Background colour of the company tokens */
	protected Color bgColour;

	/** Hexadecimal representation (RRGGBB) of the background colour. */
	protected String bgHexColour;
	
	/** Home hex & city **/
	protected MapHex homeHex = null;
	protected Station homeStation = null;
	
	/** Destination hex **/
	protected MapHex destinationHex = null;	

	/** Sequence number in the array of public companies - may not be useful */
	protected int publicNumber = -1; // For internal use

	/** @deprecated */
	//protected ArrayList baseTokenedHexes;
	protected List allBaseTokens;
	protected List freeBaseTokens;
	protected List laidBaseTokens;
	//protected boolean hasPlayedTokens = false;
	protected int numberOfBaseTokens = 0;
	//protected int maxCityTokens = 0;
	protected BaseTokensModel baseTokensModel; // Create after cloning
	
	/** Initial (par) share price, represented by a stock market location object */
	protected PriceModel parPrice = null;

	/** Current share price, represented by a stock market location object */
	// protected StockSpaceI currentPrice = null;
	protected PriceModel currentPrice = null;

	/** Company treasury, holding cash */
	protected CashModel treasury = null;

	/** Has the company started? */
	protected StateObject hasStarted = null;

	/** Revenue earned in the company's previous operating turn. */
	protected MoneyModel lastRevenue = null;

	/** Is the company operational ("has it floated")? */
	protected StateObject hasFloated = null;
	
	/** Has the company already operated? */
	protected boolean hasOperated = false;

	/** Is the company closed (or bankrupt)? */
	protected boolean closed = false;

	protected boolean canBuyStock = false;

	protected boolean canBuyPrivates = false;

	/** Minimum price for buying privates, to be multiplied by the original price */
	protected float lowerPrivatePriceFactor;

	/** Maximum price for buying privates, to be multiplied by the original price */
	protected float upperPrivatePriceFactor;

	protected boolean ipoPaysOut = false;

	protected boolean poolPaysOut = false;

	/** The certificates of this company (minimum 1) */
	protected ArrayList certificates;

	/** Privates and Certificates owned by the public company */
	protected Portfolio portfolio;

	/** What percentage of ownership constitutes "one share" */
	protected int shareUnit = DEFAULT_SHARE_UNIT;

	/** At what percentage sold does the company float */
	protected int floatPerc = 0;

	/** Does the company have a stock price (minors often don't) */
	protected boolean hasStockPrice = true;

	/** Does the company have a par price? */
	protected boolean hasParPrice = true;

	protected boolean splitAllowed = false;

	/** Is the revenue always split (typical for non-share minors) */
	protected boolean splitAlways = false;
	
	/** A map per tile colour. 
	 * Each entry contains a map per phase,
	 * of which each value is an Integer defining the number of allowed tile lays.
	 * Only numbers deviating from 1 need be specified,
	 * the default is always 1.
	 */
	protected Map numberOfTileLays = null;

	/*---- variables needed during initialisation -----*/
	protected String startSpace = null;

	protected int capitalisation = CAPITALISE_FULL;

	/** Fixed price (for a 1835-style minor) */
	protected int fixedPrice = 0;

	/** Train limit per phase (index) */
	protected int[] trainLimit = new int[0];

	/** Private to close if first train is bought */
	protected String privateToCloseOnFirstTrain = null;
	
	/** Must the company own a train */
	protected boolean mustOwnATrain = true;

	/**
	 * The constructor. The way this class is instantiated does not allow
	 * arguments.
	 */
	public PublicCompany()
	{
		super();
	}

	/** Initialisation, to be called directly after instantiation (cloning)*/
	public void init(String name, CompanyTypeI type)
	{
		super.init(name, type);
		if (!name.equals(""))
			this.publicNumber = numberOfPublicCompanies++;

		this.portfolio = new Portfolio(name, this);
		treasury = new CashModel(this);
		lastRevenue = new MoneyModel (this);
		baseTokensModel = new BaseTokensModel (this);

	    hasStarted = new StateObject ("HasStarted", Boolean.FALSE);
	    hasFloated = new StateObject ("HasFloated", Boolean.FALSE);
		
		allBaseTokens = new ArrayList();
		freeBaseTokens = new ArrayList();
		laidBaseTokens = new ArrayList();

}

	/**
	 * Final initialisation, after all XML has been processed.
	 */
	public void init2() throws ConfigurationException
	{
		if (hasStockPrice && Util.hasValue(startSpace))
		{
			parPrice.setPrice(
			        StockMarket.getInstance().getStockSpace(startSpace));
			if (parPrice.getPrice() == null)
				throw new ConfigurationException("Invalid start space "
						+ startSpace + "for company " + name);
			currentPrice.setPrice(parPrice.getPrice());
			
		}
		
		BaseToken token;
		for (int i=0; i<numberOfBaseTokens; i++) {
		    token = new BaseToken (this);
		    allBaseTokens.add (token);
		    freeBaseTokens.add (token);
		}

	}

	/**
	 * To configure all public companies from the &lt;PublicCompany&gt; XML
	 * element
	 */
	public void configureFromXML(Element element) throws ConfigurationException
	{
		NamedNodeMap nnp = element.getAttributes();
		NamedNodeMap nnp2;

		/* Configure public company features */
		fgHexColour = XmlUtils
				.extractStringAttribute(nnp, "fgColour", "FFFFFF");
		fgColour = new Color(Integer.parseInt(fgHexColour, 16));

		bgHexColour = XmlUtils
				.extractStringAttribute(nnp, "bgColour", "000000");
		bgColour = new Color(Integer.parseInt(bgHexColour, 16));

		floatPerc = XmlUtils.extractIntegerAttribute(nnp, "floatPerc",
				floatPerc);

		startSpace = XmlUtils.extractStringAttribute(nnp, "startspace");

		fixedPrice = XmlUtils.extractIntegerAttribute(nnp, "price", 0);

		numberOfBaseTokens = XmlUtils.extractIntegerAttribute(nnp, "tokens", 0);

		if (element != null)
		{
			NodeList properties = element.getChildNodes();

			for (int j = 0; j < properties.getLength(); j++)
			{

				String propName = properties.item(j).getNodeName();
				if (propName == null || propName.equals("#text"))
					continue;
				if (propName.equalsIgnoreCase("ShareUnit"))
				{
					shareUnit = XmlUtils.extractIntegerAttribute(properties
							.item(j).getAttributes(), "percentage", shareUnit);
				}
				else if (propName.equalsIgnoreCase("Home"))
				{
					nnp2 = properties.item(j).getAttributes();
				    String homeName = XmlUtils.extractStringAttribute(nnp2,
						"hex");
				    MapHex hex = MapManager.getInstance().getHex(homeName);
				    if (hex != null) {
				        homeHex = hex;
				        List stations = hex.getStations();
				        int homeCity = XmlUtils.extractIntegerAttribute(nnp2,
				                "city", 1);
				        if (stations != null && stations.size() > 0) {
				            homeStation = (Station) stations.get(Math.min(homeCity, stations.size()) - 1);
				        }
				    } else {
				        throw new ConfigurationException ("Invalid home hex "
				                + homeName + " for company "+name);
				    }
				}
				else if (propName.equalsIgnoreCase("Destination"))
				{
					nnp2 = properties.item(j).getAttributes();
				    String destName = XmlUtils.extractStringAttribute(nnp2,
						"hex");
				    MapHex hex = MapManager.getInstance().getHex(destName);
				    if (hex != null) {
				        destinationHex = hex;
				    } else {
				        throw new ConfigurationException ("Invalid destination hex "
				                + destName + " for company "+name);
				    }
				}
				else if (propName.equalsIgnoreCase("CanBuyPrivates"))
				{
					canBuyPrivates = true;
					GameManager.setCanAnyCompBuyPrivates(true);
					GameManager.setCompaniesCanBuyPrivates();
					nnp2 = properties.item(j).getAttributes();
					String lower = XmlUtils.extractStringAttribute(nnp2,
							"lowerPriceFactor");
					if (!Util.hasValue(lower))
						throw new ConfigurationException(
								"Lower private price factor missing");
					lowerPrivatePriceFactor = Float.parseFloat(lower);
					String upper = XmlUtils.extractStringAttribute(nnp2,
							"upperPriceFactor");
					if (!Util.hasValue(upper))
						throw new ConfigurationException(
								"Upper private price factor missing");
					upperPrivatePriceFactor = Float.parseFloat(upper);

				}
				else if (propName.equalsIgnoreCase("PoolPaysOut"))
				{
					poolPaysOut = true;
				}
				else if (propName.equalsIgnoreCase("IPOPaysOut"))
				{
					ipoPaysOut = true;
				}
				else if (propName.equalsIgnoreCase("Float") && floatPerc == 0)
				{
					nnp2 = properties.item(j).getAttributes();
					floatPerc = XmlUtils.extractIntegerAttribute(nnp2,
							"percentage", floatPerc);
				}
				else if (propName.equalsIgnoreCase("StockPrice"))
				{
					nnp2 = properties.item(j).getAttributes();
					hasStockPrice = XmlUtils.extractBooleanAttribute(nnp2,
							"market", true);
					hasParPrice = XmlUtils.extractBooleanAttribute(nnp2, "par",
							hasStockPrice);
				}
				else if (propName.equalsIgnoreCase("Payout"))
				{
					nnp2 = properties.item(j).getAttributes();
					String split = XmlUtils.extractStringAttribute(nnp2,
							"split", "no");
					splitAlways = split.equalsIgnoreCase("always");
					splitAllowed = split.equalsIgnoreCase("allowed");
				}
				else if (propName.equalsIgnoreCase("Trains"))
				{
					nnp2 = properties.item(j).getAttributes();
					String numbers = XmlUtils.extractStringAttribute(nnp2,
							"number", "4,4,3,2");
					String[] numberArray = numbers.split(",");
					trainLimit = new int[numberArray.length];
					for (int i = 0; i < numberArray.length; i++)
					{
						try
						{
							trainLimit[i] = Integer.parseInt(numberArray[i]);
						}
						catch (NumberFormatException e)
						{
							throw new ConfigurationException(
									"Invalid train limit " + numberArray[i], e);
						}
					}
					mustOwnATrain = XmlUtils.extractBooleanAttribute(nnp2,
							"mandatory", mustOwnATrain);
				}
				else if (propName.equalsIgnoreCase("FirstTrainCloses"))
				{
					nnp2 = properties.item(j).getAttributes();
					String typeName = XmlUtils.extractStringAttribute(nnp2,
							"type", "Private");
					if (typeName.equalsIgnoreCase("Private"))
					{
						privateToCloseOnFirstTrain = XmlUtils
								.extractStringAttribute(nnp2, "name");
					}
					else
					{
						throw new ConfigurationException(
								"Only Privates can be closed on first train buy");
					}
				}
				else if (propName.equalsIgnoreCase("Capitalisation"))
				{
					nnp2 = properties.item(j).getAttributes();
					String capType = XmlUtils.extractStringAttribute(nnp2,
							"type", "full");
					if (capType.equalsIgnoreCase("full"))
					{
						setCapitalisation (CAPITALISE_FULL);
					} else if (capType.equalsIgnoreCase("incremental")) {
						setCapitalisation (CAPITALISE_INCREMENTAL);
					}
					else
					{
						throw new ConfigurationException(
								"Invalid capitalisation type: "+capType);
					}
				} else if (propName.equalsIgnoreCase("NumberOfTileLays")) {
					
				    nnp2 = properties.item(j).getAttributes();
					String colourString = XmlUtils.extractStringAttribute(nnp2, "colour");
				    if (colourString == null) throw new ConfigurationException ("No colour entry for NumberOfTileLays");
					String phaseString = XmlUtils.extractStringAttribute(nnp2, "phase");
				    if (phaseString == null) throw new ConfigurationException ("No phase entry for NumberOfTileLays");
					int number = XmlUtils.extractIntegerAttribute(nnp2, "number");
					Integer lays = new Integer (number);
					
					String[] colours = colourString.split(",");
					HashMap phaseMap;
					/** TODO: should not be necessary to specify all phases separately */
					String[] phases = phaseString.split(",");
					for (int i=0; i<colours.length; i++){
					    if (numberOfTileLays == null) numberOfTileLays = new HashMap();
					    numberOfTileLays.put(colours[i], (phaseMap = new HashMap()));
					    for (int k=0; k<phases.length; k++) {
					        phaseMap.put(phases[k], lays);
					    }
					}
				}

			}

			if (hasParPrice) GameManager.setHasAnyParPrice(true);

			NodeList typeElements = element.getElementsByTagName("Certificate");
			if (typeElements.getLength() > 0)
			{
				int shareTotal = 0;
				boolean gotPresident = false;
				PublicCertificateI certificate;
				certificates = new ArrayList(); // Throw away the per-type specification

				for (int j = 0; j < typeElements.getLength(); j++)
				{
					Element certElement = (Element) typeElements.item(j);
					nnp2 = certElement.getAttributes();

					int shares = XmlUtils.extractIntegerAttribute(nnp2,
							"shares", 1);

					boolean president = "President".equals(XmlUtils
							.extractStringAttribute(nnp2, "type", ""));
					int number = XmlUtils.extractIntegerAttribute(nnp2,
							"number", 1);

					if (president)
					{
						if (number > 1 || gotPresident)
							throw new ConfigurationException("Company type "
									+ name
									+ " cannot have multiple President shares");
						gotPresident = true;
					}

					for (int k = 0; k < number; k++)
					{
						certificate = new PublicCertificate(shares, president);
						addCertificate(certificate);
						shareTotal += shares * shareUnit;
					}
				}
				if (shareTotal != 100)
					throw new ConfigurationException("Company type " + name
							+ " total shares is not 100%");
			}

		}
	}

	/**
	 * Return the company token background colour.
	 * 
	 * @return Color object
	 */
	public Color getBgColour()
	{
		return bgColour;
	}

	/**
	 * Return the company token background colour.
	 * 
	 * @return Hexadecimal string RRGGBB.
	 */
	public String getHexBgColour()
	{
		return bgHexColour;
	}

	/**
	 * Return the company token foreground colour.
	 * 
	 * @return Color object.
	 */
	public Color getFgColour()
	{
		return fgColour;
	}

	/**
	 * Return the company token foreground colour.
	 * 
	 * @return Hexadecimal string RRGGBB.
	 */
	public String getHexFgColour()
	{
		return fgHexColour;
	}

    /**
     * @return Returns the homeHex.
     */
    public MapHex getHomeHex() {
        return homeHex;
    }
    /**
     * @param homeHex The homeHex to set.
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
     * @param homeStation The homeStation to set.
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
	public boolean canBuyStock()
	{
		return canBuyStock;
	}

	public void start(StockSpaceI startSpace)
	{
		//this.hasStarted = true;
	    MoveSet.add (new StateChange (hasStarted, Boolean.TRUE));
		setParPrice(startSpace);
		// The current price is set via the Stock Market
		StockMarket.getInstance().start(this, startSpace);
	}

	/**
	 * Start a company with a fixed par price.
	 */
	public void start()
	{
		//this.hasStarted = true;
	    MoveSet.add (new StateChange (hasStarted, Boolean.TRUE));
		if (hasStockPrice && parPrice.getPrice() != null) {
			//setCurrentPrice (parPrice.getPrice());
			// The current price is set via the Stock Market
			StockMarket.getInstance().start(this, parPrice.getPrice());
		}
	}

	/**
	 * @return Returns true is the company has started.
	 */
	public boolean hasStarted()
	{
		return ((Boolean)hasStarted.getState()).booleanValue();
	}

	/**
	 * Float the company, put its initial cash in the treasury.
	 */
	public void setFloated()
	{

		int cash = 0;
		//hasFloated = true;
		MoveSet.add (new StateChange (hasFloated, Boolean.TRUE));
		if (hasStockPrice)
		{
			int capFactor = 0;
			if (capitalisation == CAPITALISE_FULL)
			{
				capFactor = 100 / shareUnit;
			}
			else if (capitalisation == CAPITALISE_INCREMENTAL)
			{
				capFactor = percentageOwnedByPlayers() / shareUnit;
			}
			cash = capFactor * getCurrentPrice().getPrice();
		}
		else
		{
			cash = fixedPrice;
		}
		//Bank.transferCash(null, this, cash);
		MoveSet.add (new CashMove (Bank.getInstance(), this, cash));
		ReportBuffer.add(LocalText.getText("FLOATS", new String[] {
				name,
				Bank.format(cash)
		}));
	}

	/**
	 * Has the company already floated?
	 * 
	 * @return true if the company has floated.
	 */
	public boolean hasFloated()
	{
		return ((Boolean)hasFloated.getState()).booleanValue();
	}

	/**
	 * Has the company already operated?
	 * 
	 * @return true if the company has operated.
	 */
	public boolean hasOperated()
	{
		return hasOperated;
	}

	/**
	 * Set the company par price.
	 * <p>
	 * <i>Note: this method should <b>not</b> be used to start a company!</i>
	 * Use <code><b>start()</b></code> in stead.
	 * 
	 * @param spaceI
	 */
	public void setParPrice(StockSpaceI space)
	{
		if (hasStockPrice)
		{
		    if (parPrice == null) {
		        parPrice = new PriceModel (this);
		    }
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
	public StockSpaceI getParPrice()
	{
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
	public void setCurrentPrice(StockSpaceI price)
	{
	    if (currentPrice == null) {
	        currentPrice = new PriceModel (this);
	        //log.debug ("+"+name+" currentPrice["+currentPrice.hashCode()+"] created as "+currentPrice.hashCode());
	    }
	    if (price != null) {
	        currentPrice.setPrice(price);
	        //log.debug ("+"+name+" currentPrice["+currentPrice.hashCode()+"] set to "+price);
	    }
	}

	public PriceModel getCurrentPriceModel()
	{
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
	 *         stock market.
	 */
	public StockSpaceI getCurrentPrice()
	{
		return currentPrice != null ? currentPrice.getPrice() : null;
	}

	/**
	 * Add a given amount to the company treasury.
	 * 
	 * @param amount
	 *            The amount to add (may be negative).
	 */
	public boolean addCash(int amount)
	{
		return treasury.addCash(amount);
	}

	/**
	 * Get the current company treasury.
	 * 
	 * @return The current cash amount.
	 */
	public int getCash()
	{
		return treasury.getCash();
	}

	public String getFormattedCash()
	{
		return treasury.toString();
	}

	public ModelObject getCashModel()
	{
		return treasury;
	}

	/**
	 * @return
	 */
	public static int getNumberOfPublicCompanies()
	{
		return numberOfPublicCompanies;
	}

	/**
	 * @return
	 */
	public int getPublicNumber()
	{
		return publicNumber;
	}

	/**
	 * Get a list of this company's certificates.
	 * 
	 * @return ArrayList containing the certificates (item 0 is the President's
	 *         share).
	 */
	public List getCertificates()
	{
		return certificates;
	}

	/**
	 * Assign a predefined list of certificates to this company. The list is
	 * deep cloned.
	 * 
	 * @param list
	 *            ArrayList containing the certificates.
	 */
	public void setCertificates(List list)
	{
		certificates = new ArrayList();
		Iterator it = list.iterator();
		PublicCertificateI cert;
		while (it.hasNext())
		{
			cert = ((PublicCertificateI) it.next()).copy();
			certificates.add(cert);
			cert.setCompany(this);
			// TODO Questionable if it should be put in IPO or in Unavailable.
			// Bank.getIpo().addCertificate(cert);
		}
	}

	/**
	 * Add a certificate to the end of this company's list of certificates.
	 * 
	 * @param certificate
	 *            The certificate to add.
	 */
	public void addCertificate(PublicCertificateI certificate)
	{
		if (certificates == null)
			certificates = new ArrayList();
		certificates.add(certificate);
		certificate.setCompany(this);
	}

	/**
	 * Get the Portfolio of this company, containing all privates and
	 * certificates owned..
	 * 
	 * @return The Portfolio of this company.
	 */
	public Portfolio getPortfolio()
	{
		return portfolio;
	}

	/**
	 * Get the percentage of shares that must be sold to float the company.
	 * 
	 * @return The float percentage.
	 */
	public int getFloatPercentage()
	{
		return floatPerc;
	}

	/**
	 * Get the company President.
	 * 
	 */
	public Player getPresident()
	{
		if (hasStarted())
		{
			return (Player) ((PublicCertificateI) certificates.get(0))
					.getPortfolio().getOwner();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Store the last revenue earned by this company.
	 * 
	 * @param i
	 *            The last revenue amount.
	 */
	protected void setLastRevenue(int i)
	{
		lastRevenue.setAmount(i);
	}

	/**
	 * Get the last revenue earned by this company.
	 * 
	 * @return The last revenue amount.
	 */
	public int getLastRevenue()
	{
		return lastRevenue.getAmount();
	}
	
	public ModelObject getLastRevenueModel () {
	    return lastRevenue;
	}

	/**
	 * Pay out a given amount of revenue (and store it). The amount is
	 * distributed to all the certificate holders, or the "beneficiary" if
	 * defined (e.g.: BankPool shares may pay to the company).
	 * 
	 * @param The
	 *            revenue amount.
	 */
	public void payOut(int amount)
	{

		setLastRevenue(amount);

		distributePayout(amount);

		// Move the token
		if (hasStockPrice)
			Game.getStockMarket().payOut(this);
	}

	/**
	 * Split a dividend. TODO Optional rounding down the payout
	 * 
	 * @param amount
	 */
	public void splitRevenue(int amount)
	{

		setLastRevenue(amount);

		// Withhold half of it
		// For now, hardcode the rule that payout is rounded up.
		int withheld = ((int) amount / (2 * getNumberOfShares()))
				* getNumberOfShares();
		//Bank.transferCash(null, this, withheld);
		MoveSet.add(new CashMove (null, this, withheld));
		ReportBuffer.add(name + " receives " + Bank.format(withheld));

		// Payout the remainder
		int payed = amount - withheld;
		distributePayout(payed);

		// Move the token
		if (hasStockPrice)
			Game.getStockMarket().payOut(this);
	}

	/**
	 * Distribute the dividend amongst the shareholders.
	 * 
	 * @param amount
	 */
	protected void distributePayout(int amount)
	{

		Iterator it = certificates.iterator();
		PublicCertificateI cert;
		int part;
		CashHolder recipient;
		Map split = new HashMap();
		while (it.hasNext())
		{
			cert = ((PublicCertificateI) it.next());
			recipient = getBeneficiary(cert);
			part = amount * cert.getShares() * shareUnit / 100;
			// For reporting, we want to add up the amounts per recipient
			if (split.containsKey(recipient))
			{
				part += ((Integer) split.get(recipient)).intValue();
			}
			split.put(recipient, new Integer(part));
		}
		// Report and add the cash
		it = split.keySet().iterator();
		while (it.hasNext())
		{
			recipient = (CashHolder) it.next();
			if (recipient instanceof Bank)
				continue;
			part = ((Integer) split.get(recipient)).intValue();
			ReportBuffer.add(recipient.getName() + " receives " + Bank.format(part));
			//Bank.transferCash(null, recipient, part);
			MoveSet.add(new CashMove (null, recipient, part));
		}

	}
	
	/** Who gets the per-share revenue? */
	protected CashHolder getBeneficiary (PublicCertificateI cert) {
	    
	    Portfolio holder = cert.getPortfolio();
	    CashHolder beneficiary = holder.getOwner();
	    // Special cases apply if the holder is the IPO or the Pool
	    if (holder == Bank.getIpo() && ipoPaysOut
	            || holder == Bank.getPool() && poolPaysOut) {
	        beneficiary = this;
	    }
	    return beneficiary;
	}

	/**
	 * Withhold a given amount of revenue (and store it).
	 * 
	 * @param The
	 *            revenue amount.
	 */
	public void withhold(int amount)
	{

		setLastRevenue(amount);
		//Bank.transferCash(null, this, amount);
		MoveSet.add (new CashMove (null, this, amount));
		// Move the token
		if (hasStockPrice)
			Game.getStockMarket().withhold(this);
	}

	/**
	 * Is the company completely sold out?
	 * 
	 * @return true if no certs are held by the Bank.
	 * @TODO: This rule does not apply to all games (1870). Needs be sorted out.
	 */
	public boolean isSoldOut()
	{
		Iterator it = certificates.iterator();
		PublicCertificateI cert;
		while (it.hasNext())
		{
			if (((PublicCertificateI) it.next()).getPortfolio().getOwner() instanceof Bank)
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @return
	 */
	public boolean canBuyPrivates()
	{
		return canBuyPrivates;
	}

	/**
	 * Get the unit of share.
	 * 
	 * @return The percentage of ownership that is called "one share".
	 */
	public int getShareUnit()
	{
		return shareUnit;
	}

	public String toString()
	{
		return name + ", " + publicNumber + " of " + numberOfPublicCompanies;
	}

	public static boolean startCompany(String playerName, String companyName,
			StockSpace startSpace)
	{
		// TODO: Should probably do error checking in case names aren't found.
		Player player = Game.getPlayerManager().getPlayerByName(playerName);
		PublicCompany company = (PublicCompany) Game.getCompanyManager()
				.getPublicCompany(companyName);

		PublicCertificate cert = (PublicCertificate) company.certificates
				.get(0);

		if (player.getCash() >= (startSpace.getPrice() * (cert.getShare() / company
				.getShareUnit())))
		{
			company.start(startSpace);
			// company.setClosed(false);
			int price = startSpace.getPrice()
					* (cert.getShare() / company.getShareUnit());
			player.buyShare(cert, price);

			return true;
		}
		else
			return false;
	}

	public PublicCertificateI getNextAvailableCertificate()
	{
		for (int i = 0; i < certificates.size(); i++)
		{
			if (((PublicCertificateI) certificates.get(i)).isAvailable())
			{
				return (PublicCertificateI) certificates.get(i);
			}
		}
		return null;
	}

	/**
	 * @return Returns the lowerPrivatePriceFactor.
	 */
	public float getLowerPrivatePriceFactor()
	{
		return lowerPrivatePriceFactor;
	}

	/**
	 * @return Returns the upperPrivatePriceFactor.
	 */
	public float getUpperPrivatePriceFactor()
	{
		return upperPrivatePriceFactor;
	}

	public boolean hasStockPrice()
	{
		return hasStockPrice;
	}

	public boolean hasParPrice()
	{
		return hasParPrice;
	}

	public int getFixedPrice()
	{
		return fixedPrice;
	}

	public int percentageOwnedByPlayers()
	{

		int share = 0;
		Iterator it = certificates.iterator();
		PublicCertificateI cert;
		while (it.hasNext())
		{
			cert = (PublicCertificateI) it.next();
			if (cert.getPortfolio().getOwner() instanceof Player)
			{
				share += cert.getShare();
			}
		}
		return share;
	}

	/**
	 * @return Returns the splitAllowed.
	 */
	public boolean isSplitAllowed()
	{
		return splitAllowed;
	}

	/**
	 * @return Returns the splitAlways.
	 */
	public boolean isSplitAlways()
	{
		return splitAlways;
	}

	/**
	 * Check if the presidency has changed for a <b>buying</b> player.
	 * 
	 * @param buyer
	 *            Player who has just bought a certificate.
	 */
	public void checkPresidencyOnBuy(Player buyer)
	{

		if (!hasStarted() || buyer == getPresident() || certificates.size() < 2)
			return;
		Player pres = getPresident();
		int presShare = pres.getPortfolio().ownsShare(this);
		int buyerShare = buyer.getPortfolio().ownsShare(this);
		if (buyerShare > presShare)
		{
			pres.getPortfolio().swapPresidentCertificate(this,
					buyer.getPortfolio());
			ReportBuffer.add(
					LocalText.getText("PresidencyIsTransferredTo", new String[] {
							name,
							buyer.getName()
					}));
		}
	}

	/**
	 * Check if the presidency has changed for a <b>selling</b> player.
	 */
	public void checkPresidencyOnSale(Player seller)
	{

		if (seller != getPresident())
			return;

		int presShare = seller.getPortfolio().ownsShare(this);
		int presIndex = seller.getIndex();
		Player player;
		int share;

		for (int i = presIndex + 1; i < presIndex
				+ GameManager.getNumberOfPlayers(); i++)
		{
			player = GameManager.getPlayer(i);
			share = player.getPortfolio().ownsShare(this);
			if (share > presShare)
			{
				// Presidency must be transferred
				seller.getPortfolio().swapPresidentCertificate(this,
						player.getPortfolio());
				ReportBuffer.add(
						LocalText.getText("PresidencyIsTransferredTo", new String[] {
								name,
								player.getName()
						}));
			}
		}
	}

	public void checkFlotation()
	{
		if (hasStarted() && !hasFloated()
				&& percentageOwnedByPlayers() >= floatPerc
				/*&& currentPrice.getPrice() != null*/)
		{
			// Float company (limit and capitalisation to be made configurable)
			setFloated();
		}
	}

	/**
	 * @return Returns the capitalisation.
	 */
	public int getCapitalisation()
	{
		return capitalisation;
	}

	/**
	 * @param capitalisation
	 *            The capitalisation to set.
	 */
	public void setCapitalisation(int capitalisation)
	{
		this.capitalisation = capitalisation;
	}

	public int getNumberOfShares()
	{
		return 100 / shareUnit;
	}

	public int getTrainLimit(int phaseIndex)
	{
		return trainLimit[Math.min(phaseIndex, trainLimit.length - 1)];
	}
	
	public boolean mayBuyTrains () {
	    
	    return portfolio.getTrains().length < getTrainLimit(GameManager.getCurrentPhase().getIndex());
	}

	/** Must be called in stead of Portfolio.buyTrain if side-effects can occur. */
	public void buyTrain(TrainI train, int price)
	{
		portfolio.buyTrain(train, price);
		if (this.privateToCloseOnFirstTrain != null)
		{
			PrivateCompanyI priv = Game.getCompanyManager().getPrivateCompany(
					privateToCloseOnFirstTrain);
			priv.setClosed();
			privateToCloseOnFirstTrain = null;
		}
	}

	public BaseTokensModel getBaseTokensModel() {
	    return baseTokensModel;
	}
	
	public boolean layHomeBaseTokens () {
	    
	    // Assume for now that companies have only one home base.
	    // This is not true in 1841!
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
	 * Add a base token to the company charter.
	 * This method is called when a base token is removed from a map hex.
	 * This may happen because of an Undo action. 
	 * In some games tokens can be taken back for more "regular" reasons as well.
	 * The token is removed from the company laid token list
	 * and added to the free token list.
	 */

	public boolean addToken (TokenI token) {
	    
	    boolean result = false;
	    if (token instanceof BaseToken 
	            && laidBaseTokens.remove (token)) {
	        token.setHolder(this);
	        result = freeBaseTokens.add (token);
	        this.baseTokensModel.update();
	    }
	    return result;
	    
	}

	public List getLaidBaseTokens()
	{
		return laidBaseTokens;
	}
	
	public List getTokens() {
	    return allBaseTokens;
	}
	
	public int getNumberOfBaseTokens () {
	    return allBaseTokens.size();
	}
	
	public int getNumberOfFreeBaseTokens () {
	    return freeBaseTokens.size();
	}
	
	public int getNumberOfLaidBaseTokens() {
	    return laidBaseTokens.size();
	}
	
	public boolean hasTokens() {
	    return (allBaseTokens.size() > 0);
	}

	/**
	 * Remove a base token from the company charter.
	 * This method is called when a base token is laid on a map hex.
	 * The token is removed from the company free token list
	 * and added to the laid token list.
	 * In other words: lay a base token
	 */
	public boolean removeToken (TokenI token) {
	    
	    boolean result = false;
	    if (token instanceof BaseToken 
	            && freeBaseTokens.remove (token)) {
	        result = laidBaseTokens.add (token);
	        this.baseTokensModel.update();
	    }
	    return result;
	    
	}

	public int getNumberOfTileLays (String tileColour) {
	    
	    if (numberOfTileLays == null) return 1;
	    
	    Map phaseMap = (Map) numberOfTileLays.get(tileColour);
	    if (phaseMap == null) return 1;
	    
	    PhaseI phase = PhaseManager.getInstance().getCurrentPhase();
	    Integer i = (Integer) phaseMap.get(phase.getName());
	    if (i == null) return 1;
	    
	    return i.intValue();
	}
	
	public boolean mustOwnATrain () {
	    return mustOwnATrain;
	}

	public Object clone()
	{

		Object clone = null;
		try
		{
			clone = super.clone();
		}
		catch (CloneNotSupportedException e)
		{
			log.fatal ("Cannot clone company " + name);
			return null;
		}

		/*
		 * Add the certificates, if defined with the CompanyType and absent in
		 * the Company specification
		 */
		if (certificates != null)
		{
			((PublicCompanyI) clone).setCertificates(certificates);
		}
		((PublicCompanyI) clone).setParPrice(null);
		((PublicCompanyI) clone).setCurrentPrice(null);
		
		return clone;
	}
}
