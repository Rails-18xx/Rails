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
        for (int i=0; i<companies.length; i++) {
            company = companies[i];
            if (!company.hasFloated()) continue;
            space = company.getCurrentPrice();
            // Key must put companies in reverse operating order, because sort is ascending.
            key = 1000000 * (999-space.getPrice()) + 10000 * (99-space.getColumn())
            		+ 100 * space.getRow() + space.getStackPosition(company);
            //System.out.println("OR key of "+company.getName()+" is "+key);
            operatingCompanies.put(new Integer(key), company);
        }
        
        operatingCompanyArray = (PublicCompanyI[])operatingCompanies.values()
        		.toArray(new PublicCompanyI[0]);
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];
        step = steps[0];
        
        relativeORNumber++;
        cumulativeORNumber++;
    }
    
    /*----- General methods -----*/
    
    /**
     * Return the operating round (OR) number in the format sr.or, 
     * where sr is the last stock round number and or is the relative
     * OR number.  
     * @return Composite SR/OR number.
     */
    public double getCompositeORNumber() {
        return StockRound.getLastStockRoundNumber() + 0.1 * relativeORNumber;
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
		if (amountSpent > 0) Log.write (companyName+" spends "+amountSpent+" while laying track");
    
        nextStep (operatingCompany);
        
        return true;
     }
    
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
        if (amountSpent > 0) Log.write (companyName+" spends "+amountSpent+" while laying token");
       
        nextStep (operatingCompany);
        
        return true;
     }
    
    /**
     * Save a given revenue.
     * This is a temporarily needed method, needed as long as the program 
     * cannot yet calculate revenues.
     * @param amount The revenue.
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
		Log.write (companyName+" earns "+amount);
       
        nextStep(operatingCompany);
        
        /* If the revenue is 0, the effect is the same as withholding,
         * so do that (shortcut: this is not always true).
         */
        if (amount == 0) {
            operatingCompany.withhold(0);
            nextStep (operatingCompany);
        }
        
        return true;
    }
    
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

        Log.write(companyName + " pays out full dividend of "+currentRevenue);
        operatingCompany.payOut(currentRevenue);

        nextStep(operatingCompany);

        return true;
    }
    
    /**
     * Split the revenue
     * @param company The company paying out.
     * @return false in case of an error.
     * TODO Check if split is allowed
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
           Log.error ("Cannot split revenue of "+currentRevenue+": "+errMsg);
           return false;
       }
       
       Log.write (companyName+" pays out half dividend");
       //company.splitRevenue (currentRevenue);
       nextStep(operatingCompany);
       
       return true;
   }

    /**
     * Withhold the revenue
     * @param company The company paying out.
     * @return false in case of an error.
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
       Log.write(companyName + " withholds dividend of "+currentRevenue);
      
       operatingCompany.withhold (currentRevenue);
       
       nextStep(operatingCompany);
       
       return true;
    }
    
    protected void nextStep(PublicCompanyI company) {
        if (++step >= steps.length) done(company.getName());
    }
    
    /**
     * Company is done operating.
     * @param company
     * @return
     * TODO: inform GameManager about end of OR.
     */
    public boolean done (String companyName) {

        String errMsg = null;
        
        if (!companyName.equals(operatingCompany.getName())) {
            errMsg = "Wrong company "+companyName;
            return false;
        }
        
        if (++operatingCompanyIndex >= operatingCompanyArray.length) {
            // OR done.
            return true;
        }
        
        operatingCompany = operatingCompanyArray[operatingCompanyIndex];
        step = steps[0];

        return true;
    }

    public boolean buyTrain (String companyName, int amountSpent) {
        
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
            if (step != STEP_BUY_TRAIN) {
                errMsg = "Wrong action, expected Train buying cost";
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
            Log.error ("Cannot process train buying cost of "+amountSpent+": "+errMsg);
            return false;
        }
        
        Bank.transferCash ((CashHolder)operatingCompany, null, amountSpent);
		Log.write (companyName+" spends "+amountSpent+" while buying train(s)");
       
        nextStep (operatingCompany);
        
        return true;
     }
    
    /**
     * Let a company buy a private company.
     * @param company
     * @param privateName
     * @param price
     * @return
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
            // Is private owned by a player?
            owner = privCo.getHolder().getOwner();
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

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/
    

    /**
     * @return Returns the currentPlayer.
     */
    public Player getCurrentPlayer() {
        return operatingCompany.getPresident();
    }
    /**
     * @return Returns the currentPlayer.
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    public PublicCompanyI getOperatingCompany () {
        return operatingCompany;
    }
    
    public int getStep () {
        return step;
    }
    
    public PrivateCompanyI[] getBuyablePrivates () {
        ArrayList buyablePrivates = new ArrayList();
        PrivateCompanyI privCo;
        Iterator it = Game.getCompanyManager().getAllPrivateCompanies().iterator();
        while (it.hasNext()) {
            if ((privCo = (PrivateCompanyI)it.next()).getHolder().getOwner() instanceof Player)
                buyablePrivates.add(privCo);
        }
        return (PrivateCompanyI[]) buyablePrivates.toArray(new PrivateCompanyI[0]);
    }
    
    public boolean isSplitAllowed() {
        return (splitRule != SPLIT_NOT_ALLOWED);
    }
}