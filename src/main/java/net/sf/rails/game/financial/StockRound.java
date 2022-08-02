package net.sf.rails.game.financial;

import java.util.*;

import com.google.common.collect.*;
import net.sf.rails.game.*;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.HashSetState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.Portfolio;
import rails.game.action.*;


/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class StockRound extends Round {
    private static final Logger log = LoggerFactory.getLogger(StockRound.class);

    /* Transient memory (per round only) */
    protected int numberOfPlayers;
    protected Player currentPlayer;
    protected Player startingPlayer;

    protected final GenericState<PublicCompany> companyBoughtThisTurnWrapper = new GenericState<>(this, "companyBoughtThisTurnWrapper");

    protected final BooleanState hasSoldThisTurnBeforeBuying = new BooleanState(this, "hasSoldThisTurnBeforeBuying");

    protected final BooleanState hasActed = new BooleanState(this, "hasActed");

    protected final IntegerState numPasses = IntegerState.create(this, "numPasses");

    /** Registers for which price shares have been sold.
     *  Usually, subsequent selling of the same company will use that same price.
     *  This is necessary in cases certificates of different sizes must be sold (e.g. 1835)
     */
    protected HashMapState<PublicCompany, StockSpace> sellPrices
            = HashMapState.create(this, "sellPrices");

    /** Will be set when company shares have been sold twice in a row.
     * In 1835, players will then be allowed to lower the price anyway.
     */
    protected PublicCompany lastSoldCompany;

    /**
     * Records lifted share selling obligations in the current round<p>
     * Example: >60% ownership allowed after a merger in 18EU.
     */
    protected HashSetState<PublicCompany> sellObligationLifted = null;


    /* Rule constants */
    public static final int SELL_BUY_SELL = 0;
    public static final int SELL_BUY = 1;
    public static final int SELL_BUY_OR_BUY_SELL = 2;

    /* Action constants */
    public static final int BOUGHT = 0;
    public static final int SOLD = 1;

    /* Rules */
    protected int sequenceRule;
    protected boolean raiseIfSoldOut = false;
    protected boolean certificateSplitAllowed = true;
    protected SortedMap<Integer, Integer> certCountsPerSize;

    /* Generated SellShares actions
     * (separately provided by ShareSellingRound)
     */
    protected List<SellShares> sellShareActions;

    /* Temporary variables */
    protected boolean isOverLimits = false;
    protected String overLimitsDetail = null;

    /**
     * Autopasses
     */
    private final ArrayListState<Player> autopasses = new ArrayListState<>(this, "autopasses");
    private final ArrayListState<Player> canRequestTurn = new ArrayListState<>(this, "canRequestTurn");
    private final ArrayListState<Player> hasRequestedTurn = new ArrayListState<>(this, "hasRequestedTurn");

    /*
     * Companies started this round (shares may not be sold in SOH)
     */
    private ArrayListState<PublicCompany> startedThisRound = new ArrayListState<>(
            this, "startedThisSR");

    /** Release rules, parsed and initialised in CompanyManager */
    protected List<ReleaseRule> releaseRules;


    /**
     * Constructed via Configure
     */
    public StockRound(GameManager parent, String id) {
        super(parent, id);

        numberOfPlayers = getRoot().getPlayerManager().getPlayers().size();

        sequenceRule = GameDef.getParmAsInt(this, GameDef.Parm.STOCK_ROUND_SEQUENCE);
        certificateSplitAllowed
                = !gameManager.getParmAsBoolean(GameDef.Parm.NO_CERTIFICATE_SPLIT_ON_SELLING);

        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, true);
        guiHints.setActivePanel(GuiDef.Panel.STATUS);
        log.info ("--- Starting SR type round: {} ---", getId());
    }

    /**
     * Start the Stock Round. <p>
     * Please note: subclasses that are NOT real stock rounds should NOT call this method
     * (or set raiseIfSoldOut to false after calling this method).
     */
    // called by:
    // GameManager: startStockRound
    // StockRound 1837, 18EU: (start)

    // overridden by:
    // StockRound 1837, 18EU
    // NationalFormationRound, PrussianFormationRound
    public void start() {

        ReportBuffer.add(this, LocalText.getText("StartStockRound",
                getStockRoundNumber()));

        Player priorityPlayer = playerManager.getPriorityPlayer();
        boolean initial = true;
        while (priorityPlayer.isBankrupt()) {
            if (initial) {
                ReportBuffer.add(this, LocalText.getText("PriorityPlayerIsBankrupt",
                        priorityPlayer.getId()));
                initial = false;
            } else {
                ReportBuffer.add(this, LocalText.getText("PlayerIsBankrupt",
                        priorityPlayer.getId()));
            }
            priorityPlayer = playerManager.setPriorityPlayerToNext();
        }
        playerManager.setCurrentToPriorityPlayer();
        startingPlayer = playerManager.getCurrentPlayer(); // For the Report
        ReportBuffer.add(this, LocalText.getText("HasPriority",
                startingPlayer.getId()));

        initPlayer();
        releaseRules = companyManager.getReleaseRules();

        // In the first stock round new companies may have to be released
        if (getStockRoundNumber() == 1) checkForCompanyReleases();

        raiseIfSoldOut = true;
        startedThisRound.clear();

    }

    /*----- General methods -----*/
    // called by
    // StockRound: checkFirstRoundSellRestriction, finishRound, getRoundName, start
    // StockRound 1837, 1880: finishRound
    // StatusWindow: updateStatus

    // not overridden
    public int getStockRoundNumber() {
        return gameManager.getSRNumber();
    }

    // called by:
    // GameManager: process, processOnReload
    // GameLoader: replayGame
    // StockRound 1837, 1856, 18EU: setPossibleActions

    // overridden by
    // ShareSellingRound
    // TreasuryShareRound
    // NationalFormationRound, PrussianFormationRound
    // StockRound 1837, 1856, 18EU
    // ShareSellingRound 1880
    // FinalMinorExchangeRound, FinalCoalExchangeRound
    @Override
    public boolean setPossibleActions() {

        // fix of the forced undo bug
        currentPlayer = playerManager.getCurrentPlayer();

        sellShareActions = null;
        setSellShareActions();

        // Certificate limits must be obeyed by selling excess shares
        // before any other action is allowed.
        if (isOverLimits) return true;

        boolean passAllowed = true;

        setBuyableCerts();

        setSpecialActions();

        setGameSpecificActions();

        if (passAllowed) {
            if (hasActed.value()) {
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
            } else {
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.PASS));
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.AUTOPASS));
            }
        }

        if (getAutopasses() != null) {
            for (Player player : getAutopasses()) {
                possibleActions.add(new RequestTurn(player));
            }
        }

        return true;
    }

    /**
     * Stub, can be overridden in subclasses
     */
    // called by:
    // StockRound: setPossibleActions

    // overridden by:
    // StockRound 1835, 1837, 18EU
    protected void setGameSpecificActions() {

    }

    /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     */
    // called by
    // StockRound: setPossibleActions
    // StockRound 1835: (setBuyableCerts)

    // overridden by
    // TreasuryShareRound
    // StockRound 1835, 1880,18EU, 18Scan (for its minors)
    //
    // EV sep 2020: The three share sources, IPO, Pool and Treasury,
    // are handled completely separate. But many checks apply
    // to all three, and seem sometimes to be done in different ways.
    // TODO: To be considered for rewriting.
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        ImmutableSet<PublicCertificate> certs;
        PublicCertificate cert;
        StockSpace stockSpace = null;
        PortfolioModel from;
        int price;
        int number;
        int unitsForPrice;

        /* Let's first check if there any presidents in the Pool.
         * If so, that's the only cert of that company we may buy,
         * except when the current player has one share.
         *
         * NOTE: this currently only affects 18Scan.
         * Perhaps code related to this aspect should be moved to StockRound_18Scan
         */
        Map<PublicCompany, Boolean> presidentsInPool = new HashMap<>();
        for (PublicCompany comp : gameManager.getAllPublicCompanies()) {
            if (comp.getPresidentsShare().getOwner() == pool.getParent()) {
                int sharesOfPlayer = currentPlayer.getPortfolioModel().getShares(comp);
                presidentsInPool.put (comp, sharesOfPlayer == 1);
            }
        }

        int playerCash = currentPlayer.getCashValue();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO (exception: SOH)
        PublicCompany companyBoughtThisTurn =
                companyBoughtThisTurnWrapper.value();
        if (companyBoughtThisTurn == null) {
            from = ipo;
            ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                    from.getCertsPerCompanyMap();

            for (PublicCompany comp : map.keySet()) {
                if (currentPlayer.hasSoldThisRound(comp)) continue;
                if (presidentsInPool.containsKey(comp)) {
                    // If president is in the pool, no cert from IPO may be bought
                    // except if the player has one share
                    if (!presidentsInPool.get(comp)) continue;
                }

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
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        comp.getShareUnit()) < 1) continue;

                /* Would the player exceed the total certificate limit? */
                stockSpace = comp.getCurrentSpace();
                if ((stockSpace == null || !stockSpace.isNoCertLimit()) && !mayPlayerBuyCertificate(
                        currentPlayer, comp, cert.getCertificateCount())) continue;

                if (!cert.isPresidentShare()) {
                    if (comp.hasStockPrice()) {
                        price = comp.getIPOPrice() / unitsForPrice;
                    } else {
                        price = comp.getFixedPrice();
                    }
                    if ((price * cert.getShares()) <= playerCash) {
                        possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                                from.getParent(), price));
                    }
                } else if (!comp.hasStarted()) {
                    if (!comp.hasStockPrice()) {
                        // E.g. for 1837 minors that are bought in a stock round.
                        price = comp.getFixedPrice();
                        if (price <= playerCash) {
                            possibleActions.add(new StartCompany(comp, price));
                        }
                    } else if (comp.getIPOPrice() != 0) {
                        price = comp.getIPOPrice() * cert.getShares() / unitsForPrice;
                        if (price <= playerCash) {
                            possibleActions.add(new StartCompany(comp, price));
                        }
                    } else {
                        StockSpace fixedPrice = comp.getStartSpace();
                        if (fixedPrice != null) {
                            if (fixedPrice.getPrice() <= playerCash) {
                                possibleActions.add(new StartCompany(comp, fixedPrice.getPrice()));
                            }
                        } else {
                            List<Integer> startPrices = new ArrayList<>();
                            for (int startPrice : stockMarket.getStartPrices()) {
                                if (startPrice * cert.getShares() <= playerCash) {
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
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        ImmutableSetMultimap<PublicCompany, PublicCertificate> certsPerCompanyMap =
                from.getCertsPerCompanyMap();
        /* Allow for multiple share unit certificates (e.g. 1835) */
        PublicCertificate[] uniqueCerts;
        int[] numberOfCerts;
        int shares;
        int shareUnit;
        int maxNumberOfSharesToBuy;
        boolean president = false;
        int presShareSize = 0;

        for (PublicCompany comp : certsPerCompanyMap.keySet()) {
            if (currentPlayer.hasSoldThisRound(comp)) continue;
            if (comp.hasStockPrice()) {
                stockSpace = comp.getCurrentSpace();
                price = stockSpace.getPrice() / comp.getShareUnitsForSharePrice();
            } else { // Minors may have a fixed price
                price = comp.getFixedPrice();
            }

            // If the president's share is in the Pool,
            // that is the only one we may buy, except
            // when the player has already one share
            if (presidentsInPool.containsKey(comp)) {
                cert = comp.getPresidentsShare();
                int buyPrice = price * cert.getShares();
                if (playerCash >= buyPrice) {
                    BuyCertificate bc = new BuyCertificate(comp,
                            cert.getShare(),
                            pool.getParent(), buyPrice, 1);
                    bc.setPresident(true);
                    possibleActions.addFirst(bc);
                    log.debug("Buy president {} from pool: price={} shares={} share={}",
                            comp, buyPrice, cert.getShares(), cert.getShare());
                }
                continue;
            }

            certs = certsPerCompanyMap.get(comp);
            if (certs.isEmpty()) continue;

            shareUnit = comp.getShareUnit();
            maxNumberOfSharesToBuy
                    = maxAllowedNumberOfSharesToBuy(currentPlayer, comp, shareUnit);

            /* Checks if the player can buy any shares of this company */
            if (maxNumberOfSharesToBuy < 1) continue;
            if (companyBoughtThisTurn != null) {
                // If a cert was bought before, only brown zone ones can be
                // bought again in the same turn
                if (comp != companyBoughtThisTurn) continue;
                if (stockSpace != null && !stockSpace.isNoBuyLimit()) continue;
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
                if (cert2.isPresidentShare()) {
                    president = true;
                    presShareSize = shares;
                }
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
                    BuyCertificate bc = new BuyCertificate(comp,
                            uniqueCerts[shares].getShare(),
                            from.getParent(), price,
                            number);
                    if (president && shares == presShareSize) {
                        // There is a president's share for sale!
                        // In 1835 we might even have a pres and a non-pres 20% share,
                        // in which case it's assumed that the pres must be bought first.
                        bc.setPresident(true);
                        // Let's assume that buying a 10% share instead is not forbidden.
                        // That is certainly true in 18Scan, but unspecified in 1835.

                        // Put it on top!
                        possibleActions.addFirst(bc);
                    } else {
                        possibleActions.add(bc);
                    }
                }
            }
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompany company : companyManager.getAllPublicCompanies()) {
                // TODO: Has to be rewritten (director)
                if (currentPlayer.hasSoldThisRound(company)) continue;
                if (presidentsInPool.containsKey(company)) {
                    // If president is in the pool, no cert from IPO may be bought
                    // except if the player has one share
                    if (!presidentsInPool.get(company)) continue;
                }

                certs = company.getPortfolioModel().getCertificates(company);
                if (certs.isEmpty()) continue;
                cert = Iterables.get(certs, 0);
                if (!checkAgainstHoldLimit(currentPlayer, company, 1)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, company,
                        company.getShareUnit()) < 1) continue;
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
    public void setSellShareActions() {
        findSellableShares ();
        possibleActions.addAll(sellShareActions);
    }

    protected List<SellShares> getSellableShares() {
        findSellableShares();
        return sellShareActions;
    }

    public void findSellableShares() {

        findSellableShares (false, null, 0, false);
    }

   /**
    * Create a list of certificates that a player may sell in a Stock Round,
    * taking all rules taken into account.
    */

    // FIXME Rails 2.0:
    // This is rewritten taken into account that actions will not be changed for now
    // A change of action will allow to simplify this strongly

    // called by:
    // StockRound: setPossibleActions

    // Entry point from ShareSellingRound (emergency selling)
    public void findSellableShares(boolean emergency, PublicCompany cashNeedingCompany,
                                  int cashToRaise, boolean dumpOtherCompaniesAllowed) {

        if (!mayCurrentPlayerSellAnything()) return;

        isOverLimits = false;
        overLimitsDetail = null;

        StringBuilder violations = new StringBuilder();
        PortfolioModel playerPortfolio = currentPlayer.getPortfolioModel();
        sellShareActions = null;

        /*
         * First check of which companies the player owns stock, and what
         * maximum percentage he is allowed to sell.
         */
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
Util.breakIf(company.getId(), "NYNH");
            int ownedShares = playerPortfolio.getShares(company);
            if (ownedShares == 0) {
                continue;
            }
            log.debug("company={} ownedShare={}", company, ownedShares);

            // Check if shares of this company can be sold at all
            if (!mayPlayerSellShareOfCompany(company)) {
                continue;
            }
            // Check the price. If a cert was sold before this turn, the original price is still valid.
            int price = getCurrentSellPrice(company);

            /* May not sell more than the Pool can accept */
            int poolAllowsShares = PlayerShareUtils.poolAllowsShares(company);
            log.debug("poolAllowShares={}", poolAllowsShares);
            int maxSharesToSell = Math.min(ownedShares, poolAllowsShares);

            // if no share can be sold
            if (maxSharesToSell == 0) {
                continue;
            }

            if (!emergency) {
                // Is player over the hold limit of this company?
                if (!checkAgainstHoldLimit(currentPlayer, company, 0)) {
                    // The first time this happens, remove all non-over-limits sell options
                    if (!isOverLimits) possibleActions.clear();
                    isOverLimits = true;
                    violations.append(LocalText.getText("ExceedCertificateLimitCompany",
                            company.getId(),
                            playerPortfolio.getShare(company),
                            GameDef.getParmAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)
                    ));

                } else {
                    // If within limits, but an over-limits situation exists: correct that first.
                    if (isOverLimits) continue;
                }
            }

            /*
             * If the current Player is president, check if there is a player to dump on
             * => dumpThreshold = how many shares have to be sold for dump
             * => possibleSharesToSell = list of shares that can be sold
             *    (includes check for swapping the presidency)
             * => dumpIsPossible = true
             */
            int dumpThreshold = 0;
            //SortedSet<Integer> possibleSharesToSell = null;
            SortedMap<Integer, Integer> participationsToSell = null; // certSize -> number of these
            boolean dumpIsPossible = false;
            boolean isPresident = false;
            int presidentSize = company.getPresidentsShare().getShares();

            if (company.getPresident() == currentPlayer) {
                isPresident = true;
                Player potential = company.findPlayerToDump();
                if (potential != null) {
                    dumpThreshold = ownedShares - potential.getPortfolioModel().getShares(company) + 1;
                    dumpIsPossible = true;
                }
            }

            if (emergency) {
                // In emergency cases, dumping is not always allowed.
                if (dumpIsPossible && company.getPresident() == currentPlayer
                        && (company == cashNeedingCompany || !dumpOtherCompaniesAllowed)) {
                    maxSharesToSell = Math.min(maxSharesToSell, dumpThreshold - 1);
                    dumpIsPossible = false;
                }
                // May not sell more than is needed to buy the train
                while (maxSharesToSell > 0
                       && ((maxSharesToSell - 1) * price) > cashToRaise) {
                    maxSharesToSell--;
                }
            }
            if (maxSharesToSell == 0) {
                continue;
            }

            log.debug("dumpIsPossible={} threshold={}", dumpIsPossible, dumpThreshold);

            if (certificateSplitAllowed) {
                // Selling shares
                SortedSet<Integer> possibleSharesToSell = PlayerShareUtils.sharesToSell(company, currentPlayer);
                participationsToSell = new TreeMap<>();
                participationsToSell.put (1, possibleSharesToSell.size());
            } else {
                participationsToSell = PlayerShareUtils.certificatesToSell(company, currentPlayer);
            }
            log.debug("participationsToSell={}", participationsToSell);


            /*
             * Check what share units the player actually owns. In some games
             * (e.g. 1835) companies may have different ordinary shares: 5% and
             * 10%, or 10% and 20%. The president's share counts as a multiple
             * of the smallest ordinary share unit type.
             */
            for (int certSize : participationsToSell.keySet()) {
                int certCount = participationsToSell.get(certSize);
                log.debug("---- certSize={} certCount={}", certSize, certCount);

                // If you can dump a presidency, you add the shareNumbers of the presidency
                // to the single shares to be sold
                // (this is not for 1835 where certs may not be split; that is handled below)
                if (certificateSplitAllowed) {
                    if (dumpIsPossible && certSize == 1
                            && certCount + presidentSize >= dumpThreshold) {
                        certCount += presidentSize;
                        // but limit this to the pool
                        certCount = Math.min(certCount, poolAllowsShares);
                        log.debug("Dump is possible increased single shares to {}", certCount);
                    }

                    if (certCount == 0) {
                        continue;
                    }

                    /* In some games (1856), a just bought share may not be sold */
                    // This code ignores the possibility of different share units
                    if ((Boolean) gameManager.getGameParameter(GameDef.Parm.NO_SALE_OF_JUST_BOUGHT_CERT)
                            && company.equals(companyBoughtThisTurnWrapper.value())
                            /* An 1856 clarification by Steve Thomas (backed by Bill Dixon) states that
                             * in this situation a half-presidency may be sold
                             * (apparently even if a dump would otherwise not be allowed),
                             * as long as the certCount of shares does not become zero.
                             * So the rule "can't sell a just bought share" only means,
                             * that the certCount of shares may not be sold down to zero.
                             * Added 4jun2012 by EV */
                            && certCount == ownedShares) {
                        certCount--;
                    }

                    if (certCount <= 0) {
                        continue;
                    }

                    // Check against the maximum share that can be sold
                    certCount = Math.min(certCount, maxSharesToSell / certSize);

                    if (certCount <= 0) {
                        continue;
                    }
                    log.debug("Final sellable={} certCount={}x{}{}", company, certCount, certSize,
                            isPresident ? "P" : "");
                }

                for (int certNo = 1; certNo <= certCount; certNo++) {
                    log.debug("-- certNo={}", certNo);
                    int presidentExchange = 0;
                    //if (certificateSplitAllowed) {
                    // check if selling would dump the company
                    if (dumpIsPossible && certNo * certSize >= dumpThreshold) {
                        presidentExchange = 1;
                        //log.debug ("Checking for dump={} certNo={} certSize={} threshold={}",
                        //        dumpIsPossible, certNo, certSize, dumpThreshold);
                        // dumping requires that the total is in the possibleSharesToSell list and that shareSize == 1
                        // multiple shares have to be sold separately
                        //if (certSize == 1 && possibleSharesToSell.contains(certNo * certSize)) {
                        //    possibleActions.add(new SellShares(company, certSize, certNo, price, 1));
                        //    log.debug("*1* {} units={} qty={} presEx=1", company, certSize, certNo);
                    }
                    //} else {
                    // ... no dumping: standard sell
                    addSellShareAction(new SellShares(company, certSize, certNo, price, presidentExchange));
                    log.debug("Added {} size={} qty={} presEx={}}", company, certSize, certNo, presidentExchange);
                    //}

                }

                /*else {
                        if (dumpIsPossible && certNo * certSize >= dumpThreshold) {
                            log.debug ("Checking for dump={} certNo={} certSize={} threshold={}",
                                    dumpIsPossible, certNo, certSize, dumpThreshold);
                            if (certCounts.isEmpty() && certCount == 2) { // 1835 director share only
                                //ToDO : Adjust Logic for other Games with MultipleShareDirectors where splitting the share is not allowed
                                possibleActions.add(new SellShares(company, 2, 1, price, 1));
                                log.debug("*3* {} units=2 qty=1 presEx=1", company);
                            } else if ((certNo == 1) && ((!certCounts.isEmpty()) && (certCount == 2))) { //1835 director share once and an action for the single share in the directors hand if we have the room :)
                                possibleActions.add(new SellShares(company, 2, 1, price, 1));
                                log.debug("*4a* {} units=2 qty=1 presEx=1", company);
                                possibleActions.add(new SellShares(company, certSize, certNo, price, 1));
                                log.debug("*4b* {} units={} qty={} presEx=1", company, certSize, certNo);
                            } else if (((!certCounts.isEmpty()) && (certCount == 1)) || certCount > 2) {
                                possibleActions.add(new SellShares(company, certSize, certNo, price, 1));
                                log.debug("*5* {} units={} qty={} presEx=1", company, certSize, certNo);
                            }
                        } else {
                            possibleActions.add(new SellShares(company, certSize, certNo, price, 0));
                            log.debug("*6* {} units={} qty={} presEx=0", company, certSize, certNo);
                        }
                    }
                }*/
            }
            // If certificate splitting is not allowed (1835), then
            // selling a presidency must be added as a special case
            if (!certificateSplitAllowed) {
                if (isPresident && dumpIsPossible && presidentSize >= dumpThreshold
                        && presidentSize <= poolAllowsShares
                        // After a pres.cert. is dumped, the remaining number
                        // of shares must be less that the president size
                        // So if pres is 20%, then player may have max 30% before the dump.
                        // Example: 1835 BY, dump allowed with 20%P + 1x10% but not with 20%P + 2x10%.
                        // See German rules (1990) 3.3.7. English rules seem to omit that detail.
                        && playerPortfolio.getShares(company) <= 2 * presidentSize - 1) {
                    addSellShareAction(new SellShares(company, presidentSize, 1, price, 1));
                }
            }

        }

        // Is player over the total certificate hold limit?
        float certificateCount = playerPortfolio.getCertificateCount();
        int certificateLimit = gameManager.getPlayerCertificateLimit(currentPlayer);
        if (certificateCount > certificateLimit) {
            violations.append(LocalText.getText("ExceedCertificateLimitTotal",
                    certificateCount,
                    certificateLimit));
            isOverLimits = true;
        }

        if (isOverLimits) {
            DisplayBuffer.add(this, LocalText.getText("ExceedCertificateLimit"
                    , currentPlayer.getId()
                    , violations.toString()
            ));
        }
    }

    protected void addSellShareAction (SellShares action) {
        if (sellShareActions == null) sellShareActions = new ArrayList<>();
        sellShareActions.add (action);
    }

    protected boolean checkIfCertificateSplitAllowed() {
        return !gameManager.getParmAsBoolean(GameDef.Parm.NO_CERTIFICATE_SPLIT_ON_SELLING);
        //return true;
    }

    // called by:
    // StockRound: setPossibleActions

    // not overridden
    protected void setSpecialActions() {

        List<SpecialProperty> sps =
                currentPlayer.getPortfolioModel().getSpecialProperties(
                        SpecialProperty.class, false);
        for (SpecialProperty sp : sps) {
            if (sp.isUsableDuringSR()) {
                possibleActions.add(new UseSpecialProperty(sp));
            }
        }
    }

    /*----- METHODS THAT PROCESS PLAYER ACTIONS -----*/
    // called by:
    // GameManager: process, processOnReload
    // StockRound 1880: (process)
    // ShareSellingRound 1880: (process)

    // overridden by
    // StockRound 1880
    // ShareSellingRound 1880
    @Override
    public boolean process(PossibleAction action) {

        boolean result = false;
        String playerName = action.getPlayerName();
        currentPlayer = playerManager.getCurrentPlayer();

        if (action instanceof NullAction) {

            NullAction nullAction = (NullAction) action;
            switch (nullAction.getMode()) {
                case PASS:
                case DONE:
                    result = done((NullAction) action, playerName, false);
                    break;
                case AUTOPASS:
                    result = done(null, playerName, true);
                    break;
                default:
                    break;
            }

        } else if (action instanceof StartCompany) {

            StartCompany startCompanyAction = (StartCompany) action;

            result = startCompany(playerName, startCompanyAction);

        } else if (action instanceof BuyCertificate) {

            result = buyShares(playerName, (BuyCertificate) action);

        } else if (action instanceof SellShares) {

            result = sellShares((SellShares) action);

        } else if (action instanceof UseSpecialProperty) {

            result = useSpecialProperty((UseSpecialProperty) action);

        } else if (action instanceof RequestTurn) {

            result = requestTurn((RequestTurn) action);

        } else if (result = processGameSpecificAction(action)) {

        } else {

            DisplayBuffer.add(this, LocalText.getText("UnexpectedAction",
                    action.toString()));
        }

        return result;
    }

    // Return value indicates whether the action has been processed.
    // called by:
    // StockRound: process

    // overridden by:
    // StockRound 1837, 18EU
    // PrussianFormationRound, NationalFormationRound
    protected boolean processGameSpecificAction(PossibleAction action) {

        return false;
    }

    /**
     * Start a company by buying one or more shares (more applies to e.g. 1841)
     *
     * @param playerName  The player that wants to start a company.
     * @param action containing the company to start, the price(par) and the number of shares to buy.
     * price   The start (par) price (ignored if the price is fixed).
     * shares  The number of shares to buy (can be more than 1 in e.g.
     *                1841 and SOH).
     * @return True if the company could be started. False indicates an error.
     */
    // called by:
    // StockRound: process
    // StockRound 1880: startCompany (not overridden!)

    // overridden by:
    // StockRound 18EU
    public boolean startCompany(String playerName, StartCompany action) {
        PublicCompany company = action.getCompany();
        int price = action.getPrice();
        int shares = action.getNumberBought();

        String errMsg = null;
        StockSpace startSpace = null;
        int numberOfCertsToBuy = 0;
        PublicCertificate cert = null;
        String companyName = company.getId();
        int cost = 0;

        currentPlayer = playerManager.getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            // Only the player that has the turn may buy
            if (!playerName.equals(currentPlayer.getId())) {
                errMsg = LocalText.getText("WrongPlayer", playerName, currentPlayer.getId());
                break;
            }

            // The player may not have bought this turn.
            if (companyBoughtThisTurnWrapper.value() != null) {
                errMsg = LocalText.getText("AlreadyBought", playerName);
                break;
            }

            // Check company
            if (company == null) {
                errMsg = LocalText.getText("CompanyDoesNotExist", companyName);
                break;
            }
            // The company may not have started yet.
            if (company.hasStarted()) {
                errMsg =
                        LocalText.getText("CompanyAlreadyStarted", companyName);
                break;
            }

            // Find the President's certificate
            cert = ipo.findCertificate(company, true);
            // Make sure that we buy at least one!
            if (shares < cert.getShares()) shares = cert.getShares();

            // Determine the number of Certificates to buy
            // (shortcut: assume that any additional certs are one share each)
            numberOfCertsToBuy = shares - (cert.getShares() - 1);
            // Check if the player may buy that many certificates.
            if (!mayPlayerBuyCertificate(currentPlayer, company, numberOfCertsToBuy)) {
                errMsg = LocalText.getText("CantBuyMoreCerts");
                break;
            }

            // Check if the company has a fixed par price (1835).
            startSpace = company.getStartSpace();
            if (startSpace != null) {
                // If so, it overrides whatever is given.
                price = startSpace.getPrice();
            } else if (company.hasStockPrice()){
                // Else the given price must be a valid start price
                if ((startSpace = stockMarket.getStartSpace(price)) == null) {
                    errMsg = LocalText.getText("InvalidStartPrice",
                            Bank.format(this, price),
                            company.getId());
                    break;
                }
            }

            // Check if the Player has the money.
            cost = shares * price;
            if (currentPlayer.getCashValue() < cost) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantStart",
                    playerName,
                    companyName,
                    Bank.format(this, price),
                    errMsg));
            return false;
        }


        // All is OK, now start the company
        company.start(startSpace);
        startedThisRound.add (company);

        MoneyOwner priceRecipient = getSharePriceRecipient(company, ipo.getParent(), price);

        // Transfer the President's certificate
        cert.moveTo(currentPlayer);

        // If more than one certificate is bought at the same time, transfer
        // these too.
        for (int i = 1; i < numberOfCertsToBuy; i++) {
            cert = ipo.findCertificate(company, false);
            cert.moveTo(currentPlayer);
        }

        // Pay for these shares
        String costText = Currency.wire(currentPlayer, cost, priceRecipient);

        ReportBuffer.add(this, LocalText.getText("START_COMPANY_LOG",
                playerName,
                companyName,
                bank.getCurrency().format(price), // TODO: Do this nicer
                costText,
                shares,
                cert.getShare(),
                priceRecipient.getId()));

        checkFlotation(company);

        companyBoughtThisTurnWrapper.set(company);
        hasActed.set(true);
        setPriority("StartCompany");

        // Check for any game-specific consequences
        // (such as making another company available in the IPO)
        gameSpecificChecks(ipo, company, true);
        // Check for any new companies to be made purchaseable
        checkForCompanyReleases();

        return true;
    }

    /**
     * Buying one or more single or double-share certificates (more is sometimes
     * possible)
     *
     * @param playerName The player that wants to buy shares.
     * @param action The executed BuyCertificates action
     * @return True if the certificates could be bought. False indicates an
     * error.
     */
    // called by:
    // StockRound: process

    // overriden by:
    // TreasuryShareRound
    public boolean buyShares(String playerName, BuyCertificate action) {

        PublicCompany company = action.getCompany();
        PublicCertificate cert = null;
        PortfolioModel from = action.getFromPortfolio();
        String companyName = company.getId();
        int number = action.getNumberBought();
        int shareUnit = company.getShareUnit();
        int sharePerCert = action.getSharePerCertificate();
        int share = number * sharePerCert;
        int shares = share / shareUnit;
        boolean president = action.isPresident();

        String errMsg = null;
        int price = 0;
        int cost = 0;

        currentPlayer = playerManager.getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            // Only the player that has the turn may buy
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

            // The player may not have sold the company this round.
            if (currentPlayer.hasSoldThisRound(company)) {
                errMsg =
                        LocalText.getText("AlreadySoldThisTurn",
                                currentPlayer.getId(),
                                companyName);
                break;
            }

            if (!company.isBuyable()) {
                errMsg = LocalText.getText("NotYetStarted", companyName);
                break;
            }

            // The player may not have bought this turn, unless the company
            // bought before and now is in the brown area.
            PublicCompany companyBoughtThisTurn =
                    companyBoughtThisTurnWrapper.value();
            if (companyBoughtThisTurn != null
                    && (companyBoughtThisTurn != company || !company.getCurrentSpace().isNoBuyLimit())) {
                errMsg = LocalText.getText("AlreadyBought", playerName);
                break;
            }

            // Check if that many shares are available
            if (shares > from.getShare(company)) {
                errMsg =
                        LocalText.getText("NotAvailable",
                                companyName,
                                from.getId());
                break;
            }

            StockSpace currentSpace;
            if (from == ipo && company.hasParPrice()) {
                currentSpace = company.getStartSpace();
            } else {
                currentSpace = company.getCurrentSpace();
            }

            // Check if it is allowed to buy more than one certificate (if
            // requested)
            if (number > 1 && !currentSpace.isNoBuyLimit()) {
                errMsg = LocalText.getText("CantBuyMoreThanOne", companyName);
                break;
            }

            // Check if player would not exceed the certificate limit.
            // (shortcut: assume 1 cert == 1 certificate)
            cert = from.findCertificate(company, sharePerCert / shareUnit, president);
            if (cert == null) {
                log.error("Cannot find {}% of {} in {}", sharePerCert, company.getId(), from.getId());
                errMsg = LocalText.getText ("NotFound");
                break;
            }
            if ((!company.hasStockPrice() || !currentSpace.isNoCertLimit())
                    && !mayPlayerBuyCertificate(currentPlayer, company, number * cert.getCertificateCount())) {
                errMsg =
                        currentPlayer.getId()
                                + LocalText.getText("WouldExceedCertLimit",
                                String.valueOf(gameManager.getPlayerCertificateLimit(currentPlayer)));
                break;
            }

            // Check if player would exceed the per-company share limit
            if ((!company.hasStockPrice() || !currentSpace.isNoHoldLimit())
                    && !checkAgainstHoldLimit(currentPlayer, company, shares)) {
                errMsg = LocalText.getText("WouldExceedHoldLimit",
                        currentPlayer.getId(),
                        GameDef.Parm.PLAYER_SHARE_LIMIT.defaultValueAsInt());
                break;
            }

            price = getBuyPrice(action, currentSpace);
            cost = shares * price / company.getShareUnitsForSharePrice();

            // Check if the Player has the money.
            if (currentPlayer.getCashValue() < cost) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantBuy",
                    playerName,
                    shares,
                    companyName,
                    from.getId(),
                    errMsg));
            return false;
        }

        // All seems OK, now buy the shares.


        MoneyOwner priceRecipient = getSharePriceRecipient(company, from.getParent(), cost);

        if (number == 1) {
            String key = president ? "BuyPresidentLog" : "BUY_SHARE_LOG";
            ReportBuffer.add(this, LocalText.getText(key,
                    playerName,
                    share,
                    companyName,
                    from,
                    Bank.format(this, cost)));
            cert.moveTo(currentPlayer);
            if (president) {
                ReportBuffer.add (this, LocalText.getText("IS_NOW_PRES_OF",
                        playerName, companyName));
            }
        } else {
            ReportBuffer.add(this, LocalText.getText("BUY_SHARES_LOG",
                    playerName,
                    number,
                    share,
                    shares,
                    companyName,
                    from,
                    Bank.format(this, cost)));
            for (int i = 0; i < number; i++) {
                cert = from.findCertificate(company, sharePerCert / shareUnit, false);
                if (cert == null) {
                    log.error("Cannot find {} {}% share in {}", companyName, shareUnit * sharePerCert, from.getId());
                    continue;
                }
                cert.moveTo(currentPlayer);
            }
        }

        String costText = Currency.wire(currentPlayer, cost, priceRecipient);
        if (priceRecipient != from.getMoneyOwner()) {
            ReportBuffer.add(this, LocalText.getText("PriceIsPaidTo",
                    costText,
                    priceRecipient.getId()));
        }

        companyBoughtThisTurnWrapper.set(company);
        hasActed.set(true);
        setPriority("BuyCert");

        // Check if presidency has changed
        company.checkPresidencyOnBuy(currentPlayer);

        // Check if the company has floated
        if (!company.hasFloated()) checkFlotation(company);

        // Check for any game-specific consequences
        // (such as making another company available in the IPO)
        gameSpecificChecks(from, company);

        // Check for any new companies to be made purchaseable
        if (from == ipo) checkForCompanyReleases();

        return true;
    }

    /**
     * Stub, may be overridden in subclasses
     */
    // called by:
    // StockRound: buyShares, startCompany
    // StockRound 1880: (gameSpecificChecks)

    // overridden by:
    // StockRound 1825, 1856, 1880
    protected void gameSpecificChecks(PortfolioModel boughtFrom,
                                      PublicCompany company,
                                      boolean justStarted) {}

    protected void gameSpecificChecks(PortfolioModel boughtFrom,
                                       PublicCompany company) {
        gameSpecificChecks (boughtFrom, company, false /*actually: doesn't matter*/);
    }

    protected void checkForCompanyReleases () {

        // Check if any companies must be released
        int step = gameManager.getCompanyReleaseStep();
        // Are there any such rules?
        if (releaseRules != null && !releaseRules.isEmpty()
                // Have we exhausted the rules?
                && step < releaseRules.size()) {
            ReleaseRule release = releaseRules.get(step);
            // isDone() does the job
            if (release.isDone()) {
                // if the release is complete, select the next rule
                gameManager.setCompanyReleaseStep(++step);
            }
        }
    }

    /**
     * Allow different price setting in subclasses (i.e. 1835 Nationalisation)
     */
    // called by:
    // StockRound: buyShares

    // overridden by:
    // StockRound 1835
    protected int getBuyPrice(BuyCertificate action, StockSpace currentSpace) {
        if (action.getCompany().hasStockPrice()) {
            return currentSpace.getPrice();
        } else {
            return action.getCompany().getFixedPrice();
        }
    }

    /**
     * Who receives the cash when a certificate is bought.
     * With incremental capitalization, this can be the company treasure.
     * This method must be called <i>before</i> transferring the certificate.
     *
     * @return The recipient of a buy action price
     */
    // called by:
    // StockRound: buyShares, startCompany

    // overridden by:
    // StockRound 1856
    protected MoneyOwner getSharePriceRecipient(PublicCompany comp,
                                                Owner from, int price) {

        MoneyOwner recipient;
        if (comp.hasFloated()
                && from == ipo.getParent()
                && comp.getCapitalisation() == PublicCompany.CAPITALISE_INCREMENTAL) {
            recipient = comp;
        } else if (from instanceof BankPortfolio) {
            recipient = bank;
        } else {
            recipient = (MoneyOwner) from;
        }
        return recipient;
    }

    /**
     * Make the certificates of one company available for buying
     * by putting these in the IPO.
     *
     * @param company The company to be released.
     */
    // called by:
    // StockRound 1825, 1835: gameSpecificChecks

    // not overridden
    @Deprecated  // Replaced by same method in ReleaseRules
    protected void releaseCompanyShares(PublicCompany company) {
        int share;
        int totalShare = 0;
        String reportText;

        List<PublicCertificate> certsToMove = new ArrayList<>();
        for (PublicCertificate cert : unavailable.getCertificates(company)) {
            if (cert.isInitiallyAvailable()) {
                certsToMove.add(cert);
                share = cert.getShare();
                totalShare += share;
            }
        }
        Portfolio.moveAll(certsToMove, ipo.getParent());
        company.setBuyable(true);

        if (totalShare == 100) {
            reportText = LocalText.getText("SharesReleased",
                    "All", company.getId());
        } else {
            reportText = LocalText.getText("SharesReleased",
                    totalShare + "%", company.getId());
        }
        ReportBuffer.add (this, reportText);
    }

    // called by:
    // StockRound: process
    // StockRound 1880: (sellsShares)

    // overridden by:
    // ShareSellingRound
    // TreasuryShareRound
    // StockRound 1880
    // ShareSellingRound 1880
    public boolean sellShares(SellShares action)
    // NOTE: Don't forget to keep ShareSellingRound.sellShares() in sync
    {

        PortfolioModel portfolio = currentPlayer.getPortfolioModel();
        String playerName = currentPlayer.getId();
        String errMsg = null;
        String companyName = action.getCompanyName();
        PublicCompany company =
                companyManager.getPublicCompany(action.getCompanyName());
        PublicCertificate presCert = null;
        List<PublicCertificate> certsToSell =
                new ArrayList<>();
        Player dumpedPlayer = null;
        int presidentShareNumbersToSell = 0;
        int numberToSell = action.getNumber();
        int shareSizeToSell = action.getShareUnits();

        // Dummy loop to allow a quick jump out
        while (true) {

            // Check everything
            if (checkFirstRoundSellRestriction()) {
                errMsg = LocalText.getText("FirstSRNoSell");
                break;
            }
            if (numberToSell <= 0) {
                errMsg = LocalText.getText("NoSellZero");
                break;
            }

            // May not sell in certain cases
            if (!mayCurrentPlayerSellAnything()) {
                errMsg = LocalText.getText("SoldEnough");
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

            // Find the certificates to sell
            presCert = company.getPresidentsShare();

            if (currentPlayer == company.getPresident()) {
                dumpedPlayer = company.findPlayerToDump();
                if (dumpedPlayer != null) {
                    presidentShareNumbersToSell = PlayerShareUtils.presidentShareNumberToSell(
                            company, currentPlayer, dumpedPlayer, numberToSell + shareSizeToSell - 1);
                    // reduce the numberToSell by the president (partial) sold certificate
                    numberToSell -= presidentShareNumbersToSell;
                    presCert = null;
                }
            }

            log.debug ("SR presSharesToSell={} certsToSell={}", presidentShareNumbersToSell, numberToSell);
            certsToSell = PlayerShareUtils.findCertificatesToSell(company, currentPlayer, numberToSell,
                    shareSizeToSell, dumpedPlayer != null);
            log.debug("SR soldCertificates={}", certsToSell);

            // reduce numberToSell to double check
            for (PublicCertificate c : certsToSell) {
                numberToSell -= c.getShares();
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
        int sharesSold = numberSold * shareSizeToSell;

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
        int cashAmount = sharesSold * price;

        // Save original price as it may be reused in subsequent sale actions in the same turn
        boolean soldBeforeInSameTurn = sellPrices.containsKey(company);
        if (!soldBeforeInSameTurn) {
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
                    sharesSold * company.getShareUnit(),
                    companyName,
                    cashText));
        }

        adjustSharePrice(company, currentPlayer, sharesSold, soldBeforeInSameTurn);

        if (!company.isClosed()) {
            log.debug("certsToSell={}", certsToSell);
            executeShareTransfer(company, certsToSell,
                    dumpedPlayer, presidentShareNumbersToSell);
        }

        // The above does not transfer the presidency
        // if only non-pres. shares have been sold
        company.checkPresidency (dumpedPlayer);

        // Remember that the player has sold this company this round.
        currentPlayer.setSoldThisRound(company);

        if (companyBoughtThisTurnWrapper.value() == null)
            hasSoldThisTurnBeforeBuying.set(true);
        hasActed.set(true);
        setPriority("SellCert");

        // Clear the sellable list
        sellShareActions = null;

        return true;
    }

    // FIXME: Rails 2.x This has to be rewritten to give the new presidency a choice which shares to swap (if he has multiple share certificates)
    // called by:
    // StockRound: executeShareTransfer
    // StockRound 1880: executeShareTransfer
    // ShareSellingRound 1880: executeShareTransfer

    // not overriden
    protected final boolean executeShareTransferTo(PublicCompany company,
                                                List<PublicCertificate> certsToSell, Player dumpedPlayer, int presSharesToSell,
                                                BankPortfolio bankTo) {
        boolean swapped = false;

        // Check if the presidency has changed
        if (dumpedPlayer != null && presSharesToSell > 0) {

            PlayerShareUtils.executePresidentTransferAfterDump(company, dumpedPlayer, bankTo,
                    presSharesToSell);

            ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                    dumpedPlayer.getId(),
                    company.getId()));
            swapped = true;

        }

        // Transfer the sold certificates
        log.debug ("Certs to pool: {}", certsToSell);
        Portfolio.moveAll(certsToSell, bankTo);

        return swapped;
    }

    // called by:
    // StockRound: sellShares
    // ShareSellingRound. sellShares

    // overridden by
    // StockRound 1880
    // ShareSellingRound 1880
    protected boolean executeShareTransfer(PublicCompany company,
                                        List<PublicCertificate> certsToSell,
                                        Player dumpedPlayer, int presSharesToSell) {

        return executeShareTransferTo(company, certsToSell, dumpedPlayer, presSharesToSell, (BankPortfolio) pool.getParent());
    }

    /** Return the current sell price of a given company.
     *  By default, the price of a split sale of shares of the same company
     *  does *not* change between separate actions in the same turn.
     *
     *  Now the one game where this matters because it has different certificate sizes
     *  (1835) also happens to be the one game where the rules explicitly state
     *  that such split sales *do* happen at lowering prices. Split sales are necessary here
     *  because certs of different sizes cannot be sold in the same action.
     *
     *  By default, this rule is now ignored. A new game option has been added to GameOptions.xml
     *  named "SeparateSalesAtSamePrice". The default is true, but it can be changed
     *  via the Options tab at game start.
     *
     * @param company The company
     * @return The current sell price of that company for possible action generation.
     */
    // called by:
    // StockRound: sellShares, setSellableShares
    protected int getCurrentSellPrice(PublicCompany company) {

        int price;

        if (sellPrices.containsKey(company)
                && GameOption.getAsBoolean(this, "SeparateSalesAtSamePrice")) {
            price = (sellPrices.get(company)).getPrice();
        } else {
            price = company.getCurrentSpace().getPrice();
        }
        // stored price is the previous unadjusted price
        price = price / company.getShareUnitsForSharePrice();
        return price;
    }

    // called by:
    // StockRound: sellShares
    // StockRound 1835, 1856, 1880: (adjustSharePrice)
    // ShareSellingRound 1856: (adjustSharePrice)
    // ShareSellingRound: sellShares
    // ShareSellingRound 1880: sellShares

    // overridden by:
    // StockRound 1825, 1835, 1856, 1880, SOH
    // ShareSellingRound 1856
    protected void adjustSharePrice (PublicCompany company, Owner seller, int sharesSold, boolean soldBefore) {

        if (!company.canSharePriceVary()) return;

        stockMarket.sell(company, seller, sharesSold);

        StockSpace newSpace = company.getCurrentSpace();

        if (newSpace.closesCompany() && company.canClose()) {
            company.setClosed();
            ReportBuffer.add(this, LocalText.getText("CompanyClosesAt",
                    company.getId(),
                    newSpace.getId()));
       }
    }

    /* Share price corrections */
    protected void adjustSharePrice (AdjustSharePrice action) {
        PublicCompany company = action.getCompany();
        Player player = action.getPlayer();
        AdjustSharePrice.Direction direction = action.getChosenDirection();
        if (direction != null) {
            switch (direction) {
                case DOWN:
                    stockMarket.sell(company, player, 1);
                    // Can be either down or left, depending on the StockMarket dimension
                    break;
                // The below cases are here for completeness, but are not used yet
                case UP:
                    stockMarket.soldOut(company);
                    // Can be either up or right, depending on the StockMarket dimension
                    break;
                case LEFT:
                    stockMarket.withhold(company); // left
                    break;
                case RIGHT:
                    stockMarket.payOut (company, 1);  // right
                    break;
                default:
                    // Do nothing
            }
        }
    }

    // called by:
    // StockRound: process

    // not overridden
    public boolean useSpecialProperty(UseSpecialProperty action) {

        SpecialProperty sp = action.getSpecialProperty();

        // TODO This should work for all subclasses, but not all have execute()
        // yet.
        if (sp instanceof ExchangeForShare) {

            boolean result = executeExchangeForShare(action, (ExchangeForShare) sp);
            if (result) hasActed.set(true);
            return result;

        } else {
            return false;
        }
    }

    // TODO: Check if this still does work, there is a cast involved now

    // called by:
    // StockRound: useSpecialProperty

    // not overridden
    public boolean executeExchangeForShare(UseSpecialProperty action, ExchangeForShare sp) {

        PublicCompany publicCompany =
                companyManager.getPublicCompany(sp.getPublicCompanyName());
        PrivateCompany privateCompany = (PrivateCompany) sp.getOriginalCompany();
        Owner owner = privateCompany.getOwner();
        Player player = null;
        String errMsg = null;
        boolean ipoHasShare = ipo.getShare(publicCompany) >= sp.getShare();
        boolean poolHasShare = pool.getShare(publicCompany) >= sp.getShare();

        while (true) {

            /* Check if the private is owned by a player */
            if (!(owner instanceof Player)) {
                errMsg =
                        LocalText.getText("PrivateIsNotOwnedByAPlayer",
                                privateCompany.getId());
                break;
            }

            player = (Player) owner;

            /* Check if a share is available */
            if (!ipoHasShare && !poolHasShare) {
                errMsg =
                        LocalText.getText("NoSharesAvailable",
                                publicCompany.getId());
                break;
            }
            /* Check if the player has room for a share of this company */
            if (!checkAgainstHoldLimit(player, publicCompany, 1)) {
                // TODO: Not nice to use '1' here, should be percentage.
                errMsg =
                        LocalText.getText("WouldExceedHoldLimit",
                                String.valueOf(GameDef.getParmAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)));
                break;
            }
            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText(
                    "CannotSwapPrivateForCertificate",
                    player.getId(),
                    privateCompany.getId(),
                    sp.getShare(),
                    publicCompany.getId(),
                    errMsg));
            return false;
        }


        Certificate cert =
                ipoHasShare ? ipo.findCertificate(publicCompany,
                        false) : pool.findCertificate(publicCompany,
                        false);
        cert.moveTo(player);
        ReportBuffer.add(this, LocalText.getText("SwapsPrivateForCertificate",
                player.getId(),
                privateCompany.getId(),
                sp.getShare(),
                publicCompany.getId()));
        sp.setExercised();
        privateCompany.setClosed();

        // Check if the company has floated
        if (!publicCompany.hasFloated()) checkFlotation(publicCompany);

        return true;
    }

    /**
     * A generic method to exchange minor for major certificates.
     * It has been developed for 1837, to handle minor certificates owned by
     * a player going bankrupt, and is currently only used for that purpose,
     * by calling it during a ShareSellingRound.
     *
     * Hopefully this routine can find wider application.
     * Basically it can be called by any StockRound subclass, including
     * the company-type specific exchange rounds, or by the GameManager
     * via such a round.
     * (EV dec 2021)
     *
     * @param minorCertificate The certificate to be exchanged
     * @param major The major company of which a certificate is obtained.
     *              This certificate should be found in the 'reserved' portfolio
     *              (currently still named 'unavailable')
     * @param becomeMajorPresident If true, get the president certificate
     * @return
     */
    public boolean exchangeMinorForNewShare (PublicCertificate minorCertificate,
                                             PublicCompany major,
                                             boolean becomeMajorPresident) {
        return true;
    }

    /**
     * The current Player passes or is done.
     *
     * @param action TODO
     * @param playerName Name of the passing player.
     * @return False if an error is found.
     */
    // called by
    // StockRound: finishTurn, process

    // overridden by
    // StockRound 1837, 18EU
    // TreasuryShareRound
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        if (!playerName.equals(currentPlayer.getId())) {
            DisplayBuffer.add(this, LocalText.getText("WrongPlayer", playerName, currentPlayer.getId()));
            return false;
        }


        if (hasActed.value()) {
            numPasses.set(0);
        } else {
            numPasses.add(1);
            if (hasAutopassed) {
                if (!hasAutopassed(currentPlayer)) {
                    setAutopass(currentPlayer, true);
                    setCanRequestTurn(currentPlayer, true);
                }
                ReportBuffer.add(this, LocalText.getText("Autopasses",
                        currentPlayer.getId()));
            } else {
                ReportBuffer.add(this, LocalText.getText("PASSES",
                        currentPlayer.getId()));
            }
        }

        if (numPasses.value() >= PlayerManager.getNumberOfActivePlayers(this)) {

            finishRound();

        } else {

            finishTurn();

        }
        return true;
    }

    // called by:
    // StockRound: done
    // StockRound 18367, 18EU: (finishRound)

    // overridden by:
    // StockRound 1837, 1880, 18EU
    // NationalFormationRound, PrussianFormationRound

    @Override
    protected void finishRound() {

        ReportBuffer.add(this, " ");
        ReportBuffer.add(this, LocalText.getText("END_SR",
                String.valueOf(getStockRoundNumber())));

        if (raiseIfSoldOut) {
            /* Check if any companies are sold out. */
            for (PublicCompany company : gameManager.getCompaniesInRunningOrder()) {
                if (company.hasStarted() && company.hasStockPrice() && company.isSoldOut()) {
                    ReportBuffer.add(this,LocalText.getText("SoldOut",
                            company.getId()));
                    stockMarket.soldOut(company);
                }
            }
        }

        // reset soldThisRound
        for (Player player : playerManager.getPlayers()) {
            player.resetSoldThisRound();
        }


        super.finishRound();
    }

    // called by:
    // StockRound: process

    // not overridden
    protected boolean requestTurn(RequestTurn action) {

        Player requestingPlayer = playerManager.getPlayerByName(action.getRequestingPlayerName());

        boolean result = canRequestTurn(requestingPlayer);

        if (!result) {
            DisplayBuffer.add(this, LocalText.getText("CannotRequestTurn",
                    requestingPlayer.getId()));
            return false;
        }


        if (hasAutopassed(requestingPlayer)) {
            setAutopass(requestingPlayer, false);
        } else {
            requestTurn(requestingPlayer); // TODO: Check if this still works, replaces requestTurn.add(..)
        }

        return true;
    }


    // called by:
    // StockRound: done
    // StockRound 1837, 18EU: (finishTurn)

    // overridden by:
    // StockRound 1837, 18EU

    protected void finishTurn() {

        setNextPlayer();
        sellPrices.clear();
        if (hasAutopassed(currentPlayer)) {
            if (isPlayerOverLimits(currentPlayer)) {
                // Being over a share/certificate limit undoes an Autopass setting
                setAutopass(currentPlayer, false);
            } else {
                // Process a pass for a player that has set Autopass
                done(null, currentPlayer.getId(), true);
            }
        }
    }

    /**
     * Internal method: pass the turn to the next player.
     */

    // called by
    // StockRound: finishTurn
    // NationalFormationRound, 1835 PrussianFormationRound: findNextMergingPlayer
    // 1837FinalCoalExchangeRound: setMinorMergeActions
    // 18EUFinalMInorExchangeRound: setMinorMergeActions

    // not overridden
    protected void setNextPlayer() {

        getRoot().getPlayerManager().setCurrentToNextPlayer();
        initPlayer();
    }

    // called by
    // StockRound: setNextPlayer, start
    // StockRound 1856: (initPlayer)

    // overridden by:
    // StockRound 1837, 1856, 18EU
    // FinalCoalExchangeRound
    // FinalMinorExchangeRound
    protected void initPlayer() {

        currentPlayer = playerManager.getCurrentPlayer();
        companyBoughtThisTurnWrapper.set(null);
        hasSoldThisTurnBeforeBuying.set(false);
        hasActed.set(false);
        sellShareActions = null;
        sellPrices.clear();
        lastSoldCompany = null;
        if (currentPlayer == startingPlayer) ReportBuffer.add(this, "");
    }

    /**
     * Remember the player that has the Priority Deal. <b>Must be called BEFORE
     * setNextPlayer()!</b>
     *
     * @param string Used in subclasses to be checked for a certain value
     */
    // called by
    // StockRound: buyShares, sellShares, startCompany
    // StockRound 18EU: mergeCompanies, startCompany
    // StockRound 1837: mergeCompanies

    // To be overridden in 1825, 1829,1835 (done), 1847, 1881, 18Africa
    protected void setPriority(String string) {
        //Standard: All actions change Priority but not in
        //1825, 1829, 1835, 1847, 1881, 18Africa Each player
        //consecutively not making a purchase. The priority then
        //goes to the player after the one who last made a purchase.
        getRoot().getPlayerManager().setPriorityPlayerToNext();
    }

    // called by
    // Stockround 1837, 18EU: finishRound, finishTurn
    // NationalFormationRound, 1835PrussianFormationRound: setPossibleActions, start

    // not overridden
    // @Deprecated   // Why???
    public void setCurrentPlayer(Player player) {
        getRoot().getPlayerManager().setCurrentPlayer(player);
        currentPlayer = player;
    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    /**
     * @return The index of the player that has the turn.
     */
    // called by
    // ShareSellingRound 1880: sellShares

    // not overridden
    public int getCurrentPlayerIndex() {
        return currentPlayer.getIndex();
    }

    /**
     * @return true if first round sell restriction is active
     */
    // called by
    // StockRound: mayCurrentPlayerSellAnything, sellShares

    // not overridden
    private boolean checkFirstRoundSellRestriction() {
        if (noSaleInFirstSR() && getStockRoundNumber() == 1) {
            // depending on GameOption restriction is either valid during the first (true) Stock Round or the first Round
            if ( "First Stock Round".equals(GameOption.getValue(this, "FirstRoundSellRestriction"))) {
                return true;
            } else if ( "First Round".equals(GameOption.getValue(this, "FirstRoundSellRestriction"))) {
                // if all players have passed it is not the first round
                return !gameManager.getFirstAllPlayersPassed();
            }
        }
        return false;
    }

    /**
     * Can the current player do any selling?
     *
     * @return True if any selling is allowed.
     */
    // called by
    // StockRound: sellShares, sellSellableShares

    // overridden by
    // ShareSellingRound
    // TreasuryShareRound
    public boolean mayCurrentPlayerSellAnything() {

        if (checkFirstRoundSellRestriction()) {
            return false;
        }

        if (companyBoughtThisTurnWrapper.value() != null
                && (sequenceRule == SELL_BUY_OR_BUY_SELL
                && hasSoldThisTurnBeforeBuying.value() || sequenceRule == SELL_BUY)) {
            return false;
        }
        return true;
    }


    // called by
    // StockRound: sellShares, setSellableShares
    // StockRound 1880: (mayPlayerSellShareOfCompany), sellShares
    // ShareSellingRound: getSellableShares

    // overridden by
    // StockRound 1880
    public boolean mayPlayerSellShareOfCompany(PublicCompany company) {

        // Can't sell shares that have no price
        if (!company.hasStarted() || !company.hasStockPrice()) return false;

        // In some games, can't sell shares if not operated
        if (noSaleIfNotOperated()
                && !company.hasOperated()) return false;

        // In SOH, can't sell shares of company started this round
        if (noSaleIfJustStarted()
                && startedThisRound.contains(company)) return false;

        return true;
    }


    /**
     * Can the current player do any buying?
     * <p>Note: requires sellable shares to be checked BEFORE buyable shares
     *
     * @return True if any buying is allowed.
     */
    // called by
    // StockRound: setBuyableCerts
    // StockRound 1880, 18EU: setBuyableCerts
    // StockRound 1837, 18EU: setGameSpecificActions

    // overridden by
    // ShareSellingRound
    // TreasuryShareRound
    public boolean mayCurrentPlayerBuyAnything() {
        return !isOverLimits && companyBoughtThisTurnWrapper.value() == null;
    }

    // Only used now to check if Autopass must be reset.
    // called by
    // StockRound: finishTurn

    // not overridden
    protected boolean isPlayerOverLimits(Player player) {

        // Over the total certificate hold Limit?
        if (player.getPortfolioModel().getCertificateCount() > gameManager.getPlayerCertificateLimit(player)) {
            return true;
        }

        // Over the hold limit of any company?
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (company.hasStarted() && company.hasStockPrice()
                    && !checkAgainstHoldLimit(player, company, 0)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a player may buy the given number of certificates.
     *
     * @param number Number of certificates to buy (usually 1 but not always
     *               so).
     * @return True if it is allowed.
     */
    // called by
    // StockRound: buyShares, setBuyableCerts, startCompany
    // StockRound 1835, 1880, 18EU: setBuyableCerts
    // StockRound 18EU: startCompany

    // not overridden
    public boolean mayPlayerBuyCertificate(Player player, PublicCompany comp, float number) {
        if (comp.hasFloated()
                && comp.hasStockPrice() && comp.getCurrentSpace().isNoCertLimit())
            return true;
        if (player.getPortfolioModel().getCertificateCount() + number
                > gameManager.getPlayerCertificateLimit(player))
            return false;
        return true;
    }

    /**
     * Check if a player may buy the given number of shares from a given
     * company, given the "hold limit" per company, that is the percentage of
     * shares of one company that a player may hold (typically 60%).
     *
     * @param player  the buying player
     * @param company The company from which to buy
     * @param number  The number of shares (usually 1 but not always so)
     * @return True if it is allowed.
     */
    // called by
    // StockRound: buyShares, executeExchangeForShare, isPlayerOverLimits, setBuyableCerts, setSellableShares
    // StockRound 18EU, 1880: setBuyableCerts

    // overriden by:
    // StockRound 1835
    public boolean checkAgainstHoldLimit(Player player, PublicCompany company,
                                         int number) {
        // Check for per-company share limit
        if (player.getPortfolioModel().getShare(company)
                        + number * company.getShareUnit()
                    > GameDef.getParmAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)
                && company.hasStockPrice()
                && !company.getCurrentSpace().isNoHoldLimit()
                && !isSellObligationLifted(company)) return false;
        return true;
    }

    /**
     * Return the number of <i>additional</i> shares of a certain company and
     * of a certain size that a player may buy, given the share "hold limit" per
     * company, that is the percentage of shares of one company that a player
     * may hold (typically 60%). <p>If no hold limit applies, it is taken to be
     * 100%.
     *
     * @param company The company from which to buy
     * @param shareSize  The share unit (typically 10%).
     * @return The maximum number of such shares that would not let the player
     * overrun the per-company share hold limit.
     */
    // called by
    // StockRound setBuyableCerts
    // StockRound 1880, 18EU: setBuyableCerts
    public int maxAllowedNumberOfSharesToBuy(Player player,
                                             PublicCompany company,
                                             int shareSize) {

        int limit;
        int playerShareLimit = GameDef.getParmAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT);
        if (company.getPlayerShareLimit() > -1) {
            // Used for the 1837 minors, where we ignore the 50% treasury share,
            // that the rules imply but not say.
            // Adding that would turn the revenue 'split' into a 'full payout'.
            limit = company.getPlayerShareLimit();
        } else if (!company.hasStarted() || company.isHibernating()) {
            limit = playerShareLimit;
        } else {
            limit = (company.hasStockPrice() && company.getCurrentSpace().isNoHoldLimit())
                    ? 100
                    : playerShareLimit;
        }
        int maxAllowed = (limit - player.getPortfolioModel().getShare(company)) / shareSize;
        //log.debug("MaxAllowedNumberOfSharesToBuy = " + maxAllowed + " for company =  " + company + " shareSize " + shareSize);
        return maxAllowed;
    }


    // called by
    // StockRound: checkFirstRoundSellRestriction

    // not overridden
    protected boolean noSaleInFirstSR() {
        return (Boolean) gameManager.getGameParameter(GameDef.Parm.NO_SALE_IN_FIRST_SR);
    }


    // called by
    // StockRound: mayPlayerSellShareOfCompany

    // not overridden
    protected boolean noSaleIfNotOperated() {
        return (Boolean) gameManager.getGameParameter(GameDef.Parm.NO_SALE_IF_NOT_OPERATED);
    }

    protected boolean noSaleIfJustStarted() {
        return (Boolean) gameManager.getGameParameter(GameDef.Parm.NO_SALE_OF_JUST_STARTED_COMPANY);
    }

    // called by
    // 1835PrussianFormationRound: finishRound
    // GameManager: processOnReload
    // GameUIManager: initSaveSettings, saveGame

    // not overridden
    @Override
    public String getRoundName() {
        return "StockRound " + getStockRoundNumber();
    }

    // Called by
    // StockRound: checkAgainstHoldLimit

    // not overridden
    public boolean isSellObligationLifted(PublicCompany company) {
        return sellObligationLifted != null
                && sellObligationLifted.contains(company);
    }

    // Called by
    // 18EU, 1837 StockRound: mergeCompanies

    // not overridden
    public void setSellObligationLifted(PublicCompany company) {
        if (sellObligationLifted == null) {
            sellObligationLifted = HashSetState.create(this, "sellObligationLifted");
        }
        sellObligationLifted.add(company);
    }

    public boolean requestTurn(Player player) {
        if (canRequestTurn(player)) {
            if (!hasRequestedTurn.contains(player)) hasRequestedTurn.add(player);
            return true;
        }
        return false;
    }

    public boolean canRequestTurn(Player player) {
        return canRequestTurn.contains(player);
    }

    public void setCanRequestTurn(Player player, boolean value) {
        if (value && !canRequestTurn.contains(player)) {
            canRequestTurn.add(player);
        } else if (!value && canRequestTurn.contains(player)) {
            canRequestTurn.remove(player);
        }
    }

    public void setAutopass(Player player, boolean value) {
        if (value && !autopasses.contains(player)) {
            autopasses.add(player);
        } else if (!value && autopasses.contains(player)) {
            autopasses.remove(player);
        }
    }

    public boolean hasAutopassed(Player player) {
        return autopasses.contains(player);
    }

    public List<Player> getAutopasses() {
        return autopasses.view();
    }

}
