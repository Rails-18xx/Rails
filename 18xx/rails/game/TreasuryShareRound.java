/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/TreasuryShareRound.java,v 1.16 2010/01/14 20:57:23 evos Exp $
 *
 * Created on 21-May-2006
 * Change Log:
 */
package rails.game;

import java.util.*;

import rails.common.GuiDef;
import rails.game.action.*;
import rails.game.state.BooleanState;
import rails.util.LocalText;

/**
 * @author Erik Vos
 */
public class TreasuryShareRound extends StockRound {

    Player sellingPlayer;
    PublicCompanyI operatingCompany;
    private final BooleanState hasBought;
    private final BooleanState hasSold;

	/**
	 * Constructor with the GameManager, will call super class (StockRound's) Constructor to initialize, and
	 * and other parameters used by the Treasury Share Round Class
	 *
	 * @param aGameManager The GameManager Object needed to initialize the StockRound Class
	 * @param operatingCompany The PublicCompanyI Object that is selling shares
	 *
	 */
    public TreasuryShareRound(GameManagerI aGameManager,
                             RoundI parentRound) {
		super (aGameManager);

        operatingCompany = ((OperatingRound)parentRound).getOperatingCompany();
        sellingPlayer = operatingCompany.getPresident();
        log.debug("Creating TreasuryShareRound");
        hasBought =
                new BooleanState(operatingCompany.getName() + "_boughtShares",
                        false);
        hasSold =
                new BooleanState(operatingCompany.getName() + "_soldShares",
                        false);

        setCurrentPlayerIndex(sellingPlayer.getIndex());

        guiHints.setActivePanel(GuiDef.Panel.STATUS);

    }

    @Override
    public void start() {
        log.info("Treasury share trading round started");
        currentPlayer = sellingPlayer;
    }

    @Override
    public boolean mayCurrentPlayerSellAnything() {
        return false;
    }

    @Override
    public boolean mayCurrentPlayerBuyAnything() {
        return false;
    }

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();

        if (!operatingCompany.hasOperated()) return true;

        if (!hasSold.booleanValue()) setBuyableCerts();
        if (!hasBought.booleanValue()) setSellableCerts();

        if (possibleActions.isEmpty()) {
            // TODO Finish the round before it started...
        }

        possibleActions.add(new NullAction(NullAction.DONE));

        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(operatingCompany.getName() + " may: " + pa.toString());
        }

        return true;
    }

    /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     *
     * @return List of buyable certificates.
     */
    @Override
    public void setBuyableCerts() {
        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        Portfolio from;
        int price;
        int number;

        int cash = operatingCompany.getCash();

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        Map<String, List<PublicCertificateI>> map =
                from.getCertsPerCompanyMap();

        for (String compName : map.keySet()) {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty()) continue;

            cert = certs.get(0);
            comp = cert.getCompany();

            // TODO For now, only consider own certificates.
            // This will have to be revisited with 1841.
            if (comp != operatingCompany) continue;

            // Shares already owned
            int ownedShare =
                    operatingCompany.getPortfolio().getShare(operatingCompany);
            // Max share that may be owned
            int maxShare = gameManager.getTreasuryShareLimit();
            // Max number of shares to add
            int maxBuyable =
                    (maxShare - ownedShare) / operatingCompany.getShareUnit();
            // Max number of shares to buy
            number = Math.min(certs.size(), maxBuyable);
            if (number == 0) continue;

            price = comp.getMarketPrice();

            // Does the company have enough cash?
            while (number > 0 && cash < number * price)
                number--;

            if (number > 0) {
                possibleActions.add(new BuyCertificate(cert, from, price,
                        number));
            }
        }

    }

    /**
     * Create a list of certificates that the company may sell, taking all rules
     * taken into account. <br>Note: old code that provides for ownership of
     * presidencies of other companies has been retained, but not tested. This
     * code will be needed for 1841.
     *
     * @return List of sellable certificates.
     */
    public void setSellableCerts() {
        String compName;
        int price;
        int number;
        int maxShareToSell;

        Portfolio companyPortfolio = operatingCompany.getPortfolio();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {

            // Can't sell shares that have no price
            if (!company.hasStarted()) continue;

            maxShareToSell = companyPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                    Math.min(maxShareToSell, bank.getPoolShareLimit()
                                             - pool.getShare(company));
            if (maxShareToSell == 0) continue;

             /*
             * Check what share units the player actually owns. In some games
             * (e.g. 1835) companies may have different ordinary shares: 5% and
             * 10%, or 10% and 20%. The president's share counts as a multiple
             * of the lowest ordinary share unit type.
             */
            // Take care for max. 4 share units per share
            int[] shareCountPerUnit = new int[5];
            compName = company.getName();
            for (PublicCertificateI c : companyPortfolio.getCertificatesPerCompany(compName)) {
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

                if (number > 0) {
                    possibleActions.add(new SellShares(compName, i, number,
                            price));
                }
            }
        }
    }

    /**
     * Buying one or more single or double-share certificates (more is sometimes
     * possible)
     *
     * @param player The player that wants to buy shares.
     * @param action The executed action
     * @return True if the certificates could be bought. False indicates an
     * error.
     */
    @Override
    public boolean buyShares(String playerName, BuyCertificate action) {

        PublicCertificateI cert = action.getCertificate();
        Portfolio from = cert.getPortfolio();
        String companyName = cert.getCompany().getName();
        int number = action.getNumberBought();
        int shares = number * cert.getShares();
        int shareUnit = cert.getShare();

        String errMsg = null;
        int price = 0;
        PublicCompanyI company = null;
        Portfolio portfolio = null;

        currentPlayer = getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            // Only the player that has the turn may act
            if (!playerName.equals(currentPlayer.getName())) {
                errMsg = LocalText.getText("WrongPlayer", playerName);
                break;
            }

            // Check company
            company = companyManager.getPublicCompany(companyName);
            if (company == null) {
                errMsg = LocalText.getText("CompanyDoesNotExist", companyName);
                break;
            }
            if (company != operatingCompany) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                operatingCompany.getName() );

            }

            // The company must have floated
            if (!company.hasFloated()) {
                errMsg = LocalText.getText("NotYetFloated", companyName);
                break;
            }
            if (company.mustHaveOperatedToTradeShares()
                && !company.hasOperated()) {
                errMsg = LocalText.getText("NotYetOperated", companyName);
                break;
            }

            // Company may not buy after sell
            if (hasSold.booleanValue()) {
                errMsg = LocalText.getText("MayNotBuyAndSell", companyName);
                break;
            }

            // Check if that many shares are available
            if (shares > from.getShare(company)) {
                errMsg =
                        LocalText.getText("NotAvailable",
                                companyName,
                                from.getName() );
                break;
            }

            portfolio = operatingCompany.getPortfolio();

            // Check if company would exceed the per-company share limit
            if (portfolio.getShare(company) + shares * company.getShareUnit() > gameManager.getTreasuryShareLimit()) {
                errMsg =
                        LocalText.getText("TreasuryOverHoldLimit",
                                String.valueOf(gameManager.getTreasuryShareLimit()));
                break;
            }

            price = company.getMarketPrice();

            // Check if the Player has the money.
            if (operatingCompany.getCash() < shares * price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantBuy",
                    companyName,
                    shares,
                    companyName,
                    from.getName(),
                    errMsg ));
            return false;
        }

        // All seems OK, now buy the shares.
        if (number == 1) {
            ReportBuffer.add(LocalText.getText("BUY_SHARE_LOG",
                    companyName,
                    shareUnit,
                    companyName,
                    from.getName(),
                    Bank.format(shares * price) ));
        } else {
            ReportBuffer.add(LocalText.getText("BUY_SHARES_LOG",
                    companyName,
                    number,
                    shareUnit,
                    number * shareUnit,
                    companyName,
                    from.getName(),
                    Bank.format(shares * price) ));
        }

        moveStack.start(true);
        PublicCertificateI cert2;
        for (int i = 0; i < number; i++) {
            cert2 = from.findCertificate(company, cert.getShares(), false);
            executeTradeCertificate(cert2, portfolio, shares * price);
        }

        hasBought.set(true);

        return true;
    }

    @Override
    public boolean sellShares(SellShares action) {
        Portfolio portfolio = operatingCompany.getPortfolio();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompanyI company = companyManager.getPublicCompany(companyName);
        PublicCertificateI cert = null;
        List<PublicCertificateI> certsToSell =
                new ArrayList<PublicCertificateI>();
        int numberToSell = action.getNumberSold();
        int shareUnits = action.getShareUnits();

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
            if (company != operatingCompany) {
                errMsg =
                        LocalText.getText("WrongCompany",
                                companyName,
                                operatingCompany.getName() );
                break;
            }

            // The company must have floated
            if (!company.hasFloated()) {
                errMsg = LocalText.getText("NotYetFloated", companyName);
                break;
            }
            if (company.mustHaveOperatedToTradeShares()
                && !company.hasOperated()) {
                errMsg = LocalText.getText("NotYetOperated", companyName);
                break;
            }

            // Company may not sell after buying
            if (hasSold.booleanValue()) {
                errMsg = LocalText.getText("MayNotBuyAndSell", companyName);
                break;
            }

            // The company must have the share(s)
            if (portfolio.getShare(company) < numberToSell) {
                errMsg = LocalText.getText("NoShareOwned");
                break;
            }

            // The pool may not get over its limit.
            if (pool.getShare(company) + numberToSell * company.getShareUnit() > bank.getPoolShareLimit()) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificateI> it =
                    portfolio.getCertificatesPerCompany(companyName).iterator();
            while (numberToSell > 0 && it.hasNext()) {
                cert = it.next();
                if (shareUnits != cert.getShares()) {
                    // Wrong number of share units
                    continue;
                }
                // OK, we will sell this one
                certsToSell.add(cert);
                numberToSell--;
            }

            // Check if we could sell them all
            if (numberToSell > 0) {
                errMsg = LocalText.getText("NotEnoughShares");
                break;
            }

            break;
        }

        int numberSold = action.getNumberSold();
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantSell",
                    companyName,
                    numberSold,
                    companyName,
                    errMsg ));
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

        moveStack.start(true);

        ReportBuffer.add(LocalText.getText("SELL_SHARES_LOG",
                companyName,
                numberSold,
                company.getShareUnit(),
                (numberSold * company.getShareUnit()),
                companyName,
                Bank.format(numberSold * price) ));

        // Transfer the sold certificates
        for (PublicCertificateI cert2 : certsToSell) {
            if (cert2 != null) {
                 executeTradeCertificate (cert2, pool, cert2.getShares() * price);
            }
        }
        stockMarket.sell(company, numberSold);

        hasSold.set(true);

        return true;
    }

    /**
     * The current Player passes or is done.
     *
     * @param player Name of the passing player.
     * @return False if an error is found.
     */
    @Override
    public boolean done(String playerName) {

        currentPlayer = getCurrentPlayer();

        if (!playerName.equals(currentPlayer.getName())) {
            DisplayBuffer.add(LocalText.getText("WrongPlayer", playerName));
            return false;
        }

        moveStack.start(false);

        // Inform GameManager
        gameManager.finishTreasuryShareRound();

        return true;
    }

    public PublicCompanyI getOperatingCompany() {
        return this.operatingCompany;
    }

    @Override
    public String toString() {
        return "TreasuryShareRound";
    }

}
