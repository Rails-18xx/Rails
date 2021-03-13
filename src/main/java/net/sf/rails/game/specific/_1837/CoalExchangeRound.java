
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
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
    /**
     * @param parent
     * @param id
     */
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

        currentMajorOrder = new ArrayListState (this, "MajorOrder_"+getId());
        currentPlayerOrder = new ArrayListState(this, "PlayerOrder_"+getId());
        currentMajor = new GenericState<>(this, "CurrentMajor_"+getId());

        discardableTrains = HashMultimapState.create(this, "NewTrainsPerMajor_"+getId());
        closedMinors = new ArrayListState(this, "ClosedMinorsPerMajor_"+getId());
        numberOfExcessTrains = IntegerState.create(this, "NumberOfExcessTrains");

        reachedPhase5 = getRoot().getPhaseManager().hasReachedPhase("5");

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
        //HashMultimap<Player, PublicCompany> forcedMergingMinors = HashMultimap.create();
        //String type;

        // Find all mergeable coal companies,
        // and register these per related major company and player.
        for (PublicCompany comp : comps) {
            if (!comp.isClosed()) {
                PublicCompany major = companyManager
                        .getPublicCompany(comp.getRelatedPublicCompanyName());
                /*if (majorMustMerge(major)) {
                    forcedMergingMinors.put(comp.getPresident(), comp);
                } else */if (major.hasFloated()) {
                    coalCompsPerMajor.put(major, comp);
                    coalCompsPerPlayer.put (comp.getPresident(), comp);
                }
            }
        }

        /*
        // First process the forced mergers
        if (!forcedMergingMinors.isEmpty()) {
            // Group mergers by player, starting with the PD
            List<Player> players
                    = playerManager.getNextPlayersAfter(playerManager.getPriorityPlayer(),
                    true, false);

            for (Player player : players) {
                if (forcedMergingMinors.containsKey(player)) {
                    for (PublicCompany coal : forcedMergingMinors.get(player)) {
                        PublicCompany major = coal.getRelatedPublicCompany();
                        DisplayBuffer.add(this,
                                LocalText.getText("AutoMergeMinorLog",
                                coal, major,
                                Bank.format(this, coal.getCash()),
                                coal.getPortfolioModel().getTrainList().size()));

                        mergeCompanies (coal, major);
                    }
                }
            }
         }*/

        // Put the majors in the operating order to initiate mergers
        for (PublicCompany major : setOperatingCompanies("Major")) {
            if (coalCompsPerMajor.containsKey(major)) {
                currentMajorOrder.add(major);
            }
        }
    }

    @Override
    public String getOwnWindowTitle() {
        return LocalText.getText("CoalExchangeRoundTitle", cerNumber);
    }

    private boolean majorMustMerge (PublicCompany major) {
        return reachedPhase5 || ipo.getShares(major) == 0;
    }

    /*----- Validation and execution -----*/

    @Override
    public boolean process (PossibleAction action) {

        if (action instanceof MergeCompanies) {
            return executeMerge((MergeCompanies) action);

        } else if (action instanceof LayBaseToken_1837) {
            return layBaseToken((LayBaseToken_1837) action);

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
                // If the current player does not own more minors for this major,
                // check if there is another such player
                if (!nextPlayer()) {
                    finishRound();
                }
            }
        }
        return result;
    }

    /**
     * A stripped-down version of OperatingRound.layBaseToken,
     * usable in a StockRound subclass.
     * @param action
     * @return
     */
    public boolean layBaseToken(LayBaseToken_1837 action) {

        String errMsg = null;
        String errHex = "";

        PublicCompany major = action.getMajor();
        String majorName = major.getId();
        List<PublicCompany> minors = action.getMinors();

        // Dummy loop to enable a quick jump out.
        while (true) {

            if (major.getNumberOfFreeBaseTokens() == 0) {
                errMsg = LocalText.getText("HasNoTokensLeft", majorName);
                break;
            }

            for (PublicCompany minor : minors) {
                MapHex hex = minor.getHomeHexes().get(0);
                if (hex.hasTokenOfCompany(major)) {
                    errHex += hex.getId() + " ";
                    errMsg =
                            LocalText.getText("TileAlreadyHasToken", hex.getId(),
                                    majorName);
                    break;
                }
            }

            break;
        }
        if (errMsg != null) {
            DisplayBuffer.add(
                    this,
                    LocalText.getText("CannotLayBaseTokenOn", majorName,
                            errHex, "-", errMsg));
            return false;
        }

        /* End of validation, start of execution */

        for (int i = 0; i<minors.size(); i++) {
            if (!action.getSelected(i)) continue;

            MapHex hex = minors.get(i).getHomeHexes().get(0);
            Stop stop = hex.getRelatedStop(1);
            if (hex.layBaseToken(major, stop)) {
                /* TODO: the false return value must be impossible. */

                major.layBaseToken(hex, 0);

                StringBuilder text = new StringBuilder();
                if (action.isCorrection()) {
                    text.append(LocalText.getText("CorrectionPrefix"));
                }
                text.append(LocalText.getText("LaysFreeTokenOn",
                        majorName, action.getPlayerName(), hex.getId()));
                ReportBuffer.add(this, text.toString());
            }
        }
        closedMinors.clear();

        if (!checkForExcessTrains() && !nextPlayer()) finishRound();

        return true;
    }

    /**
     * Check for the need to have the president select trains to discard.
     * This only occurs if the major has too many trains, and has
     * different types of discardable trains.
     * If there is only one such type, discarding is automatic.
     * @return true if manual excess train selection is necessary.
     */
    private boolean checkForExcessTrains () {

        // Has the major too may trains?
        PublicCompany major = currentMajor.value();
        int excess = major.getNumberOfTrains() - major.getCurrentTrainLimit();
        if (excess <= 0) return false;
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

        super.discardTrain(action);

        discardableTrains.remove (action.getDiscardedTrain().getType(),
                action.getDiscardedTrain());
        numberOfExcessTrains.add(-1);

        if (numberOfExcessTrains.value() == 0) {
            if (!nextPlayer()) finishRound();
            clearDiscardableTrains();
        }

        return true;
    }

    private void clearDiscardableTrains() {
        for (TrainType type : discardableTrains.keySet()) {
            discardableTrains.removeAll(type);
        }
    }

    @Override
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        // Autopassing does not apply here

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
        // Then continue with token laying if any minor was merged
        if (currentPlayerOrder.isEmpty()
                && (!closedMinors.isEmpty() || !discardableTrains.isEmpty())) {
            return true;
        }

        // OK, we are done with this major.
        // Is there another major, and if so, who is the next player?
        if (!nextPlayer()) {
            // Nobody, then the round ends
            finishRound();
        }
        return true;
    }

    /*--- Preparation of next actions ---*/

    @Override
    public boolean setPossibleActions() {

        //possibleActions.clear();

        // Are there more minor owners for this major?
        if (!currentPlayerOrder.isEmpty()) {
            return setMinorMergeActions();
        // If not, then handle token laying for each merged minor (if any)
        } else if (!closedMinors.isEmpty()) {
            return setTokenLayingActions();
        // ... and then check if the major must discard any excess trains
        } else if (!discardableTrains.isEmpty()) {
            PublicCompany major = currentMajor.value();
            int excess = major.getNumberOfTrains() - major.getCurrentTrainLimit();
            if (excess > 0) return setTrainDiscardActions(excess);
        }
        // If all the above is done, check if there is another major to deal with
        return setMinorMergeActions();

    }

    private boolean setMinorMergeActions() {

        //if (hasActed.value()) {  //??? Do we need this?
        //    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
        //    return true;
        //}
        //List<PublicCompany> minors = new ArrayList<>();

        /*
        if (currentPlayerOrder.isEmpty() && nextMajorCompany()) {
            PublicCompany major = currentMajor.value();
        }*/

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

                // Immediately continue with the token replacement step
                setTokenLayingActions();
                return true;

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

    /*
     * An extract of the superclass method tailored to new-style
     * forced coal company mergers.
     * TODO: this needs to be reconsidered.
     *
     * @param minor The automerging coal company
     * @param major The related major company
     * @param forced Added to distinguish this method from its superclass parent
     * @return
     */
    /*
    protected boolean mergeCompanies(PublicCompany minor, PublicCompany major, boolean forced) {

        if (!forced) return super.mergeCompanies(minor, major);

        PublicCertificate cert = null;
        MoneyOwner cashDestination = null; // Bank

        // TODO Validation to be added?
        if (major != null) {  // When would this be null?
            cert = unavailable.findCertificate(major, false);
            cashDestination = major;
        }
        //TODO: what happens if the major hasn't operated/founded/Started sofar in the FinalCoalExchangeRound ?

        // Transfer the minor assets
        int minorCash = minor.getCash();
        if (cashDestination == null) {
            // Assets go to the bank
            if (minorCash > 0) {
                Currency.toBankAll(minor);
            }
            pool.transferAssetsFrom(minor.getPortfolioModel());
        } else {
            // Assets go to the major company
            major.transferAssetsFrom(minor);
        }
    }*/

        /**
         * Find the next major company that is related to mergeable coal companies.
         * @return True if such a company is found. If not, the round is finished.
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
                    majorMustMerge(major) ? "forced " : "voluntary ",
                    coalCompanies.size() > 1 ? "s" : "",
                    coalCompanies.toString().replaceAll ("[\\[\\] ]", ""),
                    major));

            clearDiscardableTrains();
            closedMinors.clear();

            // Select the first player to act with the new major
            return nextPlayer();
        }
    }

    private boolean setTokenLayingActions () {
        if (!closedMinors.isEmpty()) {
            setCurrentPlayer(currentMajor.value().getPresident());
            possibleActions.add (new LayBaseToken_1837(getRoot(),
                    currentMajor.value(), closedMinors.view()));
        }
        // Add done?
        return true;
    }

    protected boolean setTrainDiscardActions(int excess) {

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

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

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
