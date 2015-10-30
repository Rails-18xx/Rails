package net.sf.rails.game.financial;

import java.util.*;

import rails.game.action.*;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.GameDef.Parm;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Portfolio;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;


// Change: TreasuryShareRound is a workaround as StockRound
// It is a single Activity to allow companies buying or selling shares
public class TreasuryShareRound extends StockRound {

    protected Player sellingPlayer;
    protected PublicCompany operatingCompany;
    private final BooleanState hasBought = BooleanState.create(this, "hasBought") ;
    private final BooleanState hasSold = BooleanState.create(this, "hasSold");

    /**
     * Created via Configure
     */
    public TreasuryShareRound(GameManager parent, String id) {
        super(parent, id);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);
    }

    // TODO: Check if this still works, as the initialization was moved back to here
    public void start(RoundFacade parentRound) {
        log.info("Treasury share trading round started");
        operatingCompany = ((OperatingRound)parentRound).getOperatingCompany();
        sellingPlayer = operatingCompany.getPresident();
        getRoot().getPlayerManager().setCurrentPlayer(sellingPlayer);
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

        if (operatingCompany.mustHaveOperatedToTradeShares()
                && !operatingCompany.hasOperated()) return true;

        if (!hasSold.value()) setBuyableCerts();
        if (!hasBought.value()) setSellableCerts();

        if (possibleActions.isEmpty()) {
            // TODO Finish the round before it started...
        }

        possibleActions.add(new NullAction(NullAction.Mode.DONE));

        for (PossibleAction pa : possibleActions.getList()) {
            log.debug(operatingCompany.getId() + " may: " + pa.toString());
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
        ImmutableSet<PublicCertificate> certs;
        PublicCertificate cert;
        PortfolioModel from;
        int price;
        int number;

        int cash = operatingCompany.getCash();

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
            from.getCertsPerCompanyMap();

        for (PublicCompany comp: map.keySet()) {
            certs = map.get(comp);
            // if (certs.isEmpty()) continue; // TODO: Check if removal is correct 

            cert = Iterables.get(certs, 0);

            // TODO For now, only consider own certificates.
            // This will have to be revisited with 1841.
            if (comp != operatingCompany) continue;

            // Shares already owned
            int ownedShare =
                    operatingCompany.getPortfolioModel().getShare(operatingCompany);
            // Max share that may be owned
            int maxShare = GameDef.getGameParameterAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT);
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
                possibleActions.add(new BuyCertificate(comp, cert.getShare(), from.getParent(), price,
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
        int price;
        int number;
        int maxShareToSell;

        PortfolioModel companyPortfolio = operatingCompany.getPortfolioModel();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {

            // Can't sell shares that have no price
            if (!company.hasStarted()) continue;

            maxShareToSell = companyPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                Math.min(maxShareToSell,
                        GameDef.getGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)
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
            for (PublicCertificate c : companyPortfolio.getCertificates(company)) {
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
            if (sellPrices.containsKey(company)) {
                price = (sellPrices.get(company)).getPrice();
            } else {
                price = company.getMarketPrice();
            }

            for (int shareSize = 1; shareSize <= 4; shareSize++) {
                number = shareCountPerUnit[shareSize];
                if (number == 0) continue;
                number =
                    Math.min(number, maxShareToSell
                            / (shareSize * company.getShareUnit()));
                if (number == 0) continue;

                for (int i=1; i<=number; i++) {
                    possibleActions.add(new SellShares(company, shareSize, i, price));
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

        PublicCompany company = action.getCompany();
        PortfolioModel from = action.getFromPortfolio();
        String companyName = company.getId();
        int number = action.getNumberBought();
        int shareUnit = company.getShareUnit();
        int sharePerCert = action.getSharePerCertificate();
        int share = number * sharePerCert;
        int shares = share/shareUnit;

        String errMsg = null;
        int price = 0;
        // TODO: Might not be needed anymore, replaced by company
        PortfolioModel portfolio = null;
        
        currentPlayer = playerManager.getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            // Only the player that has the turn may act
            if (!playerName.equals(currentPlayer.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, currentPlayer.getId());
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
                                operatingCompany.getId() );

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
            if (hasSold.value()) {
                errMsg = LocalText.getText("MayNotBuyAndSell", companyName);
                break;
            }

            // Check if that many shares are available
            if (share > from.getShare(company)) {
                errMsg =
                    LocalText.getText("NotAvailable",
                            companyName,
                                from.getId() );
                break;
            }

            portfolio = operatingCompany.getPortfolioModel();

            // Check if company would exceed the per-company share limit
            int treasuryShareLimit = GameDef.getGameParameterAsInt(this, GameDef.Parm.TREASURY_SHARE_LIMIT);
            if (portfolio.getShare(company) + share > treasuryShareLimit) {
                errMsg =
                    LocalText.getText("TreasuryOverHoldLimit",
                            String.valueOf(treasuryShareLimit));
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
            DisplayBuffer.add(this, LocalText.getText("CantBuy",
                    companyName,
                    shares,
                    companyName,
                    from.getId(),
                    errMsg ));
            return false;
        }

        // All seems OK, now buy the shares.
        

        int cashAmount = shares * price;
        String cashText = Currency.toBank(company, cashAmount);
        if (number == 1) {
            ReportBuffer.add(this, LocalText.getText("BUY_SHARE_LOG",
                    companyName,
                    shareUnit,
                    companyName,
                    from.getName(),
                    cashText ));
        } else {
            ReportBuffer.add(this, LocalText.getText("BUY_SHARES_LOG",
                    companyName,
                    number,
                    shareUnit,
                    number * shareUnit,
                    companyName,
                    from.getName(),
                    cashText ));
        }

        PublicCertificate cert2;
        for (int i = 0; i < number; i++) {
            cert2 = from.findCertificate(company, sharePerCert/shareUnit, false);
            cert2.moveTo(company);
        }

        hasBought.set(true);

        return true;
    }

    @Override
    public boolean sellShares(SellShares action) {
        PortfolioModel portfolio = operatingCompany.getPortfolioModel();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompany company = companyManager.getPublicCompany(companyName);
        PublicCertificate cert = null;
        List<PublicCertificate> certsToSell =
                new ArrayList<PublicCertificate>();
        int numberToSell = action.getNumber();
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
                                operatingCompany.getId() );
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
            if (hasBought.value()) {
                errMsg = LocalText.getText("MayNotBuyAndSell", companyName);
                break;
            }

            // The company must have the share(s)
            if (portfolio.getShare(company) < numberToSell) {
                errMsg = LocalText.getText("NoShareOwned");
                break;
            }

            // The pool may not get over its limit.
            if (pool.getShare(company) + numberToSell * company.getShareUnit()
                    > GameDef.getGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificate> it =
                    portfolio.getCertificates(company).iterator();
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

        int numberSold = action.getNumber();
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantSell",
                    companyName,
                    numberSold,
                    companyName,
                    errMsg ));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpace sellPrice;
        int price;

        // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(company)) {
            price = (sellPrices.get(company)).getPrice();
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(company, sellPrice);
        }

        

        int cashAmount = numberSold * price;
        String cashText = Currency.fromBank(cashAmount, company);
        ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                companyName,
                numberSold,
                company.getShareUnit(),
                (numberSold * company.getShareUnit()),
                companyName,
                cashText ));

        // Transfer the sold certificates
        Portfolio.moveAll(certsToSell, pool.getParent());
        /*
        for (PublicCertificate cert2 : certsToSell) {
            if (cert2 != null) {
                 transferCertificate (cert2, pool, cert2.getShares() * price);
            }
        }
         */
        stockMarket.sell(company, numberSold);

        hasSold.set(true);

        return true;
    }

    /**
     * The current Player passes or is done.
     * @param player Name of the passing player.
     *
     * @return False if an error is found.
     */
    @Override
    // Autopassing does not apply here
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        currentPlayer = playerManager.getCurrentPlayer();

        if (!playerName.equals(currentPlayer.getId())) {
            DisplayBuffer.add(this, LocalText.getText("WrongPlayer", playerName, currentPlayer.getId()));
            return false;
        }

        

        // Inform GameManager
        gameManager.finishTreasuryShareRound();

        return true;
    }

    public PublicCompany getOperatingCompany() {
        return this.operatingCompany;
    }

    @Override
    public String toString() {
        return "TreasuryShareRound";
    }

}
