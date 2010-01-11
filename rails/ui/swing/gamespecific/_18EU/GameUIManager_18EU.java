package rails.ui.swing.gamespecific._18EU;

import javax.swing.JOptionPane;

import rails.game.PublicCompanyI;
import rails.game.action.MergeCompanies;
import rails.game.specific._18EU.StartCompany_18EU;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;

public class GameUIManager_18EU extends GameUIManager {

	@Override
	public void dialogActionPerformed () {

    	if (currentDialog instanceof RadioButtonDialog
    			&& currentDialogAction instanceof MergeCompanies) {

    		RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
    		MergeCompanies action = (MergeCompanies) currentDialogAction;
            PublicCompanyI minor = action.getMergingCompany();

            if (action.getSelectedTargetCompany() == null) {
            	// Step 1: selection of the major company to merge into
	            int choice = dialog.getSelectedOption();
	            if (choice < 0) return;

	            PublicCompanyI major = action.getTargetCompanies().get(choice);
	            action.setSelectedTargetCompany(major);

	            if (major != null && action.canReplaceToken(choice)) {

	                boolean replaceToken =
	                        JOptionPane.showConfirmDialog(statusWindow, LocalText.getText(
	                                    "WantToReplaceToken",
	                                    minor.getName(),
	                                    major.getName() ),
	                                LocalText.getText("PleaseSelect"),
	                                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	                action.setReplaceToken(replaceToken);
	            }
            } else {
            	// To be added later when ReplaceToken dialog is modeless
            }

     	} else if (currentDialog instanceof RadioButtonDialog
        			&& currentDialogAction instanceof StartCompany_18EU) {

    		RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
    		StartCompany_18EU action = (StartCompany_18EU) currentDialogAction;

    		if (action.getMinorsToMerge() != null) {
    			// Up to phase 5: a minor to merge has been selected (or not)
                int choice = dialog.getSelectedOption();
	            if (choice >= 0) {
	                action.setChosenMinor(action.getMinorsToMerge().get(choice));
	            } else {
	                return;
	            }
    		} else if (action.getAvailableHomeStations() != null) {
    			// From phase 6: a home station has been selected (or not)

                int index = dialog.getSelectedOption();
                if (index >= 0) {
                    action.setHomeStation(action.getAvailableHomeStations().get(index));
                } else {
                    return;
                }
            }
        } else {
            super.dialogActionPerformed(false);
        }

    	super.dialogActionPerformed(true);
	}

}
