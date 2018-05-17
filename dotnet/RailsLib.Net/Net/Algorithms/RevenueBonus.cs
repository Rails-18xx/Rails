using GameLib.Net.Common;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Algorithms
{
    public sealed class RevenueBonus
    {
        private static Logger<RevenueBonus> log = new Logger<RevenueBonus>();

        // bonus values
        private int value;

        // bonus name, also identifies mutually exclusive bonuses
        private string name;

        // internal attributes
        private List<NetworkVertex> vertices;
        private List<TrainType> trainTypes;
        private List<Train> trains;
        private List<Phase> phases;

        public RevenueBonus(int value, string name)
        {
            this.value = value;
            this.name = name;

            vertices = new List<NetworkVertex>();
            trainTypes = new List<TrainType>();
            trains = new List<Train>();
            phases = new List<Phase>();
        }

        public void AddVertex(NetworkVertex vertex)
        {
            vertices.Add(vertex);
        }

        public void AddVertices(IEnumerable<NetworkVertex> vertices)
        {
            this.vertices.AddRange(vertices);
        }

        public void AddTrainType(TrainType trainType)
        {
            trainTypes.Add(trainType);
        }

        public void AddTrain(Train train)
        {
            trains.Add(train);
        }

        public void AddPhase(Phase phase)
        {
            phases.Add(phase);
        }

        public int Value
        {
            get
            {
                return value;
            }
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        public List<NetworkVertex> Vertices
        {
            get
            {
                return vertices;
            }
        }

        public List<TrainType> TrainTypes
        {
            get
            {
                return trainTypes;
            }
        }

        public List<Train> Trains
        {
            get
            {
                return trains;
            }
        }

        public List<Phase> Phases
        {
            get
            {
                return phases;
            }
        }

        public bool IsSimpleBonus
        {
            get
            {
                return (vertices.Count == 1);
            }
        }

        public bool AddToRevenueCalculator(RevenueCalculator rc, int bonusId, List<NetworkVertex> allVertices, List<NetworkTrain> trains, Phase phase)
        {
            if (IsSimpleBonus || (phases.Count > 0) && !phases.Contains(phase)) return false;
            // only non-simple bonuses and checks phase condition

            int[] verticesArray = new int[vertices.Count];
            for (int j = 0; j < vertices.Count; j++)
            {
                if (!allVertices.Contains(vertices[j])) return false; // if vertex is not on graph, do not add bonus
                verticesArray[j] = allVertices.IndexOf(vertices[j]);
            }

            bool[] trainsArray = new bool[trains.Count];
            for (int j = 0; j < trains.Count; j++)
            {
                trainsArray[j] = CheckConditions(trains[j].RailsTrain, phase);
            }

            log.Info("Add revenueBonus to RC, id = " + bonusId + ", bonus = " + this);

            rc.SetBonus(bonusId, value, verticesArray, trainsArray);

            return true;
        }

        public bool CheckSimpleBonus(NetworkVertex vertex, Train train, Phase phase)
        {
            return (IsSimpleBonus && vertices.Contains(vertex) && CheckConditions(train, phase));
        }

        public bool CheckComplexBonus(List<NetworkVertex> visitVertices, Train train, Phase phase)
        {
            bool result = !IsSimpleBonus && CheckConditions(train, phase);
            if (result)
            {
                foreach (NetworkVertex vertex in vertices)
                {
                    if (!visitVertices.Contains(vertex))
                    {
                        result = false;
                        break;
                    }
                }
            }
            return result;
        }

        public bool CheckConditions(Train train, Phase phase)
        {
            bool result = true;

            // check train
            if (trains.Count > 0)
            {
                if (train == null)
                {
                    result = false;
                }
                else
                {
                    result = result && trains.Contains(train);
                }
            }

            // check trainTypes
            if (trainTypes.Count > 0)
            {
                if (train == null)
                {
                    result = false;
                }
                else
                {
                    result = result && trainTypes.Contains(train.GetTrainType());
                }
            }

            // check phase
            if (phases.Count > 0)
            {
                if (phase == null)
                {
                    result = false;
                }
                else
                {
                    result = result && phases.Contains(phase);
                }
            }
            return result;
        }

        override public string ToString()
        {
            StringBuilder s = new StringBuilder();
            s.Append("RevenueBonus");
            if (name == null)
                s.Append(" unnamed");
            else
                s.Append(" name = " + name);
            s.Append(", value " + value);
            s.Append(", vertices = " + vertices);
            s.Append(", trainTypes = " + trainTypes);
            s.Append(", phases = " + phases);
            return s.ToString();
        }

        public static Dictionary<string, int> CombineBonuses(IEnumerable<RevenueBonus> bonuses)
        {
            Dictionary<string, int> combined = new Dictionary<string, int>();
            foreach (RevenueBonus bonus in bonuses)
            {
                string name = bonus.Name;
                if (combined.ContainsKey(name))
                {
                    combined[name] = combined[name] + bonus.Value;
                }
                else
                {
                    combined[name] = bonus.Value;
                }
            }
            return combined;
        }

        public static int GetNumberNonSimpleBonuses(IEnumerable<RevenueBonus> bonuses)
        {
            int number = 0;
            foreach (RevenueBonus bonus in bonuses)
            {
                if (!bonus.IsSimpleBonus) number++;
            }
            return number;
        }
    }
}
