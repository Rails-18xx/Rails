/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ShareSellingRound.java,v 1.33 2010/05/23 08:18:24 evos Exp $
 *
 * Created on 21-May-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import rails.common.GuiDef;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.state.IntegerState;
import rails.util.LocalText;

/**
 * @author Erik Vos
 */
public class ShareSellingRound extends StockRound {

    RoundI parentRound;
    Player sellingPlayer;
    IntegerState cashToRaise;
    PublicCompanyI cashNeedingCompany;
    boolean dumpOtherCompaniesAllowed;

    private List<SellShares> sellableShares;

    /**
     * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize, and
     * and other parameters used by the Share Selling Round Class
     *
     * @param aGameManager The GameManager Object needed to initialize the StockRound Class
     * @param compNeedingTraing The PublicCompanyI Object that needs to buy the train,
     *        who is limited on selling shares of
     * @param cashToRaise The amount of cash needed to be raised during the special sell-off
     *
     */
    public ShareSellingRound(GameManagerI gameManager,
            RoundI parentRound) {

        super (gameManager);
        this.parentRound = parentRound;

        guiHints.setActivePanel(GuiDef.Panel.STATUS);
    }

    public void start(Player sellingPlayer, int cashToRaise,
            PublicCompanyI cashNeedingCompany, boolean dumpOtherCompaniesAllowed) {
        log.info("Share selling round started, player="
                +sellingPlayer.getName()+" cash="+cashToRaise);
        ReportBuffer.add (LocalText.getText("PlayerMustSellShares",
                sellingPlayer.getName(),
                Bank.format(cashToRaise)));
        currentPlayer = this.sellingPlayer = sellingPlayer;
        this.cashNeedingCompany = cashNeedingCompany;
        this.cashToRaise = new IntegerState("CashToRaise", cashToRaise);
        this.dumpOtherCompaniesAllowed = dumpOtherCompaniesAllowed;
        log.debug("Forced selling, dumpOtherCompaniesAllowed = " + dumpOtherCompaniesAllowed);
        setCurrentPlayerIndex(sellingPlayer.getIndex());
        getSellableShares();
    }

    @Override
    public boolean mayCurrentPlayerSellAnything() {
        return true;
    }

    @Override
    public boolean mayCurrentPlayerBuyAnything() {
        return false;
    }

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();

        setSellableShares();

        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(currentPlayer.getName() + " may: " + pa.toString());
        }

        return true;
    }

    @Override
    public void setSellableShares() {
        possibleActions.addAll(sellableShares);
    }

    /**
     * Create a list of certificates that a player may sell in an emergency
     * share selling round, taking all rules taken into account.
     */
    private List<SellShares> getSellableShares () {

        sellableShares = new ArrayList<SellShares> ();

        String compName;
        int price;
        int number;
        int share, maxShareToSell;
        Portfolio playerPortfolio = currentPlayer.getPortfolio();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) continue;

            share = maxShareToSell = playerPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                    Math.min(maxShareToSell,
                            getGameParameterAsInt(GameDef.Parm.POOL_SHARE_LIMIT)
                                             - pool.getShare(company));
            if (maxShareToSell == 0) continue;

            /*
             * If the current Player is president, check if he can dump the
             * presidency onto someone else
             *
             * Two reasons for the check:
             * A) President not allowed to sell that company
             * Thus keep enough shares to stay president
             *
             * Example here
             * share = 60%, other player holds 40%, maxShareToSell > 30%
             * => requires selling of president

             * B) President allowed to sell that company
             * In that case the president share can be sold
             *
             * Example here
             * share = 60%, , president share = 20%, maxShareToSell > 40%
             * => requires selling of president
             */
            if (company.getPresident() == currentPlayer) {
                int presidentShare =
                    company.getCertificates().get(0).getShare();
                boolean dumpPossible;
                log.debug("Forced selling check: company = " + company +
                        ", share = " + share + ", maxShareToSell = " + maxShareToSell);
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    // case A: selling of president not allowed (either company triggered share selling or no dump of others)
                    int maxOtherShares = 0;
                    for (Player player : gameManager.getPlayers()) {
                        if (player == currentPlayer) continue;
                        maxOtherShares = Math.max(maxOtherShares, player.getPortfolio().getShare(company));
                    }
                    // limit shares to sell to difference between president and second largest ownership
                    maxShareToSell = Math.min(maxShareToSell, share - maxOtherShares);
                    dumpPossible = false; // and no dump is possible by definition
                } else {
                    // case B: potential sale of president certificate possible
                    if (share - maxShareToSell < presidentShare) {
                        // dump necessary
                        dumpPossible = false;
                        for (Player player : gameManager.getPlayers()) {
                            if (player == currentPlayer) continue;
                            // there is a player with holding exceeding the president share
                            if (player.getPortfolio().getShare(company) >= presidentShare) {
                                dumpPossible = true;
                                break;
                            }
                        }
                    } else {
                        dumpPossible = false; // no dump necessary
                    }
                }
                if (!dumpPossible) {
                    // keep presidentShare at minimum
                    maxShareToSell = Math.min(maxShareToSell, share - presidentShare);
                }
            }

            /*
             * Check what share units the player actually owns. In some games
             * (e.g. 1835) companies may have different ordinary shares: 5% and
             * 10%, or 10% and 20%. The president's share counts as a multiple
             * of the lowest ordinary share unit type.
             */
            // Take care for max. 4 share units per share
            int[] shareCountPerUnit = new int[5];
            compName = company.getName();
            for (PublicCertificateI c : playerPortfolio.getCertificatesPerCompany(compName)) {
                if (c.isPresidentShare()) {
                    shareCountPerUnit[1] += c.getShares();
                } else {
                    ++shareCountPerUnit[c.getShares()];
                }
            }
            // TODO The above ignores that a dumped player must be
            // able to exchange the president's share.

            /*
             * Check the price. If a cert was sold before this turn, the
             * original price is still valid
             */
            if (sellPrices.containsKey(compName)) {
                price = (sellPrices.get(compName)).getPrice();
            } else {
                price = company.getMarketPrice();
            }

            for (int i = 1; i <= 4; i++) {
                number = shareCountPerUnit[i];
                if (number == 0) continue;
                number =
                        Math.min(number, maxShareToSell
                                         / (i * company.getShareUnit()));
                if (number == 0) continue;

                // May not sell more than is needed to buy the train
                while (number > 0
                       && ((number - 1) * price) > cashToRaise.intValue())
                    number--;

                if (number > 0) {
                    sellableShares.add(new SellShares(compName, i, number,
                            price));
                }
            }
        }
        return sellableShares;
    }

    @Override
    public boolean sellShares(SellShares action) {
        Portfolio portfolio = currentPlayer.getPortfolio();
        String playerName = currentPlayer.getName();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompanyI company =
                companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificateI cert = null;
        PublicCertificateI presCert = null;
        List<PublicCertificateI> certsToSell =
                new ArrayList<PublicCertificateI>();
        Player dumpedPlayer = null;
        int presSharesToSell = 0;
        int numberToSell = action.getNumberSold();
        int shareUnits = action.getShareUnits();
        int currentIndex = getCurrentPlayerIndex();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            if (numberToSell <= 0) {
                errMsg = LocalText.getText("NoSellZero");
                break;
            }

            // Check company
            if (company == null) {
                errMsg = LocalText.getText("NoCompany");
                break;
            }

            // May player sell this company
            if (!mayPlayerSellShareOfCompany(company)) {
            	errMsg = LocalText.getText("SaleNotAllowed", companyName);
            	break;
            }

            // The player must have the share(s)
            if (portfolio.getShare(company) < numberToSell) {
                errMsg = LocalText.getText("NoShareOwned");
                break;
            }

            // The pool may not get over its limit.
            if (pool.getShare(company) + numberToSell * company.getShareUnit()
                    > getGameParameterAsInt(GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificateI> it =
                    portfolio.getCertificatesPerCompany(companyName).iterator();
            while (numberToSell > 0 && it.hasNext()) {
                cert = it.next();
                if (cert.isPresidentShare()) {
                    // Remember the president's certificate in case we need it
                    if (cert.isPresidentShare()) presCert = cert;
                    continue;
                } else if (shareUnits != cert.getShares()) {
                    // Wrong number of share units
                    continue;
                }
                // OK, we will sell this one
                certsToSell.add(cert);
                numberToSell--;
            }
            if (numberToSell == 0) presCert = null;

            if (numberToSell > 0 && presCert != null
                && numberToSell <= presCert.getShares()) {
                // Not allowed to dump the company that needs the train
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    errMsg =
                            LocalText.getText("CannotDumpTrainBuyingPresidency");
                    break;
                }
                // More to sell and we are President: see if we can dump it.
                Player otherPlayer;
                for (int i = currentIndex + 1; i < currentIndex
                                                   + numberOfPlayers; i++) {
                    otherPlayer = gameManager.getPlayerByIndex(i);
                    if (otherPlayer.getPortfolio().getShare(company) >= presCert.getShare()) {
                        // Check if he has the right kind of share
                        if (numberToSell > 1
                            || otherPlayer.getPortfolio().ownsCertificates(
                                    company, 1, false) >= 1) {
                            // The poor sod.
                            dumpedPlayer = otherPlayer;
                            presSharesToSell = numberToSell;
                            numberToSell = 0;
                            break;
                        }
                    }
                }
            }
            // Check if we could sell them all
            if (numberToSell > 0) {
                if (presCert != null) {
                    errMsg = LocalText.getText("NoDumping");
                } else {
                    errMsg = LocalText.getText("NotEnoughShares");
                }
                break;
            }

            break;
        }

        int numberSold = action.getNumberSold();
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantSell",
                    playerName,
                    numberSold,
                    companyName,
                    errMsg ));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpaceI sellPrice;
        int price;

        // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(companyName)
                && GameOption.convertValueToBoolean(getGameOption("SeparateSalesAtSamePrice"))) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(companyName, sellPrice);
        }
        int cashAmount = numberSold * price * shareUnits;

        moveStack.start(true).linkToPreviousMoveSet();

        ReportBuffer.add(LocalText.getText("SELL_SHARES_LOG",
                playerName,
                numberSold,
                company.getShareUnit(),
                numberSold * company.getShareUnit(),
                companyName,
                Bank.format(cashAmount) ));

        boolean soldBefore = sellPrices.containsKey(companyName);

        pay (bank, currentPlayer, cashAmount);
        adjustSharePrice (company, numberSold, soldBefore);

        if (!company.isClosed()) {

            executeShareTransfer (company, certsToSell,
                    dumpedPlayer, presSharesToSell);
        }

        cashToRaise.add(-numberSold * price);

        if (cashToRaise.intValue() <= 0) {
            gameManager.finishShareSellingRound();
        } else if (getSellableShares().isEmpty()) {
            DisplayBuffer.add(LocalText.getText("YouMustRaiseCashButCannot",
                    Bank.format(cashToRaise.intValue())));
            currentPlayer.setBankrupt();
            gameManager.registerBankruptcy();
        }

        return true;
    }

    public int getRemainingCashToRaise() {
        return cashToRaise.intValue();
    }

    public PublicCompanyI getCompanyNeedingCash() {
        return cashNeedingCompany;
    }

    @Override
    public String toString() {
        return "ShareSellingRound";
    }

}
