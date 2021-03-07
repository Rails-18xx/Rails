/**
 *
 */
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.HashMultimap;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.Train;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.HashMultimapState;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;

/**
 * @author Martin Brumm
 * @date 2019-01-26
 */
public class CoalExchangeRound extends StockRound_1837 {

    private HashMultimapState<PublicCompany, PublicCompany> coalCompsPerMajor;
    private HashMultimapState<Player, PublicCompany> coalCompsPerPlayer;
    private HashMultimapState<PublicCompany, Train> discardableTrainsPerMajor; // TODO
    private ArrayListState<PublicCompany> currentMajorOrder;
    private ArrayListState<Player> currentPlayerOrder;
    private GenericState<PublicCompany> currentMajor;
    private boolean reachedPhase5;
    private int cerNumber;

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

    public void start(int cerNumber) {
        this.cerNumber = cerNumber;
        ReportBuffer.add(this, LocalText.getText("StartCoalExchangeRound", cerNumber));

        coalCompsPerMajor = HashMultimapState.create(this, "CoalsPerMajor_"+getId());
        coalCompsPerPlayer = HashMultimapState.create(this, "CoalsPerPlayer_"+getId());
        currentMajorOrder = new ArrayListState (this, "MajorOrder_"+getId());
        currentPlayerOrder = new ArrayListState(this, "PlayerOrder_"+getId());
        currentMajor = new GenericState<>(this, "CurrentMajor_"+getId());
        reachedPhase5 = getRoot().getPhaseManager().hasReachedPhase("5");

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
        HashMultimap<Player, PublicCompany> mergingMinors = HashMultimap.create();
        String type;

        // Find all mergeable coal companies,
        // and register these per related major company and player.
        for (PublicCompany comp : comps) {
            if (!comp.isClosed()) {
                PublicCompany major = companyManager
                        .getPublicCompany(comp.getRelatedPublicCompanyName());
                if (majorMustMerge(major)) {
                    mergingMinors.put(comp.getPresident(), comp);
                } else if (major.hasFloated()) {
                    coalCompsPerMajor.put(major, comp);
                    coalCompsPerPlayer.put (comp.getPresident(), comp);
                }
            }
        }

        // First process the forced mergers
        if (!mergingMinors.isEmpty()) {
            // Group mergers by player, starting with the PD
            List<Player> players
                    = playerManager.getNextPlayersAfter(playerManager.getPriorityPlayer(),
                    true, false);

            for (Player player : players) {
                if (mergingMinors.containsKey(player)) {
                    for (PublicCompany coal : mergingMinors.get(player)) {
                        PublicCompany major = coal.getRelatedPublicCompany();
                        mergeCompanies (coal, major);
                        DisplayBuffer.add(this,
                                LocalText.getText("MERGE_MINOR_LOG",
                                player, coal, major,
                                Bank.format(this, coal.getCash()),
                                coal.getPortfolioModel().getTrainList().size()));
                    }
                }
            }
         }

        // Put the majors in the operating order to initiate voluntary mergers
        for (PublicCompany major : setOperatingCompanies()) {
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

    /*----- General methods -----*/

    @Override
    public boolean process (PossibleAction action) {

        if (action instanceof MergeCompanies) {
            return executeMerge ((MergeCompanies) action);

        } else if (action instanceof NullAction
                && ((NullAction)action).getMode() == NullAction.Mode.DONE) {
            return done((NullAction)action, action.getPlayerName(), false);
        } else {
            return super.process(action);
        }
    }

    public boolean executeMerge (MergeCompanies action) {
        boolean result =
                mergeCompanies(action.getMergingCompany(), action.getSelectedTargetCompany());
        if (result) {
            coalCompsPerPlayer.remove(currentPlayer, action.getMergingCompany());
            if (coalCompsPerPlayer.get(currentPlayer).isEmpty()) {
                if (!nextPlayer()) {
                    finishRound();
                }
            }
        }
        return result;
    }

    @Override
    public boolean setPossibleActions() {

        possibleActions.clear();
        if (discardingTrains.value()) {
            return setTrainDiscardActions();
        } else {
            return setMinorMergeActions();
        }

    }

    private boolean setMinorMergeActions() {

        //if (hasActed.value()) {  //???
        //    possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
        //    return true;
        //}
        //List<PublicCompany> minors = new ArrayList<>();

        // If there is anything to do, find the first player to merge into the first major company.
        if (nextPlayer()) {
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
        } else {
            return false;
        }
    }


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
            for (Player player : playerManager.getNextPlayersAfter(
                    president, true, false)) {
                for (PublicCompany coalComp : coalCompsPerMajor) {
                    if (!coalComp.isClosed() && player == coalComp.getPresident()) {
                         currentPlayerOrder.add(player);
                         // Once in the list is enough
                         break;
                    }
                }
            }

            // Select the first player to act with the new major
            return nextPlayer();
        }
    }

    /**
     * Find the next player entitled to optionally merge a coal company.
     * This process includes selecting the next major company in the OR sequence.
     * @return True if such a player has been found.
     */
    private boolean nextPlayer() {
        if (currentPlayerOrder.isEmpty()) {
            // No more players for this major, then this major is done
            currentMajorOrder.remove(currentMajor.value());
            // Find the next one, if any
            return nextMajorCompany();
        } else {
            // Find the next player to act with the current major
            Player nextPlayer = currentPlayerOrder.get(0);
            setCurrentPlayer(nextPlayer);
            return true;
        }
    }

    @Override
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        // Autopassing does not apply here
        ReportBuffer.add(this, LocalText.getText("PASSES",
                playerName));

        // Remove the player and his not chosen player actions
        // from the action list of the current major
        currentPlayerOrder.remove(currentPlayer);

        // Is there another major, and if so, who is the next player?
        if (!nextPlayer()) {
            // Nobody, then the round ends
            finishRound();
        }
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
                LocalText.getText("EndOfCoalExchangeRound",
                        String.valueOf(getCoalExchangeRoundNumber())));

        if (discardingTrains.value()) {

           return;

        } else if (!compWithExcessTrains.isEmpty()) {

            discardingTrains.set(true);

            // Make up a list of train discarding companies in operating
            // sequence.
            PublicCompany[] operatingCompanies =
                    setOperatingCompanies().toArray(new PublicCompany[0]);
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

            // Report financials
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
    }


    private int getCoalExchangeRoundNumber() {
       return ((GameManager_1837) gameManager).getCERNumber();
       }

    @Override
    public String toString() {
        return "CoalExchangeRound";
    }

}
