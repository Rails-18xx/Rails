/**
 * 
 */
package rails.game.specific._1880;

import java.util.BitSet;
import java.util.Set;

import org.jgrapht.graph.SimpleGraph;

import rails.algorithms.NetworkEdge;
import rails.algorithms.NetworkGraphBuilder;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueStaticModifier;
import rails.common.GuiDef;
import rails.common.parser.ConfigurationException;
import rails.game.CompanyManagerI;
import rails.game.GameManagerI;
import rails.game.Player;
import rails.game.PublicCompany;
import rails.game.PublicCompanyI;
import rails.game.Stop;
import rails.game.TokenHolder;
import rails.game.TokenI;
import rails.game.TrainManager;
import rails.game.state.BooleanState;
import rails.game.state.HashMapState;

/**
 * @author Martin 2011/04/11
 *
 */
public class Investor_1880 extends PublicCompany implements RevenueStaticModifier {
/*
 * Investors in 1880 get chosen at start after the initial starting package is sold out. They get one share from a new company 
 * TODO: Make sure that dividends aren't accumulated on the investors
    
*/
    protected boolean canOwnShare=true;
    
    protected int maxPercofShares=1;
    
    protected boolean hasStockPrice=false;
    
    protected boolean hasParPrice=false;
    
    protected PublicCompany linkedCompany;  // An Investor is always linked to a (exactly one) Public Major Company..
    
    /* Investors in 1880 operate with the newest train model on lease from the bank for zero costs.
    */
    protected boolean canBorrowTrain=true;

    private BitSet buildingRights = new BitSet(5); // Not used - just here as a dummy value
      
    /**
     * 
     */
    public Investor_1880() {
        super();
    }
    
    public int getCurrentTrainLimit() {
        return 0;
    }
        
    public boolean canOwnShare(){
        return canOwnShare;
    }
    
    public int maxPercofShares(){
        return maxPercofShares;
    }
    public boolean hasStockPrice(){
        return hasStockPrice;
    }
    
    public boolean hasParPrice(){
        return hasParPrice;
    }
    
    public PublicCompany getLinkedCompany(){
        return linkedCompany;
    }
    
    public boolean setLinkedCompany(PublicCompany linkedCompany){
        if (linkedCompany != null){
            //Check if Company is valid i.e. not Closed maybe check if theres already the President sold and just the president...
            if(!linkedCompany.isClosed()){
                this.linkedCompany=linkedCompany;
                return true;}
            }
        return false; 
        }
    
    public BitSet getBuildingRights() {
        return buildingRights;
    }

    public void setBuildingRights(BitSet buildingRights) {
        this.buildingRights = buildingRights;
    }

    public void finishConfiguration(GameManagerI gameManager)
            throws ConfigurationException {
        // TODO Auto-generated method stub
        super.finishConfiguration(gameManager);
        // Introducing the rights field in the Display to be used by Building Rights Display and other Special Properties...
        gameManager.setGuiParameter (GuiDef.Parm.HAS_ANY_RIGHTS, true);
        if (rights == null) rights = new HashMapState<String, String>(name+"_Rights");
        // add revenue modifier for the Investors
        gameManager.getRevenueManager().addStaticModifier(this);
        hasReachedDestination = new BooleanState (name+"_reachedDestination", false);   
    }

    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // check if running company is this company, otherwise quit
        if (revenueAdapter.getCompany() == this) {
            TrainManager trainManager=gameManager.getTrainManager();
            revenueAdapter.addTrainByString(trainManager.getAvailableNewTrains().get(0).getName());
        }
        return false;
    }

    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }
    
    public boolean canRunTrains() {
        // By the time communism hits, this company can't run anyway.
        return true;       
    }
    
    public boolean isConnectedToLinkedCompany() {
        NetworkGraphBuilder nwGraph = NetworkGraphBuilder.create(gameManager);
        NetworkCompanyGraph_1880 companyGraph = NetworkCompanyGraph_1880.create(nwGraph, this);
        SimpleGraph<NetworkVertex, NetworkEdge> graph = companyGraph.createConnectionGraph(true);
        Set<NetworkVertex> verticies = graph.vertexSet();
            
        PublicCompany_1880 linkedCompany = (PublicCompany_1880) ((Investor_1880) this).getLinkedCompany();
            
            for (TokenI token : linkedCompany.getLaidBaseTokens()) {
                TokenHolder holder = token.getHolder();
                if (!(holder instanceof Stop)) continue;
                Stop stop = (Stop) holder;                
                
                for (NetworkVertex vertex : verticies) {
                    if (vertex.getType() == NetworkVertex.VertexType.STATION) {
                        if ((stop.getRelatedStation() == vertex.getStation()) && (stop.getHolder() == vertex.getHex())) {
                            return true;
                        }
                    }
                }
            }
            
        return false;
    }
    
    static public Investor_1880 getInvestorForPlayer(CompanyManagerI companyManager, Player player) {
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if ((company instanceof Investor_1880) && (company.getPresident() == player)) {
                return (Investor_1880) company;
            }
        }
        return null;
    }
        
}
