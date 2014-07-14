/**
 * 
 */
package rails.game.specific._1880;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jgrapht.graph.SimpleGraph;

import rails.algorithms.NetworkEdge;
import rails.algorithms.NetworkGraphBuilder;
import rails.algorithms.NetworkVertex;
import rails.algorithms.RevenueAdapter;
import rails.algorithms.RevenueStaticModifier;
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
import rails.game.model.ModelObject;

/**
 * @author Martin 2011/04/11
 *
 */
public class Investor_1880 extends PublicCompany implements RevenueStaticModifier {
/*
 * Investors in 1880 get chosen at start after the initial starting package is sold out. 
 * They get one share from a new company 
 */ 
    // Values used to configure super class
    final protected boolean hasStockPrice = false;    
    final protected boolean hasParPrice = false;
    
    protected PublicCompany linkedCompany;  // An Investor is always linked to a (exactly one) Public Major Company..

    // Dummy rights...  investors really don't have any rights.  But because the ORPanel needs
    // to have it...
    private BuildingRights_1880 buildingRights = new BuildingRights_1880("buildingRights"); 

    /*
     * 
     */
    public Investor_1880() {
        super();
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
    
    public void finishConfiguration(GameManagerI gameManager)
            throws ConfigurationException {
        super.finishConfiguration(gameManager);
        gameManager.getRevenueManager().addStaticModifier(this);
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
        if (gameManager.getPhaseManager().hasReachedPhase("4")) {
            return false;
        }
        return true;       
    }
    
    public ModelObject getRightsModel () {
        return buildingRights;
    }

    public int getCurrentTrainLimit() {
        return 0;
    }
    
    public boolean isConnectedToLinkedCompany() {
        NetworkGraphBuilder nwGraph = NetworkGraphBuilder.create(gameManager);
        NetworkCompanyGraph_1880 companyGraph =
                NetworkCompanyGraph_1880.create(nwGraph, this);
        SimpleGraph<NetworkVertex, NetworkEdge> graph =
                companyGraph.createConnectionGraph(true);
        Set<NetworkVertex> verticies = graph.vertexSet();

        PublicCompany_1880 linkedCompany =
                (PublicCompany_1880) ((Investor_1880) this).getLinkedCompany();
        if (linkedCompany != null) {
            for (TokenI token : linkedCompany.getLaidBaseTokens()) {
                TokenHolder holder = token.getHolder();
                if (!(holder instanceof Stop)) continue;
                Stop stop = (Stop) holder;

                for (NetworkVertex vertex : verticies) {
                    if (vertex.getType() == NetworkVertex.VertexType.STATION) {
                        if ((stop.getRelatedStation() == vertex.getStation())
                            && (stop.getHolder() == vertex.getHex())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    static public Investor_1880 getInvestorForPlayer(CompanyManagerI companyManager, Player player) {
        for (Investor_1880 investor : getInvestors(companyManager)) {
            if (investor.getPresident() == player) {
                return investor;
            }
        }
        return null;
    }
    
    static public List<Investor_1880> getInvestors(CompanyManagerI companyManager) {
        List<Investor_1880> investors = new ArrayList<Investor_1880>();
        for (PublicCompanyI company : companyManager.getAllPublicCompanies()) {
            if (company instanceof Investor_1880) {
                investors.add((Investor_1880) company);
            }
        }
        return investors;
    }

    /* (non-Javadoc)
     * @see rails.game.PublicCompany#hasLaidHomeBaseTokens()
     */
    @Override
    public boolean hasLaidHomeBaseTokens() {
        if (this.hasOperated()) {
            return true;
        }
        return laidBaseTokens.size() > 0;
    }
        
    
}
