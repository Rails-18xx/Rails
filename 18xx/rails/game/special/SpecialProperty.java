/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialProperty.java,v 1.22 2010/01/31 22:22:30 macfreek Exp $ */
package rails.game.special;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import rails.game.*;
import rails.game.move.MoveableHolder;
import rails.game.move.ObjectMove;
import rails.game.state.BooleanState;
import rails.util.*;

public abstract class SpecialProperty implements SpecialPropertyI {

    protected PrivateCompanyI privateCompany;
    protected MoveableHolder holder = null;
    protected int closingValue = 0;
    protected BooleanState exercised;
    protected boolean usableIfOwnedByPlayer = false;
    protected boolean usableIfOwnedByCompany = false;

    protected String conditionText = "";
    protected String whenText = "";
    protected String transferText = "";
    protected boolean isORProperty = false;
    protected boolean isSRProperty = false;

    /** Optional descriptive text, for display in menus and info text.
     * Subclasses may put real text in it.
     */
    protected String description = "";

    protected int uniqueId;

    /** To give subclasses access to the various 'managers' */
    protected GameManagerI gameManager;

    protected static Map<Integer, SpecialPropertyI> spMap =
            new HashMap<Integer, SpecialPropertyI>();
    protected static int lastIndex = 0;

    protected static Logger log =
        Logger.getLogger(SpecialProperty.class.getPackage().getName());

    public SpecialProperty() {
        uniqueId = ++lastIndex;
        spMap.put(uniqueId, this);
        gameManager = GameManager.getInstance();
    }

    public void configureFromXML(Tag tag) throws ConfigurationException {

        conditionText = tag.getAttributeAsString("condition");
        if (!Util.hasValue(conditionText))
            throw new ConfigurationException(
                    "Missing condition in private special property");
        setUsableIfOwnedByPlayer(conditionText.matches("(?i).*ifOwnedByPlayer.*"));
        setUsableIfOwnedByCompany(conditionText.matches("(?i).*ifOwnedByCompany.*"));

        whenText = tag.getAttributeAsString("when");
        if (!Util.hasValue(whenText))
            throw new ConfigurationException(
                    "Missing condition in private special property");
        // to be interpreted...

        transferText = tag.getAttributeAsString("transfer", "");
    }

    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {

    }

    public int getUniqueId() {
        return uniqueId;
    }

    public static SpecialPropertyI getByUniqueId(int i) {
        return spMap.get(i);
    }

    public void setCompany(PrivateCompanyI company) {
        privateCompany = company;
        holder = company;
        exercised =
                new BooleanState(company.getName() + "_SP_" + uniqueId
                                 + "_Exercised", false);
    }

    public PrivateCompanyI getCompany() {
        return privateCompany;
    }

    public MoveableHolder getHolder() {
        return holder;
    }

    /**
     * @return Returns the usableIfOwnedByCompany.
     */
    public boolean isUsableIfOwnedByCompany() {
        return usableIfOwnedByCompany;
    }

    /**
     * @param usableIfOwnedByCompany The usableIfOwnedByCompany to set.
     */
    public void setUsableIfOwnedByCompany(boolean usableIfOwnedByCompany) {
        this.usableIfOwnedByCompany = usableIfOwnedByCompany;
    }

    /**
     * @return Returns the usableIfOwnedByPlayer.
     */
    public boolean isUsableIfOwnedByPlayer() {
        return usableIfOwnedByPlayer;
    }

    /**
     * @param usableIfOwnedByPlayer The usableIfOwnedByPlayer to set.
     */
    public void setUsableIfOwnedByPlayer(boolean usableIfOwnedByPlayer) {
        this.usableIfOwnedByPlayer = usableIfOwnedByPlayer;
    }

    public void setExercised() {
        setExercised(true);
    }

    public void setExercised (boolean value) {
        exercised.set(value);
        if (value) privateCompany.checkClosingIfExercised(false);
    }

    public boolean isExercised() {
        return exercised.booleanValue();
    }

    public int getClosingValue() {
        return closingValue;
    }

    public boolean isSRProperty() {
        return isSRProperty;
    }

    public boolean isORProperty() {
        return isORProperty;
    }

    public String getTransferText() {
        return transferText;
    }

    /**
     * Move the special property to another holder.
     * Only to be used for special properties that have the "transfer" attribute.
     */
    public void moveTo(MoveableHolder newHolder) {
        if (transferText.equals("")) return;
        //if (newHolder instanceof Portfolio) {
            new ObjectMove(this, holder, newHolder);
        //}
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " of private "
               + privateCompany.getName();
    }

    /**
     * Default menu item text, should be by all special properties that can
     * appear as a menu item
     */
    public String toMenu() {
        return toString();
    }

    /** Default Info text. To be overridden where useful. */
    public String getInfo() {
        return toString();
    }

    /** Default Help text: "You can " + the menu description */
    public String getHelp() {
        return LocalText.getText ("YouCan", Util.lowerCaseFirst(toMenu()));

    }
}
