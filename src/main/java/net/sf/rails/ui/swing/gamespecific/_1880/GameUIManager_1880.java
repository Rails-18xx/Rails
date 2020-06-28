package net.sf.rails.ui.swing.gamespecific._1880;

import com.google.common.collect.Lists;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.Train;
import net.sf.rails.game.specific._1880.BuildingRights_1880;
import net.sf.rails.game.specific._1880.GameManager_1880;
import net.sf.rails.game.specific._1880.ParSlot;
import net.sf.rails.game.specific._1880.ParSlotManager;
import net.sf.rails.ui.swing.GameUIManager;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import rails.game.specific._1880.CloseInvestor_1880;
import rails.game.specific._1880.ExchangeForCash;
import rails.game.specific._1880.ForcedRocketExchange;
import rails.game.specific._1880.StartCompany_1880;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GameUIManager_1880 extends GameUIManager {
    public static final String COMPANY_SELECT_BUILDING_RIGHT = "SelectBuildingRight";
    public static final String COMPANY_SELECT_PRESIDENT_SHARE_SIZE = "SelectPresidentShareSize";
    public static final String COMPANY_START_PRICE_DIALOG = "CompanyStartPrice";
    public static final String COMPANY_SELECT_PAR_SLOT_INDEX = "CompanySelectParSlotIndex";
    public static final String EXCHANGE_PRIVATE_FOR_CASH = "ExchangePrivateForCash";
    public static final String FORCED_ROCKET_EXCHANGE = "ForcedRocketExchange";

    public static final String NEW_COMPANY_SELECT_BUILDING_RIGHT = "NewSelectBuildingRight";


    @Override
    public void dialogActionPerformed () {
        String key = "";
        String[] presidentShareSizes;
        if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog) currentDialog).getKey();

        // Check for the dialogs that are postprocessed in this class.
/*
 * The mechanismn for starting a company and getting the necessary decisions by a player
 * is implemented with the following steps
 *                              Player chooses Startprice
 *                                        |
 *           Player chooses President share percentage (20, 30 or 40 percent share)
 *                                        |
 *           Player chooses Building Right based on percentage of president share
 *
 *           - 20 percent share will allow to choose from all Building Rights (A+B+C, B+C+D and 2 Phase and single Phase rightsModel)
 *           - 30 percent share will allow to choose from 2 Phase Building Rights (A+B, B+C, C+D and all single Phase rightsModel)
 *           - 40 percent share will limit the player to a building right for one Phase (A, B, C, D)
 */

        if (COMPANY_SELECT_PRESIDENT_SHARE_SIZE.equals(key)) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_1880 action = (StartCompany_1880) currentDialogAction;
            String[] possibleBuildingRights;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }

            int shares = 0;

            if (index > 1) { // 40 Percent Share has been chosen
                shares = 4;
            } else if ( index == 1) {
                shares = 3;
            } else {  // 20 Percent Share chosen
                shares = 2;
            }

            action.setNumberBought(shares);
            possibleBuildingRights =
                    BuildingRights_1880.getRightsForPresidentShareSize(shares).toArray(new String[0]);

            dialog = new RadioButtonDialog (COMPANY_SELECT_BUILDING_RIGHT,
                    this,
                    statusWindow,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText(
                            "WhichBuildingRight",action.getPlayerName(),
                            action.getCompanyName()),
                            possibleBuildingRights, 0);
                setCurrentDialog(dialog, action);
                statusWindow.disableButtons();
                return;

        } else if (COMPANY_SELECT_BUILDING_RIGHT.equals(key)) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_1880 action = (StartCompany_1880) currentDialogAction;

            String[] possibleBuildingRights =
                    BuildingRights_1880.getRightsForPresidentShareSize(action.getNumberBought()).toArray(new String[0]);

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }
            action.setBuildingRights(possibleBuildingRights[index]);

        } else if (COMPANY_START_PRICE_DIALOG.equals(key)
                && currentDialogAction instanceof StartCompany_1880) {

            // A start price has been selected (or not) for a starting major company.
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_1880 action = (StartCompany_1880) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }

            int[] startPrices = action.getStartPrices();
            Arrays.sort(startPrices);

            int selectedPrice = startPrices[index];
            action.setStartPrice(selectedPrice);

            ParSlotManager parSlotManager = ((GameManager_1880) getGameManager()).getParSlotManager();
            List<ParSlot> parSlots = parSlotManager.filterByPrice(action.getPossibleParSlotIndices(), selectedPrice);
            List<String> parSlotStrings = Lists.newArrayList();
            for (ParSlot slot:parSlots) {
                parSlotStrings.add(String.valueOf(slot.getIndex() + 1));
            }
            dialog = new RadioButtonDialog(
                    COMPANY_SELECT_PAR_SLOT_INDEX,
                        this, statusWindow,
                        LocalText.getText("PleaseSelect"),
                            LocalText.getText("PickParSlot", action.getPlayerName(), selectedPrice,
                                    action.getCompanyName()), parSlotStrings.toArray(new String[0]), 0);
            setCurrentDialog(dialog, action);
            statusWindow.disableButtons();
            return;
        } else if (COMPANY_SELECT_PAR_SLOT_INDEX.equals(key)
                && currentDialogAction instanceof StartCompany_1880) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_1880 action = (StartCompany_1880) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }

            int price = action.getPrice();

            ParSlotManager parSlotManager = ((GameManager_1880) getGameManager()).getParSlotManager();
            List<ParSlot> parSlots = parSlotManager.filterByPrice(action.getPossibleParSlotIndices(), price);
            action.setParSlotIndex(parSlots.get(index).getIndex());

          int freePlayerCash = getRoot().getPlayerManager().getCurrentPlayer().getFreeCash();
          if (freePlayerCash >= (price*4)) { //enough Cash for 40 Percent
              presidentShareSizes = new String[] {"20 Percent", "30 Percent", "40 Percent"};
          } else if (freePlayerCash >= (price*3)) { //enough Cash for 30 Percent
              presidentShareSizes = new String[] {"20 Percent", "30 Percent"};
          } else  { //enough Cash only for 20 Percent
              presidentShareSizes = new String[] {"20 Percent"};
          }
          dialog = new RadioButtonDialog (COMPANY_SELECT_PRESIDENT_SHARE_SIZE,
                  this,
                  statusWindow,
                  LocalText.getText("PleaseSelect"),
                  LocalText.getText(
                          "WhichPresidentShareSize",
                          action.getPlayerName(),
                          action.getCompanyName()),
                          presidentShareSizes, 0);
              setCurrentDialog(dialog, action);
              statusWindow.disableButtons();
              return;

        } else if (EXCHANGE_PRIVATE_FOR_CASH.equals(key)
                && currentDialogAction instanceof ExchangeForCash) {
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            ExchangeForCash action = (ExchangeForCash) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }

            if (index == 0) {
                action.setExchangeCompany(true);
            } else {
                action.setExchangeCompany(false);
            }

        } else if (FORCED_ROCKET_EXCHANGE.equals(key)
                && currentDialogAction instanceof ForcedRocketExchange) {
            if (handleForcedRocketExchange() == false) {
                return;
            }
        } else {
            // Current dialog not found yet, try the superclass.
            super.dialogActionPerformed(false);
            return;
        }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);

    }

    public void closeInvestor(CloseInvestor_1880 action) {
        String[] cashOptions = new String[2];
        cashOptions[0] = LocalText.getText("GiveToCompany", action.getInvestor().getCash(), action.getInvestor().getLinkedCompany().getId());
        cashOptions[1] = LocalText.getText("GiveToPresident", (action.getInvestor().getCash()/5), action.getInvestor().getPresident().getId());

        String cashChoice =
                (String) JOptionPane.showInputDialog(orWindow,
                        LocalText.getText("FIClosingAskAboutTreasury", action.getInvestor().getId()),
                        LocalText.getText("TreasuryChoice"),
                        JOptionPane.QUESTION_MESSAGE, null,
                        cashOptions, cashOptions[0]);
        if (cashChoice == cashOptions[0]) {
            action.setTreasuryToLinkedCompany(true);
        } else {
            action.setTreasuryToLinkedCompany(false);
        }
        if (action.getInvestor().getLinkedCompany().value().getNumberOfFreeBaseTokens() > 0) {
            String[] tokenOptions = new String[2];
            tokenOptions[0] = LocalText.getText("ReplaceToken", action.getInvestor().getId(), action.getInvestor().getLinkedCompany().getId());
            tokenOptions[1] = LocalText.getText("DoNotReplaceToken", action.getInvestor().getId(), action.getInvestor().getLinkedCompany().getId());
            String tokenChoice =
                    (String) JOptionPane.showInputDialog(orWindow,
                            LocalText.getText("FIClosingAskAboutToken"),
                            LocalText.getText("TokenChoice"),
                            JOptionPane.QUESTION_MESSAGE, null,
                            tokenOptions, tokenOptions[0]);
            if (tokenChoice == tokenOptions[0]) {
                action.setReplaceToken(true);
            } else {
                action.setReplaceToken(false);
            }
        } else {
            action.setReplaceToken(false);
        }

        orWindow.process(action);
    }

    public void exchangeForCash(ExchangeForCash exchangeForCash) {
        RadioButtonDialog dialog;
        String[] exchangeOptions;
        if (exchangeForCash.getOwnerHasChoice() == true) {
            exchangeOptions =
                    new String[] {LocalText.getText("ExchangeWRForCash", exchangeForCash.getCashValue()),
                        LocalText.getText("DoNotExchange") };
        } else {
            exchangeOptions =
                    new String[] { LocalText.getText("ExchangeWRForCash", exchangeForCash.getCashValue()) };
        }

        dialog =
                new RadioButtonDialog(EXCHANGE_PRIVATE_FOR_CASH, this,
                        statusWindow, LocalText.getText("PleaseSelect"),
                        LocalText.getText("CanExchangeWR", exchangeForCash.getOwnerName()), exchangeOptions, 0);
        setCurrentDialog(dialog, exchangeForCash);
        statusWindow.disableButtons();
        return;
    }

    public void forcedRocketExchange(ForcedRocketExchange forcedRocketExchange) {
        RadioButtonDialog dialog;
        String[] exchangeOptions;

        List<RocketDestination> destinations = getRocketDestinations(forcedRocketExchange);
        exchangeOptions = new String[destinations.size()];
        for (int i = 0; i < destinations.size(); i++) {
            RocketDestination destination = destinations.get(i);
            if (destination.hasReplacementTrain() == true) {
                exchangeOptions[i] = LocalText.getText("PlaceRocketAndReplace", destination.getCompany(),
                        destination.getReplacementTrain());
            } else {
                exchangeOptions[i] = LocalText.getText("PlaceRocket", destination.getCompany());
            }
        }

        dialog =
                new RadioButtonDialog(FORCED_ROCKET_EXCHANGE, this,
                        statusWindow, LocalText.getText("PleaseSelect"),
                        LocalText.getText("SelectRocketCompany"), exchangeOptions, 0);
        setCurrentDialog(dialog, forcedRocketExchange);
        statusWindow.disableButtons();
    }

    private boolean handleForcedRocketExchange() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        ForcedRocketExchange action = (ForcedRocketExchange) currentDialogAction;

        int index = dialog.getSelectedOption();
        if (index < 0) {
            currentDialogAction = null;
            return false;
        }

        List<RocketDestination> destinations = getRocketDestinations(action);
        action.setCompanyToReceiveTrain(destinations.get(index).getCompany());
        action.setTrainToReplace(destinations.get(index).getReplacementTrain());

        return true;
    }

    private List<RocketDestination> getRocketDestinations(ForcedRocketExchange forcedRocketExchange) {
        List<RocketDestination> destinations = new ArrayList<RocketDestination>();

        if (forcedRocketExchange.hasCompaniesWithSpace() == true) {
            List<String> companies = forcedRocketExchange.getCompaniesWithSpace();
            for (String company : companies) {
                destinations.add(new RocketDestination(company, null));
            }
        } else {
            Map<String, List<Train>> companies = forcedRocketExchange.getCompaniesWithNoSpace();
            for (String company : companies.keySet()) {
                for (Train train : companies.get(company)) {
                    destinations.add(new RocketDestination(company, train.getId()));
                }
            }
        }

        return destinations;
    }

    private static class RocketDestination {
        private String company;
        private String replacementTrain;

        public RocketDestination(String company, String replacementTrain) {
            this.company = company;
            this.replacementTrain = replacementTrain;
        }

        public String getCompany() {
            return company;
        }

        public boolean hasReplacementTrain() {
            return (replacementTrain != null);
        }

        public String getReplacementTrain() {
            return replacementTrain;
        }
    }

}

