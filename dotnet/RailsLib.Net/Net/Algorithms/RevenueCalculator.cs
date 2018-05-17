using GameLib.Net.Common;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    abstract public class RevenueCalculator
    {
        protected int nbVertexes;
        protected int nbTrains;
        protected int nbEdges;
        protected int nbBonuses;

        // static vertex data
        protected int[,] vertexValueByTrain; // dimensions: vertexId, trainId
        protected bool[] vertexMajor;
        protected bool[] vertexMinor;
        protected bool[] vertexSink;
        protected int[] vertexNbNeighbors;
        protected int[] vertexNbVisitSets;
        protected int[] vertexNbBonusSets;

        protected int[,] vertexNeighbors;
        protected int[,] vertexEdges;
        protected int[,] vertexVisitSets; // vertex belongs to a visit set, dimension: nbVertex x maxVertexSets
        protected int[,] vertexBonusSets; // vertex belongs to a bonus set, dimension: nbVertex x nbBonuses

        // start vertexes
        protected int[] startVertexes;

        // static edge data
        protected bool[] edgeGreedy;
        protected int[] edgeDistance;

        // static train data
        protected int[] trainMaxMajors;
        protected int[] trainMaxMinors;
        protected int[] trainMaxBonuses;
        protected bool[] trainIgnoreMinors;
    protected bool[] trainIsH; // true => train is H-train
    protected bool[] trainIsE; // true => train is Express-train
    
    // dynamic train data
        protected int[] trainCurrentValue;
        protected int[] trainMajors;
        protected int[] trainMinors;
        protected int[] trainBonuses; // counts the number of bonuses received
        protected bool[,] trainVisited;
        protected int[,] trainStack; // store either vertices or edges 
        protected int[] trainStackPos;
        protected bool[] trainBottomActive;
        protected int[] trainStartEdge;
        protected int[] trainDistance; // keeps track of distance travelled (for H-trains)

        // static bonus data
        protected int[] bonusValue;
        protected bool[,] bonusActiveForTrain; // dimensions: bonus x train
        protected int[] bonusRequiresVertices;

        // dynamic bonus data
        protected int[,] bonusTrainVertices;

        // run settings
        protected int startTrainSet;
        protected int finalTrainSet;
        protected int startTrain;
        protected int finalTrain;
        protected bool useRevenuePrediction;

        // current best run results
        protected int currentBestValue;
        protected int[,] currentBestRun;

        // prediction data
        protected int[] maxCumulatedTrainRevenues;
        protected int[,] maxMajorRevenues; // dimensions trainId x nb vertex; 
        protected int[,] maxMinorRevenues; // dimensions trainId x nb vertex;
        protected int[,] maxBonusRevenues; // dimensions trainId x nb bonuses

        // statistic data
        protected int countVisits;
        protected int countEdges;
        protected int nbEdgesTravelled;
        protected int nbEvaluations;
        protected int nbPredictions;

        // revenue Adapter
        protected RevenueAdapter revenueAdapter;

        // activate dynamic revenue modifiers
        protected bool callDynamicModifiers;

        // termination results
        protected enum Terminated
        {
            WithEvaluation,
            WithoutEvaluation,
            NotYet
        }

        protected static Logger<RevenueCalculator> log = new Logger<RevenueCalculator>();

        // These two copy functions will copy a 1D array into a row of a 2D array
        // They do the equivalent of:
        // int[,] array2D;
        // int[] array1D;
        // array2D[row] = array1D;
        static void CopyIntRow(int[] src, int[,] dst, int row)
        {
            Buffer.BlockCopy(src, 0, dst,
                row * dst.GetLength(1) * sizeof(int), src.Length * sizeof(int));
        }
        static void CopyBoolRow(bool[] src, bool[,] dst, int row)
        {
            Buffer.BlockCopy(src, 0, dst,
                row * dst.GetLength(1) * sizeof(bool), src.Length * sizeof(bool));
        }

        public RevenueCalculator(RevenueAdapter revenueAdapter, int nbVertexes, int nbEdges,
            int maxNeighbors, int maxVertexSets, int maxEdgeSets, int nbTrains, int nbBonuses)
        {

            log.Info("RC defined: nbVertexes = " + nbVertexes + ", nbEdges = " + nbEdges + ", maxNeighbors = " + maxNeighbors +
                    ", maxVertexSets = " + maxVertexSets + ", maxEdgeSets = " + maxEdgeSets + ", nbTrains = " + nbTrains + ", nbBonuses = " + nbBonuses);

            this.revenueAdapter = revenueAdapter;
            this.nbVertexes = nbVertexes;
            this.nbEdges = nbEdges;
            this.nbTrains = nbTrains;
            this.nbBonuses = nbBonuses;

            // initialize all required variables
            vertexValueByTrain = new int[nbVertexes, nbTrains];
            vertexMajor = new bool[nbVertexes];
            vertexMinor = new bool[nbVertexes];
            vertexSink = new bool[nbVertexes];
            vertexNbNeighbors = new int[nbVertexes];
            vertexNbVisitSets = new int[nbVertexes];
            vertexNbBonusSets = new int[nbVertexes];
            vertexNeighbors = new int[nbVertexes, maxNeighbors];
            vertexEdges = new int[nbVertexes, maxNeighbors];
            vertexVisitSets = new int[nbVertexes, maxVertexSets];
            vertexBonusSets = new int[nbVertexes, nbBonuses];

            edgeGreedy = new bool[nbEdges];
            edgeDistance = new int[nbEdges];

            trainMaxMajors = new int[nbTrains];
            trainMaxMinors = new int[nbTrains];
            trainMaxBonuses = new int[nbTrains]; // only required for revenue prediction
            trainIgnoreMinors = new bool[nbTrains];
            trainIsH = new bool[nbTrains];
            trainIsE = new bool[nbTrains];

            trainCurrentValue = new int[nbTrains];
            trainMajors = new int[nbTrains];
            trainMinors = new int[nbTrains];
            trainBonuses = new int[nbTrains];
            trainVisited = new bool[nbTrains, nbVertexes];
            // increase necessary due to bottom train
            trainStack = new int[nbTrains, nbVertexes + 1];
            trainStackPos = new int[nbTrains];
            trainBottomActive = new bool[nbTrains];
            trainStartEdge = new int[nbTrains];
            trainDistance = new int[nbTrains];
            maxCumulatedTrainRevenues = new int[nbTrains];

            bonusValue = new int[nbBonuses];
            bonusRequiresVertices = new int[nbBonuses];
            bonusActiveForTrain = new bool[nbBonuses, nbTrains];
            bonusTrainVertices = new int[nbBonuses, nbTrains];

            currentBestRun = new int[nbTrains, nbVertexes + 1];

            useRevenuePrediction = false;

            callDynamicModifiers = false;
        }

        public void SetVertex(int id, bool major, bool minor, bool sink)
        {
            vertexMajor[id] = major;
            vertexMinor[id] = minor;
            vertexSink[id] = sink;
            // default neighbors && visit and bonus sets
            vertexNbNeighbors[id] = 0;
            vertexNbVisitSets[id] = 0;
            vertexNbBonusSets[id] = 0;
        }

        public void SetVertexValue(int vertexId, int trainId, int value)
        {
            vertexValueByTrain[vertexId, trainId] = value;
        }

        public void SetVertexNeighbors(int id, int[] neighbors, int[] edges)
        {
            // copy neighbors
            for (int j = 0; j < neighbors.Length; j++)
            {
                vertexNeighbors[id, j] = neighbors[j];
                vertexEdges[id, j] = edges[j];
            }
            vertexNbNeighbors[id] = neighbors.Length;

        }

        public void SetStartVertexes(int[] startVertexes)
        {
            this.startVertexes = startVertexes;
        }


        virtual public void SetEdge(int edgeId, bool greedy, int distance)
        {
            edgeGreedy[edgeId] = greedy;
            edgeDistance[edgeId] = distance;
            // default travel sets
            //        edgeNbTravelSets[edgeId] = 0;
        }

        public void SetTrain(int id, int majors, int minors, bool ignoreMinors, bool isHTrain, bool isETrain)
        {
            trainMaxMajors[id] = majors;
            trainMaxMinors[id] = minors;
            trainMaxBonuses[id] = 0;
            trainIgnoreMinors[id] = ignoreMinors;
            trainIsH[id] = isHTrain;
            trainIsE[id] = isETrain;
        }

        public void SetVisitSet(int[] vertices)
        {
            for (int j = 0; j < vertices.Length; j++)
            {
                int vertexId = vertices[j];
                for (int k = 0; k < vertices.Length; k++)
                {
                    if (k == j) continue;
                    vertexVisitSets[vertexId, vertexNbVisitSets[vertexId]++] = vertices[k];
                }
            }
        }

        public void SetBonus(int id, int value, int[] vertices, bool[] bonusForTrain)
        {
            log.Info("RC: define bonus value = " + value + ", vertices = " + vertices.ToString() +
                    ", bonusForTrain = " + bonusForTrain.ToString());

            bonusValue[id] = value;
            bonusRequiresVertices[id] = vertices.Length;
            for (int j = 0; j < vertices.Length; j++)
            {
                int vertexId = vertices[j];
                vertexBonusSets[vertexId, vertexNbBonusSets[vertexId]++] = id;
            }
            //bonusActiveForTrain[id] = bonusForTrain;
            CopyBoolRow(bonusForTrain, bonusActiveForTrain, id);
        }

        public void SetDynamicModifiers(bool activate)
        {
            callDynamicModifiers = activate;
        }

        public int[,] GetOptimalRun()
        {
            log.Info("RC: currentBestRun = " + currentBestRun.ToString());
            return currentBestRun;
        }

        public int[,] GetCurrentRun()
        {
            int[,] currentRun = new int[nbTrains, nbVertexes + 1];
            for (int j = startTrainSet; j <= finalTrainSet; j++)
            {
                for (int v = 0; v < nbVertexes + 1; v++)
                {
                    if (v < trainStackPos[j])
                    {
                        currentRun[j, v] = trainStack[j, v];
                    }
                    else
                    {
                        currentRun[j, v] = -1; // terminator
                        break;
                    }
                }
            }
            return currentRun;
        }

        public int GetNumberOfEvaluations()
        {
            return nbEvaluations;
        }

        public string GetStatistics()
        {
            StringBuilder statistics = new StringBuilder();
            statistics.Append(nbEvaluations + " evaluations");
            if (useRevenuePrediction)
                statistics.Append(", " + nbPredictions + " predictions");
            statistics.Append(" and " + nbEdgesTravelled + " edges traveled.");
            return statistics.ToString();
        }

        private void NotifyRevenueAdapter(int revenue, bool finalResult)
        {
            string modifier;
            if (finalResult)
                modifier = "final";
            else
                modifier = "new best";
            log.Info("Report " + modifier + " result of " + revenue + " after " + GetStatistics());
            revenueAdapter.NotifyRevenueListener(revenue, finalResult);
        }

        private int[] BestRevenues(int[] values, int length)
        {
            int[] bestRevenues = new int[length + 1];
            Array.Sort(values);
            int cumulatedRevenues = 0;
            for (int j = 1; j <= length; j++)
            {
                cumulatedRevenues += values[values.Length - j];
                bestRevenues[j] = cumulatedRevenues;
            }
            log.Debug("Best Revenues = " + bestRevenues.ToString());
            return bestRevenues;
        }

        private void InitRevenueValues(int startTrain, int finalTrain)
        {
            // initialize values
            maxMajorRevenues = new int[nbTrains, nbVertexes];
            maxMinorRevenues = new int[nbTrains, nbVertexes];
            maxBonusRevenues = new int[nbTrains, nbVertexes + nbBonuses];
            for (int t = startTrain; t <= finalTrain; t++)
            {
                int[] majorValues = new int[nbVertexes];
                int[] minorValues = new int[nbVertexes];
                int[] bonusValues = new int[nbVertexes + nbBonuses];
                int major = 0, minor = 0, bonus = 0;
                // scan vertices for values
                for (int v = 0; v < nbVertexes; v++)
                {
                    if (vertexValueByTrain[v, t] == 0) continue;
                    if (vertexMajor[v])
                    {
                        majorValues[major++] = vertexValueByTrain[v, t];
                    }
                    else if (vertexMinor[v])
                    {
                        minorValues[minor++] = vertexValueByTrain[v, t];
                    }
                    else
                    { // define it as bonus
                        bonusValues[bonus++] = vertexValueByTrain[v, t];
                    }
                }
                // add the (complex) bonuses
                for (int b = 0; b < nbBonuses; b++)
                {
                    if (bonusValue[b] <= 0 || !bonusActiveForTrain[b, t]) continue;
                    bonusValues[bonus++] = bonusValue[b];
                }
                trainMaxBonuses[t] = bonus;

                //maxMajorRevenues[t] = BestRevenues(majorValues, trainMaxMajors[t]);
                //maxMinorRevenues[t] = BestRevenues(minorValues, trainMaxMinors[t]);
                //maxBonusRevenues[t] = BestRevenues(bonusValues, trainMaxBonuses[t]);
                CopyIntRow(BestRevenues(majorValues, trainMaxMajors[t]), maxMajorRevenues, t);
                CopyIntRow(BestRevenues(minorValues, trainMaxMinors[t]), maxMajorRevenues, t);
                CopyIntRow(BestRevenues(bonusValues, trainMaxBonuses[t]), maxMajorRevenues, t);
                // initially the cumulated train revenues are the individual run revenues
                int trainRevenues = maxMajorRevenues[t, trainMaxMajors[t]] + maxMinorRevenues[t, trainMaxMinors[t]]
                         + maxBonusRevenues[t, trainMaxBonuses[t]];
                maxCumulatedTrainRevenues[t] = trainRevenues;
            }
            log.Info("maxMajorRevenues = " + maxMajorRevenues.ToString());
            log.Info("maxMinorRevenues = " + maxMinorRevenues.ToString());
            log.Info("maxBonusRevenues = " + maxBonusRevenues.ToString());
            log.Info("maxCumulatedTrainRevenues = " + maxCumulatedTrainRevenues.ToString());
        }

        public void InitRuns(int startTrain, int finalTrain)
        {
            log.Info("RC: init runs from " + startTrain + " to " + finalTrain);
            if (startTrain > finalTrain) return;

            this.startTrainSet = startTrain;
            this.finalTrainSet = finalTrain;

            // initialize all trains and currentValues
            for (int i = startTrain; i < finalTrain; i++)
            {
                currentBestRun[i, 0] = -1;
            }
            currentBestValue = 0;

        }

        public void ExecutePredictions(int startTrain, int finalTrain)
        {
            useRevenuePrediction = true;

            if (startTrain > finalTrain) return;

            InitRevenueValues(startTrain, finalTrain);

            if (startTrain == finalTrain)
            {
                return;
            }

            // start prediction runs
            nbEvaluations = 0; nbPredictions = 0; nbEdgesTravelled = 0;

            log.Info("RC: start individual prediction Runs");
            int[] maxSingleTrainRevenues = new int[nbTrains];
            for (int j = startTrain; j <= finalTrain; j++)
            {
                this.startTrain = j;
                this.finalTrain = j;
                currentBestValue = 0;
                RunTrain(j);
                log.Info("RC: Best prediction run of train number " + j + " value = " + currentBestValue +
                    " after " + GetStatistics());
                maxSingleTrainRevenues[j] = currentBestValue;
            }
            int cumulatedRevenues = 0;
            for (int j = finalTrain; j >= startTrain; j--)
            {
                cumulatedRevenues += maxSingleTrainRevenues[j];
                maxCumulatedTrainRevenues[j] = cumulatedRevenues;
            }
            log.Info("maxCumulatedTrainRevenues = " + maxCumulatedTrainRevenues.ToString());

            if (startTrain == finalTrain - 1) return;

            log.Info("RC: start combined prediction runs");
            this.finalTrain = finalTrain;
            for (int j = finalTrain - 1; j > startTrain; j--)
            {
                this.startTrain = j;
                //            currentBestValue = 0;
                RunTrain(j);
                log.Info("RC: Best prediction run until train nb. " + j + " value = " + currentBestValue +
                    " after " + GetStatistics());
                maxCumulatedTrainRevenues[j] = currentBestValue;
                maxCumulatedTrainRevenues[j - 1] = currentBestValue + maxSingleTrainRevenues[j - 1];
                log.Info("maxCumulatedTrainRevenues = " + maxCumulatedTrainRevenues.ToString());
            }
        }

        public int CalculateRevenue(int startTrain, int finalTrain)
        {
            log.Info("RC: calculateRevenue trains from " + startTrain + " to " + finalTrain);

            this.startTrain = startTrain;
            this.finalTrain = finalTrain;

            RunTrain(startTrain);

            // inform revenue listener via adapter
            NotifyRevenueAdapter(currentBestValue, true);

            return currentBestValue;
        }

        abstract protected void RunTrain(int trainId);

        abstract protected void RunBottom(int trainId);

        // next vertex is either:
        // protected void nextVertex(int trainId, int vertexId, bool previousGreedy);
        // protected void nextVertex(int trainId, int vertexId);

        protected bool EncounterVertex(int trainId, int vertexId, bool arrive)
        {
            log.Debug("RC: EncounterVertex, trainId = " + trainId + " vertexId = " + vertexId + " arrive = " + arrive);

            // set visit to true if arriving, otherwise you leave
            trainVisited[trainId, vertexId] = arrive;

            bool stationVertex = false;
            if (arrive)
            {
                trainCurrentValue[trainId] += vertexValueByTrain[vertexId, trainId];
                if (vertexMajor[vertexId])
                {
                    trainMajors[trainId]--;
                    stationVertex = true;
                }
                else if (vertexMinor[vertexId])
                {
                    trainMinors[trainId]--;
                    stationVertex = !trainIgnoreMinors[trainId];
                }
                countVisits++;
            }
            else
            {
                trainCurrentValue[trainId] -= vertexValueByTrain[vertexId, trainId];
                if (vertexMajor[vertexId])
                {
                    trainMajors[trainId]++;
                    stationVertex = true;
                }
                else if (vertexMinor[vertexId])
                {
                    trainMinors[trainId]++;
                    stationVertex = !trainIgnoreMinors[trainId];
                }
                countVisits--;
            }

            // check vertex sets
            for (int j = 0; j < vertexNbVisitSets[vertexId]; j++)
            {
                trainVisited[trainId, vertexVisitSets[vertexId, j]] = arrive;
                log.Debug("RC: visited = " + arrive + " for vertex " + vertexVisitSets[vertexId, j] + " due to block rule");
            }

            // check bonus sets
            for (int j = 0; j < vertexNbBonusSets[vertexId]; j++)
            {
                int bonusId = vertexBonusSets[vertexId, j];
                if (!bonusActiveForTrain[bonusId, trainId]) continue;
                if (arrive)
                {
                    bonusTrainVertices[bonusId, trainId]--;
                    log.Debug("RC: Decreased bonus " + bonusId + " to " + bonusTrainVertices[bonusId, trainId]);
                    if (bonusTrainVertices[bonusId, trainId] == 0)
                    {
                        trainCurrentValue[trainId] += bonusValue[bonusId];
                        if (bonusValue[bonusId] > 0) trainBonuses[trainId]--;
                        log.Debug("RC: Added bonus " + bonusId + " with value " + bonusValue[bonusId]);
                    }
                }
                else
                {
                    if (bonusTrainVertices[bonusId, trainId] == 0)
                    {
                        trainCurrentValue[trainId] -= bonusValue[bonusId];
                        if (bonusValue[bonusId] > 0) trainBonuses[trainId]++;
                        log.Debug("RC: Removed bonus " + bonusId + " with value " + bonusValue[bonusId]);
                    }
                    bonusTrainVertices[bonusId, trainId]++;
                    log.Debug("RC: Increases bonus " + bonusId + " to " + bonusTrainVertices[bonusId, trainId]);
                }
            }

            log.Debug("RC: stationVertex = " + stationVertex);
            log.Debug("RC: Count Visits = " + countVisits);
            return stationVertex;
        }

        // travel edge is either:
        //      protected bool travelEdge(int trainId, int edgeId, bool previousGreedy);
        //      protected bool travelEdge(int trainId, int edgeId);

        abstract protected void ReturnEdge(int trainId, int edgeId);

        virtual protected Terminated TrainTerminated(int trainId)
        {
            Terminated terminated = Terminated.NotYet;
            if (trainIgnoreMinors[trainId])
            {
                // express trains
                if (trainMajors[trainId] == 0)
                    terminated = Terminated.WithEvaluation;
            }
            else
            { // default and plus trains
                if (trainMajors[trainId] < 0)
                {
                    terminated = Terminated.WithoutEvaluation;
                }
                else if (trainMajors[trainId] + trainMinors[trainId] == 0)
                    terminated = Terminated.WithEvaluation;
            }
            if (terminated != Terminated.NotYet)
            {
                log.Debug("RC: Train " + trainId + " has terminated: " +
                        "majors = " + trainMajors[trainId] + " minors = " + trainMinors[trainId]);
            }
            return terminated;
        }

        protected void FinalizeVertex(int trainId, int vertexId)
        {
            log.Debug("RC: Finalize Vertex id " + vertexId + " for train " + trainId);

            if (trainId == finalTrain)
            {
                EvaluateResults();
            }
            else
            {
                RunTrain(trainId + 1);
            }
        }

        protected void EvaluateResults()
        {
            // sum to total value
            int totalValue = 0;
            for (int j = startTrain; j <= finalTrain; j++)
            {
                totalValue += trainCurrentValue[j];
                //            check for two stations requirement not necessary if stationVertex approach works
                //            if (trainIgnoreMinors[j]) { 
                //                if (trainMaxMajors[j] - trainMajors[j] >= 2)
                //                    totalValue += trainCurrentValue[j];
                //            } else {
                //                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
                //                    totalValue += trainCurrentValue[j];
                //            }
            }

            if (callDynamicModifiers) totalValue += revenueAdapter.DynamicEvaluation();

            nbEvaluations++;
            log.Debug("RC: current total value " + totalValue);

            // compare to current best result
            if (totalValue > currentBestValue)
            {
                currentBestValue = totalValue;
                // exceed thus deep copy of vertex stack
                for (int j = startTrainSet; j <= finalTrainSet; j++)
                {
                    for (int v = 0; v < nbVertexes + 1; v++)
                    {
                        if (v < trainStackPos[j])
                        {
                            currentBestRun[j, v] = trainStack[j, v];
                        }
                        else
                        {
                            currentBestRun[j, v] = -1; // terminator
                            break;
                        }
                    }
                }
                log.Info("RC: Found better run with " + totalValue);
                // inform revenue listener via adapter
                NotifyRevenueAdapter(currentBestValue, false);
            }
        }

        // predict revenues and returns true if best value can still be exceeded
        protected bool PredictRevenues(int trainId)
        {
            // the potential revenues of the future trains
            int totalValue = 0;
            if (trainId < finalTrain)
                totalValue = maxCumulatedTrainRevenues[trainId + 1];

            // predict the current train
            int trainValue = trainCurrentValue[trainId];
            if (trainIgnoreMinors[trainId])
            {
                // express train
                trainValue += maxMajorRevenues[trainId, trainMajors[trainId]];
            }
            else
            {
                if (trainMinors[trainId] > 0)
                {
                    trainValue += maxMajorRevenues[trainId, trainMajors[trainId]];
                    trainValue += maxMinorRevenues[trainId, trainMinors[trainId]];
                }
                else
                { // <= 0
                    int trainStations = trainMajors[trainId] + trainMinors[trainId];
                    // trainStations can be zero or negative (for H trains)
                    if (trainStations > 0)
                    {
                        trainValue += maxMajorRevenues[trainId, trainStations];
                    }
                }
            }
            // add potential bonuses
            if (trainBonuses[trainId] != 0)
            {
                trainValue += maxBonusRevenues[trainId, trainBonuses[trainId]];
            }
            log.Debug("RC: Current train has predicted  value of " + trainValue);

            // maximum value for the trainId including future trains
            totalValue = Math.Min(totalValue + trainValue, maxCumulatedTrainRevenues[trainId]);

            // and add the past trains: current realized values
            for (int j = startTrain; j < trainId; j++)
            {
                totalValue += trainCurrentValue[j];
                //        check for two stations requirement not necessary if stationVertex approach works
                //            if (trainIgnoreMinors[j]) { 
                //                if (trainMaxMajors[j] - trainMajors[j] >= 2)
                //                    totalValue += trainCurrentValue[j];
                //            } else {
                //                if (trainMaxMajors[j] + trainMaxMinors[j] - trainMajors[j] - trainMinors[j] >= 2)
                //                    totalValue += trainCurrentValue[j];
                //            }
            }

            if (callDynamicModifiers) totalValue += revenueAdapter.DynamicPrediction();

            nbPredictions++;

            bool terminate = (totalValue <= currentBestValue);
            if (terminate) log.Debug("Run terminated due to predicted value of " + totalValue);

            return terminate;
        }


    override public string ToString()
        {
            StringBuilder buffer = new StringBuilder();

            buffer.Append("vertexValuesByTrain:" + vertexValueByTrain.ToString() + "\n");
            buffer.Append("vertexMajor:" + vertexMajor.ToString() + "\n");
            buffer.Append("vertexMinor:" + vertexMinor.ToString() + "\n");
            buffer.Append("vertexNeighbors:" + vertexNeighbors.ToString() + "\n");
            buffer.Append("vertexEdges:" + vertexEdges.ToString() + "\n");
            buffer.Append("vertexVisitSets:" + vertexVisitSets.ToString() + "\n");
            buffer.Append("vertexBonusSets:" + vertexBonusSets.ToString() + "\n");
            buffer.Append("vertexNbVisitSets:" + vertexNbVisitSets.ToString() + "\n");
            buffer.Append("vertexNbBonusSets:" + vertexNbBonusSets.ToString() + "\n");
            buffer.Append("edgeGreedy:" + edgeGreedy.ToString() + "\n");
            buffer.Append("edgeDistance:" + edgeDistance.ToString() + "\n");
            //        buffer.append("edgeTravelSets:" + edgeTravelSets) + "\n");
            //        buffer.append("egdeNbTravelSets:" + edgeNbTravelSets) + "\n");
            buffer.Append("startVertexes:" + startVertexes.ToString() + "\n");
            buffer.Append("trainMaxMajors:" + trainMaxMajors.ToString() + "\n");
            buffer.Append("trainMaxMinors:" + trainMaxMinors.ToString() + "\n");
            buffer.Append("trainIgnoreMinors:" + trainIgnoreMinors.ToString() + "\n");

            return buffer.ToString();
        }

    }
}
