/**
 * This class implements the 1880 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.BuyCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.game.action.UseSpecialProperty;
import rails.game.move.CashMove;
import rails.game.special.SpecialPropertyI;
import rails.game.specific._1880.PublicCompany_1880;


public class StockRound_1880 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's)
     * Constructor to initialize
     * 
     * @param aGameManager The GameManager Object needed to initialize the Stock
     * Round
     * 
     */
    public StockRound_1880(GameManagerI aGameManager) {
        super(aGameManager);
    }
    
    public void start() {
        for (PrivateCompanyI company : companyManager.getAllPrivateCompanies()) {
            if (company.hasSpecialProperties() == true) {
                List<SpecialPropertyI> properties = company.getSpecialProperties();
                // TODO: Make this part of a "generic" instead of hardcoded...
                if (company.getName().equals("WR") == true) {
                    if (properties.get(0).isExercised() == true) {
                        System.out.println("WR is exercised");
                    } else {
                        System.out.println("WR is not exercised");
                    }
                }
            }
        }
        
        super.start();
    }

    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice(PublicCompanyI company) {

        String companyName = company.getName();
        int price;

        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        if (!((PublicCompany_1880) company).isCommunistPhase()) {
            // stored price is the previous unadjusted price
            price = price / company.getShareUnitsForSharePrice();
            // Price adjusted by -5 per share for selling but only if we are not
            // in CommunistPhase...
            price = price - 5;
        }
        return price;
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice(PublicCompanyI company, int numberSold,
            boolean soldBefore) {
        // No more changes if it has already dropped
        // Or we are in the CommunistTakeOverPhase after the 4T has been bought
        // and the 6T has not yet been bought
        if ((!soldBefore)
            || (!((PublicCompany_1880) company).isCommunistPhase())) {
            super.adjustSharePrice(company, 1, soldBefore);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rails.game.StockRound#mayPlayerSellShareOfCompany(rails.game.PublicCompanyI
     * )
     */
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompanyI company) {
        if ((company.getPresident() == gameManager.getCurrentPlayer())
                && (company instanceof PublicCompany_1880)
            && (((PublicCompany_1880) company).isCommunistPhase())) {
            return false;
        }
        return super.mayPlayerSellShareOfCompany(company);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#gameSpecificChecks(rails.game.Portfolio, rails.game.PublicCompanyI)
     */
    @Override
      protected void gameSpecificChecks(Portfolio boughtFrom,
            PublicCompanyI company) {
        
        ((PublicCompany_1880) company).sharePurchased();        
        super.gameSpecificChecks(boughtFrom, company);
    }
    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1880. 
     */
    @Override
    protected void floatCompany(PublicCompanyI company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
        
        
        // For all Companies who float before the first 6T is purchased the Full Capitalisation will happen on purchase of 
        // 50 percent of the shares.
        // The exception of the rule of course are the late starting companies after the first 6 has been bought when the 
        // flotation will happen after 60 percent have been bought.
        if (((PublicCompany_1880) company).shanghaiExchangeIsOperational()) {
            cash = 10 * price;
        } else {
            cash = 5 * price;
        }
       
        company.setFloated(); 

        if (cash > 0) {
            new CashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("FloatsWithCash",
                    company.getName(),
                    Bank.format(cash) ));
        } else {
            ReportBuffer.add(LocalText.getText("Floats",
                    company.getName()));
        }

    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#processGameSpecificAction(rails.game.action.PossibleAction)
     */
    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {
        // TODO Auto-generated method stub
        return super.processGameSpecificAction(action);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#setBuyableCerts()
     */
    @Override
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        StockSpaceI stockSpace;
        Portfolio from;
        int price;
        int number;
        int unitsForPrice;

        int playerCash = currentPlayer.getCash();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompanyI companyBoughtThisTurn =
            (PublicCompanyI) companyBoughtThisTurnWrapper.get();
        if (companyBoughtThisTurn == null) {
            from = ipo;
            Map<String, List<PublicCertificateI>> map =
                from.getCertsPerCompanyMap();
            int shares;

            for (String compName : map.keySet()) {
                certs = map.get(compName);
                if (certs == null || certs.isEmpty()) continue;

                /* Only the top certificate is buyable from the IPO */
                int lowestIndex = 99;
                cert = null;
                int index;
                for (PublicCertificateI c : certs) {
                    index = c.getIndexInCompany();
                    if (index < lowestIndex) {
                        lowestIndex = index;
                        cert = c;
                    }
                }

                comp = cert.getCompany();
                unitsForPrice = comp.getShareUnitsForSharePrice();
                if (isSaleRecorded(currentPlayer, comp)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        cert.getShare()) < 1 ) continue;
                //Make sure that only 50  percent of shares are sold until all Certs are to become avail upon Phase change
                //after the first 3 Train has been sold from IPO.
                                      
                if (!((PublicCompany_1880) comp).certsAvailableForSale()) continue;
                
                /* Would the player exceed the total certificate limit? */
                stockSpace = comp.getCurrentSpace();
                if ((stockSpace == null || !stockSpace.isNoCertLimit()) && !mayPlayerBuyCertificate(
                        currentPlayer, comp, cert.getCertificateCount())) continue;

                shares = cert.getShares();

                if (!cert.isPresidentShare()) {
                    price = comp.getMarketPrice() / unitsForPrice; // Always use the market price
                    if (price <= playerCash) {
                        possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                                from, price));
                    }
                } else if (!comp.hasStarted()) {
                    ParSlotManager_1880 parSlotManager = ((GameManager_1880) gameManager).getParSlotManager();
                    Integer[] prices = parSlotManager.getAvailablePrices(playerCash/2);
                    Arrays.sort(prices);
                    int[] convertedPrices = new int[prices.length];
                    for (int i = 0; i < prices.length; i++) {
                        convertedPrices[i] = prices[i];
                    }
                    StartCompany_1880 action = new StartCompany_1880(comp, convertedPrices);
                    Integer[] parSlotIndicies = parSlotManager.getAvailableSlots(playerCash/2);
                    int[] convertedSlots = new int [parSlotIndicies.length];
                    for (int i = 0; i < parSlotIndicies.length; i++) {
                        convertedSlots[i] = parSlotIndicies[i];
                    }
                    action.setPossibleParSlotIndices(convertedSlots);
                    possibleActions.add(action);
                }
            }
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        Map<String, List<PublicCertificateI>> map =
            from.getCertsPerCompanyMap();
        /* Allow for multiple share unit certificates (e.g. 1835) */
        PublicCertificateI[] uniqueCerts;
        int[] numberOfCerts;
        int shares;
        int shareUnit;
        int maxNumberOfSharesToBuy;

        for (String compName : map.keySet()) {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty()) continue;

            comp = certs.get(0).getCompany();
            stockSpace = comp.getCurrentSpace();
            unitsForPrice = comp.getShareUnitsForSharePrice();
            price = stockSpace.getPrice() / unitsForPrice;
            shareUnit = comp.getShareUnit();
            maxNumberOfSharesToBuy
            = maxAllowedNumberOfSharesToBuy(currentPlayer, comp, shareUnit);

            /* Checks if the player can buy any shares of this company */
            if (maxNumberOfSharesToBuy < 1) continue;
            if (isSaleRecorded(currentPlayer, comp)) continue;
            if ((comp.sharesOwnedByPlayers() ==50) && (!((PublicCompany_1880) comp).getAllCertsAvail())) continue;
            if (companyBoughtThisTurn != null) {
                if (comp != companyBoughtThisTurn) continue;
                if (!stockSpace.isNoBuyLimit()) continue;
            }

            /* Check what share multiples are available
             * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
             */
            uniqueCerts = new PublicCertificateI[5];
            numberOfCerts = new int[5];
            for (PublicCertificateI cert2 : certs) {
                shares = cert2.getShares();
                if (maxNumberOfSharesToBuy < shares) continue;
                numberOfCerts[shares]++;
                if (uniqueCerts[shares] != null) continue;
                uniqueCerts[shares] = cert2;
            }

            /* Create a BuyCertificate action per share size */
            for (shares = 1; shares < 5; shares++) {
                /* Only certs in the brown zone may be bought all at once */
                number = numberOfCerts[shares];
                if (number == 0) continue;

                if (!stockSpace.isNoBuyLimit()) {
                    number = 1;
                    /* Would the player exceed the per-company share hold limit? */
                    if (!checkAgainstHoldLimit(currentPlayer, comp, number)) continue;

                    /* Would the player exceed the total certificate limit? */
                    if (!stockSpace.isNoCertLimit()
                            && !mayPlayerBuyCertificate(currentPlayer, comp,
                                    number * uniqueCerts[shares].getCertificateCount()))
                        continue;
                }

                // Does the player have enough cash?
                while (number > 0 && playerCash < number * price * shares) {
                    number--;
                }

                if (number > 0) {
                    possibleActions.add(new BuyCertificate(comp,
                            uniqueCerts[shares].getShare(),
                            from, price,
                            number));
                }
            }
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
                certs =
                    company.getPortfolio().getCertificatesPerCompany(
                            company.getName());
                if (certs == null || certs.isEmpty()) continue;
                cert = certs.get(0);
                if (isSaleRecorded(currentPlayer, company)) continue;
                if (!checkAgainstHoldLimit(currentPlayer, company, 1)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, company,
                        certs.get(0).getShare()) < 1) continue;
                stockSpace = company.getCurrentSpace();
                if (!stockSpace.isNoCertLimit()
                        && !mayPlayerBuyCertificate(currentPlayer, company, 1)) continue;
                if (company.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(company, cert.getShare(),
                            company.getPortfolio(), company.getMarketPrice()));
                }
            }
        }
     }

    /* (non-Javadoc)
     * @see rails.game.StockRound#finishRound()
     */
    @Override
    protected void finishRound() {
        ReportBuffer.add(" ");
        ReportBuffer.add(LocalText.getText("END_SR",
                String.valueOf(getStockRoundNumber())));

        if (raiseIfSoldOut) {
            /* Check if any companies are sold out. */
            for (PublicCompanyI company : gameManager.getCompaniesInRunningOrder()) {
                if (company.hasStarted() && (company instanceof PublicCompany_1880) && !((PublicCompany_1880) company).certsAvailableForSale() && (!((PublicCompany_1880) company).isCommunistPhase())) {
                    StockSpaceI oldSpace = company.getCurrentSpace();
                    stockMarket.soldOut(company);
                    StockSpaceI newSpace = company.getCurrentSpace();
                    if (newSpace != oldSpace) {
                        ReportBuffer.add(LocalText.getText("SoldOut",
                                company.getName(),
                                Bank.format(oldSpace.getPrice()),
                                oldSpace.getName(),
                                Bank.format(newSpace.getPrice()),
                                newSpace.getName()));
                    } else {
                        ReportBuffer.add(LocalText.getText("SoldOutNoRaise",
                                company.getName(),
                                Bank.format(newSpace.getPrice()),
                                newSpace.getName()));
                    }
                }
            }
        }
        
        for (PublicCompany_1880 c : PublicCompany_1880.getPublicCompanies(companyManager)) {
            if (c.hasStarted() && !c.hasFloated()) {
                checkFlotation(c);
            }
        }
        
        /** At the end of each Stockround the current amount of negative cash is subject to a fine of 50 percent
         * 
         */
        for (Player p : playerManager.getPlayers()) {
            if (p.getCash() <0 ) {
                int fine = p.getCash()/2;
                p.addCash(-fine);
            }
        }
        
        // Report financials
        ReportBuffer.add("");
        for (PublicCompanyI c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(LocalText.getText("Has", c.getName(),
                        Bank.format(c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(LocalText.getText("Has", p.getName(),
                    Bank.format(p.getCash())));
        }
        // Inform GameManager
        gameManager.nextRound(this);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#sellShares(rails.game.action.SellShares)
     */
    @Override
    public boolean sellShares(SellShares action) {
        // TODO Auto-generated method stub
        if(super.sellShares(action)) {
            int numberSold=action.getNumber();
            gameManager.getCurrentPlayer().addCash(-5*numberSold); //Deduct the Money for selling those Shares !
            return true;
        }
        else
        {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#useSpecialProperty(rails.game.action.UseSpecialProperty)
     */
    @Override
    public boolean useSpecialProperty(UseSpecialProperty action) {
        SpecialPropertyI sp = action.getSpecialProperty();

        // TODO This should work for all subclasses, but not all have execute()
        // yet.
        if (sp instanceof ExchangeForCash_1880) {

            boolean result = executeExchangeForCash((ExchangeForCash_1880) sp);
            if (result) hasActed.set(true);
            return result;

        } else {
            return super.useSpecialProperty(action);
        }
     
    }

    private boolean executeExchangeForCash(ExchangeForCash_1880 sp) {
        CompanyI privateCompany = sp.getOriginalCompany();
        Portfolio portfolio = privateCompany.getPortfolio();
        
        Player player = null;
        String errMsg = null;
        
        while (true) {

            /* Check if the private is owned by a player */
            if (!(portfolio.getOwner() instanceof Player)) {
                errMsg =
                    LocalText.getText("PrivateIsNotOwnedByAPlayer",
                            privateCompany.getName());
                break;
            }
            player = (Player) portfolio.getOwner();
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText(
                    "CannotSwapPrivateForCash",
                    player.getName(),
                    privateCompany.getName(),
                    errMsg ));
            return false;
        }
        
        moveStack.start(true);
        int amount = sp.getPhaseAmount();
        if (amount >0 ) {
        player.addCash(amount);
        sp.setExercised();
        privateCompany.setClosed();
        return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#process(rails.game.action.PossibleAction)
     */
    @Override
    public boolean process(PossibleAction action) {
    boolean result;
    String playerName = action.getPlayerName();
    
        if (action instanceof StartCompany) {
            StartCompany_1880 startCompanyAction = (StartCompany_1880) action;
            result = startCompany(playerName, startCompanyAction);
            return result;
        } else {
        return super.process(action);
        }
    }
   
    /*
    * @see rails.game.Round#checkFlotation(rails.game.PublicCompanyI)
    */
   @Override
   protected void checkFlotation(PublicCompanyI company) {
       if (!company.hasStarted() || company.hasFloated()) return;
        if (getOwnedPercentageByDirector(company) >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    
    }

    /** Determine sold percentage for floating purposes */
    protected int getOwnedPercentageByDirector (PublicCompanyI company) {

        int soldPercentage = 0;
        Player director = company.getPresident();
        for (PublicCertificateI cert : company.getCertificates()) {
            if (certCountsAsSold(cert, director)) {
                soldPercentage += cert.getShare();
            }
        }
        return soldPercentage;
    }

    private boolean certCountsAsSold(PublicCertificateI cert, Player director) {
        Portfolio holder = cert.getPortfolio();
        CashHolder owner = holder.getOwner();
        return owner.equals(director);
    }
    
    public boolean startCompany(String playerName, StartCompany_1880 action) {
        super.startCompany(playerName, action);

        Player player = gameManager.getPlayerManager().getPlayerByName(playerName);
        PublicCompany_1880 company = (PublicCompany_1880) action.getCompany();
        company.setBuildingRights(action.getBuildingRights());
        ((GameManager_1880) gameManager).getParSlotManager().setCompanyAtSlot(company, action.getParSlotIndex());
        
        // If this player's investor doesn't have a linked company yet - this is it
        Investor_1880 investor = Investor_1880.getInvestorForPlayer(gameManager.getCompanyManager(), player);
        if ((investor != null) && (investor.getLinkedCompany() == null)) {
            PublicCertificateI bcrCertificate = ipo.findCertificate(company, 1, false);
            bcrCertificate.moveTo(investor.getPortfolio());
            investor.setLinkedCompany(company);            
        }
        return true;
    }
    
    
    protected void executeShareTransfer (PublicCompanyI company,
            List<PublicCertificateI> certsToSell,
            Player dumpedPlayer, int presSharesToSell, int swapShareSize) {

        Portfolio portfolio = currentPlayer.getPortfolio();

        // Check if the presidency has changed
        if (dumpedPlayer != null && presSharesToSell > 0) {
            ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                    dumpedPlayer.getName(),
                    company.getName() ));
            // First swap the certificates
            Portfolio dumpedPortfolio = dumpedPlayer.getPortfolio();
            List<PublicCertificateI> swapped =
                portfolio.swapPresidentCertificate(company, dumpedPortfolio, swapShareSize);
            for (int i = 0; i < presSharesToSell; i++) {
                certsToSell.add(swapped.get(i));
            }
        }

        transferCertificates (certsToSell, ipo);

        // Check if we still have the presidency
        if (currentPlayer == company.getPresident()) {
            Player otherPlayer;
            int currentIndex = getCurrentPlayerIndex();
            for (int i = currentIndex + 1; i < currentIndex + numberOfPlayers; i++) {
                otherPlayer = gameManager.getPlayerByIndex(i);
                if (otherPlayer.getPortfolio().getShare(company) > portfolio.getShare(company)) {
                    portfolio.swapPresidentCertificate(company,
                            otherPlayer.getPortfolio(), swapShareSize);
                    ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                            otherPlayer.getName(),
                            company.getName() ));
                    break;
                }
            }
        }
    } 
}
