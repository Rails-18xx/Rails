using GameLib.Net.Common;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * This class coordinates the creation of company related graphs
 */

namespace GameLib.Net.Algorithms
{
    public class NetworkAdapter
    {
        protected static Logger<NetworkAdapter> log = new Logger<NetworkAdapter>();

        private RailsRoot root;

        private NetworkGraph mapGraph;
        private NetworkGraph routeGraph;
        private NetworkGraph revenueGraph;
        private NetworkMultigraph multiGraph;

        private PublicCompany company;
        private bool addHQ;

        private NetworkAdapter(RailsRoot root)
        {
            this.root = root;
        }

        public static NetworkAdapter Create(RailsRoot root)
        {
            return new NetworkAdapter(root);
        }

        public NetworkGraph GetMapGraph()
        {
            mapGraph = NetworkGraph.CreateMapGraph(root);
            log.Info("MapGraph created");
            return mapGraph;
        }

        public NetworkGraph GetRouteGraph(PublicCompany company, bool addHQ)
        {
            routeGraph = NetworkGraph.CreateRouteGraph(GetMapGraph(), company, addHQ);
            this.company = company;
            this.addHQ = addHQ;
            log.Info("RouteGraph created");
            return routeGraph;
        }

        public NetworkGraph GetRouteGraphCached(PublicCompany company, bool addHQ)
        {
            if (routeGraph == null || company != this.company || addHQ != this.addHQ)
            {
                if (mapGraph != null)
                {
                    routeGraph = NetworkGraph.CreateRouteGraph(mapGraph, company, addHQ);
                }
                else
                {
                    GetRouteGraph(company, addHQ);
                }
            }
            return routeGraph;
        }

        public NetworkGraph GetRevenueGraph(PublicCompany company,
                ICollection<NetworkVertex> protectedVertices)
        {
            if (revenueGraph == null)
            {
                revenueGraph = NetworkGraph.CreateOptimizedGraph(GetRouteGraphCached(company, false),
                        protectedVertices);
                log.Info("RevenueGraph created");
            }

            return revenueGraph;
        }

        public NetworkMultigraph GetMultigraph(PublicCompany company,
                ICollection<NetworkVertex> protectedVertices)
        {
            if (multiGraph == null)
            {
                multiGraph = NetworkMultigraph.Create(
                        GetRevenueGraph(company, protectedVertices), protectedVertices);
                log.Info("MultiGraph created");
            }
            return multiGraph;
        }
    }
}
