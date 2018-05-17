using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class TrainsModel : RailsModel
    {
        public const string ID = "TrainsModel";

        private PortfolioSet<Train> trains;

        private bool abbrList = false;

        private TrainsModel(IRailsOwner parent, string id) : base(parent, id)
        {
            trains = PortfolioSet<Train>.Create(parent, "trains");
            trains.AddModel(this);
        }

        /** 
         * @return fully initialized TrainsModel
         */
        public static TrainsModel Create(IRailsOwner parent)
        {
            return new TrainsModel(parent, ID);
        }

        new public IRailsOwner Parent
        {
            get
            {
                return (IRailsOwner)base.Parent;
            }
        }

        public Portfolio<Train> Portfolio
        {
            get
            {
                return trains;
            }
        }

        public void SetAbbrList(bool abbrList)
        {
            this.abbrList = abbrList;
        }

        public IReadOnlyCollection<Train> Trains
        {
            get
            {
                return trains.Items;
            }
        }

        public Train GetTrainOfType(TrainCertificateType type)
        {
            foreach (Train train in trains)
            {
                if (train.CertType == type) return train;
            }
            return null;
        }

        /**
         * Make a full list of trains, like "2 2 3 3", to show in any field
         * describing train possessions, except the IPO.
         */
        private string MakeListOfTrains()
        {
            if (trains.IsEmpty) return "";

            StringBuilder b = new StringBuilder();
            foreach (Train train in trains)
            {
                if (b.Length > 0) b.Append(" ");
                if (train.IsObsolete()) b.Append("[");
                b.Append(train.ToText());
                if (train.IsObsolete()) b.Append("]");
            }

            return b.ToString();
        }

        /**
         * Make an abbreviated list of trains, like "2(6) 3(5)" etc, to show in the
         * IPO.
         */
        public string MakeListOfTrainCertificates()
        {
            if (trains.IsEmpty) return "";

            // create a bag with train types
            List<TrainCertificateType> trainCertTypes = new List<TrainCertificateType>();
            foreach (Train train in trains)
            {
                trainCertTypes.Add(train.CertType);
            }

            StringBuilder b = new StringBuilder();
            trainCertTypes.Sort();
            foreach (TrainCertificateType certType in trainCertTypes)
            {
                if (b.Length > 0) b.Append(" ");
                b.Append(certType.ToText()).Append("(");
                if (certType.HasInfiniteQuantity)
                {
                    b.Append("+");
                }
                else
                {
                    b.Append(trainCertTypes.Count(p => p == certType));
                }
                b.Append(")");
            }

            return b.ToString();
        }

        override public string ToText()
        {
            if (!abbrList)
            {
                return MakeListOfTrains();
            }
            else
            {
                return MakeListOfTrainCertificates();
            }
        }
    }
}
