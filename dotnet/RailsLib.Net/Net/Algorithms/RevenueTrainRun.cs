using GameLib.Net.Common;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Links the results from the revenue calculator to the rails program Each
 * object defines the run of one train
 * 
 */

namespace GameLib.Net.Algorithms
{
    public class RevenueTrainRun : IComparable<RevenueTrainRun>
    {
        private const int PRETTY_PRINT_LENGTH = 100;
        private const int PRETTY_PRINT_INDENT = 10;

        protected static Logger<RevenueTrainRun> log = new Logger<RevenueTrainRun>();

        // definitions
        private RevenueAdapter revenueAdapter;
        private NetworkTrain train;

        // converted data
        private List<NetworkVertex> vertices;
        private List<NetworkEdge> edges;

        public RevenueTrainRun(RevenueAdapter revenueAdapter, NetworkTrain train)
        {
            this.revenueAdapter = revenueAdapter;
            this.train = train;
            vertices = new List<NetworkVertex>();
            edges = new List<NetworkEdge>();
        }

        public List<NetworkVertex> RunVertices
        {
            get
            {
                return vertices;
            }
        }

        /**
         * returns true if train has a valid run (at least two vertices)
         */
        public bool HasAValidRun
        {
            get
            {
                return vertices.Count >= 2;
            }
        }

        /**
         * returns the vertex of the initial base token of the train run
         */
        public NetworkVertex BaseVertex
        {
            get

            {
                return vertices[0];
            }
        }

        /**
         * returns the first vertex of a train run
         */
        public NetworkVertex GetFirstVertex()
        {
            NetworkVertex startVertex = null;
            NetworkVertex firstVertex = null;
            foreach (NetworkVertex vertex in vertices)
            {
                if (startVertex == vertex) return firstVertex;
                if (startVertex == null) startVertex = vertex;
                firstVertex = vertex;
            }
            return startVertex;
        }

        /**
         * returns the last vertex of a train run
         */
        public NetworkVertex GetLastVertex()
        {
            return vertices[vertices.Count - 1];
        }

        public HashSet<NetworkVertex> GetUniqueVertices()
        {
            return new HashSet<NetworkVertex>(vertices);
        }

        public NetworkTrain Train
        {
            get
            {
                return train;
            }
        }

        /**
         * @param listOfVertices defines a sublist of vertices that are used to calculate the run value
         * @return total value of the vertices in the list
         * This includes all revenue bonuses defined in the calculator
         */
        public int GetRunValueForVertices(List<NetworkVertex> listOfVertices)
        {
            int value = 0;
            NetworkVertex startVertex = null;
            foreach (NetworkVertex vertex in listOfVertices)
            {
                if (startVertex == vertex) continue;
                if (startVertex == null) startVertex = vertex;
                value += revenueAdapter.GetVertexValue(vertex, train, revenueAdapter.Phase);
            }
            // check revenueBonuses (complex)
            foreach (RevenueBonus bonus in revenueAdapter.RevenueBonuses)
            {
                if (bonus.CheckComplexBonus(listOfVertices, train.RailsTrain, revenueAdapter.Phase))
                {
                    value += bonus.Value;
                }
            }
            return value;
        }

        public int GetRunValue()
        {
            return GetRunValueForVertices(vertices);
        }

        bool HasBottomRun()
        {
            bool bottomRun = false;
            NetworkVertex startVertex = null;
            foreach (NetworkVertex vertex in vertices)
            {
                if (startVertex == vertex) bottomRun = true;
                if (startVertex == null) startVertex = vertex;
            }
            return bottomRun;
        }

        public void AddVertex(NetworkVertex vertex)
        {
            vertices.Add(vertex);
        }

        public void AddEdge(NetworkEdge edge)
        {
            edges.Add(edge);
        }

        /** defines the vertices from the list of edges */
        public void ConvertEdgesToVertices()
        {
            vertices = new List<NetworkVertex>();

            // check for empty edges
            if (edges.Count == 0)
            {
                return;
            }
            else if (edges.Count == 1)
            {
                // and for 1-edge routes
                vertices.Add(edges[0].Source);
                vertices.Add(edges[0].Target);
                return;
            }

            // figure out, what are the vertices contained
            NetworkEdge previousEdge = null;
            NetworkVertex startVertex = null;
            foreach (NetworkEdge edge in edges)
            {
                log.Debug("Processing edge " + edge.ToFullInfoString());
                // process startEdge
                if (previousEdge == null)
                {
                    previousEdge = edge;
                    continue;
                }
                // check if the current edge has a common vertex with the previous
                // one => continuous route
                NetworkVertex commonVertex = edge.GetCommonVertex(previousEdge);
                // identify start vertex first
                if (startVertex == null)
                {
                    // if there is a joint route => other vertex of previousEdge
                    if (commonVertex != null)
                    {
                        log.Debug("Head Run");
                        startVertex = previousEdge.GetOtherVertex(commonVertex);
                        vertices.Add(startVertex);
                        vertices.Add(commonVertex);
                    }
                    else
                    {
                        // otherwise it is a mistake
                        log.Error("Error in revenue train run: cannot identify startVertex");
                    }
                }
                else
                { // start vertex is known
                  // if there is a common vertex => continuous route
                    if (commonVertex != null)
                    {
                        log.Debug("Added common vertex");
                        vertices.Add(commonVertex);
                    }
                    else
                    {
                        // otherwise it is bottom run
                        // add the last vertex of the head train
                        log.Debug("Bottom Run");
                        vertices.Add(previousEdge.GetOtherVertex(vertices[vertices.Count - 1]));
                        vertices.Add(startVertex);
                    }
                }
                previousEdge = edge;
            }
            // add the last vertex of the route
            vertices.Add(previousEdge.GetOtherVertex(vertices[vertices.Count - 1]));
            log.Debug("Converted edges to vertices " + vertices);
        }

        /** defines the edges from the list of vertices */
        public void ConvertVerticesToEdges()
        {
            edges = new List<NetworkEdge>();

            // check for empty or only one vertices
            if (vertices.Count <= 1)
            {
                return;
            }

            NetworkVertex startVertex = null;
            NetworkVertex previousVertex = null;
            foreach (NetworkVertex vertex in vertices)
            {
                if (startVertex == null)
                {
                    startVertex = vertex;
                    previousVertex = vertex;
                    continue;
                }
                // return to startVertex needs no edge
                if (vertex != startVertex)
                {
                    NetworkEdge edge = revenueAdapter.RCGraph.GetEdge(previousVertex, vertex);
                    if (edge != null)
                    {
                        // found edge between vertices
                        edges.Add(edge);
                    }
                    else
                    {
                        // otherwise it is a mistake
                        log.Error("Error in revenue train run: cannot find according edge");
                    }
                }
                previousVertex = vertex;
            }
        }

        private string PrettyPrintHexName(NetworkVertex vertex)
        {
            if (vertex.IsVirtual)
            {
                return vertex.Identifier;
            }
            else
            {
                return vertex.Hex.Id;
            }
        }

        private int PrettyPrintNewLine(StringBuilder runPrettyPrint, int multiple, int initLength)
        {
            int length = runPrettyPrint.Length - initLength;
            if (length / PRETTY_PRINT_LENGTH != multiple)
            {
                multiple = length / PRETTY_PRINT_LENGTH;
                runPrettyPrint.Append("\n");
                for (int i = 0; i < PRETTY_PRINT_INDENT; i++)
                    runPrettyPrint.Append(" ");
            }
            return multiple;
        }

        public string PrettyPrint(bool includeDetails)
        {
            StringBuilder runPrettyPrint = new StringBuilder();
            runPrettyPrint.Append(LocalText.GetText("N_Train", train.ToString()));
            runPrettyPrint.Append(" = " + GetRunValue());
            if (includeDetails)
            {
                // details of the run
                HashSet<NetworkVertex> uniqueVertices = GetUniqueVertices();
                int majors =
                        NetworkVertex.NumberOfVertexType(uniqueVertices,
                                NetworkVertex.VertexTypes.STATION, NetworkVertex.StationTypes.MAJOR);
                int minors =
                        NetworkVertex.NumberOfVertexType(uniqueVertices,
                                NetworkVertex.VertexTypes.STATION, NetworkVertex.StationTypes.MINOR);
                if (train.IgnoresMinors || minors == 0)
                {
                    runPrettyPrint.Append(LocalText.GetText(
                            "RevenueStationsIgnoreMinors", majors));
                }
                else
                {
                    runPrettyPrint.Append(LocalText.GetText("RevenueStations", majors, minors));
                }
                int initLength = runPrettyPrint.Length;
                int multiple = PrettyPrintNewLine(runPrettyPrint, -1, initLength);
                string currentHexName = null;
                NetworkVertex startVertex = null;
                foreach (NetworkVertex vertex in vertices)
                {
                    if (startVertex == null)
                    {
                        currentHexName = PrettyPrintHexName(vertex);
                        startVertex = vertex;
                        runPrettyPrint.Append(PrettyPrintHexName(vertex) + "(");
                    }
                    else if (startVertex == vertex)
                    {
                        currentHexName = PrettyPrintHexName(vertex);
                        runPrettyPrint.Append(") / ");
                        multiple = PrettyPrintNewLine(runPrettyPrint, multiple, initLength);
                        runPrettyPrint.Append(PrettyPrintHexName(vertex) + "(0");
                        continue;
                    }
                    else if (!currentHexName.Equals(PrettyPrintHexName(vertex)))
                    {
                        currentHexName = PrettyPrintHexName(vertex);
                        runPrettyPrint.Append("), ");
                        multiple = PrettyPrintNewLine(runPrettyPrint, multiple, initLength);
                        runPrettyPrint.Append(PrettyPrintHexName(vertex) + "(");
                    }
                    else
                    {
                        runPrettyPrint.Append(",");
                    }
                    // TODO: Allow more options for pretty print, depending on route
                    // structure etc.
                    runPrettyPrint.Append(revenueAdapter.GetVertexValueAsString(
                            vertex, train, revenueAdapter.Phase));
                    // if (vertex.isStation()) {
                    // runPrettyPrint.append(revenueAdapter.getVertexValueAsString(vertex,
                    // train, revenueAdapter.getPhase()));
                    // } else {
                    // runPrettyPrint.append(vertex.getHex().getOrientationName(vertex.getSide()));
                    // }
                }

                if (currentHexName != null)
                {
                    runPrettyPrint.Append(")");
                }

                // check revenueBonuses (complex)
                List<RevenueBonus> activeBonuses = new List<RevenueBonus>();
                foreach (RevenueBonus bonus in revenueAdapter.RevenueBonuses)
                {
                    if (bonus.CheckComplexBonus(vertices, train.RailsTrain, revenueAdapter.Phase))
                    {
                        activeBonuses.Add(bonus);
                    }
                }
                Dictionary<string, int> printBonuses = RevenueBonus.CombineBonuses(activeBonuses);
                foreach (string bonusName in printBonuses.Keys)
                {
                    runPrettyPrint.Append(" + ");
                    runPrettyPrint.Append(bonusName + "("
                                          + printBonuses[bonusName] + ")");
                    multiple = PrettyPrintNewLine(runPrettyPrint, multiple, initLength);
                }
                runPrettyPrint.Append("\n");
            }

            return runPrettyPrint.ToString();
        }

        // #FIXME_GUI
#if false
        GeneralPath getAsPath(HexMap map)
        {
            GeneralPath path = new GeneralPath();
            foreach (NetworkEdge edge in edges)
            {
                // check vertices if they exist as points and start from there
                List<NetworkVertex> edgeVertices = edge.GetVertexPath();
                bool initPath = false;
                foreach (NetworkVertex edgeVertex in edgeVertices)
                {
                    PointF edgePoint = NetworkVertex.GetVertexPoint2D(map, edgeVertex);
                    if (edgePoint == null) continue;
                    if (!initPath)
                    {
                        path.MoveTo((float)edgePoint.getX(),
                                (float)edgePoint.getY());
                        initPath = true;
                    }
                    else
                    {
                        path.LineTo((float)edgePoint.getX(),
                                (float)edgePoint.getY());
                    }
                }
            }
            return path;
        }
#endif

        public int CompareTo(RevenueTrainRun other)
        {
            return GetRunValue().CompareTo(other.GetRunValue());
        }
    }
}
