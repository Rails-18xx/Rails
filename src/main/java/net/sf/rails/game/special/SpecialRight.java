package net.sf.rails.game.special;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.*;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.Owner;
import net.sf.rails.util.Util;

public class SpecialRight extends SpecialProperty implements Configurable, Closeable {

    protected String rightType;  // May be redundant
    protected String rightName;
    protected int cost = 0;

    protected Tag rightTag = null;

    private String removingObjectDesc = null;
    private Object removingObject = null;

    /**
     * Used by Configure (via reflection) only
     */
    public SpecialRight(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);

        rightTag = tag.getChild("SpecialRight");
        if (rightTag == null) {
            throw new ConfigurationException("<SpecialRight> tag missing");
        }

        rightType = rightTag.getAttributeAsString("type");
        if (!Util.hasValue(rightType))
            throw new ConfigurationException(
                    "SpecialRight: no Right type specified");


        rightName = rightTag.getAttributeAsString("name");
        if (!Util.hasValue(rightName))
            throw new ConfigurationException(
                    "SpecialRight: no Right name specified");

        cost = rightTag.getAttributeAsInteger("cost", 0);

        removingObjectDesc = rightTag.getAttributeAsString("remove");
    }

    @Override
    public void finishConfiguration(RailsRoot root) throws ConfigurationException {

        prepareForRemoval (root.getPhaseManager());

        rightTag = null;
    }

    /**
     * Prepare the right for removal, if so configured.
     * The only case currently implemented to trigger removal
     * is the start of a given phase.
     * Note: This method is copied from class BonusToken
     */
    public void prepareForRemoval (PhaseManager phaseManager) {

        if (removingObjectDesc == null) return;

        if (removingObject == null) {
            String[] spec = removingObjectDesc.split(":");
            if ( "Phase".equalsIgnoreCase(spec[0])) {
                removingObject = phaseManager.getPhaseByName(spec[1]);
            }
        }

        if (removingObject instanceof Phase) {
            ((Phase) removingObject).addObjectToClose(this);
        }
    }


    public String getType() {
        return rightType;
    }

    public String getName() {
        return rightName;
    }

    public int getCost() {
        return cost;
    }

    public boolean isExecutionable() {
        return true;
    }

    /**
     * Remove the right.
     * This method can be called by a certain phase when it starts.
     * See prepareForRemovel().
     */

    public void close() {
        Owner owner = getOwner();
        if (owner != null && owner instanceof PublicCompany) {
            this.moveTo (getRoot().getBank().getScrapHeap());
        }
    }

    public String getClosingInfo() { return ""; }

    public String toString() {
        return rightName;
    }

    public String toText() {
        return rightName;
    }

    public String toMenu() {
        if (cost > 0) {
            return LocalText.getText("BuyRight" , rightName, Bank.format(this, cost));
        } else {
            return LocalText.getText("GetRight", rightName);
        }
    }
}