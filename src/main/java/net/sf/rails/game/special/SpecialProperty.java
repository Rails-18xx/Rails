package net.sf.rails.game.special;

import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.Company;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsOwnableItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.util.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

public abstract class SpecialProperty extends RailsOwnableItem<SpecialProperty> implements Configurable {

    protected static Logger log = LoggerFactory.getLogger(SpecialProperty.class);

    protected static final String STORAGE_NAME = "SpecialProperty";

    protected final BooleanState exercised = BooleanState.create(this, "exercised");
    protected Company originalCompany;
    
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
    // if exercising contributes to closing, if private has the closing conditions set, thus default is true
    // allows to exclude special properties that do not close privates that are closeable
    protected boolean closesPrivate = true;
    
    
    protected boolean isORProperty = false;
    protected boolean isSRProperty = false;
    
    /** Optional descriptive text, for display in menus and info text.
     * Subclasses may put real text in it.
     */
    protected String description = "";

    protected int uniqueId;

    protected SpecialProperty(RailsItem parent, String id) {
        super(parent, convertId(id) , SpecialProperty.class);
        uniqueId = Integer.valueOf(id);
        getRoot().getGameManager().storeObject(STORAGE_NAME, this);
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
        
        permanent = tag.getAttributeAsBoolean("permanent", permanent); 
        
        closesPrivate = tag.getAttributeAsBoolean("closesPrivate", closesPrivate);
        
    }
    
    public void finishConfiguration (RailsRoot root) throws ConfigurationException {
        // do nothing specific
    }

    public int getUniqueId() {
        return uniqueId;
    }

    // Sets the first (time) owner
    public void setOriginalCompany(Company company) {
        Preconditions.checkState(originalCompany == null, "OriginalCompany can only set once");
        originalCompany = company;
    }

    public Company getOriginalCompany() {
        return originalCompany;
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
        if (value && closesPrivate && originalCompany instanceof PrivateCompany) {
            ((PrivateCompany)originalCompany).checkClosingIfExercised(false);
        }
    }

    public boolean isExercised() {
        return exercised.value();
    }
    
    public abstract boolean isExecutionable(); 
    

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
    
 
    // TODO: Rails 2.0: Move this to a new SpecialPropertyManager
    
    // convert to the full id used 
    private static String convertId(String id) {
        return STORAGE_NAME + "_" + id;
    }
    
    // return new storage id
    private static String createUniqueId(RailsItem item) {
        return String.valueOf(item.getRoot().getGameManager().getStorageId(STORAGE_NAME) + 1);
        // increase unique id to allow loading old save files (which increase by 1)
        // TODO: remove that legacy issue
    }

    // return special property by unique id
    public static SpecialProperty getByUniqueId(RailsItem item, int id) {
        id -= 1;
        // decrease retrieval id to allow loading old save files (which increase by 1)
        // TODO: remove that legacy issue
        return (SpecialProperty)item.getRoot().getGameManager().retrieveObject(STORAGE_NAME, id);
    }

    /**
     * @param company the company that owns the SpecialProperties
     * @param tag with XML to create SpecialProperties
     * @return additional InfoText
     * @throws ConfigurationException
     */
    public static String configure(Company company, Tag tag) throws ConfigurationException {

      StringBuilder text = new StringBuilder();
        
      // Special properties
      Tag spsTag = tag.getChild("SpecialProperties");
      if (spsTag != null) {

          List<Tag> spTags = spsTag.getChildren("SpecialProperty");
          String className;
          for (Tag spTag : spTags) {
              className = spTag.getAttributeAsString("class");
              if (!Util.hasValue(className))
                  throw new ConfigurationException(
                  "Missing class in private special property");
              String uniqueId = SpecialProperty.createUniqueId(company);
              SpecialProperty sp = Configure.create(SpecialProperty.class, className, company, uniqueId);
              sp.setOriginalCompany(company);
              sp.configureFromXML(spTag);
              sp.moveTo(company);
              text.append("<br>" + sp.getInfo());
          }
      }
      return text.toString();
  }

    
}
