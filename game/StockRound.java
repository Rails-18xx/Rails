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
    protected Player currentPlayer;
    protected int currentPlayerIndex;
    protected StockMarketI stockMarket;
    protected Portfolio ipo;
    protected Portfolio pool;
    protected HashMap playersThatSoldThisRound = new HashMap();
    
    static protected Player[] players;
    static protected Player priorityPlayer;
    static protected int priorityPlayerIndex;
    static protected int stockRoundNumber = 0;
    
   
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
        
        stockMarket = StockMarket.getInstance();
        ipo = Bank.getIpo();
        pool = Bank.getPool();
        
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
        
        StockSpaceI startSpace;
        
        // Check everything
        // Only the player that has the turn may buy
        if (player != currentPlayer) return false;
        // The company may not have started yet.
        if (company.hasStarted()) return false;
        
        // Find the President's certificate
        CertificateI cert = ipo.findCertificate(company, true);
        // Make sure that we buy at least one!
        if (shares < cert.getShares()) shares = cert.getShares();
        
        // Determine the number of Certificates to buy
        // (shortcut: assume that any additional certs are one share each) 
        int numberOfCertsToBuy = shares - (cert.getShares()-1);
        // Check if the player may buy that many certificates.
        if (!player.mayBuyCertificates(numberOfCertsToBuy)) return false;
        
        // Check if the company has a fixed par price (1835).
        startSpace = company.getParPrice();
        if (startSpace != null) {
            // If so, it overrides whatever is given.
            price = startSpace.getPrice();
        } else {
            // Else the given price must be a valid start price
            if ((startSpace = stockMarket.getStartSpace(price)) == null) return false;
        }
        
        // Check if the Player has the money.
        if (player.getCash() < shares * price) return false;
        
        // All is OK, now start the company
        company.start(startSpace);
        
        // Transfer the President's certificate
        player.getPortfolio().buyCertificate (cert, ipo, cert.getCertificatePrice());
        
        // If more than one certificate is bought at the same time, transfer these too.
        for (int i=1; i<numberOfCertsToBuy; i++) {
            cert = ipo.findCertificate(company, false);
            player.getPortfolio().buyCertificate (cert, ipo, cert.getCertificatePrice());
        }
        
        setNextPlayer();
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
     * TODO Does not yet cater for double non-president shares as in 1835. 
     * TODO Error messages.
     */
    public boolean buyShare (Player player, Portfolio from, PublicCompanyI company, int shares) {

        // Check everything
        // Only the player that has the turn may buy
        if (player != currentPlayer) return false;
        // The player may not have sold the company this round.
        if (playersThatSoldThisRound.containsKey(player) &&
                ((HashMap)playersThatSoldThisRound.get(player)).containsKey(company)) {
            return false;
        }
        // The company must have started before
        if (!company.hasStarted()) return false;
        // Check if that many shares are available
        if (shares > from.countShares(company)) return false;

        StockSpaceI currentSpace = company.getCurrentPrice();

        // Check if it is allowed to buy more than one certificate (if requested)
        if (shares > 1 && !currentSpace.isNoBuyLimit()) return false;
        // Check if player would not exceed the certificate limit.
        // (shortcut: assume 1 cert == 1 certificate)
        if (!currentSpace.isNoCertLimit() && !player.mayBuyCertificates(shares)) return false;
        // Check if player would exceed the per-company share limit
        if (!currentSpace.isNoHoldLimit() && !player.mayBuyCompanyShare(company, shares)) return false;

        int price = currentSpace.getPrice();
        
        // Check if the Player has the money.
        if (player.getCash() < shares * price) return false;
        
        // All seems OK, now buy the shares.
        CertificateI cert;
        for (int i=0; i<shares; i++) {
            cert = from.findCertificate(company, false);
            player.getPortfolio().buyCertificate (cert, from, price * cert.getShares());
        }
       
        setNextPlayer();
        setPriority();
        
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
        if (player != currentPlayer) return false;
        
        Portfolio portfolio = player.getPortfolio();
        
        // Check everything
        // Only the player that has the turn may buy
        if (player != currentPlayer) return false;
        // The player must have the share(s)
        if (portfolio.countShares(company) < number) return false;
        // The pool may not get over its limit (to be made configurable).
        if (pool.countShares(company) + number*company.getShareUnit() > 50) return false;
        
        // All seems OK, now do the selling.
        CertificateI cert;
        int price = company.getCurrentPrice().getPrice();
        for (int i=0; i<number; i++) {
            cert = portfolio.findCertificate(company, false);
            pool.buyCertificate (cert, portfolio, cert.getShares()*price);
        }
        
        // Remember that the player has sold this company this round.
        if (!playersThatSoldThisRound.containsKey(player)) {
            playersThatSoldThisRound.put(player, new HashMap());
        }
        ((HashMap)playersThatSoldThisRound.get(player)).put(company, null);
        
        setNextPlayer();
        setPriority();

        return true;
    }
    
    public boolean pass (Player player) {
        if (player != currentPlayer) return false;

        setNextPlayer();
        
        return true;
    }
    
    protected void setNextPlayer() {
        
        if (++currentPlayerIndex >= players.length) currentPlayerIndex = 0;
        currentPlayer = players[currentPlayerIndex];
    }
    
    /**
     * Remember the player that has the Priority Deal.
     * <b>Must be called setNextPlayer()!</b>
     */
    protected void setPriority() {
        priorityPlayerIndex = currentPlayerIndex;
        priorityPlayer = currentPlayer;
    }
    
    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/
    
    /**
     * @return Returns the priorityPlayer.
     */
    public static Player getPriorityPlayer() {
        return priorityPlayer;
    }
    /**
     * @return Returns the currentPlayer.
     */
    public Player getCurrentPlayer() {
        return currentPlayer;
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

        return source.findCertificate(company, false) != null;
    }
    
    /**
     * Return a list of all companies that the current player can sell shares of.
     * @return List sellable companies.
     * TODO Make Bank Pool share limit configurable.
     */
    public boolean isCompanySellable (PublicCompanyI company) {
 
        return currentPlayer.getPortfolio().findCertificate(company, false) != null &&
                    pool.countShares(company)*company.getShareUnit() < 50;
    }
    
     
 }