package game;

import game.action.*;
import game.move.CashMove;
import game.move.MoveSet;
import game.move.StateChange;
import game.special.*;
import game.state.StateObject;

import java.util.*;

import util.LocalText;
import util.Util;

/**
 * Implements a basic Operating Round.
 * <p>
 * A new instance must be created for each new Operating Round. At the end of a
 * round, the current instance should be discarded.
 * <p>
 * Permanent memory is formed by static attributes.
 */
public class OperatingRound extends Round implements Observer
{

	/* Transient memory (per round only) */
	//protected Player currentPlayer;
	//protected int currentPlayerIndex;
    protected StateObject stepObject;
    //protected int step;
	protected boolean actionPossible = true;
	protected String actionNotPossibleMessage = "";

	protected TreeMap operatingCompanies;
	protected PublicCompanyI[] operatingCompanyArray;
	protected int operatingCompanyIndex = 0;
	protected PublicCompanyI operatingCompany;

	protected int[] tileLayCost;
	protected String[] tilesLaid;
	protected int[] baseTokenLayCost;
	protected String[] baseTokensLaid;
	protected int[] revenue;
	protected int[] trainBuyCost;
	protected int[] privateBuyCost;

	protected List currentSpecialProperties = null;
	protected List currentSpecialTileLays = new ArrayList();
	protected List currentNormalTileLays = new ArrayList();
	protected Map tileLaysPerColour = new HashMap();
	//protected List normalTileLaysDone;
	protected Map specialPropertyPerHex = new HashMap();
	protected List currentNormalTokenLays = new ArrayList();
	protected List currentSpecialTokenLays = new ArrayList();

	protected PhaseI currentPhase;
	protected String thisOrNumber;
	
	protected BuyableTrain savedBuyableTrain = null;
	protected int savedPrice = 0;
	protected int cashToBeRaisedByPresident = 0;

	/**
	 * Number of tiles that may be laid. TODO: This does not cover cases like "2
	 * yellow or 1 upgrade allowed".
	 */
	//protected int normalTileLaysAllowed = 1;
	//protected int normalTileLaysDone = 0;
	//protected int extraTileLaysAllowed = 0;
	//protected int extraTileLaysDone = 0;

	protected int splitRule = SPLIT_NOT_ALLOWED; // To be made configurable

	/* Permanent memory */
	static protected Player[] players;
	static protected PublicCompanyI[] companies;
	static protected int numberOfCompanies = 0;
	static protected int relativeORNumber = 0;
	static protected int cumulativeORNumber = 0;

	/* Constants */
	public static final int SPLIT_NOT_ALLOWED = 0;
	public static final int SPLIT_ROUND_UP = 1; // More money to the
	// shareholders
	public static final int SPLIT_ROUND_DOWN = 2; // More to the treasury

	public static final int STEP_INITIAL = 0;
	public static final int STEP_LAY_TRACK = 0;
	public static final int STEP_LAY_TOKEN = 1;
	public static final int STEP_CALC_REVENUE = 2;
	public static final int STEP_PAYOUT = 3;
	public static final int STEP_BUY_TRAIN = 4;
	public static final int STEP_FINAL = 5;
	protected static int[] steps = new int[] { STEP_LAY_TRACK, STEP_LAY_TOKEN,
			STEP_CALC_REVENUE, STEP_PAYOUT, STEP_BUY_TRAIN, STEP_FINAL };
	
	/**
	 * The constructor.
	 */
	public OperatingRound()
	{

		if (players == null)
		{
			players = Game.getPlayerManager().getPlayersArray();
		}
		if (companies == null)
		{
			companies = (PublicCompanyI[]) Game.getCompanyManager()
					.getAllPublicCompanies()
					.toArray(new PublicCompanyI[0]);
		}
		// Determine operating sequence for this OR.
		// Shortcut: order considered fixed at the OR start. This is not always
		// true.
		operatingCompanies = new TreeMap();
		PublicCompanyI company;
		StockSpaceI space;
		int key, stackPos;
		int minorNo = 0;
		for (int i = 0; i < companies.length; i++)
		{
			company = companies[i];
			if (!company.hasFloated())
				continue;
			// Key must put companies in reverse operating order, because sort
			// is ascending.
			if (company.hasStockPrice())
			{
				space = company.getCurrentPrice();
				key = 1000000 * (999 - space.getPrice()) + 10000
						* (99 - space.getColumn()) + 100 * space.getRow()
						+ space.getStackPosition(company);
			}
			else
			{
				key = ++minorNo;
			}
			operatingCompanies.put(new Integer(key), company);
		}

		operatingCompanyArray = (PublicCompanyI[]) operatingCompanies.values()
				.toArray(new PublicCompanyI[0]);

		relativeORNumber++;
		cumulativeORNumber++;
		thisOrNumber = getCompositeORNumber();

		LogBuffer.add(LocalText.getText("START_OR", getCompositeORNumber()));

		numberOfCompanies = operatingCompanyArray.length;

		revenue = new int[numberOfCompanies];
		tilesLaid = new String[numberOfCompanies];
		tileLayCost = new int[numberOfCompanies];
		baseTokensLaid = new String[numberOfCompanies];
		baseTokenLayCost = new int[numberOfCompanies];
		trainBuyCost = new int[numberOfCompanies];
		privateBuyCost = new int[numberOfCompanies];

		// Private companies pay out
		Iterator it = Game.getCompanyManager()
				.getAllPrivateCompanies()
				.iterator();
		PrivateCompanyI priv;
		while (it.hasNext())
		{
			priv = (PrivateCompanyI) it.next();
			if (!priv.isClosed())
				priv.payOut();
		}

		if (operatingCompanyArray.length > 0)
		{
			operatingCompany = operatingCompanyArray[operatingCompanyIndex];
			GameManager.getInstance().setRound(this);

			setStep (STEP_INITIAL);
		}
		else
		{
			// No operating companies yet: close the round.
			LogBuffer.add (LocalText.getText("END_OR", getCompositeORNumber()));
			GameManager.getInstance().nextRound(this);
		}
	}

	/*----- General methods -----*/

	/**
	 * Return the operating round (OR) number in the format sr.or, where sr is
	 * the last stock round number and or is the relative OR number.
	 * 
	 * @return Composite SR/OR number.
	 */
	public String getCompositeORNumber()
	{
		return StockRound.getLastStockRoundNumber() + "." + relativeORNumber;
	}

	/**
	 * Get the relative OR number. This number restarts at 1 after each stock
	 * round.
	 * 
	 * @return Relative OR number
	 */
	public int getRelativeORNumber()
	{
		return relativeORNumber;
	}

	/**
	 * Get the cumulative OR number. This number never restarts.
	 * 
	 * @return Cumulative OR number.
	 */
	public int getCumulativeORNumber()
	{
		return cumulativeORNumber;
	}

	public static int getLastORNumber()
	{
		return cumulativeORNumber;
	}

	/**
	 * @deprecated Currently needed, but will be removed in a later stage.
	 */
	public static void resetRelativeORNumber()
	{
		relativeORNumber = 0;
	}

	/*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/

	/**
	 * Validate and, if OK, process laying a tile. 
	 * @param companyName
	 *            The name of the company that lays the track.
	 * @param hex The MapHex object where the tile is laid on.
	 * @param tile An instance of the tile that has been laid.
	 * @param orientation The orientation in which the tile has been laid.
	 * Anunrotated tile has orientation 0. For any 60 degrees of clockwise
	 * rotation, the orientation number increases by one.
	 * @param allowance
	 * A LayTile (PossibleActions) instance selected and used by the GUI
	 * to enable the actual tile laying.
	 */
	public boolean layTile(String companyName, MapHex hex, TileI tile,
			int orientation, LayTile allowance)
	{

		String errMsg = null;
		int cost = 0;
		SpecialTileLay stl = null;
		boolean extra = false;

		// Dummy loop to enable a quick jump out.
		while (true)
		{
			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_LAY_TRACK)
			{
				errMsg = LocalText.getText("WrongActionNoTileLay");
				break;
			}

			if (tile == null)
				break;

			if (!tile.isLayableNow())
			{
				errMsg = LocalText.getText("TileNotYetAvailable", tile.getName());
				break;
			}
			if (tile.countFreeTiles() == 0)
			{
				errMsg = LocalText.getText("TileNotAvailable", tile.getName());
				break;
			}

		    /* Check if the current tile is allowed via the LayTile allowance.
		     * (currently the set if tiles is always null, which means
		     * that this check is redundant. This may change in the future.
		     */  
			if (allowance != null) {
			    List tiles = allowance.getTiles();
			    if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
			        errMsg = LocalText.getText("TileMayNotBeLaidInHex", new String[] {
			                tile.getName(),
			                hex.getName()
			        });
			        break;
			    }
			    stl = allowance.getSpecialProperty();
			    if (stl != null) extra = stl.isExtra();
			}
			
			/* If this counts as a normal tile lay, check if the allowed number of
			 * normal tile lays is not exceeded.
			 */ 
			if (!extra && !validateNormalTileLay(tile)) {
			    errMsg = LocalText.getText("NumberOfNormalTileLaysExceeded", 
			            tile.getColour());
			    break;
			}

			// Sort out cost
			if (hex.getCurrentTile().getId() == hex.getPreprintedTileId())
			{
				cost = hex.getTileCost();
				if (stl != null && stl.isFree()) cost = 0;
			}
			else
			{
				cost = 0;
			}

			// Amount must be non-negative multiple of 10
			if (cost < 0)
			{
				errMsg = LocalText.getText("NegativeAmountNotAllowed",
				        Bank.format(cost));
				break;
			}
			if (cost % 10 != 0)
			{
				errMsg = LocalText.getText("AmountMustBeMultipleOf10",
				        Bank.format(cost));
				break;
			}
			// Does the company have the money?
			if (cost > operatingCompany.getCash())
			{
				errMsg = LocalText.getText("NotEnoughMoney", new String[] {
				        companyName,
				        Bank.format(operatingCompany.getCash()),
				        Bank.format(cost)
				});
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add(LocalText.getText("CannotLayTileOn", new String[] {
			        companyName,
			        tile.getName(),
			        hex.getName(),
			        Bank.format(cost),
			        errMsg
			}));
			return false;
		}

		/* End of validation, start of execution */
	    MoveSet.start();
	    
		if (tile != null)
		{
			hex.upgrade(tile, orientation);

			if (cost > 0) Bank.transferCash((CashHolder) operatingCompany, null, cost);
			tileLayCost[operatingCompanyIndex] = cost;
			tilesLaid[operatingCompanyIndex] = Util.appendWithComma(tilesLaid[operatingCompanyIndex],
					"#" + tile.getName() + "/" + hex.getName() + "/"
							+ MapHex.getOrientationName(orientation)); // FIXME:
																		// Wrong!
			if (cost > 0) {
			    LogBuffer.add(LocalText.getText("LaysTileAt", new String[] {
			            companyName,
			            tile.getName(),
			            hex.getName()
			    }));
			} else {
			    LogBuffer.add(LocalText.getText("LaysTileAtFor", new String[] {
			            companyName,
			            tile.getName(),
			            hex.getName(),
			            Bank.format(cost)
			    }));
			}

			// Was a special property used?
			if (stl != null)
			{
				stl.setExercised();
				currentSpecialTileLays.remove(allowance);
				System.out.println("This was a special tile lay, "+
				        (extra?"":" not")+" extra");
				
			}
			if (!extra)
			{
				System.out.println("This was a normal tile lay");
				registerNormalTileLay (tile);
			}

			setSpecialTileLays();
			System.out.println("There are now "+currentSpecialTileLays.size()+" special tile lay objects");
		}

		// System.out.println("Normal="+normalTileLaysDone+"/"+normalTileLaysAllowed
		// +" special="+extraTileLaysDone+"/"+extraTileLaysAllowed);
		if (tile == null 
		        || currentNormalTileLays.isEmpty() && currentSpecialTileLays.isEmpty())
		{
			nextStep();
		} else {
			updateStatus("layTile");
		}
		MoveSet.finish();

		return true;
	}
	
	protected boolean validateNormalTileLay (TileI tile) {
	    return checkNormalTileLay (tile, false);
	}
	
	protected void registerNormalTileLay (TileI tile) {
	    checkNormalTileLay (tile, true);
	}
	
	protected boolean checkNormalTileLay (TileI tile, boolean update) {
	    
	    if (currentNormalTileLays.isEmpty()) return false;
	    //normalTileLaysDone.add(tile);
	    String colour = tile.getColour();
	    //LayTile allowance = (LayTile) currentNormalTileLays.get(0);
	    //if (allowance == null) return false;
	    
	    // TODO: get(0) - perhaps we always have only one entry?
	    // Probably we have mixed up locations and number-of-tiles-allowed.
	    //Map allowancePerColour = (Map) allowance.getTileColours();
	    Integer oldAllowedNumberObject = ((Integer)tileLaysPerColour.get(colour));
	    if (oldAllowedNumberObject == null) return false;
	    int oldAllowedNumber = oldAllowedNumberObject.intValue();
	    if (oldAllowedNumber <= 0) return false;
	    
	    if (!update) return true;
	    
	    /* We will assume that in all cases the following assertions hold:
	     * 1. If the allowed number for the colour of the just laid tile 
	     * reaches zero, all normal tile lays have been consumed.  
	     * 2. If any colour is laid, no different colours may be laid.
	     * THIS MAY NOT BE TRUE FOR ALL GAMES!
	     */
	    if (oldAllowedNumber <= 1) {
	        tileLaysPerColour.clear();
	        System.out.println("No more normal tile lays allowed");
	        currentNormalTileLays.clear();
	    } else /*oldAllowedNumber > 1*/ {
	        tileLaysPerColour.clear(); // Remove all other colours
	        tileLaysPerColour.put(colour, new Integer(oldAllowedNumber-1));
	        System.out.println((oldAllowedNumber-1)+" more "+colour+" tile lays allowed");
	    }
	    
	    return true;
	}

	public String getLastTileLaid()
	{
		return tilesLaid[operatingCompanyIndex];
	}

	public int getLastTileLayCost()
	{
		return tileLayCost[operatingCompanyIndex];
	}
	
	/**
	 * Check if the allowed number of tile lays would be exceeded.
	 * The check as currently implemented covers 1835 only.
	 * 
	 * This check does specifically not cover games where the choice 
	 * is like "2 yellow or 1 green".
	 * Neither does it cover cases where 2 yellow tiles can be laid only 
	 * in a company's first OR (18EU).
	 * @param tile
	 * @return
	 */
	/*
	private boolean exceedsTilesAllowance (TileI tile) {
	    if (normalTileLaysAllowed == 0) 
	        normalTileLaysAllowed = operatingCompany.getNumberOfTileLays(tile.getColour());
	    return normalTileLaysDone >= normalTileLaysAllowed;
	}
	*/

	private SpecialORProperty checkForUseOfSpecialProperty(MapHex hex)
	{
		if (currentSpecialProperties == null)
			return null;

		Iterator it = currentSpecialProperties.iterator();
		SpecialProperty sp;
		while (it.hasNext())
		{
			sp = (SpecialProperty) it.next();
			if (sp instanceof SpecialTileLay
					&& ((SpecialTileLay) sp).getLocation() == hex)
			{
				return (SpecialORProperty) sp;
			}
		}
		return null;
	}

	/**
	 * A (perhaps temporary) method via which the cost of station token laying
	 * can be accounted for.
	 * 
	 * @param companyName
	 *            The name of the company that lays the token.
	 * @param amountSpent
	 *            The cost of laying the token, which is subtracted from the
	 *            company treasury.
	 * @return
	 */
	public boolean layBaseToken(String companyName, MapHex hex, int station,
	        LayToken allowance)
	{

		String errMsg = null;
		int cost = 0;
		SpecialTokenLay stl = null;
		boolean extra = false;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_LAY_TOKEN)
			{
				errMsg = LocalText.getText("WrongActionNoTokenLay");
				break;
			}

			if (operatingCompany.getNumberOfFreeBaseTokens() == 0)
			{
				errMsg = LocalText.getText("HasNoTokensLeft", companyName);
				break;
			}
			
			if (!hex.hasTokenSlotsLeft(station)) {
			    errMsg = LocalText.getText("CityHasNoEmptySlots");
			    break;
			}
			
			/* TODO: the below condition holds for 1830.
			 * in some games, separate cities on one tile may hold 
			 * tokens of the same company; this case is not yet covered.
			 */
			if (hex.hasTokenOfCompany(operatingCompany)) {
			    errMsg = LocalText.getText("TileAlreadyHasToken", new String[] {
			            hex.getName(),
			            companyName});
			    break;
			}
			
			if (allowance != null) {
			    MapHex location = allowance.getLocation();
			    if (location != null && location != hex) {
			        errMsg = LocalText.getText("TokenLayingHexMismatch", new String[] {
			                hex.getName(),
			                location.getName()});
			        break;
			    }
			    stl = allowance.getSpecialProperty();
			    if (stl != null) extra = stl.isExtra();
			}

			cost = Game.getCompanyManager()
					.getBaseTokenLayCostBySequence(operatingCompany.getNumberOfLaidBaseTokens());
			if (stl != null && stl.isFree()) cost = 0;

			// Does the company have the money?
			if (cost > operatingCompany.getCash())
			{
				errMsg = LocalText.getText("NotEnoughMoney", companyName);
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add(LocalText.getText("CannotLayBaseTokenOn", new String[] {
			        companyName,
			        hex.getName(),
			        Bank.format(cost),
			        errMsg}));
			return false;
		}

		/* End of validation, start of execution */
	    MoveSet.start();
	    
		if (/*operatingCompany.layBaseToken(hex, station)*/
		        hex.layBaseToken(operatingCompany, station)) {
		    /* TODO: the false return value must be impossible. */

		    /* TODO: should the below items be made ModelObjects? */
			baseTokensLaid[operatingCompanyIndex] = Util.appendWithComma(baseTokensLaid[operatingCompanyIndex],
					hex.getName());
			baseTokenLayCost[operatingCompanyIndex] = cost;
	
			if (cost > 0)
			{
				//Bank.transferCash((CashHolder) operatingCompany, null, cost);
			    MoveSet.add (new CashMove (operatingCompany, null, cost));
				LogBuffer.add(LocalText.getText("LAYS_TOKEN_ON", new String[] {
				        companyName,
				        hex.getName(),
				        Bank.format(cost)}));
			}
			else
			{
				LogBuffer.add(LocalText.getText("LAYS_FREE_TOKEN_ON", new String[] {
				        companyName,
				        hex.getName()}));
			}
	
			// Was a special property used?
			if (stl != null)
			{
				stl.setExercised();
				currentSpecialTokenLays.remove(allowance);
				//System.out.println("This was a special token lay, "+
				//        (extra?"":" not")+" extra");
				
			}
			if (!extra)
			{
				currentNormalTokenLays.clear();
				//System.out.println("This was a normal token lay");
			}
			if (currentNormalTokenLays.isEmpty()) {
			    //System.out.println("No more normal token lays are allowed");
			} else {
			    //System.out.println("A normal token lay is still allowed");
			}
			setSpecialTokenLays();
			System.out.println("There are now "+currentSpecialTokenLays.size()+" special token lay objects");
			if (currentNormalTokenLays.isEmpty() && currentSpecialTokenLays.isEmpty())
			{
				nextStep();
			} else {
			    updateStatus("layBaseToken");
			}
	
		}
		MoveSet.finish();

		return true;
	}

	/**
	 * @return The name of the hex where the last Base Token was laid.
	 */
	public String getLastBaseTokenLaid()
	{
		return baseTokensLaid[operatingCompanyIndex];
	}

	/**
	 * @return The cost of the last Base token laid.
	 */
	public int getLastBaseTokenLayCost()
	{
		return baseTokenLayCost[operatingCompanyIndex];
	}

	/**
	 * Set a given revenue. This may be a temporary method. We will have to
	 * enter revenues manually as long as the program cannot yet do the
	 * calculations.
	 * <p>Note: This method is called with a zero amount if there is no revenue.
	 * 
	 * @param amount
	 *            The revenue.
	 * @return False if an error is found.
	 */
	public boolean setRevenue(String companyName, int amount)
	{

		String errMsg = null;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_CALC_REVENUE)
			{
				errMsg = "Wrong action, expected Revenue calculation";
				break;
			}

			// Amount must be non-negative multiple of 10
			if (amount < 0)
			{
				errMsg = "Negative amount not allowed";
				break;
			}
			if (amount % 10 != 0)
			{
				errMsg = "Must be a multiple of 10";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add("Cannot process revenue of " + amount + ": " + errMsg);
			return false;
		}

		revenue[operatingCompanyIndex] = amount;
		LogBuffer.add(companyName + " earns " + Bank.format(amount));

		nextStep();  // NOT IN A MOVESET??

		return true;
	}

	/**
	 * A previously entered revenue is fully paid out as dividend.
	 * <p>
	 * Note: <b>setRevenue()</b> must have been called before this method.
	 * 
	 * @param companyName
	 *            Name of the company paying dividend.
	 * @return False if an error is found.
	 */
	public boolean fullPayout(String companyName)
	{

		String errMsg = null;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_PAYOUT)
			{
				errMsg = "Wrong action, expected Revenue Assignment";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add("Cannot payout revenue of "
					+ Bank.format(revenue[operatingCompanyIndex]) + ": "
					+ errMsg);
			return false;
		}

		LogBuffer.add(companyName + " pays out full dividend of "
				+ Bank.format(revenue[operatingCompanyIndex]));
		
		MoveSet.start();
		operatingCompany.payOut(revenue[operatingCompanyIndex]);
		nextStep();
		MoveSet.finish();

		return true;
	}

	/**
	 * A previously entered revenue is split, i.e. half of it is paid out as
	 * dividend, the other half is retained.
	 * <p>
	 * Note: <b>setRevenue()</b> must have been called before this method.
	 * 
	 * @param companyName
	 *            Name of the company splitting the dividend.
	 * @return False if an error is found. TODO Check if split is allowed. TODO
	 *         The actual payout. TODO Rounding up or down an odd revenue per
	 *         share.
	 */
	public boolean splitPayout(String companyName)
	{

		String errMsg = null;

		// Dummy loop to enable quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_PAYOUT)
			{
				errMsg = "Wrong action, expected Revenue Assignment";
				break;
			}
			// Split must be allowed
			if (splitRule == SPLIT_NOT_ALLOWED)
			{
				errMsg = "Split not allowed";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add("Cannot split revenue of "
					+ Bank.format(revenue[operatingCompanyIndex]) + ": "
					+ errMsg);
			return false;
		}

		LogBuffer.add(companyName + " pays out half dividend");
		MoveSet.start();
		operatingCompany.splitRevenue(revenue[operatingCompanyIndex]);
		nextStep();
		MoveSet.finish();

		return true;
	}

	/**
	 * A previously entered revenue is fully withheld.
	 * <p>
	 * Note: <b>setRevenue()</b> must have been called before this method.
	 * 
	 * @param companyName
	 *            Name of the company withholding the dividend.
	 * @return False if an error is found.
	 */
	public boolean withholdPayout(String companyName)
	{

		String errMsg = null;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_PAYOUT)
			{
				errMsg = "Wrong action, expected Revenue Assignment";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add("Cannot withhold revenue of " + revenue + ": " + errMsg);
			return false;
		}
		LogBuffer.add(companyName + " withholds dividend of "
				+ Bank.format(revenue[operatingCompanyIndex]));

		MoveSet.start();
		operatingCompany.withhold(revenue[operatingCompanyIndex]);

		nextStep();
		MoveSet.finish();

		return true;
	}

	/**
	 * Internal method: change the OR state to the next step. If the currently
	 * Operating Company is done, notify this.
	 * 
	 * @param company
	 *            The current company.
	 */
	protected void nextStep()
	{
		//actionPossible = true;
		//actionNotPossibleMessage = "";

		// Cycle through the steps until we reach one where a user action is expected.
		int step = getStep();
		int stepIndex;
		for (stepIndex = 0; stepIndex < steps.length; stepIndex++) {
		    if (steps[stepIndex] == step) break;
		}
		while (++stepIndex < steps.length)
		{
		    step = steps[stepIndex];
		    
			if (step == STEP_LAY_TOKEN
					&& operatingCompany.getNumberOfFreeBaseTokens() == 0) {
				continue;
			}

			if (step == STEP_CALC_REVENUE) {
			    
			    if (operatingCompany.getPortfolio().getTrains().length == 0)
				{
					// No trains, then the revenue is zero.
					revenue[operatingCompanyIndex] = 0;
					LogBuffer.add(operatingCompany.getName() + " earns " + Bank.format(0));
					continue;
				}
			}
			
			if (step == STEP_PAYOUT) {
			    
				// If we already know what to do: do it.
			    int amount = revenue[operatingCompanyIndex];
				if (amount == 0)
				{
				    /* Zero dividend: process it and go to the next step */
					operatingCompany.withhold(0);
					MessageBuffer.add ("No trains owned, so Revenue is "
							+ Bank.format(0));
					//new Exception("HERE").printStackTrace();
					continue;
				}

				else if (operatingCompany.isSplitAlways())
				{
				    /* Automatic revenue split: process it and go to the next step */
					operatingCompany.splitRevenue(amount);
					continue;
				}
			}

			// No reason found to skip this step
			break;
		}

	    
		if (step >= steps.length) {
			done(operatingCompany.getName());
		} else {
		    setStep(step);
		}
		
		//updateStatus("nextStep");  // REDUNDANT??

	}

	/*
	public boolean isActionAllowed()
	{
		return actionPossible;
	}

	public String getActionNotAllowedMessage()
	{
		return actionNotPossibleMessage;
	}
	*/

	/**
	 * This method is only called at the start of each step
	 * (unlike updateStatus(), which is called aftereach user action)
	 */
	protected void prepareStep()
	{
	    int step = ((Integer)stepObject.getState()).intValue();
	    System.out.println("Prepare step "+step);
		currentPhase = PhaseManager.getInstance().getCurrentPhase();
		
		if (step == STEP_LAY_TRACK)
		{
			//setNormalTileLays();
			tileLayCost[operatingCompanyIndex] = 0;
			tilesLaid[operatingCompanyIndex] = "";
			getNormalTileLays();
		}
		else if (step == STEP_LAY_TOKEN)
		{
			//setNormalTokenLays();
			baseTokenLayCost[operatingCompanyIndex] = 0;
			baseTokensLaid[operatingCompanyIndex] = "";
		}
		else
		{
			currentSpecialProperties = null;
		}
		
	}
	
	protected void setSpecialProperties (Class clazz) {
		currentSpecialProperties = operatingCompany.getPortfolio()
			.getSpecialProperties(clazz, false);
		currentSpecialProperties.addAll (operatingCompany.getPresident().getPortfolio()
		    .getSpecialProperties(clazz, false));

	}
	
	
	/**
	 * Create a List of allowed normal tile lays (see LayTile class).
	 * This method should be called only once per company turn in an OR:
	 * at the start of the tile laying step.
	 */
	protected void getNormalTileLays() {
	    
	    tileLaysPerColour = new HashMap (currentPhase.getTileColours()); //Clone it.
	    String colour;
	    int allowedNumber;
	    for (Iterator it = tileLaysPerColour.keySet().iterator(); it.hasNext(); ) {
	        colour = (String) it.next();
	        allowedNumber = operatingCompany.getNumberOfTileLays(colour);
	        // Replace the null map value with the allowed number of lays
	        tileLaysPerColour.put(colour, new Integer (allowedNumber));
	    }
	}
	protected void setNormalTileLays() {
	    
	    /* Normal tile lays */
	    currentNormalTileLays.clear();
	    if (!tileLaysPerColour.isEmpty()) {
	        currentNormalTileLays.add (new LayTile (tileLaysPerColour));
	    }

	}
	
	/**
	 * Create a List of allowed special tile lays (see LayTile class).
	 * This method should be called before each user action in the tile laying step.
	 */
	protected void setSpecialTileLays() {
	    
	    /* Special-property tile lays */
		currentSpecialTileLays.clear();
		specialPropertyPerHex.clear();
		/* In 1835, this only applies to major companies.
		 * TODO: For now, hardcode this, but it must become configurable later.
		 */
		if (operatingCompany.getType().getName().equals("Minor")) return;
		
		setSpecialProperties(game.special.SpecialTileLay.class);
		if (currentSpecialProperties != null && !currentSpecialProperties.isEmpty())
		{
			Iterator it = currentSpecialProperties.iterator();
			while (it.hasNext())
			{
				Object o = it.next();
				System.out.println("Spec.prop: "+o);
				SpecialTileLay stl = (SpecialTileLay) o;
				if (stl.isExtra() || !currentNormalTileLays.isEmpty()) {
				    /* If the special tile lay is not extra, it is only 
				     * allowed if normal tile lays are also (still) allowed */
					specialPropertyPerHex.put(stl.getLocation(), stl);
					currentSpecialTileLays.add (new LayTile (stl));
				}
			}
		}
	}
	
	protected void setNormalTokenLays () {
	    
	    /* Normal token lays */
	    currentNormalTokenLays.clear();
	    
	    /* For now, we allow one token of the currently operating company */
	    if (operatingCompany.getNumberOfFreeBaseTokens() > 0) {
	        currentNormalTokenLays.add (new LayToken (null, operatingCompany));
	    }
	    
	}

	/**
	 * Create a List of allowed special token lays (see LayToken class).
	 * This method should be called before each user action in the base token laying step.
	 * TODO: Token preparation is practically identical to Tile preparation,
	 * perhaps the two can be merged to one generic procedure.
	 */
	protected void setSpecialTokenLays() {
	    
	    /* Special-property tile lays */
		currentSpecialTokenLays.clear();
		specialPropertyPerHex.clear();

		/* In 1835, this only applies to major companies.
		 * TODO: For now, hardcode this, but it must become configurable later.
		 */
		if (operatingCompany.getType().getName().equals("Minor")) return;

		setSpecialProperties(game.special.SpecialTokenLay.class);
		if (currentSpecialProperties != null)
		{
			Iterator it = currentSpecialProperties.iterator();
			while (it.hasNext())
			{
				SpecialTokenLay stl = (SpecialTokenLay) it.next();
				System.out.println("Spec.prop:"+stl);
				if (stl.isExtra() || !currentNormalTokenLays.isEmpty()) {
				    /* If the special tile lay is not extra, it is only 
				     * allowed if normal tile lays are also (still) allowed */
					specialPropertyPerHex.put(stl.getLocation(), stl);
					currentSpecialTokenLays.add (new LayToken (stl));
				}
			}
		}
	}

	public List getSpecialProperties()
	{
		return currentSpecialProperties;
	}

	public void skip(String compName)
	{
	    /* TODO Should insert some validation here, as this method
	     * is called from the GUI.
	     */
	    System.out.println("Skip step "+((Integer)stepObject.getState()).intValue());
	    MoveSet.start();
		nextStep();
		MoveSet.finish();
		//updateStatus ("Skip");

	}

	/**
	 * The current Company is done operating.
	 * 
	 * @param company
	 *            Name of the company that finished operating.
	 * @return False if an error is found.
	 */
	public boolean done(String companyName)
	{
		String errMsg = null;

		if (!companyName.equals(operatingCompany.getName()))
		{
			errMsg = LocalText.getText("WrongCompany", new String[] {
			        companyName,
			        operatingCompany.getName()});
			return false;
		}
		
		if(operatingCompany.getPortfolio().getTrains().length == 0
		        && operatingCompany.mustOwnATrain())
		{
			//FIXME: Need to check for valid route before throwing an error.
			errMsg = companyName + " owns no trains.";
			setStep(STEP_BUY_TRAIN);
			MessageBuffer.add(errMsg);
			return false;
		}
		
		MoveSet.clear();

		if (++operatingCompanyIndex >= operatingCompanyArray.length)
		{
			// OR done. Inform GameManager.
			LogBuffer.add("End of Operating Round " + getCompositeORNumber());
			operatingCompany = null;
			stepObject.deleteObserver(this);
			stepObject = null;
			GameManager.getInstance().nextRound(this);
			return true;
		}

		operatingCompany = operatingCompanyArray[operatingCompanyIndex];
		//normalTileLaysDone.clear();
		setStep (STEP_INITIAL);
		//prepareStep();
		//updateStatus("Done");

		return true;
	}

	/**
	 */
	public boolean buyTrain(String companyName, BuyableTrain bTrain, int price)
	{

		return buyTrain(companyName, bTrain, price, null);
	}

	public boolean buyTrain(String companyName, BuyableTrain bTrain, int price,
			TrainI exchangedTrain)
	{

	    TrainI train = null;
		String errMsg = null;
		int presidentCash = 0;
		boolean presidentMustSellShares = false;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Portfolio oldHolder = train.getHolder();
			// CashHolder oldOwner = oldHolder.getOwner();

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}
			// Must be correct step
			if (getStep() != STEP_BUY_TRAIN)
			{
				errMsg = LocalText.getText("WrongActionNoTrainBuyingCost");
				break;
			}

			if (bTrain == null || (train = bTrain.getTrain()) == null)
			{
				errMsg = LocalText.getText("NoTrainSpecified");
				break;
			}

			// Zero price means face value.
			if (price == 0)
				price = train.getCost();

			// Amount must be non-negative
			if (price < 0)
			{
				errMsg = LocalText.getText("NegativeAmountNotAllowed",
				        Bank.format(price));
				break;
			}

			// Does the company have room for another train?
			int currentNumberOfTrains = operatingCompany.getPortfolio()
					.getTrains().length;
			int trainLimit = operatingCompany.getTrainLimit(PhaseManager.getInstance()
					.getCurrentPhaseIndex());
			if (currentNumberOfTrains >= trainLimit)
			{
				errMsg = LocalText.getText("WouldExceedTrainLimit", 
				        String.valueOf(trainLimit));
				break;
			}
			
			/* Check if this is an emergency buy */
			Player currentPlayer = operatingCompany.getPresident();
			if (bTrain.mustPresidentAddCash()) {
			    // From the Bank
		        presidentCash = bTrain.getPresidentCashToAdd();
			    if (currentPlayer.getCash() >= presidentCash) {
			        Bank.transferCash(currentPlayer, operatingCompany, presidentCash);
			    } else {
			        presidentMustSellShares = true;
			        cashToBeRaisedByPresident = presidentCash - currentPlayer.getCash();
			    }
			} else if (bTrain.mayPresidentAddCash()) {
			    // From another company
			    presidentCash = price - operatingCompany.getCash();
			    if (presidentCash > bTrain.getPresidentCashToAdd()) {
			        errMsg = LocalText.getText("PresidentMayNotAddMoreThan", 
			            Bank.format (bTrain.getPresidentCashToAdd()));
			        break;
			    } else if (currentPlayer.getCash() >= presidentCash) {
			        Bank.transferCash(currentPlayer, operatingCompany, presidentCash);
			    } else {
			        presidentMustSellShares = true;
			        cashToBeRaisedByPresident = presidentCash - currentPlayer.getCash();
			    }
			    
			} else {
			    // No forced buy - does the company have the money?
				if (price > operatingCompany.getCash())
				{
					errMsg = LocalText.getText("NotEnoughMoney", new String[] {
					        companyName,
					        Bank.format(operatingCompany.getCash()),
					        Bank.format(price)
					});
					break;
				}
			}

			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add(LocalText.getText("CannotBuyTrainFor", new String[] {
			        companyName,
			        train.getName(),
			        Bank.format(price),
			        errMsg}));
			return false;
		}
		
		if (presidentMustSellShares) {
		    savedBuyableTrain = bTrain;
		    savedPrice = price;

			GameManager.getInstance().startShareSellingRound (this, operatingCompany, 
			        cashToBeRaisedByPresident);

		    return true;
		}

		Portfolio oldHolder = train.getHolder();
		CashHolder oldOwner = oldHolder.getOwner();

		if (exchangedTrain != null)
		{
			TrainI oldTrain = operatingCompany.getPortfolio()
					.getTrainOfType(exchangedTrain.getType());
			Bank.getPool().buyTrain(oldTrain, 0);
			LogBuffer.add(LocalText.getText("ExchangesTrain", new String[] {
			        companyName,
			        exchangedTrain.getName(),
			        train.getName(),
			        oldHolder.getName(),
			        Bank.format(price)}));
		}
		else
		{
			LogBuffer.add(LocalText.getText("BuysTrain", new String[] {
			        companyName,
			        train.getName(),
			        oldHolder.getName(),
					Bank.format(price)}));
		}

		operatingCompany.buyTrain(train, price);
		if (oldHolder == Bank.getIpo()) train.getType().addToBoughtFromIPO();
		trainBuyCost[operatingCompanyIndex] += price;

		TrainManager.get().checkTrainAvailability(train, oldHolder);
		currentPhase = GameManager.getCurrentPhase();
		
		updateStatus("buyTrain");
		
		return true;
	}
	
	public void resumeTrainBuying () {
	    
	    buyTrain (operatingCompany.getName(), savedBuyableTrain, savedPrice);
	    savedBuyableTrain = null;
	}

	public int getLastTrainBuyCost()
	{
		return trainBuyCost[operatingCompanyIndex];
	}

	/**
	 * Let a public company buy a private company.
	 * 
	 * @param company
	 *            Name of the company buying a private company.
	 * @param privateName
	 *            Name of teh private company.
	 * @param price
	 *            Price to be paid.
	 * @return False if an error is found. TODO: Is private buying allowed at
	 *         all? TODO: Is the game phase correct?
	 */
	public boolean buyPrivate(String companyName, String privateName, int price)
	{

		String errMsg = null;
		PrivateCompanyI privCo = null;
		CashHolder owner = null;
		Player player = null;
		int basePrice;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = LocalText.getText("WrongCompany", new String[] {
				        companyName,
				        operatingCompany.getName()});
				break;
			}

			// Does private exist?
			if ((privCo = Game.getCompanyManager()
					.getPrivateCompany(privateName)) == null)
			{
				errMsg = LocalText.getText("PrivateDoesNotExist", privateName);
				break;
			}
			// Is private still open?
			if (privCo.isClosed())
			{
				errMsg = LocalText.getText("PrivateIsAlreadyClosed", privateName);
				break;
			}
			// Is private owned by a player?
			owner = privCo.getPortfolio().getOwner();
			if (!(owner instanceof Player))
			{
				errMsg = LocalText.getText("PrivateIsNotOwnedByAPlayer", privateName);
				break;
			}
			player = (Player) owner;
			basePrice = privCo.getBasePrice();

			// Is private buying allowed?
			if (!currentPhase.isPrivateSellingAllowed())
			{
				errMsg = LocalText.getText("PrivateBuyingIsNotAllowed");
				break;
			}

			// Price must be in the allowed range
			if (price < basePrice
					* operatingCompany.getLowerPrivatePriceFactor())
			{
				errMsg = LocalText.getText("PriceBelowLowerLimit", new String[] {
				        Bank.format(price),
				        Bank.format((int)(basePrice * operatingCompany.getLowerPrivatePriceFactor())),
				        privateName
				});
				break;
			}
			if (price > basePrice
					* operatingCompany.getUpperPrivatePriceFactor())
			{
				errMsg = LocalText.getText("PriceAboveUpperLimit", new String[] {
				        Bank.format(price),
				        Bank.format((int)(basePrice * operatingCompany.getUpperPrivatePriceFactor())),
				        privateName
				});
				break;
			}
			// Does the company have the money?
			if (price > operatingCompany.getCash())
			{
				errMsg = LocalText.getText("NotEnoughMoney", new String[] {
				        companyName,
				        Bank.format(operatingCompany.getCash()),
				        Bank.format(price)
				});
				break;
			}
			break;
		}
		if (errMsg != null)
		{
		    if (owner != null) {
		        MessageBuffer.add(LocalText.getText("CannotBuyPrivateFromFor", new String[] {
		                privateName,
		                owner.getName(),
		                Bank.format(price),
		                errMsg
		        }));
		    } else {
		        MessageBuffer.add(LocalText.getText("CannotBuyPrivateFor", new String[] {
		                privateName,
		                Bank.format(price),
		                errMsg
		        }));
		    }
			return false;
		}

		operatingCompany.getPortfolio().buyPrivate(privCo,
				player.getPortfolio(),
				price);
		privateBuyCost[operatingCompanyIndex] += price;

		// We may have got an extra tile lay right
		//setSpecialTileLays();
		
		updateStatus("buyPrivate");

		return true;

	}

	public int getLastPrivateBuyCost()
	{
		return privateBuyCost[operatingCompanyIndex];
	}

	/**
	 * Close a private. For now, this is an action to be initiated separately
	 * from the UI, but it will soon be coupled to the actual actions that
	 * initiate private closing. By then, this method will probably no longer be
	 * accessible from the UI, which why it is deprecated from its creation.
	 * 
	 * @param privateName
	 *            name of the private to be closed.
	 * @return False if an error occurs.
	 * @deprecated Will probably move elsewhere and become not accessible to the
	 *             UI.
	 */
	public boolean closePrivate(String privateName)
	{
		String errMsg = null;
		PrivateCompanyI privCo = null;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Does private exist?
			if ((privCo = Game.getCompanyManager()
					.getPrivateCompany(privateName)) == null)
			{
				errMsg = LocalText.getText("PrivateDoesNotExist", privateName);
				break;
			}
			// Is private still open?
			if (privCo.isClosed())
			{
				errMsg = LocalText.getText("PrivateIsAlreadyClosed", privateName);
				break;
			}

			break;
		}
		if (errMsg != null)
		{
			MessageBuffer.add(LocalText.getText("CannotClosePrivate", new String[] {
			        privateName,
			        errMsg
			}));
			return false;
		}

		privCo.setClosed();
		LogBuffer.add(LocalText.getText("PRIVATE_IS_CLOSED", privateName));

		return true;

	}
	
	/*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

	/**
	 * @return The player that has the turn (in this case: the President of the
	 *         currently operating company).
	 */
	public Player getCurrentPlayer()
	{
		return operatingCompany.getPresident();
	}

	/**
	 * Get the public company that has the turn to operate.
	 * 
	 * @return The currently operating company object.
	 */
	public PublicCompanyI getOperatingCompany()
	{
		return operatingCompany;
	}

	public PublicCompanyI[] getOperatingCompanies()
	{
		return operatingCompanyArray;
	}

	/**
	 * Get the current operating round step (i.e. the next action).
	 * 
	 * @return The number that defines the next action.
	 */
	public int getStep()
	{
		return ((Integer)stepObject.getState()).intValue();
	}
	
	/**
	 * Bypass normal order of operations and explicitly set round step.
	 * This should only be done for specific game exceptions, such as forced train purchases.
	 * 
	 * @param step
	 */
	protected void setStep(int step)
	{
	    if (stepObject == null) {
	        stepObject = new StateObject("ORStep", Integer.class);
	        stepObject.addObserver(this);
	    }
	    MoveSet.add(new StateChange (stepObject, new Integer(step)));
		
		prepareStep();
		updateStatus("setStep");
	}

	public int getOperatingCompanyIndex()
	{
		return operatingCompanyIndex;
	}
	
	protected void updateStatus (String fromWhere) {
	    
	    System.out.println (">>> updateStatus called from "+fromWhere);
	    updateStatus();
	    
	}
	/**
	 * To be called after each change, to re-establish the currently allowed actions.
	 * (new method, intended to absorb code from several other methods). 
	 *
	 */
	protected void updateStatus () {
	    
		/* Create a new list of possible actions for the UI */
		possibleActions.clear();
		
		int step = getStep();

		if (step == STEP_LAY_TRACK)
		{
		    setNormalTileLays();
			setSpecialTileLays();
			System.out.println("Normal tile lays: "+currentNormalTileLays.size());
			System.out.println("Special tile lays: "+currentSpecialTileLays.size());

			possibleActions.addAll (currentNormalTileLays);
			possibleActions.addAll (currentSpecialTileLays);
		}
		else if (step == STEP_LAY_TOKEN) 
		{
		    setNormalTokenLays();
		    setSpecialTokenLays();
			System.out.println("Normal token lays: "+currentNormalTokenLays.size());
			System.out.println("Special token lays: "+currentSpecialTokenLays.size());

			possibleActions.addAll (currentNormalTokenLays);
			possibleActions.addAll (currentSpecialTokenLays);
		}
		
		// Can private companies be bought?
		if (GameManager.getCurrentPhase().isPrivateSellingAllowed()) {
		    PrivateCompanyI privComp;
		    int minPrice, maxPrice;
		    for (Iterator it = Game.getCompanyManager().getPrivatesOwnedByPlayers().iterator();
		    		it.hasNext(); ) {
		        privComp = (PrivateCompanyI) it.next();
		        minPrice = (int) (privComp.getBasePrice() * operatingCompany
                        .getLowerPrivatePriceFactor());
		        maxPrice = (int) (privComp.getBasePrice() * operatingCompany
                        .getUpperPrivatePriceFactor());
		        possibleActions.add (new BuyPrivate (privComp, minPrice, maxPrice));
		    }
		}

	}

	/**
	 * Get a list of private companies that are available for buying, i.e. which
	 * are in the hands of players.
	 * 
	 * @return An array of the buyable privates. TODO Check if privates can be
	 *         bought at all.
	 */
	public PrivateCompanyI[] getBuyablePrivates()
	{
		ArrayList buyablePrivates = new ArrayList();
		PrivateCompanyI privCo;
		Iterator it = Game.getCompanyManager()
				.getAllPrivateCompanies()
				.iterator();
		while (it.hasNext())
		{
			if ((privCo = (PrivateCompanyI) it.next()).getPortfolio()
					.getOwner() instanceof Player)
				buyablePrivates.add(privCo);
		}
		return (PrivateCompanyI[]) buyablePrivates.toArray(new PrivateCompanyI[0]);
	}
	
	/** 
	 * Get a list of buyable trains for the currently operating company.
	 * Omit trains that the company has no money for. If there is no cash to
	 * buy any train from the Bank, prepare for emergency train buying.
	 * @return List of all trains that could potentially be bought.
	 */
	public List getBuyableTrains() {
	    
	    if (operatingCompany == null) return null;
	    
	    int cash = operatingCompany.getCash();
	    int cost;
	    List buyableTrains = new ArrayList();
	    List trains;
	    TrainI train;
	    boolean hasTrains = operatingCompany.getPortfolio().getTrains().length > 0;
	    boolean presidentMayHelp = false;
	    TrainI cheapestTrain = null;
	    int costOfCheapestTrain = 0;
	    
	    /* New trains */
        trains =  TrainManager.get().getAvailableNewTrains();
        for (Iterator it = trains.iterator(); it.hasNext(); ) {
            train = (TrainI) it.next();
            cost = train.getCost();
            if (cost <= cash) {
                buyableTrains.add (new BuyableTrain (train, cost));
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }
            if (train.canBeExchanged() && hasTrains) {
                cost = train.getType().getFirstExchangeCost();
                if (cost <= cash) buyableTrains.add (new BuyableTrain (train, cost).setForExchange());
            }
        }
        
        /* Used trains */
        trains = Bank.getPool().getUniqueTrains();
		for (Iterator it = trains.iterator(); it.hasNext();) {
		    train = (TrainI) it.next();
		    cost = train.getCost();
		    if (cost <= cash) {
		        buyableTrains.add (new BuyableTrain (train, cost));
		    } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
		        costOfCheapestTrain = cost;
		    }
		}
		if (!hasTrains && buyableTrains.isEmpty()) {
		    buyableTrains.add (new BuyableTrain (cheapestTrain, costOfCheapestTrain)
		            .setPresidentMustAddCash(costOfCheapestTrain - cash));
		    presidentMayHelp = true;
		}
		
		/* Other company trains */
		PublicCompanyI c;
		BuyableTrain bt;
		for (int j = 0; j < operatingCompanyArray.length; j++) {
			c = operatingCompanyArray[j];
			if (c == operatingCompany) continue;
			trains = c.getPortfolio().getUniqueTrains();
			for (Iterator it = trains.iterator(); it.hasNext();) {
			    train = (TrainI) it.next();
			    bt = new BuyableTrain (train, 0);
			    if (presidentMayHelp && cash < train.getCost()) {
			        bt.setPresidentMayAddCash(train.getCost() - cash);
			    }
			    buyableTrains.add (bt);
			}
		}
    
	    return buyableTrains;
	}
	
	public boolean isOperatingCompanyAtTrainLimit() {
	    
	    return operatingCompany.getPortfolio().getTrains().length 
	    		>= operatingCompany.getTrainLimit(currentPhase.getIndex());
	}
	
	/**
	 * Chech if revenue may be split.
	 * 
	 * @return True if revenue can be split.
	 */
	public boolean isSplitAllowed()
	{
		return (splitRule != SPLIT_NOT_ALLOWED);
	}

	/**
	 * Get all possible token laying costs in a game. This is a (perhaps
	 * temporary) method to play without a map.
	 * 
	 * @author Erik Vos
	 */
	public int[] getTokenLayCosts()
	{
		// Result is currently hardcoded, but can be made configurable.
		return new int[] { 0, 40, 100 };
	}

	/* TODO This is just a start of a possible approach to a Help system */
	public String getHelp()
	{
	    int step = getStep();
		StringBuffer b = new StringBuffer();
		b.append("<big>Operating round: ")
				.append(getCompositeORNumber())
				.append("</big><br>");
		b.append("<br><b>")
				.append(operatingCompany.getName())
				.append("</b> (president ")
				.append(getCurrentPlayer().getName())
				.append(") has the turn.");
		b.append("<br><br>Currently allowed actions:");
		if (step == STEP_LAY_TRACK)
		{
			b.append("<br> - Lay a tile");
			b.append("<br> - Press 'Done' if you do not want to lay a tile");
		}
		else if (step == STEP_LAY_TOKEN)
		{
			b.append("<br> - Lay a base token or press Done");
			b.append("<br> - Press 'Done' if you do not want to lay a base");
		}
		else if (step == STEP_CALC_REVENUE)
		{
			b.append("<br> - Enter new revenue amount");
			b.append("<br> - Press 'Done' if your revenue is zero");
		}
		else if (step == STEP_PAYOUT)
		{
			b.append("<br> - Choose how the revenue will be paid out");
		}
		else if (step == STEP_BUY_TRAIN)
		{
			b.append("<br> - Buy one or more trains");
			b.append("<br> - Press 'Done' to finish your turn");
		}
		/* TODO: The below if needs be refined. */
		if (GameManager.getCurrentPhase().isPrivateSellingAllowed()
				&& step != STEP_PAYOUT)
		{
			b.append("<br> - Buy one or more Privates");
		}

		if (step == STEP_LAY_TRACK)
		{
			b.append("<br><br><b>Tile laying</b> proceeds as follows:");
			b.append("<br><br> 1. On the map, select the hex that you want to lay a new tile upon.");
			b.append("<br>If tile laying is allowed on this hex, the current tile will shrink a bit <br>and a red background will show up around its edges;");
			b.append("<br>in addition, the tiles that can be laid on that hex will be displayed<br> in the 'upgrade panel' at the left hand side of the map.");
			b.append("<br>If tile laying is not allowed there, nothing will happen.");
			b.append("<br><br> 2. Select a tile in the upgrade panel.<br>This tile will be copied to the selected hex,<br>in some orientation");
			b.append("<br><br> 3. If you want to turn the tile just laid to a different orientation, click it.");
			b.append("<br>Repeatedly clicking the tile will rotate it through all allowed orientations.");
			b.append("<br><br> 4. Confirm tile laying by clicking 'Done'");
			b.append("<br><br>Before 'Done' has been pressed, you can change your mind<br>as often as you want");
			b.append(" (presuming that the other players don't get angry).");
			b.append("<br> - If you want to select another hex: repeat step 1");
			b.append("<br> - If you want to lay another tile on the currently selected hex: repeat step 2.");
			b.append("<br> - If you want to undo hex selection: click outside of the map hexes.");
			b.append("<br> - If you don't want to lay a tile after all: press 'Cancel'");
		}

		return b.toString();
	}
	
	/**
	 * Update the status if the step has changed by an Undo or Redo
	 */
	public void update (Observable observable, Object object) {
	    if (observable == stepObject) {
	        prepareStep();
	        updateStatus();
	    }
	}
	
	public void undo() {
	    MoveSet.undo();
	    updateStatus("undo");
	}
	public void redo() {
	    MoveSet.redo();
	    updateStatus("redo");
	}

}
