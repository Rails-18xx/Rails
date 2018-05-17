using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class TrainType : ICloneable
    {
        public const int TOWN_COUNT_MAJOR = 2;
        public const int TOWN_COUNT_MINOR = 1;
        public const int NO_TOWN_COUNT = 0;

        protected string name;
        protected TrainCertificateType certificateType;

        protected string reachBasis = "stops";
        protected bool countHexes = false;

        protected string countTowns = "major";
        protected int townCountIndicator = TOWN_COUNT_MAJOR;

        protected string scoreTowns = "yes";
        protected int townScoreFactor = 1;

        protected string scoreCities = "single";
        protected int cityScoreFactor = 1;

        protected int cost;
        protected int majorStops;
        protected int minorStops;

        protected int lastIndex = 0;

        protected BooleanState rusted;

        protected TrainManager trainManager;

        /** In some cases, trains start their life in the Pool */
        protected string initialPortfolio = "IPO";

        protected static Logger<TrainType> log = new Logger<TrainType>();

        /**
         * @param real False for the default type, else real. The default type does
         * not have top-level attributes.
         */
        public TrainType()
        {
        }

        /**
         * @see rails.common.parser.ConfigurableComponent#configureFromXML(org.w3c.dom.Element)
         */
        public void ConfigureFromXML(Tag tag)
        {

            // Name
            name = tag.GetAttributeAsString("name");

            // Cost
            cost = tag.GetAttributeAsInteger("cost");

            // Major stops
            majorStops = tag.GetAttributeAsInteger("majorStops");

            // Minor stops
            minorStops = tag.GetAttributeAsInteger("minorStops");

            // Reach
            Tag reachTag = tag.GetChild("Reach");
            if (reachTag != null)
            {
                // Reach basis
                reachBasis = reachTag.GetAttributeAsString("base", reachBasis);

                // Are towns counted (only relevant is reachBasis = "stops")
                countTowns = reachTag.GetAttributeAsString("countTowns", countTowns);
            }

            // Score
            Tag scoreTag = tag.GetChild("Score");
            if (scoreTag != null)
            {
                // Reach basis
                scoreTowns = scoreTag.GetAttributeAsString("scoreTowns", scoreTowns);

                // Are towns counted (only relevant is reachBasis = "stops")
                scoreCities = scoreTag.GetAttributeAsString("scoreCities", scoreCities);
            }

            // Check the reach and score values
            countHexes = reachBasis.Equals("hexes");
            townCountIndicator =
                countTowns.Equals("no") ? NO_TOWN_COUNT : minorStops > 0
                        ? TOWN_COUNT_MINOR : TOWN_COUNT_MAJOR;
            cityScoreFactor = scoreCities.Equals("double", StringComparison.OrdinalIgnoreCase) ? 2 : 1;
            townScoreFactor = scoreTowns.Equals("yes", StringComparison.OrdinalIgnoreCase) ? 1 : 0;
            // Actually we should meticulously check all values....
            // #TODO

        }

        public void FinishConfiguration(RailsRoot root, TrainCertificateType trainCertificateType)

        {
            trainManager = root.TrainManager;
            this.certificateType = trainCertificateType;

            if (name == null)
            {
                throw new ConfigurationException("No name specified for Train");
            }
            if (cost == 0)
            {
                throw new ConfigurationException("No price specified for Train " + name);
            }
            if (majorStops == 0)
            {
                throw new ConfigurationException("No major stops specified for Train " + name);
            }
        }

        public TrainCertificateType CertificateType
        {
            get
            {
                return certificateType;
            }
        }

        /**
         * @return Returns the cityScoreFactor.
         */
        public int CityScoreFactor
        {
            get
            {
                return cityScoreFactor;
            }
        }

        /**
         * @return Returns the cost.
         */
        public int Cost
        {
            get
            {
                return cost;
            }
        }

        /**
         * @return Returns the countHexes.
         */
        public bool CountsHexes
        {
            get
            {
                return countHexes;
            }
        }

        /**
         * @return Returns the majorStops.
         */
        public int MajorStops
        {
            get
            {
                return majorStops;
            }
        }

        /**
         * @return Returns the minorStops.
         */
        public int MinorStops
        {
            get
            {
                return minorStops;
            }
        }

        /**
         * @return Returns the name.
         */
        public string Name
        {
            get
            {
                return name;
            }
        }

        /**
         * @return Returns the townCountIndicator.
         */
        public int TownCountIndicator
        {
            get
            {
                return townCountIndicator;
            }
        }

        /**
         * @return Returns the townScoreFactor.
         */
        public int TownScoreFactor
        {
            get
            {
                return townScoreFactor;
            }
        }


        public object Clone()
        {
            return MemberwiseClone();
            //object clone = null;
            //try
            //{
            //    clone = super.clone();
            //}
            //catch (CloneNotSupportedException e)
            //{
            //    log.error("Cannot clone traintype " + name, e);
            //    return null;
            //}

            //return clone;
        }

        public TrainManager TrainManager
        {
            get
            {
                return trainManager;
            }
        }

        public string GetInfo()
        {
            StringBuilder b = new StringBuilder("<html>");
            b.Append(LocalText.GetText("TrainInfo", name, Bank.Format(trainManager, cost), 0));
            if (b.Length == 6) b.Append(LocalText.GetText("None"));

            return b.ToString();
        }

        override public string ToString()
        {
            return name;
        }
    }
}
