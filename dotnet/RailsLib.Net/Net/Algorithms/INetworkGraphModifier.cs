using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Classes that change properties of the network graph
 * before both route evaluation and revenenue calculation starts
 * implement this interface.
 *  
 * They have to register themselves to the RevenueManager (via declaration in Game.xml or by code)
 * 
 * TODO: It is possible to merge both methods if required
 */

namespace GameLib.Net.Algorithms
{
    public interface INetworkGraphModifier
    {
        /**
        * General modification of the map graph (for all situations and companies)
        * @param mapGraph reference to the map graph
        */
        void ModifyMapGraph(NetworkGraph mapGraph);

        /**
         * Modification of the route graph for a specific company
         * @param mapGraph reference to the map graph
         */
        void ModifyRouteGraph(NetworkGraph mapGraph, PublicCompany company);
    }
}
