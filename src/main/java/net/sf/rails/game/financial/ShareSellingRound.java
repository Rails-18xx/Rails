package net.sf.rails.game.financial;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rails.game.action.PossibleAction;
import rails.game.action.SellShares;
import net.sf.rails.common.*;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.GameDef.Parm;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;


// TODO: Check if un-initialized states cause undo problems
public class ShareSellingRound extends StockRound {

    protected RoundFacade parentRound;
    protected Player sellingPlayer;
    protected IntegerState cashToRaise; // intialized later
    protected PublicCompany cashNeedingCompany;
    protected boolean dumpOtherCompaniesAllowed;

    protected List<SellShares> sellableShares;

    /**
     * Created using Configure
     */
    // change: ShareSellingRound is not really a (full) Round, only a single player acting
    // requires: make an independent Round for EnforcedSelling that uses the selling shares activity
    public ShareSellingRound(GameManager parent, String id) {
        super(parent, id);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);
    }

    public void start(RoundFacade parentRound, Player sellingPlayer, int cashToRaise,
            PublicCompany cashNeedingCompany, boolean dumpOtherCompaniesAllowed) {
        log.info("Share selling round started, player="
                +sellingPlayer.getId()+" cash="+cashToRaise);
        ReportBuffer.add(this, LocalText.getText("PlayerMustSellShares",
                sellingPlayer.getId(),
                Bank.format(this, cashToRaise)));
        this.parentRound = parentRound;
        currentPlayer = this.sellingPlayer = sellingPlayer;
        this.cashNeedingCompany = cashNeedingCompany;
        this.cashToRaise = IntegerState.create(this, "CashToRaise", cashToRaise);
        
        this.dumpOtherCompaniesAllowed = dumpOtherCompaniesAllowed;
        log.debug("Forced selling, dumpOtherCompaniesAllowed = " + dumpOtherCompaniesAllowed);
        getRoot().getPlayerManager().setCurrentPlayer(sellingPlayer);
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
            log.debug(currentPlayer.getId() + " may: " + pa.toString());
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
     * 
     * FIXME: Rails 2.x Adopt the new code from StockRound
     */
    protected List<SellShares> getSellableShares () {

        sellableShares = new ArrayList<SellShares> ();

        int price;
        int number;
        int share, maxShareToSell;
        PortfolioModel playerPortfolio = currentPlayer.getPortfolioModel();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) continue;

            share = maxShareToSell = playerPortfolio.getShare(company);
            if (maxShareToSell == 0) continue;

            /* May not sell more than the Pool can accept */
            maxShareToSell =
                Math.min(maxShareToSell,
                        GameDef.getGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)
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
                    for (Player player : getRoot().getPlayerManager().getPlayers()) {
                        if (player == currentPlayer) continue;
                        maxOtherShares = Math.max(maxOtherShares, player.getPortfolioModel().getShare(company));
                    }
                    // limit shares to sell to difference between president and second largest ownership
                    maxShareToSell = Math.min(maxShareToSell, share - maxOtherShares);
                    dumpPossible = false; // and no dump is possible by definition
                } else {
                    // case B: potential sale of president certificate possible
                    if (share - maxShareToSell < presidentShare) {
                        // dump necessary
                        dumpPossible = false;
                        for (Player player : getRoot().getPlayerManager().getPlayers()) {
                            if (player == currentPlayer) continue;
                            // there is a player with holding exceeding the president share
                            if (player.getPortfolioModel().getShare(company) >= presidentShare) {
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
            for (PublicCertificate c : playerPortfolio.getCertificates(company)) {
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

                // May not sell more than is needed to buy the train
                while (number > 0
                       && ((number - 1) * price) > cashToRaise.value())
                    number--;

                if (number > 0) {
                    for (int i=1; i<=number; i++) {
                        sellableShares.add(new SellShares(company, shareSize, i, price));
                    }
                }
            }
        }
        return sellableShares;
    }

    @Override
    public boolean sellShares(SellShares action) {
        PortfolioModel portfolio = currentPlayer.getPortfolioModel();
        String playerName = currentPlayer.getId();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompany company =
            companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificate cert = null;
        PublicCertificate presCert = null;
        List<PublicCertificate> certsToSell =
                new ArrayList<PublicCertificate>();
        Player dumpedPlayer = null;
        int presSharesToSell = 0;
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
                    > GameDef.getGameParameterAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            // Find the certificates to sell
            Iterator<PublicCertificate> it =
                    portfolio.getCertificates(company).iterator();
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
                Player otherPlayer, previousPlayer;
                previousPlayer = getRoot().getPlayerManager().getCurrentPlayer();
                for (int i = 0; i <= numberOfPlayers; i++) {
                    otherPlayer = getRoot().getPlayerManager().getNextPlayerAfter(previousPlayer);
                    if (otherPlayer.getPortfolioModel().getShare(company) >= presCert.getShare()) {
                        // Check if he has the right kind of share
                        if (numberToSell > 1
                            || otherPlayer.getPortfolioModel().ownsCertificates(
                                        company, 1, false) >= 1) {
                            // The poor sod.
                            dumpedPlayer = otherPlayer;
                            presSharesToSell = numberToSell;
                            numberToSell = 0;
                            break;
                        }
                    }
                    previousPlayer = otherPlayer;
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

        int numberSold = action.getNumber();
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantSell",
                    playerName,
                    numberSold,
                    companyName,
                    errMsg ));
            return false;
        }

        // All seems OK, now do the selling.
        StockSpace sellPrice;
        int price;

        // Get the sell price (does not change within a turn)
        if (sellPrices.containsKey(company)
                && GameOption.getAsBoolean(this, "SeparateSalesAtSamePrice")) {
            price = (sellPrices.get(company).getPrice());
        } else {
            sellPrice = company.getCurrentSpace();
            price = sellPrice.getPrice();
            sellPrices.put(company, sellPrice);
        }
        int cashAmount = numberSold * price * shareUnits;

        
        // FIXME: changeStack.linkToPreviousMoveSet();

        String cashText = Currency.fromBank(cashAmount, currentPlayer);
        ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                playerName,
                numberSold,
                company.getShareUnit(),
                numberSold * company.getShareUnit(),
                companyName,
                cashText ));

        boolean soldBefore = sellPrices.containsKey(company);

        adjustSharePrice (company, numberSold, soldBefore);

        if (!company.isClosed()) {

            executeShareTransfer (company, certsToSell,
                    dumpedPlayer, presSharesToSell);
        }

        cashToRaise.add(-numberSold * price);

        if (cashToRaise.value() <= 0) {
            gameManager.finishShareSellingRound();
        } else if (getSellableShares().isEmpty()) {
            DisplayBuffer.add(this, LocalText.getText("YouMustRaiseCashButCannot",
                    Bank.format(this, cashToRaise.value())));
            currentPlayer.setBankrupt();
            gameManager.registerBankruptcy();
        }

        return true;
    }

    public int getRemainingCashToRaise() {
        return cashToRaise.value();
    }

    public PublicCompany getCompanyNeedingCash() {
        return cashNeedingCompany;
    }

    @Override
    public String toString() {
        return "ShareSellingRound";
    }

}
