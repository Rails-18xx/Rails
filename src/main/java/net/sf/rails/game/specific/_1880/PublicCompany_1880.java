/**
 * 
 */
package net.sf.rails.game.specific._1880;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;

import net.sf.rails.algorithms.RevenueAdapter;
import net.sf.rails.algorithms.RevenueStaticModifier;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.MapHex;
import net.sf.rails.game.Phase;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.financial.StockSpace;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Owner;
import net.sf.rails.common.GuiDef;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;


public class PublicCompany_1880 extends PublicCompany implements RevenueStaticModifier {


    /** 
     *  Buildingrights belong to Phases in 1880 the player will be asked to choose which combination
     *   he wants and subsequently his company will be granted the appropriate rightsModel. Further the value
     *  of the presidents share depends on the building right.
     *  A Player has the maximum of 3 phases without the use of a special power of a certain private paper.
     *  Example : A) Player chooses to build in Phase A+B+C (or B+C+D) this will lead to a president share value of 20%
     *            B) Player chooses to build in Phase A+B (or B+C or C+D) this will lead to a president share value of 30 %
     *            C) Player chooses to build in Phase A (or B or C or D) this will lead to a president share value of 40 %
     *    The BitSet BuildingRights should be able to handle the information :
     *    Bit 1 set True Player can build in Phase A
     *    Bit 2 set True Player can build in Phase B
     *    Bit 3 set True Player can build in Phase C
     *    Bit 4 set True Player can build in Phase D
     *    
     */
    private BuildingRights_1880 buildingRights = new BuildingRights_1880(this, "buildingRights"); 
   
    //Implementation of PhaseAction to be able to handle the CommunistPhase
    private BooleanState canStockPriceMove = BooleanState.create(this, "canStockPriceMove", true);
    private BooleanState canPresidentSellShare = BooleanState.create(this, "canPresidentSellShare", true);
            
    private BooleanState allCertsAvail = BooleanState.create(this, "allCertsAvail", false);
    
    private BooleanState fullyCapitalized = BooleanState.create(this, "fullyCapitalized", false);
    private BooleanState fullCapitalAvailable = BooleanState.create (this, "fullCapitalAvailable", false);
    private int extraCapital = 0; // Just one Change at Start of the game, can stay as it is..
    
    protected IntegerState formationOrderIndex;
    
    protected IntegerState operationSlotIndex = IntegerState.create(this,"OperatingSlot", 0);
  
    /**
     * 
     */
    public PublicCompany_1880(RailsItem parent, String Id) {
        super(parent, Id);
    }
    
    public void start(StockSpace startSpace) {
        extraCapital = 5 * (startSpace.getPrice());
        super.start(startSpace);
    }

    /* (non-Javadoc)
     * @see rails.game.PublicCompany#configureFromXML(rails.common.parser.Tag)
     */
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        super.configureFromXML(tag);
    }
    
    
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#finishConfiguration(rails.game.GameManagerI)
     */
    @Override
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        super.finishConfiguration(root);
        root.getGameManager().setGuiParameter (GuiDef.Parm.HAS_ANY_RIGHTS, true);
        root.getRevenueManager().addStaticModifier(this);
    }

    /**
     * @param buildingRights the buildingRights to set
     */
    public void setBuildingRights(String buildingRights) {
        this.buildingRights.set(buildingRights);
    }

    public void addBuildingPermit(String permitName) {
        buildingRights.set(buildingRights.toText() + "+" + permitName);
    }

    
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        return false;
    }

    public void stockPriceCanMove() {
        canStockPriceMove.set(true);
    }
        
    public void stockPriceCannotMove() {
        canStockPriceMove.set(false);
    }
    
    public boolean canStockPriceMove() {
        return (canStockPriceMove.value());
    }

    /** Don't move the space if the company is withholding train income during the CommunistPhase
     * 
     */
    @Override
    public void withhold(int amount) {
        if (canStockPriceMove.value() == true)  {
            getRoot().getStockMarket().withhold(this);
        }
    }
    
    public void payout(int amount) {
        if (canStockPriceMove.value() == true)  {
            getRoot().getStockMarket().payOut(this);
        }
    }

    public void presidentCanSellShare() {
        canPresidentSellShare.set(true);
    }
        
    public void presidentCannotSellShare() {
        canPresidentSellShare.set(false);
    }
    
    public boolean canPresidentSellShare() {
        return (canPresidentSellShare.value());
    }


    public void setFloatPercentage(int i) {
        this.floatPerc=i;
        
    }
    
    @Override
    public boolean canRunTrains() {
        return portfolio.getNumberOfTrains() > 0;       
    }
    
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#getNumberOfTileLays(java.lang.String)
     */
    @Override
    public int getNumberOfTileLays(String tileColour) {
        Phase phase = getRoot().getPhaseManager().getCurrentPhase();
        
         if ((tileColour.equals("yellow")) && (this.getId().equals("BCR"))) {
             return 2;
         }
         int tileLays = phase.getTileLaysPerColour(getType().getId(), tileColour);
             if (tileLays <= 1) {
                 extraTiles.set(null);
                 return tileLays;
                 }
            // More than one tile lay allowed.
             return tileLays;
     }

    
    /* (non-Javadoc)
     * @see rails.algorithms.RevenueStaticModifier#prettyPrint(rails.algorithms.RevenueAdapter)
     */
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }
    
    /*
     * @param Phase
     */
    public boolean hasBuildingRightForPhase(Phase phase) {
        return buildingRights.canBuildInPhase(phase);
    }
    
    /*
     * If we have a different president share percentage we have to remove the old certificate structure 
     * and rebuild a new structure. There will be no subsequent certificate alterations in 1880.
     * 
     * @author Martin Brumm
     * @param percentage
     * 
     * To be called from the StartRound_1880 / StockRoundWindow_1880
     */
    // TODO: Rails 2.0 Check if this is not too complicated and raises problems with undo
    public void setPresidentShares(int percentage) {
        int share = 0;
        
        //Create a new President Certificate with the shares (percentage)
        PublicCertificate certificate = new PublicCertificate(this, "President", (percentage/10), true,
                true, 1.0f, 0);
        
        //we need to bring that Certificate to the List, do we have to place it at a specific place ? I hope not...
        Owner scrapHeap = getRoot().getBank().getScrapHeap();
        for (PublicCertificate cert : certificates.view()) {
            if (cert.isPresidentShare()) { // get the president share and remove that...
                cert.moveTo(scrapHeap);
                certificates.remove(cert);
            } else if (share >= (100-(percentage) )) {
                    cert.moveTo(scrapHeap);
                    certificates.remove(cert);
            } else {
                    cert.setCertificateCount(1.0f);
                    share += cert.getShare();
            }
             
        }
        //Now add the new president share to the list ; do we have to call namecertificates ?
        
        certificates.add(0,certificate); //Need to make sure the new share is at position 0 !
        nameCertificates(); //Just to be sure..
        PublicCertificate cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(getId(), i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable());
        }
        
          Owner bankIPO= getRoot().getBank().getIpo();
          certificate.moveTo(bankIPO);
    }

    /**
     * @return the fullyCapitalised
     */
    public boolean isFullyCapitalized() {
        return fullyCapitalized.value();
    }

    /**
     * @param fullyCapitalised the fullyCapitalised to set
     */
    public void setFullyCapitalized(boolean fullyCapitalised) {
        this.fullyCapitalized.set(fullyCapitalised);
    }

    /**
     * @return the allCertsAvail
     */
    public boolean getAllCertsAvail() {
        return allCertsAvail.value();
    }

    /**
     * @param flag the allCertsAvail to set
     */
    public void setAllCertsAvail(boolean flag ) {
        this.allCertsAvail.set(flag);
    }
    
    public boolean certsAvailableForSale() {
        if ((sharesInIpo() == 5) && (allCertsAvail.value() == false)) {
            return false;
        } else if (sharesInIpo() == 0) {
            return false;
        }
        return true;
    }
    
    public int sharesInIpo() {
        int sharesInIpo = 0;
        for (PublicCertificate cert : certificates) {
            if (cert.getOwner() == getRoot().getBank().getIpo()) {
                sharesInIpo += cert.getShares();
            }
        }
        return sharesInIpo;
    }
    
    public void setFullFundingAvail() {
        this.fullCapitalAvailable.set(true);
        checkToFullyCapitalize();
    }

    public void sharePurchased() {
        if (fullyCapitalized.value() == true) {
            return;
        }
        checkToFullyCapitalize();
    }
    
    protected boolean checkToFullyCapitalize() {
        if ((hasFloated() == true) && (sharesInIpo() <= 5) && (fullCapitalAvailable.value() == true) && (getFloatPercentage() != 60)) {
            fullyCapitalized.set(true);
            Currency.wire(getRoot().getBank(),extraCapital,this);  
            ReportBuffer.add(this, LocalText.getText("ReceivesCashforRemainingShares",
                    this.getLongName(),
                    Bank.format(this, extraCapital) ));
            return true;
        }
        return false;
    }
    
   public BuildingRights_1880 getRightsModel () {
        return buildingRights;
    }
    
    static public List<PublicCompany_1880> getPublicCompanies(CompanyManager companyManager) {
        List<PublicCompany_1880> companies = new ArrayList<PublicCompany_1880>();
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (company instanceof PublicCompany_1880) {
                companies.add((PublicCompany_1880) company);
            }
        }
        return companies;
    }


    /* (non-Javadoc)
     * @see rails.game.PublicCompany#getBaseTokenLayCost(rails.game.MapHex)
     */
    @Override
    public int getBaseTokenLayCost(MapHex hex) {
        Phase phase = getRoot().getPhaseManager().getCurrentPhase();
        if (phase.getRealName().startsWith("D")) {
            int result;
            result = super.getBaseTokenLayCost(hex) * 2;
            return result;
        }
        return super.getBaseTokenLayCost(hex);
    }

    @Override
    public Set<Integer> getBaseTokenLayCosts() {
        Phase phase = getRoot().getPhaseManager().getCurrentPhase();
        // double token costs in phase D
        if (phase.getRealName().startsWith("D")) {
            ImmutableSet.Builder<Integer> doubleCosts = ImmutableSet.builder();
            for (Integer cost:super.getBaseTokenLayCosts()) {
                doubleCosts.add(cost * 2);
            }
            return doubleCosts.build();
        }
        return super.getBaseTokenLayCosts();
    }
    
}
