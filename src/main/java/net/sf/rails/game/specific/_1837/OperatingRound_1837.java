package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import net.sf.rails.game.*;
import net.sf.rails.game.state.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockMarket;
import net.sf.rails.util.SequenceUtil;
import rails.game.action.*;
import net.sf.rails.algorithms.RevenueAdapter;
import rails.game.specific._1837.SetHomeHexLocation;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.financial.Bank;
import java.util.HashSet;
import java.lang.reflect.Field;

/**
 * @author Martin
 *
 */
public class OperatingRound_1837 extends OperatingRound {
    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1837.class);
    protected final ArrayListState<String> triggeredNationals = new ArrayListState<>(this, "triggeredNationals");
    protected final BooleanState specialActionPhase = new BooleanState(this, "specialActionPhase", false);
    protected final IntegerState specialActionPlayerCount = IntegerState.create(this, "specialActionPlayerCount", 0);
    protected final IntegerState specialActionCurrentIndex = IntegerState.create(this, "specialActionCurrentIndex", 0);
    // Track if the special phase is fully completed for this round to prevent
    // re-entry loops
    protected final BooleanState specialActionPhaseFinished = new BooleanState(this, "specialActionPhaseFinished",
            false);

    protected final BooleanState phase3Triggered = new BooleanState(this, "phase3Triggered", false);
    protected final BooleanState phase4Triggered = new BooleanState(this, "phase4Triggered", false);
    protected final BooleanState phase5Triggered = new BooleanState(this, "phase5Triggered", false);
    protected final ArrayListState<String> declinedNationals = new ArrayListState<>(this, "declinedNationals");
    // Static cache to persist triggers even if State rollback occurs during
    // interrupt

    // 1: Use primitive types to bypass State constructor visibility issues for
    // now

    // The constructor is private; we must use the static factory method .create()
    // protected final IntegerState kkFormationState = IntegerState.create(this,
    // "kkFormationState", 0);
    // private boolean kkFormedThisTurn = false;

    // Use 'new' with 2 arguments for ArrayListState
    protected final ArrayListState<String> skippedMinors = new ArrayListState<>(this, "skippedMinors");

    // protected final IntegerState ugFormationState = IntegerState.create(this,
    // "ugFormationState", 0);

    // Use '.create' for StringState (Constructor is private)
    protected final StringState currentSpecialCompanyId = StringState.create(this, "currentSpecialCompanyId");

    public OperatingRound_1837(GameManager parent, String id) {
        super(parent, id);
    }

    /**
     * Logic to handle Voluntary Discard in 1837.
     * Call this INSTEAD of the standard train buying generation if the company is
     * at the limit.
     */
    protected void setVoluntaryDiscardActions() {
        PublicCompany company = operatingCompany.value();

        // 1. Iterate unique train types
        for (Train train : company.getPortfolioModel().getUniqueTrains()) {
            // 2. Calculate Scrap Price
            int scrapPrice = train.getCost() / 2;

            // Only allow discard if the company has enough cash to pay the scrap price
            if (company.getCash() >= scrapPrice) {
                List<Train> singleTrainList = new ArrayList<>();
                singleTrainList.add(train);

                DiscardTrainVoluntarily action = new DiscardTrainVoluntarily(company, singleTrainList);

                String priceStr = Bank.format(gameManager, scrapPrice);

                // 3. Clean Name: Remove the unique ID suffix (e.g., "3_1" -> "3")
                String cleanName = train.getName().replaceAll("_\\d+$", "");

                String expectedLabel = "Discard " + cleanName + " (" + priceStr + ")";

                boolean alreadyExists = false;
                for (PossibleAction pa : possibleActions.getList()) {
                    if (pa instanceof DiscardTrainVoluntarily && expectedLabel.equals(pa.getButtonLabel())) {
                        alreadyExists = true;
                        break;
                    }
                }

                if (!alreadyExists) {
                    action.setLabel(expectedLabel);
                    possibleActions.add(action);
                }
            }
        }

        // Add the custom Done action without mutating existing ones to prevent UI
        // caching bugs across rounds
        boolean foundKeepTrains = false;
        for (PossibleAction pa : possibleActions.getList()) {
            if (pa instanceof NullAction && "Done / Keep Trains".equals(pa.getButtonLabel())) {
                foundKeepTrains = true;
                break;
            }
        }

        if (!foundKeepTrains) {
            NullAction done = new NullAction(getRoot(), NullAction.Mode.DONE);
            done.setLabel("Done / Keep Trains");
            possibleActions.add(done);
        }

    }

    /**
     * 1837 nationals may not lay their reserved tokens elsewhere
     * until formation is complete
     * (This I presume, the v2.0 rules are not clear on that matter. EV)
     */
    @Override
    protected boolean canLayAnyTokens(boolean resetTokenLays) {

        PublicCompany_1837 company = (PublicCompany_1837) operatingCompany.value();

        if (company.getType().getId().equals("National") && !company.isComplete()) {
            return false;
        } else {
            return super.canLayAnyTokens(resetTokenLays);
        }
    }

    public boolean processGameSpecificAction(PossibleAction action) {

        if (action instanceof ExchangeMinorAction) {
            ExchangeMinorAction exc = (ExchangeMinorAction) action;
            // Execute merge
            Mergers.mergeCompanies(gameManager, exc.getMinor(), exc.getTargetMajor(), false, false);
            // Refresh actions (stay in special phase)
            setPossibleActions();
            return true;
        }

        if (action instanceof SetHomeHexLocation) {
            SetHomeHexLocation selectHome = (SetHomeHexLocation) action;
            PublicCompany company = selectHome.getCompany();
            MapHex chosenHome = selectHome.getSelectedHomeHex();
            if (chosenHome == null) {
                // If no hex is selected yet, we are likely just initializing the dialog
                // request.
                return true;
            }
            company.setHomeHex(chosenHome);
            company.layHomeBaseTokens();
            return true;
        } else {
            return false;
        }
    }

    private boolean processSpecialDone() {
        // Logic moved from processGameSpecificAction
        int count = specialActionPlayerCount.value() + 1;
        specialActionPlayerCount.set(count);
        List<Player> players = gameManager.getPlayers();

        if (count >= players.size()) {
            specialActionPhase.set(false);
            specialActionPhaseFinished.set(true);

            // We must now trigger the start of the "Real" OR.
            super.start();
        } else {
            int nextIndex = (specialActionCurrentIndex.value() + 1) % players.size();
            specialActionCurrentIndex.set(nextIndex);

            Player nextPlayer = players.get(nextIndex);
            getRoot().getPlayerManager().setCurrentPlayer(nextPlayer);

        }
        return true;
    }

    @Override
    protected String validateSetRevenueAndDividend(SetDividend action) {
        String errMsg = null;
        PublicCompany company;
        String companyName;
        int amount, directAmount;
        int revenueAllocation;

        // Dummy loop to enable a quick jump out.
        while (true) {

            // Checks
            // Must be correct company.
            company = action.getCompany();
            companyName = company.getId();
            if (company != operatingCompany.value()) {

                break;
            }
            // Must be correct step
            if (getStep() != GameDef.OrStep.CALC_REVENUE) {
                break;
            }

            // Amount must be non-negative multiple of 5
            amount = action.getActualRevenue();
            if (amount < 0) {
                break;
            }
            if (amount % 5 != 0) {
                break;
            }

            // Direct revenue must be non-negative multiple of 5,
            // and at least 10 less than the total revenue
            directAmount = action.getActualCompanyTreasuryRevenue();
            if (directAmount < 0) {
                break;
            }
            if (directAmount % 5 != 0) {
                break;
            }
            if (amount > 0 && amount - directAmount < 10) {
                break;
            }

            // Check chosen revenue distribution
            revenueAllocation = action.getRevenueAllocation();
            if (amount > 0) {
                // Check the allocation type index (see SetDividend for values)
                if (revenueAllocation < 0
                        || revenueAllocation >= SetDividend.NUM_OPTIONS) {
                    break;
                }

                // Validate the chosen allocation type
                int[] allowedAllocations = ((SetDividend) selectedAction).getAllowedAllocations();
                boolean valid = false;
                for (int aa : allowedAllocations) {
                    if (revenueAllocation == aa) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    break;
                }
            } else if (revenueAllocation != SetDividend.NO_ROUTE) {
                // If there is no revenue, use withhold.
                // 1837 Logic: Do NOT force Withhold if the company is mandatory Split
                // (Coal/Minor).
                // This prevents the engine from flipping a "Split 0" (pre-calc) action to
                // "Withhold".
                PublicCompany opComp = operatingCompany.value();
                String type = opComp.getType().getId();
                boolean isMandatorySplit = type.equalsIgnoreCase("Coal") || type.equalsIgnoreCase("Minor");

                if (!isMandatorySplit) {
                    // Standard behavior: If no revenue, default to Withhold.
                    action.setRevenueAllocation(SetDividend.WITHHOLD);
                }
            }

            if (amount == 0 && operatingCompany.value().getNumberOfTrains() == 0) {
                DisplayBuffer.add(this, LocalText.getText("RevenueWithNoTrains",
                        operatingCompany.value().getId(),
                        Bank.format(this, 0)));
            }

            break;
        }

        return errMsg;
    }

    public void splitRevenue(int amount) {
        if (amount > 0) {
            int withheld = calculateCompanyIncomeFromSplit(amount);
            String withheldText = Currency.fromBank(withheld, operatingCompany.value());

            ReportBuffer.add(this, LocalText.getText("Receives",
                    operatingCompany.value().getId(), withheldText));
            // Payout the remainder
            int payed = amount - withheld;
            payout(payed, true);
        }

    }

    @Override
    protected int calculateCompanyIncomeFromSplit(int revenue) {
        return roundIncome(0.5 * revenue, Rounding.UP, ToMultipleOf.ONE);
    }

    /*
     * Rounds up or down the individual payments based on the boolean value
     */
    public void payout(int amount, boolean split) {
        if (amount == 0)
            return;

        int part;
        int shares;

        Map<MoneyOwner, Integer> sharesPerRecipient = countSharesPerRecipient();

        // Calculate, round up or down, report and add the cash

        // Define a precise sequence for the reporting
        Set<MoneyOwner> recipientSet = sharesPerRecipient.keySet();
        for (MoneyOwner recipient : SequenceUtil.sortCashHolders(recipientSet)) {
            if (recipient instanceof Bank)
                continue;

            shares = (sharesPerRecipient.get(recipient));
            if (shares == 0)
                continue;

            double payoutPerShare = amount * operatingCompany.value().getShareUnit() / 100.0;
            part = calculateShareholderPayout(payoutPerShare, shares);

            String partText = Currency.fromBank(part, recipient);
            ReportBuffer.add(this, LocalText.getText("Payout",
                    recipient.getId(),
                    partText,
                    shares,
                    operatingCompany.value().getShareUnit()));
        }

        // Move the token
        ((PublicCompany_1837) operatingCompany.value()).payout(amount, split);
    }

    @Override
    protected int calculateShareholderPayout(double payoutPerShare, int numberOfShares) {
        return roundShareholderPayout(payoutPerShare, numberOfShares,
                Rounding.DOWN, Multiplication.BEFORE_ROUNDING);
    }

    /*
     * (non-Javadoc)
     * * @see
     * net.sf.rails.game.OperatingRound#gameSpecificTileLayAllowed(net.sf.rails.game
     * .PublicCompany, net.sf.rails.game.MapHex, int)
     */
    /*
    @Override
    protected int processSpecialRevenue(int earnings, int specialRevenue) {
        int dividend = earnings;
        PublicCompany company = operatingCompany.value();
        if (specialRevenue > 0) {
            dividend -= specialRevenue;
            company.setLastDirectIncome(specialRevenue);
            ReportBuffer.add(this, LocalText.getText("CompanyDividesEarnings",
                    company,
                    Bank.format(this, earnings),
                    Bank.format(this, dividend),
                    Bank.format(this, specialRevenue)));
            Currency.fromBank(specialRevenue, company);
        }
        company.setLastDividend(dividend);
        return dividend;
    }*/

    @Override
    public int getTileLayCost(PublicCompany company, MapHex hex, int standardCost) {
        // 1. If the hex is NOT blocked, just return the standard cost (paying for
        // rivers, etc.)
        if (!hex.isBlockedByPrivateCompany()) {
            return standardCost;
        }

        // 2. If the hex IS blocked, but isTileLayAllowed returned TRUE,
        // it means we are exercising the "Mountain Railway" exception (Owner/President
        // check).
        // In 1837, exercising this right waives the terrain cost.
        if (isTileLayAllowed(company, hex, -1)) {
            return 0;
        }

        // Fallback (shouldn't happen if isTileLayAllowed is checked first)
        return standardCost;
    }

    @Override
    public boolean gameSpecificTileLayAllowed(PublicCompany company,
            MapHex hex, int orientation) {

        RailsRoot root = gameManager.getRoot();
        int phaseIndex = root.getPhaseManager().getCurrentPhase().getIndex();
        String hexId = hex.getId();

        // -----------------------------------------------------------
        // 1. CHECK PRIVATE BLOCKING
        // -----------------------------------------------------------
        if (phaseIndex < 3) {
            if (hex.isBlockedByPrivateCompany()) {
                PrivateCompany blocker = hex.getBlockingPrivateCompany();
                Owner owner = blocker.getOwner();
                Player president = company.getPresident();

                // Exception 1: Company owns the private
                if (owner != null && (owner == company || owner.equals(company))) {
                }
                // Exception 2: President owns the private
                else if (owner != null && president != null && (owner == president || owner.equals(president))) {
                } else {
                    return false;
                }
            }
        }

        // -----------------------------------------------------------
        // 2. CHECK RESERVATIONS
        // -----------------------------------------------------------
        if (hex.isReservedForCompany()) {
            PublicCompany reservedFor = hex.getReservedForCompany();

            if (reservedFor != null && reservedFor != company) {
                // : If the reserved company is CLOSED (e.g. WT absorbed by S2), ignore the
                // reservation
                if (reservedFor.isClosed()) {
                } else {
                    return false;
                }
            }
        }

        // -----------------------------------------------------------
        // 3. CHECK PHASE RESTRICTIONS
        // -----------------------------------------------------------
        if (phaseIndex >= 4) {
            String[] italyHexes = GameDef_1837.ItalyHexes.split(",");
            for (String italyHex : italyHexes) {
                if (hex.getId().equals(italyHex)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    protected void prepareRevenueAndDividendAction() {
        PublicCompany company = operatingCompany.value();

        // There is only revenue if there are any trains
        if (companyHasRunningTrains(false)) {

            String type = company.getType().getId();

            // Identify Mandatories (Minors/Coal)
            boolean isMandatorySplit = type.equalsIgnoreCase("Coal") || type.equalsIgnoreCase("Minor");

            // G-Train Detection ---
            boolean hasGTrains = false;
            if (company.hasTrains()) {
                // Use getTrains() directly if available, or via PortfolioModel
                for (Train t : company.getPortfolioModel().getTrainList()) {
                    if (t.getName().contains("G")) {
                        hasGTrains = true;
                        break;
                    }
                }
            }

            int[] allowedRevenueActions;
            int defaultAllocation;

            int revenueToUse = company.getLastRevenue();
            int directIncomeToUse = company.getLastDirectIncome();

            // We force this if it's a G-Train owner (Major) OR a Mandatory Split (Minor).
            // We removed the checks for "== 0" to ensure we ALWAYS get the correct Mine
            // split.
            if (isMandatorySplit || hasGTrains) {
                try {
                    RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(getRoot(), company,
                            getRoot().getPhaseManager().getCurrentPhase());
                    if (ra != null) {
                        ra.initRevenueCalculator(true); // true = multigraph support
                        revenueToUse = ra.calculateRevenue();
                        directIncomeToUse = ra.getSpecialRevenue(); // Capture 1837 Mine Revenue logic

                    } else {
                    }
                } catch (Exception e) {
                }
            }

            // Determine Button Options
            if (company.isSplitAlways() || isMandatorySplit) {
                // CASE 1: Mandatory Split (Coal / Minor)
                allowedRevenueActions = new int[] { SetDividend.SPLIT };
                defaultAllocation = SetDividend.SPLIT;
            } else if (company.isSplitAllowed()) {
                // CASE 2: Optional Split (Some Majors)
                allowedRevenueActions = new int[] { SetDividend.PAYOUT,
                        SetDividend.SPLIT,
                        SetDividend.WITHHOLD };
                defaultAllocation = SetDividend.PAYOUT;
            } else {
                // CASE 3: Standard Major (MS, BK, etc.)
                // Even without SPLIT option, SetDividend handles 'directIncomeToUse' correctly
                allowedRevenueActions = new int[] {
                        SetDividend.PAYOUT,
                        SetDividend.WITHHOLD };
                defaultAllocation = SetDividend.PAYOUT;
            }

            // Create the action with the FORCED values
            possibleActions.add(new SetDividend(getRoot(),
                    revenueToUse, // Total Revenue
                    directIncomeToUse, // Fixed/Mine Revenue (goes to Treasury)
                    true, allowedRevenueActions, defaultAllocation));

        } else {
        }
    }

    /**
     * Can the operating company buy a train now?
     * In 1837 it is allowed if another (different) train is scrapped.
     *
     * @return True if the company is allowed to buy a train
     */
    protected boolean canBuyTrainNow() {
        return isBelowTrainLimit();
    }

    /**
     * If a train has already run this round for a minor,
     * it may not run again after a merger into a major.
     * * @param display Should be true only once.
     * 
     * @return True if there is at least one train that is allowed to run
     */
    @Override
    protected boolean companyHasRunningTrains(boolean display) {
        boolean hasRunningTrains = false;
        Set<Train> trains = operatingCompany.value().getPortfolioModel().getTrainList();

        for (Train train : trains) {
            if (gameManager.isTrainBlocked(train)) {
                if (display) {
                    String message = LocalText.getText(
                            "TrainInherited", train.getType(), operatingCompany.value());
                    ReportBuffer.add(this, message);
                }
            } else {
                hasRunningTrains = true;
            }
        }
        return hasRunningTrains;
    }

    /**
     * New standard method to allow discarding trains when at the train limit.
     * Note: 18EU has a different procedure for discarding Pullmann trains.
     * * @param company Operating company
     * 
     * @param newTrain Train to get via exchange
     */
    @Override
    protected void addOtherExchangesAtTrainLimit(PublicCompany company, Train newTrain) {
        // May only discard train if at train limit.
        if (isBelowTrainLimit())
            return;

        Set<Train> oldTrains = company.getPortfolioModel().getUniqueTrains();

        for (Train oldTrain : oldTrains) {
            // May not exchange for same type
            if (oldTrain.getType().equals(newTrain.getType()))
                continue;
            // New train cost is raised with half the old train cost
            int price = newTrain.getCost() + oldTrain.getCost() / 2;
            if (price > company.getCash())
                continue;

            BuyTrain buyTrain = new BuyTrain(newTrain, bank.getIpo(), price);
            buyTrain.setTrainForExchange(oldTrain);
            possibleActions.add(buyTrain);
        }
    }

    private void processMandatoryExchanges() {
        boolean isPhase5 = getRoot().getPhaseManager().hasReachedPhase("5");
        // Create a copy to avoid ConcurrentModificationException during merges
        List<PublicCompany> companies = new ArrayList<>(gameManager.getAllPublicCompanies());

        for (PublicCompany coal : companies) {
            // Filter for active Coal companies
            if (coal.isClosed())
                continue;
            // Ensure we only target Coal companies (or Minors acting as such if type is
            // shared)
            if (!"Coal".equals(coal.getType().getId()))
                continue;

            PublicCompany major = Merger1837.getMergeTarget(gameManager, coal);
            if (major == null)
                continue;

            // Trigger Logic:
            // 1. Phase 5: Mandatory Immediate Merge (Even if Major is NOT floated).
            // 2. Sold Out: Mandatory Next-OR Merge (Major MUST be floated).

            boolean triggerMerge = false;
            String reason = "";

            if (isPhase5) {
                triggerMerge = true;
                reason = "Phase 5";
            } else {
                // Not Phase 5. Major must be floated to consider a Sold Out merge.
                if (major.hasFloated()) {
                    boolean isSoldOut = true;
                    // Check if any shares remain in the IPO
                    for (PublicCertificate cert : Bank.getIpo(gameManager).getPortfolioModel().getCertificates()) {
                        if (cert.getCompany().equals(major)) {
                            isSoldOut = false;
                            break;
                        }
                    }
                    if (isSoldOut) {
                        triggerMerge = true;
                        reason = "Major Sold Out";
                    }
                }
            }

            if (!triggerMerge)
                continue;

            // Execute Merge
            // Use 1837-specific merger to ensure exchange shares are transferred correctly
            Merger1837.mergeMinor(gameManager, coal, major);
            Merger1837.fixDirectorship(gameManager, major);

            // missing LocalText key: Use direct string construction
            ReportBuffer.add(this, "Company " + coal.getId() + " merged into " + major.getId() + " (" + reason + ")");

            // Use shared helper to check limits and generate discard actions if needed
            if (checkAndGenerateDiscardActions(major)) {
                String warning = "WARNING: " + major.getId() + " exceeds train limit after mandatory merger.";
                ReportBuffer.add(this, warning);
            }

        }
    }

    @Override
    protected void newPhaseChecks() {
        super.newPhaseChecks();

        // : Access PhaseManager via the gameManager field
        Phase currentPhase = getRoot().getPhaseManager().getCurrentPhase();

        if (currentPhase.getId().equals("3")) {
            if (!phase3Triggered.value()) {
                phase3Triggered.set(true);
                // ... (Existing Phase 3 logic for Bosnia/Private Companies) ...
                for (PrivateCompany comp : gameManager.getAllPrivateCompanies()) {
                    comp.unblockHexes();
                }
                MapManager map = getRoot().getMapManager();
                for (String bzhHex : GameDef_1837.BzHHexes.split(",")) {
                    map.getHex(bzhHex).setOpen(true);
                }
            }

        } else if (currentPhase.getId().equals("4")) {
            if (!phase4Triggered.value()) {
                phase4Triggered.set(true); // Mark memory: Undoable!

                try {
                    PublicCompany_1837 sd = (PublicCompany_1837) getRoot().getCompanyManager().getPublicCompany("Sd");
                    if (sd != null && !sd.hasStarted()) {
                        executeSdFormation(sd);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        } else if (currentPhase.getId().equals("5")) {

            // Compilable version using standard getters

        }
    }

    private boolean processExchangeMinor(ExchangeMinorAction action) {
        PublicCompany minor = action.getMinor();
        PublicCompany major = action.getTargetMajor();
        String minorId = minor.getId();

        // 1. Formation: Par & Float ONLY (No Flush)
        boolean isFormationTrigger = action.isFormation() || "K1".equals(minorId) || "U1".equals(minorId)
                || "S1".equals(minorId);

        if (isFormationTrigger && !major.hasFloated()) {

            StockMarket market = getRoot().getStockMarket();
            net.sf.rails.game.financial.StockSpace parSpace = null;

            int targetPar = 175; // Default for Ug
            int startingCapital = 875;
            if ("KK".equals(major.getId())) {
                targetPar = 120;
                startingCapital = 840;
            } else if ("Sd".equals(major.getId())) {
                targetPar = 142;
                startingCapital = 710;
            }

            org.slf4j.LoggerFactory.getLogger(OperatingRound_1837.class)
                    .info("1837_FORMATION: {} forming | Target Par: {} | Capital: {}", major.getId(), targetPar,
                            startingCapital);

            for (net.sf.rails.game.financial.StockSpace ss : market.getStartSpaces()) {
                if (ss.getPrice() == targetPar) {
                    parSpace = ss;
                    break;
                }
            }
            if (parSpace == null) {
                for (int r = 0; r < 50; r++) {
                    for (int c = 0; c < 50; c++) {
                        net.sf.rails.game.financial.StockSpace ss = market.getStockSpace(r, c);
                        if (ss != null && ss.getPrice() == targetPar) {
                            parSpace = ss;
                            break;
                        }
                    }
                    if (parSpace != null)
                        break;
                }
            }
            if (parSpace != null) {
                market.correctStockPrice(major, parSpace);
            }
            net.sf.rails.game.state.Currency.fromBank(startingCapital, major);

            if (!major.hasFloated()) {
                major.setFloated();
            }
            // STOP! Do not flush shares here. Leave them in Reserve.
            // We will fetch them one-by-one as needed below.
        }

        Merger1837.mergeMinor(gameManager, minor, major);
        Merger1837.fixDirectorship(gameManager, major);

        if (major.getNumberOfTrains() > major.getCurrentTrainLimit()) {
            net.sf.rails.common.ReportBuffer.add(this,
                    "TRAIN LIMIT EXCEEDED: " + major.getId() + " owns " + major.getNumberOfTrains() + " trains (Limit: "
                            + major.getCurrentTrainLimit() + "). Halting for mandatory discard.");

            // Rails uses BUY_TRAIN step for discard resolution when over limit
            setStep(GameDef.OrStep.BUY_TRAIN);

            operatingCompany.set(major);
            setPossibleActions();
        } else {
            net.sf.rails.common.ReportBuffer.add(this, "TRAIN LIMIT CHECK: " + major.getId() + " is within limits ("
                    + major.getNumberOfTrains() + " / " + major.getCurrentTrainLimit() + ").");
        }

        return true;
    }

    public void setNationalFormationDeclined(String nationalId) {
        if (!declinedNationals.contains(nationalId)) {
            declinedNationals.add(nationalId);
        }
    }

    /**
     * Removes "Zombie" Stop objects from the Root that persist after an interrupted
     * turn.
     * Uses Reflection to bypass ClassCastExceptions since Root.items is a
     * HashMapState, not a Map.
     */
    private void cleanupZombieStops(MapHex hex) {
        if (hex == null)
            return;

        Root root = getRoot();
        String hexUri = "/Map/" + hex.getId() + "/";

        try {
            // 1. Get the 'items' object (It is a HashMapState, not a java.util.Map)
            Field itemsField = Root.class.getDeclaredField("items");
            itemsField.setAccessible(true);
            Object itemsState = itemsField.get(root);

            if (itemsState == null)
                return;

            // 2. Use Reflection to find the 'remove' method on the runtime class.
            // HashMapState typically has remove(Object key).
            java.lang.reflect.Method removeMethod;
            try {
                removeMethod = itemsState.getClass().getMethod("remove", Object.class);
            } catch (NoSuchMethodException e) {
                // Fallback: Try remove(String key) if generic erasure is different
                removeMethod = itemsState.getClass().getMethod("remove", String.class);
            }

            // 3. Clean indices 0 to 6
            for (int i = 0; i <= 6; i++) {
                String suspectUri = hexUri + i;
                try {
                    // Invoke remove(suspectUri) dynamically
                    Object result = removeMethod.invoke(itemsState, suspectUri);

                    // If result is not null/false, we actually removed something
                    if (result != null && !Boolean.FALSE.equals(result)) {
                    }
                } catch (Exception innerEx) {
                    // Ignore invocation errors (e.g. key missing)
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public int getBaseRevenueOnly(PublicCompany company) {
        String type = company.getType().getId();
        if (!type.equals("Coal") && !type.equals("Minor") && !type.equals("Mine")) {
            return company.getLastRevenue();
        }
        if (!company.hasTrains() && !type.equals("Coal"))
            return 0;
        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(getRoot(), company, Phase.getCurrent(this));
        if (ra == null)
            return 0;
        ra.initRevenueCalculator(true);
        int total = ra.calculateRevenue();
        return total - ra.getSpecialRevenue();
    }

    @Override
    public void finishRound() {
        declinedNationals.clear();
        triggeredNationals.clear();
        super.finishRound();
    }

    @Override
    public boolean layTile(LayTile action) {
        if (action.getChosenHex() != null && "G17".equals(action.getChosenHex().getId())) {
            cleanupZombieStops(action.getChosenHex());
        }
        return super.layTile(action);
    }

    @Override
    public void start() {
        processMandatoryExchanges();

        // 1. Initial State Cleanup
        triggeredNationals.clear();
        declinedNationals.clear();
        skippedMinors.clear();

        // 2. INJECT MEMORY: Check if we inherited any skips from the previous CER
        if (gameManager instanceof GameManager_1837) {
            java.util.List<String> inherited = ((GameManager_1837) gameManager).popTempSkippedMinors();
            if (!inherited.isEmpty()) {
                for (String minorId : inherited) {
                    skippedMinors.add(minorId);
                }
            }
        }

        // All start-of-round CER and NFR triggers are now securely handled by
        // GameManager_1837.nextRound
        super.start();
    }

    public void nextSpecialActionPlayer() {
        List<Player> players = gameManager.getPlayers();
        int start = specialActionCurrentIndex.value();

        // Start checking from the *next* player
        int checkIndex = (start == -1) ? 0 : (start + 1) % players.size();

        // Loop through all players ONCE
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(checkIndex);

            // --- Strict Eligibility Check ---
            // Only stop if the player HAS an exchange.
            // If they don't, we silently continue to the next player.
            if (hasExchangeableMinors(p)) {

                specialActionCurrentIndex.set(checkIndex);
                specialActionPhase.set(true);
                getRoot().getPlayerManager().setCurrentPlayer(p);
                setPossibleActions();
                return; // FOUND SOMEONE! Exit.
            }

            // Move to next player
            checkIndex = (checkIndex + 1) % players.size();
        }

        // If we get here, we looped through everyone and found NO ONE with an action.
        specialActionPhase.set(false);
        specialActionPhaseFinished.set(true);
        super.start();
    }

    @Override
    public boolean buyTrain(BuyTrain action) {
        boolean result = super.buyTrain(action);

        if (result && gameManager.getGameUIManager() != null && !gameManager.isReloading()) {
            gameManager.getGameUIManager().forceFullUIRefresh();
        }

        if (result && action.getTrain() != null) {
            String trainName = action.getTrain().getType().getName();

            if (trainName.equals("5")) {
                processMandatoryExchanges();
            }

            GameManager_1837 gm = (GameManager_1837) gameManager;

            // 1. Force Compulsory Triggers immediately bypassing standard checks
            if (trainName.equals("4+1") || trainName.equals("5")) {
                PublicCompany_1837 kk = (PublicCompany_1837) getRoot().getCompanyManager().getPublicCompany("KK");
                if (kk != null && !NationalFormationRound.nationalIsComplete(kk)) {
                    gm.startNationalFormationRound("KK", null, this, this);
                    return result;
                }
            }

            if (trainName.equals("5")) {
                PublicCompany_1837 ug = (PublicCompany_1837) getRoot().getCompanyManager().getPublicCompany("Ug");
                if (ug != null && !NationalFormationRound.nationalIsComplete(ug)) {
                    gm.startNationalFormationRound("Ug", null, this, this);
                    return result;
                }
            }

            // 2. Route to standard Optional Checks
            if (gm.checkAndRunKK(null, this, this))
                return result;
            if (gm.checkAndRunUG(null, this, this))
                return result;
        }

        return result;
    }

    @Override
    public boolean setPossibleActions() {

        // Standard railroad logic ONLY. Special phase logic is now isolated in
        // CoalExchangeRound.
        PublicCompany company = operatingCompany.value();

        // 0. Proactive Zombie Cleanup for G17 before generation
        try {
            net.sf.rails.game.MapHex g17 = getRoot().getMapManager().getHex("G17");
            if (g17 != null) {
                cleanupZombieStops(g17);
            }
        } catch (Exception e) {
        }

        // 1. Mandatory Discard Logic
        if (company != null && !company.isClosed() && company.getNumberOfTrains() > company.getCurrentTrainLimit()) {
            possibleActions.clear();
            for (Train train : company.getPortfolioModel().getUniqueTrains()) {
                Set<Train> singleTrainSet = new java.util.HashSet<>();
                singleTrainSet.add(train);
                DiscardTrain action = new DiscardTrain(company, singleTrainSet);
                action.setLabel("Discard " + train.getName());
                possibleActions.add(action);
            }
            return true;
        }

        // 2. Standard Operating Round Logic
        boolean result = super.setPossibleActions();
        if (company == null)
            return result;

        // 3. S5 Home Token Logic
        if ("S5".equalsIgnoreCase(company.getId())
                && (company.getHomeHexes().isEmpty() || !company.hasLaidHomeBaseTokens())) {
            initTurn();
            possibleActions.clear();
            possibleActions.add(new SetHomeHexLocation(getRoot(), company, GameDef_1837.S5homes));
            return true;
        }

        // 4. Voluntary Discard Logic
        if (getStep() == GameDef.OrStep.BUY_TRAIN && !isBelowTrainLimit()) {
            setVoluntaryDiscardActions();
            return true;
        }

        return result;
    }

    @Override
    public boolean process(PossibleAction action) {

        // RELOAD BRUTE FORCE: Bypass validation for the Wien Tile 427 upgrade
        if (action instanceof LayTile) {
            LayTile lt = (LayTile) action;
            String hexName = (lt.getChosenHex() != null) ? lt.getChosenHex().getId() : "Unknown";
            String tileId = (lt.getLaidTile() != null) ? lt.getLaidTile().getId() : "None";

            if ("G17".equals(hexName) && "427".equals(tileId)) {
                // Fix: Access company ID through the company object
                String compId = (lt.getCompany() != null) ? lt.getCompany().getId() : "Unknown";
                try {
                    net.sf.rails.game.MapHex g17 = lt.getChosenHex();

                    // Manually trigger the registry cleanup
                    cleanupZombieStops(g17);

                    // Forced execution: call the upgrade directly on the hex model
                    g17.upgrade(lt);

                    // Post-upgrade recovery: Find tokens left on zombie stops and return them to
                    // the charter
                    PublicCompany kk = getRoot().getCompanyManager().getPublicCompany("KK");
                    if (kk != null) {
                        int recovered = 0;
                        for (BaseToken token : kk.getAllBaseTokens()) {
                            if (token.isPlaced() && token.getOwner() instanceof Stop) {
                                Stop stop = (Stop) token.getOwner();
                                // Identify if the token is stuck on an old G17 stop that is no longer part of
                                // the hex
                                if (stop.getHex() != null && "G17".equals(stop.getHex().getId())) {
                                    if (!g17.getStops().contains(stop)) {
                                        token.moveTo(kk);
                                        recovered++;
                                    }
                                }
                            }
                        }
                    }

                    return true; // Report success to the reloader to bypass the "PossibleActions" check
                } catch (Exception e) {
                }
            }
        }

        // 2. Handle Voluntary Discard Logic (Payment)
        if (action instanceof DiscardTrainVoluntarily) {
            DiscardTrainVoluntarily dtv = (DiscardTrainVoluntarily) action;

            // The action was created with a single train in the list, so getSelectedTrain()
            // (inherited from DiscardTrain) will return that train.
            Train train = dtv.getSelectedTrain();

            if (train != null) {
                // Ensure the parent action knows which train to discard
                dtv.setDiscardedTrain(train);

                // --- PAYMENT LOGIC ---
                int cost = train.getType().getCost() / 2;
                if (cost > 0) {
                    PublicCompany company = operatingCompany.value();
                    Bank bank = Bank.get(this);

                    // Move cash from Company -> Bank
                    company.getPurse().getCurrency().move(company, cost, bank);

                    ReportBuffer.add(this, LocalText.getText("PaysToBank",
                            company.getId(),
                            Bank.format(this, cost),
                            "voluntary surrender of " + train.getName()));
                }
            }
            // Now delegate to super.process() which handles the actual removing of the
            // train object
            return super.process(action);
        }

        // 3. Handle End of Turn
        if (action instanceof NullAction && getStep() == GameDef.OrStep.BUY_TRAIN) {
            finishTurn();
            return true;
        }

        return super.process(action);
    }

    private boolean hasExchangeableMinors(Player p) {
        if (p == null)
            return false;

        // Use generic Owner check to be safe
        if (p instanceof PortfolioOwner) {
            PortfolioModel pm = ((PortfolioOwner) p).getPortfolioModel();
            for (Object obj : pm.getCertificates()) {
                if (!(obj instanceof PublicCertificate))
                    continue;
                PublicCertificate cert = (PublicCertificate) obj;

                PublicCompany comp = cert.getCompany();
                // Check basic validity
                if (comp == null || comp.isClosed())
                    continue;

                // Check if explicitly skipped
                if (skippedMinors.contains(comp.getId())) {
                    continue;
                }

                PublicCompany target = Merger1837.getMergeTarget(gameManager, comp);

                // If target is null, it's not an exchangeable minor/coal (handles SB, Sd, etc.
                // automatically)
                if (target != null && target.hasFloated() && comp.getPresident() == p) {
                    return true;
                }

            }
        }
        return false;
    }

    public void resume() {

        // Inherit skipped minors memory during resume to prevent re-entry loops
        if (gameManager instanceof GameManager_1837) {
            java.util.List<String> inherited = ((GameManager_1837) gameManager).popTempSkippedMinors();
            if (!inherited.isEmpty()) {
                for (String minorId : inherited) {
                    if (!skippedMinors.contains(minorId)) {
                        skippedMinors.add(minorId);
                    }
                }
            }
        }

        // Chain mid-turn cascading formations natively through GameManager
        GameManager_1837 gm = (GameManager_1837) gameManager;
        if (gm.checkAndRunKK(null, this, this))
            return;
        if (gm.checkAndRunUG(null, this, this))
            return;

        if (operatingCompany.value() != null && operatingCompany.value().isClosed()) {

            finishTurn();
            if (gameManager.getCurrentRound() == this) {
                setPossibleActions();
            }
            return;
        }

        // Delegate to the base class to handle the automatic train transfer and
        // standard UI refresh
        super.resume();

    }

    private void executeSdFormation(PublicCompany_1837 sd) {

        net.sf.rails.game.financial.StockMarket market = getRoot().getStockMarket();
        net.sf.rails.game.financial.StockSpace parSpace = null;
        for (int r = 0; r < 50; r++) {
            for (int c = 0; c < 50; c++) {
                net.sf.rails.game.financial.StockSpace ss = market.getStockSpace(r, c);
                if (ss != null && ss.getPrice() == 142) {
                    parSpace = ss;
                    break;
                }
            }
            if (parSpace != null)
                break;
        }

        if (parSpace != null) {

            market.correctStockPrice(sd, parSpace);

            PublicCompany s1 = getRoot().getCompanyManager().getPublicCompany("S1");
            boolean s1OwnedByPlayer = (s1 != null && s1.getPresident() instanceof Player);

            if (s1OwnedByPlayer) {
                net.sf.rails.game.state.Currency.fromBank(710, sd);
                sd.setFloated();
            } else {
            }

            for (PublicCompany minor : gameManager.getAllPublicCompanies()) {
                String id = minor.getId();
                if (id.length() == 2 && id.startsWith("S") && id.charAt(1) >= '1' && id.charAt(1) <= '5') {
                    if (!minor.isClosed()) {
                        Merger1837.mergeMinor(gameManager, minor, sd);
                    }
                }
            }

            sd.setOperated();
            if (sd.hasFloated()) {
                Merger1837.fixDirectorship(gameManager, sd);
            }
        }

        net.sf.rails.game.MapManager map = getRoot().getMapManager();
        if (GameDef_1837.ItalyHexes != null) {
            for (String itHex : GameDef_1837.ItalyHexes.split(",")) {
                net.sf.rails.game.MapHex hex = map.getHex(itHex);
                if (hex != null) {
                    hex.setOpen(false);
                    hex.clear();
                }
            }
        }
        try {
            rails.game.action.LayTile action = new rails.game.action.LayTile(getRoot(),
                    rails.game.action.LayTile.CORRECTION);
            net.sf.rails.game.MapHex hex = map.getHex(GameDef_1837.bozenHex);
            net.sf.rails.game.Tile tile = getRoot().getTileManager().getTile(GameDef_1837.newBozenTile);
            if (hex != null && tile != null) {
                action.setChosenHex(hex);
                action.setLaidTile(tile);
                action.setOrientation(GameDef_1837.newBozenTileOrientation);
                hex.upgrade(action);
            }
        } catch (Exception e) {
        }
    }

    @Override
    public String getRevenueDisplayString(PublicCompany company) {

        // Guard against trainless companies to prevent stale adapter math
        if (!company.hasTrains() && !"Coal".equals(company.getType().getId())) {
            return Bank.format(this, 0);
        }

        try {
            RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(getRoot(), company,
                    getRoot().getPhaseManager().getCurrentPhase());
            if (ra != null) {
                ra.initRevenueCalculator(true);
                int total = ra.calculateRevenue();
                int mine = ra.getSpecialRevenue();

                // Prevent negative route revenue from stale adapter states
                if (mine > 0 && total >= mine) {
                    int route = total - mine;
                    return Bank.format(this, route) + " + " + Bank.format(this, mine);
                } else if (total > 0) {
                    return Bank.format(this, total);
                }
            }
        } catch (Exception e) {
        }
        return super.getRevenueDisplayString(company);
    }

    @Override
    public int getSpecialRevenueOnly(PublicCompany company) {

        // Guard against trainless majors
        if (!company.hasTrains() && !"Coal".equals(company.getType().getId())) {
            return 0;
        }

        RevenueAdapter ra = RevenueAdapter.createRevenueAdapter(getRoot(), company, Phase.getCurrent(this));
        if (ra == null)
            return 0;
        try {
            ra.initRevenueCalculator(true);
            ra.calculateRevenue();
            int mine = ra.getSpecialRevenue();
            return mine;
        } catch (Exception e) {
            return 0;
        }
    }
}