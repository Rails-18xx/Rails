package net.sf.rails.game.specific._18Scan;

import net.sf.rails.common.LocalText;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.ui.swing.ORUIManager;
import rails.game.action.PossibleActions;

public class ORUIManager_18Scan extends ORUIManager {

    private OperatingRound operatingRound;

    public ORUIManager_18Scan () {super();}

   @Override
    protected void initOR (OperatingRound or) {
        super.initOR (or);
        operatingRound = or;
        if (or instanceof DestinationRound_18Scan) {
            orWindow.setTitle(LocalText.getText("MapWindowDestinationRoundTitle",
                   LocalText.getText("Minor"),
                   or.getOperatingCompany().getId()));
        }

    }

    @Override
    protected void checkForGameSpecificActions(PublicCompany orComp,
                                               GameDef.OrStep orStep,
                                               PossibleActions possibleActions) {
        if (operatingRound instanceof DestinationRound_18Scan && orStep == GameDef.OrStep.CALC_REVENUE) {
            messagePanel.updateMessage("Set revenue - K80 will be added to what is shown here");
        }

    }

}
