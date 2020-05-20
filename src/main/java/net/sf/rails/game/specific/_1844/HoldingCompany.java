/**
 * 
 */
package net.sf.rails.game.specific._1844;

import java.awt.Color;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.Company;
import net.sf.rails.game.CompanyType;
import net.sf.rails.game.Player;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.model.CountingMoneyModel;
import net.sf.rails.game.model.PortfolioModel;
import net.sf.rails.game.model.PortfolioOwner;
import net.sf.rails.game.model.PriceModel;
import net.sf.rails.game.model.RightsModel;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.special.SpecialRight;
import net.sf.rails.game.state.ArrayListState;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Observable;
import net.sf.rails.game.state.StringState;
import net.sf.rails.util.Util;

/**
 * @author Martin Brumm
 * 
 * This class provides an implementation of a Company that holds certificates/actors that act itself 
 * in lieu of a full fledged public company. The Company will not have a flexible share price its 
 * certificates sell for fixed baseprice. 
 * 
 *  In 1844 this Company will be used to hold the certificate for the Tunnel and the Mountain Company.  
 *  They can be bought from the Bank, but not sold back or sold to other players. The Companies in 1844 
 *  come with a special action that is linked to each certificate.
 *   
 */

public class HoldingCompany extends RailsAbstractItem implements Company, PortfolioOwner, Comparable<HoldingCompany>{

    
    protected static Logger log = LoggerFactory.getLogger(HoldingCompany.class);
    
    protected static final int DEFAULT_SHARE_UNIT = 10;

    protected static int numberOfHoldingCompanies = 0;
    
    /**
     * Foreground (i.e. text) colour of the company for the StatusWindow only
     */
    protected Color fgColour;

    /** Hexadecimal representation (RRGGBB) of the foreground colour. */
    protected String fgHexColour = "FFFFFF";

    /** Background colour of the company for the StatusWindow only */
    protected Color bgColour;

    /** Hexadecimal representation (RRGGBB) of the background colour. */
    protected String bgHexColour = "000000";

    /**
     * Initial (par) share price, represented by a stock market location object
     */
    protected PriceModel parPrice;

    /** Current share price, represented by a stock market location object */
    protected PriceModel currentPrice;

    /** Are company shares buyable (i.e. before started)? */
    protected final BooleanState buyable = new BooleanState(this, "buyable");

    protected final PortfolioModel portfolio = PortfolioModel.create(this);
    
    /** In-game state.
     * <p> Will only be set false if the company is closed and cannot ever be reopened.
     * By default it will be set false if a company is closed. */
    protected final BooleanState inGameState = new BooleanState(this, "inGameState", true);
    
    protected final StringState tilesLaidThisTurn = StringState.create(this, "tilesLaidThisTurn");

    protected final CountingMoneyModel tilesCostThisTurn = CountingMoneyModel.create(this, "tilesCostThisTurn", false); 

    protected final StringState tokensLaidThisTurn = StringState.create(this, "tokenLaidThisTurn");

    protected final CountingMoneyModel tokensCostThisTurn = CountingMoneyModel.create(this, "tokensCostThisTurn", false);

    protected final CountingMoneyModel trainsCostThisTurn = CountingMoneyModel.create(this, "trainsCostThisTurn", false);

    protected boolean canBuyStock = false;

    protected boolean canBuyPrivates = false;

    protected boolean canUseSpecialProperties = false;

    /** Multiple certificates those that represent more than one nominal share unit (except president share) */
    protected boolean hasMultipleCertificates = false;

    /** Can a company be restarted once it is closed? */
    protected boolean canBeRestarted = false;
    
    protected List<Tag> certificateTags = null;
    
    /** The certificates of this company (minimum 1) */
    protected final ArrayListState<HoldingCompanyCertificate> certificates = new ArrayListState(this, "ownCertificates");
    /** Are the certificates available from the first SR? */
    boolean certsAreInitiallyAvailable = true;

    /** What percentage of ownership constitutes "one share" */
    protected IntegerState shareUnit = IntegerState.create(this, "shareUnit", DEFAULT_SHARE_UNIT);
    
    /** What number of share units relates to the share price
     * (normally 1, but 2 for 1835 Prussian)
     */
    protected int shareUnitsForSharePrice = 1;
    
    /** Does the company have a stock price (minors often don't) */
    protected boolean hasStockPrice = true;

    /** Does the company have a par price? */
    protected boolean hasParPrice = true;
    
    /** Fixed price (for a 1835-style minor) */
    protected int fixedPrice = 0;
    
    protected RightsModel_1844 rightsModel = null; // init if required
    // created in finishConfiguration

    // used for Company interface
    private String longName;
    private String alias;
    private CompanyType type;
    private String infoText;
    private String parentInfoText;
    private final BooleanState closed = new BooleanState(this, "closed", false);
    
    protected BooleanState canSharePriceVary;

    
    /**
     * @param parent
     * @param id
     */
    public HoldingCompany(RailsItem parent, String id) {
        super(parent, id);
    }

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        longName = tag.getAttributeAsString("longname", getId());
        infoText = "<html>"+longName;

        alias = tag.getAttributeAsString("alias", alias);

        /* Configure public company features */
        fgHexColour = tag.getAttributeAsString("fgColour", fgHexColour);
        fgColour = Util.parseColour(fgHexColour);

        bgHexColour = tag.getAttributeAsString("bgColour", bgHexColour);
        bgColour = Util.parseColour(bgHexColour);

        fixedPrice = tag.getAttributeAsInteger("price", 0);

        certsAreInitiallyAvailable
        = tag.getAttributeAsBoolean("available", certsAreInitiallyAvailable);

        Tag shareUnitTag = tag.getChild("ShareUnit");
        if (shareUnitTag != null) {
            shareUnit.set(shareUnitTag.getAttributeAsInteger("percentage", DEFAULT_SHARE_UNIT));
            shareUnitsForSharePrice
            = shareUnitTag.getAttributeAsInteger("sharePriceUnits", shareUnitsForSharePrice);
        }

        // Extra info text(usually related to extra-share special properties)
        Tag infoTag = tag.getChild("Info");
        if (infoTag != null) {
            String infoKey = infoTag.getAttributeAsString("key");
            String[] infoParms = infoTag.getAttributeAsString("parm", "").split(",");
            infoText += "<br>"+LocalText.getText(infoKey, (Object[])infoParms);
        }

        // Special properties (as in the 1835 black minors)
        parentInfoText += SpecialProperty.configure(this, tag);
        
        List<Tag> certTags = tag.getChildren("Certificate");
        if (certTags != null) certificateTags = certTags;

        
/*        Tag sellSharesTag = tag.getChild("TradeShares");
        if (sellSharesTag != null) {
            mayTradeShares = true;
            mustHaveOperatedToTradeShares =
                sellSharesTag.getAttributeAsBoolean("mustHaveOperated",
                        mustHaveOperatedToTradeShares);
        }
        */

        
        
    }

    @Override
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        int certIndex = 0;
        if (certificateTags != null) {
            int shareTotal = 0;

           HoldingCompanyCertificate certificate;
            // Throw away
            // the per-type
            // specification
            
            // TODO: Move this to respective Certificate class, as it belongs there
            for (Tag certificateTag : certificateTags) {
                int shares = certificateTag.getAttributeAsInteger("shares", 1);

                int number = certificateTag.getAttributeAsInteger("number", 1);

                boolean certIsInitiallyAvailable
                = certificateTag.getAttributeAsBoolean("available",
                        certsAreInitiallyAvailable);

                float certificateCount = certificateTag.getAttributeAsFloat("certificateCount", 1.0f);
                
                for (int k = 0; k < number; k++) {
                    certificate = new HoldingCompanyCertificate(this, "cert_" + certIndex, shares, 
                            certIsInitiallyAvailable, certificateCount, certIndex++);
                    certificates.add(certificate);
                    shareTotal += shares * shareUnit.value();
                }
            }
            /*
            if (shareTotal != 100)
                throw new ConfigurationException("Company type " + getId()
                        + " total shares is not 100%");
                        */
        }
        
        nameCertificates();

        // Give each certificate an unique Id
        HoldingCompanyCertificate cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(getId(), i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable()
                    && this.certsAreInitiallyAvailable);
        }


        infoText += parentInfoText;
        parentInfoText = "";

        // Can companies acquire special rightsModel (such as in 1830 Coalfields)?
        // TODO: Can this be simplified?
        if (portfolio.hasSpecialProperties()) {
            for (SpecialProperty sp : portfolio.getPersistentSpecialProperties()) {
                if (sp instanceof SpecialRight) {
                    getRoot().getGameManager().setGuiParameter (GuiDef.Parm.HAS_ANY_RIGHTS, true);
                    // Initialize rightsModel here to prevent overhead if not used,
                    // but if rightsModel are used, the GUI needs it from the start.
                    if (rightsModel == null) {
                        rightsModel = RightsModel_1844.create(this, "rightsModel");
                    }
                    // TODO: This is only a workaround for the missing finishConfiguration of special properties (SFY)
                    sp.finishConfiguration(root);
                }
            }
        }
        
        // finish Configuration of portfolio
        portfolio.finishConfiguration();
        
        // set multipleCertificates
        for (HoldingCompanyCertificate c:certificates) {
            if (c.getShares() != 1) {
                hasMultipleCertificates = true;
            }
        }
        
    }
    
    /**
     * Get a list of this company's certificates.
     *
     * @return ArrayList containing the certificates (item 0 is the President's
     * share).
     */
    public List<HoldingCompanyCertificate> getCertificates() {
        return certificates.view();
    }

    /**
     * Backlink the certificates to this company,
     * and give each one a type getId().
     *
     */
    public void nameCertificates () {
        for (HoldingCompanyCertificate cert : certificates.view()) {
            cert.setCompany(this);
        }
    }

    @Override
    public void initType(CompanyType type) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public CompanyType getType() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isClosed() {
        // Cant be closed.
        return false;
    }

    @Override
    public String getLongName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAlias() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getInfoText() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ImmutableSet<SpecialProperty> getSpecialProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int compareTo(HoldingCompany o) {
            return this.getId().compareTo(o.getId());
    }

    @Override
    public PortfolioModel getPortfolioModel() {
        // TODO Auto-generated method stub
        return null;
    }


    
    /**
     * Return the company token background colour.
     *
     * @return Color object
     */
    public Color getBgColour() {
        return bgColour;
    }

    /**
     * Return the company token background colour.
     *
     * @return Hexadecimal string RRGGBB.
     */
    public String getHexBgColour() {
        return bgHexColour;
    }

    /**
     * Return the company token foreground colour.
     *
     * @return Color object.
     */
    public Color getFgColour() {
        return fgColour;
    }

    /**
     * Return the company token foreground colour.
     *
     * @return Hexadecimal string RRGGBB.
     */
    public String getHexFgColour() {
        return fgHexColour;
    }
    
    /** Make company shares buyable. Only useful where shares become
     * buyable before the company has started (e.g. 1835 Prussian).
     * */
    public void setBuyable(boolean buyable) {
        this.buyable.set(buyable);
    }

    public boolean isBuyable() {
        return buyable.value();
    }

    /** Return the price per share at game end.
     *  Depends on the activation of the specialproperties of the single shares/certificates
     * @return
     */
    public int getGameEndPrice() {
        return 0;
    }

    public int getIPOPrice () {
            return fixedPrice;
    }
    
    public int getMarketPrice () {
         return fixedPrice;
        }

    public PriceModel getCurrentPriceModel() {
        return currentPrice;
    }

    public PriceModel getParPriceModel() {
        // Temporary fix to satisfy GameStatus window. Should be removed there.
        return currentPrice;
    }
    
    public boolean canUseSpecialProperties() {
        return canUseSpecialProperties;
    }
    
    /**
     * Get the unit of share.
     *
     * @return The percentage of ownership that is called "one share".
     */
    public int getShareUnit() {
        return shareUnit.value();
    }

    public int getShareUnitsForSharePrice() {
        return shareUnitsForSharePrice;
    }
    
    @Override
    public String toString() {
        return getId();
    }

    public boolean hasStockPrice() {
        return hasStockPrice;
    }

    public boolean hasParPrice() {
        return hasParPrice;
    }

    public boolean canSharePriceVary() {
        return canSharePriceVary.value();
    }

    public int getFixedPrice() {
        return fixedPrice;
    }
    
    public int sharesOwnedByPlayers() {
        int shares = 0;
        for (HoldingCompanyCertificate cert : certificates.view()) {
            if (cert.getOwner() instanceof Player) {
                shares += cert.getShares();
            }
        }
        return shares;
    }
    
    public Observable getRightsModel () {
        return rightsModel;
    }
    public void setRight (SpecialRight right) {
        if (rightsModel == null) {
            rightsModel = RightsModel_1844.create(this, "RightsModel");
        }
        rightsModel.add(right);
    }

    public boolean hasRight(SpecialRight right) {
        return rightsModel.contains(right);
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

    @Override
    public void setClosed() {
        // Cant be closed...
        
    }

    public void setIndex(int i) {
        // TODO Auto-generated method stub
        
    }

}
