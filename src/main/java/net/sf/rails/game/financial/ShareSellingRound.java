package net.sf.rails.game.financial;

import com.google.common.collect.Sets;
import com.google.common.collect.SortedMultiset;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
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
    protected IntegerState cashToRaise; // initialized later
    protected PublicCompany cashNeedingCompany;
    protected boolean dumpOtherCompaniesAllowed;
    //protected boolean partialPresidencyDumpAllowed;

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
        String companyName = cashNeedingCompany.getId();
        log.info("Share selling round started, company={}, seller={} cashToRaise={}",
                companyName, sellingPlayer.getId(), cashToRaise);
        ReportBuffer.add(this, LocalText.getText("PlayerMustSellShares",
                sellingPlayer.getId(),
                Bank.format(this, cashToRaise)));
        this.parentRound = parentRound;
        currentPlayer = this.sellingPlayer = sellingPlayer;
        this.cashNeedingCompany = cashNeedingCompany;
        this.cashToRaise = IntegerState.create(this, "CashToRaise", cashToRaise);

        this.dumpOtherCompaniesAllowed = dumpOtherCompaniesAllowed;
        this.certificateSplitAllowed = checkIfCertificateSplitAllowed();
        log.debug("dumpOtherCompaniesAllowed={}", this.dumpOtherCompaniesAllowed);
        log.debug("certificateSplitAllowed={}", certificateSplitAllowed);
        getRoot().getPlayerManager().setCurrentPlayer(sellingPlayer);
        List<SellShares> sellable = getSellableShares();
        if (sellable == null || sellable.isEmpty()) {
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

        setSellShareActions();

        return true;
    }

    public void setSellShareActions() {
        findSellableShares (true, cashNeedingCompany,
                cashToRaise.value(), dumpOtherCompaniesAllowed);
        possibleActions.addAll(sellShareActions);
    }

    protected List<SellShares> getSellableShares() {
        findSellableShares(true, cashNeedingCompany,
                cashToRaise.value(), dumpOtherCompaniesAllowed);
        return sellShareActions;
    }

    /* Merged into StockRound
    protected void findSellableShares(boolean emergency) {

        log.info ("{} must raise {} to buy a train", cashNeedingCompany, Bank.format(this, cashToRaise.value()));
        sellShareActions = new ArrayList<>();
        //sellableSharesValue = 0;

        PortfolioModel playerPortfolio = currentPlayer.getPortfolioModel();

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         *//*
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) continue;

            int ownedShares = playerPortfolio.getShares(company);
            if (ownedShares == 0) {
                continue;
            }
            log.debug("------ company={} shares={}", company, ownedShares);

            /* May not sell more than the Pool can accept *//*
            int poolAllowsShares = PlayerShareUtils.poolAllowsShares(company);
            log.debug("poolAllowShares={}", poolAllowsShares);
            int maxSharesToSell = Math.min(ownedShares, poolAllowsShares);

            // if no share can be sold
            if (maxSharesToSell == 0) {
                continue;
            }
            // May not sell more than is needed to buy the train
            int price = getCurrentSellPrice(company); // Price per single share (PR: 5%)
            boolean reduced = false;
            while (maxSharesToSell > 0
                    && ((maxSharesToSell - 1) * price) > cashToRaise.value()) {
                maxSharesToSell--;
                reduced = true;
            }
            log.debug("maxSharesToSell is {}{} ({}, needed {})",
                    reduced ? "reduced to " : "",
                    maxSharesToSell,
                    Bank.format(this, price * maxSharesToSell),
                    Bank.format(this, cashToRaise.value()));

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
             *//*
            int dumpThreshold = 0;
            SortedSet<Integer> possibleSharesToSell = null;
            boolean dumpIsPossible = false;
            Player dumpedPlayer = company.findPlayerToDump();
            int dumpedPlayerShares = (dumpedPlayer != null
                    ? dumpedPlayer.getPortfolioModel().getShares(company)
                    : 0);
            int presidentShareSize = company.getPresidentsShare().getShares();
            if (company.getPresident() == currentPlayer) {

                log.debug("Forced selling check: company={}, ownedShares={}, maxSharesToSell={}",
                        company, ownedShares, maxSharesToSell);
                if (company == cashNeedingCompany || !dumpOtherCompaniesAllowed) {
                    // case A: selling of president not allowed (either company triggered share selling or no dump of others)
                     if (dumpedPlayer != null) {
                         // limit shares to sell to difference between president and second largest ownership
                         maxSharesToSell = Math.min(maxSharesToSell, ownedShares - dumpedPlayerShares);
                         possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                         log.debug("case A: {} presidency may not be dumped", company);
                         log.debug("maxSharesToSell={}", maxSharesToSell);
                         log.debug("possibleSharesToSell={}", possibleSharesToSell);
                     }
                } else if (maxSharesToSell > ownedShares -  presidentShareSize /* Direct dump *//*
                        || maxSharesToSell > ownedShares - dumpedPlayerShares /* Indirect dump *//*) {
                    // case B: potential sale of president certificate possible
                    dumpThreshold = ownedShares - Math.max(presidentShareSize, dumpedPlayerShares) + 1;
                    // Is dumping possible?
                    if (dumpedPlayer != null) {
                        possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                        dumpIsPossible = true;
                        log.debug("case B1: {} can be dumped to {}, sharesToSell={}, dumpThreshold={}",
                                company, dumpedPlayer, possibleSharesToSell, dumpThreshold);
                        // Half-selling to be checked later
                    } else {
                        // No dumpee found
                        maxSharesToSell = Math.min(maxSharesToSell,
                                ownedShares - presidentShareSize);
                        log.debug("case B2: {} no dump victim found, maxSharesToSell={}",
                                company, maxSharesToSell);
                    }
                } else {
                    // no dump necessary (not even possible! (EV))
                    possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                    log.debug("case C: {} no dump possible, sharesToSell={}",
                            company, possibleSharesToSell);
                }
            }
            log.debug("maxSharesToSell={} dumpPossible={} sharesToSell={}",
                    maxSharesToSell, dumpIsPossible, possibleSharesToSell);

            /* Allow for different share units (as in 1835) */ /*
            SortedMultiset<Integer> certCounts = playerPortfolio.getCertificateTypeCounts(company);
            // certCounts has counts of all owned non-president certificates, per share size
            log.debug ("certCounts={}", certCounts);

            // Make sure that single shares are always considered (due to possible dumping)
            SortedSet<Integer> certSizeElements = Sets.newTreeSet(certCounts.elementSet());
            if (certificateSplitAllowed) certSizeElements.add(1);
            log.debug ("certSizes={}", certSizeElements);

            for (int certSize : certSizeElements) {
                int certCount = certCounts.count(certSize);
                // certCount has the number of owned certs of the given share size
                log.debug("---- certSize={} certCount={}", certSize, certCount);

                // Does the company have a single share?
                //if (certSize == 1 && certCount == 0) continue;

                // If you can dump a presidency, add the shareNumbers of the presidency
                // to the single shares to be sold
                if (dumpIsPossible && certSize == 1
                        && certCount + presidentShareSize >= dumpThreshold) {
                    certCount += presidentShareSize;
                    //dumpThreshold -= presidentShareSize;  //??
                    // but limit this to the pool
                    certCount = Math.min(certCount, poolAllowsShares);
                    log.debug("Dump is possible increased single shares to {}", certCount);
                }

                if (certCount == 0) {
                    continue;
                }

                // Check against the maximum share that can be sold
                certCount = Math.min(certCount, maxSharesToSell / certSize);

                if (certCount <= 0) {
                    continue;
                }
                log.debug("Final sellable {} certCount={}x{}", company, certCount, certSize);
                for (int certNo = 1; certNo <= certCount; certNo++) {
                    log.debug ("-- Checking for dump={} certNo={} certCount={} certSize={} threshold={} split={}",
                            dumpIsPossible, certNo, certCount, certSize, dumpThreshold, certificateSplitAllowed);
                    if (certificateSplitAllowed) {
                        // check if selling would dump the company
                        if (dumpIsPossible && certNo * certSize >= dumpThreshold) {
                            // dumping requires that the total is in the possibleSharesToSell list and that shareSize == 1
                            // multiple shares have to be sold separately
                            if (certSize == 1 && possibleSharesToSell.contains(certNo * certSize)) {
                                //               ^^^^ Only used here!
                                sellShareActions.add(new SellShares(company, certSize, certNo, price, 1));
                                //sellableShares.add(new SellShares(company, shareSize, i, price,
                                //        i - presidentShareSize));
                                log.debug("*1* {} units={} qty={} presEx=1", company, certSize, certNo);
                            }
                        } else {
                            // ... no dumping: standard sell
                            sellShareActions.add(new SellShares(company, certSize, certNo, price, 0));
                            log.debug("*2* {} units={} qty={} presEx=0", company, certSize, certNo);
                        }
                    } else if (dumpIsPossible && certNo * certSize >= dumpThreshold) {
                        // Selling half a president is not allowed in 1835
                        if (certCounts.isEmpty() && certCount == 2) { // 1835 director share only
                            //ToDO : Adjust Logic for other Games with MultipleShareDirectors where splitting the share is not allowed
                            sellShareActions.add(new SellShares(company, 2, 1, price, 1));
                            log.debug("*3* {} units=2 qty=1 presEx=1", company);
                            //sellableSharesValue += price;
                        } else if ((certNo == 1) && ((!certCounts.isEmpty()) && (certCount == 2))) {
                            //1835 director share once and an action for the single share in the directors hand if we have the room :)
                            //sellableShares.add(new SellShares(company, 2, 1, price, 1));
                            //log.debug("*4a* {} units=2 qty=1 presEx=1", company);
                            sellShareActions.add(new SellShares(company, certSize, certNo, price, 1));
                            log.debug("*4b* {} units={} qty={} presEx=1", company, certSize, certNo);
                        } else if (((!certCounts.isEmpty()) && (certCount == 1)) || certCount > 2) {
                            sellShareActions.add(new SellShares(company, certSize, certNo, price, 1));
                            log.debug("*5* {} units={} qty={} presEx=1", company, certSize, certNo);
                        }
                    } else {
                        sellShareActions.add(new SellShares(company, certSize, certNo, price, 0));
                        log.debug("*6* {} units={} qty={} presEx=0", company, certSize, certNo);
                    }
                }
            }
        }
    }*/

    @Override
    public boolean sellShares(SellShares action) {

        /* Action attributes */
        PortfolioModel portfolio = currentPlayer.getPortfolioModel();
        String playerName = currentPlayer.getId();
        String companyName = action.getCompanyName();
        PublicCompany company =
            companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificate presCert = company.getPresidentsShare();
        int presCertShares = presCert.getShares();
        int certsToSell = action.getNumber();

        /* Indicates to what certificate size (1 or 2 shares) the pres.cert
         *  must be exchanged. In 1835: to one 20% or two 10% certs).
         *  The old president may choose.
         *  FIXME: currently not used, the choice is fixed.
         */
         int presidentExchange = action.getPresidentExchange();

        /* The certificate size (number of shares) to sell.
         *  Usually 1, in some games 2 is possible
         *  (in 1835: either 10% or 20% certificates)
         */
        int shareSizeToSell = action.getShareUnits();

        /* The number of shares (share units) to sell */
        int sharesToSell = certsToSell * shareSizeToSell;

        /* The number of shareUnits of a president certificate to sell.
         *  Usually 1 or 2 (in 1835: two 10% or one 20% share).
         *  It is derived from the chosen number of shares to sell
         *  and the currently owned number of shares (i.e. share units).
         *  N.B. This has nothing to do with presidentExchange, see there.
         */
        int presCertSharesToSell = 0;

        /* variables */
        Player dumpedPlayer = null;
        int dumpedPlayerShares = 0;
        boolean dumpWillHappen = false;
        List<PublicCertificate> soldCertificates =  new ArrayList<>();
        String errMsg = null;

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            if (certsToSell <= 0) {
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

            // If cert splitting is allowed, the player must have the share(s)
            //if (certificateSplitAllowed) {
                int playerShares = portfolio.getShares(company);
                if (playerShares < certsToSell) {
                    errMsg = LocalText.getText("NoShareOwned");
                    break;
                }
            //} else { // If splitting is not allowed, the player must have the exact certificate(s)

            //}

            // The pool may not get over its limit.
            if (pool.getShare(company) + certsToSell * company.getShareUnit()
                    > GameDef.getParmAsInt(this, GameDef.Parm.POOL_SHARE_LIMIT)) {
                errMsg = LocalText.getText("PoolOverHoldLimit");
                break;
            }

            boolean dumpedPlayerFound;
            boolean dumpAllowed = false;
            // Find the next president if a dump would occur
            dumpedPlayer = company.findPlayerToDump();
            dumpedPlayerFound = dumpedPlayer != null;
            if (dumpedPlayerFound) {
                dumpedPlayerShares = dumpedPlayer.getPortfolioModel().getShares(company);
            }
            int maxSharesToSell = playerShares;
            // If we are president...
            if (currentPlayer == company.getPresident()
                    // ... and not allowed to dump...
                    && (company == cashNeedingCompany || !dumpOtherCompaniesAllowed)) {
                // ... then find how many shares can be sold without dumping
                if (dumpedPlayerFound) {
                    // If we can but are not allowed to dump,
                    // we must keep at least as many shares as the next eligible player
                    maxSharesToSell -= dumpedPlayerShares;
                } else {
                    // Otherwise, we can sell all non-president shares
                    maxSharesToSell -= presCertShares;
                }
                if (certsToSell > maxSharesToSell) {
                    errMsg = LocalText.getText("CannotDumpPresidencyOf", companyName);
                    break;
                }
                dumpAllowed = false;
            } else if (currentPlayer == company.getPresident() && dumpedPlayerFound) {
                //  no selling restriction beyond actual need and pool capacity
                dumpAllowed = true;
                // Will dump happen?
                log.debug("Will dump? plSh={} sellSh={} dmpPlSh={}: {}",
                        playerShares, sharesToSell, dumpedPlayerShares,
                        playerShares - sharesToSell < dumpedPlayerShares);
                if (playerShares - sharesToSell < dumpedPlayerShares) {
                    dumpWillHappen = true;
                    //presCertSharesToSell = Math.min(presCert.getShares(), sharesToSell);
                    presCertSharesToSell = PlayerShareUtils.presidentShareNumberToSell(
                            company, currentPlayer, dumpedPlayer, sharesToSell - 1 + shareSizeToSell);
                    // reduce the numberToSell by the president (partial) sold certificate
                    sharesToSell -= presCertSharesToSell;
                }

            }

            if (maxSharesToSell < sharesToSell) {
                errMsg = LocalText.getText("CannotDumpTrainBuyingPresidency");
                break;
            }
            log.debug ("SSR presSharesToSell={} sharesToSell={}", presCertSharesToSell, sharesToSell);

            soldCertificates = PlayerShareUtils.findCertificatesToSell(company, currentPlayer,
                     sharesToSell/shareSizeToSell, shareSizeToSell, dumpAllowed);
            //                            ^^^^^ number of certificates
            log.debug("SSR soldCertificates={}", soldCertificates);

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
        int cashAmount = numberSold * price * shareSizeToSell;

        // Save original price as it may be reused in subsequent sale actions in the same turn
        boolean soldBefore = sellPrices.containsKey(company);
        if (!soldBefore) {
            sellPrices.put(company, company.getCurrentSpace());
            if (lastSoldCompany != company) lastSoldCompany = null;
        }

        String cashText = Currency.fromBank(cashAmount, currentPlayer);

        if (numberSold == 1) {
            ReportBuffer.add(this, LocalText.getText("SELL_SHARE_LOG",
                    playerName,
                    company.getShareUnit() * shareSizeToSell,
                    companyName,
                    cashText));
        } else {
            ReportBuffer.add(this, LocalText.getText("SELL_SHARES_LOG",
                    playerName,
                    numberSold,
                    company.getShareUnit() * shareSizeToSell,
                    numberSold * company.getShareUnit() * shareSizeToSell,
                    companyName,
                    cashText));
        }

        adjustSharePrice(company, currentPlayer, numberSold, soldBefore);

        if (!company.isClosed()) {
            if (executeShareTransfer(company, /*certsToSell*/ soldCertificates,
                    dumpedPlayer, presCertSharesToSell)) {
                log.debug ("Pre-selling pres.swap of {}", company);
            }
            if (dumpWillHappen) {
                if (company.checkPresidency (dumpedPlayer)) {
                    log.debug("Post-selling pres.swap of {}", company);
                }
            }
        }

        cashToRaise.add(-cashAmount);

        // Save original price as it may be reused in subsequent sale actions in the same turn
        boolean soldBeforeInSameTurn = sellPrices.containsKey(company);
        if (!soldBeforeInSameTurn) {
            sellPrices.put(company, company.getCurrentSpace());
        }

        if (cashToRaise.value() <= 0) {
            sellPrices.clear();
            gameManager.finishShareSellingRound();
        } else {
            getSellableShares();
            if (sellShareActions == null || sellShareActions.isEmpty()) {
                declareBankruptcy();
            }
        }

        return true;
    }

    private void declareBankruptcy () {
        String message = LocalText.getText("YouMustRaiseCashButCannot",
                currentPlayer,
                Bank.format(this, this.cashToRaise.value()));
        ReportBuffer.add(this, message);
        DisplayBuffer.add(this, message);
        if (GameDef.getParmAsBoolean(this, GameDef.Parm.EMERGENCY_COMPANY_BANKRUPTCY)) {
            // Currently not used, replaced by code in operatingRound.buyTrain().
            cashNeedingCompany.setBankrupt();
            gameManager.registerCompanyBankruptcy(cashNeedingCompany);
        } else {
            Currency.wireAll(currentPlayer, cashNeedingCompany);
            currentPlayer.setBankrupt();
            gameManager.registerPlayerBankruptcy(currentPlayer);

            // A bankrupt player must pass on the priority
            PlayerManager pmgr = gameManager.getRoot().getPlayerManager();
            if (pmgr.getPriorityPlayer().equals(currentPlayer)) {
                pmgr.setPriorityPlayerToNext();
            }
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
