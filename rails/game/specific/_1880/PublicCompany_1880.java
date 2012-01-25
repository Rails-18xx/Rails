/**
 * 
 */
package rails.game.specific._1880;

import java.util.BitSet;

import rails.common.GuiDef;
import rails.common.parser.ConfigurationException;
import rails.common.parser.Tag;
import rails.game.*;
import rails.game.model.ModelObject;
import rails.game.state.BooleanState;
import rails.game.state.HashMapState;


/**
 * @author Martin
 *
 */
public class PublicCompany_1880 extends PublicCompany {


    /** 
     *  Buildingrights belong to Phases in 1880 the player will be asked to choose which combination
     *   he wants and subsequently his company will be granted the appropriate rights. Further the value
     *  of the presidents share depends on the building right.
     *  A Player has the maximum of 3 phases without the use of a special power of a certain private paper.
     *  Example : A) Player chooses to build in Phase A+B+C (or B+C+D) this will lead to a president share value of 20%
     *            B) Player chooses to build in Phase A+B (or B+C or C+D) this will lead to a president share value of 30 %
     *            C) Player chooses to build in Phase A (or B or C or D) this will lead to a president share value of 40 %
     *    The BitSet BuildingRights should be able to handle the information :
     *    Bit 0 set True Player can build in Phase A
     *    Bit 1 set True Player can build in Phase B
     *    Bit 2 set True Player can build in Phase C
     *    Bit 3 set True Player can build in Phase D
     *    
     */
    private BitSet buildingRights = new BitSet(4); 
    

    
    //Implementation of PhaseAction to be able to handle the CommunistPhase
    private BooleanState communistTakeOver = new BooleanState ("communistTakeOver",false);
    
    //Implementation of PhaseAction to be able to handle the Change in Capitalisation
    private BooleanState capitalChanged = new BooleanState ("capitalChanged",false);
    
    //Implementation of Phase Action to be able to handle the Post Communist Phase
    private BooleanState shanghaiExchangeFounded = new BooleanState ("shanghaiExchangeFounded",false);
    
  

    /**
     * 
     */
    public PublicCompany_1880() {
        super();
        // TODO Auto-generated constructor stub
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
}
