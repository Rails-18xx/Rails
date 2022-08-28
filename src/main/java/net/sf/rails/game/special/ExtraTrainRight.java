package net.sf.rails.game.special;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.financial.Bank;

public class ExtraTrainRight extends SpecialRight {

    /** The number of <i>additional</i> trains that a company may own above the current train limit.
     * The default value is 1.
     */
    int extraTrains = 1;

    public ExtraTrainRight(RailsItem parent, String id) {
        super(parent, id);
        rightType = "extraTrain";
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        extraTrains = rightTag.getAttributeAsInteger("extraTrains", extraTrains);
    }

    public int getExtraTrains() {
        return extraTrains;
    }

    @Override
    public String toText() {
        return "Tr+"+extraTrains;
    }
    // Note: for toMenu() see the superclass SpecialRight

    public String getInfo() {
        return toMenu();
    }


}
