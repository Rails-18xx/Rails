package net.sf.rails.game.specific._1835;

import net.sf.rails.common.GameOption;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.ShareSellingRound;
import net.sf.rails.game.financial.StockRound;
import net.sf.rails.game.round.RoundFacade;
import net.sf.rails.game.state.BooleanState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.sf.rails.game.special.SpecialBaseTokenLay; // Ensure this is imported
import net.sf.rails.game.special.SpecialProperty;
import rails.game.action.PossibleAction;
import rails.game.specific._1835.StartPrussian;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

public class GameManager_1835 extends GameManager {

    private Round previousRound = null;
    private Player prFormStartingPlayer = null;
    private static final Logger log = LoggerFactory.getLogger(GameManager_1835.class);

    // State Variables
    private final BooleanState prussianFormationOffered = new BooleanState(this, "PrussianFormationOffered");
    private final BooleanState pfrDeclinedThisCycle = new BooleanState(this, "PfrDeclinedThisCycle");

    /**
     * A wrapper to hold a round, its priority, and its idempotent state key.
     */
    private static class PriorityRoundWrapper implements Comparable<PriorityRoundWrapper> {
        final int priority;
        final String stateKey;
        final RoundFacade round;

        PriorityRoundWrapper(int priority, String stateKey, RoundFacade round) {
            this.priority = priority;
            this.stateKey = stateKey;
            this.round = round;
        }

        @Override
        public int compareTo(PriorityRoundWrapper other) {
            return Integer.compare(this.priority, other.priority);
        }
    }

    private final PriorityQueue<PriorityRoundWrapper> priorityRoundQueue = new PriorityQueue<>();
    private final Set<String> pendingStateKeys = new HashSet<>();
    private Round lastStandardRound = null;

    public GameManager_1835(RailsRoot parent, String id) {
        super(parent, id);
    }



    @Override
    public List<PublicCompany> getCompaniesInDisplayOrder(List<PublicCompany> companies) {
        // 1835 has idiosyncratic display rules (e.g., PR displays before floating, Minors top)
        // We replicate the classic UI sort logic here, but resolve ties with the engine's running order.
        List<PublicCompany> sortedList = new java.util.ArrayList<>(companies);
        final List<PublicCompany> runningOrder = getCompaniesInRunningOrder();
        
        java.util.Collections.sort(sortedList, (c1, c2) -> {
            boolean c1IsPR = "PR".equals(c1.getId());
            boolean c2IsPR = "PR".equals(c2.getId());

            int p1 = c1.getCurrentSpace() != null ? c1.getCurrentSpace().getPrice()
                    : (c1.getStartSpace() != null ? c1.getStartSpace().getPrice() : 0);
            int p2 = c2.getCurrentSpace() != null ? c2.getCurrentSpace().getPrice()
                    : (c2.getStartSpace() != null ? c2.getStartSpace().getPrice() : 0);

            boolean c1Minor = c1IsPR ? (p1 == 0) : !c1.hasStockPrice();
            boolean c2Minor = c2IsPR ? (p2 == 0) : !c2.hasStockPrice();

            if (c1Minor && !c2Minor) return -1;
            if (!c1Minor && c2Minor) return 1;
            if (c1Minor) return Integer.compare(c1.getPublicNumber(), c2.getPublicNumber());
            
            if (p1 != p2) return Integer.compare(p2, p1);
            
            // Resolve ties by referencing the authoritative running order
            int idx1 = runningOrder.indexOf(c1);
            int idx2 = runningOrder.indexOf(c2);
            return Integer.compare(idx1, idx2);
        });
        return sortedList;
    }

    @Override
    protected boolean runIfStartPacketIsNotCompletelySold() {
        if (GameOption.getAsBoolean(this, "Clemens")
                || GameOption.getAsBoolean(this, "MinorsRequireFloatedBY")) {
            return getRoot().getCompanyManager().getPublicCompany(GameDef_1835.BY_ID).hasFloated();
        }
        return true;
    }

    public void queuePriorityRound(String stateKey, RoundFacade round, int priority) {
        if (round == null || stateKey == null)
            return;

        if (pendingStateKeys.contains(stateKey)) {
            return;
        }
        priorityRoundQueue.add(new PriorityRoundWrapper(priority, stateKey, round));
        pendingStateKeys.add(stateKey);
    }

    public boolean hasPrussianFormationBeenOffered() {
        return prussianFormationOffered.value();
    }

    public void setPrussianFormationOffered() {
        if (!prussianFormationOffered.value()) {
            this.prussianFormationOffered.set(true);
        }
    }

    public void resetPrussianFormationOffered() {
        this.prussianFormationOffered.set(false);
    }

    public Player getPrussianFormationStartingPlayer() {
        return prFormStartingPlayer;
    }

    public void setPrussianFormationStartingPlayer(Player prFormStartingPlayer) {
        this.prFormStartingPlayer = prFormStartingPlayer;
    }

    @Override
    public int getPlayerCertificateLimit(Player player) {
        int limit = getRoot().getPlayerManager().getPlayerCertificateLimit(player);
        for (PublicCompany company : getRoot().getCompanyManager().getAllPublicCompanies()) {
            if (company.getType().getId().equalsIgnoreCase("Major")
                    && company.getPresident() == player
                    && player.getPortfolioModel().getShare(company) >= 80)
                limit++;
        }
        return limit;
    }

    @Override
    public void setNumberOfOperatingRounds(int number) {
        if (getCurrentRound() instanceof OperatingRound) {
            if (number != getNumberOfOperatingRounds()) {

                return;
            }
        }
        super.setNumberOfOperatingRounds(number);
    }

    @Override
    protected void startStockRound() {
        if (shouldTriggerPFR()) {
            startPrussianFormationRound(null);
            return;
        }
        super.startStockRound();
    }

    @Override
    protected void startOperatingRound(boolean operate) {
        if (shouldTriggerPFR()) {
            startPrussianFormationRound(null);
            return;
        }
        super.startOperatingRound(operate);
    }

    // Replace this method in GameManager_1835.java
    public void setPfrDeclined() {
        this.pfrDeclinedThisCycle.set(true);
    }

    @Override
    public void nextRound(Round round) {

        String roundName = (round != null) ? round.getRoundName() : "null";
        boolean isPFR = (round instanceof PrussianFormationRound);



        // CASE 1: A Standard Round (OR or SR) just finished.
        // We reset the flag so we can offer the PFR again at the start of the NEW
        // round.
        if (round != null && !isPFR) {
            this.lastStandardRound = round; // Remember where we were
            pfrDeclinedThisCycle.set(false);
            super.nextRound(round);
        }

        // CASE 2: The PFR just finished.
        else if (isPFR) {

            // Always mark this cycle as "Declined/Handled" when PFR finishes.
            // Whether the user started PR, exchanged shares, or did nothing,
            // we must NOT trigger PFR again immediately, or we get an infinite loop.
            pfrDeclinedThisCycle.set(true);

            // Resume the normal game flow
            if (this.lastStandardRound != null) {

                super.nextRound(this.lastStandardRound);
            } else {
                super.nextRound(round);
            }
        }

        // Fallback
        else {
            super.nextRound(round);
        }
    }

    private boolean shouldTriggerPFR() {
        // 1. Safety: Prevent infinite loops within the same round transition
        if (pfrDeclinedThisCycle.value())
            return false;

        // 2. Global Trigger: Has the 4/4+4/5 train event happened?
        if (!hasPrussianFormationBeenOffered())
            return false;

        // 3. Completion Check: If no minors/privates are left to exchange, stop asking.
        // This stops the prompt forever once everything is merged.
        if (PrussianFormationRound.prussianIsComplete(this))
            return false;

        // 4. Context Check:
        PublicCompany pr = getRoot().getCompanyManager().getPublicCompany(GameDef_1835.PR_ID);

        // Scenario A: PR Not Started yet.
        // We need M2 to be active to trigger the "Start Prussian?" question.
        if (!pr.hasStarted()) {
            PublicCompany m2 = getRoot().getCompanyManager().getPublicCompany(GameDef_1835.M2_ID);
            if (m2 == null || m2.isClosed() || m2.getPresident() == null)
                return false;
        }

        // Scenario B: PR is Started.
        // If we reached here, we know minors still exist (Step 3).
        // Therefore, we MUST trigger PFR to allow exchanges.

        return true;
    }

    // ... (inside GameManager_1835.java) ...
    // --- FIX 1: Correct Private Company Valuation ---
    @Override
    public int getPrivateWorth(PrivateCompany priv) {
        String id = priv.getId();

        // 1. Privates that come with a share (LD, NF, OBB, PfB).
        // The value is in the Share, so the Private itself is worth 0.
        if ("LD".equals(id) || "NF".equals(id) || "OBB".equals(id) || "PfB".equals(id)) {
            return 0;
        }

        // 2. Privates that exchange for a share later (BB, HB).
        // Value = 10% PR (154)
        if ("BB".equals(id) || "HB".equals(id)) {
            return 154;
        }

        return super.getPrivateWorth(priv);
    }

    // --- FIX 2: Correct Minor Company Valuation (M1-M6) ---
    @Override
    public int getPublicCompanyWorth(PublicCompany company) {
        String id = company.getId();

        // 10% PR Equivalent (Value 154)
        if ("M2".equals(id) || "M4".equals(id)) {
            // log.info("1835 Valuation: {} is worth 154", id);
            return 154;
        }

        // 5% PR Equivalent (Value 77)
        if ("M1".equals(id) || "M3".equals(id) || "M5".equals(id) || "M6".equals(id)) {
            // log.info("1835 Valuation: {} is worth 77", id);
            return 77;
        }

        return super.getPublicCompanyWorth(company);
    }


    public void startPrussianFormationRound(Round currentRound) {

        setInterruptedRound(currentRound);

        String roundName;
        if (getInterruptedRound() == null) {
            roundName = "PrussianFormationRound_after_" + (previousRound != null ? previousRound.getId() : "Start");
        } else {
            roundName = "PrussianFormationRound_in_" + currentRound.getId();
            if (getCurrentPhase() != null) {
                roundName += "_after_" + getCurrentPhase().getId();
            }
        }
        roundName += "_" + getCurrentActionCount() + "_" + System.nanoTime();

        
        // Cast is necessary because createRound returns generic Round
        PrussianFormationRound pfr = (PrussianFormationRound) createRound(PrussianFormationRound.class, roundName);
        
        setRound(pfr);
        pfr.start();

        this.setPrussianFormationOffered();
        
// CRITICAL: Force UI to acknowledge the round switch and repaint.
        // Fix: Pass 'pfr' (Round/RailsItem) as the source, and the text as the message.
        net.sf.rails.common.ReportBuffer.add(pfr, LocalText.getText("PRUSSIAN_FORMATION_ROUND_STARTED"));
        
    }


        /**
     * Helper to expose M2 company to the PrussianFormationRound.
     * Required because PFR cannot access protected CompanyManager methods of the base GameManager.
     */
    public PublicCompany getM2() {
        return getRoot().getCompanyManager().getPublicCompany(GameDef_1835.M2_ID);
    }


}