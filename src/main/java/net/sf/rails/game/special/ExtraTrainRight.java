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

    public String toMenu() {
        StringBuilder b = new StringBuilder();
        b.append(getCost() > 0 ? "Buy '" : "Use '").append(getName()).append("' right");
        if (getCost() > 0) b.append(" for ").append(Bank.format(this, getCost()));
        return b.toString();
    }

    @Override
    public String toText() {
        //return LocalText.getText("ExtraTrains",
        //        getOwner(), extraTrains);
        return "Tr+"+extraTrains;
    }

    public String getInfo() {
        return toMenu();
    }


}
