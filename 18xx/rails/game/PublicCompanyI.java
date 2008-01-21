/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCompanyI.java,v 1.11 2008/01/21 22:57:28 evos Exp $ */
package rails.game;


import java.awt.Color;
import java.util.*;

import rails.game.model.BaseTokensModel;
import rails.game.model.ModelObject;
import rails.game.model.MoneyModel;
import rails.game.model.PriceModel;

/**
 * Interface to be used to access PublicCompany instances.
 */
public interface PublicCompanyI extends CompanyI, CashHolder, TokenHolderI
{

	public static final int CAPITALISE_FULL = 0;

	public static final int CAPITALISE_INCREMENTAL = 1;

	public void init2() throws ConfigurationException;

	/**
	 * Return the company token background colour.
	 * 
	 * @return Color object
	 */
	public Color getBgColour();

	/**
	 * Return the company token background colour.
	 * 
	 * @return Hexadecimal string RRGGBB.
	 */
	public String getHexBgColour();

	/**
	 * Return the company token foreground colour.
	 * 
	 * @return Color object.
	 */

	public Color getFgColour();

	/**
	 * Return the company token foreground colour.
	 * 
	 * @return Hexadecimal string RRGGBB.
	 */
	public String getHexFgColour();

	/**
	 * @return
	 */
	public boolean canBuyStock();

	/**
	 * @return
	 */
	public boolean canBuyPrivates();
	
    public boolean mustHaveOperatedToSellShares ();


	/**
	 * Start the company.
	 */
	public void start(StockSpaceI startSpace);

	public void start();
	
	public void start (int price);

	/**
	 * @return Returns true is the company has started.
	 */
	public boolean hasStarted();

	/**
	 * Float the company, put its initial cash in the treasury.
	 */
	public void setFloated();

	/**
	 * Has the company already floated?
	 * 
	 * @return true if the company has floated.
	 */

	public boolean hasFloated();

	/**
	 * Has the company already operated?
	 * 
	 * @return true if the company has operated.
	 */
	public boolean hasOperated();

	public void setOperated (boolean value);
    
	/**
	 * Start the company and set its initial (par) price.
	 * 
	 * @param spaceI
	 */
	

	public void setParPrice(StockSpaceI parPrice);

	/**
	 * Get the company par (initial) price.
	 * 
	 * @return StockSpace object, which defines the company start position on
	 *         the stock chart.
	 */
	public StockSpaceI getParPrice();

	/**
	 * Set a new company price.
	 * 
	 * @param price
	 *            The StockSpace object that defines the new location on the
	 *            stock market.
	 */
	public void setCurrentPrice(StockSpaceI price);

	/**
	 * Get the current company share price.
	 * 
	 * @return The StockSpace object that defines the current location on the
	 *         stock market.
	 */
	public StockSpaceI getCurrentPrice();
	
	public PriceModel getCurrentPriceModel ();
	
	public PriceModel getParPriceModel();
	/**
	 * @return
	 */
	public int getPublicNumber();

	/**
	 * Get a list of this company's certificates.
	 * 
	 * @return ArrayList containing the certificates (item 0 is the President's
	 *         share).
	 */
	public List<PublicCertificateI> getCertificates();

	/**
	 * Assign a predefined array of certificates to this company.
	 * 
	 * @param list
	 *            ArrayList containing the certificates.
	 */
	public void setCertificates(List<PublicCertificateI> list);

	/**
	 * Add a certificate to the end of this company's list of certificates.
	 * 
	 * @param certificate
	 *            The certificate to add.
	 */
	public void addCertificate(PublicCertificateI certificate);

	/**
	 * Get the current company treasury.
	 * <p>
	 * <i>Note: other cash-related methods are declared in interface CashHolder
	 * </i>
	 * 
	 * @return The current cash amount.
	 */
	public int getCash();

	public String getFormattedCash();
	
	public void setLastRevenue (int revenue);

	/**
	 * Get the last revenue earned by this company.
	 * 
	 * @return The last revenue amount.
	 */
	public int getLastRevenue();
	
	public ModelObject getLastRevenueModel ();

    public void setLastRevenueAllocation(int allocation);
    public String getlastRevenueAllocationText ();
    public ModelObject getLastRevenueAllocationModel ();
    
	public Player getPresident();

	public int getFloatPercentage();

	public Portfolio getPortfolio();

	public void payout(int amount);

	public void splitRevenue(int amount);

	public void withhold(int amount);

	public boolean isSoldOut();

	/**
	 * Get the unit of share.
	 * 
	 * @return The percentage of ownership that is called "one share".
	 */
	public int getShareUnit();

	/**
	 * @return Returns the lowerPrivatePriceFactor.
	 */
	public float getLowerPrivatePriceFactor();

	/**
	 * @return Returns the upperPrivatePriceFactor.
	 */
	public float getUpperPrivatePriceFactor();

	/**
	 * Is company present on the Stock Market?
	 * 
	 * @return True if the company has a stock price.
	 */
	public boolean hasStockPrice();

	public boolean hasParPrice();

	//public int percentageOwnedByPlayers();

	public boolean isSplitAllowed();

	public boolean isSplitAlways();

	public void checkPresidencyOnSale(Player seller);

	public void checkPresidencyOnBuy(Player buyer);

	public void checkFlotation();

	public int getCapitalisation();
	public void setCapitalisation(int capitalisation);

	public int getTrainLimit (int phaseIndex);
	public boolean mayBuyTrains ();
	
	public void initTurn();
	
	public void buyTrain (TrainI train, int price);
	public ModelObject getTrainsSpentThisTurnModel ();
	
	public void buyPrivate(PrivateCompanyI privateCompany, Portfolio from,
			int price);
	public ModelObject getPrivatesSpentThisTurnModel ();
	
	public void layTile (MapHex hex, TileI tile, int orientation, int cost);
	public ModelObject getTilesLaidThisTurnModel ();
	public ModelObject getTilesCostThisTurnModel ();
	
	public void layBaseToken (MapHex hex, int cost);
	public ModelObject getTokensLaidThisTurnModel ();
	public ModelObject getTokensCostThisTurnModel ();
	
	public boolean layHomeBaseTokens();
	public BaseToken getFreeToken();
	public boolean addToken (TokenI token);
	public boolean removeToken (TokenI token);
	public int getNumberOfBaseTokens ();
	public int getNumberOfFreeBaseTokens ();
	public int getNumberOfLaidBaseTokens();
	public BaseTokensModel getBaseTokensModel();
	public MoneyModel getBonusTokensModel ();
	public void layBonusToken (MapHex hex, int cost, BonusToken token);
    public void removeBonusToken (BonusToken token);
	
    public MapHex getHomeHex();
    public void setHomeHex(MapHex homeHex);
    public Station getHomeStation();
    public void setHomeStation(Station homeStation);
    public MapHex getDestinationHex();

	public int getNumberOfTileLays (String tileColour);

	public boolean mustOwnATrain ();


}
