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
 * Implements a basic Stock Round.
 * <p>A new instance must be created for each new Stock Round.
 * At the end of a round, the current instance should be discarded.
 * <p>Permanent memory is formed by static attributes (like who has the Priority Deal). 
 * @author Erik Vos
 * TODO Implement end of the stock round if all player pass.
 */
public class StockRound implements Round
{
    /* Transient memory (per round only) */
    protected Player currentPlayer;
    protected int currentPlayerIndex;
    protected boolean hasBoughtThisTurn = false;
    protected boolean hasSoldThisTurnBeforeBuying = false;
    protected boolean hasPassed = true; // Is set false on any player action
    int numPasses = 0;
    
    /* Transient data needed for rule enforcing */
    /** HashMap per player containing a HashMap per company */
    protected HashMap playersThatSoldThisRound = new HashMap();
    /** HashMap per player */
    protected HashMap playersThatBoughtThisRound = new HashMap();
    
    /* Rule constants */
    static protected final int SELL_BUY_SELL = 0;
    static protected final int SELL_BUY = 1;
    static protected final int SELL_BUY_OR_BUY_SELL = 2;
    
    /* Permanent memory */
    static protected Player[] players;
    static protected Player priorityPlayer;
    static protected int priorityPlayerIndex;
    static protected int stockRoundNumber = 0;
    static protected StockMarketI stockMarket;
    static protected Portfolio ipo;
    static protected Portfolio pool;
    
    /* Rules */
    static protected int sequenceRule = SELL_BUY_SELL; // Currently fixed
    static protected boolean buySellInSameRound = true;
    
   
    /**
     * The constructor.
     */
    public StockRound() {
        
        if (players == null) {
            players = Game.getPlayerManager().getPlayersArray();
            priorityPlayerIndex = 0;
            priorityPlayer = players[priorityPlayerIndex];
        }
        currentPlayerIndex = priorityPlayerIndex;
        currentPlayer = players[priorityPlayerIndex];
        
        if (stockMarket == null) stockMarket = StockMarket.getInstance();
        if (ipo == null) ipo = Bank.getIpo();
        if (pool == null) pool = Bank.getPool();
        
        stockRoundNumber++;
    }
    
    /*----- General methods -----*/
    
    public int getStockRoundNumber() {
        return stockRoundNumber;
    }
   
    /*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/
    
    /**
     * Start a company by buying the President's share only
     * @param company The company to start.
     * @return True if the company could be started.
     */
    public boolean startCompany (Player player, PublicCompanyI company, int price) {
        return startCompany(player, company, price, 1);
    }
    
    /**
     * Start a company by buying one or more shares (more applies to e.g. 1841)
     * @param player The player that wants to start a company.
     * @param company The company to start.
     * @param price The start (par) price (ignored if the price is fixed).
     * @param shares The number of shares to buy (can be more than 1 in e.g. 1841).
     * @return True if the company could be started. False indicates an error.
     * TODO Error messages.
     */
    public boolean startCompany (Player player, PublicCompanyI company, int price, int shares) {
        
        String errMsg = null;
        StockSpaceI startSpace = null;
        int numberOfCertsToBuy = 0;
        CertificateI cert = null;
        
        // Dummy loop to allow a quick jump out
        while (true) {

	        // Check everything
	        // Only the player that has the turn may buy
	        if (player != currentPlayer) {
	            errMsg = "Wrong player";
	            break;
	        }
	            
	        // The player may not have bought this turn.
	        if (hasBoughtThisTurn) { 
	            errMsg = player.getName()+" already bought this turn";
	            break;
	        }
	        
	        // The company may not have started yet.
	        if (company.hasStarted()) {
	            errMsg = company.getName()+" was started before";
	            break;
	        }
	        
	        // Find the President's certificate
	        cert = ipo.findCertificate(company, true);
	        // Make sure that we buy at least one!
	        if (shares < cert.getShares()) shares = cert.getShares();
	        
	        // Determine the number of Certificates to buy
	        // (shortcut: assume that any additional certs are one share each) 
	        numberOfCertsToBuy = shares - (cert.getShares()-1);
	        // Check if the player may buy that many certificates.
	        if (!player.mayBuyCertificates(numberOfCertsToBuy)) {
	            errMsg = "Player "+player.getName()+" cannot buy more certificates";
	            break;
	        }
	        
	        // Check if the company has a fixed par price (1835).
	        startSpace = company.getParPrice();
	        if (startSpace != null) {
	            // If so, it overrides whatever is given.
	            price = startSpace.getPrice();
	        } else {
	            // Else the given price must be a valid start price
	            if ((startSpace = stockMarket.getStartSpace(price)) == null) {
	                errMsg = "Invalid start price: "+price;
	                break;
	            }
	        }
	        
	        // Check if the Player has the money.
	        if (player.getCash() < shares * price) {
	            errMsg = "Player "+player.getName()+" has not enough money";
	            break;
	        }
	        
	        break;
	    }
        
        if (errMsg != null) {
            Log.error (player.getName()+" cannot start "
                    + company.getName()+": "+errMsg);
            return false;
        }
        
        // All is OK, now start the company
        company.start(startSpace);
        
        // Transfer the President's certificate
        player.getPortfolio().buyCertificate (cert, ipo, cert.getCertificatePrice());
        
        // If more than one certificate is bought at the same time, transfer these too.
        for (int i=1; i<numberOfCertsToBuy; i++) {
            cert = ipo.findCertificate(company, false);
            player.getPortfolio().buyCertificate (cert, ipo, cert.getCertificatePrice());
        }
        Log.write(player.getName() + " starts "+company.getName() +" and buys " 
                + shares+" share(s) ("+cert.getShare() + "%) for " + price  + ".");
       
        hasBoughtThisTurn = true;
        hasPassed = false;
        setPriority();
        
        return true;
    }
    
    /**
     * Buying one or more shares (more is sometimes possible)
     * @param player The player that wants to buy shares.
     * @param portfolio The portfolio from which to buy shares. 
     * @param company The company of which to buy shares.
     * @param shares The number of shares to buy.
     * @return True if the company could be started. False indicates an error.
     * TODO FLoat level is hardcoded.
     * TODO Does not yet cater for double non-president shares as in 1835. 
     * TODO Error messages.
     */
    public boolean buyShare (Player player, Portfolio from, PublicCompanyI company, int shares) {

        String errMsg = null;
        int price = 0;
        
        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
	        // Only the player that has the turn may buy
	        if (player != currentPlayer) {
	            errMsg = "Wrong player"; break;
	        }
	                	
	        // The player may not have bought this turn (shortcut: shares in brown disregarded)
	        if (hasBoughtThisTurn) {
	            errMsg = player.getName()+" already bought this turn"; 
	            break;
	        }
	        
	        // The player may not have sold the company this round.
	        if (playersThatSoldThisRound.containsKey(player) &&
	                ((HashMap)playersThatSoldThisRound.get(player)).containsKey(company)) {
	            errMsg =  player.getName()+" already sold "+company.getName()+" this turn";
	            break;
	        }
	        
	        // The company must have started before
	        if (!company.hasStarted()) { 
	            errMsg = "Company "+company.getName()+" is not yet started";
	            break;
	        }
	        
	        // Check if that many shares are available
	        if (shares > from.countShares(company)) { 
	            errMsg =  company.getName()+" share(s) not available";
	            break;
	        }
	
	        StockSpaceI currentSpace = company.getCurrentPrice();
	
	        // Check if it is allowed to buy more than one certificate (if requested)
	        if (shares > 1 && !currentSpace.isNoBuyLimit()) {
	            errMsg = "Cannot buy more than 1 "+company.getName()+ " share";
	            break;
	        }
	        
	        // Check if player would not exceed the certificate limit.
	        // (shortcut: assume 1 cert == 1 certificate)
	        if (!currentSpace.isNoCertLimit() && !player.mayBuyCertificates(shares)) {
	            errMsg = player.getName()+" would exceed certificate limit";
	            break;
	        }
	        
	        // Check if player would exceed the per-company share limit
	        if (!currentSpace.isNoHoldLimit() && !player.mayBuyCompanyShare(company, shares)) { 
	            errMsg = player.getName()+" would exceed holding limit";
	            break;
	        }
	
	        price = currentSpace.getPrice();
	        
	        // Check if the Player has the money.
	        if (player.getCash() < shares * price) { 
	            errMsg = player.getName()+" does not have enough money";
	            break;
	        }
	        
	        break;
        }
        
        if (errMsg != null) {
            Log.error (player.getName()+" cannot buy "+shares+" share(s) of "
                    + company.getName()+" from "
                    +from.getName()+": "+errMsg);
            return false;
        }
        
        // All seems OK, now buy the shares.
        CertificateI cert;
        for (int i=0; i<shares; i++) {
            cert = from.findCertificate(company, false);
            player.getPortfolio().buyCertificate (cert, from, price * cert.getShares());
            Log.write(player.getName() + " buys " + shares+" share(s) ("+cert.getShare() + "%) of "
                    + company.getName() + " from " + from.getName()
                    + " for " + price  + ".");
       }

        hasBoughtThisTurn = true;
        hasPassed = false;
        setPriority();

        // Check if the company has floated
        /* Shortcut: float level and capitalisation hardcoded */
		if (from == ipo && !company.hasFloated() && from.countShares(company) <= 40) {
			// Float company (limit and capitalisation to be made configurable)
			company.setFloated(10*price);
			Log.write (company.getName()+ " floats and receives "+company.getCash());
		}

        return true;
    }
    
    public boolean sellShare (Player player, PublicCompanyI company) {
        return sellShares (player, company, 1);
        
    }
    
    /**
     * Sell one or more shares.
     * @param player The selling player.
     * @param company The company of which shares to sell.
     * @param number The number of shares to sell.
     * TODO Does not yet cater for double shares (incl. president).
     * TODO Bank pool limit to be made configurable.
     * @return
     */
    public boolean sellShares (Player player, PublicCompanyI company, int number) {
        
        Portfolio portfolio = player.getPortfolio();
        String errMsg = null;
        
        // Dummy loop to allow a quick jump out
        while (true) {

           // Check everything
           if (player != currentPlayer) {
                errMsg = "Wrong player";
                break;
            }

	        // May not sell in certain cases
	        if (sequenceRule == SELL_BUY_OR_BUY_SELL && hasBoughtThisTurn 
	                && hasSoldThisTurnBeforeBuying
	        	|| sequenceRule == SELL_BUY && hasBoughtThisTurn) {
	            errMsg = "May not sell anymore in this turn";
	            break;
	        }
	        
	        // The player must have the share(s)
	        if (portfolio.countShares(company) < number) {
	            errMsg = "Does not have the share(s)";
	            break;
	        }
	        
	        // The pool may not get over its limit (to be made configurable).
	        if (pool.countShares(company) + number*company.getShareUnit() > 50) {
	            errMsg = "Pool would get over its share holding limit";
	            break;
	        }
	        
	        break;
        }
        
        if (errMsg != null) {
            Log.error (player.getName()+" cannot sell "+number+" share(s) of "
                    + company.getName()+": "+errMsg);
            return false;
        }
        
        // All seems OK, now do the selling.
        CertificateI cert;
        int price = company.getCurrentPrice().getPrice();
        for (int i=0; i<number; i++) {
            cert = portfolio.findCertificate(company, false);
            pool.buyCertificate (cert, portfolio, cert.getShares()*price);
			Log.write(player.getName()+" sells "+number+" shares of "+company.getName()
			        +" at "+price);
        }
        
        // Remember that the player has sold this company this round.
        if (!playersThatSoldThisRound.containsKey(player)) {
            playersThatSoldThisRound.put(player, new HashMap());
        }
        ((HashMap)playersThatSoldThisRound.get(player)).put(company, null);
        
        if (!hasBoughtThisTurn) hasSoldThisTurnBeforeBuying = true;
        hasPassed = false;
        setPriority();

        return true;
    }
    
    /**
     * The current Player passes or is done.
     * @param player 
     * @return
     * TODO: Inform GameManager about round change.
     */
    public boolean done (Player player) {
        if (player != currentPlayer) return false;

        if (hasPassed) {
            numPasses++;
            Log.write (currentPlayer.getName()+" passes.");
        } else {
            numPasses = 0;
        }
        if (numPasses >= players.length) {
            Log.write("All players have passed, end of SR "+stockRoundNumber);
            // TODO: Inform GameManager
        } else {        
            setNextPlayer();
        }

        return true;
    }
    
    /**
     * Internal method: pass the turn to another player.
     */
    protected void setNextPlayer() {
        
        if (++currentPlayerIndex >= players.length) currentPlayerIndex = 0;
        currentPlayer = players[currentPlayerIndex];
        hasBoughtThisTurn = false;
        hasSoldThisTurnBeforeBuying = false;
        hasPassed = true;
    }
    
    /**
     * Remember the player that has the Priority Deal.
     * <b>Must be called BEFORE setNextPlayer()!</b>
     */
    protected void setPriority() {
        priorityPlayerIndex = (currentPlayerIndex < players.length-1 ? currentPlayerIndex+1 : 0);
        priorityPlayer = players[priorityPlayerIndex];
    }
    
    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/
    
    /**
     * @return Returns the priorityPlayer.
     */
    public static Player getPriorityPlayer() {
        return priorityPlayer;
    }
    /**
     * @return Returns the priorityPlayer.
     */
    public static int getPriorityPlayerIndex() {
        return priorityPlayerIndex;
    }
   /**
     * @return Returns the currentPlayer.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
    }
    /**
     * @return Returns the currentPlayer.
     */
    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }
    
    /**
     * Check if a public company can be started by the current player.
     * @param company The company to be checked. 
     * @return True of false.
     * TODO Check for unstarted companies that may not yet be started.
     * TODO Check if current player has enough money to start at the lowest price.
     */
    public boolean isCompanyStartable(PublicCompanyI company) {
        
        return !company.hasStarted();
    }
    
    /**
     * Check if a company can be bought by the current player from a given Portfolio.
     * @param company The company to be checked. 
     * @param source The portfolio that is checked for presence of company shares. 
     * TODO Buying from company treasuries if just IPO is specified.
     * TODO Add checks that the current player may buy and has the money.
     * TODO Presidencies in the Pool (rare!) 
     */
    public boolean isCompanyBuyable (PublicCompanyI company, Portfolio source) {

        if (!company.hasStarted()) return false;
        if (source.findCertificate(company, false) == null) return false;
        return true;
    }
    
    /**
     * Return a list of all companies that the current player can sell shares of.
     * @return List sellable companies.
     * TODO Make Bank Pool share limit configurable.
     */
    public boolean isCompanySellable (PublicCompanyI company) {
 
        if (currentPlayer.getPortfolio().findCertificate(company, false) == null) return false;
        if (pool.countShares(company)*company.getShareUnit() >= 50) return false;
        return true;
    }
    
    /**
     * Can the current player do any selling?
     * @return
     */
    public boolean mayCurrentPlayerSellAtAll () {
        if (sequenceRule == SELL_BUY_OR_BUY_SELL && hasBoughtThisTurn 
                && hasSoldThisTurnBeforeBuying
            || sequenceRule == SELL_BUY && hasBoughtThisTurn) return false;
        return true;
    }
    
    /**
     * Can the current player do any buying?
     * @return
     */
    public boolean mayCurrentPlayerBuyAtAll () {
        return !hasBoughtThisTurn;
   }
 }