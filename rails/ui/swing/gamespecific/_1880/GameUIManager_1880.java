/**
 * 
 */
package rails.ui.swing.gamespecific._1880;



import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import rails.ui.swing.GameUIManager;
import rails.common.LocalText;
import rails.game.specific._1880.BuildingRights_1880;
import rails.game.specific._1880.CloseInvestor_1880;
import rails.game.specific._1880.ExchangeForCash;
import rails.game.specific._1880.ForcedRocketExchange;
import rails.game.specific._1880.ParSlotManager_1880;
import rails.game.specific._1880.SetupNewPublicDetails_1880;
import rails.game.specific._1880.StartCompany_1880;
import rails.ui.swing.elements.NonModalDialog;
import rails.ui.swing.elements.RadioButtonDialog;

/**
 * @author Martin Brumm
 * @date 5-2-2012
 * 
 */
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
 *           - 20 percent share will allow to choose from all Building Rights (A+B+C, B+C+D and 2 Phase and single Phase rights)
 *           - 30 percent share will allow to choose from 2 Phase Building Rights (A+B, B+C, C+D and all single Phase rights)
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
            possibleBuildingRights = BuildingRights_1880.getRightsForPresidentShareSize(shares);                

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

            String[] possibleBuildingRights = BuildingRights_1880.getRightsForPresidentShareSize(action.getNumberBought());                

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
            
            int[] parSlots = ParSlotManager_1880.filterByPrice(action.getPossibleParSlotIndices(), selectedPrice);
            String[] parSlotStrings = new String[parSlots.length];
            for (int i = 0; i < parSlots.length; i++) {
                parSlotStrings[i] = Integer.toString(parSlots[i] + 1);
            }
            dialog = new RadioButtonDialog(
                    COMPANY_SELECT_PAR_SLOT_INDEX, 
                        this, statusWindow,
                        LocalText.getText("PleaseSelect"),       
                            LocalText.getText("PickParSlot", action.getPlayerName(), selectedPrice, 
                                    action.getCompanyName()), parSlotStrings, 0);
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
            int[] parSlots = ParSlotManager_1880.filterByPrice(action.getPossibleParSlotIndices(), price);
            action.setParSlotIndex(parSlots[index]);

          int freePlayerCash = gameManager.getCurrentPlayer().getFreeCash();
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
            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            ForcedRocketExchange action = (ForcedRocketExchange) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }
            
            List<String> companiesWithSpace = action.getCompaniesWithSpace();
            if (companiesWithSpace.isEmpty() == false) {
                action.setCompanyToReceiveTrain(companiesWithSpace.get(index));
            }
            
        } else {
            // Current dialog not found yet, try the superclass.
            super.dialogActionPerformed(false);
            return;
        }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);
    
    }
    
    public void setupNewPublicDetails(SetupNewPublicDetails_1880 action) {
        // TODO: Check if this is the right first step
        RadioButtonDialog dialog;
        String[] rightsOptions = BuildingRights_1880.getRightsForPresidentShareSize(action.getShares());
        
        dialog = new RadioButtonDialog (NEW_COMPANY_SELECT_BUILDING_RIGHT,
                this,
                statusWindow,
                LocalText.getText("PleaseSelect"),
                LocalText.getText(
                        "WhichBuildingRight",action.getPlayerName(),
                        action.getCompanyName()),
                        rightsOptions, 0);
            setCurrentDialog(dialog, action);
            statusWindow.disableButtons();
        setCurrentDialog(dialog, action);
        statusWindow.disableButtons();
        return;
    }
    
    public void closeInvestor(CloseInvestor_1880 action) {
        String[] cashOptions = new String[2];
        cashOptions[0] = LocalText.getText("GiveToCompany", action.getInvestor().getCash(), action.getInvestor().getLinkedCompany().getName());
        cashOptions[1] = LocalText.getText("GiveToPresident", (action.getInvestor().getCash()/5), action.getInvestor().getPresident());
        
        String cashChoice =
                (String) JOptionPane.showInputDialog(orWindow,
                        LocalText.getText("FIClosingAskAboutTreasury", action.getInvestor().getName()),
                        LocalText.getText("TreasuryChoice"),
                        JOptionPane.QUESTION_MESSAGE, null,
                        cashOptions, cashOptions[0]);
        if (cashChoice == cashOptions[0]) {
            action.setTreasuryToLinkedCompany(true);
        } else {
            action.setTreasuryToLinkedCompany(false);
        }
        if (action.getInvestor().getLinkedCompany().getNumberOfFreeBaseTokens() > 0) {
            String[] tokenOptions = new String[2];
            tokenOptions[0] = LocalText.getText("ReplaceToken", action.getInvestor().getName(), action.getInvestor().getLinkedCompany().getName());
            tokenOptions[1] = LocalText.getText("DoNotReplaceToken", action.getInvestor().getName(), action.getInvestor().getLinkedCompany().getName());
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
        
        List<String> companiesWithSpace = forcedRocketExchange.getCompaniesWithSpace();
        if (companiesWithSpace.isEmpty() == false) {
            exchangeOptions = new String[companiesWithSpace.size()];
            for (int i = 0; i < companiesWithSpace.size(); i++) {
                exchangeOptions[i] = "Put 4-train in " + companiesWithSpace.get(i); 
            }
        } else {
            exchangeOptions = new String[1];
        }

        dialog =
                new RadioButtonDialog(FORCED_ROCKET_EXCHANGE, this,
                        statusWindow, LocalText.getText("PleaseSelect"),
                        "Which company should receive the 4-train?", exchangeOptions, 0);
        setCurrentDialog(dialog, forcedRocketExchange);
        statusWindow.disableButtons();        
    }
        
}

