/**
 *
 */
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.HashMultimapState;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;
import rails.game.action.PossibleAction;

/**
 * @author Martin Brumm
 * @date 2019-01-26
 */
public class CoalExchangeRound extends StockRound_1837 {

    private Player playerStartingCERound;
    private HashMultimapState<PublicCompany, PublicCompany> coalCompsPerMajor;
    private ArrayListState<PublicCompany> currentMajorOrder;
    private ArrayListState<Player> currentPlayerOrder;

    /**
     * @param parent
     * @param id
     */
    public CoalExchangeRound(GameManager parent, String id) {
        super(parent, id);
           guiHints.setVisibilityHint(GuiDef.Panel.MAP, true);
           guiHints.setActivePanel(GuiDef.Panel.STATUS);

           raiseIfSoldOut = false;
        }

        public static CoalExchangeRound create(GameManager parent, String id){
            return new CoalExchangeRound(parent, id);
        }

        public void start() {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("StartCoalExchangeRound"));

            coalCompsPerMajor = HashMultimapState.create(this, "CoalComps_"+getId());
            currentPlayerOrder = new ArrayListState(this, "PlayerOrder_"+getId());

            //playerManager.setCurrentPlayer(playerToStartCERound);
            //initPlayer();
            //playerStartingCERound=playerToStartCERound;
            //ReportBuffer.add(this, LocalText.getText("HasFirstTurn",
            //        playerToStartCERound.getId() ));

            init();
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
        List<PublicCompany> minors = new ArrayList<>();
        List<PublicCompany> majors = new ArrayList<>();
        String type;
        Player president;

        // Find all mergeable coal companies,
        // and register these per related major company.
        for (PublicCompany comp : comps) {
            type = comp.getType().getId();
            if (!comp.isClosed() && type.equals("Coal")) {
                PublicCompany major = companyManager
                        .getPublicCompany(comp.getRelatedNationalCompany());
                if (major.hasFloated()) {
                    coalCompsPerMajor.put(major, comp);
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
        return true;
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

            if (hasActed.value()) {  //???
                possibleActions.add(new NullAction(getRoot(), NullAction.Mode.DONE));
                return true;
            }
            List<PublicCompany> minors = new ArrayList<>();

            // Pick the first major company that has minors to merge
            for (PublicCompany major : currentMajorOrder) {
                // Determine the player sequence, president first
                Player president = major.getPresident();
                List<Player> playerOrder = playerManager.getNextPlayersAfter(
                        president, true, false);
                // Search who owns the coal companies related to that major
                for (Player player : playerOrder) {
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



        @Override
        // Autopassing does not apply here
        public boolean done(NullAction action, String playerName, boolean hasAutopassed) {

            for (PublicCompany comp : companyManager.getAllPublicCompanies()) {
                if ((comp.getType().getId().equals("Coal")) && (!comp.isClosed()) && (companyManager.getPublicCompany(comp.getRelatedNationalCompany()).hasFloated())) {

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
