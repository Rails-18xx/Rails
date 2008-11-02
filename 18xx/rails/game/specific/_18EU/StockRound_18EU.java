package rails.game.specific._18EU;

import java.util.*;

import rails.game.*;
import rails.game.action.*;
import rails.game.move.*;
import rails.game.state.BooleanState;
import rails.game.state.IntegerState;
import rails.util.LocalText;
import rails.util.Util;

/**
 * Implements a basic Stock Round. <p> A new instance must be created for each
 * new Stock Round. At the end of a round, the current instance should be
 * discarded. <p> Permanent memory is formed by static attributes (like who has
 * the Priority Deal).
 */
public class StockRound_18EU extends StockRound {
    protected List<PublicCompanyI> compWithExcessTrains =
            new ArrayList<PublicCompanyI>();
    protected PublicCompanyI[] discardingCompanies;
    protected IntegerState discardingCompanyIndex;
    protected BooleanState discardingTrains =
            new BooleanState("DiscardingTrains", false);
    protected boolean phase5Reached = false;

    @Override
    public void start() {
        super.start();
        if (discardingTrains.booleanValue()) {
            discardingTrains.set(false);
        }

        phase5Reached = gameManager.getPhaseManager().hasReachedPhase("5");

    }

    @Override
    public boolean setPossibleActions() {
        if (discardingTrains.booleanValue()) {
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

        List<PublicCertificateI> certs;
        PublicCertificateI cert;
        PublicCompanyI comp;
        StockSpaceI stockSpace;
        Portfolio from;
        int price;
        int number;

        // 18EU special: until phase 5, we can only
        // start a company by trading in a Minor
        boolean mustMergeMinor = !phase5Reached;
        List<PublicCompanyI> minors = null;
        List<City> freeStations = null;
        if (mustMergeMinor) {
            minors = new ArrayList<PublicCompanyI>();
            for (PublicCertificateI c : getCurrentPlayer().getPortfolio().getCertificates()) {
                if (c.getCompany().getTypeName().equalsIgnoreCase("Minor")) {
                    minors.add(c.getCompany());
                }
            }
        } else {
            freeStations = new ArrayList<City>();
            MapManager map =
                    (MapManager) Game.getComponentManager().findComponent("Map");
            for (City city : map.getCurrentStations()) {
                if (city.getSlots() > city.getTokens().size()) {
                    freeStations.add(city);
                }
            }
        }

        int playerCash = currentPlayer.getCash();

        /* Get the next available IPO certificates */
        // Never buy more than one from the IPO
        PublicCompanyI companyBoughtThisTurn =
                (PublicCompanyI) companyBoughtThisTurnWrapper.getObject();
        if (companyBoughtThisTurn == null) {
            from = Bank.getIpo();
            Map<String, List<PublicCertificateI>> map =
                    from.getCertsPerCompanyMap();
            int shares;

            for (String compName : map.keySet()) {
                certs = map.get(compName);
                if (certs == null || certs.isEmpty()) continue;
                /* Only the top certificate is buyable from the IPO */
                cert = certs.get(0);
                comp = cert.getCompany();
                if (isSaleRecorded(currentPlayer, comp)) continue;
                if (currentPlayer.maxAllowedNumberOfSharesToBuy(comp,
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
                                new StartCompany_18EU(cert, prices);
                        if (mustMergeMinor) {
                            action.setMinorsToMerge(minors);
                        } else {
                            action.setAvailableHomeStations(freeStations);
                        }
                        possibleActions.add(action);
                    }
                } else if (comp.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(cert, from,
                            comp.getMarketPrice()));
                }

            }
        }

        /* Get the unique Pool certificates and check which ones can be bought */
        from = Bank.getPool();
        Map<String, List<PublicCertificateI>> map =
                from.getCertsPerCompanyMap();

        for (String compName : map.keySet()) {
            certs = map.get(compName);
            if (certs == null || certs.isEmpty()) continue;
            number = certs.size();
            cert = certs.get(0);
            comp = cert.getCompany();
            if (isSaleRecorded(currentPlayer, comp)) continue;
            if (currentPlayer.maxAllowedNumberOfSharesToBuy(comp,
                    cert.getShare()) < 1) continue;
            //stockSpace = comp.getCurrentSpace();
            price = comp.getMarketPrice();

            if (companyBoughtThisTurn != null) {
                continue;
            }

            // Does the player have enough cash?
            while (number > 0 && playerCash < number * price)
                number--;

            if (number > 0) {
                possibleActions.add(new BuyCertificate(cert, from, price,
                        number));
            }
        }

        // Get any shares in company treasuries that can be bought
        if (gameManager.canAnyCompanyHoldShares()) {

            for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
                certs =
                        company.getPortfolio().getCertificatesPerCompany(
                                company.getName());
                if (certs == null || certs.isEmpty()) continue;
                cert = certs.get(0);
                if (isSaleRecorded(currentPlayer, company)) continue;
                if (!currentPlayer.mayBuyCompanyShare(company, 1)) continue;
                if (currentPlayer.maxAllowedNumberOfSharesToBuy(company,
                        certs.get(0).getShare()) < 1) continue;
                stockSpace = company.getCurrentSpace();
                if (!stockSpace.isNoCertLimit()
                    && !currentPlayer.mayBuyCertificate(company, 1)) continue;
                if (company.getMarketPrice() <= playerCash) {
                    possibleActions.add(new BuyCertificate(cert,
                            company.getPortfolio(),
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

        List<PublicCompanyI> comps =
                companyManager.getAllPublicCompanies();
        List<PublicCompanyI> minors = new ArrayList<PublicCompanyI>();
        List<PublicCompanyI> targetCompanies = new ArrayList<PublicCompanyI>();
        String type;

        for (PublicCompanyI comp : comps) {
            type = comp.getTypeName();
            if (type.equals("Major") && comp.hasStarted()
                && !comp.hasOperated()) {
                targetCompanies.add(comp);
            } else if (type.equals("Minor")
                       && comp.getPresident() == currentPlayer) {
                minors.add(comp);
            }
        }
        if (minors.isEmpty() || targetCompanies.isEmpty()) return;

        for (PublicCompanyI minor : minors) {
            possibleActions.add(new MergeCompanies(minor, targetCompanies));
        }
    }

    protected boolean setTrainDiscardActions() {

        PublicCompanyI discardingCompany =
                discardingCompanies[discardingCompanyIndex.intValue()];
        log.debug("Company " + discardingCompany.getName()
                  + " to discard a train");
        possibleActions.add(new DiscardTrain(discardingCompany,
                discardingCompany.getPortfolio().getUniqueTrains()));
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
        PublicCompanyI company = action.getCertificate().getCompany();
        int price = action.getPrice();
        int shares = action.getNumberBought();

        String errMsg = null;
        StockSpaceI startSpace = null;
        int numberOfCertsToBuy = 0;
        PublicCertificateI cert = null;
        String companyName = company.getName();
        PublicCompanyI minor = null;
        StartCompany_18EU startAction = null;
        City selectedHomeCity = null;

        currentPlayer = getCurrentPlayer();

        // Dummy loop to allow a quick jump out
        while (true) {
            if (!(action instanceof StartCompany_18EU)) {
                errMsg = LocalText.getText("InvalidAction");
                break;
            }
            startAction = (StartCompany_18EU) action;

            // The player may not have bought this turn.
            if (companyBoughtThisTurnWrapper.getObject() != null) {
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
            if (!currentPlayer.mayBuyCertificate(company, numberOfCertsToBuy)) {
                errMsg = LocalText.getText("CantBuyMoreCerts");
                break;
            }

            // The given price must be a valid start price
            if ((startSpace = stockMarket.getStartSpace(price)) == null) {
                errMsg =
                        LocalText.getText("InvalidStartPrice", new String[] {
                                Bank.format(price), company.getName() });
                break;
            }

            // Check if the Player has the money.
            if (currentPlayer.getCash() < shares * price) {
                errMsg = LocalText.getText("NoMoney");
                break;
            }

            if (!phase5Reached) {
                // Check if the player owns the merged minor
                minor = startAction.getChosenMinor();
                if (minor != null
                    && currentPlayer.getPortfolio().getCertificatesPerCompany(
                            minor.getName()) == null) {
                    errMsg =
                            LocalText.getText("PlayerDoesNotOwn", new String[] {
                                    currentPlayer.getName(), minor.getName() });
                    break;
                }
            } else {
                // Check if a valid home base has been selected
                selectedHomeCity = startAction.getSelectedHomeStation();
                if (selectedHomeCity.getSlots() <= selectedHomeCity.getTokens().size()) {
                    errMsg =
                            LocalText.getText("InvalidHomeBase", new String[] {
                                    selectedHomeCity.toString(),
                                    company.getName() });
                    break;
                }

            }
            numberOfCertsToBuy++;

            break;
        }

        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CantStart", new String[] {
                    playerName, companyName, Bank.format(price), errMsg }));
            return false;
        }

        MoveSet.start(true);

        // All is OK, now start the company
        MapHex homeHex = null;
        int homeCityNumber = 1;
        if (minor != null) {
            homeHex = minor.getHomeHex();
            homeCityNumber = homeHex.getCityOfBaseToken(minor);
        } else if (selectedHomeCity != null) {
            homeHex = (MapHex) selectedHomeCity.getHolder();
            homeCityNumber = selectedHomeCity.getNumber();
        }
        company.setHomeHex(homeHex);
        company.setHomeCityNumber(homeCityNumber);

        company.start(startSpace);
        ReportBuffer.add(LocalText.getText("START_COMPANY_LOG", new String[] {
                playerName, companyName, Bank.format(price),
                Bank.format(shares * price), String.valueOf(shares),
                String.valueOf(cert.getShare()), company.getName() }));

        // Transfer the President's certificate
        // currentPlayer.getPortfolio().buyCertificate(cert,
        // ipo,
        // cert.getCertificatePrice());
        cert.moveTo(currentPlayer.getPortfolio());

        new CashMove(currentPlayer, company, shares * price);

        if (minor != null) {
            // Get the extra certificate for the minor, for free
            PublicCertificateI cert2 = ipo.findCertificate(company, false);
            cert2.moveTo(currentPlayer.getPortfolio());
            // Transfer the minor assets into the started company
            int minorCash = minor.getCash();
            int minorTrains = minor.getPortfolio().getTrainList().size();
            company.transferAssetsFrom(minor);
            minor.setClosed();
            ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG", new String[] {
                    currentPlayer.getName(), minor.getName(),
                    company.getName(), Bank.format(minorCash),
                    String.valueOf(minorTrains) }));
            ReportBuffer.add(LocalText.getText("GetShareForMinor",
                    new String[] { currentPlayer.getName(),
                            String.valueOf(cert2.getShare()),
                            company.getName(), minor.getName() }));
        } else {
            ReportBuffer.add(LocalText.getText("SelectedHomeBase",
                    new String[] { company.getName(), homeHex.getName(),
                            selectedHomeCity.toString() }));
        }

        // Move the remaining certificates to the company treasury
        Util.moveObjects(ipo.getCertificatesPerCompany(company.getName()),
                company.getPortfolio());

        ReportBuffer.add(LocalText.getText("SharesPutInTreasury", new String[] {
                String.valueOf(company.getPortfolio().getShare(company)),
                company.getName() }));

        // TODO must get this amount from XML
        int tokensCost = 100;
        new CashMove(company, null, tokensCost);
        ReportBuffer.add(LocalText.getText("PaysForTokens", new String[] {
                company.getName(), Bank.format(100),
                String.valueOf(company.getNumberOfBaseTokens()) }));

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

        PublicCompanyI minor = action.getMergingCompany();
        PublicCompanyI major = action.getSelectedTargetCompany();
        PublicCertificateI cert = null;
        Portfolio pool = Bank.getPool();
        CashHolder cashDestination = null; // Bank

        // TODO Validation to be added?

        MoveSet.start(true);

        if (major != null) {
            cert = major.getPortfolio().findCertificate(major, false);
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
        int minorTrains = minor.getPortfolio().getTrainList().size();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) new CashMove(minor, null, minorCash);
            pool.transferAssetsFrom(minor.getPortfolio());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);
        }

        MapHex homeHex = minor.getHomeHex();
        int homeCityNumber = homeHex.getCityOfBaseToken(minor);
        minor.setClosed();

        if (major != null && action.getReplaceToken()) {
            if (homeHex.layBaseToken(major, homeCityNumber)) {
                major.layBaseToken(homeHex, homeCityNumber);
            }
        }

        if (major != null) {
            checkFlotation(major);
            if (major.getNumberOfTrains() > major.getCurrentTrainLimit()
                && !compWithExcessTrains.contains(major)) {

                new AddToList<PublicCompanyI>(compWithExcessTrains, major,
                        "CompaniesWithExcessTrains");
            }
        }

        if (cert != null) {
            ReportBuffer.add(LocalText.getText("MERGE_MINOR_LOG", new String[] {
                    currentPlayer.getName(), minor.getName(), major.getName(),
                    Bank.format(minorCash), String.valueOf(minorTrains) }));
            ReportBuffer.add(LocalText.getText("GetShareForMinor",
                    new String[] { currentPlayer.getName(),
                            String.valueOf(cert.getShare()), major.getName(),
                            cert.getPortfolio().getName(), minor.getName() }));
            cert.moveTo(currentPlayer.getPortfolio());
            ReportBuffer.add(LocalText.getText("MinorCloses", minor.getName()));
        } else {
            ReportBuffer.add(LocalText.getText("CLOSE_MINOR_LOG", new String[] {
                    currentPlayer.getName(), minor.getName(),
                    Bank.format(minorCash), String.valueOf(minorTrains) }));
        }
        hasActed.set(true);

        if (!(this instanceof FinalMinorExchangeRound)) {
            companyBoughtThisTurnWrapper.set(major);
            setPriority();
        }

        return true;
    }

    @Override
    protected void floatCompany(PublicCompanyI company) {

        company.setFloated();
        ReportBuffer.add(LocalText.getText("Floats", company.getName()));

        // Before phase 5, no other actions are required.

        if (phase5Reached) {
            // Put the remaining 5 shares in the pool,
            // getting cash in return
            // Move the remaining certificates to the company treasury
            Util.moveObjects(company.getPortfolio().getCertificatesPerCompany(
                    company.getName()), pool);
            int cash = 5 * company.getMarketPrice();
            new CashMove(null, company, cash);
            ReportBuffer.add(LocalText.getText("MonetiseTreasuryShares",
                    new String[] { company.getName(), Bank.format(cash) }));

        }
    }

    public boolean discardTrain(DiscardTrain action) {

        TrainI train = action.getDiscardedTrain();
        PublicCompanyI company = action.getCompany();
        String companyName = company.getName();

        String errMsg = null;

        // Dummy loop to enable a quick jump out.
        while (true) {
            // Checks
            // Must be correct step
            if (!discardingTrains.booleanValue()) {
                errMsg = LocalText.getText("WrongActionNoDiscardTrain");
                break;
            }

            if (train == null) {
                errMsg = LocalText.getText("NoTrainSpecified");
                break;
            }

            // Does the company own such a train?

            if (!company.getPortfolio().getTrainList().contains(train)) {
                errMsg =
                        LocalText.getText("CompanyDoesNotOwnTrain",
                                new String[] { company.getName(),
                                        train.getName() });
                break;
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(LocalText.getText("CannotDiscardTrain",
                    new String[] { companyName, train.getName(), errMsg }));
            return false;
        }

        /* End of validation, start of execution */
        MoveSet.start(false);
        //
        if (action.isForced()) MoveSet.setLinkedToPrevious();

        Bank.getPool().buyTrain(train, 0);
        ReportBuffer.add(LocalText.getText("CompanyDiscardsTrain",
                new String[] { companyName, train.getName() }));

        finishTurn();

        return true;
    }

    @Override
    protected void finishTurn() {

        // log.debug("+++ FinishTurn");
        if (!discardingTrains.booleanValue()) {
            super.setNextPlayer();
        } else {
            PublicCompanyI comp =
                    discardingCompanies[discardingCompanyIndex.intValue()];
            if (comp.getNumberOfTrains() <= comp.getCurrentTrainLimit()) {
                discardingCompanyIndex.add(1);
                if (discardingCompanyIndex.intValue() >= discardingCompanies.length) {
                    // All excess trains have been discarded
                    finishRound();
                    return;
                }
            }
            PublicCompanyI discardingCompany =
                    discardingCompanies[discardingCompanyIndex.intValue()];
            setCurrentPlayer(discardingCompany.getPresident());
        }
    }

    @Override
    protected void finishRound() {
        // log.debug("+++ FinishRound");

        if (discardingTrains.booleanValue()) {

            super.finishRound();

        } else if (!compWithExcessTrains.isEmpty()) {

            discardingTrains.set(true);

            // Make up a list of train discarding companies in sequence of the
            // last OR
            // TODO: this disregards any changes in the operating sequence
            // during the last OR. This is probably wrong.
            PublicCompanyI[] operatingCompanies = getOperatingCompanies();
            discardingCompanies =
                    new PublicCompanyI[compWithExcessTrains.size()];
            for (int i = 0, j = 0; i < operatingCompanies.length; i++) {
                if (compWithExcessTrains.contains(operatingCompanies[i])) {
                    discardingCompanies[j++] = operatingCompanies[i];
                }
            }

            if (discardingCompanyIndex == null) {
                discardingCompanyIndex =
                        new IntegerState("DiscardingCompanyIndex", 0);
            } else {
                discardingCompanyIndex.set(0);
            }
            PublicCompanyI discardingCompany =
                    discardingCompanies[discardingCompanyIndex.intValue()];
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
