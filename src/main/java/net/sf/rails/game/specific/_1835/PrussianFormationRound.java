package net.sf.rails.game.specific._1835;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.game.action.NullAction;
import rails.game.specific._1835.StartPrussian;
import rails.game.specific._1835.ExchangeForPrussianShare;
import rails.game.action.DiscardTrain;
import rails.game.action.PossibleAction;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.round.I_MapRenderableRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.*; // Added for StringState, BooleanState
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.game.financial.Bank;
import java.util.stream.Collectors;
import com.google.common.collect.Iterables;

public class PrussianFormationRound extends Round implements I_MapRenderableRound {

    private static final Logger log = LoggerFactory.getLogger(PrussianFormationRound.class);

    public static final String PENDING_PFR_STATE_KEY = "PENDING_PFR_OFFER";
    public static final int PFR_PRIORITY = 10;

    private PublicCompany prussian;
    private PublicCompany m2;
    private Phase phase;

    // Transient booleans (Recalculated in resume/start)
    private boolean startPr;
    private boolean forcedStart;
    private boolean mergePr;
    private boolean forcedMerge;

    private final StringState swapOldPresName = StringState.create(this, "SwapOldPresName", null);
    private final StringState swapNewPresName = StringState.create(this, "SwapNewPresName", null);

    public enum Step {
        START,
        MERGE,
        DISCARD_TRAINS,
        PRESIDENCY_SWAP
    }

    // Inner Action Class for the Swap Choice
    public static class PresidencySwapChoice extends PossibleAction {
        private static final long serialVersionUID = 1L;
        private final int optionIndex;

        // FIX: Cast null to (RailsRoot)
        public PresidencySwapChoice(int index) {
            super((RailsRoot) null);
            this.optionIndex = index;
        }

        public int getOptionIndex() {
            return optionIndex;
        }

        @Override
        public String toString() {
            return "SwapOption:" + optionIndex;
        }
    }

    private List<Company> foldablePrePrussians;
    private RoundFacade interruptedRound;

    // We replace raw fields with State objects so the Engine's TransactionManager
    // tracks them.
    // StringState uses a static factory (.create)
    private final StringState stepState = StringState.create(this, "StepState", Step.START.name());

    // BooleanState uses a public Constructor (new ...)
    private final BooleanState roundFinishedState = new BooleanState(this, "RoundFinishedState");

    // StringState uses a static factory (.create)
    private final StringState startingPlayerName = StringState.create(this, "StartingPlayerName", null);

    // Kept from previous fix
    private final IntegerState mergeTurnCount = IntegerState.create(this, "MergeTurnCount", 0);

    // Transient reference (re-fetched via startingPlayerName)
    protected Player startingPlayer;
    protected Player currentPlayer;

    private static String PR_ID = GameDef_1835.PR_ID;
    private static String M2_ID = GameDef_1835.M2_ID;

    public PrussianFormationRound(GameManager parent, String id) {
        super(parent, id);
        guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
        guiHints.setVisibilityHint(GuiDef.Panel.STATUS, true);
        guiHints.setActivePanel(GuiDef.Panel.MAP);
    }

    // --- Helper Methods for State Access ---
    public Step getPrussianStep() {
        try {
            return Step.valueOf(stepState.value());
        } catch (Exception e) {
            return Step.START;
        }
    }

    private void setPrussianStep(Step s) {
        stepState.set(s.name());
    }

    private boolean isRoundFinished() {
        return roundFinishedState.value();
    }

    private void setRoundFinished(boolean val) {
        roundFinishedState.set(val);
    }

    private void setStartingPlayer(Player p) {
        this.startingPlayer = p;
        if (p != null) {
            startingPlayerName.set(p.getName());
        } else {
            startingPlayerName.set(null);
        }
    }

    private Player getStartingPlayer() {
        String name = startingPlayerName.value();
        if (name == null)
            return null;
        for (Player p : playerManager.getPlayers()) {
            if (p.getName().equals(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Called by the engine when loading a save OR performing an UNDO.
     * We must reconstruct transient variables here.
     */
    @Override
    public void resume() {
        super.resume();

        this.interruptedRound = gameManager.getInterruptedRound();
        this.prussian = companyManager.getPublicCompany(PR_ID);
        this.m2 = companyManager.getPublicCompany(M2_ID);
        this.phase = Phase.getCurrent(this);

        // Restore transient logic flags
        this.startPr = !prussian.hasStarted();
        this.forcedMerge = phase.getId().equals("5");
        this.forcedStart = phase.getId().equals("4+4") || forcedMerge;
        this.mergePr = !prussianIsComplete(gameManager);

        // Restore Player References
        this.startingPlayer = getStartingPlayer();
        this.currentPlayer = playerManager.getCurrentPlayer(); // Engine handles this one

    }

    private Player getPlayerByName(String name) {
        if (name == null)
            return null;
        for (Player p : playerManager.getPlayers()) {
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }


public void start() {
        // CRITICAL: Initialize this BEFORE checking for completion/abort.
        this.interruptedRound = gameManager.getInterruptedRound();
        
        log.info("--- PFR STARTING ---");
        PublicCompany pr = companyManager.getPublicCompany(GameDef_1835.PR_ID);
        
        if (PrussianFormationRound.prussianIsComplete(gameManager)) {
            log.warn("PFR Start Aborted: prussianIsComplete() returned TRUE immediately.");
            finishRound();
            return;
        }

        // 1. Determine Force Status
        phase = Phase.getCurrent(this);
        boolean prussianStarted = (pr != null && pr.hasStarted());
        
        forcedMerge = phase.getId().startsWith("5");
        
        // PFR is forced if: Phase 5 OR Phase 4+4 (if PR not started)
        boolean isPhase4Plus4Forced = phase.getId().equals("4+4") && !prussianStarted;
        
        // --- START FIX ---
        // Removed 'isOrTrigger'. Buying a 4-train in an OR should NOT force the formation.
        forcedStart = forcedMerge || isPhase4Plus4Forced;
        // --- END FIX ---

        // Force 'MERGE' step if we are in Phase 5 to ensure cleanup runs
        if (forcedMerge && !PrussianFormationRound.prussianIsComplete(gameManager)) {
            if (getPrussianStep() != Step.MERGE) {
                setPrussianStep(Step.MERGE);
            }
        }
        
        // 2. LOOP GUARD: Check if PFR was already offered.
        if (!forcedStart && gameManager instanceof GameManager_1835
                && ((GameManager_1835) gameManager).hasPrussianFormationBeenOffered()) {
            if (interruptedRound != null) {
                gameManager.setInterruptedRound(null);
                gameManager.setRound(interruptedRound);
                interruptedRound.resume();
                return;
            }
        }

        if (PrussianFormationRound.prussianIsComplete(gameManager)) {
            finishRound();
            return;
        }

        // Initialize locals
        prussian = companyManager.getPublicCompany(PR_ID);
        m2 = companyManager.getPublicCompany(M2_ID);
        startPr = !prussian.hasStarted();
        mergePr = !PrussianFormationRound.prussianIsComplete(gameManager);

        // FINAL SAFETY NET: Ensure currentPlayer is NEVER null
        if (this.currentPlayer == null) {
            this.currentPlayer = playerManager.getCurrentPlayer();
            if (this.currentPlayer == null) {
                if (prussian.getPresident() != null)
                    this.currentPlayer = prussian.getPresident();
                else if (m2 != null && m2.getPresident() != null)
                    this.currentPlayer = m2.getPresident();
                else
                    this.currentPlayer = playerManager.getPriorityPlayer();

                if (this.currentPlayer != null) {
                    playerManager.setCurrentPlayer(this.currentPlayer);
                }
            }
        }

        // DYNAMIC HEADER UPDATE
        if (this.currentPlayer != null) {
            try {
                Class<?> orPanelClass = Class.forName("net.sf.rails.ui.swing.ORPanel");
                java.lang.reflect.Method setHeader = orPanelClass.getMethod("setGlobalCustomHeader", String.class, String.class);
                setHeader.invoke(null, "Prussian Formation", this.currentPlayer.getName() + " to Act");
            } catch (Throwable t) { }
        }

        // --- Only Initialize State if this is a fresh start ---
        if (getPrussianStep() == Step.START && startingPlayerName.value() == null) {

            ReportBuffer.add(this, LocalText.getText("StartFormationRound", PR_ID));
            setPrussianStep(startPr ? Step.START : Step.MERGE);
            mergeTurnCount.set(0);

            Player m2President = (m2 != null) ? m2.getPresident() : null;

            if (getPrussianStep() == Step.START) {
                // Default: M2 President decides
                if (m2President != null) {
                    setStartingPlayer(m2President);
                    setCurrentPlayer(m2President);
                } else {
                    setStartingPlayer(((GameManager_1835) gameManager).getPrussianFormationStartingPlayer());
                    setCurrentPlayer(this.startingPlayer);
                }

                if (forcedStart) {
                    ReportBuffer.add(this, LocalText.getText("PFR_ForcedStart", phase.getId()));
                    executeStartPrussian(true);
                    setPrussianStep(Step.MERGE);

                    // --- START FIX: Enforce Rule 4.3 (Director Starts) ---
                    Player nextPlayer = prussian.getPresident();
                    if (nextPlayer == null) nextPlayer = m2President;
                    if (nextPlayer == null) nextPlayer = playerManager.getPriorityPlayer();

                    setStartingPlayer(nextPlayer);
                    setCurrentPlayer(nextPlayer);
                    // --- END FIX ---

                    setFoldablePrePrussians();
                    if (foldablePrePrussians.isEmpty()) {
                        advanceToNextValidPlayer();
                    }
                }
            } else if (getPrussianStep() == Step.MERGE) {
                Player sp = playerManager.getPriorityPlayer();
                if (sp == null) sp = ((GameManager_1835) gameManager).getPrussianFormationStartingPlayer();

                setStartingPlayer(sp);
                setCurrentPlayer(this.startingPlayer);
                advanceToNextValidPlayer();
            }
        }

        // Phase 5 Forced Merge Cleanup
        if (getPrussianStep() == Step.MERGE && forcedMerge) {
            Set<Company> foldablesSet = new LinkedHashSet<>();
            for (PrivateCompany company : gameManager.getAllPrivateCompanies()) {
                if (!company.isClosed() && hasExchangeProperty(company)) foldablesSet.add(company);
            }
            for (PublicCompany company : gameManager.getAllPublicCompanies()) {
                if (!company.isClosed() && hasExchangeProperty(company)) foldablesSet.add(company);
            }
            if (!foldablesSet.isEmpty()) {
                executeExchange(new ArrayList<>(foldablesSet), false, false);
            }
            finishMergeStep();
        }
    }




    private boolean hasExchangeProperty(Company c) {

        // Whitelist Check: Strictly limit candidates to the 6 Minors and 2 Privates defined in 1835 rules.
        // This prevents Majors (like MS) from being selected due to "ghost" properties inherited from closed privates.
        String id = c.getId();
        boolean isPrussianCandidate = 
               id.equals("M1") || id.equals("M2") || id.equals("M3") 
            || id.equals("M4") || id.equals("M5") || id.equals("M6") 
            || id.equals("BB") || id.equals("HB");
        
        if (!isPrussianCandidate) {
            return false;
        }



        Set<SpecialProperty> sps = c.getSpecialProperties();
        if (sps == null || sps.isEmpty())
            return false;
        // Robust check: Search for the specific property type, don't assume index 0
        for (SpecialProperty sp : sps) {
            if (sp instanceof ExchangeForShare)
                // If a Major Company (has stock price) has this property, it's an error/ghost.
                // We log the property details to find out WHERE it came from.
                if (c instanceof PublicCompany && ((PublicCompany)c).hasStockPrice()) {
                    log.warn("!!! GHOST PROPERTY DETECTED ON MAJOR [{}] !!!", c.getId());
                    log.warn("Property Object: {}", sp);
                    log.warn("Property Class: {}", sp.getClass().getName());
                    log.warn("Property Info: {}", sp.getInfo()); // Often contains "Exchangeable for..."
                }
                return true;
        }
        return false;
    }

    private void setFoldablePrePrussians() {
        foldablePrePrussians = new ArrayList<>();

        if (currentPlayer == null) {
            return;
        }

        // WINDOW CHECK: The optional exchange loop is only valid between the 4-train and 5-train.
        
        // 1. Refresh Phase (Field 'phase' might be stale)
        Phase currentPhase = Phase.getCurrent(this);
        String pId = (currentPhase != null) ? currentPhase.getId() : "";

        // 2. Window Opens: When 4-train is sold (Phase "4"/"4+4") or Prussia has started.
        // We check "startsWith(5)" here to ensure the window is logically 'open' during the forced merge transition,
        // though the 'Closed' check below will handle the exclusion.
        boolean windowOpen = (prussian != null && prussian.hasStarted()) 
                           || pId.contains("4") || pId.startsWith("5");

        if (!windowOpen) {
            return;
        }

        // 3. Window Closes: When 5-train is bought (Phase "5" / Brown).
        // The start() method handles the MANDATORY forced merge for Phase 5.
        // We must DISABLE this interactive/optional loop to prevent infinite prompts.
        if (pId.startsWith("5")) {
             return;
        }


        for (PrivateCompany company : currentPlayer.getPortfolioModel().getPrivateCompanies()) {
            if (!company.isClosed() && hasExchangeProperty(company)) {
                foldablePrePrussians.add(company);
            }
        }
        for (PublicCertificate cert : currentPlayer.getPortfolioModel().getCertificates()) {
            if (!cert.isPresidentShare())
                continue;
            PublicCompany company = cert.getCompany();
            // CRITICAL FILTER: Only "Minor" companies can exchange into Prussia.
            // Majors (like MS) might return true for hasExchangeProperty if they own a private,
            // so we MUST explicitly exclude them by checking the company type.
            if (!company.getType().getId().equalsIgnoreCase("Minor")) {
                continue;
            }
            if (!company.isClosed() && hasExchangeProperty(company)) {
                foldablePrePrussians.add(company);
            }
        }
    }

    @Override
    public boolean process(PossibleAction action) {
        boolean result = false;
        Player currentPlayer = playerManager.getCurrentPlayer();
        String playerName = (currentPlayer == null ? "N/A" : currentPlayer.getName());

        if (action instanceof PresidencySwapChoice) {
            executePresidencySwapChoice((PresidencySwapChoice) action);
            return true;
        }

        if (action instanceof StartPrussian) {
            executeStartPrussian(false);
            setPrussianStep(Step.MERGE);

            // Update State
            setStartingPlayer(((GameManager_1835) gameManager).getPrussianFormationStartingPlayer());
            mergeTurnCount.set(0);

// After starting Prussia, check if this player has other papers (e.g. BB/HB)
            // If not, auto-advance to the next valid player immediately.
            advanceToNextValidPlayer();
            return true;

        } else if (action instanceof ExchangeForPrussianShare) {
            ExchangeForPrussianShare a = (ExchangeForPrussianShare) action;
            executeExchange(Arrays.asList(a.getCompanyToExchange()), false, false);

// If the player has no more shares to exchange, automatically finish their turn.
            setFoldablePrePrussians();
            if (foldablePrePrussians.isEmpty()) {
                finishTurn(); 
                // Note: finishTurn() now calls advanceToNextValidPlayer(), so we are safe.
            }
            
            return true;

        } else if (action instanceof DiscardTrain) {
            discardTrain((DiscardTrain) action);
            return true;

        } else if (action instanceof NullAction) {
            NullAction nullAction = (NullAction) action;

            if (nullAction.getMode() == NullAction.Mode.PASS) {
                result = pass(nullAction, playerName, false);
                return result;
            }

            if (nullAction.getMode() == NullAction.Mode.DONE) {
                finishTurn();
                return true;
            }
        }
        return result;
    }

    protected void finishTurn() {
// We increment the count for the CURRENT player who just finished/passed
        mergeTurnCount.add(1);

        if (mergeTurnCount.value() >= playerManager.getNumberOfPlayers()) {
            finishMergeStep();
            return;
        }

        // Move to next player
        Player nextPlayer = playerManager.getNextPlayer();
        setCurrentPlayer(nextPlayer);

        // Recursively skip any subsequent players who have no shares
        advanceToNextValidPlayer();
    }

    private void finishMergeStep() {
        if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
            setPrussianStep(Step.DISCARD_TRAINS);
            setCurrentPlayer(prussian.getPresident());
        } else {
            finishRound();
        }
    }

    protected boolean pass(NullAction action, String playerName, boolean hasAutopassed) {
        PublicCompany m2 = companyManager.getPublicCompany(GameDef_1835.M2_ID);

        if (getPrussianStep() == Step.START && playerManager.getCurrentPlayer() == m2.getPresident()) {
            ReportBuffer.add(this, playerName + " passes the Prussian Formation option.");

            // 1. Tell the Game Manager we declined, so it doesn't ask again IMMEDIATELY.
            // It will reset this flag when the *next* round finishes.
            if (gameManager instanceof GameManager_1835) {
                ((GameManager_1835) gameManager).setPfrDeclined();
            }

            // 2. Finish this PFR round.
            // This will return control to GameManager.nextRound().
            // GameManager will see pfrDeclined=true, skip the hook, and start the actual
            // OR/SR.
            finishRound();

            return true;
        }
        return false;
    }

    public Player getCurrentPlayer() {
        return this.currentPlayer;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        playerManager.setCurrentPlayer(player);
    }

private void executeStartPrussian(boolean auto) {
        if (m2 == null) {
            m2 = companyManager.getPublicCompany(M2_ID);
        }

        prussian.start();

        String msg = LocalText.getText("START_MERGED_COMPANY",
                PR_ID,
                Bank.format(this, prussian.getIPOPrice()),
                prussian.getStartSpace().toText());
        ReportBuffer.add(this, msg);

        // 1. Merge M2 (Force Exchange)
        // FIX: Added 'true' (isPresident) and 'false' (display) to match the method signature
        executeExchange(Collections.singletonList(m2), true, false);

        prussian.setFloated();
        setPrussianStep(Step.MERGE);

        // 2. Set the Starting Player for the Exchange Phase
        // CORRECTED RULE 4.3: Start with the NEW Prussian Director.
        Player newDirector = prussian.getPresident();
        if (newDirector == null) {
            newDirector = playerManager.getPriorityPlayer(); // Fallback
        }

        setStartingPlayer(newDirector);
        setCurrentPlayer(newDirector);

        // Reset turn count so we cycle through everyone starting with the Director
        mergeTurnCount.set(0);
    }


    /**
     * Expose the step name as a String to avoid Enum visibility issues in generic
     * UI classes.
     */
    public String getPrussianStepName() {
        return getPrussianStep().toString();
    }

    public boolean discardTrain(DiscardTrain action) {
        Train train = action.getDiscardedTrain();
        PublicCompany company = action.getCompany();

        if (company != prussian || train == null || !company.getPortfolioModel().getTrainList().contains(train)) {
            return false;
        }
        train.discard();

        if (prussian.getNumberOfTrains() > prussian.getCurrentTrainLimit()) {
            setPrussianStep(Step.DISCARD_TRAINS);
        } else {
            finishRound();
        }
        return true;
    }

    public static boolean prussianIsComplete(GameManager gameManager) {
        for (PublicCompany company : gameManager.getAllPublicCompanies()) {
            if (!checkForPrussianMinorExchange(company))
                return false;
        }
        for (PrivateCompany company : gameManager.getAllPrivateCompanies()) {
            if (!checkForPrussianPrivateExchange(company))
                return false;
        }
        return true;
    }

    static boolean checkForPrussianMinorExchange(PublicCompany company) {
        if (!company.getType().getId().equalsIgnoreCase("Minor"))
            return true;
        return company.isClosed();
    }

    private static boolean checkForPrussianPrivateExchange(PrivateCompany company) {
        if ((!company.getId().equals("HB")) && (!company.getId().equals("BB")))
            return true;
        return company.isClosed();
    }

    @Override
    protected void finishRound() {
        if (isRoundFinished())
            return;
        setRoundFinished(true);
        // UI CLEANUP: Force ORPanel to clear sticky buttons and reset to "Stock Round"
        // gray state.
        // We use reflection to avoid hard dependency on the UI package.
        try {
            Class<?> orPanelClass = Class.forName("net.sf.rails.ui.swing.ORPanel");
            java.lang.reflect.Method cleanupMethod = orPanelClass.getMethod("forceGlobalCleanup");
            cleanupMethod.invoke(null);
        } catch (Throwable t) {
        }

        if (this.interruptedRound != null) {
            ReportBuffer.add(this, "End of " + GameDef_1835.PR_ID + " formation. Resuming "
                    + this.interruptedRound.getRoundName() + ".");
        } else {
            ReportBuffer.add(this, "End of " + GameDef_1835.PR_ID + " formation.");
        }

        getRoot().getReportManager().getDisplayBuffer().clear();
        PublicCompany prussian = companyManager.getPublicCompany(GameDef_1835.PR_ID);
        if (prussian.hasStarted())
            prussian.checkPresidency();
        prussian.setOperated();

        // Fix for Double-PFR trigger: Ensure GameManager knows PFR has been
        // handled/offered
        // for this phase, so it doesn't trigger again immediately upon resume.
        if (gameManager instanceof GameManager_1835) {
            ((GameManager_1835) gameManager).setPfrDeclined();
        }

        if (this.interruptedRound != null) {
            RoundFacade roundToResume = gameManager.getInterruptedRound();
            gameManager.setInterruptedRound(null);
            gameManager.setRound(roundToResume);

            guiHints.setCurrentRoundType(roundToResume.getClass());
            guiHints.setVisibilityHint(GuiDef.Panel.STOCK_MARKET, false);
            guiHints.setActivePanel(GuiDef.Panel.MAP);

            // If the Operating Round was interrupted by a company (e.g. M2) that closed 
            // during the formation, we must ensure the OR step is reset to INITIAL.
            // This prevents the next company (e.g. M3) from inheriting a stale step 
            // like 'BUY_TRAIN', which causes it to skip its track/token phases.
            if (roundToResume instanceof OperatingRound) {
                OperatingRound or = (OperatingRound) roundToResume;
                if (or.getOperatingCompany() != null && or.getOperatingCompany().isClosed()) {
                    or.forceStep(GameDef.OrStep.INITIAL);
                }
            }
            
            roundToResume.resume();
        } else {
            gameManager.nextRound(this);
        }
    }

    @Override
    public String toString() {
        return "1835 PrussianFormationRound";
    }

    @Override
    public boolean setPossibleActions() {
        Player m2Pres = (m2 != null) ? m2.getPresident() : null;
        log.info("TRACE_PFR_ACTIONS: Step=[{}], M2_Pres=[{}], CurrentPlayer=[{}], RoundFinished=[{}]",
                getPrussianStep(),
                (m2Pres != null ? m2Pres.getName() : "null"),
                (this.currentPlayer != null ? this.currentPlayer.getName() : "null"),
                isRoundFinished());

        if (isRoundFinished()) {
            possibleActions.clear();
            return false;
        }
        possibleActions.clear();
        log.info("PFR setPossibleActions: Checking for actions...");

        log.info("TRACE_PFR_GEN: Generating actions for Step: {}. Acting Company: {}",
                getPrussianStep(), (m2 != null ? m2.getId() : "null"));

        this.currentPlayer = playerManager.getCurrentPlayer();

        // Safety fallback if player is null (happens during reloads/undos if not
        // synced)
        if (this.currentPlayer == null) {
            if (this.startingPlayer != null) {
                this.currentPlayer = this.startingPlayer;
                playerManager.setCurrentPlayer(this.currentPlayer);
            } else {
                PublicCompany p = companyManager.getPublicCompany(GameDef_1835.PR_ID);
                if (p != null && p.getPresident() != null) {
                    this.currentPlayer = p.getPresident();
                    setStartingPlayer(p.getPresident());
                    playerManager.setCurrentPlayer(this.currentPlayer);
                }
            }
        }
        
        // ... (inside setPossibleActions) ...
        if (getPrussianStep() == Step.PRESIDENCY_SWAP) {
// --- START FIX ---
            // Automate the Presidency Swap (10% for 10%)
            Player oldPres = getPlayerByName(swapOldPresName.value());
            Player newPres = getPlayerByName(swapNewPresName.value());

            if (oldPres != null && newPres != null) {
                // Reuse existing logic to find the valid 10% share (or shares) to return
                List<List<PublicCertificate>> options = calculateSwapOptions(newPres, oldPres);

                if (!options.isEmpty()) {
                    log.info("PFR: Automating presidency swap from {} to {}", oldPres.getName(), newPres.getName());
                    // Execute the first valid option using the EXISTING method in this class
                    performSwap(newPres, oldPres, options.get(0));
                }
                
                // Ensure the loop continues with the NEW president (who triggered the swap)
                setCurrentPlayer(newPres);
            }

            // Reset internal swap state
            swapOldPresName.set(null);
            swapNewPresName.set(null);

            // Advance step to MERGE and immediately recurse to generate the next valid actions
            setPrussianStep(Step.MERGE);
            return setPossibleActions();
// --- END FIX ---
        }
// ...


        PublicCompany m2 = companyManager.getPublicCompany(M2_ID);

        // ROBUST PLAYER ENFORCEMENT
        // If we are in the START step, M2 MUST be the active player.
        // If the engine's cleanup logic (from OperatingRound) reverted the player
        // to the previous Operating Company (e.g. M1), we detect it and FIX it here.
        if (getPrussianStep() == Step.START) {
            Player m2President = (m2 != null) ? m2.getPresident() : null;

            if (m2President != null && this.currentPlayer != m2President) {
                log.info("PFR Context Drift Detected: Player is {}, forcing {}",
                        this.currentPlayer.getName(), m2President.getName());
                setCurrentPlayer(m2President);
                // The currentPlayer field is now updated, so the check below will pass.
            }

            // (The original failure block is effectively removed/bypassed by the fix above)
            if (m2President == null) {
                // Only fail if M2 has no president at all (impossible in active game)
                gameManager.process(new NullAction(getRoot(), NullAction.Mode.DONE));
                return false;
            }

            StartPrussian startAction = new StartPrussian(m2);
            possibleActions.add(startAction);
            // Allow the user to Decline/Pass the formation offer
            NullAction passAction = new NullAction(getRoot(), NullAction.Mode.PASS);
            passAction.setButtonLabel("Do NOT start Prussia yet");
            possibleActions.add(passAction);

            return true;
        } else if (getPrussianStep() == Step.MERGE) {
            setFoldablePrePrussians();

            if (currentPlayer == null) {
                return false;
            }

            String playerName = currentPlayer.getName();

            int index = 1;
            for (Company company : foldablePrePrussians) {
                ExchangeForPrussianShare action = new ExchangeForPrussianShare(company);
                String key = String.valueOf(index++);
                String label = String.format("<html><center><b>%s: %s</b><br>Exchange %s</center></html>",
                        key, playerName, company.getId());
                action.setButtonLabel(label);
                possibleActions.add(action);
            }

            NullAction done = new NullAction(getRoot(), NullAction.Mode.DONE);
            String doneLabel = foldablePrePrussians.isEmpty() ? "Done (Nothing)" : "Pass (Keep)";

            done.setButtonLabel(doneLabel);
            possibleActions.add(done);

        } else if (getPrussianStep() == Step.DISCARD_TRAINS) {
            int index = 1;
            for (Train train : prussian.getPortfolioModel().getUniqueTrains()) {
                DiscardTrain action = new DiscardTrain(prussian, train);
                String key = String.valueOf(index++);
                action.setButtonLabel("<html><center><b>" + key + ": Discard</b><br>" + train.getType().getName()
                        + "</center></html>");
                possibleActions.add(action);
            }
        }
        return true;
    }


    private List<List<PublicCertificate>> calculateSwapOptions(Player newCandidate, Player currentPres) {
        int needed = prussian.getPresidentsShare().getShares(); // 10% usually (2 shares of 5%)
        List<PublicCertificate> ordinaryCerts = new ArrayList<>();

        for (PublicCertificate c : newCandidate.getPortfolioModel().getCertificates(prussian)) {
            if (!c.isPresidentShare()) {
                ordinaryCerts.add(c);
            }
        }

        List<List<PublicCertificate>> options = new ArrayList<>();

        // Option A: Single certificates matching the size
        for (PublicCertificate c : ordinaryCerts) {
            if (c.getShares() == needed) {
                options.add(Collections.singletonList(c));
            }
        }

        // Option B: Pairs summing to size
        for (int i = 0; i < ordinaryCerts.size(); i++) {
            for (int j = i + 1; j < ordinaryCerts.size(); j++) {
                PublicCertificate c1 = ordinaryCerts.get(i);
                PublicCertificate c2 = ordinaryCerts.get(j);
                if (c1.getShares() + c2.getShares() == needed) {
                    List<PublicCertificate> pair = new ArrayList<>();
                    pair.add(c1);
                    pair.add(c2);
                    options.add(pair);
                }
            }
        }

        // Deduplicate options by signature (to avoid showing "5%+5%" twice if they are
        // identical logic)
        Map<String, List<PublicCertificate>> unique = new HashMap<>();
        for (List<PublicCertificate> opt : options) {
            String sig = opt.stream()
                    .map(PublicCertificate::getShares)
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining("+"));
            unique.putIfAbsent(sig, opt);
        }

        return new ArrayList<>(unique.values());
    }

    private void checkAndHandlePresidencySwap(Player newCandidate) {
        Player currentPres = prussian.getPresident();

        // Standard checks
        if (currentPres == null || currentPres == newCandidate) {
            prussian.checkPresidency();
            return;
        }

        int newPct = newCandidate.getPortfolioModel().getShare(prussian);
        int oldPct = currentPres.getPortfolioModel().getShare(prussian);

        if (newPct > oldPct) {

            List<List<PublicCertificate>> options = calculateSwapOptions(newCandidate, currentPres);

            if (options.isEmpty()) {
                // No valid swap found? Fallback to engine default (might be messy but safe)
                prussian.checkPresidency();
            } else if (options.size() == 1) {
                // Only one way to do it, execute immediately
                performSwap(newCandidate, currentPres, options.get(0));
            } else {
                // Ambiguity! Enter SWAP Step.
                swapOldPresName.set(currentPres.getName());
                swapNewPresName.set(newCandidate.getName());
                setPrussianStep(Step.PRESIDENCY_SWAP);
                setCurrentPlayer(currentPres);
                // The next call to setPossibleActions will generate the buttons
            }
        }
    }

    private void executePresidencySwapChoice(PresidencySwapChoice action) {
        Player oldPres = getPlayerByName(swapOldPresName.value());
        Player newPres = getPlayerByName(swapNewPresName.value());

        List<List<PublicCertificate>> options = calculateSwapOptions(newPres, oldPres);
        if (action.getOptionIndex() >= 0 && action.getOptionIndex() < options.size()) {
            performSwap(newPres, oldPres, options.get(action.getOptionIndex()));
        } else {
            prussian.checkPresidency(); // Fallback
        }

        // Reset state
        swapOldPresName.set(null);
        swapNewPresName.set(null);
        setPrussianStep(Step.MERGE);

        // Restore current player to the one who was acting (usually the new
        // president/exchanger)
        // Actually, PFR Merge logic expects 'currentPlayer' to be the one iterating
        // exchanges.
        // We should ensure that flow continues correctly.
        // The loop in Merge uses `currentPlayer`, so we should probably restore it to
        // `newPres`
        // (who triggered the swap by buying/exchanging).
        setCurrentPlayer(newPres);
    }

    private void performSwap(Player newCandidate, Player currentPres, List<PublicCertificate> selectedCertificates) {

        // 1. Move President Cert to New Candidate
        PublicCertificate presCert = prussian.getPresidentsShare();
        presCert.moveTo(newCandidate);

        // 2. Move Chosen Ordinary Certs to Old President
        for (PublicCertificate c : selectedCertificates) {
            c.moveTo(currentPres);
        }

        // 3. Official Engine Check (should now pass without issue)
        // We call setPresident directly or rely on checkPresidency to update the
        // reference
        prussian.setPresident(newCandidate); // Explicit set to ensure engine is in sync

        ReportBuffer.add(this, LocalText.getText("IS_NOW_PRES_OF", newCandidate.getName(), prussian.getId()));
    }

    private void executeExchange(List<Company> companies, boolean president, boolean display) {
        ExchangeForShare efs;
        PublicCertificate cert;
        Owner owner;

        for (Company company : companies) {
            if (company instanceof PrivateCompany) {
                owner = ((PrivateCompany) company).getOwner();
            } else {
                owner = ((PublicCompany) company).getPresident();
                if (owner == null) {
                    owner = ipo.getParent();
                }
            }

            // Robustly find the ExchangeForShare property
            efs = null;
            Set<SpecialProperty> sps = company.getSpecialProperties();
            if (sps != null) {
                for (SpecialProperty sp : sps) {
                    if (sp instanceof ExchangeForShare) {
                        efs = (ExchangeForShare) sp;
                        break;
                    }
                }
            }
            if (efs == null) {
                // FALLBACK for BB/HB if property is missing but hardcoded exchange logic
                // triggered
                if (company.getId().equals("BB") || company.getId().equals("HB")) {
                    // Logic handled below by neededShare fallback
                } else {
                    continue;
                }
            }

            boolean isPresidentShare = president && (owner instanceof Player);

            // Fix for M1 (5%) and integer division issues.
            // New approach: Find by explicit percentage matching
            cert = null;

            int neededShare;
            if (efs != null) {
                neededShare = efs.getShare();
            } else {
                // User defined rules:
                // 10% Shares: BB, HB, M2, M4
                // 5% Shares: M1, M3, M5, M6
                String id = company.getId();
                if ("M1".equals(id) || "M3".equals(id) || "M5".equals(id) || "M6".equals(id)) {
                    neededShare = 5;
                } else {
                    // BB, HB, M2, M4 default to 10%
                    neededShare = 10;
                }
            }

            // 1. Try to find exact match in UNAVAILABLE pile (ID Check + Pres Check)
            for (PublicCertificate c : unavailable.getCertificates()) {
                if (c.getCompany().getId().equals(prussian.getId()) && c.getShare() == neededShare) {
                    if (isPresidentShare == c.isPresidentShare()) {
                        cert = c;
                        break;
                    }
                }
            }

            // 2. Try to find exact match in IPO pile (ID Check + Pres Check)
            // Shares often move to IPO after the company starts, so we MUST check here too.
            if (cert == null) {
                for (PublicCertificate c : ipo.getCertificates()) {
                    if (c.getCompany().getId().equals(prussian.getId()) && c.getShare() == neededShare) {
                        if (isPresidentShare == c.isPresidentShare()) {
                            cert = c;
                            break;
                        }
                    }
                }
            }

            // 3. Fallback: UNAVAILABLE pile - Match size only (ignore President status)
            if (cert == null) {
                for (PublicCertificate c : unavailable.getCertificates()) {
                    if (c.getCompany().getId().equals(prussian.getId()) && c.getShare() == neededShare) {
                        cert = c;
                        break;
                    }
                }
            }

            // 4. Fallback: IPO pile - Match size only (ignore President status)
            if (cert == null) {
                for (PublicCertificate c : ipo.getCertificates()) {
                    if (c.getCompany().getId().equals(prussian.getId()) && c.getShare() == neededShare) {
                        cert = c;
                        break;
                    }
                }
            }

            if (cert != null) {
                cert.moveTo(owner);
            } else {
                // Fallback to original method just in case (though likely to fail if it was 0)
                try {
                    int shareUnits = neededShare / prussian.getShareUnit();
                    if (shareUnits == 0)
                        shareUnits = 1; // Safety for 5% shares

                    cert = unavailable.findCertificate(prussian, shareUnits, isPresidentShare);
                    if (cert != null)
                        cert.moveTo(owner);
                } catch (Exception e) {
                    log.warn("Failed to find certificate for exchange via fallback method.");
                }
            }

            String ownerName = owner.getId();
            ReportBuffer.add(this, LocalText.getText("MERGE_MINOR_LOG",
                    ownerName, company.getId(), PR_ID,
                    company instanceof PrivateCompany ? "no" : Bank.format(this, ((PublicCompany) company).getCash()),
                    company instanceof PrivateCompany ? "no"
                            : ((PublicCompany) company).getPortfolioModel().getTrainList().size()));

            if (company instanceof PublicCompany) {
                PublicCompany minor = (PublicCompany) company;
                boolean isM5 = minor.getId().equals("M5");

                BaseToken minorToken = null;
                for (BaseToken token : minor.getAllBaseTokens()) {
                    if (token.getOwner() instanceof Stop) {
                        minorToken = token;
                        break;
                    }
                }

                if (minorToken != null && minorToken.getOwner() instanceof Stop) {
                    Stop city = (Stop) minorToken.getOwner();
                    MapHex hex = city.getParent();
                    minorToken.moveTo(minor);

                    if (!isM5) {
                        if (hex.layBaseToken(prussian, city)) {
                            String msg = LocalText.getText("ExchangesBaseToken", PR_ID, minor.getId(),
                                    city.getStationComposedId());
                            ReportBuffer.add(this, msg);
                            if (display)
                                DisplayBuffer.add(this, msg);
                            prussian.layBaseToken(hex, 0);
                        } else if (hex.hasTokenOfCompany(prussian)) {
                            ReportBuffer.add(this,
                                    LocalText.getText("ReplacesMinorToken", minor.getId(), prussian.getId()));
                        }
                    }
                }

                if (minor.getCash() > 0)
                    net.sf.rails.game.state.Currency.wireAll(minor, prussian);
                List<Train> trains = new ArrayList<>(minor.getPortfolioModel().getTrainList());
                for (Train train : trains)
                    prussian.getPortfolioModel().addTrain(train);
            }
            company.setClosed();

            // After receiving shares, the player might have overtaken the current PR
            // Director.
            if (owner instanceof Player) {
                checkAndHandlePresidencySwap((Player) owner);
            }
        }
    }

    private void advanceToNextValidPlayer() {
        int count = 0;
        int maxPlayers = playerManager.getNumberOfPlayers();

        // Loop until we find a player with shares OR we cycled through everyone
        while (count < maxPlayers) {
            setFoldablePrePrussians(); // Updates foldablePrePrussians for currentPlayer

            if (!foldablePrePrussians.isEmpty()) {
                return; // Found a player with actions, let them play
            }

            // No actions possible? Log it and move to next immediately (Atomic State
            // Change)
            ReportBuffer.add(this, currentPlayer.getName() + " has no exchangeable shares. Auto-Pass.");

            // Use standard next player logic
            Player nextPlayer = playerManager.getNextPlayer();
            setCurrentPlayer(nextPlayer);

            mergeTurnCount.add(1); // Track turns for end condition
            // Stop immediately if we have processed all players (including this auto-skip)
            if (mergeTurnCount.value() >= maxPlayers) {
                finishMergeStep();
                return;
            }
            count++;
        }

        // If we loop through everyone and nobody can do anything:
        finishMergeStep();
    }

}