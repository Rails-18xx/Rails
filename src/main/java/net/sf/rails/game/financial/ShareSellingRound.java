package net.sf.rails.game.financial;

import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleAction;
import rails.game.action.SellShares;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;


// TODO: Check if un-initialized states cause undo problems
public class ShareSellingRound extends StockRound {
    private static final Logger log = LoggerFactory.getLogger(ShareSellingRound.class);

    protected RoundFacade parentRound;
    protected Player sellingPlayer;
    protected IntegerState cashToRaise; // intialized later
    protected PublicCompany cashNeedingCompany;
    protected boolean dumpOtherCompaniesAllowed;

    protected List<SellShares> sellableShares;
    //protected int sellableSharesValue; // unused, may be useful later

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
        log.info("Share selling round started, player={} cash={}",
                sellingPlayer.getId(), cashToRaise);
        ReportBuffer.add(this, LocalText.getText("PlayerMustSellShares",
                sellingPlayer.getId(),
                Bank.format(this, cashToRaise)));
        this.parentRound = parentRound;
        currentPlayer = this.sellingPlayer = sellingPlayer;
        this.cashNeedingCompany = cashNeedingCompany;
        this.cashToRaise = IntegerState.create(this, "CashToRaise", cashToRaise);

        this.dumpOtherCompaniesAllowed = dumpOtherCompaniesAllowed;
        log.debug("Forced selling, dumpOtherCompaniesAllowed={}", dumpOtherCompaniesAllowed);
        getRoot().getPlayerManager().setCurrentPlayer(sellingPlayer);
        if (getSellableShares().isEmpty()) {
            declareBankruptcy();
        }
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
            log.debug("{} may: {}", currentPlayer.getId(), pa.toString());
        }

        return true;
    }

    @Override
    public void setSellableShares() {
        possibleActions.addAll(sellableShares);
    }

    protected List<SellShares> getSellableShares() {

        findSellableShares();
        return sellableShares;

    }

    protected void findSellableShares() {

        sellableShares = new ArrayList<>();
        //sellableSharesValue = 0;

        PortfolioModel playerPortfolio = currentPlayer.getPortfolioModel();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) continue;

            int ownedShares = playerPortfolio.getShareNumber(company);
            if (ownedShares == 0) {
                continue;
            }

            /* May not sell more than the Pool can accept */
            int poolAllowsShares = PlayerShareUtils.poolAllowsShareNumbers(company);
            log.debug("company={}", company);
            log.debug("poolAllowShares={}", poolAllowsShares);
            int maxSharesToSell = Math.min(ownedShares, poolAllowsShares);

            // if no share can be sold
            if (maxSharesToSell == 0) {
                continue;
            }
            // May not sell more than is needed to buy the train
            int price = getCurrentSellPrice(company);
            while (maxSharesToSell > 0
                    && ((maxSharesToSell - 1) * (price/company.getShareUnitsForSharePrice()))
                        > cashToRaise.value()) {
                maxSharesToSell--;
            }

            // For debugging 18Scan only
            //maxSharesToSell = 5;

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
            int dumpThreshold = 0;
            SortedSet<Integer> possibleSharesToSell = null;
            boolean dumpIsPossible = false;
            if (company.getPresident() == currentPlayer) {

                log.debug("Forced selling check: company={}, ownedShare={}, maxShareToSell={}",
                        company, ownedShares, maxSharesToSell);
                int presidentShareSize = company.getPresidentsShare().getShares();
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    // case A: selling of president not allowed (either company triggered share selling or no dump of others)
                     Player potential = company.findPlayerToDump();
                     if (potential != null) {
                         // limit shares to sell to difference between president and second largest ownership
                         maxSharesToSell = ownedShares - potential.getPortfolioModel().getShareNumber(company);
                         possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                         log.debug("dumpThreshold={}", dumpThreshold);
                         log.debug("possibleSharesToSell={}", possibleSharesToSell);
                     }
                } else {
                    // case B: potential sale of president certificate possible
                    if (ownedShares - maxSharesToSell < presidentShareSize) {
                        dumpThreshold = ownedShares - presidentShareSize + 1;
                        // Is dumping possible?
                         if (company.findPlayerToDump() != null) {
                             possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                             dumpIsPossible = true;
                         } else {
                             // No dumpee found
                             maxSharesToSell = Math.min (maxSharesToSell,
                                     ownedShares - presidentShareSize);
                         }
                    } else {
                        // no dump necessary
                        possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                        dumpIsPossible = false;
                    }
                }
            }

            /* Allow for different share units (as in 1835) */
            SortedMultiset<Integer> certCount = playerPortfolio.getCertificateTypeCounts(company);
            log.debug ("certCount={}", certCount);
            // Make sure that single shares are always considered (due to possible dumping)
            SortedSet<Integer> certSizeElements = Sets.newTreeSet(certCount.elementSet());
            certSizeElements.add(1);
            log.debug ("CertSizeElements={}", certSizeElements);

            for (int shareSize : certSizeElements) {
                int number = certCount.count(shareSize);

                // If you can dump a presidency, you add the shareNumbers of the presidency
                // to the single shares to be sold
                if (dumpIsPossible && shareSize == 1
                        && number + company.getPresidentsShare().getShares() >= dumpThreshold) {
                    number += company.getPresidentsShare().getShares();
                    // but limit this to the pool
                    number = Math.min(number, poolAllowsShares);
                    log.debug("Dump is possible increased single shares to {}", number);
                }

                if (number == 0) {
                    continue;
                }

                // Check against the maximum share that can be sold
                number = Math.min(number, maxSharesToSell / shareSize);

                if (number <= 0) {
                    continue;
                }
                for (int i = 1; i <= number; i++) {
                    if (checkIfSplitSaleOfPresidentAllowed()) {
                        // check if selling would dump the company
                        if (dumpIsPossible && i * shareSize >= dumpThreshold) {
                            // dumping requires that the total is in the possibleSharesToSell list and that shareSize == 1
                            // multiple shares have to be sold separately
                            if (shareSize == 1 && possibleSharesToSell.contains(i * shareSize)) {
                                sellableShares.add(new SellShares(company, shareSize, i, price, 1));
                            }
                        } else {
                            // ... no dumping: standard sell
                            sellableShares.add(new SellShares(company, shareSize, i, price, 0));
                        }
                    } else if (dumpIsPossible && i * shareSize >= dumpThreshold) {
                        if (certCount.isEmpty() && number == 2) { // 1835 director share only
                            //ToDO : Adjust Logic for other Games with MultipleShareDirectors where splitting the share is not allowed
                            sellableShares.add(new SellShares(company, 2, 1, price, 1));
                            //sellableSharesValue += price;
                        } else if ((i == 1) && ((!certCount.isEmpty()) && (number == 2))) { //1835 director share once and an action for the single share in the directors hand if we have the room :)
                            sellableShares.add(new SellShares(company, 2, 1, price, 1));
                            sellableShares.add(new SellShares(company, shareSize, i, price, 1));
                        } else if (((!certCount.isEmpty()) && (number == 1)) || number > 2) {
                            sellableShares.add(new SellShares(company, shareSize, i, price, 1));
                        }
                    } else {
                        sellableShares.add(new SellShares(company, shareSize, i, price, 0));
                    }
                }
            }
        }
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
        PublicCertificate presCert;
        List<PublicCertificate> certsToSell =  new ArrayList<>();
        Player dumpedPlayer = null;
        int numberToSell = action.getNumber();
        int shareUnits = action.getShareUnits();
        int presidentShareNumbersToSell = 0;

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
                    > GameDef.getParmAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }
            presCert = company.getPresidentsShare();

            // ... check if there is a dump required
            // Player is president => dump is possible
            if (currentPlayer == company.getPresident() && shareUnits == 1) {
                dumpedPlayer = company.findPlayerToDump();
                if (dumpedPlayer != null) {
                    presidentShareNumbersToSell = PlayerShareUtils.presidentShareNumberToSell(
                            company, currentPlayer, dumpedPlayer, numberToSell);
                    // reduce the numberToSell by the president (partial) sold certificate
                    numberToSell -= presidentShareNumbersToSell;
                    presCert = null;
                }
            } else {
                if (currentPlayer == company.getPresident() && shareUnits == 2) {
                    dumpedPlayer = company.findPlayerToDump();
                    if (dumpedPlayer != null) {
                        presidentShareNumbersToSell = PlayerShareUtils.presidentShareNumberToSell(
                                company, currentPlayer, dumpedPlayer, numberToSell + 1);
                        // reduce the numberToSell by the president (partial) sold certificate
                        numberToSell -= presidentShareNumbersToSell;
                        presCert = null;
                    }
                }
            }

            certsToSell = PlayerShareUtils.findCertificatesToSell(company, currentPlayer, numberToSell, shareUnits);

            // reduce numberToSell to double check
            for (PublicCertificate c : certsToSell) {
                numberToSell -= c.getShares();
            }

            if (numberToSell > 0 && presCert != null
                    && numberToSell <= presCert.getShares()) {
                // Not allowed to dump the company that needs the train
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    errMsg =
                            LocalText.getText("CannotDumpTrainBuyingPresidency");
                    break;
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
            }
            break;
        }

        int numberSold = action.getNumber();
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantSell",
                    playerName,
                    numberSold,
                    companyName,
                    errMsg));
            return false;
        }

        // All seems OK, now do the selling.

        // Selling price
        int price = getCurrentSellPrice(company);
        int cashAmount = numberSold * price * shareUnits;

        // Save original price as it may be reused in subsequent sale actions in the same turn
        boolean soldBefore = sellPrices.containsKey(company);
        if (!soldBefore) {
            sellPrices.put(company, company.getCurrentSpace());
        }

        String cashText = Currency.fromBank(cashAmount, currentPlayer);

        if (numberSold == 1) {
            ReportBuffer.add(this, LocalText.getText("SELL_SHARE_LOG",
                    playerName,
                    company.getShareUnit() * shareUnits,
                    companyName,
                    cashText));
        } else {
            ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                    playerName,
                    numberSold,
                    company.getShareUnit() * shareUnits,
                    numberSold * company.getShareUnit() * shareUnits,
                    companyName,
                    cashText));
        }

        adjustSharePrice(company, currentPlayer, numberSold, soldBefore);

        if (!company.isClosed()) {

            executeShareTransfer(company, certsToSell,
                    dumpedPlayer, presidentShareNumbersToSell);
        }

        cashToRaise.add(-cashAmount);

        if (cashToRaise.value() <= 0) {
            gameManager.finishShareSellingRound();
        } else if (getSellableShares().isEmpty()) {
            declareBankruptcy();
        }
        return true;
    }

    private void declareBankruptcy () {
        String message = LocalText.getText("YouMustRaiseCashButCannot",
                Bank.format(this, this.cashToRaise.value()));
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);
        if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY)) {
            // Currently not used, replaced by code in operatingRound.buyTrain().
            cashNeedingCompany.setBankrupt();
            gameManager.registerCompanyBankruptcy(cashNeedingCompany);
        } else {
            Currency.wireAll(currentPlayer, cashNeedingCompany);
            gameManager.registerPlayerBankruptcy(currentPlayer);
            currentPlayer.setBankrupt();
        }
    }

    public int getRemainingCashToRaise() {
        return cashToRaise.value();
    }

    public PublicCompany getCompanyNeedingCash() {
        return cashNeedingCompany;
    }

    @Override
    public String toString() {
        return getId();
    }

}
