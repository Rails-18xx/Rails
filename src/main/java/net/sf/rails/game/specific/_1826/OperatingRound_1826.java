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
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.*;
import net.sf.rails.game.state.Currency;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.*;

import java.util.*;

public class OperatingRound_1826 extends OperatingRound {

    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1826.class);

    /** To prevent a double check, create BuyBonds and RepayLoans actions
     *  already when the possibility check is done in the nextStep() process,
     *  and temporarily store these in a local list.
     */
    private List<BuyBonds> buyableBonds = new ArrayList<>();
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
                GameDef.OrStep.TRADE_SHARES, // including Bonds
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
                doneAllowed=true;
            }

        } else if (getStep() == GameDef.OrStep.BUY_BONDS) {

            // The possibility has already been checked in gameSpecificNextStep()
            if (!buyableBonds.isEmpty()) {
                possibleActions.addAll(buyableBonds);
                buyableBonds.clear();
                doneAllowed=true;            }
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
                    return true;
                }
            }

            return false;

       } else if (newStep == GameDef.OrStep.BUY_BONDS) {

            buyableBonds.clear();

            // Check if any bonds can be bought

            // Check if any bonds are NOT in its Treasury
            if (company.hasBonds()
                    && company.getPortfolioModel().getBondsCount(company) < company.getNumberOfBonds()) {
                // Scan all potential owners: the Pool first, then all players.
                if (pool.getBondsModel(company).getBondsCount() > 0) {
                    buyableBonds.add (new BuyBonds(pool.getParent(),
                            company, company, 1, company.getPriceOfBonds()));
                    return true;
                }
                for (Player player : playerManager.getPlayers()) {
                    if (player.getPortfolioModel().getBondsCount(company) > 0) {
                        buyableBonds.add(new BuyBonds(player,
                                company, company, 1, company.getPriceOfBonds()));
                    }
                }
            }
            return !buyableBonds.isEmpty();
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
        } else if (phaseId.equals("10H") || phaseId.equals("E") && !sncf.hasStarted()) {
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
                bank.format(nationalStockSpace.getPrice()),
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

        int loansTransferred = 0;
        int maxLoansTransferred = national.getMaxNumberOfLoans() - national.getCurrentNumberOfLoans();
        for (PublicCompany company : trainlessCompanies) {

            ReportBuffer.add (this, LocalText.getText("AutoMergeMinorLog",
                    company, national, company.getCash(), company.getNumberOfTrains()));

            // Move all company assets to the national company
            national.transferAssetsFrom(company);

            // Take over max. 2 loans
            int loans = company.getCurrentNumberOfLoans();
            int loansToTransfer = Math.min(loans, maxLoansTransferred - loansTransferred);
            if (loansToTransfer > 0) {
                loansTransferred += loansToTransfer;
                ReportBuffer.add(this, LocalText.getText("LoansTransfer",
                        national, loansToTransfer, company));
            }

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

        // Take the loans to be transferred
        if (loansTransferred > 0) {
            national.addLoans(loansTransferred);
            getRoot().getStockMarket().moveLeftOrDown(national, loansTransferred);
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

    /**
     * Execute the interest payments of loans and bonds.
     * This has to be done before actual dividend payouts.
     * If needed, any missing cash will be raised first.
     *
     * @param action The SetDividend action returned by the player
     * @return The remaining earnings after any deductions
     */
    @Override
    protected int executeDeductions(SetDividend action) {

        PublicCompany company = action.getCompany();
        int dividend = action.getActualRevenue();
        int availableCash = company.getCash();
        int loanInterest = 0;
        int bondInterest = 0;
        int remainsDueForLoans = 0;
        int remainsDueForBonds = 0;
        int payment;

        // First we'll check if interests can be paid.
        // If not, cash will be raised.
        // Actual interest payments will be done at the end.

        // Check for loan interest payment
        if (company.canLoan()) {
            // First try to pay the full amount from company treasury
            // (per the 1826 rules, a partial amount may not be deducted
            // if the treasury cash is insufficient)
            loanInterest = calculateLoanInterest(company);
            remainsDueForLoans = loanInterest;

            if (loanInterest > 0) {
                if (loanInterest <= availableCash) {
                    // Pay whole amount from treasury
                    payment = loanInterest;
                    ReportBuffer.add(this, LocalText.getText("InterestPaidFromTreasury",
                            company,
                            bank.format(payment),
                            LocalText.getText("loan")));
                    availableCash -= payment;
                    remainsDueForLoans = 0;
                } else if (dividend > 0) {
                    // Pay from earnings (partially if insufficient)
                    payment = Math.min(dividend, loanInterest);
                    Currency.fromBank(payment, company);
                    // Earmarked for loans, so unavailable for other purposes
                    ReportBuffer.add(this, LocalText.getText("InterestPaidFromRevenue",
                            company,
                            bank.format(payment),
                            bank.format(loanInterest),
                            LocalText.getText("loan")));
                    remainsDueForLoans -= payment;
                    dividend -= payment;
                }
            }
        }

        if (company.hasBonds()) {
            // First try to pay the full amount from company treasury
            // (per the 1826 rules, a partial amount may not be deducted
            // if the treasury cash is insufficient)
            bondInterest = calculateBondInterest(company);
            remainsDueForBonds = bondInterest;
            if (bondInterest > 0) {
                if (bondInterest <= availableCash) {
                    payment = bondInterest;
                    ReportBuffer.add(this, LocalText.getText("InterestPaidFromTreasury",
                            company,
                            bank.format(payment),
                            LocalText.getText("bond")));
                    availableCash -= payment;
                    remainsDueForBonds = 0;
                } else if (dividend > 0) {
                    payment = Math.min(dividend, bondInterest);
                    Currency.fromBank(payment, company);
                    // Earmarked for Bonds
                    ReportBuffer.add(this, LocalText.getText("InterestPaidFromRevenue",
                            company,
                            bank.format(payment),
                            bank.format(bondInterest),
                            LocalText.getText("bond")));
                    remainsDueForBonds -= payment;
                    dividend -= payment;

                }
            }
        }

        if (dividend < action.getActualRevenue()) {
            String msg = LocalText.getText("RevenueReduced",
                    company, bank.format(dividend));
            ReportBuffer.add (this, msg);
            DisplayBuffer.add (this, msg);
        }

        // TODO Testing has not progressed yet beyond this point

        int remainder = remainsDueForLoans + remainsDueForBonds;
        if (remainder > 0) {
            // Take a loan if possible. One loan should always be sufficient.
            if (company.canLoan()
                    && company.getCurrentNumberOfLoans() < company.getMaxNumberOfLoans()) {
                executeTakeLoans(1);
                int loanValue = company.getValuePerLoan();
                ReportBuffer.add(this, LocalText.getText("CompanyTakesLoan",
                        company, bank.format(loanValue),
                        bank.format(loanValue)));
                remainder -= loanValue;
            }
        }

        // Pay any remainder from president cash
        if (remainder > 0) {
            Player president = company.getPresident();
            int presCash = president.getCash();
            // First check if president has enough cash
            if (remainder > presCash) {
                // For now, we assume that this will not happen.
                // Nevertheless, give a warning
                // TODO Insert running a ShareSellingRound here
                log.warn("??? The president still cannot pay ${} loan interest???", remainder);
                return 0;
            } else {
                payment = remainder;
                ReportBuffer.add(this, LocalText.getText("InterestPaidFromPresidentCash",
                        operatingCompany.value().getId(),
                        bank.format(payment),
                        bank.format(loanInterest),
                        LocalText.getText("loan/bond"),
                        president.getId()));
                Currency.wire (president, payment, company);
            }
        }

        // Payout loan interest
        if (loanInterest > 0) Currency.toBank (company, loanInterest);

        // Payout bond interest
        for (PortfolioModel portfolio : dueInterests.keySet()) {
            payment = dueInterests.get(portfolio);
            Currency.wire (company, payment, portfolio.getMoneyOwner());
            ReportBuffer.add(this, LocalText.getText("PayoutForBonds",
                    portfolio.getMoneyOwner().getId(),
                    bank.format(payment),
                    portfolio.getBondsCount(company),
                    company));
        }
        return dividend;
    }

    /* A map noting to whom bond interest amounts must be paid */
    private Map<PortfolioModel, Integer> dueInterests = new HashMap<>();

    /** Calculate the bonds interest due to a bond holder
     *
     * @param company The company that has issued bonds
     * @return The interest to pay
     * As a side effect, the map 'dueInterests' will be composed
     */
    private int calculateBondInterest (PublicCompany company) {

        if (!company.hasBonds()) return 0;

        int bonds, payout;
        int total = 0;
        dueInterests.clear();
        for (Player player : playerManager.getPlayers()) {
            bonds = player.getPortfolioModel().getBondsCount(company);
            if (bonds > 0) {
                payout = calculateBondInterestPayout(company, player.getPortfolioModel());
                dueInterests.put (player.getPortfolioModel(), payout);
                total += payout;
            }
        }
        bonds = pool.getBondsCount(company);
        if (bonds > 0) {
            payout = calculateBondInterestPayout(company, pool);
            dueInterests.put (pool, payout);
            total += payout;
        }
        return total;
    }

    private int calculateBondInterestPayout(PublicCompany company, PortfolioModel portfolio) {

        return portfolio.getBondsCount(company)
                * company.getPriceOfBonds()
                * company.getBondsInterest() / 100;
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
    protected void executeTakeLoans(int number) {
        super.executeTakeLoans(number);
        getRoot().getStockMarket().moveLeftOrDown(operatingCompany.value(), number);
    }

    @Override
    protected boolean repayLoans(RepayLoans action) {

        String errMsg = validateRepayLoans(action);

        if (errMsg != null) {
            DisplayBuffer.add(this, LocalText.getText("CannotRepayLoans",
                    action.getCompanyName(), action.getNumberRepaid(),
                    bank.format(action.getPrice()), errMsg));
            return false;
        }

        int repayNumber = action.getNumberRepaid();
        if (repayNumber > 0) {
            executeRepayLoans(action);
            getRoot().getStockMarket().moveRightOrUp(getOperatingCompany(), repayNumber);
        }
        nextStep();
        return true;
    }

    @Override
    public void setBuyableTrains() {

        super.setBuyableTrains();

        PublicCompany company = operatingCompany.value();
        boolean buyerHasLoan = company.getLoanValue() > 0;

        for (PossibleAction action : possibleActions.getType(BuyTrain.class)) {
            BuyTrain bt = (BuyTrain) action;
            Owner seller = ((BuyTrain) action).getFromOwner();
            if (seller instanceof PublicCompany
                   && !company.mustTradeTrainsAtFixedPrice()
                   && !((PublicCompany)seller).mustTradeTrainsAtFixedPrice()) {

                boolean sellerHasLoan = ((PublicCompany) seller).getLoanValue() > 0;
                log.debug("Seller {} BuyerLoan {} SellerLoan {}", seller, buyerHasLoan, sellerHasLoan);

                if (buyerHasLoan || sellerHasLoan) {
                    // Set a fixed price or price limit
                    // The somewhat complex relationship between fixedCost and mode
                    // is explained in the Javadoc of the Mode enum in the BuyTrain class.
                    // Mode is only effective if a nonzero fixed price has been set
                    bt.setFixedCost(bt.getTrain().getCost());
                    if (buyerHasLoan && !sellerHasLoan) {
                        bt.setFixedCostMode(BuyTrain.Mode.MAX);
                    } else if (sellerHasLoan && !buyerHasLoan) {
                        bt.setFixedCostMode(BuyTrain.Mode.MIN);
                    } else {
                        bt.setFixedCostMode(BuyTrain.Mode.FIXED);
                    }
                }
            }
        }
    }

    @Override
    public boolean buyTrain(BuyTrain action) {

        boolean result = super.buyTrain(action);

        GameManager_1826 gm = (GameManager_1826)getRoot().getGameManager();
        TrainType type = action.getType();
        int[] eTrainStops = new int[] {2,3,3,4};
        String[] scoreFactors = new String[] {"single", "double"};

        // Change E-train properties as these and the first TGV are bought
        if (type.getName().equals("E") && action.getFromName().equals("IPO")) {
            gm.addETrainsBought();
            type.setMajorStops(eTrainStops[(gm.getETrainsBought()) - 1]);
            ReportBuffer.add(this, LocalText.getText("TrainScoreAndReach",
                    type.getName(),
                    LocalText.getText(scoreFactors[type.getCityScoreFactor() - 1]),
                    type.getMajorStops()));

        } else if (type.getName().equals("TGV")) {
            if (!gm.getTgvTrainBought()) {
                TrainType eType = getRoot().getTrainManager().getTrainTypeByName("E");
                eType.setMajorStops(5);
                eType.setCityScoreFactor(1);
                gm.setTgvTrainBought(true);
                ReportBuffer.add(this, LocalText.getText("TrainScoreAndReach",
                        eType.getName(),
                        LocalText.getText(scoreFactors[type.getCityScoreFactor() - 1]),
                        eType.getMajorStops()));
            }
        }

        return result;
    }

}
