/*
  This class implements the 1835 rules for making new companies
  being available in the IPO after buying shares of another company.
 */
package net.sf.rails.game.specific._1835;

import java.util.*;

import com.google.common.collect.Sets;

import net.sf.rails.game.financial.*;
import net.sf.rails.game.model.CertificatesModel;
import net.sf.rails.game.state.Owner;
import rails.game.action.AdjustSharePrice;
import rails.game.action.BuyCertificate;
import rails.game.action.NullAction;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.Portfolio;
import rails.game.action.PossibleAction;


public class StockRound_1835 extends StockRound {

    /**
     * Constructed via Configure
     */
    public StockRound_1835 (GameManager parent, String id) {
        super(parent, id);
    }

    /** Add nationalisations */
    // change: nationalization is a specific BuyCertificate activity
    // requires: add a new activity
    @Override
    public void setBuyableCerts() {

        super.setBuyableCerts();
        if (companyBoughtThisTurnWrapper.value() != null) return;

        int price;
        int cash = currentPlayer.getCash();
        Set<PublicCertificate> certs;
        StockSpace stockSpace;
        PortfolioModel from;
        int unitsForPrice;

        // Nationalisation
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (!company.getType().getId().equalsIgnoreCase("Major")) continue;
            if (!company.hasFloated()) continue;
            if (company.getPresident() != currentPlayer) continue;
            if (currentPlayer.getPortfolioModel().getShare(company) < 55) continue;
            if (currentPlayer.hasSoldThisRound(company)) continue;
            
            for (Player otherPlayer : getRoot().getPlayerManager().getPlayers()) {
                if (otherPlayer == currentPlayer) continue;

                /* Get the unique player certificates and check which ones can be bought */
                from = otherPlayer.getPortfolioModel();
                certs = from.getCertificates(company);
                if (certs == null || certs.isEmpty()) continue;

                /* Allow for multiple share unit certificates (e.g. 1835) */
                PublicCertificate[] uniqueCerts;
                int shares;

                stockSpace = company.getCurrentSpace();
                unitsForPrice = company.getShareUnitsForSharePrice();
                price = (int)(1.5 * stockSpace.getPrice() / unitsForPrice);

                /* Check what share multiples are available
                 * Normally only 1, but 1 and 2 in 1835. Allow up to 4.
                 */
                uniqueCerts = new PublicCertificate[5];
                for (PublicCertificate cert2 : certs) {
                    shares = cert2.getShares();
                    if (uniqueCerts[shares] != null) continue;
                    uniqueCerts[shares] = cert2;
                }

                /* Create a BuyCertificate action per share size */
                for (shares = 1; shares < 5; shares++) {
                    if (uniqueCerts[shares] == null) continue;

                    /* Would the player exceed the total certificate limit? */
                    if (!stockSpace.isNoCertLimit()
                            && !mayPlayerBuyCertificate(currentPlayer, company,
                                    uniqueCerts[shares].getCertificateCount()))
                        continue;

                    // Does the player have enough cash?
                    if (cash < price * shares) continue;

                    possibleActions.add(new BuyCertificate(company,
                            uniqueCerts[shares].getShare(),
                            from.getParent(), price, 1));
                }
            }
        }
    }

    @Override
    // change: there is no holding limit in 1835
    // requires: should be parameterized?
    public boolean checkAgainstHoldLimit(Player player, PublicCompany company, int number) {
        return true;
    }

    @Override
    // change: price differs for nationalization action
    // requires: move into new activity
    protected int getBuyPrice (BuyCertificate action, StockSpace currentSpace) {
        int price = currentSpace.getPrice();
        if (action.getFromPortfolio().getParent() instanceof Player) {
            price *= 1.5;
        }
        return price;
    }

    /** Share price goes down 1 space for any number of shares sold.
     */
    // change: specific share price adjustment
    // requires: do a parameterization
    @Override
    protected void adjustSharePrice (PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {
        // No more changes if it has already dropped
        if (soldBefore) {
            lastSoldCompany = company;
        } else {
            super.adjustSharePrice (company, seller,1, soldBefore);
        }
    }

    @Override
    public boolean done(NullAction action, String playerName,
            boolean hasAutopassed) {
        if (hasActed.value()) {
            if (companyBoughtThisTurnWrapper.value() == null) {
                hasActed.set(false);
            }
        }
         return super.done(action, playerName, hasAutopassed);
    }

  /*  (non-Javadoc)
     * @see net.sf.rails.game.StockRound#mayPlayerSellShareOfCompany(net.sf.rails.game.PublicCompany)
    */ 
    @Override
    public boolean mayPlayerSellShareOfCompany(PublicCompany company) {
        if (!super.mayPlayerSellShareOfCompany(company) ) 
            { 
            return false;
            }
        else {
          /*
           * Player is President and ia allowed to sell his director share if there is enough space in the pool in 1835
           * But if he has sold a share in this round he is allowed to sell dump the presidency...
           *
           * */
            if (company.getPresident() == currentPlayer) { 
                if (PlayerShareUtils.poolAllowsShares(company) >1) return true;
                }
        }
        return true;
    }

    protected void setGameSpecificActions() {
        /* If in one turn multiple sales of the same company occur,
         * this is normally done at the same price.
         * In 1835 the rules state otherwise, a special action
         * enables following that rule strictly.
         */
        if (lastSoldCompany != null) {
            possibleActions.add(new AdjustSharePrice(lastSoldCompany, EnumSet.of(AdjustSharePrice.Direction.DOWN)));
        }
    }

    protected boolean processGameSpecificAction(PossibleAction action) {
        if (action instanceof AdjustSharePrice) {
            super.adjustSharePrice ((AdjustSharePrice)action);
            return true;
        } else {
            return false;
        }
    }


    /*
    @Override
    protected boolean checkIfSplitSaleOfPresidentAllowed() {
        // in 1835 its not allowed to Split the President Certificate on sale
        return false;
    }*/

	@Override
	protected void setPriority(String string) {
		if (string.matches("BuyCert|StartCompany")) {
			super.setPriority(string);
		}
	}

    @Override
    protected boolean executeShareTransfer(PublicCompany company, List<PublicCertificate> certsToSell, Player dumpedPlayer, int presSharesToSell) {

        boolean swapped = false;
        BankPortfolio bankTo = (BankPortfolio) pool.getParent();

        if (dumpedPlayer != null && presSharesToSell > 0) {
            executePresidentTransferAfterDump(company, new TreeSet<>(certsToSell), dumpedPlayer, presSharesToSell, company.getPresident(), bankTo);

            ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                    dumpedPlayer.getId(),
                    company.getId()));
            swapped = true;

        }

        // Transfer the sold certificates
        Portfolio.moveAll(certsToSell, bankTo);
        return swapped;
    }

    private void executePresidentTransferAfterDump(PublicCompany company, Set<PublicCertificate> certsToSell, Player newPresident, int presSharesToSell, Player oldPresident, BankPortfolio bankTo) {
        PublicCertificate presidentCert = company.getPresidentsShare();

        SortedSet<PublicCertificate.Combination> newPresidentsReplacementForPresidentShare = CertificatesModel.certificateCombinations(  newPresident.getPortfolioModel().getCertificates(company), presidentCert.getShares());

        // FIXME: This should be based on a selection of the old president, however it chooses the combination with least certificates, which is favorable in most cases
        PublicCertificate.Combination swapToOldPresident = newPresidentsReplacementForPresidentShare.first();

        Portfolio.moveAll(swapToOldPresident, oldPresident);
        presidentCert.moveTo(newPresident);

        Set<PublicCertificate> oldPresidentsCertsWithoutCertsToSell = Sets.difference(oldPresident.getPortfolioModel().getCertificates(company), certsToSell);
        SortedSet<PublicCertificate.Combination> sellableCertificateCombinations = CertificatesModel.certificateCombinations(
                oldPresidentsCertsWithoutCertsToSell,
                presSharesToSell);

        Portfolio.moveAll(sellableCertificateCombinations.last(), bankTo);
    }
}
