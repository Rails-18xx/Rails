/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

import game.special.*;

import java.util.*;

/**
 * Implements a basic Operating Round.
 * <p>
 * A new instance must be created for each new Operating Round. At the end of a
 * round, the current instance should be discarded.
 * <p>
 * Permanent memory is formed by static attributes.
 * 
 * @author Erik Vos
 */
public class OperatingRound implements Round
{

	/* Transient memory (per round only) */
	protected Player currentPlayer;
	protected int currentPlayerIndex;
	protected int step;

	protected TreeMap operatingCompanies;
	protected PublicCompanyI[] operatingCompanyArray;
	protected int operatingCompanyIndex = 0;
	protected PublicCompanyI operatingCompany;

	protected int currentRevenue;
	protected int lastTileLayCost = 0;
	protected String lastTileLaid = "";
	protected int lastBaseTokenLayCost = 0;
	protected String lastBaseTokenLaid = "";
	
	protected List currentSpecialProperties = null;
	
	/** Number of tiles that may be laid. 
	 * TODO: This does not cover cases like "2 yellow or 1 upgrade allowed".
	 */
	protected int normalTileLaysAllowed = 1;
	protected int normalTileLaysDone = 0;
	protected int extraTileLaysAllowed = 0;
	protected int extraTileLaysDone = 0;

	protected int splitRule = SPLIT_NOT_ALLOWED; // To be made configurable

	/* Permanent memory */
	static protected Player[] players;
	static protected PublicCompanyI[] companies;
	static protected int relativeORNumber = 0;
	static protected int cumulativeORNumber = 0;

	/* Constants */
	public static final int SPLIT_NOT_ALLOWED = 0;
	public static final int SPLIT_ROUND_UP = 1; // More money to the
												// shareholders
	public static final int SPLIT_ROUND_DOWN = 2; // More to the treasury

	public static final int STEP_LAY_TRACK = 0;
	public static final int STEP_LAY_TOKEN = 1;
	public static final int STEP_CALC_REVENUE = 2;
	public static final int STEP_PAYOUT = 3;
	public static final int STEP_BUY_TRAIN = 4;
	public static final int STEP_FINAL = 5;
	protected static int[] steps = new int[] { STEP_LAY_TRACK, 
			STEP_LAY_TOKEN,
			STEP_CALC_REVENUE, 
			STEP_PAYOUT, 
			STEP_BUY_TRAIN, 
			STEP_FINAL };

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
			space = company.getCurrentPrice();
			// Key must put companies in reverse operating order, because sort
			// is ascending.
			if (company.hasStockPrice())
			{
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
		step = steps[0];

		relativeORNumber++;
		cumulativeORNumber++;

		Log.write("\nStart of Operating Round " + getCompositeORNumber());

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
			
			// prepare any specials
			prepareStep(step);
		}
		else
		{
			// No operating companies yet: close the round.
			Log.write("End of Operating Round" + getCompositeORNumber());
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

	/**
	 * @deprecated Currently needed, but will be removed in a later stage.
	 */
	public static void resetRelativeORNumber()
	{
		relativeORNumber = 0;
	}

	/*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/

	/**
	 * A (perhaps temporary) method via which the cost of track laying can be
	 * accounted for.
	 * 
	 * @param companyName
	 *            The name of the company that lays the track.
	 * @param amountSpent
	 *            The cost of laying the track, which is subtracted from the
	 *            company treasury.
	 */
	public boolean layTile(String companyName, MapHex hex, TileI tile,
			int orientation)
	{

		String errMsg = null;
		int cost = 0;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_LAY_TRACK)
			{
				errMsg = "Wrong action, expected Tile laying cost";
				break;
			}

			if (tile != null)
			{

				// Sort out cost
				if (hex.getCurrentTile().getId() == hex.getPreprintedTileId())
				{
					cost = hex.getTileCost();
				}
				else
				{
					cost = 0;
				}

				// Amount must be non-negative multiple of 10
				if (cost < 0)
				{
					errMsg = "Negative amount not allowed";
					break;
				}
				if (cost % 10 != 0)
				{
					errMsg = "Amount must be a multiple of 10";
					break;
				}
				// Does the company have the money?
				if (cost > operatingCompany.getCash())
				{
					errMsg = "Not enough money";
					break;
				}
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot process tile laying: " + errMsg);
			return false;
		}

		if (tile != null)
		{
			hex.upgrade(tile, orientation);

			Bank.transferCash((CashHolder) operatingCompany, null, cost);
			lastTileLayCost = cost;
			lastTileLaid = "#" + tile.getName() + "/" + hex.getName() + "/"
				+ MapHex.getOrientationName(orientation); // FIXME: Wrong!
			Log.write(operatingCompany.getName() + " lays tile "
			        + lastTileLaid
					+ (cost > 0 ? " for " + Bank.format(cost) : ""));
			
			// Was a special property used?
			SpecialTileLay stl = (SpecialTileLay) checkForUseOfSpecialProperty (hex);
			if (stl != null) {
			    System.out.println("A special property of "+stl.getCompany().getName()+" is used");
			    stl.setExercised();
			    if (stl.isExtra()) extraTileLaysDone++;
			    else normalTileLaysDone++;
		        currentSpecialProperties = 
		            operatingCompany.getPortfolio().getSpecialProperties(game.special.SpecialTileLay.class);
			} else {
			    normalTileLaysDone++;
			}
		}

		if (tile == null || 
		        normalTileLaysDone >= normalTileLaysAllowed 
		        && extraTileLaysDone >= extraTileLaysAllowed) {
		    nextStep(operatingCompany);
		}

		return true;
	}
	
	public String getLastTileLaid () {
	    return lastTileLaid;
	}
	public int getLastTileLayCost()
	{
		return lastTileLayCost;
	}
	
 	private SpecialORProperty checkForUseOfSpecialProperty (MapHex hex) {
	    if (currentSpecialProperties == null) return null;

	    Iterator it = currentSpecialProperties.iterator();
	    SpecialProperty sp;
	    while (it.hasNext()) {
	        sp = (SpecialProperty) it.next();
	        if (sp instanceof SpecialTileLay
	                && ((SpecialTileLay)sp).getLocation() == hex) {
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
	public boolean layBaseToken(String companyName, MapHex hex)
	{

		String errMsg = null;
		int cost = 0;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_LAY_TOKEN)
			{
				errMsg = "Wrong action, not expecting Token lay";
				break;
			}

			if (!operatingCompany.hasTokens()) {
			    errMsg = "Company has no more tokens";
			    break;
			}
			cost = Game.getCompanyManager().getBaseTokenLayCostBySequence(operatingCompany.getNextBaseTokenIndex());

			// Does the company have the money?
			if (cost > operatingCompany.getCash())
			{
				errMsg = "Not enough money";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot process token laying on "+hex.getName()
			        + " for " + Bank.format(cost) 
					+ ": " + errMsg);
			return false;
		}

		operatingCompany.layBaseToken(hex);
		lastBaseTokenLaid = hex.getName(); // Need to specify station!
		lastBaseTokenLayCost = cost;
		
		if (cost > 0) {
		    Bank.transferCash((CashHolder) operatingCompany, null, cost);
			Log.write(companyName + " lays a token on " + hex.getName()
			        + " for " + Bank.format(cost));
		} else {
		    Log.write (companyName +" lays a free token on "+hex.getName());
		}

		nextStep(operatingCompany);

		return true;
	}

	   /**
     * @return The name of the hex where the last Base Token was laid.
     */
    public String getLastBaseTokenLaid() {
        return lastBaseTokenLaid;
    }
    /**
     * @return The cost of the last Base token laid.
     */
    public int getLastBaseTokenLayCost() {
        return lastBaseTokenLayCost;
    }

	/**
	 * Set a given revenue. This may be a temporary method. We will have to
	 * enter revenues manually as long as the program cannot yet do the
	 * calculations.
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
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_CALC_REVENUE)
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
			Log.error("Cannot process revenue of " + amount + ": " + errMsg);
			return false;
		}

		currentRevenue = amount;
		Log.write(companyName + " earns " + Bank.format(amount));

		nextStep(operatingCompany);

		// If we already know what to do: do it.
		if (amount == 0)
		{
			operatingCompany.withhold(0);
			nextStep(operatingCompany);
		}
		else if (operatingCompany.isSplitAlways())
		{
			operatingCompany.splitRevenue(amount);
			nextStep(operatingCompany);
		}

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
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_PAYOUT)
			{
				errMsg = "Wrong action, expected Revenue Assignment";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot payout revenue of " + currentRevenue + ": "
					+ errMsg);
			return false;
		}

		Log.write(companyName + " pays out full dividend of "
				+ Bank.format(currentRevenue));
		operatingCompany.payOut(currentRevenue);

		nextStep(operatingCompany);

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

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_PAYOUT)
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
			Log.error("Cannot split revenue of " + Bank.format(currentRevenue)
					+ ": " + errMsg);
			return false;
		}

		Log.write(companyName + " pays out half dividend");
		operatingCompany.splitRevenue(currentRevenue);
		nextStep(operatingCompany);

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
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_PAYOUT)
			{
				errMsg = "Wrong action, expected Revenue Assignment";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot withhold revenue of " + currentRevenue + ": "
					+ errMsg);
			return false;
		}
		Log.write(companyName + " withholds dividend of "
				+ Bank.format(currentRevenue));

		operatingCompany.withhold(currentRevenue);

		nextStep(operatingCompany);

		return true;
	}

	/**
	 * Internal method: change the OR state to the next step. If the currently
	 * Operating Company is done, notify this.
	 * 
	 * @param company
	 *            The current company.
	 */
	protected void nextStep(PublicCompanyI company)
	{
	    // Cycle through the steps until we reach one where action is allowed. 
		while (++step < steps.length) {
		    
		    if (step == STEP_LAY_TOKEN 
		            && operatingCompany.getNumCityTokens() == 0) continue;
		    
		    if (step == STEP_CALC_REVENUE
		            && operatingCompany.getPortfolio().getTrains().length == 0) {
		        // No trains, then the revenue is zero.
		        setRevenue (operatingCompany.getName(), 0);
		        // which will call this method again twice,
		        // so by now the step will be increased to STEP_BUY_TRAIN.
		    }
		    
	    
		    // No reason found to skip this step
		    return;
		}
		
	    if (step >= steps.length) done(company.getName());
		
	}
	
	protected void prepareStep (int step) {
	    
	    if (step == STEP_LAY_TRACK) {

	        normalTileLaysDone = 0;
	        extraTileLaysAllowed = 0;
	        extraTileLaysDone = 0;
	        lastTileLayCost = 0;
	        lastTileLaid = "";
	        
	        // Check for extra or special tile lays
	        currentSpecialProperties = 
	            operatingCompany.getPortfolio().getSpecialProperties(game.special.SpecialTileLay.class);
	        if (currentSpecialProperties != null) {
	            Iterator it = currentSpecialProperties.iterator();
	            while (it.hasNext()) {
	                if (((SpecialTileLay)it.next()).isExtra()) extraTileLaysAllowed++;
	            }
	        }
	    } else {
	        
	        currentSpecialProperties = null;
	    }
	}
	
	public List getSpecialProperties () {
	    return currentSpecialProperties;
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
			errMsg = "Wrong company " + companyName;
			return false;
		}

		if (++operatingCompanyIndex >= operatingCompanyArray.length)
		{
			// OR done. Inform GameManager.
			Log.write("End of Operating Round " + getCompositeORNumber());
			operatingCompany = null;
			GameManager.getInstance().nextRound(this);
			return true;
		}

		operatingCompany = operatingCompanyArray[operatingCompanyIndex];
		step = steps[0];
		prepareStep (step);

		return true;
	}

	/**
	 */
	public boolean buyTrain(String companyName, TrainI train, int price)
	{

		return buyTrain(companyName, train, price, null);
	}

	public boolean buyTrain(String companyName, TrainI train, int price,
			TrainI exchangedTrain)
	{

		String errMsg = null;

		// Dummy loop to enable a quick jump out.
		while (true)
		{

			// Portfolio oldHolder = train.getHolder();
			// CashHolder oldOwner = oldHolder.getOwner();

			// Checks
			// Must be correct company.
			if (!companyName.equals(operatingCompany.getName()))
			{
				errMsg = "Wrong company " + companyName;
				break;
			}
			// Must be correct step
			if (step != STEP_BUY_TRAIN)
			{
				errMsg = "Wrong action, expected Train buying cost";
				break;
			}

			if (train == null)
			{
				errMsg = "No train specified";
				break;
			}
			// Assume for now that buying this train is allowed.
			// Actually we should check this here.

			// Zero price means face value.
			if (price == 0)
				price = train.getCost();

			// Amount must be non-negative
			if (price < 0)
			{
				errMsg = "Negative amount not allowed";
				break;
			}

			// Does the company have room for another train?
			int currentNumberOfTrains = operatingCompany.getPortfolio()
					.getTrains().length;
			int trainLimit = operatingCompany.getTrainLimit(PhaseManager.getInstance()
					.getCurrentPhaseIndex());
			if (currentNumberOfTrains >= trainLimit)
			{
				errMsg = "Would exceed train limit of " + trainLimit;
				break;
			}

			// Does the company have the money?
			if (price > operatingCompany.getCash())
			{
				errMsg = "Not enough money";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error(companyName + " cannot buy " + train.getName()
					+ "-train for " + Bank.format(price) + ": " + errMsg);
			return false;
		}

		Portfolio oldHolder = train.getHolder();
		CashHolder oldOwner = oldHolder.getOwner();

		if (exchangedTrain != null)
		{
			TrainI oldTrain = operatingCompany.getPortfolio()
					.getTrainOfType(exchangedTrain.getType());
			Bank.getPool().buyTrain(oldTrain, 0);
			Log.write(operatingCompany.getName() + " exchanges "
					+ exchangedTrain + "-train for a " + train.getName()
					+ "-train from " + oldHolder.getName() + " for "
					+ Bank.format(price));
		}
		else
		{
			Log.write(operatingCompany.getName() + " buys " + train.getName()
					+ "-train from " + oldHolder.getName() + " for "
					+ Bank.format(price));
		}

		operatingCompany.buyTrain(train, price);

		TrainManager.get().checkTrainAvailability(train, oldHolder);

		return true;
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
				errMsg = "Wrong company " + companyName;
				break;
			}

			// Does private exist?
			if ((privCo = Game.getCompanyManager()
					.getPrivateCompany(privateName)) == null)
			{
				errMsg = "Private " + privateName + " does not exist";
				break;
			}
			// Is private still open?
			if (privCo.isClosed())
			{
				errMsg = "Private " + privateName + " is already closed";
				break;
			}
			// Is private owned by a player?
			owner = privCo.getPortfolio().getOwner();
			if (!(owner instanceof Player))
			{
				errMsg = "Private " + privateName + " is not owned by a player";
				break;
			}
			player = (Player) owner;
			basePrice = privCo.getBasePrice();

			// Price must be in the allowed range
			if (price < basePrice
					* operatingCompany.getLowerPrivatePriceFactor())
			{
				errMsg = "Price is less than lower limit of "
						+ (int) (basePrice * operatingCompany.getLowerPrivatePriceFactor());
				break;
			}
			if (price > basePrice
					* operatingCompany.getUpperPrivatePriceFactor())
			{
				errMsg = "Price is more than upper limit of "
						+ (int) (basePrice * operatingCompany.getUpperPrivatePriceFactor());
				break;
			}
			// Does the company have the money?
			if (price > operatingCompany.getCash())
			{
				errMsg = "Not enough money";
				break;
			}
			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot buy private " + privateName + " from "
					+ (owner == null ? "?" : owner.getName()) + " for " + price
					+ ": " + errMsg);
			return false;
		}

		operatingCompany.getPortfolio().buyPrivate(privCo,
				player.getPortfolio(),
				price);

		return true;

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
				errMsg = "Private " + privateName + " does not exist";
				break;
			}
			// Is private still open?
			if (privCo.isClosed())
			{
				errMsg = "Private " + privateName + " is already closed";
				break;
			}

			break;
		}
		if (errMsg != null)
		{
			Log.error("Cannot close private " + privateName + ": " + errMsg);
			return false;
		}

		privCo.setClosed();
		Log.write("Private " + privateName + " is closed");

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
		return step;
	}

	public int getOperatingCompanyIndex()
	{
		return operatingCompanyIndex;
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

	public String getHelp () {
	    StringBuffer b = new StringBuffer();
	    b.append("<big>Operating round: ").append(getCompositeORNumber()).append("</big><br>");
	    b.append("<br><b>").append(operatingCompany.getName())
	    	.append("</b> (president ").append(getCurrentPlayer().getName())
	    	.append(") has the turn.");
	    b.append("<br><br>Currently allowed actions:");
	    if (step == STEP_LAY_TRACK) {
	        b.append("<br> - Lay a tile");
	        b.append("<br> - Press 'Done' if you do not want to lay a tile");
	    } else if (step == STEP_LAY_TOKEN) {
	        b.append("<br> - Lay a base token or press Done");
	        b.append("<br> - Press 'Done' if you do not want to lay a base");
	    } else if (step == STEP_CALC_REVENUE) {
	        b.append ("<br> - Enter new revenue amount");
	        b.append("<br> - Press 'Done' if your revenue is zero");
	    } else if (step == STEP_PAYOUT) {
	        b.append ("<br> - Choose how the revenue will be paid out");
	    } else if (step == STEP_BUY_TRAIN) {
	        b.append ("<br> - Buy one or more trains");
	        b.append("<br> - Press 'Done' to finish your turn");
	    }
	    /* TODO: The below if needs be refined. */
	    if (GameManager.getInstance().getPhase() > 1 && step != STEP_PAYOUT) {
	        b.append("<br> - Buy one or more Privates");
	    }
	    
	    if (step == STEP_LAY_TRACK) {
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

}
