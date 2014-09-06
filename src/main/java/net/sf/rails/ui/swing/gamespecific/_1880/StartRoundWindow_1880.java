/**
 * 
 */
package net.sf.rails.ui.swing.gamespecific._1880;



import javax.swing.JDialog;

import net.sf.rails.common.LocalText;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;
import rails.game.action.BuyStartItem;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
import rails.game.specific._1880.SetupNewPublicDetails_1880;


/**
 * @author Martin
 * @date 07.05.2011
 */
public class StartRoundWindow_1880 extends StartRoundWindow {


    /* Keys of dialogues owned by this class */
    public static final String COMPANY_BUILDING_RIGHT_DIALOG = "CompanyBuildingRight";
    public static final String COMPANY_PAR_SLOT_DIALOG = "CompanyParSlotRight";

    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;
    protected int[] startPrices = null;
    protected int[] operationOrder = null;
    private static final String[] bRights = {"A+B+C", "B+C+D"};
    private static final String[] parSlots = {"1", "2", "3", "4"};

    private static final long serialVersionUID = 1L;
    /**
     * @param round
     * @param parent
     */
    public StartRoundWindow_1880() {
    }


    @Override
    public boolean processImmediateAction() {

        log.debug("ImmediateAction=" + immediateAction);
        if (immediateAction != null) {
            // Make a local copy and discard the original,
            // so that it's not going to loop.
            PossibleAction nextAction = immediateAction;
            immediateAction = null;
            if (nextAction instanceof StartItemAction) {
                StartItemAction action = (StartItemAction) nextAction;
                if (action instanceof BuyStartItem) {
                    requestStartPrice((BuyStartItem) action);
                    return false;
                }
            }
        }
        return true;
    }


    public void dialogActionPerformed () {
        String key="";

        if (currentDialog instanceof NonModalDialog) {
            key = ((NonModalDialog) currentDialog).getKey();
        }

        if (COMPANY_PAR_SLOT_DIALOG.equals(key)) {
            handleStartSlot();
        } else if (COMPANY_BUILDING_RIGHT_DIALOG.equals(key)) {
            handleBuildingRights();

        }
        return;
    }

    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
        disableButtons();
    }

    public void updateStatus(boolean myTurn) {
        for (PossibleAction action : possibleActions.getList()) {
            if (action instanceof SetupNewPublicDetails_1880) {
                requestStartSlot((SetupNewPublicDetails_1880) action);
                return;
            }
        }
        super.updateStatus(myTurn);
    }


    //-- Start slot
    private boolean requestStartSlot(SetupNewPublicDetails_1880 action) {
        RadioButtonDialog dialog = new RadioButtonDialog(
                COMPANY_PAR_SLOT_DIALOG, this, this,
                LocalText.getText("PleaseSelect"),       
                LocalText.getText("PickParSlot", action.getPlayerName(), action.getPrice(), action.getCompanyName()),
                parSlots, 0);
        setCurrentDialog (dialog, action);
        return true;
    }

    private void handleStartSlot() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        SetupNewPublicDetails_1880 action =
                (SetupNewPublicDetails_1880) currentDialogAction;

        int index = dialog.getSelectedOption();
        if (index >= 0) {
            action.setParSlotIndex(index);  // Fortunately this lines up...
            requestBuildingRights(action);
        } 
    }


    //-- Building Rights
    private boolean requestBuildingRights(SetupNewPublicDetails_1880 action) {
        RadioButtonDialog dialog = new RadioButtonDialog(
                COMPANY_BUILDING_RIGHT_DIALOG, this, this,
                LocalText.getText("PleaseSelect"),       
                LocalText.getText("WhichBuildingRight", action.getPlayerName(), action.getCompanyName()),
                bRights, 0);
        setCurrentDialog (dialog, action);
        return true;
    }   


    private void handleBuildingRights() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        SetupNewPublicDetails_1880 action =
                (SetupNewPublicDetails_1880) currentDialogAction;

        int index = dialog.getSelectedOption();
        if (index >= 0) {
            String buildingRight = bRights[index];
            action.setBuildRightsString(buildingRight);
            process(action);
        } 
    }

}

