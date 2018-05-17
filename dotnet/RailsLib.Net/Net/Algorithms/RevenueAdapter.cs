using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Graph;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

/**
 * RevenueAdapter links the revenue algorithm to Rails.
 */

namespace GameLib.Net.Algorithms
{
    public sealed class RevenueAdapter
    {
        private static Logger<RevenueAdapter> log = new Logger<RevenueAdapter>();

        // define VertexVisitSet
        public class VertexVisit
        {
            public HashSet<NetworkVertex> set;
            public VertexVisit() { set = new HashSet<NetworkVertex>(); }
            public VertexVisit(IEnumerable<NetworkVertex> coll) { set = new HashSet<NetworkVertex>(coll); }
            override public string ToString()
            {
                return "VertexVisit Set:" + set;
            }
        }

        // define EdgeTravelSet
        public class EdgeTravel
        {
            public HashSet<NetworkEdge> set;
            public EdgeTravel() { set = new HashSet<NetworkEdge>(); }
            public EdgeTravel(IEnumerable<NetworkEdge> coll) { set = new HashSet<NetworkEdge>(coll); }
            override public string ToString()
            {
                return "EdgeTravel Set:" + set;
            }
        }

        // basic links, to be defined at creation
        private RailsRoot root;
        private RevenueManager revenueManager;
        private NetworkAdapter networkAdapter;
        private PublicCompany company;
        private Phase phase;

        // basic components, defined empty at creation
        private NetworkGraph graph;
        private HashSet<NetworkVertex> startVertices;
        private List<NetworkTrain> trains;
        private List<VertexVisit> vertexVisitSets;
        private List<RevenueBonus> revenueBonuses;
        private HashSet<NetworkVertex> protectedVertices;
        private Dictionary<NetworkEdge, EdgeTravel> edgeTravelSets;

        // components related to the revenue calculator
        private RevenueCalculator rc;
        private bool useMultiGraph;
        IGraph<NetworkVertex, NetworkEdge> rcGraph;
        private List<NetworkVertex> rcVertices;
        private List<NetworkEdge> rcEdges;
        private List<RevenueTrainRun> optimalRun;
        private bool hasDynamicModifiers;

        // revenue listener to communicate results
        //private IRevenueListener revenueListener;

        public RevenueAdapter(RailsRoot root, NetworkAdapter networkAdapter,
                PublicCompany company, Phase phase)
        {
            this.root = root;
            this.revenueManager = root.RevenueManager;
            this.networkAdapter = networkAdapter;
            this.company = company;
            this.phase = phase;

            this.graph = null;
            this.trains = new List<NetworkTrain>();
            this.startVertices = new HashSet<NetworkVertex>();
            this.vertexVisitSets = new List<VertexVisit>();
            this.edgeTravelSets = new Dictionary<NetworkEdge, EdgeTravel>();
            this.revenueBonuses = new List<RevenueBonus>();
            this.protectedVertices = new HashSet<NetworkVertex>();
        }

        public static RevenueAdapter CreateRevenueAdapter(RailsRoot root, PublicCompany company, Phase phase)
        {
            NetworkAdapter networkAdapter = NetworkAdapter.Create(root);
            RevenueAdapter ra = new RevenueAdapter(root, networkAdapter, company, phase);
            ra.PopulateFromRails();
            return ra;
        }


        public PublicCompany Company
        {
            get
            {
                return company;
            }
        }

        public Phase Phase
        {
            get
            {
                return phase;
            }
        }

        public SimpleGraph<NetworkVertex, NetworkEdge> Graph
        {
            get
            {
                return graph.Graph;
            }
        }

        public ICollection<NetworkVertex> Vertices
        {
            get
            {
                return graph.Graph.Vertices;
            }
        }

        public ICollection<NetworkEdge> Edges
        {
            get
            {
                return graph.Graph.Edges;
            }
        }

        public IGraph<NetworkVertex, NetworkEdge> RCGraph
        {
            get
            {
                return rcGraph;
            }
        }

        public int GetRCVertexId(NetworkVertex vertex)
        {
            return rcVertices.IndexOf(vertex);
        }

        public int GetRCEdgeId(NetworkEdge edge)
        {
            return rcEdges.IndexOf(edge);
        }

        public HashSet<NetworkVertex> StartVertices
        {
            get
            {
                return startVertices;
            }
        }

        public bool AddStartVertices(IEnumerable<NetworkVertex> startVertices)
        {
            this.startVertices.UnionWith(startVertices);
            protectedVertices.UnionWith(startVertices);
            return true;
        }

        public List<NetworkTrain> Trains
        {
            get
            {
                return trains;
            }
        }

        public bool AddTrain(Train railsTrain)
        {
            NetworkTrain train = NetworkTrain.CreateFromRailsTrain(railsTrain);
            if (train == null)
            {
                return false;
            }
            else
            {
                trains.Add(train);
                return true;
            }
        }

        public void AddTrain(NetworkTrain train)
        {
            trains.Add(train);
        }

        public void RemoveTrain(NetworkTrain train)
        {
            trains.Remove(train);
        }

        public bool AddTrainByString(string trainString)
        {
            NetworkTrain train = NetworkTrain.CreateFromString(trainString);
            if (train == null) return false;
            trains.Add(train);
            return true;
        }

        public List<VertexVisit> VertexVisitSets
        {
            get
            {
                return vertexVisitSets;
            }
        }

        public void AddVertexVisitSet(VertexVisit visit)
        {
            vertexVisitSets.Add(visit);
            protectedVertices.UnionWith(visit.set);
        }

        public List<RevenueBonus> RevenueBonuses
        {
            get
            {
                return revenueBonuses;
            }
        }

        public void AddRevenueBonus(RevenueBonus bonus)
        {
            revenueBonuses.Add(bonus);
            protectedVertices.UnionWith(bonus.Vertices);
        }

        public void RemoveRevenueBonus(RevenueBonus bonus)
        {
            revenueBonuses.Remove(bonus);
            // TODO: Change protectedVertices to multiSet then you can un-protect vertices
        }

        public void PopulateFromRails()
        {
            // define graph, without HQ
            graph = networkAdapter.GetRouteGraphCached(company, false);

            // initialize vertices
            NetworkVertex.InitAllRailsVertices(graph, company, phase);

            // define startVertexes
            AddStartVertices(graph.GetCompanyBaseTokenVertexes(company));

            // define visit sets
            DefineVertexVisitSets();

            // define revenueBonuses
            DefineRevenueBonuses();

            // define Trains
            foreach (Train train in company.PortfolioModel.GetTrainList())
            {
                AddTrain(train);
            }

            // add all static modifiers
            if (revenueManager != null)
            {
                revenueManager.InitStaticModifiers(this);
            }

        }

        private void DefineVertexVisitSets()
        {
            // define map of all locationNames 
            Dictionary<string, VertexVisit> locations = new Dictionary<string, VertexVisit>();
            foreach (NetworkVertex vertex in Vertices)
            {
                string ln = vertex.StopName;
                if (ln == null) continue;
                if (locations.ContainsKey(ln))
                {
                    locations[ln].set.Add(vertex);
                }
                else
                {
                    VertexVisit v = new VertexVisit();
                    v.set.Add(vertex);
                    locations[ln] = v;
                }
            }
            log.Info("Locations = " + locations);
            // convert the location map to the vertex sets
            foreach (VertexVisit location in locations.Values)
            {
                if (location.set.Count >= 2)
                {
                    AddVertexVisitSet(location);
                }
            }
        }

        private void DefineRevenueBonuses()
        {
            // create set of all hexes
            HashSet<MapHex> hexes = new HashSet<MapHex>();
            foreach (NetworkVertex vertex in Vertices)
            {
                MapHex hex = vertex.Hex;
                if (hex != null) hexes.Add(hex);
            }

            // check each vertex hex for a potential revenue bonus
            foreach (MapHex hex in hexes)
            {
                List<RevenueBonusTemplate> bonuses = new List<RevenueBonusTemplate>();
                List<RevenueBonusTemplate> hexBonuses = hex.RevenueBonuses;
                if (hexBonuses != null) bonuses.AddRange(hexBonuses);
                List<RevenueBonusTemplate> tileBonuses = hex.CurrentTile.RevenueBonuses;
                if (tileBonuses != null) bonuses.AddRange(tileBonuses);

                foreach (RevenueBonusTemplate bonus in bonuses)
                {
                    RevenueBonus bonusConverted = bonus.ToRevenueBonus(hex, root, graph);
                    if (bonusConverted != null)
                    {
                        AddRevenueBonus(bonusConverted);
                    }
                }
            }
            log.Info("RA: RevenueBonuses = " + revenueBonuses);
        }

        /**
         * checks the set of trains for H-trains
         * @return true if H-trains are used
         */
        private bool UseHTrains
        {
            get
            {
                bool useHTrains = false;
                foreach (NetworkTrain train in trains)
                {
                    if (train.IsHTrain)
                    {
                        useHTrains = true;
                    }
                }
                return useHTrains;
            }
        }

        public void InitRevenueCalculator(bool useMultiGraph)
        {

            this.useMultiGraph = useMultiGraph;

            // check for dynamic modifiers (including an own calculator
            if (revenueManager != null)
            {
                hasDynamicModifiers = revenueManager.InitDynamicModifiers(this);
            }

            // define optimized graph

            if (useMultiGraph)
            {
                // generate phase 2 graph
                NetworkMultigraph multiGraph = networkAdapter.GetMultigraph(company, protectedVertices);
                rcGraph = multiGraph.Graph;
                // retrieve edge sets
                multiGraph.GetPhaseTwoEdgeSets(this).ToList().ForEach(x => edgeTravelSets[x.Key] = x.Value); // This code adds all the items to edgeTravelSets
            }
            else
            {
                // generate standard graph
                rcGraph = networkAdapter.GetRevenueGraph(company, protectedVertices).Graph;
            }

            // define the vertices and edges lists
            rcVertices = new List<NetworkVertex>(rcGraph.Vertices);
            // define ordering on vertexes by value
            rcVertices.Sort(new NetworkVertex.ValueOrder());
            rcEdges = new List<NetworkEdge>(rcGraph.Edges);
            rcEdges.Sort(new NetworkEdge.CostOrder());

            // prepare train length
            PrepareTrainLengths(rcVertices);

            // check dimensions
            int maxVisitVertices = MaxVisitVertices();
            int maxBonusVertices = MaxRevenueBonusVertices();
            int maxNeighbors = MaxVertexNeighbors(rcVertices);
            int maxTravelEdges = MaxTravelEdges();

            if (useMultiGraph)
            {
                if (UseHTrains)
                {
                    rc = new RevenueCalculatorMultiHex(this, rcVertices.Count, rcEdges.Count,
                            maxNeighbors, maxVisitVertices, maxTravelEdges, trains.Count, maxBonusVertices);
                }
                else
                {
                    rc = new RevenueCalculatorMulti(this, rcVertices.Count, rcEdges.Count,
                            maxNeighbors, maxVisitVertices, maxTravelEdges, trains.Count, maxBonusVertices);
                }
            }
            else
            {
                rc = new RevenueCalculatorSimple(this, rcVertices.Count, rcEdges.Count,
                        maxNeighbors, maxVisitVertices, trains.Count, maxBonusVertices);
            }

            PopulateRevenueCalculator();
        }

        private int MaxVisitVertices()
        {
            int maxNbVertices = 0;
            foreach (VertexVisit vertexVisit in vertexVisitSets)
            {
                maxNbVertices = Math.Max(maxNbVertices, vertexVisit.set.Count);
            }
            log.Info("RA: Block of " + vertexVisitSets + ", maximum vertices in a set = " + maxNbVertices);
            return maxNbVertices;
        }

        private int MaxVertexNeighbors(IEnumerable<NetworkVertex> vertices)
        {
            int maxNeighbors = 0;
            foreach (NetworkVertex vertex in vertices)
            {
                maxNeighbors = Math.Max(maxNeighbors, rcGraph.EdgesOf(vertex).Count);
            }
            log.Info("RA: Maximum neighbors in graph = " + maxNeighbors);
            return maxNeighbors;
        }

        private int MaxRevenueBonusVertices()
        {
            // get the number of non-simple bonuses
            int nbBonuses = RevenueBonus.GetNumberNonSimpleBonuses(revenueBonuses);
            log.Info("Number of non simple bonuses = " + nbBonuses);
            return nbBonuses;
        }

        private int MaxTravelEdges()
        {
            int maxNbEdges = 0;
            foreach (EdgeTravel edgeTravel in edgeTravelSets.Values)
            {
                maxNbEdges = Math.Max(maxNbEdges, edgeTravel.set.Count);
            }
            foreach (NetworkEdge edge in edgeTravelSets.Keys)
            {
                EdgeTravel edgeTravel = edgeTravelSets[edge];
                StringBuilder edgeString = new StringBuilder("RA: EdgeSet for " + edge.ToFullInfoString() +
                       " size = " + edgeTravel.set.Count + "\n");
                foreach (NetworkEdge edgeInSet in edgeTravel.set)
                {
                    edgeString.Append(edgeInSet.ToFullInfoString() + "\n");
                }
                log.Info(edgeString.ToString());
            }
            log.Info("RA: maximum edges in a set = " + maxNbEdges);
            return maxNbEdges;
        }


        private void PrepareTrainLengths(IEnumerable<NetworkVertex> vertices)
        {
            // separate vertexes
            List<NetworkVertex> cities = new List<NetworkVertex>();
            List<NetworkVertex> towns = new List<NetworkVertex>();
            foreach (NetworkVertex vertex in vertices)
            {
                if (vertex.IsMajor) cities.Add(vertex);
                if (vertex.IsMinor) towns.Add(vertex);
            }

            int maxCities = cities.Count;
            int maxTowns = towns.Count;

            // check train lengths
            int maxCityLength = 0, maxTownLength = 0;
            foreach (NetworkTrain train in trains)
            {
                int trainTowns = train.Minors;
                if (train.Majors > maxCities)
                {
                    trainTowns = trainTowns + train.Majors - maxCities;
                    train.Majors = maxCities;
                }
                train.Minors = Math.Min(trainTowns, maxTowns);

                maxCityLength = Math.Max(maxCityLength, train.Majors);
                maxTownLength = Math.Max(maxTownLength, train.Minors);
            }

        }

        private void PopulateRevenueCalculator()
        {
            for (int index = 0; index < rcVertices.Count; index++)
            {
                NetworkVertex v = rcVertices[index];
                // add to revenue calculator
                v.AddToRevenueCalculator(rc, index);
                for (int trainId = 0; trainId < trains.Count; trainId++)
                {
                    NetworkTrain train = trains[trainId];
                    rc.SetVertexValue(index, trainId, GetVertexValue(v, train, phase));
                }

                // set neighbors, now regardless of sink property
                // this is covered by the vertex attribute
                // and required for startvertices that are sinks themselves
                if (useMultiGraph)
                {
                    ICollection<NetworkEdge> edges = rcGraph.EdgesOf(v);
                    int e = 0;
                    int[] edgesArray = new int[edges.Count];
                    foreach (NetworkEdge edge in edges)
                    {
                        edgesArray[e++] = rcEdges.IndexOf(edge);
                    }
                    // sort by order on edges
                    Array.Sort(edgesArray, 0, e);
                    // define according vertices
                    int[] neighborsArray = new int[e];
                    for (int j = 0; j < e; j++)
                    {
                        NetworkVertex toVertex = rcGraph.GetOppositeVertex(v, rcEdges[edgesArray[j]]);
                        neighborsArray[j] = rcVertices.IndexOf(toVertex);
                    }
                    rc.SetVertexNeighbors(index, neighborsArray, edgesArray);
                }
                else
                {
                    ICollection<NetworkVertex> neighbors = rcGraph.NeighborsOf(v);
                    int j = 0;
                    int[] neighborsArray = new int[neighbors.Count];
                    foreach (NetworkVertex n in neighbors)
                    {
                        neighborsArray[j++] = rcVertices.IndexOf(n);
                    }
                    // sort by value orderboolean activatePrediction
                    Array.Sort(neighborsArray, 0, j);
                    //Arrays.sort(neighborsArray, 0, j);
                    // define according edges
                    int[] edgesArray = new int[j];
                    for (int e = 0; e < j; e++)
                    {
                        NetworkVertex toVertex = rcVertices[neighborsArray[e]];
                        edgesArray[e] = rcEdges.IndexOf(rcGraph.GetEdge(v, toVertex));
                    }
                    rc.SetVertexNeighbors(index, neighborsArray, edgesArray);
                }
            }

            // set startVertexes
            int startVertexId = 0;
            int[] sv = new int[startVertices.Count];
            foreach (NetworkVertex startVertex in startVertices)
            {
                sv[startVertexId++] = rcVertices.IndexOf(startVertex);
            }
            Array.Sort(sv);
            //Arrays.sort(sv); // sort by value order 
            rc.SetStartVertexes(sv);

            // set edges
            for (int loop = 0; loop < rcEdges.Count; loop++)
            {
                // prepare values
                NetworkEdge e = rcEdges[loop];
                bool greedy = e.IsGreedy;
                int distance = e.Distance;
                rc.SetEdge(loop, greedy, distance);
            }

            // set trains, check for H-trains
            for (int loop = 0; loop < trains.Count; loop++)
            {
                NetworkTrain train = trains[loop];
                train.AddToRevenueCalculator(rc, loop);
            }

            // set vertex sets
            foreach (VertexVisit visit in vertexVisitSets)
            {
                int j = 0;
                int[] setArray = new int[visit.set.Count];
                foreach (NetworkVertex n in visit.set)
                {
                    setArray[j++] = rcVertices.IndexOf(n);
                }
                rc.SetVisitSet(setArray);
            }
            log.Info("RA: rcVertices:" + rcVertices);
            log.Info("RA: rcEdges:" + rcEdges);

            // set revenue bonuses
            int id = 0;
            foreach (RevenueBonus bonus in revenueBonuses)
            {
                if (bonus.AddToRevenueCalculator(rc, id, rcVertices, trains, phase)) id++;
            }

            log.Info("RA: edgeTravelSets:" + edgeTravelSets);

            // set edge sets
            if (useMultiGraph)
            {
                foreach (NetworkEdge edge in edgeTravelSets.Keys)
                {
                    EdgeTravel edgeTravel = edgeTravelSets[edge];
                    int j = 0;
                    int[] setArray = new int[edgeTravel.set.Count];
                    foreach (NetworkEdge n in edgeTravel.set)
                    {
                        setArray[j++] = rcEdges.IndexOf(n);
                    }
                    ((RevenueCalculatorMulti)rc).SetTravelSet(rcEdges.IndexOf(edge), setArray);
                }
            }


            // activate dynamic modifiers
            rc.SetDynamicModifiers(hasDynamicModifiers);
        }

        public int GetVertexValue(NetworkVertex vertex, NetworkTrain train, Phase phase)
        {

            // base value
            int value = vertex.GetValueByTrain(train);

            // add potential revenueBonuses
            foreach (RevenueBonus bonus in revenueBonuses)
            {
                if (bonus.CheckSimpleBonus(vertex, train.RailsTrain, phase))
                {
                    value += bonus.Value;
                }
            }

            return value;
        }

        public string GetVertexValueAsString(NetworkVertex vertex, NetworkTrain train, Phase phase)
        {
            StringBuilder s = new StringBuilder();

            // base value
            s.Append(vertex.GetValueByTrain(train));

            // add potential revenueBonuses
            foreach (RevenueBonus bonus in revenueBonuses)
            {
                if (bonus.CheckSimpleBonus(vertex, train.RailsTrain, phase))
                {
                    s.Append("+" + bonus.Value);
                }
            }
            return s.ToString();
        }


        private List<RevenueTrainRun> ConvertRcRun(int[,] rcRun)
        {

            List<RevenueTrainRun> convertRun = new List<RevenueTrainRun>();

            for (int j = 0; j < rcRun.Length; j++)
            {
                RevenueTrainRun trainRun = new RevenueTrainRun(this, trains[j]);
                convertRun.Add(trainRun);

                if (rcEdges.Count == 0) continue;
                for (int v = 0; v < /*rcRun[j].Length*/rcRun.GetLength(1); v++)
                {
                    int id = rcRun[j, v];
                    if (id == -1) break;
                    if (useMultiGraph)
                    {
                        trainRun.AddEdge(rcEdges[id]);
                    }
                    else
                    {
                        trainRun.AddVertex(rcVertices[id]);
                    }
                }
                if (useMultiGraph)
                {
                    trainRun.ConvertEdgesToVertices();
                }
                else
                {
                    trainRun.ConvertVerticesToEdges();
                }
            }
            return convertRun;
        }

        public int CalculateRevenue()
        {
            // allows (one) dynamic modifiers to have their own revenue calculation method
            // TODO: Still to be added 
            //        if (hasDynamicCalculator) {
            //            return revenueManager.revenueFromDynamicCalculator(this);
            //        } else { // otherwise standard calculation
            return CalculateRevenue(0, trains.Count - 1);
            //        }
        }

        public int CalculateRevenue(int startTrain, int finalTrain)
        {
            if (startTrain < 0 || finalTrain >= trains.Count || startTrain > finalTrain)
            {
                return 0;
            }
            // the optimal run might change
            optimalRun = null;
            rc.InitRuns(startTrain, finalTrain);
            rc.ExecutePredictions(startTrain, finalTrain);
            int value = rc.CalculateRevenue(startTrain, finalTrain);
            return value;
        }

        public List<RevenueTrainRun> GetOptimalRun()
        {
            if (optimalRun == null)
            {
                optimalRun = ConvertRcRun(rc.GetOptimalRun());
                if (hasDynamicModifiers)
                {
                    revenueManager.AdjustOptimalRun(optimalRun);
                }
            }
            return optimalRun;
        }

        public List<RevenueTrainRun> GetCurrentRun()
        {
            return ConvertRcRun(rc.GetCurrentRun());
        }

        /**
         * is called by rc for dynamic evaluations
         */
        public int DynamicEvaluation()
        {
            int value = 0;
            if (hasDynamicModifiers)
            {
                value = revenueManager.EvaluationValue(GetCurrentRun(), false);
            }
            return value;
        }

        /**
         * is called by rc for dynamic predictions
         */
        public int DynamicPrediction()
        {
            int value = 0;
            if (hasDynamicModifiers)
            {
                value = revenueManager.PredictionValue(GetCurrentRun());
            }
            return value;
        }

        public event EventHandler<IRevenueListenerArgs> RevenueListenerEvent;
        // use the event
        //public void AddRevenueListener(IRevenueListener listener)
        //{
        //    this.revenueListener = listener;
        //}

        public class RevenueListenerArgs : IRevenueListenerArgs
        {
            public int Revenue { get; set; }
            public bool FinalResult { get; set; }

            public RevenueListenerArgs(int revenue, bool result)
            {
                Revenue = revenue;
                FinalResult = result;
            }
        }

        public void NotifyRevenueListener(int revenue, bool finalResult)
        {
            EventHandler<IRevenueListenerArgs> handler = RevenueListenerEvent;
            if (handler == null) return;

            handler(this, new RevenueListenerArgs(revenue, finalResult));

            //    if (revenueListener == null) return;

            //    EventQueue.invokeLater(
            //            new Runnable()
            //            {
            //                public void run()
            //    {
            //        //listener could have unregistered himself in the meantime
            //        if (revenueListener != null) revenueListener.revenueUpdate(revenue, finalResult);
            //    }
            //});
        }

        public void Run()
        {
            CalculateRevenue(0, trains.Count - 1);
        }

        // use event
        //public void RemoveRevenueListener()
        //{
        //    //only removes revenueListener

        //   revenueListener = null;
        //}


        public string GetOptimalRunPrettyPrint(bool includeDetails)
        {
            List<RevenueTrainRun> listRuns = GetOptimalRun();
            if (listRuns == null) return LocalText.GetText("RevenueNoRun");

            StringBuilder runPrettyPrint = new StringBuilder();
            foreach (RevenueTrainRun run in listRuns)
            {
                runPrettyPrint.Append(run.PrettyPrint(includeDetails));
                if (!includeDetails && run != listRuns[listRuns.Count - 1])
                {
                    runPrettyPrint.Append("; ");
                }
            }
            if (includeDetails)
            {
                if (revenueManager != null)
                {
                    runPrettyPrint.Append(revenueManager.PrettyPrint(this));
                }
            }
            else
            {
                int dynamicBonuses = 0;
                if (hasDynamicModifiers)
                {
                    dynamicBonuses = revenueManager.EvaluationValue(this.GetOptimalRun(), true);
                }
                if (dynamicBonuses != 0)
                {
                    runPrettyPrint.Append("; " +
                            LocalText.GetText("RevenueBonus", dynamicBonuses));
                }
            }
            return runPrettyPrint.ToString();
        }

        // #FIXME_GUI
#if false
        public void DrawOptimalRunAsPath(HexMap map)
    {
        List<RevenueTrainRun> listRuns = GetOptimalRun();

        List<GeneralPath> pathList = new List<GeneralPath>();
        if (listRuns != null)
        {
            foreach (RevenueTrainRun run in listRuns)
            {
                pathList.Add(run.GetAsPath(map));
            }
        }
        map.SetTrainPaths(pathList);
    }
#endif

        override public string ToString()
        {
            StringBuilder buffer = new StringBuilder();
            buffer.Append("RevenueCalculator:\n" + rc + "\n");
            buffer.Append("rcVertices:\n" + rcVertices + "\n");
            buffer.Append("rcEdges:\n" + rcEdges + "\n");
            buffer.Append("startVertices:" + startVertices);
            return buffer.ToString();
        }
    }
}
