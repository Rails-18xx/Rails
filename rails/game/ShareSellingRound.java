/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ShareSellingRound.java,v 1.19 2009/01/14 20:45:07 evos Exp $
 *
 * Created on 21-May-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import rails.game.move.MoveSet;
import rails.game.state.IntegerState;
import rails.util.LocalText;

/**
 * @author Erik Vos
 */
public class ShareSellingRound extends StockRound {

    OperatingRound or;
    Player sellingPlayer;
    PublicCompanyI companyNeedingCash;
    IntegerState cashToRaise;

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
		or = ((OperatingRound) parentRound);
        companyNeedingCash = or.getOperatingCompany();
        cashToRaise = new IntegerState("CashToRaise", or.getCashToBeRaisedByPresident());
        sellingPlayer = companyNeedingCash.getPresident();
        currentPlayer = sellingPlayer;
        setCurrentPlayerIndex(sellingPlayer.getIndex());

    }

    @Override
    public void start() {
        log.info("Share selling round started");
        ReportBuffer.add (LocalText.getText("PlayerMustSellShares",
                sellingPlayer.getName(),
                Bank.format(cashToRaise.intValue()),
                companyNeedingCash.getName()));
        currentPlayer = sellingPlayer;
        setPossibleActions();
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

        if (possibleActions.isEmpty() && cashToRaise.intValue() > 0) {
            DisplayBuffer.add(LocalText.getText("YouAreBankrupt",
                    Bank.format(cashToRaise.intValue())));

            gameManager.registerBankruptcy();
            return false;
        }

        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(currentPlayer.getName() + " may: " + pa.toString());
        }

        return true;
    }

    /**
     * Create a list of certificates that a player may sell in a Stock Round,
     * taking all rules taken into account.
     *
     * @return List of sellable certificates.
     */
    @Override
    public void setSellableShares() {
        String compName;
        int price;
        int number;
        int share, maxShareToSell;
        boolean dumpAllowed;
        Portfolio playerPortfolio = currentPlayer.getPortfolio();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {

            // Can't sell shares that have no price
            if (!company.hasStarted()) continue;

            share = maxShareToSell = playerPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                    Math.min(maxShareToSell, Bank.getPoolShareLimit()
                                             - pool.getShare(company));
            if (maxShareToSell == 0) continue;

            /*
             * If the current Player is president, check if he can dump the
             * presidency onto someone else
             */
            if (company.getPresident() == currentPlayer) {
                int presidentShare =
                        company.getCertificates().get(0).getShare();
                if (maxShareToSell > share - presidentShare) {
                    dumpAllowed = false;
                    if (company != companyNeedingCash) {
                        int playerShare;
                        List<Player> players = gameManager.getPlayers();
                        for (Player player : players) {
                            if (player == currentPlayer) continue;
                            playerShare =
                                    player.getPortfolio().getShare(company);
                            if (playerShare >= presidentShare) {
                                dumpAllowed = true;
                                break;
                            }
                        }
                    }
                    if (!dumpAllowed) maxShareToSell = share - presidentShare;
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
                    possibleActions.add(new SellShares(compName, i, number,
                            price));
                }
            }
        }
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

            // The player must have the share(s)
            if (portfolio.getShare(company) < numberToSell) {
                errMsg = LocalText.getText("NoShareOwned");
                break;
            }

            // The pool may not get over its limit.
            if (pool.getShare(company) + numberToSell * company.getShareUnit() > Bank.getPoolShareLimit()) {
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
                if (company == companyNeedingCash) {
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
                    new String[] { playerName, String.valueOf(numberSold),
                            companyName, errMsg }));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpaceI sellPrice;
        int price;

        // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(companyName)) {
            price = (sellPrices.get(companyName)).getPrice();
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(companyName, sellPrice);
        }

        MoveSet.start(true);

        ReportBuffer.add(LocalText.getText("SELL_SHARES_LOG",
                playerName,
                numberSold,
                company.getShareUnit(),
                numberSold * company.getShareUnit(),
                companyName,
                Bank.format(numberSold * price) ));

        // Check if the presidency has changed
        if (presCert != null && dumpedPlayer != null && presSharesToSell > 0) {
            ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF", new String[] {
                    dumpedPlayer.getName(), companyName}));
            // First swap the certificates
            Portfolio dumpedPortfolio = dumpedPlayer.getPortfolio();
            List<PublicCertificateI> swapped =
                    portfolio.swapPresidentCertificate(company, dumpedPortfolio);
            for (int i = 0; i < presSharesToSell; i++) {
                certsToSell.add(swapped.get(i));
            }
        }

        // Transfer the sold certificates
        for (PublicCertificateI cert2 : certsToSell) {
            if (cert2 != null) {
                executeTradeCertificate (cert2, pool, cert2.getShares() * price);
            }
        }
        stockMarket.sell(company, numberSold);

        // Check if we still have the presidency
        if (currentPlayer == company.getPresident()) {
            Player otherPlayer;
            for (int i = currentIndex + 1; i < currentIndex + numberOfPlayers; i++) {
                otherPlayer = gameManager.getPlayerByIndex(i);
                if (otherPlayer.getPortfolio().getShare(company) > portfolio.getShare(company)) {
                    portfolio.swapPresidentCertificate(company,
                            otherPlayer.getPortfolio());
                    ReportBuffer.add(LocalText.getText("IS_NOW_PRES_OF",
                            new String[] { otherPlayer.getName(),
                                company.getName()
                                     }));
                    break;
                }
            }
        }

        cashToRaise.add(-numberSold * price);

        if (cashToRaise.intValue() <= 0) {
            gameManager.finishShareSellingRound();
        }

        return true;
    }

    public int getRemainingCashToRaise() {
        return cashToRaise.intValue();
    }

    public PublicCompanyI getCompanyNeedingTrain() {
        return companyNeedingCash;
    }

    @Override
    public String toString() {
        return "ShareSellingRound";
    }

}
