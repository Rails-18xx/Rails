package rails.game.specific._18EU;

import java.util.*;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Iterables;

import rails.common.DisplayBuffer;
import rails.common.LocalText;
import rails.game.*;
import rails.game.action.*;
import rails.game.model.CashOwner;
import rails.game.model.MoneyModel;
import rails.game.model.PortfolioModel;
import rails.game.state.ArrayListState;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;

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

    protected boolean phase5Reached = false;
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
        if (discardingTrains.value()) {
            discardingTrains.set(false);
        }

        phase5Reached = gameManager.getPhaseManager().hasReachedPhase("5");

    }

    @Override
    public boolean setPossibleActions() {
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
        List<Stop> freeStations = null;
        if (mustMergeMinor) {
            minors = new ArrayList<PublicCompany>();
            for (PublicCertificate c : getCurrentPlayer().getPortfolioModel().getCertificates()) {
                if (c.getCompany().getType().getId().equalsIgnoreCase("Minor")) {
                    minors.add(c.getCompany());
                }
            }
        } else {
            freeStations = new ArrayList<Stop>();
            MapManager map = gameManager.getMapManager();
            for (Stop city : map.getCurrentStations()) {
                if (city.getSlots() > city.getBaseTokens().size()) {
                    freeStations.add(city);
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
                if (isSaleRecorded(currentPlayer, comp)) continue;
                if (maxAllowedNumberOfSharesToBuy(currentPlayer, comp,
                        cert.getShare()) < 1) continue;
                shares = cert.getShares();

                if (!comp.hasStarted()) {
                    if (mustMergeMinor) {
                        if (minors.isEmpty()) continue;
                    } else {
                        if (freeStations.isEmpty()) continue;
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
                            action.setAvailableHomeStations(freeStations);
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
            if (isSaleRecorded(currentPlayer, comp)) continue;
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
                if (isSaleRecorded(currentPlayer, company)) continue;
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
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }
    }

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

        currentPlayer = getCurrentPlayer();

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
                                Bank.format(price),
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
            DisplayBuffer.add(LocalText.getText("CantStart",
                    playerName,
                    companyName,
                    Bank.format(price),
                    errMsg ));
            return false;
        }

        getRoot().getChangeStack().newChangeSet(action);

        // All is OK, now start the company
        MapHex homeHex = null;
        int homeCityNumber = 1;
        if (minor != null) {
            homeHex = minor.getHomeHexes().get(0);
            homeCityNumber = homeHex.getCityOfBaseToken(minor);
        } else if (selectedHomeCity != null) {
            homeHex = selectedHomeCity.getHolder();
            homeCityNumber = selectedHomeCity.getNumber();
        }
        company.setHomeHex(homeHex);
        company.setHomeCityNumber(homeCityNumber);

        company.start(startSpace);
        ReportBuffer.add(LocalText.getText("START_COMPANY_LOG",
                playerName,
                companyName,
                Bank.format(price),
                Bank.format(shares * price),
                shares,
                cert.getShare(),
                company.getId() ));

        // Transfer the President's certificate
        cert.moveTo(currentPlayer);
        MoneyModel.cashMove(currentPlayer, company, shares * price);

        if (minor != null) {
            // Get the extra certificate for the minor, for free
            PublicCertificate cert2 = ipo.findCertificate(company, false);
            cert2.moveTo(currentPlayer);
            // Transfer the minor assets into the started company
            int minorCash = minor.getCash();
            int minorTrains = minor.getPortfolioModel().getTrainList().size();
            company.transferAssetsFrom(minor);
            minor.setClosed();
            ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    company.getId(),
                    Bank.format(minorCash),
                    minorTrains ));
            ReportBuffer.add(LocalText.getText("GetShareForMinor",
                    currentPlayer.getId(),
                    cert2.getShare(),
                    company.getId(),
                    ipo.getId(),
                    minor.getId() ));
        } else {
            ReportBuffer.add(LocalText.getText("SelectedHomeBase",
                    company.getId(),
                    selectedHomeCity.toString() ));
        }

        // Move the remaining certificates to the company treasury
        // FIXME: This has to be only those certificates that relate to that public company 
        // Owners.moveAll(ipo, company, PublicCertficate.class);

        ReportBuffer.add(LocalText.getText("SharesPutInTreasury",
                company.getPortfolioModel().getShare(company),
                company.getId() ));

        // TODO must get this amount from XML
        int tokensCost = 100;
        MoneyModel.cashMove(company, bank, tokensCost);
        ReportBuffer.add(LocalText.getText("PaysForTokens",
                company.getId(),
                Bank.format(100),
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
    protected boolean mergeCompanies(MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();
        PublicCertificate cert = null;
        CashOwner cashDestination = null; // Bank
        Train pullmannToDiscard = null;

        // TODO Validation to be added?

        getRoot().getChangeStack().newChangeSet(action);

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
            if (minorCash > 0) MoneyModel.cashMove(minor, bank, minorCash);
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);

            // Check for multiple Pullmanns
            boolean hasPullmann = false;
            for (Train train : major.getPortfolioModel().getTrainList()) {
                if (train.getId().equalsIgnoreCase("P")) {
                    if (!hasPullmann) {
                        hasPullmann = true;
                    } else {
                        pullmannToDiscard = train; // Can only have two Pullmanns.
                    }
                }
            }
        }

        MapHex homeHex = minor.getHomeHexes().get(0);
        int homeCityNumber = homeHex.getCityOfBaseToken(minor);
        minor.setClosed();

        if (major != null && action.getReplaceToken()) {
            if (homeHex.layBaseToken(major, homeCityNumber)) {
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
            ReportBuffer.add("");
            ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    major.getId(),
                    Bank.format(minorCash),
                    minorTrains ));
            // FIXME: CHeck if this still works correctly
            ReportBuffer.add(LocalText.getText("GetShareForMinor",
                    currentPlayer.getId(),
                    cert.getShare(),
                    major.getId(),
                    cert.getOwner().getId(),
                    minor.getId() ));
            if (major != null) {
                if (action.getReplaceToken()) {
                    ReportBuffer.add(LocalText.getText("ExchangesBaseToken",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                } else {
                    ReportBuffer.add(LocalText.getText("NoBaseTokenExchange",
                            major.getId(),
                            minor.getId(),
                            homeHex.getId()));
                }
            }
            cert.moveTo(currentPlayer);
            ReportBuffer.add(LocalText.getText("MinorCloses", minor.getId()));
            checkFlotation(major);

            if (pullmannToDiscard != null) {
                pool.addTrain(pullmannToDiscard);
                ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                        major.getId(),
                        pullmannToDiscard.getId() ));
            }
        } else {
            ReportBuffer.add("");
            ReportBuffer.add(LocalText.getText("CLOSE_MINOR_LOG",
                    currentPlayer.getId(),
                    minor.getId(),
                    Bank.format(minorCash),
                    minorTrains ));
        }
        hasActed.set(true);

        if (!(this instanceof FinalMinorExchangeRound)) {
            companyBoughtThisTurnWrapper.set(major);

            // If >60% shares owned, lift sell obligation this round.
            if (currentPlayer.getPortfolioModel().getShare(major)
            		> getGameParameterAsInt(GameDef.Parm.PLAYER_SHARE_LIMIT)) {
            	setSellObligationLifted (major);
            }

            setPriority();
        }

        return true;
    }

    @Override
    protected void floatCompany(PublicCompany company) {

        company.setFloated();
        ReportBuffer.add(LocalText.getText("Floats", company.getId()));

        // Before phase 5, no other actions are required.

        if (phase5Reached) {
            // Put the remaining 5 shares in the pool,
            // getting cash in return
            // Move the remaining certificates to the company treasury
            company.getPortfolioModel().moveAllCertificates(pool.getParent());
            int cash = 5 * company.getMarketPrice();
            MoneyModel.cashMove(bank, company, cash);
            ReportBuffer.add(LocalText.getText("MonetiseTreasuryShares",
                    company.getId(),
                    Bank.format(cash) ));

        }
    }

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
                                train.getId() );
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    companyName,
                    train.getId(),
                    errMsg ));
            return false;
        }

        /* End of validation, start of execution */
        getRoot().getChangeStack().newChangeSet(action);
        // FIXME: if (action.isForced()) changeStack.linkToPreviousMoveSet();
        pool.addTrain(train);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                companyName,
                train.getId() ));

        finishTurn();

        return true;
    }

    @Override
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

    @Override
    public String toString() {
        return "StockRound_18EU " + getStockRoundNumber();
    }
}
