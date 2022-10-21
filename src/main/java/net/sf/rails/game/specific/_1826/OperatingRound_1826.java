package net.sf.rails.game.specific._1826;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.Portfolio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import java.util.*;

public class OperatingRound_1826 extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1826.class);

    /** To prevent a double check, create BuyBond and RepayLoans actions
     *  already when the possibility check is done in the nextStep() process,
     *  and temporarily store these in a local list.
     */
    private List<BuyBond> buyableBonds = new ArrayList<>();
    private List<RepayLoans> repayableLoans = new ArrayList<>();

    public OperatingRound_1826(GameManager parent, String id) {
        super(parent, id);

        steps = new GameDef.OrStep[]{
                GameDef.OrStep.INITIAL,
                GameDef.OrStep.LAY_TRACK,
                GameDef.OrStep.LAY_TOKEN,
                GameDef.OrStep.CALC_REVENUE,
                GameDef.OrStep.PAYOUT,
                GameDef.OrStep.BUY_TRAIN,
                GameDef.OrStep.REPAY_LOANS,
                GameDef.OrStep.BUY_BONDS,
                GameDef.OrStep.TRADE_SHARES,
                GameDef.OrStep.FINAL
        };
    }

    private PublicCompany_1826 etat
            = (PublicCompany_1826)companyManager.getPublicCompany(PublicCompany_1826.ETAT);
    private PublicCompany_1826 sncf
            = (PublicCompany_1826)companyManager.getPublicCompany(PublicCompany_1826.SNCF);

    @Override
    protected void setDestinationActions() {

        List<PublicCompany> possibleDestinations = new ArrayList<>();
        for (PublicCompany comp : operatingCompanies.view()) {
            if (comp.hasDestination()
                    && comp.getActiveShareCount() == 5
                    && !comp.hasReachedDestination()) {
                possibleDestinations.add(comp);
            }
        }
        if (possibleDestinations.size() > 0) {
            possibleActions.add(new ReachDestinations(getRoot(), possibleDestinations));
        }
    }

    @Override
    protected void executeDestinationActions(List<PublicCompany> companies) {

        for (PublicCompany company : companies) {
            company.setReachedDestination(true);
        }
    }

    @Override
    public boolean growCompany (GrowCompany action) {

        return ((PublicCompany_1826)action.getCompany()).grow(true);
    }

    @Override
    protected void setGameSpecificPossibleActions() {

        PublicCompany_1826 company = (PublicCompany_1826) getOperatingCompany();

        // Once the destination is reached, a 5-share company
        // may convert to a 10-share company
        // NOTE: the rules may imply that this is only allowed in the LAY_TRACK step.
        // That interpretation is not (yet) followed here.
        if (company.hasDestination() && company.hasReachedDestination()
                && company.getShareUnit() == 20) {
            possibleActions.add(new GrowCompany(getRoot(), 10));
        }

        // Check if the current player can use the extra train right
        // NOTE: the rules may imply that this is only allowed in the BUY_TRAINS step.
        // That interpretation is not (yet) followed here.
        Player player = playerManager.getCurrentPlayer();
        if (player != null) {
            List<SpecialRight> srs = player.getPortfolioModel()
                    .getSpecialProperties(SpecialRight.class, false);
            if (srs != null && !srs.isEmpty()) {
                possibleActions.add(new UseSpecialProperty(srs.get(0)));
            }
        }

        if (getStep() == GameDef.OrStep.REPAY_LOANS) {

            // The possibility has already been checked in gameSpecificNextStep()
            if (!repayableLoans.isEmpty()) {
                possibleActions.addAll (repayableLoans);
                repayableLoans.clear();
            }

        } else if (getStep() == GameDef.OrStep.BUY_BONDS) {

            // The possibility has already been checked in gameSpecificNextStep()
            if (!buyableBonds.isEmpty()) {
                possibleActions.addAll(buyableBonds);
                buyableBonds.clear();
            }
        }

    }

    @Override
    protected boolean gameSpecificNextStep (GameDef.OrStep newStep) {

        PublicCompany_1826 company = ((PublicCompany_1826) operatingCompany.value());

        if (newStep == GameDef.OrStep.REPAY_LOANS) {

            repayableLoans.clear();

            // Has company any outstanding loans to repay?
            if (company.getMaxNumberOfLoans() != 0
                    && company.getCurrentNumberOfLoans() > 0) {

                // Minimum number to repay
                int minNumber = 0; // In 1826, repayment is optional
                int maxNumber = Math.min(company.getCurrentNumberOfLoans(),
                        company.getCash() / company.getValuePerLoan());

                if (maxNumber > 0) {
                    repayableLoans.add(new RepayLoans(company,
                            minNumber, maxNumber, company.getValuePerLoan()));
                    doneAllowed = true;  // ??? Needed here?
                    return true;
                }
            }

            return false;

        } else if (newStep == GameDef.OrStep.BUY_BONDS) {

            buyableBonds.clear();

            // Check if any bonds can be bought

            // Check if any bonds are NOT in its Treasury
            if (company.hasBonds() && company.getPortfolioModel().getBondsModel(company).getBondsCount()
                    < company.getNumberOfBonds()) {
                // Scan all potential owners: the Pool first, then all players.
                if (pool.getBondsModel(company).getBondsCount() > 0) {
                    buyableBonds.add (new BuyBond (pool.getParent(),
                            company, company, company.getPriceOfBonds()));
                    return true;
                }
                for (Player player : playerManager.getPlayers()) {
                    if (player.getPortfolioModel().getBondsModel(company).getBondsCount() > 0) {
                        buyableBonds.add(new BuyBond(player,
                                company, company, company.getPriceOfBonds()));
                        return true;
                    }
                }
            }
            return false; // Skip this step
        }
        return true;  // We are at a step that is not relevant here
    }

    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        String phaseId = phase.getId();

        // How many companies are trainless?
        List<PublicCompany_1826> trainlessCompanies = new ArrayList<>();
        for (PublicCompany company : operatingCompanies) {
            if (!company.getId().equals(PublicCompany_1826.BELG)
                    && !company.getId().equals(PublicCompany_1826.SNCF)
                    && company.hasOperated() && company.getNumberOfTrains() == 0) {
                trainlessCompanies.add((PublicCompany_1826)company);
            }
        }
        PublicCompany_1826 national = null;

        if (phaseId.equals("6H")) {
            // Form Etat
            national = etat;
        } else if (phaseId.equals("10H") || phaseId.equals("E")) {
            // Form SNCF (if not started before)
            national = sncf;
            growRemaining5shares();
        }
        if (trainlessCompanies.size() >= 2) {
            if (national != null && !national.hasStarted()) {
                ReportBuffer.add(this,
                        LocalText.getText("WillMergeInto", trainlessCompanies, national));
                formNational(national, trainlessCompanies);
           }
        } else {
            ReportBuffer.add(this,
                    LocalText.getText("DoesNotForm", national));
        }

        if (phaseId.equals("10H")) {

            // Change number of 10H-trains, if necessary
            // Count companies that are open or (still) startable
            int compCount = 0;
            for (PublicCompany company : companyManager.getAllPublicCompanies()) {
                if (!company.isClosed() && (!(company == etat) || etat.hasStarted())) {
                    compCount++;
                }
            }
            int trainsToRemove = Math.min (3, 10 - compCount);
            if (trainsToRemove > 0) {
                TrainType type = getRoot().getTrainManager().getTrainTypeByName("10H");
                for (int i=0; i<trainsToRemove; i++) {
                    ipo.getTrainOfType(type).getCard().moveTo(Bank.getScrapHeap(this));
                }
                String msg = LocalText.getText("RemovedTrains",
                        trainsToRemove, "10H", 5 - trainsToRemove);
                ReportBuffer.add (this, msg);
                DisplayBuffer.add (this, msg);
            }
        }
    }

    private void growRemaining5shares() {
        // Convert all remaining 5-share companies to 10-share
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (!company.isClosed() && company.getShareUnit() == 20) {
                ((PublicCompany_1826)company).grow(false);
            }
        }
    }

    /**
     * Non-home locations where a base token may be exchanged.
     */
    private Multimap<PublicCompany, Stop> exchangeableTokenStops;
    private ExchangeTokens2 exchangeTokensAction;
    private PublicCompany interruptedCompany;
    private Player interruptedPlayer;

    private void formNational (PublicCompany_1826 national,
                               List<PublicCompany_1826> trainlessCompanies) {

        NavigableSet<Integer> prices = new TreeSet<>();
        boolean nationalCanOperate = true;

        // Make all trainless companies 10-share ones
        for (PublicCompany_1826 company : trainlessCompanies) {
            if (company.getShareUnit() == 20) {  // 5-share
                company.grow();
            }
            prices.add (company.getCurrentSpace().getPrice());
            log.debug ("Price of {} is {}", company, company.getCurrentSpace().getPrice());
            if (companiesOperatedThisRound.contains(company)) nationalCanOperate = false;
        }
        log.debug ("Sorted prices: {}", prices);

        // Determine the national's start price
        int averagePrice = (prices.pollLast() + prices.pollLast()) / 2;
        //int minimumPrice = (isEtat ? 82 : 110);
        int nationalPrice = Math.max(averagePrice, national.getMinimumStartPrice());
        log.debug ("Top-2 average price is {}", averagePrice);
        StockMarket stockMarket = getRoot().getStockMarket();
        int row = 0;
        int col = 0;
        while (stockMarket.getStockSpace(row, col).getPrice() < nationalPrice) { col++; }
        if (!(stockMarket.getStockSpace(row, col).getPrice() == nationalPrice)) col--;
        StockSpace nationalStockSpace = stockMarket.getStockSpace(row, col);

        // TODO Actual national start price will decrease with loans
        national.start(nationalStockSpace);
        log.debug ("National price is {} at {}", nationalStockSpace.getPrice(),
                nationalStockSpace.getId());
        String msg = LocalText.getText("START_MERGED_COMPANY", national.getId(),
                Bank.format(this, nationalStockSpace.getPrice()),
                nationalStockSpace.getId());
        DisplayBuffer.add (this, msg);
        ReportBuffer.add (this, msg);

        msg = (nationalCanOperate
                ? LocalText.getText("CanOperate", national)
                : LocalText.getText("CannotOperate", national));
        DisplayBuffer.add (this, msg);
        ReportBuffer.add (this, msg);

        // Move the national certificates to the company treasury
        Portfolio.moveAll(unavailable.getCertificates(national), national);

        // To exchange player shares, start with player who started this phase,i.e. the current player
        // We don't explicitly deal with the old certificates, these are discarded as companies close.
        //
        // Although the logic is different, it remains to be seen
        // whether the below merging code can be united with Mergers.mergeCompanies().
        boolean president = true;
        int extraPoolShares = 0;

        for (Player player : playerManager.getNextPlayersAfter(
                playerManager.getCurrentPlayer(), true, false)) {

            PortfolioModel playerPortfolio = player.getPortfolioModel();

            /*
            int playerShares = 0;
            for (PublicCompany_1826 company : trainlessCompanies) {
                playerShares += playerPortfolio.getShares(company);
            }
            if (playerShares == 0) continue;*/

            extraPoolShares += replaceMergerShares (player.getPortfolioModel(), 0,
                    national, president, trainlessCompanies);
            president = false;
        }

        // Now deal with the Pool similarly
        /*
        for (PublicCompany_1826 company : trainlessCompanies) {
            poolShares += pool.getShares(company);
        }*/

        replaceMergerShares (pool, extraPoolShares, national, false, trainlessCompanies);

        if (!national.checkPresidency()) {
            ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF",
                    national.getPresident(), national));
        }

        // Merge and close the trainless companies
        Map<Stop, PublicCompany> homeTokens = new HashMap<>();
        // A TreeMap failed to .get() some companies - weird!

        for (PublicCompany company : trainlessCompanies) {

            ReportBuffer.add (this, LocalText.getText("AutoMergeMinorLog",
                    company, national, company.getCash(), company.getNumberOfTrains()));

            // Move all company assets to the national company
            national.transferAssetsFrom(company);

            // Note any tokens that can be converted to national tokens
            for (BaseToken token : company.getLaidBaseTokens()) {
                Owner owner = token.getOwner();
                if (owner instanceof Stop) { // Must be
                    Stop stop = (Stop) owner;
                    MapHex hex = stop.getHex();
                    if (company.getHomeHexes().contains(hex)) {
                        homeTokens.put (stop, company);
                    } else {
                        if (exchangeableTokenStops == null) {
                            exchangeableTokenStops = ArrayListMultimap.create();
                        }
                        if (!stop.hasTokenOf(national)) {
                            exchangeableTokenStops.put(company, stop);
                        }
                    }
                }
            }
            // Close the merging company, this also removes the tokens
            //company.setClosed();
            closeCompany (company);
        }

        // Immediately replace all home tokens.
        // The national tokens must be created and do not count against
        // the number of configured tokens of the national company.

        for (Stop stop : homeTokens.keySet()) {
            if (!stop.hasTokenOf(national)) {
                PublicCompany company = homeTokens.get(stop);
                BaseToken token = BaseToken.create(national);
                token.moveTo(stop);
                national.getBaseTokensModel().addBaseToken(token, true);
                ReportBuffer.add(this, LocalText.getText("ExchangesHomeBaseToken",
                        national, company, stop));
            }
        }


        /*
         * Prepare action to exchange any non-home tokens.
         */
        if (exchangeableTokenStops != null && !exchangeableTokenStops.isEmpty()) {
            int minNumberToExchange = national.getMinNumberToExchange(); // 0
            // Subtract home token from the max exchange count. Value becomes Etat: 1, SNCF: 2
            int maxNumberToExchange = Math.max(national.getMaxNumberToExchange() - 1, 0);
            boolean exchangeCountPerCompany = national.isExchangeCountPerCompany(); // true
            exchangeTokensAction = new ExchangeTokens2(getRoot(),
                    national, minNumberToExchange, maxNumberToExchange, exchangeCountPerCompany);
            exchangeTokensAction.setPlayer(national.getPresident());
            for (PublicCompany company : exchangeableTokenStops.keySet()) {
                for (Stop stop : exchangeableTokenStops.get(company)) {
                    exchangeTokensAction.addStop(company, stop);
                }
            }
        }

        national.setCapitalizationShares();
        checkFlotation(national);
        // Move the remaining national certificates to the pool
        if (national == sncf) {
            SortedSet<PublicCertificate> certs = national.getPortfolioModel().getCertificates(national);
            if (!certs.isEmpty()) {
                Portfolio.moveAll(certs, bank.getPool());
                ReportBuffer.add(this, LocalText.getText("HasPutSharesInPool",
                        national, certs.size() * national.getShareUnit()));
            }
        }

        if (nationalCanOperate) insertNewOperatingCompany (national);

        // Set up for executing the token exchange action, if applicable.
        // As this is only a single action, there does not seem to be
        // a need to set up a separate round.
        if (exchangeTokensAction != null) {
            interruptedCompany = operatingCompany.value();
            interruptedPlayer = playerManager.getCurrentPlayer();
            operatingCompany.set(national);
            playerManager.setCurrentPlayer(national.getPresident());
        }
    }

    /** Execute the share replacement procedure for one player or the pool.
     *
     * @param portfolio The portfolio owning the shares that must be exchanged
     * @param national The new national company being formed
     * @param trainlessCompanies The old companies being merged into the national
     * @return The number of remaining shares that cannot be exchanged
     */
    private int replaceMergerShares (PortfolioModel portfolio,
                                      int extraSharesToReplace,
                                      PublicCompany_1826 national,
                                      boolean president,
                                      List<PublicCompany_1826> trainlessCompanies) {

        PortfolioModel nationalPortfolio = national.getPortfolioModel();
        Player player;
        PublicCertificate cert;
        int remainingShares = 0;
        int totalShares, newShares, newBonds, unreplacedShares, availableBonds;

        RailsOwner owner = portfolio.getParent();
        if (owner instanceof Player) {
            player = (Player) owner;
        } else {
            player = null;  // Pool
        }
        totalShares = extraSharesToReplace;
        for (PublicCompany_1826 company : trainlessCompanies) {
            totalShares += portfolio.getShares(company);
        }
        if (totalShares == 0) return 0;

        newShares = 0;
        newBonds = 0;
        unreplacedShares = 0;
        if (national.hasBonds()) {
            availableBonds = national.getPortfolioModel().getBondsModel(national).getBondsCount();
        } else {
            availableBonds = 0;
        }

        // Exchange certificates two-for-one
        while (totalShares > 1) {
            // The first player getting a national certificate becomes President
            if (president && player != null) {
                national.setPresident(player);
            }
            cert = nationalPortfolio.findCertificate(national, president);
            if (cert != null) {
                cert.moveTo(portfolio);
                newShares++;
            } else if (national.hasBonds() && availableBonds > 0) {
                newBonds++;
                availableBonds--;
            } else {
                unreplacedShares += 2;
            }
            totalShares -= 2;
            president = false;
        }
        if (totalShares == 1) {
            // Discard odd share to the Pool or Scrapheap
            remainingShares += 1;
        }
        if (newShares > 0) {
            ReportBuffer.add(this, LocalText.getText("SharesReplacedForShares",
                    owner.getId(), 2 * newShares, newShares, national.getId()));
        }
        if (newBonds > 0) {
            nationalPortfolio.getBondsModel(national).addBondsCount(-newBonds);
            portfolio.getBondsModel(national).addBondsCount(newBonds);
            ReportBuffer.add(this, LocalText.getText("SharesReplacedForBonds",
                    owner.getId(), 2 * newBonds, newBonds, national.getId()));
        }
        if (unreplacedShares > 0) {
            ReportBuffer.add(this, LocalText.getText("SharesNotReplaced",
                    (player != null ? player : pool.getParent()),
                    unreplacedShares));

        }
        if (totalShares == 1) {
            if (player != null) {
                ReportBuffer.add(this, LocalText.getText("HasPutShareInPool",
                        player.getId()));
            } else {
                ReportBuffer.add(this, LocalText.getText("HasDiscardedShare", pool));
            }
        }
        return remainingShares;
    }

    @Override
    public boolean setPossibleActions() {
        if (exchangeTokensAction != null) {
            // Action was set up under the train buying player
            // rather than the national president.
            exchangeTokensAction.setPlayer();
            possibleActions.add (exchangeTokensAction);
            return true;
        } else {
            return super.setPossibleActions();
        }
    }

    @Override
    public boolean process(PossibleAction action) {
        if (action instanceof ExchangeTokens2) {
            return processExchangeTokens ((ExchangeTokens2)action, false);
        } else {
            return super.process(action);
        }
    }

    private boolean processExchangeTokens (ExchangeTokens2 action, boolean isHome) {

        for (ExchangeTokens2.Location location : action.getLocations()) {
            PublicCompany national = action.getNewCompany();
            Stop stop = location.getStop();
            if (location.isSelected() && !stop.hasTokenOf(national)) {
                BaseToken token = BaseToken.create(national);
                token.moveTo(stop);
                national.getBaseTokensModel().addBaseToken(token, true);
                ReportBuffer.add (this, LocalText.getText(
                        isHome ? "ExchangesHomeBaseToken": "ExchangesBaseToken",
                        national, location.getOldCompany(), stop));
            } else {
                ReportBuffer.add (this, LocalText.getText("NoBaseTokenExchange",
                        national, location.getOldCompany(), location.getStop()));
            }
        }

        exchangeTokensAction = null;
        operatingCompany.set(interruptedCompany);
        playerManager.setCurrentPlayer(interruptedPlayer);

        return true;
    }

    @Override
    protected int canTakeLoans (PublicCompany company, int cashToRaise) {

        int roomForLoans = company.getMaxNumberOfLoans() - company.getCurrentNumberOfLoans();
        if (roomForLoans == 0) return 0;

        int loansNeeded = 1 + (cashToRaise - 1) / company.getValuePerLoan();
        return Math.min (loansNeeded, roomForLoans);
    }

    @Override
    protected boolean repayLoans(RepayLoans action) {

        String errMsg = validateRepayLoans(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotRepayLoans",
                    action.getCompanyName(), action.getNumberRepaid(),
                    Bank.format(this, action.getPrice()), errMsg));
            return false;
        }

        int repayNumber = action.getNumberRepaid();
        if (repayNumber > 0) {
            executeRepayLoans(action);
            getRoot().getStockMarket().moveRight(getOperatingCompany(), 1);
        }

        return true;
    }


}
