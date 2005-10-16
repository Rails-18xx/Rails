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

import java.util.*;

/**
 * Implements a basic Operating Round.
 * <p>A new instance must be created for each new Operating Round.
 * At the end of a round, the current instance should be discarded.
 * <p>Permanent memory is formed by static attributes. 
 * @author Erik Vos
 * TODO Configure split rule
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
    
    protected int splitRule = SPLIT_NOT_ALLOWED; // To be made configurable
    
    /* Permanent memory */
    static protected Player[] players;
    static protected PublicCompanyI[] companies;
    static protected int relativeORNumber = 0;
    static protected int cumulativeORNumber = 0;
    
    /* Constants */
    public static final int SPLIT_NOT_ALLOWED = 0; 
    public static final int SPLIT_ROUND_UP = 1; // More money to the shareholders
    public static final int SPLIT_ROUND_DOWN = 2; // More to the treasury
    
    public static final int STEP_LAY_TRACK = 0;
    public static final int STEP_LAY_TOKEN = 1;
    public static final int STEP_CALC_REVENUE = 2;
    public static final int STEP_PAYOUT = 3;
    public static final int STEP_BUY_TRAIN = 4;
    public static final int STEP_FINAL = 5;
    protected static int[] steps = new int[] {STEP_LAY_TRACK, STEP_LAY_TOKEN, 
            STEP_CALC_REVENUE, STEP_PAYOUT, STEP_BUY_TRAIN, STEP_FINAL};
     
    /**
     * The constructor.
     */
    public OperatingRound() {
        
        if (players == null) {
            players = Game.getPlayerManager().getPlayersArray();
        }
        if (companies == null) {
            companies = (PublicCompanyI[]) Game.getCompanyManager().getAllPublicCompanies()
            		.toArray(new PublicCompanyI[0]);
        }

        // Determine operating sequence for this OR.
        // Shortcut: order considered fixed at the OR start. This is not always true.
        operatingCompanies = new TreeMap();
        PublicCompanyI company;
        StockSpaceI space;
        int key, stackPos;
        int minorNo = 0;
        for (int i=0; i<companies.length; i++) {
            company = companies[i];
            if (!company.hasFloated()) continue;
            space = company.getCurrentPrice();
            // Key must put companies in reverse operating order, because sort is ascending.
            if (company.hasStockPrice()) {
	            key = 1000000 * (999-space.getPrice()) + 10000 * (99-space.getColumn())
	            		+ 100 * space.getRow() + space.getStackPosition(company);
            } else {
                key = ++minorNo;
            }
            //System.out.println("OR key of "+company.getName()+" is "+key);
            operatingCompanies.put(new Integer(key), company);
        }
        
        operatingCompanyArray = (PublicCompanyI[])operatingCompanies.values()
        		.toArray(new PublicCompanyI[0]);
        step = steps[0];
        
        relativeORNumber++;
        cumulativeORNumber++;
        
        Log.write("\nStart of Operating Round "+getCompositeORNumber());

        // Private companies pay out
		Iterator it = Game.getCompanyManager().getAllPrivateCompanies().iterator();
		PrivateCompanyI priv;
		while (it.hasNext()) {
			priv = (PrivateCompanyI) it.next();
			if (!priv.isClosed()) priv.payOut();
		}
		
        if (operatingCompanyArray.length > 0) {
            operatingCompany = operatingCompanyArray[operatingCompanyIndex];
            GameManager.getInstance().setRound(this);
        } else {
            // No operating companies yet: close the round.
            Log.write("End of Operating Round" + getCompositeORNumber());
            GameManager.getInstance().nextRound(this);
        }
     }

    /*----- General methods -----*/
    
    /**
     * Return the operating round (OR) number in the format sr.or, 
     * where sr is the last stock round number and or is the relative
     * OR number.  
     * @return Composite SR/OR number.
     */
    public String getCompositeORNumber() {
        return StockRound.getLastStockRoundNumber() + "." + relativeORNumber;
    }
    
    /**
     * Get the relative OR number. This number restarts at 1 
     * after each stock round.
     * @return Relative OR number 
     */
    public int getRelativeORNumber() {
        return relativeORNumber;
    }
    
    /**
     * Get the cumulative OR number. This number never restarts.
     * @return Cumulative OR number.
     */
    public int getCumulativeORNumber() {
        return cumulativeORNumber;
    }

    /**
     * @deprecated Currently needed, but will be removed in a later stage.
     */
    public static void resetRelativeORNumber () {
        relativeORNumber = 0;
    }
   
    /*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/
    
    /**
     * A (perhaps temporary) method via which the cost of track laying
     * can be accounted for.
     * @param companyName The name of the company that lays the track.
     * @param amountSpent The cost of laying the track, which is
     * subtracted from the company treasury.
     */
    public boolean layTrack (String companyName, int amountSpent) {
        
        String errMsg = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            // Must be correct step
            if (step != STEP_LAY_TRACK) {
                errMsg = "Wrong action, expected Track laying cost";
                break;
            }
            
            // Amount must be non-negative multiple of 10
            if (amountSpent < 0) {
                errMsg = "Negative amount not allowed";
                break;
            }
            if (amountSpent%10 != 0) {
                errMsg = "Amount must be a multiple of 10";
                break;
            }
            // Does the company have the money?
            if (amountSpent > operatingCompany.getCash()) {
                errMsg = "Not enough money";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot process track laying cost of "+amountSpent+": "+errMsg);
            return false;
        }
        
        Bank.transferCash ((CashHolder)operatingCompany, null, amountSpent);
		if (amountSpent > 0) Log.write (companyName+" spends " 
		        + Bank.format(amountSpent) + " while laying track");
    
        nextStep (operatingCompany);
        
        return true;
     }
    
    /**
     * A (perhaps temporary) method via which the cost of station token
     * laying can be accounted for.
     * @param companyName The name of the company that lays the token.
     * @param amountSpent The cost of laying the token, which is
     * subtracted from the company treasury.
     * @return
     */
    public boolean layToken (String companyName, int amountSpent) {
        
        String errMsg = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            // Must be correct step
            if (step != STEP_LAY_TOKEN) {
                errMsg = "Wrong action, expected Token laying cost";
                break;
            }
            
            // Amount must be non-negative multiple of 10
            if (amountSpent < 0) {
                errMsg = "Negative amount not allowed";
                break;
            }
            if (amountSpent%10 != 0) {
                errMsg = "Must be a multiple of 10";
                break;
            }
            // Does the company have the money?
            if (amountSpent > operatingCompany.getCash()) {
                errMsg = "Not enough money";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot process token laying cost of "+amountSpent+": "+errMsg);
            return false;
        }
        
        Bank.transferCash ((CashHolder)operatingCompany, null, amountSpent);
        if (amountSpent > 0) Log.write (companyName+" spends " 
                + Bank.format(amountSpent) + " while laying token");
       
        nextStep (operatingCompany);
        
        return true;
     }
    
    /**
     * Set a given revenue.
     * This may be a temporary method. We will have to enter
     * revenues manually as long as the program 
     * cannot yet do the calculations.
     * @param amount The revenue.
     * @return False if an error is found.
     */
    public boolean setRevenue (String companyName, int amount) {
        
        String errMsg = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            // Must be correct step
            if (step != STEP_CALC_REVENUE) {
                errMsg = "Wrong action, expected Revenue calculation";
                break;
            }
            
            // Amount must be non-negative multiple of 10
            if (amount < 0) {
                errMsg = "Negative amount not allowed";
                break;
            }
            if (amount%10 != 0) {
                errMsg = "Must be a multiple of 10";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot process revenue of "+amount+": "+errMsg);
            return false;
        }
        
        currentRevenue = amount;
		Log.write (companyName+" earns " + Bank.format(amount));
       
        nextStep(operatingCompany);
        
        // If we already know what to do: do it.
        if (amount == 0) {
            operatingCompany.withhold(0);
            nextStep (operatingCompany);
        } else if (operatingCompany.isSplitAlways()) {
            operatingCompany.splitRevenue (amount);
            nextStep (operatingCompany);
       }
        
        return true;
    }
    
    /**
     * A previously entered revenue is fully paid out as dividend.
     * <p>Note: <b>setRevenue()</b> must have been called before this method.
     * @param companyName Name of the company paying dividend.
     * @return False if an error is found.
     */
    public boolean fullPayout (String companyName) {
 
        String errMsg = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            // Must be correct step
            if (step != STEP_PAYOUT) {
                errMsg = "Wrong action, expected Revenue Assignment";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot payout revenue of "+currentRevenue+": "+errMsg);
            return false;
        }

        Log.write(companyName + " pays out full dividend of " + Bank.format(currentRevenue));
        operatingCompany.payOut(currentRevenue);

        nextStep(operatingCompany);

        return true;
    }
    
    /**
     * A previously entered revenue is split, i.e. half of it
     * is paid out as dividend, the other half is retained.
     * <p>Note: <b>setRevenue()</b> must have been called before this method.
     * @param companyName Name of the company splitting the dividend.
     * @return False if an error is found.
     * TODO Check if split is allowed.
     * TODO The actual payout.
     * TODO Rounding up or down an odd revenue per share.
     */
    public boolean splitPayout (String companyName) {
        
       String errMsg = null;
       
       // Dummy loop to enable a quick jump out.
       while (true) {
           
           // Checks
           // Must be correct company.
           if (!companyName.equals(operatingCompany.getName())) {
               errMsg = "Wrong company "+companyName;
               break;
           }
           // Must be correct step
           if (step != STEP_PAYOUT) {
               errMsg = "Wrong action, expected Revenue Assignment";
              break;
           }
           // Split must be allowed
           if (splitRule == SPLIT_NOT_ALLOWED) {
               errMsg = "Split not allowed";
               break;
           }
           break;
       }
       if (errMsg != null) {
           Log.error ("Cannot split revenue of " + Bank.format(currentRevenue) +": "+errMsg);
           return false;
       }
       
       Log.write (companyName+" pays out half dividend");
       operatingCompany.splitRevenue (currentRevenue);
       nextStep(operatingCompany);
       
       return true;
   }

    /**
     * A previously entered revenue is fully withheld.
     * <p>Note: <b>setRevenue()</b> must have been called before this method.
     * @param companyName Name of the company withholding the dividend.
     * @return False if an error is found.
     */
     public boolean withholdPayout (String companyName) {
        
       String errMsg = null;
       
       // Dummy loop to enable a quick jump out.
       while (true) {
           
           // Checks
           // Must be correct company.
           if (!companyName.equals(operatingCompany.getName())) {
               errMsg = "Wrong company "+companyName;
               break;
           }
           // Must be correct step
           if (step != STEP_PAYOUT) {
               errMsg = "Wrong action, expected Revenue Assignment";
               break;
           }
           break;
       }
       if (errMsg != null) {
           Log.error ("Cannot withhold revenue of "+currentRevenue+": "+errMsg);
           return false;
       }
       Log.write(companyName + " withholds dividend of " + Bank.format(currentRevenue));
      
       operatingCompany.withhold (currentRevenue);
       
       nextStep(operatingCompany);
       
       return true;
    }
    
    /**
     * Internal method: change the OR state to the next step.
     * If the currently Operating Company is done, notify this.  
     * @param company The current company.
     */
    protected void nextStep(PublicCompanyI company) {
        if (++step >= steps.length) done(company.getName());
    }
    
    /**
     * The current Company is done operating.
     * @param company Name of the company that finished operating.
     * @return False if an error is found.
     */
    public boolean done (String companyName) {

        String errMsg = null;
        
        if (!companyName.equals(operatingCompany.getName())) {
            errMsg = "Wrong company "+companyName;
            return false;
        }
        
        if (++operatingCompanyIndex >= operatingCompanyArray.length) {
            // OR done. Inform GameManager.
            Log.write("End of Operating Round " + getCompositeORNumber());
            GameManager.getInstance().nextRound(this);
            return true;
        }
        
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];
        step = steps[0];

        return true;
    }

    /**
     */
     public boolean buyTrain (String companyName, TrainI train, int price) {
         
         return buyTrain (companyName, train, price, null);
     }
     
     public boolean buyTrain (String companyName, TrainI train, int price,
             TrainI exchangedTrain) {
   
        String errMsg = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            //Portfolio oldHolder = train.getHolder();
            //CashHolder oldOwner = oldHolder.getOwner();
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            // Must be correct step
            if (step != STEP_BUY_TRAIN) {
                errMsg = "Wrong action, expected Train buying cost";
                break;
            }
            
            if (train == null) {
                errMsg = "No train specified";
                break;
            }
            // Assume for now that buying this train is allowed.
            // Actually we should check this here.

            // Zero price means face value.
            if (price == 0) price = train.getCost();

            // Amount must be non-negative
            if (price < 0) {
                errMsg = "Negative amount not allowed";
                break;
            }

            // Does the company have room for another train?
            int currentNumberOfTrains = operatingCompany.getPortfolio().getTrains().length;
            int trainLimit = operatingCompany.getTrainLimit(PhaseManager.getInstance().getCurrentPhaseIndex());
            if (currentNumberOfTrains >= trainLimit) {
                errMsg = "Would exceed train limit of "+trainLimit;
                break;
            }

            // Does the company have the money?
            if (price > operatingCompany.getCash()) {
                errMsg = "Not enough money";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error (companyName+ " cannot buy "+train.getName()+"-train for "
                    +Bank.format(price)+": "+errMsg);
            return false;
        }
        
        Portfolio oldHolder = train.getHolder();
        CashHolder oldOwner = oldHolder.getOwner();
        
        if (exchangedTrain != null) {
            TrainI oldTrain = operatingCompany.getPortfolio().getTrainOfType(exchangedTrain.getType());
            Bank.getPool().buyTrain(oldTrain, 0);
            Log.write(operatingCompany.getName()+" exchanges "+exchangedTrain
                    +"-train for a "+train.getName()
                    +"-train from "+oldHolder.getName()+" for "
                    +Bank.format(price));
        } else {
            Log.write(operatingCompany.getName()+" buys "+train.getName()
                    +"-train from "+oldHolder.getName()+" for "
                    +Bank.format(price));
        }

        operatingCompany.getPortfolio().buyTrain(train, price);

        TrainManager.get().checkTrainAvailability(train, oldHolder);
        
        return true;
     }
    
    /**
     * Let a public company buy a private company.
     * @param company Name of the company buying a private company.
     * @param privateName Name of teh private company.
     * @param price Price to be paid.
     * @return False if an error is found.
     * TODO: Is private buying allowed at all?
     * TODO: Is the game phase correct?
     */
    public boolean buyPrivate (String companyName, String privateName, int price) {

        String errMsg = null;
        PrivateCompanyI privCo = null;
        CashHolder owner = null;
        Player player = null;
        int basePrice;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Must be correct company.
            if (!companyName.equals(operatingCompany.getName())) {
                errMsg = "Wrong company "+companyName;
                break;
            }
            
            // Does private exist?
            if ((privCo = Game.getCompanyManager().getPrivateCompany(privateName)) == null) {
                errMsg = "Private "+privateName+" does not exist";
                break;
            }
            // Is private still open?
            if (privCo.isClosed()) {
                errMsg = "Private "+privateName+" is already closed";
                break;
            }
            // Is private owned by a player?
            owner = privCo.getPortfolio().getOwner();
            if (!(owner instanceof Player)) {
                errMsg = "Private "+privateName+" is not owned by a player";
                break;
            }
            player = (Player) owner;
            basePrice = privCo.getBasePrice();
            
            // Price must be in the allowed range
            if (price < basePrice * operatingCompany.getLowerPrivatePriceFactor()) {
                errMsg = "Price is less than lower limit of "
                    + (int)(basePrice * operatingCompany.getLowerPrivatePriceFactor());
                break;
            }
            if (price > basePrice * operatingCompany.getUpperPrivatePriceFactor()) {
                errMsg = "Price is more than upper limit of "
                    + (int)(basePrice * operatingCompany.getUpperPrivatePriceFactor());
                break;
            }
            // Does the company have the money?
            if (price > operatingCompany.getCash()) {
                errMsg = "Not enough money";
                break;
            }
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot buy private "+privateName+" from "
                    + (owner == null ? "?" : owner.getName()) + " for " + price + ": "+errMsg);
            return false;
        }
        
        operatingCompany.getPortfolio().buyPrivate(privCo, player.getPortfolio(), price);
        
        return true;

    }
    
    /**
     * Close a private. For now, this is an action to be initiated separately
     * from the UI, but it will soon be coupled to the actual actions that 
     * initiate private closing. By then, this method will probably no longer
     * be accessible from the UI, which why it is deprecated from its creation.
     * @param privateName name of the private to be closed.
     * @return False if an error occurs.
     * @deprecated Will probably move elsewhere and become not accessible to the UI.
     */
    public boolean closePrivate (String privateName) {
        String errMsg = null;
        PrivateCompanyI privCo = null;
        
        // Dummy loop to enable a quick jump out.
        while (true) {
            
            // Checks
            // Does private exist?
            if ((privCo = Game.getCompanyManager().getPrivateCompany(privateName)) == null) {
                errMsg = "Private "+privateName+" does not exist";
                break;
            }
            // Is private still open?
            if (privCo.isClosed()) {
                errMsg = "Private "+privateName+" is already closed";
                break;
            }
            
            break;
        }
        if (errMsg != null) {
            Log.error ("Cannot close private "+privateName+": "+errMsg);
            return false;
        }
        
        privCo.setClosed();
        Log.write("Private "+privateName+" is closed");
        
        return true;

        
    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/
    

    /**
     * @return The player that has the turn (in this case:
     * the President of the currently operating company).
     */
    public Player getCurrentPlayer() {
        return operatingCompany.getPresident();
    }
    /**
     * @return The index of the player that has the turn.
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Get the public company that has the turn to operate.
     * @return The currently operating company object.
     */
    public PublicCompanyI getOperatingCompany () {
        return operatingCompany;
    }
    
    public PublicCompanyI[] getOperatingCompanies () {
        return operatingCompanyArray;
    }
    
    /**
     * Get the current operating round step (i.e. the next action).
     * @return The number that defines the next action.
     */
    public int getStep () {
        return step;
    }
    
    public int getOperatingCompanyIndex () {
        return operatingCompanyIndex;
    }
    
    /**
     * Get a list of private companies that are available for buying,
     * i.e. which are in the hands of players.
     * @return An array of the buyable privates.
     * TODO Check if privates can be bought at all. 
     */
    public PrivateCompanyI[] getBuyablePrivates () {
        ArrayList buyablePrivates = new ArrayList();
        PrivateCompanyI privCo;
        Iterator it = Game.getCompanyManager().getAllPrivateCompanies().iterator();
        while (it.hasNext()) {
            if ((privCo = (PrivateCompanyI)it.next()).getPortfolio().getOwner() instanceof Player)
                buyablePrivates.add(privCo);
        }
        return (PrivateCompanyI[]) buyablePrivates.toArray(new PrivateCompanyI[0]);
    }
    
    /**
     * Chech if revenue may be split.
     * @return True if revenue can be split.
     */
    public boolean isSplitAllowed() {
        return (splitRule != SPLIT_NOT_ALLOWED);
    }
    
    /** Get all possible tile build costs in a game.
     * This is a (perhaps temporary) method to play without a map.
     * @author Erik Vos
     */
    public int[] getTileBuildCosts () {
       // Result is currently hardcoded, but can be made configurable. 
        return new int[] {0,80,120};
    }
    
    /** Get all possible token laying costs in a game.
     * This is a (perhaps temporary) method to play without a map.
     * @author Erik Vos
     */
    public int[] getTokenLayCosts () {
       // Result is currently hardcoded, but can be made configurable. 
        return new int[] {0,40,100};
    }
    
    /** Get all train costs in a game.
     * This is a (temporary) method to play without trains.
     * @author Erik Vos
     */
    public int[] getTrainCosts () {
        // Result is currently hardcoded, but can be made configurable. 
        return new int[] {0,80,180,300,450,630,1100};
        
    }

}