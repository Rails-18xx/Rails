/**
 * 
 */
package rails.ui.swing.gamespecific._1880;



import rails.ui.swing.GameUIManager;
import rails.common.LocalText;
import rails.game.action.PossibleORAction;
import rails.game.specific._1880.OperatingRound_1880;
import rails.game.specific._1880.PublicCompany_1880;
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
    public static final String Investor_has_Destination = "Investor_at_Destination";
    

    @Override
    public void dialogActionPerformed () {

        String key = "";
        String[] brights;
        String[] brights2= {"A", "B", "C","D", "A+B", "A+B+C", "B+C", "B+C+D", "C+D"}; 
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

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }
            
            if (index > 1) { // 40 Percent Share has been chosen
                action.setPresidentPercentage((PublicCompany_1880) action.getCompany(), 40);
               brights= new String[] {"A", "B", "C", "D"};
                
            } else if ( index == 1) {
                action.setPresidentPercentage((PublicCompany_1880) action.getCompany(), 30);
                brights= new String[] {"A", "B", "C", "D", "A+B", "B+C", "C+D"};
            } else {  // 20 Percent Share chosen
                action.setPresidentPercentage((PublicCompany_1880) action.getCompany(), 20);
                brights= new String[] {"A", "B", "C","D", "A+B", "A+B+C", "B+C", "B+C+D", "C+D"};
            }
            
            dialog = new RadioButtonDialog (COMPANY_SELECT_BUILDING_RIGHT,
                    this,
                    statusWindow,
                    LocalText.getText("PleaseSelect"),
                    LocalText.getText(
                            "WhichBuildingRight",action.getPlayerName(),
                            action.getCompanyName()),
                            brights, -1);
                setCurrentDialog(dialog, action);
                statusWindow.disableButtons();
                return;
            
        } else if (COMPANY_SELECT_BUILDING_RIGHT.equals(key)) {

            RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
            StartCompany_1880 action = (StartCompany_1880) currentDialogAction;

            int index = dialog.getSelectedOption();
            if (index < 0) {
                currentDialogAction = null;
                return;
            }
                action.setBuildingRight((PublicCompany_1880) action.getCompany(), brights2[index]);
            
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
            
            int index2 = index /4;
            action.setStartPrice(action.getStartPrices()[index2], index);
            action.setOperatingSlot(index);
            

            /* Set up another dialog for the next step
            *  need to setup Options based on the Presidents Certificate Size...
            *  The player should only get valid percentages presented to him for selection
            *  This leads to the check what amount of cash does the player have
            */
             
            int freePlayerCash = gameManager.getCurrentPlayer().getFreeCash();
            if (freePlayerCash >= (action.getStartPrices()[index2]*4)) { //enough Cash for 40 Percent 
                presidentShareSizes = new String[] {"20 Percent", "30 Percent", "40 Percent"};
            } else if (freePlayerCash >= (action.getStartPrices()[index2]*3)) { //enough Cash for 30 Percent 
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
                            presidentShareSizes, -1);
                setCurrentDialog(dialog, action);
                statusWindow.disableButtons();
                return;
        } else {
            // Current dialog not found yet, try the superclass.
            super.dialogActionPerformed(false);
            return;
        }

        // Dialog action found and processed, let the superclass initiate processing.
        super.dialogActionPerformed(true);
    
    }

}

