package net.sf.rails.game.specific._1880;

import com.google.common.collect.Multimap;
import net.sf.rails.algorithms.*;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.game.*;
import net.sf.rails.game.state.Owner;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;



public class Investor_1880 extends PublicCompany implements RevenueStaticModifier {
    /*
     * Investors in 1880 get chosen at start after the initial starting package is sold out. They get one share from a new company
     *
     */
    protected boolean canOwnShare=true;

    protected int maxPercofShares=1;

    protected PublicCompany linkedCompany;  // An Investor is always linked to a (exactly one) Public Major Company..

    /* Investors in 1880 operate with the newest train model on lease from the bank for zero costs.
     */
    protected boolean canBorrowTrain=true;

    private BuildingRights_1880 buildingRights = new BuildingRights_1880(this,"buildingRights");


    /*
     *
     */

    public Investor_1880(RailsItem parent, String id) {
        super(parent, id, false);
        hasParPrice = false;
    }

    public boolean canOwnShare(){
        return canOwnShare;
    }

    public int maxPercofShares(){
        return maxPercofShares;
    }
    @Override
    public boolean hasStockPrice(){
        return hasStockPrice;
    }

    @Override
    public boolean hasParPrice(){
        return hasParPrice;
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

    public PublicCompany getLinkedCompany(){
        return linkedCompany;
    }

    @Override
    public void finishConfiguration(RailsRoot root)
            throws ConfigurationException {
        super.finishConfiguration(root);
        getRoot().getRevenueManager().addStaticModifier(this);
    }

    @Override
    public boolean modifyCalculator(RevenueAdapter revenueAdapter) {
        // check if running company is this company, otherwise quit
        if (revenueAdapter.getCompany() == this) {
            TrainManager trainManager=getRoot().getTrainManager();
            Set<Train> NewTrains = trainManager.getAvailableNewTrains();
            Train [] NewTrainArray = NewTrains.toArray(new Train[NewTrains.size()]);
            revenueAdapter.addTrainByString( NewTrainArray[0].getType().getName());
        }
        return false;
    }

    @Override
    public String prettyPrint(RevenueAdapter revenueAdapter) {
        return null;
    }

    @Override
    public boolean hasTrains() {
        // By the time communism hits, this company can't run anyway.
        return true;
    }

    @Override
    public int getCurrentTrainLimit() {
        return 0;
    }

    public boolean isConnectedToLinkedCompany() {
        Multimap<MapHex,Station> lStations;
        Multimap<MapHex,Station> iStations;
        NetworkGraph nwGraph = NetworkGraph.createMapGraph(getRoot());
        NetworkGraph companyGraph =
                NetworkGraph.createRouteGraph(nwGraph, this, true, false);
        SimpleGraph<NetworkVertex, NetworkEdge> graph =
                companyGraph.getGraph();
        Set<NetworkVertex> verticies = graph.vertexSet();



        PublicCompany_1880 linkedCompany =
                (PublicCompany_1880) ((Investor_1880) this).getLinkedCompany();

        if (linkedCompany != null) {
            NetworkGraph linkedCompanyGraph=NetworkGraph.createRouteGraph(nwGraph, linkedCompany, true, false);
            // Creating a list of stations blocked by tokens.
            // The connection between investor and Linked Company is NOT blocked by any token of any company.
            // A token that is counted as blocked can be reached by the company for which it blocks the route.
            // Based on that logic a blocking token is reachable by both actors.
            lStations = linkedCompanyGraph.getNonPassableStations();
            iStations = companyGraph.getNonPassableStations();
            //Case A) the token in Question from a linked Company is actually on the route of the Investor
            for (BaseToken token : linkedCompany.getLaidBaseTokens()) {
                Owner holder = token.getOwner();
                if (!(holder instanceof Stop)) continue;
                Stop stop = (Stop) holder;
                for (NetworkVertex vertex : verticies) {
                    if (vertex.getType() == NetworkVertex.VertexType.STATION) {
                        if ((stop.getRelatedStation() == vertex.getStation())
                                && (stop.getParent() == vertex.getHex())) {
                            return true;
                        }
                    }
                }
            }
            // Case B) the Blocking Token is not from the linked Company
            // so we need to check if the MapHex of a blocking station is showing up in the
            // List of non Passable Stations
             for (MapHex blockedHex:lStations.keys()) {
                 if (iStations.containsKey(blockedHex)) {
                     //Make sure its not an Offboard Map Hex
                     if (blockedHex.getCurrentTile().getColour().toString() == "RED" ) continue;
                     if (blockedHex.getStopName().equals("Beijing")) continue;
                     return true;
                    }
             }

        }
        return false;
    }

    static public Investor_1880 getInvestorForPlayer(CompanyManager companyManager, Player player) {
        for (Investor_1880 investor : getInvestors(companyManager)) {
            if (investor.getPresident() == player) {
                return investor;
            }
        }
        return null;
    }

    static public List<Investor_1880> getInvestors(CompanyManager companyManager) {
        List<Investor_1880> investors = new ArrayList<Investor_1880>();
        for (PublicCompany company : companyManager.getAllPublicCompanies()) {
            if (company instanceof Investor_1880) {
                investors.add((Investor_1880) company);
            }
        }
        return investors;
    }

    @Override
    public BuildingRights_1880 getRightsModel() {
        return buildingRights;
    }

    /* (non-Javadoc)
     * @see rails.game.PublicCompany#hasLaidHomeBaseTokens()
     */
    @Override
    public boolean hasLaidHomeBaseTokens() {
        if (this.hasOperated()) {
            return true;
        }
        return baseTokens.nbLaidTokens() > 0;
    }


}
