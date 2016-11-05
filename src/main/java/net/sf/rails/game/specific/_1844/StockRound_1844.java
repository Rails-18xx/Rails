package net.sf.rails.game.specific._1844;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Currency;
import rails.game.action.BuyCertificate;
import rails.game.action.StartCompany;

public class StockRound_1844 extends StockRound {

    public StockRound_1844(GameManager parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    /**
     * Float a company, including a default implementation of moving cash and
     * shares as a result of flotation. <p>Fifty Percent capitalisation is implemented
     * as in 1844. 
     */
    @Override
    protected void floatCompany(PublicCompany company) {
        // Move cash and shares where required
        int cash = 0;
        int price = company.getIPOPrice();
        
        
        // For all Companies who float before the Phase 6 is reached the Full Capitalisation will happen reching their destination hex. 
        // 
        // The exception of the rule of course are the late starting companies after the first 6 has been bought when the 
        // flotation will happen after 50 percent have been bought.
        int phaseIndex=gameManager.getCurrentPhase().getIndex();
        if (phaseIndex > 5) {
            cash = 10 * price;
        } else if (company.getType().getId().equals("VOR-SBB")){
            cash = 2* price;
        } else {
            cash = 5 * price;
        }
       
        company.setFloated(); 
        //If someone floats a company with 40% share and investor the usual routines fails, this will be handled below
        if (((PublicCompany_1844) company).checkToFullyCapitalize()) {
            cash = 5* price;
        }

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
    
    
    /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     *
     * @return List of buyable certificates.
     */
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        ImmutableSet<PublicCertificate> certs;
        PublicCertificate cert;
        StockSpace stockSpace;
        PortfolioModel from;
        int price;
        int number;
        int unitsForPrice;

        int playerCash = currentPlayer.getCashValue();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompany companyBoughtThisTurn =
            (PublicCompany) companyBoughtThisTurnWrapper.value();
        if (companyBoughtThisTurn == null) {
            from = ipo;
            ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                from.getCertsPerCompanyMap();
            int shares;

            for (PublicCompany comp : map.keySet()) {
                certs = map.get(comp);
                // if (certs.isEmpty()) continue; // TODO: is this removal correct?

                /* Only the top certificate is buyable from the IPO */
                // TODO: This is code that should be deprecated
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

                unitsForPrice = comp.getShareUnitsForSharePrice();
                if (currentPlayer.hasSoldThisRound(comp)) continue;
                // In 1844 the player is allowed to buy 50% of shares from the IPO only.
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        cert.getShare()) < 1 ) continue;

                /* Would the player exceed the total certificate limit? */
                stockSpace = comp.getCurrentSpace();
                if ((stockSpace == null || !stockSpace.isNoCertLimit()) && !mayPlayerBuyCertificate(
                        currentPlayer, comp, cert.getCertificateCount())) continue;

                shares = cert.getShares();

                if (!cert.isPresidentShare()) {
                    price = comp.getIPOPrice() / unitsForPrice;
                    if (price <= playerCash) {
                        possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                                from.getParent(), price));
                    }
                } else if (!comp.hasStarted()) {
                    if (comp.getIPOPrice() != 0) {
                        price = comp.getIPOPrice() * cert.getShares() / unitsForPrice;
                        if (price <= playerCash) {
                            possibleActions.add(new StartCompany(comp, price));
                        }
                    } else {
                        List<Integer> startPrices = new ArrayList<Integer>();
                        for (int startPrice : stockMarket.getStartPrices()) {
                            if (startPrice * shares <= playerCash) {
                                startPrices.add(startPrice);
                            }
                        }
                        if (startPrices.size() > 0) {
                            int[] prices = new int[startPrices.size()];
                            Arrays.sort(prices);
                            for (int i = 0; i < prices.length; i++) {
                                prices[i] = startPrices.get(i);
                            }
                            possibleActions.add(new StartCompany(comp, prices));
                        }
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
        //int maxNumberOfSharesToBuy;

        for (PublicCompany comp : map.keySet()) {
            certs = map.get(comp);
            // if (certs.isEmpty()) continue; // TODO: Is this removal correct?

            stockSpace = comp.getCurrentSpace();
            unitsForPrice = comp.getShareUnitsForSharePrice();
            price = stockSpace.getPrice() / unitsForPrice;
            if (currentPlayer.hasSoldThisRound(comp)) continue;
            if (companyBoughtThisTurn != null) {
                // If a cert was bought before, only brown zone ones can be
                // bought again in the same turn
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
    }
    
    /**
     * Return the number of <i>additional</i> shares of a certain company and
     * of a certain size that a player may buy, given the share "hold limit" per
     * company, that is the percentage of shares of one company that a player
     * may hold (here 50% for buying from IPO). 
     *
     * @param company The company from which to buy
     * @param number The share unit (typically 10%).
     * @return The maximum number of such shares that would not let the player
     * overrun the per-company share hold limit.
     */
    public int maxAllowedNumberOfSharesToBuy(Player player,
            PublicCompany company,
            int shareSize) {

        int limit =50;
        int maxAllowed = (limit - player.getPortfolioModel().getShare(company)) / shareSize;
        //        log.debug("MaxAllowedNumberOfSharesToBuy = " + maxAllowed + " for company =  " + company + " shareSize " + shareSize);
        return maxAllowed;
    }



}
