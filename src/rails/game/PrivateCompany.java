package rails.game;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import rails.common.LocalText;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialProperty;
import rails.game.state.BooleanState;
import rails.game.state.OwnableItem;
import rails.game.state.PortfolioSet;
import rails.util.*;

// FIXME: Move static field numberOfPrivateCompanies to CompanyManager

public class PrivateCompany extends OwnableItem<PrivateCompany> implements Company, Certificate, Closeable {

    private static Logger log = LoggerFactory.getLogger(PrivateCompany.class);

    public static final String TYPE_TAG = "Private";
    public static final String REVENUE = "revenue";
    //used by getUpperPrice and getLowerPrice to signal no limit
    public static final int NO_PRICE_LIMIT = -1;    

    
    protected static int numberOfPrivateCompanies = 0;
    protected int privateNumber; // For internal use

    protected int basePrice = 0;
    // list of revenue sfy 1889
    protected int[] revenue;
    protected String auctionType;

    // Closing conditions
    protected int closingPhase;
    // Closing when special properties are used
    protected boolean closeIfAllExercised = false;
    protected boolean closeIfAnyExercised = false;
    protected boolean closeAtEndOfTurn = false; // E.g. 1856 W&SR
    // Prevent closing conditions sfy 1889
    protected List<String> preventClosingConditions = new ArrayList<String>();
    // Close at start of phase
    protected String closeAtPhaseName = null;
    // Manual close possible
    protected boolean closeManually = false;

    protected String blockedHexesString = null;
    protected List<MapHex> blockedHexes = null;
  
    // Maximum and minimum prices the private can be sold in for.
    protected int upperPrice = NO_PRICE_LIMIT;
    protected int lowerPrice = NO_PRICE_LIMIT;
    
    // Maximum and minimum price factor (used to set upperPrice and lowerPrice)
    protected float lowerPriceFactor = NO_PRICE_LIMIT;
    protected float upperPriceFactor = NO_PRICE_LIMIT;

    // Maximum and minimum prices the private can be sold to a player for.
    protected int upperPlayerPrice = NO_PRICE_LIMIT;
    protected int lowerPlayerPrice = NO_PRICE_LIMIT;
    
    // Maximum and minimum price factor when selling to another player
    protected float lowerPlayerPriceFactor = NO_PRICE_LIMIT;
    protected float upperPlayerPriceFactor = NO_PRICE_LIMIT;
    
    // Can the private be bought by companies / players (when held by a player)
    protected boolean tradeableToCompany = true;
    protected boolean tradeableToPlayer = false;
    
    private final PortfolioSet<SpecialProperty> specialProperties = 
            PortfolioSet.create(this, "specialProperties", SpecialProperty.class);

    // used for Company interface
    private String longName;
    private String alias;
    private CompanyType type;
    private String infoText;
    private String parentInfoText;
    private final BooleanState closed = BooleanState.create(this, "closed", false);
    
    /**
     * Used by Configure (via reflection) only
     */
    public PrivateCompany(RailsItem parent, String id) {
        super(parent, id, PrivateCompany.class);
        this.privateNumber = numberOfPrivateCompanies++;
    }

    /**
     * @see rails.common.parser.Configurable#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Configure private company features */
        try {

            longName= tag.getAttributeAsString("longname", getId());
            infoText = "<html>"+longName;
            basePrice = tag.getAttributeAsInteger("basePrice", 0);

            // sfy 1889 changed to IntegerArray
            revenue = tag.getAttributeAsIntegerArray("revenue", new int[0]);

            // pld: adding revenue to info text
            infoText += "<br>Revenue: ";
            for (int i = 0; i < revenue.length;i++) {
                infoText += (Currency.format(this, revenue[i]));
                if (i < revenue.length-1) {infoText += ", ";};
            }

            // Blocked hexes (until bought by a company)
            Tag blockedTag = tag.getChild("Blocking");
            if (blockedTag != null) {
                blockedHexesString =
                    blockedTag.getAttributeAsString("hex");
                infoText += "<br>Blocking: "+blockedHexesString;
            }

            // Extra info text(usually related to extra-share special properties)
            Tag infoTag = tag.getChild("Info");
            if (infoTag != null) {
                String infoKey = infoTag.getAttributeAsString("key");
                String[] infoParms = infoTag.getAttributeAsString("parm", "").split(",");
                infoText += "<br>"+LocalText.getText(infoKey, (Object[])infoParms);
            }


            // SpecialProperties
            parentInfoText += SpecialProperty.configure(this, tag);

            // Closing conditions
            // Currently only used to handle closure following laying
            // tiles and/or tokens because of special properties.
            // Other cases are currently handled elsewhere.
            Tag closureTag = tag.getChild("ClosingConditions");

            if (closureTag != null) {

                Tag spTag = closureTag.getChild("SpecialProperties");

                if (spTag != null) {

                    String ifAttribute = spTag.getAttributeAsString("condition");
                    if (ifAttribute != null) {
                        closeIfAllExercised = ifAttribute.equalsIgnoreCase("ifExercised")
                        || ifAttribute.equalsIgnoreCase("ifAllExercised");
                        closeIfAnyExercised = ifAttribute.equalsIgnoreCase("ifAnyExercised");
                    }
                    String whenAttribute = spTag.getAttributeAsString("when");
                    if (whenAttribute != null) {
                        closeAtEndOfTurn = whenAttribute.equalsIgnoreCase("endOfORTurn");
                    }
                }

                /* conditions that prevent closing */
                List<Tag> preventTags = closureTag.getChildren("PreventClosing");
                if (preventTags != null) {
                    for (Tag preventTag: preventTags) {
                        String conditionText = preventTag.getAttributeAsString("condition");
                        if (conditionText != null) {
                            preventClosingConditions.add(conditionText);
                        }
                    }
                }

                /* allow manual closure */
                Tag manualTag = closureTag.getChild("CloseManually");
                if (manualTag != null) {
                    closeManually = true;
                }

                // Close at start of phase
                Tag closeTag = closureTag.getChild("Phase");
                if (closeTag != null) {
                    closeAtPhaseName = closeTag.getText();
                }           
            }
            
            // start: br
            // Reads the Tradeable tags
            List<Tag> tradeableTags = tag.getChildren("Tradeable");
            if (tradeableTags != null) {
                for (Tag tradeableTag : tradeableTags) {                        

                    if (tradeableTag.hasAttribute("toCompany")) {
                        tradeableToCompany = tradeableTag.getAttributeAsBoolean("toCompany");
                    
                        if (tradeableToCompany) {
                            upperPrice =
                                tradeableTag.getAttributeAsInteger("upperPrice", upperPrice);                            
                            lowerPrice =
                                tradeableTag.getAttributeAsInteger("lowerPrice", lowerPrice);                            
                            lowerPriceFactor =
                                tradeableTag.getAttributeAsFloat("lowerPriceFactor", lowerPriceFactor);
                            upperPriceFactor =
                                tradeableTag.getAttributeAsFloat("upperPriceFactor", upperPriceFactor);                        
                        }
                    }

                    if (tradeableTag.hasAttribute("toPlayer")) {
                        tradeableToPlayer = tradeableTag.getAttributeAsBoolean("toPlayer");
                    
                        if (tradeableToPlayer) {
                            upperPlayerPrice =
                                tradeableTag.getAttributeAsInteger("upperPrice", upperPlayerPrice);                            
                            lowerPlayerPrice =
                                tradeableTag.getAttributeAsInteger("lowerPrice", lowerPlayerPrice);                            
                            lowerPlayerPriceFactor =
                                tradeableTag.getAttributeAsFloat("lowerPriceFactor", lowerPlayerPriceFactor);
                            upperPlayerPriceFactor =
                                tradeableTag.getAttributeAsFloat("upperPriceFactor", upperPlayerPriceFactor);
                        }
                    }                    
                }
            }
            //end: br

        } catch (Exception e) {
            throw new ConfigurationException("Configuration error for Private "
                    + getId(), e);
        }

    }

    public void finishConfiguration (GameManager gameManager)
    throws ConfigurationException {

        for (SpecialProperty sp : specialProperties) {
            sp.finishConfiguration(gameManager);
        }

        if (Util.hasValue(blockedHexesString)) {
            MapManager mapManager = gameManager.getMapManager();
            blockedHexes = new ArrayList<MapHex>();
            for (String hexName : blockedHexesString.split(",")) {
                MapHex hex = mapManager.getHex(hexName);
                blockedHexes.add(hex);
                hex.setBlockedForTileLays(true);
            }
        }

        infoText += parentInfoText;
        parentInfoText = "";

        if (Util.hasValue(closeAtPhaseName)) {
            Phase closingPhase = gameManager.getPhaseManager().getPhaseByName(closeAtPhaseName);
            if (closingPhase != null) {
                closingPhase.addObjectToClose(this);
            }
        }

        // start: br
        //if {upper,lower}PriceFactor is set but {upper,lower}Price is not, calculate the right value
        if (upperPrice == NO_PRICE_LIMIT && upperPriceFactor != NO_PRICE_LIMIT) { 
            
            if (basePrice==0) {
                throw new ConfigurationException("Configuration error for Private "
                        + getId() + ": upperPriceFactor needs basePrice to be set");
            }
                             
            upperPrice = (int)(basePrice * upperPriceFactor + 0.5f);
        }
        if (lowerPrice == NO_PRICE_LIMIT && lowerPriceFactor != NO_PRICE_LIMIT) { 

            if (basePrice==0) {
                throw new ConfigurationException("Configuration error for Private "
                        + getId() + ": lowerPriceFactor needs basePrice to be set");
            }

            lowerPrice = (int)(basePrice * lowerPriceFactor + 0.5f);
        }
        // end: br
    }


    /**
     * @return Private Company Number
     */
    public int getPrivateNumber() {
        return privateNumber;
    }

    /**
     * @return Base Price
     */
    public int getBasePrice() {
        return basePrice;
    }

    /**
     * @return Revenue
     */
    public int[] getRevenue() {
        return revenue;
    }

    //  start: sfy 1889: new method
    public int getRevenueByPhase(Phase phase){
        if (phase != null) {
            return revenue[Math.min(
                    revenue.length,
                    phase.getPrivatesRevenueStep()) - 1];
        } else {
            return 0;
        }
    }
    // end: sfy 1889

    /**
     * @return Phase this Private closes
     */
    public int getClosingPhase() {
        return closingPhase;
    }

    public void setClosed() {

        if (isClosed()) return;
        //        if (!isCloseable()) return;  /* moved hat to call in closeAllPrivates, to allow other closing actions */
        
        closed.set(true);

        unblockHexes();

        moveTo(GameManager.getInstance().getBank().getScrapHeap());
        
        ReportBuffer.add(LocalText.getText("PrivateCloses", getId()));

        // For 1856: buyable tokens still owned by the private will now
        // become commonly buyable, i.e. owned by GameManager.
        // (Note: all such tokens will be made buyable from the Bank too,
        // this is done in OperatingRound_1856).
        List<SellBonusToken> moveToGM = new ArrayList<SellBonusToken>(4);
        for (SpecialProperty sp : specialProperties) {
            if (sp instanceof SellBonusToken) {
                moveToGM.add((SellBonusToken)sp);
            }
        }
        for (SellBonusToken sbt : moveToGM) {
            GameManager.getInstance().getCommonSpecialPropertiesPortfolio().moveInto(sbt);
            log.debug("SP "+sbt.getId()+" is now a common property");
        }
    }

    /* start sfy 1889 */
    public boolean isCloseable() {

        if ((preventClosingConditions == null) || preventClosingConditions.isEmpty()) return true;

        if (preventClosingConditions.contains("doesNotClose")) {
            log.debug("Private Company "+getId()+" does not close (unconditional).");
            return false;
        }
        if (preventClosingConditions.contains("ifOwnedByPlayer")
                && getOwner() instanceof Player) {
            log.debug("Private Company "+getId()+" does not close, as it is owned by a player.");
            return false;
        }
        return true;
    }

    public List<String> getPreventClosingConditions() {
        return preventClosingConditions;
    }
    /* end sfy 1889 */

    /**
     * @param i
     */
    public void setClosingPhase(int i) {
        closingPhase = i;
    }

    /**
     * FIXME: We have a side effect as soon as private company moves to a public company, this has to be considered somewhere else
     */
//    public void setHolder(PortfolioModel portfolio) {
//        this.portfolio = portfolio;
//
//        /*
//         * If this private is blocking map hexes, unblock these hexes as soon as
//         * it is bought by a company.
//         */
//        if (portfolio.getOwner() instanceof Company) {
//            unblockHexes();
//        }
//    }

    protected void unblockHexes() {
        if (blockedHexes != null) {
            for (MapHex hex : blockedHexes) {
                hex.setBlockedForTileLays(false);
            }
        }
    }

    @Override
    public String toString() {
        return "Private: " + getId();
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.error("Cannot clone company " + getId());
            return null;
        }

        return clone;
    }

    public List<MapHex> getBlockedHexes() {
        return blockedHexes;
    }

    public boolean closesIfAllExercised() {
        return closeIfAllExercised;
    }

    public boolean closesIfAnyExercised() {
        return closeIfAnyExercised;
    }

    public boolean closesAtEndOfTurn() {
        return closeAtEndOfTurn;
    }

    public boolean closesManually() {
        return closeManually;
    }

    public void checkClosingIfExercised (boolean endOfTurn) {

        if (isClosed() || endOfTurn != closeAtEndOfTurn) return;

        if (closeIfAllExercised) {
            for (SpecialProperty sp : specialProperties) {
                if (!sp.isExercised()) return;
            }
            log.debug("CloseIfAll: closing "+getId());
            setClosed();

        } else if (closeIfAnyExercised) {
            for (SpecialProperty sp : specialProperties) {
                if (sp.isExercised()) {
                    log.debug("CloseIfAny: closing "+getId());
                    setClosed();
                    return;
                }
            }
        }
    }

    public String getClosingInfo() {
        return null;
    }

    public void close() {
        setClosed();
    }
    
    /**
     * @return Returns the upperPrice that the company can be sold in for.
     */
    public int getUpperPrice() {    
        return getUpperPrice(false);
    }
    
    public int getUpperPrice(boolean saleToPlayer) {
        if (saleToPlayer) {
            return upperPlayerPrice;
        }
        
        return upperPrice;
    }   
    
    /**
     * @return Returns the lowerPrice that the company can be sold in for.
     */    
    public int getLowerPrice() {
        return getLowerPrice(false);       
    }
    
    public int getLowerPrice(boolean saleToPlayer) {
        if (saleToPlayer) {       
            return lowerPlayerPrice;
        }
            
        return lowerPrice;
    }
    
    /**
     * @return Returns whether or not the company can be bought by a company
     */    
    public boolean tradeableToCompany() {
        return tradeableToCompany;
    }
    
    /**
     * @return Returns whether or not the company can be bought by a player (from another player)
     */
    public boolean tradeableToPlayer() {
        return tradeableToPlayer;
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return !specialProperties.isEmpty();
    }

    // Company methods
    public void initType(CompanyType type) {
        this.type = type;
    }

    public CompanyType getType() {
        return type;
    }

    public boolean isClosed() {
        return closed.value();
    }

    public String getLongName() {
        return longName;
    }

    public String getAlias() {
        return alias;
    }

    public String getInfoText() {
        return infoText;
    }

    public ImmutableSet<SpecialProperty> getSpecialProperties() {
        return specialProperties.items();
    }
    
    // RailsItem methods
    @Override
    public RailsItem getParent() {
        return (RailsItem)super.getParent();
    }
    
    @Override
    public RailsRoot getRoot() {
        return (RailsRoot)super.getRoot();
    }

    // Certficate method
    public String getName() {
        return getId();
    }
    
}
