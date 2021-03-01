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
    private ArrayListState<PublicCompany> currentMajorOrder;
    private ArrayListState<Player> currentPlayerOrder;
    private GenericState<PublicCompany> currentMajor;
    private List<PublicCompany> mergingCompanies;
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
        reachedPhase5 = getRoot().getPhaseManager().hasReachedPhase("5");

        init();

        setPossibleActions();
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
        List<PublicCompany> comps = companyManager.getAllPublicCompanies();
        HashMultimap<Player, PublicCompany> mergingMinors = HashMultimap.create();
        String type;

        // Find all mergeable coal companies,
        // and register these per related major company and player.
        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
            if (!comp.isClosed() && type.equals("Coal")) {
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

        // Put the majors in the operating order
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
        return mergeCompanies(action.getMergingCompany(), action.getSelectedTargetCompany());
    }

    @Override
    public boolean setPossibleActions() {

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
        List<PublicCompany> minors = new ArrayList<>();

        // Pick the first remaining major company that has minors to merge
        for (PublicCompany major : currentMajorOrder) {
            // Determine the player sequence, president first
            // Search who owns the coal companies related to that major
            for (Player player : currentPlayerOrder.view()) {
                for (PublicCompany minor : coalCompsPerMajor.get(major)) {
                    if (player == minor.getPresident()) {
                        minors.add(minor);
                    }
                }
                if (!minors.isEmpty()) break;
            }
            if (!minors.isEmpty()) {
                Player playerToAct = minors.get(0).getPresident();
                for (PublicCompany minor : minors){
                    possibleActions.add(new MergeCompanies(minor, major, false));
                }
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                playerManager.setCurrentPlayer(playerToAct);
                return true;
            }
        }


/**
* we need to make sure that everyone is asked who has a share of a minor that is corresponding to the floated Major..
*/
        return false;
    }

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
                    if (!coalComp.isClosed()) {
                        currentPlayerOrder.add(player);
                    }
                }
            }

            return nextPlayer();
        }
    }

    private boolean nextPlayer() {
        if (currentPlayerOrder.isEmpty()) {
            return nextMajorCompany();
        } else {
            Player nextPlayer = currentPlayerOrder.get(0);
            setCurrentPlayer(nextPlayer);
            return true;
        }
    }



    @Override
    // Autopassing does not apply here
    public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

        for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
            if ((comp.getType().getId().equals("Coal")) && (!comp.isClosed()) && (companyManager.getPublicCompany(comp.getRelatedPublicCompanyName()).hasFloated())) {

                    finishTurn();
                    return true;
            }
        }

        finishRound();
        return true;
    }

    @Override
    protected void initPlayer() {

        currentPlayer = playerManager.getCurrentPlayer();
        hasActed.set(false);

    }

    /*----- METHODS TO BE CALLED TO SET UP THE NEXT TURN -----*/

    @Override
    protected void finishRound() {
        ReportBuffer.add(this, " ");
        ReportBuffer.add(
                this,
                LocalText.getText("END_CoalExchangeRound",
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
