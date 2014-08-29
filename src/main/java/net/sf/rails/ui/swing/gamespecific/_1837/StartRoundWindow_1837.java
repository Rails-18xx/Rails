package net.sf.rails.ui.swing.gamespecific._1837;

import javax.swing.JDialog;

import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
import rails.game.specific._1837.SetHomeHexLocation;
import net.sf.rails.common.LocalText;
import net.sf.rails.ui.swing.StartRoundWindow;
import net.sf.rails.ui.swing.elements.NonModalDialog;
import net.sf.rails.ui.swing.elements.RadioButtonDialog;

public class StartRoundWindow_1837 extends StartRoundWindow {
    
    /* Keys of dialogues owned by this class */
    public static final String COMPANY_START_HEX_DIALOG = "CompanyStartHex";
    private static final String[] hexes = {"L3", "L9"};
    
    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;

    private static final long serialVersionUID = 1L;

    public StartRoundWindow_1837() {
       
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
           }
        }
        return true;
    }
    
    public void dialogActionPerformed () {
        String key="";
        
        if (currentDialog instanceof NonModalDialog) {
            key = ((NonModalDialog) currentDialog).getKey();
        }
        
        if (COMPANY_START_HEX_DIALOG.equals(key)) {
            handleStartHex();
        }
        return;
    }
    
    

    //-- Start Hex for S5
    private boolean requestHomeHex(SetHomeHexLocation action) {
        RadioButtonDialog dialog = new RadioButtonDialog(
                COMPANY_START_HEX_DIALOG, this, this,
                LocalText.getText("PleaseSelect"),       
                LocalText.getText("StartingHex", action.getPlayerName(), action.getCompanyName()),
                hexes, 0);
        setCurrentDialog (dialog, action);
        return true;
    }

    private void handleStartHex() {
        RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
        SetHomeHexLocation action =
                (SetHomeHexLocation) currentDialogAction;
       
        int index = dialog.getSelectedOption();
        if (index >= 0) {
            action.setHomeHex(hexes[index]);  
        } 
    }
    
    public void updateStatus(boolean myTurn) {
        for (PossibleAction action : possibleActions.getList()) {
            if (action instanceof SetHomeHexLocation) {
                requestHomeHex((SetHomeHexLocation) action);
                return;
            }
        }
        super.updateStatus(myTurn);
    }

    public void setCurrentDialog (JDialog dialog, PossibleAction action) {
        if (currentDialog != null) {
            currentDialog.dispose();
        }
        currentDialog = dialog;
        currentDialogAction = action;
        disableButtons();
    }
}
