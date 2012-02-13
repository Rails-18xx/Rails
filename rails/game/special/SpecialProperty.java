package rails.game.special;

import org.apache.log4j.Logger;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.game.model.Owner;
import rails.game.state.AbstractItem;
import rails.game.state.BooleanState;
import rails.game.model.Owners;
import rails.util.*;

// TODO: Check if we could extend AbstractOwnable
public abstract class SpecialProperty extends AbstractItem implements SpecialPropertyI {

    protected Company originalCompany;
    protected Owner owner = null;
    protected int closingValue = 0;
    protected BooleanState exercised;
    
    /* Usability conditions. Not all of these are already being used. */
    protected boolean usableIfOwnedByPlayer = false;
    protected boolean usableIfOwnedByCompany = false;
    protected boolean usableDuringSR = false;
    protected boolean usableDuringOR = false;
    protected boolean usableDuringTileLayingStep = false;
    protected boolean usableDuringTokenLayingStep = false;

    protected String conditionText = "";
    protected String whenText = "";
    protected String transferText = "";
    protected boolean permanent = false;
    protected boolean isORProperty = false;
    protected boolean isSRProperty = false;
    
    /** Priority indicates whether or not the UI should assign priority to
     * the execution of a PossibleAction. For instance, if the same tile can
     * be laid on a hex using this special property, and by not using it, 
     * this attribute indicates which option will be used.
     * TODO A third value means: ask the user (NOT YET IMPLEMENTED).
     */
    protected Priority priority = DEFAULT_PRIORITY;
    
    /** Optional descriptive text, for display in menus and info text.
     * Subclasses may put real text in it.
     */
    protected String description = "";

    protected int uniqueId;
    
    protected static final String STORAGE_NAME = "SpecialProperty";

    /** To give subclasses access to the various 'managers' */
    protected GameManager gameManager;

    protected static Logger log =
        Logger.getLogger(SpecialProperty.class.getPackage().getName());

    public SpecialProperty() {
        gameManager = GameManager.getInstance();
        uniqueId = gameManager.storeObject(STORAGE_NAME, this) + 1;
        // increase unique id to allow loading old save files (which increase by 1)
        // TODO: remove that legacy issue
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
        setUsableDuringSR(whenText.equalsIgnoreCase("anyTurn") 
                || whenText.equalsIgnoreCase("srTurn"));
        setUsableDuringOR(whenText.equalsIgnoreCase("anyTurn") 
                || whenText.equalsIgnoreCase("orTurn"));
        setUsableDuringTileLayingStep(whenText.equalsIgnoreCase("tileLayingStep"));
        setUsableDuringTokenLayingStep(whenText.equalsIgnoreCase("tokenLayingStep"));

        transferText = tag.getAttributeAsString("transfer", "");
        
        // sfy 1889
        permanent = tag.getAttributeAsBoolean("permanent", false);  
        
        String priorityString = tag.getAttributeAsString("priority");
        if (Util.hasValue(priorityString)) {
            try {
                priority = Priority.valueOf(priorityString.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException ("Illegal value for SpecialProperty priority: "+priorityString, e);
            }
        }
        
    }

    public void finishConfiguration (GameManager gameManager)
    throws ConfigurationException {

    }

    public int getUniqueId() {
        return uniqueId;
    }

    public static SpecialPropertyI getByUniqueId(int id) {
        id -= 1;
        // decrease retrieval id to allow loading old save files (which increase by 1)
        // TODO: remove that legacy issue
        return (SpecialPropertyI)GameManager.getInstance().retrieveObject(STORAGE_NAME, id);
    }

    public void setCompany(Company company) {
        originalCompany = company;
        owner = company;
        exercised =
                BooleanState.create(this, company.getId() + "_SP_" + uniqueId
                                 + "_Exercised", false);
    }

    public Company getOriginalCompany() {
        return originalCompany;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Owner getOwner() {
        return owner;
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
    

    public boolean isUsableDuringOR(GameDef.OrStep step) {
        
        if (usableDuringOR) return true;
        
        switch (step) {
        case LAY_TRACK:
            return usableDuringTileLayingStep;
        case LAY_TOKEN:
            return usableDuringTokenLayingStep;
        default:
            return false;
        }
    }

    public void setUsableDuringOR(boolean usableDuringOR) {
        this.usableDuringOR = usableDuringOR;
    }

    public boolean isUsableDuringSR() {
        return usableDuringSR;
    }

    public void setUsableDuringSR(boolean usableDuringSR) {
        this.usableDuringSR = usableDuringSR;
    }

    public boolean isUsableDuringTileLayingStep() {
        return usableDuringTileLayingStep;
    }

    public void setUsableDuringTileLayingStep(boolean usableDuringTileLayingStep) {
        this.usableDuringTileLayingStep = usableDuringTileLayingStep;
    }

    public boolean isUsableDuringTokenLayingStep() {
        return usableDuringTokenLayingStep;
    }

    public void setUsableDuringTokenLayingStep(boolean usableDuringTokenLayingStep) {
        this.usableDuringTokenLayingStep = usableDuringTokenLayingStep;
    }

    public void setExercised() {
        setExercised(true);
    }

    public void setExercised (boolean value) {
        if (permanent) return; // sfy 1889 
        exercised.set(value);
        if (value && originalCompany instanceof PrivateCompany) {
            ((PrivateCompany)originalCompany).checkClosingIfExercised(false);
        }
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

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * Move the special property to another holder.
     * Only to be used for special properties that have the "transfer" attribute.
     */
    public void moveTo(Owner newOwner) {
        if (transferText.equals("")) return;
        Owners.move(this, newOwner);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " of private "
               + originalCompany.getId();
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
