/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/OperatingRound.java,v 1.19 2007/10/27 15:26:34 evos Exp $ */
package rails.game;


import java.util.*;

import rails.game.action.*;
import rails.game.move.CashMove;
import rails.game.move.MoveSet;
import rails.game.special.*;
import rails.game.state.IntegerState;
import rails.util.LocalText;

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
    protected IntegerState stepObject;
	protected boolean actionPossible = true;
	protected String actionNotPossibleMessage = "";

	protected TreeMap<Integer, PublicCompanyI> operatingCompanies;
	protected PublicCompanyI[] operatingCompanyArray;
	protected IntegerState operatingCompanyIndexObject;
    protected int operatingCompanyIndex;
	protected PublicCompanyI operatingCompany;

    // Non-persistent lists (are recreated after each user action)
	protected List<SpecialPropertyI> currentSpecialProperties = null;
	protected List<LayTile> currentSpecialTileLays 
		= new ArrayList<LayTile>();
	protected List<LayTile> currentNormalTileLays 
		= new ArrayList<LayTile>();
	protected Map<String, Integer> tileLaysPerColour 
		= new HashMap<String, Integer>();
	//protected Map<MapHex, SpecialPropertyI> specialPropertyPerHex 
	//	= new HashMap<MapHex, SpecialPropertyI>();
	protected List<LayBaseToken> currentNormalTokenLays 
		= new ArrayList<LayBaseToken>();
	protected List<LayBaseToken> currentSpecialTokenLays 
		= new ArrayList<LayBaseToken>();
    /** A List per player with owned companies that have excess trains */
    protected Map<Player, List<PublicCompanyI>> excessTrainCompanies = null;

	protected PhaseI currentPhase;
	protected String thisOrNumber;
    
    protected PossibleAction selectedAction = null;
	
	protected BuyTrain savedAction = null;
	protected int cashToBeRaisedByPresident = 0;

	protected int splitRule = SPLIT_NOT_ALLOWED; // To be made configurable

	/* Permanent memory */
	static protected List<Player> players;
	static protected int numberOfPlayers = 0;
	static protected PublicCompanyI[] companies;
	static protected int numberOfCompanies = 0;
	static protected int relativeORNumber = 0;
	static protected int cumulativeORNumber = 0;

	/* Constants */
	public static final int SPLIT_NOT_ALLOWED = 0;
	public static final int SPLIT_ROUND_UP = 1; // More money to the shareholders
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
    // side steps
    public static final int STEP_DISCARD_TRAINS = -2;
    
	
	/**
	 * The constructor.
	 * @param operate If false, only the privates pay out.
	 * This applies if the Start Packet has not yet been sold completely.
	 */
	public OperatingRound(boolean operate)
	{
		relativeORNumber++;
		cumulativeORNumber++;
		thisOrNumber = getCompositeORNumber();
        
        if (operatingCompanyIndexObject == null) {
            operatingCompanyIndexObject = new IntegerState ("OperatingCompanyIndex", 0);
        }

		ReportBuffer.add(LocalText.getText("START_OR", getCompositeORNumber()));


		if (players == null)
		{
			players = Game.getPlayerManager().getPlayers();
			numberOfPlayers = players.size();
		}
		if (companies == null)
		{
			companies = (PublicCompanyI[]) Game.getCompanyManager()
					.getAllPublicCompanies()
					.toArray(new PublicCompanyI[0]);
		}
		
		for (PrivateCompanyI priv : Game.getCompanyManager().getAllPrivateCompanies()) 
		{
			if (!priv.isClosed())
				priv.payOut();
		}
		
		if (operate) {
		
			// Determine operating sequence for this OR.
			// Shortcut: order considered fixed at the OR start. This is not always
			// true.
			operatingCompanies = new TreeMap<Integer, PublicCompanyI>();
			StockSpaceI space;
			int key;
			int minorNo = 0;
			for (PublicCompanyI company : companies)
			{
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
	
			numberOfCompanies = operatingCompanyArray.length;
	
			if (operatingCompanyArray.length > 0)
			{
                GameManager.getInstance().setRound(this);
                operatingCompanyIndex = operatingCompanyIndexObject.intValue();
				operatingCompany = operatingCompanyArray[operatingCompanyIndex];
	
				setStep (STEP_INITIAL);
			}
			
			return;
		}
		
		// No operating companies yet: close the round.
		String text = LocalText.getText("ShortORExecuted");
		ReportBuffer.add (text);
		DisplayBuffer.add (text);
		GameManager.getInstance().nextRound(this);
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

    public boolean process (PossibleAction action) {
        
        boolean result = false;
        
        /*--- Common OR checks ---*/
        /* Check operating company */
        if (action instanceof PossibleORAction
                && !(action instanceof DiscardTrain)) {
            PublicCompanyI company = ((PossibleORAction)action).getCompany(); 
            if (company != operatingCompany)
            {
                DisplayBuffer.add (LocalText.getText("WrongCompany", new String[] {
                        company.getName(),
                        operatingCompany.getName()
                        }));
                return false;
            }
        }

        selectedAction = action;
        
        if (selectedAction instanceof LayTile) {
        	
        	result = layTile ((LayTile) selectedAction);

        } else if (selectedAction instanceof LayBaseToken) {
                
                result = layBaseToken ((LayBaseToken) selectedAction);
           
        } else if (selectedAction instanceof SetDividend) {
            
            result = setRevenueAndDividend ((SetDividend) selectedAction);
            
        } else if (selectedAction instanceof BuyTrain) {
        	
        	result = buyTrain ((BuyTrain) selectedAction);
            
        } else if (selectedAction instanceof DiscardTrain) {
            
            result = discardTrain ((DiscardTrain) selectedAction);
            
        } else if (selectedAction instanceof BuyPrivate) {
            
            result = buyPrivate ((BuyPrivate) selectedAction);
            
        } else if (selectedAction instanceof NullAction) {
            
            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
            case NullAction.PASS:
            case NullAction.DONE:
                result = done ();
                break;
            case NullAction.SKIP:
            	skip();
            	result = true;
            	break;
            }
        
        } else {
        
            DisplayBuffer.add (LocalText.getText("UnexpectedAction",
                    selectedAction.toString()));
            return false;
        }
        
        return result;
    }
    
	public boolean layTile (LayTile action)
	{

		String errMsg = null;
		int cost = 0;
		SpecialTileLay stl = null;
		boolean extra = false;
		
		PublicCompanyI company = action.getCompany();
		String companyName = company.getName();
		TileI tile = action.getLaidTile();
		MapHex hex = action.getChosenHex();
		int orientation = action.getOrientation();

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
			if (action != null) {
			    List tiles = action.getTiles();
			    if (tiles != null && !tiles.isEmpty() && !tiles.contains(tile)) {
			        errMsg = LocalText.getText("TileMayNotBeLaidInHex", new String[] {
			                tile.getName(),
			                hex.getName()
			        });
			        break;
			    }
			    stl = action.getSpecialProperty();
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
			DisplayBuffer.add(LocalText.getText("CannotLayTileOn", new String[] {
			        companyName,
			        tile.getName(),
			        hex.getName(),
			        Bank.format(cost),
			        errMsg
			}));
			return false;
		}

		/* End of validation, start of execution */
	    MoveSet.start(true);
	    
		if (tile != null)
		{
			hex.upgrade(tile, orientation);

			if (cost > 0) //Bank.transferCash((CashHolder) operatingCompany, null, cost);
			    new CashMove ((CashHolder) operatingCompany, null, cost);
			operatingCompany.layTile(hex, tile, orientation, cost);
			
			if (cost > 0) {
			    ReportBuffer.add(LocalText.getText("LaysTileAt", new String[] {
			            companyName,
			            tile.getName(),
			            hex.getName()
			    }));
			} else {
			    ReportBuffer.add(LocalText.getText("LaysTileAtFor", new String[] {
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
				currentSpecialTileLays.remove(action);
				log.debug ("This was a special tile lay, "+
				        (extra?"":" not")+" extra");
				
			}
			if (!extra)
			{
			    log.debug ("This was a normal tile lay");
				registerNormalTileLay (tile);
			}

			setSpecialTileLays();
			log.debug ("There are now "+currentSpecialTileLays.size()+" special tile lay objects");
		}

		if (tile == null 
		        || currentNormalTileLays.isEmpty() && currentSpecialTileLays.isEmpty())
		{
			nextStep();
		} else {
			//setPossibleActions("layTile");
		}
		
		return true;
	}
	
	protected boolean validateNormalTileLay (TileI tile) {
	    return checkNormalTileLay (tile, false);
	}
	
	protected void registerNormalTileLay (TileI tile) {
	    checkNormalTileLay (tile, true);
	}
	
	protected boolean checkNormalTileLay (TileI tile, boolean update) {
	    
	    //if (currentNormalTileLays.isEmpty()) return false;
        if (tileLaysPerColour.isEmpty()) return false;
	    String colour = tile.getColour();

	    Integer oldAllowedNumberObject = tileLaysPerColour.get(colour);
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
	        log.debug ("No more normal tile lays allowed");
	        currentNormalTileLays.clear();
	    } else {
	        tileLaysPerColour.clear(); // Remove all other colours
	        tileLaysPerColour.put(colour, new Integer(oldAllowedNumber-1));
	        log.debug ((oldAllowedNumber-1)+" more "+colour+" tile lays allowed");
	    }
	    
	    return true;
	}

    public boolean layBaseToken(LayBaseToken action)
    {

        String errMsg = null;
        int cost = 0;
        SpecialTokenLay stl = null;
        boolean extra = false;
        
        MapHex hex = action.getChosenHex();
        int station = action.getChosenStation();
        String companyName = operatingCompany.getName();

        // Dummy loop to enable a quick jump out.
        while (true)
        {

            // Checks
            // Must be correct step
            if (getStep() != STEP_LAY_TOKEN)
            {
                errMsg = LocalText.getText("WrongActionNoTokenLay");
                break;
            }

            if (operatingCompany.getNumberOfFreeBaseTokens() == 0)
            {
                errMsg = LocalText.getText("HasNoTokensLeft", 
                        companyName);
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
            
            if (action != null) {
                MapHex location = action.getLocation();
                if (location != null && location != hex) {
                    errMsg = LocalText.getText("TokenLayingHexMismatch", new String[] {
                            hex.getName(),
                            location.getName()});
                    break;
                }
                stl = action.getSpecialProperty();
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
            DisplayBuffer.add(LocalText.getText("CannotLayBaseTokenOn", new String[] {
                    companyName,
                    hex.getName(),
                    Bank.format(cost),
                    errMsg}));
            return false;
        }

        /* End of validation, start of execution */
        MoveSet.start(true);
        
        if (hex.layBaseToken(operatingCompany, station)) {
            /* TODO: the false return value must be impossible. */

            operatingCompany.layBaseToken (hex, cost);
    
            if (cost > 0)
            {
                new CashMove (operatingCompany, null, cost);
                ReportBuffer.add(LocalText.getText("LAYS_TOKEN_ON", new String[] {
                        companyName,
                        hex.getName(),
                        Bank.format(cost)}));
            }
            else
            {
                ReportBuffer.add(LocalText.getText("LAYS_FREE_TOKEN_ON", new String[] {
                        companyName,
                        hex.getName()}));
            }
    
            // Was a special property used?
            if (stl != null)
            {
                stl.setExercised();
                currentSpecialTokenLays.remove(action);
                log.debug ("This was a special token lay, "+
                        (extra?"":" not")+" extra");
                
            }
            if (!extra)
            {
                currentNormalTokenLays.clear();
                log.debug ("This was a normal token lay");
            }
            if (currentNormalTokenLays.isEmpty()) {
                log.debug ("No more normal token lays are allowed");
            } else {
                log.debug ("A normal token lay is still allowed");
            }
            setSpecialTokenLays();
            log.debug ("There are now "+currentSpecialTokenLays.size()+" special token lay objects");
            if (currentNormalTokenLays.isEmpty() && currentSpecialTokenLays.isEmpty())
            {
                nextStep();
            } else {
                //setPossibleActions("layBaseToken");
            }
    
        }

        return true;
    }

    public boolean setRevenueAndDividend (SetDividend action)
    {

        String errMsg = null;
        PublicCompanyI company;
        String companyName;
        int amount = 0;
        int revenueAllocation = -1;

        // Dummy loop to enable a quick jump out.
        while (true)
        {

            // Checks
            // Must be correct company.
            company = action.getCompany();
            companyName = company.getName();
            if (company != operatingCompany)
            {
                errMsg = LocalText.getText("WrongCompany", new String[] {
                        companyName,
                        operatingCompany.getName()});
                break;
            }
            // Must be correct step
            if (getStep() != STEP_CALC_REVENUE)
            {
                errMsg = LocalText.getText("WrongActionNoRevenue");
                break;
            }

            // Amount must be non-negative multiple of 10
            amount = action.getActualRevenue();
            if (amount < 0)
            {
                errMsg = LocalText.getText("NegativeAmountNotAllowed",
                        String.valueOf(amount));
                break;
            }
            if (amount % 10 != 0)
            {
                errMsg = LocalText.getText("AmountMustBeMultipleOf10",
                        String.valueOf(amount));
                break;
            }
            
            // Check chosen revenue distribution
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                revenueAllocation = action.getRevenueAllocation();
                if (revenueAllocation < 0 || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    errMsg = LocalText.getText("InvalidAllocationTypeIndex",
                            String.valueOf(revenueAllocation));
                    break;
                }
                
                // Validate the chosen allocation type
                int[] allowedAllocations = ((SetDividend)selectedAction).getAllowedAllocations();
                boolean valid = false;
                for (int aa : allowedAllocations) {
                    if (revenueAllocation == aa) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    errMsg = LocalText.getText(SetDividend.getAllocationNameKey(revenueAllocation));
                    break;
                }
            } else {
                // If there is no revenue, use withhold.
                revenueAllocation = SetDividend.WITHHOLD;
            }
            
            break;
        }
        if (errMsg != null)
        {
            DisplayBuffer.add(LocalText.getText("CannotProcessRevenue", new String[] {
                    String.valueOf(amount),
                    companyName,
                    errMsg
            }));
            return false;
        }
        
        MoveSet.start(true);
        
        operatingCompany.setLastRevenue (amount);
        operatingCompany.setLastRevenueAllocation(revenueAllocation);
        ReportBuffer.add(LocalText.getText("CompanyRevenue", new String[] {
                companyName,
                Bank.format(amount)
        }));

        if (revenueAllocation == SetDividend.PAYOUT) {
            
            ReportBuffer.add(
                    LocalText.getText("CompanyPaysOutFull", new String[] {
                            companyName,
                            Bank.format(amount)
            }));

            operatingCompany.payOut(amount);

        } else if (revenueAllocation == SetDividend.SPLIT) {
            
            ReportBuffer.add(
                    LocalText.getText("CompanySplits", new String[] {
                            companyName,
                            Bank.format(amount)
                    }));
            
            operatingCompany.splitRevenue(amount);

        } else if (revenueAllocation == SetDividend.WITHHOLD) {
            
            ReportBuffer.add(
                    LocalText.getText("CompanyWithholds", new String[] {
                            companyName,
                            Bank.format(amount)
                    }));

            operatingCompany.withhold(amount);

        }
        
        // We have done the payout step, so continue from there
        nextStep(STEP_PAYOUT);

        return true;
    }
    
	/**
	 * Internal method: change the OR state to the next step. If the currently
	 * Operating Company is done, notify this.
	 * 
	 * @param company
	 *            The current company.
	 */
    protected void nextStep () {
        nextStep (getStep());
    }
    
    /** Take the next step after a given one (see nextStep()) */
	protected void nextStep(int step)
	{
		// Cycle through the steps until we reach one where a user action is expected.
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
			    
			    if (operatingCompany.getPortfolio().getNumberOfTrains() == 0)
				{
					// No trains, then the revenue is zero.
					operatingCompany.setLastRevenue(0);
                    operatingCompany.setLastRevenueAllocation(SetDividend.UNKNOWN);
					ReportBuffer.add(LocalText.getText("CompanyRevenue", new String[] {
							operatingCompany.getName(),
							Bank.format(0)
					}));
					continue;
				}
			}
			
			if (step == STEP_PAYOUT) {
			    
				// If we already know what to do: do it.
			    int amount = operatingCompany.getLastRevenue();
				if (amount == 0)
				{
				    /* Zero dividend: process it and go to the next step */
					operatingCompany.withhold(0);
					DisplayBuffer.add (
							LocalText.getText("RevenueWithNoTrains", new String[] {
									operatingCompany.getName(),
									Bank.format(0)
							}));
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
			done();
		} else {
		    setStep(step);
		}
		
	}
    
    protected void initTurn() {
        GameManager.setCurrentPlayer(operatingCompany.getPresident());
        operatingCompany.initTurn();
    }

	/**
	 * This method is only called at the start of each step
	 * (unlike updateStatus(), which is called after each user action)
	 */
	protected void prepareStep()
	{
	    int step = stepObject.intValue();

		currentPhase = PhaseManager.getInstance().getCurrentPhase();
		
		if (step == STEP_LAY_TRACK)
		{
			getNormalTileLays();
		}
		else if (step == STEP_LAY_TOKEN)
		{
		}
		else
		{
			currentSpecialProperties = null;
		}
		
	}
	
    protected <T extends SpecialPropertyI> List<T> getSpecialProperties (Class<T> clazz) {
        List<T> specialProperties = new ArrayList<T>();
        specialProperties.addAll(operatingCompany.getPortfolio()
            .getSpecialProperties(clazz, false));
        specialProperties.addAll (operatingCompany.getPresident().getPortfolio()
            .getSpecialProperties(clazz, false));
        return specialProperties;
    }
    
	
	/**
	 * Create a List of allowed normal tile lays (see LayTile class).
	 * This method should be called only once per company turn in an OR:
	 * at the start of the tile laying step.
	 */
	protected void getNormalTileLays() {
	    
	    tileLaysPerColour = new HashMap<String, Integer> (currentPhase.getTileColours()); //Clone it.
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
		//specialPropertyPerHex.clear();
		/* In 1835, this only applies to major companies.
		 * TODO: For now, hardcode this, but it must become configurable later.
		 */
		if (operatingCompany.getType().getName().equals("Minor")) return;
		
        for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class))
		{
			if (stl.isExtra() || !currentNormalTileLays.isEmpty()) {
			    /* If the special tile lay is not extra, it is only 
			     * allowed if normal tile lays are also (still) allowed */
                //for (MapHex hex : stl.getLocations()) {
                    //specialPropertyPerHex.put(hex, stl);
                //}
				currentSpecialTileLays.add (new LayTile (stl));
			}
		}
	}
	
	protected void setNormalTokenLays () {
	    
	    /* Normal token lays */
	    currentNormalTokenLays.clear();
	    
	    /* For now, we allow one token of the currently operating company */
	    if (operatingCompany.getNumberOfFreeBaseTokens() > 0) {
	        currentNormalTokenLays.add (new LayBaseToken ((List<MapHex>)null));
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
		//specialPropertyPerHex.clear();

		/* In 1835, this only applies to major companies.
		 * TODO: For now, hardcode this, but it must become configurable later.
		 */
		if (operatingCompany.getType().getName().equals("Minor")) return;

        for (SpecialTokenLay stl : getSpecialProperties (SpecialTokenLay.class))
		{
			log.debug ("Spec.prop:"+stl);
			if (stl.getTokenClass().equals(BaseToken.class)
                    && (stl.isExtra() || !currentNormalTokenLays.isEmpty())) {
			    /* If the special tile lay is not extra, it is only 
			     * allowed if normal tile lays are also (still) allowed */
				//specialPropertyPerHex.put(stl.getLocation(), stl);
				currentSpecialTokenLays.add (new LayBaseToken (stl));
			}
		}
	}
    
    /** TODO Should be merged with setSpecialTokenLays() in the future.
     * Assumptions: 
     * 1. Bonus tokens can  be laid anytime during the OR.
     * 2. Bonus token laying is always extra.
     * TODO This assumptions will be made configurable conditions. 
     * */ 
    protected void setBonusTokenLays () {
        
        for (SpecialTokenLay stl : getSpecialProperties (SpecialTokenLay.class))
        {
            if (stl.getTokenClass().equals(BonusToken.class)) {
                possibleActions.add(new LayBonusToken (stl));
            }
        }
    }

	public List<SpecialPropertyI> getSpecialProperties()
	{
		return currentSpecialProperties;
	}

	public void skip()
	{
	    log.debug ("Skip step "+stepObject.intValue());
	    MoveSet.start(true);
		nextStep();
	}

	/**
	 * The current Company is done operating.
	 * 
	 * @param company
	 *            Name of the company that finished operating.
	 * @return False if an error is found.
	 */
	public boolean done()
	{
		String errMsg = null;

		if(operatingCompany.getPortfolio().getNumberOfTrains() == 0
		        && operatingCompany.mustOwnATrain())
		{
			//FIXME: Need to check for valid route before throwing an error.
			errMsg = LocalText.getText("CompanyMustOwnATrain", 
			    operatingCompany.getName());
			setStep(STEP_BUY_TRAIN);
			DisplayBuffer.add(errMsg);
			return false;
		}
		
		MoveSet.start (false);

		if (operatingCompanyIndex >= operatingCompanyArray.length - 1)
		{
			// OR done. Inform GameManager.
			ReportBuffer.add(
					LocalText.getText("EndOfOperatingRound", getCompositeORNumber()));
			GameManager.getInstance().nextRound(this);
			return true;
		}

        operatingCompanyIndexObject.add(1);
        operatingCompanyIndex = operatingCompanyIndexObject.intValue();
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];
        
		setStep (STEP_INITIAL);

		return true;
	}

	public boolean buyTrain (BuyTrain action)
	{

	    TrainI train = action.getTrain();
		PublicCompanyI company = action.getCompany();
		String companyName = company.getName();
		TrainI exchangedTrain = action.getExchangedTrain();
	    
		String errMsg = null;
		int presidentCash = action.getPresidentCashToAdd();
		boolean presidentMustSellShares = false;
		int price = action.getPricePaid();

		// Dummy loop to enable a quick jump out.
		while (true)
		{
			// Checks
			// Must be correct step
			if (getStep() != STEP_BUY_TRAIN)
			{
				errMsg = LocalText.getText("WrongActionNoTrainBuyingCost");
				break;
			}

			if (train == null)
			{
				errMsg = LocalText.getText("NoTrainSpecified");
				break;
			}

			// Amount must be non-negative
			if (price < 0)
			{
				errMsg = LocalText.getText("NegativeAmountNotAllowed",
				        Bank.format(price));
				break;
			}

			// Does the company have room for another train?
			int currentNumberOfTrains = operatingCompany.getPortfolio()
					.getNumberOfTrains();
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
			if (action.mustPresidentAddCash()) {
			    // From the Bank
		        presidentCash = action.getPresidentCashToAdd();
			    if (currentPlayer.getCash() >= presidentCash) {
			        new CashMove(currentPlayer, operatingCompany, presidentCash);
			    } else {
			        presidentMustSellShares = true;
			        cashToBeRaisedByPresident = presidentCash - currentPlayer.getCash();
                }
			} else if (action.mayPresidentAddCash()) {
			    // From another company
			    presidentCash = price - operatingCompany.getCash();
			    if (presidentCash > action.getPresidentCashToAdd()) {
			        errMsg = LocalText.getText("PresidentMayNotAddMoreThan", 
			            Bank.format (action.getPresidentCashToAdd()));
			        break;
			    } else if (currentPlayer.getCash() >= presidentCash) {
			        new CashMove(currentPlayer, operatingCompany, presidentCash);
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
			DisplayBuffer.add(LocalText.getText("CannotBuyTrainFor", new String[] {
			        companyName,
			        train.getName(),
			        Bank.format(price),
			        errMsg}));
			return false;
		}
		
        /* End of validation, start of execution */
        MoveSet.start(true);
        PhaseI previousPhase = GameManager.getCurrentPhase();
        
		if (presidentMustSellShares) {
		    savedAction = action;

			GameManager.getInstance().startShareSellingRound (this, operatingCompany, 
			        cashToBeRaisedByPresident);

		    return true;
		}

		Portfolio oldHolder = train.getHolder();

		if (exchangedTrain != null)
		{
			TrainI oldTrain = operatingCompany.getPortfolio()
					.getTrainOfType(exchangedTrain.getType());
			Bank.getPool().buyTrain(oldTrain, 0);
			ReportBuffer.add(LocalText.getText("ExchangesTrain", new String[] {
			        companyName,
			        exchangedTrain.getName(),
			        train.getName(),
			        oldHolder.getName(),
			        Bank.format(price)}));
		}
		else
		{
			ReportBuffer.add(LocalText.getText("BuysTrain", new String[] {
			        companyName,
			        train.getName(),
			        oldHolder.getName(),
					Bank.format(price)}));
		}

		operatingCompany.buyTrain(train, price);
		if (oldHolder == Bank.getIpo()) train.getType().addToBoughtFromIPO();

        // Check if the phase has changed.
		TrainManager.get().checkTrainAvailability(train, oldHolder);
		currentPhase = GameManager.getCurrentPhase();
        
        // Check if any companies must discard trains
        if (currentPhase != previousPhase
                && checkForExcessTrains()) {
            stepObject.set(STEP_DISCARD_TRAINS);
        }
		
		//setPossibleActions("buyTrain");
		return true;
	}
    
    public boolean checkForExcessTrains() {
        
        excessTrainCompanies = new HashMap<Player, List<PublicCompanyI>>();
        Player player;
        for (PublicCompanyI comp : operatingCompanyArray) {
            if (comp.getPortfolio().getNumberOfTrains() > comp.getTrainLimit(currentPhase.getIndex()))
            {
                player = comp.getPresident();
                if (!excessTrainCompanies.containsKey(player)) {
                    excessTrainCompanies.put(player, new ArrayList<PublicCompanyI>(2));
                }
                excessTrainCompanies.get(player).add(comp);
            }

        }
        return !excessTrainCompanies.isEmpty();
   }
	
	public void resumeTrainBuying () {
	    
       buyTrain (savedAction);
	}
    
    public boolean discardTrain (DiscardTrain action) {
        
        TrainI train = action.getDiscardedTrain();
        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();
        
        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true)
        {
            // Checks
            // Must be correct step
            if (getStep() != STEP_DISCARD_TRAINS)
            {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null)
            {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?
            
            if (!company.getPortfolio().getTrainList().contains(train))
            {
                errMsg = LocalText.getText("CompanyDoesNotOwnTrain", new String[] {
                        company.getName(),
                        train.getName()
                });
                break;
            }
            
            break;
        }
        if (errMsg != null)
        {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain", new String[] {
                    companyName,
                    train.getName(),
                    errMsg}));
            return false;
        }
        
        /* End of validation, start of execution */
        MoveSet.start(true);
        // 
        MoveSet.setLinkedToPrevious();
        
        Bank.getPool().buyTrain(train, 0);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain", new String[] {
                companyName,
                train.getName()
        }));

        // Check if any more companies must discard trains,
        // otherwise continue train buying
        if (!checkForExcessTrains()) {
            stepObject.set(STEP_BUY_TRAIN);
        }
        
        setPossibleActions();

        return true;
    }

    public boolean buyPrivate (BuyPrivate action)
    {

        String errMsg = null;
        PublicCompanyI publicCompany = action.getCompany();
        String publicCompanyName = publicCompany.getName();
        PrivateCompanyI privateCompany = action.getPrivateCompany();
        String privateCompanyName = privateCompany.getName();
        int price = action.getPrice();
        CashHolder owner = null;
        Player player = null;
        int basePrice;

        // Dummy loop to enable a quick jump out.
        while (true)
        {

            // Checks
            // Does private exist?
            if ((privateCompany = Game.getCompanyManager()
                    .getPrivateCompany(privateCompanyName)) == null)
            {
                errMsg = LocalText.getText("PrivateDoesNotExist", 
                        privateCompanyName);
                break;
            }
            // Is private still open?
            if (privateCompany.isClosed())
            {
                errMsg = LocalText.getText("PrivateIsAlreadyClosed", 
                        privateCompanyName);
                break;
            }
            // Is private owned by a player?
            owner = privateCompany.getPortfolio().getOwner();
            if (!(owner instanceof Player))
            {
                errMsg = LocalText.getText("PrivateIsNotOwnedByAPlayer", 
                        privateCompanyName);
                break;
            }
            player = (Player) owner;
            basePrice = privateCompany.getBasePrice();

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
                        privateCompanyName
                });
                break;
            }
            if (price > basePrice
                    * operatingCompany.getUpperPrivatePriceFactor())
            {
                errMsg = LocalText.getText("PriceAboveUpperLimit", new String[] {
                        Bank.format(price),
                        Bank.format((int)(basePrice * operatingCompany.getUpperPrivatePriceFactor())),
                        privateCompanyName
                });
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.getCash())
            {
                errMsg = LocalText.getText("NotEnoughMoney", new String[] {
                        publicCompanyName,
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
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFromFor", new String[] {
                        publicCompanyName,
                        privateCompanyName,
                        owner.getName(),
                        Bank.format(price),
                        errMsg
                }));
            } else {
                DisplayBuffer.add(LocalText.getText("CannotBuyPrivateFor", new String[] {
                        publicCompanyName,
                        privateCompanyName,
                        Bank.format(price),
                        errMsg
                }));
            }
            return false;
        }

        MoveSet.start(true);
        
        operatingCompany.buyPrivate(privateCompany,
                player.getPortfolio(),
                price);
        
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
		return stepObject.intValue();
	}
	
	/**
	 * Bypass normal order of operations and explicitly set round step.
	 * This should only be done for specific rails.game exceptions, such as forced train purchases.
	 * 
	 * @param step
	 */
	protected void setStep(int step)
	{
        //log.debug("+++Setting step to "+step);
        if (step == STEP_INITIAL) initTurn();
        
	    if (stepObject == null) {
	        stepObject = new IntegerState("ORStep", -1);
	        stepObject.addObserver(this);
	    }
	    stepObject.set(step);
		
	}

	public int getOperatingCompanyIndex()
	{
		return operatingCompanyIndexObject.intValue();
	}
	
	/**
	 * To be called after each change, to re-establish the currently allowed actions.
	 * (new method, intended to absorb code from several other methods). 
	 *
	 */
	public boolean setPossibleActions () {
        
        //log.debug("Called from ", new Exception("HERE"));

        operatingCompanyIndex = operatingCompanyIndexObject.intValue();
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];
	    
		/* Create a new list of possible actions for the UI */
		possibleActions.clear();
        selectedAction = null;
		
		int step = getStep();

		if (step == STEP_LAY_TRACK)
		{
		    setNormalTileLays();
			setSpecialTileLays();
			log.debug ("Normal tile lays: "+currentNormalTileLays.size());
			log.debug ("Special tile lays: "+currentSpecialTileLays.size());

			possibleActions.addAll (currentNormalTileLays);
			possibleActions.addAll (currentSpecialTileLays);
			possibleActions.add (new NullAction (NullAction.SKIP));
		}
		else if (step == STEP_LAY_TOKEN) 
		{
		    setNormalTokenLays();
		    setSpecialTokenLays();
		    log.debug ("Normal token lays: "+currentNormalTokenLays.size());
		    log.debug ("Special token lays: "+currentSpecialTokenLays.size());

			possibleActions.addAll (currentNormalTokenLays);
			possibleActions.addAll (currentSpecialTokenLays);
			possibleActions.add (new NullAction (NullAction.SKIP));
		}
		else if (step == STEP_CALC_REVENUE)
		{
			if (operatingCompany.getPortfolio().getNumberOfTrains() == 0) {
				// No trains: the revenue is fixed at 0
			} else {
				possibleActions.add (new SetDividend (
						operatingCompany.getLastRevenue(),
						true,
						new int[]{SetDividend.PAYOUT, SetDividend.WITHHOLD}));
			}
		}
		else if (step == STEP_BUY_TRAIN)
		{
			setBuyableTrains();
		}
        else if (step == STEP_DISCARD_TRAINS)
        {
            setTrainsToDiscard();
        }
        
        setBonusTokenLays();
		
		// Can private companies be bought?
		if (GameManager.getCurrentPhase().isPrivateSellingAllowed()) {
			
			// Create a list of players with the current one in front
			int currentPlayerIndex = operatingCompany.getPresident().getIndex();
			Player player;
		    int minPrice, maxPrice;
			for (int i = currentPlayerIndex; 
					 i < currentPlayerIndex + numberOfPlayers;
					 i++) {
				player = players.get(i % numberOfPlayers);
			    for (PrivateCompanyI privComp : player.getPortfolio().getPrivateCompanies()) {
			    	
			        minPrice = (int) (privComp.getBasePrice() * operatingCompany
	                        .getLowerPrivatePriceFactor());
			        maxPrice = (int) (privComp.getBasePrice() * operatingCompany
	                        .getUpperPrivatePriceFactor());
			        possibleActions.add (new BuyPrivate (
                            privComp, minPrice, maxPrice));
			    }
			}
		}
        
        if (getStep() >= STEP_BUY_TRAIN 
                && (!operatingCompany.mustOwnATrain()
                        || operatingCompany.getPortfolio().getNumberOfTrains() > 0)) {
            possibleActions.add(new NullAction (NullAction.DONE));
        }
		
		for (PossibleAction pa : possibleActions.getList()) {
			try {
				log.debug(operatingCompany.getName()+ " may: "+pa.toString());
			} catch (Exception e) {
				log.error("Error in toString() of "+pa.getClass(), e);
			}
		}

        return true;
	}

	/** 
	 * Get a list of buyable trains for the currently operating company.
	 * Omit trains that the company has no money for. If there is no cash to
	 * buy any train from the Bank, prepare for emergency train buying.
	 * @return List of all trains that could potentially be bought.
	 */
	public void setBuyableTrains() {
	    
	    if (operatingCompany == null) return;
	    
	    int cash = operatingCompany.getCash();
	    int cost;
	    List trains;
	    TrainI train;
	    boolean hasTrains = operatingCompany.getPortfolio().getNumberOfTrains() > 0;
	    boolean presidentMayHelp = false;
	    TrainI cheapestTrain = null;
	    int costOfCheapestTrain = 0;
	    Portfolio ipo = Bank.getIpo();
	    Portfolio pool = Bank.getPool();
	    
	    /* New trains */
        trains =  TrainManager.get().getAvailableNewTrains();
        for (Iterator it = trains.iterator(); it.hasNext(); ) {
            train = (TrainI) it.next();
            cost = train.getCost();
            if (cost <= cash) {
            	possibleActions.add (new BuyTrain (train, ipo, cost));
            } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
                costOfCheapestTrain = cost;
            }
            if (train.canBeExchanged() && hasTrains) {
                cost = train.getType().getFirstExchangeCost();
                if (cost <= cash) {
                    List<TrainI> exchangeableTrains 
                        = operatingCompany.getPortfolio().getUniqueTrains();
                    possibleActions.add (new BuyTrain (train, ipo, cost)
                            .setTrainsForExchange(exchangeableTrains));
                }
            }
        }
        
        /* Used trains */
        trains = pool.getUniqueTrains();
		for (Iterator it = trains.iterator(); it.hasNext();) {
		    train = (TrainI) it.next();
		    cost = train.getCost();
		    if (cost <= cash) {
            	possibleActions.add (new BuyTrain (train, pool, cost));
		    } else if (costOfCheapestTrain == 0 || cost < costOfCheapestTrain) {
                cheapestTrain = train;
		        costOfCheapestTrain = cost;
		    }
		}
		if (!hasTrains && possibleActions.getType(BuyTrain.class).isEmpty() 
				&& cheapestTrain != null) {
			possibleActions.add (new BuyTrain (cheapestTrain, cheapestTrain.getHolder(), costOfCheapestTrain)
            		.setPresidentMustAddCash(costOfCheapestTrain - cash));
		    presidentMayHelp = true;
		}
		
		/* Other company trains, sorted by president (current player first) */
		PublicCompanyI c;
		BuyTrain bt;
		Player p;
		Portfolio pf;
		int index;
		// Set up a list per player of presided companies
		List<List<PublicCompanyI>> companiesPerPlayer 
				= new ArrayList<List<PublicCompanyI>>(numberOfPlayers);
		for (int i=0; i<numberOfPlayers; i++) companiesPerPlayer.add(new ArrayList<PublicCompanyI>(4));
		List<PublicCompanyI> companies;
		// Sort out which players preside over wich companies.
		for (int j = 0; j < operatingCompanyArray.length; j++) {
			c = operatingCompanyArray[j];
			if (c == operatingCompany) continue;
			p = c.getPresident();
			index = p.getIndex();
			companiesPerPlayer.get(index).add(c);
		}
		// Scan trains per company per player, operating company president first
		int currentPlayerIndex = operatingCompany.getPresident().getIndex();
		for (int i = currentPlayerIndex; 
				 i < currentPlayerIndex + numberOfPlayers; 
				 i++) {
			companies = companiesPerPlayer.get(i % numberOfPlayers);
			for (PublicCompanyI company : companies) {
				pf = company.getPortfolio();
				trains = pf.getUniqueTrains();
				for (Iterator it = trains.iterator(); it.hasNext();) {
				    train = (TrainI) it.next();
				    bt = new BuyTrain (train, pf, 0);
				    if (presidentMayHelp && cash < train.getCost()) {
				        bt.setPresidentMayAddCash(train.getCost() - cash);
				    }
				    possibleActions.add (bt);
				}
			}
		}
	}
    
    protected void setTrainsToDiscard() {
        
        // Scan the players in SR sequence, starting with the current player
        Player player;
        List<PublicCompanyI> list;
        int currentPlayerIndex = GameManager.getCurrentPlayerIndex();
        for (int i=currentPlayerIndex; i<currentPlayerIndex + numberOfPlayers; i++) {
            player = GameManager.getPlayer(i);
            if (excessTrainCompanies.containsKey(player)) {
                list = excessTrainCompanies.get(player);
                for (PublicCompanyI comp : list) {
                    possibleActions.add(new DiscardTrain(comp, 
                            comp.getPortfolio().getUniqueTrains()));
                    // We handle one company at at time.
                    // We come back here until all excess trains have been discarded.
                    GameManager.setCurrentPlayer(player);
                    return;
                }
            }
        }
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
	    }
	}
    public String toString() {
        return "OperatingRound " + getCumulativeORNumber();
    }
    
    /** @Overrides */
    public boolean equals (RoundI round) {
        return round instanceof OperatingRound
            && thisOrNumber.equals(((OperatingRound)round).thisOrNumber);
    }

}
