package net.sf.rails.game.specific._18EU;

import java.util.*;

import rails.game.action.*;
import rails.game.specific._18EU.StartCompany_18EU;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Portfolio;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;


/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class StockRound_18EU extends StockRound {
    protected final ArrayListState<PublicCompany> compWithExcessTrains =
            ArrayListState.create(this, "compWithExcessTrains");
    protected final IntegerState discardingCompanyIndex = IntegerState.create(this, "discardingCompanyIndex");
    protected final BooleanState discardingTrains = BooleanState.create(this, "discardingTrains");

    // should this not be a state variable ?
    protected boolean phase5Reached = false;
    // when is this created, should it not be a state variable?
    protected PublicCompany[] discardingCompanies;

    /**
     * Constructed via Configure
     */
    public StockRound_18EU (GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    public void start() {
        super.start();
        // Is this really required? Can it set to true at the start?
        if (discardingTrains.value()) {
            discardingTrains.set(false);
        }
        
        // if it is done this way, should it not be a state variable?
        phase5Reached = getRoot().getPhaseManager().hasReachedPhase("5");

    }

    @Override
    public boolean setPossibleActions() {
        // discardingTrains during stockRounds, when does this happen?
        if (discardingTrains.value()) {
            return setTrainDiscardActions();
        } else {
            return super.setPossibleActions();
        }
    }

    /**
     * Create a list of certificates that a player may buy in a Stock Round,
     * taking all rules into account.
     *
     * @return List of buyable certificates.
     */
    @Override
    // changes: 18EU only allows to start a company with a merged minor (until phase 5)
    // requires: a StartCompany18EUActivity
    public void setBuyableCerts() {
        if (!mayCurrentPlayerBuyAnything()) return;

        ImmutableSet<PublicCertificate> certs;
        PublicCertificate cert;
        StockSpace stockSpace;
        PortfolioModel from;
        int price;

        // 18EU special: until phase 5, we can only
        // start a company by trading in a Minor
        boolean mustMergeMinor = !phase5Reached;
        List<PublicCompany> minors = null;
        List<Stop> freeStops = null;
        if (mustMergeMinor) {
            minors = new ArrayList<PublicCompany>();
            for (PublicCertificate c : playerManager.getCurrentPlayer().getPortfolioModel().getCertificates()) {
                if (c.getCompany().getType().getId().equalsIgnoreCase("Minor")) {
                    minors.add(c.getCompany());
                }
            }
        } else {
            freeStops = new ArrayList<Stop>();
            MapManager map = getRoot().getMapManager();
            for (Stop stop : map.getCurrentStops()) {
                if (stop.hasTokenSlotsLeft()) {
                    freeStops.add(stop);
                }
            }
        }

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
                // if (certs.isEmpty()) continue; // TODO: Check if this removal is correct

                /* Only the top certificate is buyable from the IPO */
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

                comp = cert.getCompany();
                if (currentPlayer.hasSoldThisRound(comp)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        cert.getShare()) < 1) continue;
                shares = cert.getShares();

                if (!comp.hasStarted()) {
                    if (mustMergeMinor) {
                        if (minors.isEmpty()) continue;
                    } else {
                        if (freeStops.isEmpty()) continue;
                    }

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
                        StartCompany_18EU action =
                                new StartCompany_18EU(comp, prices);
                        if (mustMergeMinor) {
                            action.setMinorsToMerge(minors);
                        } else {
                            action.setAvailableHomeStations(freeStops);
                        }
                        possibleActions.add(action);
                    }
                } else if (comp.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(comp, cert.getShare(),
                            from.getParent(),
                            comp.getMarketPrice()));
                }

            }
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = pool;
        ImmutableSetMultimap<PublicCompany, PublicCertificate> map =
                from.getCertsPerCompanyMap();

        for (PublicCompany comp : map.keySet()) {
            certs = map.get(comp);
            // if (certs.isEmpty()) continue; // TODO: Check if this removal is correct
            cert = Iterables.get(certs, 0);
            if (currentPlayer.hasSoldThisRound(comp)) continue;
            if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                    cert.getShare()) < 1) continue;
            price = comp.getMarketPrice();

            if (companyBoughtThisTurn != null) {
                continue;
            }

            // Does the player have enough cash?
            if (playerCash < price) continue;

            possibleActions.add(new BuyCertificate(comp, cert.getShare(), from.getParent(), price, 1));
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompany company : companyManager.getAllPublicCompanies()) {
                certs = company.getPortfolioModel().getCertificates(company);
                if (certs.isEmpty()) continue;
                cert = Iterables.get(certs, 0);
                if (currentPlayer.hasSoldThisRound(company)) continue;
                if (!checkAgainstHoldLimit(currentPlayer, company, 1)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, company,
                        cert.getShare()) < 1) continue;
                stockSpace = company.getCurrentSpace();
                if (!stockSpace.isNoCertLimit()
                    && !mayPlayerBuyCertificate(currentPlayer, company, 1)) continue;
                if (company.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(company, cert.getShare(),
                            company,
                            company.getMarketPrice()));
                }
            }
        }
    }

    /**
     * An 18EU extension to StockRound.setSellableShares() that adds any
     * mergeable Minor companies.
     */
    // changes: it allows to merge minors into accepting majors
    // requires: a specific MergeCompanyActivity
    @Override
    protected void setGameSpecificActions() {
        if (!mayCurrentPlayerBuyAnything()) return;

        List<PublicCompany> comps =
                companyManager.getAllPublicCompanies();
        List<PublicCompany> minors = new ArrayList<PublicCompany>();
        List<PublicCompany> targetCompanies = new ArrayList<PublicCompany>();
        String type;

        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
            if (type.equals("Major") && comp.hasStarted()
                && !comp.hasOperated()) {
                targetCompanies.add(comp);
            } else if (type.equals("Minor")
                       && comp.getPresident() == currentPlayer) {
                minors.add(comp);
            }
        }
        if (minors.isEmpty() || targetCompanies.isEmpty()) return;

        for (PublicCompany minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies, false));
        }
    }

    // called from setPossibleActions in StockRound_18EU and FinalMinorExchangeRound
    protected boolean setTrainDiscardActions() {

        PublicCompany discardingCompany =
                discardingCompanies[discardingCompanyIndex.value()];
        log.debug("Company " + discardingCompany.getId()
                  + " to discard a train");
        possibleActions.add(new DiscardTrain(discardingCompany,
                discardingCompany.getPortfolioModel().getUniqueTrains()));
        // We handle one train at at time.
        // We come back here until all excess trains have been discarded.
        return true;
    }

    /**
     * Start a company by buying one or more shares (more applies to e.g. 1841)
     *
     * @param player The player that wants to start a company.
     * @param company The company to start.
     * @param price The start (par) price (ignored if the price is fixed).
     * @param shares The number of shares to buy (can be more than 1 in e.g.
     * 1841).
     * @return True if the company could be started. False indicates an error.
     */
    @Override
    // changes: substantial changes to the usual startCompany behavior
    // requires: an own 18EU StartCompany Activity
    public boolean startCompany(String playerName, StartCompany action) {
        PublicCompany company = action.getCompany();
        int price = action.getPrice();
        int shares = action.getNumberBought();

        String errMsg = null;
        StockSpace startSpace = null;
        int numberOfCertsToBuy = 0;
        PublicCertificate cert = null;
        String companyName = company.getId();
        PublicCompany minor = null;
        StartCompany_18EU startAction = null;
        Stop selectedHomeCity = null;

        currentPlayer = playerManager.getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {
            if (!(action instanceof StartCompany_18EU)) {
                errMsg = LocalText.getText("InvalidAction");
                break;
            }
            startAction = (StartCompany_18EU) action;

            // The player may not have bought this turn.
            if (companyBoughtThisTurnWrapper.value() != null) {
                errMsg = LocalText.getText("AlreadyBought", playerName);
                break;
            }

            // Check company
            company = companyManager.getPublicCompany(companyName);
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

            // The given price must be a valid start price
            if ((startSpace = stockMarket.getStartSpace(price)) == null) {
                errMsg =
                        LocalText.getText("InvalidStartPrice",
                                Bank.format(this, price),
                                company.getId() );
                break;
            }

            // Check if the Player has the money.
            if (currentPlayer.getCashValue() < shares * price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            if (!phase5Reached) {
                // Check if the player owns the merged minor
                minor = startAction.getChosenMinor();
                if (minor != null
                    && currentPlayer.getPortfolioModel().getCertificates(
                            minor) == null) {
                    errMsg =
                            LocalText.getText("PlayerDoesNotOwn",
                                    currentPlayer.getId(),
                                    minor.getId() );
                    break;
                }
            } else {
                // Check if a valid home base has been selected
                selectedHomeCity = startAction.getSelectedHomeStation();
                if (selectedHomeCity.getSlots() <= selectedHomeCity.getBaseTokens().size()) {
                    errMsg =
                            LocalText.getText("InvalidHomeBase",
                                    selectedHomeCity.toString(),
                                    company.getId() );
                    break;
                }

            }
            numberOfCertsToBuy++;

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CantStart",
                    playerName,
                    companyName,
                    Bank.format(this, price),
                    errMsg ));
            return false;
        }

        

        // All is OK, now start the company
        MapHex homeHex = null;
        int homeCityNumber = 1;
        if (minor != null) {
            homeHex = minor.getHomeHexes().get(0);
            homeCityNumber = homeHex.getStopOfBaseToken(minor).getRelatedNumber();
        } else if (selectedHomeCity != null) {
            homeHex = selectedHomeCity.getParent();
            homeCityNumber = selectedHomeCity.getRelatedNumber();
            //Bugfix for Error reported by John Galt- Mar 31 2012 ; Martin Brumm
            //The maphex needs to have the homes map set with the company value.
            homeHex.addHome(company, selectedHomeCity);
        }
        company.setHomeHex(homeHex);
        company.setHomeCityNumber(homeCityNumber);

        company.start(startSpace);
        ReportBuffer.add(this, LocalText.getText("START_COMPANY_LOG",
                playerName,
                companyName,
                Bank.format(this, price),
                Bank.format(this, shares * price),
                shares,
                cert.getShare(),
                company.getId() ));

        // Transfer the President's certificate
        cert.moveTo(currentPlayer);
        Currency.wire(currentPlayer, shares * price, company);

        if (minor != null) {
            // Get the extra certificate for the minor, for free
            PublicCertificate cert2 = ipo.findCertificate(company, false);
            cert2.moveTo(currentPlayer);
            // Transfer the minor assets into the started company
            int minorCash = minor.getCash();
            int minorTrains = minor.getPortfolioModel().getTrainList().size();
            company.transferAssetsFrom(minor);
            minor.setClosed();
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    company.getId(),
                    Bank.format(this, minorCash),
                    minorTrains ));
            ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                    currentPlayer.getId(),
                    cert2.getShare(),
                    company.getId(),
                    ipo.getParent().getId(),
                    minor.getId() ));
        } else {
            ReportBuffer.add(this, LocalText.getText("SelectedHomeBase",
                    company.getId(),
                    selectedHomeCity.toText() ));
        }

        // Move the remaining certificates to the company treasury
        Portfolio.moveAll(ipo.getCertificates(company), company);

        ReportBuffer.add(this, LocalText.getText("SharesPutInTreasury",
                company.getPortfolioModel().getShare(company),
                company.getId() ));

        // TODO must get this amount from XML
        int tokensCost = 100;
        String costText = Currency.toBank(company, tokensCost);
        ReportBuffer.add(this, LocalText.getText("PaysForTokens",
                company.getId(),
                costText,
                company.getNumberOfBaseTokens() ));

        companyBoughtThisTurnWrapper.set(company);
        hasActed.set(true);
        setPriority();

        return true;
    }

    @Override
    protected boolean processGameSpecificAction(PossibleAction action) {

        log.debug("GameSpecificAction: " + action.toString());

        boolean result = false;

        if (action instanceof MergeCompanies) {

            result = mergeCompanies((MergeCompanies) action);

        } else if (action instanceof DiscardTrain) {

            result = discardTrain((DiscardTrain) action);
        }

        return result;
    }

    /**
     * Merge a minor into an already started company. <p>Also covers the
     * actions of the Final Minor Exchange Round, in which minors can also be
     * closed (in that case, the MergeCompanies.major attribute is null, which
     * never occurs in normal stock rounds).
     *
     * @param action
     * @return
     */
    // changes: this is a game specific action
    // requires: an own MergeCompany Activity
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();
        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank
        Train pullmannToDiscard = null;

        // TODO Validation to be added?

        

        if (major != null) {
            cert = major.getPortfolioModel().findCertificate(major, false);
            if (cert != null) {
                // Assets go to the major company.
                cashDestination = major;
            } else {
                cert = pool.findCertificate(major, false);
                // If null, player gets nothing in return
            }
        }

        // Transfer the minor assets
        int minorCash = minor.getCash();
        int minorTrains = minor.getPortfolioModel().getTrainList().size();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) {
                Currency.toBankAll(minor);
            }
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);

            // Check for multiple Pullmanns
            boolean hasPullmann = false;
            for (Train train : major.getPortfolioModel().getTrainList()) {
                if (train.toText().equalsIgnoreCase("P")) {
                    if (!hasPullmann) {
                        hasPullmann = true;
                    } else {
                        pullmannToDiscard = train; // Can only have two Pullmanns.
                    }
                }
            }
        }

        MapHex homeHex = minor.getHomeHexes().get(0);
        Stop homeStop  = homeHex.getStopOfBaseToken(minor);
        minor.setClosed();

        if (major != null && action.getReplaceToken()) {
            if (homeHex.layBaseToken(major, homeStop)) {
                major.layBaseToken(homeHex, 0);
            }   
        }

        if (major != null) {
            if (major.getNumberOfTrains() > major.getCurrentTrainLimit()
                && !compWithExcessTrains.contains(major)) {
                compWithExcessTrains.add(major);
            }
        }

        if (cert != null) {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    major.getId(),
                    Bank.format(this, minorCash),
                    minorTrains ));
            // FIXME: CHeck if this still works correctly
            ReportBuffer.add(this, LocalText.getText("GetShareForMinor",
                    currentPlayer.getId(),
                    cert.getShare(),
                    major.getId(),
                    cert.getOwner().getId(),
                    minor.getId() ));
            if (major != null) {
                if (action.getReplaceToken()) {
                    ReportBuffer.add(this, LocalText.getText("ExchangesBaseToken",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                } else {
                    ReportBuffer.add(this, LocalText.getText("NoBaseTokenExchange",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                }
            }
            cert.moveTo(currentPlayer);
            ReportBuffer.add(this, LocalText.getText("MinorCloses", minor.getId()));
            checkFlotation(major);

            if (pullmannToDiscard != null) {
                pullmannToDiscard.discard();
            }
        } else {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("CLOSE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    Bank.format(this, minorCash),
                    minorTrains ));
        }
        hasActed.set(true);

        if (!(this instanceof FinalMinorExchangeRound)) {
            companyBoughtThisTurnWrapper.set(major);

            // If >60% shares owned, lift sell obligation this round.
            if (currentPlayer.getPortfolioModel().getShare(major)
            		> GameDef.getGameParameterAsInt(this, GameDef.Parm.PLAYER_SHARE_LIMIT)) {
            	setSellObligationLifted (major);
            }

            setPriority();
        }

        return true;
    }

    @Override
    // changes: this changes the floation behavior
    // requires: move this to PublicCompany, potentially add a FloatCompanyStrategy
    protected void floatCompany(PublicCompany company) {

        company.setFloated();
        ReportBuffer.add(this, LocalText.getText("Floats", company.getId()));

        // Before phase 5, no other actions are required.

        if (phase5Reached) {
            // Put the remaining 5 shares in the pool,
            // getting cash in return
            // Move the remaining certificates to the company treasury
            company.getPortfolioModel().moveAllCertificates(pool.getParent());
            int cash = 5 * company.getMarketPrice();
            String cashText = Currency.fromBank(cash, company);
            ReportBuffer.add(this, LocalText.getText("MonetiseTreasuryShares",
                    company.getId(),
                    cashText ));

        }
    }

    // change: discardTrain action are usually outside of StockRounds
    // requires: think about triggered activities?
    public boolean discardTrain(DiscardTrain action) {

        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();
        String companyName = company.getId();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (!discardingTrains.value()) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (!company.getPortfolioModel().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                company.getId(),
                                train.toText() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.toText(),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        train.discard();

        finishTurn();

        return true;
    }

    @Override
    // change: discardTrain action are usually outside of StockRounds
    // requires: think about triggered activities?
    protected void finishTurn() {

        if (!discardingTrains.value()) {
            super.finishTurn();
        } else {
            PublicCompany comp =
                    discardingCompanies[discardingCompanyIndex.value()];
            if (comp.getNumberOfTrains() <= comp.getCurrentTrainLimit()) {
                discardingCompanyIndex.add(1);
                if (discardingCompanyIndex.value() >= discardingCompanies.length) {
                    // All excess trains have been discarded
                    finishRound();
                    return;
                }
            }
            PublicCompany discardingCompany =
                    discardingCompanies[discardingCompanyIndex.value()];
            setCurrentPlayer(discardingCompany.getPresident());
        }
    }

    @Override
    // change: discardTrain action are usually outside of StockRounds
    // requires: think about triggered activities?
    protected void finishRound() {

        if (discardingTrains.value()) {

            super.finishRound();

        } else if (!compWithExcessTrains.isEmpty()) {

            discardingTrains.set(true);

            // Make up a list of train discarding companies in operating sequence.
            PublicCompany[] operatingCompanies = setOperatingCompanies().toArray(new PublicCompany[0]);
            discardingCompanies =
                    new PublicCompany[compWithExcessTrains.size()];
            for (int i = 0, j = 0; i < operatingCompanies.length; i++) {
                if (compWithExcessTrains.contains(operatingCompanies[i])) {
                    discardingCompanies[j++] = operatingCompanies[i];
                }
            }

            discardingCompanyIndex.set(0);
            PublicCompany discardingCompany =
                    discardingCompanies[discardingCompanyIndex.value()];
            setCurrentPlayer(discardingCompany.getPresident());

        } else {

            super.finishRound();
        }
    }

    // TODO: Is this required somwhere?
//    @Override
//    public String toString() {
//        return "StockRound_18EU " + getStockRoundNumber();
//    }
}
