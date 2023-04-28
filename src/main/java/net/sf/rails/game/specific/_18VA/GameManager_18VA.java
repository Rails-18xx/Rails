package net.sf.rails.game.specific._18VA;

import net.sf.rails.common.GuiDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;

public class GameManager_18VA extends GameManager {

    public GameManager_18VA(RailsRoot parent, String id) {

        super(parent, id);
    }

    @Override
    public void setGuiParameters() {
        super.setGuiParameters();
        guiParameters.put(GuiDef.Parm.HAS_SPECIAL_COMPANY_INCOME, true);

    }

    /** Calculate value of a CMD */
    public int getValuePerTrain (Train train) {
        if (train.getType().getCategory().equalsIgnoreCase("goods")) {
            return 20 * train.getType().getMajorStops();
        } else {
            return 0;
        }
    }

}
