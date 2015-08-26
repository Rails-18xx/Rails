package net.sf.rails.game.specific._1844;

import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.graph.SimpleGraph;

import com.google.common.collect.Sets;

import net.sf.rails.algorithms.NetworkEdge;
import net.sf.rails.algorithms.NetworkGraph;
import net.sf.rails.algorithms.NetworkVertex;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.Bank;
import net.sf.rails.game.BaseToken;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.StockSpace;
import net.sf.rails.game.Stop;
import net.sf.rails.game.model.BaseTokensModel;
import net.sf.rails.game.specific._1880.Investor_1880;
import net.sf.rails.game.specific._1880.PublicCompany_1880;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.Owner;

public class PublicCompany_1844 extends PublicCompany {

    private BooleanState fullyCapitalized = BooleanState.create(this, "fullyCapitalized", false);
    private BooleanState fullCapitalAvailable = BooleanState.create (this, "fullCapitalAvailable", false);
    
    private int extraCapital = 0; // Just one Change at Start of the game, can stay as it is..

    
    public PublicCompany_1844(RailsItem parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }
    
    public void start(StockSpace startSpace) {
         if(this.getType().getId().equals("VOR-SBB")) {
          extraCapital = 2 * (this.getIPOPrice());   
         } else {
        extraCapital = 5 * (this.getIPOPrice());
         }
         //Determine the number of tokens available to the historic companies by the startprice
<<<<<<< Upstream, based on origin/rails_2_develop
         if (this.getNumberOfBaseTokens()<=0) {
             switch(startSpace.getPrice()) {
             case 100:
                 this.setNumberOfBaseTokens(5);
                 break;
             case 90:
                 this.setNumberOfBaseTokens(4);
                 break;
             case 80:
                 this.setNumberOfBaseTokens(3);
                 break;
             case 70:
                 this.setNumberOfBaseTokens(2);
                 break;
             case 60:
                 this.setNumberOfBaseTokens(1);
                 break;
=======
         if (this.getNumberOfBaseTokens()<0) {
             switch(startSpace.getPrice()) {
             case 100:
                 this.setNumberOfBaseTokens(5);
             case 90:
                 this.setNumberOfBaseTokens(4);
             case 80:
                 this.setNumberOfBaseTokens(3);
             case 70:
                 this.setNumberOfBaseTokens(2);
             case 60:
                 this.setNumberOfBaseTokens(1);
>>>>>>> dd894c8 1844 : Starts up but the map needs tile and tiles need orientation :)
             default:
                 this.setNumberOfBaseTokens(1);
             }
         }
         
        super.start(startSpace);
    }
    
    private void setNumberOfBaseTokens(int i) {
        this.numberOfBaseTokens=i;
<<<<<<< Upstream, based on origin/rails_2_develop
        TreeSet<BaseToken> newTokens = Sets.newTreeSet();
        for (int j = 0; j < numberOfBaseTokens; j++) {
            BaseToken token =  BaseToken.create(this);
            newTokens.add(token);
        }
        baseTokens.initTokens(newTokens);
=======
>>>>>>> dd894c8 1844 : Starts up but the map needs tile and tiles need orientation :)
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
    
    public void setFullFundingAvail() {
        this.fullCapitalAvailable.set(true);
        checkToFullyCapitalize();
    }
    
    protected boolean checkToFullyCapitalize() {
        if ((hasFloated() == true) && (isConnectedToDestinationHex()) || (fullCapitalAvailable.value() == true)) {
            fullyCapitalized.set(true);
            Currency.wire(getRoot().getBank(),extraCapital,this);  
            ReportBuffer.add(this, LocalText.getText("ReceivesFullWorkingCapital",
                    this.getLongName(),
                    Bank.format(this, extraCapital) ));
            return true;
        }
        return false;
    }
    
    public boolean isConnectedToDestinationHex() {
        NetworkGraph nwGraph = NetworkGraph.createMapGraph(getRoot());
        NetworkGraph companyGraph =
                NetworkGraph.createRouteGraph(nwGraph, this, true);
        SimpleGraph<NetworkVertex, NetworkEdge> graph =
                companyGraph.getGraph();
        Set<NetworkVertex> verticies = graph.vertexSet();
        for (NetworkVertex vertex : verticies) {
                    if ( vertex.getHex() == this.getDestinationHex()) {
                            return true;
                        }
                    }
        return false;
    }

    
    
}
