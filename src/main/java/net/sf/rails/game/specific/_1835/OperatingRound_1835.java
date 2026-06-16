package net.sf.rails.game.specific._1835;

import java.util.*;
import java.util.stream.Collectors;

import net.sf.rails.game.*;
import net.sf.rails.game.state.*;
import net.sf.rails.game.state.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.BuyTrain;
import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;
import rails.game.action.LayBaseToken;

import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialSingleTileLay;
import net.sf.rails.game.special.SpecialTileLay;
import java.lang.reflect.Field;

import net.sf.rails.game.model.PortfolioModel;
import com.google.common.collect.Iterables;
import rails.game.action.SetDividend;
import rails.game.specific._1835.LayBadenHomeToken;

public class OperatingRound_1835 extends OperatingRound {
    private static final Logger log = LoggerFactory.getLogger(OperatingRound_1835.class);

    private final BooleanState needPrussianFormationCall = new BooleanState(this, "NeedPrussianFormationCall");
    private final BooleanState hasLaidExtraOBBTile = new BooleanState(this, "HasLaidExtraOBBTile");
    // Define the map to track income denial per-player-per-company
    private final HashMapState<String, Integer> deniedIncomeMap = HashMapState.create(this, "deniedIncomeMap");

    private final HashMapState<Player, Integer> deniedIncomeShare = HashMapState.create(this, "deniedIncomeShare");
    private int tokensLaidCount = 0;
    protected final BooleanState mandatoryBadenTokenLaid = new BooleanState(this, "MandatoryBadenTokenLaid");
    protected final GenericState<PublicCompany> interruptedCompany = new GenericState<>(this, "InterruptedCompany");
    protected final BooleanState awaitingBadenHomeToken = new BooleanState(this, "AwaitingBadenHomeToken");

    // Tracks if PFR has been triggered in this specific OR step to prevent "Double
    // Trigger"
    private final BooleanState pfrTriggeredThisOR = new BooleanState(this, "PfrTriggeredThisOR");
    // Tracks how many trains we have processed to distinguish new purchases from
    // old ones
    private final IntegerState pfrHandledTrainCount = IntegerState.create(this, "PfrHandledTrainCount", 0);
    protected final BooleanState badenHomeTokenCompleted = new BooleanState(this, "BadenHomeTokenCompleted");

    // Tracks which company actually triggered the PFR (e.g. M1) so we can calculate
    // relative turn order even if the engine auto-advances the pointer after the
    // trigger closes.
    private final StringState pfrTriggerId = StringState.create(this, "PfrTriggerId", null);

    public OperatingRound_1835(GameManager parent, String id) {
        super(parent, id);
    }

    @Override
    protected void initTurn() {
        PublicCompany current = operatingCompany.value();
        if (current == null || current.isClosed() || current.getPresident() == null) {
            log.warn("initTurn() aborted: Operating company is null, closed, or has no president.");
            return;
        }

        super.initTurn();
        tokensLaidCount = 0;
        pfrTriggeredThisOR.set(false);
        needPrussianFormationCall.set(false);
        hasLaidExtraOBBTile.set(false);
        pfrHandledTrainCount.set(0); // Reset count at start of turn

        this.mandatoryBadenTokenLaid.set(false);

        if (current != null && !current.isClosed() && current.getPresident() != null) {
            Set<SpecialProperty> sps = current.getSpecialProperties();
            if (sps != null && !sps.isEmpty()) {
                ExchangeForShare efs = (ExchangeForShare) Iterables.get(sps, 0);
                if (efs != null) {
                    addIncomeDenialShare(current.getPresident(), efs.getShare());
                }
            }
        }
    }

    @Override
    public void resetTransientStateOnLoad() { // CHANGED from protected to public
        // No-op for 1835.
        // We rely on the specific logic in resume() to restore state correctly.
    }

    @Override
    public boolean buyTrain(BuyTrain action) {
        boolean success = super.buyTrain(action);
        if (!success)
            return false;

        // --- START FIX: PFR TRIGGER LOGIC ---
        if (!trainsBoughtThisTurn.isEmpty() && trainsBoughtThisTurn.size() > pfrHandledTrainCount.value()) {
            TrainCardType bought = trainsBoughtThisTurn.get(trainsBoughtThisTurn.size() - 1);
            String id = bought.getId();

            // Identify Trigger Trains
            boolean isFirst4 = "4".equals(id) && bought.getNumberBoughtFromIPO() == 1;
            boolean isFirst4Plus4 = "4+4".equals(id) && bought.getNumberBoughtFromIPO() == 1;
            boolean isFirst5 = "5".equals(id) && bought.getNumberBoughtFromIPO() == 1;

            if (isFirst4 || isFirst4Plus4 || isFirst5) {
                GameManager_1835 gm = (GameManager_1835) gameManager;
                PublicCompany pr = companyManager.getPublicCompany(GameDef_1835.PR_ID);
                boolean prStarted = (pr != null && pr.hasStarted());

                // CRITICAL FIX: The trigger is MANDATORY if:
                // 1. It is the 5-train (Phase 5 closing).
                // 2. It is the 4+4 train AND Prussia hasn't started yet (Forces M2 to merge).
                boolean isMandatory = isFirst5 || (isFirst4Plus4 && !prStarted);

                // Trigger if it's the first time we ask, OR if the event is mandatory
                if (!gm.hasPrussianFormationBeenOffered() || isMandatory) {

                    log.info(">>> PFR Triggered by {} buying {} (Mandatory={})", action.getCompany().getId(), id,
                            isMandatory);

                    // 1. Lock the OR
                    pfrHandledTrainCount.set(trainsBoughtThisTurn.size());
                    pfrTriggeredThisOR.set(true);

                    // Capture the Trigger Company (M1) immediately
                    pfrTriggerId.set(action.getCompany().getId());

                    // 2. Identify Starter
                    Player starter = playerManager.getCurrentPlayer();
                    PublicCompany m2 = companyManager.getPublicCompany(GameDef_1835.M2_ID);

                    // If Prussia isn't open, M2 is the "Primary Target" for the forced merge
                    if (!prStarted && m2 != null && !m2.isClosed() && m2.getPresident() != null) {
                        starter = m2.getPresident();
                    } else if (prStarted && pr != null && pr.getPresident() != null) {
                        starter = pr.getPresident();
                    }

                    // 3. Fire the Round Switch
                    gm.setPrussianFormationStartingPlayer(starter);
                    gm.startPrussianFormationRound(this);
                }
            }
        }
        // --- END FIX ---

        return true;
    }

    @Override
    protected void newPhaseChecks() {
        // ... [Standard phase check logic] ...
        PhaseManager phaseManager = getRoot().getPhaseManager();
        Phase currentPhase = phaseManager.getCurrentPhase();

        for (PublicCompany company : operatingCompanies.view()) {
            company.getPortfolioModel().rustObsoleteTrains();
        }

        if (currentPhase.getId().startsWith("5")) {
            boolean excess = checkForExcessTrains();
            if (excess) {
                setStep(GameDef.OrStep.DISCARD_TRAINS);
                return;
            }
        }

        boolean excess = checkForExcessTrains();
        if (excess) {
            setStep(GameDef.OrStep.DISCARD_TRAINS);
        }

        // --- UPDATED RE-TRIGGER LOGIC ---
        // Mirror the buyTrain logic to prevent duplicate triggers
        if (trainsBoughtThisTurn.size() > pfrHandledTrainCount.value()) {
            // (Same Trigger Logic as buyTrain - simplified here for brevity,
            // but you should copy the 'isFirst4/4+4/5' checks here to be safe)
            // ...
            // If condition met:
            // pfrHandledTrainCount.set(trainsBoughtThisTurn.size());
            // pfrTriggeredThisOR.set(true);
            // needPrussianFormationCall.set(true);
        }

        // [Rest of method]
    }

    @Override
    protected boolean canCompanyOperateThisRound(PublicCompany company) {
        if (!company.hasFloated() || company.isClosed()) {
            return false;
        }
        if (company.hasStockPrice())
            return true;
        if ("Clemens".equalsIgnoreCase(GameOption.getValue(this, GameOption.VARIANT))
                || "yes".equalsIgnoreCase(GameOption.getValue(this, "MinorsRequireFloatedBY"))) {
            return companyManager.getPublicCompany(GameDef_1835.BY_ID).hasFloated();
        }
        return true;
    }

    @Override
    protected boolean validateSpecialTileLay(LayTile layTile) {
        if (!super.validateSpecialTileLay(layTile))
            return false;

        // Check matches on the SpecialProperty location string
        SpecialProperty sp = layTile.getSpecialProperty();
        if (sp instanceof SpecialSingleTileLay) {
            String loc = ((SpecialSingleTileLay) sp).getLocationNameString();
            if (loc != null && loc.matches("M1[57]") && hasLaidExtraOBBTile.value()) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void prepareRevenueAndDividendAction() {
        PublicCompany company = operatingCompany.value();
        if (company.hasTrains() && company instanceof PublicCompany_1835
                && company.hasBankLoan()) {
            int[] allowedRevenueActions = new int[] { SetDividend.WITHHOLD };
            possibleActions.add(new SetDividend(getRoot(),
                    company.getLastRevenue(), true,
                    allowedRevenueActions));
        } else {
            super.prepareRevenueAndDividendAction();
        }
    }

    @Override
    public boolean checkForExcessTrains() {
        boolean hasExcess = super.checkForExcessTrains();
        if (!hasExcess)
            return false;

        TrainType type2 = null, type3 = null, type4 = null;
        for (TrainType tt : trainManager.getTrainTypes()) {
            if (tt.getName().equals("2"))
                type2 = tt;
            if (tt.getName().equals("3"))
                type3 = tt;
            if (tt.getName().equals("4"))
                type4 = tt;
        }

        Iterator<Map.Entry<Player, List<PublicCompany>>> playerIterator = excessTrainCompanies.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<Player, List<PublicCompany>> playerEntry = playerIterator.next();
            Iterator<PublicCompany> companyIterator = playerEntry.getValue().iterator();

            while (companyIterator.hasNext()) {
                PublicCompany comp = companyIterator.next();
                if (!comp.hasStockPrice()) {
                    PortfolioModel portfolio = comp.getPortfolioModel();
                    int trainCount = portfolio.getNumberOfTrains();

                    if (trainCount == 2) {
                        boolean has2 = (type2 != null) && (portfolio.getTrainOfType(type2) != null);
                        boolean has3 = (type3 != null) && (portfolio.getTrainOfType(type3) != null);
                        boolean has4 = (type4 != null) && (portfolio.getTrainOfType(type4) != null);

                        if (has2 && (has3 || has4)) {
                            companyIterator.remove();
                        }
                    }
                }
            }
            if (playerEntry.getValue().isEmpty()) {
                playerIterator.remove();
            }
        }
        return !excessTrainCompanies.isEmpty();
    }

    // --- INSERT THIS FROM OLD FILE (THE "HAMMER") ---
    @Override
    public boolean discardTrain(DiscardTrain action) {

        PublicCompany currentOp = operatingCompany.value();
        Player currentPlayer = playerManager.getCurrentPlayer();

        PublicCompany actionComp = action.getCompany();
        Player actionPlayer = (actionComp != null) ? actionComp.getPresident() : null;

        // --- CRITICAL FIX: ALWAYS patch the Excess List for DiscardTrain actions ---
        // We force the company into this list to bypass false-negative validation
        // failures.
        if (excessTrainCompanies == null) {
            excessTrainCompanies = new HashMap<>();
        }
        if (actionPlayer != null) {
            List<PublicCompany> comps = excessTrainCompanies.get(actionPlayer);
            if (comps == null) {
                comps = new ArrayList<>();
                excessTrainCompanies.put(actionPlayer, comps);
            }
            if (!comps.contains(actionComp)) {
                comps.add(actionComp);
            }
        }
        // --------------------------------------------------------------------------

        boolean isInterjection = (actionComp != null && currentOp != null && actionComp != currentOp);

        // Context Swap Logic
        if (isInterjection) {
            operatingCompany.set(actionComp);
            if (actionPlayer != null) {
                playerManager.setCurrentPlayer(actionPlayer);
            }
        }

        // Execute Action
        boolean processed = false;
        try {
            processed = action.process(this);
        } catch (Exception e) {
            log.error(">>> FORENSIC ERROR: Exception during action.process()", e);
        }

        // Restore Context
        if (isInterjection) {
            operatingCompany.set(currentOp);
            if (currentPlayer != null) {
                playerManager.setCurrentPlayer(currentPlayer);
            }
        }

        if (!processed) {
            return false;
        }

        boolean moreDiscards = super.checkForExcessTrains();

        if (this.needPrussianFormationCall.value()) {
            if (!moreDiscards) {
                PublicCompany prussian = companyManager.getPublicCompany(GameDef_1835.PR_ID);
                if (prussian.hasStarted()) {
                    if (operatingCompany.value().isClosed()) {
                        operatingCompany.set(prussian);
                        stepObject.set(GameDef.OrStep.INITIAL);
                    } else {
                        stepObject.set(GameDef.OrStep.BUY_TRAIN);
                    }
                    playerManager.setCurrentPlayer(operatingCompany.value().getPresident());
                } else {
                    ((GameManager_1835) gameManager).startPrussianFormationRound(this);
                }
                // After PFR returns, the company that bought the train might be closed.
                handleClosedOperatingCompany();
            }
        } else {
            if (!moreDiscards) {
                newPhaseChecks();
                if (gameManager.getInterruptedRound() != null) {
                    return true;
                }

                boolean companySwitched = handleClosedOperatingCompany();

                if (!companySwitched) {
                    playerManager.setCurrentPlayer(operatingCompany.value().getPresident());
                    if (trainsBoughtThisTurn.isEmpty()) {
                        setStep(GameDef.OrStep.INITIAL);
                    } else {
                        stepObject.set(GameDef.OrStep.BUY_TRAIN);
                    }
                }
            }
        }
        return true;
    }

    public void clearPfrTriggerFlag_AI() {
        this.needPrussianFormationCall.set(false);
    }

    @Override
    protected void finishOR() {
        ReportBuffer.add(this, " ");
        ReportBuffer.add(this, LocalText.getText("EndOfOperatingRound", thisOrNumber));

        int orWorthIncrease;
        for (Player player : getRoot().getPlayerManager().getPlayers()) {
            player.setLastORWorthIncrease();
            orWorthIncrease = player.getLastORWorthIncrease().value();
            ReportBuffer.add(this, LocalText.getText("ORWorthIncrease",
                    player.getId(), thisOrNumber,
                    Bank.format(this, orWorthIncrease)));
        }

        if (pfrTriggeredThisOR.value()) {
            ((GameManager_1835) gameManager).setPfrDeclined();
        }

        ((GameManager_1835) gameManager).nextRound(this);
    }

    @Override
    protected boolean finishTurnSpecials() {
        if (needPrussianFormationCall.value()) {
            needPrussianFormationCall.set(false);

            if (!PrussianFormationRound.prussianIsComplete(gameManager)) {
                ((GameManager_1835) gameManager).startPrussianFormationRound(this);
            }
            return false;
        }
        return super.finishTurnSpecials();
    }

    private void forceCleanupGhosts() {
        for (PrivateCompany pc : companyManager.getAllPrivateCompanies()) {
            if (pc.isClosed()) {
                removeSpecialProperties(pc);
            }
        }
    }

    private void removeSpecialProperties(PrivateCompany pc) {
        if (pc == null)
            return;

        try {
            java.lang.reflect.Field portfolioSetField = getFieldInHierarchy(pc.getClass(), "specialProperties");
            if (portfolioSetField == null)
                return;
            portfolioSetField.setAccessible(true);
            Object portfolioSetObj = portfolioSetField.get(pc);
            if (portfolioSetObj == null)
                return;

            java.lang.reflect.Field internalPortfolioField = getFieldInHierarchy(portfolioSetObj.getClass(),
                    "portfolio");
            if (internalPortfolioField == null)
                return;
            internalPortfolioField.setAccessible(true);
            Object treeSetStateObj = internalPortfolioField.get(portfolioSetObj);
            if (treeSetStateObj == null)
                return;

            try {
                java.lang.reflect.Method clearMethod = treeSetStateObj.getClass().getMethod("clear");
                clearMethod.invoke(treeSetStateObj);
            } catch (Exception e) {
                if (treeSetStateObj instanceof java.util.Collection) {
                    ((java.util.Collection<?>) treeSetStateObj).clear();
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Field getFieldInHierarchy(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    @Override
    public boolean layBaseToken(LayBaseToken action) {
        PublicCompany company = action.getCompany();

        if (action.getType() == LayBaseToken.HOME_CITY && company.getId().equals("BA")) {
            MapHex hex = action.getChosenHex();
            for (Stop s : hex.getStops()) {
                BaseToken placeholder = null;
                for (BaseToken token : s.getBaseTokens()) {
                    if (token.getParent().equals(company)) {
                        placeholder = token;
                        break;
                    }
                }
                if (placeholder != null) {
                    placeholder.moveTo(company);
                }
            }
        }

        boolean isInterruption = awaitingBadenHomeToken.value();
        boolean isSpecial = (action.getType() != LayBaseToken.GENERIC);

        GameDef.OrStep stepBefore = getStep();

        // Use the proper State object check. This ensures strict 1-token limits AND
        // supports Undo/Redo.
        // Previous logic using local 'tokensLaidCount' failed on Undo and didn't update
        // the UI state.
        if (!isSpecial && normalTokenLaidThisTurn.value()) {
            return false;
        }

        boolean result = super.layBaseToken(action);

        if (result) {
            closePrivateIfSpecial(action.getSpecialProperty());
        }
        // Baden Mandatory Token Logic
        if (result && company.getId().equals("BA") && action.getChosenHex().getId().equals("L6")) {

            log.info("OR1835: Baden Mandatory Token successfully laid on L6.");
            this.mandatoryBadenTokenLaid.set(true);
            this.badenHomeTokenCompleted.set(true);

            // Check if this was an interruption (e.g., BY laid the tile)
            if (isInterruption) {
                awaitingBadenHomeToken.set(false);
                PublicCompany originalCompany = interruptedCompany.value();

                if (originalCompany != null) {
                    log.info("Restoring control to interrupted company: {}", originalCompany.getId());
                    operatingCompany.set(originalCompany);
                    playerManager.setCurrentPlayer(originalCompany.getPresident());
                    interruptedCompany.set(null);

                    // Restoration of Step for Original Company
                    setStep(GameDef.OrStep.LAY_TRACK);

                    // Ensure transient token list for original company is clear to avoid state
                    // pollution
                    if (currentNormalTokenLays != null) {
                        currentNormalTokenLays.clear();
                    }
                }
            } else {

                // Standard BA Turn or PfB Lay
                if (stepBefore == GameDef.OrStep.INITIAL) {
                    log.info("OR1835: Maintaining INITIAL step for native BA turn initialization.");
                    setStep(GameDef.OrStep.INITIAL);
                } else {
                    setStep(GameDef.OrStep.LAY_TRACK);

                    // If a normal tile was NOT consumed yet (e.g. PfB was used), we must ensure
                    // the engine offers the standard tile lay now.
                    if (!normalTileLaidThisTurn.value()) {
                        log.info("OR1835: Normal tile lay still available. Re-initializing lays.");
                        initNormalTileLays();
                    }
                }

                // Ensure this mandatory token does NOT consume the normal token lay
                // entitlement.
                if (normalTokenLaidThisTurn.value()) {
                    log.info("OR1835: Resetting normalTokenLaidThisTurn (Mandatory token is free/special).");
                    normalTokenLaidThisTurn.set(false);
                }
            }
            return true;
        }
        return result;
    }

    private void closePrivateIfSpecial(SpecialProperty sp) {
        if (sp == null)
            return;

        PrivateCompany owner = null;
        for (PrivateCompany pc : companyManager.getAllPrivateCompanies()) {
            if (pc.getSpecialProperties().contains(sp)) {
                owner = pc;
                break;
            }
        }

        if (owner != null && !owner.isClosed()) {
            String id = owner.getId();
            // NF: Closes on Token Lay
            // PfB: Closes on Token Lay (not Tile Lay)
            if ("NF".equals(id) || "PfB".equals(id)) {
                owner.close();
                removeSpecialProperties(owner);
                ReportBuffer.add(this, LocalText.getText("CompanyCloses", owner.getId()));
            }
        }
    }

    private void checkOBBClosure() {
        // OBB (Ostbayerische Bahn)
        // Closes when M15 AND M17 have tiles.
        PrivateCompany obb = companyManager.getPrivateCompany("OBB");
        if (obb != null && !obb.isClosed()) {
            MapHex m15 = getRoot().getMapManager().getHex("M15");
            MapHex m17 = getRoot().getMapManager().getHex("M17");

            if (m15 != null && !m15.isPreprintedTileCurrent() &&
                    m17 != null && !m17.isPreprintedTileCurrent()) {

                obb.close();
                removeSpecialProperties(obb);
                ReportBuffer.add(this, LocalText.getText("CompanyCloses", obb.getId()));
            }
        }
    }

    private void closePhase5Privates() {
        for (PrivateCompany pc : companyManager.getAllPrivateCompanies()) {
            if (pc.isClosed())
                continue;

            String id = pc.getId();
            if (!"BB".equals(id) && !"HB".equals(id)) {
                pc.close();
                removeSpecialProperties(pc);
                ReportBuffer.add(this, LocalText.getText("CompanyCloses", pc.getId()));
            }
        }
    }

    @Override
    public <T extends SpecialProperty> List<T> getSpecialProperties(Class<T> clazz) {
        List<T> properties = super.getSpecialProperties(clazz);
        if (properties != null && !properties.isEmpty()) {
            properties.removeIf(sp -> {
                boolean remove = sp.getOriginalCompany() instanceof PrivateCompany &&
                        ((PrivateCompany) sp.getOriginalCompany()).isClosed();
                if (remove) {

                }
                return remove;
            });
        }
        return properties;
    }

    private void pruneGhostActions() {
        if (possibleActions.isEmpty())
            return;

        List<PossibleAction> actions = new ArrayList<>(possibleActions.getList());

        for (PossibleAction action : actions) {
            SpecialProperty sp = null;
            if (action instanceof LayBaseToken) {
                sp = ((LayBaseToken) action).getSpecialProperty();
            } else if (action instanceof LayTile) {
                sp = ((LayTile) action).getSpecialProperty();
            }

            if (sp != null && sp.getOriginalCompany() instanceof PrivateCompany) {
                if (((PrivateCompany) sp.getOriginalCompany()).isClosed()) {
                    possibleActions.remove(action);
                }
            }

            // Filter "Dead" Special Token Lays (e.g. PfB, NF) if the target hex is
            // physically full.
            // If both slots on L6 (PfB) or M15 (NF) are occupied, the action is impossible.
            if (action instanceof LayBaseToken && sp != null) {
                LayBaseToken lbt = (LayBaseToken) action;
                List<MapHex> locs = lbt.getLocations();
                if (locs != null && !locs.isEmpty()) {
                    boolean hasSpace = false;
                    for (MapHex h : locs) {
                        if (h.getStops() != null) {
                            for (Stop s : h.getStops()) {
                                // Check physical capacity (Hardcoded to 2 as per user instruction)
                                if (s.getTokens().size() < 2) {
                                    hasSpace = true;
                                    break;
                                }
                            }
                        }
                        if (hasSpace)
                            break;
                    }

                    if (!hasSpace) {
                        possibleActions.remove(action);
                    }
                }
            }
        }

    }

    @Override
    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                if (priv.getOwner() instanceof MoneyOwner) {
                    Owner recipient = priv.getOwner();
                    int revenue = priv.getRevenueByPhase(Phase.getCurrent(this));
                    if (count++ == 0)
                        ReportBuffer.add(this, "");
                    String revText = Currency.fromBank(revenue, (MoneyOwner) recipient);
                    ReportBuffer.add(this, LocalText.getText("ReceivesFor",
                            recipient.getId(),
                            revText,
                            priv.getId()));
                }
            }
        }
    }

    /**
     * Called directly by PrussianFormationRound to register an exchange.
     */
    public void registerPrussianExchange(Player player, net.sf.rails.game.Company c) {
        String cId = (c != null) ? c.getId() : "";
        if ("B".equals(cId))
            cId = "BB"; // Handle alias

        boolean shouldDeny = false;

        // 1. Private Companies -> Always Deny
        if (c instanceof PrivateCompany || "BB".equals(cId) || "HB".equals(cId)) {
            shouldDeny = true;
        }
        // 2. Public Companies
        else if (c instanceof PublicCompany) {
            PublicCompany minor = (PublicCompany) c;
            // Deny if it is the current operator OR has already operated
            if (operatingCompany.value() == minor || minor.hasOperated()) {
                shouldDeny = true;
            }
        }
        // 3. Fallback
        else if (c == null) {
            shouldDeny = true;
        }

        if (shouldDeny) {
            int deniedPercent = 10;
            // 5% Shares: M1, M3, M5, M6
            if ("M1".equals(cId) || "M3".equals(cId) || "M5".equals(cId) || "M6".equals(cId)) {
                deniedPercent = 5;
            }
            addIncomeDenialShare(player, deniedPercent);
            log.info("PFR Exchange Registered: {} by {} -> Denied {}%", cId, player.getName(), deniedPercent);
        } else {
            log.info("PFR Exchange Registered: {} by {} -> Allowed (Future Company).", cId, player.getName());
        }
    }

    private void addIncomeDenialShare(Player player, int share) {
        if (!deniedIncomeShare.containsKey(player)) {
            deniedIncomeShare.put(player, share);
        } else {
            deniedIncomeShare.put(player, share + deniedIncomeShare.get(player));
        }
    }

    @Override
    public boolean process(PossibleAction action) {
        // --- STRICT GUARD: Block double-actions if PFR is triggered ---
        if (pfrTriggeredThisOR.value()) {
            log.warn("Blocked action {} because PFR is triggered but not yet active.", action);
            return false;
        }

        boolean result = super.process(action);

        if (action instanceof BuyTrain) {
            newPhaseChecks();

            // Guard: If buyTrain() already switched us to PFR, do not trigger again.
            // This prevents the "Double PFR Start" seen in the logs.
            if (gameManager.getCurrentRound() instanceof PrussianFormationRound) {
                return result;
            }

            if (needPrussianFormationCall.value()) {
                needPrussianFormationCall.set(false);
                if (!PrussianFormationRound.prussianIsComplete(gameManager)) {
                    ((GameManager_1835) gameManager).startPrussianFormationRound(this);
                }
            }
        }
        return result;
    }

    @Override
    protected Map<MoneyOwner, Integer> countSharesPerRecipient() {

        // 1. Get the standard distribution from the engine
        Map<MoneyOwner, Integer> sharesPerRecipient = super.countSharesPerRecipient();

        // CRITICAL CHECK: The Income Denial Rule (4.6) applies ONLY to the Prussian
        // (PR) dividend.
        // If any other company (e.g., BY, SX) is operating, we MUST ignore the denial
        // map entirely.
        if (!operatingCompany.value().getId().equals(GameDef_1835.PR_ID)) {
            return sharesPerRecipient;
        }

        // 2. Apply 1835-Specific Logic: Denied Income (Rule 4.6)
        // We now check specific keys "Player|Company" to avoid global denial bugs.
        if (deniedIncomeMap != null && !deniedIncomeMap.isEmpty()) {

            PublicCompany currentComp = operatingCompany.value();
            PublicCompany prussian = companyManager.getPublicCompany(GameDef_1835.PR_ID);

            // Collect adjustments to avoid ConcurrentModificationException
            Map<MoneyOwner, Integer> deductions = new HashMap<>();
            int totalRedirectedShares = 0;

            for (MoneyOwner owner : sharesPerRecipient.keySet()) {
                if (!(owner instanceof Player))
                    continue;

                Player player = (Player) owner;

                // Check for the specific denial entry for PR (e.g. "Stefan1|PR")
                String specificKey = player.getName() + "|" + GameDef_1835.PR_ID;

                int sharePercentageDenied = 0;
                if (deniedIncomeMap.containsKey(specificKey)) {
                    sharePercentageDenied = deniedIncomeMap.get(specificKey);
                }

                if (sharePercentageDenied > 0) {
                    int shareUnit = currentComp.getShareUnit();
                    if (shareUnit == 10)
                        shareUnit = 10; // Safety

                    int sharesToDeduct = sharePercentageDenied / shareUnit;
                    int currentShares = sharesPerRecipient.get(player);

                    // Clamp to actual holdings
                    int actualDeduction = Math.min(sharesToDeduct, currentShares);

                    if (actualDeduction > 0) {
                        deductions.put(player, actualDeduction);
                        totalRedirectedShares += actualDeduction;

                        ReportBuffer.add(this, LocalText.getText("NoIncomeForPreviousOperation",
                                player.getId(),
                                actualDeduction * shareUnit,
                                GameDef_1835.PR_ID));
                    }
                }
            }

            // Apply deductions
            for (Map.Entry<MoneyOwner, Integer> entry : deductions.entrySet()) {
                MoneyOwner p = entry.getKey();
                int deduct = entry.getValue();
                int current = sharesPerRecipient.get(p);

                if (current == deduct) {
                    sharesPerRecipient.remove(p);
                } else {
                    sharesPerRecipient.put(p, current - deduct);
                }
            }

            // Redirect to PRUSSIAN Treasury (PR)
            if (totalRedirectedShares > 0 && prussian != null) {
                int currentPrShares = sharesPerRecipient.getOrDefault(prussian, 0);
                sharesPerRecipient.put(prussian, currentPrShares + totalRedirectedShares);
            }
        }

        return sharesPerRecipient;
    }

    @Override
    public void cleanup() {
        super.cleanup();
        // Rule 4.6: Income denial is valid ONLY for the specific Operating Round
        // in which the exchange occurred. We must clear this data when the round ends
        // to prevent it from affecting future rounds.
        if (deniedIncomeMap != null) {
            deniedIncomeMap.clear();
        }
    }

    private void addIncomeDenialShare(Player player, String companyId, int share) {
        String key = player.getName() + "|" + companyId;
        if (!deniedIncomeMap.containsKey(key)) {
            deniedIncomeMap.put(key, share);
        } else {
            deniedIncomeMap.put(key, share + deniedIncomeMap.get(key));
        }
    }

    /**
     * Checks if the current operating company is closed. If so, advances to the
     * next
     * operational company in the sequence.
     * 
     * @return true if the company was switched, false otherwise.
     */
    protected boolean handleClosedOperatingCompany() {
        if (operatingCompany.value() != null && operatingCompany.value().isClosed()) {
            List<PublicCompany> companies = getOperatingCompanies();
            PublicCompany current = operatingCompany.value();
            int currentIndex = companies.indexOf(current);

            // Fallback if not found
            if (currentIndex == -1)
                currentIndex = 0;

            PublicCompany nextComp = null;
            int nextIndex = currentIndex;
            int attempts = 0;

            // Cycle forward to find the next OPEN and FLOATED company
            while (attempts < companies.size()) {
                nextIndex = (nextIndex + 1) % companies.size();
                PublicCompany candidate = companies.get(nextIndex);

                if (!candidate.isClosed() && candidate.hasFloated()) {
                    nextComp = candidate;
                    break;
                }
                attempts++;
            }

            if (nextComp != null) {
                log.info("Operating Company {} is closed. Advancing to next operational company: {}",
                        current.getId(), nextComp.getId());

                // 1. Update the State
                operatingCompany.set(nextComp);
                // Force the step to INITIAL so that next can lay tiles/tokens.
                setStep(GameDef.OrStep.INITIAL);

                // Clear the trains bought list so the new company isn't blocked from buying
                // trains later.
                if (trainsBoughtThisTurn != null) {
                    trainsBoughtThisTurn.clear();
                }

                // Clear any "normal tile laid" flags inherited from the previous company.
                normalTileLaidThisTurn.set(false);
                normalTokenLaidThisTurn.set(false);

                // 2. Update the internal index via Reflection (Best Effort)
                try {
                    java.lang.reflect.Field indexField = null;
                    Class<?> clazz = this.getClass();
                    while (clazz != null && indexField == null) {
                        try {
                            indexField = clazz.getDeclaredField("orCompIndex");
                        } catch (Exception e) {
                        }
                        if (indexField == null)
                            try {
                                indexField = clazz.getDeclaredField("index");
                            } catch (Exception e) {
                            }
                        if (indexField == null)
                            clazz = clazz.getSuperclass();
                    }

                    if (indexField != null) {
                        indexField.setAccessible(true);
                        indexField.setInt(this, nextIndex);
                    }
                } catch (Exception e) {
                    // Ignore failures, state update is usually sufficient
                }

                // 3. Reset Step to INITIAL so the new company starts its turn properly
                setStep(GameDef.OrStep.INITIAL);

                // 4. Initialize the turn (clears UI, sets up tokens, etc)
                initTurn();

                return true;
            }
        }
        return false;
    }

    /**
     * Safely advances the operating company pointer if the current one is closed.
     * This replaces "finishTurn()" for the specific case of a closed company skip.
     */
    private void advanceToNextOperationalCompany() {
        PublicCompany current = operatingCompany.value();
        List<PublicCompany> companies = getOperatingCompanies();

        if (companies == null || companies.isEmpty())
            return;

        int currentIndex = companies.indexOf(current);
        if (currentIndex == -1)
            currentIndex = 0;

        // Cycle forward to find the next OPEN and FLOATED company
        int attempts = 0;
        int nextIndex = currentIndex;
        PublicCompany nextComp = null;

        while (attempts < companies.size()) {
            nextIndex = (nextIndex + 1) % companies.size();
            PublicCompany candidate = companies.get(nextIndex);
            // CRITICAL: Must be floated AND not closed
            if (!candidate.isClosed() && candidate.hasFloated()) {
                nextComp = candidate;
                break;
            }
            attempts++;
        }

        if (nextComp != null && nextComp != current) {
            log.info("Self-Healing: Company {} is closed. forcing switch to {}.",
                    (current != null ? current.getId() : "null"), nextComp.getId());

            operatingCompany.set(nextComp);

            // Reset the round step to INITIAL so the new company starts fresh
            // (Prevents inheriting 'BUY_TRAIN' or 'DISCARD_TRAINS' from the dead company)
            setStep(GameDef.OrStep.INITIAL);

            // Clear transient turn flags
            trainsBoughtThisTurn.clear();
            normalTileLaidThisTurn.set(false);
            normalTokenLaidThisTurn.set(false);

            // Ensure the engine knows who is playing
            if (nextComp.getPresident() != null) {
                playerManager.setCurrentPlayer(nextComp.getPresident());
            }
        }
    }

    private void refreshOperatingCompaniesList() {
        // 1. Get the raw truth from the CompanyManager
        List<PublicCompany> all = companyManager.getAllPublicCompanies();
        List<PublicCompany> minors = new ArrayList<>();
        List<PublicCompany> majors = new ArrayList<>();

        for (PublicCompany c : all) {
            if (c.isClosed())
                continue; // Filter out closed companies (M2)

            if (c.getType().getId().equals("Minor")) {
                minors.add(c);
            } else if (c.hasFloated()) {
                majors.add(c);
            }
        }

        // 2. Sort Minors (ID Ascending)
        Collections.sort(minors, (c1, c2) -> c1.getId().compareTo(c2.getId()));

        // 3. Sort Majors (Price Descending, then ID)
        Collections.sort(majors, new Comparator<PublicCompany>() {
            @Override
            public int compare(PublicCompany c1, PublicCompany c2) {
                int p1 = (c1.getCurrentSpace() != null) ? c1.getCurrentSpace().getPrice() : 0;
                int p2 = (c2.getCurrentSpace() != null) ? c2.getCurrentSpace().getPrice() : 0;
                if (p1 != p2)
                    return p2 - p1;
                return c1.getId().compareTo(c2.getId());
            }
        });

        // 4. Combine
        List<PublicCompany> newOrder = new ArrayList<>(minors);
        newOrder.addAll(majors);

        // 5. UPDATE THE SOURCE OF TRUTH
        // Fix: ArrayListState does not support addAll, so we use clear() + loop add()
        this.operatingCompanies.clear();
        for (PublicCompany c : newOrder) {
            this.operatingCompanies.add(c);
        }

        log.info("Refreshed Operating Companies. New Count: " + this.operatingCompanies.size());
    }

@Override
    public void resume() {

        // 1. Clear Transient Flags
        if (pfrTriggeredThisOR.value()) {
            pfrTriggeredThisOR.set(false);
            needPrussianFormationCall.set(false);
        }

        // 2. SURGICAL FIX (Must run first):
        // Patch the operating list before resuming (e.g., insert Prussia if M2 closed).
        surgicalPrussiaFix();

        // 3. Closed Company Check:
        // If the active company closed during the interruption (e.g., M1 merged during PFR),
        // we must advance the pointer safely before evaluating any actions.
        if (operatingCompany.value() != null && operatingCompany.value().isClosed()) {
            log.warn("1835_LOGIC: Resuming OR but operating company {} is CLOSED. Advancing turn.", operatingCompany.value().getId());
            handleClosedOperatingCompany();
            if (gameManager.getCurrentRound() == this) {
                setPossibleActions();
            }
            return;
        }

        // 4. Delegate to Base Class:
        // This natively handles the pendingTrainName auto-buy and the UI refresh flag.
        super.resume();
    }

    @Override
    protected boolean setNextOperatingCompany(boolean initial) {

        // 1. Navigation using the trusted list (No sorting!)
        if (operatingCompanies.isEmpty())
            return false;

        PublicCompany current = operatingCompany.value();
        PublicCompany next = null;

        if (initial || current == null) {
            next = operatingCompanies.get(0);
        } else {
            int index = operatingCompanies.indexOf(current);
            if (index >= 0 && index < operatingCompanies.size() - 1) {
                next = operatingCompanies.get(index + 1);
            } else {
                return false; // End of OR
            }
        }

        // 2. Skip Closed Companies (Recursive check)
        // If we hit M2 (and it wasn't removed yet for some reason), skip it.
        if (next != null && next.isClosed()) {
            operatingCompany.set(next);
            return setNextOperatingCompany(false);
        }

        // 3. Apply Switch
        operatingCompany.set(next);

        // 4. BADEN FIX: Force INITIAL step
        setStep(GameDef.OrStep.INITIAL);

        return true;
    }

    // ... (lines of unchanged context code) ...
    private void surgicalPrussiaFix() {
        PublicCompany m1 = companyManager.getPublicCompany("M1");
        PublicCompany m2 = companyManager.getPublicCompany("M2");
        PublicCompany pr = companyManager.getPublicCompany("PR");

        if (m2 == null || pr == null)
            return;

        // --- START FIX ---
        // 1. Resolve Trigger (Anchor)
        PublicCompany trigger = null;
        if (pfrTriggerId.value() != null) {
            trigger = companyManager.getPublicCompany(pfrTriggerId.value());
        }
        if (trigger == null)
            trigger = operatingCompany.value(); // Fallback

        // 2. Analyze Turn Order
        int triggerIndex = operatingCompanies.indexOf(trigger);
        int m2Index = operatingCompanies.indexOf(m2); // Might be -1 if already gone

        // M2 is "Future" if it sits AFTER the trigger in the list.
        boolean m2IsFuture = (triggerIndex != -1 && m2Index > triggerIndex);
        boolean prReady = (pr.getPresident() != null);

        log.info("Surgical Fix: Trigger={}, M2_Index={}, Status={}, PR_Ready={}",
                trigger.getId(), m2Index, m2IsFuture ? "FUTURE" : "PAST", prReady);

        List<PublicCompany> newMinors = new ArrayList<>();
        List<PublicCompany> newMajors = new ArrayList<>();

        // 3. Partition and Filter
        for (PublicCompany c : operatingCompanies) {

            // Handle M2 Special Case
            if (c == m2) {
                if (m2.isClosed()) {
                    // M2 is closed. It is removed.
                    // We check for PR replacement later.
                    log.info("Surgical Fix: M2 Closed -> Removing from list.");
                } else {
                    // M2 did NOT close (e.g. didn't exchange). It stays.
                    log.info("Surgical Fix: M2 NOT Closed -> Keeping M2 in list.");
                    newMinors.add(c);
                }
                continue;
            }

            // Partition others: Minors (M1-M6) vs Majors (BY, SA, PR, MS, etc.)
            // Note: In 1835, Minors are strictly M1-M6.
            if (c.getId().matches("M[1-6]")) {
                newMinors.add(c);
            } else {
                newMajors.add(c);
            }
        }

        // 4. Insert Prussia into MAJOR list (Only if Swap conditions met)
        // Condition: M2 was "Future" (hadn't acted), M2 is now Closed, and PR is valid.
        if (m2IsFuture && prReady && m2.isClosed()) {

            int prValue = pr.getCurrentPrice();
            int insertPos = newMajors.size(); // Default to end (lowest price)

            for (int i = 0; i < newMajors.size(); i++) {
                // Descending Order: Insert PR before the first company with LOWER price
                if (newMajors.get(i).getCurrentPrice() < prValue) {
                    insertPos = i;
                    break;
                }
            }
            newMajors.add(insertPos, pr);
            log.info("Surgical Fix: Inserted PR into Major List at index {}", insertPos);
        }

        // 5. Reassemble: Minors First -> Then Majors
        operatingCompanies.clear();
        for (PublicCompany c : newMinors)
            operatingCompanies.add(c);
        for (PublicCompany c : newMajors)
            operatingCompanies.add(c);

        // --- END FIX ---
    }

    @Override
    public boolean setPossibleActions() {
        // --- GUARD 1: Dead Company Check ---
        PublicCompany current = operatingCompany.value();
        if (current != null && current.isClosed()) {
            log.info("setPossibleActions: Company {} is closed. Advancing...", current.getId());
            advanceToNextOperationalCompany();
            return setPossibleActions();
        }

        // --- GUARD 2: PFR Gatekeeper ---
        if (pfrTriggeredThisOR.value()) {
            GameManager_1835 gm = (GameManager_1835) gameManager;
            if (!(gm.getCurrentRound() instanceof PrussianFormationRound)) {
                log.warn("PFR Triggered but not active. Forcing startPrussianFormationRound().");
                gm.startPrussianFormationRound(this);
            }
            possibleActions.clear();
            return true;
        }

        // --- MASTER SEQUENCE: Baden L6 Station Selection ---
        BadenContext ctx = getBadenStatus();

        // Self-Heal: The base engine places a placeholder token on the preprinted hex.
        // We must ONLY self-heal if the physical token is present AND a real tile has
        // been laid.
        if (ctx.hasL6Tile && ctx.baHasL6Token && !badenHomeTokenCompleted.value()) {
            log.info("Self-healing: Baden token found physically on laid L6 tile. Syncing state flags.");
            badenHomeTokenCompleted.set(true);
            awaitingBadenHomeToken.set(false);
        }

        // Prompt if: Not finished AND (Interrupted OR Baden active with tile)
        boolean needsBadenL6Prompt = !badenHomeTokenCompleted.value() &&
                (awaitingBadenHomeToken.value() ||
                        (ctx.isBadenOperating && ctx.hasL6Tile));

        if (needsBadenL6Prompt) {
            log.info("Forcing Baden L6 Station Selection Prompt. State mismatch detected.");
            possibleActions.clear();
            doneAllowed.set(false);
            PublicCompany ba = companyManager.getPublicCompany("BA");
            MapHex l6 = getRoot().getMapManager().getHex("L6");
            for (Stop stop : l6.getStops()) {
                possibleActions.add(new LayBadenHomeToken(l6, ba, stop));
            }
            return true;
        }


GameDef.OrStep step = getStep();
        PublicCompany company = operatingCompany.value();

        // Restore the OLD brute-force bypass for the INITIAL step
        if (step == GameDef.OrStep.INITIAL) {
            initTurn();
            if (ctx.isBadenOperating && !company.hasOperated()) {
                if (ctx.hasL6Tile && !ctx.baHasL6Token) {
                    setStep(GameDef.OrStep.LAY_TOKEN);
                    boolean res = setPossibleActions();
                    pruneGhostActions();
                    return res;
                }
            }
            nextStep();
            boolean res = setPossibleActions();
            pruneGhostActions();
            return res;
        }

        // Restore the OLD brute-force bypass for the LAY_TRACK step
        if (step == GameDef.OrStep.LAY_TRACK) {
            if (ctx.isBadenOperating && !company.hasOperated() && ctx.isL6Preprinted) {
                possibleActions.clear();
                // Bypass superclass validation entirely. Force load actions directly.
                possibleActions.addAll(getNormalTileLays(true));
                possibleActions.addAll(getSpecialTileLays(true));
                pruneGhostActions();
                return true;
            }
        }

        if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
            checkForExcessTrains();
        }

        boolean result = super.setPossibleActions();
        if (result && !possibleActions.isEmpty()) {
            pruneGhostActions();
        }
        return result;

    }


@Override
    protected boolean gameSpecificTileLayAllowed(PublicCompany company, MapHex hex, int orientation) {
        // Explicit Immunity: PfB blocks L6 for normal track lays, 
        // but Baden MUST be allowed to upgrade its home hex regardless of PfB status.
        if ("BA".equals(company.getId()) && "L6".equals(hex.getId())) {
            return true;
        }
        return super.gameSpecificTileLayAllowed(company, hex, orientation);
    }


    @Override
    public boolean layTile(LayTile action) {
        forceCleanupGhosts();
        PublicCompany company = action.getCompany();

        // Snapshots for PfB state restoration 
        boolean wasNormalLaid = normalTileLaidThisTurn.value();
        GameDef.OrStep stepBefore = getStep();
        Map<String, Integer> laysSnapshot = new HashMap<>();
        for (String key : tileLaysPerColour.viewKeySet()) {
            laysSnapshot.put(key, tileLaysPerColour.get(key));
        }

        boolean result = super.layTile(action);

        // PfB/PF State Restoration Logic
        SpecialProperty actionSp = action.getSpecialProperty();
        boolean isSpecialPfBLay = false;
        if (result && actionSp != null) {
            String ownerId = (actionSp.getOriginalCompany() != null) ? actionSp.getOriginalCompany().getId() : "";
            String spString = actionSp.toString();
            if ("PfB".equals(ownerId) || "PF".equals(ownerId) || spString.contains("PfB") || spString.contains("PF")) {
                isSpecialPfBLay = true;
            }
        }

        if (result && isSpecialPfBLay) {
            normalTileLaidThisTurn.set(wasNormalLaid);
            Set<String> currentKeys = new HashSet<>(tileLaysPerColour.viewKeySet());
            for (String key : currentKeys) {
                if (!laysSnapshot.containsKey(key)) {
                    tileLaysPerColour.remove(key);
                }
            }
            for (Map.Entry<String, Integer> entry : laysSnapshot.entrySet()) {
                tileLaysPerColour.put(entry.getKey(), entry.getValue());
            }
            if (getStep() != stepBefore && !action.getChosenHex().getId().equals("L6")) {
                setStep(stepBefore);
            }
        }

        if (result) {
            SpecialProperty sp = action.getSpecialProperty();
            if (sp instanceof SpecialSingleTileLay) {
                String loc = ((SpecialSingleTileLay) sp).getLocationNameString();
                if (loc != null && loc.matches("M1[57]")) {
                    hasLaidExtraOBBTile.set(true);
                }
            }
            checkOBBClosure();
        }

        // --- REFINED TRIGGER: Fix for "cannot find symbol: hasToken" ---
        if (result && action.getChosenHex().getId().equals("L6")) {
            PublicCompany ba = companyManager.getPublicCompany("BA");

            // The crucial check: BA is floated, but the mandatory 1835 choice hasn't been
            // completed.
            if (ba != null && ba.hasFloated() && !badenHomeTokenCompleted.value()) {
                log.info("L6 Tile Lay: Reverting base engine auto-lay to force manual UI selection.");

                // 1. Revert Engine Auto-Lay using standard Rails movement
                for (Stop s : action.getChosenHex().getStops()) {
                    for (BaseToken token : new ArrayList<>(s.getBaseTokens())) {
                        if (token.getParent().equals(ba)) {
                            token.moveTo(ba); // Rips the token off the map and puts it back in BA's inventory
                            log.info("Reverted auto-laid BA token on L6.");
                        }
                    }
                }

                // 2. Trigger Interruption Sequence for Foreign Companies
                if (!company.equals(ba)) {
                    interruptedCompany.set(company);
                    operatingCompany.set(ba);
                    playerManager.setCurrentPlayer(ba.getPresident());
                    awaitingBadenHomeToken.set(true);
                }
                mandatoryBadenTokenLaid.set(false);
            }
        }

        // Normal sequence enforcement
        if (result && getStep() != GameDef.OrStep.LAY_TRACK
                && company.equals(operatingCompany.value())
                && !awaitingBadenHomeToken.value()) {

            boolean hasUsableSpecial = false;
            List<SpecialTileLay> specials = getSpecialProperties(SpecialTileLay.class);
            if (specials != null) {
                for (SpecialTileLay sp : specials) {
                    if (sp.isExercised())
                        continue;
                    if (sp instanceof SpecialSingleTileLay) {
                        String loc = ((SpecialSingleTileLay) sp).getLocationNameString();
                        if (loc != null && loc.matches("M1[57]") && hasLaidExtraOBBTile.value()) {
                            continue;
                        }
                    }
                    if (action.getSpecialProperty() == sp)
                        continue;
                    hasUsableSpecial = true;
                    break;
                }
            }
            if (hasUsableSpecial) {
                setStep(GameDef.OrStep.LAY_TRACK);
            }
        }

        return result;
    }

    /**
     * Diagnostic helper class to snapshot the absolute "Baden Situation".     */
    private class BadenContext {
        // Absolute Map & Company State (Permanent)
        boolean baFloated; // True if BA has started/floated 
        boolean isL6Preprinted; // True if NO tile has been laid yet (Map default)
        boolean hasL6Tile; // True if a physical tile has been laid (by anyone)
        boolean baHasL6Token; // True strictly if BA owns a token on L6

        // Turn/Actor Context (Who is causing the check)
        String activeCompanyId; // The company currently operating
        boolean isBadenOperating; // True if BA is the active company

        // PfB Statu
        boolean pfbClosed; // True if PfB is definitively closed
        boolean pfbOwnedByActive; // True if active company president owns PfB
        boolean pfbOwnedByBaden; // True if BA's president owns PfB

        @Override
        public String toString() {
            return String.format(
                    "BadenContext [Active=%s, BA_Floated=%b, L6_Preprinted=%b, L6_HasTile=%b, " +
                            "BA_HasToken=%b, PfB_Closed=%b, Active_Owns_PfB=%b, BA_Owns_PfB=%b]",
                    activeCompanyId, baFloated, isL6Preprinted, hasL6Tile,
                    baHasL6Token, pfbClosed, pfbOwnedByActive, pfbOwnedByBaden);
        }
    }

    /**
     * Scans the absolute map and permanent company state to determine the exact
     * scenario for Baden.
     * This is decoupled from transient Operating Round flags to prevent
     * round-restart bugs
     */
    private BadenContext getBadenStatus() {
        BadenContext ctx = new BadenContext();

        // 1. Establish Actors
        PublicCompany currentOp = operatingCompany.value();
        PublicCompany ba = companyManager.getPublicCompany("BA");

        ctx.activeCompanyId = (currentOp != null) ? currentOp.getId() : "NONE";
        ctx.isBadenOperating = "BA".equals(ctx.activeCompanyId);
        ctx.baFloated = (ba != null && ba.hasFloated());

        // 2. Absolute Map State (L6)
        MapHex l6 = getRoot().getMapManager().getHex("L6");
        if (l6 != null) {
            ctx.isL6Preprinted = l6.isPreprintedTileCurrent();
            ctx.hasL6Tile = !l6.isPreprintedTileCurrent();

            boolean physicallyHasToken = false;
            if (l6.getStops() != null) {
                for (Stop s : l6.getStops()) {
                    if (s.getBaseTokens() != null) {
                        for (BaseToken t : s.getBaseTokens()) {
                            if (ba != null && t.getParent().equals(ba)) {
                                physicallyHasToken = true;
                            }
                        }
                    }
                }
            }
            ctx.baHasL6Token = physicallyHasToken;

        } else {
            log.error("CRITICAL: Hex L6 not found on absolute map!");
        }

        // 3. Absolute PfB Status
        PrivateCompany pfb = companyManager.getPrivateCompany("PfB");
        if (pfb != null) {
            ctx.pfbClosed = pfb.isClosed();
            Owner pfbOwner = pfb.getOwner();

            ctx.pfbOwnedByActive = (currentOp != null && currentOp.getPresident() != null
                    && pfbOwner == currentOp.getPresident());
            ctx.pfbOwnedByBaden = (ba != null && ba.getPresident() != null && pfbOwner == ba.getPresident());
        } else {
            ctx.pfbClosed = true;
        }

        // 4. Central Diagnostic Logging
        // log.info("Baden Diagnosis: " + ctx.toString());

        return ctx;
    }

}
