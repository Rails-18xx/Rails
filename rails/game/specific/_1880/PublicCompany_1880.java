/**
 * 
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueBonus;
import rails.algorithms.RevenueStaticModifier;
import rails.common.GuiDef;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.game.model.ModelObject;
import rails.game.move.MoveableHolder;
import rails.game.move.RemoveFromList;
import rails.game.state.BooleanState;
import rails.game.state.HashMapState;
import rails.game.state.IntegerState;


/**
 * @author Martin
 *
 */
public class PublicCompany_1880 extends PublicCompany implements RevenueStaticModifier {


    /** 
     *  Buildingrights belong to Phases in 1880 the player will be asked to choose which combination
     *   he wants and subsequently his company will be granted the appropriate rights. Further the value
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
    private BitSet buildingRights = new BitSet(5); 
    

    
    //Implementation of PhaseAction to be able to handle the CommunistPhase
    private BooleanState communistTakeOver = new BooleanState ("communistTakeOver",false);
    
    //Implementation of PhaseAction to be able to handle the Change in Capitalisation
    private BooleanState capitalChanged = new BooleanState ("capitalChanged",false);
    
    //Implementation of Phase Action to be able to handle the Post Communist Phase
    private BooleanState shanghaiExchangeFounded = new BooleanState ("shanghaiExchangeFounded",false);
    
    private BooleanState allCertsAvail = new BooleanState ("allCertsAvail", false);
    
    private boolean fullyCapitalised = false;
    
    protected IntegerState formationOrderIndex;
    
    protected IntegerState operationSlotIndex = new IntegerState ("OperatingSlot, 0");
  
    /**
     * 
     */
    public PublicCompany_1880() {
        super();
        // TODO Auto-generated constructor stub
    }

    public void start(StockSpaceI startSpace) {
        super.start(startSpace);
        //PD: used to track flotation order
        formationOrderIndex = new IntegerState(name+"_formationOrderIndex");
    }

    public int getFormationOrderIndex() {
        return formationOrderIndex.intValue();
    }

    public void setFormationOrderIndex(int formationOrderIndex) {
        this.formationOrderIndex.set(formationOrderIndex);
    }


    /* (non-Javadoc)
     * @see rails.game.PublicCompany#configureFromXML(rails.common.parser.Tag)
     */
    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {
        // TODO Auto-generated method stub
        super.configureFromXML(tag);
    }
    
    
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#finishConfiguration(rails.game.GameManagerI)
     */
    @Override
    public void finishConfiguration(GameManagerI gameManager)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        super.finishConfiguration(gameManager);
        // Introducing the rights field in the Display to be used by Building Rights Display and other Special Properties...
        gameManager.setGuiParameter (GuiDef.Parm.HAS_ANY_RIGHTS, true);
        if (rights == null) rights = new HashMapState<String, String>(name+"_Rights");
        // add revenue modifier for the Investors
        gameManager.getRevenueManager().addStaticModifier(this);
        if (this.getTypeName().equals("Minor")) {
        hasReachedDestination = new BooleanState (name+"_reachedDestination", false);
        }
    }

    /**
     * @return the buildingRights
     */
    public BitSet getBuildingRights() {
        return buildingRights;
    }

    /**
     * @param buildingRights the buildingRights to set
     */
    public void setBuildingRights(BitSet buildingRights) {
        this.buildingRights = buildingRights;
    }

    public void setCommunistTakeOver(boolean b) {
        communistTakeOver.set(b);
        
    }
    /**
     * @return the communistTakeOver
     */
    public Boolean isCommunistPhase() {
        return communistTakeOver.booleanValue();
    }
    
    public ModelObject getCommunistTakeOver() {
        return communistTakeOver;
    }

    /** Don't move the space if the company is withholding train income during the CommunistPhase
     * 
     */
    @Override
    public void withhold(int amount) {
        if (isCommunistPhase()) return;
        if (hasStockPrice) stockMarket.withhold(this);
    }

    public void setFloatPercentage(int i) {
        this.floatPerc=i;
        
    }
    
    @Override
    public boolean canRunTrains() {
        if (!isCommunistPhase() && (!hasStockPrice()) ){
            return true;
            }
        return portfolio.getNumberOfTrains() > 0;
       
    }
    
    
    
    /* (non-Javadoc)
     * @see rails.game.PublicCompany#getNumberOfTileLays(java.lang.String)
     */
    @Override
    public int getNumberOfTileLays(String tileColour) {

         if ((tileColour.equals("yellow")) && (this.getName().equals("BCR"))) {
             int result=2;
             return result;
         } else {
        return super.getNumberOfTileLays(tileColour);
         }
    }

    /**
     * @return the capitalChanged
     */
    public BooleanState getCapitalChanged() {
        return capitalChanged;
    }

    /**
     * @param capitalChanged the capitalChanged to set
     */
    public void setCapitalChanged(BooleanState capitalChanged) {
        this.capitalChanged = capitalChanged;
    }
    
    public boolean shouldBeCapitalisedFull() {
        return this.capitalChanged.booleanValue();
    }
    
    /**
     * @return the shanghaiExchangeFounded
     */
    public BooleanState getShanghaiExchangeFounded() {
        return shanghaiExchangeFounded;
    }

    /**
     * @param shanghaiExchangeFounded the shanghaiExchangeFounded to set
     */
    public void setShanghaiExchangeFounded(BooleanState shanghaiExchangeFounded) {
        this.shanghaiExchangeFounded = shanghaiExchangeFounded;
    }

    public boolean shanghaiExchangeIsOperational(){
        return this.shanghaiExchangeFounded.booleanValue();
    }
    
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // check if running company is this company, otherwise quit
        if (revenueAdapter.getCompany() != this) return false; 
        
        // check if company is a minor
        if (this.getTypeName().equals("Minor")) {
            // add the current Available train from the IPO and not the last train in Use
            TrainManager trainManager=gameManager.getTrainManager();
            revenueAdapter.addTrainByString(trainManager.getAvailableNewTrains().get(0).getName());
        } else {
            int additionalStockRevenue = revenueAdapter.getCompany().getCurrentSpace().getType().hasAddRevenue()*10;
            RevenueBonus bonus = new RevenueBonus(additionalStockRevenue, "StockPosition");
            revenueAdapter.addRevenueBonus(bonus);
        }
        // no text needed
        return false;
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
    public boolean hasBuildingRightForPhase(PhaseI phase){
        
        String currentPhase=phase.getRealName();
        
        if ((this.buildingRights.get(0)) && ((currentPhase.startsWith("A")))) {
        return true;
        } else if ((this.buildingRights.get(1)) && ((currentPhase.startsWith("B")))) {
            return true;
        } else if ((this.buildingRights.get(2)) && ((currentPhase.startsWith("C")))) {
            return true;
        } else if ((this.buildingRights.get(3)) && ((currentPhase.startsWith("D")))) {
            return true;    
        } else {
            return false;
        }
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
    
    public void setPresidentShares(int percentage) {
        
        List<PublicCertificateI>certs = new ArrayList<PublicCertificateI>(certificates);
        int share = 0;
        
        //Create a new President Certificate with the shares (percentage)
        PublicCertificateI certificate = new PublicCertificate((percentage/10), true,
                true, 1.0f, 0);
        
        //we need to bring that Certificate to the List, do we have to place it at a specific place ? I hope not...
        MoveableHolder scrapHeap = bank.getScrapHeap();
        for (PublicCertificateI cert : certs) {
            if (cert.isPresidentShare()) { // get the president share and remove that...
                cert.moveTo(scrapHeap);
                new RemoveFromList<PublicCertificateI>(certificates, cert, this.name+"_certs");
            } else if (share >= (100-(percentage) )) {
                    cert.moveTo(scrapHeap);
                    new RemoveFromList<PublicCertificateI>(certificates, cert, this.name+"_certs");
            } else {
                    cert.setCertificateCount(1.0f);
                    share += cert.getShare();
            }
             
        }
        //Now add the new president share to the list ; do we have to call namecertificates ?
        
        certificates.add(0,certificate); //Need to make sure the new share is at position 0 !
        nameCertificates(); //Just to be sure..
        PublicCertificateI cert;
        for (int i = 0; i < certificates.size(); i++) {
            cert = certificates.get(i);
            cert.setUniqueId(name, i);
            cert.setInitiallyAvailable(cert.isInitiallyAvailable());
        }
        
          MoveableHolder bankIPO= bank.getIpo();
          certificate.moveTo(bankIPO);
        
       /*     // Update all owner ShareModels (once)
        *    // to have the UI get the correct percentage
        * // Martin: perhaps this not necessary as the certificates in question havent been handed out...
        */
           List<Portfolio> done = new ArrayList<Portfolio>();
            Portfolio portfolio;
            for (PublicCertificateI cert2 : certificates) {
              portfolio = (Portfolio)cert2.getHolder();
              if (!done.contains(portfolio)) {
                 portfolio.getShareModel(this).setShare();
                  done.add(portfolio);
              }
           }
       
    }

    /**
     * @return the fullyCapitalised
     */
    public boolean isFullyCapitalised() {
        return fullyCapitalised;
    }

    /**
     * @param fullyCapitalised the fullyCapitalised to set
     */
    public void setFullyCapitalised(boolean fullyCapitalised) {
        this.fullyCapitalised = fullyCapitalised;
    }

    /**
     * @return the allCertsAvail
     */
    public boolean getAllCertsAvail() {
        return allCertsAvail.booleanValue();
    }

    /**
     * @param flag the allCertsAvail to set
     */
    public void setAllCertsAvail(boolean flag ) {
        this.allCertsAvail.set(flag);
    }

/*    @Override
    public void setFloated() {
        super.setFloated();
        if (this.getTypeName().equals("Minor")) return;
        //Need to find out if other corps exist at this par price
        //If so increment formationOrderIndex to control Operating sequence
        for (PublicCompanyI company : gameManager.getAllPublicCompanies()) {
            if ((company.hasStarted()) && (!company.getTypeName().equals("Minor"))){
                if (this.getStartSpace().getPrice() == company.getStartSpace().getPrice() && (this.getName() != company.getName())){
                    //Yes, we share IPO prices, has this other company been launched yet?
                    if (company.hasFloated()){
                        //it has, we need to skip ahead of this corp
                        formationOrderIndex.add(1);
                    }
                }
            }
                
        }
    }
*/
    
}
