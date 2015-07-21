package net.sf.rails.algorithms;

import net.sf.rails.game.PublicCompany;

/**
 * Classes that change properties of the network graph
 * before both route evaluation and revenenue calculation starts
 * implement this interface.
 *  
 * They have to register themselves to the RevenueManager (via declaration in Game.xml or by code)
 * 
 * TODO: It is possible to merge both methods if required
 */

public interface NetworkGraphModifier {

    /**
     * General modification of the map graph (for all situations and companies)
     * @param mapGraph reference to the map graph
     */
    public void modifyMapGraph(NetworkGraph mapGraph);
    
    /**
     * Modification of the route graph for a specific company
     * @param mapGraph reference to the map graph
     */
    public void modifyRouteGraph(NetworkGraph mapGraph, PublicCompany company);
    
}
