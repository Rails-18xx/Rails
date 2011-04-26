/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PublicCompanyI.java,v 1.56 2010/05/24 11:42:35 evos Exp $ */
package rails.game;

import java.awt.Color;
import java.util.List;

import rails.game.model.*;

/**
 * Interface to be used to access PublicCompany instances.
 */
public interface PublicCompanyI extends CompanyI, CashHolder, TokenHolder {

    public static final int CAPITALISE_FULL = 0;

    public static final int CAPITALISE_INCREMENTAL = 1;

    public static final int CAPITALISE_WHEN_BOUGHT = 2;

    public void setIndex (int index);


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
    public boolean canUseSpecialProperties();

    public boolean mustHaveOperatedToTradeShares();

    public boolean mayTradeShares();

    public boolean mayBuyTrainType (TrainI train);

    /**
     * Start the company.
     */
    public void start(StockSpaceI startSpace);

    public void start();

    public void start(int price);

    public void transferAssetsFrom(PublicCompanyI otherCompany);

    /**
     * @return Returns true is the company has started.
     */
    public boolean hasStarted();
    public void setBuyable(boolean buyable);
    public boolean isBuyable();

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

    public void setOperated();

    /**
     * Start the company and set its initial (par) price.
     *
     * @param spaceI
     */

    public void setParSpace(StockSpaceI parPrice);

    /**
     * Get the company par (initial) price.
     *
     * @return StockSpace object, which defines the company start position on
     * the stock chart.
     */
    public StockSpaceI getStartSpace();

    /**
     * Set a new company price.
     *
     * @param price The StockSpace object that defines the new location on the
     * stock market.
     */
    public void setCurrentSpace(StockSpaceI price);

    /**
     * Get the current company share price.
     *
     * @return The StockSpace object that defines the current location on the
     * stock market.
     */
    public StockSpaceI getCurrentSpace();

    public PriceModel getCurrentPriceModel();

    public PriceModel getParPriceModel();

    public int getFixedPrice();

    public int getIPOPrice ();

    public int getMarketPrice ();
    public int getGameEndPrice();

    public int getPublicNumber();

    public int getBaseTokensBuyCost();

    public int getBaseTokenLayCost(MapHex hex);
    public int[] getBaseTokenLayCosts();

    public boolean canHoldOwnShares();

    /**
     * Get a list of this company's certificates.
     *
     * @return ArrayList containing the certificates (item 0 is the President's
     * share).
     */
    public List<PublicCertificateI> getCertificates();

    /**
     * Assign a predefined array of certificates to this company.
     *
     * @param list ArrayList containing the certificates.
     */
    public void setCertificates(List<PublicCertificateI> list);

    /**
     * Add a certificate to the end of this company's list of certificates.
     *
     * @param certificate The certificate to add.
     */
    public void addCertificate(PublicCertificateI certificate);

    /**
     * Get the current company treasury. <p> <i>Note: other cash-related methods
     * are declared in interface CashHolder </i>
     *
     * @return The current cash amount.
     */
    public int getCash();

    public String getFormattedCash();

    public void setLastRevenue(int revenue);

    /**
     * Get the last revenue earned by this company.
     *
     * @return The last revenue amount.
     */
    public int getLastRevenue();

    public ModelObject getLastRevenueModel();

    public void setLastRevenueAllocation(int allocation);

    public String getlastRevenueAllocationText();

    public ModelObject getLastRevenueAllocationModel();

    public Player getPresident();

    public PresidentModel getPresidentModel();

    public PublicCertificateI getPresidentsShare();

    public int getFloatPercentage();

    public void payout(int amount);

    public void withhold(int amount);

    public boolean paysOutToTreasury (PublicCertificateI cert);

    public boolean isSoldOut();

    /**
     * Get the unit of share.
     *
     * @return The percentage of ownership that is called "one share".
     */
    public int getShareUnit();
    public int getShareUnitsForSharePrice();
    public int getNumberOfShares();

    /**
     * Is company present on the Stock Market?
     *
     * @return True if the company has a stock price.
     */
    public boolean hasStockPrice();

    public boolean hasParPrice();

    public boolean canSharePriceVary();

    public void updatePlayersWorth();

    public boolean isSplitAllowed();

    public boolean isSplitAlways();

    public void checkPresidencyOnSale(Player seller);

    public void checkPresidencyOnBuy(Player buyer);
    public void checkPresidency ();

    public int getCapitalisation();

    public void setCapitalisation(int capitalisation);

    public int getTrainLimit(int phaseIndex);

    public int getCurrentTrainLimit();

    public int getNumberOfTrains();
    public boolean canRunTrains();

    public void initTurn();

    public void buyTrain(TrainI train, int price);

    public ModelObject getTrainsSpentThisTurnModel();

    public void buyPrivate(PrivateCompanyI privateCompany, Portfolio from,
            int price);

    public ModelObject getPrivatesSpentThisTurnModel();

    public void layTile(MapHex hex, TileI tile, int orientation, int cost);

    public void layTileInNoMapMode(int cost);

    public ModelObject getTilesLaidThisTurnModel();

    public ModelObject getTilesCostThisTurnModel();

    public void layBaseToken(MapHex hex, int cost);

    public void layBaseTokenInNoMapMode(int cost);

    public ModelObject getTokensLaidThisTurnModel();

    public ModelObject getTokensCostThisTurnModel();

    public boolean layHomeBaseTokens();
    public boolean hasLaidHomeBaseTokens();

    public BaseToken getFreeToken();

    public int getNumberOfBaseTokens();

    public int getNumberOfFreeBaseTokens();

    public int getNumberOfLaidBaseTokens();

    public BaseTokensModel getBaseTokensModel();

    public BonusModel getBonusTokensModel();

    public boolean addBonus(Bonus bonus);

    public boolean removeBonus(Bonus bonus);
    public boolean removeBonus (String name);
    public List<Bonus> getBonuses();

    public List<MapHex> getHomeHexes();

    public void setHomeHex(MapHex homeHex);

    public int getHomeCityNumber();

    public void setHomeCityNumber(int homeCityNumber);
    public boolean isHomeBlockedForAllCities();

    public MapHex getDestinationHex();
    public boolean hasDestination ();
    public boolean hasReachedDestination();
    public void setReachedDestination (boolean value);

    public int getNumberOfTileLays(String tileColour);

    public boolean mustOwnATrain();
    public boolean mustTradeTrainsAtFixedPrice();

    public int getCurrentNumberOfLoans();
    public int getCurrentLoanValue ();
    public void addLoans(int number);
    public int getLoanInterestPct();
    public int getMaxNumberOfLoans();
    public boolean canLoan();
    public boolean canClose();
    public int getMaxLoansPerRound();
    public int getValuePerLoan();
    public MoneyModel getLoanValueModel ();

    public int sharesOwnedByPlayers();
    public String getExtraShareMarks ();

    public ModelObject getInGameModel ();
    public ModelObject getIsClosedModel ();
    
}
