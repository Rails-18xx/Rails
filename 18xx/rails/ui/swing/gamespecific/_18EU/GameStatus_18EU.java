package rails.ui.swing.gamespecific._18EU;


import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.JOptionPane;

import rails.game.PublicCompanyI;
import rails.game.action.PossibleAction;
import rails.game.specific._18EU.StartCompany_18EU;
import rails.ui.swing.GameStatus;
import rails.ui.swing.elements.RadioButtonDialog;
import rails.util.LocalText;


/**
 * This class is incorporated into StatusWindow and displays the bulk of rails.game
 * status information.
 */
public class GameStatus_18EU extends GameStatus
{
	/** Stub allowing game-specific extensions */
	@Override
    protected PossibleAction processGameSpecificActions (
	        ActionEvent actor,
	        PossibleAction chosenAction) {

	    if (chosenAction instanceof StartCompany_18EU) {

	        StartCompany_18EU action = (StartCompany_18EU) chosenAction;
	        List<PublicCompanyI> minors =
	            ((StartCompany_18EU)chosenAction).getMinorsToMerge();

	        if (minors == null || minors.isEmpty()) {
	            // Do nothing
	        } else if (minors.size() == 1) {
	            PublicCompanyI minor = minors.get(0);
	            int answer = JOptionPane.showConfirmDialog(
	                    parent,
	                    LocalText.getText("MergeMinorConfirm",
	                            new String[] {
	                                minor.getName(),
	                                action.getCertificate().getCompany().getName()
	                    }),
	                    LocalText.getText("PleaseConfirm"),
	                    JOptionPane.OK_CANCEL_OPTION,
	                    JOptionPane.QUESTION_MESSAGE);
	            if (answer == JOptionPane.OK_OPTION) {
	                action.setChosenMinor(minor);
	                chosenAction = action;
	            } else {
	            	chosenAction = null;
	            }
	        } else {
	            String[] options = new String[minors.size()];
	            int i = 0;
	            for (PublicCompanyI minor : minors) {
	                options[i++] = "Minor " + minor.getName()+ " " + minor.getLongName();
	            }
	            int choice = new RadioButtonDialog(
	                    this,
	                    LocalText.getText("PleaseSelect"),
	                    LocalText.getText("SelectMinorToMerge",
	                            action.getCertificate().getCompany().getName()),
	                    options,
	                    -1)
	                .getSelectedOption();
	            if (choice >= 0) {
	                action.setChosenMinor(minors.get(choice));
	                chosenAction = action;
	            } else {
	            	chosenAction = null;
	            }
	        }
	    }
	    return chosenAction;
	}

}
