/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompany.java,v 1.42 2010/05/01 16:08:13 stefanfrey Exp $ */
package rails.game;

import java.util.ArrayList;
import java.util.List;

import rails.game.move.*;
import rails.game.special.SellBonusToken;
import rails.game.special.SpecialPropertyI;
import rails.util.*;

public class PrivateCompany extends Company implements PrivateCompanyI {

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
    protected List<String> preventClosingConditions = null;
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
        
    public PrivateCompany() {
        super();
        this.privateNumber = numberOfPrivateCompanies++;
    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Configure private company features */
        try {

            longName= tag.getAttributeAsString("longname", name);
            infoText = "<html>"+longName;
            basePrice = tag.getAttributeAsInteger("basePrice", 0);

            // sfy 1889 changed to IntegerArray
            revenue = tag.getAttributeAsIntegerArray("revenue", new int[0]);

            // pld: adding revenue to info text
            infoText += "<br>Revenue: ";
            for (int i = 0; i < revenue.length;i++) {
                infoText += (Bank.format(revenue[i]));
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


            // For special properties (now included in Company).
            super.configureFromXML(tag);

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
                    + name, e);
        }

    }

    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {

        for (SpecialPropertyI sp : specialProperties) {
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
            PhaseI closingPhase = gameManager.getPhaseManager().getPhaseByName(closeAtPhaseName);
            if (closingPhase != null) {
                closingPhase.addObjectToClose(this);
            }
        }

        // start: br
        //if {upper,lower}PriceFactor is set but {upper,lower}Price is not, calculate the right value
        if (upperPrice == NO_PRICE_LIMIT && upperPriceFactor != NO_PRICE_LIMIT) { 
            
            if (basePrice==0) {
                throw new ConfigurationException("Configuration error for Private "
                        + name + ": upperPriceFactor needs basePrice to be set");
            }
                             
            upperPrice = (int)(basePrice * upperPriceFactor + 0.5f);
        }
        if (lowerPrice == NO_PRICE_LIMIT && lowerPriceFactor != NO_PRICE_LIMIT) { 

            if (basePrice==0) {
                throw new ConfigurationException("Configuration error for Private "
                        + name + ": lowerPriceFactor needs basePrice to be set");
            }

            lowerPrice = (int)(basePrice * lowerPriceFactor + 0.5f);
        }
        // end: br
    }

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);

        specialProperties = new ArrayList<SpecialPropertyI>();

        /* start sfy 1889 */
        preventClosingConditions = new ArrayList<String>();
    }

    public void moveTo(MoveableHolder newHolder) {
        new ObjectMove(this, portfolio, newHolder);
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
    public int getRevenueByPhase(PhaseI phase){
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

    /**
     * @return Portfolio (holder) of this Private
     */
    @Override
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * @param b
     */
    @Override
    public void setClosed() {

        if (isClosed()) return;
        //        if (!isCloseable()) return;  /* moved hat to call in closeAllPrivates, to allow other closing actions */

        super.setClosed();
        unblockHexes();
        moveTo(GameManager.getInstance().getBank().getScrapHeap());
        ReportBuffer.add(LocalText.getText("PrivateCloses", name));

        // For 1856: buyable tokens still owned by the private will now
        // become commonly buyable, i.e. owned by GameManager.
        // (Note: all such tokens will be made buyable from the Bank too,
        // this is done in OperatingRound_1856).
        List<SellBonusToken> moveToGM = new ArrayList<SellBonusToken>(4);
        for (SpecialPropertyI sp : specialProperties) {
            if (sp instanceof SellBonusToken) {
                moveToGM.add((SellBonusToken)sp);
            }
        }
        for (SellBonusToken sbt : moveToGM) {
            sbt.moveTo(GameManager.getInstance());
            log.debug("SP "+sbt.getName()+" is now a common property");
        }
    }

    /* start sfy 1889 */
    public boolean isCloseable() {

        if ((preventClosingConditions == null) || preventClosingConditions.isEmpty()) return true;

        if (preventClosingConditions.contains("doesNotClose")) {
            log.debug("Private Company "+getName()+" does not close (unconditional).");
            return false;
        }
        if (preventClosingConditions.contains("ifOwnedByPlayer")
                && portfolio.getOwner() instanceof Player) {
            log.debug("Private Company "+getName()+" does not close, as it is owned by a player.");
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
     * @param portfolio
     */
    public void setHolder(Portfolio portfolio) {
        this.portfolio = portfolio;

        /*
         * If this private is blocking map hexes, unblock these hexes as soon as
         * it is bought by a company.
         */
        if (portfolio.getOwner() instanceof CompanyI) {
            unblockHexes();
        }
    }

    protected void unblockHexes() {
        if (blockedHexes != null) {
            for (MapHex hex : blockedHexes) {
                hex.setBlockedForTileLays(false);
            }
        }
    }

    @Override
    public String toString() {
        return "Private: " + name;
    }

    @Override
    public Object clone() {

        Object clone = null;
        try {
            clone = super.clone();
        } catch (CloneNotSupportedException e) {
            log.fatal("Cannot clone company " + name);
            return null;
        }

        return clone;
    }

    /**
     * Stub to satisfy MoveableHolderI. Special properties are never added after
     * completing the initial setup.
     */
    public boolean addObject(Moveable object, int[] position) {
        if (object instanceof SpecialPropertyI) {
            return Util.addToList(specialProperties, (SpecialPropertyI)object,
                    position == null ? -1 : position[0]);
        } else {
            return false;
        }
    }

    /**
     * Remove a special property. Only used to transfer a persistent special
     * property to a Portfolio, where it becomes independent of the private.
     *
     * @param token The special property object to remove.
     * @return True if successful.
     */
    public boolean removeObject(Moveable object) {
        if (object instanceof SpecialPropertyI) {
            return specialProperties.remove(object);
        } else {
            return false;
        }
    }

    public int[] getListIndex (Moveable object) {
        if (object instanceof SpecialPropertyI) {
            return new int[] {specialProperties.indexOf(object)};
        } else {
            return Moveable.AT_END;
        }
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
            for (SpecialPropertyI sp : specialProperties) {
                if (!sp.isExercised()) return;
            }
            log.debug("CloseIfAll: closing "+name);
            setClosed();

        } else if (closeIfAnyExercised) {
            for (SpecialPropertyI sp : specialProperties) {
                if (sp.isExercised()) {
                    log.debug("CloseIfAny: closing "+name);
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
    
    public int getUpperPrice() {    
        return getUpperPrice(false);
    }
    
    public int getUpperPrice(boolean saleToPlayer) {
        if (saleToPlayer) {
            return upperPlayerPrice;
        }
        
        return upperPrice;
    }   
    
    public int getLowerPrice() {
        return getLowerPrice(false);       
    }
    
    public int getLowerPrice(boolean saleToPlayer) {
        if (saleToPlayer) {       
            return lowerPlayerPrice;
        }
            
        return lowerPrice;
    }
    
    public boolean tradeableToCompany() {
        return tradeableToCompany;
    }
    
    public boolean tradeableToPlayer() {
        return tradeableToPlayer;
    }
}
