/**
 * This class implements the 1880 rules for making new companies
 * being available in the IPO after buying shares of another company.
 */
package net.sf.rails.game.specific._1880;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCertificate;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.StockRound;
import net.sf.rails.game.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Owner;
import java.util.Arrays;
import java.util.List;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;

import rails.game.action.BuyCertificate;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.action.StartCompany;
import rails.game.specific._1880.StartCompany_1880;


public class StockRound_1880 extends StockRound {

    /**
     * Constructor with the GameManager, will call super class (StockRound's)
     * Constructor to initialize
     * 
     * @param aGameManager The GameManager Object needed to initialize the Stock
     * Round
     * 
     */
    public StockRound_1880 (GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    // The sell-in-same-turn-at-decreasing-price option does not apply here
    protected int getCurrentSellPrice(PublicCompany company) {
        return (super.getCurrentSellPrice(company) - 5);
    }

    /**
     * Share price goes down 1 space for any number of shares sold.
     */
    @Override
    protected void adjustSharePrice(PublicCompany company, int numberSold,
            boolean soldBefore) {
        if (((PublicCompany_1880) company).canStockPriceMove() == true) {      
            super.adjustSharePrice(company, numberSold, soldBefore);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * rails.game.StockRound#mayPlayerSellShareOfCompany(rails.game.PublicCompany
     * )
     */
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompany company) {
        if ((company.getPresident() == playerManager.getCurrentPlayer()) && 
                (company instanceof PublicCompany_1880) &&
                (((PublicCompany_1880) company).canPresidentSellShare() == false)) {
            return false;
        }
        return super.mayPlayerSellShareOfCompany(company);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#gameSpecificChecks(rails.game.Portfolio, rails.game.PublicCompany)
     */
    @Override
      protected void gameSpecificChecks(PortfolioModel boughtFrom,
            PublicCompany company) {
        
        ((PublicCompany_1880) company).sharePurchased();        
        super.gameSpecificChecks(boughtFrom, company);
    }
    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1880. 
     */
    @Override
    protected void floatCompany(PublicCompany company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
        
        
        // For all Companies who float before the first 6T is purchased the Full Capitalisation will happen on purchase of 
        // 50 percent of the shares.
        // The exception of the rule of course are the late starting companies after the first 6 has been bought when the 
        // flotation will happen after 60 percent have been bought.
        if (((PublicCompany_1880) company).getFloatPercentage() == 60) {
            cash = 10 * price;
        } else {
            cash = 5 * price;
        }
       
        company.setFloated(); 

        if (cash > 0) {
            Currency.wire(bank, cash, company);
            ReportBuffer.add(this,LocalText.getText("FloatsWithCash",
                    company.getLongName(),
                    Bank.format(this,cash) ));
        } else {
            ReportBuffer.add(this,LocalText.getText("Floats",
                    company.getLongName()));
        }

    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#setBuyableCerts()
     */
    @Override
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        ImmutableSet<PublicCertificate> certs;
        PublicCertificate cert;
        StockSpace stockSpace;
        PortfolioModel from;
        int price;
        int number;
        int unitsForPrice;

        int playerCash = currentPlayer.getCash();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompany companyBoughtThisTurn =
            (PublicCompany) companyBoughtThisTurnWrapper.value();
        if (companyBoughtThisTurn == null) {
            from = ipo;
            ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                    from.getCertsPerCompanyMap();


            for (PublicCompany comp : map.keySet()) {
                certs = map.get(comp);
                if (certs == null || certs.isEmpty()) continue;

                /* Only the top certificate is buyable from the IPO */
                int lowestIndex = 99;
                cert = null;
                int index;
                for (PublicCertificate c : certs) {
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

                if (!cert.isPresidentShare()) {
                    price = comp.getMarketPrice() / unitsForPrice; // Always use the market price
                    if (price <= playerCash) {
                        possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                                from.getParent(), price));
                    }
                } else if (!comp.hasStarted()) {
                    ParSlotManager_1880 parSlotManager = ((GameManager_1880) gameManager).getParSlotManager();
                    Integer[] prices = parSlotManager.getAvailablePrices(playerCash/2);
                    if (prices.length > 0) {
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
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                from.getCertsPerCompanyMap();
        /* Allow for multiple share unit certificates (e.g. 1835) */
        PublicCertificate[] uniqueCerts;
        int[] numberOfCerts;
        int shares;
        int shareUnit;
        int maxNumberOfSharesToBuy;

        for (PublicCompany comp : map.keySet()) {
            certs = map.get(comp);
            if (certs == null || certs.isEmpty()) continue;

           
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
            uniqueCerts = new PublicCertificate[5];
            numberOfCerts = new int[5];
            for (PublicCertificate cert2 : certs) {
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
                            from.getParent(), price,
                            number));
                }
            }
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompany company : companyManager.getAllPublicCompanies()) {
             // TODO: Has to be rewritten (director)
                certs =
                    company.getPortfolioModel().getCertificates(
                            company);
                if (certs == null || certs.isEmpty()) continue;
                cert = Iterables.get(certs, 0);
                if (isSaleRecorded(currentPlayer, company)) continue;
                if (!checkAgainstHoldLimit(currentPlayer, company, 1)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, company,
                        cert.getShare()) < 1) continue;
                stockSpace = company.getCurrentSpace();
                if (!stockSpace.isNoCertLimit()
                        && !mayPlayerBuyCertificate(currentPlayer, company, 1)) continue;
                if (company.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(company, cert.getShare(),
                            company, company.getMarketPrice()));
                }
            }
        }
     }

    /* (non-Javadoc)
     * @see rails.game.StockRound#finishRound()
     */
    @Override
    protected void finishRound() {
        ReportBuffer.add(this," ");
        ReportBuffer.add(this, LocalText.getText("END_SR",
                String.valueOf(getStockRoundNumber())));

        if (raiseIfSoldOut) {
            /* Check if any companies are sold out. */
            for (PublicCompany company : gameManager.getCompaniesInRunningOrder()) {
                if (company.hasStarted() && (company instanceof PublicCompany_1880) && !((PublicCompany_1880) company).certsAvailableForSale() && (((PublicCompany_1880) company).canStockPriceMove())) {
                    StockSpace oldSpace = company.getCurrentSpace();
                    stockMarket.soldOut(company);
                    StockSpace newSpace = company.getCurrentSpace();
                    if (newSpace != oldSpace) {
                        ReportBuffer.add(this,LocalText.getText("SoldOut",
                                company.getLongName(),
                                Bank.format(this, oldSpace.getPrice()),
                                oldSpace.getId(),
                                Bank.format(this, newSpace.getPrice()),
                                newSpace.getId()));
                    } else {
                        ReportBuffer.add(this,LocalText.getText("SoldOutNoRaise",
                                company.getLongName(),
                                Bank.format(this, newSpace.getPrice()),
                                newSpace.getId()));
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
                int fine = Math.abs(p.getCash() / 2);
                ReportBuffer.add(this, LocalText.getText("DebtPenaltyStockRound", p.getId(),
                       Bank.format(this,fine)));
                Currency.wire(p,fine,bank);
            }
        }
        
        // Report financials
        ReportBuffer.add(this, "");
        for (PublicCompany c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(this, LocalText.getText("Has", c.getLongName(),
                        Bank.format(this, c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(this, LocalText.getText("Has", p.getId(),
                    Bank.format(this, p.getCash())));
        }
        // Inform GameManager
        gameManager.nextRound(this);
    }

    /* (non-Javadoc)
     * @see rails.game.StockRound#sellShares(rails.game.action.SellShares)
     */
    @Override
    public boolean sellShares(SellShares action) {
        if(super.sellShares(action)) {
            int numberSold=action.getNumber();
            int sellingfee = 5*numberSold;
            Currency.wire(currentPlayer, sellingfee, bank); //Deduct the Money for selling those Shares !
            return true;
        }
        else
        {
            return false;
        }
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
    * @see rails.game.Round#checkFlotation(rails.game.PublicCompany)
    */
   @Override
   protected void checkFlotation(PublicCompany company) {
       if (!company.hasStarted() || company.hasFloated()) return;
        if (getOwnedPercentageByDirector(company) >= company.getFloatPercentage()) {
            // Company floats
            floatCompany(company);
        }
    
    }

    /** Determine sold percentage for floating purposes */
    protected int getOwnedPercentageByDirector (PublicCompany company) {

        int soldPercentage = 0;
        Player director = company.getPresident();
        for (PublicCertificate cert : company.getCertificates()) {
            if (certCountsAsSold(cert, director)) {
                soldPercentage += cert.getShare();
            }
        }
        return soldPercentage;
    }

    private boolean certCountsAsSold(PublicCertificate cert, Player director) {
        Owner holder = cert.getOwner();
        return holder.equals(director);
    }
    
    public boolean startCompany(String playerName, StartCompany_1880 action) {
        int numShares = action.getNumberBought();
        PublicCompany startCompany = action.getCompany();
        
        if (numShares > 2) {
            PublicCertificate presCert = ipo.findCertificate(startCompany, true);
            presCert.setShares(numShares);
            for (int i = 0; i < (numShares - 2); i++) {
                PublicCertificate scrapCert = ipo.findCertificate(startCompany, false);
                scrapCert.setShares(0);
                scrapCert.moveTo(scrapHeap);
            }            
        }
                
        super.startCompany(playerName, action);

        Player player = playerManager.getPlayerByName(playerName);
        PublicCompany_1880 company = (PublicCompany_1880) action.getCompany();
        company.setBuildingRights(action.getBuildingRights());
        ((GameManager_1880) gameManager).getParSlotManager().setCompanyAtSlot(company, action.getParSlotIndex());

        // If this player's investor doesn't have a linked company yet - this is it
        Investor_1880 investor = Investor_1880.getInvestorForPlayer(companyManager, player);
        if ((investor != null) && (investor.getLinkedCompany() == null)) {
            PublicCertificate bcrCertificate = ipo.findCertificate(company, 1, false);
            bcrCertificate.moveTo(investor.getPortfolioModel());
            investor.setLinkedCompany(company);            
        }

        return true;
    }
    
    // In 1880 all share transfers via ipo
    @Override
    protected void executeShareTransfer( PublicCompany company,
            List<PublicCertificate> certsToSell, 
            Player dumpedPlayer, int presSharesToSell, int swapShareSize) {
        
        executeShareTransferTo(company, certsToSell, dumpedPlayer, presSharesToSell, swapShareSize, ipo );
    }
    
}

 