/**
 *
 */
package net.sf.rails.game.specific._1837;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.GameOption;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.StockRound;
import rails.game.action.MergeCompanies;
import rails.game.action.NullAction;

/**
 * @author Martin Brumm
 * @date 2019-01-26
 */
public class CoalExchangeRound extends StockRound_1837 {

    private Player playerStartingCERound;
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

        public void start(Player playerToStartCERound) {
            ReportBuffer.add(this, "");
            ReportBuffer.add(this, LocalText.getText("StartCoalExchangeRound"));

            playerManager.setCurrentPlayer(playerToStartCERound);
            initPlayer();
            playerStartingCERound=playerToStartCERound;
            ReportBuffer.add(this, LocalText.getText("HasFirstTurn",
                    playerToStartCERound.getId() ));
        }
        /*----- General methods -----*/

        @Override
        public boolean setPossibleActions() {

            if (discardingTrains.value()) {
                return setTrainDiscardActions();
            } else {
                return setMinorMergeActions();
            }

        }

        private boolean setMinorMergeActions() {

            if (hasActed.value()) {
                possibleActions.add(new NullAction(NullAction.Mode.DONE));
                return true;
            }

            List<PublicCompany> comps =
                companyManager.getAllPublicCompanies();
            List<PublicCompany> minors = new ArrayList<PublicCompany>();
            PublicCompany targetCompany = null;
            String type;
/**
 * Minor Companies are merged on request until the final Exchange Round, Director goes first and then everyone else is asked each round
 */
            for (PublicCompany comp : comps) {
                type = comp.getType().getId();



                 if ((type.equals("Coal")) && (companyManager.getPublicCompany(comp.getRelatedNationalCompany()).hasFloated())) {
                    if (comp.isClosed()) continue;
                    if (comp.getPresident() == currentPlayer) {
                        minors.add(comp);
                        targetCompany = companyManager.getPublicCompany(comp.getRelatedNationalCompany());
                        possibleActions.add(new MergeCompanies(comp, targetCompany, true));

                    }
                }
            }
            if (!minors.isEmpty()) {
                possibleActions.add(new NullAction(NullAction.Mode.DONE));
                return true;
            }

/**
 * The current Player wasnt the director we are looking for the next one
 */
           while (minors.isEmpty()) {
                setNextPlayer();
                for (PublicCompany comp : comps) {
                    type = comp.getType().getId();
                    if ((type.equals("Coal")) && (companyManager.getPublicCompany(comp.getRelatedNationalCompany()).hasFloated())) {
                        if (comp.isClosed()) continue;
                        if (comp.getPresident() == currentPlayer) {
                            targetCompany = companyManager.getPublicCompany(comp.getRelatedNationalCompany());
                            possibleActions.add(new MergeCompanies(comp, targetCompany, true));

                            minors.add(comp);
                        }
                    }
                }
                if (!minors.isEmpty()) {
                    possibleActions.add(new NullAction(NullAction.Mode.DONE));
                    return true;
                }
                //Inner loop
                if (currentPlayer == playerStartingCERound) {
                    finishRound();
                    return false;
                }
            } //While Loop
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
