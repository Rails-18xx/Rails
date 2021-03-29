
package net.sf.rails.game.specific._1837;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.rails.common.*;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.*;
import rails.game.action.*;

/**
 * @author Martin Brumm
 * @date 2019-01-26
 */
public class CoalExchangeRound extends StockRound_1837 {

    // Collections to register the potential mergers
    private ArrayListMultimapState<PublicCompany, PublicCompany> coalCompsPerMajor;
    private ArrayListMultimapState<Player, PublicCompany> coalCompsPerPlayer;

    // Collections to manage the merging process
    private ArrayListState<PublicCompany> currentMajorOrder;
    private GenericState<PublicCompany> currentMajor;
    private ArrayListState<Player> currentPlayerOrder; // for the current major

    // Collections to register follow-up actions per major
    private HashMultimapState<TrainType, Train> discardableTrains;
    private ArrayListState<PublicCompany> closedMinors;
    private IntegerState numberOfExcessTrains;

    private boolean reachedPhase5;
    private String cerNumber;

    /** A state variable to set the next action to take */
    private IntegerState step; // Wish we had an EnumState!
    private static final int INITIAL = 0;
    private static final int MERGE = 1;
    private static final int DISCARD = 2;
    private static final int FINAL = 3;

    public CoalExchangeRound(GameManager parent, String id) {
       super(parent, id);
       //guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
       guiHints.setActivePanel(GuiDef.Panel.STATUS);

       raiseIfSoldOut = false;
    }

    public static CoalExchangeRound create(GameManager parent, String id){
        return new CoalExchangeRound(parent, id);
    }

    public void start() {

        cerNumber = getId().replaceFirst("CER_(.+)", "$1");

        coalCompsPerMajor = ArrayListMultimapState.create(this, "CoalsPerMajor_"+getId());
        coalCompsPerPlayer = ArrayListMultimapState.create(this, "CoalsPerPlayer_"+getId());

        currentMajorOrder = new ArrayListState<> (this, "MajorOrder_"+getId());
        currentPlayerOrder = new ArrayListState<>(this, "PlayerOrder_"+getId());
        currentMajor = new GenericState<>(this, "CurrentMajor_"+getId());

        discardableTrains = HashMultimapState.create(this, "NewTrainsPerMajor_"+getId());
        closedMinors = new ArrayListState<>(this, "ClosedMinorsPerMajor_"+getId());
        numberOfExcessTrains = IntegerState.create(this, "NumberOfExcessTrains");

        reachedPhase5 = getRoot().getPhaseManager().hasReachedPhase("5");

        step = IntegerState.create(this, "CERstep");

        String message = LocalText.getText("StartCoalExchangeRound", cerNumber);
        ReportBuffer.add(this, message);
        DisplayBuffer.add (this, message.replaceAll("-+", ""));

        init();

        if (currentMajorOrder.isEmpty()) {
            finishRound();
        }
    }

    /**
     *  Determine the possible mergers, if any, in the following order:
     *  1. The (operational) major companies, in current operating order,
     *  2. The players owning coal companies of that major, president first.
     *  NOTE: this is the detailed process as described for version 2.0
     *  The original v1 documentation does not describe any order,
     *  but Steve Thomas has issued a clarification to the same effect.
     */
    private void init() {
        List<PublicCompany> comps = companyManager.getPublicCompaniesByType("Coal");

        // Find all mergeable coal companies,
        // and register these per related major company and player.
        for (PublicCompany comp : comps) {
            if (!comp.isClosed()) {
                PublicCompany major = companyManager
                        .getPublicCompany(comp.getRelatedPublicCompanyName());
                if (major.hasFloated()) {
                    coalCompsPerMajor.put(major, comp);
                    coalCompsPerPlayer.put (comp.getPresident(), comp);
                }
            }
        }

        // Put the majors in the operating order to initiate mergers
        for (PublicCompany major : setOperatingCompanies("Major")) {
            if (coalCompsPerMajor.containsKey(major)) {
                currentMajorOrder.add(major);
            }
        }

        step.set(MERGE);
    }

    @Override
    public String getOwnWindowTitle() {
        return LocalText.getText("CoalExchangeRoundTitle", cerNumber);
    }

    private boolean majorMustMerge (PublicCompany major) {
        return reachedPhase5
                || (ipo.getShares(major) == 0
                    // In the Romoth variant, merging remains optional until phase 5
                    && !GameOption.getValue(this, GameOption.VARIANT).equals("Romoth"));
    }

    /*----- Validation and execution -----*/

    @Override
    public boolean process (PossibleAction action) {

        if (action instanceof MergeCompanies) {
            return executeMerge((MergeCompanies) action);

        } else if (action instanceof DiscardTrain) {
            return discardTrain((DiscardTrain) action);

        } else if (action instanceof NullAction
                && ((NullAction)action).getMode() == NullAction.Mode.DONE) {
            return done((NullAction)action, action.getPlayerName(), false);
        } else {
            return super.process(action);
        }
    }

    /**
     * Merge a coal company minor with its related major company.
     * @param action A MergeCompanies action selected by the minor owner.
     * @return True if the merge is successful, and new possible action(s) can be selected.
     */
    public boolean executeMerge (MergeCompanies action) {

        PublicCompany minor = action.getMergingCompany();
        PublicCompany major = action.getSelectedTargetCompany();

        for (Train train : minor.getPortfolioModel().getTrainList()) {
            discardableTrains.put (train.getType(), train);
        }

        boolean result = mergeCompanies(minor, major);
        closedMinors.add (minor);
        coalCompsPerMajor.remove (major, minor);

        // TODO: to be moved outside this method
        if (result) {
            coalCompsPerPlayer.remove(currentPlayer, minor);
            if (coalCompsPerPlayer.get(currentPlayer).isEmpty()) {
                step.set(DISCARD);
            }
        }
        return result;
    }

    /**
     * Check for the need to have the president select trains to discard.
     * This only occurs if the major has too many trains, and has
     * different types of discardable trains.
     * If there is only one such type, discarding is automatic.
     * @return True if manual excess train selection is necessary.
     * False if the major has no excess trains, or if it had only one type of train to discard;
     * in the latter case, discarding is automatic, without user interaction.
     */
    private boolean checkForExcessTrains () {

        // Has the major too may trains?
        PublicCompany major = currentMajor.value();
        int excess = major.getNumberOfTrains() - major.getCurrentTrainLimit();
        if (excess <= 0) {
            step.set(MERGE);
            return false;
        }
        numberOfExcessTrains.set(excess);

        // Has he different discardable train types?
        // If so, trigger a separate train discarding step.
        if (discardableTrains.keySet().size() > 1) return true;

        // Only one discardable train type: no need to ask the president
        // which train(s) will be discarded.
        List<Train> trains = discardableTrains.values().asList();
        for (int i=0; i<numberOfExcessTrains.value(); i++) {
            Train train = trains.get(i);
            train.discard();
            // Reported in the discard() method
            DisplayBuffer.add (this, LocalText.getText(
                    "CompanyDiscardsTrain", major,
                            train.getType(), pool));
        }
        clearDiscardableTrains();
        numberOfExcessTrains.set(0);
        return false;
    }

    public boolean discardTrain (DiscardTrain action) {

        boolean result = super.discardTrain(action);

        discardableTrains.remove (action.getDiscardedTrain().getType(),
                action.getDiscardedTrain());
        numberOfExcessTrains.add(-1);

        if (numberOfExcessTrains.value() == 0) {
            step.set(MERGE);
        }

        return result;
    }

    private void clearDiscardableTrains() {
        for (TrainType type : discardableTrains.keySet()) {
            discardableTrains.removeAll(type);
        }
    }

    @Override
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        // Report not (yet) merged coal companies
        List<PublicCompany> remainingMinors = coalCompsPerMajor.get(currentMajor.value());
        if (!remainingMinors.isEmpty()) {
            ReportBuffer.add(this, LocalText.getText("PlayerDoesNoWantToMerge",
                    playerName,
                    // Remove the square brackets from the minors list
                    remainingMinors.toString().replaceAll("[\\[\\]]", ""),
                    currentMajor.value()));
        }

        // Remove the player and his not chosen player actions
        // from the action list of the current major
        currentPlayerOrder.remove(currentPlayer);

        // Are we out of players with possible mergers for this major?
        // Then continue with train discarding if any minor was merged
        if (currentPlayerOrder.isEmpty() && !closedMinors.isEmpty()) {
            step.set(DISCARD);
        }

        return true;
    }

    /*--- Preparation of next actions ---*/

    @Override
    public boolean setPossibleActions() {

        while (true) {
            if (step.value() == MERGE && setMinorMergeActions()) {
                return true;
            } else if (step.value() == DISCARD
                    && checkForExcessTrains()
                    && setTrainDiscardActions()) {
                return true;
            } else if (step.value() == FINAL) {
                finishRound();
                return true;
            }
        }
    }

    private boolean setMinorMergeActions() {

        // If there is anything to do, find the first player to merge into the first major company.
        if (nextPlayer()) {
            PublicCompany major = currentMajor.value();

            // If mergers are forced for this major, a different procedure applies.
            if (majorMustMerge(major)) {

                currentPlayer = null; // This indicates a forced merge
                // TODO This is duplicate code, can it be refactored?
                for (PublicCompany minor : coalCompsPerMajor.get(major)) {
                    for (Train train : minor.getPortfolioModel().getTrainList()) {
                        discardableTrains.put(train.getType(), train);
                    }
                    DisplayBuffer.add(this,
                            LocalText.getText("AutoMergeMinorLog",
                            minor, major,
                            Bank.format(this, minor.getCash()),
                            minor.getPortfolioModel().getTrainList().size()));
                    mergeCompanies(minor, major);
                    closedMinors.add(minor);
                }
                currentPlayerOrder.clear();
                step.set(DISCARD);
                return false; // Run this again

            } else {

                Player player = currentPlayer;

                // Get the mergeable minors of the current major
                for (PublicCompany minor : coalCompsPerMajor.get(currentMajor.value())) {
                    if (player == minor.getPresident()) {
                        //minors.add(minor);
                        possibleActions.add(new MergeCompanies(minor, currentMajor.value(), false));
                    }
                }
                // It's optional, so pass is allowed
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                return true;
            }
        } else {
            step.set(FINAL);
            return false;
        }
    }

    /**
     * Find the next player entitled to optionally merge a coal company.
     * This process includes selecting the next major company in the OR sequence.
     * @return True if such a player has been found.
     */
    private boolean nextPlayer() {
        PublicCompany major = currentMajor.value();
        if (currentPlayerOrder.isEmpty()) {
            if (major != null) {
                currentMajorOrder.remove(major);
            }
            // Find the next one, if any
            return nextMajorCompany();
        } else {
            // Find the next player to act with the current major
            Player nextPlayer = currentPlayerOrder.get(0);
            setCurrentPlayer(nextPlayer);
            return true;
        }
    }

    /**
     * Find the next major company that is related to mergeable coal companies.
     * @return True if such a company is found and is not forcibly merged.
     * False if a (forced) automatic merge has been executed.
     */
    private boolean nextMajorCompany () {
        if (currentMajorOrder.isEmpty()) {
            return false;
        } else {
            // Select the next major company with mergeable coal companies
            PublicCompany major = currentMajorOrder.get(0);
            currentMajor.set(major);
            Player president = major.getPresident();

            // Determine the sequence of players to get a turn for this major
            currentPlayerOrder.clear();
            List<PublicCompany> coalCompanies = coalCompsPerMajor.get(major);
            for (Player player : playerManager.getNextPlayersAfter(
                    president, true, false)) {
                for (PublicCompany coalComp : coalCompanies) {
                    if (!coalComp.isClosed() && player == coalComp.getPresident()) {
                         currentPlayerOrder.add(player);
                         // Once in the list is enough
                         break;
                    }
                }
            }

            DisplayBuffer.add (this, LocalText.getText("MergingStart",
                    majorMustMerge(major) ? "compulsory" : "voluntary",
                    coalCompanies.size() > 1 ? "s" : "",
                    coalCompanies.toString().replaceAll ("[\\[\\] ]", ""),
                    major));

            clearDiscardableTrains();
            closedMinors.clear();

            // Select the first player to act with the new major
            return nextPlayer();
        }
    }

    protected boolean setTrainDiscardActions() {

        PublicCompany major = currentMajor.value();
        // We already have filtered out the case that
        // only one train type can be discarded.

        Set<Train> trains = new HashSet<>();
        for (TrainType type : discardableTrains.keySet()) {
            List<Train> trainsPerType = discardableTrains.get(type).asList();
            trains.add(trainsPerType.get(0));
        }
        possibleActions.add(new DiscardTrain(major, trains));
        // We handle one train at at time.
        // We come back here until all excess trains have been discarded.

        return true;
    }

    @Override
    protected void initPlayer() {  // Still used?

        currentPlayer = playerManager.getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT ROUND -----*/

    @Override
    protected void finishRound() {
        ReportBuffer.add(this, " ");
        ReportBuffer.add(
                this,
                LocalText.getText("EndOfCoalExchangeRound", cerNumber));

        // Report financials
        // TODO: This is a standard procedure, IMO it should not appear here (EV)
        ReportBuffer.add(this, "");
        for (PublicCompany c : companyManager.getAllPublicCompanies()) {
            if (c.hasFloated() && !c.isClosed()) {
                ReportBuffer.add(this, LocalText.getText("Has", c.getId(),
                        Bank.format(this, c.getCash())));
            }
        }
        for (Player p : playerManager.getPlayers()) {
            ReportBuffer.add(this, LocalText.getText("Has", p.getId(),
                    Bank.format(this, p.getCashValue())));
        }
        // Inform GameManager
        gameManager.nextRound(this);
    }

    @Override
    public String toString() {
        return getId();
    }

}
