using GameLib.Net.Common;
using GameLib.Net.Game;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Algorithms
{
    public sealed class NetworkTrain : IComparable<NetworkTrain>
    {
        private static Logger<NetworkTrain> log = new Logger<NetworkTrain>();

        private int majors;
        private int minors;
        private bool ignoreMinors;
        private int multiplyMajors;
        private int multiplyMinors;
        private bool isHTrain;
        private bool isETrain;
        private string trainName;
        private Train railsTrain;


        private NetworkTrain(int majors, int minors, bool ignoreMinors,
                int multiplyMajors, int multiplyMinors, bool isHTrain, bool isETrain, string trainName, Train train)
        {
            this.majors = majors;
            this.minors = minors;
            this.ignoreMinors = ignoreMinors;
            this.multiplyMajors = multiplyMajors;
            this.multiplyMinors = multiplyMinors;
            this.isHTrain = isHTrain;
            this.isETrain = isETrain;
            this.trainName = trainName;
            this.railsTrain = train;
            log.Info("Created NetworkTrain " + ToString() + " / " + Attributes);
        }

        public static NetworkTrain CreateFromRailsTrain(Train railsTrain)
        {
            int majors = railsTrain.MajorStops;
            int minors = railsTrain.MinorStops;
            if (railsTrain.TownCountIndicator == 0)
            {
                minors = 999;
            }
            int multiplyMajors = railsTrain.CityScoreFactor;
            int multiplyMinors = railsTrain.TownScoreFactor;
            bool ignoreMinors = false;
            if (multiplyMinors == 0)
            {
                ignoreMinors = true;
            }
            bool isHTrain = railsTrain.IsHTrain;
            bool isETrain = railsTrain.IsETrain;
            string trainName = railsTrain.ToText();

            return new NetworkTrain(majors, minors, ignoreMinors, multiplyMajors, multiplyMinors,
                    isHTrain, isETrain, trainName, railsTrain);
        }

        public static NetworkTrain CreateFromString(string trainString)
        {
            string t = trainString.Trim();
            int cities = 0; int towns = 0;
            bool ignoreTowns = false; int multiplyCities = 1; int multiplyTowns = 1;
            bool isHTrain = false;
            bool isETrain = false;
            if (t.Equals("D"))
            {
                log.Info("RA: found Diesel train");
                cities = 99;
            }
            else if (t.Equals("TGV"))
            {
                log.Info("RA: found TGV  train");
                cities = 3;
                ignoreTowns = true;
                multiplyCities = 2;
                multiplyTowns = 0;
            }
            else if (t.Contains("+"))
            {
                log.Info("RA: found Plus train");
                cities = int.Parse(Regex.Split(t, "\\+")[0]); // + train
                towns = int.Parse(Regex.Split(t, "\\+")[1]);
            }
            else if (t.Contains("E"))
            {
                log.Info("RA: found Express train");
                //cities = Integer.parseInt(t.replace("E", ""));
                ignoreTowns = true;
                isETrain = true;
                multiplyTowns = 0;
                cities = 99; //for now in 1880, specific implementation in ExpressTrainModifier
            }
            else if (t.Contains("D"))
            {
                log.Info("RA: found Double Express train");
                cities = int.Parse(t.Replace("D", ""));
                ignoreTowns = true;
                isETrain = true;
                multiplyCities = 2;
                multiplyTowns = 0;
            }
            else if (t.Contains("H"))
            {
                log.Info("RA: found Hex train");
                cities = int.Parse(t.Replace("H", ""));
                isHTrain = true;
            }
            else
            {
                log.Info("RA: found Default train");
                cities = int.Parse(t);
            }
            NetworkTrain train = new NetworkTrain(cities, towns, ignoreTowns, multiplyCities,
                    multiplyTowns, isHTrain, isETrain, t, null);
            return train;
        }

        public void AddToRevenueCalculator(RevenueCalculator rc, int trainId)
        {
            rc.SetTrain(trainId, majors, minors, ignoreMinors, isHTrain, isETrain);
        }

        public int Majors
        {
            get
            {
                return majors;
            }
            set
            {
                majors = value;
            }
        }

        public int Minors
        {
            get
            {
                return minors;
            }
            set
            {
                minors = value;
            }
        }

        public int MultiplyMajors
        {
            get
            {
                return multiplyMajors;
            }
        }

        public int MultiplyMinors
        {
            get
            {
                return multiplyMinors;
            }
        }

        public bool IgnoresMinors
        {
            get
            {
                return ignoreMinors;
            }
        }

        public bool IsHTrain
        {
            get
            {
                return isHTrain;
            }
        }

        public bool IsETrain
        {
            get
            {
                return isETrain;
            }
        }

        public string TrainName
        {
            get
            {
                return trainName;
            }
            set
            {
                trainName = value;
            }
        }

        public Train RailsTrain
        {
            get
            {
                return railsTrain;
            }
        }

        public TrainType RailsTrainType
        {
            get
            {
                if (railsTrain == null) return null;

                return railsTrain.GetTrainType();
            }
        }

        public string Attributes
        {
            get
            {
                StringBuilder attributes = new StringBuilder();
                attributes.Append("majors = " + majors);
                attributes.Append(", minors = " + minors);
                attributes.Append(", ignoreMinors = " + ignoreMinors);
                attributes.Append(", mulitplyMajors = " + multiplyMajors);
                attributes.Append(", mulitplyMinors = " + multiplyMinors);
                attributes.Append(", isHTrain = " + isHTrain);
                return attributes.ToString();
            }
        }

        override public string ToString()
        {
            return trainName;
        }


        /**
         * Comparator on trains as defined by train domination
         * 
         * A train dominates:
         * it has to be longer in either majors and minors
         * and at least equally long in both
         * 
         * Furthermore the dominating train has at least the same multiples as the shorter
         */

        public int CompareTo(NetworkTrain other)
        {

            // Check if A is the longer train first
            bool longerA = this.majors > other.majors && this.minors >= other.minors || this.majors == other.majors && this.minors > other.minors;

            if (longerA)
            {
                // then check the multiples
                if (this.multiplyMajors >= other.multiplyMajors && this.multiplyMinors >= other.multiplyMinors)
                {
                    return 1;
                }
                else
                {
                    return 0;
                }
            }
            else
            {
                // otherwise B might B longer
                bool longerB = this.majors < other.majors && this.minors <= other.minors || this.majors == other.majors && this.minors < other.minors;
                if (longerB)
                {
                    // then check the multiples
                    if (this.multiplyMajors <= other.multiplyMajors && this.multiplyMinors <= other.multiplyMinors)
                    {
                        return -1;
                    }
                    else
                    {
                        return 0;
                    }
                }
                else
                {
                    // none is longer
                    return 0;
                }
            }
        }
    }
}
