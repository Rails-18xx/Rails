/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/PrivateCompany.java,v 1.31 2010/01/31 22:22:28 macfreek Exp $ */
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
    protected int revenue = 0;
    protected List<SpecialPropertyI> specialProperties = null;
    protected String auctionType;

    // Closing conditions
    protected int closingPhase;
    // Closing when special properties are used
    protected boolean closeIfAllExercised = false;
    protected boolean closeIfAnyExercised = false;
    protected boolean closeAtEndOfTurn = false; // E.g. 1856 W&SR

    protected String blockedHexesString = null;
    protected List<MapHex> blockedHexes = null;

    public PrivateCompany() {
        super();
        this.privateNumber = numberOfPrivateCompanies++;
    }

    /**
     * @see rails.game.ConfigurableComponentI#configureFromXML(org.w3c.dom.Element)
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        /* Configure private company features */
        try {
            longName= tag.getAttributeAsString("longName", "");
            basePrice = tag.getAttributeAsInteger("basePrice", 0);
            revenue = tag.getAttributeAsInteger("revenue", 0);

            // Blocked hexes (until bought by a company)
            Tag blockedTag = tag.getChild("Blocking");
            if (blockedTag != null) {
                blockedHexesString =
                        blockedTag.getAttributeAsString("hex");
            }

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

                    SpecialPropertyI sp =
                            (SpecialPropertyI) Class.forName(className).newInstance();
                    sp.setCompany(this);
                    specialProperties.add(sp);
                    sp.configureFromXML(spTag);
                }
            }
            
            // Extra info text(usually related to extra-share special properties)
            Tag infoTag = tag.getChild("Info");
            if (infoTag != null) {
                String infoKey = infoTag.getAttributeAsString("key");
                String[] infoParms = infoTag.getAttributeAsString("parm", "").split(",");
                infoText = LocalText.getText(infoKey, (Object[])infoParms);
            }

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
                        closeAtEndOfTurn = whenAttribute.equalsIgnoreCase("atEndOfORTurn");
                    }
                }
            }


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
                hex.setBlocked(true);
            }
        }
}

    /** Initialisation, to be called directly after instantiation (cloning) */
    @Override
    public void init(String name, CompanyTypeI type) {
        super.init(name, type);

        specialProperties = new ArrayList<SpecialPropertyI>();

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
    public int getRevenue() {
        return revenue;
    }

    /**
     * @return Phase this Private closes
     */
    public int getClosingPhase() {
        return closingPhase;
    }

    /**
     * @return Portfolio of this Private
     */
    public Portfolio getPortfolio() {
        return portfolio;
    }

    /**
     * @param b
     */
    @Override
    public void setClosed() {

        if (isClosed()) return;

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
                hex.setBlocked(false);
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
    public boolean addObject(Moveable object) {
        return false;
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

    /**
     * @return ArrayList of all special properties we have.
     */
    public List<SpecialPropertyI> getSpecialProperties() {
        return specialProperties;
    }

    /**
     * Do we have any special properties?
     *
     * @return Boolean
     */
    public boolean hasSpecialProperties() {
        return specialProperties != null && !specialProperties.isEmpty();
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

    public void checkClosingIfExercised (boolean endOfOR) {

        if (isClosed() || endOfOR != closeAtEndOfTurn) return;

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


}
